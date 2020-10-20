package com.naviigator.FahAutoShutdown;

import java.util.HashSet;
import java.util.Set;

public class Settings {
    private Set<String> idsToCheck = null;

    public synchronized void setCheck(String id, boolean check) {
        if (idsToCheck == null) {
            idsToCheck = new HashSet<>();
        }
        if (check) {
            idsToCheck.add(id);
        } else {
            idsToCheck.remove(id);
        }
    }

    public synchronized boolean shouldCheck(String id) {
        return idsToCheck == null || idsToCheck.isEmpty() || idsToCheck.contains(id);
    }

    public synchronized void reset() {
        idsToCheck = null;
    }

    public boolean isEmpty() {
        return idsToCheck.isEmpty();
    }
}
