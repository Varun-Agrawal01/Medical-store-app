package com.example.project.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.example.project.utils.PasswordHasher;
import com.example.project.utils.SeedLoader;

/**
 * SQLite Database Helper for MediStore pharmacy application.
 *
 * Schema Version History:
 * - Version 1 (medistore_v2.db): Complete redesign.
 *     - Removed: categories table, medicines.category_id FK, medicines_fts virtual table,
 *       all FTS triggers. These were the root cause of SQLITE_CONSTRAINT (code 19) on every insert.
 *     - "Category" concept is now derived from the first linked symptom name (or "General").
 *     - No triggers anywhere. No virtual tables.
 */
public class DBHelper extends SQLiteOpenHelper {

    private static final String TAG = "DBHelper";

    /** New database file name — ensures the old broken medistore.db is ignored on first run. */
    public static final String DATABASE_NAME = "medistore_v2.db";
    public static final int DATABASE_VERSION = 1;

    private final Context context;
    private final boolean isInMemory;

    public DBHelper(Context context) {
        this(context, DATABASE_NAME);
    }

    public DBHelper(Context context, String dbName) {
        super(context != null ? context.getApplicationContext() : null, dbName, null, DATABASE_VERSION);
        this.context = context != null ? context.getApplicationContext() : null;
        this.isInMemory = (dbName == null);

        // On first construction delete the old broken database so no stale data interferes.
        if (!isInMemory && context != null) {
            context.getApplicationContext().deleteDatabase("medistore.db");
        }
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(true);
    }

    // onOpen intentionally left empty — no trigger recreation, no per-open seeding.

