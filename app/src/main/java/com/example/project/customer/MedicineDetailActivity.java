package com.example.project.customer;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.project.R;
import com.example.project.data.FirestoreRepository;
import com.example.project.models.CartItem;
import com.example.project.models.Medicine;
import com.example.project.models.User;
import com.example.project.utils.FormatUtils;

import java.util.List;

public class MedicineDetailActivity extends AppCompatActivity {

    private TextView tvName, tvGeneric, tvPrice, tvRxTag, tvStockStatus, tvDescription, tvQuantity;
    private Button btnMinus, btnPlus, btnAddToCart;
    private ImageButton btnBack;

    private FirestoreRepository repository;
    private String medicineId;
    private Medicine currentMedicine;
    private int selectedQuantity = 1;
    private User currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.medicine_detail);

        repository = FirestoreRepository.getInstance(this);
        currentUser = repository.getCurrentUser();

        btnBack = findViewById(R.id.btnBackDetail);
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
        repository.getMedicines(new FirestoreRepository.Callback<List<Medicine>>() {
            @Override
            public void onSuccess(List<Medicine> result) {
                for (Medicine m : result) {
                    if (m.getId().equals(medicineId)) {
                        currentMedicine = m;
                        displayMedicineInfo(m);
                        break;
                    }
                }
            }

            @Override
            public void onError(String error) {
                Toast.makeText(MedicineDetailActivity.this, "Error: " + error, Toast.LENGTH_SHORT).show();
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

        String userId = currentUser != null ? currentUser.getUid() : "guest";
        repository.getCart(userId, new FirestoreRepository.Callback<List<CartItem>>() {
            @Override
            public void onSuccess(List<CartItem> cartItems) {
                boolean exists = false;
                for (CartItem ci : cartItems) {
                    if (ci.getMedicineId().equals(currentMedicine.getId())) {
                        ci.setQuantity(ci.getQuantity() + selectedQuantity);
                        exists = true;
                        break;
                    }
                }
                if (!exists) {
                    cartItems.add(new CartItem(currentMedicine.getId(), currentMedicine.getName(), currentMedicine.getPrice(), selectedQuantity, currentMedicine.isRequiresPrescription()));
                }

                repository.saveCart(userId, cartItems, new FirestoreRepository.Callback<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        Toast.makeText(MedicineDetailActivity.this, selectedQuantity + " x " + currentMedicine.getName() + " added to cart!", Toast.LENGTH_SHORT).show();
                        finish();
                    }

                    @Override
                    public void onError(String error) {
                        Toast.makeText(MedicineDetailActivity.this, "Error: " + error, Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onError(String error) {
                Toast.makeText(MedicineDetailActivity.this, "Error: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
