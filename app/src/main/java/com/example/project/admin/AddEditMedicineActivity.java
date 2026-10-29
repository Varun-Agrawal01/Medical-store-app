package com.example.project.admin;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import com.example.project.R;
import com.example.project.auth.AdminLoginActivity;
import com.example.project.data.MediRepository;
import com.example.project.models.Medicine;
import com.example.project.models.User;
import com.example.project.utils.SessionManager;

public class AddEditMedicineActivity extends AppCompatActivity {

    private EditText etName, etGeneric, etPrice, etStock, etCategory, etDescription;
    private SwitchCompat switchActive, switchRxRequired;
    private Button btnSave;
    private ImageButton btnBack;
    private TextView tvTitle;

    private MediRepository repository;
    private SessionManager sessionManager;
    private String medicineId = null;
    private Medicine currentMedicine = null;

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

        setContentView(R.layout.activity_add_edit_medicine);

        tvTitle = findViewById(R.id.tvAddEditTitle);
        btnBack = findViewById(R.id.btnBackAddEdit);

        etName = findViewById(R.id.etMedName);
        etGeneric = findViewById(R.id.etMedGeneric);
        etPrice = findViewById(R.id.etMedPrice);
        etStock = findViewById(R.id.etMedStock);
        etCategory = findViewById(R.id.etMedCategory);
        etDescription = findViewById(R.id.etMedDescription);

        switchActive = findViewById(R.id.switchActive);
        switchRxRequired = findViewById(R.id.switchRxRequired);
        btnSave = findViewById(R.id.btnSaveMedicine);

        btnBack.setOnClickListener(v -> finish());

        if (getIntent().hasExtra("medicineId")) {
            medicineId = getIntent().getStringExtra("medicineId");
            tvTitle.setText("Edit Medicine Record");
            loadExistingMedicine();
        } else {
            tvTitle.setText("Add New Medicine");
            // New medicines default to active so they appear immediately in the customer list
            switchActive.setChecked(true);
        }

        btnSave.setOnClickListener(v -> saveMedicine());
    }

    private void redirectToLogin() {
        Intent intent = new Intent(this, AdminLoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void loadExistingMedicine() {
        // Query single row directly rather than loading all medicines and looping
        repository.getMedicineById(medicineId, new MediRepository.Callback<Medicine>() {
            @Override
            public void onSuccess(Medicine m) {
                currentMedicine = m;
                populateFields(m);
            }

            @Override
            public void onError(String error) {
                Toast.makeText(AddEditMedicineActivity.this, "Failed to load medicine: " + error, Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void populateFields(Medicine m) {
        etName.setText(m.getName());
        etGeneric.setText(m.getGenericName());
        etPrice.setText(String.valueOf(m.getPrice()));
        etStock.setText(String.valueOf(m.getStock()));
        etCategory.setText(m.getCategory());
        etDescription.setText(m.getDescription());
        switchActive.setChecked(m.isActive());
        switchRxRequired.setChecked(m.isRequiresPrescription());
    }

    private void saveMedicine() {
        try {
            String name = etName.getText().toString().trim();
            String generic = etGeneric.getText().toString().trim();
            String priceStr = etPrice.getText().toString().trim();
            String stockStr = etStock.getText().toString().trim();
            String category = etCategory.getText().toString().trim();
            String description = etDescription.getText().toString().trim();

            if (name.isEmpty() || priceStr.isEmpty() || stockStr.isEmpty()) {
                Toast.makeText(this, "Please fill in all required fields (Name, Price, Stock)", Toast.LENGTH_SHORT).show();
                return;
            }

            double price;
            int stock;
            try {
                price = Double.parseDouble(priceStr);
                stock = Integer.parseInt(stockStr);
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Please enter valid numbers for Price and Stock", Toast.LENGTH_SHORT).show();
                return;
            }

            Medicine m = currentMedicine != null ? currentMedicine : new Medicine();
            m.setName(name);
            m.setGenericName(generic);
            m.setPrice(price);
            m.setStock(stock);
            m.setCategory(category.isEmpty() ? "cat_analgesics" : category);
            m.setDescription(description);
            m.setActive(switchActive.isChecked());
            m.setRequiresPrescription(switchRxRequired.isChecked());

            btnSave.setEnabled(false);
            repository.saveMedicine(m, new MediRepository.Callback<Void>() {
                @Override
                public void onSuccess(Void result) {
                    btnSave.setEnabled(true);
                    Toast.makeText(AddEditMedicineActivity.this, "Medicine record saved successfully!", Toast.LENGTH_SHORT).show();
                    finish();
                }

                @Override
                public void onError(String error) {
                    btnSave.setEnabled(true);
                    Log.e("MEDSAVE", "save failed: " + error);
                    showErrorDialog(error);
                }
            });
        } catch (Exception e) {
            btnSave.setEnabled(true);
            Log.e("MEDSAVE", "save failed", e);
            showErrorDialog(e.getMessage());
        }
    }

    private void showErrorDialog(String errorMessage) {
        TextView textView = new TextView(this);
        textView.setText(errorMessage);
        textView.setTextIsSelectable(true);
        int padding = (int) (18 * getResources().getDisplayMetrics().density);
        textView.setPadding(padding, padding, padding, padding);
        textView.setTextSize(14f);

        new AlertDialog.Builder(this)
                .setTitle("Save Failed")
                .setView(textView)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }
}
