package com.example.project;

import android.content.Intent;
import android.os.Bundle;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.project.customer.CartActivity;
import com.example.project.customer.MedicineDetailActivity;
import com.example.project.customer.MedicineListActivity;
import com.example.project.customer.OrderHistoryActivity;
import com.example.project.customer.SymptomsActivity;
import com.example.project.data.FirestoreRepository;
import com.example.project.models.User;

public class Home_page extends AppCompatActivity {

    private TextView tvGreeting;
    private ImageButton btnHomeCart, btnHomeLogout;
    private EditText etSearchMedicines;
    private TextView tvSymptomsHeader;
    private Button btnAllSymptoms;
    private Button btnFever, btnCough, btnHeadache, btnAcidity;
    private CheckBox cbCrocin, cbDigene, cbParacetamol;
    private Button btnNavHome, btnNavMedicines, btnNavCart, btnNavOrders;

    private FirestoreRepository repository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.home_page);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_home), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
            return insets;
        });

        repository = FirestoreRepository.getInstance(this);

        initViews();
        setupListeners();
        updateGreeting();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateGreeting();
        // Reset popular medicine checkboxes so they can be clicked again
        if (cbCrocin != null) cbCrocin.setChecked(false);
        if (cbDigene != null) cbDigene.setChecked(false);
        if (cbParacetamol != null) cbParacetamol.setChecked(false);
    }

    private void initViews() {
        tvGreeting = findViewById(R.id.textView13);
        btnHomeCart = findViewById(R.id.btnHomeCart);
        btnHomeLogout = findViewById(R.id.btnHomeLogout);

        etSearchMedicines = findViewById(R.id.editTextText6);
        tvSymptomsHeader = findViewById(R.id.textView15);
        btnAllSymptoms = findViewById(R.id.btnAllSymptoms);

        btnFever = findViewById(R.id.button4);
        btnCough = findViewById(R.id.button5);
        btnHeadache = findViewById(R.id.button16);
        btnAcidity = findViewById(R.id.button7);

        cbCrocin = findViewById(R.id.checkBox);
        cbDigene = findViewById(R.id.checkBox2);
        cbParacetamol = findViewById(R.id.checkBox3);

        btnNavHome = findViewById(R.id.button6);
        btnNavMedicines = findViewById(R.id.button17);
        btnNavCart = findViewById(R.id.btnNavCart);
        btnNavOrders = findViewById(R.id.btnNavOrders);
    }

    private void updateGreeting() {
        User user = repository.getCurrentUser();
        if (user != null && user.getName() != null && !user.getName().isEmpty()) {
            tvGreeting.setText("Hello, " + user.getName() + "!");
        } else {
            tvGreeting.setText("Hello, Customer!");
        }
    }

    private void setupListeners() {
        // Top Bar - Cart & Logout
        if (btnHomeCart != null) {
            btnHomeCart.setOnClickListener(v -> {
                Intent intent = new Intent(Home_page.this, CartActivity.class);
                startActivity(intent);
            });
        }

        if (btnHomeLogout != null) {
            btnHomeLogout.setOnClickListener(v -> performLogout());
        }

        // Search Bar - on enter/search action
        etSearchMedicines.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_DONE) {
                openMedicineCatalogueWithSearch(etSearchMedicines.getText().toString().trim());
                return true;
            }
            return false;
        });

        // Symptoms Category Link & View All
        tvSymptomsHeader.setOnClickListener(v -> {
            Intent intent = new Intent(Home_page.this, SymptomsActivity.class);
            startActivity(intent);
        });

        if (btnAllSymptoms != null) {
            btnAllSymptoms.setOnClickListener(v -> {
                Intent intent = new Intent(Home_page.this, SymptomsActivity.class);
                startActivity(intent);
            });
        }

        // Symptom Quick Buttons
        btnFever.setOnClickListener(v -> openCategoryMedicines("Fever"));
        btnCough.setOnClickListener(v -> openCategoryMedicines("Cold"));
        btnHeadache.setOnClickListener(v -> openCategoryMedicines("Headache"));
        btnAcidity.setOnClickListener(v -> openCategoryMedicines("Acidity"));

        // Popular Medicines Cards / Checkboxes
        cbCrocin.setOnClickListener(v -> openMedicineDetail("med_crocin"));
        cbDigene.setOnClickListener(v -> openMedicineDetail("med_digene"));
        cbParacetamol.setOnClickListener(v -> openMedicineDetail("med_dolo650"));

        // Bottom Navigation Bar
        btnNavHome.setOnClickListener(v -> {
            Toast.makeText(Home_page.this, "You are on the Home screen", Toast.LENGTH_SHORT).show();
        });

        btnNavMedicines.setOnClickListener(v -> {
            Intent intent = new Intent(Home_page.this, MedicineListActivity.class);
            startActivity(intent);
        });

        if (btnNavCart != null) {
            btnNavCart.setOnClickListener(v -> {
                Intent intent = new Intent(Home_page.this, CartActivity.class);
                startActivity(intent);
            });
        }

        if (btnNavOrders != null) {
            btnNavOrders.setOnClickListener(v -> {
                Intent intent = new Intent(Home_page.this, OrderHistoryActivity.class);
                startActivity(intent);
            });
        }
    }

    private void openCategoryMedicines(String category) {
        Intent intent = new Intent(Home_page.this, MedicineListActivity.class);
        intent.putExtra("category", category);
        startActivity(intent);
    }

    private void openMedicineCatalogueWithSearch(String searchQuery) {
        Intent intent = new Intent(Home_page.this, MedicineListActivity.class);
        if (!searchQuery.isEmpty()) {
            intent.putExtra("searchQuery", searchQuery);
        }
        startActivity(intent);
    }

    private void openMedicineDetail(String medicineId) {
        Intent intent = new Intent(Home_page.this, MedicineDetailActivity.class);
        intent.putExtra("medicineId", medicineId);
        startActivity(intent);
    }

    private void performLogout() {
        repository.logout();
        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(Home_page.this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }
}