    @Override
    public void onCreate(SQLiteDatabase db) {
        // 1. Users table
        db.execSQL("CREATE TABLE users (" +
                "id TEXT PRIMARY KEY, " +
                "name TEXT NOT NULL, " +
                "email TEXT UNIQUE NOT NULL, " +
                "phone TEXT, " +
                "password_hash TEXT, " +
                "salt TEXT, " +
                "role TEXT, " +
                "created_at INTEGER);");

        // 2. Symptoms table  (replaces old "categories" for filtering purposes)
        db.execSQL("CREATE TABLE symptoms (" +
                "id TEXT PRIMARY KEY, " +
                "name TEXT NOT NULL, " +
                "red_flag_advice TEXT);");

        // 3. Medicines table — NO category_id column, NO foreign key to any categories table.
        //    "Category" is derived at query time from medicine_symptoms.
        db.execSQL("CREATE TABLE medicines (" +
                "id TEXT PRIMARY KEY, " +
                "name TEXT NOT NULL, " +
                "price REAL NOT NULL, " +
                "generic_name TEXT DEFAULT '', " +
                "manufacturer TEXT DEFAULT '', " +
                "strength TEXT DEFAULT '', " +
                "pack_size TEXT DEFAULT '', " +
                "dosage_form TEXT DEFAULT 'Tablet', " +
                "description TEXT DEFAULT '', " +
                "usage_info TEXT DEFAULT '', " +
                "side_effects TEXT DEFAULT '', " +
                "mrp REAL DEFAULT 0, " +
                "gst_rate REAL DEFAULT 12, " +
                "stock INTEGER DEFAULT 0, " +
                "requires_rx INTEGER DEFAULT 0, " +
                "image_path TEXT DEFAULT '', " +
                "active INTEGER DEFAULT 1, " +
                "min_age INTEGER DEFAULT 0, " +
                "avoid_in_pregnancy INTEGER DEFAULT 0, " +
                "created_at INTEGER, " +
                "updated_at INTEGER);");

        // 4. Medicine–Symptom junction table
        db.execSQL("CREATE TABLE medicine_symptoms (" +
                "medicine_id TEXT, " +
                "symptom_id TEXT, " +
                "PRIMARY KEY(medicine_id, symptom_id), " +
                "FOREIGN KEY(medicine_id) REFERENCES medicines(id) ON DELETE CASCADE, " +
                "FOREIGN KEY(symptom_id) REFERENCES symptoms(id) ON DELETE CASCADE);");

        // 5. Cart items table
        db.execSQL("CREATE TABLE cart_items (" +
                "id TEXT PRIMARY KEY, " +
                "user_id TEXT, " +
                "medicine_id TEXT, " +
                "quantity INTEGER, " +
                "FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE, " +
                "FOREIGN KEY(medicine_id) REFERENCES medicines(id) ON DELETE CASCADE);");

        // 6. Addresses table
        db.execSQL("CREATE TABLE addresses (" +
                "id TEXT PRIMARY KEY, " +
                "user_id TEXT, " +
                "label TEXT, " +
                "line1 TEXT, " +
                "line2 TEXT, " +
                "pincode TEXT, " +
                "is_default INTEGER DEFAULT 0, " +
                "FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE);");

        // 7. Orders table
        db.execSQL("CREATE TABLE orders (" +
                "id TEXT PRIMARY KEY, " +
                "order_no TEXT UNIQUE NOT NULL, " +
                "user_id TEXT, " +
                "customer_name TEXT, " +
                "phone TEXT, " +
                "address_text TEXT, " +
                "subtotal REAL, " +
                "delivery_fee REAL, " +
                "gst_total REAL, " +
                "total REAL, " +
                "payment_method TEXT, " +
                "payment_status TEXT, " +
                "status TEXT, " +
                "rx_required INTEGER DEFAULT 0, " +
                "created_at INTEGER, " +
                "updated_at INTEGER, " +
                "FOREIGN KEY(user_id) REFERENCES users(id));");

        // 8. Order items table
        db.execSQL("CREATE TABLE order_items (" +
                "id TEXT PRIMARY KEY, " +
                "order_id TEXT, " +
                "medicine_id TEXT, " +
                "name TEXT, " +
                "unit_price REAL, " +
                "gst_rate REAL, " +
                "quantity INTEGER, " +
                "batch_no TEXT, " +
                "FOREIGN KEY(order_id) REFERENCES orders(id) ON DELETE CASCADE, " +
                "FOREIGN KEY(medicine_id) REFERENCES medicines(id));");

        // 9. Order status history table
        db.execSQL("CREATE TABLE order_status_history (" +
                "id TEXT PRIMARY KEY, " +
                "order_id TEXT, " +
                "status TEXT, " +
                "note TEXT, " +
                "changed_at INTEGER, " +
                "FOREIGN KEY(order_id) REFERENCES orders(id) ON DELETE CASCADE);");

        // 10. Prescriptions table
        db.execSQL("CREATE TABLE prescriptions (" +
                "id TEXT PRIMARY KEY, " +
                "user_id TEXT, " +
                "order_id TEXT, " +
                "file_path TEXT, " +
                "status TEXT, " +
                "review_note TEXT, " +
                "created_at INTEGER, " +
                "FOREIGN KEY(user_id) REFERENCES users(id), " +
                "FOREIGN KEY(order_id) REFERENCES orders(id));");

        // 11. Medicine Batches table for FEFO inventory management
        createBatchesTable(db);

        // 12. Service Pincodes table
        createServicePincodesTable(db);

        // 13. Order Counter table for unique order numbers
        createOrderCounterTable(db);

        // 14. Drug Interactions table
        createDrugInteractionsTable(db);

        // 15. Shop Settings table
        createShopSettingsTable(db);

        // Indexes for performance — NO category index (column does not exist)
        db.execSQL("CREATE INDEX idx_medicines_name ON medicines(name);");
        db.execSQL("CREATE INDEX idx_medicines_active ON medicines(active);");
        db.execSQL("CREATE INDEX idx_cart_user ON cart_items(user_id);");
        db.execSQL("CREATE INDEX idx_orders_user ON orders(user_id);");
        db.execSQL("CREATE INDEX idx_orders_created ON orders(created_at);");
        db.execSQL("CREATE INDEX idx_order_items_order ON order_items(order_id);");
        db.execSQL("CREATE INDEX idx_order_history_order ON order_status_history(order_id);");
        db.execSQL("CREATE INDEX idx_med_sym_sym ON medicine_symptoms(symptom_id);");
        db.execSQL("CREATE INDEX idx_batches_med ON medicine_batches(medicine_id);");

        // Seed default operational data and medicine catalogue
        if (!isInMemory && context != null) {
            seedDefaultAdmin(db);
            seedDefaultPincodes(db);
            seedDefaultInteractions(db);
            seedDefaultShopSettings(db);
            try {
                SeedLoader.loadSeedData(context, db);
            } catch (Exception e) {
                Log.e(TAG, "Seed failed during onCreate — app will retry on next launch", e);
            }
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // medistore_v2.db starts at version 1. Future incremental migrations go here.
        Log.i(TAG, "onUpgrade from " + oldVersion + " to " + newVersion);
    }

    // -----------------------------------------------------------------------
    // Table creation helpers (CREATE TABLE IF NOT EXISTS for safe onUpgrade use)
    // -----------------------------------------------------------------------

    private void createBatchesTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS medicine_batches (" +
                "id TEXT PRIMARY KEY, " +
                "medicine_id TEXT NOT NULL, " +
                "batch_no TEXT NOT NULL, " +
                "expiry_date TEXT NOT NULL, " +
                "quantity INTEGER NOT NULL, " +
                "created_at INTEGER, " +
                "FOREIGN KEY(medicine_id) REFERENCES medicines(id) ON DELETE CASCADE);");
    }

