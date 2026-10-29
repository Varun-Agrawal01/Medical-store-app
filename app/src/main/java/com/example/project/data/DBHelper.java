package com.example.project.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.example.project.utils.PasswordHasher;
import com.example.project.utils.SeedLoader;

/**
 * SQLite Database Helper for MediStore pharmacy application.
 *
 * Schema Version History:
 * - Version 1: Initial schema comprising core tables: users, categories, medicines,
 *              symptoms, medicine_symptoms, cart_items, addresses, orders, order_items,
 *              order_status_history, prescriptions, and medicines_fts FTS4 virtual table with triggers.
 * - Version 2: Added min_age and avoid_in_pregnancy columns to medicines table to support
 *              demographic and pregnancy-safe medical filtering in the Symptom Checker.
 * - Version 3: Added medicine_batches table for FEFO inventory batch tracking,
 *              service_pincodes table for address serviceability verification,
 *              order_counter table for guaranteed unique sequential order numbers (MS-YYYY-XXXXXX),
 *              drug_interactions table for contraindication warnings in cart/checkout,
 *              shop_settings table for Indian tax invoice compliance,
 *              and batch_no column in order_items.
 */
public class DBHelper extends SQLiteOpenHelper {

    public static final String DATABASE_NAME = "medistore.db";
    public static final int DATABASE_VERSION = 3;

    private final Context context;
    private final boolean isInMemory;

    public DBHelper(Context context) {
        this(context, DATABASE_NAME);
    }

    public DBHelper(Context context, String dbName) {
        super(context != null ? context.getApplicationContext() : null, dbName, null, DATABASE_VERSION);
        this.context = context != null ? context.getApplicationContext() : null;
        this.isInMemory = (dbName == null);
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(true);
    }

    @Override
    public void onOpen(SQLiteDatabase db) {
        super.onOpen(db);
        // Fix the FTS UPDATE trigger: FTS4 external-content tables need DELETE+INSERT,
        // not a plain UPDATE statement, to keep the index accurate.
        db.execSQL("DROP TRIGGER IF EXISTS medicines_after_update");
        db.execSQL("CREATE TRIGGER medicines_after_update AFTER UPDATE ON medicines BEGIN " +
                "DELETE FROM medicines_fts WHERE id = old.id; " +
                "INSERT INTO medicines_fts(id, name, generic_name, manufacturer) " +
                "VALUES(new.id, new.name, new.generic_name, new.manufacturer); " +
                "END;");

        // Repair databases that were created but whose seeding failed (e.g. due to the
        // is.available() bug on compressed assets). This runs on every open but the
        // COUNT(*) query is cheap and seeding is skipped when rows already exist.
        if (!isInMemory && context != null) {
            SeedLoader.seedIfEmpty(context, db);
        }
    }

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

        // 2. Categories table
        db.execSQL("CREATE TABLE categories (" +
                "id TEXT PRIMARY KEY, " +
                "name TEXT NOT NULL, " +
                "description TEXT, " +
                "icon_res_name TEXT);");

        // 3. Medicines table
        db.execSQL("CREATE TABLE medicines (" +
                "id TEXT PRIMARY KEY, " +
                "name TEXT NOT NULL, " +
                "generic_name TEXT, " +
                "manufacturer TEXT, " +
                "strength TEXT, " +
                "pack_size TEXT, " +
                "dosage_form TEXT, " +
                "description TEXT, " +
                "usage_info TEXT, " +
                "side_effects TEXT, " +
                "mrp REAL, " +
                "price REAL, " +
                "gst_rate REAL, " +
                "stock INTEGER, " +
                "requires_rx INTEGER, " +
                "image_path TEXT, " +
                "category_id TEXT, " +
                "active INTEGER DEFAULT 1, " +
                "min_age INTEGER DEFAULT 0, " +
                "avoid_in_pregnancy INTEGER DEFAULT 0, " +
                "created_at INTEGER, " +
                "updated_at INTEGER, " +
                "FOREIGN KEY(category_id) REFERENCES categories(id));");

        // 4. Symptoms table
        db.execSQL("CREATE TABLE symptoms (" +
                "id TEXT PRIMARY KEY, " +
                "name TEXT NOT NULL, " +
                "red_flag_advice TEXT);");

        // 5. Medicine Symptoms junction table
        db.execSQL("CREATE TABLE medicine_symptoms (" +
                "medicine_id TEXT, " +
                "symptom_id TEXT, " +
                "PRIMARY KEY(medicine_id, symptom_id), " +
                "FOREIGN KEY(medicine_id) REFERENCES medicines(id) ON DELETE CASCADE, " +
                "FOREIGN KEY(symptom_id) REFERENCES symptoms(id) ON DELETE CASCADE);");

