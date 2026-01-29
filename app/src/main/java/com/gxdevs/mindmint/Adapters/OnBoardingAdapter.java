package com.gxdevs.mindmint.Adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.viewpager.widget.PagerAdapter;

import com.google.android.material.textview.MaterialTextView;
import com.gxdevs.mindmint.R;

public class OnBoardingAdapter extends PagerAdapter {
    Context context;

    int[] images = {
            R.drawable.s1,
            R.drawable.s2,
            R.drawable.s3,
            R.drawable.s4,
            R.drawable.s5
    };

    int[] headings = {
            R.string.onboarding_heading1,
            R.string.onboarding_heading2,
            R.string.onboarding_heading3,
            R.string.onboarding_heading4,
            R.string.onboarding_heading5
    };

    int[] descriptions = {
            R.string.onboarding_desc1,
            R.string.onboarding_desc2,
            R.string.onboarding_desc3,
            R.string.onboarding_desc4,
            R.string.onboarding_desc5
    };

    public OnBoardingAdapter(Context context) {
        this.context = context;
    }

    @Override
    public int getCount() {
        return headings.length;
    }

    @Override
    public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
        return view == object;
    }

    @NonNull
    @Override
    public Object instantiateItem(@NonNull ViewGroup container, int position) {
        LayoutInflater layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = layoutInflater.inflate(R.layout.slides_layout, container, false);

        ImageView sliderImg = view.findViewById(R.id.sliderImg);
        MaterialTextView heading = view.findViewById(R.id.sliderHead);
        MaterialTextView desc = view.findViewById(R.id.sliderDesc);

        sliderImg.setImageResource(images[position]);
        heading.setText(headings[position]);
        desc.setText(descriptions[position]);
        // Apply layout changes based on the slide position

        container.addView(view);
        view.setTag("view" + position);
        return view;
    }

    @Override
    public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
        container.removeView((View) object);
    }
}
