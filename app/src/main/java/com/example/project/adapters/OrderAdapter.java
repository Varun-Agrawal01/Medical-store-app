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
import com.example.project.models.Order;
import com.example.project.models.OrderItem;
import com.example.project.utils.FormatUtils;

import java.util.List;
import java.util.Objects;

public class OrderAdapter extends ListAdapter<Order, OrderAdapter.ViewHolder> {

    public interface OnOrderClickListener {
        void onOrderClick(Order order);
        void onViewInvoiceClick(Order order);
    }

    private static final DiffUtil.ItemCallback<Order> DIFF_CALLBACK = new DiffUtil.ItemCallback<Order>() {
        @Override
        public boolean areItemsTheSame(@NonNull Order oldItem, @NonNull Order newItem) {
            return Objects.equals(oldItem.getOrderId(), newItem.getOrderId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull Order oldItem, @NonNull Order newItem) {
            return Objects.equals(oldItem.getStatus(), newItem.getStatus()) &&
                    Double.compare(oldItem.getTotal(), newItem.getTotal()) == 0 &&
                    oldItem.getUpdatedAt() == newItem.getUpdatedAt();
        }
    };

    private final Context context;
    private final OnOrderClickListener listener;

    public OrderAdapter(Context context, OnOrderClickListener listener) {
        super(DIFF_CALLBACK);
        this.context = context;
        this.listener = listener;
    }

    public OrderAdapter(Context context, List<Order> initialList, OnOrderClickListener listener) {
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
        View view = LayoutInflater.from(context).inflate(R.layout.item_order, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Order order = getItem(position);
        holder.tvOrderId.setText("Order #" + order.getOrderId());
        holder.tvOrderStatus.setText(order.getStatus());

        String status = order.getStatus() != null ? order.getStatus() : "Pending";
        switch (status.toLowerCase()) {
            case "completed":
                holder.tvOrderStatus.setTextColor(Color.parseColor("#2E7D32"));
                break;
            case "cancelled":
                holder.tvOrderStatus.setTextColor(Color.parseColor("#C62828"));
                break;
            case "confirmed":
            case "preparing":
                holder.tvOrderStatus.setTextColor(Color.parseColor("#0288D1"));
                break;
            default:
                holder.tvOrderStatus.setTextColor(Color.parseColor("#EF6C00"));
                break;
        }

        holder.tvOrderDate.setText(FormatUtils.formatDate(order.getCreatedAt()));
        holder.tvCustomerInfo.setText("Customer: " + order.getCustomerName() + " (" + order.getCustomerPhone() + ")");

        StringBuilder sb = new StringBuilder();
        if (order.getItems() != null) {
            sb.append(order.getItems().size()).append(" Items: ");
            for (int i = 0; i < Math.min(order.getItems().size(), 3); i++) {
                OrderItem item = order.getItems().get(i);
                if (i > 0) sb.append(", ");
                sb.append(item.getMedicineName()).append(" x").append(item.getQuantity());
            }
            if (order.getItems().size() > 3) sb.append("...");
        }
        holder.tvSummary.setText(sb.toString());
        holder.tvTotal.setText("Total: " + FormatUtils.formatPrice(order.getTotal()));

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onOrderClick(order);
        });

        holder.btnInvoice.setOnClickListener(v -> {
            if (listener != null) listener.onViewInvoiceClick(order);
        });
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvOrderId, tvOrderStatus, tvOrderDate, tvCustomerInfo, tvSummary, tvTotal;
        Button btnInvoice;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvOrderId = itemView.findViewById(R.id.tvOrderId);
            tvOrderStatus = itemView.findViewById(R.id.tvOrderStatus);
            tvOrderDate = itemView.findViewById(R.id.tvOrderDate);
            tvCustomerInfo = itemView.findViewById(R.id.tvCustomerInfo);
            tvSummary = itemView.findViewById(R.id.tvOrderSummary);
            tvTotal = itemView.findViewById(R.id.tvOrderTotal);
            btnInvoice = itemView.findViewById(R.id.btnViewInvoice);
        }
    }
}
