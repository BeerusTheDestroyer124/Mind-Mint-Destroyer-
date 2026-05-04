package com.gxdevs.mindmint.Fragments;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Pair;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.airbnb.lottie.LottieAnimationView;
import com.airbnb.lottie.LottieProperty;
import com.airbnb.lottie.model.KeyPath;
import com.airbnb.lottie.value.LottieValueCallback;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.gxdevs.mindmint.Activities.HabitCreateActivity;
import com.gxdevs.mindmint.Activities.HabitStatActivity;
import com.gxdevs.mindmint.Adapters.HabitAdapter;
import com.gxdevs.mindmint.Models.Habit;
import com.gxdevs.mindmint.Models.Task;
import com.gxdevs.mindmint.R;
import com.gxdevs.mindmint.Utils.HabitManager;
import com.gxdevs.mindmint.Utils.AnimUtils;
import com.gxdevs.mindmint.Utils.MintCrystals;
import com.gxdevs.mindmint.Utils.StreakManager;
import com.gxdevs.mindmint.Utils.TaskManager;
import com.gxdevs.mindmint.Widgets.HabitListWidgetProvider;
import com.skydoves.balloon.ArrowOrientation;
import com.skydoves.balloon.Balloon;
import com.skydoves.balloon.BalloonAnimation;
import com.skydoves.balloon.BalloonSizeSpec;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import antonkozyriatskyi.circularprogressindicator.CircularProgressIndicator;

