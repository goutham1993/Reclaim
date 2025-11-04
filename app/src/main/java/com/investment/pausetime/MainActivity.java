package com.investment.pausetime;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.view.accessibility.AccessibilityManager;
import android.widget.LinearLayout;

import com.google.android.material.snackbar.Snackbar;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.investment.pausetime.adapter.MonitoredAppAdapter;
import com.investment.pausetime.databinding.ActivityMainBinding;
import com.investment.pausetime.model.MonitoredApp;
import com.investment.pausetime.repository.AppRepository;

import android.view.Menu;
import android.view.MenuItem;

import java.util.List;

public class MainActivity extends AppCompatActivity implements MonitoredAppAdapter.OnAppActionListener {

    private ActivityMainBinding binding;
    private AppRepository repository;
    private MonitoredAppAdapter adapter;
    private LinearLayout welcomeLayout;
    private LinearLayout contentLayout;
    private RecyclerView recyclerView;
    private boolean permissionDialogShown = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        repository = new AppRepository(this);

        // Initialize views
        welcomeLayout = findViewById(R.id.welcomeLayout);
        contentLayout = findViewById(R.id.contentLayout);
        recyclerView = findViewById(R.id.recyclerView);

        // Setup RecyclerView
        adapter = new MonitoredAppAdapter(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        binding.fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                checkPermissionsAndOpenAppList();
            }
        });
        
        // Load monitored apps
        loadMonitoredApps();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Only reset permission dialog flag if permissions are still missing
        // This allows re-checking when returning from settings
        if (hasOverlayPermission() && isAccessibilityServiceEnabled()) {
            // Permissions are granted, don't show dialog again
            permissionDialogShown = true;
        } else {
            // Permissions are missing, allow showing dialog again if needed
            permissionDialogShown = false;
        }
        // Reload apps when returning from other activities
        loadMonitoredApps();
    }

    private void loadMonitoredApps() {
        List<MonitoredApp> apps = repository.getMonitoredApps();
        
        if (apps.isEmpty()) {
            // Show welcome screen
            welcomeLayout.setVisibility(View.VISIBLE);
            contentLayout.setVisibility(View.GONE);
        } else {
            // Show monitored apps
            welcomeLayout.setVisibility(View.GONE);
            contentLayout.setVisibility(View.VISIBLE);
            adapter.setAppList(apps);
            
            // Show permission reminder only if apps are monitored but permissions are missing
            checkPermissionsForMonitoredApps();
        }
    }
    
    private void checkPermissionsForMonitoredApps() {
        // Only show permission reminder once per session if we have monitored apps but missing permissions
        if (!permissionDialogShown && (!hasOverlayPermission() || !isAccessibilityServiceEnabled())) {
            permissionDialogShown = true;
            Snackbar.make(binding.getRoot(), 
                    "Permissions needed for Reclaim to work", 
                    Snackbar.LENGTH_LONG)
                    .setAction("Enable", v -> {
                        if (!hasOverlayPermission()) {
                            showOverlayPermissionDialog();
                        } else {
                            showAccessibilityPermissionDialog();
                        }
                    })
                    .show();
        }
    }

    private void checkPermissionsAndOpenAppList() {
        if (!hasOverlayPermission()) {
            showOverlayPermissionDialog();
        } else if (!isAccessibilityServiceEnabled()) {
            showAccessibilityPermissionDialog();
        } else {
            openAppListActivity();
        }
    }

    private boolean hasOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return Settings.canDrawOverlays(this);
        }
        return true;
    }

    private boolean isAccessibilityServiceEnabled() {
        // Use AccessibilityManager for more reliable detection
        AccessibilityManager accessibilityManager = (AccessibilityManager) getSystemService(ACCESSIBILITY_SERVICE);
        if (accessibilityManager != null) {
            List<AccessibilityServiceInfo> enabledServices = accessibilityManager.getEnabledAccessibilityServiceList(
                    AccessibilityServiceInfo.FEEDBACK_ALL_MASK);
            String serviceId = getPackageName() + "/com.investment.pausetime.service.AppMonitoringService";
            for (AccessibilityServiceInfo serviceInfo : enabledServices) {
                String id = serviceInfo.getId();
                if (id != null && id.equals(serviceId)) {
                    return true;
                }
            }
        }
        
        // Fallback to Settings check
        String service = getPackageName() + "/" + 
                "com.investment.pausetime.service.AppMonitoringService";
        try {
            int accessibilityEnabled = Settings.Secure.getInt(
                    getContentResolver(),
                    Settings.Secure.ACCESSIBILITY_ENABLED);
            if (accessibilityEnabled == 1) {
                String settingValue = Settings.Secure.getString(
                        getContentResolver(),
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

    private void showOverlayPermissionDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Overlay Permission Required")
                .setMessage("Reclaim needs permission to draw over other apps to show the delay screen.")
                .setPositiveButton("Grant Permission", (dialog, which) -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:" + getPackageName()));
                        startActivity(intent);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showAccessibilityPermissionDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Accessibility Service Required")
                .setMessage("Reclaim needs accessibility service enabled to detect when monitored apps are opened.\n\nPlease enable 'Reclaim' in Accessibility settings.")
                .setPositiveButton("Open Settings", (dialog, which) -> {
                    Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                    startActivity(intent);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void openAppListActivity() {
        Intent intent = new Intent(this, AppListActivity.class);
        startActivity(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            // Open settings activity to manage monitored apps
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onDelayChanged(MonitoredApp app, int newDelay) {
        repository.updateAppDelay(app.getPackageName(), newDelay);
        loadMonitoredApps();
    }

    @Override
    public void onAppDeleted(MonitoredApp app) {
        repository.removeMonitoredApp(app.getPackageName());
        loadMonitoredApps();
    }
}