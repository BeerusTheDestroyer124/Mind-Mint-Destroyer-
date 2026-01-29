package com.gxdevs.mindmint.Adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.gxdevs.mindmint.Models.UpdateLogItem;
import com.gxdevs.mindmint.R;
import java.util.List;

public class UpdateLogAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final List<UpdateLogItem> items;

    private static final int TYPE_TOGGLE = 3;
    private boolean isExpanded = false;
    private int cutoffIndex = -1;

    public UpdateLogAdapter(List<UpdateLogItem> items) {
        this.items = items;
        // Find split point (index of second header)
        int headerCount = 0;
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).getType() == UpdateLogItem.TYPE_HEADER) {
                headerCount++;
                if (headerCount == 2) {
                    cutoffIndex = i;
                    break;
                }
            }
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (cutoffIndex != -1) {
            int displayedCount = isExpanded ? items.size() : cutoffIndex;
            if (position == displayedCount) {
                return TYPE_TOGGLE;
            }
        }
        return items.get(position).getType();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == UpdateLogItem.TYPE_HEADER) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_update_log_version, parent,
                    false);
            return new HeaderViewHolder(view);
        } else if (viewType == TYPE_TOGGLE) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_update_log_toggle, parent,
                    false);
            return new ToggleViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_update_log_change, parent,
                    false);
            return new ItemViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof ToggleViewHolder) {
            ToggleViewHolder th = (ToggleViewHolder) holder;
            th.text.setText(isExpanded ? "Show Less" : "Load more history");
            th.itemView.setOnClickListener(v -> {
                isExpanded = !isExpanded;
                notifyDataSetChanged();
            });
            return;
        }

        UpdateLogItem item = items.get(position);
        if (holder instanceof HeaderViewHolder) {
            ((HeaderViewHolder) holder).title.setText(item.getText());
        } else if (holder instanceof ItemViewHolder) {
            ((ItemViewHolder) holder).text.setText(item.getText());
        }
    }

    @Override
    public int getItemCount() {
        if (cutoffIndex == -1) return items.size();
        return isExpanded ? items.size() + 1 : cutoffIndex + 1;
    }

    static class HeaderViewHolder extends RecyclerView.ViewHolder {
        TextView title;

        public HeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.versionTitle);
        }
    }

    static class ItemViewHolder extends RecyclerView.ViewHolder {
        TextView text;

        public ItemViewHolder(@NonNull View itemView) {
            super(itemView);
            text = itemView.findViewById(R.id.logText);
        }
    }

    static class ToggleViewHolder extends RecyclerView.ViewHolder {
        TextView text;

        public ToggleViewHolder(@NonNull View itemView) {
            super(itemView);
            text = itemView.findViewById(R.id.toggleText);
        }
    }
}
