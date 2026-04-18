package com.gxdevs.mindmint.Fragments;

import static android.app.Activity.RESULT_OK;
import static android.content.Context.POWER_SERVICE;
import static com.gxdevs.mindmint.Utils.Utils.isAccessibilityPermissionGranted;

import android.Manifest;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.text.style.ImageSpan;
import android.text.style.StyleSpan;
import android.text.style.UnderlineSpan;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.gxdevs.mindmint.Activities.CustomAppSelectionActivity;
import com.gxdevs.mindmint.Activities.HomeActivity;
import com.gxdevs.mindmint.Activities.SiteBlockerActivity;
import com.gxdevs.mindmint.Adapters.SettingsAdapter;
import com.gxdevs.mindmint.Models.SettingsItem;
import com.gxdevs.mindmint.R;
import com.gxdevs.mindmint.Services.AppUsageAccessibilityService;
import com.gxdevs.mindmint.Utils.AdultDomainListManager;
import com.gxdevs.mindmint.Utils.AlarmUtils;
import com.gxdevs.mindmint.Utils.BackupManager;
import com.gxdevs.mindmint.Utils.BlockedSitesManager;
import com.gxdevs.mindmint.Utils.SettingsLockManager;
import com.gxdevs.mindmint.Utils.Utils;
import com.gxdevs.mindmint.Utils.WarningUtils;

import java.util.ArrayList;
import java.util.List;

import com.skydoves.balloon.ArrowOrientation;
import com.skydoves.balloon.Balloon;
import com.skydoves.balloon.BalloonAnimation;
import com.skydoves.balloon.BalloonSizeSpec;

public class SettingsFragment extends Fragment {

    private static final int ID_REMIND_DOOM = 1;
    private static final int ID_BLOCK_CONTENT = 2;
    private static final int ID_KEEP_ALIVE = 3;
    private static final int ID_CUSTOM_APP = 4;
    private static final int ID_BROWSER_BLOCKER = 5;
    private static final int ID_ADULT_BLOCK = 7;
    private static final int ID_POPUP_DURATION = 8;
    private static final int ID_THEME = 9;
    private static final int ID_SCROLL_COUNTER = 10;
    private static final int ID_SCROLL_TAB = 11;
    private static final int ID_SETTINGS_LOCK = 12;
    private static final int ID_LOCK_TYPE_TAB = 13;
    private static final int ID_ALWAYS_LOCK_IN = 14;
    private static final int ID_PERM_ACCESSIBILITY = 100;
    private static final int ID_PERM_NOTIFICATION = 101;
    private static final int ID_PERM_ALARM = 102;
    private static final int ID_PERM_BATTERY = 103;
    private static final int ID_BACKUP = 104;
    public static final String PREF_BLOCK_AFTER_WASTED_TIME_ENABLED = "pref_block_after_wasted_time_enabled";
    public static final String PREF_BLOCK_AFTER_WASTED_TIME_HOURS = "pref_block_after_wasted_time_hours";
    public static final float DEFAULT_BLOCK_AFTER_WASTED_TIME_HOURS = 1.0f;
    public static final String PREF_BLOCK_BROWSERS_DOOMSCROLLING_ENABLED = "pref_block_browsers_doom_enabled";
    public static final String PREF_BLOCK_ADULT_SITES_ENABLED = "pref_block_adult_sites_enabled";
    public static final String PREF_THEME_MODE = "pref_theme_mode";
    private static final String PREF_BROWSER_BLOCK_TUTORIAL_SHOWN = "pref_browser_block_tutorial_shown";

    private SharedPreferences defaultSharedPreferences;
    private RecyclerView recyclerView;
    private SettingsAdapter adapter;
    private List<SettingsItem> settingsItems;

    private ActivityResultLauncher<Intent> batteryOptimizationLauncher;
    private ActivityResultLauncher<Intent> accessibilityLauncher;
    private ActivityResultLauncher<String> notificationPermissionLauncher;
    private ActivityResultLauncher<Intent> exportLauncher;
    private ActivityResultLauncher<Intent> importLauncher;

