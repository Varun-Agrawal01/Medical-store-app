package com.example.project.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.example.project.utils.PasswordHasher;
import com.example.project.utils.SeedLoader;

public class DBHelper extends SQLiteOpenHelper {

    public static final String DATABASE_NAME = "medistore.db";
    public static final int DATABASE_VERSION = 2;

    private final Context context;

    public DBHelper(Context context) {
        super(context.getApplicationContext(), DATABASE_NAME, null, DATABASE_VERSION);
        this.context = context.getApplicationContext();
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(true);
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

        // 3. Medicines table (includes min_age, avoid_in_pregnancy from Prompt 4)
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

        // 12. FTS4 Virtual Table for medicine search (Prompt 5)
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

        // Seed ONE default admin user as required
        seedDefaultAdmin(db);

        // Load realistic seed dataset in a single transaction with prepared statements
        SeedLoader.loadSeedData(context, db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Simple migration switch so future versions don't wipe data
        switch (oldVersion) {
            case 1:
                db.execSQL("ALTER TABLE medicines ADD COLUMN min_age INTEGER DEFAULT 0;");
                db.execSQL("ALTER TABLE medicines ADD COLUMN avoid_in_pregnancy INTEGER DEFAULT 0;");
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
        db.insert("users", null, values);
    }
}
