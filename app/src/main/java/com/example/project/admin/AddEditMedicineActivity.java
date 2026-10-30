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
import com.example.project.models.Symptom;
import com.example.project.models.User;
import com.example.project.utils.SessionManager;

import java.util.ArrayList;
import java.util.List;

public class AddEditMedicineActivity extends AppCompatActivity {

    private EditText etName, etGeneric, etPrice, etStock, etDescription;
    private Button btnSelectSymptoms, btnSave;
    private TextView tvSelectedSymptoms;
    private SwitchCompat switchActive, switchRxRequired;
    private ImageButton btnBack;
    private TextView tvTitle;

    private MediRepository repository;
    private SessionManager sessionManager;
    private String medicineId = null;
    private Medicine currentMedicine = null;

    // All loaded symptoms and selected state
    private List<Symptom> allSymptoms = new ArrayList<>();
    private boolean[] symptomChecked;
    private final List<String> selectedSymptomIds = new ArrayList<>();

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
        etDescription = findViewById(R.id.etMedDescription);
        btnSelectSymptoms = findViewById(R.id.btnSelectSymptoms);
        tvSelectedSymptoms = findViewById(R.id.tvSelectedSymptoms);
        switchActive = findViewById(R.id.switchActive);
        switchRxRequired = findViewById(R.id.switchRxRequired);
        btnSave = findViewById(R.id.btnSaveMedicine);

        btnBack.setOnClickListener(v -> finish());
        btnSelectSymptoms.setOnClickListener(v -> showSymptomPicker());

        loadSymptoms();

        if (getIntent().hasExtra("medicineId")) {
            medicineId = getIntent().getStringExtra("medicineId");
            tvTitle.setText("Edit Medicine Record");
            loadExistingMedicine();
        } else {
            tvTitle.setText("Add New Medicine");
            switchActive.setChecked(true);
        }