    private BottomSheetDialog timerPicker;
    private boolean batteryOptimizationIgnored = false;
    private boolean isImportOverride = false; // For import state

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);
        defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext());

        View rootFade = view.findViewById(R.id.headerContainer);
        if (rootFade != null) {
            rootFade.setAlpha(0f);
            rootFade.animate().alpha(1f).setDuration(180).start();
        }

        recyclerView = view.findViewById(R.id.settingsRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        settingsItems = new ArrayList<>();
        adapter = new SettingsAdapter(requireContext(), settingsItems);
        recyclerView.setAdapter(adapter);

        registerForPermission();
        registerAccessibilityLauncher();
        registerNotificationPermissionLauncher();
        registerBackupLaunchers();
        refreshList();

        return view;
    }

    private void registerBackupLaunchers() {
        exportLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        android.net.Uri uri = result.getData().getData();
                        if (uri != null) {
                            try {
                                BackupManager.exportData(requireContext(), uri);
                                Toast.makeText(requireContext(), "Backup exported successfully", Toast.LENGTH_SHORT)
                                        .show();
                            } catch (Exception e) {
                                Toast.makeText(requireContext(), "Export failed: " + e.getMessage(), Toast.LENGTH_SHORT)
                                        .show();
                            }
                        }
                    }
                });

        importLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        android.net.Uri uri = result.getData().getData();
                        if (uri != null) {
                            try {
                                BackupManager.importData(requireContext(), uri, isImportOverride);
                                Toast.makeText(requireContext(), "Data imported successfully", Toast.LENGTH_SHORT)
                                        .show();
                                refreshList();
                            } catch (Exception e) {
                                Toast.makeText(requireContext(), "Import failed: " + e.getMessage(), Toast.LENGTH_SHORT)
                                        .show();
                            }
                        }
                    }
                });
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshList();
    }

    private void refreshList() {
        settingsItems.clear();
        buildSettingsList();
        adapter.setCurrentTheme(defaultSharedPreferences.getString(PREF_THEME_MODE, "Dark Theme"));
        adapter.setOnSeekbarChangeListener(progress -> {
            int minSeconds = 3;
            int seconds = minSeconds + progress;
            defaultSharedPreferences.edit()
                    .putInt(AppUsageAccessibilityService.PREF_BLOCKING_POPUP_DURATION_SEC, seconds).apply();
        });

        adapter.setOnThemeChangeListener(this::applyTheme);
        adapter.setOnLockTabActionListener(new SettingsAdapter.OnLockTabActionListener() {
            @Override
            public void onRequestLockTypeChange(String newLockType, Runnable onSuccess) {
                authenticateToChangeSetting("Change lock type", () -> {
                    SettingsLockManager lockMgr = new SettingsLockManager(requireContext());
                    if (SettingsLockManager.LOCK_TYPE_DEVICE.equals(newLockType)) {
                        if (!lockMgr.isDeviceLockAvailable()) {
                            Toast.makeText(requireContext(), "Device lock not found. Please set a custom PIN.", Toast.LENGTH_LONG).show();
                            refreshList();
                            return;
                        }
                        lockMgr.clearCustomPin();
                    }
                    onSuccess.run();
                    if (SettingsLockManager.LOCK_TYPE_CUSTOM.equals(newLockType) && !lockMgr.hasCustomPin()) {
                        lockMgr.showSetCustomPinDialog(requireContext(), false, null);
                    }
                });
            }

            @Override
            public void onEditCustomPin() {
                SettingsLockManager lockMgr = new SettingsLockManager(requireContext());
                if (lockMgr.hasCustomPin()) {
                    lockMgr.showVerifyPinDialog(requireContext(), "Enter current PIN to continue", verified -> {
                        if (verified) lockMgr.showSetCustomPinDialog(requireContext(), true, null);
                    });
                } else {
                    lockMgr.showSetCustomPinDialog(requireContext(), false, null);
                }
            }
        });
        adapter.setOnBackupActionListener(new SettingsAdapter.OnBackupActionListener() {
            @Override
            public void onExport() {
                Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("application/json");
                intent.putExtra(Intent.EXTRA_TITLE, "mindmint_backup_" + System.currentTimeMillis() + ".brain");
                exportLauncher.launch(intent);
            }

            @Override
            public void onImport(boolean override) {
                isImportOverride = override;
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("*/*"); // Allow all because extension is .brain
                importLauncher.launch(intent);
            }
        });

        adapter.notifyDataSetChanged();
    }

    private int getThemeColor(int attr) {
        TypedValue typedValue = new TypedValue();
        if (getContext() != null && getContext().getTheme().resolveAttribute(attr, typedValue, true)) {
            return typedValue.data;
        }
        return 0;
    }

    private void buildSettingsList() {
        int eyeBg = getThemeColor(R.attr.eye_bg);
        int mobileBg = getThemeColor(R.attr.mobile_bg);
        int browserBg = getThemeColor(R.attr.browser_bg);
        int blockBg = getThemeColor(R.attr.block_bg);
        int popupBg = getThemeColor(R.attr.popup_bg);
        int textSecondary = getThemeColor(R.attr.text_secondary);

        // Hex Tints from backup
        int redIcon = Color.parseColor("#F77381");
        int blueIcon = Color.parseColor("#61A2F2");
        int greenIcon = Color.parseColor("#3DD7A5");
        int purpleIcon = Color.parseColor("#BF83FB");
        int grayIcon = Color.parseColor("#ABABAB");
        int tealIcon = Color.parseColor("#009688");

        // FOCUS CONTROLS
        settingsItems.add(new SettingsItem(SettingsItem.TYPE_HEADER, "FOCUS CONTROLS"));

        // Remind Doom Scrolling
        boolean isRemindEnabled = defaultSharedPreferences.getBoolean(AppUsageAccessibilityService.PREF_REMIND_DOOM_SCROLLING_ENABLED, false);
        SettingsItem remindItem = new SettingsItem(ID_REMIND_DOOM, SettingsItem.TYPE_SWITCH, "Remind me",
                "Get nudges to return to focus", R.drawable.bell, textSecondary)
                .setSwitch(true, isRemindEnabled, (buttonView, isChecked) -> {
                    lockedSwitchAction("Change Remind me", buttonView, !isChecked, isChecked, () -> {
                        if (isChecked && !isAccessibilityPermissionGranted(requireContext())) {
                            buttonView.setChecked(false);
                            defaultSharedPreferences.edit()
                                    .putBoolean(AppUsageAccessibilityService.PREF_REMIND_DOOM_SCROLLING_ENABLED, false)
                                    .apply();
                            shakeCard(ID_PERM_ACCESSIBILITY);
                            return;
                        }
                        defaultSharedPreferences.edit()
                                .putBoolean(AppUsageAccessibilityService.PREF_REMIND_DOOM_SCROLLING_ENABLED, isChecked)
                                .apply();
                    });
                });

        if (isRemindEnabled) {
            remindItem.setFormattedSubtitle(getRemindDoomFormattedSubtitle());
            remindItem.setOnClickListener(v -> authenticateToChangeSetting("Edit Reminder Time", () -> showTimePickerBottomSheet(true)));
        }
        settingsItems.add(remindItem);

        boolean isBlockEnabled = defaultSharedPreferences.getBoolean(PREF_BLOCK_AFTER_WASTED_TIME_ENABLED, false);
        SettingsItem blockItem = new SettingsItem(ID_BLOCK_CONTENT, SettingsItem.TYPE_SWITCH, "Block content",
                "Automatically block content", R.drawable.eye_off, redIcon)
                .setIconValues(R.drawable.shape_circle, eyeBg)
                .setSwitch(true, isBlockEnabled, (buttonView, isChecked) -> {
                    lockedSwitchAction("Change Block content", buttonView, !isChecked, isChecked, () -> {
                        if (isChecked && !isAccessibilityPermissionGranted(requireContext())) {
                            buttonView.setChecked(false);
                            defaultSharedPreferences.edit().putBoolean(PREF_BLOCK_AFTER_WASTED_TIME_ENABLED, false).apply();
                            shakeCard(ID_PERM_ACCESSIBILITY);
                            return;
                        }
                        defaultSharedPreferences.edit().putBoolean(PREF_BLOCK_AFTER_WASTED_TIME_ENABLED, isChecked).apply();
                    });
                });

        if (isBlockEnabled) {
            blockItem.setFormattedSubtitle(getBlockTimeFormattedSubtitle());
            blockItem.setOnClickListener(v -> authenticateToChangeSetting("Edit Block Time", () -> showTimePickerBottomSheet(false)));
        }
        settingsItems.add(blockItem);

        // Keep Service Alive
        boolean isKeepAlive = defaultSharedPreferences.getBoolean("keepServiceAlive", false);
        settingsItems.add(new SettingsItem(ID_KEEP_ALIVE, SettingsItem.TYPE_SWITCH, "Keep service alive",
                "Prevent OS from killing app", R.drawable.zap, grayIcon).setSwitch(true, isKeepAlive, (buttonView, isChecked) -> {
            lockedSwitchAction("Change Keep service alive", buttonView, !isChecked, isChecked, () -> handleKeepAliveToggle(buttonView, isChecked));
        }));

        // --- Scroll Counter ---
        boolean isScrollCounterOn = defaultSharedPreferences.getBoolean("pref_scroll_counter_enabled", false);
        settingsItems.add(new SettingsItem(ID_SCROLL_COUNTER, SettingsItem.TYPE_SWITCH, "Show scroll counter",
                isScrollCounterOn ? "Pill shown on blocked app screens" : "Show daily scroll count on blocking screen",
                R.drawable.scroll_text, tealIcon)
                .setSwitch(true, isScrollCounterOn, (btn, isChecked) -> {
                    lockedSwitchAction("Change Scroll counter", btn, !isChecked, isChecked, () -> {
                        if (isChecked && !isAccessibilityPermissionGranted(requireContext())) {
                            btn.setChecked(false);
                            defaultSharedPreferences.edit().putBoolean("pref_scroll_counter_enabled", false).apply();
                            shakeCard(ID_PERM_ACCESSIBILITY);
                            return;
                        }
                        defaultSharedPreferences.edit().putBoolean("pref_scroll_counter_enabled", isChecked).apply();
                    });
                }));

        if (isScrollCounterOn) {
            boolean perApp = defaultSharedPreferences.getBoolean("pref_scroll_counter_per_app", false);
            settingsItems.add(new SettingsItem(ID_SCROLL_TAB, SettingsItem.TYPE_SCROLL_TAB,
                    "", "", 0, 0)
                    .setScrollTabPerApp(perApp));
        }

        // BLOCKING RULES
        settingsItems.add(new SettingsItem(SettingsItem.TYPE_HEADER, "BLOCKING RULES"));

        // Browser Blocking
        boolean isBrowserBlockEnabled = defaultSharedPreferences.getBoolean(PREF_BLOCK_BROWSERS_DOOMSCROLLING_ENABLED, false);
        SettingsItem browserItem = new SettingsItem(ID_BROWSER_BLOCKER, SettingsItem.TYPE_SWITCH, "Blocker on browsers",
                isBrowserBlockEnabled ? "Blocking active on browsers" : "Activate blocking on browsers",
                R.drawable.globe, greenIcon)
                .setIconValues(R.drawable.shape_circle, browserBg)
                .setSwitch(true, isBrowserBlockEnabled, (buttonView, isChecked) -> {
                    lockedSwitchAction("Change Browser Blocker", buttonView, !isChecked, isChecked, () -> {
                        if (isChecked && !isAccessibilityPermissionGranted(requireContext())) {
                            buttonView.setChecked(false);
                            defaultSharedPreferences.edit().putBoolean(PREF_BLOCK_BROWSERS_DOOMSCROLLING_ENABLED, false).apply();
                            shakeCard(ID_PERM_ACCESSIBILITY);
                            return;
                        }
                        defaultSharedPreferences.edit().putBoolean(PREF_BLOCK_BROWSERS_DOOMSCROLLING_ENABLED, isChecked)
                                .apply();
                        if (isChecked) {
                            BlockedSitesManager.seedDefaultsIfFirstTimeAndEmpty(requireContext());
                            showBrowserBlockingTutorial();
                        }
                    });
                })
                .setOnClickListener(v -> authenticateToChangeSetting("Edit Blocked Sites", () -> {
                    if (!isAccessibilityPermissionGranted(requireContext())) {
                        shakeCard(ID_PERM_ACCESSIBILITY);
                        return;
                    }
                    startActivity(new Intent(requireContext(), SiteBlockerActivity.class));
                }));
        settingsItems.add(browserItem);

        // Block Adult Sites
        boolean isAdultEnabled = defaultSharedPreferences.getBoolean(PREF_BLOCK_ADULT_SITES_ENABLED, false);
        settingsItems.add(new SettingsItem(ID_ADULT_BLOCK, SettingsItem.TYPE_SWITCH, "Block adult sites", "In Beta",
                R.drawable.shield, redIcon)
                .setIconValues(R.drawable.shape_circle, blockBg)
                .setSwitch(true, isAdultEnabled, (buttonView, isChecked) -> {
                    lockedSwitchAction("Change Adult Blocker", buttonView, !isChecked, isChecked, () -> {
                        if (isChecked && !isAccessibilityPermissionGranted(requireContext())) {
                            buttonView.setChecked(false);
                            defaultSharedPreferences.edit().putBoolean(PREF_BLOCK_ADULT_SITES_ENABLED, false).apply();
                            shakeCard(ID_PERM_ACCESSIBILITY);
                            return;
                        }
                        if (isChecked) {
                            showAdultListDownloadDialogAndEnsure();
                        } else {
                            defaultSharedPreferences.edit().putBoolean(PREF_BLOCK_ADULT_SITES_ENABLED, false).apply();
                        }
                    });
                })
                .setActionText(isAdultEnabled ? "Update list" : null)
                .setArrow(isAdultEnabled)
                .setOnClickListener(v -> authenticateToChangeSetting("Update list", this::showAdultListDownloadDialogAndEnsure)));

        // Blocking Popup Duration
        int savedDuration = defaultSharedPreferences.getInt(AppUsageAccessibilityService.PREF_BLOCKING_POPUP_DURATION_SEC, 5);
        if (savedDuration < 3)
            savedDuration = 3;
        if (savedDuration > 15)
            savedDuration = 15;

        settingsItems.add(new SettingsItem(ID_POPUP_DURATION, SettingsItem.TYPE_SEEKBAR,
                getString(R.string.blocking_popup_duration), getString(R.string.min_5_seconds_in_seconds),
                R.drawable.hourglass, purpleIcon)
                .setIconValues(R.drawable.shape_circle, popupBg)
                .setSeekbar(12, savedDuration - 3, savedDuration + "s"));


        SettingsLockManager lockMgr = new SettingsLockManager(requireContext());
        boolean isLockEnabled = lockMgr.isLockEnabled();
        String subtitleText;
        if (!isLockEnabled) {
            subtitleText = "Lock settings changes";
        } else if (lockMgr.isCustomPin()) {
            subtitleText = "Long press to change PIN";
        } else {
            subtitleText = "Settings are protected";
        }

        settingsItems.add(new SettingsItem(ID_SETTINGS_LOCK, SettingsItem.TYPE_SWITCH,
                "Require password", subtitleText,
                R.drawable.shield, purpleIcon)
                .setIconValues(R.drawable.shape_circle, popupBg)
                .setSwitch(true, isLockEnabled, (buttonView, isChecked) -> {
                    SettingsLockManager lm = new SettingsLockManager(requireContext());
                    if (isChecked) {
                        if (lm.isDeviceLock() && !lm.isDeviceLockAvailable()) {
                            lm.setLockType(SettingsLockManager.LOCK_TYPE_CUSTOM);
                            Toast.makeText(requireContext(), "Device lock not found. Please set a custom PIN.", Toast.LENGTH_LONG).show();
                        }
                        
                        // Enabling: if Custom PIN mode but no PIN set yet, prompt to create one
                        lm.setLockEnabled(true);
                        if (lm.isCustomPin() && !lm.hasCustomPin()) {
                            lm.showSetCustomPinDialog(requireContext(), false, this::refreshList);
                        } else {
                            refreshList();
                        }
                    } else {
                        // Disabling: gate behind current auth
                        buttonView.setChecked(true); // revert visually until verified
                        authenticateToChangeSetting("Disable settings lock", () -> {
                            lm.setLockEnabled(false);
                            lm.clearCustomPin();
                            refreshList();
                        });
                        return;
                    }
                })
                .setOnLongClickListener(v -> {
                    SettingsLockManager lm = new SettingsLockManager(requireContext());
                    if (lm.isLockEnabled() && lm.isCustomPin()) {
                        if (lm.hasCustomPin()) {
                            lm.showVerifyPinDialog(requireContext(), "Enter current PIN to continue", verified -> {
                                if (verified)
                                    lm.showSetCustomPinDialog(requireContext(), true, this::refreshList);
                            });
                        } else {
                            lm.showSetCustomPinDialog(requireContext(), false, this::refreshList);
                        }
                        return true;
                    }
                    return false;
                }));

        // Lock type sub-tab (shown only when lock is enabled)
        if (isLockEnabled) {
            settingsItems.add(new SettingsItem(ID_LOCK_TYPE_TAB, SettingsItem.TYPE_LOCK_TAB, "", "", 0, 0));
        }

        // APPEARANCE
        settingsItems.add(new SettingsItem(SettingsItem.TYPE_HEADER, "APPEARANCE"));
        settingsItems.add(new SettingsItem(ID_THEME, SettingsItem.TYPE_THEME, "Theme", "", 0, 0));

        // BACKUP
        settingsItems.add(new SettingsItem(SettingsItem.TYPE_HEADER, "DATA MANAGEMENT"));
        settingsItems.add(new SettingsItem(ID_BACKUP, SettingsItem.TYPE_BACKUP, "Data Backup",
                "Import or export your data", R.drawable.backup, tealIcon).setIconValues(R.drawable.shape_circle, mobileBg));

        // PERMISSIONS
        addPermissionCards();
    }

    private void showBrowserBlockingTutorial() {
        if (defaultSharedPreferences.getBoolean(PREF_BROWSER_BLOCK_TUTORIAL_SHOWN, false)) {
            return;
        }

        int pos = -1;
        for (int i = 0; i < settingsItems.size(); i++) {
            if (settingsItems.get(i).getId() == ID_BROWSER_BLOCKER) {
                pos = i;
                break;
            }
        }

        if (pos != -1) {
            final int finalPos = pos;
            recyclerView.post(() -> {
                RecyclerView.ViewHolder vh = recyclerView.findViewHolderForAdapterPosition(finalPos);
                if (vh != null) {
                    Balloon balloon = new Balloon.Builder(requireContext())
                            .setArrowSize(10)
                            .setArrowOrientation(ArrowOrientation.BOTTOM)
                            .setArrowPosition(0.5f)
                            .setWidthRatio(0.7f)
                            .setHeight(BalloonSizeSpec.WRAP)
                            .setTextSize(14f)
                            .setCornerRadius(10f)
                            .setAlpha(0.9f)
                            .setPadding(8)
                            .setText("Tap to add more sites")
                            .setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
                            .setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.brainColor))
                            .setBalloonAnimation(BalloonAnimation.ELASTIC)
                            .setDismissWhenClicked(true)
                            .setLifecycleOwner(getViewLifecycleOwner())
                            .setOnBalloonDismissListener(() -> defaultSharedPreferences.edit()
                                    .putBoolean(PREF_BROWSER_BLOCK_TUTORIAL_SHOWN, true).apply())
                            .build();
                    balloon.showAlignTop(vh.itemView);
                }
            });
        }
    }

    private void addPermissionCards() {
        PowerManager pm = (PowerManager) requireContext().getSystemService(POWER_SERVICE);
        batteryOptimizationIgnored = pm != null && pm.isIgnoringBatteryOptimizations(requireContext().getPackageName());

        int statusErrorBg = getThemeColor(R.attr.status_error_bg);
        int statusErrorIcon = getThemeColor(R.attr.status_error_icon);
        int statusErrorText = getThemeColor(R.attr.status_error_text);
        int statusInfoIcon = getThemeColor(R.attr.status_info_icon);
        int statusWarningBg = getThemeColor(R.attr.status_warning_bg);
        int statusWarningIcon = getThemeColor(R.attr.status_warning_icon);
        int textPrimary = getThemeColor(R.attr.text_primary);
        int textTertiary = getThemeColor(R.attr.text_tertiary);

        if (!isAccessibilityPermissionGranted(requireContext())) {
            settingsItems.add(new SettingsItem(ID_PERM_ACCESSIBILITY, SettingsItem.TYPE_PERMISSION,
                    "Permission Required",
                    "Accessibility permission is required for scroll count, blocking and other stats.",
                    android.R.drawable.ic_dialog_alert, statusErrorIcon)
                    .setPermissionColors(statusErrorBg, statusErrorIcon, statusErrorText, statusErrorIcon,
                            statusErrorIcon)
                    .setOnClickListener(v -> showAccessibilityBottomSheet()));
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!isNotificationPermissionGranted()) {
                settingsItems.add(new SettingsItem(ID_PERM_NOTIFICATION, SettingsItem.TYPE_PERMISSION,
                        "Permission Required",
                        "Provide notification permission to show notifications on time",
                        R.drawable.bell, statusInfoIcon)
                        .setPermissionColors(0, statusInfoIcon, textTertiary, textTertiary, textPrimary)
                        .setOnClickListener(v -> showNotificationPermissionBottomSheet()));
            }
        }

        // 3. Alarm (Warning) - title uses text_primary, icon uses warning color
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!isAlarmPermissionGranted()) {
                settingsItems.add(new SettingsItem(ID_PERM_ALARM, SettingsItem.TYPE_PERMISSION,
                        "Alarm Permission",
                        getString(R.string.accurate),
                        R.drawable.alarm, statusWarningIcon)
                        .setPermissionColors(0, statusWarningIcon, textTertiary, textTertiary, textPrimary)
                        .setOnClickListener(v -> askForExactAlarmPermission()));
            }
        }

        // 4. Battery (Warning/Amber) - card root has NO bg tint, icon bg has warning
        // tint
        if (!batteryOptimizationIgnored) {
            settingsItems.add(new SettingsItem(ID_PERM_BATTERY, SettingsItem.TYPE_PERMISSION,
                    "Battery Optimization",
                    "Turn off to ensure app runs in background without interruption.",
                    R.drawable.zap, statusWarningIcon)
                    .setIconValues(0, statusWarningBg) // Icon background gets the warning tint
                    .setPermissionColors(0, statusWarningIcon, textTertiary, textTertiary, textPrimary)
                    .setOnClickListener(v -> showBatteryBottomSheet()));
        }
    }

    private void handleKeepAliveToggle(android.widget.CompoundButton buttonView, boolean isChecked) {
        if (isChecked) {
            // 1. Notifications
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !isNotificationPermissionGranted()) {
                shakeCard(ID_PERM_NOTIFICATION);
                buttonView.setChecked(false);
                defaultSharedPreferences.edit().putBoolean("keepServiceAlive", false).apply();
                return;
            }

            // 2. Alarms
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !isAlarmPermissionGranted()) {
                shakeCard(ID_PERM_ALARM);
                buttonView.setChecked(false);
                defaultSharedPreferences.edit().putBoolean("keepServiceAlive", false).apply();
                return;
            }

            // 3. Battery
            if (!batteryOptimizationIgnored) {
                shakeCard(ID_PERM_BATTERY);
                buttonView.setChecked(false);
                defaultSharedPreferences.edit().putBoolean("keepServiceAlive", false).apply();
                return;
            }

            // 4. Accessibility (Critical)
            if (!isAccessibilityPermissionGranted(requireContext())) {
                shakeCard(ID_PERM_ACCESSIBILITY);
                buttonView.setChecked(false);
                defaultSharedPreferences.edit().putBoolean("keepServiceAlive", false).apply();
                return;
            }

            // All granted
            defaultSharedPreferences.edit().putBoolean("keepServiceAlive", true).apply();
            AlarmUtils.scheduleAlarm(requireContext());
            // Force immediate update
            requireContext().sendBroadcast(new Intent(AppUsageAccessibilityService.ACTION_UPDATE_KEEP_ALIVE));
        } else {
            defaultSharedPreferences.edit().putBoolean("keepServiceAlive", false).apply();
            AlarmUtils.cancelAlarm(requireContext());
            WarningUtils.remove(requireContext());
        }
    }

    private void shakeCard(int itemId) {
        if (!isAdded())
            return;

        // Message determining logic
        String message = "Permission required";
        if (itemId == ID_PERM_ACCESSIBILITY)
            message = "Accessibility permission needed";
        else if (itemId == ID_PERM_NOTIFICATION)
            message = "Notification permission needed";
        else if (itemId == ID_PERM_ALARM)
            message = "Alarm permission needed";
        else if (itemId == ID_PERM_BATTERY)
            message = "Battery optimization permission needed";

        String finalMessage = message;

        // Try to find current position
        int initialPos = -1;
        for (int i = 0; i < settingsItems.size(); i++) {
            if (settingsItems.get(i).getId() == itemId) {
                initialPos = i;
                break;
            }
        }

        if (initialPos != -1) {
            recyclerView.smoothScrollToPosition(initialPos);
        }

        // Delay to allow scroll and potential refresh to settle
        recyclerView.postDelayed(() -> {
            if (!isAdded())
                return;

            int currentPos = -1;
            for (int i = 0; i < settingsItems.size(); i++) {
                if (settingsItems.get(i).getId() == itemId) {
                    currentPos = i;
                    break;
                }
            }

            if (currentPos != -1) {
                RecyclerView.ViewHolder vh = recyclerView.findViewHolderForAdapterPosition(currentPos);
                if (vh != null) {
                    shakeView(vh.itemView);
                }
            }
            Toast.makeText(requireContext(), finalMessage, Toast.LENGTH_SHORT).show();
        }, 500);
    }

    private void shakeView(View view) {
        if (view == null)
            return;

        view.setScaleX(1f);
        view.setScaleY(1f);
        view.setTranslationX(0f);

        ObjectAnimator scaleUpX = ObjectAnimator.ofFloat(view, "scaleX", 1f, 1.05f);
        ObjectAnimator scaleUpY = ObjectAnimator.ofFloat(view, "scaleY", 1f, 1.05f);
        scaleUpX.setDuration(200);
        scaleUpY.setDuration(200);

        ObjectAnimator shake = ObjectAnimator.ofFloat(
                view,
                "translationX",
                0, 20, -20, 15, -15, 10, -10, 5, -5, 0);
        shake.setDuration(650);

        ObjectAnimator scaleDownX = ObjectAnimator.ofFloat(view, "scaleX", 1.05f,
                1f);
        ObjectAnimator scaleDownY = ObjectAnimator.ofFloat(view, "scaleY", 1.05f,
                1f);
        scaleDownX.setDuration(200);
        scaleDownY.setDuration(200);

        AnimatorSet set = new AnimatorSet();
        set.play(scaleUpX).with(scaleUpY);
        set.play(shake).after(scaleUpX);
        set.play(scaleDownX).with(scaleDownY).after(shake);
        set.start();

        // Haptic
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            android.os.Vibrator v = (android.os.Vibrator) requireContext().getSystemService(android.content.Context.VIBRATOR_SERVICE);
            if (v != null)
                v.vibrate(android.os.VibrationEffect.createOneShot(40, android.os.VibrationEffect.EFFECT_HEAVY_CLICK));
        }
    }


    private CharSequence getRemindDoomFormattedSubtitle() {
        int minutes = defaultSharedPreferences.getInt(AppUsageAccessibilityService.PREF_REMIND_DOOM_SCROLLING_MINUTES, AppUsageAccessibilityService.DEFAULT_REMIND_DOOM_SCROLLING_MINUTES);
        String reminderText = "Remind me to stop scroll at every " + minutes + " minutes ";
        SpannableString spannable = new SpannableString(reminderText);
        int start = reminderText.indexOf(String.valueOf(minutes));
        int end = start + String.valueOf(minutes).length() + " minutes".length();
        int cyan = ContextCompat.getColor(requireContext(), R.color.cyan);
        spannable.setSpan(new StyleSpan(Typeface.BOLD), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        spannable.setSpan(new ForegroundColorSpan(cyan), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        spannable.setSpan(new UnderlineSpan(), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        Drawable icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_edit);
        if (icon != null) {
            DrawableCompat.setTint(icon, cyan);
            int size = (int) (13 * getResources().getDisplayMetrics().scaledDensity);
            icon.setBounds(0, 0, size, size);
            ImageSpan imageSpan = new ImageSpan(icon, ImageSpan.ALIGN_BOTTOM);
            SpannableStringBuilder builder = new SpannableStringBuilder(spannable);
            builder.append(" ");
            builder.setSpan(imageSpan, builder.length() - 1, builder.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            return builder;
        }
        return spannable;
    }

    private CharSequence getBlockTimeFormattedSubtitle() {
        float hours = defaultSharedPreferences.getFloat(PREF_BLOCK_AFTER_WASTED_TIME_HOURS, DEFAULT_BLOCK_AFTER_WASTED_TIME_HOURS);
        int wholeHours = (int) hours;
        int minutes = Math.round((hours - wholeHours) * 60);
        String timeText = formatTimeDisplay(wholeHours, minutes);
        String displayText = "Block content after " + timeText + " ";

        SpannableString spannable = new SpannableString(displayText);
        int start = displayText.indexOf(timeText);
        int end = start + timeText.length();
        int cyan = ContextCompat.getColor(requireContext(), R.color.cyan);

        spannable.setSpan(new StyleSpan(Typeface.BOLD), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        spannable.setSpan(new ForegroundColorSpan(cyan), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        spannable.setSpan(new UnderlineSpan(), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        Drawable icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_edit);
        if (icon != null) {
            DrawableCompat.setTint(icon, cyan);
            int size = (int) (13 * getResources().getDisplayMetrics().scaledDensity);
            icon.setBounds(0, 0, size, size);
            ImageSpan imageSpan = new ImageSpan(icon, ImageSpan.ALIGN_BOTTOM);
            SpannableStringBuilder builder = new SpannableStringBuilder(spannable);
            builder.append(" ");
            builder.setSpan(imageSpan, builder.length() - 1, builder.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            return builder;
        }
        return spannable;
    }

    private String formatTimeDisplay(int hours, int minutes) {
        if (hours == 0)
            return minutes + " minutes";
        else if (minutes == 0)
            return hours + " hours";
        else
            return hours + "h " + minutes + "m";
    }

    private void registerForPermission() {
        batteryOptimizationLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    refreshList();
                });
    }

    private void registerAccessibilityLauncher() {
        accessibilityLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    refreshList();
                });
    }

    private void registerNotificationPermissionLauncher() {
        notificationPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        Toast.makeText(requireContext(), "Granted", Toast.LENGTH_SHORT).show();
                    }
                    refreshList();
                });
    }

    private boolean isNotificationPermissionGranted() {
        if (Build.VERSION.SDK_INT < 33)
            return true;
        return ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
    }

    private boolean isAlarmPermissionGranted() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S)
            return true;
        AlarmManager alarmManager = (AlarmManager) requireContext().getSystemService(Context.ALARM_SERVICE);
        return alarmManager != null && alarmManager.canScheduleExactAlarms();
    }

    private void askForExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (isAlarmPermissionGranted()) {
                Toast.makeText(requireContext(), "Permission already granted", Toast.LENGTH_SHORT).show();
                refreshList();
                return;
            }
            Utils.showPermissionSheet(requireContext(), Utils.PermissionType.ALARM, null,
                    () -> Toast.makeText(requireContext(), "Permission required", Toast.LENGTH_SHORT).show());
        }
    }

    private void showBatteryBottomSheet() {
        Utils.showPermissionSheet(requireContext(), Utils.PermissionType.BATTERY,
                new Utils.PermissionLauncher() {
                    @Override
                    public void launchAccessibility(Intent intent) {
                    }

                    @Override
                    public void launchBattery(Intent intent) {
                        batteryOptimizationLauncher.launch(intent);
                    }

                    @Override
                    public void launchNotification(String permission) {
                    }
                },
                () -> Toast.makeText(requireContext(), "Not ignored", Toast.LENGTH_SHORT).show());
    }

    private void showAccessibilityBottomSheet() {
        Utils.showPermissionSheet(requireContext(), Utils.PermissionType.ACCESSIBILITY,
                new Utils.PermissionLauncher() {
                    @Override
                    public void launchAccessibility(Intent intent) {
                        accessibilityLauncher.launch(intent);
                    }

                    @Override
                    public void launchBattery(Intent intent) {
                    }

                    @Override
                    public void launchNotification(String permission) {
                    }
                }, null);
    }

    private void showNotificationPermissionBottomSheet() {
        if (Build.VERSION.SDK_INT >= 33) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
        }
    }

    private void showTimePickerBottomSheet(boolean isRemindDoomScrolling) {
        if (!isAccessibilityPermissionGranted(requireContext())) {
            shakeCard(ID_PERM_ACCESSIBILITY);
            return;
        }
        timerPicker = new BottomSheetDialog(requireContext());
        View bottomSheetView = LayoutInflater.from(requireContext()).inflate(R.layout.bottom_sheet_time, null);

        NumberPicker hourPicker = bottomSheetView.findViewById(R.id.hours_selector_bottom_sheet);
        NumberPicker minutePicker = bottomSheetView.findViewById(R.id.minutes_selector_bottom_sheet);
        Button setLimitBtn = bottomSheetView.findViewById(R.id.setLimitBtnBottomSheet);
        TextView hoursLabel = bottomSheetView.findViewById(R.id.hours_textView_bottom_sheet);

        if (isRemindDoomScrolling) {
            hourPicker.setVisibility(View.GONE);
            hoursLabel.setVisibility(View.GONE);
            minutePicker.setMinValue(1);
            minutePicker.setMaxValue(59);
            int current = defaultSharedPreferences.getInt(
                    AppUsageAccessibilityService.PREF_REMIND_DOOM_SCROLLING_MINUTES,
                    AppUsageAccessibilityService.DEFAULT_REMIND_DOOM_SCROLLING_MINUTES);
            minutePicker.setValue(current);
            setLimitBtn.setText(R.string.set_reminder_time);
        } else {
            hourPicker.setVisibility(View.VISIBLE);
            hoursLabel.setVisibility(View.VISIBLE);
            hourPicker.setMinValue(0);
            hourPicker.setMaxValue(23);
            minutePicker.setMinValue(0);
            minutePicker.setMaxValue(59);
            float current = defaultSharedPreferences.getFloat(PREF_BLOCK_AFTER_WASTED_TIME_HOURS,
                    DEFAULT_BLOCK_AFTER_WASTED_TIME_HOURS);
            int h = (int) current;
            int m = Math.round((current - h) * 60);
            hourPicker.setValue(h);
            minutePicker.setValue(m);
            setLimitBtn.setText(R.string.set_block_time);
        }

        setLimitBtn.setOnClickListener(v -> {
            if (isRemindDoomScrolling) {
                defaultSharedPreferences.edit().putInt(AppUsageAccessibilityService.PREF_REMIND_DOOM_SCROLLING_MINUTES,
                        minutePicker.getValue()).apply();
                Toast.makeText(requireContext(), "Reminder set", Toast.LENGTH_SHORT).show();
            } else {
                float val = hourPicker.getValue() + (minutePicker.getValue() / 60.0f);
                defaultSharedPreferences.edit().putFloat(PREF_BLOCK_AFTER_WASTED_TIME_HOURS, val).apply();
                Toast.makeText(requireContext(), "Block time set", Toast.LENGTH_SHORT).show();
            }
            timerPicker.dismiss();
            refreshList();
        });

        bottomSheetView.findViewById(R.id.crossBtn).setOnClickListener(v -> timerPicker.dismiss());
        timerPicker.setContentView(bottomSheetView);
        timerPicker.show();
    }

    private void showAdultListDownloadDialogAndEnsure() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        // Inflate custom layout
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_adult_list_progress, null);
        builder.setView(dialogView);
        builder.setCancelable(false);

        AlertDialog progressDialog = builder.create();
        // Set background transparent for the CardView radius to work
        if (progressDialog.getWindow() != null) {
            progressDialog.getWindow().setBackgroundDrawable(new ColorDrawable(0));
        }
        progressDialog.show();

        AdultDomainListManager.downloadAndBuildList(requireContext(),
                new AdultDomainListManager.OnDownloadCompleteListener() {
                    @Override
                    public void onSuccess(long mergedFileBytes, String sha256Hex, boolean deduped) {
                        if (getActivity() == null)
                            return;
                        getActivity().runOnUiThread(() -> {
                            progressDialog.dismiss();
                            Toast.makeText(requireContext(), "List updated successfully", Toast.LENGTH_SHORT).show();
                            defaultSharedPreferences.edit().putBoolean(PREF_BLOCK_ADULT_SITES_ENABLED, true).apply();
                            refreshList();
                        });
                    }

                    @Override
                    public void onError(Exception e) {
                        if (getActivity() == null)
                            return;
                        getActivity().runOnUiThread(() -> {
                            progressDialog.dismiss();
                            Toast.makeText(requireContext(), "Update failed: " + e.getMessage(), Toast.LENGTH_SHORT)
                                    .show();
                            defaultSharedPreferences.edit().putBoolean(PREF_BLOCK_ADULT_SITES_ENABLED, false).apply();
                            refreshList();
                        });
                    }
                }, false);
    }

    private void applyTheme(String theme) {
        String current = defaultSharedPreferences.getString(PREF_THEME_MODE, "Dark Theme");
        if (!theme.equals(current)) {
            defaultSharedPreferences.edit().putString(PREF_THEME_MODE, theme).apply();
            String existingStartFragment = defaultSharedPreferences.getString(HomeActivity.PREF_START_FRAGMENT, null);
            if (!HomeActivity.START_FRAGMENT_SETTINGS.equals(existingStartFragment)) {
                defaultSharedPreferences.edit()
                        .putString(HomeActivity.PREF_START_FRAGMENT, HomeActivity.START_FRAGMENT_SETTINGS).apply();
            }
            Utils.applyAppThemeFromPrefs(requireContext());
            View root = getView().findViewById(R.id.headerContainer);
            Runnable doRecreate = () -> {
                if (isAdded())
                    requireActivity().recreate();
            };
            if (root != null)
                root.animate().alpha(0f).setDuration(180).withEndAction(doRecreate).start();
            else
                doRecreate.run();
        }
    }

    // ─── Settings Lock PIN helpers ────────────────────────────────────────────

    /**
     * Authenticate via device lock or custom PIN (depending on current setting)
     * before allowing a sensitive change. Calls onAuthenticated when verified.
     */
    private void authenticateToChangeSetting(String reason, Runnable onAuthenticated, Runnable onCancelled) {
        SettingsLockManager lm = new SettingsLockManager(requireContext());
        lm.authenticate((AppCompatActivity) requireActivity(), reason, new SettingsLockManager.AuthCallback() {
            @Override
            public void onSuccess() {
                onAuthenticated.run();
            }

            @Override
            public void onFailure(String reason2) {
                if (onCancelled != null) onCancelled.run();
                if (!"Cancelled".equals(reason2)) {
                    Toast.makeText(requireContext(), reason2, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void authenticateToChangeSetting(String reason, Runnable onAuthenticated) {
        authenticateToChangeSetting(reason, onAuthenticated, null);
    }

    private void lockedSwitchAction(String reason, android.widget.CompoundButton buttonView, boolean originalState, boolean isChecked, Runnable onVerifiedAndChanged) {
        SettingsLockManager lm = new SettingsLockManager(requireContext());

        // Lock is only required when TURNING OFF (disabling a feature)
        if (!lm.isLockEnabled() || isChecked) {
            onVerifiedAndChanged.run();
            refreshList();
            return;
        }

        // Revert switch visually first (since auth is async)
        buttonView.setOnCheckedChangeListener(null);
        buttonView.setChecked(originalState);

        authenticateToChangeSetting(reason, () -> {
            buttonView.setChecked(isChecked);
            onVerifiedAndChanged.run();
            refreshList(); // Restore listeners by refreshing list
        }, this::refreshList); // If cancelled or failed, refresh list to rebind the switch!

        // Temporarily assign a no-op listener until refreshList triggers
        buttonView.setOnCheckedChangeListener((v, c) -> {
        });
    }
}