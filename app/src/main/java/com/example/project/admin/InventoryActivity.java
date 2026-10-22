package com.example.project.admin;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.project.R;
import com.example.project.adapters.MedicineAdapter;
import com.example.project.data.FirestoreRepository;
import com.example.project.models.Medicine;

import java.util.ArrayList;
import java.util.List;

public class InventoryActivity extends AppCompatActivity {

    private RecyclerView rvInventory;
    private EditText etSearch;
    private Button btnAddMedicine;
    private ImageButton btnBack;

    private FirestoreRepository repository;
    private MedicineAdapter adapter;
    private List<Medicine> allMedicines = new ArrayList<>();
    private List<Medicine> displayedMedicines = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.inventory_page);

        repository = FirestoreRepository.getInstance(this);

        rvInventory = findViewById(R.id.rvInventoryMedicines);
        etSearch = findViewById(R.id.editTextText9);
        btnAddMedicine = findViewById(R.id.btnAddMedicine);
        btnBack = findViewById(R.id.btnBackInventory);

        btnBack.setOnClickListener(v -> finish());

        btnAddMedicine.setOnClickListener(v -> {
            Intent intent = new Intent(InventoryActivity.this, AddEditMedicineActivity.class);
            startActivity(intent);
        });

        rvInventory.setLayoutManager(new LinearLayoutManager(this));
        adapter = new MedicineAdapter(this, displayedMedicines, true, new MedicineAdapter.OnMedicineClickListener() {
            @Override
            public void onMedicineClick(Medicine medicine) {
                showStockOptionDialog(medicine);
            }

            @Override
            public void onAddToCartClick(Medicine medicine) {
                // Not used in admin mode
            }

            @Override
            public void onEditClick(Medicine medicine) {
                showStockOptionDialog(medicine);
            }
        });
        rvInventory.setAdapter(adapter);

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
        loadInventory();
    }

    private void loadInventory() {
        repository.getMedicines(new FirestoreRepository.Callback<List<Medicine>>() {
            @Override
            public void onSuccess(List<Medicine> result) {
                allMedicines.clear();
                allMedicines.addAll(result);
                filterMedicines(etSearch.getText().toString());
            }

            @Override
            public void onError(String error) {
                Toast.makeText(InventoryActivity.this, "Error loading inventory: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void filterMedicines(String query) {
        displayedMedicines.clear();
        String q = query.trim().toLowerCase();
        for (Medicine m : allMedicines) {
            if (q.isEmpty() || m.getName().toLowerCase().contains(q) || (m.getGenericName() != null && m.getGenericName().toLowerCase().contains(q))) {
                displayedMedicines.add(m);
            }
        }
        adapter.notifyDataSetChanged();
    }

    private void showStockOptionDialog(Medicine medicine) {
        String[] options = {"Refill / Set Stock", "Edit Full Medicine Record"};
        new AlertDialog.Builder(this)
                .setTitle(medicine.getName() + " - Admin Action")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        showRefillStockDialog(medicine);
                    } else {
                        Intent intent = new Intent(InventoryActivity.this, AddEditMedicineActivity.class);
                        intent.putExtra("medicineId", medicine.getId());
                        startActivity(intent);
                    }
                })
                .show();
    }

    private void showRefillStockDialog(Medicine medicine) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Refill / Update Stock for " + medicine.getName());

        final EditText etQuantity = new EditText(this);
        etQuantity.setHint("Enter Quantity");
        etQuantity.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        etQuantity.setPadding(32, 32, 32, 32);
        builder.setView(etQuantity);

        builder.setPositiveButton("Add Stock (+)", (dialog, which) -> {
            String val = etQuantity.getText().toString().trim();
            if (!val.isEmpty()) {
                int addQty = Integer.parseInt(val);
                repository.updateStock(medicine.getId(), addQty, false, new FirestoreRepository.Callback<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        Toast.makeText(InventoryActivity.this, "Stock added successfully!", Toast.LENGTH_SHORT).show();
                        loadInventory();
                    }

                    @Override
                    public void onError(String error) {
                        Toast.makeText(InventoryActivity.this, "Error: " + error, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });

        builder.setNeutralButton("Set Exact Stock (=)", (dialog, which) -> {
            String val = etQuantity.getText().toString().trim();
            if (!val.isEmpty()) {
                int setQty = Integer.parseInt(val);
                repository.updateStock(medicine.getId(), setQty, true, new FirestoreRepository.Callback<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        Toast.makeText(InventoryActivity.this, "Stock set to " + setQty, Toast.LENGTH_SHORT).show();
                        loadInventory();
                    }

                    @Override
                    public void onError(String error) {
                        Toast.makeText(InventoryActivity.this, "Error: " + error, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }
}
