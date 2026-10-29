package com.example.project.utils;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Loads the realistic seed dataset into SQLite inside a single transaction using prepared statements.
 */
public final class SeedLoader {

    private static final String TAG = "SeedLoader";

    private SeedLoader() {
        // Utility class
    }

    public static void loadSeedData(Context context, SQLiteDatabase db) {
        long startTime = System.currentTimeMillis();
        db.beginTransaction();
        try {
            loadCategories(context, db);
            loadSymptoms(context, db);
            loadMedicines(context, db);
            loadMedicineSymptoms(context, db);

            db.setTransactionSuccessful();
            long elapsed = System.currentTimeMillis() - startTime;
            Log.i(TAG, "Seed data loaded successfully in " + elapsed + " ms");
        } catch (Exception e) {
            Log.e(TAG, "Failed to load seed data into SQLite", e);
        } finally {
            db.endTransaction();
        }
    }

    private static String readAssetJson(Context context, String assetPath) throws Exception {
        try (InputStream is = context.getAssets().open(assetPath)) {
            byte[] buffer = new byte[is.available()];
            int bytesRead = is.read(buffer);
            return new String(buffer, 0, bytesRead, StandardCharsets.UTF_8);
        }
    }

    private static void loadCategories(Context context, SQLiteDatabase db) {
        try {
            String jsonStr = readAssetJson(context, "seed/categories.json");
            JSONObject root = new JSONObject(jsonStr);
            JSONArray arr = root.getJSONArray("categories");

            String sql = "INSERT OR REPLACE INTO categories(id, name, description, icon_res_name) VALUES(?, ?, ?, ?);";
            try (SQLiteStatement stmt = db.compileStatement(sql)) {
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject obj = arr.getJSONObject(i);
                    stmt.clearBindings();
                    stmt.bindString(1, obj.optString("id", "cat_" + i));
                    stmt.bindString(2, obj.optString("name", ""));
                    stmt.bindString(3, obj.optString("description", ""));
                    stmt.bindString(4, obj.optString("icon_res_name", ""));
                    stmt.executeInsert();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error seeding categories", e);
        }
    }

    private static void loadSymptoms(Context context, SQLiteDatabase db) {
        try {
            String jsonStr = readAssetJson(context, "seed/symptoms.json");
            JSONObject root = new JSONObject(jsonStr);
            JSONArray arr = root.getJSONArray("symptoms");

            String sql = "INSERT OR REPLACE INTO symptoms(id, name, red_flag_advice) VALUES(?, ?, ?);";
            try (SQLiteStatement stmt = db.compileStatement(sql)) {
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject obj = arr.getJSONObject(i);
                    stmt.clearBindings();
                    stmt.bindString(1, obj.optString("id", "sym_" + i));
                    stmt.bindString(2, obj.optString("name", ""));
                    stmt.bindString(3, obj.optString("red_flag_advice", ""));
                    stmt.executeInsert();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error seeding symptoms", e);
        }
    }

    private static void loadMedicines(Context context, SQLiteDatabase db) {
        try {
            String jsonStr = readAssetJson(context, "seed/medicines.json");
            JSONObject root = new JSONObject(jsonStr);
            JSONArray arr = root.getJSONArray("medicines");
            long now = System.currentTimeMillis();

            String sql = "INSERT OR REPLACE INTO medicines(" +
                    "id, name, generic_name, manufacturer, strength, pack_size, dosage_form, " +
                    "description, usage_info, side_effects, mrp, price, gst_rate, stock, " +
                    "requires_rx, image_path, category_id, active, min_age, avoid_in_pregnancy, " +
                    "created_at, updated_at) VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);";

            String ftsSql = "INSERT INTO medicines_fts(id, name, generic_name, manufacturer) VALUES(?, ?, ?, ?);";

            try (SQLiteStatement stmt = db.compileStatement(sql);
                 SQLiteStatement ftsStmt = db.compileStatement(ftsSql)) {

                for (int i = 0; i < arr.length(); i++) {
                    JSONObject obj = arr.getJSONObject(i);
                    String medId = obj.optString("id", "med_" + (i + 1));
                    String name = obj.optString("name", "");
                    String genericName = obj.optString("generic_name", "");
                    String manufacturer = obj.optString("manufacturer", "");

                    stmt.clearBindings();
                    stmt.bindString(1, medId);
                    stmt.bindString(2, name);
                    stmt.bindString(3, genericName);
                    stmt.bindString(4, manufacturer);
                    stmt.bindString(5, obj.optString("strength", ""));
                    stmt.bindString(6, obj.optString("pack_size", ""));
                    stmt.bindString(7, obj.optString("dosage_form", ""));
                    stmt.bindString(8, obj.optString("description", ""));
                    stmt.bindString(9, obj.optString("usage_info", ""));
                    stmt.bindString(10, obj.optString("side_effects", ""));
                    stmt.bindDouble(11, obj.optDouble("mrp", 0.0));
                    stmt.bindDouble(12, obj.optDouble("price", 0.0));
                    stmt.bindDouble(13, obj.optDouble("gst_rate", 12.0));
                    stmt.bindLong(14, obj.optLong("stock", 0));
                    stmt.bindLong(15, obj.optBoolean("requires_rx", false) ? 1 : 0);
                    stmt.bindString(16, obj.optString("image_path", ""));
                    stmt.bindString(17, obj.optString("category_id", "cat_analgesics"));
                    stmt.bindLong(18, obj.optLong("active", 1));
                    stmt.bindLong(19, obj.optLong("min_age", 0));
                    stmt.bindLong(20, obj.optLong("avoid_in_pregnancy", 0));
                    stmt.bindLong(21, now);
                    stmt.bindLong(22, now);
                    stmt.executeInsert();

                    ftsStmt.clearBindings();
                    ftsStmt.bindString(1, medId);
                    ftsStmt.bindString(2, name);
                    ftsStmt.bindString(3, genericName);
                    ftsStmt.bindString(4, manufacturer);
                    ftsStmt.executeInsert();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error seeding medicines", e);
        }
    }

    private static void loadMedicineSymptoms(Context context, SQLiteDatabase db) {
        try {
            String jsonStr = readAssetJson(context, "seed/medicine_symptoms.json");
            JSONObject root = new JSONObject(jsonStr);
            JSONArray arr = root.getJSONArray("medicine_symptoms");

            String sql = "INSERT OR REPLACE INTO medicine_symptoms(medicine_id, symptom_id) VALUES(?, ?);";
            try (SQLiteStatement stmt = db.compileStatement(sql)) {
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject obj = arr.getJSONObject(i);
                    stmt.clearBindings();
                    stmt.bindString(1, obj.optString("medicine_id", ""));
                    stmt.bindString(2, obj.optString("symptom_id", ""));
                    stmt.executeInsert();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error seeding medicine_symptoms", e);
        }
    }
}
