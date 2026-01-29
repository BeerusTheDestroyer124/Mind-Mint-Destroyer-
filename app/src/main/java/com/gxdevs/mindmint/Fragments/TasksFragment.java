package com.gxdevs.mindmint.Fragments;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.Manifest;
import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.apachat.swipereveallayout.core.SwipeLayout;
import com.google.android.material.button.MaterialButton;
import com.gxdevs.mindmint.Adapters.TaskAdapter;
import com.gxdevs.mindmint.Models.Habit;
import com.gxdevs.mindmint.Models.Task;
import com.gxdevs.mindmint.R;
import com.gxdevs.mindmint.Utils.HabitManager;
import com.gxdevs.mindmint.Utils.MintCrystals;
import com.gxdevs.mindmint.Utils.StreakManager;
import com.gxdevs.mindmint.Utils.TaskManager;
import com.gxdevs.mindmint.Utils.TaskNotificationManager;
import com.gxdevs.mindmint.Utils.Utils;
import com.skydoves.balloon.ArrowOrientation;
import com.skydoves.balloon.Balloon;
import com.skydoves.balloon.BalloonAnimation;
import com.skydoves.balloon.BalloonSizeSpec;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TasksFragment extends Fragment implements TaskAdapter.OnTaskClickListener, AddTaskBottomSheet.OnTaskActionListener {
    private static final String KEY_TASK_CREATE_TUTORIAL_SHOWN = "task_create_tutorial_shown";
    private static final String KEY_TASK_SWIPE_FOCUS_TUTORIAL_SHOWN = "task_swipe_focus_tutorial_shown";
    private static final String KEY_TASK_SWIPE_LIST_TUTORIAL_SHOWN = "task_swipe_list_tutorial_shown";
    private View view;
    private MaterialButton addTaskButton;
    private EditText searchEditText;
    private ImageView filterButton;
    private RecyclerView tasksRecyclerView;
    private ConstraintLayout emptyStateLayout;
    private TextView emptyStateText;
    private HorizontalScrollView filterScrollView;
    private TaskAdapter taskAdapter;
    private List<Task> taskList;
    private String currentFilter = "All";
    private String currentSearchQuery = "";
    private TaskManager taskManager;
    private TaskNotificationManager notificationManager;
    private HabitManager habitManager;
    private StreakManager streakManager;
    private MintCrystals mintCrystals;
    private LinearLayout filterChipContainer;
    private SwipeLayout focusSwipeLayout;
    private androidx.cardview.widget.CardView currentFocusCard;
    private TextView currentFocusPriorityLabel;
    private TextView currentFocusDueDate;
    private TextView currentFocusTaskName;
    private ConstraintLayout currentFocusMarkButton;
    private Task currentFocusTask;
    private TextView currentFocusLabel;
    private ImageView currentFocusMenu;
    private TextView dateDetails;
    private final ActivityResultLauncher<String> permissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    Toast.makeText(requireContext(), "Notification permission granted", Toast.LENGTH_SHORT).show();
                    updateNotificationPermissionCard();
                } else {
                    Toast.makeText(requireContext(), "Notification permission denied. Task reminders won't work.",
                            Toast.LENGTH_LONG).show();
                }
            });

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_tasks, container, false);

        taskManager = new TaskManager(requireContext());
        habitManager = new HabitManager(requireContext());
        notificationManager = new TaskNotificationManager(requireContext());
        streakManager = new StreakManager(requireContext());
        mintCrystals = new MintCrystals(requireContext());
        initViews();
        setupRecyclerView();
        setupClickListeners();
        updateNotificationPermissionCard();
        loadTasks();
        checkPermissionAndMoveOn();
        setupSearchAndFilter();

        return view;
    }

    private void checkPermissionAndMoveOn() {
        AlarmManager alarmManager = (AlarmManager) requireContext().getSystemService(Context.ALARM_SERVICE);
        ConstraintLayout permissionCard = view.findViewById(R.id.permissionCard);
        if (permissionCard == null)
            return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
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

    private void askForExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Utils.showPermissionSheet(requireContext(), Utils.PermissionType.ALARM,
                    null,
                    () -> Toast.makeText(requireContext(), "Alarm permission is required for task reminders.",
                            Toast.LENGTH_SHORT).show());
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        checkPermissionAndMoveOn();
        updateNotificationPermissionCard();
        view.postDelayed(this::checkAndShowTaskTutorials, 500);
    }

    private void checkAndShowTaskTutorials() {
        if (getContext() == null)
            return;
        SharedPreferences prefs = requireContext().getSharedPreferences("AppData", Context.MODE_PRIVATE);
        boolean createShown = prefs.getBoolean(KEY_TASK_CREATE_TUTORIAL_SHOWN, false);
        boolean focusSwipeShown = prefs.getBoolean(KEY_TASK_SWIPE_FOCUS_TUTORIAL_SHOWN, false);
        boolean listSwipeShown = prefs.getBoolean(KEY_TASK_SWIPE_LIST_TUTORIAL_SHOWN, false);

        // 1. Create Task Tutorial (Only if empty)
        if (!createShown && (taskList == null || taskList.isEmpty())) {
            showTutorialBalloon(addTaskButton, "Add your first task",
                    () -> prefs.edit().putBoolean(KEY_TASK_CREATE_TUTORIAL_SHOWN, true).apply(), 0.85f);
            return;
        }

        // 2. Focus Card Swipe Tutorial (If Focus Card is visible)
        if (!focusSwipeShown && currentFocusCard != null && currentFocusCard.getVisibility() == VISIBLE) {
            showTutorialBalloon(currentFocusCard, "Swipe the card down to quick edit or delete",
                    () -> prefs.edit().putBoolean(KEY_TASK_SWIPE_FOCUS_TUTORIAL_SHOWN, true).apply(), 0.5f);
        }

        // 3. List Swipe Tutorial (If List has items)
        if (!listSwipeShown && taskAdapter != null && taskAdapter.getItemCount() > 0) {
            tasksRecyclerView.postDelayed(() -> {
                RecyclerView.ViewHolder vh = tasksRecyclerView.findViewHolderForAdapterPosition(0);
                if (vh != null) {
                    showTutorialBalloon(vh.itemView, "Swipe task left to quick edit or delete",
                            () -> prefs.edit().putBoolean(KEY_TASK_SWIPE_LIST_TUTORIAL_SHOWN, true).apply(), 0.5f);
                }
            }, 200);
        }
    }

    private void showTutorialBalloon(View target, String text, Runnable onDismiss, float position) {
        if (target == null)
            return;
        Balloon balloon = new Balloon.Builder(requireContext())
                .setArrowSize(10)
                .setArrowOrientation(ArrowOrientation.TOP)
                .setArrowPosition(position)
                .setWidthRatio(0.7f)
                .setHeight(BalloonSizeSpec.WRAP)
                .setTextSize(14f)
                .setCornerRadius(10f)
                .setAlpha(0.9f)
                .setPadding(8)
                .setText(text)
                .setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
                .setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.brainColor))
                .setBalloonAnimation(BalloonAnimation.ELASTIC)
                .setDismissWhenClicked(true)
                .setLifecycleOwner(getViewLifecycleOwner())
                .setOnBalloonDismissListener(onDismiss::run)
                .build();

        balloon.showAlignBottom(target);
    }

    private void initViews() {
        ConstraintLayout focusEditButton = view.findViewById(R.id.editButton);
        ConstraintLayout focusDeleteButton = view.findViewById(R.id.deleteButton);
        addTaskButton = view.findViewById(R.id.addTasksBtn);
        searchEditText = view.findViewById(R.id.searchEditText);
        filterButton = view.findViewById(R.id.filterButton);
        tasksRecyclerView = view.findViewById(R.id.tasksRecyclerView);
        emptyStateLayout = view.findViewById(R.id.emptyStateLayout);
        emptyStateText = view.findViewById(R.id.emptyStateText);
        filterScrollView = view.findViewById(R.id.filterScrollView);
        filterChipContainer = view.findViewById(R.id.filterChipContainer);
        focusSwipeLayout = view.findViewById(R.id.swipeLayout);
        currentFocusCard = view.findViewById(R.id.currentFocusCard);
        currentFocusPriorityLabel = view.findViewById(R.id.currentFocusPriorityLabel);
        currentFocusDueDate = view.findViewById(R.id.currentFocusDueDate);
        currentFocusTaskName = view.findViewById(R.id.currentFocusTaskName);
        currentFocusMarkButton = view.findViewById(R.id.currentFocusMarkButton);
        currentFocusLabel = view.findViewById(R.id.currentFocusLabel);
        currentFocusMenu = view.findViewById(R.id.currentFocusMenu);
        dateDetails = view.findViewById(R.id.dateDetails);

        // Initialize card as hidden
        if (currentFocusCard != null) {
            currentFocusCard.setVisibility(View.GONE);
            currentFocusCard.setAlpha(0f);
        }

        if (currentFocusLabel != null) {
            currentFocusLabel.setVisibility(View.GONE);
            currentFocusLabel.setAlpha(0f);
        }
        if (currentFocusMenu != null) {
            currentFocusMenu.setVisibility(View.GONE);
            currentFocusMenu.setAlpha(0f);
        }

        if (currentFocusMarkButton != null) {
            currentFocusMarkButton.setOnClickListener(v -> {
                if (currentFocusTask != null && !currentFocusTask.isCompleted()) {
                    animateMarkComplete();
                }
            });
        }

        // Setup focus card swipe actions
        if (focusEditButton != null) {
            focusEditButton.setOnClickListener(v -> {
                if (currentFocusTask != null) {
                    showEditTaskBottomSheet(currentFocusTask);
                }
                if (focusSwipeLayout != null) {
                    focusSwipeLayout.close(true);
                }
            });
        }

        if (focusDeleteButton != null) {
            focusDeleteButton.setOnClickListener(v -> {
                if (currentFocusTask != null) {
                    int position = findTaskPosition(currentFocusTask);
                    if (position < 0) {
                        position = 0;
                    }
                    onTaskDelete(currentFocusTask, position);
                }
                if (focusSwipeLayout != null) {
                    focusSwipeLayout.close(true);
                }
            });
        }
    }

    private int findTaskPosition(Task targetTask) {
        for (int i = 0; i < taskList.size(); i++) {
            if (taskList.get(i).getId().equals(targetTask.getId())) {
                return i;
            }
        }
        return -1;
    }

    private void setupRecyclerView() {
        taskList = new ArrayList<>();
        taskAdapter = new TaskAdapter(requireContext(), taskList);
        taskAdapter.setOnTaskClickListener(this);
        tasksRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        tasksRecyclerView.setAdapter(taskAdapter);
    }

    private void setupSearchAndFilter() {
        // Search functionality
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                currentSearchQuery = s.toString();
                filterTasks();
            }
        });

        // Filter chips with blurs
        String[] chipLabels = { "All", "Pending", "Completed", "High Priority", "Medium Priority", "Low Priority" };
        filterChipContainer.removeAllViews();

        final View[] selectedChip = { null };

        for (String label : chipLabels) {
            FrameLayout chipWrapper = new FrameLayout(requireContext());
            TextView chipText = new TextView(requireContext());
            chipText.setText(label);
            chipText.setTextColor(ContextCompat.getColor(requireContext(), R.color.white));
            chipText.setTextSize(14f);
            chipText.setPadding(25, 12, 25, 12);
            chipText.setGravity(Gravity.CENTER);

            GradientDrawable bg = new GradientDrawable();
            bg.setColor(ContextCompat.getColor(requireContext(), R.color.transparent));
            bg.setCornerRadius(50f);

            GradientDrawable bgSelected = new GradientDrawable();
            bgSelected.setColor(ContextCompat.getColor(requireContext(), R.color.sexyGrey));
            bgSelected.setCornerRadius(50f);

            chipText.setBackground(bg);
            chipWrapper.addView(chipText);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            params.setMargins(8, 0, 8, 0);
            chipWrapper.setLayoutParams(params);
            chipWrapper.setOnClickListener(v -> {
                if (selectedChip[0] != null) {
                    TextView previousText = (TextView) ((FrameLayout) selectedChip[0]).getChildAt(0);
                    previousText.setBackground(bg);
                }
                chipText.setBackground(bgSelected);
                selectedChip[0] = chipWrapper;
                currentFilter = label;
                filterTasks();
            });

            filterChipContainer.addView(chipWrapper);
            if (label.equals("All")) {
                chipWrapper.performClick();
            }

        }
    }

    private void setupClickListeners() {
        addTaskButton.setOnClickListener(v -> showAddTaskBottomSheet());
        filterButton.setOnClickListener(v -> {
            if (filterScrollView.getVisibility() == VISIBLE) {
                filterScrollView.setVisibility(GONE);
            } else {
                filterScrollView.setVisibility(VISIBLE);
            }
        });
    }

    private void filterTasks() {
        updateCurrentFocusCardVisibility();
        Task taskToExclude = (shouldShowCurrentFocusCard() && currentFocusTask != null) ? currentFocusTask : null;
        taskAdapter.filterTasks(currentSearchQuery, currentFilter, taskToExclude);
        updateEmptyState();
        updateRemainingTasksCount();
    }

    private void updateEmptyState() {
        boolean isListEmpty = taskAdapter.getItemCount() == 0;
        boolean isFocusVisible = currentFocusCard != null && currentFocusCard.getVisibility() == View.VISIBLE;
        boolean isEmpty = isListEmpty && !isFocusVisible;
        emptyStateLayout.setVisibility(isEmpty ? VISIBLE : GONE);
        tasksRecyclerView.setVisibility(isEmpty ? GONE : VISIBLE);

        if (isEmpty) {
            if (!currentSearchQuery.isEmpty()) {
                emptyStateText.setText(R.string.no_tasks_match_your_search);
            } else if (!currentFilter.equals("All")) {
                String curTxt = "No " + currentFilter.toLowerCase() + " tasks";
                emptyStateText.setText(curTxt);
            } else {
                emptyStateText.setText(R.string.no_tasks_found);
            }
        }
    }

    public void showAddTaskBottomSheet() {
        AddTaskBottomSheet bottomSheet = AddTaskBottomSheet.newInstance();
        bottomSheet.setOnTaskActionListener(this);
        bottomSheet.show(getChildFragmentManager(), "AddTaskBottomSheet");
    }

    private void showEditTaskBottomSheet(Task task) {
        AddTaskBottomSheet bottomSheet = AddTaskBottomSheet.newInstance(task);
        bottomSheet.setOnTaskActionListener(this);
        bottomSheet.show(getChildFragmentManager(), "EditTaskBottomSheet");
    }

    private void loadTasks() {
        taskList = taskManager.loadTasks();
        for (Task task : taskList) {
            if (task.getScheduledDate() != null && !task.isCompleted()) {
                notificationManager.scheduleTaskReminder(task);
            }
        }

        taskAdapter.updateTaskList(taskList);
        filterTasks(); // Apply initial filter and sorting (this will also update focus card)
        updateEmptyState();
        updateRemainingTasksCount();
    }

    private void updateCurrentFocusCard() {
        // Only update if card should be visible
        if (!shouldShowCurrentFocusCard()) {
            hideCurrentFocusCard();
            return;
        }

        if (taskList == null || taskList.isEmpty()) {
            currentFocusTask = null;
            hideCurrentFocusCard();
            return;
        }

        List<Task> incompleteTasks = new ArrayList<>();
        for (Task task : taskList) {
            if (!task.isCompleted()) {
                incompleteTasks.add(task);
            }
        }

        if (incompleteTasks.isEmpty()) {
            currentFocusTask = null;
            hideCurrentFocusCard();
            return;
        }

        incompleteTasks.sort((t1, t2) -> {
            int p1 = t1.getPriority() != null ? t1.getPriority().getValue() : 0;
            int p2 = t2.getPriority() != null ? t2.getPriority().getValue() : 0;
            int priorityCompare = Integer.compare(p2, p1); // Descending order (high to low)
            if (priorityCompare != 0) return priorityCompare;

            Date d1 = t1.getScheduledDate();
            Date d2 = t2.getScheduledDate();
            if (d1 != null && d2 != null) {
                return d1.compareTo(d2); // Ascending order (earliest first)
            } else if (d1 != null)
                return -1; // t1 has date, t2 doesn't - t1 comes first
            else if (d2 != null)
                return 1; // t2 has date, t1 doesn't - t2 comes first
            else
                return 0; // Both null, keep order
        });

        if (!incompleteTasks.isEmpty() && incompleteTasks.get(0) != null) {
            currentFocusTask = incompleteTasks.get(0);
            populateCurrentFocusCard(currentFocusTask);
            showCurrentFocusCard();
        } else {
            currentFocusTask = null;
            hideCurrentFocusCard();
        }
    }

    private boolean shouldShowCurrentFocusCard() {
        return currentFilter != null && currentFilter.equals("All") && (currentSearchQuery == null || currentSearchQuery.isEmpty());
    }

    private void updateCurrentFocusCardVisibility() {
        boolean shouldShow = shouldShowCurrentFocusCard();
        if (shouldShow) {
            updateCurrentFocusCard();
        } else {
            hideCurrentFocusCard();
        }
    }

    private void showCurrentFocusCard() {
        if (currentFocusCard == null)
            return;
        if (!shouldShowCurrentFocusCard()) {
            return;
        }

        currentFocusCard.clearAnimation();
        if (currentFocusLabel != null)
            currentFocusLabel.clearAnimation();
        if (currentFocusMenu != null)
            currentFocusMenu.clearAnimation();

        if (currentFocusCard.getVisibility() == View.GONE) {
            currentFocusCard.setAlpha(0f);
            currentFocusCard.setVisibility(View.VISIBLE);
            currentFocusCard.animate()
                    .alpha(1f)
                    .setDuration(300)
                    .setInterpolator(new AccelerateDecelerateInterpolator())
                    .start();
        } else if (currentFocusCard.getAlpha() < 1f) {
            // If already visible but fading, ensure it's fully visible
            currentFocusCard.animate()
                    .alpha(1f)
                    .setDuration(300)
                    .setInterpolator(new AccelerateDecelerateInterpolator())
                    .start();
        }

        // Show label and menu
        if (currentFocusLabel != null) {
            if (currentFocusLabel.getVisibility() == View.GONE) {
                currentFocusLabel.setAlpha(0f);
                currentFocusLabel.setVisibility(View.VISIBLE);
                currentFocusLabel.animate()
                        .alpha(1f)
                        .setDuration(300)
                        .setInterpolator(new AccelerateDecelerateInterpolator())
                        .start();
            } else if (currentFocusLabel.getAlpha() < 1f) {
                currentFocusLabel.animate()
                        .alpha(1f)
                        .setDuration(300)
                        .setInterpolator(new AccelerateDecelerateInterpolator())
                        .start();
            }
        }
        if (currentFocusMenu != null) {
            if (currentFocusMenu.getVisibility() == View.GONE) {
                currentFocusMenu.setAlpha(0f);
                currentFocusMenu.setVisibility(View.VISIBLE);
                currentFocusMenu.animate()
                        .alpha(1f)
                        .setDuration(300)
                        .setInterpolator(new AccelerateDecelerateInterpolator())
                        .start();
            } else if (currentFocusMenu.getAlpha() < 1f) {
                currentFocusMenu.animate()
                        .alpha(1f)
                        .setDuration(300)
                        .setInterpolator(new AccelerateDecelerateInterpolator())
                        .start();
            }
        }
    }

    private void hideCurrentFocusCard() {
        if (currentFocusCard == null)
            return;

        currentFocusCard.animate().cancel();
        currentFocusCard.setVisibility(View.GONE);
        currentFocusCard.setAlpha(0f);

        // Hide label and menu
        if (currentFocusLabel != null) {
            currentFocusLabel.animate().cancel();
            currentFocusLabel.setVisibility(View.GONE);
            currentFocusLabel.setAlpha(0f);
        }
        if (currentFocusMenu != null) {
            currentFocusMenu.animate().cancel();
            currentFocusMenu.setVisibility(View.GONE);
            currentFocusMenu.setAlpha(0f);
        }
    }

    private void populateCurrentFocusCard(Task task) {
        if (task == null || currentFocusTaskName == null || currentFocusPriorityLabel == null || currentFocusDueDate == null || currentFocusMarkButton == null) {
            return;
        }

        // Set task name dynamically
        currentFocusTaskName.setText(task.getName());

        if (task.getPriority() != null) {
            String priorityText = task.getPriority().name();
            currentFocusPriorityLabel.setText(priorityText);

            int bgColor = getPriorityColor(task);
            int txtColor = getPriorityTxtColor(task);
            currentFocusPriorityLabel.setBackgroundTintList(ColorStateList.valueOf(bgColor));
            currentFocusPriorityLabel.setTextColor(txtColor);
        }

        if (task.getScheduledDate() != null) {
            SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm a", Locale.getDefault());
            Calendar taskCal = Calendar.getInstance();
            taskCal.setTime(task.getScheduledDate());
            Calendar today = Calendar.getInstance();
            Date now = new Date();
            boolean isPassed = task.getScheduledDate().before(now);
            String passedPrefix = isPassed ? "(passed) " : "";

            if (taskCal.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                    taskCal.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)) {
                currentFocusDueDate.setText("Due " + passedPrefix + timeFormat.format(task.getScheduledDate()));
            } else {
                SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd", Locale.getDefault());
                currentFocusDueDate.setText("Due " + passedPrefix + dateFormat.format(task.getScheduledDate()) + " " + timeFormat.format(task.getScheduledDate()));
            }
        } else {
            currentFocusDueDate.setText("No due date");
        }

        // Reset button state to initial (white background, black icon)
        TypedValue typedValue = new TypedValue();
        requireContext().getTheme().resolveAttribute(R.attr.text_primary, typedValue, true);
        ColorStateList colorStateList = ColorStateList.valueOf(typedValue.data);
        currentFocusMarkButton.setBackgroundTintList(colorStateList);
    }

    private void animateMarkComplete() {
        if (currentFocusTask == null || currentFocusMarkButton == null)
            return;

        // Animate button background from white to green
        ValueAnimator bgAnimator = ValueAnimator.ofArgb(
                Color.WHITE,
                Color.parseColor("#34D399") // Green color
        );
        bgAnimator.setDuration(600);
        bgAnimator.addUpdateListener(animator -> {
            if (currentFocusMarkButton != null) {
                int color = (int) animator.getAnimatedValue();
                ColorStateList colorStateList = ColorStateList.valueOf(color);
                currentFocusMarkButton.setBackgroundTintList(colorStateList);
            }
        });

        int brandPinkColor = ContextCompat.getColor(requireContext(), R.color.brand_pink);
        ValueAnimator iconTintAnimator = ValueAnimator.ofArgb(
                Color.BLACK,
                brandPinkColor);
        iconTintAnimator.setDuration(600);
        iconTintAnimator.addUpdateListener(animator -> {
            if (currentFocusMarkButton != null) {
                int color = (int) animator.getAnimatedValue();
            }
        });

        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(bgAnimator, iconTintAnimator);
        animatorSet.setInterpolator(new AccelerateDecelerateInterpolator());
        animatorSet.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                if (currentFocusTask != null && taskList != null) {
                    currentFocusTask.setCompleted(true);
                    currentFocusTask.setCompletedDate(new Date());
                    int position = taskList.indexOf(currentFocusTask);
                    onTaskCompleted(currentFocusTask, Math.max(position, 0));
                }
            }
        });
        animatorSet.start();
    }

    private void saveTasks() {
        taskManager.saveTasks(taskList);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        saveTasks();
    }

    @Override
    public void onTaskClick(Task task, int position) {
    }

    @Override
    public void onTaskCompleted(Task task, int position) {
        if (!task.isCompleted()) {
            task.setCompleted(true);
            task.setCompletedDate(new Date());
        }
        if (!task.isHabit()) {
            mintCrystals.addCoins(2);
        }

        if (task.isRecurring()) {
            String nextRepeatDate = getNextRepeatDateString(task);
            Toast.makeText(requireContext(), "Task completed for today! Will repeat on " + nextRepeatDate, Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(requireContext(), "Task completed: " + task.getName(), Toast.LENGTH_SHORT).show();
        }

        taskManager.updateTask(task);
        notificationManager.cancelTaskReminder(task);
        if (task.isHabit() && task.getHabitId() != null) {
            List<Task> tasks = taskManager.loadTasks();
            boolean allComplete = true;
            for (Task t : tasks) {
                if (task.getHabitId().equals(t.getHabitId()) && !t.isCompleted()) {
                    allComplete = false;
                    break;
                }
            }
            if (allComplete) {
                List<Habit> habits = habitManager.loadHabits();
                for (int i = 0; i < habits.size(); i++) {
                    if (habits.get(i).getId().equals(task.getHabitId())) {
                        habits.get(i).markDoneToday();
                        break;
                    }
                }
                habitManager.saveHabits(habits);
                streakManager.updateStreakOnHabitCompletion();
            }
        }

        loadTasks();
        updateCurrentFocusCardVisibility();
        updateRemainingTasksCount();
    }

    @Override
    public void onTaskUncompleted(Task task, int position) {
        if (!task.isHabit()) {
            mintCrystals.subtractCoins(2);
        }

        Toast.makeText(requireContext(), "Task marked as pending: " + task.getName(), Toast.LENGTH_SHORT).show();
        taskManager.updateTask(task);
        if (task.isHabit() && task.getHabitId() != null) {
            List<Habit> habits = habitManager.loadHabits();
            for (int i = 0; i < habits.size(); i++) {
                if (habits.get(i).getId().equals(task.getHabitId())) {
                    habits.get(i).unmarkToday();
                    break;
                }
            }
            habitManager.saveHabits(habits);
            streakManager.checkAndResetStreakIfNeeded(habits);
        }

        filterTasks();
        updateRemainingTasksCount();
    }

    @Override
    public void onTaskEdit(Task task, int position) {
        showEditTaskBottomSheet(task);
    }

    @Override
    public void onTaskDelete(Task task, int position) {
        new AlertDialog.Builder(requireContext(), R.style.AlertDialogTheme)
                .setTitle("Delete Task")
                .setMessage("Are you sure you want to delete requireContext() task?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    // Cancel any scheduled notifications for requireContext() task
                    notificationManager.cancelTaskReminder(task);

                    // Remove from main task list using task ID
                    taskList.removeIf(t -> t.getId().equals(task.getId()));

                    // Update adapter with new task list
                    taskAdapter.updateTaskList(taskList);

                    // Re-apply current filter
                    filterTasks();

                    // Save to persistent storage
                    taskManager.deleteTask(task.getId());
                    updateEmptyState();
                    updateRemainingTasksCount();
                    Toast.makeText(requireContext(), "Task deleted", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onTaskCreated(Task task) {
        taskList.add(task);
        taskAdapter.updateTaskList(taskList);
        // Update focus card first, then filter (which will exclude the focus task)
        updateCurrentFocusCardVisibility();
        filterTasks(); // Apply current filter to newly added task
        taskManager.addTask(task);

        // Schedule notification if task has scheduled date
        if (task.getScheduledDate() != null) notificationManager.scheduleTaskReminder(task);

        updateEmptyState();
        updateRemainingTasksCount();
        view.postDelayed(this::checkAndShowTaskTutorials, 300);
        Toast.makeText(requireContext(), "Task added successfully", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onTaskUpdated(Task task) {
        // Task object is already updated in place since we pass the reference
        taskAdapter.updateTaskList(taskList);
        filterTasks(); // Re-apply filter to show updated task in correct position
        taskManager.updateTask(task);

        // Cancel existing notifications and reschedule if task has scheduled date
        notificationManager.cancelTaskReminder(task);
        if (task.getScheduledDate() != null) {
            notificationManager.scheduleTaskReminder(task);
        }

        updateRemainingTasksCount();
        view.postDelayed(this::checkAndShowTaskTutorials, 300);
        Toast.makeText(requireContext(), "Task updated successfully", Toast.LENGTH_SHORT).show();
    }

    private String getNextRepeatDateString(Task task) {
        if (!task.isRecurring()) {
            return "never";
        }

        Calendar nextDate = Calendar.getInstance();

        switch (task.getRecurringType()) {
            case WEEKLY:
                nextDate.add(Calendar.WEEK_OF_YEAR, 1);
                break;
            case MONTHLY:
                nextDate.add(Calendar.MONTH, 1);
                break;
            default:
                nextDate.add(Calendar.DAY_OF_MONTH, 1);
                break;
        }

        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd", Locale.getDefault());
        return dateFormat.format(nextDate.getTime());
    }

    private int getPriorityColor(Task task) {
        if (task.getPriority() == null) {
            return Color.parseColor("#1B3D37");
        }
        switch (task.getPriority()) {
            case HIGH:
                return Color.parseColor("#4e2A35");
            case MEDIUM:
                return Color.parseColor("#3C3020");
            case LOW:
            default:
                return Color.parseColor("#1B3D37");
        }
    }

    private int getPriorityTxtColor(Task task) {
        if (task.getPriority() == null) {
            return Color.parseColor("#34D399");
        }
        switch (task.getPriority()) {
            case HIGH:
                return Color.parseColor("#FB7185");
            case MEDIUM:
                return Color.parseColor("#E3AE24");
            case LOW:
            default:
                return Color.parseColor("#34D399");
        }
    }

    private void updateNotificationPermissionCard() {
        if (view == null)
            return;
        ConstraintLayout notiPermissionCard = view.findViewById(R.id.notiPermissionCard);
        if (notiPermissionCard == null)
            return;

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            notiPermissionCard.setVisibility(GONE);
            notiPermissionCard.setOnClickListener(null);
            return;
        }

        boolean granted = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
        notiPermissionCard.setVisibility(granted ? GONE : VISIBLE);

        if (!granted) {
            notiPermissionCard
                    .setOnClickListener(v -> permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS));
        } else {
            notiPermissionCard.setOnClickListener(null);
        }
    }

    private void updateRemainingTasksCount() {
        if (dateDetails == null)
            return;

        int remainingCount = 0;
        for (Task task : taskList) {
            if (!task.isCompleted()) {
                remainingCount++;
            }
        }

        String countText = remainingCount + " REMAINING";
        dateDetails.setText(countText);
    }

}