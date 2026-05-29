package com.gxdevs.mindmint.Services;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.util.Log;
import com.gxdevs.mindmint.Models.UserStats;

public class SocialSharingService {
    
    private static final String TAG = "SocialSharingService";
    private final Context context;
    
    public SocialSharingService(Context context) {
        this.context = context;
    }
    
    public void shareOnWhatsApp(UserStats stats) {
        try {
            String message = generateShareMessage(stats);
            Intent intent = new Intent();
            intent.setAction(Intent.ACTION_SEND);
            intent.putExtra(Intent.EXTRA_TEXT, message);
            intent.setType("text/plain");
            intent.setPackage("com.whatsapp");
            if (isAppInstalled("com.whatsapp")) context.startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "WhatsApp share error", e);
        }
    }
    
    public void shareOnInstagram(UserStats stats) {
        try {
            String message = generateShareMessage(stats);
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.setPackage("com.instagram.android");
            intent.putExtra(Intent.EXTRA_TEXT, message);
            if (isAppInstalled("com.instagram.android")) context.startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "Instagram share error", e);
        }
    }
    
    public void shareOnTwitter(UserStats stats) {
        try {
            String message = generateShareMessage(stats);
            String url = "https://twitter.com/intent/tweet?text=" + Uri.encode(message);
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            context.startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "Twitter share error", e);
        }
    }
    
    public void shareOnFacebook(UserStats stats) {
        try {
            String message = generateShareMessage(stats);
            Intent intent = new Intent();
            intent.setAction(Intent.ACTION_SEND);
            intent.putExtra(Intent.EXTRA_TEXT, message);
            intent.setType("text/plain");
            intent.setPackage("com.facebook.katana");
            if (isAppInstalled("com.facebook.katana")) context.startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "Facebook share error", e);
        }
    }
    
    private String generateShareMessage(UserStats stats) {
        return "🎯 I'm crushing my productivity goals on Mind Mint!\n" +
                "📊 Level: " + stats.getUserLevel() + "\n" +
                "⭐ XP: " + stats.getTotalXP() + "\n" +
                "🔥 Streak: " + stats.getConsecutiveStreak() + " days\n" +
                "💎 Badge: " + stats.getBadge() + "\n" +
                "\n💪 Join me on Mind Mint! 🧠";
    }
    
    private boolean isAppInstalled(String packageName) {
        try {
            context.getPackageManager().getApplicationInfo(packageName, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }
}
