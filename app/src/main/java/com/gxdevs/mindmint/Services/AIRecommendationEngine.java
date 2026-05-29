package com.gxdevs.mindmint.Services;

import android.util.Log;
import com.gxdevs.mindmint.Models.AIRecommendation;
import com.gxdevs.mindmint.Models.UserStats;

public class AIRecommendationEngine {
    
    private static final String TAG = "AIRecommendationEngine";
    
    public AIRecommendation analyzeFocusPatterns(UserStats stats) {
        int recommendedDuration = 25;
        String reason = "Standard Pomodoro technique";
        int confidence = 70;
        
        if (stats.getTotalFocusSessions() > 10) {
            int avgDuration = (stats.getTotalXP() / stats.getTotalFocusSessions()) / 10;
            
            if (avgDuration >= 45) {
                recommendedDuration = 60;
                reason = "Your focus sessions are typically longer. Consider 60-min sessions.";
                confidence = 85;
            } else if (avgDuration >= 30) {
                recommendedDuration = 45;
                reason = "Based on your patterns, 45-minute sessions work best.";
                confidence = 80;
            }
        }
        
        AIRecommendation rec = new AIRecommendation("focus_duration", recommendedDuration + " minutes", reason, 3);
        rec.setConfidenceScore(confidence);
        Log.d(TAG, "Recommended focus duration: " + recommendedDuration);
        return rec;
    }
    
    public AIRecommendation recommendBreakTime(UserStats stats) {
        int breakMinutes = 5;
        String reason = "Quick break to refresh";
        
        if (stats.getTotalFocusSessions() % 4 == 0) {
            breakMinutes = 15;
            reason = "Long break! You've completed 4 focus sessions.";
        } else if (stats.getTotalFocusSessions() % 2 == 0) {
            breakMinutes = 10;
            reason = "Mid-session break recommended.";
        }
        
        AIRecommendation rec = new AIRecommendation("break_time", "Take a " + breakMinutes + "-minute break", reason, 2);
        rec.setConfidenceScore(75);
        return rec;
    }
    
    public AIRecommendation generateMotivationalTip(int productivityScore, int streak) {
        String motivation = "";
        String tip = "";
        int priority = 2;
        
        if (productivityScore >= 90) {
            motivation = "You're absolutely crushing it! 🔥";
            tip = "You're in the top 5% of users!";
            priority = 1;
        } else if (streak >= 7) {
            motivation = "Amazing streak! Keep it going! 🎯";
            tip = "You have a " + streak + "-day streak. Don't break it!";
            priority = 2;
        } else if (productivityScore >= 70) {
            motivation = "Great progress! 🎯";
            tip = "You're maintaining good productivity!";
        } else {
            motivation = "Every day is a new opportunity! 💪";
            tip = "Start with a 25-minute focus session!";
            priority = 3;
        }
        
        AIRecommendation rec = new AIRecommendation("motivation", motivation, tip, priority);
        rec.setConfidenceScore(100);
        return rec;
    }
    
    public String getBestFocusTime() {
        java.util.Calendar calendar = java.util.Calendar.getInstance();
        int hour = calendar.get(java.util.Calendar.HOUR_OF_DAY);
        
        if (hour >= 5 && hour < 12) return "Morning (5-12 AM)";
        if (hour >= 12 && hour < 17) return "Afternoon (12-5 PM)";
        if (hour >= 17 && hour < 21) return "Evening (5-9 PM)";
        return "Night (9 PM-5 AM)";
    }
}
