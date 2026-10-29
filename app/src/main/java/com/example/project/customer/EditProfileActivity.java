package com.example.project.customer;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.project.R;
import com.example.project.data.MediRepository;
import com.example.project.models.User;
import com.example.project.utils.SessionManager;

public class EditProfileActivity extends AppCompatActivity {

    private EditText etName, etPhone;
    private Button btnSave;
    private ImageButton btnBack;

    private MediRepository repository;
    private SessionManager sessionManager;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        repository = MediRepository.getInstance(this);
        sessionManager = new SessionManager(this);
        currentUserId = sessionManager.getUserId();

        if (currentUserId == null) {
            finish();
            return;
        }

        btnBack = findViewById(R.id.btnBackEditProfile);
        etName = findViewById(R.id.etEditProfileName);
        etPhone = findViewById(R.id.etEditProfilePhone);
        btnSave = findViewById(R.id.btnSaveProfile);

        btnBack.setOnClickListener(v -> finish());
        btnSave.setOnClickListener(v -> saveProfileChanges());

        loadCurrentDetails();
    }

    private void loadCurrentDetails() {
        repository.getUserById(currentUserId, new MediRepository.Callback<User>() {
            @Override
            public void onSuccess(User user) {
                if (user != null) {
                    etName.setText(user.getName());
                    etPhone.setText(user.getPhone());
                }
            }

            @Override
            public void onError(String error) {
                Toast.makeText(EditProfileActivity.this, error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveProfileChanges() {
        String name = etName.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();

        if (name.isEmpty()) {
            Toast.makeText(this, getString(R.string.err_empty_name), Toast.LENGTH_SHORT).show();
            return;
        }

        if (!phone.matches("^[6-9][0-9]{9}$")) {
            Toast.makeText(this, getString(R.string.err_invalid_phone), Toast.LENGTH_SHORT).show();
            return;
        }

        btnSave.setEnabled(false);
        repository.updateProfile(currentUserId, name, phone, new MediRepository.Callback<Void>() {
            @Override
            public void onSuccess(Void result) {
                btnSave.setEnabled(true);
                Toast.makeText(EditProfileActivity.this, getString(R.string.msg_profile_updated), Toast.LENGTH_SHORT).show();
                finish();
            }

            @Override
            public void onError(String error) {
                btnSave.setEnabled(true);
                Toast.makeText(EditProfileActivity.this, error, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
