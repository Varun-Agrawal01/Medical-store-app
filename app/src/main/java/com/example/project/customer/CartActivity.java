package com.example.project.customer;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.project.R;
import com.example.project.adapters.CartAdapter;
import com.example.project.models.CartItem;
import com.example.project.utils.FormatUtils;
import com.example.project.viewmodels.CartViewModel;

import java.util.ArrayList;

public class CartActivity extends AppCompatActivity {

    private RecyclerView rvCartItems;
    private LinearLayout layoutEmptyCart, layoutCartError;
    private TextView tvSubtotal, tvDeliveryFee, tvFinalTotal, tvCartErrorMsg;
    private Button btnProceedCheckout, btnCartRetry;
    private ImageButton btnBack;
    private ProgressBar pbCartLoading;
    private SwipeRefreshLayout swipeRefreshCart;

    private CartViewModel viewModel;
    private CartAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.cart);

        viewModel = new ViewModelProvider(this).get(CartViewModel.class);

        rvCartItems = findViewById(R.id.rvCartItems);
        layoutEmptyCart = findViewById(R.id.layoutEmptyCart);
        layoutCartError = findViewById(R.id.layoutCartError);
        tvCartErrorMsg = findViewById(R.id.tvCartErrorMsg);
        btnCartRetry = findViewById(R.id.btnCartRetry);
        pbCartLoading = findViewById(R.id.pbCartLoading);
        swipeRefreshCart = findViewById(R.id.swipeRefreshCart);

        tvSubtotal = findViewById(R.id.tvSubtotal);
        tvDeliveryFee = findViewById(R.id.tvDeliveryFee);
        tvFinalTotal = findViewById(R.id.tvFinalTotal);
        btnProceedCheckout = findViewById(R.id.btnProceedCheckout);
        btnBack = findViewById(R.id.btnBackCart);

        btnBack.setOnClickListener(v -> finish());

        rvCartItems.setLayoutManager(new LinearLayoutManager(this));
        adapter = new CartAdapter(this, new CartAdapter.OnCartChangeListener() {
            @Override
            public void onQuantityChange(CartItem item, int newQuantity) {
                viewModel.updateQuantity(item, newQuantity);
            }

            @Override
            public void onRemoveItem(CartItem item) {
                viewModel.removeItem(item);
            }
        });
        rvCartItems.setAdapter(adapter);

        btnProceedCheckout.setOnClickListener(v -> {
            if (adapter.getCurrentList().isEmpty()) {
                Toast.makeText(CartActivity.this, "Cart is empty", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent intent = new Intent(CartActivity.this, CheckoutActivity.class);
            startActivity(intent);
        });

        swipeRefreshCart.setOnRefreshListener(() -> viewModel.loadCart());
        btnCartRetry.setOnClickListener(v -> viewModel.loadCart());

        observeViewModel();
    }

    @Override
    protected void onResume() {
        super.onResume();
        viewModel.loadCart();
    }

    private void observeViewModel() {
        viewModel.getCartItems().observe(this, items -> {
            swipeRefreshCart.setRefreshing(false);
            if (items == null || items.isEmpty()) {
                rvCartItems.setVisibility(View.GONE);
                layoutEmptyCart.setVisibility(View.VISIBLE);
                btnProceedCheckout.setEnabled(false);
                adapter.submitList(new ArrayList<>());
            } else {
                rvCartItems.setVisibility(View.VISIBLE);
                layoutEmptyCart.setVisibility(View.GONE);
                btnProceedCheckout.setEnabled(true);
                adapter.submitList(new ArrayList<>(items));
            }
        });

        viewModel.getSubtotal().observe(this, sub -> tvSubtotal.setText(FormatUtils.formatPrice(sub != null ? sub : 0.0)));
        viewModel.getDeliveryFee().observe(this, fee -> tvDeliveryFee.setText(FormatUtils.formatPrice(fee != null ? fee : 0.0)));
        viewModel.getGrandTotal().observe(this, total -> tvFinalTotal.setText(FormatUtils.formatPrice(total != null ? total : 0.0)));

        viewModel.getIsLoading().observe(this, loading -> {
            if (Boolean.TRUE.equals(loading) && !swipeRefreshCart.isRefreshing()) {
                pbCartLoading.setVisibility(View.VISIBLE);
            } else {
                pbCartLoading.setVisibility(View.GONE);
            }
        });

        viewModel.getErrorMessage().observe(this, error -> {
            swipeRefreshCart.setRefreshing(false);
            if (error != null) {
                layoutCartError.setVisibility(View.VISIBLE);
                tvCartErrorMsg.setText(error);
            } else {
                layoutCartError.setVisibility(View.GONE);
            }
        });
    }
}
