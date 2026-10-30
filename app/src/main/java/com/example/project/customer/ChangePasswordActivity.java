package com.example.project.customer;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.project.R;
import com.example.project.data.MediRepository;
import com.example.project.utils.SessionManager;

public class ChangePasswordActivity extends AppCompatActivity {

    private EditText etCurrentPassword, etNewPassword, etConfirmNewPassword;
    private Button btnSubmit;
    private ImageButton btnBack;

    private MediRepository repository;
    private SessionManager sessionManager;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_password);

        repository = MediRepository.getInstance(this);
        sessionManager = new SessionManager(this);
        currentUserId = sessionManager.getUserId();

        if (currentUserId == null) {
            finish();
            return;
        }

        btnBack = findViewById(R.id.btnBackChangePassword);
        etCurrentPassword = findViewById(R.id.etCurrentPassword);
        etNewPassword = findViewById(R.id.etNewPassword);
        etConfirmNewPassword = findViewById(R.id.etConfirmNewPassword);
        btnSubmit = findViewById(R.id.btnSubmitChangePassword);

        btnBack.setOnClickListener(v -> finish());
        btnSubmit.setOnClickListener(v -> attemptChangePassword());
    }

    private void attemptChangePassword() {
        String currentPass = etCurrentPassword.getText().toString();
        String newPass = etNewPassword.getText().toString();
        String confirmPass = etConfirmNewPassword.getText().toString();

        if (currentPass.isEmpty() || newPass.isEmpty() || confirmPass.isEmpty()) {
            Toast.makeText(this, getString(R.string.err_fill_all_fields), Toast.LENGTH_SHORT).show();
            return;
        }

        if (newPass.length() < 8 || !newPass.matches(".*[A-Za-z].*") || !newPass.matches(".*\\d.*")) {
            Toast.makeText(this, getString(R.string.err_invalid_password), Toast.LENGTH_LONG).show();
            return;
        }

        if (!newPass.equals(confirmPass)) {
            Toast.makeText(this, getString(R.string.err_password_mismatch), Toast.LENGTH_SHORT).show();
            return;
        }

        btnSubmit.setEnabled(false);
        repository.changePassword(currentUserId, currentPass, newPass, new MediRepository.Callback<Void>() {
            @Override
            public void onSuccess(Void result) {
                btnSubmit.setEnabled(true);
                Toast.makeText(ChangePasswordActivity.this, getString(R.string.msg_password_updated), Toast.LENGTH_SHORT).show();
                finish();
            }

            @Override
            public void onError(String error) {
                btnSubmit.setEnabled(true);
                Toast.makeText(ChangePasswordActivity.this, error, Toast.LENGTH_LONG).show();
            }
        });
    }
}
