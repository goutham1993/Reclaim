package com.investment.pausetime.service;

import android.animation.ValueAnimator;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import com.investment.pausetime.R;

public class OverlayService extends Service {

    private static final String TAG = "OverlayService";
    public static final String ACTION_DISMISS = "com.investment.pausetime.DISMISS_OVERLAY";
    
    private WindowManager windowManager;
    private View overlayView;
    private View waveView;
    private android.widget.ImageView breathingCircleImage;
    private CountDownTimer countDownTimer;
    private ValueAnimator waveAnimator;
    private ValueAnimator breathingAnimator;
    private boolean isShowing = false;
    private String currentPackageName;
    private int totalDelaySeconds;
    private int remainingSeconds;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();
            
            // Check if this is a dismiss action
            if (ACTION_DISMISS.equals(action)) {
                Log.d(TAG, "Received dismiss action");
                dismissOverlay();
                return START_NOT_STICKY;
            }
            
            // Otherwise, show overlay
            String packageName = intent.getStringExtra("packageName");
            String appName = intent.getStringExtra("appName");
            int delaySeconds = intent.getIntExtra("delaySeconds", 45);

            Log.d(TAG, "Overlay requested for: " + appName + " (" + delaySeconds + "s)");
            
            if (!isShowing) {
                currentPackageName = packageName;
                showOverlay(packageName, appName, delaySeconds);
            }
        }
        return START_NOT_STICKY;
    }
    
    public String getCurrentPackageName() {
        return currentPackageName;
    }

    private void showOverlay(String packageName, String appName, int delaySeconds) {
        if (isShowing) {
            Log.d(TAG, "Overlay already showing");
            return;
        }
        
        isShowing = true;
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        // Create overlay view with application theme context
        Context themedContext = getApplicationContext();
        LayoutInflater inflater = LayoutInflater.from(themedContext);
        overlayView = inflater.inflate(R.layout.activity_overlay, null);

        // Setup window parameters to block all interactions
        WindowManager.LayoutParams params;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            params = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL |
                            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN |
                            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
                            WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                    PixelFormat.TRANSLUCENT
            );
        } else {
            params = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.TYPE_PHONE,
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL |
                            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN |
                            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
                            WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                    PixelFormat.TRANSLUCENT
            );
        }
        params.gravity = Gravity.TOP | Gravity.START;

        // Add view to window
        try {
            windowManager.addView(overlayView, params);
            Log.d(TAG, "Overlay view added to window");
        } catch (Exception e) {
            Log.e(TAG, "Failed to add overlay view", e);
            isShowing = false;
            stopSelf();
            return;
        }

        // Make overlay intercept all touches and key events
        overlayView.setClickable(true);
        overlayView.setFocusable(true);
        overlayView.setFocusableInTouchMode(true);
        overlayView.setOnKeyListener((v, keyCode, event) -> {
            // Block all key events including back button
            return true;
        });

        // Setup UI
        ImageView appIcon = overlayView.findViewById(R.id.appIcon);
        TextView appNameText = overlayView.findViewById(R.id.appName);
        waveView = overlayView.findViewById(R.id.waveView);
        breathingCircleImage = overlayView.findViewById(R.id.breathingCircleImage);

        // Load app icon
        try {
            Drawable icon = getPackageManager().getApplicationIcon(packageName);
            appIcon.setImageDrawable(icon);
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Failed to load app icon", e);
        }

        appNameText.setText(appName);
        
        totalDelaySeconds = delaySeconds;
        remainingSeconds = delaySeconds;
        
        Log.d(TAG, "Starting wave animation for " + delaySeconds + " seconds");

        // Start countdown timer (backup dismissal, wave animation will handle primary dismissal)
        countDownTimer = new CountDownTimer(delaySeconds * 1000L, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                remainingSeconds = (int) (millisUntilFinished / 1000);
                // Timer runs in background, wave animation handles dismissal
            }

            @Override
            public void onFinish() {
                // Backup dismissal (in case animation completes slightly before timer)
                dismissOverlay();
            }
        }.start();

        // Start animations
        overlayView.post(() -> {
            startWaveAnimation();
            startBreathingAnimation();
            Log.d(TAG, "Animations started");
        });
    }
    
    private void startBreathingAnimation() {
        if (breathingCircleImage == null) {
            return;
        }
        
        // Cancel any existing animation
        if (breathingAnimator != null) {
            breathingAnimator.cancel();
            breathingAnimator = null;
        }
        
        // Reset circle to normal size
        breathingCircleImage.setScaleX(1.0f);
        breathingCircleImage.setScaleY(1.0f);
        breathingCircleImage.setAlpha(0.6f);
        
        // Create gentle breathing animation: gently expand and contract
        // Slower, more calming rhythm (4 seconds per breath cycle)
        breathingAnimator = ValueAnimator.ofFloat(1.0f, 1.12f, 1.0f);
        breathingAnimator.setDuration(4000); // 4 seconds per breath (calming pace)
        breathingAnimator.setRepeatCount(ValueAnimator.INFINITE);
        breathingAnimator.setInterpolator(new android.view.animation.AccelerateDecelerateInterpolator());
        
        breathingAnimator.addUpdateListener(animation -> {
            try {
                if (breathingCircleImage != null) {
                    float scale = (float) animation.getAnimatedValue();
                    breathingCircleImage.setScaleX(scale);
                    breathingCircleImage.setScaleY(scale);
                    
                    // Subtle alpha change for extra calming effect
                    float alpha = 0.55f + (scale - 1.0f) * 0.15f; // Range: 0.55 to 0.67
                    breathingCircleImage.setAlpha(alpha);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error updating breathing animation", e);
            }
        });
        
        breathingAnimator.start();
        Log.d(TAG, "Breathing circle animation started");
    }
    
    private void startWaveAnimation() {
        if (waveView == null) {
            return;
        }
        
        // Cancel any existing animation
        if (waveAnimator != null) {
            waveAnimator.cancel();
            waveAnimator = null;
        }
        
        // Get screen height for animation
        int screenHeight = overlayView.getHeight();
        if (screenHeight == 0) {
            screenHeight = 1000; // Fallback if height not available yet
        }
        
        // Reset wave to bottom (0 height)
        ViewGroup.LayoutParams params = waveView.getLayoutParams();
        params.height = 0;
        waveView.setLayoutParams(params);
        
        // Create single wave animation: up (0 → full height) then down (full height → 0)
        // Duration: exactly matches remaining time (remainingSeconds * 1000ms)
        long duration = remainingSeconds * 1000L;
        
        waveAnimator = ValueAnimator.ofInt(0, screenHeight, 0);
        waveAnimator.setDuration(duration);
        waveAnimator.setRepeatCount(0); // Single cycle only - no looping
        waveAnimator.setInterpolator(new android.view.animation.AccelerateDecelerateInterpolator());
        
        waveAnimator.addUpdateListener(animation -> {
            try {
                if (waveView != null) {
                    int height = (int) animation.getAnimatedValue();
                    ViewGroup.LayoutParams layoutParams = waveView.getLayoutParams();
                    layoutParams.height = height;
                    waveView.setLayoutParams(layoutParams);
                    waveView.requestLayout();
                }
            } catch (Exception e) {
                Log.e(TAG, "Error updating wave animation", e);
            }
        });
        
        // When animation completes, dismiss overlay
        waveAnimator.addListener(new android.animation.Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(android.animation.Animator animation) {
                // Animation started
            }

            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                // Wave completed its cycle - dismiss overlay
                Log.d(TAG, "Wave animation completed, dismissing overlay");
                dismissOverlay();
            }

            @Override
            public void onAnimationCancel(android.animation.Animator animation) {
                // Animation cancelled
            }

            @Override
            public void onAnimationRepeat(android.animation.Animator animation) {
                // Not used (no repeat)
            }
        });
        
        waveAnimator.start();
        Log.d(TAG, "Wave animation started: one cycle in " + duration + "ms (remaining: " + remainingSeconds + "s)");
    }
    
    private void updateWaveSpeed() {
        // Wave animation now runs for exactly the remaining time
        // No need to update speed dynamically - it's already perfectly synced
        // The countdown timer will handle dismissal when time runs out
    }

    private void dismissOverlay() {
        Log.d(TAG, "Dismissing overlay (isShowing=" + isShowing + ")");
        
        if (!isShowing) {
            Log.d(TAG, "Overlay not showing, nothing to dismiss");
            return;
        }
        
        if (countDownTimer != null) {
            countDownTimer.cancel();
            countDownTimer = null;
            Log.d(TAG, "Countdown timer cancelled");
        }
        
        if (waveAnimator != null) {
            if (waveAnimator.isRunning()) {
                waveAnimator.cancel();
            }
            waveAnimator = null;
            Log.d(TAG, "Wave animator cancelled");
        }
        
        if (breathingAnimator != null) {
            if (breathingAnimator.isRunning()) {
                breathingAnimator.cancel();
            }
            breathingAnimator = null;
            Log.d(TAG, "Breathing animator cancelled");
        }
        
        if (overlayView != null && windowManager != null) {
            try {
                windowManager.removeView(overlayView);
                Log.d(TAG, "Overlay view removed from window");
            } catch (Exception e) {
                Log.e(TAG, "Error removing overlay view", e);
            }
            overlayView = null;
        }
        
        isShowing = false;
        currentPackageName = null;
        
        Log.d(TAG, "Overlay dismissed, stopping service");
        stopSelf();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        dismissOverlay();
        Log.d(TAG, "Service destroyed");
    }
}