    private void createServicePincodesTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS service_pincodes (" +
                "pincode TEXT PRIMARY KEY, " +
                "area_name TEXT NOT NULL, " +
                "is_active INTEGER DEFAULT 1);");
    }

    private void createOrderCounterTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS order_counter (" +
                "counter_year INTEGER PRIMARY KEY, " +
                "last_number INTEGER NOT NULL);");
    }

    private void createDrugInteractionsTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS drug_interactions (" +
                "id TEXT PRIMARY KEY, " +
                "generic_a TEXT NOT NULL, " +
                "generic_b TEXT NOT NULL, " +
                "severity TEXT NOT NULL, " +
                "message TEXT NOT NULL);");
    }

    private void createShopSettingsTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS shop_settings (" +
                "id TEXT PRIMARY KEY, " +
                "shop_name TEXT NOT NULL, " +
                "address TEXT NOT NULL, " +
                "gstin TEXT NOT NULL, " +
                "dl_number TEXT NOT NULL, " +
                "pharmacist_name TEXT NOT NULL, " +
                "reg_number TEXT NOT NULL, " +
                "invoice_prefix TEXT NOT NULL);");
    }

    // -----------------------------------------------------------------------
    // Default seed data (non-medicine catalogue — always inserted)
    // -----------------------------------------------------------------------

    private void seedDefaultAdmin(SQLiteDatabase db) {
        ContentValues values = new ContentValues();
        values.put("id", "admin_001");
        values.put("name", "Medi Store Admin");
        values.put("email", "admin@medistore.com");
        values.put("phone", "9876543210");
        values.put("password_hash", PasswordHasher.hash("admin123!".toCharArray()));
        values.put("salt", "");
        values.put("role", "admin");
        values.put("created_at", System.currentTimeMillis());
        db.insertWithOnConflict("users", null, values, SQLiteDatabase.CONFLICT_IGNORE);
    }

    public static void seedDefaultPincodes(SQLiteDatabase db) {
        String[][] pincodes = {
                {"400001", "Fort, Mumbai"},
                {"400002", "Kalbadevi, Mumbai"},
                {"400050", "Bandra West, Mumbai"},
                {"400051", "Bandra East, Mumbai"},
                {"400053", "Andheri West, Mumbai"},
                {"400069", "Andheri East, Mumbai"},
                {"400076", "Powai, Mumbai"},
                {"400104", "Goregaon West, Mumbai"},
                {"400703", "Vashi, Navi Mumbai"},
                {"400705", "Sanpada, Navi Mumbai"}
        };
        for (String[] p : pincodes) {
            ContentValues cv = new ContentValues();
            cv.put("pincode", p[0]);
            cv.put("area_name", p[1]);
            cv.put("is_active", 1);
            db.insertWithOnConflict("service_pincodes", null, cv, SQLiteDatabase.CONFLICT_IGNORE);
        }
    }

    public static void seedDefaultInteractions(SQLiteDatabase db) {
        String[][] interactions = {
                {"Aspirin", "Ibuprofen", "Severe", "Concurrent NSAID use significantly increases gastrointestinal bleeding risks."},
                {"Paracetamol", "Paracetamol", "Moderate", "Duplicate acetaminophen dosing increases liver toxicity hazard."},
                {"Warfarin", "Aspirin", "Severe", "Additive anticoagulant and antiplatelet effects elevate hemorrhage danger."}
        };
        for (int i = 0; i < interactions.length; i++) {
            ContentValues cv = new ContentValues();
            cv.put("id", "int_" + (i + 1));
            cv.put("generic_a", interactions[i][0]);
            cv.put("generic_b", interactions[i][1]);
            cv.put("severity", interactions[i][2]);
            cv.put("message", interactions[i][3]);
            db.insertWithOnConflict("drug_interactions", null, cv, SQLiteDatabase.CONFLICT_IGNORE);
        }
    }

    public static void seedDefaultShopSettings(SQLiteDatabase db) {
        ContentValues cv = new ContentValues();
        cv.put("id", "shop_001");
        cv.put("shop_name", "MediStore Pharmacy Ltd.");
        cv.put("address", "Shop 4, Sunrise Arcade, Bandra West, Mumbai 400050");
        cv.put("gstin", "27AAAAA0000A1Z5 (demo)");
        cv.put("dl_number", "MH-MZ4-123456 (demo)");
        cv.put("pharmacist_name", "Priya Sharma");
        cv.put("reg_number", "PHAR-MH-88492");
        cv.put("invoice_prefix", "MS");
        db.insertWithOnConflict("shop_settings", null, cv, SQLiteDatabase.CONFLICT_IGNORE);
    }
}
