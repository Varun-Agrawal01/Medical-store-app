package com.example.project;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.project.admin.AdminDashboardActivity;
import com.example.project.data.MediRepository;
import com.example.project.models.User;
import com.example.project.utils.SessionManager;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        SessionManager sessionManager = new SessionManager(this);
        String userId = sessionManager.getUserId();

        if (userId == null) {
            navigateToLogin();
            return;
        }

        MediRepository repo = MediRepository.getInstance(this);
        repo.getUserById(userId, new MediRepository.Callback<User>() {
            @Override
            public void onSuccess(User user) {
                if (user != null && "admin".equalsIgnoreCase(user.getRole())) {
                    startActivity(new Intent(SplashActivity.this, AdminDashboardActivity.class));
                } else {
                    startActivity(new Intent(SplashActivity.this, HomeActivity.class));
                }
                finish();
            }

            @Override
            public void onError(String error) {
                sessionManager.clear();
                navigateToLogin();
            }
        });
    }

    private void navigateToLogin() {
        startActivity(new Intent(SplashActivity.this, MainActivity.class));
        finish();
    }
}