        btnSave.setOnClickListener(v -> saveMedicine());
    }

    private void loadSymptoms() {
        repository.getSymptoms(new MediRepository.Callback<List<Symptom>>() {
            @Override
            public void onSuccess(List<Symptom> symptoms) {
                allSymptoms = symptoms != null ? symptoms : new ArrayList<>();
                symptomChecked = new boolean[allSymptoms.size()];
                // Re-apply pre-selections if we already have them (edit mode loaded first)
                reapplySymptomSelections();
            }

            @Override
            public void onError(String error) {
                Log.e("AddEditMedicine", "Failed to load symptoms: " + error);
                allSymptoms = new ArrayList<>();
                symptomChecked = new boolean[0];
            }
        });
    }

    private void showSymptomPicker() {
        if (allSymptoms.isEmpty()) {
            Toast.makeText(this, "Symptoms are still loading, please wait…", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] names = new String[allSymptoms.size()];
        for (int i = 0; i < allSymptoms.size(); i++) {
            names[i] = allSymptoms.get(i).getName();
        }

        // Make a defensive copy of checked state to support Cancel
        boolean[] tempChecked = symptomChecked.clone();

        new AlertDialog.Builder(this)
                .setTitle("Select Symptoms / Category")
                .setMultiChoiceItems(names, tempChecked, (dialog, which, isChecked) -> tempChecked[which] = isChecked)
                .setPositiveButton("OK", (dialog, which) -> {
                    System.arraycopy(tempChecked, 0, symptomChecked, 0, symptomChecked.length);
                    rebuildSelectedIds();
                    updateSymptomLabel();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void rebuildSelectedIds() {
        selectedSymptomIds.clear();
        for (int i = 0; i < allSymptoms.size(); i++) {
            if (symptomChecked != null && i < symptomChecked.length && symptomChecked[i]) {
                selectedSymptomIds.add(allSymptoms.get(i).getId());
            }
        }
    }

    private void updateSymptomLabel() {
        if (selectedSymptomIds.isEmpty()) {
            tvSelectedSymptoms.setText("None selected");
        } else {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < allSymptoms.size(); i++) {
                if (symptomChecked != null && i < symptomChecked.length && symptomChecked[i]) {
                    if (sb.length() > 0) sb.append(", ");
                    sb.append(allSymptoms.get(i).getName());
                }
            }
            tvSelectedSymptoms.setText(sb.toString());
        }
    }

    /**
     * Called after allSymptoms is loaded to tick boxes for symptoms already linked to this medicine.
     */
    private void reapplySymptomSelections() {
        if (symptomChecked == null || allSymptoms.isEmpty() || selectedSymptomIds.isEmpty()) {
            updateSymptomLabel();
            return;
        }
        for (int i = 0; i < allSymptoms.size(); i++) {
            symptomChecked[i] = selectedSymptomIds.contains(allSymptoms.get(i).getId());
        }
        updateSymptomLabel();
    }

    private void redirectToLogin() {
        Intent intent = new Intent(this, AdminLoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void loadExistingMedicine() {
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
        etDescription.setText(m.getDescription());
        switchActive.setChecked(m.isActive());
        switchRxRequired.setChecked(m.isRequiresPrescription());

        // Load symptom links for this medicine so the picker can pre-check them
        loadMedicineSymptomLinks(m.getId());
    }

    private void loadMedicineSymptomLinks(String medId) {
        // Use the symptom IDs from medicine_symptoms via a dedicated query
        // We reuse getSymptoms and medicine_symptoms indirectly via Category (which is now symptoms).
        // Simpler approach: store the category name as first symptom; but for editing we need full list.
        // We'll call a background query via the repository executor.
        repository.getMedicineSymptomIds(medId, new MediRepository.Callback<List<String>>() {
            @Override
            public void onSuccess(List<String> ids) {
                selectedSymptomIds.clear();
                if (ids != null) selectedSymptomIds.addAll(ids);
                reapplySymptomSelections();
            }

            @Override
            public void onError(String error) {
                // Not fatal — just no pre-selection
                Log.w("AddEditMedicine", "Could not load symptom links: " + error);
            }
        });
    }

    private void saveMedicine() {
        try {
            String name = etName.getText().toString().trim();
            String generic = etGeneric.getText().toString().trim();
            String priceStr = etPrice.getText().toString().trim();
            String stockStr = etStock.getText().toString().trim();
            String description = etDescription.getText().toString().trim();

            if (name.isEmpty()) {
                etName.setError("Medicine name is required");
                etName.requestFocus();
                return;
            }

            if (priceStr.isEmpty()) {
                etPrice.setError("Price is required");
                etPrice.requestFocus();
                return;
            }

            double price;
            int stock = 0;
            try {
                price = Double.parseDouble(priceStr);
                if (price <= 0) {
                    etPrice.setError("Price must be greater than 0");
                    etPrice.requestFocus();
                    return;
                }
            } catch (NumberFormatException e) {
                etPrice.setError("Enter a valid price");
                etPrice.requestFocus();
                return;
            }

            if (!stockStr.isEmpty()) {
                try {
                    stock = Integer.parseInt(stockStr);
                    if (stock < 0) {
                        etStock.setError("Stock cannot be negative");
                        etStock.requestFocus();
                        return;
                    }
                } catch (NumberFormatException e) {
                    etStock.setError("Enter a valid stock number");
                    etStock.requestFocus();
                    return;
                }
            }

            Medicine m = currentMedicine != null ? currentMedicine : new Medicine();
            m.setName(name);
            m.setGenericName(generic);
            m.setPrice(price);
            m.setStock(stock);
            m.setDescription(description);
            m.setActive(switchActive.isChecked());
            m.setRequiresPrescription(switchRxRequired.isChecked());
            m.setSelectedSymptomIds(new ArrayList<>(selectedSymptomIds));

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
            showErrorDialog(e.toString());
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
