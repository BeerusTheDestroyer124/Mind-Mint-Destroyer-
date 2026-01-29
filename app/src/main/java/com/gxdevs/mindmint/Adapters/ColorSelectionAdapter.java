package com.gxdevs.mindmint.Adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.gxdevs.mindmint.R;

import java.util.List;

public class ColorSelectionAdapter extends RecyclerView.Adapter<ColorSelectionAdapter.ColorViewHolder> {

    public interface OnColorSelectedListener {
        void onColorSelected(int[] colorPair);
    }

    private final List<int[]> colorPairs;
    private final OnColorSelectedListener listener;
    private int selectedPosition = -1;

    public ColorSelectionAdapter(List<int[]> colorPairs, OnColorSelectedListener listener) {
        this.colorPairs = colorPairs;
        this.listener = listener;
    }

    public void setSelectedColorPair(int[] colorPair) {
        for (int i = 0; i < colorPairs.size(); i++) {
            if (colorPairs.get(i)[0] == colorPair[0] && colorPairs.get(i)[1] == colorPair[1]) {
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
    public ColorViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_color_selection, parent, false);
        return new ColorViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ColorViewHolder holder, int position) {
        int[] colorPair = colorPairs.get(position);
        int backgroundColor = colorPair[1]; // Use background tint color
        
        holder.colorView.setBackgroundTintList(android.content.res.ColorStateList.valueOf(backgroundColor));
        
        boolean isSelected = position == selectedPosition;
        
        // Set initial scale
        float scale = isSelected ? 1.3f : 1.0f;
        holder.itemView.setScaleX(scale);
        holder.itemView.setScaleY(scale);
        
        holder.itemView.setOnClickListener(v -> {
            int oldPos = selectedPosition;
            selectedPosition = position;
            if (oldPos != -1) {
                notifyItemChanged(oldPos);
            }
            notifyItemChanged(selectedPosition);
            
            if (listener != null) listener.onColorSelected(colorPair);
        });
    }

    @Override
    public int getItemCount() {
        return colorPairs.size();
    }

    static class ColorViewHolder extends RecyclerView.ViewHolder {
        View colorView;

        ColorViewHolder(@NonNull View itemView) {
            super(itemView);
            colorView = itemView.findViewById(R.id.colorView);
        }
    }
}

