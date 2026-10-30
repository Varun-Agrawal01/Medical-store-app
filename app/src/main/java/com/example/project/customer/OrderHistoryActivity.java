package com.example.project.customer;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.project.R;
import com.example.project.adapters.OrderAdapter;
import com.example.project.models.Order;
import com.example.project.utils.PdfGenerator;
import com.example.project.viewmodels.OrderHistoryViewModel;

import java.io.File;
import java.util.List;

public class OrderHistoryActivity extends AppCompatActivity {

    private RecyclerView rvOrders;
    private TextView tvNoOrders, tvTitle, tvErrorMsg;
    private ImageButton btnBack;
    private SwipeRefreshLayout swipeRefreshOrders;
    private ProgressBar pbLoading;
    private View layoutError;
    private Button btnRetry;

    private OrderHistoryViewModel viewModel;
    private OrderAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.order_page);

        // Hide Admin filter bar
        View filterScrollView = findViewById(R.id.scrollViewOrderFilters);
        if (filterScrollView != null) {
            filterScrollView.setVisibility(View.GONE);
        }

        rvOrders = findViewById(R.id.rvAdminOrders);
        tvNoOrders = findViewById(R.id.textView28);
        btnBack = findViewById(R.id.btnBackOrderPage);
        tvTitle = findViewById(R.id.textView27);
        swipeRefreshOrders = findViewById(R.id.swipeRefreshOrders);
        pbLoading = findViewById(R.id.pbOrdersLoading);
        layoutError = findViewById(R.id.layoutOrdersError);
        tvErrorMsg = findViewById(R.id.tvOrdersErrorMsg);
        btnRetry = findViewById(R.id.btnOrdersRetry);

        tvTitle.setText(R.string.title_order_history);
        btnBack.setOnClickListener(v -> finish());

        rvOrders.setLayoutManager(new LinearLayoutManager(this));
        adapter = new OrderAdapter(this, new OrderAdapter.OnOrderClickListener() {
            @Override
            public void onOrderClick(Order order) {
                Intent intent = new Intent(OrderHistoryActivity.this, BillActivity.class);
                intent.putExtra("orderId", order.getOrderId());
                startActivity(intent);
            }

            @Override
            public void onViewInvoiceClick(Order order) {
                File pdfFile = PdfGenerator.generateInvoicePdf(OrderHistoryActivity.this, order);
                if (pdfFile != null) {
                    PdfGenerator.openPdf(OrderHistoryActivity.this, pdfFile);
                } else {
                    Intent intent = new Intent(OrderHistoryActivity.this, BillActivity.class);
                    intent.putExtra("orderId", order.getOrderId());
                    startActivity(intent);
                }
            }
        });
        rvOrders.setAdapter(adapter);

        viewModel = new ViewModelProvider(this).get(OrderHistoryViewModel.class);

        swipeRefreshOrders.setOnRefreshListener(() -> viewModel.loadOrders());
        btnRetry.setOnClickListener(v -> viewModel.loadOrders());

        setupObservers();
    }

    private void setupObservers() {
        viewModel.getOrders().observe(this, orders -> {
            boolean hasOrders = orders != null && !orders.isEmpty();
            adapter.submitList(orders);

            if (hasOrders) {
                rvOrders.setVisibility(View.VISIBLE);
                tvNoOrders.setVisibility(View.GONE);
                layoutError.setVisibility(View.GONE);
            } else {
                rvOrders.setVisibility(View.GONE);
                // Only show empty state if not showing an error
                if (layoutError.getVisibility() != View.VISIBLE) {
                    tvNoOrders.setVisibility(View.VISIBLE);
                }
            }
        });

        viewModel.getIsLoading().observe(this, loading -> {
            boolean isLoading = Boolean.TRUE.equals(loading);
            swipeRefreshOrders.setRefreshing(isLoading);

            List<Order> current = viewModel.getOrders().getValue();
            if (isLoading && (current == null || current.isEmpty())) {
                pbLoading.setVisibility(View.VISIBLE);
                tvNoOrders.setVisibility(View.GONE);
                layoutError.setVisibility(View.GONE);
            } else {
                pbLoading.setVisibility(View.GONE);
            }
        });

        viewModel.getErrorMessage().observe(this, error -> {
            if (error != null && !error.isEmpty()) {
                List<Order> current = viewModel.getOrders().getValue();
                if (current == null || current.isEmpty()) {
                    layoutError.setVisibility(View.VISIBLE);
                    tvErrorMsg.setText(error);
                    tvNoOrders.setVisibility(View.GONE);
                    rvOrders.setVisibility(View.GONE);
                } else {
                    Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
                }
            } else {
                layoutError.setVisibility(View.GONE);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        viewModel.loadOrders();
    }
}
