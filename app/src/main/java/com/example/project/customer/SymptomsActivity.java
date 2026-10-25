package com.example.project.customer;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.project.R;

public class SymptomsActivity extends AppCompatActivity {

    private CheckBox cbFever, cbCold, cbHeadache, cbThroat, cbAcidity, cbBodyPain;
    private Button btnFind;
    private ImageButton btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.symtoms_page);

        btnBack = findViewById(R.id.btnBackSymptoms);
        cbFever = findViewById(R.id.checkBox4);
        cbCold = findViewById(R.id.checkBox5);
        cbHeadache = findViewById(R.id.checkBox6);
        cbThroat = findViewById(R.id.checkBox7);
        cbAcidity = findViewById(R.id.checkBox8);
        cbBodyPain = findViewById(R.id.checkBox9);
        btnFind = findViewById(R.id.button2);

        btnBack.setOnClickListener(v -> finish());

        btnFind.setOnClickListener(v -> {
            String selectedCategory = "";
            if (cbFever.isChecked()) selectedCategory = "Fever";
            else if (cbCold.isChecked() || cbThroat.isChecked()) selectedCategory = "Cold";
            else if (cbHeadache.isChecked() || cbBodyPain.isChecked()) selectedCategory = "Headache";
            else if (cbAcidity.isChecked()) selectedCategory = "Acidity";

            if (selectedCategory.isEmpty()) {
                Toast.makeText(this, "Please select at least one symptom", Toast.LENGTH_SHORT).show();
                return;
            }

            Intent intent = new Intent(SymptomsActivity.this, MedicineListActivity.class);
            intent.putExtra("category", selectedCategory);
            startActivity(intent);
        });
    }
}
