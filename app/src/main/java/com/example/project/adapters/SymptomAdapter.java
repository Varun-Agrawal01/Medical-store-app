package com.example.project.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.project.R;
import com.example.project.models.SymptomCategory;

import java.util.List;

public class SymptomAdapter extends RecyclerView.Adapter<SymptomAdapter.ViewHolder> {

    public interface OnCategoryClickListener {
        void onCategoryClick(SymptomCategory category);
    }

    private final Context context;
    private final List<SymptomCategory> categoryList;
    private final OnCategoryClickListener listener;

    public SymptomAdapter(Context context, List<SymptomCategory> categoryList, OnCategoryClickListener listener) {
        this.context = context;
        this.categoryList = categoryList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_symptom, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SymptomCategory cat = categoryList.get(position);
        holder.tvName.setText(cat.getName());
        holder.tvDescription.setText(cat.getDescription());

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onCategoryClick(cat);
        });
    }

    @Override
    public int getItemCount() {
        return categoryList != null ? categoryList.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvDescription;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvCategoryName);
            tvDescription = itemView.findViewById(R.id.tvCategoryDescription);
        }
    }
}
