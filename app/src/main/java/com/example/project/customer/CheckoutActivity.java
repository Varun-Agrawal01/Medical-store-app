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
import com.example.project.data.MediRepository;
import com.example.project.models.CartItem;
import com.example.project.models.Order;
import com.example.project.models.User;
import com.example.project.utils.FormatUtils;
import com.example.project.utils.PricingUtils;
import com.example.project.utils.SessionManager;

import java.util.ArrayList;
import java.util.List;

public class CheckoutActivity extends AppCompatActivity {

    private EditText etName, etPhone, etAddress;
    private TextView tvSubtotal, tvDeliveryFee, tvTotal;
    private Button btnPlaceOrder;
    private ImageButton btnBack;

    private MediRepository repository;
    private SessionManager sessionManager;
    private String currentUserId;
    private final List<CartItem> cartItems = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_checkout);

        repository = MediRepository.getInstance(this);
        sessionManager = new SessionManager(this);
        currentUserId = sessionManager.getUserId() != null ? sessionManager.getUserId() : "guest";

        btnBack = findViewById(R.id.btnBackCheckout);
        etName = findViewById(R.id.etCheckoutName);
        etPhone = findViewById(R.id.etCheckoutPhone);
        etAddress = findViewById(R.id.etCheckoutAddress);

        tvSubtotal = findViewById(R.id.tvCheckoutSubtotal);
        tvDeliveryFee = findViewById(R.id.tvCheckoutDeliveryFee);
        tvTotal = findViewById(R.id.tvCheckoutTotal);
        btnPlaceOrder = findViewById(R.id.btnPlaceOrder);

        btnBack.setOnClickListener(v -> finish());

        // Pre-fill full user row from DB
        if (sessionManager.getUserId() != null) {
            repository.getUserById(sessionManager.getUserId(), new MediRepository.Callback<User>() {
                @Override
                public void onSuccess(User user) {
                    if (user != null) {
                        if (user.getName() != null) etName.setText(user.getName());
                        if (user.getPhone() != null) etPhone.setText(user.getPhone());
                    }
                }

                @Override
                public void onError(String error) {
                    // Non-fatal
                }
            });
        }

        btnPlaceOrder.setOnClickListener(v -> placeOrder());
        loadCartSummary();
    }

    private void loadCartSummary() {
        repository.getCart(currentUserId, new MediRepository.Callback<List<CartItem>>() {
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

                double deliveryFee = PricingUtils.calculateDeliveryFee(subtotal);
                double total = PricingUtils.calculateTotal(subtotal, deliveryFee);

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
            Toast.makeText(this, "Please enter all delivery details", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!phone.matches("^[6-9][0-9]{9}$")) {
            Toast.makeText(this, getString(R.string.err_invalid_phone), Toast.LENGTH_SHORT).show();
            return;
        }

        btnPlaceOrder.setEnabled(false);
        btnPlaceOrder.setText("Processing Order...");

        repository.placeOrderTransaction(currentUserId, name, phone, address, cartItems, new MediRepository.Callback<Order>() {
            @Override
            public void onSuccess(Order createdOrder) {
                btnPlaceOrder.setEnabled(true);
                Toast.makeText(CheckoutActivity.this, "Order placed successfully!", Toast.LENGTH_LONG).show();
                Intent intent = new Intent(CheckoutActivity.this, BillActivity.class);
                intent.putExtra("orderId", createdOrder.getOrderId());
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                finish();
            }

            @Override
            public void onError(String error) {
                btnPlaceOrder.setEnabled(true);
                btnPlaceOrder.setText("Place Order");
                Toast.makeText(CheckoutActivity.this, "Order failed: " + error, Toast.LENGTH_LONG).show();
            }
        });
    }
}
