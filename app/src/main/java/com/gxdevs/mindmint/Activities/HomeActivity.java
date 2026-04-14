package com.gxdevs.mindmint.Activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
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
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.play.core.review.ReviewManager;
import com.google.android.play.core.review.ReviewManagerFactory;
import com.gxdevs.mindmint.Adapters.HomePagerAdapter;
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

    public static final String EXTRA_START_FRAGMENT  = "extra_start_fragment";
    public static final String START_FRAGMENT_HOME     = "home";
    public static final String START_FRAGMENT_TASKS    = "tasks";
    public static final String START_FRAGMENT_HABITS   = "habits";
    public static final String START_FRAGMENT_SETTINGS = "settings";
    public static final String PREF_START_FRAGMENT     = "pref_start_fragment";

    private static final String PREF_REVIEW    = "in_app_review";
    private static final String KEY_COUNT      = "launch_count";
    private static final String KEY_REVIEW_DONE = "review_done";

    private ViewPager2 viewPager;
    private HomePagerAdapter pagerAdapter;
    private ImageButton btnHome;

    private final List<FrameLayout> navItems = new ArrayList<>();
    private final List<ImageView>   navIcons = new ArrayList<>();

    private final ViewPager2.OnPageChangeCallback pageChangeCallback =
            new ViewPager2.OnPageChangeCallback() {
                @Override
                public void onPageSelected(int position) {
                    syncNavToPage(position);
                }
            };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        Utils.setPad(findViewById(R.id.main), "bottom", this);

        findViews();
        setupAdapter();
        setupNavigation();
        setupBackPressHandler();

        if (savedInstanceState == null) {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
            String startFromIntent = getIntent().getStringExtra(EXTRA_START_FRAGMENT);
            String startFromPrefs  = prefs.getString(PREF_START_FRAGMENT, null);
            if (startFromPrefs != null) {
                prefs.edit().remove(PREF_START_FRAGMENT).apply();
            }
            String start = startFromIntent != null ? startFromIntent : startFromPrefs;

            boolean shouldOpenAddTask = getIntent().getBooleanExtra("open_add_task", false);
            if (shouldOpenAddTask) start = START_FRAGMENT_TASKS;

            if (start == null) start = START_FRAGMENT_HOME;

            viewPager.setCurrentItem(pageIndexFor(start), false);

            if (shouldOpenAddTask) {
                viewPager.post(() -> {
                    Fragment f = getSupportFragmentManager()
                            .findFragmentByTag("f" + HomePagerAdapter.PAGE_TASKS);
                    if (f instanceof TasksFragment) {
                        ((TasksFragment) f).showAddTaskBottomSheet();
                    }
                });
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        MidnightResetManager.checkAndPerformReset(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        viewPager.unregisterOnPageChangeCallback(pageChangeCallback);
    }

    private void findViews() {
        viewPager = findViewById(R.id.nav_host_container);
        btnHome   = findViewById(R.id.btn_home);

        navItems.add(findViewById(R.id.nav_unlink));
        navItems.add(findViewById(R.id.nav_task));
        navItems.add(findViewById(R.id.nav_habits));
        navItems.add(findViewById(R.id.nav_setting));

        navIcons.add(findViewById(R.id.quitIcon));
        navIcons.add(findViewById(R.id.taskIcon));
        navIcons.add(findViewById(R.id.habitIcon));
        navIcons.add(findViewById(R.id.settingsIcon));
    }

    private void setupAdapter() {
        pagerAdapter = new HomePagerAdapter(this);
        viewPager.setAdapter(pagerAdapter);

        viewPager.setOffscreenPageLimit(3);

        viewPager.setOverScrollMode(ViewPager2.OVER_SCROLL_NEVER);

        viewPager.registerOnPageChangeCallback(pageChangeCallback);
    }

    private void setupNavigation() {
        navItems.get(0).setOnClickListener(v ->   // "Friends" — coming soon
                Toast.makeText(this, "Your friends are coming soon!", Toast.LENGTH_SHORT).show());

        navItems.get(1).setOnClickListener(v ->   // Tasks
                viewPager.setCurrentItem(HomePagerAdapter.PAGE_TASKS, true));

        navItems.get(2).setOnClickListener(v ->   // Habits
                viewPager.setCurrentItem(HomePagerAdapter.PAGE_HABITS, true));

        navItems.get(3).setOnClickListener(v ->   // Settings
                viewPager.setCurrentItem(HomePagerAdapter.PAGE_SETTINGS, true));

        btnHome.setOnClickListener(v -> {
            btnHome.animate()
                    .scaleX(0.9f).scaleY(0.9f).setDuration(100)
                    .withEndAction(() -> btnHome.animate().scaleX(1f).scaleY(1f).start());
            viewPager.setCurrentItem(HomePagerAdapter.PAGE_HOME, true);
        });

        btnHome.setOnLongClickListener(v -> {
            startActivity(new Intent(this, OnBoarding.class));
            return true;
        });

        viewPager.post(() -> syncNavToPage(viewPager.getCurrentItem()));
    }

    private void setupBackPressHandler() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (viewPager.getCurrentItem() == HomePagerAdapter.PAGE_HOME) {
                    finish();
                } else {
                    viewPager.setCurrentItem(HomePagerAdapter.PAGE_HOME, true);
                }
            }
        });
    }

    public void navigateTo(int page) {
        viewPager.setCurrentItem(page, true);
    }

    public void updateNavigationForFragment(Fragment fragment) {
        syncNavToPage(pageForFragment(fragment));
    }
    private void syncNavToPage(int page) {
        resetNav();
        int navIdx = navIndexForPage(page);
        if (navIdx >= 0 && navIdx < navItems.size()) {
            navItems.get(navIdx).setSelected(true);
            navIcons.get(navIdx).setColorFilter(
                    ContextCompat.getColor(this, R.color.icon_selected));
        }
    }

    private int navIndexForPage(int page) {
        switch (page) {
            case HomePagerAdapter.PAGE_TASKS:    return 1;
            case HomePagerAdapter.PAGE_HABITS:   return 2;
            case HomePagerAdapter.PAGE_SETTINGS: return 3;
            default:                             return -1; // Home — no nav item
        }
    }

    private int pageForFragment(Fragment f) {
        if (f instanceof TasksFragment)    return HomePagerAdapter.PAGE_TASKS;
        if (f instanceof HabitFragment)    return HomePagerAdapter.PAGE_HABITS;
        if (f instanceof SettingsFragment) return HomePagerAdapter.PAGE_SETTINGS;
        return HomePagerAdapter.PAGE_HOME;
    }

    private int pageIndexFor(String startKey) {
        switch (startKey) {
            case START_FRAGMENT_TASKS:    return HomePagerAdapter.PAGE_TASKS;
            case START_FRAGMENT_HABITS:   return HomePagerAdapter.PAGE_HABITS;
            case START_FRAGMENT_SETTINGS: return HomePagerAdapter.PAGE_SETTINGS;
            default:                      return HomePagerAdapter.PAGE_HOME;
        }
    }

    private void resetNav() {
        for (FrameLayout item : navItems) item.setSelected(false);
        for (ImageView icon : navIcons)
            icon.setColorFilter(ContextCompat.getColor(this, R.color.icon_unselected));
    }

    public void maybeAskForReview() {
        SharedPreferences prefs = getSharedPreferences(PREF_REVIEW, MODE_PRIVATE);
        if (prefs.getBoolean(KEY_REVIEW_DONE, false)) return;
        int count = prefs.getInt(KEY_COUNT, 0) + 1;
        prefs.edit().putInt(KEY_COUNT, count).apply();
        if (count != 5) return;
        ReviewManager manager = ReviewManagerFactory.create(this);
        manager.requestReviewFlow().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                manager.launchReviewFlow(this, task.getResult())
                        .addOnCompleteListener(t ->
                                prefs.edit().putBoolean(KEY_REVIEW_DONE, true).apply());
            }
        });
    }
}
