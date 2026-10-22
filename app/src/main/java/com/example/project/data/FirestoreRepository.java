package com.example.project.data;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.project.models.CartItem;
import com.example.project.models.Medicine;
import com.example.project.models.Order;
import com.example.project.models.OrderItem;
import com.example.project.models.User;
import com.example.project.utils.SeedData;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class FirestoreRepository {

    private static FirestoreRepository instance;
    private final Context context;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private boolean isFirebaseAvailable = false;

    // Fallback local memory storage if Firebase is not yet configured
    private static User currentLocalUser = null;
    private static final Map<String, User> localUsers = new HashMap<>();
    private static final Map<String, Medicine> localMedicines = new HashMap<>();
    private static final Map<String, List<CartItem>> localCarts = new HashMap<>();
    private static final Map<String, Order> localOrders = new HashMap<>();

    public interface Callback<T> {
        void onSuccess(T result);
        void onError(String error);
    }

    private FirestoreRepository(Context context) {
        this.context = context.getApplicationContext();
        try {
            if (!FirebaseApp.getApps(this.context).isEmpty()) {
                db = FirebaseFirestore.getInstance();
                auth = FirebaseAuth.getInstance();
                isFirebaseAvailable = true;
            }
        } catch (Exception e) {
            isFirebaseAvailable = false;
        }

        initDefaultLocalData();
    }

    public static synchronized FirestoreRepository getInstance(Context context) {
        if (instance == null) {
            instance = new FirestoreRepository(context);
        }
        return instance;
    }

    public boolean isFirebaseAvailable() {
        return isFirebaseAvailable;
    }

    private void initDefaultLocalData() {
        if (localUsers.isEmpty()) {
            User demoAdmin = new User("admin_123", "Medi Admin", "admin@medistore.com", "9876543210", "admin", System.currentTimeMillis());
            User demoCustomer = new User("cust_123", "John Doe", "john@example.com", "9123456789", "customer", System.currentTimeMillis());
            localUsers.put(demoAdmin.getEmail(), demoAdmin);
            localUsers.put(demoCustomer.getEmail(), demoCustomer);
            currentLocalUser = demoCustomer;
        }

        if (localMedicines.isEmpty()) {
            List<Medicine> seeds = SeedData.getSampleMedicines();
            for (Medicine m : seeds) {
                localMedicines.put(m.getId(), m);
            }
        }
    }

    // AUTHENTICATION
    public void registerUser(String name, String email, String phone, String password, String role, Callback<User> callback) {
        if (isFirebaseAvailable) {
            auth.createUserWithEmailAndPassword(email, password).addOnSuccessListener(authResult -> {
                FirebaseUser fUser = authResult.getUser();
                if (fUser == null) {
                    callback.onError("Failed to obtain created user ID");
                    return;
                }
                String uid = fUser.getUid();
                User user = new User(uid, name, email, phone, role, System.currentTimeMillis());
                db.collection("users").document(uid).set(user)
                        .addOnSuccessListener(aVoid -> callback.onSuccess(user))
                        .addOnFailureListener(e -> callback.onError(e.getMessage()));
            }).addOnFailureListener(e -> callback.onError(e.getMessage()));
        } else {
            if (localUsers.containsKey(email)) {
                callback.onError("User already exists with email: " + email);
                return;
            }
            String uid = "user_" + UUID.randomUUID().toString().substring(0, 8);
            User user = new User(uid, name, email, phone, role, System.currentTimeMillis());
            localUsers.put(email, user);
            currentLocalUser = user;
            callback.onSuccess(user);
        }
    }

    public void loginUser(String email, String password, Callback<User> callback) {
        if (isFirebaseAvailable) {
            auth.signInWithEmailAndPassword(email, password).addOnSuccessListener(authResult -> {
                FirebaseUser fUser = authResult.getUser();
                if (fUser == null) {
                    callback.onError("Invalid user record");
                    return;
                }
                db.collection("users").document(fUser.getUid()).get()
                        .addOnSuccessListener(snapshot -> {
                            if (snapshot.exists()) {
                                User user = snapshot.toObject(User.class);
                                callback.onSuccess(user);
                            } else {
                                // Default customer role if doc missing
                                User user = new User(fUser.getUid(), "Customer", email, "", "customer", System.currentTimeMillis());
                                callback.onSuccess(user);
                            }
                        })
                        .addOnFailureListener(e -> callback.onError(e.getMessage()));
            }).addOnFailureListener(e -> callback.onError(e.getMessage()));
        } else {
            User user = localUsers.get(email);
            if (user != null) {
                currentLocalUser = user;
                callback.onSuccess(user);
            } else {
                // Auto create demo customer if credentials provided
                User newCust = new User("cust_" + System.currentTimeMillis(), "Customer", email, "9876543210", "customer", System.currentTimeMillis());
                localUsers.put(email, newCust);
                currentLocalUser = newCust;
                callback.onSuccess(newCust);
            }
        }
    }

    public void adminLogin(String shopkeeperId, String password, Callback<User> callback) {
        if (isFirebaseAvailable) {
            String email = shopkeeperId.contains("@") ? shopkeeperId : shopkeeperId + "@medistore.com";
            auth.signInWithEmailAndPassword(email, password).addOnSuccessListener(authResult -> {
                FirebaseUser fUser = authResult.getUser();
                if (fUser == null) {
                    callback.onError("Invalid admin credential");
                    return;
                }
                db.collection("users").document(fUser.getUid()).get()
                        .addOnSuccessListener(snapshot -> {
                            User u = snapshot.toObject(User.class);
                            if (u != null && "admin".equalsIgnoreCase(u.getRole())) {
                                callback.onSuccess(u);
                            } else {
                                callback.onError("Access denied: User is not an authorized Admin");
                            }
                        })
                        .addOnFailureListener(e -> callback.onError(e.getMessage()));
            }).addOnFailureListener(e -> callback.onError("Admin Login failed: " + e.getMessage()));
        } else {
            if ("admin".equalsIgnoreCase(shopkeeperId) || shopkeeperId.contains("admin")) {
                User admin = new User("admin_123", "Medi Store Admin", shopkeeperId, "9876543210", "admin", System.currentTimeMillis());
                currentLocalUser = admin;
                callback.onSuccess(admin);
            } else {
                callback.onError("Invalid Shopkeeper ID or Admin Password");
            }
        }
    }

    public User getCurrentUser() {
        if (isFirebaseAvailable && auth.getCurrentUser() != null) {
            FirebaseUser fu = auth.getCurrentUser();
            return new User(fu.getUid(), fu.getDisplayName() != null ? fu.getDisplayName() : "Customer", fu.getEmail(), "", "customer", System.currentTimeMillis());
        }
        return currentLocalUser;
    }

    public void logout() {
        if (isFirebaseAvailable && auth != null) {
            auth.signOut();
        }
        currentLocalUser = null;
    }

    // MEDICINES / INVENTORY
    public void getMedicines(Callback<List<Medicine>> callback) {
        if (isFirebaseAvailable) {
            db.collection("medicines").get().addOnSuccessListener(queryDocumentSnapshots -> {
                List<Medicine> list = new ArrayList<>();
                for (DocumentSnapshot doc : queryDocumentSnapshots) {
                    Medicine m = doc.toObject(Medicine.class);
                    if (m != null) list.add(m);
                }
                callback.onSuccess(list);
            }).addOnFailureListener(e -> callback.onError(e.getMessage()));
        } else {
            callback.onSuccess(new ArrayList<>(localMedicines.values()));
        }
    }

    public void saveMedicine(Medicine medicine, Callback<Void> callback) {
        if (medicine.getId() == null || medicine.getId().isEmpty()) {
            medicine.setId("med_" + UUID.randomUUID().toString().substring(0, 8));
        }
        medicine.setUpdatedAt(System.currentTimeMillis());

        if (isFirebaseAvailable) {
            db.collection("medicines").document(medicine.getId()).set(medicine)
                    .addOnSuccessListener(aVoid -> callback.onSuccess(null))
                    .addOnFailureListener(e -> callback.onError(e.getMessage()));
        } else {
            localMedicines.put(medicine.getId(), medicine);
            callback.onSuccess(null);
        }
    }

    public void updateStock(String medicineId, int quantityValue, boolean isSetMode, Callback<Void> callback) {
        if (isFirebaseAvailable) {
            DocumentReference docRef = db.collection("medicines").document(medicineId);
            db.runTransaction(transaction -> {
                DocumentSnapshot snapshot = transaction.get(docRef);
                if (!snapshot.exists()) {
                    throw new RuntimeException("Medicine not found");
                }
                Medicine m = snapshot.toObject(Medicine.class);
                if (m == null) throw new RuntimeException("Invalid medicine object");

                int newStock = isSetMode ? quantityValue : m.getStock() + quantityValue;
                if (newStock < 0) {
                    throw new RuntimeException("Stock cannot be negative");
                }
                transaction.update(docRef, "stock", newStock, "updatedAt", System.currentTimeMillis());
                return null;
            }).addOnSuccessListener(aVoid -> callback.onSuccess(null))
              .addOnFailureListener(e -> callback.onError(e.getMessage()));
        } else {
            Medicine m = localMedicines.get(medicineId);
            if (m == null) {
                callback.onError("Medicine not found");
                return;
            }
            int newStock = isSetMode ? quantityValue : m.getStock() + quantityValue;
            if (newStock < 0) {
                callback.onError("Stock cannot be negative");
                return;
            }
            m.setStock(newStock);
            m.setUpdatedAt(System.currentTimeMillis());
            callback.onSuccess(null);
        }
    }

    // CART
    public void getCart(String userId, Callback<List<CartItem>> callback) {
        if (isFirebaseAvailable) {
            db.collection("carts").document(userId).get().addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    List<Map<String, Object>> itemsRaw = (List<Map<String, Object>>) documentSnapshot.get("items");
                    List<CartItem> list = new ArrayList<>();
                    if (itemsRaw != null) {
                        for (Map<String, Object> map : itemsRaw) {
                            String medId = (String) map.get("medicineId");
                            String name = (String) map.get("medicineName");
                            double price = map.get("price") != null ? ((Number) map.get("price")).doubleValue() : 0.0;
                            int qty = map.get("quantity") != null ? ((Number) map.get("quantity")).intValue() : 1;
                            boolean rx = Boolean.TRUE.equals(map.get("requiresPrescription"));
                            list.add(new CartItem(medId, name, price, qty, rx));
                        }
                    }
                    callback.onSuccess(list);
                } else {
                    callback.onSuccess(new ArrayList<>());
                }
            }).addOnFailureListener(e -> callback.onError(e.getMessage()));
        } else {
            List<CartItem> items = localCarts.get(userId);
            callback.onSuccess(items != null ? new ArrayList<>(items) : new ArrayList<>());
        }
    }

    public void saveCart(String userId, List<CartItem> items, Callback<Void> callback) {
        if (isFirebaseAvailable) {
            Map<String, Object> cartMap = new HashMap<>();
            cartMap.put("items", items);
            cartMap.put("updatedAt", System.currentTimeMillis());

            db.collection("carts").document(userId).set(cartMap)
                    .addOnSuccessListener(aVoid -> callback.onSuccess(null))
                    .addOnFailureListener(e -> callback.onError(e.getMessage()));
        } else {
            localCarts.put(userId, new ArrayList<>(items));
            callback.onSuccess(null);
        }
    }

    // ORDERS & CHECKOUT
    public void placeOrderTransaction(String userId, String customerName, String customerPhone,
                                       String deliveryAddress, List<CartItem> cartItems, Callback<Order> callback) {
        if (cartItems == null || cartItems.isEmpty()) {
            callback.onError("Cart is empty");
            return;
        }

        String orderId = "ORD" + (System.currentTimeMillis() % 1000000);
        long now = System.currentTimeMillis();

        if (isFirebaseAvailable) {
            db.runTransaction(transaction -> {
                List<OrderItem> orderItems = new ArrayList<>();
                double subtotal = 0.0;

                for (CartItem ci : cartItems) {
                    DocumentReference medRef = db.collection("medicines").document(ci.getMedicineId());
                    DocumentSnapshot medSnap = transaction.get(medRef);

                    if (!medSnap.exists()) {
                        throw new RuntimeException("Medicine " + ci.getMedicineName() + " no longer exists.");
                    }

                    Medicine m = medSnap.toObject(Medicine.class);
                    if (m == null || !m.isActive()) {
                        throw new RuntimeException("Medicine " + ci.getMedicineName() + " is currently inactive.");
                    }

                    if (m.getStock() < ci.getQuantity()) {
                        throw new RuntimeException("Insufficient stock for " + m.getName() + ". Available: " + m.getStock());
                    }

                    // Deduct stock
                    int remainingStock = m.getStock() - ci.getQuantity();
                    transaction.update(medRef, "stock", remainingStock, "updatedAt", now);

                    double trustedUnitPrice = m.getPrice();
                    double itemTotal = trustedUnitPrice * ci.getQuantity();
                    subtotal += itemTotal;

                    orderItems.add(new OrderItem(m.getId(), m.getName(), trustedUnitPrice, ci.getQuantity()));
                }

                double deliveryFee = subtotal > 500.0 || subtotal == 0 ? 0.0 : 50.0;
                double total = subtotal + deliveryFee;

                Order newOrder = new Order(orderId, userId, customerName, customerPhone, deliveryAddress,
                        orderItems, subtotal, deliveryFee, total, "Pending", now, now, true);

                DocumentReference orderRef = db.collection("orders").document(orderId);
                transaction.set(orderRef, newOrder);

                // Clear cart
                DocumentReference cartRef = db.collection("carts").document(userId);
                transaction.delete(cartRef);

                return newOrder;
            }).addOnSuccessListener(callback::onSuccess)
              .addOnFailureListener(e -> callback.onError(e.getMessage()));
        } else {
            // Local atomic order processing
            List<OrderItem> orderItems = new ArrayList<>();
            double subtotal = 0.0;

            for (CartItem ci : cartItems) {
                Medicine m = localMedicines.get(ci.getMedicineId());
                if (m == null || !m.isActive()) {
                    callback.onError("Medicine " + ci.getMedicineName() + " is unavailable.");
                    return;
                }
                if (m.getStock() < ci.getQuantity()) {
                    callback.onError("Insufficient stock for " + m.getName() + ". Available: " + m.getStock());
                    return;
                }
            }

            // Deduct stock
            for (CartItem ci : cartItems) {
                Medicine m = localMedicines.get(ci.getMedicineId());
                m.setStock(m.getStock() - ci.getQuantity());
                m.setUpdatedAt(now);
                double itemTotal = m.getPrice() * ci.getQuantity();
                subtotal += itemTotal;

                orderItems.add(new OrderItem(m.getId(), m.getName(), m.getPrice(), ci.getQuantity()));
            }

            double deliveryFee = subtotal > 500.0 || subtotal == 0 ? 0.0 : 50.0;
            double total = subtotal + deliveryFee;

            Order newOrder = new Order(orderId, userId, customerName, customerPhone, deliveryAddress,
                    orderItems, subtotal, deliveryFee, total, "Pending", now, now, true);

            localOrders.put(orderId, newOrder);
            localCarts.remove(userId);

            callback.onSuccess(newOrder);
        }
    }

    public void getCustomerOrders(String userId, Callback<List<Order>> callback) {
        if (isFirebaseAvailable) {
            db.collection("orders")
                    .whereEqualTo("userId", userId)
                    .orderBy("createdAt", Query.Direction.DESCENDING)
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        List<Order> list = new ArrayList<>();
                        for (DocumentSnapshot doc : queryDocumentSnapshots) {
                            Order o = doc.toObject(Order.class);
                            if (o != null) list.add(o);
                        }
                        callback.onSuccess(list);
                    })
                    .addOnFailureListener(e -> callback.onError(e.getMessage()));
        } else {
            List<Order> list = new ArrayList<>();
            for (Order o : localOrders.values()) {
                if (userId.equals(o.getUserId())) {
                    list.add(o);
                }
            }
            callback.onSuccess(list);
        }
    }

    public void getAllOrders(Callback<List<Order>> callback) {
        if (isFirebaseAvailable) {
            db.collection("orders")
                    .orderBy("createdAt", Query.Direction.DESCENDING)
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        List<Order> list = new ArrayList<>();
                        for (DocumentSnapshot doc : queryDocumentSnapshots) {
                            Order o = doc.toObject(Order.class);
                            if (o != null) list.add(o);
                        }
                        callback.onSuccess(list);
                    })
                    .addOnFailureListener(e -> callback.onError(e.getMessage()));
        } else {
            callback.onSuccess(new ArrayList<>(localOrders.values()));
        }
    }

    public void updateOrderStatus(Order order, String newStatus, Callback<Void> callback) {
        if ("Cancelled".equalsIgnoreCase(newStatus) && order.isStockDeducted()) {
            cancelOrderAndRestoreStock(order, callback);
            return;
        }

        order.setStatus(newStatus);
        order.setUpdatedAt(System.currentTimeMillis());

        if (isFirebaseAvailable) {
            db.collection("orders").document(order.getOrderId())
                    .update("status", newStatus, "updatedAt", System.currentTimeMillis())
                    .addOnSuccessListener(aVoid -> callback.onSuccess(null))
                    .addOnFailureListener(e -> callback.onError(e.getMessage()));
        } else {
            localOrders.put(order.getOrderId(), order);
            callback.onSuccess(null);
        }
    }

    private void cancelOrderAndRestoreStock(Order order, Callback<Void> callback) {
        long now = System.currentTimeMillis();

        if (isFirebaseAvailable) {
            db.runTransaction(transaction -> {
                DocumentReference orderRef = db.collection("orders").document(order.getOrderId());
                DocumentSnapshot orderSnap = transaction.get(orderRef);

                if (!orderSnap.exists()) {
                    throw new RuntimeException("Order not found");
                }

                Order currentOrder = orderSnap.toObject(Order.class);
                if (currentOrder == null) throw new RuntimeException("Invalid order doc");

                if (!currentOrder.isStockDeducted()) {
                    // Already restored stock previously
                    transaction.update(orderRef, "status", "Cancelled", "updatedAt", now);
                    return null;
                }

                // Restore stock for all items
                if (currentOrder.getItems() != null) {
                    for (OrderItem item : currentOrder.getItems()) {
                        DocumentReference medRef = db.collection("medicines").document(item.getMedicineId());
                        DocumentSnapshot medSnap = transaction.get(medRef);
                        if (medSnap.exists()) {
                            Medicine m = medSnap.toObject(Medicine.class);
                            if (m != null) {
                                transaction.update(medRef, "stock", m.getStock() + item.getQuantity(), "updatedAt", now);
                            }
                        }
                    }
                }

                transaction.update(orderRef, "status", "Cancelled", "stockDeducted", false, "updatedAt", now);
                return null;
            }).addOnSuccessListener(aVoid -> {
                order.setStatus("Cancelled");
                order.setStockDeducted(false);
                callback.onSuccess(null);
            }).addOnFailureListener(e -> callback.onError(e.getMessage()));
        } else {
            if (order.isStockDeducted() && order.getItems() != null) {
                for (OrderItem item : order.getItems()) {
                    Medicine m = localMedicines.get(item.getMedicineId());
                    if (m != null) {
                        m.setStock(m.getStock() + item.getQuantity());
                        m.setUpdatedAt(now);
                    }
                }
                order.setStockDeducted(false);
            }
            order.setStatus("Cancelled");
            order.setUpdatedAt(now);
            localOrders.put(order.getOrderId(), order);
            callback.onSuccess(null);
        }
    }
}
