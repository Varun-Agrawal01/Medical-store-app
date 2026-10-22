package com.example.project.admin;

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
import com.example.project.data.FirestoreRepository;
import com.example.project.models.Order;
import com.example.project.utils.PdfGenerator;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class AdminOrdersActivity extends AppCompatActivity {

    private RecyclerView rvOrders;
    private TextView tvNoOrders;
    private ImageButton btnBack;
    private Button btnAll, btnPending, btnConfirmed, btnPreparing, btnCompleted, btnCancelled;

    private FirestoreRepository repository;
    private OrderAdapter adapter;
    private List<Order> allOrders = new ArrayList<>();
    private List<Order> displayedOrders = new ArrayList<>();
    private String currentFilter = "All";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.order_page);

        repository = FirestoreRepository.getInstance(this);

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
        adapter = new OrderAdapter(this, displayedOrders, new OrderAdapter.OnOrderClickListener() {
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
        repository.getAllOrders(new FirestoreRepository.Callback<List<Order>>() {
            @Override
            public void onSuccess(List<Order> result) {
                allOrders.clear();
                allOrders.addAll(result);
                filterOrders();
            }

            @Override
            public void onError(String error) {
                Toast.makeText(AdminOrdersActivity.this, "Error loading orders: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void filterOrders() {
        displayedOrders.clear();
        for (Order o : allOrders) {
            if ("All".equalsIgnoreCase(currentFilter) || currentFilter.equalsIgnoreCase(o.getStatus())) {
                displayedOrders.add(o);
            }
        }

        if (displayedOrders.isEmpty()) {
            tvNoOrders.setVisibility(View.VISIBLE);
            rvOrders.setVisibility(View.GONE);
        } else {
            tvNoOrders.setVisibility(View.GONE);
            rvOrders.setVisibility(View.VISIBLE);
        }
        adapter.notifyDataSetChanged();
    }

    private void showOrderStatusDialog(Order order) {
        String[] statuses = {"Pending", "Confirmed", "Preparing", "Completed", "Cancelled"};
        new AlertDialog.Builder(this)
                .setTitle("Update Status for Order #" + order.getOrderId())
                .setItems(statuses, (dialog, which) -> {
                    String selectedStatus = statuses[which];
                    if (selectedStatus.equalsIgnoreCase(order.getStatus())) {
                        Toast.makeText(AdminOrdersActivity.this, "Status is already " + selectedStatus, Toast.LENGTH_SHORT).show();
                        return;
                    }

                    repository.updateOrderStatus(order, selectedStatus, new FirestoreRepository.Callback<Void>() {
                        @Override
                        public void onSuccess(Void result) {
                            Toast.makeText(AdminOrdersActivity.this, "Order status updated to " + selectedStatus, Toast.LENGTH_SHORT).show();
                            loadOrders();
                        }

                        @Override
                        public void onError(String error) {
                            Toast.makeText(AdminOrdersActivity.this, "Failed to update status: " + error, Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
