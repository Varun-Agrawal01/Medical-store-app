package com.example.project.auth;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.project.R;
import com.example.project.admin.AdminDashboardActivity;
import com.example.project.data.FirestoreRepository;
import com.example.project.models.User;

public class AdminLoginActivity extends AppCompatActivity {

    private EditText etShopkeeperId, etAdminPass;
    private Button btnAdminLogin, btnBackToCustomerLogin;
    private FirestoreRepository repository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.admin_login);

        repository = FirestoreRepository.getInstance(this);

        etShopkeeperId = findViewById(R.id.editTextText7);
        etAdminPass = findViewById(R.id.editTextText8);
        btnAdminLogin = findViewById(R.id.button8);
        btnBackToCustomerLogin = findViewById(R.id.btnBackToCustomerLogin);

        btnBackToCustomerLogin.setOnClickListener(v -> finish());

        btnAdminLogin.setOnClickListener(v -> performAdminLogin());
    }

    private void performAdminLogin() {
        String shopkeeperId = etShopkeeperId.getText().toString().trim();
        String password = etAdminPass.getText().toString().trim();

        if (shopkeeperId.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please enter Shopkeeper ID and password", Toast.LENGTH_SHORT).show();
            return;
        }

        btnAdminLogin.setEnabled(false);
        repository.adminLogin(shopkeeperId, password, new FirestoreRepository.Callback<User>() {
            @Override
            public void onSuccess(User adminUser) {
                btnAdminLogin.setEnabled(true);
                Toast.makeText(AdminLoginActivity.class.getCanonicalName() != null ? AdminLoginActivity.this : null, "Admin Login Successful!", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(AdminLoginActivity.this, AdminDashboardActivity.class);
                startActivity(intent);
                finish();
            }

            @Override
            public void onError(String error) {
                btnAdminLogin.setEnabled(true);
                Toast.makeText(AdminLoginActivity.this, error, Toast.LENGTH_LONG).show();
            }
        });
    }
}
