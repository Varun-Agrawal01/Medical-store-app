package com.example.project.customer;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.project.R;
import com.example.project.adapters.MedicineAdapter;
import com.example.project.data.MediRepository;
import com.example.project.models.CartItem;
import com.example.project.models.Medicine;
import com.example.project.models.Symptom;
import com.example.project.utils.SessionManager;
import com.example.project.viewmodels.SymptomsViewModel;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SymptomsActivity extends AppCompatActivity {

    private ImageButton btnBack;
    private EditText etSearchSymptoms;
    private ChipGroup cgSymptoms;
    private Spinner spAgeGroup;
    private SwitchMaterial switchPregnant, switchExcludeRx;
    private Button btnFindMedicines;
    private MaterialCardView cardRedFlagBanner;
    private TextView tvRedFlagAdvice, tvResultsHeader, tvNoResults;
    private RecyclerView rvSymptomMedicines;

    private SymptomsViewModel viewModel;
    private MedicineAdapter medicineAdapter;
    private List<Symptom> allSymptomsList = new ArrayList<>();
    private final Map<String, Chip> symptomChipsMap = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_symptoms);

        viewModel = new ViewModelProvider(this).get(SymptomsViewModel.class);

        btnBack = findViewById(R.id.btnBackSymptoms);
        etSearchSymptoms = findViewById(R.id.etSearchSymptoms);
        cgSymptoms = findViewById(R.id.cgSymptoms);
        spAgeGroup = findViewById(R.id.spAgeGroup);
        switchPregnant = findViewById(R.id.switchPregnant);
        switchExcludeRx = findViewById(R.id.switchExcludeRx);
        btnFindMedicines = findViewById(R.id.btnFindMedicines);

        cardRedFlagBanner = findViewById(R.id.cardRedFlagBanner);
        tvRedFlagAdvice = findViewById(R.id.tvRedFlagAdvice);
        tvResultsHeader = findViewById(R.id.tvResultsHeader);
        tvNoResults = findViewById(R.id.tvNoResults);
        rvSymptomMedicines = findViewById(R.id.rvSymptomMedicines);

        btnBack.setOnClickListener(v -> finish());

        // Setup Age Group Spinner
        String[] ageOptions = {
                getString(R.string.age_all),
                getString(R.string.age_child),
                getString(R.string.age_adult),
                getString(R.string.age_senior)
        };
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, ageOptions);
        spAgeGroup.setAdapter(spinnerAdapter);

        // Setup RecyclerView
        rvSymptomMedicines.setLayoutManager(new LinearLayoutManager(this));
        medicineAdapter = new MedicineAdapter(this, false, new MedicineAdapter.OnMedicineClickListener() {
            @Override
            public void onMedicineClick(Medicine medicine) {
                Intent intent = new Intent(SymptomsActivity.this, MedicineDetailActivity.class);
                intent.putExtra("medicineId", medicine.getId());
                startActivity(intent);
            }

            @Override
            public void onAddToCartClick(Medicine medicine) {
                addToCart(medicine);
            }

            @Override
            public void onEditClick(Medicine medicine) {
                // Not in admin mode
            }
        });
        rvSymptomMedicines.setAdapter(medicineAdapter);

        // Setup filter text search
        etSearchSymptoms.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterSymptomChips(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        btnFindMedicines.setOnClickListener(v -> executeFindMedicines());

        observeViewModel();

        // Check if pre-selected symptom passed via intent
        if (getIntent().hasExtra("symptomId") || getIntent().hasExtra("category")) {
            String target = getIntent().getStringExtra("symptomId");
            if (target == null) target = getIntent().getStringExtra("category");
            checkPreselectedSymptom(target);
        }
    }

    private void observeViewModel() {
        viewModel.getSymptoms().observe(this, symptoms -> {
            allSymptomsList = symptoms;
            populateSymptomChips(symptoms);
        });

        viewModel.getRedFlagAdvice().observe(this, advice -> {
            if (advice != null && !advice.trim().isEmpty()) {
                cardRedFlagBanner.setVisibility(View.VISIBLE);
                tvRedFlagAdvice.setText(advice);
            } else {
                cardRedFlagBanner.setVisibility(View.GONE);
            }
        });

        viewModel.getMatchingMedicines().observe(this, medicines -> {
            if (medicines != null && !medicines.isEmpty()) {
                tvResultsHeader.setVisibility(View.VISIBLE);
                tvNoResults.setVisibility(View.GONE);
                rvSymptomMedicines.setVisibility(View.VISIBLE);
                medicineAdapter.submitList(medicines);
            } else if (tvResultsHeader.getVisibility() == View.VISIBLE) {
                tvNoResults.setVisibility(View.VISIBLE);
                rvSymptomMedicines.setVisibility(View.GONE);
                medicineAdapter.submitList(new ArrayList<>());
            }
        });

        viewModel.getErrorMessage().observe(this, err -> {
            if (err != null) {
                Toast.makeText(this, err, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void populateSymptomChips(List<Symptom> symptoms) {
        cgSymptoms.removeAllViews();
        symptomChipsMap.clear();

        for (Symptom s : symptoms) {
            Chip chip = new Chip(this);
            chip.setText(s.getName());
            chip.setCheckable(true);
            chip.setClickable(true);
            chip.setChecked(s.isSelected());
            chip.setOnCheckedChangeListener((buttonView, isChecked) -> s.setSelected(isChecked));
            cgSymptoms.addView(chip);
            symptomChipsMap.put(s.getId(), chip);
        }
    }

    private void filterSymptomChips(String query) {
        String q = query.trim().toLowerCase();
        for (Symptom s : allSymptomsList) {
            Chip chip = symptomChipsMap.get(s.getId());
            if (chip != null) {
                if (q.isEmpty() || s.getName().toLowerCase().contains(q)) {
                    chip.setVisibility(View.VISIBLE);
                } else {
                    chip.setVisibility(View.GONE);
                }
            }
        }
    }

    private void checkPreselectedSymptom(String target) {
        if (target == null) return;
        String t = target.toLowerCase();
        for (Symptom s : allSymptomsList) {
            if (s.getId().equalsIgnoreCase(t) || s.getName().toLowerCase().contains(t)) {
                s.setSelected(true);
                Chip chip = symptomChipsMap.get(s.getId());
                if (chip != null) chip.setChecked(true);
            }
        }
    }

    private void executeFindMedicines() {
        List<Symptom> selected = new ArrayList<>();
        for (Symptom s : allSymptomsList) {
            if (s.isSelected()) {
                selected.add(s);
            }
        }

        if (selected.isEmpty()) {
            Toast.makeText(this, "Please select at least one symptom", Toast.LENGTH_SHORT).show();
            return;
        }

        tvResultsHeader.setVisibility(View.VISIBLE);

        String selectedAge = "all";
        int agePos = spAgeGroup.getSelectedItemPosition();
        if (agePos == 1) selectedAge = "child";
        else if (agePos == 2) selectedAge = "adult";
        else if (agePos == 3) selectedAge = "senior";

        boolean pregnant = switchPregnant.isChecked();
        boolean excludeRx = switchExcludeRx.isChecked();

        viewModel.findMedicines(selected, selectedAge, pregnant, excludeRx);
    }

    private void addToCart(Medicine medicine) {
        String userId = new SessionManager(this).getUserId() != null ?
                new SessionManager(this).getUserId() : "guest";

        CartItem item = new CartItem(medicine.getId(), medicine.getName(), medicine.getPrice(), 1, medicine.isRequiresPrescription());
        MediRepository.getInstance(this).addToCart(userId, item, new MediRepository.Callback<Void>() {
            @Override
            public void onSuccess(Void result) {
                Toast.makeText(SymptomsActivity.this, medicine.getName() + " added to cart", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(String error) {
                Toast.makeText(SymptomsActivity.this, error, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
