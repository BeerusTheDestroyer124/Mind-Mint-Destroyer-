package com.gxdevs.mindmint.Fragments;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CalendarView;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.AppCompatButton;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;
import com.google.gson.Gson;
import com.gxdevs.mindmint.Models.Task;
import com.gxdevs.mindmint.R;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class AddTaskBottomSheet extends BottomSheetDialogFragment {

    public interface OnTaskActionListener {
        void onTaskCreated(Task task);

        void onTaskUpdated(Task task);
    }

    private EditText taskNameInput;
    private RadioGroup priorityRadioGroup;
    private RadioButton rbLow, rbMedium, rbHigh;
    private TextView dateText, timeText;
    private MaterialSwitch reminderSwitch;
    private MaterialSwitch focusModeSwitch;
    private SeekBar focusDurationSeekbar;
    private TextView focusDurationLabel;
    private LinearLayout focusDurationContainer;
    private LinearLayout focusLockedBanner;
    private AppCompatButton cancelButton, saveButton;
    private TextView sheetTitle;
    private ImageView taskIconView;
    private int currentIconIndex = 0;
    private OnTaskActionListener listener;
    private Task editingTask;
    private Calendar selectedDateTime;
    private boolean isEditMode = false;
    private int focusDurationMinutes = 0; // 0 = open-ended
    private RepeatOptionsBottomSheet.RepeatOptions currentRepeatOptions;

    public static AddTaskBottomSheet newInstance() {
        return new AddTaskBottomSheet();
    }

    public static AddTaskBottomSheet newInstance(Task task) {
        AddTaskBottomSheet fragment = new AddTaskBottomSheet();
        fragment.editingTask = task;
        fragment.isEditMode = true;
        return fragment;
    }

    public void setOnTaskActionListener(OnTaskActionListener listener) {
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_add_task, container, false);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        BottomSheetDialog dialog = (BottomSheetDialog) super.onCreateDialog(savedInstanceState);
        dialog.setOnShowListener(d -> {
            FrameLayout bottomSheet = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null) {
                bottomSheet.setBackground(new ColorDrawable(Color.TRANSPARENT));
            }
        });
        return dialog;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);
        setupSpinners();
        setupDateTimePicker();
        setupClickListeners();

        if (isEditMode && editingTask != null) {
            populateFieldsForEdit();
        } else {
            setDefaultValues();
        }
    }

    private void initViews(View view) {
        taskNameInput = view.findViewById(R.id.taskNameInput);
        priorityRadioGroup = view.findViewById(R.id.rgPriority);
        rbLow = view.findViewById(R.id.rbLow);
        rbMedium = view.findViewById(R.id.rbMedium);
        rbHigh = view.findViewById(R.id.rbHigh);
        dateText = view.findViewById(R.id.dateText);
        timeText = view.findViewById(R.id.timeText);
        reminderSwitch = view.findViewById(R.id.reminderSwitch);
        focusModeSwitch = view.findViewById(R.id.focusModeSwitch);
        focusDurationSeekbar = view.findViewById(R.id.focusDurationSeekbar);
        focusDurationLabel = view.findViewById(R.id.focusDurationLabel);
        focusDurationContainer = view.findViewById(R.id.focusDurationContainer);
        focusLockedBanner = view.findViewById(R.id.focusLockedBanner);
        cancelButton = view.findViewById(R.id.cancelButton);
        saveButton = view.findViewById(R.id.saveButton);
        sheetTitle = view.findViewById(R.id.greetings);
        taskIconView = view.findViewById(R.id.taskIconView);
        ImageView crossBtn = view.findViewById(R.id.crossBtn);

        selectedDateTime = Calendar.getInstance();
        crossBtn.setOnClickListener(v -> {
            if (this.isVisible()) {
                this.dismiss();
            }
        });

        // Setup icon picker
        setupIconPicker();
        setupFocusModeSection();
    }

    private void setupSpinners() {
        priorityRadioGroup.setOnCheckedChangeListener((group, checkedId) -> updatePriorityTextColors());
        // Initial update for default selection
        updatePriorityTextColors();
    }

    private void updatePriorityTextColors() {
        int textPrimaryColor = getAttrColor(R.attr.text_primary);
        int textSecondaryColor = getAttrColor(R.attr.text_disabled);

        rbLow.setTextColor(rbLow.isChecked() ? textPrimaryColor : textSecondaryColor);
        rbMedium.setTextColor(rbMedium.isChecked() ? textPrimaryColor : textSecondaryColor);
        rbHigh.setTextColor(rbHigh.isChecked() ? textPrimaryColor : textSecondaryColor);
    }

    private int getAttrColor(int attr) {
        TypedValue typedValue = new TypedValue();
        requireContext().getTheme().resolveAttribute(attr, typedValue, true);
        return typedValue.data;
    }

    private void setupIconPicker() {
        final int[] iconOptions = {
                R.drawable.flame,
                R.drawable.droplets,
                R.drawable.dumbbell,
                R.drawable.book,
                R.drawable.moon,
                R.drawable.gamepad,
                R.drawable.palette,
                R.drawable.party_popper
        };

        // Initialize current icon index
        if (editingTask != null && editingTask.getIcon() != 0) {
            for (int i = 0; i < iconOptions.length; i++) {
                if (iconOptions[i] == editingTask.getIcon()) {
                    currentIconIndex = i;
                    break;
                }
            }
        } else {
            currentIconIndex = 0;
        }

        taskIconView.setOnClickListener(v -> {
            currentIconIndex = (currentIconIndex + 1) % iconOptions.length;
            int nextIcon = iconOptions[currentIconIndex];
            taskIconView.setImageResource(nextIcon);
        });
    }

    private void setupFocusModeSection() {
        updateFocusDurationLabel(focusDurationMinutes);

        focusModeSwitch.setOnCheckedChangeListener((btn, isChecked) -> {
            if (isChecked) {
                focusDurationContainer.setVisibility(View.VISIBLE);
                focusDurationContainer.setAlpha(0f);
                focusDurationContainer.animate().alpha(1f).setDuration(200).start();
            } else {
                focusDurationContainer.animate().alpha(0f).setDuration(150).withEndAction(() ->
                        focusDurationContainer.setVisibility(View.GONE)).start();
            }
        });

        focusDurationSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                focusDurationMinutes = progress;
                updateFocusDurationLabel(progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
    }

    private void updateFocusDurationLabel(int minutes) {
        if (focusDurationLabel == null) return;
        if (minutes == 0) {
            focusDurationLabel.setText("Open Ended");
        } else {
            focusDurationLabel.setText(minutes + " min");
        }
    }

    private void setupDateTimePicker() {
        LinearLayout dateClickable = requireView().findViewById(R.id.dateClickable);
        LinearLayout timeClickable = requireView().findViewById(R.id.timeClickable);
        dateClickable.setOnClickListener(v -> showCustomDatePicker());
        timeClickable.setOnClickListener(v -> showCustomTimePicker());
    }

    private void showCustomDatePicker() {
        com.google.android.material.dialog.MaterialAlertDialogBuilder builder = new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext(), R.style.AlertDialogTheme);
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_date_picker, null);
        CalendarView calendarView = dialogView.findViewById(R.id.calendarView);
        LinearLayout repeatButton = dialogView.findViewById(R.id.repeatButton);
        Button cancelButton = dialogView.findViewById(R.id.cancelButton);
        Button doneButton = dialogView.findViewById(R.id.doneButton);

        calendarView.setDate(selectedDateTime.getTimeInMillis());
        AlertDialog dialog = builder.setView(dialogView).create();
        calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            selectedDateTime.set(Calendar.YEAR, year);
            selectedDateTime.set(Calendar.MONTH, month);
            selectedDateTime.set(Calendar.DAY_OF_MONTH, dayOfMonth);
        });

        repeatButton.setOnClickListener(v -> {
            dialog.dismiss();
            showRepeatOptionsDialog();
        });

        cancelButton.setOnClickListener(v -> dialog.dismiss());

        doneButton.setOnClickListener(v -> {
            updateDateTimeDisplay();
            dialog.dismiss();
        });

        dialog.show();
    }

    private void showCustomTimePicker() {
        int currentHour = selectedDateTime.get(Calendar.HOUR_OF_DAY);
        int currentMinute = selectedDateTime.get(Calendar.MINUTE);
        MaterialTimePicker timePicker = new MaterialTimePicker.Builder()
                .setTheme(R.style.ThemeOverlay_MindMint_TimePicker)
                .setTimeFormat(TimeFormat.CLOCK_12H)
                .setHour(currentHour)
                .setMinute(currentMinute)
                .setTitleText("Select Time")
                .build();
        timePicker.addOnPositiveButtonClickListener(v -> {
            selectedDateTime.set(Calendar.HOUR_OF_DAY, timePicker.getHour());
            selectedDateTime.set(Calendar.MINUTE, timePicker.getMinute());
            updateDateTimeDisplay();
        });
        timePicker.show(getParentFragmentManager(), "task_time_picker");
    }



    private void updateDateTimeDisplay() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd", Locale.getDefault());
        SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm a", Locale.getDefault());

        String dateStr = dateFormat.format(selectedDateTime.getTime());
        String timeStr = timeFormat.format(selectedDateTime.getTime());

        // Update date text - check if it's today
        Calendar today = Calendar.getInstance();
        if (selectedDateTime.get(Calendar.YEAR) == today.get(Calendar.YEAR) && selectedDateTime.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)) {
            dateText.setText("Today");
        } else {
            dateText.setText(dateStr);
        }

        // Update time text
        timeText.setText(timeStr);
    }

    private void setupClickListeners() {
        cancelButton.setOnClickListener(v -> dismiss());

        saveButton.setOnClickListener(v -> {
            if (validateInput()) {
                if (isEditMode) {
                    updateTask();
                } else {
                    createTask();
                }
                dismiss();
            }
        });
    }

    private boolean validateInput() {
        String taskName = taskNameInput.getText().toString().trim();
        if (taskName.isEmpty()) {
            Toast.makeText(getContext(), "Task name is required", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private void createTask() {
        String taskName = taskNameInput.getText().toString().trim();
        Task.Priority priority = getSelectedPriority();
        Task.RecurringType recurringType = getRecurringTypeFromRepeatOptions();

        // Create task with icon
        int[] iconOptions = {
                R.drawable.flame,
                R.drawable.droplets,
                R.drawable.dumbbell,
                R.drawable.book,
                R.drawable.moon,
                R.drawable.gamepad,
                R.drawable.palette,
                R.drawable.party_popper
        };
        int selectedIcon = iconOptions[currentIconIndex];
        Task newTask = new Task(taskName, "", R.drawable.list_todo, priority);
        newTask.setRecurringType(recurringType);
        newTask.setScheduledDate(selectedDateTime.getTime());
        newTask.setHasReminder(reminderSwitch.isChecked());
        newTask.setIcon(selectedIcon);
        newTask.setFocusModeEnabled(focusModeSwitch.isChecked());
        newTask.setFocusDurationMinutes(focusDurationMinutes);
        newTask.setFocusStatus("IDLE");

        // Save advanced repeat options if available
        if (currentRepeatOptions != null) {
            Gson gson = new Gson();
            newTask.setRepeatOptionsJson(gson.toJson(currentRepeatOptions));
        }

        if (listener != null) {
            listener.onTaskCreated(newTask);
        }
    }

    private void updateTask() {
        String taskName = taskNameInput.getText().toString().trim();
        Task.Priority priority = getSelectedPriority();
        Task.RecurringType recurringType = getRecurringTypeFromRepeatOptions();

        editingTask.setName(taskName);
        editingTask.setPriority(priority);
        editingTask.setRecurringType(recurringType);
        editingTask.setScheduledDate(selectedDateTime.getTime());
        editingTask.setHasReminder(reminderSwitch.isChecked());

        editingTask.setFocusModeEnabled(focusModeSwitch.isChecked());
        editingTask.setFocusDurationMinutes(focusDurationMinutes);
        // Never reset focusStatus here — preserve IN_PROGRESS state

        // Update icon if changed
        int[] iconOptions = {
                R.drawable.flame,
                R.drawable.droplets,
                R.drawable.dumbbell,
                R.drawable.book,
                R.drawable.moon,
                R.drawable.gamepad,
                R.drawable.palette,
                R.drawable.party_popper
        };
        int selectedIcon = iconOptions[currentIconIndex];
        editingTask.setIcon(selectedIcon);

        // Save advanced repeat options if available
        if (currentRepeatOptions != null) {
            Gson gson = new Gson();
            editingTask.setRepeatOptionsJson(gson.toJson(currentRepeatOptions));
        }

        if (listener != null) {
            listener.onTaskUpdated(editingTask);
        }
    }

    private Task.RecurringType getRecurringTypeFromRepeatOptions() {
        if (currentRepeatOptions == null) {
            return Task.RecurringType.NONE;
        }

        return switch (currentRepeatOptions.frequencyType) {
            case DAILY -> Task.RecurringType.DAILY;
            case WEEKLY -> Task.RecurringType.WEEKLY;
            case MONTHLY -> Task.RecurringType.MONTHLY;
            case YEARLY -> Task.RecurringType.MONTHLY; // Use monthly as closest approximation
        };
    }

    private void populateFieldsForEdit() {
        sheetTitle.setText("Edit Task");
        saveButton.setText("Update");

        taskNameInput.setText(editingTask.getName());
        setPrioritySelection(editingTask.getPriority());

        // Set icon
        int iconRes = editingTask.getIcon() != 0 ? editingTask.getIcon() : R.drawable.list_todo;
        taskIconView.setImageResource(iconRes);

        // Find current icon index
        int[] iconOptions = {
                R.drawable.flame,
                R.drawable.droplets,
                R.drawable.dumbbell,
                R.drawable.book,
                R.drawable.moon,
                R.drawable.gamepad,
                R.drawable.palette,
                R.drawable.party_popper
        };
        for (int i = 0; i < iconOptions.length; i++) {
            if (iconOptions[i] == iconRes) {
                currentIconIndex = i;
                break;
            }
        }

        // Set scheduled date
        if (editingTask.getScheduledDate() != null) {
            selectedDateTime.setTime(editingTask.getScheduledDate());
            updateDateTimeDisplay();
        }

        // Set reminder switch
        reminderSwitch.setChecked(editingTask.hasReminder());

        // Focus mode fields
        boolean focusEnabled = editingTask.isFocusModeEnabled();
        focusModeSwitch.setChecked(focusEnabled);
        focusDurationMinutes = editingTask.getFocusDurationMinutes();
        if (focusEnabled) {
            focusDurationContainer.setVisibility(View.VISIBLE);
        }
        focusDurationSeekbar.setProgress(focusDurationMinutes);
        updateFocusDurationLabel(focusDurationMinutes);

        // If task is IN_PROGRESS, show lock banner and disable save
        if (editingTask.isFocusInProgress()) {
            focusLockedBanner.setVisibility(View.VISIBLE);
            saveButton.setEnabled(false);
            saveButton.setAlpha(0.5f);
        }

        // Load existing repeat options
        if (editingTask.getRepeatOptionsJson() != null && !editingTask.getRepeatOptionsJson().isEmpty()) {
            try {
                Gson gson = new Gson();
                currentRepeatOptions = gson.fromJson(editingTask.getRepeatOptionsJson(),
                        RepeatOptionsBottomSheet.RepeatOptions.class);
            } catch (Exception e) {
                // If parsing fails, ignore and use default options
                currentRepeatOptions = null;
            }
        }
    }

    private void setDefaultValues() {
        sheetTitle.setText("Add Task");
        saveButton.setText("Save");

        // Set default priority to Medium (already set in XML)
        rbMedium.setChecked(true);

        // Set default time to current time + 1 hour
        selectedDateTime.add(Calendar.HOUR_OF_DAY, 1);
        updateDateTimeDisplay();
    }

    private Task.Priority getSelectedPriority() {
        int checkedId = priorityRadioGroup.getCheckedRadioButtonId();
        if (checkedId == R.id.rbLow) {
            return Task.Priority.LOW;
        } else if (checkedId == R.id.rbHigh) {
            return Task.Priority.HIGH;
        } else {
            return Task.Priority.MEDIUM; // Default
        }
    }

    private void setPrioritySelection(Task.Priority priority) {
        switch (priority) {
            case LOW:
                rbLow.setChecked(true);
                break;
            case HIGH:
                rbHigh.setChecked(true);
                break;
            case MEDIUM:
            default:
                rbMedium.setChecked(true);
                break;
        }
    }

    private void showRepeatOptionsDialog() {
        RepeatOptionsBottomSheet repeatSheet;
        if (currentRepeatOptions != null) {
            repeatSheet = RepeatOptionsBottomSheet.newInstance(selectedDateTime, currentRepeatOptions);
        } else {
            repeatSheet = RepeatOptionsBottomSheet.newInstance(selectedDateTime);
        }

        repeatSheet.setOnRepeatOptionsListener(repeatOptions -> {
            currentRepeatOptions = repeatOptions;
            showCustomDatePicker();
        });

        repeatSheet.show(getParentFragmentManager(), "RepeatOptionsBottomSheet");
    }

}
