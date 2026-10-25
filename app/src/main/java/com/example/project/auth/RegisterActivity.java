package com.example.project.auth;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.project.Home_page;
import com.example.project.R;
import com.example.project.data.FirestoreRepository;
import com.example.project.models.User;

public class RegisterActivity extends AppCompatActivity {

    private EditText etName, etPhone, etEmail, etPass;
    private Button btnCreateAccount;
    private ImageView ivBack;
    private FirestoreRepository repository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.registration_page);

        repository = FirestoreRepository.getInstance(this);

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
        String pass = etPass.getText().toString().trim();

        if (name.isEmpty() || phone.isEmpty() || email.isEmpty() || pass.isEmpty()) {
            Toast.makeText(this, "Please fill in all registration fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (pass.length() < 6) {
            Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
            return;
        }

        btnCreateAccount.setEnabled(false);
        repository.registerUser(name, email, phone, pass, "customer", new FirestoreRepository.Callback<User>() {
            @Override
            public void onSuccess(User newUser) {
                btnCreateAccount.setEnabled(true);
                Toast.makeText(RegisterActivity.this, "Account created successfully!", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(RegisterActivity.this, Home_page.class);
                startActivity(intent);
                finish();
            }

            @Override
            public void onError(String error) {
                btnCreateAccount.setEnabled(true);
                Toast.makeText(RegisterActivity.this, "Registration Failed: " + error, Toast.LENGTH_LONG).show();
            }
        });
    }
}
