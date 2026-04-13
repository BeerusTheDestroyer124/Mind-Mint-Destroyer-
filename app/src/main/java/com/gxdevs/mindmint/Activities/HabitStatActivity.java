package com.gxdevs.mindmint.Activities;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.charts.RadarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.data.RadarData;
import com.github.mikephil.charting.data.RadarDataSet;
import com.github.mikephil.charting.data.RadarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.gxdevs.mindmint.Adapters.CalendarDayAdapter;
import com.gxdevs.mindmint.R;
import com.gxdevs.mindmint.Utils.HabitManager;
import com.gxdevs.mindmint.Utils.StatsManager;
import com.gxdevs.mindmint.Views.HabitHeatmapView;
import com.gxdevs.mindmint.db.MindMintRoomDatabase;
import com.gxdevs.mindmint.db.dao.HabitCompletionDao;
import com.gxdevs.mindmint.db.dao.HabitDao;
import com.gxdevs.mindmint.db.entities.HabitCompletionEntity;
import com.gxdevs.mindmint.db.entities.HabitEntity;
import com.gxdevs.mindmint.db.models.DayOfWeekCount;
import com.gxdevs.mindmint.db.models.DayPartCount;
import com.gxdevs.mindmint.db.models.EmotionCount;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class HabitStatActivity extends AppCompatActivity implements CalendarDayAdapter.OnDayClickListener {

    public static final String EXTRA_HABIT_ID = "habit_id";

    private String habitId;
    private HabitEntity habit;
    private HabitCompletionDao completionDao;
    private StatsManager statsManager;
    private HabitManager habitManager;
    private int calendarYear;
    private int calendarMonth;
    private CalendarDayAdapter calendarAdapter;
    private boolean isWeeklyTrend = true;
    private boolean isEmotionWeekly = true;
    private ImageView habitIcon;
    private TextView habitName;
    private TextView habitGoalBadge;
    private TextView currentStreakValue;
    private TextView bestStreakValue;
    private TextView totalCompletionsValue;
    private TextView mostActiveDay;
    private LinearLayout weeklyBarsContainer;
    private PieChart timeBreakdownChart;
    private TextView timeBreakdownInsight;
    private ImageView dominantDayPartIcon;
    private TextView dominantDayPartLabel;
    private TextView calendarMonthLabel;
    private HabitHeatmapView heatmapView;
    private LineChart trendLineChart;
    private TextView trendInsight;
    private TextView trendWeeklyTab;
    private TextView trendMonthlyTab;

    private View emotionRadarCard;
    private RadarChart emotionRadarChart;
    private TextView emotionWeeklyTab;
    private TextView emotionMonthlyTab;
    private TextView emotionInsight;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_habit_stat);

        // Get habit ID from intent
        habitId = getIntent().getStringExtra(EXTRA_HABIT_ID);
        if (habitId == null) {
            finish();
            return;
        }

        // Initialize database
        MindMintRoomDatabase db = MindMintRoomDatabase.getInstance(this);
        HabitDao habitDao = db.habitDao();
        completionDao = db.habitCompletionDao();
        statsManager = new StatsManager(this);
        habitManager = new com.gxdevs.mindmint.Utils.HabitManager(this);

        // Load habit
        habit = habitDao.getById(habitId);
        if (habit == null) {
            finish();
            return;
        }

        initViews();
        setupListeners();
    }

    @Override
    protected void onResume() {
        super.onResume();
        reloadHabitData();
    }

    private void reloadHabitData() {
        MindMintRoomDatabase db = MindMintRoomDatabase.getInstance(this);
        HabitDao habitDao = db.habitDao();
        habit = habitDao.getById(habitId);
        if (habit == null) {
            finish();
            return;
        }
        loadData();
    }

    private void initViews() {
        habitIcon = findViewById(R.id.habitIcon);
        habitName = findViewById(R.id.habitName);
        habitGoalBadge = findViewById(R.id.habitGoalBadge);
        currentStreakValue = findViewById(R.id.currentStreakValue);
        bestStreakValue = findViewById(R.id.bestStreakValue);
        totalCompletionsValue = findViewById(R.id.totalCompletionsValue);
        mostActiveDay = findViewById(R.id.mostActiveDay);
        weeklyBarsContainer = findViewById(R.id.weeklyBarsContainer);
        timeBreakdownChart = findViewById(R.id.timeBreakdownChart);
        timeBreakdownInsight = findViewById(R.id.timeBreakdownInsight);
        dominantDayPartIcon = findViewById(R.id.dominantDayPartIcon);
        dominantDayPartLabel = findViewById(R.id.dominantDayPartLabel);
        calendarMonthLabel = findViewById(R.id.calendarMonthLabel);
        RecyclerView calendarGrid = findViewById(R.id.calendarGrid);
        heatmapView = findViewById(R.id.heatmapView);
        trendLineChart = findViewById(R.id.trendLineChart);
        trendInsight = findViewById(R.id.trendInsight);
        trendWeeklyTab = findViewById(R.id.trendWeeklyTab);
        trendMonthlyTab = findViewById(R.id.trendMonthlyTab);

        emotionRadarCard = findViewById(R.id.emotionRadarCard);
        emotionRadarChart = findViewById(R.id.emotionRadarChart);
        emotionWeeklyTab = findViewById(R.id.emotionWeeklyTab);
        emotionMonthlyTab = findViewById(R.id.emotionMonthlyTab);
        emotionInsight = findViewById(R.id.emotionInsight);

        // Calendar adapter
        calendarAdapter = new CalendarDayAdapter(this);
        calendarGrid.setAdapter(calendarAdapter);

        // Set current month
        Calendar cal = Calendar.getInstance();
        calendarYear = cal.get(Calendar.YEAR);
        calendarMonth = cal.get(Calendar.MONTH);
    }

    private void setupListeners() {
        findViewById(R.id.editButton).setOnClickListener(v -> {
            Intent intent = new Intent(this, HabitCreateActivity.class);
            intent.putExtra(HabitCreateActivity.EXTRA_HABIT_ID, habitId);
            startActivity(intent);
        });

        findViewById(R.id.calendarPrevMonth).setOnClickListener(v -> {
            calendarMonth--;
            if (calendarMonth < 0) {
                calendarMonth = 11;
                calendarYear--;
            }
            updateCalendar();
        });

        findViewById(R.id.calendarNextMonth).setOnClickListener(v -> {
            calendarMonth++;
            if (calendarMonth > 11) {
                calendarMonth = 0;
                calendarYear++;
            }
            updateCalendar();
        });

        trendWeeklyTab.setOnClickListener(v -> {
            if (!isWeeklyTrend) {
                isWeeklyTrend = true;
                updateTrendTabs();
                updateTrendChart();
            }
        });

        trendMonthlyTab.setOnClickListener(v -> {
            if (isWeeklyTrend) {
                isWeeklyTrend = false;
                updateTrendTabs();
                updateTrendChart();
            }
        });

        emotionWeeklyTab.setOnClickListener(v -> {
            if (!isEmotionWeekly) {
                isEmotionWeekly = true;
                updateEmotionTrendTabs();
                updateEmotionRadar();
            }
        });

        emotionMonthlyTab.setOnClickListener(v -> {
            if (isEmotionWeekly) {
                isEmotionWeekly = false;
                updateEmotionTrendTabs();
                updateEmotionRadar();
            }
        });

        // Long-press on habit name to load demo data (for testing)
        habitName.setOnLongClickListener(v -> {
            loadDemoData();
            return true;
        });
    }

    private void loadDemoData() {
        currentStreakValue.setText("23");
        bestStreakValue.setText("45");
        totalCompletionsValue.setText("342");
        habitGoalBadge.setText("Daily Goal: 8 glasses");
        habitGoalBadge.setVisibility(View.VISIBLE);

        // Demo weekly rhythm
        mostActiveDay.setText("Most active on Tuesdays");
        weeklyBarsContainer.removeAllViews();
        int[] demoCounts = {3, 5, 8, 6, 7, 4, 2};
        int maxCount = 8;
        for (int i = 0; i < 7; i++) {
            View bar = new View(this);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, 0, 1f);
            params.setMargins(dpToPx(3), 0, dpToPx(3), 0);
            float fillRatio = (float) demoCounts[i] / maxCount;
            params.height = (int) (dpToPx(60) * Math.max(0.1f, fillRatio));
            params.gravity = android.view.Gravity.BOTTOM;
            bar.setLayoutParams(params);
            bar.setBackgroundResource(i == 2 ? R.drawable.bg_heatmap_level_4 : R.drawable.bg_heatmap_level_2);
            weeklyBarsContainer.addView(bar);
        }

        // Demo pie chart
        List<PieEntry> entries = new ArrayList<>();
        entries.add(new PieEntry(40, "Morning"));
        entries.add(new PieEntry(20, "Afternoon"));
        entries.add(new PieEntry(30, "Evening"));
        entries.add(new PieEntry(10, "Night"));
        PieDataSet dataSet = new PieDataSet(entries, "");

        List<Integer> demoColors = new ArrayList<>();
        int basePink = ContextCompat.getColor(this, R.color.brand_pink);
        demoColors.add(Color.argb(100, Color.red(basePink), Color.green(basePink), Color.blue(basePink))); // Morning
        demoColors.add(Color.argb(150, Color.red(basePink), Color.green(basePink), Color.blue(basePink))); // Afternoon
        demoColors.add(Color.argb(200, Color.red(basePink), Color.green(basePink), Color.blue(basePink))); // Evening
        demoColors.add(Color.argb(255, Color.red(basePink), Color.green(basePink), Color.blue(basePink))); // Night

        dataSet.setColors(demoColors);
        dataSet.setDrawValues(false);
        PieData data = new PieData(dataSet);
        timeBreakdownChart.setData(data);
        timeBreakdownChart.invalidate();

        dominantDayPartLabel.setText("Morning");
        timeBreakdownInsight.setText("You tend to complete this habit in the Morning (65%). Try to maintain this consistency.");

        List<Entry> trendEntries = new ArrayList<>();
        int[] demoVolumes = {4, 5, 3, 6, 7, 5, 8, 7};
        for (int i = 0; i < 8; i++) {
            trendEntries.add(new Entry(i, demoVolumes[i]));
        }
        LineDataSet lineDataSet = new LineDataSet(trendEntries, "");
        lineDataSet.setColor(ContextCompat.getColor(this, R.color.brand_pink));
        lineDataSet.setLineWidth(2f);
        lineDataSet.setDrawCircles(true);
        lineDataSet.setCircleColor(ContextCompat.getColor(this, R.color.brand_pink));
        lineDataSet.setCircleRadius(4f);
        lineDataSet.setDrawValues(false);
        lineDataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        lineDataSet.setDrawFilled(true);
        lineDataSet.setFillColor(ContextCompat.getColor(this, R.color.brand_pink));
        lineDataSet.setFillAlpha(50);
        trendLineChart.setData(new LineData(lineDataSet));
        trendLineChart.invalidate();
        trendInsight.setText("↗ You are doing 17% better than last week.");
        trendInsight.setTextColor(ContextCompat.getColor(this, R.color.status_emerald));

        // Demo heatmap - generate random completion data for past year
        Map<Long, Integer> demoHeatmapData = new HashMap<>();
        Calendar cal = Calendar.getInstance();
        java.util.Random rand = new java.util.Random(42);
        for (int i = 0; i < 365; i++) {
            cal.setTimeInMillis(System.currentTimeMillis());
            cal.add(Calendar.DAY_OF_YEAR, -i);
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            if (rand.nextFloat() < 0.7f) {
                demoHeatmapData.put(cal.getTimeInMillis(), rand.nextInt(8) + 1);
            }
        }
        heatmapView.setCompletionData(demoHeatmapData, 8, true, 1);

        // Demo calendar - mark some days as completed
        Set<Integer> completedDays = new HashSet<>();
        for (int d = 1; d <= 28; d++) {
            if (rand.nextFloat() < 0.6f) {
                completedDays.add(d);
            }
        }
        calendarAdapter.setMonth(calendarYear, calendarMonth, completedDays);

        // Demo emotion radar chart
        emotionRadarCard.setVisibility(View.VISIBLE);

        String[] demoLabels = new String[]{"Proud", "Motivated", "Satisfied", "Neutral", "Relieved", "Tired"};
        List<RadarEntry> demoRadarEntries = new ArrayList<>();
        // Mock values
        demoRadarEntries.add(new RadarEntry(8)); // Proud
        demoRadarEntries.add(new RadarEntry(9)); // Motivated
        demoRadarEntries.add(new RadarEntry(6)); // Satisfied
        demoRadarEntries.add(new RadarEntry(1)); // Neutral
        demoRadarEntries.add(new RadarEntry(4)); // Relieved
        demoRadarEntries.add(new RadarEntry(2)); // Tired

        RadarDataSet demoRadarDataSet = new RadarDataSet(demoRadarEntries, "Emotions");
        demoRadarDataSet.setColor(ContextCompat.getColor(this, R.color.brand_pink));
        demoRadarDataSet.setFillColor(ContextCompat.getColor(this, R.color.brand_pink));
        demoRadarDataSet.setDrawFilled(true);
        demoRadarDataSet.setFillAlpha(50);
        demoRadarDataSet.setLineWidth(2f);
        demoRadarDataSet.setDrawHighlightCircleEnabled(true);
        demoRadarDataSet.setDrawHighlightIndicators(false);

        RadarData demoRadarData = new RadarData(demoRadarDataSet);
        demoRadarData.setValueTextSize(8f);
        demoRadarData.setDrawValues(false);
        demoRadarData.setValueTextColor(getAttrColor(R.attr.text_primary));

        emotionRadarChart.setData(demoRadarData);
        emotionRadarChart.getDescription().setEnabled(false);
        emotionRadarChart.getLegend().setEnabled(false);

        // Web lines styling
        emotionRadarChart.setWebLineWidth(1f);
        emotionRadarChart.setWebColor(getAttrColor(R.attr.text_tertiary));
        emotionRadarChart.setWebLineWidthInner(1f);
        emotionRadarChart.setWebColorInner(getAttrColor(R.attr.text_tertiary));
        emotionRadarChart.setWebAlpha(100);

        // X Axis (Labels)
        XAxis xAxis = emotionRadarChart.getXAxis();
        xAxis.setTextSize(10f);
        xAxis.setYOffset(0f);
        xAxis.setXOffset(0f);
        xAxis.setValueFormatter(new IndexAxisValueFormatter(demoLabels));
        xAxis.setTextColor(getAttrColor(R.attr.text_primary));

        // Y Axis
        emotionRadarChart.getYAxis().setLabelCount(5, false);
        emotionRadarChart.getYAxis().setTextSize(9f);
        emotionRadarChart.getYAxis().setAxisMinimum(0f);
        emotionRadarChart.getYAxis().setDrawLabels(false);

        emotionRadarChart.invalidate();

        emotionInsight.setText("✨ You're feeling mostly Motivated! This mimics the 'Winner Effect' – success breeds more success. Keep riding this wave!");

        Toast.makeText(this, "Demo data loaded", android.widget.Toast.LENGTH_SHORT).show();
    }

    private void loadData() {
        // Habit info
        habitName.setText(habit.name);

        // Goal badge
        if (habit.target_count > 1 && habit.target_unit != null && !habit.target_unit.isEmpty()) {
            habitGoalBadge.setText("Daily Goal: " + habit.target_count + " " + habit.target_unit);
            habitGoalBadge.setVisibility(View.VISIBLE);
        } else if ("DAILY".equals(habit.frequency_goal)) {
            habitGoalBadge.setText("Daily Habit");
            habitGoalBadge.setVisibility(View.VISIBLE);
        } else if ("TIMES_PER_WEEK".equals(habit.frequency_goal)) {
            habitGoalBadge.setText(habit.frequency_times_per_week + "x per week");
            habitGoalBadge.setVisibility(View.VISIBLE);
        } else {
            habitGoalBadge.setVisibility(View.GONE);
        }

        // Icon
        if (habit.icon_name != null && !habit.icon_name.isEmpty()) {
            int resId = getResources().getIdentifier(habit.icon_name, "drawable", getPackageName());
            if (resId != 0) {
                habitIcon.setImageResource(resId);
            }
        }
        if (habit.icon_tint != 0) {
            habitIcon.setColorFilter(habit.icon_tint);
        }

        int currentStreak = statsManager.calculateHabitStreak(habitId);
        int bestStreak = statsManager.getHighestStreakForHabit(habitId);
        currentStreakValue.setText(String.valueOf(currentStreak));
        bestStreakValue.setText(String.valueOf(bestStreak));
        int total = completionDao.getTotalCompletionCount(habitId);
        totalCompletionsValue.setText(String.valueOf(total));

        updateWeeklyRhythm();
        updateTimeBreakdown();
        updateCalendar();
        updateHeatmap();
        updateTrendChart();

        if (habit.is_ask_emotion) {
            emotionRadarCard.setVisibility(View.VISIBLE);
            updateEmotionRadar();
        } else {
            emotionRadarCard.setVisibility(View.GONE);
        }
    }

    private void updateWeeklyRhythm() {
        List<DayOfWeekCount> dayCounts = completionDao.getCompletionsByDayOfWeek(habitId);

        int[] counts = new int[7];
        int maxCount = 0;
        int minCount = Integer.MAX_VALUE;
        int maxDayIndex = 0;
        int minDayIndex = 0;

        for (DayOfWeekCount dc : dayCounts) {
            try {
                int day = Integer.parseInt(dc.day_of_week);
                counts[day] = dc.count;
            } catch (NumberFormatException ignored) {
            }
        }

        for (int i = 0; i < 7; i++) {
            if (counts[i] > maxCount) {
                maxCount = counts[i];
                maxDayIndex = i;
            }
            if (counts[i] < minCount) {
                minCount = counts[i];
                minDayIndex = i;
            }
        }

        if (maxCount == 0)
            maxCount = 1;

        String[] dayNames = {"Sundays", "Mondays", "Tuesdays", "Wednesdays", "Thursdays", "Fridays", "Saturdays"};

        boolean allEqual = true;
        for (int i = 1; i < 7; i++) {
            if (counts[i] != counts[0]) {
                allEqual = false;
                break;
            }
        }

        if (allEqual && counts[0] > 0) {
            // Perfect rhythm - all days equal
            mostActiveDay.setText("🔥 Perfect Weekly Rhythm!");
            mostActiveDay.setTextColor(ContextCompat.getColor(this, R.color.amber));
        } else {
            // Show best and worst day with highlights
            String bestDay = dayNames[maxDayIndex];
            String worstDay = dayNames[minDayIndex];
            String insightText = "Most consistent on " + bestDay + ". Watch out for " + worstDay + "—lowest day.";
            SpannableString spannable = new SpannableString(insightText);

            // Highlight best day in brand pink
            int bestStart = insightText.indexOf(bestDay);
            if (bestStart >= 0) {
                spannable.setSpan(
                        new ForegroundColorSpan(ContextCompat.getColor(this, R.color.brand_pink)),
                        bestStart, bestStart + bestDay.length(),
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                spannable.setSpan(
                        new StyleSpan(android.graphics.Typeface.BOLD),
                        bestStart, bestStart + bestDay.length(),
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }


            int worstStart = insightText.indexOf(worstDay);
            if (worstStart >= 0) {
                spannable.setSpan(
                        new ForegroundColorSpan(ContextCompat.getColor(this, R.color.status_rose)),
                        worstStart, worstStart + worstDay.length(),
                        android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                spannable.setSpan(
                        new StyleSpan(android.graphics.Typeface.BOLD),
                        worstStart, worstStart + worstDay.length(),
                        android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }

            mostActiveDay.setText(spannable);
            mostActiveDay.setTextColor(getAttrColor(R.attr.text_primary));
        }

        // Create bar views
        weeklyBarsContainer.removeAllViews();
        for (int i = 0; i < 7; i++) {
            View bar = new View(this);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, 0, 1f);
            params.setMargins(dpToPx(3), 0, dpToPx(3), 0);

            float fillRatio = (float) counts[i] / maxCount;
            params.height = (int) (dpToPx(60) * Math.max(0.1f, fillRatio));
            params.gravity = Gravity.BOTTOM;
            bar.setLayoutParams(params);

            if (i == maxDayIndex) {
                bar.setBackgroundResource(R.drawable.bg_heatmap_level_4);
            } else if (i == minDayIndex && !allEqual) {
                bar.setBackgroundResource(R.drawable.bg_heatmap_level_1);
            } else {
                bar.setBackgroundResource(R.drawable.bg_heatmap_level_2);
            }
            weeklyBarsContainer.addView(bar);
        }
    }

    private void updateTimeBreakdown() {
        List<DayPartCount> partCounts = completionDao.getCompletionsByDayPart(habitId);

        Map<String, Integer> countMap = new HashMap<>();
        countMap.put("MORNING", 0);
        countMap.put("AFTERNOON", 0);
        countMap.put("EVENING", 0);
        countMap.put("LATE_NIGHT", 0);

        int total = 0;
        String dominantPart = "MORNING";
        int dominantCount = 0;

        for (DayPartCount pc : partCounts) {
            if (pc.day_part != null) {
                countMap.put(pc.day_part, pc.count);
                total += pc.count;
                if (pc.count > dominantCount) {
                    dominantCount = pc.count;
                    dominantPart = pc.day_part;
                }
            }
        }

        List<PieEntry> entries = new ArrayList<>();
        List<Integer> colors = new ArrayList<>();
        int baseColor = getAttrColor(R.attr.brand_pink);

        String[] keys = {"MORNING", "AFTERNOON", "EVENING", "LATE_NIGHT"};
        int[] alphas = {100, 150, 200, 255};

        for (int i = 0; i < keys.length; i++) {
            int val = countMap.get(keys[i]);
            if (val > 0) {
                entries.add(new PieEntry(val));
                int shadedColor = Color.argb(alphas[i], Color.red(baseColor), Color.green(baseColor),
                        Color.blue(baseColor));
                colors.add(shadedColor);
            }
        }

        if (entries.isEmpty()) {
            entries.add(new PieEntry(1));
            colors.add(getAttrColor(R.attr.glass_circle));
        }

        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setColors(colors);
        dataSet.setDrawValues(false);

        PieData data = new PieData(dataSet);
        timeBreakdownChart.setData(data);
        timeBreakdownChart.setDrawHoleEnabled(true);
        timeBreakdownChart.setHoleColor(Color.TRANSPARENT);
        timeBreakdownChart.setHoleRadius(80f);
        timeBreakdownChart.setTransparentCircleRadius(75f);
        timeBreakdownChart.getLegend().setEnabled(false);
        timeBreakdownChart.getDescription().setEnabled(false);
        timeBreakdownChart.setDrawEntryLabels(false); // Hide entry labels
        timeBreakdownChart.setTouchEnabled(false);
        timeBreakdownChart.invalidate();

        // Update dominant part icon and label
        String partName;
        int iconRes;
        switch (dominantPart) {
            case "AFTERNOON":
                partName = "Afternoon";
                iconRes = R.drawable.clock;
                break;
            case "EVENING":
                partName = "Evening";
                iconRes = R.drawable.clock;
                break;
            case "LATE_NIGHT":
                partName = "Night";
                iconRes = R.drawable.clock;
                break;
            default:
                partName = "Morning";
                iconRes = R.drawable.clock;
        }
        dominantDayPartLabel.setText(partName);
        dominantDayPartIcon.setImageResource(iconRes);

        int percentage = total > 0 ? (dominantCount * 100 / total) : 0;
        String insightText = "You tend to complete this habit in the " + partName + " (" + percentage + "%). Try to maintain this consistency.";

        // Create SpannableString to highlight the day part name
        SpannableString spannable = new SpannableString(insightText);
        int startIdx = insightText.indexOf(partName);
        if (startIdx >= 0) {
            spannable.setSpan(
                    new ForegroundColorSpan(getAttrColor(R.attr.brand_pink)),
                    startIdx,
                    startIdx + partName.length(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            spannable.setSpan(
                    new StyleSpan(android.graphics.Typeface.BOLD),
                    startIdx,
                    startIdx + partName.length(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        timeBreakdownInsight.setText(spannable);
    }

    private void updateCalendar() {
        // Update month label
        Calendar cal = Calendar.getInstance();
        cal.set(calendarYear, calendarMonth, 1);
        SimpleDateFormat sdf = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
        calendarMonthLabel.setText(sdf.format(cal.getTime()));

        // Get completions for this month
        long monthStart = cal.getTimeInMillis();
        cal.add(Calendar.MONTH, 1);
        long monthEnd = cal.getTimeInMillis();

        List<HabitCompletionEntity> completions = completionDao.getByHabitIdAndDateRange(habitId, monthStart, monthEnd);

        Set<Integer> completedDays = new HashSet<>();
        Calendar dayCal = Calendar.getInstance();
        for (HabitCompletionEntity c : completions) {
            dayCal.setTimeInMillis(c.completion_date_ms);
            if (dayCal.get(Calendar.YEAR) == calendarYear &&
                    dayCal.get(Calendar.MONTH) == calendarMonth) {
                completedDays.add(dayCal.get(Calendar.DAY_OF_MONTH));
            }
        }

        calendarAdapter.setMonth(calendarYear, calendarMonth, completedDays);
    }

    private void updateHeatmap() {
        Calendar cal = Calendar.getInstance();
        int year = cal.get(Calendar.YEAR);
        heatmapView.setYear(year);

        cal.set(Calendar.DAY_OF_YEAR, 1);
        long startOfYear = cal.getTimeInMillis();

        cal.set(Calendar.MONTH, 11);
        cal.set(Calendar.DAY_OF_MONTH, 31);
        long endOfYear = cal.getTimeInMillis();

        List<HabitCompletionEntity> completions = completionDao.getByHabitIdAndDateRange(habitId, startOfYear, endOfYear);
        Map<Long, Integer> dailyCounts = getLongIntegerMap(completions);

        int oneTap = habit.one_tap_value > 0 ? habit.one_tap_value : 1;
        boolean isGoalShaded = habit.target_count > 1 && habit.target_count > oneTap;
        heatmapView.setCompletionData(dailyCounts, habit.target_count, isGoalShaded, oneTap);
    }

    @NonNull
    private static Map<Long, Integer> getLongIntegerMap(List<HabitCompletionEntity> completions) {
        Map<Long, Integer> dailyCounts = new HashMap<>();
        Calendar dayCal = Calendar.getInstance();

        for (HabitCompletionEntity c : completions) {
            dayCal.setTimeInMillis(c.completion_date_ms);
            dayCal.set(Calendar.HOUR_OF_DAY, 0);
            dayCal.set(Calendar.MINUTE, 0);
            dayCal.set(Calendar.SECOND, 0);
            dayCal.set(Calendar.MILLISECOND, 0);
            long dayMs = dayCal.getTimeInMillis();

            int prev = dailyCounts.getOrDefault(dayMs, 0);
            dailyCounts.put(dayMs, prev + c.count);
        }
        return dailyCounts;
    }

    private void updateTrendChart() {
        List<Entry> entries = new ArrayList<>();

        Calendar cal = Calendar.getInstance();
        int periods = isWeeklyTrend ? 8 : 6; // 8 weeks or 6 months
        int[] volumes = new int[periods];

        for (int i = periods - 1; i >= 0; i--) {
            Calendar periodStart = (Calendar) cal.clone();
            Calendar periodEnd = (Calendar) cal.clone();

            if (isWeeklyTrend) {
                periodStart.add(Calendar.WEEK_OF_YEAR, -i);
                periodStart.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
                periodEnd.setTimeInMillis(periodStart.getTimeInMillis());
                periodEnd.add(Calendar.DAY_OF_YEAR, 7);
            } else {
                periodStart.add(Calendar.MONTH, -i);
                periodStart.set(Calendar.DAY_OF_MONTH, 1);
                periodEnd.setTimeInMillis(periodStart.getTimeInMillis());
                periodEnd.add(Calendar.MONTH, 1);
            }

            periodStart.set(Calendar.HOUR_OF_DAY, 0);
            periodStart.set(Calendar.MINUTE, 0);
            periodEnd.set(Calendar.HOUR_OF_DAY, 0);
            periodEnd.set(Calendar.MINUTE, 0);

            int volume = completionDao.getVolumeInRange(habitId,
                    periodStart.getTimeInMillis(), periodEnd.getTimeInMillis());
            volumes[periods - 1 - i] = volume;
            entries.add(new Entry(periods - 1 - i, volume));
        }

        // Setup chart
        LineDataSet dataSet = new LineDataSet(entries, "");
        dataSet.setColor(ContextCompat.getColor(this, R.color.brand_pink));
        dataSet.setLineWidth(2f);
        dataSet.setDrawCircles(true);
        dataSet.setCircleColor(ContextCompat.getColor(this, R.color.brand_pink));
        dataSet.setCircleRadius(4f);
        dataSet.setDrawValues(false);
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        dataSet.setDrawFilled(true);
        dataSet.setFillColor(ContextCompat.getColor(this, R.color.brand_pink));
        dataSet.setFillAlpha(50);

        LineData lineData = new LineData(dataSet);
        trendLineChart.setData(lineData);
        trendLineChart.getDescription().setEnabled(false);
        trendLineChart.getLegend().setEnabled(false);
        trendLineChart.getAxisRight().setEnabled(false);
        trendLineChart.getAxisLeft().setTextColor(getAttrColor(R.attr.text_tertiary));
        trendLineChart.getAxisLeft().setGridColor(ContextCompat.getColor(this, R.color.glass_stroke));
        trendLineChart.getXAxis().setEnabled(false);
        trendLineChart.setTouchEnabled(false);
        trendLineChart.invalidate();

        // Update insight
        int current = volumes[volumes.length - 1];
        int previous = volumes[volumes.length - 2];

        if (previous > 0) {
            int changePercent = ((current - previous) * 100) / previous;
            String periodName = isWeeklyTrend ? "week" : "month";

            if (changePercent >= 0) {
                trendInsight.setText("↗ You are doing " + changePercent + "% better than last " + periodName + ".");
                trendInsight.setTextColor(ContextCompat.getColor(this, R.color.status_emerald));
                trendInsight.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_trending_up, 0, 0, 0);
            } else {
                trendInsight
                        .setText("↘ You are doing " + Math.abs(changePercent) + "% less than last " + periodName + ".");
                trendInsight.setTextColor(ContextCompat.getColor(this, R.color.status_rose));
                trendInsight.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_trending_down, 0, 0, 0);
            }
        } else {
            trendInsight.setText("Keep going to build your trend!");
            trendInsight.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
        }
    }

    private void updateTrendTabs() {
        if (isWeeklyTrend) {
            trendWeeklyTab.setBackgroundResource(R.drawable.bg_segment_selected);
            trendWeeklyTab.setTextColor(getAttrColor(R.attr.text_primary));
            trendMonthlyTab.setBackground(null);
            trendMonthlyTab.setTextColor(getAttrColor(R.attr.text_tertiary));
        } else {
            trendMonthlyTab.setBackgroundResource(R.drawable.bg_segment_selected);
            trendMonthlyTab.setTextColor(getAttrColor(R.attr.text_primary));
            trendWeeklyTab.setBackground(null);
            trendWeeklyTab.setTextColor(getAttrColor(R.attr.text_tertiary));
        }
    }

    @Override
    public void onDayClick(int dayOfMonth, boolean isCompleted) {
        // Calculate date of clicked day
        Calendar cal = Calendar.getInstance();
        cal.set(calendarYear, calendarMonth, dayOfMonth, 12, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);

        // Block future dates — user cannot mark a future day as completed
        Calendar today = Calendar.getInstance();
        today.set(Calendar.HOUR_OF_DAY, 23);
        today.set(Calendar.MINUTE, 59);
        today.set(Calendar.SECOND, 59);
        today.set(Calendar.MILLISECOND, 999);
        if (!isCompleted && cal.after(today)) {
            Toast.makeText(this, "Cannot mark future dates as completed", Toast.LENGTH_SHORT).show();
            return;
        }

        long todayMs = System.currentTimeMillis();
        long clickedMs = cal.getTimeInMillis();
        long diffMs = todayMs - clickedMs;
        long daysDiff = diffMs / (24 * 60 * 60 * 1000);

        boolean isOldHistory = daysDiff > 7;

        if (!isCompleted && isOldHistory) {
            new MaterialAlertDialogBuilder(this)
                    .setTitle("Update Old History?")
                    .setMessage("You are fulfilling a habit from more than 7 days ago. This will be saved to your history stats, but it will NOT repair your streak.")
                    .setPositiveButton("Mark Anyway", (dialog, which) -> {
                        toggleDayCompletion(dayOfMonth, false);
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
            return;
        }

        String message = isCompleted ? "Remove completion for day " + dayOfMonth + "?" : "Mark day " + dayOfMonth + " as completed?";
        new MaterialAlertDialogBuilder(this)
                .setTitle("Confirm " + (isCompleted ? "Removal" : "Completion"))
                .setMessage(message)
                .setPositiveButton(isCompleted ? "Remove" : "Complete", (dialog, which) -> toggleDayCompletion(dayOfMonth, isCompleted))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void toggleDayCompletion(int dayOfMonth, boolean wasCompleted) {
        Calendar cal = Calendar.getInstance();
        cal.set(calendarYear, calendarMonth, dayOfMonth, 12, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long dateMs = cal.getTimeInMillis();

        habitManager.toggleHistoryCompletion(habitId, dateMs, !wasCompleted);
        Toast.makeText(this, wasCompleted ? "Completion removed" : "Day completed!", Toast.LENGTH_SHORT).show();
        reloadHabitData();
    }

    private int dpToPx(int dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics());
    }

    private void updateEmotionTrendTabs() {
        if (isEmotionWeekly) {
            emotionWeeklyTab.setBackgroundResource(R.drawable.bg_segment_selected);
            emotionWeeklyTab.setTextColor(getAttrColor(R.attr.text_primary));
            emotionMonthlyTab.setBackground(null);
            emotionMonthlyTab.setTextColor(getAttrColor(R.attr.text_tertiary));
        } else {
            emotionMonthlyTab.setBackgroundResource(R.drawable.bg_segment_selected);
            emotionMonthlyTab.setTextColor(getAttrColor(R.attr.text_primary));
            emotionWeeklyTab.setBackground(null);
            emotionWeeklyTab.setTextColor(getAttrColor(R.attr.text_tertiary));
        }
    }

    private void updateEmotionRadar() {
        Calendar cal = Calendar.getInstance();

        // End: Today at 23:59:59 (End of active day)
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        cal.set(Calendar.MILLISECOND, 999);
        long endMs = cal.getTimeInMillis();
        long startMs;

        if (isEmotionWeekly) {
            // Last 7 Days (Start of day)
            Calendar startCal = (Calendar) cal.clone();
            startCal.add(Calendar.DAY_OF_YEAR, -6); // 6 days back + today = 7 days
            startCal.set(Calendar.HOUR_OF_DAY, 0);
            startCal.set(Calendar.MINUTE, 0);
            startCal.set(Calendar.SECOND, 0);
            startCal.set(Calendar.MILLISECOND, 0);
            startMs = startCal.getTimeInMillis();
        } else {
            // Last 30 Days (Start of day)
            Calendar startCal = (Calendar) cal.clone();
            startCal.add(Calendar.DAY_OF_YEAR, -29);
            startCal.set(Calendar.HOUR_OF_DAY, 0);
            startCal.set(Calendar.MINUTE, 0);
            startCal.set(Calendar.SECOND, 0);
            startCal.set(Calendar.MILLISECOND, 0);
            startMs = startCal.getTimeInMillis();
        }

        List<EmotionCount> emotions = completionDao.getEmotionCounts(habitId, startMs, endMs);

        // Define fixed emotions order (Matches StatsHabitsFragment)
        String[] labels = new String[]{"Proud", "Motivated", "Satisfied", "Neutral", "Relieved", "Tired"};
        Map<String, Integer> counts = new HashMap<>();
        for (String label : labels)
            counts.put(label, 0);

        int maxCount = 0;

        // Fill counts
        for (EmotionCount ec : emotions) {
            String key = getString(ec);

            if (key != null) {
                int val = counts.get(key) + ec.count;
                counts.put(key, val);
                if (val > maxCount)
                    maxCount = val;
            }
        }

        List<RadarEntry> entries = new ArrayList<>();
        for (String label : labels) {
            entries.add(new RadarEntry(counts.get(label)));
        }

        RadarDataSet dataSet = new RadarDataSet(entries, "Emotions");
        dataSet.setColor(ContextCompat.getColor(this, R.color.brand_pink));
        dataSet.setFillColor(ContextCompat.getColor(this, R.color.brand_pink));
        dataSet.setDrawFilled(true);
        dataSet.setFillAlpha(50);
        dataSet.setLineWidth(2f);
        dataSet.setDrawHighlightCircleEnabled(true);
        dataSet.setDrawHighlightIndicators(false);

        RadarData data = new RadarData(dataSet);
        data.setValueTextSize(8f);
        data.setDrawValues(false);
        data.setValueTextColor(getAttrColor(R.attr.text_primary));

        emotionRadarChart.setData(data);
        emotionRadarChart.getDescription().setEnabled(false);
        emotionRadarChart.getLegend().setEnabled(false);

        // Web lines
        emotionRadarChart.setWebLineWidth(1f);
        emotionRadarChart.setWebColor(getAttrColor(R.attr.text_tertiary));
        emotionRadarChart.setWebLineWidthInner(1f);
        emotionRadarChart.setWebColorInner(getAttrColor(R.attr.text_tertiary));
        emotionRadarChart.setWebAlpha(100);

        // X Axis (Labels)
        XAxis xAxis = emotionRadarChart.getXAxis();
        xAxis.setTextSize(10f);
        xAxis.setYOffset(0f);
        xAxis.setXOffset(0f);
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setTextColor(getAttrColor(R.attr.text_primary));

        emotionRadarChart.getYAxis().setLabelCount(4, false);
        emotionRadarChart.getYAxis().setTextSize(9f);
        emotionRadarChart.getYAxis().setAxisMinimum(0f);
        emotionRadarChart.getYAxis().setDrawLabels(false);
        emotionRadarChart.invalidate();

        // Calculate dominant emotion for "AI Insight"
        String dominantEmotion = null;
        int maxVal = 0;
        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            if (entry.getValue() > maxVal) {
                maxVal = entry.getValue();
                dominantEmotion = entry.getKey();
            }
        }

        if (dominantEmotion != null && maxVal > 0) {
            String insight = getFakeAIInsight(dominantEmotion);
            emotionInsight.setText(insight + "\n[Debug: Found " + emotions.size() + " emotions]");
        } else {
            emotionInsight.setText("Log more emotions to unlock AI insights.\n[Debug: Found " + emotions.size() + " emotions]");
        }
    }

    @Nullable
    private static String getString(EmotionCount ec) {
        String key = null;
        if (ec.emotion.contains("Proud"))
            key = "Proud";
        else if (ec.emotion.contains("Motivated"))
            key = "Motivated";
        else if (ec.emotion.contains("Satisfied"))
            key = "Satisfied";
        else if (ec.emotion.contains("Neutral"))
            key = "Neutral";
        else if (ec.emotion.contains("Relieved"))
            key = "Relieved";
        else if (ec.emotion.contains("Tired"))
            key = "Tired";
        return key;
    }

    private String getFakeAIInsight(String emotion) {
        switch (emotion) {
            case "Proud":
                return "✨ You're feeling mostly Proud! This mimics the 'Winner Effect' – success breeds more success. Keep riding this wave!";
            case "Satisfied":
                return "😊 Satisfaction is your top mood. This indicates a healthy dopamine balance. You're building sustainable habits.";
            case "Relieved":
                return "😌 Relief is dominant. You might be procrastinating until the last minute. Try starting earlier to reduce cortisol levels.";
            case "Motivated":
                return "🔥 High Motivation detected! This is great, but remember: discipline carries you when motivation fades.";
            case "Tired":
                return "😴 You're collecting Tired completions. Consistency when tired builds grit, but ensure you're getting enough recovery.";
            case "Neutral":
                return "😐 Mostly Neutral. This is actually perfect – it means this habit is becoming automatic and requires less willpower.";
            default:
                return "AI is analyzing your emotional patterns...";
        }
    }

    private int getAttrColor(int attr) {
        TypedValue typedValue = new TypedValue();
        getTheme().resolveAttribute(attr, typedValue, true);
        return typedValue.data;
    }
}
