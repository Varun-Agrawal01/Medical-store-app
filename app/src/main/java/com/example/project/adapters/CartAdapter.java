package com.example.project.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.project.R;
import com.example.project.models.CartItem;
import com.example.project.utils.FormatUtils;

import java.util.List;

public class CartAdapter extends RecyclerView.Adapter<CartAdapter.ViewHolder> {

    public interface OnCartChangeListener {
        void onQuantityChange(CartItem item, int newQuantity);
        void onRemoveItem(CartItem item);
    }

    private final Context context;
    private final List<CartItem> cartList;
    private final OnCartChangeListener listener;

    public CartAdapter(Context context, List<CartItem> cartList, OnCartChangeListener listener) {
        this.context = context;
        this.cartList = cartList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_cart, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CartItem item = cartList.get(position);
        holder.tvName.setText(item.getMedicineName());
        holder.tvUnitPrice.setText(FormatUtils.formatPrice(item.getPrice()) + " each");
        holder.tvQuantity.setText(String.valueOf(item.getQuantity()));
        holder.tvItemTotal.setText("Total: " + FormatUtils.formatPrice(item.getItemTotal()));

        holder.btnMinus.setOnClickListener(v -> {
            if (item.getQuantity() > 1 && listener != null) {
                listener.onQuantityChange(item, item.getQuantity() - 1);
            }
        });

        holder.btnPlus.setOnClickListener(v -> {
            if (listener != null) {
                listener.onQuantityChange(item, item.getQuantity() + 1);
            }
        });

        holder.btnRemove.setOnClickListener(v -> {
            if (listener != null) {
                listener.onRemoveItem(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return cartList != null ? cartList.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvUnitPrice, tvQuantity, tvItemTotal;
        Button btnMinus, btnPlus;
        ImageButton btnRemove;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvCartMedicineName);
            tvUnitPrice = itemView.findViewById(R.id.tvCartUnitPrice);
            tvQuantity = itemView.findViewById(R.id.tvCartQuantity);
            tvItemTotal = itemView.findViewById(R.id.tvCartItemTotal);
            btnMinus = itemView.findViewById(R.id.btnMinus);
            btnPlus = itemView.findViewById(R.id.btnPlus);
            btnRemove = itemView.findViewById(R.id.btnRemove);
        }
    }
}
