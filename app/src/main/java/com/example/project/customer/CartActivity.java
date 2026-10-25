package com.example.project.customer;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.project.R;
import com.example.project.adapters.CartAdapter;
import com.example.project.data.FirestoreRepository;
import com.example.project.models.CartItem;
import com.example.project.models.User;
import com.example.project.utils.FormatUtils;

import java.util.ArrayList;
import java.util.List;

public class CartActivity extends AppCompatActivity {

    private RecyclerView rvCartItems;
    private LinearLayout layoutEmptyCart;
    private TextView tvSubtotal, tvDeliveryFee, tvFinalTotal;
    private Button btnProceedCheckout;
    private ImageButton btnBack;

    private FirestoreRepository repository;
    private CartAdapter adapter;
    private List<CartItem> cartItems = new ArrayList<>();
    private User currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.cart);

        repository = FirestoreRepository.getInstance(this);
        currentUser = repository.getCurrentUser();

        rvCartItems = findViewById(R.id.rvCartItems);
        layoutEmptyCart = findViewById(R.id.layoutEmptyCart);
        tvSubtotal = findViewById(R.id.tvSubtotal);
        tvDeliveryFee = findViewById(R.id.tvDeliveryFee);
        tvFinalTotal = findViewById(R.id.tvFinalTotal);
        btnProceedCheckout = findViewById(R.id.btnProceedCheckout);
        btnBack = findViewById(R.id.btnBackCart);

        btnBack.setOnClickListener(v -> finish());

        rvCartItems.setLayoutManager(new LinearLayoutManager(this));
        adapter = new CartAdapter(this, cartItems, new CartAdapter.OnCartChangeListener() {
            @Override
            public void onQuantityChange(CartItem item, int newQuantity) {
                item.setQuantity(newQuantity);
                saveCartChanges();
            }

            @Override
            public void onRemoveItem(CartItem item) {
                cartItems.remove(item);
                saveCartChanges();
            }
        });
        rvCartItems.setAdapter(adapter);

        btnProceedCheckout.setOnClickListener(v -> {
            if (cartItems.isEmpty()) {
                Toast.makeText(CartActivity.this, "Cart is empty", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent intent = new Intent(CartActivity.this, CheckoutActivity.class);
            startActivity(intent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadCart();
    }

    private void loadCart() {
        String userId = currentUser != null ? currentUser.getUid() : "guest";
        repository.getCart(userId, new FirestoreRepository.Callback<List<CartItem>>() {
            @Override
            public void onSuccess(List<CartItem> items) {
                cartItems.clear();
                cartItems.addAll(items);
                updateCartUI();
            }

            @Override
            public void onError(String error) {
                Toast.makeText(CartActivity.this, "Error loading cart: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveCartChanges() {
        String userId = currentUser != null ? currentUser.getUid() : "guest";
        repository.saveCart(userId, cartItems, new FirestoreRepository.Callback<Void>() {
            @Override
            public void onSuccess(Void result) {
                updateCartUI();
            }

            @Override
            public void onError(String error) {
                Toast.makeText(CartActivity.this, "Failed to save cart: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateCartUI() {
        if (cartItems.isEmpty()) {
            rvCartItems.setVisibility(View.GONE);
            layoutEmptyCart.setVisibility(View.VISIBLE);
            btnProceedCheckout.setEnabled(false);

            tvSubtotal.setText(FormatUtils.formatPrice(0.0));
            tvDeliveryFee.setText(FormatUtils.formatPrice(0.0));
            tvFinalTotal.setText(FormatUtils.formatPrice(0.0));
        } else {
            rvCartItems.setVisibility(View.VISIBLE);
            layoutEmptyCart.setVisibility(View.GONE);
            btnProceedCheckout.setEnabled(true);

            double subtotal = 0.0;
            for (CartItem item : cartItems) {
                subtotal += item.getItemTotal();
            }

            double deliveryFee = subtotal > 500.0 || subtotal == 0 ? 0.0 : 50.0;
            double grandTotal = subtotal + deliveryFee;

            tvSubtotal.setText(FormatUtils.formatPrice(subtotal));
            tvDeliveryFee.setText(FormatUtils.formatPrice(deliveryFee));
            tvFinalTotal.setText(FormatUtils.formatPrice(grandTotal));
        }
        adapter.notifyDataSetChanged();
    }
}
