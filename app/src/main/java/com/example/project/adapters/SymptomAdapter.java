package com.example.project.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.project.R;
import com.example.project.models.Symptom;

import java.util.List;
import java.util.Objects;

public class SymptomAdapter extends ListAdapter<Symptom, SymptomAdapter.ViewHolder> {

    public interface OnSymptomClickListener {
        void onSymptomClick(Symptom symptom);
    }

    private static final DiffUtil.ItemCallback<Symptom> DIFF_CALLBACK = new DiffUtil.ItemCallback<Symptom>() {
        @Override
        public boolean areItemsTheSame(@NonNull Symptom oldItem, @NonNull Symptom newItem) {
            return Objects.equals(oldItem.getId(), newItem.getId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull Symptom oldItem, @NonNull Symptom newItem) {
            return Objects.equals(oldItem.getName(), newItem.getName()) &&
                    oldItem.isSelected() == newItem.isSelected();
        }
    };

    private final Context context;
    private final OnSymptomClickListener listener;

    public SymptomAdapter(Context context, OnSymptomClickListener listener) {
        super(DIFF_CALLBACK);
        this.context = context;
        this.listener = listener;
    }

    public SymptomAdapter(Context context, List<Symptom> initialList, OnSymptomClickListener listener) {
        super(DIFF_CALLBACK);
        this.context = context;
        this.listener = listener;
        if (initialList != null) {
            submitList(initialList);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_popular_symptom, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Symptom symptom = getItem(position);
        holder.tvName.setText(symptom.getName());

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onSymptomClick(symptom);
        });
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvPopularSymptomName);
        }
    }
}
