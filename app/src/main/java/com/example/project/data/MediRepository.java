package com.example.project.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.os.Handler;
import android.os.Looper;

import com.example.project.models.CartItem;
import com.example.project.models.Medicine;
import com.example.project.models.Order;
import com.example.project.models.OrderItem;
import com.example.project.models.Symptom;
import com.example.project.models.User;
import com.example.project.utils.PasswordHasher;
import com.example.project.utils.PricingUtils;
import com.example.project.utils.SessionManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Singleton repository for local SQLite database operations.
 * All SQLite operations run on a single background thread and deliver results to the main thread.
 * All multi-table writes are wrapped in SQLite transactions.
 * All queries are strictly parameterized.
 */
public class MediRepository {

    private static MediRepository instance;
    private final Context context;
    private final DBHelper dbHelper;
    private final SessionManager sessionManager;
    private final ExecutorService executor;
    private final Handler mainHandler;

    public interface Callback<T> {
        void onSuccess(T result);
        void onError(String error);
    }

    private MediRepository(Context context) {
        this.context = context.getApplicationContext();
        this.dbHelper = new DBHelper(this.context);
        this.sessionManager = new SessionManager(this.context);
        this.executor = Executors.newSingleThreadExecutor();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public static synchronized MediRepository getInstance(Context context) {
        if (instance == null) {
            instance = new MediRepository(context);
        }
        return instance;
    }

    private <T> void postSuccess(Callback<T> callback, T result) {
        if (callback != null) {
            mainHandler.post(() -> callback.onSuccess(result));
        }
    }

    private <T> void postError(Callback<T> callback, String error) {
        if (callback != null) {
            mainHandler.post(() -> callback.onError(error));
        }
    }

    // ==========================================
    // AUTHENTICATION & SESSION
    // ==========================================

    public void registerUser(String name, String email, String phone, String password, String role, Callback<User> callback) {
        executor.execute(() -> {
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            String cleanEmail = email.trim().toLowerCase();

            // Check if user already exists
            try (Cursor cursor = db.rawQuery("SELECT 1 FROM users WHERE email = ?", new String[]{cleanEmail})) {
                if (cursor != null && cursor.moveToFirst()) {
                    postError(callback, "An account with this email already exists");
                    return;
                }
            } catch (Exception e) {
                postError(callback, "Database query error: " + e.getMessage());
                return;
            }

            String userId = "usr_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
            String hash = PasswordHasher.hash(password.toCharArray());
            long now = System.currentTimeMillis();

            ContentValues values = new ContentValues();
            values.put("id", userId);
            values.put("name", name.trim());
            values.put("email", cleanEmail);
            values.put("phone", phone.trim());
            values.put("password_hash", hash);
            values.put("salt", "");
            values.put("role", "customer"); // Always customer from registration
            values.put("created_at", now);

            long rowId = db.insert("users", null, values);
            if (rowId != -1) {
                User user = new User(userId, name.trim(), cleanEmail, phone.trim(), "customer", now);
                sessionManager.saveUserId(userId);
                postSuccess(callback, user);
            } else {
                postError(callback, "Failed to register user account");
            }
        });
    }

    public void loginUser(String email, String password, Callback<User> callback) {
        executor.execute(() -> {
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            String cleanEmail = email.trim().toLowerCase();

            try (Cursor cursor = db.rawQuery(
                    "SELECT id, name, email, phone, password_hash, role, created_at FROM users WHERE email = ?",
                    new String[]{cleanEmail})) {

                if (cursor != null && cursor.moveToFirst()) {
                    String storedHash = cursor.getString(cursor.getColumnIndexOrThrow("password_hash"));
                    if (PasswordHasher.verify(password.toCharArray(), storedHash)) {
                        String id = cursor.getString(cursor.getColumnIndexOrThrow("id"));
                        String name = cursor.getString(cursor.getColumnIndexOrThrow("name"));
                        String phone = cursor.getString(cursor.getColumnIndexOrThrow("phone"));
                        String role = cursor.getString(cursor.getColumnIndexOrThrow("role"));
                        long createdAt = cursor.getLong(cursor.getColumnIndexOrThrow("created_at"));

                        User user = new User(id, name, cleanEmail, phone, role, createdAt);
                        sessionManager.saveUserId(id);
                        postSuccess(callback, user);
                        return;
                    }
                }
                postError(callback, "Invalid email or password");
            } catch (Exception e) {
                postError(callback, "Login error: " + e.getMessage());
            }
        });
    }

    public void adminLogin(String shopkeeperId, String password, Callback<User> callback) {
        executor.execute(() -> {
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            String identifier = shopkeeperId.trim().toLowerCase();

            try (Cursor cursor = db.rawQuery(
                    "SELECT id, name, email, phone, password_hash, role, created_at FROM users WHERE email = ? OR id = ?",
                    new String[]{identifier, identifier})) {

                if (cursor != null && cursor.moveToFirst()) {
                    String role = cursor.getString(cursor.getColumnIndexOrThrow("role"));
                    if (!"admin".equalsIgnoreCase(role)) {
                        postError(callback, "Access denied: Account is not an administrator");
                        return;
                    }

                    String storedHash = cursor.getString(cursor.getColumnIndexOrThrow("password_hash"));
                    if (PasswordHasher.verify(password.toCharArray(), storedHash)) {
                        String id = cursor.getString(cursor.getColumnIndexOrThrow("id"));
                        String name = cursor.getString(cursor.getColumnIndexOrThrow("name"));
                        String email = cursor.getString(cursor.getColumnIndexOrThrow("email"));
                        String phone = cursor.getString(cursor.getColumnIndexOrThrow("phone"));
                        long createdAt = cursor.getLong(cursor.getColumnIndexOrThrow("created_at"));

                        User admin = new User(id, name, email, phone, role, createdAt);
                        sessionManager.saveUserId(id);
                        postSuccess(callback, admin);
                        return;
                    }
                }
                postError(callback, "Invalid Shopkeeper ID or Admin Password");
            } catch (Exception e) {
                postError(callback, "Admin login error: " + e.getMessage());
            }
        });
    }

    public void getUserById(String userId, Callback<User> callback) {
        executor.execute(() -> {
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            try (Cursor cursor = db.rawQuery(
                    "SELECT id, name, email, phone, role, created_at FROM users WHERE id = ?",
                    new String[]{userId})) {

                if (cursor != null && cursor.moveToFirst()) {
                    String id = cursor.getString(cursor.getColumnIndexOrThrow("id"));
                    String name = cursor.getString(cursor.getColumnIndexOrThrow("name"));
                    String email = cursor.getString(cursor.getColumnIndexOrThrow("email"));
                    String phone = cursor.getString(cursor.getColumnIndexOrThrow("phone"));
                    String role = cursor.getString(cursor.getColumnIndexOrThrow("role"));
                    long createdAt = cursor.getLong(cursor.getColumnIndexOrThrow("created_at"));

                    User user = new User(id, name, email, phone, role, createdAt);
                    postSuccess(callback, user);
                } else {
                    postError(callback, "User not found");
                }
            } catch (Exception e) {
                postError(callback, "Database error: " + e.getMessage());
            }
        });
    }

    /**
     * Synchronously returns cached user or loads from DB based on SessionManager.
     */
    public User getCurrentUser() {
        String userId = sessionManager.getUserId();
        if (userId == null) {
            return null;
        }
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        try (Cursor cursor = db.rawQuery(
                "SELECT id, name, email, phone, role, created_at FROM users WHERE id = ?",
                new String[]{userId})) {

            if (cursor != null && cursor.moveToFirst()) {
                String id = cursor.getString(cursor.getColumnIndexOrThrow("id"));
                String name = cursor.getString(cursor.getColumnIndexOrThrow("name"));
                String email = cursor.getString(cursor.getColumnIndexOrThrow("email"));
                String phone = cursor.getString(cursor.getColumnIndexOrThrow("phone"));
                String role = cursor.getString(cursor.getColumnIndexOrThrow("role"));
                long createdAt = cursor.getLong(cursor.getColumnIndexOrThrow("created_at"));
                return new User(id, name, email, phone, role, createdAt);
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    public void updateProfile(String userId, String newName, String newPhone, Callback<Void> callback) {
        executor.execute(() -> {
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            ContentValues cv = new ContentValues();
            cv.put("name", newName.trim());
            cv.put("phone", newPhone.trim());

            int updated = db.update("users", cv, "id = ?", new String[]{userId});
            if (updated > 0) {
                postSuccess(callback, null);
            } else {
                postError(callback, "Failed to update profile");
            }
        });
    }

    public void changePassword(String userId, String currentPassword, String newPassword, Callback<Void> callback) {
        executor.execute(() -> {
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            try (Cursor cursor = db.rawQuery("SELECT password_hash FROM users WHERE id = ?", new String[]{userId})) {
                if (cursor != null && cursor.moveToFirst()) {
                    String storedHash = cursor.getString(cursor.getColumnIndexOrThrow("password_hash"));
                    if (!PasswordHasher.verify(currentPassword.toCharArray(), storedHash)) {
                        postError(callback, "Incorrect current password");
                        return;
                    }
                    String newHash = PasswordHasher.hash(newPassword.toCharArray());
                    ContentValues cv = new ContentValues();
                    cv.put("password_hash", newHash);
                    db.update("users", cv, "id = ?", new String[]{userId});
                    postSuccess(callback, null);
                } else {
                    postError(callback, "User record not found");
                }
            } catch (Exception e) {
                postError(callback, "Failed to change password: " + e.getMessage());
            }
        });
    }

    public void logout() {
        sessionManager.clear();
    }

    // ==========================================
    // MEDICINES & INVENTORY
    // ==========================================

    public void getMedicines(Callback<List<Medicine>> callback) {
        executor.execute(() -> {
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            List<Medicine> list = new ArrayList<>();
            try (Cursor cursor = db.rawQuery(
                    "SELECT * FROM medicines ORDER BY name ASC", null)) {

                while (cursor != null && cursor.moveToNext()) {
                    list.add(cursorToMedicine(cursor));
                }
                postSuccess(callback, list);
            } catch (Exception e) {
                postError(callback, "Error fetching medicines: " + e.getMessage());
            }
        });
    }

    public void getMedicinesPaged(int limit, int offset, String categoryFilter, Callback<List<Medicine>> callback) {
        executor.execute(() -> {
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            List<Medicine> list = new ArrayList<>();
            String sql;
            String[] args;

            if (categoryFilter != null && !categoryFilter.isEmpty()) {
                sql = "SELECT * FROM medicines WHERE category_id = ? ORDER BY name ASC LIMIT ? OFFSET ?";
                args = new String[]{categoryFilter, String.valueOf(limit), String.valueOf(offset)};
            } else {
                sql = "SELECT * FROM medicines ORDER BY name ASC LIMIT ? OFFSET ?";
                args = new String[]{String.valueOf(limit), String.valueOf(offset)};
            }

            try (Cursor cursor = db.rawQuery(sql, args)) {
                while (cursor != null && cursor.moveToNext()) {
                    list.add(cursorToMedicine(cursor));
                }
                postSuccess(callback, list);
            } catch (Exception e) {
                postError(callback, "Pagination error: " + e.getMessage());
            }
        });
    }

    public void searchMedicines(String query, int limit, int offset, Callback<List<Medicine>> callback) {
        executor.execute(() -> {
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            List<Medicine> list = new ArrayList<>();
            String trimmed = query != null ? query.trim() : "";

            if (trimmed.isEmpty()) {
                getMedicinesPaged(limit, offset, null, callback);
                return;
            }

            // Prefix match using FTS4
            String ftsQuery = trimmed + "*";
            String ftsSql = "SELECT m.* FROM medicines m " +
                    "JOIN medicines_fts fts ON m.id = fts.id " +
                    "WHERE medicines_fts MATCH ? " +
                    "ORDER BY m.name ASC LIMIT ? OFFSET ?";

            try (Cursor cursor = db.rawQuery(ftsSql, new String[]{ftsQuery, String.valueOf(limit), String.valueOf(offset)})) {
                while (cursor != null && cursor.moveToNext()) {
                    list.add(cursorToMedicine(cursor));
                }
                postSuccess(callback, list);
            } catch (Exception ftsException) {
                // Fallback to LIKE if FTS expression is unusual
                String likePattern = "%" + trimmed + "%";
                String fallbackSql = "SELECT * FROM medicines WHERE name LIKE ? OR generic_name LIKE ? OR manufacturer LIKE ? " +
                        "ORDER BY name ASC LIMIT ? OFFSET ?";
                try (Cursor fallbackCursor = db.rawQuery(fallbackSql, new String[]{likePattern, likePattern, likePattern, String.valueOf(limit), String.valueOf(offset)})) {
                    while (fallbackCursor != null && fallbackCursor.moveToNext()) {
                        list.add(cursorToMedicine(fallbackCursor));
                    }
                    postSuccess(callback, list);
                } catch (Exception e) {
                    postError(callback, "Search error: " + e.getMessage());
                }
            }
        });
    }

    public void getMedicineById(String medicineId, Callback<Medicine> callback) {
        executor.execute(() -> {
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            try (Cursor cursor = db.rawQuery("SELECT * FROM medicines WHERE id = ?", new String[]{medicineId})) {
                if (cursor != null && cursor.moveToFirst()) {
                    Medicine m = cursorToMedicine(cursor);
                    postSuccess(callback, m);
                } else {
                    postError(callback, "Medicine not found");
                }
            } catch (Exception e) {
                postError(callback, "Query error: " + e.getMessage());
            }
        });
    }

    public void saveMedicine(Medicine medicine, Callback<Void> callback) {
        executor.execute(() -> {
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            if (medicine.getId() == null || medicine.getId().isEmpty()) {
                medicine.setId("med_" + UUID.randomUUID().toString().replace("-", "").substring(0, 10));
            }
            long now = System.currentTimeMillis();
            medicine.setUpdatedAt(now);

            ContentValues cv = new ContentValues();
            cv.put("id", medicine.getId());
            cv.put("name", medicine.getName());
            cv.put("generic_name", medicine.getGenericName());
            cv.put("manufacturer", medicine.getManufacturer() != null ? medicine.getManufacturer() : "");
            cv.put("strength", medicine.getStrength() != null ? medicine.getStrength() : "");
            cv.put("pack_size", medicine.getPackSize() != null ? medicine.getPackSize() : "");
            cv.put("dosage_form", medicine.getDosageForm() != null ? medicine.getDosageForm() : "Tablet");
            cv.put("description", medicine.getDescription());
            cv.put("usage_info", medicine.getUsageInfo() != null ? medicine.getUsageInfo() : "");
            cv.put("side_effects", medicine.getSideEffects() != null ? medicine.getSideEffects() : "");
            cv.put("mrp", medicine.getMrp() > 0 ? medicine.getMrp() : medicine.getPrice());
            cv.put("price", medicine.getPrice());
            cv.put("gst_rate", medicine.getGstRate() > 0 ? medicine.getGstRate() : 12.0);
            cv.put("stock", medicine.getStock());
            cv.put("requires_rx", medicine.isRequiresPrescription() ? 1 : 0);
            cv.put("image_path", medicine.getImageUrl() != null ? medicine.getImageUrl() : "");
            cv.put("category_id", medicine.getCategory() != null ? medicine.getCategory() : "cat_analgesics");
            cv.put("active", medicine.isActive() ? 1 : 0);
            cv.put("min_age", medicine.getMinAge());
            cv.put("avoid_in_pregnancy", medicine.getAvoidInPregnancy());
            cv.put("created_at", medicine.getCreatedAt() > 0 ? medicine.getCreatedAt() : now);
            cv.put("updated_at", now);

            long rowId = db.insertWithOnConflict("medicines", null, cv, SQLiteDatabase.CONFLICT_REPLACE);
            if (rowId != -1) {
                postSuccess(callback, null);
            } else {
                postError(callback, "Failed to save medicine");
            }
        });
    }

    public void updateStock(String medicineId, int quantityValue, boolean isSetMode, Callback<Void> callback) {
        executor.execute(() -> {
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            db.beginTransaction();
            try {
                int currentStock = 0;
                try (Cursor cursor = db.rawQuery("SELECT stock FROM medicines WHERE id = ?", new String[]{medicineId})) {
                    if (cursor != null && cursor.moveToFirst()) {
                        currentStock = cursor.getInt(cursor.getColumnIndexOrThrow("stock"));
                    } else {
                        throw new RuntimeException("Medicine not found");
                    }
                }

                int newStock = isSetMode ? quantityValue : (currentStock + quantityValue);
                if (newStock < 0) {
                    throw new RuntimeException("Stock cannot be negative");
                }

                ContentValues cv = new ContentValues();
                cv.put("stock", newStock);
                cv.put("updated_at", System.currentTimeMillis());

                db.update("medicines", cv, "id = ?", new String[]{medicineId});
                db.setTransactionSuccessful();
                postSuccess(callback, null);
            } catch (Exception e) {
                postError(callback, e.getMessage());
            } finally {
                db.endTransaction();
            }
        });
    }

    // ==========================================
    // CART OPERATIONS
    // ==========================================

    public void getCart(String userId, Callback<List<CartItem>> callback) {
        executor.execute(() -> {
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            List<CartItem> list = new ArrayList<>();
            String sql = "SELECT c.medicine_id, c.quantity, m.name, m.price, m.requires_rx " +
                    "FROM cart_items c " +
                    "JOIN medicines m ON c.medicine_id = m.id " +
                    "WHERE c.user_id = ?";

            try (Cursor cursor = db.rawQuery(sql, new String[]{userId})) {
                while (cursor != null && cursor.moveToNext()) {
                    String medId = cursor.getString(0);
                    int qty = cursor.getInt(1);
                    String medName = cursor.getString(2);
                    double price = cursor.getDouble(3);
                    boolean rx = cursor.getInt(4) == 1;
                    list.add(new CartItem(medId, medName, price, qty, rx));
                }
                postSuccess(callback, list);
            } catch (Exception e) {
                postError(callback, "Failed to load cart: " + e.getMessage());
            }
        });
    }

    public void addToCart(String userId, CartItem item, Callback<Void> callback) {
        executor.execute(() -> {
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            db.beginTransaction();
            try {
                int existingQty = 0;
                try (Cursor cursor = db.rawQuery(
                        "SELECT quantity FROM cart_items WHERE user_id = ? AND medicine_id = ?",
                        new String[]{userId, item.getMedicineId()})) {
                    if (cursor != null && cursor.moveToFirst()) {
                        existingQty = cursor.getInt(0);
                    }
                }

                if (existingQty > 0) {
                    ContentValues cv = new ContentValues();
                    cv.put("quantity", existingQty + item.getQuantity());
                    db.update("cart_items", cv, "user_id = ? AND medicine_id = ?",
                            new String[]{userId, item.getMedicineId()});
                } else {
                    ContentValues cv = new ContentValues();
                    cv.put("id", UUID.randomUUID().toString());
                    cv.put("user_id", userId);
                    cv.put("medicine_id", item.getMedicineId());
                    cv.put("quantity", item.getQuantity());
                    db.insert("cart_items", null, cv);
                }
                db.setTransactionSuccessful();
                postSuccess(callback, null);
            } catch (Exception e) {
                postError(callback, "Failed to add to cart: " + e.getMessage());
            } finally {
                db.endTransaction();
            }
        });
    }

    public void saveCart(String userId, List<CartItem> items, Callback<Void> callback) {
        executor.execute(() -> {
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            db.beginTransaction();
            try {
                db.delete("cart_items", "user_id = ?", new String[]{userId});
                String insertSql = "INSERT INTO cart_items(id, user_id, medicine_id, quantity) VALUES(?, ?, ?, ?);";
                try (SQLiteStatement stmt = db.compileStatement(insertSql)) {
                    for (CartItem ci : items) {
                        stmt.clearBindings();
                        stmt.bindString(1, UUID.randomUUID().toString());
                        stmt.bindString(2, userId);
                        stmt.bindString(3, ci.getMedicineId());
                        stmt.bindLong(4, ci.getQuantity());
                        stmt.executeInsert();
                    }
                }
                db.setTransactionSuccessful();
                postSuccess(callback, null);
            } catch (Exception e) {
                postError(callback, "Error saving cart: " + e.getMessage());
            } finally {
                db.endTransaction();
            }
        });
    }

    // ==========================================
    // ORDERS & TRANSACTIONAL CHECKOUT
    // ==========================================

    public void placeOrderTransaction(String userId, String customerName, String customerPhone,
                                      String deliveryAddress, List<CartItem> cartItems, Callback<Order> callback) {
        executor.execute(() -> {
            if (cartItems == null || cartItems.isEmpty()) {
                postError(callback, "Cart is empty");
                return;
            }

            SQLiteDatabase db = dbHelper.getWritableDatabase();
            db.beginTransaction();
            try {
                List<OrderItem> orderItems = new ArrayList<>();
                double subtotal = 0.0;
                double gstTotal = 0.0;
                boolean rxRequired = false;
                long now = System.currentTimeMillis();

                // 1. Validate and deduct stock for each medicine
                for (CartItem ci : cartItems) {
                    try (Cursor cursor = db.rawQuery(
                            "SELECT name, price, stock, active, requires_rx, gst_rate FROM medicines WHERE id = ?",
                            new String[]{ci.getMedicineId()})) {

                        if (cursor == null || !cursor.moveToFirst()) {
                            throw new RuntimeException("Medicine " + ci.getMedicineName() + " is no longer available.");
                        }

                        String name = cursor.getString(0);
                        double trustedPrice = cursor.getDouble(1);
                        int stock = cursor.getInt(2);
                        int active = cursor.getInt(3);
                        int rx = cursor.getInt(4);
                        double gstRate = cursor.getDouble(5);

                        if (active == 0) {
                            throw new RuntimeException("Medicine " + name + " is currently discontinued.");
                        }
                        if (stock < ci.getQuantity()) {
                            throw new RuntimeException("Insufficient stock for " + name + " (Only " + stock + " left).");
                        }

                        if (rx == 1) {
                            rxRequired = true;
                        }

                        // Deduct stock
                        ContentValues stockCv = new ContentValues();
                        stockCv.put("stock", stock - ci.getQuantity());
                        stockCv.put("updated_at", now);
                        db.update("medicines", stockCv, "id = ?", new String[]{ci.getMedicineId()});

                        double itemTotal = trustedPrice * ci.getQuantity();
                        subtotal += itemTotal;
                        gstTotal += PricingUtils.calculateGstAmount(itemTotal, gstRate);

                        orderItems.add(new OrderItem(ci.getMedicineId(), name, trustedPrice, ci.getQuantity()));
                    }
                }

                double deliveryFee = PricingUtils.calculateDeliveryFee(subtotal);
                double total = PricingUtils.calculateTotal(subtotal, deliveryFee);
                String orderId = "ORD" + (now % 1000000) + (new Random().nextInt(90) + 10);

                // 2. Insert into orders table
                ContentValues orderCv = new ContentValues();
                orderCv.put("id", orderId);
                orderCv.put("order_no", orderId);
                orderCv.put("user_id", userId);
                orderCv.put("customer_name", customerName);
                orderCv.put("phone", customerPhone);
                orderCv.put("address_text", deliveryAddress);
                orderCv.put("subtotal", subtotal);
                orderCv.put("delivery_fee", deliveryFee);
                orderCv.put("gst_total", gstTotal);
                orderCv.put("total", total);
                orderCv.put("payment_method", "Cash on Delivery");
                orderCv.put("payment_status", "Pending");
                orderCv.put("status", "Pending");
                orderCv.put("rx_required", rxRequired ? 1 : 0);
                orderCv.put("created_at", now);
                orderCv.put("updated_at", now);

                long insertResult = db.insert("orders", null, orderCv);
                if (insertResult == -1) {
                    throw new RuntimeException("Failed to generate order record");
                }

                // 3. Insert order_items
                String itemSql = "INSERT INTO order_items(id, order_id, medicine_id, name, unit_price, gst_rate, quantity) VALUES(?, ?, ?, ?, ?, ?, ?);";
                try (SQLiteStatement itemStmt = db.compileStatement(itemSql)) {
                    for (OrderItem oi : orderItems) {
                        itemStmt.clearBindings();
                        itemStmt.bindString(1, UUID.randomUUID().toString());
                        itemStmt.bindString(2, orderId);
                        itemStmt.bindString(3, oi.getMedicineId());
                        itemStmt.bindString(4, oi.getMedicineName());
                        itemStmt.bindDouble(5, oi.getUnitPrice());
                        itemStmt.bindDouble(6, 12.0);
                        itemStmt.bindLong(7, oi.getQuantity());
                        itemStmt.executeInsert();
                    }
                }

                // 4. Insert first row into order_status_history
                ContentValues historyCv = new ContentValues();
                historyCv.put("id", UUID.randomUUID().toString());
                historyCv.put("order_id", orderId);
                historyCv.put("status", "Pending");
                historyCv.put("note", "Order placed successfully by customer.");
                historyCv.put("changed_at", now);
                db.insert("order_status_history", null, historyCv);

                // 5. Clear cart
                db.delete("cart_items", "user_id = ?", new String[]{userId});

                db.setTransactionSuccessful();

                Order createdOrder = new Order(orderId, userId, customerName, customerPhone, deliveryAddress,
                        orderItems, subtotal, deliveryFee, total, "Pending", now, now, true);

                postSuccess(callback, createdOrder);
            } catch (Exception e) {
                postError(callback, e.getMessage());
            } finally {
                db.endTransaction();
            }
        });
    }

    public void getCustomerOrders(String userId, Callback<List<Order>> callback) {
        executor.execute(() -> {
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            List<Order> list = new ArrayList<>();
            try (Cursor cursor = db.rawQuery(
                    "SELECT * FROM orders WHERE user_id = ? ORDER BY created_at DESC",
                    new String[]{userId})) {

                while (cursor != null && cursor.moveToNext()) {
                    Order order = cursorToOrder(cursor);
                    loadOrderItems(db, order);
                    list.add(order);
                }
                postSuccess(callback, list);
            } catch (Exception e) {
                postError(callback, "Error fetching orders: " + e.getMessage());
            }
        });
    }

    public void getAllOrders(Callback<List<Order>> callback) {
        executor.execute(() -> {
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            List<Order> list = new ArrayList<>();
            try (Cursor cursor = db.rawQuery(
                    "SELECT * FROM orders ORDER BY created_at DESC", null)) {

                while (cursor != null && cursor.moveToNext()) {
                    Order order = cursorToOrder(cursor);
                    loadOrderItems(db, order);
                    list.add(order);
                }
                postSuccess(callback, list);
            } catch (Exception e) {
                postError(callback, "Error fetching all orders: " + e.getMessage());
            }
        });
    }

    public void getOrderById(String orderId, Callback<Order> callback) {
        executor.execute(() -> {
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            try (Cursor cursor = db.rawQuery("SELECT * FROM orders WHERE id = ? OR order_no = ?",
                    new String[]{orderId, orderId})) {

                if (cursor != null && cursor.moveToFirst()) {
                    Order order = cursorToOrder(cursor);
                    loadOrderItems(db, order);
                    postSuccess(callback, order);
                } else {
                    postError(callback, "Order not found");
                }
            } catch (Exception e) {
                postError(callback, "Query error: " + e.getMessage());
            }
        });
    }

    private void loadOrderItems(SQLiteDatabase db, Order order) {
        List<OrderItem> items = new ArrayList<>();
        try (Cursor cursor = db.rawQuery(
                "SELECT medicine_id, name, unit_price, quantity FROM order_items WHERE order_id = ?",
                new String[]{order.getOrderId()})) {

            while (cursor != null && cursor.moveToNext()) {
                String mid = cursor.getString(0);
                String mname = cursor.getString(1);
                double price = cursor.getDouble(2);
                int qty = cursor.getInt(3);
                items.add(new OrderItem(mid, mname, price, qty));
            }
        }
        order.setItems(items);
    }

    public void updateOrderStatus(Order order, String newStatus, Callback<Void> callback) {
        executor.execute(() -> {
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            db.beginTransaction();
            try {
                long now = System.currentTimeMillis();

                // If status is Cancelled and stock was deducted, restore stock in medicines table
                if ("Cancelled".equalsIgnoreCase(newStatus) && order.isStockDeducted()) {
                    // Fetch items for this order
                    try (Cursor cursor = db.rawQuery(
                            "SELECT medicine_id, quantity FROM order_items WHERE order_id = ?",
                            new String[]{order.getOrderId()})) {

                        while (cursor != null && cursor.moveToNext()) {
                            String medId = cursor.getString(0);
                            int qty = cursor.getInt(1);

                            db.execSQL("UPDATE medicines SET stock = stock + ?, updated_at = ? WHERE id = ?",
                                    new Object[]{qty, now, medId});
                        }
                    }
                    order.setStockDeducted(false);
                }

                ContentValues orderCv = new ContentValues();
                orderCv.put("status", newStatus);
                orderCv.put("updated_at", now);
                db.update("orders", orderCv, "id = ?", new String[]{order.getOrderId()});

                // Status history tracking
                ContentValues historyCv = new ContentValues();
                historyCv.put("id", UUID.randomUUID().toString());
                historyCv.put("order_id", order.getOrderId());
                historyCv.put("status", newStatus);
                historyCv.put("note", "Status changed to " + newStatus);
                historyCv.put("changed_at", now);
                db.insert("order_status_history", null, historyCv);

                db.setTransactionSuccessful();
                order.setStatus(newStatus);
                order.setUpdatedAt(now);
                postSuccess(callback, null);
            } catch (Exception e) {
                postError(callback, "Error updating status: " + e.getMessage());
            } finally {
                db.endTransaction();
            }
        });
    }

    // ==========================================
    // SYMPTOMS & SYMPTOM CHECKER V2
    // ==========================================

    public void getSymptoms(Callback<List<Symptom>> callback) {
        executor.execute(() -> {
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            List<Symptom> list = new ArrayList<>();
            try (Cursor cursor = db.rawQuery("SELECT id, name, red_flag_advice FROM symptoms ORDER BY name ASC", null)) {
                while (cursor != null && cursor.moveToNext()) {
                    String id = cursor.getString(0);
                    String name = cursor.getString(1);
                    String advice = cursor.getString(2);
                    list.add(new Symptom(id, name, advice));
                }
                postSuccess(callback, list);
            } catch (Exception e) {
                postError(callback, "Failed to load symptoms: " + e.getMessage());
            }
        });
    }

    public void getPopularSymptoms(int limit, Callback<List<Symptom>> callback) {
        executor.execute(() -> {
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            List<Symptom> list = new ArrayList<>();
            // Popular symptoms: highest count of linked medicines
            String sql = "SELECT s.id, s.name, s.red_flag_advice, COUNT(ms.medicine_id) AS med_count " +
                    "FROM symptoms s " +
                    "LEFT JOIN medicine_symptoms ms ON s.id = ms.symptom_id " +
                    "GROUP BY s.id " +
                    "ORDER BY med_count DESC LIMIT ?";

            try (Cursor cursor = db.rawQuery(sql, new String[]{String.valueOf(limit)})) {
                while (cursor != null && cursor.moveToNext()) {
                    list.add(new Symptom(cursor.getString(0), cursor.getString(1), cursor.getString(2)));
                }
                postSuccess(callback, list);
            } catch (Exception e) {
                postError(callback, "Failed to load popular symptoms: " + e.getMessage());
            }
        });
    }

    /**
     * Symptom Checker v2 matching query:
     * Ranks medicines by:
     * 1. Matched symptoms count DESC
     * 2. In stock first (stock > 0 DESC)
     * 3. Price ASC
     * Filters:
     * - age group (child <= 12, etc.)
     * - pregnant/breastfeeding (avoid_in_pregnancy == 0)
     * - exclude prescription-only (requires_rx == 0)
     */
    public void findMedicinesForSymptoms(List<String> symptomIds, String ageGroup, boolean pregnantOrBreastfeeding,
                                         boolean excludeRx, Callback<List<Medicine>> callback) {
        executor.execute(() -> {
            if (symptomIds == null || symptomIds.isEmpty()) {
                postSuccess(callback, Collections.emptyList());
                return;
            }

            SQLiteDatabase db = dbHelper.getReadableDatabase();
            List<Medicine> result = new ArrayList<>();

            StringBuilder inClause = new StringBuilder();
            List<String> argsList = new ArrayList<>();
            for (int i = 0; i < symptomIds.size(); i++) {
                if (i > 0) inClause.append(", ");
                inClause.append("?");
                argsList.add(symptomIds.get(i));
            }

            StringBuilder query = new StringBuilder(
                    "SELECT m.*, COUNT(DISTINCT ms.symptom_id) AS match_count " +
                    "FROM medicines m " +
                    "JOIN medicine_symptoms ms ON m.id = ms.medicine_id " +
                    "WHERE ms.symptom_id IN (" + inClause + ") " +
                    "AND m.active = 1 "
            );

            if (pregnantOrBreastfeeding) {
                query.append("AND m.avoid_in_pregnancy = 0 ");
            }

            if (excludeRx) {
                query.append("AND m.requires_rx = 0 ");
            }

            if ("child".equalsIgnoreCase(ageGroup)) {
                query.append("AND m.min_age <= 12 ");
            } else if ("senior".equalsIgnoreCase(ageGroup)) {
                query.append("AND m.min_age <= 65 ");
            }

            query.append("GROUP BY m.id ");
            query.append("ORDER BY match_count DESC, (m.stock > 0) DESC, m.price ASC;");

            try (Cursor cursor = db.rawQuery(query.toString(), argsList.toArray(new String[0]))) {
                while (cursor != null && cursor.moveToNext()) {
                    Medicine m = cursorToMedicine(cursor);
                    int matchCount = cursor.getInt(cursor.getColumnIndexOrThrow("match_count"));
                    m.setMatchedSymptomsCount(matchCount);
                    result.add(m);
                }
                postSuccess(callback, result);
            } catch (Exception e) {
                postError(callback, "Matching error: " + e.getMessage());
            }
        });
    }

    // ==========================================
    // CURSOR HELPERS
    // ==========================================

    private Medicine cursorToMedicine(Cursor cursor) {
        Medicine m = new Medicine();
        m.setId(cursor.getString(cursor.getColumnIndexOrThrow("id")));
        m.setName(cursor.getString(cursor.getColumnIndexOrThrow("name")));
        m.setGenericName(cursor.getString(cursor.getColumnIndexOrThrow("generic_name")));
        m.setManufacturer(cursor.getString(cursor.getColumnIndexOrThrow("manufacturer")));
        m.setStrength(cursor.getString(cursor.getColumnIndexOrThrow("strength")));
        m.setPackSize(cursor.getString(cursor.getColumnIndexOrThrow("pack_size")));
        m.setDosageForm(cursor.getString(cursor.getColumnIndexOrThrow("dosage_form")));
        m.setDescription(cursor.getString(cursor.getColumnIndexOrThrow("description")));
        m.setUsageInfo(cursor.getString(cursor.getColumnIndexOrThrow("usage_info")));
        m.setSideEffects(cursor.getString(cursor.getColumnIndexOrThrow("side_effects")));
        m.setMrp(cursor.getDouble(cursor.getColumnIndexOrThrow("mrp")));
        m.setPrice(cursor.getDouble(cursor.getColumnIndexOrThrow("price")));
        m.setGstRate(cursor.getDouble(cursor.getColumnIndexOrThrow("gst_rate")));
        m.setStock(cursor.getInt(cursor.getColumnIndexOrThrow("stock")));
        m.setRequiresPrescription(cursor.getInt(cursor.getColumnIndexOrThrow("requires_rx")) == 1);
        m.setImageUrl(cursor.getString(cursor.getColumnIndexOrThrow("image_path")));
        m.setCategory(cursor.getString(cursor.getColumnIndexOrThrow("category_id")));
        m.setActive(cursor.getInt(cursor.getColumnIndexOrThrow("active")) == 1);
        m.setMinAge(cursor.getInt(cursor.getColumnIndexOrThrow("min_age")));
        m.setAvoidInPregnancy(cursor.getInt(cursor.getColumnIndexOrThrow("avoid_in_pregnancy")));
        m.setCreatedAt(cursor.getLong(cursor.getColumnIndexOrThrow("created_at")));
        m.setUpdatedAt(cursor.getLong(cursor.getColumnIndexOrThrow("updated_at")));
        return m;
    }

    private Order cursorToOrder(Cursor cursor) {
        Order o = new Order();
        o.setOrderId(cursor.getString(cursor.getColumnIndexOrThrow("id")));
        o.setUserId(cursor.getString(cursor.getColumnIndexOrThrow("user_id")));
        o.setCustomerName(cursor.getString(cursor.getColumnIndexOrThrow("customer_name")));
        o.setCustomerPhone(cursor.getString(cursor.getColumnIndexOrThrow("phone")));
        o.setDeliveryAddress(cursor.getString(cursor.getColumnIndexOrThrow("address_text")));
        o.setSubtotal(cursor.getDouble(cursor.getColumnIndexOrThrow("subtotal")));
        o.setDeliveryFee(cursor.getDouble(cursor.getColumnIndexOrThrow("delivery_fee")));
        o.setTotal(cursor.getDouble(cursor.getColumnIndexOrThrow("total")));
        o.setStatus(cursor.getString(cursor.getColumnIndexOrThrow("status")));
        o.setCreatedAt(cursor.getLong(cursor.getColumnIndexOrThrow("created_at")));
        o.setUpdatedAt(cursor.getLong(cursor.getColumnIndexOrThrow("updated_at")));
        o.setStockDeducted(!"Cancelled".equalsIgnoreCase(o.getStatus()));
        return o;
    }
}

