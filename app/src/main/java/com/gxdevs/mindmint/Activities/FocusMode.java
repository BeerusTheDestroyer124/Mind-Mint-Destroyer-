package com.gxdevs.mindmint.Activities;

import static android.view.View.GONE;
import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;
import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

import android.Manifest;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.util.Log;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;

import com.airbnb.lottie.LottieAnimationView;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textview.MaterialTextView;
import com.gxdevs.mindmint.Views.NebulaStarfieldView;
import com.gxdevs.mindmint.R;
import com.gxdevs.mindmint.Services.FocusService;
import com.gxdevs.mindmint.Utils.MintCrystals;
import com.gxdevs.mindmint.Utils.Utils;
import com.gxdevs.mindmint.Views.RevealMaskImageView;
import com.gxdevs.mindmint.db.MindMintRoomDatabase;
import com.gxdevs.mindmint.db.entities.FocusTopicEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;

import me.tankery.lib.circularseekbar.CircularSeekBar;

public class FocusMode extends AppCompatActivity {

    private static final String TAG = "FocusMode";
    private static final float REVEAL_COMPLETE_AT = 0.95f;
    private ActivityResultLauncher<String> requestNotificationPermissionLauncher;
    private TextView timerText, instructionText;
    private Button startButton, focusStop;
    private View btnDivider;
    private FocusService focusService;
    private boolean isBound = false;
    private final Handler handler = new Handler();
    private int selectedMinutes = 10;
    private CircularSeekBar circularSeekBar;
    private ImageView crystalBase;
    private RevealMaskImageView crystalColor;
    private LottieAnimationView lottieAnimation;
    private boolean warnedInvalidDuration = false;
    private boolean revealCompleteNotified = false;
    private ValueAnimator revealAnimator;
    private MaterialTextView mintCrystalsTxt;
    private MintCrystals mintCrystals;
    private ChipGroup topicsChipGroup;
    private MindMintRoomDatabase db;
    private List<FocusTopicEntity> currentTopics = new ArrayList<>();
    private int selectedTopicId = -1;
    private ImageView settingsBtn;
    private TextView pomodoroIndicator;
    private View topicsContainer;
    private boolean isTestMode = false;

    // Pomodoro Preferences
    private static final String PREF_POMODORO_ENABLED = "pref_pomodoro_enabled";
    private static final String PREF_FOCUS_DURATION = "pref_focus_duration";
    private static final String PREF_BREAK_DURATION = "pref_break_duration";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Apply local night mode for this Activity only if user saved a nebula override
        SharedPreferences prefsInit = PreferenceManager.getDefaultSharedPreferences(this);
        if (prefsInit.contains("nebula_theme_override")) {
            boolean dark = prefsInit.getBoolean("nebula_theme_override", false);
            getDelegate().setLocalNightMode(dark ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
        }

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_focus_mode);
        Utils.setPad(findViewById(R.id.main), "bottom", this);

        db = MindMintRoomDatabase.getInstance(this);

        setupPermissionLauncher();
        initViews();
        setupOnUI();
        setupReveal();
        setupCircularSeekBar();
        loadTopics();

