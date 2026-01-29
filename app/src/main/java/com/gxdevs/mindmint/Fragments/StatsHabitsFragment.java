package com.gxdevs.mindmint.Fragments;

import android.graphics.Color;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.gxdevs.mindmint.Views.HabitHeatmapView;
import com.gxdevs.mindmint.R;
import com.gxdevs.mindmint.Utils.StreakManager;
import com.gxdevs.mindmint.Models.StatsViewModel;
import com.gxdevs.mindmint.db.MindMintRoomDatabase;
import com.gxdevs.mindmint.db.dao.HabitCompletionDao;
import com.gxdevs.mindmint.db.dao.HabitDao;
import com.gxdevs.mindmint.db.models.DateCount;
import com.gxdevs.mindmint.db.models.DayPartCount;
import com.gxdevs.mindmint.db.models.EmotionCount;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class StatsHabitsFragment extends Fragment {

    private StatsViewModel viewModel;
    private HabitDao habitDao;
    private HabitCompletionDao habitCompletionDao;
    private StreakManager streakManager;
    private TextView tvMetricLeftValue;
    private TextView tvMetricRightValue;
    private TextView tvMetricRightSub;
    private TextView tvSuggestionText;
    private HabitHeatmapView globalHeatmap;
    private PieChart habitPieChart;
    private TextView tvHabitTimeInsight;
    private LineChart habitLineChart;
    private PieChart globalEmotionChart;
    private TextView globalAverageMoodValue;
    private TextView globalEmotionInsight;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_stats_habits, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        MindMintRoomDatabase db = MindMintRoomDatabase.getInstance(requireContext());
        habitDao = db.habitDao();
        habitCompletionDao = db.habitCompletionDao();
        streakManager = new StreakManager(requireContext());

        viewModel = new ViewModelProvider(requireActivity()).get(StatsViewModel.class);

        initViews(view);

        viewModel.getPeriodRange().observe(getViewLifecycleOwner(), range -> {
            if (range != null) {
                updateStats(range);
            }
        });
    }

    private void initViews(View view) {
        View suggestionCard = view.findViewById(R.id.statsSuggestionCard);
        tvMetricLeftValue = view.findViewById(R.id.tvMetricLeftValue);
        tvMetricRightValue = view.findViewById(R.id.tvMetricRightValue);
        tvMetricRightSub = view.findViewById(R.id.tvMetricRightSub);
        tvSuggestionText = suggestionCard.findViewById(R.id.tvSuggestionText);
        globalHeatmap = view.findViewById(R.id.globalHeatmap);
        habitPieChart = view.findViewById(R.id.habitPieChart);
        tvHabitTimeInsight = view.findViewById(R.id.tvHabitTimeInsight);
        habitLineChart = view.findViewById(R.id.habitLineChart);
        globalEmotionChart = view.findViewById(R.id.globalEmotionChart);
        globalAverageMoodValue = view.findViewById(R.id.globalAverageMoodValue);
        globalEmotionInsight = view.findViewById(R.id.globalEmotionInsight);
    }

    private void updateStats(StatsViewModel.PeriodRange range) {
        boolean isWeekly = Boolean.TRUE.equals(viewModel.getIsWeekly().getValue());

        // 1. Streak
        int streak = streakManager.getGlobalStreak();
        tvMetricLeftValue.setText(String.valueOf(streak));

        // 2. Completed Volume
        int volume = habitCompletionDao.getAllCompletionsVolumeInRange(range.startMs, range.endMs);
        tvMetricRightValue.setText(String.valueOf(volume));
        tvMetricRightSub.setText(isWeekly ? "This Week" : "This Month");

        // 3. Heatmap
        updateGlobalHeatmap();

        // 4. Time Breakdown
        updateHabitTimeBreakdown(range.startMs, range.endMs);

        // 5. Trend Chart
        updateHabitTrendChart(isWeekly);

        // 6. Emotion
        updateGlobalEmotionStats(range.startMs, range.endMs);

        // 7. Suggestion
        generateHabitSuggestion();
    }

    private void updateGlobalHeatmap() {
        Calendar cal = Calendar.getInstance();
        int year = cal.get(Calendar.YEAR);
        globalHeatmap.setYear(year);

        cal.set(Calendar.DAY_OF_YEAR, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long startMs = cal.getTimeInMillis();
        cal.set(Calendar.MONTH, 11);
        cal.set(Calendar.DAY_OF_MONTH, 31);
        long endMs = cal.getTimeInMillis();

        List<DateCount> dailyUniques = habitCompletionDao.getUniqueHabitsCountPerDay(startMs, endMs);
        Map<Long, Integer> heatmapData = new HashMap<>();

        for (DateCount dc : dailyUniques) {
            long dateMs = dc.date_ms;
            heatmapData.put(dateMs, 5);
        }

        globalHeatmap.setRawLevelData(heatmapData);
    }

    private void updateHabitTimeBreakdown(long startMs, long endMs) {
        List<DayPartCount> parts = habitCompletionDao.getAllCompletionsByDayPartInRange(startMs, endMs);
        Map<String, Integer> map = new HashMap<>();
        int total = 0;
        String dominant = "MORNING";
        int max = -1;

        for (DayPartCount p : parts) {
            map.put(p.day_part, p.count);
            total += p.count;
            if (p.count > max) {
                max = p.count;
                dominant = p.day_part;
            }
        }

        int morning = map.getOrDefault("MORNING", 0);
        int afternoon = map.getOrDefault("AFTERNOON", 0);
        int evening = map.getOrDefault("EVENING", 0);
        int night = map.getOrDefault("LATE_NIGHT", 0);
        int pm = afternoon + evening + night;

        List<PieEntry> entries = new ArrayList<>();
        if (morning > 0)
            entries.add(new PieEntry(morning, "Morning"));
        if (pm > 0)
            entries.add(new PieEntry(pm, "Afternoon"));

        if (entries.isEmpty())
            entries.add(new PieEntry(1, "No Data"));

        PieDataSet set = new PieDataSet(entries, "");
        set.setColors(getAttrColor(R.attr.brand_pink), getAttrColor(R.attr.text_tertiary));
        set.setDrawValues(false);
        habitPieChart.setData(new PieData(set));
        habitPieChart.setHoleColor(Color.TRANSPARENT);
        habitPieChart.setHoleRadius(55f);
        habitPieChart.getDescription().setEnabled(false);
        habitPieChart.getLegend().setEnabled(false);
        habitPieChart.setTouchEnabled(false);
        habitPieChart.setDrawEntryLabels(false);
        habitPieChart.animateY(600);
        habitPieChart.invalidate();

        String partName = getDayPartName(dominant);
        int pct = total > 0 ? (max * 100 / total) : 0;
        String text = "You mostly complete habits in the " + partName + " (" + pct + "%).";

        SpannableString ss = new SpannableString(text);
        int start = text.indexOf(partName);
        if (start != -1) {
            ss.setSpan(new ForegroundColorSpan(getAttrColor(R.attr.brand_pink)), start, start + partName.length(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            ss.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), start, start + partName.length(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        tvHabitTimeInsight.setText(ss);
    }

    private String getDayPartName(String code) {
        switch (code) {
            case "AFTERNOON":
                return "Afternoon";
            case "EVENING":
                return "Evening";
            case "LATE_NIGHT":
                return "Night";
            default:
                return "Morning";
        }
    }

    private void updateHabitTrendChart(boolean isWeekly) {
        setupLineChart(habitLineChart);

        List<Entry> entries = new ArrayList<>();
        int periods = isWeekly ? 8 : 6;
        Calendar cal = Calendar.getInstance();

        for (int i = periods - 1; i >= 0; i--) {
            Calendar pStart = (Calendar) cal.clone();
            Calendar pEnd = (Calendar) cal.clone();

            if (isWeekly) {
                pStart.add(Calendar.WEEK_OF_YEAR, -i);
                pStart.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
                pStart.set(Calendar.HOUR_OF_DAY, 0);
                pStart.set(Calendar.MINUTE, 0);
                pStart.set(Calendar.SECOND, 0);
                pStart.set(Calendar.MILLISECOND, 0);

                pEnd.setTimeInMillis(pStart.getTimeInMillis());
                pEnd.add(Calendar.DAY_OF_YEAR, 7);
            } else {
                pStart.add(Calendar.MONTH, -i);
                pStart.set(Calendar.DAY_OF_MONTH, 1);
                pStart.set(Calendar.HOUR_OF_DAY, 0);
                pStart.set(Calendar.MINUTE, 0);
                pStart.set(Calendar.SECOND, 0);
                pStart.set(Calendar.MILLISECOND, 0);

                pEnd.setTimeInMillis(pStart.getTimeInMillis());
                pEnd.add(Calendar.MONTH, 1);
            }

            int val = habitCompletionDao.getAllCompletionsVolumeInRange(pStart.getTimeInMillis(),
                    pEnd.getTimeInMillis());
            entries.add(new Entry(periods - 1 - i, val));
        }

        LineDataSet set = new LineDataSet(entries, "Habits");
        styleLineDataSet(set, getAttrColor(R.attr.brand_pink));
        habitLineChart.setData(new LineData(set));
        habitLineChart.animateX(800);
    }

    private void generateHabitSuggestion() {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.WEEK_OF_YEAR, -1);
        int saturdayMissed = 0, sundayMissed = 0;

        for (int i = 0; i < 7; i++) {
            int dow = cal.get(Calendar.DAY_OF_WEEK);
            long dayStart = cal.getTimeInMillis();
            long dayEnd = dayStart + 86400000;
            int active = habitDao.getCountActiveHabitsBeforeDate(dayEnd);
            int completed = habitCompletionDao.getUniqueHabitsCompletedForDate(dayStart, dayEnd);

            if (dow == Calendar.SATURDAY && completed < active)
                saturdayMissed++;
            if (dow == Calendar.SUNDAY && completed < active)
                sundayMissed++;
            cal.add(Calendar.DAY_OF_YEAR, 1);
        }

        if (saturdayMissed > 0 || sundayMissed > 0) {
            String days = saturdayMissed > 0 && sundayMissed > 0 ? "weekends"
                    : saturdayMissed > 0 ? "Saturdays" : "Sundays";
            tvSuggestionText.setText("You missed habits last " + days + ". Try setting smaller goals on those days.");
        } else {
            tvSuggestionText.setText("Great consistency! Keep up the momentum.");
        }
    }

    private void updateGlobalEmotionStats(long startMs, long endMs) {
        List<EmotionCount> emotions = habitCompletionDao.getGlobalEmotionCounts(startMs, endMs);

        String[] labels = new String[] { "Proud", "Motivated", "Satisfied", "Neutral", "Relieved", "Tired" };
        Map<String, Integer> counts = new HashMap<>();
        for (String label : labels)
            counts.put(label, 0);

        int totalCount = 0;
        int weightedScore = 0;

        for (EmotionCount ec : emotions) {
            String key = null;
            int score = 0;
            if (ec.emotion.contains("Proud")) {
                key = "Proud";
                score = 2;
            } else if (ec.emotion.contains("Motivated")) {
                key = "Motivated";
                score = 2;
            } else if (ec.emotion.contains("Satisfied")) {
                key = "Satisfied";
                score = 1;
            } else if (ec.emotion.contains("Relieved")) {
                key = "Relieved";
                score = -1;
            } else if (ec.emotion.contains("Tired")) {
                key = "Tired";
                score = -2;
            } else if (ec.emotion.contains("Neutral")) {
                key = "Neutral";
            }

            if (key != null) {
                counts.put(key, counts.get(key) + ec.count);
                totalCount += ec.count;
                weightedScore += (ec.count * score);
            }
        }

        if (totalCount == 0) {
            globalEmotionChart.setVisibility(View.GONE);
            globalAverageMoodValue.setVisibility(View.GONE);
            globalEmotionInsight.setText("Log some emotions for stats");
            globalEmotionInsight.setVisibility(View.VISIBLE);
            return;
        }

        globalEmotionChart.setVisibility(View.VISIBLE);
        globalAverageMoodValue.setVisibility(View.VISIBLE);
        globalEmotionInsight.setVisibility(View.VISIBLE);

        List<PieEntry> entries = new ArrayList<>();
        List<Integer> colors = new ArrayList<>();
        List<Integer> pastelColors = getPastelColors(labels.length);

        for (int i = 0; i < labels.length; i++) {
            String label = labels[i];
            int count = counts.get(label);
            if (count > 0) {
                entries.add(new PieEntry(count, label));
                colors.add(pastelColors.get(i));
            }
        }

        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setColors(colors);
        dataSet.setDrawValues(false);
        dataSet.setValueTextSize(12f);
        dataSet.setValueTextColor(Color.WHITE);

        PieData data = new PieData(dataSet);
        globalEmotionChart.setData(data);
        globalEmotionChart.getDescription().setEnabled(false);
        globalEmotionChart.getLegend().setEnabled(true);
        globalEmotionChart.getLegend().setTextColor(getAttrColor(R.attr.text_secondary));
        globalEmotionChart.getLegend().setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);
        globalEmotionChart.getLegend().setWordWrapEnabled(true);
        globalEmotionChart.setDrawHoleEnabled(true);
        globalEmotionChart.setHoleColor(Color.TRANSPARENT);
        globalEmotionChart.setHoleRadius(70f);
        globalEmotionChart.setTransparentCircleRadius(55f);
        globalEmotionChart.setDrawEntryLabels(true);
        globalEmotionChart.setEntryLabelColor(Color.WHITE);
        globalEmotionChart.setEntryLabelTextSize(10f);

        globalEmotionChart.animateY(800);
        globalEmotionChart.invalidate();

        float average = 0;
        if (totalCount > 0) {
            average = (float) weightedScore / totalCount;
        }

        updateGlobalAIAnalytics(average, totalCount);
    }

    private void updateGlobalAIAnalytics(float average, int totalCount) {
        String sign = average > 0 ? "+" : "";
        String avgStr = String.format(Locale.getDefault(), "%.1f", average);
        globalAverageMoodValue.setText("Average Mood: " + sign + avgStr);

        int color;
        if (average >= 1.0)
            color = R.color.brand_pink;
        else if (average >= 0.0)
            color = R.color.status_emerald;
        else if (average >= -1.0)
            color = R.color.text_tertiary;
        else
            color = R.color.status_rose;

        globalAverageMoodValue.setTextColor(ContextCompat.getColor(requireContext(), color));

        String insight = getString(average);
        globalEmotionInsight.setText(insight);
    }

    @NonNull
    private static String getString(float average) {
        String insight;
        if (average >= 1.5) {
            insight = "✨ You're feeling fantastic! Your positive momentum is strong. Keep capitalizing on this energy!";
        } else if (average >= 0.5) {
            insight = "😊 Generally positive vibes. You are building healthy habits with good satisfaction.";
        } else if (average >= -0.5) {
            insight = "😐 You are in a neutral zone. This is good for stability, but try adding some excitement.";
        } else if (average >= -1.5) {
            insight = "😌 You seem a bit relieved or tired. Ensure you are getting enough rest after your habits.";
        } else {
            insight = "😴 High fatigue detected. Don't push too hard; sustainable habits require recovery.";
        }
        return insight;
    }

    private List<Integer> getPastelColors(int count) {
        List<Integer> colors = new ArrayList<>();
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

    // Helpers
    private int getAttrColor(int attr) {
        TypedValue typedValue = new TypedValue();
        requireActivity().getTheme().resolveAttribute(attr, typedValue, true);
        return typedValue.data;
    }

    private void setupLineChart(LineChart chart) {
        chart.getDescription().setEnabled(false);
        chart.getLegend().setEnabled(false);
        chart.getAxisRight().setEnabled(false);
        chart.getAxisLeft().setDrawGridLines(true);
        chart.getAxisLeft().setGridColor(getAttrColor(R.attr.glass_stroke));
        chart.getAxisLeft().setTextColor(getAttrColor(R.attr.text_tertiary));
        chart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        chart.getXAxis().setDrawGridLines(false);
        chart.getXAxis().setTextColor(getAttrColor(R.attr.text_tertiary));
        chart.setTouchEnabled(false);
    }

    private void styleLineDataSet(LineDataSet set, int color) {
        set.setColor(color);
        set.setLineWidth(2f);
        set.setDrawCircles(true);
        set.setCircleColor(color);
        set.setCircleRadius(4f);
        set.setDrawValues(false);
        set.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        set.setDrawFilled(true);
        set.setFillColor(color);
        set.setFillAlpha(50);
    }
}
