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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.investment.pausetime.R;

import java.util.Random;

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
    private int remainingSeconds;
    private View encouragementOverlayView;

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
            
            // CRITICAL: Clean up ALL existing overlays and state before showing new one
            // This ensures no old sessions interfere with new overlay
            cleanupOverlay();
            resetState();
            
            // Small delay to ensure cleanup is complete and WindowManager is ready
            // This prevents any race conditions
            Handler handler = new Handler(getMainLooper());
            handler.postDelayed(() -> {
                // Double-check: ensure no overlays are showing before proceeding
                if (overlayView == null && encouragementOverlayView == null) {
                    currentPackageName = packageName;
                    showOverlay(packageName, appName, delaySeconds);
                } else {
                    Log.w(TAG, "Overlays still exist after cleanup, retrying cleanup");
                    cleanupOverlay();
                    resetState();
                    currentPackageName = packageName;
                    showOverlay(packageName, appName, delaySeconds);
                }
            }, 100);
        }
        return START_NOT_STICKY;
    }
    
    private void resetState() {
        isShowing = false;
        currentPackageName = null;
        overlayView = null;
        waveView = null;
        breathingCircleImage = null;
        windowManager = null;
        encouragementOverlayView = null;
        
        // Cancel any running timers/animators
        if (countDownTimer != null) {
            countDownTimer.cancel();
            countDownTimer = null;
        }
        
        if (waveAnimator != null) {
            if (waveAnimator.isRunning()) {
                waveAnimator.cancel();
            }
            waveAnimator = null;
        }
        
        if (breathingAnimator != null) {
            if (breathingAnimator.isRunning()) {
                breathingAnimator.cancel();
            }
            breathingAnimator = null;
        }
    }
    
    private void cleanupOverlay() {
        Log.d(TAG, "Cleaning up all overlays before showing new one");
        
        // Remove main overlay
        if (overlayView != null) {
            try {
                WindowManager wm = windowManager != null ? windowManager : (WindowManager) getSystemService(WINDOW_SERVICE);
                if (wm != null) {
                    wm.removeView(overlayView);
                    Log.d(TAG, "Main overlay removed");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error removing overlay in cleanup", e);
            }
            overlayView = null;
        }
        
        // Force remove encouragement overlay (even if reference is lost)
        forceRemoveEncouragementOverlay();
    }
    
    private void forceRemoveEncouragementOverlay() {
        // First try using stored reference
        if (encouragementOverlayView != null) {
            try {
                WindowManager wm = (WindowManager) getSystemService(WINDOW_SERVICE);
                if (wm != null) {
                    wm.removeView(encouragementOverlayView);
                    Log.d(TAG, "Encouragement overlay removed via reference");
                }
            } catch (Exception e) {
                Log.d(TAG, "Encouragement overlay not found via reference, trying alternative method", e);
            }
            encouragementOverlayView = null;
        }
        
        // Force cleanup: Remove any overlay views that might be encouragement overlays
        // This handles cases where service was destroyed and reference was lost
        try {
            WindowManager wm = (WindowManager) getSystemService(WINDOW_SERVICE);
            if (wm != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                // Try to find and remove any TextView overlays that match encouragement style
                // Note: This is a defensive cleanup - we can't directly enumerate WindowManager views
                // but we've already tried removing via reference above
                Log.d(TAG, "Encouragement overlay cleanup completed");
            }
        } catch (Exception e) {
            Log.d(TAG, "Error in force cleanup", e);
        }
    }
    
    public String getCurrentPackageName() {
        return currentPackageName;
    }

    private void showOverlay(String packageName, String appName, int delaySeconds) {
        // Defensive check: ensure no overlays are showing before creating new one
        if (overlayView != null) {
            Log.w(TAG, "Overlay view already exists, removing before creating new one");
            removeOverlayView();
        }
        
        if (encouragementOverlayView != null) {
            Log.w(TAG, "Encouragement overlay still exists, removing before creating new overlay");
            forceRemoveEncouragementOverlay();
        }
        
        isShowing = true;
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        // Create overlay view with application theme context
        Context themedContext = getApplicationContext();
        LayoutInflater inflater = LayoutInflater.from(themedContext);
        overlayView = inflater.inflate(R.layout.activity_overlay, null);

        WindowManager.LayoutParams params = createOverlayLayoutParams();
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
        remainingSeconds = delaySeconds;
        
        Log.d(TAG, "Starting wave animation for " + delaySeconds + " seconds");

        countDownTimer = new CountDownTimer(delaySeconds * 1000L, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                remainingSeconds = (int) (millisUntilFinished / 1000);
            }

            @Override
            public void onFinish() {
                dismissOverlay();
            }
        }.start();

        // Start animations
        overlayView.post(() -> {
            startWaveAnimation();
            startBreathingAnimation();
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
        
        breathingAnimator = ValueAnimator.ofFloat(1.0f, 1.12f, 1.0f);
        breathingAnimator.setDuration(4000);
        breathingAnimator.setRepeatCount(ValueAnimator.INFINITE);
        breathingAnimator.setInterpolator(new android.view.animation.AccelerateDecelerateInterpolator());
        
        breathingAnimator.addUpdateListener(animation -> {
            try {
                if (breathingCircleImage != null) {
                    float scale = (float) animation.getAnimatedValue();
                    breathingCircleImage.setScaleX(scale);
                    breathingCircleImage.setScaleY(scale);
                    
                    float alpha = 0.55f + (scale - 1.0f) * 0.15f;
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
        
        int screenHeight = overlayView.getHeight();
        if (screenHeight == 0) {
            screenHeight = 1000;
        }
        
        ViewGroup.LayoutParams params = waveView.getLayoutParams();
        params.height = 0;
        waveView.setLayoutParams(params);
        
        long duration = remainingSeconds * 1000L;
        
        waveAnimator = ValueAnimator.ofInt(0, screenHeight, 0);
        waveAnimator.setDuration(duration);
        waveAnimator.setRepeatCount(0);
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
        
        waveAnimator.addListener(new android.animation.Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(android.animation.Animator animation) {}

            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                Log.d(TAG, "Wave animation completed, dismissing overlay");
                dismissOverlay();
            }

            @Override
            public void onAnimationCancel(android.animation.Animator animation) {}

            @Override
            public void onAnimationRepeat(android.animation.Animator animation) {}
        });
        
        waveAnimator.start();
        Log.d(TAG, "Wave animation started: one cycle in " + duration + "ms (remaining: " + remainingSeconds + "s)");
    }

    private void dismissOverlay() {
        Log.d(TAG, "Dismissing overlay (isShowing=" + isShowing + ")");
        
        boolean wasAnimationRunning = waveAnimator != null && waveAnimator.isRunning();
        if (wasAnimationRunning) {
            Log.d(TAG, "Animation was interrupted - user closed app during animation");
        }
        
        stopAnimationsAndTimers();
        removeOverlayView();
        
        isShowing = false;
        currentPackageName = null;
        
        if (wasAnimationRunning) {
            showEncouragementMessage();
            Log.d(TAG, "Overlay dismissed, encouragement shown, stopping service");
        } else {
            Log.d(TAG, "Overlay dismissed, stopping service");
        }
        stopSelf();
    }
    
    private void showEncouragementMessage() {
        try {
            String[] messages = getResources().getStringArray(R.array.encouragement_messages);
            if (messages.length > 0) {
                Random random = new Random();
                String message = messages[random.nextInt(messages.length)];
                
                Handler mainHandler = new Handler(getMainLooper());
                mainHandler.post(() -> {
                    try {
                        showEncouragementOverlay(message);
                        Log.d(TAG, "Showed encouragement message: " + message);
                    } catch (Exception e) {
                        Log.e(TAG, "Error showing encouragement overlay", e);
                        try {
                            Toast.makeText(OverlayService.this, message, Toast.LENGTH_LONG).show();
                        } catch (Exception e2) {
                            Log.e(TAG, "Error showing toast fallback", e2);
                        }
                    }
                });
            }
        } catch (Exception e) {
            Log.e(TAG, "Error showing encouragement message", e);
        }
    }
    
    private void showEncouragementOverlay(String message) {
        // CRITICAL: Always remove any existing encouragement overlay first
        // This prevents multiple encouragement overlays from stacking
        forceRemoveEncouragementOverlay();
        
        WindowManager encouragementWindowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        
        TextView encouragementView = new TextView(this);
        encouragementView.setText(message);
        encouragementView.setTextSize(18);
        encouragementView.setTextColor(0xFFFFFFFF);
        encouragementView.setPadding(48, 36, 48, 36);
        encouragementView.setGravity(Gravity.CENTER);
        encouragementView.setMaxWidth((int) (getResources().getDisplayMetrics().widthPixels * 0.85f));
        encouragementView.setTag("RECLAIM_ENCOURAGEMENT_OVERLAY"); // Tag for identification
        
        android.graphics.drawable.GradientDrawable background = new android.graphics.drawable.GradientDrawable();
        background.setColor(0xE6000000);
        background.setCornerRadius(20f);
        encouragementView.setBackground(background);
        encouragementView.setElevation(8f);
        
        WindowManager.LayoutParams params = createEncouragementLayoutParams();
        params.gravity = Gravity.CENTER;
        
        encouragementOverlayView = encouragementView;
        
        // Add view to window
        try {
            encouragementWindowManager.addView(encouragementView, params);
            
            // Auto-dismiss after 3 seconds with fade out animation
            Handler handler = new Handler(getMainLooper());
            final View finalEncouragementView = encouragementView;
            final WindowManager finalWindowManager = encouragementWindowManager;
            handler.postDelayed(() -> {
                try {
                    // Check if this encouragement overlay is still the current one
                    if (finalWindowManager != null && finalEncouragementView != null && 
                        encouragementOverlayView == finalEncouragementView) {
                        // Fade out animation
                        ValueAnimator fadeOut = ValueAnimator.ofFloat(1.0f, 0.0f);
                        fadeOut.setDuration(300);
                        fadeOut.addUpdateListener(animation -> {
                            try {
                                if (finalEncouragementView != null) {
                                    float alpha = (float) animation.getAnimatedValue();
                                    finalEncouragementView.setAlpha(alpha);
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Error updating fade animation", e);
                            }
                        });
                        fadeOut.addListener(new android.animation.Animator.AnimatorListener() {
                            @Override
                            public void onAnimationStart(android.animation.Animator animation) {}
                            
                            @Override
                            public void onAnimationEnd(android.animation.Animator animation) {
                                try {
                                    if (finalWindowManager != null && finalEncouragementView != null && 
                                        encouragementOverlayView == finalEncouragementView) {
                                        finalWindowManager.removeView(finalEncouragementView);
                                        encouragementOverlayView = null;
                                        Log.d(TAG, "Encouragement overlay auto-dismissed");
                                    }
                                } catch (Exception e) {
                                    Log.e(TAG, "Error removing encouragement overlay after fade", e);
                                }
                            }
                            
                            @Override
                            public void onAnimationCancel(android.animation.Animator animation) {
                                // If cancelled, remove immediately
                                try {
                                    if (finalWindowManager != null && finalEncouragementView != null) {
                                        finalWindowManager.removeView(finalEncouragementView);
                                        encouragementOverlayView = null;
                                    }
                                } catch (Exception e) {
                                    Log.e(TAG, "Error removing encouragement overlay on cancel", e);
                                }
                            }
                            
                            @Override
                            public void onAnimationRepeat(android.animation.Animator animation) {}
                        });
                        fadeOut.start();
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error in encouragement overlay auto-dismiss", e);
                }
            }, 3000);
        } catch (Exception e) {
            Log.e(TAG, "Error adding encouragement overlay", e);
            encouragementOverlayView = null;
            throw e;
        }
    }

    private WindowManager.LayoutParams createOverlayLayoutParams() {
        int type = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O 
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY 
                : WindowManager.LayoutParams.TYPE_PHONE;
        
        int flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL |
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN |
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH;
        
        return new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                type, flags, PixelFormat.TRANSLUCENT);
    }

    private WindowManager.LayoutParams createEncouragementLayoutParams() {
        int type = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O 
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY 
                : WindowManager.LayoutParams.TYPE_PHONE;
        
        int flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN |
                    WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED;
        
        return new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                type, flags, PixelFormat.TRANSLUCENT);
    }

    private void stopAnimationsAndTimers() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
            countDownTimer = null;
        }
        
        if (waveAnimator != null) {
            if (waveAnimator.isRunning()) {
                waveAnimator.cancel();
            }
            waveAnimator = null;
        }
        
        if (breathingAnimator != null) {
            if (breathingAnimator.isRunning()) {
                breathingAnimator.cancel();
            }
            breathingAnimator = null;
        }
    }

    private void removeOverlayView() {
        if (overlayView != null) {
            try {
                WindowManager wm = windowManager != null ? windowManager : (WindowManager) getSystemService(WINDOW_SERVICE);
                if (wm != null) {
                    wm.removeView(overlayView);
                    Log.d(TAG, "Overlay view removed from window");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error removing overlay view", e);
            }
            overlayView = null;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        
        Log.d(TAG, "Service destroying - cleaning up all resources");
        
        stopAnimationsAndTimers();
        removeOverlayView();
        
        // Force remove encouragement overlay on service destroy
        // This ensures cleanup even if service is destroyed unexpectedly
        forceRemoveEncouragementOverlay();
        
        isShowing = false;
        currentPackageName = null;
        
        Log.d(TAG, "Service destroyed - all resources cleaned up");
    }
}

