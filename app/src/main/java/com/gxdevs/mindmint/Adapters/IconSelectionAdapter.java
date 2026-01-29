package com.gxdevs.mindmint.Adapters;

import android.content.Context;
import android.graphics.Color;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.gxdevs.mindmint.R;

import java.util.List;

public class IconSelectionAdapter extends RecyclerView.Adapter<IconSelectionAdapter.IconViewHolder> {

    public interface OnIconSelectedListener {
        void onIconSelected(int iconRes);
    }

    private final List<Integer> iconDrawables;
    private final OnIconSelectedListener listener;
    private int selectedPosition = -1;
    private int selectedIconRes = 0;
    private final Context context;

    public IconSelectionAdapter(Context context, List<Integer> iconDrawables, OnIconSelectedListener listener) {
        this.iconDrawables = iconDrawables;
        this.listener = listener;
        this.context = context;
    }

    public void setSelectedIcon(int iconRes) {
        selectedIconRes = iconRes;
        for (int i = 0; i < iconDrawables.size(); i++) {
            if (iconDrawables.get(i) == iconRes) {
                int oldPos = selectedPosition;
                selectedPosition = i;
                if (oldPos != -1) notifyItemChanged(oldPos);
                notifyItemChanged(selectedPosition);
                break;
            }
        }
    }

    @NonNull
    @Override
    public IconViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_icon_mini, parent, false);
        return new IconViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull IconViewHolder holder, int position) {
        int iconRes = iconDrawables.get(position);
        holder.iconView.setImageResource(iconRes);

        boolean isSelected = (position == selectedPosition) || (selectedIconRes == iconRes && selectedPosition == -1);

        if (isSelected) {
            holder.itemView.setBackgroundResource(R.drawable.bg_segment_selected);
            holder.iconView.setColorFilter(getAttrColor(R.attr.text_primary));
            holder.iconView.setAlpha(1.0f);
        } else {
            holder.itemView.setBackgroundResource(R.drawable.glass_circle);
            holder.iconView.setColorFilter(getAttrColor(R.attr.text_primary));
            holder.iconView.setAlpha(0.5f);
        }

        holder.itemView.setOnClickListener(v -> {
            int oldPos = selectedPosition;
            selectedPosition = position;
            selectedIconRes = iconRes;
            if (oldPos != -1) notifyItemChanged(oldPos);
            notifyItemChanged(selectedPosition);
            if (listener != null) listener.onIconSelected(iconRes);
        });
    }

    private int getAttrColor(int attr) {
        TypedValue typedValue = new TypedValue();
        context.getTheme().resolveAttribute(attr, typedValue, true);
        return typedValue.data;
    }

    @Override
    public int getItemCount() {
        return iconDrawables.size();
    }

    static class IconViewHolder extends RecyclerView.ViewHolder {
        ImageView iconView;

        IconViewHolder(@NonNull View itemView) {
            super(itemView);
            iconView = itemView.findViewById(R.id.ivIcon);
        }
    }
}

