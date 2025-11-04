package com.investment.pausetime;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.investment.pausetime.adapter.MonitoredAppAdapter;
import com.investment.pausetime.model.MonitoredApp;
import com.investment.pausetime.repository.AppRepository;

import java.util.List;

public class SettingsActivity extends AppCompatActivity implements MonitoredAppAdapter.OnAppActionListener {

    private RecyclerView recyclerView;
    private TextView emptyText;
    private MonitoredAppAdapter adapter;
    private AppRepository repository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        repository = new AppRepository(this);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        recyclerView = findViewById(R.id.recyclerView);
        emptyText = findViewById(R.id.emptyText);

        adapter = new MonitoredAppAdapter(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        loadMonitoredApps();
    }

    private void loadMonitoredApps() {
        List<MonitoredApp> apps = repository.getMonitoredApps();
        
        if (apps.isEmpty()) {
            emptyText.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            emptyText.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
            adapter.setAppList(apps);
        }
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

    @Override
    protected void onResume() {
        super.onResume();
        loadMonitoredApps();
    }
}

