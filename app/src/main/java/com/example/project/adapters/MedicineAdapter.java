package com.example.project.adapters;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.project.R;
import com.example.project.models.Medicine;
import com.example.project.utils.FormatUtils;

import java.util.List;

public class MedicineAdapter extends RecyclerView.Adapter<MedicineAdapter.ViewHolder> {

    public interface OnMedicineClickListener {
        void onMedicineClick(Medicine medicine);
        void onAddToCartClick(Medicine medicine);
        void onEditClick(Medicine medicine);
    }

    private final Context context;
    private final List<Medicine> medicineList;
    private final OnMedicineClickListener listener;
    private final boolean isAdminMode;

    public MedicineAdapter(Context context, List<Medicine> medicineList, boolean isAdminMode, OnMedicineClickListener listener) {
        this.context = context;
        this.medicineList = medicineList;
        this.isAdminMode = isAdminMode;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_medicine_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Medicine med = medicineList.get(position);
        holder.tvName.setText(med.getName());
        holder.tvGeneric.setText(med.getGenericName() != null ? med.getGenericName() : "");
        holder.tvPrice.setText(FormatUtils.formatPrice(med.getPrice()));

        if (med.isRequiresPrescription()) {
            holder.tvRxTag.setVisibility(View.VISIBLE);
        } else {
            holder.tvRxTag.setVisibility(View.GONE);
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

    @Override
    public int getItemCount() {
        return medicineList != null ? medicineList.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvGeneric, tvPrice, tvStockStatus, tvRxTag;
        Button btnAction;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvMedicineName);
            tvGeneric = itemView.findViewById(R.id.tvGenericName);
            tvPrice = itemView.findViewById(R.id.tvPrice);
            tvStockStatus = itemView.findViewById(R.id.tvStockStatus);
            tvRxTag = itemView.findViewById(R.id.tvRxTag);
            btnAction = itemView.findViewById(R.id.btnAction);
        }
    }
}
