package com.naviigator.FahAutoShutdown;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class Launcher {

    public static void main(String[] args) {
        JPanel resultPanel = new JPanel();
        resultPanel.setLayout(new BoxLayout(resultPanel, BoxLayout.PAGE_AXIS));
        JLabel statusLabel = new JLabel("Initializing...");
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.PAGE_AXIS));
        mainPanel.add(resultPanel, BorderLayout.CENTER);
        mainPanel.add(statusLabel);
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
                    JLabel label = new JLabel("test");
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
            if(settings.isEmpty()) {
                statusMessage = "Nothing selected, ignoring to check settings. Also: " + result.message;
            }
            statusLabel.setText(statusMessage);
            frame.pack();
        }), settings);
    }
}
