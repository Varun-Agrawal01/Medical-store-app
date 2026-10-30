package com.example.project.viewmodels;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.project.data.MediRepository;
import com.example.project.models.CartItem;
import com.example.project.utils.PricingUtils;
import com.example.project.utils.SessionManager;

import java.util.ArrayList;
import java.util.List;

public class CartViewModel extends AndroidViewModel {

    private final MediRepository repository;
    private final SessionManager sessionManager;

    private final MutableLiveData<List<CartItem>> cartItems = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Double> subtotal = new MutableLiveData<>(0.0);
    private final MutableLiveData<Double> deliveryFee = new MutableLiveData<>(0.0);
    private final MutableLiveData<Double> grandTotal = new MutableLiveData<>(0.0);
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>(null);

    public CartViewModel(@NonNull Application application) {
        super(application);
        this.repository = MediRepository.getInstance(application);
        this.sessionManager = new SessionManager(application);
        loadCart();
    }

    public LiveData<List<CartItem>> getCartItems() {
        return cartItems;
    }

    public LiveData<Double> getSubtotal() {
        return subtotal;
    }

    public LiveData<Double> getDeliveryFee() {
        return deliveryFee;
    }

    public LiveData<Double> getGrandTotal() {
        return grandTotal;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    private String getUserId() {
        return sessionManager.getUserId() != null ? sessionManager.getUserId() : "guest";
    }

    public void loadCart() {
        isLoading.setValue(true);
        errorMessage.setValue(null);
        repository.getCart(getUserId(), new MediRepository.Callback<List<CartItem>>() {
            @Override
            public void onSuccess(List<CartItem> items) {
                isLoading.setValue(false);
                updatePricing(items);
            }

            @Override
            public void onError(String error) {
                isLoading.setValue(false);
                errorMessage.setValue(error);
            }
        });
    }

    public void updateQuantity(CartItem item, int newQty) {
        List<CartItem> current = new ArrayList<>(cartItems.getValue() != null ? cartItems.getValue() : new ArrayList<>());
        for (CartItem ci : current) {
            if (ci.getMedicineId().equals(item.getMedicineId())) {
                ci.setQuantity(newQty);
                break;
            }
        }
        saveCartList(current);
    }

    public void removeItem(CartItem item) {
        List<CartItem> current = new ArrayList<>(cartItems.getValue() != null ? cartItems.getValue() : new ArrayList<>());
        current.removeIf(ci -> ci.getMedicineId().equals(item.getMedicineId()));
        saveCartList(current);
    }

    private void saveCartList(List<CartItem> items) {
        updatePricing(items);
        repository.saveCart(getUserId(), items, new MediRepository.Callback<Void>() {
            @Override
            public void onSuccess(Void result) {
                // saved successfully
            }

            @Override
            public void onError(String error) {
                errorMessage.setValue(error);
            }
        });
    }

    private void updatePricing(List<CartItem> items) {
        cartItems.setValue(items);
        double sub = 0.0;
        for (CartItem ci : items) {
            sub += ci.getItemTotal();
        }
        double fee = PricingUtils.calculateDeliveryFee(sub);
        double total = PricingUtils.calculateTotal(sub, fee);

        subtotal.setValue(sub);
        deliveryFee.setValue(fee);
        grandTotal.setValue(total);
    }
}
