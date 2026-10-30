package com.example.project.admin;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.project.R;
import com.example.project.adapters.OrderAdapter;
import com.example.project.auth.AdminLoginActivity;
import com.example.project.data.MediRepository;
import com.example.project.models.Order;
import com.example.project.models.User;
import com.example.project.utils.PdfGenerator;
import com.example.project.utils.SessionManager;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class AdminOrdersActivity extends AppCompatActivity {

    private RecyclerView rvOrders;
    private TextView tvNoOrders;
    private ImageButton btnBack;
    private Button btnAll, btnPending, btnConfirmed, btnPreparing, btnCompleted, btnCancelled;

    private MediRepository repository;
    private SessionManager sessionManager;
    private OrderAdapter adapter;
    private final List<Order> allOrders = new ArrayList<>();
    private final List<Order> displayedOrders = new ArrayList<>();
    private String currentFilter = "All";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Role Guard
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

        setContentView(R.layout.order_page);

        rvOrders = findViewById(R.id.rvAdminOrders);
        tvNoOrders = findViewById(R.id.textView28);
        btnBack = findViewById(R.id.btnBackOrderPage);

        btnAll = findViewById(R.id.btnFilterAll);
        btnPending = findViewById(R.id.btnFilterPending);
        btnConfirmed = findViewById(R.id.btnFilterConfirmed);
        btnPreparing = findViewById(R.id.btnFilterPreparing);
        btnCompleted = findViewById(R.id.btnFilterCompleted);
        btnCancelled = findViewById(R.id.btnFilterCancelled);

        btnBack.setOnClickListener(v -> finish());
        setupFilterListeners();

        rvOrders.setLayoutManager(new LinearLayoutManager(this));
        adapter = new OrderAdapter(this, new OrderAdapter.OnOrderClickListener() {
            @Override
            public void onOrderClick(Order order) {
                showOrderStatusDialog(order);
            }

            @Override
            public void onViewInvoiceClick(Order order) {
                File pdfFile = PdfGenerator.generateInvoicePdf(AdminOrdersActivity.this, order);
                if (pdfFile != null) {
                    PdfGenerator.openPdf(AdminOrdersActivity.this, pdfFile);
                }
            }
        });
        rvOrders.setAdapter(adapter);
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
        loadOrders();
    }

    private void setupFilterListeners() {
        btnAll.setOnClickListener(v -> applyFilter("All"));
        btnPending.setOnClickListener(v -> applyFilter("Pending"));
        btnConfirmed.setOnClickListener(v -> applyFilter("Confirmed"));
        btnPreparing.setOnClickListener(v -> applyFilter("Preparing"));
        btnCompleted.setOnClickListener(v -> applyFilter("Completed"));
        btnCancelled.setOnClickListener(v -> applyFilter("Cancelled"));
    }

    private void applyFilter(String filter) {
        currentFilter = filter;
        filterOrders();
    }

    private void loadOrders() {
        repository.getAllOrders(new MediRepository.Callback<List<Order>>() {
            @Override
            public void onSuccess(List<Order> orders) {
                allOrders.clear();
                allOrders.addAll(orders);
                filterOrders();
            }

            @Override
            public void onError(String error) {
                Toast.makeText(AdminOrdersActivity.this, "Failed to load orders: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void filterOrders() {
        displayedOrders.clear();
        if ("All".equalsIgnoreCase(currentFilter)) {
            displayedOrders.addAll(allOrders);
        } else {
            for (Order o : allOrders) {
                if (currentFilter.equalsIgnoreCase(o.getStatus())) {
                    displayedOrders.add(o);
                }
            }
        }

        if (displayedOrders.isEmpty()) {
            tvNoOrders.setVisibility(View.VISIBLE);
            rvOrders.setVisibility(View.GONE);
        } else {
            tvNoOrders.setVisibility(View.GONE);
            rvOrders.setVisibility(View.VISIBLE);
        }
        adapter.submitList(new ArrayList<>(displayedOrders));
    }

    private void showOrderStatusDialog(Order order) {
        String[] statuses = {"Pending", "Confirmed", "Preparing", "Completed", "Cancelled"};
        new AlertDialog.Builder(this)
                .setTitle("Update Status: Order #" + order.getOrderId())
                .setItems(statuses, (dialog, which) -> {
                    String selectedStatus = statuses[which];
                    if (!selectedStatus.equalsIgnoreCase(order.getStatus())) {
                        updateOrderStatus(order, selectedStatus);
                    }
                })
                .show();
    }

    private void updateOrderStatus(Order order, String newStatus) {
        repository.updateOrderStatus(order, newStatus, new MediRepository.Callback<Void>() {
            @Override
            public void onSuccess(Void result) {
                Toast.makeText(AdminOrdersActivity.this, "Order updated to " + newStatus, Toast.LENGTH_SHORT).show();
                loadOrders();
            }

            @Override
            public void onError(String error) {
                Toast.makeText(AdminOrdersActivity.this, "Error updating status: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
