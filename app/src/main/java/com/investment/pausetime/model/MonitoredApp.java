package com.investment.pausetime.model;

public class MonitoredApp {
    private String packageName;
    private String appName;
    private int delaySeconds;
    private boolean enabled;

    public MonitoredApp() {
        this.delaySeconds = 45; // Default 45 seconds
        this.enabled = true;
    }

    public MonitoredApp(String packageName, String appName, int delaySeconds, boolean enabled) {
        this.packageName = packageName;
        this.appName = appName;
        this.delaySeconds = delaySeconds;
        this.enabled = enabled;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public int getDelaySeconds() {
        return delaySeconds;
    }

    public void setDelaySeconds(int delaySeconds) {
        this.delaySeconds = delaySeconds;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}

