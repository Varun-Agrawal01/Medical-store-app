package com.example.project.admin;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import com.example.project.R;
import com.example.project.data.FirestoreRepository;
import com.example.project.models.Medicine;

import java.util.List;

public class AddEditMedicineActivity extends AppCompatActivity {

    private EditText etName, etGeneric, etPrice, etStock, etCategory, etDescription;
    private SwitchCompat switchActive, switchRxRequired;
    private Button btnSave;
    private ImageButton btnBack;
    private TextView tvTitle;

    private FirestoreRepository repository;
    private String medicineId = null;
    private Medicine currentMedicine = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_medicine);

        repository = FirestoreRepository.getInstance(this);

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
        }

        btnSave.setOnClickListener(v -> saveMedicine());
    }

    private void loadExistingMedicine() {
        repository.getMedicines(new FirestoreRepository.Callback<List<Medicine>>() {
            @Override
            public void onSuccess(List<Medicine> result) {
                for (Medicine m : result) {
                    if (m.getId().equals(medicineId)) {
                        currentMedicine = m;
                        populateFields(m);
                        break;
                    }
                }
            }

            @Override
            public void onError(String error) {
                Toast.makeText(AddEditMedicineActivity.this, "Failed to load medicine: " + error, Toast.LENGTH_SHORT).show();
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
        String name = etName.getText().toString().trim();
        String generic = etGeneric.getText().toString().trim();
        String priceStr = etPrice.getText().toString().trim();
        String stockStr = etStock.getText().toString().trim();
        String category = etCategory.getText().toString().trim();
        String description = etDescription.getText().toString().trim();

        if (name.isEmpty() || priceStr.isEmpty() || stockStr.isEmpty()) {
            Toast.makeText(this, "Name, Price, and Stock quantity are required!", Toast.LENGTH_SHORT).show();
            return;
        }

        double price;
        int stock;
        try {
            price = Double.parseDouble(priceStr);
            stock = Integer.parseInt(stockStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid Price or Stock format", Toast.LENGTH_SHORT).show();
            return;
        }

        if (price < 0 || stock < 0) {
            Toast.makeText(this, "Price and Stock cannot be negative!", Toast.LENGTH_SHORT).show();
            return;
        }

        Medicine m = currentMedicine != null ? currentMedicine : new Medicine();
        m.setName(name);
        m.setGenericName(generic);
        m.setPrice(price);
        m.setStock(stock);
        m.setCategory(category.isEmpty() ? "General" : category);
        m.setDescription(description);
        m.setActive(switchActive.isChecked());
        m.setRequiresPrescription(switchRxRequired.isChecked());

        btnSave.setEnabled(false);
        repository.saveMedicine(m, new FirestoreRepository.Callback<Void>() {
            @Override
            public void onSuccess(Void result) {
                btnSave.setEnabled(true);
                Toast.makeText(AddEditMedicineActivity.this, "Medicine record saved!", Toast.LENGTH_SHORT).show();
                finish();
            }

            @Override
            public void onError(String error) {
                btnSave.setEnabled(true);
                Toast.makeText(AddEditMedicineActivity.this, "Save failed: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
