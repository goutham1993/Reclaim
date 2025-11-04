package com.investment.pausetime.repository;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.investment.pausetime.model.MonitoredApp;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class AppRepository {
    private static final String PREFS_NAME = "ReclaimPrefs";
    private static final String KEY_MONITORED_APPS = "monitored_apps";
    
    private final SharedPreferences sharedPreferences;
    private final Gson gson;

    public AppRepository(Context context) {
        this.sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.gson = new Gson();
    }

    public void saveMonitoredApps(List<MonitoredApp> apps) {
        String json = gson.toJson(apps);
        sharedPreferences.edit()
                .putString(KEY_MONITORED_APPS, json)
                .apply();
    }

    public List<MonitoredApp> getMonitoredApps() {
        String json = sharedPreferences.getString(KEY_MONITORED_APPS, null);
        if (json == null) {
            return new ArrayList<>();
        }
        
        Type listType = new TypeToken<ArrayList<MonitoredApp>>(){}.getType();
        return gson.fromJson(json, listType);
    }

    public MonitoredApp getMonitoredApp(String packageName) {
        List<MonitoredApp> apps = getMonitoredApps();
        for (MonitoredApp app : apps) {
            if (app.getPackageName().equals(packageName)) {
                return app;
            }
        }
        return null;
    }

    public void updateAppDelay(String packageName, int delaySeconds) {
        List<MonitoredApp> apps = getMonitoredApps();
        for (MonitoredApp app : apps) {
            if (app.getPackageName().equals(packageName)) {
                app.setDelaySeconds(delaySeconds);
                break;
            }
        }
        saveMonitoredApps(apps);
    }

    public void removeMonitoredApp(String packageName) {
        List<MonitoredApp> apps = getMonitoredApps();
        apps.removeIf(app -> app.getPackageName().equals(packageName));
        saveMonitoredApps(apps);
    }

    public boolean isAppMonitored(String packageName) {
        List<MonitoredApp> apps = getMonitoredApps();
        for (MonitoredApp app : apps) {
            if (app.getPackageName().equals(packageName) && app.isEnabled()) {
                return true;
            }
        }
        return false;
    }
}

