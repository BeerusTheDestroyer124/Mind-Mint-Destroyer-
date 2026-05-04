package com.gxdevs.mindmint.Utils;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.View;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.gxdevs.mindmint.R;

public class CustomDialogUtils {

    public interface InputCallback {
        void onInput(String text);
    }

    public static void showCustomDialog(Context context, String title, String message, String positiveText, String negativeText, Runnable onPositive, Runnable onNegative) {
        showCustomDialog(context, title, message, positiveText, negativeText, null, onPositive, onNegative, null);
    }

    public static void showCustomDialog(Context context, String title, String message, String positiveText, String negativeText, String neutralText, Runnable onPositive, Runnable onNegative, Runnable onNeutral) {
        Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_custom_confirm);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        TextView tvTitle = dialog.findViewById(R.id.dialogTitle);
        TextView tvMessage = dialog.findViewById(R.id.dialogMessage);
        MaterialButton btnCancel = dialog.findViewById(R.id.btnCancel);
        MaterialButton btnNeutral = dialog.findViewById(R.id.btnNeutral);
        MaterialButton btnConfirm = dialog.findViewById(R.id.btnConfirm);

        tvTitle.setText(title);
        tvMessage.setText(message);
        btnConfirm.setText(positiveText);

        if (negativeText != null && !negativeText.isEmpty()) {
            btnCancel.setText(negativeText);
            btnCancel.setVisibility(View.VISIBLE);
        } else {
            btnCancel.setVisibility(View.GONE);
        }

        if (neutralText != null && !neutralText.isEmpty()) {
            btnNeutral.setText(neutralText);
            btnNeutral.setVisibility(View.VISIBLE);

            LinearLayout buttonsParent = (LinearLayout) btnCancel.getParent();
            buttonsParent.setOrientation(LinearLayout.VERTICAL);
            buttonsParent.removeAllViews();
            buttonsParent.addView(btnConfirm);
            buttonsParent.addView(btnCancel);
            buttonsParent.addView(btnNeutral);

            int height = (int) (55 * context.getResources().getDisplayMetrics().density);
            int margin = (int) (8 * context.getResources().getDisplayMetrics().density);

            LinearLayout.LayoutParams paramsWithMargin = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, height);
            paramsWithMargin.setMargins(0, 0, 0, margin);
            LinearLayout.LayoutParams paramsNoMargin = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, height);

            btnConfirm.setLayoutParams(paramsWithMargin);
            btnCancel.setLayoutParams(paramsWithMargin);
            btnNeutral.setLayoutParams(paramsNoMargin);
        } else {
            btnNeutral.setVisibility(View.GONE);
        }

        btnCancel.setOnClickListener(v -> {
            dialog.dismiss();
            if (onNegative != null) onNegative.run();
        });

        btnNeutral.setOnClickListener(v -> {
            dialog.dismiss();
            if (onNeutral != null) onNeutral.run();
        });

        btnConfirm.setOnClickListener(v -> {
            dialog.dismiss();
            if (onPositive != null) onPositive.run();
        });

        dialog.setCancelable(false);
        dialog.show();
    }

    public static void showCustomInputDialog(Context context, String title, String hint, InputCallback onPositive) {
        Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_custom_confirm);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        TextView tvTitle = dialog.findViewById(R.id.dialogTitle);
        TextView tvMessage = dialog.findViewById(R.id.dialogMessage);
        tvMessage.setVisibility(View.GONE); // Hide standard message

        // Add an input field programmatically since it's an input dialog
        LinearLayout rootLayout = dialog.findViewById(R.id.dialogMessage).getParent() != null ? (LinearLayout) dialog.findViewById(R.id.dialogMessage).getParent() : null;
        TextInputEditText input = new TextInputEditText(context);
        input.setHint(hint);
        input.setTextColor(Color.WHITE);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(16, 32, 16, 16);
        input.setLayoutParams(params);
        if (rootLayout != null) {
            rootLayout.addView(input, 2); // Insert after title and message
        }

        MaterialButton btnCancel = dialog.findViewById(R.id.btnCancel);
        MaterialButton btnConfirm = dialog.findViewById(R.id.btnConfirm);

        tvTitle.setText(title);
        btnConfirm.setText("Submit");
        btnCancel.setText("Cancel");

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnConfirm.setOnClickListener(v -> {
            String val = input.getText() != null ? input.getText().toString().trim() : "";
            if (!val.isEmpty() && onPositive != null) {
                onPositive.onInput(val);
                dialog.dismiss();
            } else if (val.isEmpty()) {
                input.setError("Content cannot be empty");
            }
        });

        dialog.setCancelable(true);
        dialog.show();
    }
}
