package com.investment.pausetime.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.investment.pausetime.R;
import com.investment.pausetime.model.AppInfo;

import java.util.ArrayList;
import java.util.List;

public class AppListAdapter extends RecyclerView.Adapter<AppListAdapter.ViewHolder> {

    private List<AppInfo> appList;
    private List<AppInfo> appListFull; // Backup of full list for search
    private OnFilterListener filterListener;

    public interface OnFilterListener {
        void onFilterComplete(int filteredCount, int totalCount);
    }

    public AppListAdapter() {
        this.appList = new ArrayList<>();
        this.appListFull = new ArrayList<>();
    }

    public void setOnFilterListener(OnFilterListener listener) {
        this.filterListener = listener;
    }

    public void setAppList(List<AppInfo> appList) {
        this.appList = appList;
        this.appListFull = new ArrayList<>(appList); // Keep a copy for filtering
        notifyDataSetChanged();
    }

    public List<AppInfo> getSelectedApps() {
        List<AppInfo> selectedApps = new ArrayList<>();
        // Check in the full list to include selected apps that might be filtered out
        for (AppInfo app : appListFull) {
            if (app.isSelected()) {
                selectedApps.add(app);
            }
        }
        return selectedApps;
    }

    public void filter(String query) {
        appList.clear();
        
        if (query == null || query.isEmpty()) {
            // Show all apps if query is empty
            appList.addAll(appListFull);
        } else {
            // Filter apps by name
            String lowerCaseQuery = query.toLowerCase();
            for (AppInfo app : appListFull) {
                if (app.getAppName().toLowerCase().contains(lowerCaseQuery)) {
                    appList.add(app);
                }
            }
        }
        
        notifyDataSetChanged();
        
        // Notify listener about filter results
        if (filterListener != null) {
            filterListener.onFilterComplete(appList.size(), appListFull.size());
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_app, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AppInfo app = appList.get(position);
        holder.bind(app);
    }

    @Override
    public int getItemCount() {
        return appList.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private ImageView appIcon;
        private TextView appName;
        private CheckBox checkbox;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            appIcon = itemView.findViewById(R.id.appIcon);
            appName = itemView.findViewById(R.id.appName);
            checkbox = itemView.findViewById(R.id.checkbox);
        }

        public void bind(AppInfo app) {
            appIcon.setImageDrawable(app.getIcon());
            appName.setText(app.getAppName());
            checkbox.setChecked(app.isSelected());

            itemView.setOnClickListener(v -> {
                app.setSelected(!app.isSelected());
                checkbox.setChecked(app.isSelected());
            });

            checkbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                app.setSelected(isChecked);
            });
        }
    }
}

