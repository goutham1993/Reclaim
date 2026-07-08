package com.investment.pausetime.model;

public class MonitoredApp {
    private String packageName;
    private String appName;
    private int delaySeconds;
    private boolean enabled;

    // Temporary reduction feature. When the mode is enabled the effective delay
    // can be temporarily lowered until an expiry time, after which it reverts to
    // the original delaySeconds.
    private boolean tempReductionModeEnabled;
    private int tempReducedDelaySeconds;
    private long tempReductionExpiryMillis;

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

    public boolean isTempReductionModeEnabled() {
        return tempReductionModeEnabled;
    }

    public void setTempReductionModeEnabled(boolean tempReductionModeEnabled) {
        this.tempReductionModeEnabled = tempReductionModeEnabled;
    }

    public int getTempReducedDelaySeconds() {
        return tempReducedDelaySeconds;
    }

    public void setTempReducedDelaySeconds(int tempReducedDelaySeconds) {
        this.tempReducedDelaySeconds = tempReducedDelaySeconds;
    }

    public long getTempReductionExpiryMillis() {
        return tempReductionExpiryMillis;
    }

    public void setTempReductionExpiryMillis(long tempReductionExpiryMillis) {
        this.tempReductionExpiryMillis = tempReductionExpiryMillis;
    }

    /**
     * Whether a temporary reduction is currently active (mode on, a reduced
     * value set, and the expiry time still in the future).
     */
    public boolean isTemporaryReductionActive() {
        return tempReductionModeEnabled
                && tempReducedDelaySeconds > 0
                && tempReductionExpiryMillis > System.currentTimeMillis();
    }

    /**
     * The delay that should actually be enforced right now. Falls back to the
     * original delaySeconds once any temporary reduction has expired.
     */
    public int getEffectiveDelaySeconds() {
        if (isTemporaryReductionActive()) {
            return tempReducedDelaySeconds;
        }
        return delaySeconds;
    }

    /**
     * Milliseconds remaining before the temporary reduction expires, or 0 if
     * none is active.
     */
    public long getTemporaryRemainingMillis() {
        if (!isTemporaryReductionActive()) {
            return 0;
        }
        return tempReductionExpiryMillis - System.currentTimeMillis();
    }
}

