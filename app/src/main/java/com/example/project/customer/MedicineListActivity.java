package com.example.project.customer;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.project.R;
import com.example.project.adapters.MedicineAdapter;
import com.example.project.data.FirestoreRepository;
import com.example.project.models.CartItem;
import com.example.project.models.Medicine;
import com.example.project.models.User;

import java.util.ArrayList;
import java.util.List;

public class MedicineListActivity extends AppCompatActivity {

    private RecyclerView rvMedicines;
    private EditText etSearch;
    private ImageButton btnBack, btnCart;
    private TextView tvCategoryTitle;

    private FirestoreRepository repository;
    private MedicineAdapter adapter;
    private List<Medicine> allMedicines = new ArrayList<>();
    private List<Medicine> displayedMedicines = new ArrayList<>();
    private String filterCategory = null;
    private User currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.inventory_page); // Reuse inventory layout XML safely for browsing

        repository = FirestoreRepository.getInstance(this);
        currentUser = repository.getCurrentUser();

        rvMedicines = findViewById(R.id.rvInventoryMedicines);
        etSearch = findViewById(R.id.editTextText9);
        btnBack = findViewById(R.id.btnBackInventory);
        tvCategoryTitle = findViewById(R.id.textView26);

        findViewById(R.id.btnAddMedicine).setVisibility(android.view.View.GONE);

        btnBack.setOnClickListener(v -> finish());

        if (getIntent().hasExtra("category")) {
            filterCategory = getIntent().getStringExtra("category");
            tvCategoryTitle.setText("Category: " + filterCategory);
        } else {
            tvCategoryTitle.setText("Medicines Catalogue");
        }

        if (getIntent().hasExtra("searchQuery")) {
            String initialQuery = getIntent().getStringExtra("searchQuery");
            if (initialQuery != null && !initialQuery.isEmpty()) {
                etSearch.setText(initialQuery);
            }
        }

        rvMedicines.setLayoutManager(new LinearLayoutManager(this));
        adapter = new MedicineAdapter(this, displayedMedicines, false, new MedicineAdapter.OnMedicineClickListener() {
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
                // Customer mode
            }
        });
        rvMedicines.setAdapter(adapter);

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterMedicines(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadMedicines();
    }

    private void loadMedicines() {
        repository.getMedicines(new FirestoreRepository.Callback<List<Medicine>>() {
            @Override
            public void onSuccess(List<Medicine> result) {
                allMedicines.clear();
                allMedicines.addAll(result);
                filterMedicines(etSearch.getText().toString());
            }

            @Override
            public void onError(String error) {
                Toast.makeText(MedicineListActivity.this, "Error loading catalogue: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void filterMedicines(String query) {
        displayedMedicines.clear();
        String q = query.trim().toLowerCase();
        for (Medicine m : allMedicines) {
            boolean matchCategory = filterCategory == null || (m.getCategory() != null && m.getCategory().equalsIgnoreCase(filterCategory));
            boolean matchQuery = q.isEmpty() || m.getName().toLowerCase().contains(q) || (m.getGenericName() != null && m.getGenericName().toLowerCase().contains(q));
            if (matchCategory && matchQuery) {
                displayedMedicines.add(m);
            }
        }
        adapter.notifyDataSetChanged();
    }

    private void addToCart(Medicine medicine) {
        if (!medicine.isAvailable()) {
            Toast.makeText(this, medicine.getName() + " is out of stock", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = currentUser != null ? currentUser.getUid() : "guest";
        repository.getCart(userId, new FirestoreRepository.Callback<List<CartItem>>() {
            @Override
            public void onSuccess(List<CartItem> cartItems) {
                boolean exists = false;
                for (CartItem ci : cartItems) {
                    if (ci.getMedicineId().equals(medicine.getId())) {
                        ci.setQuantity(ci.getQuantity() + 1);
                        exists = true;
                        break;
                    }
                }
                if (!exists) {
                    cartItems.add(new CartItem(medicine.getId(), medicine.getName(), medicine.getPrice(), 1, medicine.isRequiresPrescription()));
                }

                repository.saveCart(userId, cartItems, new FirestoreRepository.Callback<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        Toast.makeText(MedicineListActivity.this, medicine.getName() + " added to cart!", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(String error) {
                        Toast.makeText(MedicineListActivity.this, "Error: " + error, Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onError(String error) {
                Toast.makeText(MedicineListActivity.this, "Error: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
