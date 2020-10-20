package com.naviigator.FahAutoShutdown;

import java.util.Collections;
import java.util.List;

public class Result {
    public final String message;
    public final List<FahJobDescription> jobDescriptions;

    public Result(String message) {
        this(message, Collections.emptyList());
    }

    public Result(List<FahJobDescription> jobDescriptions) {
        this("", jobDescriptions);
    }

    public Result(String message, List<FahJobDescription> jobDescriptions) {
        this.message = message;
        this.jobDescriptions = Collections.unmodifiableList(jobDescriptions);
    }
}
