package com.investment.pausetime;

import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.investment.pausetime.adapter.AppListAdapter;
import com.investment.pausetime.model.AppInfo;
import com.investment.pausetime.model.MonitoredApp;
import com.investment.pausetime.repository.AppRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AppListActivity extends AppCompatActivity implements AppListAdapter.OnFilterListener {

    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private SearchView searchView;
    private TextView appCountText;
    private TextView noResultsText;
    private AppListAdapter adapter;
    private AppRepository repository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_list);

        repository = new AppRepository(this);
        
        recyclerView = findViewById(R.id.recyclerView);
        progressBar = findViewById(R.id.progressBar);
        searchView = findViewById(R.id.searchView);
        appCountText = findViewById(R.id.appCountText);
        noResultsText = findViewById(R.id.noResultsText);
        MaterialButton btnSave = findViewById(R.id.btnSave);
        MaterialButton btnCancel = findViewById(R.id.btnCancel);

        adapter = new AppListAdapter();
        adapter.setOnFilterListener(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        // Setup search functionality
        searchView.setIconifiedByDefault(false);
        searchView.setFocusable(true);
        searchView.setIconified(false);
        searchView.setSubmitButtonEnabled(false); // Disable submit button
        searchView.requestFocusFromTouch();
        searchView.clearFocus(); // Clear initial focus so keyboard doesn't auto-show
        
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                adapter.filter(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                adapter.filter(newText);
                return true;
            }
        });
        
        // Make search view clickable
        searchView.setOnClickListener(v -> {
            searchView.setIconified(false);
        });
        
        // Handle search view expand/collapse
        searchView.setOnQueryTextFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                searchView.setIconified(false);
            }
        });

        btnSave.setOnClickListener(v -> saveSelectedApps());
        btnCancel.setOnClickListener(v -> finish());

        loadInstalledApps();
    }

    private void loadInstalledApps() {
        new LoadAppsTask().execute();
    }

    private void saveSelectedApps() {
        List<AppInfo> selectedApps = adapter.getSelectedApps();
        List<MonitoredApp> monitoredApps = new ArrayList<>();

        for (AppInfo appInfo : selectedApps) {
            MonitoredApp monitoredApp = new MonitoredApp();
            monitoredApp.setPackageName(appInfo.getPackageName());
            monitoredApp.setAppName(appInfo.getAppName());
            monitoredApp.setDelaySeconds(45); // Default 45 seconds
            monitoredApp.setEnabled(true);
            monitoredApps.add(monitoredApp);
        }

        // Merge with existing apps
        List<MonitoredApp> existingApps = repository.getMonitoredApps();
        for (MonitoredApp existingApp : existingApps) {
            boolean stillMonitored = false;
            for (MonitoredApp newApp : monitoredApps) {
                if (newApp.getPackageName().equals(existingApp.getPackageName())) {
                    stillMonitored = true;
                    // Keep the existing delay setting
                    newApp.setDelaySeconds(existingApp.getDelaySeconds());
                    break;
                }
            }
            // If an app was previously monitored but not selected now, remove it
            if (!stillMonitored) {
                // It will be excluded from the new list
            }
        }

        repository.saveMonitoredApps(monitoredApps);
        
        // Return to main activity
        finish();
    }

    private class LoadAppsTask extends AsyncTask<Void, Void, List<AppInfo>> {

        @Override
        protected void onPreExecute() {
            progressBar.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        }

        @Override
        protected List<AppInfo> doInBackground(Void... voids) {
            List<AppInfo> appList = new ArrayList<>();
            PackageManager pm = getPackageManager();
            List<ApplicationInfo> packages = pm.getInstalledApplications(PackageManager.GET_META_DATA);
            
            List<MonitoredApp> monitoredApps = repository.getMonitoredApps();

            for (ApplicationInfo packageInfo : packages) {
                try {
                    // Only show apps that have a launcher icon (launchable apps)
                    if (pm.getLaunchIntentForPackage(packageInfo.packageName) != null) {
                        String appName = pm.getApplicationLabel(packageInfo).toString();
                        AppInfo appInfo = new AppInfo(
                                packageInfo.packageName,
                                appName,
                                pm.getApplicationIcon(packageInfo)
                        );
                        
                        // Check if this app is already monitored
                        for (MonitoredApp monitoredApp : monitoredApps) {
                            if (monitoredApp.getPackageName().equals(packageInfo.packageName)) {
                                appInfo.setSelected(true);
                                break;
                            }
                        }
                        
                        appList.add(appInfo);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            // Sort by app name
            Collections.sort(appList, (a, b) -> a.getAppName().compareToIgnoreCase(b.getAppName()));

            return appList;
        }

        @Override
        protected void onPostExecute(List<AppInfo> appInfos) {
            adapter.setAppList(appInfos);
            progressBar.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
            
            // Show initial count
            updateAppCount(appInfos.size(), appInfos.size());
        }
    }

    @Override
    public void onFilterComplete(int filteredCount, int totalCount) {
        updateAppCount(filteredCount, totalCount);
    }

    private void updateAppCount(int filteredCount, int totalCount) {
        if (filteredCount == 0) {
            // No results found
            recyclerView.setVisibility(View.GONE);
            noResultsText.setVisibility(View.VISIBLE);
            appCountText.setVisibility(View.GONE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            noResultsText.setVisibility(View.GONE);
            appCountText.setVisibility(View.VISIBLE);
            
            if (filteredCount == totalCount) {
                appCountText.setText(String.format("%d apps available", totalCount));
            } else {
                appCountText.setText(String.format("Showing %d of %d apps", filteredCount, totalCount));
            }
        }
    }
}

