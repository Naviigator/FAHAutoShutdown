package com.naviigator.FahAutoShutdown;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class Launcher {

    private static final String ABORT_SHUTDOWN_COMMAND = "shutdown -a";
    private static final String SHUTDOWN_CMD = "shutdown -s -t 60";

    public static void main(String[] args) {
        JPanel resultPanel = new JPanel();
        resultPanel.setLayout(new BoxLayout(resultPanel, BoxLayout.PAGE_AXIS));
        JLabel statusLabel = new JLabel("Initializing...");
        statusLabel.setMinimumSize(new Dimension(280, 10));
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.PAGE_AXIS));
        mainPanel.add(resultPanel, BorderLayout.PAGE_START);
        mainPanel.add(statusLabel, BorderLayout.PAGE_END);
        JFrame frame = new JFrame();
        frame.setTitle("FAH Autoshutdown");
        frame.setMinimumSize(new Dimension(300, 100));
        frame.add(mainPanel);
        frame.pack();
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setVisible(true);

        final Settings settings = new Settings();

        final Map<String, JLabel> internalLabels = new HashMap<>();
        new Checker(result -> SwingUtilities.invokeLater(() -> {
            System.out.println(statusLabel.getSize());
            boolean addNewPanels = false;

            for (FahJobDescription value : result.jobDescriptions) {
                if (!internalLabels.containsKey(value.id)) {
                    addNewPanels = true;
                    break;
                }
            }
            if (addNewPanels) {
                resultPanel.removeAll();
                internalLabels.clear();
                settings.reset();

                for (FahJobDescription value : result.jobDescriptions) {
                    JLabel label = new JLabel();
                    settings.setCheck(value.id, true);
                    JCheckBox includeInCheck = new JCheckBox();
                    includeInCheck.setSelected(true);
                    includeInCheck.addChangeListener(e -> settings.setCheck(value.id, includeInCheck.isSelected()));
                    internalLabels.put(value.id, label);
                    JPanel panel = new JPanel();
                    panel.add(label);
                    panel.add(includeInCheck);
                    resultPanel.add(panel);
                }

            }

            for (FahJobDescription value : result.jobDescriptions) {
                String status = "<html>" +
                        "id: " + value.id + "<br/>" +
                        "eta: " + value.eta + "<br/>" +
                        "status: " + value.status + "<br/>" +
                        "</html>";
                internalLabels.get(value.id).setText(status);
            }
            resultPanel.invalidate();


            String statusMessage = result.message;
            if (settings.isEmpty()) {
                statusMessage = "Nothing selected, ignoring to check settings.<br/>Also: " + result.message;
            }
            statusLabel.setText("<html><font color='red'>" + statusMessage + "</html>");
            SwingUtilities.invokeLater(frame::pack);
        }), () -> SwingUtilities.invokeLater(() -> {
            try {
                resultPanel.removeAll();
                internalLabels.clear();
                final JButton cancelShutdownButton = new JButton("Cancel shutdown");
                cancelShutdownButton.addActionListener(ignored -> {
                    try {
                        Runtime.getRuntime().exec(ABORT_SHUTDOWN_COMMAND);
                        resultPanel.removeAll();
                        statusLabel.setText("Shutdown aborted!");
                        frame.pack();
                    } catch (IOException e) {
                        JOptionPane.showMessageDialog(frame,
                                "Could not abort the shutdown try the command + `" + ABORT_SHUTDOWN_COMMAND + "` manually!\n" + e.getMessage(),
                                "Error aborting shutdown!",
                                JOptionPane.ERROR_MESSAGE);
                    }
                });
                resultPanel.add(cancelShutdownButton);
                statusLabel.setText("Finished! shutting down the system.");
                frame.pack();
                Runtime.getRuntime().exec(SHUTDOWN_CMD);
            } catch (IOException e) {
                JOptionPane.showMessageDialog(frame, e.getMessage(), "Could not shut down...", JOptionPane.ERROR_MESSAGE);
            }
        }) ,settings);
    }
}
