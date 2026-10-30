package com.example.project;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.project.auth.AdminLoginActivity;
import com.example.project.auth.RegisterActivity;
import com.example.project.data.MediRepository;
import com.example.project.models.User;
import com.example.project.utils.SessionManager;

public class MainActivity extends AppCompatActivity {

    private EditText user, pass;
    private Button submit;
    private TextView tvRegister, tvAdminLoginLink;
    private MediRepository repository;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.c_login_page);

        repository = MediRepository.getInstance(this);
        sessionManager = new SessionManager(this);

        user = findViewById(R.id.user);
        pass = findViewById(R.id.pass);
        submit = findViewById(R.id.submit);
        tvRegister = findViewById(R.id.textView7);
        tvAdminLoginLink = findViewById(R.id.tvAdminLoginLink);

        submit.setOnClickListener(v -> performLogin());

        tvRegister.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, RegisterActivity.class);
            startActivity(intent);
        });

        tvAdminLoginLink.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AdminLoginActivity.class);
            startActivity(intent);
        });
    }

    private void performLogin() {
        String username = user.getText().toString().trim();
        String password = pass.getText().toString();

        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show();
            return;
        }

        submit.setEnabled(false);
        repository.loginUser(username, password, new MediRepository.Callback<User>() {
            @Override
            public void onSuccess(User loggedInUser) {
                submit.setEnabled(true);
                sessionManager.saveUserId(loggedInUser.getId());
                Toast.makeText(MainActivity.this, "Welcome " + loggedInUser.getName(), Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(MainActivity.this, HomeActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }

            @Override
            public void onError(String error) {
                submit.setEnabled(true);
                Toast.makeText(MainActivity.this, error, Toast.LENGTH_LONG).show();
            }
        });
    }
}
//done with it
