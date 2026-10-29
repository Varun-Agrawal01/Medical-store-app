package com.example.project.auth;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.project.HomeActivity;
import com.example.project.R;
import com.example.project.data.MediRepository;
import com.example.project.models.User;
import com.example.project.utils.SessionManager;

public class RegisterActivity extends AppCompatActivity {

    private EditText etName, etPhone, etEmail, etPass;
    private Button btnCreateAccount;
    private ImageView ivBack;
    private MediRepository repository;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.registration_page);

        repository = MediRepository.getInstance(this);
        sessionManager = new SessionManager(this);

        etName = findViewById(R.id.editTextText2);
        etPhone = findViewById(R.id.editTextText3);
        etEmail = findViewById(R.id.editTextText4);
        etPass = findViewById(R.id.editTextText5);
        btnCreateAccount = findViewById(R.id.button3);
        ivBack = findViewById(R.id.imageView);

        ivBack.setOnClickListener(v -> finish());
        btnCreateAccount.setOnClickListener(v -> performRegistration());
    }

    private void performRegistration() {
        String name = etName.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String pass = etPass.getText().toString();

        if (name.isEmpty()) {
            Toast.makeText(this, getString(R.string.err_empty_name), Toast.LENGTH_SHORT).show();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, getString(R.string.err_invalid_email), Toast.LENGTH_SHORT).show();
            return;
        }

        if (!phone.matches("^[6-9][0-9]{9}$")) {
            Toast.makeText(this, getString(R.string.err_invalid_phone), Toast.LENGTH_SHORT).show();
            return;
        }

        if (pass.length() < 8 || !pass.matches(".*[A-Za-z].*") || !pass.matches(".*\\d.*")) {
            Toast.makeText(this, getString(R.string.err_invalid_password), Toast.LENGTH_LONG).show();
            return;
        }

        btnCreateAccount.setEnabled(false);
        // Role is always "customer" from the registration screen
        repository.registerUser(name, email, phone, pass, "customer", new MediRepository.Callback<User>() {
            @Override
            public void onSuccess(User newUser) {
                btnCreateAccount.setEnabled(true);
                sessionManager.saveUserId(newUser.getId());
                Toast.makeText(RegisterActivity.this, "Account created successfully!", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(RegisterActivity.this, HomeActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }

            @Override
            public void onError(String error) {
                btnCreateAccount.setEnabled(true);
                Toast.makeText(RegisterActivity.this, error, Toast.LENGTH_LONG).show();
            }
        });
    }
}
