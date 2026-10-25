package com.example.project.customer;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.project.R;
import com.example.project.data.FirestoreRepository;
import com.example.project.models.CartItem;
import com.example.project.models.Order;
import com.example.project.models.User;
import com.example.project.utils.FormatUtils;

import java.util.ArrayList;
import java.util.List;

public class CheckoutActivity extends AppCompatActivity {

    private EditText etName, etPhone, etAddress;
    private TextView tvSubtotal, tvDeliveryFee, tvTotal;
    private Button btnPlaceOrder;
    private ImageButton btnBack;

    private FirestoreRepository repository;
    private User currentUser;
    private List<CartItem> cartItems = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_checkout);

        repository = FirestoreRepository.getInstance(this);
        currentUser = repository.getCurrentUser();

        btnBack = findViewById(R.id.btnBackCheckout);
        etName = findViewById(R.id.etCheckoutName);
        etPhone = findViewById(R.id.etCheckoutPhone);
        etAddress = findViewById(R.id.etCheckoutAddress);

        tvSubtotal = findViewById(R.id.tvCheckoutSubtotal);
        tvDeliveryFee = findViewById(R.id.tvCheckoutDeliveryFee);
        tvTotal = findViewById(R.id.tvCheckoutTotal);
        btnPlaceOrder = findViewById(R.id.btnPlaceOrder);

        btnBack.setOnClickListener(v -> finish());

        if (currentUser != null) {
            if (currentUser.getName() != null) etName.setText(currentUser.getName());
            if (currentUser.getPhone() != null) etPhone.setText(currentUser.getPhone());
        }

        btnPlaceOrder.setOnClickListener(v -> placeOrder());

        loadCartSummary();
    }

    private void loadCartSummary() {
        String userId = currentUser != null ? currentUser.getUid() : "guest";
        repository.getCart(userId, new FirestoreRepository.Callback<List<CartItem>>() {
            @Override
            public void onSuccess(List<CartItem> items) {
                if (items.isEmpty()) {
                    Toast.makeText(CheckoutActivity.this, "Cart is empty!", Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }

                cartItems.clear();
                cartItems.addAll(items);

                double subtotal = 0.0;
                for (CartItem ci : cartItems) {
                    subtotal += ci.getItemTotal();
                }

                double deliveryFee = subtotal > 500.0 || subtotal == 0 ? 0.0 : 50.0;
                double total = subtotal + deliveryFee;

                tvSubtotal.setText(FormatUtils.formatPrice(subtotal));
                tvDeliveryFee.setText(FormatUtils.formatPrice(deliveryFee));
                tvTotal.setText(FormatUtils.formatPrice(total));
            }

            @Override
            public void onError(String error) {
                Toast.makeText(CheckoutActivity.this, "Error: " + error, Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void placeOrder() {
        String name = etName.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String address = etAddress.getText().toString().trim();

        if (name.isEmpty() || phone.isEmpty() || address.isEmpty()) {
            Toast.makeText(this, "Please enter complete delivery details!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (phone.length() < 10) {
            Toast.makeText(this, "Please enter a valid 10-digit phone number", Toast.LENGTH_SHORT).show();
            return;
        }

        btnPlaceOrder.setEnabled(false);
        String userId = currentUser != null ? currentUser.getUid() : "guest";

        repository.placeOrderTransaction(userId, name, phone, address, cartItems, new FirestoreRepository.Callback<Order>() {
            @Override
            public void onSuccess(Order createdOrder) {
                btnPlaceOrder.setEnabled(true);
                Toast.makeText(CheckoutActivity.this, "Order Placed Successfully!", Toast.LENGTH_SHORT).show();

                Intent intent = new Intent(CheckoutActivity.this, BillActivity.class);
                intent.putExtra("orderId", createdOrder.getOrderId());
                startActivity(intent);
                finish();
            }

            @Override
            public void onError(String error) {
                btnPlaceOrder.setEnabled(true);
                Toast.makeText(CheckoutActivity.this, "Checkout Failed: " + error, Toast.LENGTH_LONG).show();
            }
        });
    }
}
