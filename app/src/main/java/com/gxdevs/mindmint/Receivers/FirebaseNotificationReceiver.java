package com.gxdevs.mindmint.Receivers;

import static com.gxdevs.mindmint.Utils.Utils.isAccessibilityPermissionGranted;

import android.content.Intent;

import androidx.annotation.NonNull;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.gxdevs.mindmint.Activities.HomeActivity;

import java.util.Map;

public class FirebaseNotificationReceiver extends FirebaseMessagingService {

    private static final String TAG = "MindMIntFCM";

    @Override
    public void onMessageReceived(@NonNull RemoteMessage message) {
        if (!message.getData().isEmpty()) {
            Map<String, String> data = message.getData();
            String action = data.get("action");

            if ("check_service_status".equals(action)) {
                checkAndReviveService();
            }
        }
    }

    private void checkAndReviveService() {
        if (!isAccessibilityPermissionGranted(this)) {
            Intent intent = new Intent(this, HomeActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            intent.putExtra("from_guard", true);
            startActivity(intent);
        }
    }

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
    }
}

