        // Setup blur on permission card and update its visibility
        checkPermissionAndMoveOn();
    }

    private void setupOnUI() {
        findViewById(R.id.coinImg).setOnClickListener(v -> showPeaceCoinsDialog());
        findViewById(R.id.mintCrystals).setOnClickListener(v -> showPeaceCoinsDialog());
        settingsBtn.setOnClickListener(v -> showSettingsBottomSheet());

        focusStop.setOnClickListener(v -> {
            if (isBound && focusService != null) {
                focusService.stopService();
            }
        });

        startButton.setOnLongClickListener(v -> {
            isTestMode = !isTestMode;
            String msg = isTestMode ? "Test Mode: Enabled (1 min focus / 30 sec break)" : "Test Mode: Disabled";
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
            v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
            updatePomodoroIndicator();
            return true;
        });

        startButton.setOnClickListener(v -> {
            // Require exact alarm permission on Android 12+ before any timer action
            if (!hasExactAlarmPermission()) {
                checkPermissionAndMoveOn();
                askForExactAlarmPermission();
                Toast.makeText(this, "Allow exact alarm permission to use Focus Mode timer.", Toast.LENGTH_SHORT)
                        .show();
                return;
            }
            if (checkNotification()) {
                if (isBound && focusService != null) {
                    if (focusService.isTimerRunning()) {
                        if (focusService.isPaused() && !focusService.isBreak()) {
                            // WAITING_USER state - Resume the timer
                            focusService.resumeTimer();
                            updateButtonStates(true);
                            handler.post(updateUITask);
                        } else {
                            // Running or in break - Stop the timer
                            focusService.stopService();
                        }
                    } else {
                        if (selectedMinutes < 10) {
                            Toast.makeText(this, "Minimum timer is 10 minutes", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        hideCrystalForRunningInit();
                        Intent serviceIntent = new Intent(this, FocusService.class);
                        serviceIntent.setAction(FocusService.ACTION_START_FOREGROUND_SERVICE);
                        long duration = isTestMode ? 60000L : getDuration();
                        serviceIntent.putExtra("durationInMillis", duration);

                        // Pass configurations
                        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
                        boolean isPomodoro = prefs.getBoolean(PREF_POMODORO_ENABLED, false);
                        long focusInterval = isTestMode ? 30000L : prefs.getInt(PREF_FOCUS_DURATION, 25) * 60 * 1000L;
                        long breakInterval = isTestMode ? 15000L : prefs.getInt(PREF_BREAK_DURATION, 5) * 60 * 1000L;
                        String topicName = null;
                        if (selectedTopicId != -1) {
                            for (FocusTopicEntity t : currentTopics) {
                                if (t.id == selectedTopicId) {
                                    topicName = t.name;
                                    break;
                                }
                            }
                        }

                        serviceIntent.putExtra("isPomodoroEnabled", isPomodoro || isTestMode);
                        serviceIntent.putExtra("pomodoroFocusInterval", focusInterval);
                        serviceIntent.putExtra("pomodoroBreakInterval", breakInterval);
                        serviceIntent.putExtra("topicName", topicName);

                        ContextCompat.startForegroundService(this, serviceIntent);
                        circularSeekBar.setProgress(0f);
                        if (!isBound) {
                            bindService();
                        } else {
                            updateButtonStates(true);
                            handler.post(updateUITask);
                        }
                    }
                } else {
                    hideCrystalForRunningInit();
                    Intent serviceIntent = new Intent(this, FocusService.class);
                    serviceIntent.setAction(FocusService.ACTION_START_FOREGROUND_SERVICE);
                    long duration = isTestMode ? 60000L : getDuration();
                    serviceIntent.putExtra("durationInMillis", duration);

                    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
                    boolean isPomodoro = prefs.getBoolean(PREF_POMODORO_ENABLED, false);
                    long focusInterval = isTestMode ? 30000L : prefs.getInt(PREF_FOCUS_DURATION, 25) * 60 * 1000L;
                    long breakInterval = isTestMode ? 15000L : prefs.getInt(PREF_BREAK_DURATION, 5) * 60 * 1000L;
                    String topicName = null;
                    if (selectedTopicId != -1) {
                        for (FocusTopicEntity t : currentTopics) {
                            if (t.id == selectedTopicId) {
                                topicName = t.name;
                                break;
                            }
                        }
                    }

                    serviceIntent.putExtra("isPomodoroEnabled", isPomodoro || isTestMode);
                    serviceIntent.putExtra("pomodoroFocusInterval", focusInterval);
                    serviceIntent.putExtra("pomodoroBreakInterval", breakInterval);
                    serviceIntent.putExtra("topicName", topicName);

                    ContextCompat.startForegroundService(this, serviceIntent);
                    bindService();
                }
            }
        });

        NebulaStarfieldView galaxy = findViewById(R.id.starFieldView);
        galaxy.setContext(this);
        galaxy.setStarCount(200);
        galaxy.setDriftRange(2f);

        // Initialize galaxy theme on start
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        Boolean savedOverride = prefs.contains("nebula_theme_override")
                ? prefs.getBoolean("nebula_theme_override", false)
                : null;

        boolean isDark;
        if (savedOverride != null) {
            isDark = savedOverride;
        } else {
            String mode = prefs.getString("pref_theme_mode", "Dark Theme");
            if ("Dark Theme".equalsIgnoreCase(mode) || "dark".equalsIgnoreCase(mode)) {
                isDark = true;
            } else if ("Light Theme".equalsIgnoreCase(mode) || "light".equalsIgnoreCase(mode)) {
                isDark = false;
            } else {
                int nightModeFlags = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
                isDark = (nightModeFlags == Configuration.UI_MODE_NIGHT_YES);
            }
        }
        galaxy.setThemeOverrideDark(isDark);

        mintCrystalsTxt.setText(String.valueOf(mintCrystals.getCoins()));
    }

    private void initViews() {
        startButton = findViewById(R.id.focusStart);

        timerText = findViewById(R.id.timerText);
        pomodoroIndicator = findViewById(R.id.pomodoroIndicator);
        pomodoroIndicator = findViewById(R.id.pomodoroIndicator);
        instructionText = findViewById(R.id.instructionText);
        focusStop = findViewById(R.id.focusStop);
        btnDivider = findViewById(R.id.btnDivider);
        topicsContainer = findViewById(R.id.topicsContainer);

        circularSeekBar = findViewById(R.id.circularProgress);
        crystalBase = findViewById(R.id.crystalBase);
        crystalColor = findViewById(R.id.crystalColor);
        lottieAnimation = findViewById(R.id.lottie);
        mintCrystalsTxt = findViewById(R.id.mintCrystals);
        mintCrystals = new MintCrystals(this);
        // themeSwitch = findViewById(R.id.themeSwitch);
        topicsChipGroup = findViewById(R.id.topicsChipGroup);
        settingsBtn = findViewById(R.id.settingsBtn);
    }

    private void setupReveal() {
        if (crystalColor != null) {
            crystalColor.setRevealFraction(0f);
            crystalColor.invalidate();
        }
        if (lottieAnimation != null) {
            lottieAnimation.setVisibility(INVISIBLE);
        }
    }

    private void setupCircularSeekBar() {
        circularSeekBar.setProgress(0f);
        try {
            circularSeekBar.setMax(180f);
        } catch (Throwable t) {
            Log.e(TAG, String.valueOf(t));
        }

        circularSeekBar.setOnSeekBarChangeListener(new CircularSeekBar.OnCircularSeekBarChangeListener() {
            @Override
            public void onProgressChanged(CircularSeekBar circularSeekBar, float progress, boolean fromUser) {
                if (fromUser) {
                    int val = (int) progress;
                    // Enforce Pomodoro minimum
                    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(FocusMode.this);
                    boolean isPomodoro = prefs.getBoolean(PREF_POMODORO_ENABLED, false);
                    if (isPomodoro) {
                        int minDuration = prefs.getInt(PREF_FOCUS_DURATION, 25);
                        if (val < minDuration) {
                            val = minDuration;
                            // Update seekbar to reflect the forced minimum
                            circularSeekBar.setProgress((float) val);
                        }
                    }

                    selectedMinutes = val;
                    timerText.setText(String.format(Locale.US, "%d min", selectedMinutes));
                    updateThemeForDuration(selectedMinutes);
                }
            }

            @Override
            public void onStopTrackingTouch(CircularSeekBar seekBar) {
            }

            @Override
            public void onStartTrackingTouch(CircularSeekBar seekBar) {
            }
        });

        updateThemeForDuration(selectedMinutes);
        timerText.setText(String.format(Locale.US, "%d min", selectedMinutes));
    }

    private void updateThemeForDuration(int minutes) {
        int crystalResource;
        String lottieResource;
        int progressColor;

        if (minutes <= 30) {
            crystalResource = R.drawable.ruby;
            lottieResource = "ruby.json";
            progressColor = ContextCompat.getColor(this, R.color.ruby);
        } else if (minutes <= 60) {
            crystalResource = R.drawable.emerald;
            lottieResource = "emerald.json";
            progressColor = ContextCompat.getColor(this, R.color.emerald);
        } else if (minutes <= 90) {
            crystalResource = R.drawable.amethyst;
            lottieResource = "amethyst.json";
            progressColor = ContextCompat.getColor(this, R.color.amethyst);
        } else if (minutes <= 120) {
            crystalResource = R.drawable.moonstone;
            lottieResource = "moonstone.json";
            progressColor = ContextCompat.getColor(this, R.color.moonstone);
        } else if (minutes <= 150) {
            crystalResource = R.drawable.aquamarine;
            lottieResource = "aquamarine.json";
            progressColor = ContextCompat.getColor(this, R.color.aquamarine);
        } else {
            crystalResource = R.drawable.amber;
            lottieResource = "amber.json";
            progressColor = ContextCompat.getColor(this, R.color.amber);
        }

        if (crystalBase != null)
            crystalBase.setImageResource(crystalResource);
        if (crystalColor != null)
            crystalColor.setImageResource(crystalResource);
        if (lottieAnimation != null) {
            lottieAnimation.setAnimation(lottieResource);
            lottieAnimation.playAnimation();
        }
        if (circularSeekBar != null) {
            circularSeekBar.setPointerColor(progressColor);
            circularSeekBar.setCircleProgressColor(progressColor);
        }
    }

    private void applyCrystalEffectBase() {
        if (crystalBase != null) {
            ColorMatrix colorMatrix = new ColorMatrix();
            colorMatrix.setSaturation(0);
            ColorMatrixColorFilter filter = new ColorMatrixColorFilter(colorMatrix);
            crystalBase.setColorFilter(filter);
        }
    }

    private void fadeIn(View view) {
        if (view == null)
            return;
        AlphaAnimation fadeIn = new AlphaAnimation(0.0f, 1.0f);
        fadeIn.setDuration(400);
        fadeIn.setFillAfter(true);
        view.startAnimation(fadeIn);
        view.setVisibility(VISIBLE);
    }

    private void fadeOut(View view) {
        if (view == null)
            return;
        AlphaAnimation fadeOut = new AlphaAnimation(1.0f, 0.0f);
        fadeOut.setDuration(500);
        fadeOut.setFillAfter(true);
        fadeOut.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                view.setVisibility(INVISIBLE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });
        view.startAnimation(fadeOut);
    }

    private void showTimerRunningState() {
        String topicName = "Deep Focus";
        if (selectedTopicId != -1) {
            for (FocusTopicEntity t : currentTopics) {
                if (t.id == selectedTopicId) {
                    topicName = t.name;
                    break;
                }
            }
        }
        instructionText.setText("Focusing: " + topicName);

        // Hide settings and topics to prevent changes during run
        if (settingsBtn != null)
            settingsBtn.setVisibility(INVISIBLE);
        if (topicsContainer != null)
            topicsContainer.setVisibility(INVISIBLE);

        fadeIn(lottieAnimation);
        circularSeekBar.setEnabled(false);
        circularSeekBar.setVisibility(INVISIBLE);
    }

    private void showTimerStoppedState() {
        instructionText.setText(getString(R.string.set_your_focus_time));

        // restore settings and topics
        if (settingsBtn != null)
            settingsBtn.setVisibility(VISIBLE);
        if (topicsContainer != null)
            topicsContainer.setVisibility(VISIBLE);

        fadeOut(lottieAnimation);
        circularSeekBar.setEnabled(true);
        circularSeekBar.setVisibility(VISIBLE);
    }

    private void showCrystalForSelection() {
        if (crystalColor == null)
            return;

        if (revealAnimator != null && revealAnimator.isRunning()) {
            revealAnimator.cancel();
        }

        float currentFraction = crystalColor.getRevealFraction();

        revealAnimator = ValueAnimator.ofFloat(currentFraction, 1f);
        revealAnimator.setDuration(600);
        revealAnimator.addUpdateListener(animation -> {
            float value = (float) animation.getAnimatedValue();
            crystalColor.setRevealFraction(value);
            // lastRevealFraction = value;
        });
        revealAnimator.start();
    }

    private void hideCrystalForRunningInit() {
        if (crystalColor == null)
            return;

        if (revealAnimator != null && revealAnimator.isRunning()) {
            revealAnimator.cancel();
        }

        float currentFraction = crystalColor.getRevealFraction();

        revealAnimator = ValueAnimator.ofFloat(currentFraction, 0f);
        revealAnimator.setDuration(600);
        revealAnimator.addUpdateListener(animation -> {
            float value = (float) animation.getAnimatedValue();
            crystalColor.setRevealFraction(value);
            // lastRevealFraction = value;
        });
        revealAnimator.start();
        revealCompleteNotified = false;
        applyCrystalEffectBase();
    }

    private void updateCrystalVisualsForProgress(float progress) {
        if (crystalColor == null)
            return;
        float targetFraction = progress <= 0f ? 0f : Math.min(1f, progress / REVEAL_COMPLETE_AT);
        crystalColor.setRevealFraction(targetFraction);

        if (targetFraction >= 1f && !revealCompleteNotified) {
            revealCompleteNotified = true;
        } else if (targetFraction < 1f) {
            revealCompleteNotified = false;
        }
    }

    private boolean checkNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this,
                    "android.permission.POST_NOTIFICATIONS") != PackageManager.PERMISSION_GRANTED) {
                showNotificationPermissionBottomSheet();
                return false;
            }
        }
        return true;
    }

    private void showNotificationPermissionBottomSheet() {
        if (Build.VERSION.SDK_INT < 33) {
            Toast.makeText(this, "Notification permission not required on this Android version", Toast.LENGTH_SHORT)
                    .show();
            return;
        }

        if (isNotificationPermissionGranted()) {
            Toast.makeText(this, "Notification permission already granted", Toast.LENGTH_SHORT).show();
            return;
        }

        Utils.showPermissionSheet(this, Utils.PermissionType.NOTIFICATION,
                new Utils.PermissionLauncher() {
                    @Override
                    public void launchAccessibility(Intent intent) {
                        // Not used for notification
                    }

                    @Override
                    public void launchBattery(Intent intent) {
                        // Not used for notification
                    }

                    @Override
                    public void launchNotification(String permission) {
                        if (requestNotificationPermissionLauncher != null) {
                            requestNotificationPermissionLauncher.launch(permission);
                        }
                    }
                },
                () -> Toast.makeText(this, "Notification permission not granted", Toast.LENGTH_SHORT).show());
    }

    private boolean isNotificationPermissionGranted() {
        if (Build.VERSION.SDK_INT < 33)
            return true;
        return ContextCompat.checkSelfPermission(this,
                Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
    }

    private void setupPermissionLauncher() {
        requestNotificationPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(), isGranted -> {
                    if (isGranted) {
                        Log.d(TAG, "Notification permission granted");
                        Toast.makeText(this, "Notification permission granted.", Toast.LENGTH_SHORT).show();
                    } else {
                        Log.w(TAG, "Notification permission denied");
                        Toast.makeText(this, "Notification permission is required to show focus mode notifications.",
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void checkPermissionAndMoveOn() {
        ConstraintLayout permissionCard = findViewById(R.id.permissionCard);
        if (permissionCard == null)
            return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            boolean allowed = alarmManager != null && alarmManager.canScheduleExactAlarms();
            permissionCard.setVisibility(allowed ? GONE : VISIBLE);
            if (!allowed) {
                permissionCard.setOnClickListener(v -> askForExactAlarmPermission());
            } else {
                permissionCard.setOnClickListener(null);
            }
        } else {
            permissionCard.setVisibility(GONE);
            permissionCard.setOnClickListener(null);
        }
    }

    private boolean hasExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            return alarmManager != null && alarmManager.canScheduleExactAlarms();
        }
        return true;
    }

    private void askForExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Utils.showPermissionSheet(this, Utils.PermissionType.ALARM,
                    null,
                    () -> {
                        Toast.makeText(FocusMode.this, "Alarm permission is required for Focus Mode timer.",
                                Toast.LENGTH_SHORT).show();
                        checkPermissionAndMoveOn();
                    });
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        bindService();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mintCrystalsTxt.setText(String.valueOf(mintCrystals.getCoins()));
        handler.removeCallbacks(updateUITask);
        applyCrystalEffectBase();
        updatePomodoroIndicator();
        // Re-check exact alarm permission when returning to screen
        checkPermissionAndMoveOn();

        if (isBound && focusService != null) {
            boolean isRunning = focusService.isTimerRunning();

            if (isRunning) {
                if (revealAnimator != null && revealAnimator.isRunning()) {
                    revealAnimator.cancel();
                }

                updateButtonStates(true);

                // Handle pause state - restore frozen crystal position
                if (focusService.isPaused()) {
                    // Restore saved crystal reveal fraction
                    float savedFraction = focusService.getCrystalRevealFraction();
                    if (crystalColor != null && savedFraction > 0) {
                        crystalColor.setRevealFraction(savedFraction);
                    }
                } else {
                    // Normal running state - calculate reveal from progress
                    long totalDuration = focusService.getCurrentDuration();
                    long elapsedMillis = focusService.getElapsedMillis();

                    if (totalDuration <= 0)
                        totalDuration = 1;
                    if (elapsedMillis < 0)
                        elapsedMillis = 0;
                    if (elapsedMillis > totalDuration)
                        elapsedMillis = totalDuration;

                    float progress = elapsedMillis / (float) totalDuration;
                    float targetReveal = Math.min(1f, progress / REVEAL_COMPLETE_AT);

                    if (crystalColor != null) {
                        crystalColor.setRevealFraction(targetReveal);
                    }
                }

                handler.post(updateUITask);
            } else {
                updateButtonStates(false);
                timerText.setText(String.format(Locale.US, "%d min", selectedMinutes));
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(updateUITask);
    }

    @Override
    protected void onStop() {
        super.onStop();
        handler.removeCallbacks(updateUITask);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(updateUITask);
        if (isBound) {
            try {
                unbindService(connection);
                isBound = false;
            } catch (IllegalArgumentException e) {
                // Service was not bound
            }
        }
    }

    private final ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            FocusService.TimerBinder binder = (FocusService.TimerBinder) service;
            focusService = binder.getService();
            isBound = true;
            boolean isRunning = focusService.isTimerRunning();

            if (isRunning) {
                if (revealAnimator != null && revealAnimator.isRunning()) {
                    revealAnimator.cancel();
                }
                long totalDuration = focusService.getCurrentDuration();
                selectedMinutes = (int) (totalDuration / (1000 * 60));
            } else {
                timerText.setText(String.format(Locale.US, "%d min", selectedMinutes));
            }

            updateButtonStates(isRunning);

            // If timer is NOT running, animate crystal to full reveal for selection
            if (!isRunning) {
                showCrystalForSelection();
            }

            if (isRunning) {
                updateTimerUI();

                // Handle pause state - restore frozen crystal position
                if (focusService.isPaused()) {
                    float savedFraction = focusService.getCrystalRevealFraction();
                    if (crystalColor != null && savedFraction > 0) {
                        crystalColor.setRevealFraction(savedFraction);
                    }
                } else {
                    // Normal running - sync crystal from progress
                    long elapsedMillis = focusService.getElapsedMillis();
                    long totalDuration = focusService.getCurrentDuration();
                    if (totalDuration > 0 && crystalColor != null) {
                        float progress = elapsedMillis / (float) totalDuration;
                        float targetReveal = Math.min(1f, progress / REVEAL_COMPLETE_AT);
                        crystalColor.setRevealFraction(targetReveal);
                    }
                }

                handler.post(updateUITask);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isBound = false;
            focusService = null;
            updateButtonStates(false);
        }
    };

    private void updateTimerUI() {
        if (isBound && focusService != null && focusService.isTimerRunning()) {
            if (focusService.isPaused()) {
                updatePomodoroIndicator();
                updateButtonStates(true);

                if (focusService.isBreak()) {
                    // BREAK_PAUSED state - show break countdown, freeze crystal
                    long breakRemaining = focusService.getBreakRemainingMillis();
                    long minutes = breakRemaining / (1000 * 60);
                    long seconds = (breakRemaining / 1000) % 60;
                    timerText.setText(String.format(Locale.US, "Break %02d:%02d", minutes, seconds));
                } else {
                    // WAITING_USER state - show tap to resume
                    timerText.setText("Tap Resume");
                }
                return;
            }

            // FOCUS_RUNNING state - normal logic
            long elapsedMillis = focusService.getElapsedMillis();
            long totalDuration = focusService.getCurrentDuration();

            if (totalDuration <= 0) {
                totalDuration = 1;
            }
            if (elapsedMillis < 0) {
                elapsedMillis = 0;
            }
            if (elapsedMillis > totalDuration) {
                elapsedMillis = totalDuration;
            }

            long remainingMillis = totalDuration - elapsedMillis;

            // Update timer text
            long minutes = remainingMillis / (1000 * 60);
            long seconds = (remainingMillis / 1000) % 60;
            String timeStr = String.format(Locale.US, "%02d:%02d", minutes, seconds);
            timerText.setText(timeStr);

            updatePomodoroIndicator();

            float progress = elapsedMillis / (float) totalDuration;
            float seekbarProgress = progress * 180f;
            if (Float.isNaN(seekbarProgress) || Float.isInfinite(seekbarProgress)) {
                seekbarProgress = 0f;
            }
            if (seekbarProgress < 0f || seekbarProgress > 180f) {
                if (!warnedInvalidDuration) {
                    warnedInvalidDuration = true;
                }
            }
            seekbarProgress = Math.max(0f, Math.min(seekbarProgress, 180f));
            circularSeekBar.setProgress(seekbarProgress);

            updateCrystalVisualsForProgress(progress);

            // Store reveal fraction for pause recovery
            if (crystalColor != null) {
                focusService.setCrystalRevealFraction(crystalColor.getRevealFraction());
            }
        } else if (focusService != null && !focusService.isTimerRunning()) {
            timerText.setText(String.format(Locale.US, "%d min", selectedMinutes));
        }
    }

    private void updateButtonStates(boolean timerIsRunning) {
        if (timerIsRunning && focusService != null) {
            if (revealAnimator != null && revealAnimator.isRunning()) {
                revealAnimator.cancel();
            }
            showTimerRunningState();
            updateThemeForDuration(selectedMinutes);
            mintCrystalsTxt.setText(String.valueOf(mintCrystals.getCoins()));

            // Update button text based on pause state
            if (focusService.isPaused() && !focusService.isBreak()) {
                // WAITING_USER state - show Resume AND Stop button
                startButton.setText("Resume");
                focusStop.setVisibility(View.VISIBLE);
                btnDivider.setVisibility(View.VISIBLE);
            } else {
                // Running or Break - show Stop only (on the main button)
                startButton.setText(getString(R.string.stop));
                focusStop.setVisibility(View.GONE);
                btnDivider.setVisibility(View.GONE);
            }
        } else {
            updateThemeForDuration(selectedMinutes);
            showTimerStoppedState();
            startButton.setText(getString(R.string.start));
            focusStop.setVisibility(View.GONE);
            btnDivider.setVisibility(View.GONE);
            mintCrystalsTxt.setText(String.valueOf(mintCrystals.getCoins()));
            circularSeekBar.setProgress(selectedMinutes);
            timerText.setText(String.format(Locale.US, "%d min", selectedMinutes));
            handler.removeCallbacks(updateUITask);
        }
    }

    private final Runnable updateUITask = new Runnable() {
        @Override
        public void run() {
            if (isBound && focusService != null) {
                if (focusService.isTimerRunning()) {
                    updateTimerUI();
                    handler.postDelayed(this, 900);
                } else {
                    updateButtonStates(false);
                    // Timer stopped - animate crystal to full reveal for selection
                    showCrystalForSelection();
                    // If completed naturally, show completion dialog while user is on UI
                    if (focusService.consumeCompletedNaturally()) {
                        int minutes = focusService.getLastCompletedDurationMinutes();
                        showFocusCompleteDialog(minutes);
                        mintCrystalsTxt.setText(String.valueOf(mintCrystals.getCoins()));
                    }
                }
            }
        }
    };

    private void bindService() {
        if (!isBound) {
            Intent serviceIntent = new Intent(this, FocusService.class);
            bindService(serviceIntent, connection, Context.BIND_AUTO_CREATE);
        }
    }

    private long getDuration() {
        return selectedMinutes * 60L * 1000L;
    }

    private void showPeaceCoinsDialog() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_peace_coins);
        dialog.findViewById(R.id.btnClose).setOnClickListener(v -> dialog.dismiss());

        setupTimelineNode(dialog, R.id.node30, "30m", "+2");
        setupTimelineNode(dialog, R.id.node60, "60m", "+5"); // Active
        setupTimelineNode(dialog, R.id.node90, "90m", "+7");
        setupTimelineNode(dialog, R.id.node120, "120m", "+10");
        setupTimelineNode(dialog, R.id.node150, "150m", "+15");
        setupTimelineNode(dialog, R.id.node180, "180m", "+20"); // Goal

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setDimAmount(0.7f);
            dialog.getWindow().setLayout(MATCH_PARENT, WRAP_CONTENT);
        }

        dialog.setCanceledOnTouchOutside(true);
        dialog.show();
    }

    private void setupTimelineNode(Dialog dialog, int includeId, String time, String reward) {
        View nodeView = dialog.findViewById(includeId);
        if (nodeView == null)
            return;

        TextView tvReward = nodeView.findViewById(R.id.tvReward);
        TextView tvTime = nodeView.findViewById(R.id.tvTime);
        View viewDot = nodeView.findViewById(R.id.viewDot);
        tvTime.setText(time);
        tvReward.setText(reward);

        int colorPink = ContextCompat.getColor(this, R.color.brand_pink);

        tvReward.setTextColor(colorPink);
        viewDot.setBackgroundResource(R.drawable.glass_circle);
        viewDot.setBackgroundTintList(ColorStateList.valueOf(colorPink));

        viewDot.setScaleX(1.3f);
        viewDot.setScaleY(1.3f);

    }

    @SuppressLint("ClickableViewAccessibility")
    private void showFocusCompleteDialog(int minutes) {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_focus_complete);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setDimAmount(0.6f);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                dialog.getWindow().setBackgroundBlurRadius(10);
            }
            dialog.getWindow().setLayout(MATCH_PARENT, MATCH_PARENT);
        }

        ImageView crystalImg = dialog.findViewById(R.id.crystalImage);
        LottieAnimationView sparkle = dialog.findViewById(R.id.sparkleAnim);
        TextView title = dialog.findViewById(R.id.titleText);
        TextView desc = dialog.findViewById(R.id.descText);
        TextView coinsText = dialog.findViewById(R.id.coinsText);
        MaterialTextView okBtn = dialog.findViewById(R.id.okBtn);

        int coins;
        int crystalRes;

        int progressColor;
        String lottieResource;
        if (minutes <= 30) {
            coins = 2;
            crystalRes = R.drawable.ruby;
            lottieResource = "ruby.json";
            progressColor = ContextCompat.getColor(this, R.color.ruby);
        } else if (minutes <= 60) {
            coins = 5;
            crystalRes = R.drawable.emerald;
            lottieResource = "emerald.json";
            progressColor = ContextCompat.getColor(this, R.color.emerald);
        } else if (minutes <= 90) {
            coins = 7;
            crystalRes = R.drawable.amethyst;
            lottieResource = "amethyst.json";
            progressColor = ContextCompat.getColor(this, R.color.amethyst);
        } else if (minutes <= 120) {
            coins = 10;
            crystalRes = R.drawable.moonstone;
            lottieResource = "moonstone.json";
            progressColor = ContextCompat.getColor(this, R.color.moonstone);
        } else if (minutes <= 150) {
            coins = 15;
            crystalRes = R.drawable.aquamarine;
            lottieResource = "aquamarine.json";
            progressColor = ContextCompat.getColor(this, R.color.aquamarine);
        } else {
            coins = 20;
            crystalRes = R.drawable.amber;
            lottieResource = "amber.json";
            progressColor = ContextCompat.getColor(this, R.color.amber);
        }

        if (crystalImg != null)
            crystalImg.setImageResource(crystalRes);
        if (title != null)
            title.setText(R.string.congratulations);
        if (desc != null) {
            String compText = "Focus complete: " + minutes + " min";
            desc.setText(compText);
            desc.setTextColor(progressColor);
            desc.setAlpha(0.8f);
        }
        if (coinsText != null) {
            coinsText.setTextColor(progressColor);
        }
        if (okBtn != null) {
            okBtn.setOnClickListener(v -> dialog.dismiss());
        }

        View root = dialog.findViewById(android.R.id.content);
        if (root != null) {
            root.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_PRESS);
        }

        // Crystal scale-in and float micro-motion
        if (crystalImg != null) {
            crystalImg.setScaleX(0.9f);
            crystalImg.setScaleY(0.9f);
            crystalImg.animate()
                    .scaleX(1.0f)
                    .scaleY(1.0f)
                    .setDuration(450)
                    .setInterpolator(new OvershootInterpolator(1.5f))
                    .withEndAction(() -> crystalImg.animate()
                            .translationYBy(-8f)
                            .setDuration(900)
                            .setInterpolator(new DecelerateInterpolator())
                            .start())
                    .start();
        }

        // Sparkle effect around crystal (best-effort if animation present)
        if (sparkle != null) {
            try {
                sparkle.setAnimation(lottieResource);
                sparkle.playAnimation();
            } catch (Throwable ignored) {
                sparkle.setVisibility(View.GONE);
            }
        }

        // Count-up reward text
        if (coinsText != null) {
            ValueAnimator va = ValueAnimator.ofInt(0, coins);
            va.setDuration(900);
            va.addUpdateListener(a -> {
                int val = (int) a.getAnimatedValue();
                String crystalTxt = "+" + val + " Mint Crystals";
                coinsText.setText(crystalTxt);
            });
            va.start();
        }

        if (okBtn != null) {
            okBtn.setOnClickListener(v -> dialog.dismiss());

            okBtn.setOnTouchListener((v, e) -> {
                switch (e.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        v.animate().scaleX(0.98f).scaleY(0.98f).setDuration(80).start();
                        break;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        v.animate().scaleX(1f).scaleY(1f).setDuration(120).start();
                        break;
                }
                v.performClick();
                return true;
            });
        }

        dialog.setCanceledOnTouchOutside(true);
        dialog.show();
        vibrateCompletion();

    }

    private void vibrateCompletion() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                VibratorManager vm = (VibratorManager) getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
                if (vm != null) {
                    long[] pattern = new long[] { 0, 220, 120, 220 };
                    VibrationEffect effect = VibrationEffect.createWaveform(pattern, -1);
                    vm.getDefaultVibrator().vibrate(effect);
                }
            } else {
                Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                if (v != null) {
                    v.vibrate(VibrationEffect.createOneShot(260, VibrationEffect.DEFAULT_AMPLITUDE));
                }
            }
        } catch (Throwable ignored) {
        }
    }

    private void loadTopics() {
        db.focusTopicDao().getAllTopics().observe(this, topics -> {
            currentTopics = topics;
            refreshChipGroup();
        });
    }

    private void refreshChipGroup() {
        topicsChipGroup.removeAllViews();

        for (FocusTopicEntity topic : currentTopics) {
            Chip chip = new Chip(this, null);
            chip.setText(topic.name);
            chip.setCheckable(true);
            chip.setCheckedIconVisible(true);
            chip.setChipBackgroundColor(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.transparent)));
            chip.setChipStrokeColor(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.glass_stroke)));
            chip.setChipStrokeWidth(Utils.dpToPx(1, this));
            chip.setChipCornerRadius(Utils.dpToPx(20, this));
            chip.setChipBackgroundColorResource(R.color.white_10); // 10% white for glass effect
            chip.setTextColor(ContextCompat.getColor(this, R.color.text_primary));

            if (topic.id == selectedTopicId) {
                chip.setChecked(true);
                chip.setChipBackgroundColor(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.brainColor)));
                chip.setTextColor(Color.WHITE);
            }

            chip.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    selectedTopicId = topic.id;
                    chip.setChipBackgroundColor(
                            ColorStateList.valueOf(ContextCompat.getColor(this, R.color.brainColor)));
                    chip.setTextColor(Color.WHITE);
                } else if (selectedTopicId == topic.id) {
                    selectedTopicId = -1;
                    chip.setChipBackgroundColorResource(R.color.white_10);
                    chip.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
                }
            });

            chip.setOnLongClickListener(v -> {
                showDeleteTopicDialog(topic);
                return true;
            });

            topicsChipGroup.addView(chip);
        }

        Chip newChip = new Chip(this);
        newChip.setText("New");
        newChip.setChipIcon(ContextCompat.getDrawable(this, R.drawable.ic_add));
        newChip.setChipIconTint(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.brainColor)));
        newChip.setTextColor(ContextCompat.getColor(this, R.color.brainColor));
        newChip.setChipBackgroundColorResource(R.color.white_10);
        newChip.setChipStrokeColor(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.brainColor)));
        newChip.setChipStrokeWidth(Utils.dpToPx(1, this));
        newChip.setChipCornerRadius(Utils.dpToPx(20, this));
        newChip.setOnClickListener(v -> showAddTopicDialog());

        topicsChipGroup.addView(newChip);
    }

    private void showAddTopicDialog() {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        builder.setTitle("New Focus Topic");
        final TextInputEditText input = new TextInputEditText(this);
        input.setHint("Topic Name");
        ConstraintLayout container = new ConstraintLayout(this);
        ConstraintLayout.LayoutParams params = new ConstraintLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT);
        params.setMargins(Utils.dpToPx(20, this), 0, Utils.dpToPx(20, this), 0);
        input.setLayoutParams(params);
        container.addView(input);
        container.setPadding(Utils.dpToPx(20, this), Utils.dpToPx(10, this), Utils.dpToPx(20, this), 0);

        builder.setView(container);

        builder.setPositiveButton("Add", (dialog, which) -> {
            String name = input.getText() != null ? input.getText().toString().trim() : "";
            if (!name.isEmpty()) {
                Executors.newSingleThreadExecutor().execute(() -> {
                    db.focusTopicDao().insert(new FocusTopicEntity(name));
                });
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void showDeleteTopicDialog(FocusTopicEntity topic) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Delete Topic")
                .setMessage("Are you sure you want to delete '" + topic.name + "'?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    Executors.newSingleThreadExecutor().execute(() -> {
                        db.focusTopicDao().delete(topic);
                        if (selectedTopicId == topic.id) {
                            selectedTopicId = -1;
                        }
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showSettingsBottomSheet() {
        BottomSheetDialog sheet = new BottomSheetDialog(this, R.style.CustomBottomSheetTheme);
        sheet.setContentView(R.layout.bottom_sheet_focus);
        sheet.setOnDismissListener(dialog -> updatePomodoroIndicator());

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        MaterialSwitch themeSwitchSheet = sheet.findViewById(R.id.themeSwitch);
        NebulaStarfieldView galaxy = findViewById(R.id.starFieldView);

        if (themeSwitchSheet != null && galaxy != null) {
            Boolean savedOverride = prefs.contains("nebula_theme_override")
                    ? prefs.getBoolean("nebula_theme_override", false)
                    : null;

            boolean isDark;
            if (savedOverride != null) {
                isDark = savedOverride;
            } else {
                String mode = prefs.getString("pref_theme_mode", "Dark Theme");
                if ("Dark Theme".equalsIgnoreCase(mode) || "dark".equalsIgnoreCase(mode)) {
                    isDark = true;
                } else if ("Light Theme".equalsIgnoreCase(mode) || "light".equalsIgnoreCase(mode)) {
                    isDark = false;
                } else {
                    int nightModeFlags = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
                    isDark = (nightModeFlags == Configuration.UI_MODE_NIGHT_YES);
                }
            }

            themeSwitchSheet.setChecked(isDark);
            galaxy.setThemeOverrideDark(isDark);

            themeSwitchSheet.setOnCheckedChangeListener((buttonView, checked) -> {
                prefs.edit().putBoolean("nebula_theme_override", checked).apply();
                galaxy.setThemeOverrideDark(checked);
                getDelegate().setLocalNightMode(
                        checked ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
            });
        }

        MaterialSwitch pomodoroSwitch = sheet.findViewById(R.id.pomodoroSwitch);
        View pomodoroSettings = sheet.findViewById(R.id.pomodoroSettings);
        View breakCard = sheet.findViewById(R.id.breakCard);

        if (pomodoroSwitch != null && pomodoroSettings != null) {
            boolean enabled = prefs.getBoolean(PREF_POMODORO_ENABLED, false);
            pomodoroSwitch.setChecked(enabled);
            pomodoroSettings.setVisibility(enabled ? VISIBLE : GONE);
            if (breakCard != null)
                breakCard.setVisibility(enabled ? VISIBLE : GONE);

            pomodoroSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                prefs.edit().putBoolean(PREF_POMODORO_ENABLED, isChecked).apply();
                pomodoroSettings.setVisibility(isChecked ? VISIBLE : GONE);
                if (breakCard != null)
                    breakCard.setVisibility(isChecked ? VISIBLE : GONE);

                // Auto-enable Auto Resume when Pomodoro is turned ON
                if (isChecked) {
                    MaterialSwitch bSwitch = sheet.findViewById(R.id.breakSwitch);
                    View bSettings = sheet.findViewById(R.id.breakSettings);
                    if (bSwitch != null) {
                        bSwitch.setChecked(true);
                        prefs.edit().putBoolean("pref_auto_start_break", true).apply();
                        if (bSettings != null)
                            bSettings.setVisibility(VISIBLE);
                    }
                    showPomodoroFirstTimeDialogIfNeeded();
                }
            });
        }

        SeekBar focusSeekBar = sheet.findViewById(R.id.focusSeekBar);
        TextView focusLabel = sheet.findViewById(R.id.focusDurationLabel);
        if (focusSeekBar != null && focusLabel != null) {
            int savedDuration = prefs.getInt(PREF_FOCUS_DURATION, 30);
            focusSeekBar.setProgress(savedDuration - 20);
            focusLabel.setText("Focus Interval: " + savedDuration + " min");
            focusSeekBar.setOnSeekBarChangeListener(new android.widget.SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(android.widget.SeekBar seekBar, int progress, boolean fromUser) {
                    int val = progress + 20;
                    focusLabel.setText("Focus Interval: " + val + " min");
                    prefs.edit().putInt(PREF_FOCUS_DURATION, val).apply();
                }

                @Override
                public void onStartTrackingTouch(android.widget.SeekBar seekBar) {
                }

                @Override
                public void onStopTrackingTouch(android.widget.SeekBar seekBar) {
                }
            });
        }

        MaterialSwitch breakSwitch = sheet.findViewById(R.id.breakSwitch);
        View breakSettings = sheet.findViewById(R.id.breakSettings);
        if (breakSwitch != null && breakSettings != null) {
            // Auto Resume toggle - controls whether focus auto-resumes after break
            boolean autoResumeEnabled = prefs.getBoolean("pref_auto_start_break", true);
            breakSwitch.setChecked(autoResumeEnabled);
            breakSettings.setVisibility(autoResumeEnabled ? VISIBLE : GONE); // Hide duration if Auto Resume is OFF

            breakSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                prefs.edit().putBoolean("pref_auto_start_break", isChecked).apply();
                breakSettings.setVisibility(isChecked ? VISIBLE : GONE);

                if (!isChecked) {
                    showAutoKillWarning();
                }
            });
        }

        SeekBar breakSeekBar = sheet.findViewById(R.id.breakSeekBar);
        TextView breakLabel = sheet.findViewById(R.id.breakDurationLabel);
        if (breakSeekBar != null && breakLabel != null) {
            int savedDuration = prefs.getInt(PREF_BREAK_DURATION, 5);
            breakSeekBar.setProgress(savedDuration - 3);
            breakLabel.setText("Break Duration: " + savedDuration + " min");
            breakSeekBar.setOnSeekBarChangeListener(new android.widget.SeekBar.OnSeekBarChangeListener() {

                @Override
                public void onProgressChanged(android.widget.SeekBar seekBar, int progress, boolean fromUser) {
                    int val = progress + 3;
                    breakLabel.setText("Break Duration: " + val + " min");
                    prefs.edit().putInt(PREF_BREAK_DURATION, val).apply();
                }

                @Override
                public void onStartTrackingTouch(android.widget.SeekBar seekBar) {
                }

                @Override
                public void onStopTrackingTouch(android.widget.SeekBar seekBar) {
                }
            });
        }

        View crossBtn = sheet.findViewById(R.id.crossBtn);
        if (crossBtn != null) {
            crossBtn.setOnClickListener(v -> sheet.dismiss());
        }

        sheet.setOnDismissListener(dialog -> {
            // Re-validate duration when settings close
            SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(this);
            boolean isPomo = p.getBoolean(PREF_POMODORO_ENABLED, false);
            if (isPomo) {
                int minDur = p.getInt(PREF_FOCUS_DURATION, 25);
                if (selectedMinutes < minDur) {
                    selectedMinutes = minDur;
                    circularSeekBar.setProgress((float) minDur);
                    timerText.setText(String.format(Locale.US, "%d min", selectedMinutes));
                    updateThemeForDuration(selectedMinutes);
                }
            }
            updatePomodoroIndicator();
        });

        sheet.show();
    }

    private void updatePomodoroIndicator() {
        if (pomodoroIndicator == null)
            return;

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        boolean isPomoPref = prefs.getBoolean(PREF_POMODORO_ENABLED, false);

        if (isBound && focusService != null && focusService.isTimerRunning()) {
            if (focusService.isPomodoroEnabled()) {
                pomodoroIndicator.setVisibility(View.VISIBLE);
                if (focusService.isPaused() && focusService.isBreak()) {
                    // BREAK_PAUSED state
                    pomodoroIndicator.setText(isTestMode ? "TEST: Break" : "Pomodoro: Break Time");
                    pomodoroIndicator.setTextColor(ContextCompat.getColor(this, R.color.greenIcon));
                } else if (focusService.isPaused()) {
                    // WAITING_USER state
                    pomodoroIndicator.setText(isTestMode ? "TEST: Paused" : "Pomodoro: Tap Resume");
                    pomodoroIndicator.setTextColor(ContextCompat.getColor(this, R.color.status_amber));
                } else {
                    // FOCUS_RUNNING state
                    pomodoroIndicator.setText(isTestMode ? "TEST: Focus" : "Pomodoro: Focus");
                    pomodoroIndicator.setTextColor(ContextCompat.getColor(this, R.color.redIcon));
                }
            } else {
                pomodoroIndicator.setVisibility(View.GONE);
            }
        } else {
            // Not running
            if (isTestMode) {
                pomodoroIndicator.setVisibility(View.VISIBLE);
                pomodoroIndicator.setText("TEST MODE ACTIVE");
                pomodoroIndicator.setTextColor(ContextCompat.getColor(this, R.color.status_amber));
            } else if (isPomoPref) {
                pomodoroIndicator.setVisibility(View.VISIBLE);
                long focus = prefs.getInt(PREF_FOCUS_DURATION, 25);
                long brk = prefs.getInt(PREF_BREAK_DURATION, 5);
                pomodoroIndicator.setText("Pomodoro Enabled (" + focus + "/" + brk + ")");
                pomodoroIndicator.setTextColor(ContextCompat.getColor(this, R.color.text_tertiary));
            } else {
                pomodoroIndicator.setVisibility(View.GONE);
            }
        }
    }

    private void showPomodoroFirstTimeDialogIfNeeded() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        if (prefs.getBoolean("pref_pomodoro_first_time_shown", false)) {
            return; // Already shown
        }
        showAutoKillWarning(); // Use the same message
        prefs.edit().putBoolean("pref_pomodoro_first_time_shown", true).apply();
    }

    private void showAutoKillWarning() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Important Info")
                .setMessage("If you don't resume the timer within 20 minutes after a break, " +
                        "the session will automatically end.\n\n" +
                        "This won't deduct any coins.")
                .setPositiveButton("Got it", null)
                .show();
    }
}
