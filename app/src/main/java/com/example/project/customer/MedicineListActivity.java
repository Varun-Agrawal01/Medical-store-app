package com.example.project.customer;

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

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.project.R;
import com.example.project.adapters.MedicineAdapter;
import com.example.project.data.MediRepository;
import com.example.project.models.CartItem;
import com.example.project.models.Medicine;
import com.example.project.utils.SessionManager;
import com.example.project.viewmodels.MedicineListViewModel;

import java.util.ArrayList;

public class MedicineListActivity extends AppCompatActivity {

    private RecyclerView rvMedicines;
    private EditText etSearch;
    private ImageButton btnBack, btnCart;
    private TextView tvCategoryTitle, tvMedListEmpty, tvMedListErrorMsg;
    private ProgressBar pbMedListLoading;
    private LinearLayout layoutMedListError;
    private Button btnMedListRetry;
    private SwipeRefreshLayout swipeRefreshMedList;

    private MedicineListViewModel viewModel;
    private MedicineAdapter adapter;
    private SessionManager sessionManager;

    private final Handler searchHandler = new Handler(Looper.getMainLooper());
    private Runnable searchRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_medicine_list);

        sessionManager = new SessionManager(this);
        viewModel = new ViewModelProvider(this).get(MedicineListViewModel.class);

        rvMedicines = findViewById(R.id.rvMedList);
        etSearch = findViewById(R.id.etSearchMedList);
        btnBack = findViewById(R.id.btnBackMedList);
        btnCart = findViewById(R.id.btnCartMedList);
        tvCategoryTitle = findViewById(R.id.tvMedListTitle);

        pbMedListLoading = findViewById(R.id.pbMedListLoading);
        tvMedListEmpty = findViewById(R.id.tvMedListEmpty);
        layoutMedListError = findViewById(R.id.layoutMedListError);
        tvMedListErrorMsg = findViewById(R.id.tvMedListErrorMsg);
        btnMedListRetry = findViewById(R.id.btnMedListRetry);
        swipeRefreshMedList = findViewById(R.id.swipeRefreshMedList);

        btnBack.setOnClickListener(v -> finish());
        btnCart.setOnClickListener(v -> startActivity(new Intent(MedicineListActivity.this, CartActivity.class)));

        String filterCategory = null;
        if (getIntent().hasExtra("category")) {
            filterCategory = getIntent().getStringExtra("category");
            tvCategoryTitle.setText("Category: " + filterCategory);
        } else {
            tvCategoryTitle.setText("Medicines Catalogue");
        }

        setupRecyclerView();
        setupSearchAndSwipe();
        observeViewModel();

        viewModel.init(filterCategory);
    }

    private void setupRecyclerView() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        rvMedicines.setLayoutManager(layoutManager);
        adapter = new MedicineAdapter(this, false, new MedicineAdapter.OnMedicineClickListener() {
            @Override
            public void onMedicineClick(Medicine medicine) {
                Intent intent = new Intent(MedicineListActivity.this, MedicineDetailActivity.class);
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
        rvMedicines.setAdapter(adapter);

        // Pagination: load medicines 30 at a time using LIMIT/OFFSET and load more on scroll
        rvMedicines.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (dy > 0) { // Scrolling down
                    int visibleItemCount = layoutManager.getChildCount();
                    int totalItemCount = layoutManager.getItemCount();
                    int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();

                    if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount - 5 && firstVisibleItemPosition >= 0) {
                        viewModel.loadNextPage();
                    }
                }
            }
        });
    }

    private void setupSearchAndSwipe() {
        swipeRefreshMedList.setOnRefreshListener(() -> viewModel.refresh());
        btnMedListRetry.setOnClickListener(v -> viewModel.refresh());

        // 250ms search debounce with FTS
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchHandler.removeCallbacks(searchRunnable);
                searchRunnable = () -> viewModel.search(s.toString());
                searchHandler.postDelayed(searchRunnable, 250);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void observeViewModel() {
        viewModel.getMedicines().observe(this, list -> {
            swipeRefreshMedList.setRefreshing(false);
            if (list == null || list.isEmpty()) {
                tvMedListEmpty.setVisibility(View.VISIBLE);
                rvMedicines.setVisibility(View.GONE);
                adapter.submitList(new ArrayList<>());
            } else {
                tvMedListEmpty.setVisibility(View.GONE);
                rvMedicines.setVisibility(View.VISIBLE);
                adapter.submitList(list);
            }
        });

        viewModel.getIsLoading().observe(this, loading -> {
            if (Boolean.TRUE.equals(loading) && !swipeRefreshMedList.isRefreshing()) {
                pbMedListLoading.setVisibility(View.VISIBLE);
            } else {
                pbMedListLoading.setVisibility(View.GONE);
            }
        });

        viewModel.getErrorMessage().observe(this, error -> {
            swipeRefreshMedList.setRefreshing(false);
            if (error != null) {
                layoutMedListError.setVisibility(View.VISIBLE);
                tvMedListErrorMsg.setText(error);
                rvMedicines.setVisibility(View.GONE);
            } else {
                layoutMedListError.setVisibility(View.GONE);
            }
        });
    }

    private void addToCart(Medicine medicine) {
        String userId = sessionManager.getUserId() != null ? sessionManager.getUserId() : "guest";
        CartItem item = new CartItem(medicine.getId(), medicine.getName(), medicine.getPrice(), 1, medicine.isRequiresPrescription());

        MediRepository.getInstance(this).addToCart(userId, item, new MediRepository.Callback<Void>() {
            @Override
            public void onSuccess(Void result) {
                Toast.makeText(MedicineListActivity.this, medicine.getName() + " added to cart", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(String error) {
                Toast.makeText(MedicineListActivity.this, "Failed to add to cart: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
