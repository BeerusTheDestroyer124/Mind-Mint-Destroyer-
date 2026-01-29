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
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.gxdevs.mindmint.Models.StatsViewModel;
import com.gxdevs.mindmint.R;
import com.gxdevs.mindmint.Views.RoundedBarChart;
import com.gxdevs.mindmint.db.MindMintRoomDatabase;
import com.gxdevs.mindmint.db.dao.FocusDao;
import com.gxdevs.mindmint.db.entities.FocusSessionEntity;

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class StatsFocusFragment extends Fragment {

    private StatsViewModel viewModel;
    private FocusDao focusDao;
    private TextView tvMetricLeftValue;
    private TextView tvMetricRightValue;
    private TextView tvMetricRightSub;
    private TextView tvSuggestionText;
    private RoundedBarChart focusBarChart;
    private PieChart focusPieChart;
    private TextView tvPieHighlight;
    private TextView tvHistorySuggestion;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_stats_focus, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        focusDao = MindMintRoomDatabase.getInstance(requireContext()).focusDao();
        viewModel = new ViewModelProvider(requireActivity()).get(StatsViewModel.class);

        initViews(view);

        viewModel.getPeriodRange().observe(getViewLifecycleOwner(), this::updateStats);
    }

    private void initViews(View view) {
        View suggestionCard = view.findViewById(R.id.statsSuggestionCard);
        tvMetricLeftValue = view.findViewById(R.id.tvMetricLeftValue);
        tvMetricRightValue = view.findViewById(R.id.tvMetricRightValue);
        tvMetricRightSub = view.findViewById(R.id.tvMetricRightSub);
        tvSuggestionText = suggestionCard.findViewById(R.id.tvSuggestionText);
        focusBarChart = view.findViewById(R.id.focusBarChart);
        focusPieChart = view.findViewById(R.id.focusPieChart);
        tvPieHighlight = view.findViewById(R.id.tvPieHighlight);
        tvHistorySuggestion = view.findViewById(R.id.tvHistorySuggestion);
    }

    private void updateStats(StatsViewModel.PeriodRange range) {
        boolean isWeekly = Boolean.TRUE.equals(viewModel.getIsWeekly().getValue());

        long totalSeconds = getFocusVolume(range.startMs, range.endMs);

        tvMetricLeftValue.setText(String.format(Locale.getDefault(), "%.1f", totalSeconds / 3600f));
        TextView tvMetricLeftSub = getView().findViewById(R.id.tvMetricLeftSub);
        if (tvMetricLeftSub == null) tvMetricLeftSub = getView().findViewById(R.id.tvMetricLeftSub);
        tvMetricLeftSub.setText(isWeekly ? "This Week" : "This Month");

        // RIGHT: Peak Time
        calculateBestFocusTime();

        // Bar Chart
        updateFocusBarChart(range);

        // Pie Chart
        updateFocusTopicsChart(range);

        // Suggestion
        generateFocusSuggestion();
    }

    private long getFocusVolume(long start, long end) {
        List<FocusSessionEntity> list = focusDao.getSessionsInDateRange(start, end);
        long sum = 0;
        for (FocusSessionEntity e : list) sum += e.duration_ms / 1000;
        return sum;
    }

    private void calculateBestFocusTime() {
        Calendar cal = Calendar.getInstance();
        long end = cal.getTimeInMillis();
        cal.add(Calendar.MONTH, -1);
        long start = cal.getTimeInMillis();

        List<FocusSessionEntity> sessions = focusDao.getSessionsInDateRange(start, end);
        int[] hourCounts = new int[24];

        for (FocusSessionEntity s : sessions) {
            cal.setTimeInMillis(s.start_time_ms);
            int h = cal.get(Calendar.HOUR_OF_DAY);
            hourCounts[h]++;
        }

        int bestHour = 0;
        int max = 0;
        for (int i = 0; i < 24; i++) {
            if (hourCounts[i] > max) {
                max = hourCounts[i];
                bestHour = i;
            }
        }

        if (max == 0) {
            tvMetricRightValue.setText("--");
            tvMetricRightSub.setText("No data");
        } else {
            String startStr = formatHour(bestHour);
            String endStr = formatHour((bestHour + 1) % 24);
            tvMetricRightValue.setText(startStr);
            tvMetricRightSub.setText("to " + endStr);
        }
    }

    private String formatHour(int hour) {
        int h = hour % 12;
        if (h == 0)
            h = 12;
        return h + (hour < 12 ? "am" : "pm");
    }

    private void updateFocusTopicsChart(StatsViewModel.PeriodRange range) {
        List<FocusSessionEntity> sessions = focusDao.getSessionsInDateRange(range.startMs, range.endMs);
        Map<String, Integer> topicMap = new HashMap<>();
        int totalMinutes = 0;
        String dominantTopic = "Untitled";
        int maxMinutes = -1;

        for (FocusSessionEntity s : sessions) {
            String topic = s.topic_name;
            if (topic == null || topic.trim().isEmpty()) {
                topic = "Untitled";
            }
            int mins = (int) (s.duration_ms / 60000);
            if (mins > 0) { // filter out < 1 min if deemed insignificant, or keep raw
                topicMap.put(topic, topicMap.getOrDefault(topic, 0) + mins);
                totalMinutes += mins;
            }
        }

        // Find dominant
        for (Map.Entry<String, Integer> entry : topicMap.entrySet()) {
            if (entry.getValue() > maxMinutes) {
                maxMinutes = entry.getValue();
                dominantTopic = entry.getKey();
            }
        }

        if (totalMinutes == 0) {
            focusPieChart.setVisibility(View.GONE);
            tvPieHighlight.setText("No Focus Data");
            tvHistorySuggestion.setText("Complete a focus session to see topic breakdown.");
            return;
        } else {
            focusPieChart.setVisibility(View.VISIBLE);
        }

        List<PieEntry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>(topicMap.keySet());
        List<Integer> colors = getPastelColors(labels.size());
        List<Integer> datasetColors = new ArrayList<>();

        for (int i = 0; i < labels.size(); i++) {
            String label = labels.get(i);
            int val = topicMap.get(label);
            entries.add(new PieEntry(val, label));
            datasetColors.add(colors.get(i));
        }

        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setColors(datasetColors);
        dataSet.setDrawValues(false); // No text on chart as requested
        dataSet.setSliceSpace(0f); // Make it solid

        PieData data = new PieData(dataSet);
        focusPieChart.setData(data);
        focusPieChart.getDescription().setEnabled(false);
        focusPieChart.getLegend().setEnabled(true);
        focusPieChart.getLegend().setTextColor(getAttrColor(R.attr.text_secondary));
        focusPieChart.getLegend().setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);
        focusPieChart.getLegend().setWordWrapEnabled(true);
        focusPieChart.setDrawHoleEnabled(true);
        focusPieChart.setHoleColor(Color.TRANSPARENT);
        focusPieChart.setHoleRadius(55f);
        focusPieChart.setTransparentCircleRadius(55f);
        focusPieChart.setDrawEntryLabels(true);
        focusPieChart.setEntryLabelColor(Color.WHITE);
        focusPieChart.setEntryLabelTextSize(10f);
        focusPieChart.setTouchEnabled(false);
        focusPieChart.animateY(800);
        focusPieChart.invalidate();
        int pct = totalMinutes > 0 ? (maxMinutes * 100 / totalMinutes) : 0;
        tvPieHighlight.setText(dominantTopic + " (" + pct + "%)");
        tvHistorySuggestion.setText("You spend most of your focus time on " + dominantTopic + ".");
    }

    private List<Integer> getPastelColors(int count) {
        List<Integer> colors = new ArrayList<>();
        // 6 Pastel Colors
        int[] preset = {
                Color.parseColor("#FFB3BA"), // Paste Red
                Color.parseColor("#FFDFBA"), // Pastel Orange
                Color.parseColor("#FFFFBA"), // Pastel Yellow
                Color.parseColor("#BAFFC9"), // Pastel Green
                Color.parseColor("#BAE1FF"), // Pastel Blue
                Color.parseColor("#E6B3FF") // Pastel Purple
        };

        for (int i = 0; i < count; i++) {
            if (i < preset.length) {
                colors.add(preset[i]);
            } else {
                float hue = (i * 137.508f) % 360;
                colors.add(Color.HSVToColor(new float[] { hue, 0.4f, 0.95f }));
            }
        }
        return colors;
    }

    private void updateFocusBarChart(StatsViewModel.PeriodRange range) {
        List<BarEntry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();

        List<StatsViewModel.PeriodSlot> slots = range.slots;
        for (int i = 0; i < slots.size(); i++) {
            StatsViewModel.PeriodSlot slot = slots.get(i);
            long secs = getFocusVolume(slot.startMs, slot.endMs);
            entries.add(new BarEntry(i, secs / 3600f));
            labels.add(slot.label);
        }

        setupBarChart(focusBarChart, entries, labels, getAttrColor(R.attr.brand_pink));
    }

    private void generateFocusSuggestion() {
        Calendar cal = Calendar.getInstance();
        long end = cal.getTimeInMillis();
        cal.add(Calendar.WEEK_OF_YEAR, -1);
        long start = cal.getTimeInMillis();

        List<FocusSessionEntity> sessions = focusDao.getSessionsInDateRange(start, end);
        long morningSeconds = 0, totalS = 0;

        for (FocusSessionEntity s : sessions) {
            cal.setTimeInMillis(s.start_time_ms);
            int h = cal.get(Calendar.HOUR_OF_DAY);
            totalS += s.duration_ms / 1000;
            if (h < 11)
                morningSeconds += s.duration_ms / 1000;
        }

        if (totalS > 0) {
            int morningPct = (int) (morningSeconds * 100 / totalS);
            if (morningPct < 30) {
                tvSuggestionText.setText("Only " + morningPct + "% of focus happens before 11am. Schedule deep work earlier for peak performance.");
            } else {
                tvSuggestionText.setText("Your morning focus is strong at " + morningPct + "%. Keep leveraging early hours.");
            }
        } else {
            tvSuggestionText.setText("Start a focus session to see insights about your productivity patterns.");
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
