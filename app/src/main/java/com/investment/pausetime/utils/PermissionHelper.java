package com.investment.pausetime.utils;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Context;
import android.os.Build;
import android.provider.Settings;
import android.view.accessibility.AccessibilityManager;

import java.util.List;

public class PermissionHelper {

    public static boolean hasOverlayPermission(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return Settings.canDrawOverlays(context);
        }
        return true;
    }

    public static boolean isAccessibilityServiceEnabled(Context context) {
        // Use AccessibilityManager for more reliable detection
        AccessibilityManager accessibilityManager = (AccessibilityManager) context.getSystemService(Context.ACCESSIBILITY_SERVICE);
        if (accessibilityManager != null) {
            List<AccessibilityServiceInfo> enabledServices = accessibilityManager.getEnabledAccessibilityServiceList(
                    AccessibilityServiceInfo.FEEDBACK_ALL_MASK);
            String serviceId = context.getPackageName() + "/com.investment.pausetime.service.AppMonitoringService";
            for (AccessibilityServiceInfo serviceInfo : enabledServices) {
                String id = serviceInfo.getId();
                if (id != null && id.equals(serviceId)) {
                    return true;
                }
            }
        }
        
        // Fallback to Settings check
        String service = context.getPackageName() + "/" + 
                "com.investment.pausetime.service.AppMonitoringService";
        try {
            int accessibilityEnabled = Settings.Secure.getInt(
                    context.getContentResolver(),
                    Settings.Secure.ACCESSIBILITY_ENABLED);
            if (accessibilityEnabled == 1) {
                String settingValue = Settings.Secure.getString(
                        context.getContentResolver(),
                        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
                if (settingValue != null) {
                    return settingValue.contains(service);
                }
            }
        } catch (Settings.SettingNotFoundException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean areAllPermissionsGranted(Context context) {
        return hasOverlayPermission(context) && isAccessibilityServiceEnabled(context);
    }
}

