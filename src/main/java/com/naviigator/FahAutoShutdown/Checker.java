package com.naviigator.FahAutoShutdown;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class Checker {
    private final Consumer<Result> resultSetter;
    private final Runnable shutdownProcedure;
    private final Settings settings;
    private volatile boolean running = true;

    private String sessionId;
    private int cacheKiller = new Random().nextInt();

    private ObjectMapper mapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    public Checker(Consumer<Result> resultSetter, Runnable shutdownProcedure, Settings settings) {
        this.resultSetter = resultSetter;
        this.shutdownProcedure = shutdownProcedure;
        this.settings = settings;
        if (cacheKiller < 0) {
            cacheKiller *= -1;
        }
        start();
    }

    private void start() {
        try {
            sessionId = getWebResult("http://127.0.0.1:7396/api/session", "PUT", "", true);

            int updateId = 0;
            int updateRate = 10;
            getWebResult("http://127.0.0.1:7396/api/updates/set", "GET", "sid=" + sessionId + "&update_id=" + updateId + "&update_rate=" + updateRate + "&update_path=%2Fapi%2Fslots", false);

            getWebResult("http://127.0.0.1:7396/api/configured", "GET", "sid=" + sessionId + "&update_id=" + updateId, false);

            Executors.newSingleThreadExecutor().execute(() -> run(updateRate));
        } catch (Exception e) {
            System.out.println("something went wrong");
            System.out.println(e);
            System.exit(-1);
        }
    }

    private String getWebResult(String inputUrl, String method, String payload, boolean expectResult) throws IOException {
        if (!payload.isEmpty()) {
            payload = payload + "&";
        }
        payload += "_=" + cacheKiller++;
        String fullUrl = inputUrl + "?" + payload;
        System.out.println(fullUrl);
        URL url = new URL(fullUrl);
        HttpURLConnection httpCon = (HttpURLConnection) url.openConnection();
        httpCon.setRequestProperty("accept", "application/json, text/javascript, */*; q=0.01");
        httpCon.setRequestMethod(method);

        if (method.equals("PUT")) {
            httpCon.setDoOutput(true);
            OutputStreamWriter out = new OutputStreamWriter(
                    httpCon.getOutputStream());
            out.write(payload);
            out.close();
        }

        InputStream inputStream = httpCon.getInputStream();
        String result = "";
        if (expectResult) {
            result = new BufferedReader(new InputStreamReader(inputStream))
                    .lines().collect(Collectors.joining("\n"));
        }
        inputStream.close();
        httpCon.disconnect();

        System.out.println(result);
        return result;
    }

    private void run(int updateRate) {
        while (running) {
            FahJobDescriptions descriptions = null;
            try {
                String json = getWebResult("http://127.0.0.1:7396/api/updates", "GET", "sid=" + sessionId, true);
                if ("[[\"reset\"]]".equals(json)) {
                    stop();
                    new Checker(resultSetter, shutdownProcedure, settings);
                    continue;
                }
                if("[[\"heartbeat\"]]".equals(json)) {
                    throw new IllegalArgumentException("Only heartbeat recieved - a correct update should follow shortly");
                }
                final String start = "[[\"/api/slots\", [\n";
                if (!json.startsWith(start)) {
                    throw new IllegalArgumentException("unexpected begin");
                }
                final String end = "\n]]]";
                if (!json.endsWith(end)) {
                    throw new IllegalArgumentException("unexpected end");
                }
                json = "{\"values\": [" + json.substring(start.length(), json.length() - end.length()) + "]}";
                descriptions = mapper.readValue(json, FahJobDescriptions.class);

                int runningJobs = 0;
                boolean cancelProcessing = false;
                for (FahJobDescription value : descriptions.values) {
                    if (settings.shouldCheck(value.id)) {
                        if (value.status.equals(FahJobDescription.RUNNING)) {
                            cancelProcessing = true;
                            break;
                        } else if (!value.status.equals(FahJobDescription.PAUSED)) {
                            ++runningJobs;
                        }
                    }
                }
                if (cancelProcessing) {
                    setErrorStatusWithResult("still running - please finish!", descriptions.values);
                } else {
                    setResult(descriptions.values);
                    if (runningJobs == 0) {
                        setErrorStatus("Shutting down!");
                        stop();
                        shutdownProcedure.run();
                    }
                }
            } catch (Exception e) {
                if (descriptions != null) {
                    setErrorStatusWithResult(e.getMessage(), descriptions.values);
                } else {
                    setErrorStatus(e.getMessage());
                }
            }

            try {
                Thread.sleep(updateRate * 1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void setErrorStatus(String status) {
        resultSetter.accept(new Result(status));
    }

    private void setErrorStatusWithResult(String status, List<FahJobDescription> descriptions) {
        resultSetter.accept(new Result(status, descriptions));
    }

    private void setResult(List<FahJobDescription> descriptions) {
        resultSetter.accept(new Result(descriptions));
    }

    public void stop() {
        running = false;
    }
}
