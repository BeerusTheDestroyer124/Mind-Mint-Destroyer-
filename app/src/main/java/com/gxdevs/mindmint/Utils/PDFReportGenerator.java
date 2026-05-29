package com.gxdevs.mindmint.Utils;

import android.content.Context;
import android.os.Environment;
import android.util.Log;
import com.gxdevs.mindmint.Models.AttendanceRecord;
import com.gxdevs.mindmint.Models.UserStats;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PDFReportGenerator {
    
    private static final String TAG = "PDFReportGenerator";
    private final Context context;
    
    public PDFReportGenerator(Context context) {
        this.context = context;
    }
    
    public String generateMonthlyReport(UserStats stats, List<AttendanceRecord> records) {
        try {
            String timestamp = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
            String fileName = "MindMint_Report_" + timestamp + ".txt";
            
            File reportsDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "MindMintReports");
            if (!reportsDir.exists()) reportsDir.mkdirs();
            
            File reportFile = new File(reportsDir, fileName);
            
            StringBuilder reportContent = new StringBuilder();
            reportContent.append("=".repeat(60)).append("\n");
            reportContent.append("MIND MINT - MONTHLY PRODUCTIVITY REPORT\n");
            reportContent.append("=".repeat(60)).append("\n\n");
            
            reportContent.append("GENERATED: ").append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(new Date())).append("\n\n");
            
            reportContent.append("USER STATISTICS\n");
            reportContent.append("-".repeat(60)).append("\n");
            reportContent.append("Current Level: ").append(stats.getUserLevel()).append("\n");
            reportContent.append("Total XP: ").append(stats.getTotalXP()).append("\n");
            reportContent.append("Badge: ").append(stats.getBadge()).append("\n");
            reportContent.append("Current Streak: ").append(stats.getConsecutiveStreak()).append(" days\n");
            reportContent.append("Longest Streak: ").append(stats.getLongestStreak()).append(" days\n\n");
            
            reportContent.append("ACTIVITY SUMMARY\n");
            reportContent.append("-".repeat(60)).append("\n");
            reportContent.append("Total Focus Sessions: ").append(stats.getTotalFocusSessions()).append("\n");
            reportContent.append("Tasks Completed: ").append(stats.getTotalTasksCompleted()).append("\n");
            reportContent.append("Habits Completed: ").append(stats.getTotalHabitsCompleted()).append("\n");
            reportContent.append("Crystals Earned: ").append(stats.getTotalCrystals()).append("\n\n");
            
            reportContent.append("ATTENDANCE DATA\n");
            reportContent.append("-".repeat(60)).append("\n");
            int checkIns = 0, totalMinutes = 0;
            for (AttendanceRecord r : records) {
                if (r.isCheckedIn()) {
                    checkIns++;
                    totalMinutes += r.getTotalFocusMinutes();
                }
            }
            reportContent.append("Total Check-ins: ").append(checkIns).append("\n");
            reportContent.append("Total Focus Minutes: ").append(totalMinutes).append("\n");
            reportContent.append("Average per Day: ").append(checkIns > 0 ? totalMinutes/checkIns : 0).append(" min\n\n");
            
            reportContent.append("=".repeat(60)).append("\n");
            
            try (FileOutputStream fos = new FileOutputStream(reportFile)) {
                fos.write(reportContent.toString().getBytes());
                fos.flush();
            }
            
            Log.d(TAG, "Report generated: " + reportFile.getAbsolutePath());
            return reportFile.getAbsolutePath();
            
        } catch (IOException e) {
            Log.e(TAG, "Report generation error", e);
            return null;
        }
    }
    
    public String generateStatisticsCSV(UserStats stats, List<AttendanceRecord> records) {
        try {
            String timestamp = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
            String fileName = "MindMint_Stats_" + timestamp + ".csv";
            
            File reportsDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "MindMintReports");
            if (!reportsDir.exists()) reportsDir.mkdirs();
            
            File csvFile = new File(reportsDir, fileName);
            
            StringBuilder csv = new StringBuilder();
            csv.append("Date,CheckedIn,FocusSessions,TotalMinutes,Mood\n");
            
            for (AttendanceRecord record : records) {
                csv.append(new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date(record.getCheckInDate())))
                   .append(",").append(record.isCheckedIn())
                   .append(",").append(record.getFocusSessionCount())
                   .append(",").append(record.getTotalFocusMinutes())
                   .append(",").append(record.getMood() != null ? record.getMood() : "N/A")
                   .append("\n");
            }
            
            try (FileOutputStream fos = new FileOutputStream(csvFile)) {
                fos.write(csv.toString().getBytes());
                fos.flush();
            }
            
            Log.d(TAG, "CSV generated: " + csvFile.getAbsolutePath());
            return csvFile.getAbsolutePath();
            
        } catch (IOException e) {
            Log.e(TAG, "CSV generation error", e);
            return null;
        }
    }
}
