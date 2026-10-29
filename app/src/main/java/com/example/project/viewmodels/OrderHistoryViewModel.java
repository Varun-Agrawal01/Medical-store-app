package com.example.project.viewmodels;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.project.data.MediRepository;
import com.example.project.models.Order;
import com.example.project.utils.SessionManager;

import java.util.ArrayList;
import java.util.List;

public class OrderHistoryViewModel extends AndroidViewModel {

    private final MediRepository repository;
    private final SessionManager sessionManager;

    private final MutableLiveData<List<Order>> orders = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>(null);

    public OrderHistoryViewModel(@NonNull Application application) {
        super(application);
        this.repository = MediRepository.getInstance(application);
        this.sessionManager = new SessionManager(application);
        loadOrders();
    }

    public LiveData<List<Order>> getOrders() {
        return orders;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public void loadOrders() {
        String userId = sessionManager.getUserId();
        if (userId == null) {
            userId = "guest";
        }
        isLoading.setValue(true);
        errorMessage.setValue(null);

        repository.getCustomerOrders(userId, new MediRepository.Callback<List<Order>>() {
            @Override
            public void onSuccess(List<Order> result) {
                isLoading.setValue(false);
                orders.setValue(result != null ? result : new ArrayList<>());
            }

            @Override
            public void onError(String error) {
                isLoading.setValue(false);
                errorMessage.setValue(error);
            }
        });
    }
}
