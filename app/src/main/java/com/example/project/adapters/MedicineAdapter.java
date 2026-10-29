package com.example.project.adapters;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.project.R;
import com.example.project.models.Medicine;
import com.example.project.utils.FormatUtils;

import java.util.List;
import java.util.Objects;

/**
 * ListAdapter with DiffUtil for high performance medicine list rendering.
 */
public class MedicineAdapter extends ListAdapter<Medicine, MedicineAdapter.ViewHolder> {

    public interface OnMedicineClickListener {
        void onMedicineClick(Medicine medicine);
        void onAddToCartClick(Medicine medicine);
        void onEditClick(Medicine medicine);
    }

    private static final DiffUtil.ItemCallback<Medicine> DIFF_CALLBACK = new DiffUtil.ItemCallback<Medicine>() {
        @Override
        public boolean areItemsTheSame(@NonNull Medicine oldItem, @NonNull Medicine newItem) {
            return Objects.equals(oldItem.getId(), newItem.getId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull Medicine oldItem, @NonNull Medicine newItem) {
            return oldItem.getStock() == newItem.getStock() &&
                    oldItem.getPrice() == newItem.getPrice() &&
                    oldItem.isActive() == newItem.isActive() &&
                    oldItem.getMatchedSymptomsCount() == newItem.getMatchedSymptomsCount() &&
                    Objects.equals(oldItem.getName(), newItem.getName());
        }
    };

    private final Context context;
    private final boolean isAdminMode;
    private final OnMedicineClickListener listener;

    public MedicineAdapter(Context context, boolean isAdminMode, OnMedicineClickListener listener) {
        super(DIFF_CALLBACK);
        this.context = context;
        this.isAdminMode = isAdminMode;
        this.listener = listener;
    }

    public MedicineAdapter(Context context, List<Medicine> initialList, boolean isAdminMode, OnMedicineClickListener listener) {
        super(DIFF_CALLBACK);
        this.context = context;
        this.isAdminMode = isAdminMode;
        this.listener = listener;
        if (initialList != null) {
            submitList(initialList);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_medicine_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Medicine med = getItem(position);
        holder.tvName.setText(med.getName());
        holder.tvGeneric.setText(med.getGenericName() != null ? med.getGenericName() : "");
        holder.tvPrice.setText(FormatUtils.formatPrice(med.getPrice()));

        if (med.isRequiresPrescription()) {
            holder.tvRxTag.setVisibility(View.VISIBLE);
        } else {
            holder.tvRxTag.setVisibility(View.GONE);
        }

        if (med.getMatchedSymptomsCount() > 0) {
            holder.tvMatchedCount.setVisibility(View.VISIBLE);
            holder.tvMatchedCount.setText(context.getString(R.string.matched_symptoms_badge, med.getMatchedSymptomsCount()));
        } else {
            holder.tvMatchedCount.setVisibility(View.GONE);
        }

        if (!med.isActive()) {
            holder.tvStockStatus.setText("Inactive");
            holder.tvStockStatus.setTextColor(Color.GRAY);
            holder.btnAction.setEnabled(false);
            holder.btnAction.setText("Unavailable");
        } else if (med.getStock() > 0) {
            holder.tvStockStatus.setText("Available (" + med.getStock() + " in stock)");
            holder.tvStockStatus.setTextColor(Color.parseColor("#2E7D32"));
            holder.btnAction.setEnabled(true);
            holder.btnAction.setText(isAdminMode ? "Edit / Stock" : "Add to Cart");
        } else {
            holder.tvStockStatus.setText("Out of Stock");
            holder.tvStockStatus.setTextColor(Color.parseColor("#C62828"));
            holder.btnAction.setEnabled(isAdminMode);
            holder.btnAction.setText(isAdminMode ? "Refill Stock" : "Out of Stock");
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onMedicineClick(med);
        });

        holder.btnAction.setOnClickListener(v -> {
            if (listener != null) {
                if (isAdminMode) {
                    listener.onEditClick(med);
                } else {
                    listener.onAddToCartClick(med);
                }
            }
        });
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvGeneric, tvPrice, tvStockStatus, tvRxTag, tvMatchedCount;
        Button btnAction;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvMedicineName);
            tvGeneric = itemView.findViewById(R.id.tvGenericName);
            tvPrice = itemView.findViewById(R.id.tvPrice);
            tvStockStatus = itemView.findViewById(R.id.tvStockStatus);
            tvRxTag = itemView.findViewById(R.id.tvRxTag);
            tvMatchedCount = itemView.findViewById(R.id.tvMatchedSymptomsCount);
            btnAction = itemView.findViewById(R.id.btnAction);
        }
    }
}
