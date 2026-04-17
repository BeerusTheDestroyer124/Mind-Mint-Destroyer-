package com.gxdevs.mindmint.Adapters;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.gxdevs.mindmint.Fragments.HabitFragment;
import com.gxdevs.mindmint.Fragments.HomeFragment;
import com.gxdevs.mindmint.Fragments.SettingsFragment;
import com.gxdevs.mindmint.Fragments.TasksFragment;

/**
 * ViewPager2 adapter for the main bottom-nav tabs.
 *
 * Page order (intentionally):
 *   0 → Tasks
 *   1 → Home       ← default start page
 *   2 → Habits
 *   3 → Settings
 *
 * This matches the visual layout of the bottom bar so swiping feels natural.
 */
public class HomePagerAdapter extends FragmentStateAdapter {

    public static final int PAGE_TASKS    = 0;
    public static final int PAGE_HOME     = 1;
    public static final int PAGE_HABITS   = 2;
    public static final int PAGE_SETTINGS = 3;
    public static final int PAGE_COUNT    = 4;

    public HomePagerAdapter(@NonNull FragmentActivity activity) {
        super(activity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case PAGE_TASKS:    return new TasksFragment();
            case PAGE_HABITS:   return new HabitFragment();
            case PAGE_SETTINGS: return new SettingsFragment();
            case PAGE_HOME:
            default:            return new HomeFragment();
        }
    }

    @Override
    public int getItemCount() {
        return PAGE_COUNT;
    }
}
