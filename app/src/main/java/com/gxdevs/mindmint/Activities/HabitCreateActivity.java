package com.gxdevs.mindmint.Activities;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.materialswitch.MaterialSwitch;
import com.gxdevs.mindmint.Adapters.ColorSelectionAdapter;
import com.gxdevs.mindmint.Adapters.IconSelectionAdapter;
import com.gxdevs.mindmint.Adapters.TaskAdapter;
import com.gxdevs.mindmint.Fragments.AddTaskBottomSheet;
import com.gxdevs.mindmint.Models.Habit;
import com.gxdevs.mindmint.Models.Task;
import com.gxdevs.mindmint.R;
import com.gxdevs.mindmint.Utils.HabitManager;
import com.gxdevs.mindmint.Utils.TaskManager;
import com.gxdevs.mindmint.Utils.Utils;
import com.gxdevs.mindmint.Widgets.HabitListWidgetProvider;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HabitCreateActivity extends AppCompatActivity {

    public static final String EXTRA_HABIT_ID = "extra_habit_id";
    private HabitManager habitManager;
    private TaskManager taskManager;
    private Habit habit;
    private TaskAdapter subTaskAdapter;
    private ImageView habitIconView;
    private EditText habitTitleEdit;
    private EditText reasonEdit;
    private AppCompatButton addTaskCard;
    private AppCompatButton btnDone;
    private AppCompatButton btnDelete;
    private RecyclerView subTasksRecyclerView;
    private RecyclerView rvIcons;
    private RecyclerView rvColors;
    private TextView effortEasy;
    private TextView effortMedium;
    private TextView effortHard;
    private int selectedIconRes = R.drawable.flame;
    private int[] selectedColorPair = { Color.parseColor("#FF6B6B"), Color.parseColor("#4DFF6B6B") };
    private TextView dateDetails;
    private com.google.android.material.materialswitch.MaterialSwitch swAskEmotion;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_habit_create);

        Utils.setPad(findViewById(R.id.main), "bottom", this);

        initViews();
        updateDateHeader();

        boolean isEditMode = false;
        String habitId = getIntent().getStringExtra(EXTRA_HABIT_ID);
        if (habitId != null) {
            for (Habit h : habitManager.loadHabits()) {
                if (habitId.equals(h.getId())) {
                    habit = h;
                    isEditMode = true;
                    break;
                }
            }
        }
        if (habit == null)
            habit = new Habit();

        TextView screenTitle = findViewById(R.id.greetings);
        screenTitle.setText(isEditMode ? "Edit Ritual" : "Create Ritual");

        btnDelete.setVisibility(isEditMode ? View.VISIBLE : View.GONE);
        btnDelete.setOnClickListener(v -> new AlertDialog.Builder(this)
                .setTitle("Delete habit?")
                .setMessage("This cannot be undone.")
                .setPositiveButton("Delete", (d, w) -> {
                    habitManager.deleteHabit(habit.getId());
                    setResult(RESULT_OK);
                    finish();
                })
                .setNegativeButton("Cancel", null)
                .show());

        habitTitleEdit.setText(habit.getName());
        reasonEdit.setText(habit.getReason());
        swAskEmotion.setChecked(habit.isAskEmotion());
        goalTrackingSwitch.setChecked(habit.isGoalTracking());
        detailsContainer.setVisibility(habit.isGoalTracking() ? View.VISIBLE : View.GONE);
        if (habit.getTargetUnit() != null)
            unitName.setText(habit.getTargetUnit());
        dailyTarget.setText(String.valueOf(habit.getTargetCount()));
        oneTapAdds.setText(String.valueOf(habit.getOneTapValue()));

        goalTrackingSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> detailsContainer.setVisibility(isChecked ? View.VISIBLE : View.GONE));

        selectedIconRes = habit.getIcon() != 0 ? habit.getIcon() : R.drawable.flame;
        habitIconView.setImageResource(selectedIconRes);

        if (habit.getIconTint() != 0 && habit.getIconBackgroundTint() != 0) {
            selectedColorPair = new int[] { habit.getIconTint(), habit.getIconBackgroundTint() };
        }

        applyColorToIcon();

        List<Integer> iconDrawables = new java.util.ArrayList<>();
        iconDrawables.add(R.drawable.flame);
        iconDrawables.add(R.drawable.droplets);
        iconDrawables.add(R.drawable.dumbbell);
        iconDrawables.add(R.drawable.book);
        iconDrawables.add(R.drawable.moon);
        iconDrawables.add(R.drawable.gamepad);
        iconDrawables.add(R.drawable.palette);
        iconDrawables.add(R.drawable.party_popper);

        rvIcons.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        IconSelectionAdapter iconAdapter = new IconSelectionAdapter(this, iconDrawables, iconRes -> {
            selectedIconRes = iconRes;
            habitIconView.setImageResource(iconRes);
            applyColorToIcon();
        });
        rvIcons.setAdapter(iconAdapter);
        iconAdapter.setSelectedIcon(selectedIconRes);

        List<int[]> colorPairs = new java.util.ArrayList<>();
        colorPairs.add(new int[] { Color.parseColor("#FF6B6B"), Color.parseColor("#4DFF6B6B") }); // Red
        colorPairs.add(new int[] { Color.parseColor("#4ECDC4"), Color.parseColor("#4D4ECDC4") }); // Teal
        colorPairs.add(new int[] { Color.parseColor("#45B7D1"), Color.parseColor("#4D45B7D1") }); // Blue
        colorPairs.add(new int[] { Color.parseColor("#FFA07A"), Color.parseColor("#4DFFA07A") }); // Light Salmon
        colorPairs.add(new int[] { Color.parseColor("#98D8C8"), Color.parseColor("#4D98D8C8") }); // Mint
        colorPairs.add(new int[] { Color.parseColor("#F7DC6F"), Color.parseColor("#4DF7DC6F") }); // Yellow
        colorPairs.add(new int[] { Color.parseColor("#BB8FCE"), Color.parseColor("#4DBB8FCE") }); // Purple
        colorPairs.add(new int[] { Color.parseColor("#85C1E2"), Color.parseColor("#4D85C1E2") }); // Sky Blue

        rvColors.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        ColorSelectionAdapter colorAdapter = new ColorSelectionAdapter(colorPairs, colorPair -> {
            selectedColorPair = colorPair;
            applyColorToIcon();
        });
        rvColors.setAdapter(colorAdapter);
        colorAdapter.setSelectedColorPair(selectedColorPair);

        updateEffortSelector(habit.getDifficulty());
        effortEasy.setOnClickListener(v -> {
            habit.setDifficulty(Habit.Difficulty.EASY);
            updateEffortSelector(Habit.Difficulty.EASY);
        });
        effortMedium.setOnClickListener(v -> {
            habit.setDifficulty(Habit.Difficulty.MEDIUM);
            updateEffortSelector(Habit.Difficulty.MEDIUM);
        });
        effortHard.setOnClickListener(v -> {
            habit.setDifficulty(Habit.Difficulty.HARD);
            updateEffortSelector(Habit.Difficulty.HARD);
        });

        subTasksRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        subTasksRecyclerView.setNestedScrollingEnabled(false);
        List<Task> subTasks = new java.util.ArrayList<>();
        for (Task t : taskManager.loadTasks()) {
            if (habit.getId().equals(t.getHabitId()))
                subTasks.add(t);
        }
        subTaskAdapter = new TaskAdapter(this, subTasks, true);
        subTasksRecyclerView.setAdapter(subTaskAdapter);

        addTaskCard.setOnClickListener(v -> {
            AddTaskBottomSheet sheet = AddTaskBottomSheet.newInstance();
            sheet.setOnTaskActionListener(new AddTaskBottomSheet.OnTaskActionListener() {
                @Override
                public void onTaskCreated(Task task) {
                    task.setHabit(true);
                    task.setHabitId(habit.getId());
                    task.setRecurringType(Task.RecurringType.DAILY);
                    taskManager.addTask(task);
                    subTaskAdapter.addTask(task);
                    subTasksRecyclerView.scrollToPosition(0);
                    tryAutoCompleteHabit();
                }

                @Override
                public void onTaskUpdated(Task task) {
                    taskManager.updateTask(task);
                    List<Task> current = subTaskAdapter.getTaskList();
                    int idx = -1;
                    for (int i = 0; i < current.size(); i++) {
                        if (current.get(i).getId().equals(task.getId())) {
                            idx = i;
                            break;
                        }
                    }
                    if (idx != -1) {
                        current.set(idx, task);
                        subTaskAdapter.notifyItemChanged(idx);
                    } else {
                        subTaskAdapter.addTask(task);
                        subTasksRecyclerView.scrollToPosition(0);
                    }
                    tryAutoCompleteHabit();
                }
            });
            sheet.show(getSupportFragmentManager(), "AddTaskBottomSheet");
        });

        subTaskAdapter.setOnTaskClickListener(new TaskAdapter.OnTaskClickListener() {

            @Override
            public void onTaskClick(Task task, int position) {
            }

            @Override
            public void onTaskCompleted(Task task, int position) {
            }

            @Override
            public void onTaskUncompleted(Task task, int position) {
            }

            @Override
            public void onTaskEdit(Task task, int position) {
                AddTaskBottomSheet sheet = AddTaskBottomSheet.newInstance(task);
                sheet.setOnTaskActionListener(new AddTaskBottomSheet.OnTaskActionListener() {
                    @Override
                    public void onTaskCreated(Task t) {
                    }

                    @Override
                    public void onTaskUpdated(Task t) {
                        taskManager.updateTask(t);
                        List<Task> current = subTaskAdapter.getTaskList();
                        for (int i = 0; i < current.size(); i++) {
                            if (current.get(i).getId().equals(t.getId())) {
                                current.set(i, t);
                                subTaskAdapter.notifyItemChanged(i);
                                break;
                            }
                        }
                        tryAutoCompleteHabit();
                    }
                });
                sheet.show(getSupportFragmentManager(), "EditTaskBottomSheet");
            }

            @Override
            public void onTaskDelete(Task task, int position) {
                taskManager.deleteTask(task.getId());
                subTaskAdapter.removeTask(position);
                tryAutoCompleteHabit();
            }

        });

        btnDone.setOnClickListener(v -> {
            String name = habitTitleEdit.getText() != null ? habitTitleEdit.getText().toString().trim() : "";
            String reason = reasonEdit.getText() != null ? reasonEdit.getText().toString().trim() : "";

            if (name.isEmpty()) {
                habitTitleEdit.setError("Ritual name is required");
                habitTitleEdit.requestFocus();
                return;
            }

            if (reason.isEmpty()) {
                reasonEdit.setError("Description is required");
                reasonEdit.requestFocus();
                return;
            }

            habit.setName(name);
            habit.setReason(reason);
            habit.setIcon(selectedIconRes);
            habit.setIconTint(selectedColorPair[0]);
            habit.setIconBackgroundTint(selectedColorPair[1]);
            habit.setAskEmotion(swAskEmotion.isChecked());

            boolean isGoal = goalTrackingSwitch.isChecked();
            habit.setGoalTracking(isGoal);
            if (isGoal) {
                String tStr = dailyTarget.getText().toString().trim();
                int target = tStr.isEmpty() ? 1 : Integer.parseInt(tStr);
                if (target < 1)
                    target = 1;
                habit.setTargetCount(target);

                String oStr = oneTapAdds.getText().toString().trim();
                int oneTap = oStr.isEmpty() ? 1 : Integer.parseInt(oStr);
                if (oneTap < 1)
                    oneTap = 1;
                habit.setOneTapValue(oneTap);
                habit.setTargetUnit(unitName.getText().toString().trim());
            }
            habitManager.addHabit(habit);

            // Update Widgets
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
            int[] appWidgetIds = appWidgetManager.getAppWidgetIds(
                    new ComponentName(this, HabitListWidgetProvider.class));
            appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.widget_habit_list_view);

            setResult(RESULT_OK, new Intent().putExtra(EXTRA_HABIT_ID, habit.getId()));
            finish();
        });
    }

    private void initViews() {
        habitManager = new HabitManager(this);
        taskManager = new TaskManager(this);
        habitTitleEdit = findViewById(R.id.habitTitleEdit);
        reasonEdit = findViewById(R.id.inputReason);
        addTaskCard = findViewById(R.id.addTaskFAB);
        btnDone = findViewById(R.id.btnDone);
        btnDelete = findViewById(R.id.btnDelete);
        subTasksRecyclerView = findViewById(R.id.subTasksRecyclerView);
        habitIconView = findViewById(R.id.habitTileIcon);
        rvIcons = findViewById(R.id.rvIcons);
        rvColors = findViewById(R.id.rvColors);
        effortEasy = findViewById(R.id.effortEasy);
        effortMedium = findViewById(R.id.effortMedium);
        effortHard = findViewById(R.id.effortHard);
        dateDetails = findViewById(R.id.dateDetails);
        swAskEmotion = findViewById(R.id.swAskEmotion);
        goalTrackingSwitch = findViewById(R.id.goalTrackingSwitch);
        unitName = findViewById(R.id.unitName);
        dailyTarget = findViewById(R.id.dailyTarget);
        oneTapAdds = findViewById(R.id.oneTapAdds);
        detailsContainer = findViewById(R.id.detailsContainer);
    }

    private MaterialSwitch goalTrackingSwitch;
    private EditText unitName, dailyTarget, oneTapAdds;
    private View detailsContainer;

    private void applyColorToIcon() {
        habitIconView.setColorFilter(selectedColorPair[0]);
        habitIconView.setBackgroundTintList(ColorStateList.valueOf(selectedColorPair[1]));
    }

    private void updateEffortSelector(Habit.Difficulty difficulty) {
        // Reset all
        effortEasy.setBackground(null);
        effortEasy.setTextColor(getAttrColor(R.attr.text_tertiary));
        effortMedium.setBackground(null);
        effortMedium.setTextColor(getAttrColor(R.attr.text_tertiary));
        effortHard.setBackground(null);
        effortHard.setTextColor(getAttrColor(R.attr.text_tertiary));

        // Set selected
        TextView selected;
        if (difficulty == Habit.Difficulty.EASY) {
            selected = effortEasy;
        } else if (difficulty == Habit.Difficulty.MEDIUM) {
            selected = effortMedium;
        } else {
            selected = effortHard;
        }

        if (selected != null) {
            selected.setBackgroundResource(R.drawable.bg_segment_selected);
            selected.setTextColor(getAttrColor(R.attr.text_primary));
        }
    }

    private int getAttrColor(int attr) {
        TypedValue typedValue = new TypedValue();
        getTheme().resolveAttribute(attr, typedValue, true);
        return typedValue.data;
    }

    private void updateDateHeader() {
        if (dateDetails == null)
            return;
        Date now = new Date();
        SimpleDateFormat fmt = new SimpleDateFormat("EEEE, MMM dd", Locale.getDefault());
        String formatted = fmt.format(now).toUpperCase(Locale.getDefault());
        dateDetails.setText(formatted);
    }

    private void tryAutoCompleteHabit() {
        List<Task> current = subTaskAdapter.getTaskList();
        if (current.isEmpty())
            return;

        new AlertDialog.Builder(this)
                .setTitle("Mark today complete?")
                .setMessage("Mark habit as completed for today since all tasks are set?")
                .setPositiveButton("Yes", (d, w) -> habitManager.markHabit(habit))
                .setNegativeButton("No", null)
                .show();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }
}
