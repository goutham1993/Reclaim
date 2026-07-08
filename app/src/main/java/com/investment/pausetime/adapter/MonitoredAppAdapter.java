package com.investment.pausetime.adapter;

import android.app.AlertDialog;
import android.content.Context;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.investment.pausetime.R;
import com.investment.pausetime.model.MonitoredApp;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class MonitoredAppAdapter extends RecyclerView.Adapter<MonitoredAppAdapter.ViewHolder> {

    private List<MonitoredApp> appList;
    private OnAppActionListener listener;

    public interface OnAppActionListener {
        void onDelayChanged(MonitoredApp app, int newDelay);
        void onAppDeleted(MonitoredApp app);
        void onTemporaryReductionModeToggled(MonitoredApp app, boolean enabled);
        void onTemporaryReductionApplied(MonitoredApp app, int reducedSeconds, long durationMillis);
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
        private SwitchMaterial switchTempReduction;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            appName = itemView.findViewById(R.id.appName);
            delayText = itemView.findViewById(R.id.delayText);
            btnEditDelay = itemView.findViewById(R.id.btnEditDelay);
            btnDelete = itemView.findViewById(R.id.btnDelete);
            switchTempReduction = itemView.findViewById(R.id.switchTempReduction);
        }

        public void bind(MonitoredApp app) {
            appName.setText(app.getAppName());
            delayText.setText(buildDelayText(app));

            // Set the checked state before attaching the listener so we don't
            // fire a spurious toggle callback while binding.
            switchTempReduction.setOnCheckedChangeListener(null);
            switchTempReduction.setChecked(app.isTempReductionModeEnabled());
            switchTempReduction.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (listener != null) {
                    listener.onTemporaryReductionModeToggled(app, isChecked);
                }
            });

            btnEditDelay.setOnClickListener(v -> {
                if (app.isTempReductionModeEnabled()) {
                    showTemporaryReductionDialog(itemView.getContext(), app);
                } else {
                    showEditDelayDialog(itemView.getContext(), app);
                }
            });
            btnDelete.setOnClickListener(v -> showDeleteConfirmation(itemView.getContext(), app));
        }

        private String buildDelayText(MonitoredApp app) {
            if (app.isTemporaryReductionActive()) {
                long remainingMinutes = TimeUnit.MILLISECONDS.toMinutes(app.getTemporaryRemainingMillis());
                if (remainingMinutes < 1) {
                    remainingMinutes = 1; // Show "1m left" rather than "0m left".
                }
                return app.getTempReducedDelaySeconds() + " seconds (temporary, "
                        + remainingMinutes + "m left -> reverts to " + app.getDelaySeconds() + "s)";
            }
            return app.getDelaySeconds() + " seconds";
        }

        private void showTemporaryReductionDialog(Context context, MonitoredApp app) {
            LinearLayout container = new LinearLayout(context);
            container.setOrientation(LinearLayout.VERTICAL);
            int pad = (int) (context.getResources().getDisplayMetrics().density * 20);
            container.setPadding(pad, pad, pad, 0);

            TextView reducedLabel = new TextView(context);
            reducedLabel.setText("Reduced delay (seconds)");
            container.addView(reducedLabel);

            final EditText reducedInput = new EditText(context);
            reducedInput.setInputType(InputType.TYPE_CLASS_NUMBER);
            reducedInput.setText(String.valueOf(
                    app.getTempReducedDelaySeconds() > 0 ? app.getTempReducedDelaySeconds() : 1));
            reducedInput.setSelection(reducedInput.getText().length());
            container.addView(reducedInput);

            final long[] durationMillis = {TimeUnit.MINUTES.toMillis(15)};

            TextView durationLabel = new TextView(context);
            durationLabel.setText("For how long?");
            LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            labelParams.topMargin = pad;
            durationLabel.setLayoutParams(labelParams);
            container.addView(durationLabel);

            final TextView selectedDuration = new TextView(context);
            container.addView(selectedDuration);

            LinearLayout presetRow = new LinearLayout(context);
            presetRow.setOrientation(LinearLayout.HORIZONTAL);
            container.addView(presetRow);

            final MaterialButton customButton = new MaterialButton(
                    context, null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
            customButton.setText("Custom");

            String[] presetLabels = {"15 min", "30 min", "1 hour"};
            final long[] presetValues = {
                    TimeUnit.MINUTES.toMillis(15),
                    TimeUnit.MINUTES.toMillis(30),
                    TimeUnit.HOURS.toMillis(1)
            };
            for (int i = 0; i < presetLabels.length; i++) {
                final long value = presetValues[i];
                MaterialButton presetButton = new MaterialButton(
                        context, null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
                presetButton.setText(presetLabels[i]);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
                presetButton.setLayoutParams(params);
                presetButton.setOnClickListener(v -> {
                    durationMillis[0] = value;
                    selectedDuration.setText("Selected: " + formatDuration(value));
                });
                presetRow.addView(presetButton);
            }

            customButton.setOnClickListener(v -> {
                final EditText minutesInput = new EditText(context);
                minutesInput.setInputType(InputType.TYPE_CLASS_NUMBER);
                minutesInput.setHint("Minutes");
                new AlertDialog.Builder(context)
                        .setTitle("Custom duration")
                        .setView(minutesInput)
                        .setPositiveButton("Set", (d, w) -> {
                            try {
                                int minutes = Integer.parseInt(minutesInput.getText().toString());
                                if (minutes > 0) {
                                    durationMillis[0] = TimeUnit.MINUTES.toMillis(minutes);
                                    selectedDuration.setText("Selected: " + formatDuration(durationMillis[0]));
                                }
                            } catch (NumberFormatException ignored) {
                            }
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            });
            LinearLayout.LayoutParams customParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            container.addView(customButton, customParams);

            selectedDuration.setText("Selected: " + formatDuration(durationMillis[0]));

            new AlertDialog.Builder(context)
                    .setTitle("Temporarily reduce " + app.getAppName())
                    .setView(container)
                    .setPositiveButton("Apply", (dialog, which) -> {
                        try {
                            int reduced = Integer.parseInt(reducedInput.getText().toString());
                            if (reduced > 0 && reduced <= 300) {
                                if (listener != null) {
                                    listener.onTemporaryReductionApplied(app, reduced, durationMillis[0]);
                                }
                            } else {
                                showInvalidInput(context, "Please enter a delay between 1 and 300 seconds.");
                            }
                        } catch (NumberFormatException e) {
                            showInvalidInput(context, "Please enter a valid number.");
                        }
                    })
                    .setNegativeButton("Cancel", (dialog, which) -> dialog.cancel())
                    .show();
        }

        private String formatDuration(long millis) {
            long hours = TimeUnit.MILLISECONDS.toHours(millis);
            long minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60;
            if (hours > 0 && minutes > 0) {
                return hours + "h " + minutes + "m";
            } else if (hours > 0) {
                return hours + "h";
            }
            return TimeUnit.MILLISECONDS.toMinutes(millis) + "m";
        }

        private void showInvalidInput(Context context, String message) {
            new AlertDialog.Builder(context)
                    .setTitle("Invalid Input")
                    .setMessage(message)
                    .setPositiveButton("OK", null)
                    .show();
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

