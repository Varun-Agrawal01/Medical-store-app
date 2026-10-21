package com.example.project.utils;

import com.example.project.models.Medicine;
import com.example.project.models.SymptomCategory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SeedData {

    public static List<Medicine> getSampleMedicines() {
        List<Medicine> list = new ArrayList<>();
        long now = System.currentTimeMillis();

        list.add(new Medicine(
                "med_dolo650",
                "Dolo 650",
                "Paracetamol 650mg",
                "Used for relieving fever and mild to moderate pain.",
                32.50,
                50,
                "",
                true,
                false,
                "Fever",
                now,
                now
        ));

        list.add(new Medicine(
                "med_crocin",
                "Crocin Pain Relief",
                "Paracetamol + Caffeine",
                "Fast action for headaches and body aches.",
                45.00,
                30,
                "",
                true,
                false,
                "Headache",
                now,
                now
        ));

        list.add(new Medicine(
                "med_cetirizine",
                "Cetirizine 10mg",
                "Cetirizine Hydrochloride",
                "Provides relief from runny nose, sneezing, and allergies.",
                25.00,
                40,
                "",
                true,
                false,
                "Cold",
                now,
                now
        ));

        list.add(new Medicine(
                "med_digene",
                "Digene Antacid Liquid",
                "Magnesium Hydroxide + Aluminium Hydroxide",
                "Provides quick relief from acidity, heartburn, and gas.",
                115.00,
                20,
                "",
                true,
                false,
                "Acidity",
                now,
                now
        ));

        list.add(new Medicine(
                "med_coughsyrup",
                "Benadryl Cough Formula",
                "Diphenhydramine HCl",
                "Relieves cough and throat irritation.",
                135.00,
                15,
                "",
                true,
                false,
                "Cold",
                now,
                now
        ));

        list.add(new Medicine(
                "med_aspirin",
                "Disprin 325mg",
                "Aspirin",
                "Effective for acute headaches and fever.",
                18.00,
                0, // Out of stock for testing
                "",
                true,
                false,
                "Headache",
                now,
                now
        ));

        list.add(new Medicine(
                "med_amoxicillin",
                "Amoxicillin 500mg",
                "Amoxicillin Trihydrate",
                "Antibiotic used for bacterial infections.",
                85.00,
                25,
                "",
                true,
                true, // Prescription required
                "General",
                now,
                now
        ));

        return list;
    }

    public static List<SymptomCategory> getSampleCategories() {
        List<SymptomCategory> list = new ArrayList<>();

        list.add(new SymptomCategory(
                "cat_fever",
                "Fever",
                "Medicines for managing fever and body temperature.",
                Arrays.asList("med_dolo650", "med_crocin")
        ));

        list.add(new SymptomCategory(
                "cat_cold",
                "Cold & Cough",
                "Medicines for runny nose, allergies, and cough.",
                Arrays.asList("med_cetirizine", "med_coughsyrup")
        ));

        list.add(new SymptomCategory(
                "cat_headache",
                "Headache",
                "Pain relievers for mild to severe headaches.",
                Arrays.asList("med_crocin", "med_aspirin")
        ));

        list.add(new SymptomCategory(
                "cat_acidity",
                "Acidity",
                "Antacids for heartburn, acidity, and bloating.",
                Arrays.asList("med_digene")
        ));

        return list;
    }
}