        // 6. Cart items table
        db.execSQL("CREATE TABLE cart_items (" +
                "id TEXT PRIMARY KEY, " +
                "user_id TEXT, " +
                "medicine_id TEXT, " +
                "quantity INTEGER, " +
                "FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE, " +
                "FOREIGN KEY(medicine_id) REFERENCES medicines(id) ON DELETE CASCADE);");

        // 7. Addresses table
        db.execSQL("CREATE TABLE addresses (" +
                "id TEXT PRIMARY KEY, " +
                "user_id TEXT, " +
                "label TEXT, " +
                "line1 TEXT, " +
                "line2 TEXT, " +
                "pincode TEXT, " +
                "is_default INTEGER DEFAULT 0, " +
                "FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE);");

        // 8. Orders table
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

        // 9. Order items table
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

        // 10. Order status history table
        db.execSQL("CREATE TABLE order_status_history (" +
                "id TEXT PRIMARY KEY, " +
                "order_id TEXT, " +
                "status TEXT, " +
                "note TEXT, " +
                "changed_at INTEGER, " +
                "FOREIGN KEY(order_id) REFERENCES orders(id) ON DELETE CASCADE);");

        // 11. Prescriptions table
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

        // 12. Medicine Batches table for FEFO inventory management
        createBatchesTable(db);

        // 13. Service Pincodes table
        createServicePincodesTable(db);

        // 14. Order Counter table for unique order numbers
        createOrderCounterTable(db);

        // 15. Drug Interactions table
        createDrugInteractionsTable(db);

        // 16. Shop Settings table
        createShopSettingsTable(db);

        // 17. FTS4 Virtual Table for medicine search
        db.execSQL("CREATE VIRTUAL TABLE medicines_fts USING fts4(" +
                "content=\"medicines\", id, name, generic_name, manufacturer);");

        // Triggers to keep FTS table in sync
        db.execSQL("CREATE TRIGGER medicines_after_insert AFTER INSERT ON medicines BEGIN " +
                "INSERT INTO medicines_fts(id, name, generic_name, manufacturer) " +
                "VALUES(new.id, new.name, new.generic_name, new.manufacturer); " +
                "END;");

        db.execSQL("CREATE TRIGGER medicines_after_update AFTER UPDATE ON medicines BEGIN " +
                "UPDATE medicines_fts SET name = new.name, generic_name = new.generic_name, manufacturer = new.manufacturer " +
                "WHERE id = old.id; " +
                "END;");

        db.execSQL("CREATE TRIGGER medicines_after_delete AFTER DELETE ON medicines BEGIN " +
                "DELETE FROM medicines_fts WHERE id = old.id; " +
                "END;");

        // Indexes for performance
        db.execSQL("CREATE INDEX idx_medicines_name ON medicines(name);");
        db.execSQL("CREATE INDEX idx_medicines_category ON medicines(category_id);");
        db.execSQL("CREATE INDEX idx_medicines_active ON medicines(active);");
        db.execSQL("CREATE INDEX idx_cart_user ON cart_items(user_id);");
        db.execSQL("CREATE INDEX idx_orders_user ON orders(user_id);");
        db.execSQL("CREATE INDEX idx_orders_created ON orders(created_at);");
        db.execSQL("CREATE INDEX idx_order_items_order ON order_items(order_id);");
        db.execSQL("CREATE INDEX idx_order_history_order ON order_status_history(order_id);");
        db.execSQL("CREATE INDEX idx_med_sym_sym ON medicine_symptoms(symptom_id);");
        db.execSQL("CREATE INDEX idx_batches_med ON medicine_batches(medicine_id);");

        // If running in production (persistent DB), seed default admin and initial catalog
        if (!isInMemory && context != null) {
            seedDefaultAdmin(db);
            seedDefaultPincodes(db);
            seedDefaultInteractions(db);
            seedDefaultShopSettings(db);
            SeedLoader.loadSeedData(context, db);
        }
    }

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

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Safe incremental migrations - NO DROP TABLE statements to protect user data
        switch (oldVersion) {
            case 1:
                db.execSQL("ALTER TABLE medicines ADD COLUMN min_age INTEGER DEFAULT 0;");
                db.execSQL("ALTER TABLE medicines ADD COLUMN avoid_in_pregnancy INTEGER DEFAULT 0;");
                // fall through to version 2 upgrade
            case 2:
                createBatchesTable(db);
                createServicePincodesTable(db);
                createOrderCounterTable(db);
                createDrugInteractionsTable(db);
                createShopSettingsTable(db);
                try {
                    db.execSQL("ALTER TABLE order_items ADD COLUMN batch_no TEXT;");
                } catch (Exception ignored) {
                    // Column may already exist
                }
                break;
            default:
                break;
        }
    }

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
        // Mumbai pincodes 400001 to 400104 range and Navi Mumbai
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
