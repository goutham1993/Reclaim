package com.investment.pausetime.adapter;

import android.app.AlertDialog;
import android.content.Context;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.investment.pausetime.R;
import com.investment.pausetime.model.MonitoredApp;

import java.util.ArrayList;
import java.util.List;

public class MonitoredAppAdapter extends RecyclerView.Adapter<MonitoredAppAdapter.ViewHolder> {

    private List<MonitoredApp> appList;
    private OnAppActionListener listener;

    public interface OnAppActionListener {
        void onDelayChanged(MonitoredApp app, int newDelay);
        void onAppDeleted(MonitoredApp app);
    }

    public MonitoredAppAdapter(OnAppActionListener listener) {
        this.appList = new ArrayList<>();
        this.listener = listener;
    }

    public void setAppList(List<MonitoredApp> appList) {
        this.appList = appList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_monitored_app, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MonitoredApp app = appList.get(position);
        holder.bind(app);
    }

    @Override
    public int getItemCount() {
        return appList.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private TextView appName;
        private TextView delayText;
        private MaterialButton btnEditDelay;
        private ImageButton btnDelete;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            appName = itemView.findViewById(R.id.appName);
            delayText = itemView.findViewById(R.id.delayText);
            btnEditDelay = itemView.findViewById(R.id.btnEditDelay);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }

        public void bind(MonitoredApp app) {
            appName.setText(app.getAppName());
            delayText.setText(app.getDelaySeconds() + " seconds");

            btnEditDelay.setOnClickListener(v -> showEditDelayDialog(itemView.getContext(), app));
            btnDelete.setOnClickListener(v -> showDeleteConfirmation(itemView.getContext(), app));
        }

        private void showEditDelayDialog(Context context, MonitoredApp app) {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle("Edit Delay for " + app.getAppName());

            final EditText input = new EditText(context);
            input.setInputType(InputType.TYPE_CLASS_NUMBER);
            input.setText(String.valueOf(app.getDelaySeconds()));
            input.setSelection(input.getText().length());
            builder.setView(input);

            builder.setPositiveButton("Save", (dialog, which) -> {
                try {
                    int newDelay = Integer.parseInt(input.getText().toString());
                    if (newDelay > 0 && newDelay <= 300) { // Max 5 minutes
                        if (listener != null) {
                            listener.onDelayChanged(app, newDelay);
                        }
                    } else {
                        // Show error
                        new AlertDialog.Builder(context)
                                .setTitle("Invalid Input")
                                .setMessage("Please enter a delay between 1 and 300 seconds.")
                                .setPositiveButton("OK", null)
                                .show();
                    }
                } catch (NumberFormatException e) {
                    // Show error
                    new AlertDialog.Builder(context)
                            .setTitle("Invalid Input")
                            .setMessage("Please enter a valid number.")
                            .setPositiveButton("OK", null)
                            .show();
                }
            });

            builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
            builder.show();
        }

        private void showDeleteConfirmation(Context context, MonitoredApp app) {
            new AlertDialog.Builder(context)
                    .setTitle("Remove App")
                    .setMessage("Stop monitoring " + app.getAppName() + "?")
                    .setPositiveButton("Remove", (dialog, which) -> {
                        if (listener != null) {
                            listener.onAppDeleted(app);
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        }
    }
}

