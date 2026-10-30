package com.example.project.viewmodels;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.project.data.MediRepository;
import com.example.project.models.Medicine;

import java.util.ArrayList;
import java.util.List;

public class MedicineListViewModel extends AndroidViewModel {

    public static final int PAGE_SIZE = 30;

    private final MediRepository repository;
    private final MutableLiveData<List<Medicine>> medicines = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> isPaginationLoading = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> isLastPage = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>(null);

    private String currentCategory = null;
    private String currentSearchQuery = "";
    private int currentOffset = 0;
    private final List<Medicine> loadedItems = new ArrayList<>();

    public MedicineListViewModel(@NonNull Application application) {
        super(application);
        this.repository = MediRepository.getInstance(application);
    }

    public LiveData<List<Medicine>> getMedicines() {
        return medicines;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public LiveData<Boolean> getIsPaginationLoading() {
        return isPaginationLoading;
    }

    public LiveData<Boolean> getIsLastPage() {
        return isLastPage;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public void init(String category) {
        this.currentCategory = category;
        this.currentSearchQuery = "";
        refresh();
    }

    public void refresh() {
        currentOffset = 0;
        isLastPage.setValue(false);
        loadedItems.clear();
        loadData(true);
    }

    public void search(String query) {
        this.currentSearchQuery = query != null ? query.trim() : "";
        currentOffset = 0;
        isLastPage.setValue(false);
        loadedItems.clear();
        loadData(true);
    }

    public void loadNextPage() {
        if (Boolean.TRUE.equals(isLastPage.getValue()) ||
                Boolean.TRUE.equals(isLoading.getValue()) ||
                Boolean.TRUE.equals(isPaginationLoading.getValue())) {
            return;
        }
        loadData(false);
    }

    private void loadData(boolean isInitial) {
        if (isInitial) {
            isLoading.setValue(true);
        } else {
            isPaginationLoading.setValue(true);
        }
        errorMessage.setValue(null);

        MediRepository.Callback<List<Medicine>> callback = new MediRepository.Callback<List<Medicine>>() {
            @Override
            public void onSuccess(List<Medicine> result) {
                if (isInitial) {
                    isLoading.setValue(false);
                } else {
                    isPaginationLoading.setValue(false);
                }

                if (result.size() < PAGE_SIZE) {
                    isLastPage.setValue(true);
                }

                currentOffset += result.size();
                loadedItems.addAll(result);
                medicines.setValue(new ArrayList<>(loadedItems));
            }

            @Override
            public void onError(String error) {
                if (isInitial) {
                    isLoading.setValue(false);
                } else {
                    isPaginationLoading.setValue(false);
                }
                errorMessage.setValue(error);
            }
        };

        if (!currentSearchQuery.isEmpty()) {
            repository.searchMedicines(currentSearchQuery, PAGE_SIZE, currentOffset, callback);
        } else {
            repository.getMedicinesPaged(PAGE_SIZE, currentOffset, currentCategory, callback);
        }
    }
}
