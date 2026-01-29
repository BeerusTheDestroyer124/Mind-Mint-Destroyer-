package com.gxdevs.mindmint.Models;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class StatsViewModel extends ViewModel {

    private final MutableLiveData<Boolean> isWeekly = new MutableLiveData<>(true);
    private final MutableLiveData<String> periodLabel = new MutableLiveData<>();
    private final MutableLiveData<PeriodRange> periodRange = new MutableLiveData<>();

    private int currentWeekOffset = 0;
    private int currentMonthOffset = 0;

    public StatsViewModel() {
        updatePeriod();
    }

    public LiveData<Boolean> getIsWeekly() {
        return isWeekly;
    }

    public LiveData<String> getPeriodLabel() {
        return periodLabel;
    }

    public LiveData<PeriodRange> getPeriodRange() {
        return periodRange;
    }

    public void setWeekly(boolean weekly) {
        if (Boolean.TRUE.equals(isWeekly.getValue()) != weekly) {
            isWeekly.setValue(weekly);
            updatePeriod();
        }
    }

    public void incrementOffset() {
        if (Boolean.TRUE.equals(isWeekly.getValue())) {
            currentWeekOffset++;
        } else {
            currentMonthOffset++;
        }
        updatePeriod();
    }

    public void decrementOffset() {
        if (Boolean.TRUE.equals(isWeekly.getValue())) {
            currentWeekOffset--;
        } else {
            currentMonthOffset--;
        }
        updatePeriod();
    }

    public int getCurrentWeekOffset() {
        return currentWeekOffset;
    }

    public int getCurrentMonthOffset() {
        return currentMonthOffset;
    }

    private void updatePeriod() {
        boolean weekly = Boolean.TRUE.equals(isWeekly.getValue());
        Calendar cal = Calendar.getInstance();

        // Update Label
        if (weekly) {
            cal.add(Calendar.WEEK_OF_YEAR, currentWeekOffset);
            int week = cal.get(Calendar.WEEK_OF_YEAR);
            int year = cal.get(Calendar.YEAR);
            periodLabel.setValue("Week " + week + ", " + year);
        } else {
            cal.add(Calendar.MONTH, currentMonthOffset);
            int year = cal.get(Calendar.YEAR);
            String monthName = new SimpleDateFormat("MMMM", Locale.getDefault()).format(cal.getTime());
            periodLabel.setValue(monthName + " " + year);
        }

        // Calculate Range
        List<PeriodSlot> slots = new ArrayList<>();
        cal = Calendar.getInstance(); // Reset
        SimpleDateFormat dayFmt = new SimpleDateFormat("EEE", Locale.getDefault());

        if (weekly) {
            cal.add(Calendar.WEEK_OF_YEAR, currentWeekOffset);
            cal.set(Calendar.DAY_OF_WEEK, cal.getFirstDayOfWeek());
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);

            for (int i = 0; i < 7; i++) {
                long s = cal.getTimeInMillis();
                cal.add(Calendar.DAY_OF_YEAR, 1);
                long e = cal.getTimeInMillis();
                slots.add(new PeriodSlot(s, e, dayFmt.format(new Date(s))));
            }
        } else {
            cal.add(Calendar.MONTH, currentMonthOffset);
            cal.set(Calendar.DAY_OF_MONTH, 1);
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            int days = cal.getActualMaximum(Calendar.DAY_OF_MONTH);

            for (int i = 0; i < days; i++) {
                long s = cal.getTimeInMillis();
                cal.add(Calendar.DAY_OF_YEAR, 1);
                long e = cal.getTimeInMillis();
                slots.add(new PeriodSlot(s, e, String.valueOf(i + 1)));
            }
        }

        if (!slots.isEmpty()) {
            long start = slots.get(0).startMs;
            long end = slots.get(slots.size() - 1).endMs;
            periodRange.setValue(new PeriodRange(start, end, slots));
        }
    }

    public static class PeriodRange {
        public final long startMs;
        public final long endMs;
        public final List<PeriodSlot> slots;

        public PeriodRange(long startMs, long endMs, List<PeriodSlot> slots) {
            this.startMs = startMs;
            this.endMs = endMs;
            this.slots = slots;
        }
    }

    public static class PeriodSlot {
        public long startMs, endMs;
        public String label;

        PeriodSlot(long s, long e, String l) {
            startMs = s;
            endMs = e;
            label = l;
        }
    }
}
