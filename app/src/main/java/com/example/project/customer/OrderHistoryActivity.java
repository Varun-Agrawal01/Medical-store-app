package com.example.project.customer;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.project.R;
import com.example.project.adapters.OrderAdapter;
import com.example.project.data.FirestoreRepository;
import com.example.project.models.Order;
import com.example.project.models.User;

import java.util.ArrayList;
import java.util.List;

public class OrderHistoryActivity extends AppCompatActivity {

    private RecyclerView rvOrders;
    private TextView tvNoOrders, tvTitle;
    private ImageButton btnBack;

    private FirestoreRepository repository;
    private OrderAdapter adapter;
    private List<Order> orderList = new ArrayList<>();
    private User currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.order_page);

        repository = FirestoreRepository.getInstance(this);
        currentUser = repository.getCurrentUser();

        rvOrders = findViewById(R.id.rvAdminOrders);
        tvNoOrders = findViewById(R.id.textView28);
        btnBack = findViewById(R.id.btnBackOrderPage);
        tvTitle = findViewById(R.id.textView27);

        findViewById(R.id.btnFilterAll).setVisibility(View.GONE);
        findViewById(R.id.btnFilterPending).setVisibility(View.GONE);
        findViewById(R.id.btnFilterConfirmed).setVisibility(View.GONE);
        findViewById(R.id.btnFilterPreparing).setVisibility(View.GONE);
        findViewById(R.id.btnFilterCompleted).setVisibility(View.GONE);
        findViewById(R.id.btnFilterCancelled).setVisibility(View.GONE);

        tvTitle.setText("My Order History");
        btnBack.setOnClickListener(v -> finish());

        rvOrders.setLayoutManager(new LinearLayoutManager(this));
        adapter = new OrderAdapter(this, orderList, new OrderAdapter.OnOrderClickListener() {
            @Override
            public void onOrderClick(Order order) {
                Intent intent = new Intent(OrderHistoryActivity.this, BillActivity.class);
                intent.putExtra("orderId", order.getOrderId());
                startActivity(intent);
            }

            @Override
            public void onViewInvoiceClick(Order order) {
                Intent intent = new Intent(OrderHistoryActivity.this, BillActivity.class);
                intent.putExtra("orderId", order.getOrderId());
                startActivity(intent);
            }
        });
        rvOrders.setAdapter(adapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadCustomerOrders();
    }

    private void loadCustomerOrders() {
        String userId = currentUser != null ? currentUser.getUid() : "guest";
        repository.getCustomerOrders(userId, new FirestoreRepository.Callback<List<Order>>() {
            @Override
            public void onSuccess(List<Order> orders) {
                orderList.clear();
                orderList.addAll(orders);

                if (orderList.isEmpty()) {
                    tvNoOrders.setText("You have no previous orders.");
                    tvNoOrders.setVisibility(View.VISIBLE);
                    rvOrders.setVisibility(View.GONE);
                } else {
                    tvNoOrders.setVisibility(View.GONE);
                    rvOrders.setVisibility(View.VISIBLE);
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onError(String error) {
                Toast.makeText(OrderHistoryActivity.this, "Error: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
