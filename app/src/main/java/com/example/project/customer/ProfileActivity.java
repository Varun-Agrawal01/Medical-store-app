package com.example.project.customer;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.project.MainActivity;
import com.example.project.R;
import com.example.project.data.MediRepository;
import com.example.project.models.User;
import com.example.project.utils.SessionManager;

public class ProfileActivity extends AppCompatActivity {

    private TextView tvName, tvEmail, tvPhone, tvRole;
    private Button btnEditProfile, btnChangePassword, btnLogout;
    private ImageButton btnBack;

    private MediRepository repository;
    private SessionManager sessionManager;
    private User currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        repository = MediRepository.getInstance(this);
        sessionManager = new SessionManager(this);

        btnBack = findViewById(R.id.btnBackProfile);
        tvName = findViewById(R.id.tvProfileName);
        tvEmail = findViewById(R.id.tvProfileEmail);
        tvPhone = findViewById(R.id.tvProfilePhone);
        tvRole = findViewById(R.id.tvProfileRole);

        btnEditProfile = findViewById(R.id.btnNavEditProfile);
        btnChangePassword = findViewById(R.id.btnNavChangePassword);
        btnLogout = findViewById(R.id.btnProfileLogout);

        btnBack.setOnClickListener(v -> finish());

        btnEditProfile.setOnClickListener(v -> {
            Intent intent = new Intent(ProfileActivity.this, EditProfileActivity.class);
            startActivity(intent);
        });

        btnChangePassword.setOnClickListener(v -> {
            Intent intent = new Intent(ProfileActivity.this, ChangePasswordActivity.class);
            startActivity(intent);
        });

        btnLogout.setOnClickListener(v -> performLogout());
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadUserProfile();
    }

    private void loadUserProfile() {
        String userId = sessionManager.getUserId();
        if (userId == null) {
            performLogout();
            return;
        }

        repository.getUserById(userId, new MediRepository.Callback<User>() {
            @Override
            public void onSuccess(User user) {
                currentUser = user;
                tvName.setText(user.getName());
                tvEmail.setText(user.getEmail());
                tvPhone.setText(user.getPhone() != null && !user.getPhone().isEmpty() ? user.getPhone() : "Not provided");
                tvRole.setText("admin".equalsIgnoreCase(user.getRole()) ? "Administrator" : "Customer Account");
            }

            @Override
            public void onError(String error) {
                Toast.makeText(ProfileActivity.this, error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void performLogout() {
        sessionManager.clear();
        Toast.makeText(this, getString(R.string.msg_logout_success), Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(ProfileActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
