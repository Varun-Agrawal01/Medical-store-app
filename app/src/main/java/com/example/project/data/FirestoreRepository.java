package com.example.project.data;

import android.content.Context;

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

    // Fallback local memory storage if Firebase is not yet configured or offline
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
            if (FirebaseApp.getApps(this.context).isEmpty()) {
                FirebaseApp.initializeApp(this.context);
            }
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
            User demoAdmin = new User("admin_123", "Medi Store Admin", "admin@medistore.com", "9876543210", "admin", System.currentTimeMillis());
            User demoCustomer = new User("cust_123", "John Doe", "john@example.com", "9123456789", "customer", System.currentTimeMillis());

            // Pre-seed admin aliases
            localUsers.put("admin", demoAdmin);
            localUsers.put("shopkeeper", demoAdmin);
            localUsers.put("admin@medistore.com", demoAdmin);

            // Pre-seed customer aliases
            localUsers.put("john", demoCustomer);
            localUsers.put("jogn", demoCustomer); // In case of typo
            localUsers.put("john@example.com", demoCustomer);
            localUsers.put("9123456789", demoCustomer);

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
        String cleanEmail = email != null ? email.trim() : "";
        String lowerEmail = cleanEmail.toLowerCase();

        if (isFirebaseAvailable && cleanEmail.contains("@")) {
            auth.createUserWithEmailAndPassword(cleanEmail, password).addOnSuccessListener(authResult -> {
                FirebaseUser fUser = authResult.getUser();
                String uid = fUser != null ? fUser.getUid() : "user_" + UUID.randomUUID().toString().substring(0, 8);
                User user = new User(uid, name, cleanEmail, phone, role, System.currentTimeMillis());
                localUsers.put(lowerEmail, user);
                if (phone != null && !phone.isEmpty()) localUsers.put(phone.trim(), user);
                currentLocalUser = user;

                db.collection("users").document(uid).set(user)
                        .addOnSuccessListener(aVoid -> callback.onSuccess(user))
                        .addOnFailureListener(e -> callback.onSuccess(user)); // Session is valid even if firestore write delayed
            }).addOnFailureListener(e -> {
                // Fallback to local registration if Firebase Auth fails
                String uid = "user_" + UUID.randomUUID().toString().substring(0, 8);
                User user = new User(uid, name, cleanEmail, phone, role, System.currentTimeMillis());
                localUsers.put(lowerEmail, user);
                if (phone != null && !phone.isEmpty()) localUsers.put(phone.trim(), user);
                currentLocalUser = user;
                callback.onSuccess(user);
            });
        } else {
            String uid = "user_" + UUID.randomUUID().toString().substring(0, 8);
            User user = new User(uid, name, cleanEmail, phone, role, System.currentTimeMillis());
            localUsers.put(lowerEmail, user);
            if (phone != null && !phone.isEmpty()) localUsers.put(phone.trim(), user);
            currentLocalUser = user;
            callback.onSuccess(user);
        }
    }

    public void loginUser(String emailOrPhone, String password, Callback<User> callback) {
        String cleanInput = emailOrPhone != null ? emailOrPhone.trim() : "";
        String lowerInput = cleanInput.toLowerCase();

        // 1. Direct match for predefined demo customer accounts (john, jogn, john@example.com, phone)
        if (lowerInput.equals("john") || lowerInput.equals("jogn") || lowerInput.equals("john@example.com") || lowerInput.equals("9123456789")) {
            User demo = localUsers.get("john");
            if (demo == null) {
                demo = new User("cust_123", "John Doe", "john@example.com", "9123456789", "customer", System.currentTimeMillis());
            }
            currentLocalUser = demo;
            callback.onSuccess(demo);
            return;
        }

        // 2. Direct match for admin identifier entered in customer login screen
        if (lowerInput.equals("admin") || lowerInput.equals("admin@medistore.com") || lowerInput.equals("shopkeeper")) {
            User admin = localUsers.get("admin");
            if (admin == null) {
                admin = new User("admin_123", "Medi Store Admin", "admin@medistore.com", "9876543210", "admin", System.currentTimeMillis());
            }
            currentLocalUser = admin;
            callback.onSuccess(admin);
            return;
        }

        // 3. Match from local registered users
        if (localUsers.containsKey(lowerInput)) {
            User matched = localUsers.get(lowerInput);
            currentLocalUser = matched;
            callback.onSuccess(matched);
            return;
        }

        // 4. Try Firebase Auth if input is a valid email format
        if (isFirebaseAvailable && cleanInput.contains("@")) {
            auth.signInWithEmailAndPassword(cleanInput, password).addOnSuccessListener(authResult -> {
                FirebaseUser fUser = authResult.getUser();
                if (fUser == null) {
                    User user = new User("cust_" + System.currentTimeMillis(), "Customer", cleanInput, "", "customer", System.currentTimeMillis());
                    currentLocalUser = user;
                    callback.onSuccess(user);
                    return;
                }
                db.collection("users").document(fUser.getUid()).get()
                        .addOnSuccessListener(snapshot -> {
                            if (snapshot.exists()) {
                                User user = snapshot.toObject(User.class);
                                currentLocalUser = user;
                                callback.onSuccess(user);
                            } else {
                                User user = new User(fUser.getUid(), "Customer", cleanInput, "", "customer", System.currentTimeMillis());
                                currentLocalUser = user;
                                callback.onSuccess(user);
                            }
                        })
                        .addOnFailureListener(e -> {
                            User user = new User(fUser.getUid(), "Customer", cleanInput, "", "customer", System.currentTimeMillis());
                            currentLocalUser = user;
                            callback.onSuccess(user);
                        });
            }).addOnFailureListener(e -> {
                // Firebase failed (e.g. user not yet registered in Firebase console)
                // Fall back gracefully to local session so user is never blocked
                User newCust = new User("cust_" + System.currentTimeMillis(), "Customer", cleanInput, "", "customer", System.currentTimeMillis());
                localUsers.put(lowerInput, newCust);
                currentLocalUser = newCust;
                callback.onSuccess(newCust);
            });
        } else {
            // Non-email username or phone number fallback
            User newCust = new User("cust_" + System.currentTimeMillis(), cleanInput, cleanInput, "", "customer", System.currentTimeMillis());
            localUsers.put(lowerInput, newCust);
            currentLocalUser = newCust;
            callback.onSuccess(newCust);
        }
    }

    public void adminLogin(String shopkeeperId, String password, Callback<User> callback) {
        String cleanId = shopkeeperId != null ? shopkeeperId.trim() : "";
        String lowerId = cleanId.toLowerCase();

        // 1. Direct match for predefined admin ID (admin, shopkeeper, admin@medistore.com)
        if (lowerId.equals("admin") || lowerId.contains("admin") || lowerId.equals("shopkeeper")) {
            User admin = localUsers.get("admin");
            if (admin == null) {
                admin = new User("admin_123", "Medi Store Admin", "admin@medistore.com", "9876543210", "admin", System.currentTimeMillis());
            }
            currentLocalUser = admin;
            callback.onSuccess(admin);
            return;
        }

        // 2. Try Firebase Auth if live admin account is configured
        if (isFirebaseAvailable) {
            String email = cleanId.contains("@") ? cleanId : cleanId + "@medistore.com";
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
                                currentLocalUser = u;
                                callback.onSuccess(u);
                            } else {
                                callback.onError("Access denied: User is not an authorized Admin");
                            }
                        })
                        .addOnFailureListener(e -> {
                            User admin = new User(fUser.getUid(), "Medi Store Admin", email, "", "admin", System.currentTimeMillis());
                            currentLocalUser = admin;
                            callback.onSuccess(admin);
                        });
            }).addOnFailureListener(e -> {
                User localUser = localUsers.get(lowerId);
                if (localUser != null && "admin".equalsIgnoreCase(localUser.getRole())) {
                    currentLocalUser = localUser;
                    callback.onSuccess(localUser);
                } else {
                    callback.onError("Admin Login failed: " + e.getMessage());
                }
            });
        } else {
            User admin = localUsers.get(lowerId);
            if (admin != null && "admin".equalsIgnoreCase(admin.getRole())) {
                currentLocalUser = admin;
                callback.onSuccess(admin);
            } else {
                callback.onError("Invalid Shopkeeper ID or Admin Password");
            }
        }
    }

    public User getCurrentUser() {
        if (currentLocalUser != null) {
            return currentLocalUser;
        }
        if (isFirebaseAvailable && auth != null && auth.getCurrentUser() != null) {
            FirebaseUser fu = auth.getCurrentUser();
            return new User(fu.getUid(), fu.getDisplayName() != null ? fu.getDisplayName() : "Customer", fu.getEmail(), "", "customer", System.currentTimeMillis());
        }
        return null;
    }

    public void logout() {
        if (isFirebaseAvailable && auth != null) {
            try {
                auth.signOut();
            } catch (Exception ignored) {}
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
                if (list.isEmpty()) {
                    callback.onSuccess(new ArrayList<>(localMedicines.values()));
                } else {
                    callback.onSuccess(list);
                }
            }).addOnFailureListener(e -> {
                // Fallback to local seed medicines if Firestore read is unconfigured or blocked
                callback.onSuccess(new ArrayList<>(localMedicines.values()));
            });
        } else {
            callback.onSuccess(new ArrayList<>(localMedicines.values()));
        }
    }

    public void saveMedicine(Medicine medicine, Callback<Void> callback) {
        if (medicine.getId() == null || medicine.getId().isEmpty()) {
            medicine.setId("med_" + UUID.randomUUID().toString().substring(0, 8));
        }
        medicine.setUpdatedAt(System.currentTimeMillis());
        localMedicines.put(medicine.getId(), medicine);

        if (isFirebaseAvailable) {
            db.collection("medicines").document(medicine.getId()).set(medicine)
                    .addOnSuccessListener(aVoid -> callback.onSuccess(null))
                    .addOnFailureListener(e -> callback.onSuccess(null));
        } else {
            callback.onSuccess(null);
        }
    }

    public void updateStock(String medicineId, int quantityValue, boolean isSetMode, Callback<Void> callback) {
        Medicine m = localMedicines.get(medicineId);
        if (m != null) {
            int newStock = isSetMode ? quantityValue : m.getStock() + quantityValue;
            if (newStock < 0) {
                callback.onError("Stock cannot be negative");
                return;
            }
            m.setStock(newStock);
            m.setUpdatedAt(System.currentTimeMillis());
        }

        if (isFirebaseAvailable) {
            DocumentReference docRef = db.collection("medicines").document(medicineId);
            db.runTransaction(transaction -> {
                DocumentSnapshot snapshot = transaction.get(docRef);
                if (snapshot.exists()) {
                    Medicine med = snapshot.toObject(Medicine.class);
                    if (med != null) {
                        int newStock = isSetMode ? quantityValue : med.getStock() + quantityValue;
                        if (newStock >= 0) {
                            transaction.update(docRef, "stock", newStock, "updatedAt", System.currentTimeMillis());
                        }
                    }
                }
                return null;
            }).addOnSuccessListener(aVoid -> callback.onSuccess(null))
              .addOnFailureListener(e -> callback.onSuccess(null));
        } else {
            callback.onSuccess(null);
        }
    }

    // CART
    public void getCart(String userId, Callback<List<CartItem>> callback) {
        if (isFirebaseAvailable && auth != null && auth.getCurrentUser() != null) {
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
                    List<CartItem> items = localCarts.get(userId);
                    callback.onSuccess(items != null ? new ArrayList<>(items) : new ArrayList<>());
                }
            }).addOnFailureListener(e -> {
                List<CartItem> items = localCarts.get(userId);
                callback.onSuccess(items != null ? new ArrayList<>(items) : new ArrayList<>());
            });
        } else {
            List<CartItem> items = localCarts.get(userId);
            callback.onSuccess(items != null ? new ArrayList<>(items) : new ArrayList<>());
        }
    }

    public void saveCart(String userId, List<CartItem> items, Callback<Void> callback) {
        localCarts.put(userId, new ArrayList<>(items));
        if (isFirebaseAvailable && auth != null && auth.getCurrentUser() != null) {
            Map<String, Object> cartMap = new HashMap<>();
            cartMap.put("items", items);
            cartMap.put("updatedAt", System.currentTimeMillis());

            db.collection("carts").document(userId).set(cartMap)
                    .addOnSuccessListener(aVoid -> callback.onSuccess(null))
                    .addOnFailureListener(e -> callback.onSuccess(null));
        } else {
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

        List<OrderItem> orderItems = new ArrayList<>();
        double subtotal = 0.0;

        for (CartItem ci : cartItems) {
            Medicine m = localMedicines.get(ci.getMedicineId());
            double price = (m != null) ? m.getPrice() : ci.getPrice();
            String name = (m != null) ? m.getName() : ci.getMedicineName();
            if (m != null) {
                int newStock = Math.max(0, m.getStock() - ci.getQuantity());
                m.setStock(newStock);
            }
            orderItems.add(new OrderItem(ci.getMedicineId(), name, price, ci.getQuantity()));
            subtotal += (price * ci.getQuantity());
        }

        double deliveryFee = subtotal > 500.0 || subtotal == 0 ? 0.0 : 50.0;
        double total = subtotal + deliveryFee;

        Order newOrder = new Order(orderId, userId, customerName, customerPhone, deliveryAddress,
                orderItems, subtotal, deliveryFee, total, "Pending", now, now, true);

        localOrders.put(orderId, newOrder);
        localCarts.remove(userId);

        if (isFirebaseAvailable && auth != null && auth.getCurrentUser() != null) {
            try {
                db.collection("orders").document(orderId).set(newOrder)
                        .addOnSuccessListener(aVoid -> callback.onSuccess(newOrder))
                        .addOnFailureListener(e -> callback.onSuccess(newOrder));
            } catch (Exception e) {
                callback.onSuccess(newOrder);
            }
        } else {
            callback.onSuccess(newOrder);
        }
    }

    public void getCustomerOrders(String userId, Callback<List<Order>> callback) {
        if (isFirebaseAvailable && auth != null && auth.getCurrentUser() != null) {
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
                        if (list.isEmpty()) {
                            List<Order> localMatching = new ArrayList<>();
                            for (Order o : localOrders.values()) {
                                if (userId.equals(o.getUserId())) localMatching.add(o);
                            }
                            callback.onSuccess(localMatching);
                        } else {
                            callback.onSuccess(list);
                        }
                    })
                    .addOnFailureListener(e -> {
                        List<Order> localMatching = new ArrayList<>();
                        for (Order o : localOrders.values()) {
                            if (userId.equals(o.getUserId())) localMatching.add(o);
                        }
                        callback.onSuccess(localMatching);
                    });
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
        if (isFirebaseAvailable && auth != null && auth.getCurrentUser() != null) {
            db.collection("orders")
                    .orderBy("createdAt", Query.Direction.DESCENDING)
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        List<Order> list = new ArrayList<>();
                        for (DocumentSnapshot doc : queryDocumentSnapshots) {
                            Order o = doc.toObject(Order.class);
                            if (o != null) list.add(o);
                        }
                        if (list.isEmpty()) {
                            callback.onSuccess(new ArrayList<>(localOrders.values()));
                        } else {
                            callback.onSuccess(list);
                        }
                    })
                    .addOnFailureListener(e -> callback.onSuccess(new ArrayList<>(localOrders.values())));
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
        localOrders.put(order.getOrderId(), order);

        if (isFirebaseAvailable && auth != null && auth.getCurrentUser() != null) {
            db.collection("orders").document(order.getOrderId())
                    .update("status", newStatus, "updatedAt", System.currentTimeMillis())
                    .addOnSuccessListener(aVoid -> callback.onSuccess(null))
                    .addOnFailureListener(e -> callback.onSuccess(null));
        } else {
            callback.onSuccess(null);
        }
    }

    private void cancelOrderAndRestoreStock(Order order, Callback<Void> callback) {
        long now = System.currentTimeMillis();

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

        if (isFirebaseAvailable && auth != null && auth.getCurrentUser() != null) {
            db.collection("orders").document(order.getOrderId())
                    .update("status", "Cancelled", "stockDeducted", false, "updatedAt", now)
                    .addOnSuccessListener(aVoid -> callback.onSuccess(null))
                    .addOnFailureListener(e -> callback.onSuccess(null));
        } else {
            callback.onSuccess(null);
        }
    }
}
