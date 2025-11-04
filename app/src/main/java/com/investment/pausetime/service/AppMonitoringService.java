package com.investment.pausetime.service;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.os.Handler;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;

import com.investment.pausetime.model.MonitoredApp;
import com.investment.pausetime.repository.AppRepository;

import java.util.HashSet;
import java.util.Set;

public class AppMonitoringService extends AccessibilityService {

    private static final String TAG = "AppMonitoringService";
    private static final long DEBOUNCE_DELAY = 1000; // 1 second debounce for complex apps
    private static final long COOLDOWN_PERIOD = 3000; // 3 second cooldown between overlay shows
    
    private AppRepository repository;
    private String currentPackageName = "";
    private String lastMonitoredPackage = ""; // Track the last app we showed overlay for
    private Set<String> activeOverlays = new HashSet<>();
    private Set<String> pendingOverlays = new HashSet<>(); // Track overlays scheduled but not yet shown
    private Set<String> recentlyShownOverlays = new HashSet<>(); // Track apps that recently showed overlay
    private Handler handler = new Handler();
    private Runnable pendingShowOverlayRunnable;
    private long lastEventTime = 0;
    private String lastEventPackage = "";

    @Override
    public void onCreate() {
        super.onCreate();
        repository = new AppRepository(this);
        Log.d(TAG, "Service created");
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            if (event.getPackageName() != null) {
                String packageName = event.getPackageName().toString();
                
                // Debounce rapid events from the same package (like YouTube with multiple windows)
                long currentTime = System.currentTimeMillis();
                if (packageName.equals(lastEventPackage) && 
                    (currentTime - lastEventTime) < DEBOUNCE_DELAY) {
                    Log.d(TAG, "Ignoring rapid duplicate event for: " + packageName);
                    return;
                }
                lastEventTime = currentTime;
                lastEventPackage = packageName;
                
                Log.d(TAG, "App opened: " + packageName);
                
                // Don't block our own app or system UI
                if (packageName.equals(getPackageName()) || 
                    packageName.equals("com.android.systemui") ||
                    packageName.equals("com.google.android.apps.nexuslauncher") ||
                    packageName.equals("com.android.launcher3")) {
                    return;
                }
                
                // Check if user is switching away from a monitored app
                if (!packageName.equals(lastMonitoredPackage) && 
                    !lastMonitoredPackage.isEmpty()) {
                    
                    Log.d(TAG, "Switching from " + lastMonitoredPackage + " to " + packageName);
                    
                    // Check if there's an active overlay to dismiss
                    if (activeOverlays.contains(lastMonitoredPackage)) {
                        Log.d(TAG, "Dismissing active overlay for: " + lastMonitoredPackage);
                        Intent dismissIntent = new Intent(this, OverlayService.class);
                        dismissIntent.setAction(OverlayService.ACTION_DISMISS);
                        startService(dismissIntent);
                        activeOverlays.remove(lastMonitoredPackage);
                        
                        // Remove from cooldown so it can show again when reopened
                        recentlyShownOverlays.remove(lastMonitoredPackage);
                    }
                    
                    // Check if there's a pending overlay to cancel
                    if (pendingOverlays.contains(lastMonitoredPackage)) {
                        Log.d(TAG, "Canceling pending overlay for: " + lastMonitoredPackage);
                        if (pendingShowOverlayRunnable != null) {
                            handler.removeCallbacks(pendingShowOverlayRunnable);
                            pendingShowOverlayRunnable = null;
                        }
                        pendingOverlays.remove(lastMonitoredPackage);
                        
                        // Remove from cooldown
                        recentlyShownOverlays.remove(lastMonitoredPackage);
                    }
                    
                    // Clear last monitored package
                    lastMonitoredPackage = "";
                }
                
                // Only trigger if we're switching to a different app
                if (!packageName.equals(currentPackageName)) {
                    currentPackageName = packageName;
                    
                    // Check if this app is monitored
                    MonitoredApp monitoredApp = repository.getMonitoredApp(packageName);
                    if (monitoredApp != null && monitoredApp.isEnabled()) {
                        
                        // Don't show overlay if already showing or pending for this package
                        if (activeOverlays.contains(packageName)) {
                            Log.d(TAG, "Overlay already active for: " + packageName + ", skipping");
                            return;
                        }
                        
                        if (pendingOverlays.contains(packageName)) {
                            Log.d(TAG, "Overlay already pending for: " + packageName + ", skipping");
                            return;
                        }
                        
                        // Don't show overlay if recently shown (cooldown period)
                        if (recentlyShownOverlays.contains(packageName)) {
                            Log.d(TAG, "Overlay recently shown for: " + packageName + ", in cooldown period, skipping");
                            return;
                        }
                        
                        Log.d(TAG, "Scheduling overlay for: " + monitoredApp.getAppName());
                        
                        // Remember this package and mark as pending
                        lastMonitoredPackage = packageName;
                        pendingOverlays.add(packageName);
                        
                        // Small delay to ensure the app window is ready
                        final String finalPackageName = packageName;
                        pendingShowOverlayRunnable = new Runnable() {
                            @Override
                            public void run() {
                                // Only show if user is still in the app
                                if (finalPackageName.equals(currentPackageName)) {
                                    Log.d(TAG, "Showing overlay for: " + monitoredApp.getAppName());
                                    
                                    // Move from pending to active
                                    pendingOverlays.remove(finalPackageName);
                                    activeOverlays.add(finalPackageName);
                                    recentlyShownOverlays.add(finalPackageName);
                                    
                                    Intent intent = new Intent(AppMonitoringService.this, OverlayService.class);
                                    intent.putExtra("packageName", finalPackageName);
                                    intent.putExtra("appName", monitoredApp.getAppName());
                                    intent.putExtra("delaySeconds", monitoredApp.getDelaySeconds());
                                    startService(intent);
                                    
                                    // Schedule cleanup after delay completes
                                    handler.postDelayed(() -> {
                                        Log.d(TAG, "Cleaning up overlay state for: " + finalPackageName);
                                        activeOverlays.remove(finalPackageName);
                                        if (finalPackageName.equals(lastMonitoredPackage)) {
                                            lastMonitoredPackage = "";
                                        }
                                    }, (monitoredApp.getDelaySeconds() + 2) * 1000L);
                                    
                                    // Remove from cooldown after cooldown period
                                    handler.postDelayed(() -> {
                                        Log.d(TAG, "Cooldown period ended for: " + finalPackageName);
                                        recentlyShownOverlays.remove(finalPackageName);
                                    }, COOLDOWN_PERIOD);
                                } else {
                                    Log.d(TAG, "User left app before overlay could show, not showing overlay");
                                    pendingOverlays.remove(finalPackageName);
                                }
                                pendingShowOverlayRunnable = null;
                            }
                        };
                        
                        handler.postDelayed(pendingShowOverlayRunnable, 500);
                    }
                }
            }
        }
    }

    @Override
    public void onInterrupt() {
        Log.d(TAG, "Service interrupted");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (pendingShowOverlayRunnable != null) {
            handler.removeCallbacks(pendingShowOverlayRunnable);
            pendingShowOverlayRunnable = null;
        }
        handler.removeCallbacksAndMessages(null);
        activeOverlays.clear();
        pendingOverlays.clear();
        recentlyShownOverlays.clear();
        Log.d(TAG, "Service destroyed");
    }
}

