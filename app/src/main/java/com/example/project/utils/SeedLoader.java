package com.example.project.utils;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Loads the realistic seed dataset into SQLite inside a single transaction using prepared
 * statements and insertOrThrow. Any failure rolls back the whole seed so the database is
 * never left in a half-seeded state.
 *
 * Seed order: symptoms → medicines → medicine_symptoms.
 * categories.json is intentionally ignored — category concept replaced by symptoms.
 * medicines.json's category_id field is intentionally ignored.
 */
public final class SeedLoader {

    private static final String TAG = "SeedLoader";

    private SeedLoader() {
        // Utility class — do not instantiate
    }

    /**
     * Seeds symptoms, medicines and medicine_symptoms in one atomic transaction.
     * Throws on failure so the caller (DBHelper.onCreate) can log the exact error.
     */
    public static void loadSeedData(Context context, SQLiteDatabase db) throws Exception {
        if (context == null) throw new IllegalArgumentException("context must not be null");

        long startTime = System.currentTimeMillis();
        db.beginTransaction();
        try {
            int symptomsCount = loadSymptoms(context, db);
            int medicinesCount = loadMedicines(context, db);
            int linksCount = loadMedicineSymptoms(context, db);

            db.setTransactionSuccessful();
            long elapsed = System.currentTimeMillis() - startTime;
            Log.i(TAG, "MEDDB medicines=" + medicinesCount
                    + " symptoms=" + symptomsCount
                    + " links=" + linksCount
                    + " (" + elapsed + " ms)");
        } catch (Exception e) {
            Log.e(TAG, "Seed FAILED — transaction rolled back: " + e, e);
            throw e;   // rethrow so DBHelper.onCreate can decide what to do
        } finally {
            db.endTransaction();
        }
    }

    /**
     * Re-runs seeding if the medicines table is empty.
     * Safe to call from any background thread; wraps loadSeedData's exception.
     */
    public static void seedIfEmpty(Context context, SQLiteDatabase db) {
        if (context == null || db == null) return;
        try (Cursor c = db.rawQuery("SELECT COUNT(*) FROM medicines", null)) {
            if (c != null && c.moveToFirst() && c.getInt(0) == 0) {
                Log.i(TAG, "medicines table is empty — re-seeding");
                loadSeedData(context, db);
            }
        } catch (Exception e) {
            Log.e(TAG, "seedIfEmpty failed", e);
        }
    }

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

    private static String readAssetJson(Context context, String assetPath) throws Exception {
        try (InputStream is = context.getAssets().open(assetPath);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buf = new byte[8192];
            int n;
            while ((n = is.read(buf)) != -1) {
                out.write(buf, 0, n);
            }
            return out.toString(StandardCharsets.UTF_8.name());
        }
    }

    /** @return number of symptom rows inserted */
    private static int loadSymptoms(Context context, SQLiteDatabase db) throws Exception {
        String jsonStr = readAssetJson(context, "seed/symptoms.json");
        JSONObject root = new JSONObject(jsonStr);
        JSONArray arr = root.getJSONArray("symptoms");

        // INSERT OR IGNORE so re-seeding after a partial failure does not fail on duplicates
        String sql = "INSERT OR IGNORE INTO symptoms(id, name, red_flag_advice) VALUES(?, ?, ?);";
        int count = 0;
        try (SQLiteStatement stmt = db.compileStatement(sql)) {
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                stmt.clearBindings();
                stmt.bindString(1, obj.optString("id", "sym_" + i));
                stmt.bindString(2, obj.optString("name", ""));
                stmt.bindString(3, obj.optString("red_flag_advice", ""));
                long rowId = stmt.executeInsert();
                if (rowId != -1) count++;
            }
        }
        return count;
    }

    /** @return number of medicine rows inserted */
    private static int loadMedicines(Context context, SQLiteDatabase db) throws Exception {
        String jsonStr = readAssetJson(context, "seed/medicines.json");
        JSONObject root = new JSONObject(jsonStr);
        JSONArray arr = root.getJSONArray("medicines");
        long now = System.currentTimeMillis();
        int count = 0;

        // category_id field from medicines.json is intentionally IGNORED — no such column exists.
        String sql = "INSERT OR IGNORE INTO medicines(" +
                "id, name, price, generic_name, manufacturer, strength, pack_size, dosage_form, " +
                "description, usage_info, side_effects, mrp, gst_rate, stock, " +
                "requires_rx, image_path, active, min_age, avoid_in_pregnancy, " +
                "created_at, updated_at) VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);";

        try (SQLiteStatement stmt = db.compileStatement(sql)) {
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                double price = obj.optDouble("price", 0.0);
                if (price <= 0) {
                    Log.w(TAG, "Skipping medicine at index " + i + " — price is 0 or missing");
                    continue;
                }

                stmt.clearBindings();
                stmt.bindString(1,  obj.optString("id", "med_" + (i + 1)));
                stmt.bindString(2,  obj.optString("name", ""));
                stmt.bindDouble(3,  price);
                stmt.bindString(4,  obj.optString("generic_name", ""));
                stmt.bindString(5,  obj.optString("manufacturer", ""));
                stmt.bindString(6,  obj.optString("strength", ""));
                stmt.bindString(7,  obj.optString("pack_size", ""));
                stmt.bindString(8,  obj.optString("dosage_form", "Tablet"));
                stmt.bindString(9,  obj.optString("description", ""));
                stmt.bindString(10, obj.optString("usage_info", ""));
                stmt.bindString(11, obj.optString("side_effects", ""));
                stmt.bindDouble(12, obj.optDouble("mrp", price));
                stmt.bindDouble(13, obj.optDouble("gst_rate", 12.0));
                stmt.bindLong(14,   obj.optLong("stock", 0));
                stmt.bindLong(15,   obj.optBoolean("requires_rx", false) ? 1 : 0);
                stmt.bindString(16, obj.optString("image_path", ""));
                stmt.bindLong(17,   obj.optLong("active", 1));
                stmt.bindLong(18,   obj.optLong("min_age", 0));
                stmt.bindLong(19,   obj.optLong("avoid_in_pregnancy", 0));
                stmt.bindLong(20,   now);
                stmt.bindLong(21,   now);
                long rowId = stmt.executeInsert();
                if (rowId != -1) count++;
            }
        }
        Log.i(TAG, "Seeded " + count + " medicines into database");
        return count;
    }

    /** @return number of medicine_symptom link rows inserted */
    private static int loadMedicineSymptoms(Context context, SQLiteDatabase db) throws Exception {
        String jsonStr = readAssetJson(context, "seed/medicine_symptoms.json");
        JSONObject root = new JSONObject(jsonStr);
        JSONArray arr = root.getJSONArray("medicine_symptoms");

        String sql = "INSERT OR IGNORE INTO medicine_symptoms(medicine_id, symptom_id) VALUES(?, ?);";
        int count = 0;
        try (SQLiteStatement stmt = db.compileStatement(sql)) {
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                String medId = obj.optString("medicine_id", "");
                String symId = obj.optString("symptom_id", "");
                if (medId.isEmpty() || symId.isEmpty()) continue;
                stmt.clearBindings();
                stmt.bindString(1, medId);
                stmt.bindString(2, symId);
                long rowId = stmt.executeInsert();
                if (rowId != -1) count++;
            }
        }
        return count;
    }
}
