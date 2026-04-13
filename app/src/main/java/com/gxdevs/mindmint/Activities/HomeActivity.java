package com.gxdevs.mindmint.Activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;

import com.google.android.play.core.review.ReviewManager;
import com.google.android.play.core.review.ReviewManagerFactory;
import com.gxdevs.mindmint.Fragments.HabitFragment;
import com.gxdevs.mindmint.Fragments.HomeFragment;
import com.gxdevs.mindmint.Fragments.SettingsFragment;
import com.gxdevs.mindmint.Fragments.TasksFragment;
import com.gxdevs.mindmint.R;
import com.gxdevs.mindmint.Utils.MidnightResetManager;
import com.gxdevs.mindmint.Utils.Utils;

import java.util.ArrayList;
import java.util.List;

public class HomeActivity extends AppCompatActivity {
    private final List<FrameLayout> navItems = new ArrayList<>();
    private final List<ImageView> navIcons = new ArrayList<>();
    public static final String EXTRA_START_FRAGMENT = "extra_start_fragment";
    public static final String START_FRAGMENT_HOME = "home";
    public static final String START_FRAGMENT_TASKS = "tasks";
    public static final String START_FRAGMENT_HABITS = "habits";
    public static final String START_FRAGMENT_SETTINGS = "settings";
    public static final String PREF_START_FRAGMENT = "pref_start_fragment";
    private static final String PREF_REVIEW = "in_app_review";
    private static final String KEY_COUNT = "launch_count";
    private static final String KEY_REVIEW_DONE = "review_done";

    // Swipe navigation — ordered list of tab indices matching navItems
    // 0 = Home (center btn), 1 = Tasks, 2 = Habits, 3 = Settings
    // We exclude index 0 (Friends/unlink — coming soon) from the swipe cycle.
    private static final int[] SWIPE_TAB_ORDER = {-1, 1, 2, 3}; // -1 = Home
    private GestureDetector swipeGestureDetector;
    private long lastSwipeTime = 0;
    // Minimum horizontal distance (px) for a swipe to be recognised
    private static final int SWIPE_MIN_DISTANCE_PX = 120;
    // Minimum horizontal velocity (px/s) required
    private static final int SWIPE_MIN_VELOCITY = 400;
    // Max ratio of vertical/horizontal motion to be treated as a horizontal swipe
    private static final float SWIPE_MAX_VERT_RATIO = 0.6f;
    // Debounce time between consecutive swipe navigations (ms)
    private static final long SWIPE_DEBOUNCE_MS = 400;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        Utils.setPad(findViewById(R.id.main), "bottom", this);

        setupNavigation();
        setupSwipeGesture();
        setupBackPressHandler();

        // Check if opened from widget to add task
        boolean shouldOpenAddTask = getIntent().getBooleanExtra("open_add_task", false);

