package com.gxdevs.mindmint.Fragments;

import android.graphics.Color;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.gxdevs.mindmint.Views.RoundedBarChart;
import com.gxdevs.mindmint.R;
import com.gxdevs.mindmint.Models.StatsViewModel;
import com.gxdevs.mindmint.db.MindMintRoomDatabase;
import com.gxdevs.mindmint.db.dao.TaskDao;
import com.gxdevs.mindmint.db.models.DayOfWeekCount;

import java.util.ArrayList;
import java.util.List;

public class StatsTasksFragment extends Fragment {

    private StatsViewModel viewModel;
    private TaskDao taskDao;

    // UI
    private TextView tvMetricLeftValue;
    private TextView tvMetricRightValue;
    private TextView tvMetricRightSub;
    private TextView tvSuggestionText;
    private RoundedBarChart taskBarChart;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_stats_tasks, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        taskDao = MindMintRoomDatabase.getInstance(requireContext()).taskDao();
        viewModel = new ViewModelProvider(requireActivity()).get(StatsViewModel.class);

        initViews(view);

        viewModel.getPeriodRange().observe(getViewLifecycleOwner(), this::updateStats);
    }

    private void initViews(View view) {
        tvMetricLeftValue = view.findViewById(R.id.tvMetricLeftValue);
        tvMetricRightValue = view.findViewById(R.id.tvMetricRightValue);
        tvMetricRightSub = view.findViewById(R.id.tvMetricRightSub);

        View suggestionCard = view.findViewById(R.id.statsSuggestionCard);
        tvSuggestionText = suggestionCard.findViewById(R.id.tvSuggestionText);

        taskBarChart = view.findViewById(R.id.taskBarChart);
    }

    private void updateStats(StatsViewModel.PeriodRange range) {
        boolean isWeekly = Boolean.TRUE.equals(viewModel.getIsWeekly().getValue());

        int count = taskDao.getCompletedCountInDateRange(range.startMs, range.endMs);
        tvMetricLeftValue.setText(String.valueOf(count));
        TextView tvMetricLeftSub = getView().findViewById(R.id.tvMetricLeftSub);
        if (tvMetricLeftSub == null) tvMetricLeftSub = getView().findViewById(R.id.tvMetricLeftSub);
        tvMetricLeftSub.setText(isWeekly ? "This Week" : "This Month");

        // Pending count
        int pending = taskDao.getPendingCount();

        // RIGHT: Pending
        tvMetricRightValue.setText(String.valueOf(pending));

        // Best Day
        List<DayOfWeekCount> dayCounts = taskDao.getCompletedTasksByDayOfWeek();
        String bestDay = "No data";
        int max = 0;
        String[] days = { "Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat" };

        for (DayOfWeekCount dc : dayCounts) {
            try {
                int d = Integer.parseInt(dc.day_of_week);
                if (d >= 0 && d < days.length) {
                    if (dc.count > max) {
                        max = dc.count;
                        bestDay = days[d];
                    }
                }
            } catch (Exception ignored) {
            }
        }
        tvMetricRightSub.setText("Best: " + bestDay);

        updateTaskBarChart(range);
        generateTaskSuggestion(pending);
    }

    private void updateTaskBarChart(StatsViewModel.PeriodRange range) {
        List<BarEntry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();

        List<StatsViewModel.PeriodSlot> slots = range.slots;
        for (int i = 0; i < slots.size(); i++) {
            StatsViewModel.PeriodSlot slot = slots.get(i);
            int c = taskDao.getCompletedCountInDateRange(slot.startMs, slot.endMs);
            entries.add(new BarEntry(i, c));
            labels.add(slot.label);
        }

        setupBarChart(taskBarChart, entries, labels, getAttrColor(R.attr.brand_pink));
    }

    private void generateTaskSuggestion(int pending) {
        if (pending >= 5) {
            tvSuggestionText.setText("You have " + pending + " pending tasks. Try grouping similar ones and completing them in batches.");
        } else if (pending > 0) {
            tvSuggestionText.setText("Only " + pending + " tasks left! A quick focus session can clear them.");
        } else {
            tvSuggestionText.setText("All caught up! Great job staying on top of your tasks.");
        }
    }

    private void setupBarChart(RoundedBarChart chart, List<BarEntry> entries, List<String> labels, int color) {
        int endColor = Color.argb(60, Color.red(color), Color.green(color), Color.blue(color));
        BarDataSet dataSet = new BarDataSet(entries, "Data");
        dataSet.setDrawValues(false);
        dataSet.setGradientColor(endColor, color);

        BarData barData = new BarData(dataSet);
        barData.setBarWidth(0.5f);
        chart.setData(barData);

        XAxis x = chart.getXAxis();
        x.setValueFormatter(new IndexAxisValueFormatter(labels));
        x.setGranularity(1f);
        x.setTextColor(getAttrColor(R.attr.text_tertiary));
        x.setPosition(XAxis.XAxisPosition.BOTTOM);
        x.setDrawGridLines(false);

        YAxis left = chart.getAxisLeft();
        left.setTextColor(getAttrColor(R.attr.text_tertiary));
        left.setDrawGridLines(false);
        left.setAxisMinimum(0f);
        chart.getAxisRight().setEnabled(false);

        chart.setBackgroundColor(Color.TRANSPARENT);
        chart.getDescription().setEnabled(false);
        chart.getLegend().setEnabled(false);
        chart.animateY(700, Easing.EaseInOutQuad);
        chart.invalidate();
    }

    private int getAttrColor(int attr) {
        TypedValue typedValue = new TypedValue();
        requireActivity().getTheme().resolveAttribute(attr, typedValue, true);
        return typedValue.data;
    }
}
