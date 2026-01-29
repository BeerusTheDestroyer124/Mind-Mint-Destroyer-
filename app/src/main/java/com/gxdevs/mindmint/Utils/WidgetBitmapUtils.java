package com.gxdevs.mindmint.Utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;

public class WidgetBitmapUtils {

    public static Bitmap createHabitIconBitmap(Context context, int iconResId, int iconTint, int bgTint) {
        int size = dpToPx(context, 32);
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bgPaint.setColor(bgTint);
        bgPaint.setStyle(Paint.Style.FILL);

        float center = size / 2f;
        canvas.drawCircle(center, center, center, bgPaint);

        try {
            Drawable icon = androidx.appcompat.content.res.AppCompatResources.getDrawable(context, iconResId);
            if (icon != null) {
                Drawable iconMut = icon.mutate();
                iconMut.setColorFilter(new PorterDuffColorFilter(iconTint, PorterDuff.Mode.SRC_IN));
                int padding = dpToPx(context, 6);
                iconMut.setBounds(padding, padding, size - padding, size - padding);
                iconMut.draw(canvas);
            }
        } catch (Exception e) {
            Paint iconPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            iconPaint.setColor(iconTint);
            iconPaint.setStyle(Paint.Style.FILL);
            canvas.drawCircle(center, center, size / 4f, iconPaint);
        }

        return bitmap;
    }

    private static int dpToPx(Context context, int dp) {
        return (int) (dp * context.getResources().getDisplayMetrics().density);
    }
}
