package com.gxdevs.mindmint.Utils;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;

public class AnimUtils {

    public static void attachTouchRipple(View view) {
        view.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case android.view.MotionEvent.ACTION_DOWN:
                    v.animate().scaleX(0.83f).scaleY(0.83f)
                            .setDuration(100).setInterpolator(new DecelerateInterpolator()).start();
                    break;
                case android.view.MotionEvent.ACTION_UP:
                case android.view.MotionEvent.ACTION_CANCEL:
                    v.animate().scaleX(1f).scaleY(1f)
                            .setDuration(300).setInterpolator(new OvershootInterpolator(3f)).start();
                    break;
            }
            return false; // pass through to original listeners
        });
    }

    public static void enterSlideUp(View view, long delayMs) {
        view.setAlpha(0f);
        view.setTranslationY(60f);
        view.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(320)
                .setStartDelay(delayMs)
                .setInterpolator(new DecelerateInterpolator(2.2f))
                .start();
    }

    /**
     * Simple fade-in for any view.
     */
    public static void fadeIn(View view, long delayMs, long durationMs) {
        view.setAlpha(0f);
        view.setVisibility(View.VISIBLE);
        view.animate()
                .alpha(1f)
                .setDuration(durationMs)
                .setStartDelay(delayMs)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .start();
    }

    public static void bounceIn(View view, long delayMs) {
        view.setAlpha(0f);
        view.setScaleX(0.60f);
        view.setScaleY(0.60f);
        view.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(360)
                .setStartDelay(delayMs)
                .setInterpolator(new OvershootInterpolator(2.2f))
                .start();
    }

    public static void animateItemEnter(View view, int position, int[] lastPosition) {
        if (position > lastPosition[0]) {
            lastPosition[0] = position;
            view.setAlpha(0f);
            view.setTranslationY(40f);
            view.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(260)
                    .setStartDelay(Math.min(position * 40L, 240L))
                    .setInterpolator(new DecelerateInterpolator(1.8f))
                    .start();
        }
    }

    public static void navIconSelect(View icon) {
        icon.animate()
                .scaleX(1.35f).scaleY(1.35f)
                .setDuration(130)
                .setInterpolator(new DecelerateInterpolator())
                .withEndAction(() ->
                        icon.animate()
                                .scaleX(1f).scaleY(1f)
                                .setDuration(240)
                                .setInterpolator(new OvershootInterpolator(3.5f))
                                .start())
                .start();
    }
}
