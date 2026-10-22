package com.example.project.admin;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.project.MainActivity;
import com.example.project.R;
import com.example.project.data.FirestoreRepository;
import com.example.project.models.Medicine;
import com.example.project.models.Order;

import java.util.List;

public class AdminDashboardActivity extends AppCompatActivity {

    private TextView tvActiveCount, tvLowStockCount, tvOutOfStockCount, tvPendingOrdersCount;
    private Button btnManageInventory, btnViewOrders;
    private ImageButton btnAdminLogout;
    private FirestoreRepository repository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.admin_dashboard);

        repository = FirestoreRepository.getInstance(this);

        tvActiveCount = findViewById(R.id.tvActiveCount);
        tvLowStockCount = findViewById(R.id.tvLowStockCount);
        tvOutOfStockCount = findViewById(R.id.tvOutOfStockCount);
        tvPendingOrdersCount = findViewById(R.id.tvPendingOrdersCount);

        btnManageInventory = findViewById(R.id.button10);
        btnViewOrders = findViewById(R.id.button9);
        btnAdminLogout = findViewById(R.id.btnAdminLogout);

        btnManageInventory.setOnClickListener(v -> {
            Intent intent = new Intent(AdminDashboardActivity.this, InventoryActivity.class);
            startActivity(intent);
        });

        btnViewOrders.setOnClickListener(v -> {
            Intent intent = new Intent(AdminDashboardActivity.this, AdminOrdersActivity.class);
            startActivity(intent);
        });

        btnAdminLogout.setOnClickListener(v -> {
            repository.logout();
            Toast.makeText(this, "Admin Logged Out", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(AdminDashboardActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadDashboardStats();
    }

    private void loadDashboardStats() {
        repository.getMedicines(new FirestoreRepository.Callback<List<Medicine>>() {
            @Override
            public void onSuccess(List<Medicine> medicines) {
                int active = 0;
                int lowStock = 0;
                int outOfStock = 0;

                for (Medicine m : medicines) {
                    if (m.isActive()) active++;
                    if (m.getStock() == 0) outOfStock++;
                    else if (m.getStock() < 10) lowStock++;
                }

                tvActiveCount.setText(String.valueOf(active));
                tvLowStockCount.setText(String.valueOf(lowStock));
                tvOutOfStockCount.setText(String.valueOf(outOfStock));
            }

            @Override
            public void onError(String error) {
                // Ignore silent stats error
            }
        });

        repository.getAllOrders(new FirestoreRepository.Callback<List<Order>>() {
            @Override
            public void onSuccess(List<Order> orders) {
                int pending = 0;
                for (Order o : orders) {
                    if ("Pending".equalsIgnoreCase(o.getStatus())) {
                        pending++;
                    }
                }
                tvPendingOrdersCount.setText(String.valueOf(pending));
            }

            @Override
            public void onError(String error) {
                // Ignore silent stats error
            }
        });
    }
}
