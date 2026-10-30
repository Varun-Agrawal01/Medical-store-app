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
import com.example.project.auth.AdminLoginActivity;
import com.example.project.data.MediRepository;
import com.example.project.models.Medicine;
import com.example.project.models.Order;
import com.example.project.models.User;
import com.example.project.utils.SessionManager;

import java.util.List;

public class AdminDashboardActivity extends AppCompatActivity {

    private TextView tvActiveCount, tvLowStockCount, tvOutOfStockCount, tvPendingOrdersCount;
    private Button btnManageInventory, btnViewOrders;
    private ImageButton btnAdminLogout;
    private MediRepository repository;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Role Guard: redirects to admin login if current session user is not an admin
        sessionManager = new SessionManager(this);
        repository = MediRepository.getInstance(this);

        String currentUserId = sessionManager.getUserId();
        if (currentUserId == null) {
            redirectToLogin();
            return;
        }

        repository.getUserById(currentUserId, new MediRepository.Callback<User>() {
            @Override
            public void onSuccess(User user) {
                if (user == null || !"admin".equalsIgnoreCase(user.getRole())) {
                    redirectToLogin();
                }
            }

            @Override
            public void onError(String error) {
                redirectToLogin();
            }
        });

        setContentView(R.layout.admin_dashboard);

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
            sessionManager.clear();
            Toast.makeText(this, "Admin Logged Out", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(AdminDashboardActivity.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    private void redirectToLogin() {
        Intent intent = new Intent(this, AdminLoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadDashboardStats();
    }

    private void loadDashboardStats() {
        repository.getMedicines(new MediRepository.Callback<List<Medicine>>() {
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
                // Silent
            }
        });

        repository.getAllOrders(new MediRepository.Callback<List<Order>>() {
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
                // Silent
            }
        });
    }
}
