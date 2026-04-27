package com.gxdevs.mindmint.Activities;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.core.splashscreen.SplashScreen;
import androidx.viewpager.widget.ViewPager;

import com.gxdevs.mindmint.Adapters.OnBoardingAdapter;
import com.gxdevs.mindmint.R;
import com.gxdevs.mindmint.Utils.Utils;

public class OnBoarding extends AppCompatActivity {
    private ViewPager viewPager;
    private TextView goText, skipBtn;
    private LinearLayout indicatorLayout;
    private View[] indicators;
    private int currentPosition;
    private Animation buttonAnimation;
    private ImageView goIcon;
    private ConstraintLayout goLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Utils.applyAppThemeFromPrefs(this);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            SplashScreen splashScreen = SplashScreen.installSplashScreen(this);
            splashScreen.setKeepOnScreenCondition(() -> false);
        }

        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        boolean isFirstRun = getSharedPreferences("PREFERENCE", MODE_PRIVATE).getBoolean("isFirstRun", true);
        if (isFirstRun) {
            setContentView(R.layout.activity_on_boarding);
            Utils.setPad(findViewById(R.id.main), "bottom", this);
            viewPager = findViewById(R.id.slider);
            goText = findViewById(R.id.goText);
            skipBtn = findViewById(R.id.skipBtn);
            indicatorLayout = findViewById(R.id.indicatorLayout);
            goIcon = findViewById(R.id.goIcon);
            goLayout = findViewById(R.id.goLayout);

            OnBoardingAdapter sliderAdapter = new OnBoardingAdapter(this);
            viewPager.setAdapter(sliderAdapter);

            viewPager.setPageTransformer(true, (page, position) -> {
                int pageWidth = page.getWidth();
                View imgContainer = page.findViewById(R.id.sliderImgContainer);
                View head = page.findViewById(R.id.sliderHead);
                View desc = page.findViewById(R.id.sliderDesc);

                if (position < -1) { // [-Infinity,-1)
                    page.setAlpha(0f);
                } else if (position <= 1) { // [-1,1]
                    page.setCameraDistance(pageWidth * 20);

                    // Parallax speeds
                    if (imgContainer != null) {
                        imgContainer.setTranslationX(-position * (pageWidth / 2f));
                        imgContainer.setScaleX(1.0f - Math.abs(position) * 0.05f);
                        imgContainer.setScaleY(1.0f - Math.abs(position) * 0.05f);
                        imgContainer.setRotation(position * 30f); // Increased rotation
                        imgContainer.setAlpha(1.0f - Math.abs(position) * 1.2f); // Slower fade
                    }
                    if (head != null) {
                        head.setTranslationX(-position * (pageWidth / 3.5f));
                        head.setAlpha(1.0f - Math.abs(position) * 1.3f);
                    }
                    if (desc != null) {
                        desc.setTranslationX(-position * (pageWidth / 4.5f));
                        desc.setAlpha(0.5f - Math.abs(position) * 1.5f);
                    }
                    page.setAlpha(1f);
                } else { // (1,+Infinity]
                    page.setAlpha(0f);
                }
            });

            setupIndicators(sliderAdapter.getCount());
            viewPager.addOnPageChangeListener(onPageChangeListener);

            buttonAnimation = AnimationUtils.loadAnimation(OnBoarding.this, R.anim.button_anim);
            goText.setText(ContextCompat.getString(OnBoarding.this, R.string.next));
            goLayout.setOnClickListener(v -> viewPager.setCurrentItem(viewPager.getCurrentItem() + 1));

            skipBtn.setOnClickListener(v -> moveToNextActivity());

        } else {
            // If it's not the first run, go directly to MainActivity
            Intent homeIntent = new Intent(OnBoarding.this, HomeActivity.class);
            if (getIntent().getExtras() != null) {
                homeIntent.putExtras(getIntent().getExtras());
            }
            startActivity(homeIntent);
            finish();
        }
    }

    private void setupIndicators(int count) {
        indicators = new View[count];
        indicatorLayout.removeAllViews();
        for (int i = 0; i < count; i++) {
            indicators[i] = new View(this);
            int width = (i == 0) ? dpToPx(30) : dpToPx(8);
            int height = dpToPx(8);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(width, height);
            params.setMargins(dpToPx(4), 0, dpToPx(4), 0);
            indicators[i].setLayoutParams(params);
            indicators[i].setBackground(createIndicatorDrawable(i == 0));
            indicatorLayout.addView(indicators[i]);
        }
    }

    private Drawable createIndicatorDrawable(boolean active) {
        GradientDrawable shape = new GradientDrawable();
        shape.setShape(GradientDrawable.RECTANGLE);
        shape.setCornerRadius(dpToPx(8));
        int color = ContextCompat.getColor(this, active ? R.color.brand_pink : R.color.track_unchecked_color);
        shape.setColor(color);
        return shape;
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    ViewPager.OnPageChangeListener onPageChangeListener = new ViewPager.OnPageChangeListener() {
        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            if (indicators == null || indicators.length == 0)
                return;

            int maxWidth = dpToPx(30);
            int minWidth = dpToPx(8);

            for (int i = 0; i < indicators.length; i++) {
                LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) indicators[i].getLayoutParams();

                if (i == position) {
                    params.width = (int) (maxWidth - (maxWidth - minWidth) * positionOffset);
                    updateIndicatorColor(indicators[i], positionOffset < 0.5f);
                } else if (i == position + 1) {
                    params.width = (int) (minWidth + (maxWidth - minWidth) * positionOffset);
                    updateIndicatorColor(indicators[i], positionOffset >= 0.5f);
                } else {
                    params.width = minWidth;
                    updateIndicatorColor(indicators[i], false);
                }
                indicators[i].setLayoutParams(params);
            }
        }

        @Override
        public void onPageSelected(int position) {
            currentPosition = position;
            if (buttonAnimation != null) {
                goLayout.startAnimation(buttonAnimation);
            }

            if (position < 4) {
                goIcon.setImageDrawable(ContextCompat.getDrawable(OnBoarding.this, R.drawable.ic_right_arrow));
                goText.setText(ContextCompat.getString(OnBoarding.this, R.string.next));
                goIcon.setPadding(dpToPx(0), dpToPx(0), dpToPx(0), dpToPx(0));
                goLayout.setOnClickListener(v -> viewPager.setCurrentItem(currentPosition + 1));
                skipBtn.setVisibility(View.VISIBLE);
            } else {
                goIcon.setImageDrawable(ContextCompat.getDrawable(OnBoarding.this, R.drawable.circle_check));
                goIcon.setPadding(dpToPx(1), dpToPx(1), dpToPx(1), dpToPx(1));
                goText.setText(ContextCompat.getString(OnBoarding.this, R.string.lets_go));
                goLayout.setOnClickListener(v -> moveToNextActivity());
                skipBtn.setVisibility(View.INVISIBLE);
            }
        }

        @Override
        public void onPageScrollStateChanged(int state) {
        }
    };

    private void updateIndicatorColor(View v, boolean active) {
        if (v.getBackground() instanceof GradientDrawable) {
            GradientDrawable gd = (GradientDrawable) v.getBackground();
            int color = ContextCompat.getColor(this, active ? R.color.brand_pink : R.color.track_unchecked_color);
            gd.setColor(color);
        }
    }

    private void moveToNextActivity() {
        getSharedPreferences("PREFERENCE", MODE_PRIVATE).edit().putBoolean("isFirstRun", false).apply();
        Intent homeIntent = new Intent(OnBoarding.this, HomeActivity.class);
        if (getIntent().getExtras() != null) {
            homeIntent.putExtras(getIntent().getExtras());
        }
        startActivity(homeIntent);
        finish();
    }
}
