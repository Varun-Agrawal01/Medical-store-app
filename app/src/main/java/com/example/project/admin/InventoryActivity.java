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
import com.example.project.auth.AdminLoginActivity;
import com.example.project.data.MediRepository;
import com.example.project.models.Medicine;
import com.example.project.models.User;
import com.example.project.utils.SessionManager;

import java.util.ArrayList;
import java.util.List;

public class InventoryActivity extends AppCompatActivity {

    private RecyclerView rvInventory;
    private EditText etSearch;
    private Button btnAddMedicine;
    private ImageButton btnBack;

    private MediRepository repository;
    private SessionManager sessionManager;
    private MedicineAdapter adapter;
    private final List<Medicine> allMedicines = new ArrayList<>();
    private final List<Medicine> displayedMedicines = new ArrayList<>();

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

        setContentView(R.layout.inventory_page);

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
        adapter = new MedicineAdapter(this, true, new MedicineAdapter.OnMedicineClickListener() {
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

    private void redirectToLogin() {
        Intent intent = new Intent(this, AdminLoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadInventory();
    }

    private void loadInventory() {
        repository.getMedicines(new MediRepository.Callback<List<Medicine>>() {
            @Override
            public void onSuccess(List<Medicine> result) {
                allMedicines.clear();
                allMedicines.addAll(result);
                filterMedicines(etSearch.getText().toString());
            }

            @Override
            public void onError(String error) {
                Toast.makeText(InventoryActivity.this, "Failed to load inventory: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void filterMedicines(String query) {
        displayedMedicines.clear();
        String q = query.trim().toLowerCase();
        if (q.isEmpty()) {
            displayedMedicines.addAll(allMedicines);
        } else {
            for (Medicine m : allMedicines) {
                if ((m.getName() != null && m.getName().toLowerCase().contains(q)) ||
                        (m.getGenericName() != null && m.getGenericName().toLowerCase().contains(q)) ||
                        (m.getCategory() != null && m.getCategory().toLowerCase().contains(q))) {
                    displayedMedicines.add(m);
                }
            }
        }
        adapter.submitList(new ArrayList<>(displayedMedicines));
    }

    private void showStockOptionDialog(Medicine medicine) {
        String[] options = {"Edit Details", "Add Stock (+10)", "Add Stock (+50)", "Set Specific Stock", "Toggle Active / Inactive"};
        new AlertDialog.Builder(this)
                .setTitle(medicine.getName() + " (Current Stock: " + medicine.getStock() + ")")
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            Intent intent = new Intent(InventoryActivity.this, AddEditMedicineActivity.class);
                            intent.putExtra("medicineId", medicine.getId());
                            startActivity(intent);
                            break;
                        case 1:
                            quickUpdateStock(medicine, 10, false);
                            break;
                        case 2:
                            quickUpdateStock(medicine, 50, false);
                            break;
                        case 3:
                            showCustomStockDialog(medicine);
                            break;
                        case 4:
                            toggleMedicineActive(medicine);
                            break;
                    }
                })
                .show();
    }

    private void quickUpdateStock(Medicine m, int amount, boolean isSetMode) {
        repository.updateStock(m.getId(), amount, isSetMode, new MediRepository.Callback<Void>() {
            @Override
            public void onSuccess(Void result) {
                Toast.makeText(InventoryActivity.this, "Stock updated successfully", Toast.LENGTH_SHORT).show();
                loadInventory();
            }

            @Override
            public void onError(String error) {
                Toast.makeText(InventoryActivity.this, "Stock update failed: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showCustomStockDialog(Medicine medicine) {
        final EditText input = new EditText(this);
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        input.setHint("Enter stock count");

        new AlertDialog.Builder(this)
                .setTitle("Set Stock for " + medicine.getName())
                .setView(input)
                .setPositiveButton("Set", (dialog, which) -> {
                    String str = input.getText().toString().trim();
                    if (!str.isEmpty()) {
                        int newStock = Integer.parseInt(str);
                        quickUpdateStock(medicine, newStock, true);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void toggleMedicineActive(Medicine m) {
        m.setActive(!m.isActive());
        repository.saveMedicine(m, new MediRepository.Callback<Void>() {
            @Override
            public void onSuccess(Void result) {
                Toast.makeText(InventoryActivity.this, "Medicine status toggled to " + (m.isActive() ? "Active" : "Inactive"), Toast.LENGTH_SHORT).show();
                loadInventory();
            }

            @Override
            public void onError(String error) {
                Toast.makeText(InventoryActivity.this, "Failed to toggle status: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
