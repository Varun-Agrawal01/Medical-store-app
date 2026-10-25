package com.example.project;

import android.content.Intent;
import android.os.Bundle;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.project.auth.AdminLoginActivity;
import com.example.project.auth.RegisterActivity;
import com.example.project.data.FirestoreRepository;
import com.example.project.models.User;

public class MainActivity extends AppCompatActivity {

    private EditText etEmailOrPhone;
    private EditText etPassword;
    private Button btnLogin;
    private TextView tvRegisterLink;
    private TextView tvForgotPassword;
    private TextView tvAdminLoginLink;
    private Switch switchShowPassword;
    private FirestoreRepository repository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.c_login_page);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        repository = FirestoreRepository.getInstance(this);

        etEmailOrPhone = findViewById(R.id.editTextText);
        etPassword = findViewById(R.id.editTextTextPassword);
        btnLogin = findViewById(R.id.button);
        tvRegisterLink = findViewById(R.id.textView7);
        tvForgotPassword = findViewById(R.id.textView8);
        tvAdminLoginLink = findViewById(R.id.tvAdminLoginLink);
        switchShowPassword = findViewById(R.id.switch1);

        // Login Button Listener
        btnLogin.setOnClickListener(v -> performCustomerLogin());

        // Register Link Listener
        tvRegisterLink.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, RegisterActivity.class);
            startActivity(intent);
        });

        // Admin Login Link Listener
        if (tvAdminLoginLink != null) {
            tvAdminLoginLink.setOnClickListener(v -> {
                Intent intent = new Intent(MainActivity.this, AdminLoginActivity.class);
                startActivity(intent);
            });
        }

        // Forgot Password Listener
        tvForgotPassword.setOnClickListener(v -> {
            String input = etEmailOrPhone.getText().toString().trim();
            if (input.isEmpty()) {
                Toast.makeText(MainActivity.this, "Enter your email above to reset password", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(MainActivity.this, "Password reset instructions sent to " + input, Toast.LENGTH_LONG).show();
            }
        });

        // Show/Hide Password Switch Listener
        switchShowPassword.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                etPassword.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
            } else {
                etPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
            }
            etPassword.setSelection(etPassword.getText().length());
        });
    }

    private void performCustomerLogin() {
        String emailOrPhone = etEmailOrPhone.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (emailOrPhone.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please enter both Email/Phone and Password", Toast.LENGTH_SHORT).show();
            return;
        }

        btnLogin.setEnabled(false);
        repository.loginUser(emailOrPhone, password, new FirestoreRepository.Callback<User>() {
            @Override
            public void onSuccess(User user) {
                btnLogin.setEnabled(true);
                String displayName = user != null && user.getName() != null && !user.getName().isEmpty()
                        ? user.getName()
                        : "Customer";
                Toast.makeText(MainActivity.this, "Welcome, " + displayName + "!", Toast.LENGTH_SHORT).show();

                Intent intent = new Intent(MainActivity.this, Home_page.class);
                startActivity(intent);
                finish();
            }

            @Override
            public void onError(String error) {
                btnLogin.setEnabled(true);
                Toast.makeText(MainActivity.this, "Login Failed: " + error, Toast.LENGTH_LONG).show();
            }
        });
    }
}
