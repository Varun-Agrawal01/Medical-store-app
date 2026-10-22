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
import com.example.project.models.Order;
import com.example.project.models.OrderItem;
import com.example.project.utils.FormatUtils;

import java.util.List;

public class OrderAdapter extends RecyclerView.Adapter<OrderAdapter.ViewHolder> {

    public interface OnOrderClickListener {
        void onOrderClick(Order order);
        void onViewInvoiceClick(Order order);
    }

    private final Context context;
    private final List<Order> orderList;
    private final OnOrderClickListener listener;

    public OrderAdapter(Context context, List<Order> orderList, OnOrderClickListener listener) {
        this.context = context;
        this.orderList = orderList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_order, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Order order = orderList.get(position);
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

    @Override
    public int getItemCount() {
        return orderList != null ? orderList.size() : 0;
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
