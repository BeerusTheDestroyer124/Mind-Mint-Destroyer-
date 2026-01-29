package com.gxdevs.mindmint.Views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.gxdevs.mindmint.R;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class HabitHeatmapView extends View {

    private int year;
    private int weeksToShow = 52;
    private static final int DAYS_IN_WEEK = 7;
    private static final float CELL_SIZE_DP = 12f;
    private static final float CELL_GAP_DP = 3f;
    private static final float CORNER_RADIUS_DP = 3f;
    private static final float LABEL_WIDTH_DP = 20f;
    private static final float MONTH_LABEL_HEIGHT_DP = 15f;
    private static final float MONTH_LABEL_SIZE_DP = 10f;
    private Paint cellPaint;
    private Paint labelPaint;
    private Paint monthLabelPaint;
    private float cellSize;
    private float cellGap;
    private float cornerRadius;
    private float labelWidth;
    private float monthLabelHeight;
    // Level -> Color mapping (0-5)
    private int[] levelColors;

    // Map of dayOfYear -> level (0-5)
    private final Map<Long, Integer> completionData = new HashMap<>();

    private static final String[] DAY_LABELS = { "S", "M", "T", "W", "T", "F", "S" };
    private static final String[] MONTH_LABELS = { "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct",
            "Nov", "Dec" };

    public HabitHeatmapView(Context context) {
        super(context);
        init();
    }

    public HabitHeatmapView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public HabitHeatmapView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        year = Calendar.getInstance().get(Calendar.YEAR);

        float density = getResources().getDisplayMetrics().density;
        cellSize = CELL_SIZE_DP * density;
        cellGap = CELL_GAP_DP * density;
        cornerRadius = CORNER_RADIUS_DP * density;
        labelWidth = LABEL_WIDTH_DP * density;
        monthLabelHeight = MONTH_LABEL_HEIGHT_DP * density;

        cellPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        labelPaint.setColor(getAttrColor(R.attr.text_tertiary));
        labelPaint.setTextSize(10 * density);
        labelPaint.setTextAlign(Paint.Align.CENTER);

        monthLabelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        monthLabelPaint.setColor(getAttrColor(R.attr.text_tertiary));
        monthLabelPaint.setTextSize(MONTH_LABEL_SIZE_DP * density);
        monthLabelPaint.setTextAlign(Paint.Align.LEFT);

        int brandColor = getAttrColor(R.attr.brand_pink);
        int r = Color.red(brandColor);
        int g = Color.green(brandColor);
        int b = Color.blue(brandColor);

        levelColors = new int[] {
                getAttrColor(R.attr.glass_stroke), // Level 0 - empty
                Color.argb(80, r, g, b), // Level 1 - 30% (Boosted from 20%)
                Color.argb(120, r, g, b), // Level 2 - 47% (Boosted from 40%)
                Color.argb(160, r, g, b), // Level 3 - 63% (Boosted from 60%)
                Color.argb(200, r, g, b), // Level 4 - 78% (Boosted from 80%)
                Color.argb(255, r, g, b) // Level 5 - 100%
        };
    }

    private int getAttrColor(int attr) {
        TypedValue typedValue = new TypedValue();
        getContext().getTheme().resolveAttribute(attr, typedValue, true);
        return typedValue.data;
    }

    public void setYear(int year) {
        this.year = year;
        requestLayout();
        invalidate();
    }

    public void setCompletionData(Map<Long, Integer> data, int target, boolean unitBased, int oneTapValue) {
        this.completionData.clear();
        int targetCount = Math.max(1, target);

        for (Map.Entry<Long, Integer> entry : data.entrySet()) {
            int count = entry.getValue();
            int level;

            if (unitBased) {
                float percentage = (float) count / targetCount;
                if (percentage <= 0) {
                    level = 0;
                } else if (percentage >= 1.0f) {
                    level = 5;
                } else {
                    level = (int) Math.ceil(percentage * 5.0f);
                    level = Math.max(1, Math.min(5, level));
                }
            } else {
                level = count > 0 ? 5 : 0;
            }

            completionData.put(entry.getKey(), level);
        }

        invalidate();
    }

    public void setRawLevelData(Map<Long, Integer> levelData) {
        this.completionData.clear();
        for (Map.Entry<Long, Integer> entry : levelData.entrySet()) {
            int level = Math.max(0, Math.min(5, entry.getValue()));
            completionData.put(entry.getKey(), level);
        }
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        long diffDays = getDiffDays();
        weeksToShow = (int) Math.ceil((diffDays + 1) / 7.0);
        if (weeksToShow < 52)
            weeksToShow = 52;

        int width = (int) (labelWidth + weeksToShow * (cellSize + cellGap) - cellGap);
        int height = (int) (monthLabelHeight + DAYS_IN_WEEK * (cellSize + cellGap) - cellGap);
        setMeasuredDimension(width, height);
    }

    private long getDiffDays() {
        Calendar startCal = Calendar.getInstance();
        startCal.set(year, Calendar.JANUARY, 1, 0, 0, 0);
        startCal.set(Calendar.MILLISECOND, 0);
        while (startCal.get(Calendar.DAY_OF_WEEK) != Calendar.SUNDAY) {
            startCal.add(Calendar.DAY_OF_YEAR, -1);
        }
        Calendar endCal = Calendar.getInstance();
        endCal.set(year, Calendar.DECEMBER, 31, 23, 59, 59);
        long diffMs = endCal.getTimeInMillis() - startCal.getTimeInMillis();
        return diffMs / (24 * 60 * 60 * 1000);
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        Paint.FontMetrics fm = labelPaint.getFontMetrics();
        for (int day = 0; day < DAYS_IN_WEEK; day++) {
            float top = monthLabelHeight + day * (cellSize + cellGap);
            float centerY = top + cellSize / 2 - (fm.ascent + fm.descent) / 2;
            canvas.drawText(DAY_LABELS[day], labelWidth / 2, centerY, labelPaint);
        }

        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.YEAR, year);
        cal.set(Calendar.DAY_OF_YEAR, 1);

        // Find start of the week for Jan 1st
        Calendar startCal = (Calendar) cal.clone();
        while (startCal.get(Calendar.DAY_OF_WEEK) != Calendar.SUNDAY) {
            startCal.add(Calendar.DAY_OF_YEAR, -1);
        }

        // Normalize
        startCal.set(Calendar.HOUR_OF_DAY, 0);
        startCal.set(Calendar.MINUTE, 0);
        startCal.set(Calendar.SECOND, 0);
        startCal.set(Calendar.MILLISECOND, 0);

        RectF rect = new RectF();
        Calendar labelCheckCal = (Calendar) startCal.clone();

        for (int week = 0; week < weeksToShow; week++) {
            int monthToLabel = -1;

            for (int d = 0; d < 7; d++) {
                if (labelCheckCal.get(Calendar.DAY_OF_MONTH) == 1 && labelCheckCal.get(Calendar.YEAR) == year) {
                    monthToLabel = labelCheckCal.get(Calendar.MONTH);
                }
                // Special case for Jan 1st if it's in the very first column
                if (week == 0 && labelCheckCal.get(Calendar.MONTH) == Calendar.JANUARY
                        && labelCheckCal.get(Calendar.YEAR) == year) {
                    monthToLabel = Calendar.JANUARY;
                }

                labelCheckCal.add(Calendar.DAY_OF_YEAR, 1);
            }

            if (monthToLabel != -1) {
                float x = labelWidth + week * (cellSize + cellGap);
                canvas.drawText(
                        MONTH_LABELS[monthToLabel], x, monthLabelHeight - TypedValue
                                .applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4, getResources().getDisplayMetrics()),
                        monthLabelPaint);
            }

            // Draw cells for this week
            for (int day = 0; day < DAYS_IN_WEEK; day++) {
                float left = labelWidth + week * (cellSize + cellGap);
                float top = monthLabelHeight + day * (cellSize + cellGap);

                // Only draw if it belongs to the current year
                if (startCal.get(Calendar.YEAR) == year) {
                    rect.set(left, top, left + cellSize, top + cellSize);

                    long dayMs = startCal.getTimeInMillis();
                    int level = completionData.getOrDefault(dayMs, 0);

                    cellPaint.setColor(levelColors[Math.min(level, 5)]);
                    canvas.drawRoundRect(rect, cornerRadius, cornerRadius, cellPaint);
                }

                startCal.add(Calendar.DAY_OF_YEAR, 1);
            }
        }
    }
}
