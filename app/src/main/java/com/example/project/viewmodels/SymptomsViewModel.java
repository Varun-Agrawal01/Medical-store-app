package com.example.project.viewmodels;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.project.data.MediRepository;
import com.example.project.models.Medicine;
import com.example.project.models.Symptom;

import java.util.ArrayList;
import java.util.List;

public class SymptomsViewModel extends AndroidViewModel {

    private final MediRepository repository;
    private final MutableLiveData<List<Symptom>> symptoms = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<Medicine>> matchingMedicines = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<String> redFlagAdvice = new MutableLiveData<>("");
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>(null);

    public SymptomsViewModel(@NonNull Application application) {
        super(application);
        this.repository = MediRepository.getInstance(application);
        loadSymptoms();
    }

    public LiveData<List<Symptom>> getSymptoms() {
        return symptoms;
    }

    public LiveData<List<Medicine>> getMatchingMedicines() {
        return matchingMedicines;
    }

    public LiveData<String> getRedFlagAdvice() {
        return redFlagAdvice;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public void loadSymptoms() {
        isLoading.setValue(true);
        repository.getSymptoms(new MediRepository.Callback<List<Symptom>>() {
            @Override
            public void onSuccess(List<Symptom> result) {
                isLoading.setValue(false);
                symptoms.setValue(result);
            }

            @Override
            public void onError(String error) {
                isLoading.setValue(false);
                errorMessage.setValue(error);
            }
        });
    }

    public void findMedicines(List<Symptom> selectedSymptoms, String ageGroup,
                              boolean avoidInPregnancy, boolean excludeRx) {
        if (selectedSymptoms == null || selectedSymptoms.isEmpty()) {
            matchingMedicines.setValue(new ArrayList<>());
            redFlagAdvice.setValue("");
            return;
        }

        // Build combined red flag advice
        StringBuilder adviceSb = new StringBuilder();
        List<String> symptomIds = new ArrayList<>();
        for (Symptom s : selectedSymptoms) {
            symptomIds.add(s.getId());
            if (s.getRedFlagAdvice() != null && !s.getRedFlagAdvice().trim().isEmpty()) {
                if (adviceSb.length() > 0) adviceSb.append("\n\n");
                adviceSb.append("• ").append(s.getName()).append(": ").append(s.getRedFlagAdvice());
            }
        }
        redFlagAdvice.setValue(adviceSb.toString());

        isLoading.setValue(true);
        repository.findMedicinesForSymptoms(symptomIds, ageGroup, avoidInPregnancy, excludeRx,
                new MediRepository.Callback<List<Medicine>>() {
                    @Override
                    public void onSuccess(List<Medicine> result) {
                        isLoading.setValue(false);
                        matchingMedicines.setValue(result);
                    }

                    @Override
                    public void onError(String error) {
                        isLoading.setValue(false);
                        errorMessage.setValue(error);
                    }
                });
    }
}
