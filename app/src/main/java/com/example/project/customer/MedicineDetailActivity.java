package com.example.project.customer;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.project.R;
import com.example.project.data.MediRepository;
import com.example.project.models.CartItem;
import com.example.project.models.Medicine;
import com.example.project.models.User;
import com.example.project.utils.FormatUtils;
import com.example.project.utils.SessionManager;

import java.util.List;

public class MedicineDetailActivity extends AppCompatActivity {

    private TextView tvName, tvGeneric, tvPrice, tvRxTag, tvStockStatus, tvDescription, tvQuantity;
    private Button btnMinus, btnPlus, btnAddToCart;
    private ImageButton btnBack, btnCart;

    private MediRepository repository;
    private SessionManager sessionManager;
    private String medicineId;
    private Medicine currentMedicine;
    private int selectedQuantity = 1;
    private User currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.medicine_detail);

        repository = MediRepository.getInstance(this);
        sessionManager = new SessionManager(this);
        currentUser = repository.getCurrentUser();

        btnBack = findViewById(R.id.btnBackDetail);
        btnCart = findViewById(R.id.btnCartDetail);

        tvName = findViewById(R.id.tvDetailName);
        tvGeneric = findViewById(R.id.tvDetailGeneric);
        tvPrice = findViewById(R.id.tvDetailPrice);
        tvRxTag = findViewById(R.id.tvDetailRxTag);
        tvStockStatus = findViewById(R.id.tvDetailStockStatus);
        tvDescription = findViewById(R.id.tvDetailDescription);
        tvQuantity = findViewById(R.id.tvDetailQuantity);

        btnMinus = findViewById(R.id.btnDetailMinus);
        btnPlus = findViewById(R.id.btnDetailPlus);
        btnAddToCart = findViewById(R.id.btnAddToCartDetail);

        btnBack.setOnClickListener(v -> finish());
        btnCart.setOnClickListener(v -> startActivity(new Intent(MedicineDetailActivity.this, CartActivity.class)));

        if (getIntent().hasExtra("medicineId")) {
            medicineId = getIntent().getStringExtra("medicineId");
            loadMedicineDetails();
        } else {
            Toast.makeText(this, "Invalid Medicine ID", Toast.LENGTH_SHORT).show();
            finish();
        }

        btnMinus.setOnClickListener(v -> {
            if (selectedQuantity > 1) {
                selectedQuantity--;
                tvQuantity.setText(String.valueOf(selectedQuantity));
            }
        });

        btnPlus.setOnClickListener(v -> {
            if (currentMedicine != null && selectedQuantity < currentMedicine.getStock()) {
                selectedQuantity++;
                tvQuantity.setText(String.valueOf(selectedQuantity));
            } else if (currentMedicine != null) {
                Toast.makeText(this, "Cannot exceed available stock of " + currentMedicine.getStock(), Toast.LENGTH_SHORT).show();
            }
        });

        btnAddToCart.setOnClickListener(v -> addToCart());
    }

    private void loadMedicineDetails() {
        // Query single row directly rather than loading all medicines and looping
        repository.getMedicineById(medicineId, new MediRepository.Callback<Medicine>() {
            @Override
            public void onSuccess(Medicine m) {
                currentMedicine = m;
                displayMedicineInfo(m);
            }

            @Override
            public void onError(String error) {
                Toast.makeText(MedicineDetailActivity.this, "Error: " + error, Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void displayMedicineInfo(Medicine m) {
        tvName.setText(m.getName());
        tvGeneric.setText(m.getGenericName());
        tvPrice.setText(FormatUtils.formatPrice(m.getPrice()));
        tvDescription.setText(m.getDescription());

        if (m.isRequiresPrescription()) {
            tvRxTag.setVisibility(View.VISIBLE);
        } else {
            tvRxTag.setVisibility(View.GONE);
        }

        if (!m.isActive() || m.getStock() <= 0) {
            tvStockStatus.setText("Out of Stock");
            tvStockStatus.setTextColor(getColor(R.color.status_out_of_stock));
            btnAddToCart.setEnabled(false);
            btnAddToCart.setText("Out of Stock");
        } else {
            tvStockStatus.setText("Available (" + m.getStock() + " in stock)");
            tvStockStatus.setTextColor(getColor(R.color.status_available));
            btnAddToCart.setEnabled(true);
            btnAddToCart.setText("Add to Cart");
        }
    }

    private void addToCart() {
        if (currentMedicine == null || !currentMedicine.isAvailable()) {
            Toast.makeText(this, "Item is unavailable", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = sessionManager.getUserId() != null ? sessionManager.getUserId() : "guest";
        CartItem cartItem = new CartItem(
                currentMedicine.getId(),
                currentMedicine.getName(),
                currentMedicine.getPrice(),
                selectedQuantity,
                currentMedicine.isRequiresPrescription()
        );

        btnAddToCart.setEnabled(false);
        repository.addToCart(userId, cartItem, new MediRepository.Callback<Void>() {
            @Override
            public void onSuccess(Void result) {
                btnAddToCart.setEnabled(true);
                showAddedToCartDialog();
            }

            @Override
            public void onError(String error) {
                btnAddToCart.setEnabled(true);
                Toast.makeText(MedicineDetailActivity.this, "Failed to add to cart: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showAddedToCartDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Added to Cart")
                .setMessage(currentMedicine.getName() + " (" + selectedQuantity + ") has been added to your cart.")
                .setPositiveButton("View Cart", (dialog, which) -> {
                    Intent intent = new Intent(MedicineDetailActivity.this, CartActivity.class);
                    startActivity(intent);
                })
                .setNegativeButton("Continue Shopping", null)
                .show();
    }
}