public class HabitFragment extends Fragment implements HabitAdapter.OnHabitActionListener {
    View view;
    private static final String KEY_HABIT_TUTORIAL_SHOWN = "habit_tutorial_shown";
    private static final String KEY_HABIT_INTRO_SHOWN = "habit_intro_shown";
    private HabitAdapter adapter;
    private HabitManager habitManager;
    private TaskManager taskManager;
    private StreakManager streakManager;
    private MintCrystals mintCrystals;
    private List<Habit> habits = new ArrayList<>();
    private RecyclerView recyclerView;
    private EditText searchEditText;
    private TextView dateDetails;
    private TextView streakText;
    private TextView streakRunText;
    private CircularProgressIndicator circularProgress;
    private LottieAnimationView streakLottie;
    private View streakGlow;
    /** Guards entrance animation — only plays on first tab visit. */
    private boolean firstResume = true;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_habit, container, false);
        habitManager = new HabitManager(requireContext());
        taskManager = new TaskManager(requireContext());
        streakManager = new StreakManager(requireContext());
        mintCrystals = new MintCrystals(requireContext());
        recyclerView = view.findViewById(R.id.habitsRecyclerView);
        recyclerView.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        adapter = new HabitAdapter(requireContext(), habits, this);
        recyclerView.setAdapter(adapter);
        View addHabitBtn = view.findViewById(R.id.addHabitBtn);
        AnimUtils.attachTouchRipple(addHabitBtn);
        addHabitBtn.setOnClickListener(v -> startActivity(new Intent(requireContext(), HabitCreateActivity.class)));
        searchEditText = view.findViewById(R.id.searchEditText);
        dateDetails = view.findViewById(R.id.dateDetails);
        streakText = view.findViewById(R.id.streakText);
        streakRunText = view.findViewById(R.id.streakRunText);
        circularProgress = view.findViewById(R.id.circularProgress);
        streakLottie = view.findViewById(R.id.streakLottie);
        streakGlow = view.findViewById(R.id.streakGlow);

        if (circularProgress != null) {
            circularProgress.setMaxProgress(100); // We will use percentage or raw count
        }

        setupSearch();
        load();
        setupLottieColors();
        preHideAnimatedViews(); // Prevent flash: views are invisible until animateEntrance() in onResume fires
        // Post to ensure view is ready
        view.post(this::updateOverallStreakUI);
        updateDateHeader();
        return view;
    }

    /** Pre-hides views that animateEntrance() animates in, so there is no flash before onResume fires. */
    private void preHideAnimatedViews() {
        View streakCard = view.findViewById(R.id.streakCard);
        if (streakCard != null) { streakCard.setAlpha(0f); streakCard.setTranslationY(90f); }
        if (searchEditText != null) searchEditText.setAlpha(0f);
        if (dateDetails != null) dateDetails.setAlpha(0f);
        View addBtn = view.findViewById(R.id.addHabitBtn);
        if (addBtn != null) { addBtn.setAlpha(0f); addBtn.setScaleX(0.6f); addBtn.setScaleY(0.6f); }
        if (recyclerView != null) recyclerView.setAlpha(0f);
    }

    /** Entrance animations for the Habits screen — runs from onResume so it is visible. */
    private void animateEntrance() {
        // Streak card slides up prominently
        View streakCard = view.findViewById(R.id.streakCard);
        if (streakCard != null) AnimUtils.enterSlideUp(streakCard, 0);

        // Search and date fade in
        if (searchEditText != null) AnimUtils.fadeIn(searchEditText, 60, 260);
        if (dateDetails != null) AnimUtils.fadeIn(dateDetails, 40, 240);

        // Add button bounces in after streak card is visible
        View addBtn = view.findViewById(R.id.addHabitBtn);
        if (addBtn != null) AnimUtils.bounceIn(addBtn, 140);

        // Recycler fades in last
        if (recyclerView != null) AnimUtils.fadeIn(recyclerView, 180, 300);
    }

    private void setupSearch() {
        if (searchEditText == null)
            return;
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                applySearchAndFilterCustomChips();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    private void applySearchAndFilterCustomChips() {
        String q = searchEditText != null ? searchEditText.getText().toString().toLowerCase() : "";
        List<Habit> base = new ArrayList<>(habits);
        if (!q.isEmpty()) {
            base = base.stream().filter(h -> (h.getName() != null && h.getName().toLowerCase().contains(q)) || (h.getReason() != null && h.getReason().toLowerCase().contains(q))).collect(Collectors.toList());
        }
        adapter.setData(base);
    }

    private void load() {
        habits = habitManager.loadHabits();
        applySearchAndFilterCustomChips();
        if (view != null) {
            view.post(this::updateOverallStreakUI);
        }
    }

    private void updateOverallStreakUI() {
        if (habitManager == null || streakManager == null || streakText == null || circularProgress == null
                || streakLottie == null)
            return;

        Pair<Integer, Integer> progress = streakManager.getTodayProgress(habits);
        int completed = progress.first;
        int total = progress.second;

        int streakRun = streakManager.getGlobalStreak();
        streakRunText.setText(String.valueOf(streakRun));

        streakText.setText(completed + "/" + total);

        if (total > 0) {
            circularProgress.setMaxProgress(total);
            circularProgress.setCurrentProgress(completed);
        } else {
            circularProgress.setMaxProgress(1);
            circularProgress.setCurrentProgress(0);
        }

        // Lottie Logic: Always visible
        streakLottie.setVisibility(View.VISIBLE);

        if (total > 0 && completed == total) {
            if (!streakLottie.isAnimating()) {
                streakLottie.playAnimation();
                animateEnlargeEffect(streakLottie);
            }
            if (streakGlow != null)
                streakGlow.setVisibility(View.VISIBLE);
        } else {
            // Not Completed: Stop animation
            if (streakLottie.isAnimating()) {
                streakLottie.cancelAnimation();
            }
            streakLottie.setProgress(0f);
            if (streakGlow != null)
                streakGlow.setVisibility(View.INVISIBLE);
        }
    }

    private void animateEnlargeEffect(View view) {
        view.animate()
                .scaleX(2f)
                .scaleY(2f)
                .setDuration(450)
                .withEndAction(() -> {
                    view.animate()
                            .scaleX(1.0f)
                            .scaleY(1.0f)
                            .setDuration(350)
                            .setInterpolator(new android.view.animation.OvershootInterpolator())
                            .start();
                })
                .start();
    }

    private void updateDateHeader() {
        if (dateDetails == null)
            return;
        Date now = new Date();
        SimpleDateFormat fmt = new SimpleDateFormat("EEEE, MMM dd", Locale.getDefault());
        String formatted = fmt.format(now).toUpperCase(Locale.getDefault());
        dateDetails.setText(formatted);
    }

    @Override
    public void onHabitCompletedToday(Habit habit, int position) {
        if (habit.isAskEmotion()) {
            onShowEmotionLog(habit);
        } else {
            completeHabit(habit, position, null);
        }
    }

    @Override
    public void onShowEmotionLog(Habit habit) {
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        View sheet = getLayoutInflater().inflate(R.layout.bottom_sheet_mood, null);
        dialog.setContentView(sheet);

        View.OnClickListener emotionListener = v -> {
            String emotion = "";
            int id = v.getId();
            if (id == R.id.moodTired)
                emotion = "Tired";
            else if (id == R.id.moodMotivated)
                emotion = "Motivated";
            else if (id == R.id.moodRelieved)
                emotion = "Relieved";
            else if (id == R.id.moodSatisfied)
                emotion = "Satisfied";
            else if (id == R.id.moodProud)
                emotion = "Proud";
            else if (id == R.id.moodNeutral)
                emotion = "Neutral";

            completeHabit(habit, -1, emotion);
            dialog.dismiss();
        };

        sheet.findViewById(R.id.moodTired).setOnClickListener(emotionListener);
        sheet.findViewById(R.id.moodMotivated).setOnClickListener(emotionListener);
        sheet.findViewById(R.id.moodRelieved).setOnClickListener(emotionListener);
        sheet.findViewById(R.id.moodSatisfied).setOnClickListener(emotionListener);
        sheet.findViewById(R.id.moodProud).setOnClickListener(emotionListener);
        sheet.findViewById(R.id.moodNeutral).setOnClickListener(emotionListener);

        sheet.findViewById(R.id.btnSkip).setOnClickListener(v -> {
            completeHabit(habit, -1, null);
            dialog.dismiss();
        });

        sheet.findViewById(R.id.crossBtn).setOnClickListener(v -> {
            dialog.dismiss();
        });

        dialog.show();
    }

    @Override
    public void onEditHabit(Habit habit) {
        startActivity(new Intent(requireContext(), HabitCreateActivity.class)
                .putExtra(HabitCreateActivity.EXTRA_HABIT_ID, habit.getId()));
    }

    @Override
    public void onDeleteHabit(Habit habit) {
        new android.app.AlertDialog.Builder(requireContext())
                .setTitle("Delete Habit")
                .setMessage("Are you sure you want to delete this habit?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    habitManager.deleteHabit(habit.getId());
                    load();
                    updateWidgets();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void completeHabit(Habit habit, int position, String emotion) {
        habitManager.markHabit(habit, emotion);
        mintCrystals.addCoins(5);

        TaskManager tm = taskManager != null ? taskManager : new TaskManager(requireContext());
        List<Task> all = tm.loadTasks();
        for (Task t : all) {
            if (habit.getId().equals(t.getHabitId())) {
                t.setCompleted(true);
            }
        }
        tm.saveTasks(all);
        if (position >= 0)
            adapter.notifyItemChanged(position);
        else
            adapter.notifyDataSetChanged();

        streakManager.updateStreakOnHabitCompletion();
        updateOverallStreakUI();
        updateWidgets();
    }

    @Override
    public void onHabitClicked(Habit habit, int position) {
        startActivity(new Intent(requireContext(), HabitCreateActivity.class).putExtra(HabitCreateActivity.EXTRA_HABIT_ID, habit.getId()));
    }

    @Override
    public void onHabitLongPressed(Habit habit, int position) {
        startActivity(new Intent(requireContext(), HabitStatActivity.class).putExtra(HabitStatActivity.EXTRA_HABIT_ID, habit.getId()));
    }

    @Override
    public void onHabitUncompletedToday(Habit habit, int position) {
        habitManager.unmarkHabit(habit);

        // Subtract 5 coins for un-completing a habit
        mintCrystals.subtractCoins(5);

        // Unmark all related tasks as not completed
        TaskManager tm = taskManager != null ? taskManager : new TaskManager(requireContext());
        List<Task> all = tm.loadTasks();
        for (Task t : all) {
            if (habit.getId().equals(t.getHabitId())) {
                t.setCompleted(false);
            }
        }
        tm.saveTasks(all);
        adapter.notifyItemChanged(position);

        // Check if any habits are still completed today and reset streak if needed
        streakManager.checkAndResetStreakIfNeeded(habits);
        updateOverallStreakUI();
        updateWidgets();
    }

    @Override
    public void onStart() {
        super.onStart();
        load();
    }

    @Override
    public void onHabitProgressUpdated(Habit habit) {
        streakManager.updateStreakOnHabitCompletion(); // Recalc just in case
        updateOverallStreakUI();
        updateWidgets();
    }

    private void setupLottieColors() {
        if (streakLottie == null)
            return;

        // Use brand_pink as base brand color since brand_color doesn't exist
        int brandColor = getThemeColor(R.attr.brand_pink);
        int darkerColor = manipulateColor(brandColor, 0.7f);
        int lighterColor = manipulateColor(brandColor, 1.3f);

        streakLottie.addValueCallback(
                new KeyPath("**"),
                LottieProperty.COLOR_FILTER,
                new LottieValueCallback<>(new PorterDuffColorFilter(lighterColor, PorterDuff.Mode.SRC_ATOP)));

        // 2. Override "flame fill": The main body of the flame. Use Base Color.
        streakLottie.addValueCallback(
                new KeyPath("flame fill", "**"),
                LottieProperty.COLOR_FILTER,
                new LottieValueCallback<>(new PorterDuffColorFilter(brandColor, PorterDuff.Mode.SRC_ATOP)));

        // 3. Override "flame outline": The border or detail. Use Darker Color for
        // contrast.
        streakLottie.addValueCallback(
                new KeyPath("flame outline", "**"),
                LottieProperty.COLOR_FILTER,
                new LottieValueCallback<>(new PorterDuffColorFilter(darkerColor, PorterDuff.Mode.SRC_ATOP)));
    }

    private int getThemeColor(int attr) {
        TypedValue typedValue = new TypedValue();
        if (getContext() != null && getContext().getTheme().resolveAttribute(attr, typedValue, true)) {
            return typedValue.data;
        }
        return Color.parseColor("#FF6B6B"); // Fallback to default brand pink
    }

    private int manipulateColor(int color, float factor) {
        int a = Color.alpha(color);
        int r = Math.round(Color.red(color) * factor);
        int g = Math.round(Color.green(color) * factor);
        int b = Math.round(Color.blue(color) * factor);
        return Color.argb(a,
                Math.min(r, 255),
                Math.min(g, 255),
                Math.min(b, 255));
    }

    /** Returns true only when this fragment is the page currently shown by the host ViewPager2. */
    private boolean isCurrentPage(int expectedPageIndex) {
        if (getActivity() instanceof com.gxdevs.mindmint.Activities.HomeActivity) {
            androidx.viewpager2.widget.ViewPager2 vp =
                    getActivity().findViewById(com.gxdevs.mindmint.R.id.nav_host_container);
            if (vp != null) return vp.getCurrentItem() == expectedPageIndex;
        }
        return true;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Run entrance animation only the first time this tab is actually VISIBLE.
        // ViewPager2 with offscreenPageLimit calls onResume for ALL pre-loaded fragments
        // when the Activity resumes — guard against that.
        if (firstResume && isCurrentPage(com.gxdevs.mindmint.Adapters.HomePagerAdapter.PAGE_HABITS)) {
            firstResume = false;
            if (view != null) view.post(this::animateEntrance);
        }
        load();
        updateDateHeader();
        updateOverallStreakUI();
        updateOverallStreakUI();
        updateWidgets();
        checkAndShowHabitTutorial();
        checkAndShowHabitIntroSheet();
    }

    private void checkAndShowHabitIntroSheet() {
        // Only show if at least one habit exists
        if (habits.isEmpty())
            return;

        SharedPreferences prefs = requireContext().getSharedPreferences("AppData", android.content.Context.MODE_PRIVATE);
        if (!prefs.getBoolean(KEY_HABIT_INTRO_SHOWN, false)) {
            BottomSheetDialog introSheet = new BottomSheetDialog(requireContext(), R.style.CustomBottomSheetTheme);
            View bottomSheetView = LayoutInflater.from(requireContext()).inflate(R.layout.bottom_sheet_habit_intro, view.findViewById(R.id.bottomSheetHabitIntroLayout));

            bottomSheetView.findViewById(R.id.crossBtn).setOnClickListener(v -> introSheet.dismiss());

            introSheet.setContentView(bottomSheetView);
            introSheet.setOnDismissListener(dialog -> {
                prefs.edit().putBoolean(KEY_HABIT_INTRO_SHOWN, true).apply();
                showHabitInteractionBalloon();
            });
            introSheet.show();
        }
    }

    private void showHabitInteractionBalloon() {
        if (habits.isEmpty() || recyclerView == null)
            return;

        // Post to ensure RecyclerView has laid out children and sheet is closed
        recyclerView.postDelayed(() -> {
            recyclerView.smoothScrollToPosition(0);

            recyclerView.postDelayed(() -> {
                RecyclerView.ViewHolder vh = recyclerView.findViewHolderForAdapterPosition(0);
                View target = (vh != null) ? vh.itemView : recyclerView.getChildAt(0);

                if (target != null) {
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
                            .setText("Tap for stats and long tap to quick edit")
                            .setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
                            .setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.brainColor))
                            .setBalloonAnimation(BalloonAnimation.ELASTIC)
                            .setDismissWhenClicked(true)
                            .setLifecycleOwner(getViewLifecycleOwner())
                            .build();

                    balloon.showAlignTop(target);
                }
            }, 300);
        }, 300);
    }

    private void checkAndShowHabitTutorial() {
        // Only show strictly if NO habits exist yet
        if (!habits.isEmpty())
            return;

        SharedPreferences prefs = requireContext().getSharedPreferences("AppData", android.content.Context.MODE_PRIVATE);
        if (!prefs.getBoolean(KEY_HABIT_TUTORIAL_SHOWN, false)) {
            View addBtn = view.findViewById(R.id.addHabitBtn);
            if (addBtn == null)
                return;

            Balloon balloon = new Balloon.Builder(requireContext())
                    .setArrowSize(10)
                    .setArrowOrientation(ArrowOrientation.BOTTOM)
                    .setArrowPosition(0.75f)
                    .setWidthRatio(0.6f)
                    .setHeight(BalloonSizeSpec.WRAP)
                    .setTextSize(14f)
                    .setCornerRadius(10f)
                    .setAlpha(0.9f)
                    .setPadding(8)
                    .setText("Create your first habit!")
                    .setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
                    .setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.brainColor))
                    .setBalloonAnimation(BalloonAnimation.ELASTIC)
                    .setDismissWhenClicked(true)
                    .setLifecycleOwner(getViewLifecycleOwner())
                    .setOnBalloonDismissListener(() -> {
                        prefs.edit().putBoolean(KEY_HABIT_TUTORIAL_SHOWN, true).apply();
                    })
                    .build();

            addBtn.post(() -> balloon.showAlignTop(addBtn));
        }
    }

    private void updateWidgets() {
        AppWidgetManager man = AppWidgetManager.getInstance(requireContext());
        int[] listIds = man.getAppWidgetIds(new ComponentName(requireContext(), HabitListWidgetProvider.class));
        if (listIds.length > 0) {
            man.notifyAppWidgetViewDataChanged(listIds, R.id.widget_habit_list_view);
        }
    }
}
