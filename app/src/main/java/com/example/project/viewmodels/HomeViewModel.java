package com.example.project.viewmodels;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.project.data.MediRepository;
import com.example.project.models.Medicine;
import com.example.project.models.Symptom;
import com.example.project.models.User;
import com.example.project.utils.SessionManager;

import java.util.ArrayList;
import java.util.List;

public class HomeViewModel extends AndroidViewModel {

    private final MediRepository repository;
    private final SessionManager sessionManager;

    private final MutableLiveData<User> currentUser = new MutableLiveData<>(null);
    private final MutableLiveData<List<Symptom>> popularSymptoms = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<Medicine>> medicines = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>(null);

    public HomeViewModel(@NonNull Application application) {
        super(application);
        this.repository = MediRepository.getInstance(application);
        this.sessionManager = new SessionManager(application);

        loadUserData();
        loadPopularSymptoms();
        loadMedicines();
    }

    public LiveData<User> getCurrentUser() {
        return currentUser;
    }

    public LiveData<List<Symptom>> getPopularSymptoms() {
        return popularSymptoms;
    }

    public LiveData<List<Medicine>> getMedicines() {
        return medicines;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public void loadUserData() {
        String userId = sessionManager.getUserId();
        if (userId != null) {
            repository.getUserById(userId, new MediRepository.Callback<User>() {
                @Override
                public void onSuccess(User user) {
                    currentUser.setValue(user);
                }

                @Override
                public void onError(String error) {
                    // Non fatal
                }
            });
        }
    }

    public void loadPopularSymptoms() {
        repository.getPopularSymptoms(10, new MediRepository.Callback<List<Symptom>>() {
            @Override
            public void onSuccess(List<Symptom> result) {
                popularSymptoms.setValue(result);
            }

            @Override
            public void onError(String error) {
                // Non fatal
            }
        });
    }

    public void loadMedicines() {
        isLoading.setValue(true);
        errorMessage.setValue(null);
        repository.getMedicinesPaged(30, 0, null, new MediRepository.Callback<List<Medicine>>() {
            @Override
            public void onSuccess(List<Medicine> result) {
                isLoading.setValue(false);
                medicines.setValue(result);
            }

            @Override
            public void onError(String error) {
                isLoading.setValue(false);
                errorMessage.setValue(error);
            }
        });
    }

    public void searchMedicines(String query) {
        isLoading.setValue(true);
        errorMessage.setValue(null);
        repository.searchMedicines(query, 30, 0, new MediRepository.Callback<List<Medicine>>() {
            @Override
            public void onSuccess(List<Medicine> result) {
                isLoading.setValue(false);
                medicines.setValue(result);
            }

            @Override
            public void onError(String error) {
                isLoading.setValue(false);
                errorMessage.setValue(error);
            }
        });
    }
}
