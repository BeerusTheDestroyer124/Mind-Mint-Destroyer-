package com.gxdevs.mindmint.Receivers;

import android.app.admin.DeviceAdminReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.gxdevs.mindmint.R;

/**
 * Device Administration receiver for Mind Mint.
 *
 * When the user grants Device Admin rights, Android prevents both:
 *  - Force-stopping the app from Settings → Apps
 *  - Uninstalling the app without first revoking admin permission
 *
 * This is the approved, Play-Store-compliant mechanism for self-protection.
 */
public class MindMintDeviceAdminReceiver extends DeviceAdminReceiver {

    @Override
    public void onEnabled(@NonNull Context context, @NonNull Intent intent) {
    }

    @Override
    public void onDisabled(@NonNull Context context, @NonNull Intent intent) {
        Toast.makeText(context,
                "Admin permission revoked — app can now be uninstalled.",
                Toast.LENGTH_LONG).show();
    }

    @Override
    public CharSequence onDisableRequested(@NonNull Context context, @NonNull Intent intent) {
        return "Revoking this permission will allow the app to be uninstalled. Are you sure?";
    }
}
