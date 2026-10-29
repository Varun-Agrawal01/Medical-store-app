package com.example.project.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.example.project.models.CartItem;
import com.example.project.models.CsvImportResult;
import com.example.project.models.DrugInteraction;
import com.example.project.models.Medicine;
import com.example.project.models.MedicineBatch;
import com.example.project.models.Order;
import com.example.project.models.OrderItem;
import com.example.project.models.Symptom;
import com.example.project.models.User;
import com.example.project.utils.OrderStateMachine;
import com.example.project.utils.PasswordHasher;
import com.example.project.utils.PricingUtils;
import com.example.project.utils.SessionManager;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Singleton repository for local SQLite database operations.
 * All SQLite operations run on a background thread and deliver results to the main thread.
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

    public MediRepository(Context context) {
        this.context = context != null ? context.getApplicationContext() : null;
        this.dbHelper = new DBHelper(this.context);
        this.sessionManager = new SessionManager(this.context);
        this.executor = Executors.newSingleThreadExecutor();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public MediRepository(Context context, DBHelper dbHelper) {
        this.context = context != null ? context.getApplicationContext() : null;
        this.dbHelper = dbHelper;
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

    public DBHelper getDbHelper() {
        return dbHelper;
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

    public User registerUserSync(String name, String email, String phone, String password, String role) throws Exception {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        String cleanEmail = email.trim().toLowerCase();

        // Check duplicate email
        try (Cursor cursor = db.rawQuery("SELECT 1 FROM users WHERE email = ?", new String[]{cleanEmail})) {
            if (cursor != null && cursor.moveToFirst()) {
                throw new RuntimeException("An account with this email already exists");
            }
        }

        String userId = "usr_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        String hash = PasswordHasher.hash(password.toCharArray());
        long now = System.currentTimeMillis();

        ContentValues values = new ContentValues();
        values.put("id", userId);
        values.put("name", name.trim());
        values.put("email", cleanEmail);
        values.put("phone", phone != null ? phone.trim() : "");
        values.put("password_hash", hash);
        values.put("salt", "");
        values.put("role", role != null ? role : "customer");
        values.put("created_at", now);

        long rowId = db.insert("users", null, values);
        if (rowId == -1) {
            throw new RuntimeException("Failed to register user account");
        }

        User user = new User(userId, name.trim(), cleanEmail, phone != null ? phone.trim() : "", role != null ? role : "customer", now);
        if (sessionManager != null) {
            sessionManager.saveUserId(userId);
        }
        return user;
    }

    public void registerUser(String name, String email, String phone, String password, String role, Callback<User> callback) {
        executor.execute(() -> {
            try {
                User user = registerUserSync(name, email, phone, password, role);
                postSuccess(callback, user);
            } catch (Exception e) {
                postError(callback, e.getMessage());
            }
        });
    }

    public User loginUserSync(String email, String password) throws Exception {
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
                    if (sessionManager != null) {
                        sessionManager.saveUserId(id);
                    }
                    return user;
                }
            }
            throw new RuntimeException("Invalid email or password");
        }
    }

    public void loginUser(String email, String password, Callback<User> callback) {
        executor.execute(() -> {
            try {
                User user = loginUserSync(email, password);
                postSuccess(callback, user);
            } catch (Exception e) {
                postError(callback, e.getMessage());
            }
        });
    }

    public void getUserProfile(String userId, Callback<User> callback) {
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

                    postSuccess(callback, new User(id, name, email, phone, role, createdAt));
                } else {
                    postError(callback, "User not found");
                }
            } catch (Exception e) {
                postError(callback, "Error fetching user profile: " + e.getMessage());
            }
        });
    }

    public void updateUserProfile(String userId, String name, String phone, Callback<Void> callback) {
        executor.execute(() -> {
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            ContentValues cv = new ContentValues();
            cv.put("name", name.trim());
            cv.put("phone", phone.trim());

            int rows = db.update("users", cv, "id = ?", new String[]{userId});
            if (rows > 0) {
                postSuccess(callback, null);
            } else {
                postError(callback, "Profile update failed: User not found");
            }
        });
    }

    public void changePassword(String userId, String currentPassword, String newPassword, Callback<Void> callback) {
        executor.execute(() -> {
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            try (Cursor cursor = db.rawQuery("SELECT password_hash FROM users WHERE id = ?", new String[]{userId})) {
                if (cursor != null && cursor.moveToFirst()) {
                    String storedHash = cursor.getString(0);
                    if (!PasswordHasher.verify(currentPassword.toCharArray(), storedHash)) {
                        postError(callback, "Current password is incorrect");
                        return;
                    }

                    String newHash = PasswordHasher.hash(newPassword.toCharArray());
                    ContentValues cv = new ContentValues();
                    cv.put("password_hash", newHash);

                    int rows = db.update("users", cv, "id = ?", new String[]{userId});
                    if (rows > 0) {
                        postSuccess(callback, null);
                    } else {
                        postError(callback, "Password update failed");
                    }
                } else {
                    postError(callback, "User not found");
                }
            } catch (Exception e) {
                postError(callback, "Error updating password: " + e.getMessage());
            }
        });
    }

    // ==========================================
    // MEDICINES & CATALOG
    // ==========================================

    public void getMedicines(int limit, int offset, String categoryFilter, Callback<List<Medicine>> callback) {
        executor.execute(() -> {
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            List<Medicine> list = new ArrayList<>();
            String sql;
            String[] args;

            if (categoryFilter != null && !categoryFilter.isEmpty() && !"all".equalsIgnoreCase(categoryFilter)) {
                sql = "SELECT * FROM medicines WHERE category_id = ? AND active = 1 ORDER BY name ASC LIMIT ? OFFSET ?";
                args = new String[]{categoryFilter, String.valueOf(limit), String.valueOf(offset)};
            } else {
                sql = "SELECT * FROM medicines WHERE active = 1 ORDER BY name ASC LIMIT ? OFFSET ?";
                args = new String[]{String.valueOf(limit), String.valueOf(offset)};
            }

            try (Cursor cursor = db.rawQuery(sql, args)) {
                while (cursor != null && cursor.moveToNext()) {
                    list.add(cursorToMedicine(cursor));
                }
                postSuccess(callback, list);
            } catch (Exception e) {
                postError(callback, "Query error: " + e.getMessage());
            }
        });
    }

    public void searchMedicines(String query, int limit, int offset, Callback<List<Medicine>> callback) {
        executor.execute(() -> {
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            List<Medicine> list = new ArrayList<>();

            String trimmed = query != null ? query.trim() : "";
            if (trimmed.isEmpty()) {
                getMedicines(limit, offset, null, callback);
                return;
            }

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
                // Fallback to LIKE query if FTS expression encounters non-standard syntax
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
            db.beginTransaction();
            try {
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
                cv.put("category_id", medicine.getCategory() != null && !medicine.getCategory().isEmpty()
                        ? medicine.getCategory() : "cat_analgesics");
                cv.put("active", medicine.isActive() ? 1 : 0);
                cv.put("min_age", medicine.getMinAge());
                cv.put("avoid_in_pregnancy", medicine.getAvoidInPregnancy());
                cv.put("created_at", medicine.getCreatedAt() > 0 ? medicine.getCreatedAt() : now);
                cv.put("updated_at", now);

                long rowId = db.insertWithOnConflict("medicines", null, cv, SQLiteDatabase.CONFLICT_REPLACE);
                if (rowId != -1) {
                    db.setTransactionSuccessful();
                    postSuccess(callback, null);
                } else {
                    postError(callback, "database insert returned -1");
                }
            } catch (Exception e) {
                Log.e("MEDSAVE", "save failed", e);
                postError(callback, e.getMessage());
            } finally {
                db.endTransaction();
            }
        });
    }

    public List<Medicine> getGenericSubstitutesSync(String medicineId) throws Exception {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String genericName = null;
        String strength = null;

        try (Cursor c = db.rawQuery("SELECT generic_name, strength FROM medicines WHERE id = ?", new String[]{medicineId})) {
            if (c != null && c.moveToFirst()) {
                genericName = c.getString(0);
                strength = c.getString(1);
            }
        }

        if (genericName == null || genericName.trim().isEmpty() || strength == null || strength.trim().isEmpty()) {
            return Collections.emptyList();
        }

        List<Medicine> substitutes = new ArrayList<>();
        String sql = "SELECT * FROM medicines WHERE generic_name = ? AND strength = ? AND id != ? AND active = 1 ORDER BY price ASC";
        try (Cursor cursor = db.rawQuery(sql, new String[]{genericName, strength, medicineId})) {
            while (cursor != null && cursor.moveToNext()) {
                substitutes.add(cursorToMedicine(cursor));
            }
        }
        return substitutes;
    }

    public void getGenericSubstitutes(String medicineId, Callback<List<Medicine>> callback) {
        executor.execute(() -> {
            try {
                List<Medicine> subs = getGenericSubstitutesSync(medicineId);
                postSuccess(callback, subs);
            } catch (Exception e) {
                postError(callback, e.getMessage());
            }
        });
    }

    // ==========================================
    // CART OPERATIONS & INTERACTIONS
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

    public List<DrugInteraction> checkCartInteractionsSync(List<String> genericNames) throws Exception {
        if (genericNames == null || genericNames.size() < 2) {
            return Collections.emptyList();
        }
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        List<DrugInteraction> interactions = new ArrayList<>();

        for (int i = 0; i < genericNames.size(); i++) {
            for (int j = i + 1; j < genericNames.size(); j++) {
                String genA = genericNames.get(i).trim();
                String genB = genericNames.get(j).trim();

                String sql = "SELECT id, generic_a, generic_b, severity, message FROM drug_interactions " +
                        "WHERE (LOWER(generic_a) = LOWER(?) AND LOWER(generic_b) = LOWER(?)) " +
                        "   OR (LOWER(generic_a) = LOWER(?) AND LOWER(generic_b) = LOWER(?))";

                try (Cursor cursor = db.rawQuery(sql, new String[]{genA, genB, genB, genA})) {
                    while (cursor != null && cursor.moveToNext()) {
                        interactions.add(new DrugInteraction(
                                cursor.getString(0),
                                cursor.getString(1),
                                cursor.getString(2),
                                cursor.getString(3),
                                cursor.getString(4)
                        ));
                    }
                }
            }
        }
        return interactions;
    }

    public void checkCartInteractions(List<String> genericNames, Callback<List<DrugInteraction>> callback) {
        executor.execute(() -> {
            try {
                List<DrugInteraction> list = checkCartInteractionsSync(genericNames);
                postSuccess(callback, list);
            } catch (Exception e) {
                postError(callback, e.getMessage());
            }
        });
    }

    // ==========================================
    // ORDERS & TRANSACTIONAL CHECKOUT
    // ==========================================

    private static final Pattern PINCODE_REGEX = Pattern.compile("\\b([1-9]\\d{5})\\b");

    public Order placeOrderSync(String userId, String customerName, String customerPhone,
                               String deliveryAddress, List<CartItem> cartItems) throws Exception {
        if (cartItems == null || cartItems.isEmpty()) {
            throw new RuntimeException("Cart is empty");
        }

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            // 1. Verify serviceability of delivery pincode if service_pincodes table is configured
            if (deliveryAddress != null) {
                Matcher matcher = PINCODE_REGEX.matcher(deliveryAddress);
                if (matcher.find()) {
                    String pincode = matcher.group(1);
                    boolean tableHasData = false;
                    try (Cursor checkCount = db.rawQuery("SELECT 1 FROM service_pincodes LIMIT 1", null)) {
                        if (checkCount != null && checkCount.moveToFirst()) {
                            tableHasData = true;
                        }
                    }
                    if (tableHasData) {
                        try (Cursor pinCursor = db.rawQuery(
                                "SELECT 1 FROM service_pincodes WHERE pincode = ? AND is_active = 1",
                                new String[]{pincode})) {
                            if (pinCursor == null || !pinCursor.moveToFirst()) {
                                throw new RuntimeException("Delivery is not available in pincode " + pincode + ".");
                            }
                        }
                    }
                }
            }

            List<OrderItem> orderItems = new ArrayList<>();
            double subtotal = 0.0;
            double gstTotal = 0.0;
            boolean rxRequired = false;
            long now = System.currentTimeMillis();

            // 2. Validate stock and perform FEFO batch deduction
            for (CartItem ci : cartItems) {
                try (Cursor cursor = db.rawQuery(
                        "SELECT name, price, stock, active, requires_rx, gst_rate FROM medicines WHERE id = ?",
                        new String[]{ci.getMedicineId()})) {

                    if (cursor == null || !cursor.moveToFirst()) {
                        throw new RuntimeException("Medicine " + ci.getMedicineName() + " is no longer available.");
                    }

                    String name = cursor.getString(0);
                    double trustedPrice = cursor.getDouble(1);
                    int currentStock = cursor.getInt(2);
                    int active = cursor.getInt(3);
                    int rx = cursor.getInt(4);
                    double gstRate = cursor.getDouble(5);

                    if (active == 0) {
                        throw new RuntimeException("Medicine " + name + " is currently discontinued.");
                    }
                    if (currentStock < ci.getQuantity()) {
                        throw new RuntimeException("Insufficient stock for " + name + " (Only " + currentStock + " left).");
                    }

                    if (rx == 1) {
                        rxRequired = true;
                    }

                    // FEFO Batch deduction: deduct from earliest expiry batch first
                    String usedBatchNo = "";
                    String batchSql = "SELECT id, batch_no, expiry_date, quantity FROM medicine_batches " +
                            "WHERE medicine_id = ? AND quantity > 0 ORDER BY expiry_date ASC";

                    try (Cursor batchCursor = db.rawQuery(batchSql, new String[]{ci.getMedicineId()})) {
                        int remainingToDeduct = ci.getQuantity();
                        while (batchCursor != null && batchCursor.moveToNext() && remainingToDeduct > 0) {
                            String batchId = batchCursor.getString(0);
                            String batchNo = batchCursor.getString(1);
                            int bQty = batchCursor.getInt(3);

                            if (usedBatchNo.isEmpty()) {
                                usedBatchNo = batchNo;
                            }
                            int deduct = Math.min(bQty, remainingToDeduct);
                            db.execSQL("UPDATE medicine_batches SET quantity = quantity - ? WHERE id = ?",
                                    new Object[]{deduct, batchId});
                            remainingToDeduct -= deduct;
                        }
                    }

                    // Deduct overall medicine stock
                    ContentValues stockCv = new ContentValues();
                    stockCv.put("stock", currentStock - ci.getQuantity());
                    stockCv.put("updated_at", now);
                    db.update("medicines", stockCv, "id = ?", new String[]{ci.getMedicineId()});

                    double itemTotal = trustedPrice * ci.getQuantity();
                    subtotal += itemTotal;
                    gstTotal += PricingUtils.calculateGstAmount(itemTotal, gstRate);

                    orderItems.add(new OrderItem(ci.getMedicineId(), name, trustedPrice, ci.getQuantity(), usedBatchNo));
                }
            }

            // 3. Verify prescription requirement if any rx item is included
            if (rxRequired) {
                boolean hasPrescription = false;
                try (Cursor rxCursor = db.rawQuery(
                        "SELECT id FROM prescriptions WHERE user_id = ? AND (status IS NULL OR status != 'Rejected') LIMIT 1",
                        new String[]{userId})) {
                    if (rxCursor != null && rxCursor.moveToFirst()) {
                        hasPrescription = true;
                    }
                }
                if (!hasPrescription) {
                    throw new RuntimeException("Prescription is required for prescription-only medicines.");
                }
            }

            double deliveryFee = PricingUtils.calculateDeliveryFee(subtotal);
            double total = PricingUtils.calculateTotal(subtotal, deliveryFee);

            // 4. Sequential, collision-free order number from order_counter table (MS-YYYY-XXXXXX)
            int currentYear = Calendar.getInstance().get(Calendar.YEAR);
            int nextCounter = 1;
            try (Cursor counterCursor = db.rawQuery("SELECT last_number FROM order_counter WHERE counter_year = ?",
                    new String[]{String.valueOf(currentYear)})) {
                if (counterCursor != null && counterCursor.moveToFirst()) {
                    nextCounter = counterCursor.getInt(0) + 1;
                    db.execSQL("UPDATE order_counter SET last_number = ? WHERE counter_year = ?",
                            new Object[]{nextCounter, currentYear});
                } else {
                    ContentValues cvCounter = new ContentValues();
                    cvCounter.put("counter_year", currentYear);
                    cvCounter.put("last_number", 1);
                    db.insert("order_counter", null, cvCounter);
                    nextCounter = 1;
                }
            }

            String orderId = String.format(Locale.US, "MS-%d-%06d", currentYear, nextCounter);
            String initialStatus = rxRequired ? OrderStateMachine.STATUS_AWAITING_RX : OrderStateMachine.STATUS_PENDING;

            // 5. Insert into orders table
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
            orderCv.put("status", initialStatus);
            orderCv.put("rx_required", rxRequired ? 1 : 0);
            orderCv.put("created_at", now);
            orderCv.put("updated_at", now);

            long insertResult = db.insert("orders", null, orderCv);
            if (insertResult == -1) {
                throw new RuntimeException("Failed to generate order record");
            }

            // 6. Insert order_items
            String itemSql = "INSERT INTO order_items(id, order_id, medicine_id, name, unit_price, gst_rate, quantity, batch_no) VALUES(?, ?, ?, ?, ?, ?, ?, ?);";
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
                    if (oi.getBatchNo() != null) {
                        itemStmt.bindString(8, oi.getBatchNo());
                    } else {
                        itemStmt.bindNull(8);
                    }
                    itemStmt.executeInsert();
                }
            }

            // 7. Status history record
            ContentValues historyCv = new ContentValues();
            historyCv.put("id", UUID.randomUUID().toString());
            historyCv.put("order_id", orderId);
            historyCv.put("status", initialStatus);
            historyCv.put("note", "Order placed with initial status: " + initialStatus);
            historyCv.put("changed_at", now);
            db.insert("order_status_history", null, historyCv);

            // 8. Link open prescription to order if applicable
            ContentValues linkPrescriptionCv = new ContentValues();
            linkPrescriptionCv.put("order_id", orderId);
            db.update("prescriptions", linkPrescriptionCv, "user_id = ? AND (order_id IS NULL OR order_id = '')", new String[]{userId});

            // 9. Clear cart
            db.delete("cart_items", "user_id = ?", new String[]{userId});

            db.setTransactionSuccessful();

            return new Order(orderId, userId, customerName, customerPhone, deliveryAddress,
                    orderItems, subtotal, deliveryFee, total, initialStatus, now, now, true);
        } finally {
            db.endTransaction();
        }
    }

    public void placeOrderTransaction(String userId, String customerName, String customerPhone,
                                      String deliveryAddress, List<CartItem> cartItems, Callback<Order> callback) {
        executor.execute(() -> {
            try {
                Order order = placeOrderSync(userId, customerName, customerPhone, deliveryAddress, cartItems);
                postSuccess(callback, order);
            } catch (Exception e) {
                postError(callback, e.getMessage());
            }
        });
    }

    public void cancelOrderSync(String orderId) throws Exception {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            String currentStatus = "";
            try (Cursor c = db.rawQuery("SELECT status FROM orders WHERE id = ? OR order_no = ?", new String[]{orderId, orderId})) {
                if (c != null && c.moveToFirst()) {
                    currentStatus = c.getString(0);
                } else {
                    throw new RuntimeException("Order not found: " + orderId);
                }
            }

            // If already cancelled, do not cancel or restore stock again
            if (OrderStateMachine.STATUS_CANCELLED.equalsIgnoreCase(currentStatus)) {
                db.setTransactionSuccessful();
                return;
            }

            // State machine validation
            OrderStateMachine.validateTransition(currentStatus, OrderStateMachine.STATUS_CANCELLED);

            long now = System.currentTimeMillis();

            // Restore stock for all items
            try (Cursor itemCursor = db.rawQuery(
                    "SELECT medicine_id, quantity, batch_no FROM order_items WHERE order_id = ? OR order_id = " +
                            "(SELECT id FROM orders WHERE order_no = ?)",
                    new String[]{orderId, orderId})) {

                while (itemCursor != null && itemCursor.moveToNext()) {
                    String medId = itemCursor.getString(0);
                    int qty = itemCursor.getInt(1);
                    String batchNo = itemCursor.getString(2);

                    db.execSQL("UPDATE medicines SET stock = stock + ?, updated_at = ? WHERE id = ?",
                            new Object[]{qty, now, medId});

                    if (batchNo != null && !batchNo.isEmpty()) {
                        db.execSQL("UPDATE medicine_batches SET quantity = quantity + ? WHERE medicine_id = ? AND batch_no = ?",
                                new Object[]{qty, medId, batchNo});
                    }
                }
            }

            // Update order status
            ContentValues cv = new ContentValues();
            cv.put("status", OrderStateMachine.STATUS_CANCELLED);
            cv.put("updated_at", now);
            db.update("orders", cv, "id = ? OR order_no = ?", new String[]{orderId, orderId});

            // Add history entry
            ContentValues historyCv = new ContentValues();
            historyCv.put("id", UUID.randomUUID().toString());
            historyCv.put("order_id", orderId);
            historyCv.put("status", OrderStateMachine.STATUS_CANCELLED);
            historyCv.put("note", "Order cancelled and inventory restored.");
            historyCv.put("changed_at", now);
            db.insert("order_status_history", null, historyCv);

            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public void cancelOrder(String orderId, Callback<Void> callback) {
        executor.execute(() -> {
            try {
                cancelOrderSync(orderId);
                postSuccess(callback, null);
            } catch (Exception e) {
                postError(callback, e.getMessage());
            }
        });
    }

    public void rejectPrescriptionSync(String orderId, String reason) throws Exception {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            ContentValues rxCv = new ContentValues();
            rxCv.put("status", "Rejected");
            rxCv.put("review_note", reason);
            db.update("prescriptions", rxCv, "order_id = ?", new String[]{orderId});

            // Automatically cancel order and restore stock
            cancelOrderSync(orderId);

            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public void rejectPrescription(String orderId, String reason, Callback<Void> callback) {
        executor.execute(() -> {
            try {
                rejectPrescriptionSync(orderId, reason);
                postSuccess(callback, null);
            } catch (Exception e) {
                postError(callback, e.getMessage());
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
                "SELECT medicine_id, name, unit_price, quantity, batch_no FROM order_items WHERE order_id = ?",
                new String[]{order.getOrderId()})) {

            while (cursor != null && cursor.moveToNext()) {
                String mid = cursor.getString(0);
                String mname = cursor.getString(1);
                double price = cursor.getDouble(2);
                int qty = cursor.getInt(3);
                String batch = cursor.getString(4);
                items.add(new OrderItem(mid, mname, price, qty, batch));
            }
        }
        order.setItems(items);
    }

    public void updateOrderStatus(Order order, String newStatus, Callback<Void> callback) {
        executor.execute(() -> {
            try {
                if (OrderStateMachine.STATUS_CANCELLED.equalsIgnoreCase(newStatus)) {
                    cancelOrderSync(order.getOrderId());
                    order.setStatus(OrderStateMachine.STATUS_CANCELLED);
                    postSuccess(callback, null);
                    return;
                }

                OrderStateMachine.validateTransition(order.getStatus(), newStatus);

                SQLiteDatabase db = dbHelper.getWritableDatabase();
                long now = System.currentTimeMillis();

                ContentValues orderCv = new ContentValues();
                orderCv.put("status", newStatus);
                orderCv.put("updated_at", now);
                db.update("orders", orderCv, "id = ? OR order_no = ?", new String[]{order.getOrderId(), order.getOrderId()});

                ContentValues historyCv = new ContentValues();
                historyCv.put("id", UUID.randomUUID().toString());
                historyCv.put("order_id", order.getOrderId());
                historyCv.put("status", newStatus);
                historyCv.put("note", "Status changed to " + newStatus);
                historyCv.put("changed_at", now);
                db.insert("order_status_history", null, historyCv);

                order.setStatus(newStatus);
                order.setUpdatedAt(now);
                postSuccess(callback, null);
            } catch (Exception e) {
                postError(callback, e.getMessage());
            }
        });
    }

    // ==========================================
    // CSV IMPORT
    // ==========================================

    public CsvImportResult importMedicinesFromCsvSync(String csvContent) throws Exception {
        CsvImportResult result = new CsvImportResult();
        if (csvContent == null || csvContent.trim().isEmpty()) {
            return result;
        }

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            String[] lines = csvContent.split("\\r?\\n");
            boolean isHeader = true;

            for (int lineNum = 0; lineNum < lines.length; lineNum++) {
                String line = lines[lineNum].trim();
                if (line.isEmpty()) continue;

                if (isHeader) {
                    isHeader = false;
                    if (line.toLowerCase().startsWith("name")) {
                        continue;
                    }
                }

                String[] cols = line.split(",", -1);
                if (cols.length < 5) {
                    result.addRejectedReason("Line " + (lineNum + 1) + ": Insufficient columns");
                    continue;
                }

                String name = cols[0].trim();
                String genericName = cols.length > 1 ? cols[1].trim() : "";
                String manufacturer = cols.length > 2 ? cols[2].trim() : "";
                String strength = cols.length > 3 ? cols[3].trim() : "";

                if (name.isEmpty()) {
                    result.addRejectedReason("Line " + (lineNum + 1) + ": Medicine name cannot be empty");
                    continue;
                }

                double price;
                double mrp;
                int stock;
                try {
                    price = Double.parseDouble(cols[4].trim());
                    mrp = cols.length > 5 && !cols[5].trim().isEmpty() ? Double.parseDouble(cols[5].trim()) : price;
                    stock = cols.length > 6 && !cols[6].trim().isEmpty() ? Integer.parseInt(cols[6].trim()) : 0;
                } catch (NumberFormatException e) {
                    result.addRejectedReason("Line " + (lineNum + 1) + ": Invalid numeric format for price, mrp, or stock");
                    continue;
                }

                if (price <= 0) {
                    result.addRejectedReason("Line " + (lineNum + 1) + ": Price must be greater than zero");
                    continue;
                }
                if (price > mrp) {
                    result.addRejectedReason("Line " + (lineNum + 1) + ": Price (" + price + ") cannot exceed MRP (" + mrp + ")");
                    continue;
                }
                if (stock < 0) {
                    result.addRejectedReason("Line " + (lineNum + 1) + ": Stock cannot be negative");
                    continue;
                }

                int requiresRx = cols.length > 7 && "1".equals(cols[7].trim()) ? 1 : 0;
                String categoryId = cols.length > 8 && !cols[8].trim().isEmpty() ? cols[8].trim() : "cat_analgesics";

                String medId = "med_csv_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
                long now = System.currentTimeMillis();

                ContentValues cv = new ContentValues();
                cv.put("id", medId);
                cv.put("name", name);
                cv.put("generic_name", genericName);
                cv.put("manufacturer", manufacturer);
                cv.put("strength", strength);
                cv.put("price", price);
                cv.put("mrp", mrp);
                cv.put("stock", stock);
                cv.put("gst_rate", 12.0);
                cv.put("requires_rx", requiresRx);
                cv.put("category_id", categoryId);
                cv.put("active", 1);
                cv.put("created_at", now);
                cv.put("updated_at", now);

                long insertId = db.insert("medicines", null, cv);
                if (insertId != -1) {
                    result.incrementInserted();
                } else {
                    result.addRejectedReason("Line " + (lineNum + 1) + ": Database insert failed");
                }
            }

            db.setTransactionSuccessful();
            return result;
        } finally {
            db.endTransaction();
        }
    }

    public void importMedicinesFromCsv(String csvContent, Callback<CsvImportResult> callback) {
        executor.execute(() -> {
            try {
                CsvImportResult result = importMedicinesFromCsvSync(csvContent);
                postSuccess(callback, result);
            } catch (Exception e) {
                postError(callback, e.getMessage());
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

    public List<Medicine> findMedicinesForSymptomsSync(List<String> symptomIds, String ageGroup,
                                                       boolean pregnantOrBreastfeeding, boolean excludeRx) throws Exception {
        if (symptomIds == null || symptomIds.isEmpty()) {
            return Collections.emptyList();
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
        }
        return result;
    }

    public void findMedicinesForSymptoms(List<String> symptomIds, String ageGroup, boolean pregnantOrBreastfeeding,
                                         boolean excludeRx, Callback<List<Medicine>> callback) {
        executor.execute(() -> {
            try {
                List<Medicine> list = findMedicinesForSymptomsSync(symptomIds, ageGroup, pregnantOrBreastfeeding, excludeRx);
                postSuccess(callback, list);
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
        o.setStockDeducted(!OrderStateMachine.STATUS_CANCELLED.equalsIgnoreCase(o.getStatus()));
        return o;
    }

    public void getUserById(String userId, Callback<User> callback) {
        getUserProfile(userId, callback);
    }

    public void updateProfile(String userId, String newName, String newPhone, Callback<Void> callback) {
        updateUserProfile(userId, newName, newPhone, callback);
    }

    public User getCurrentUser() {
        if (sessionManager == null) return null;
        String userId = sessionManager.getUserId();
        if (userId == null) return null;
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
        } catch (Exception e) {
            return null;
        }
        return null;
    }

    public void logout() {
        if (sessionManager != null) {
            sessionManager.clear();
        }
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

                        User user = new User(id, name, email, phone, role, createdAt);
                        if (sessionManager != null) {
                            sessionManager.saveUserId(id);
                        }
                        postSuccess(callback, user);
                        return;
                    }
                }
                postError(callback, "Invalid administrator credentials");
            } catch (Exception e) {
                postError(callback, "Database error during admin login: " + e.getMessage());
            }
        });
    }

    public void getMedicines(Callback<List<Medicine>> callback) {
        getMedicines(100, 0, null, callback);
    }

    public void getMedicinesPaged(int limit, int offset, String categoryFilter, Callback<List<Medicine>> callback) {
        getMedicines(limit, offset, categoryFilter, callback);
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

}
