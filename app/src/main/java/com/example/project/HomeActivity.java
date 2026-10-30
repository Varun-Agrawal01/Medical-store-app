package com.example.project;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
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

import com.example.project.adapters.MedicineAdapter;
import com.example.project.adapters.SymptomAdapter;
import com.example.project.customer.CartActivity;
import com.example.project.customer.MedicineDetailActivity;
import com.example.project.customer.MedicineListActivity;
import com.example.project.customer.OrderHistoryActivity;
import com.example.project.customer.ProfileActivity;
import com.example.project.customer.SymptomsActivity;
import com.example.project.data.MediRepository;
import com.example.project.models.CartItem;
import com.example.project.models.Medicine;
import com.example.project.models.Symptom;
import com.example.project.utils.SessionManager;
import com.example.project.viewmodels.HomeViewModel;

import java.util.ArrayList;

public class HomeActivity extends AppCompatActivity {

    private TextView tvWelcomeGreeting, tvHomeEmpty, tvHomeErrorMsg;
    private EditText etHomeSearch;
    private ImageButton btnProfileShortcut, btnCartShortcut, btnLogout;
    private Button btnAllSymptoms, btnHomeNav, btnMedicinesNav, btnCartNav, btnOrdersNav, btnHomeRetry;
    private SwipeRefreshLayout swipeRefreshHome;
    private ProgressBar pbHomeLoading;
    private LinearLayout layoutHomeError;
    private RecyclerView rvPopularSymptoms, rvHomeMedicines;

    private HomeViewModel viewModel;
    private SymptomAdapter symptomAdapter;
    private MedicineAdapter medicineAdapter;
    private SessionManager sessionManager;