        if (savedInstanceState == null) {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
            String startFromIntent = getIntent() != null ? getIntent().getStringExtra(EXTRA_START_FRAGMENT) : null;
            String startFromPrefs = prefs.getString(PREF_START_FRAGMENT, null);

            // Consume the pref so it's only used once
            if (startFromPrefs != null) {
                prefs.edit().remove(PREF_START_FRAGMENT).apply();
            }

            String start = startFromIntent != null ? startFromIntent : startFromPrefs;

            if (shouldOpenAddTask) {
                start = START_FRAGMENT_TASKS;
            }

            if (start == null)
                start = START_FRAGMENT_HOME;

            openStartFragment(start);

            // Trigger add task after fragment is loaded
            if (shouldOpenAddTask) {
                findViewById(R.id.nav_host_container).postDelayed(() -> {
                    Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.nav_host_container);
                    if (currentFragment instanceof TasksFragment) {
                        ((TasksFragment) currentFragment).showAddTaskBottomSheet();
                    }
                }, 300);
            }
        } else {
            // On configuration change, update navigation icons based on current fragment
            findViewById(R.id.nav_host_container).post(this::updateNavigationForCurrentFragment);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        MidnightResetManager.checkAndPerformReset(this);
    }

    private void setupBackPressHandler() {
        OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.nav_host_container);
                if (currentFragment instanceof HomeFragment) {
                    // If on home fragment, exit app
                    finish();
                } else {
                    // Navigate to home fragment
                    resetNav();
                    loadFragment(new HomeFragment());
                }
            }
        };
        getOnBackPressedDispatcher().addCallback(this, callback);
    }

    private void updateNavigationForCurrentFragment() {
        Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.nav_host_container);
        if (currentFragment == null)
            return;

        resetNav();
        if (currentFragment instanceof TasksFragment) {
            if (navItems.size() > 1 && navIcons.size() > 1) {
                navItems.get(1).setSelected(true);
                navIcons.get(1).setColorFilter(ContextCompat.getColor(this, R.color.icon_selected));
            }
        } else if (currentFragment instanceof HabitFragment) {
            if (navItems.size() > 2 && navIcons.size() > 2) {
                navItems.get(2).setSelected(true);
                navIcons.get(2).setColorFilter(ContextCompat.getColor(this, R.color.icon_selected));
            }
        } else if (currentFragment instanceof SettingsFragment) {
            if (navItems.size() > 3 && navIcons.size() > 3) {
                navItems.get(3).setSelected(true);
                navIcons.get(3).setColorFilter(ContextCompat.getColor(this, R.color.icon_selected));
            }
        }
    }

    private void setupNavigation() {
        // 1. Find the Container Frames
        FrameLayout item1 = findViewById(R.id.nav_unlink);
        FrameLayout item2 = findViewById(R.id.nav_task);
        FrameLayout item3 = findViewById(R.id.nav_habits);
        FrameLayout item4 = findViewById(R.id.nav_setting);
        ImageButton btnHome = findViewById(R.id.btn_home);

        navItems.clear(); // Good practice to clear if called multiple times
        navItems.add(item1);
        navItems.add(item2);
        navItems.add(item3);
        navItems.add(item4);

        // 2. Find the Icons directly by ID (Fixes your Crash)
        navIcons.clear();
        navIcons.add(findViewById(R.id.quitIcon)); // Make sure you added this ID in XML
        navIcons.add(findViewById(R.id.taskIcon)); // Make sure you added this ID in XML
        navIcons.add(findViewById(R.id.habitIcon)); // Make sure you added this ID in XML
        navIcons.add(findViewById(R.id.settingsIcon)); // Make sure you added this ID in XML

        // 3. Create the Click Listener
        View.OnClickListener onNavClick = view -> {
            int id = view.getId();
            if (id == R.id.nav_unlink) {
                Toast.makeText(HomeActivity.this, "Your friends are coming soon!", Toast.LENGTH_SHORT).show();
                return;
            }
            resetNav();
            view.setSelected(true);

            int index = navItems.indexOf(view);
            if (index != -1) {
                navIcons.get(index).setColorFilter(ContextCompat.getColor(HomeActivity.this, R.color.icon_selected));
            }

            Fragment selectedFragment = null;
            if (id == R.id.nav_task) {
                selectedFragment = new TasksFragment();
            } else if (id == R.id.nav_habits) {
                selectedFragment = new HabitFragment();
            } else if (id == R.id.nav_setting) {
                selectedFragment = new SettingsFragment();
            }

            if (selectedFragment != null) {
                loadFragment(selectedFragment);
            }
        };

        for (FrameLayout item : navItems) {
            item.setOnClickListener(onNavClick);
        }

        btnHome.setOnClickListener(v -> {
            resetNav();
            v.animate().scaleX(0.9f).scaleY(0.9f).setDuration(100).withEndAction(() -> v.animate().scaleX(1f).scaleY(1f).start());
            loadFragment(new HomeFragment());
        });

        // Long press home to test OnBoarding system
        btnHome.setOnLongClickListener(v -> {
            startActivity(new Intent(HomeActivity.this, OnBoarding.class));
            return true;
        });
    }

    // ─── Swipe Gesture ────────────────────────────────────────────────────────

    private void setupSwipeGesture() {
        swipeGestureDetector = new GestureDetector(this,
                new GestureDetector.SimpleOnGestureListener() {
                    @Override
                    public boolean onFling(MotionEvent e1, MotionEvent e2,
                                          float velocityX, float velocityY) {
                        if (e1 == null || e2 == null) return false;

                        float dx = e2.getX() - e1.getX();
                        float dy = e2.getY() - e1.getY();
                        float absDx = Math.abs(dx);
                        float absDy = Math.abs(dy);

                        // Must be clearly horizontal: distance threshold + velocity + direction ratio
                        if (absDx < SWIPE_MIN_DISTANCE_PX) return false;
                        if (Math.abs(velocityX) < SWIPE_MIN_VELOCITY) return false;
                        if (absDy / absDx > SWIPE_MAX_VERT_RATIO) return false;

                        // Debounce rapid swipes
                        long now = System.currentTimeMillis();
                        if (now - lastSwipeTime < SWIPE_DEBOUNCE_MS) return false;
                        lastSwipeTime = now;

                        int current = getCurrentTabIndex(); // -1=Home, 1=Tasks, 2=Habits, 3=Settings
                        if (dx < 0) {
                            // Swipe LEFT
                            handleSwipe(current, true);
                        } else {
                            // Swipe RIGHT
                            handleSwipe(current, false);
                        }
                        return false; // never consume — child views keep the event
                    }
                });
        // Allow long-press events so GestureDetector doesn't suppress long-click listeners
        swipeGestureDetector.setIsLongpressEnabled(false);
    }

    /**
     * Returns the logical tab index of the currently shown fragment.
     * -1 = Home, 1 = Tasks, 2 = Habits, 3 = Settings.
     */
    private int getCurrentTabIndex() {
        Fragment f = getSupportFragmentManager().findFragmentById(R.id.nav_host_container);
        if (f instanceof TasksFragment) return 1;
        if (f instanceof HabitFragment) return 2;
        if (f instanceof SettingsFragment) return 3;
        return -1; // HomeFragment or null
    }

    /**
     * Custom directional swipe map:
     *
     *   Screen    Swipe LEFT   Swipe RIGHT
     *   ───────   ──────────   ───────────
     *   Home      → Tasks      → Habits
     *   Tasks     (nothing)    → Home
     *   Habits    → Home       → Settings
     *   Settings  → Habits     (nothing)
     *
     * @param currentIndex  -1=Home, 1=Tasks, 2=Habits, 3=Settings
     * @param swipeLeft     true = finger moved left, false = finger moved right
     */
    private void handleSwipe(int currentIndex, boolean swipeLeft) {
        int target;
        if (currentIndex == -1) {          // Home
            target = swipeLeft ? 1 : 2;    //   LEFT→Tasks, RIGHT→Habits
        } else if (currentIndex == 1) {    // Tasks
            target = swipeLeft ? -99 : -1; //   LEFT→nothing, RIGHT→Home
        } else if (currentIndex == 2) {    // Habits
            target = swipeLeft ? -1 : 3;   //   LEFT→Home, RIGHT→Settings
        } else if (currentIndex == 3) {    // Settings
            target = swipeLeft ? 2 : -99;  //   LEFT→Habits, RIGHT→nothing
        } else {
            return; // unknown state
        }

        if (target == -99) return; // dead-end direction, do nothing

        switch (target) {
            case -1: // Home
                resetNav();
                loadFragmentWithSlide(new HomeFragment(), swipeLeft);
                break;
            case 1:  // Tasks
                resetNav();
                navItems.get(1).setSelected(true);
                navIcons.get(1).setColorFilter(ContextCompat.getColor(this, R.color.icon_selected));
                loadFragmentWithSlide(new TasksFragment(), swipeLeft);
                break;
            case 2:  // Habits
                resetNav();
                navItems.get(2).setSelected(true);
                navIcons.get(2).setColorFilter(ContextCompat.getColor(this, R.color.icon_selected));
                loadFragmentWithSlide(new HabitFragment(), swipeLeft);
                break;
            case 3:  // Settings
                resetNav();
                navItems.get(3).setSelected(true);
                navIcons.get(3).setColorFilter(ContextCompat.getColor(this, R.color.icon_selected));
                loadFragmentWithSlide(new SettingsFragment(), swipeLeft);
                break;
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        // Feed every event into the gesture detector BEFORE children see it.
        // We return false from onFling so no event is ever consumed here —
        // child views (RecyclerViews, buttons, etc.) always receive full touch sequences.
        if (swipeGestureDetector != null) {
            swipeGestureDetector.onTouchEvent(ev);
        }
        return super.dispatchTouchEvent(ev);
    }

    // ─────────────────────────────────────────────────────────────────────────

    private void openStartFragment(String start) {
        Fragment fragment;
        int navIndex = -1; // which bottom‑nav item to highlight, if any

        switch (start) {
            case START_FRAGMENT_TASKS:
                fragment = new TasksFragment();
                navIndex = 1; // nav_task is second in navItems
                break;
            case START_FRAGMENT_HABITS:
                fragment = new HabitFragment();
                navIndex = 2; // nav_habits is third
                break;
            case START_FRAGMENT_SETTINGS:
                fragment = new SettingsFragment();
                navIndex = 3; // nav_setting is fourth
                break;
            case START_FRAGMENT_HOME:
            default:
                fragment = new HomeFragment();
                break;
        }

        // Highlight nav item when applicable
        if (navIndex >= 0 && navIndex < navItems.size() && navIndex < navIcons.size()) {
            resetNav();
            FrameLayout item = navItems.get(navIndex);
            ImageView icon = navIcons.get(navIndex);
            if (item != null)
                item.setSelected(true);
            if (icon != null) {
                icon.setColorFilter(ContextCompat.getColor(this, R.color.icon_selected));
            }
        }

        loadFragment(fragment);
    }

    private void resetNav() {
        for (FrameLayout item : navItems) {
            item.setSelected(false);
        }

        for (ImageView icon : navIcons) {
            icon.setColorFilter(ContextCompat.getColor(this, R.color.icon_unselected));
        }
    }

    /** Instant replacement — used by nav-bar clicks and app startup (no animation). */
    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.nav_host_container, fragment)
                .commit();
        findViewById(R.id.nav_host_container).post(() -> updateNavigationForFragment(fragment));
    }

    /**
     * Animated slide replacement — used by swipe gestures.
     * Swipe LEFT  → new screen slides in from the RIGHT (→ ←)
     * Swipe RIGHT → new screen slides in from the LEFT  (← →)
     */
    private void loadFragmentWithSlide(Fragment fragment, boolean swipeLeft) {
        int enter, exit;
        if (swipeLeft) {
            enter = R.anim.slide_in_right;   // new fragment enters from right
            exit  = R.anim.slide_out_left;   // old fragment exits to left
        } else {
            enter = R.anim.slide_in_left;    // new fragment enters from left
            exit  = R.anim.slide_out_right;  // old fragment exits to right
        }
        getSupportFragmentManager().beginTransaction()
                .setCustomAnimations(enter, exit)
                .replace(R.id.nav_host_container, fragment)
                .commit();
        findViewById(R.id.nav_host_container).post(() -> updateNavigationForFragment(fragment));
    }

    public void updateNavigationForFragment(Fragment fragment) {
        resetNav();
        if (fragment instanceof TasksFragment) {
            if (navItems.size() > 1 && navIcons.size() > 1) {
                navItems.get(1).setSelected(true);
                navIcons.get(1).setColorFilter(ContextCompat.getColor(this, R.color.icon_selected));
            }
        } else if (fragment instanceof HabitFragment) {
            if (navItems.size() > 2 && navIcons.size() > 2) {
                navItems.get(2).setSelected(true);
                navIcons.get(2).setColorFilter(ContextCompat.getColor(this, R.color.icon_selected));
            }
        } else if (fragment instanceof SettingsFragment) {
            if (navItems.size() > 3 && navIcons.size() > 3) {
                navItems.get(3).setSelected(true);
                navIcons.get(3).setColorFilter(ContextCompat.getColor(this, R.color.icon_selected));
            }
        }
    }

    public void maybeAskForReview() {
        SharedPreferences prefs = getSharedPreferences(PREF_REVIEW, MODE_PRIVATE);
        boolean reviewDone = prefs.getBoolean(KEY_REVIEW_DONE, false);
        if (reviewDone)
            return;
        int count = prefs.getInt(KEY_COUNT, 0) + 1;
        prefs.edit().putInt(KEY_COUNT, count).apply();
        if (count != 5)
            return;
        ReviewManager manager = ReviewManagerFactory.create(this);
        manager.requestReviewFlow().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                manager.launchReviewFlow(this, task.getResult()).addOnCompleteListener(t -> prefs.edit().putBoolean(KEY_REVIEW_DONE, true).apply());
            }
        });
    }
}