    private final Handler searchHandler = new Handler(Looper.getMainLooper());
    private Runnable searchRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.home_page);

        sessionManager = new SessionManager(this);
        viewModel = new ViewModelProvider(this).get(HomeViewModel.class);

        tvWelcomeGreeting = findViewById(R.id.tvWelcomeGreeting);
        tvHomeEmpty = findViewById(R.id.tvHomeEmpty);
        tvHomeErrorMsg = findViewById(R.id.tvHomeErrorMsg);
        etHomeSearch = findViewById(R.id.etHomeSearch);

        btnProfileShortcut = findViewById(R.id.btnProfileShortcut);
        btnCartShortcut = findViewById(R.id.btnCartShortcut);
        btnLogout = findViewById(R.id.btnLogout);
        btnAllSymptoms = findViewById(R.id.btnAllSymptoms);
        btnHomeRetry = findViewById(R.id.btnHomeRetry);

        btnHomeNav = findViewById(R.id.btnHomeNav);
        btnMedicinesNav = findViewById(R.id.btnMedicinesNav);
        btnCartNav = findViewById(R.id.btnCartNav);
        btnOrdersNav = findViewById(R.id.btnOrdersNav);

        swipeRefreshHome = findViewById(R.id.swipeRefreshHome);
        pbHomeLoading = findViewById(R.id.pbHomeLoading);
        layoutHomeError = findViewById(R.id.layoutHomeError);

        rvPopularSymptoms = findViewById(R.id.rvPopularSymptoms);
        rvHomeMedicines = findViewById(R.id.rvHomeMedicines);

        setupRecyclerViews();
        setupListeners();
        observeViewModel();
    }

    private void setupRecyclerViews() {
        // Horizontal Popular Symptoms RecyclerView from DB
        rvPopularSymptoms.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        symptomAdapter = new SymptomAdapter(this, symptom -> {
            Intent intent = new Intent(HomeActivity.this, SymptomsActivity.class);
            intent.putExtra("symptomId", symptom.getId());
            startActivity(intent);
        });
        rvPopularSymptoms.setAdapter(symptomAdapter);

        // Medicines RecyclerView
        rvHomeMedicines.setLayoutManager(new LinearLayoutManager(this));
        medicineAdapter = new MedicineAdapter(this, false, new MedicineAdapter.OnMedicineClickListener() {
            @Override
            public void onMedicineClick(Medicine medicine) {
                Intent intent = new Intent(HomeActivity.this, MedicineDetailActivity.class);
                intent.putExtra("medicineId", medicine.getId());
                startActivity(intent);
            }

            @Override
            public void onAddToCartClick(Medicine medicine) {
                addToCart(medicine);
            }

            @Override
            public void onEditClick(Medicine medicine) {
                // Not in customer mode
            }
        });
        rvHomeMedicines.setAdapter(medicineAdapter);
    }

    private void setupListeners() {
        btnProfileShortcut.setOnClickListener(v -> startActivity(new Intent(HomeActivity.this, ProfileActivity.class)));
        btnCartShortcut.setOnClickListener(v -> startActivity(new Intent(HomeActivity.this, CartActivity.class)));
        btnCartNav.setOnClickListener(v -> startActivity(new Intent(HomeActivity.this, CartActivity.class)));
        btnOrdersNav.setOnClickListener(v -> startActivity(new Intent(HomeActivity.this, OrderHistoryActivity.class)));
        btnMedicinesNav.setOnClickListener(v -> startActivity(new Intent(HomeActivity.this, MedicineListActivity.class)));
        btnAllSymptoms.setOnClickListener(v -> startActivity(new Intent(HomeActivity.this, SymptomsActivity.class)));

        btnLogout.setOnClickListener(v -> {
            sessionManager.clear();
            Toast.makeText(this, getString(R.string.msg_logout_success), Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(HomeActivity.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        swipeRefreshHome.setOnRefreshListener(() -> {
            viewModel.loadUserData();
            viewModel.loadPopularSymptoms();
            viewModel.loadMedicines();
        });

        btnHomeRetry.setOnClickListener(v -> {
            viewModel.loadMedicines();
            viewModel.loadPopularSymptoms();
        });

        // Search with 250ms debounce
        etHomeSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchHandler.removeCallbacks(searchRunnable);
                searchRunnable = () -> viewModel.searchMedicines(s.toString());
                searchHandler.postDelayed(searchRunnable, 250);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void observeViewModel() {
        viewModel.getCurrentUser().observe(this, user -> {
            if (user != null && user.getName() != null) {
                tvWelcomeGreeting.setText("Hello, " + user.getName() + "!");
            }
        });

        viewModel.getPopularSymptoms().observe(this, symptoms -> {
            symptomAdapter.submitList(symptoms);
        });

        viewModel.getMedicines().observe(this, list -> {
            swipeRefreshHome.setRefreshing(false);
            if (list == null || list.isEmpty()) {
                tvHomeEmpty.setVisibility(View.VISIBLE);
                rvHomeMedicines.setVisibility(View.GONE);
                medicineAdapter.submitList(new ArrayList<>());
            } else {
                tvHomeEmpty.setVisibility(View.GONE);
                rvHomeMedicines.setVisibility(View.VISIBLE);
                medicineAdapter.submitList(list);
            }
        });

        viewModel.getIsLoading().observe(this, loading -> {
            if (Boolean.TRUE.equals(loading) && !swipeRefreshHome.isRefreshing()) {
                pbHomeLoading.setVisibility(View.VISIBLE);
            } else {
                pbHomeLoading.setVisibility(View.GONE);
            }
        });

        viewModel.getErrorMessage().observe(this, error -> {
            swipeRefreshHome.setRefreshing(false);
            if (error != null) {
                layoutHomeError.setVisibility(View.VISIBLE);
                tvHomeErrorMsg.setText(error);
                rvHomeMedicines.setVisibility(View.GONE);
            } else {
                layoutHomeError.setVisibility(View.GONE);
            }
        });
    }

    private void addToCart(Medicine medicine) {
        String userId = sessionManager.getUserId() != null ? sessionManager.getUserId() : "guest";
        CartItem item = new CartItem(medicine.getId(), medicine.getName(), medicine.getPrice(), 1, medicine.isRequiresPrescription());

        MediRepository.getInstance(this).addToCart(userId, item, new MediRepository.Callback<Void>() {
            @Override
            public void onSuccess(Void result) {
                Toast.makeText(HomeActivity.this, medicine.getName() + " added to cart", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(String error) {
                Toast.makeText(HomeActivity.this, "Failed to add to cart: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        viewModel.loadUserData();
        viewModel.loadMedicines();
    }
}
