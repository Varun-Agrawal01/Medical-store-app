package com.example.project;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.project.data.DBHelper;
import com.example.project.data.MediRepository;
import com.example.project.models.CartItem;
import com.example.project.models.CsvImportResult;
import com.example.project.models.DrugInteraction;
import com.example.project.models.Medicine;
import com.example.project.models.Order;
import com.example.project.models.OrderItem;
import com.example.project.models.User;
import com.example.project.utils.OrderStateMachine;
import com.example.project.utils.SeedLoader;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.*;

/**
 * Instrumented tests for MediStore using an in-memory SQLite database (passing null as DB name).
 * Tests all core database transactions and business rules with hand-made test datasets.
 */
@RunWith(AndroidJUnit4.class)
public class MediStoreInstrumentedTest {

    private Context context;
    private DBHelper inMemoryDbHelper;
    private MediRepository repository;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        // Null DB name creates an isolated in-memory SQLite database
        inMemoryDbHelper = new DBHelper(context, null);
        repository = new MediRepository(context, inMemoryDbHelper);
    }

    @After
    public void tearDown() {
        if (inMemoryDbHelper != null) {
            inMemoryDbHelper.close();
        }
    }

    // Helper to insert a minimal test medicine
    private void insertTestMedicine(String id, String name, String generic, String strength,
                                    double price, double mrp, int stock, boolean requiresRx) {
        SQLiteDatabase db = inMemoryDbHelper.getWritableDatabase();
        // No categories table in the new schema — category is derived from medicine_symptoms

        ContentValues cv = new ContentValues();
        cv.put("id", id);
        cv.put("name", name);
        cv.put("generic_name", generic);
        cv.put("manufacturer", "Test Labs");
        cv.put("strength", strength);
        cv.put("pack_size", "10 tablets");
        cv.put("dosage_form", "Tablet");
        cv.put("price", price);
        cv.put("mrp", mrp);
        cv.put("stock", stock);
        cv.put("gst_rate", 12.0);
        cv.put("requires_rx", requiresRx ? 1 : 0);
        cv.put("active", 1);
        cv.put("created_at", System.currentTimeMillis());
        cv.put("updated_at", System.currentTimeMillis());
        db.insert("medicines", null, cv);
    }

    // 1. placeOrder deducts stock correctly and rolls back everything when one item is short
    @Test
    public void testPlaceOrderDeductStockAndRollbackOnShortStock() {
        insertTestMedicine("med_1", "Dolo 650", "Paracetamol", "650mg", 30.0, 35.0, 10, false);
        insertTestMedicine("med_2", "Aspirin 75", "Aspirin", "75mg", 15.0, 20.0, 2, false);

        String userId = "user_test_1";
        // Cart has med_1 (qty 2, available) and med_2 (qty 5, SHORT because stock=2)
        List<CartItem> cartItems = new ArrayList<>();
        cartItems.add(new CartItem("med_1", "Dolo 650", 30.0, 2, false));
        cartItems.add(new CartItem("med_2", "Aspirin 75", 15.0, 5, false));

        // Attempting to place order must fail due to short stock
        boolean orderFailed = false;
        try {
            repository.placeOrderSync(userId, "John Doe", "9876543210", "123 Street", cartItems);
        } catch (Exception e) {
            orderFailed = true;
            assertTrue(e.getMessage().contains("Insufficient stock"));
        }
        assertTrue("Order must fail when any item has insufficient stock", orderFailed);

        // Verify total rollback: stock of both medicines must remain unchanged
        SQLiteDatabase db = inMemoryDbHelper.getReadableDatabase();
        try (Cursor c = db.rawQuery("SELECT stock FROM medicines WHERE id = 'med_1'", null)) {
            assertTrue(c.moveToFirst());
            assertEquals("med_1 stock must remain 10 after rollback", 10, c.getInt(0));
        }
        try (Cursor c = db.rawQuery("SELECT stock FROM medicines WHERE id = 'med_2'", null)) {
            assertTrue(c.moveToFirst());
            assertEquals("med_2 stock must remain 2 after rollback", 2, c.getInt(0));
        }

        // Verify no order rows were created
        try (Cursor c = db.rawQuery("SELECT COUNT(*) FROM orders", null)) {
            assertTrue(c.moveToFirst());
            assertEquals(0, c.getInt(0));
        }

        // Now place order with valid quantities
        List<CartItem> validCart = new ArrayList<>();
        validCart.add(new CartItem("med_1", "Dolo 650", 30.0, 2, false));
        validCart.add(new CartItem("med_2", "Aspirin 75", 15.0, 1, false));

        try {
            Order placed = repository.placeOrderSync(userId, "John Doe", "9876543210", "123 Street", validCart);
            assertNotNull(placed);
            assertEquals("Pending", placed.getStatus());
        } catch (Exception e) {
            fail("Valid order should succeed: " + e.getMessage());
        }

        // Stock must now be correctly deducted
        try (Cursor c = db.rawQuery("SELECT stock FROM medicines WHERE id = 'med_1'", null)) {
            assertTrue(c.moveToFirst());
            assertEquals("med_1 stock must be 8", 8, c.getInt(0));
        }
        try (Cursor c = db.rawQuery("SELECT stock FROM medicines WHERE id = 'med_2'", null)) {
            assertTrue(c.moveToFirst());
            assertEquals("med_2 stock must be 1", 1, c.getInt(0));
        }
    }

    // 2. Two orders placed back to back never get the same order number
    @Test
    public void testTwoOrdersBackToBackNeverGetSameOrderNumber() throws Exception {
        insertTestMedicine("med_x", "Vitamin C", "Ascorbic Acid", "500mg", 50.0, 60.0, 100, false);

        List<CartItem> cart1 = Collections.singletonList(new CartItem("med_x", "Vitamin C", 50.0, 1, false));
        List<CartItem> cart2 = Collections.singletonList(new CartItem("med_x", "Vitamin C", 50.0, 1, false));

        Order order1 = repository.placeOrderSync("usr_1", "Alice", "9876543210", "Address 1", cart1);
        Order order2 = repository.placeOrderSync("usr_2", "Bob", "9876543211", "Address 2", cart2);

        assertNotNull(order1);
        assertNotNull(order2);
        assertNotNull(order1.getOrderId());
        assertNotNull(order2.getOrderId());

        assertNotEquals("Sequential orders must have unique order numbers", order1.getOrderId(), order2.getOrderId());
        assertTrue("Order number format must be MS-YYYY-XXXXXX", order1.getOrderId().startsWith("MS-"));
        assertTrue("Order number format must be MS-YYYY-XXXXXX", order2.getOrderId().startsWith("MS-"));
    }

    // 3. cancelOrder restores stock exactly once, and cancelling twice does not restore twice
    @Test
    public void testCancelOrderRestoresStockExactlyOnce() throws Exception {
        insertTestMedicine("med_cancel", "Paracetamol", "Paracetamol", "500mg", 20.0, 25.0, 10, false);

        List<CartItem> cart = Collections.singletonList(new CartItem("med_cancel", "Paracetamol", 20.0, 4, false));
        Order order = repository.placeOrderSync("usr_c", "Charlie", "9876543210", "Street 1", cart);

        // Stock after purchase of 4 units: 10 - 4 = 6
        SQLiteDatabase db = inMemoryDbHelper.getReadableDatabase();
        try (Cursor c = db.rawQuery("SELECT stock FROM medicines WHERE id = 'med_cancel'", null)) {
            assertTrue(c.moveToFirst());
            assertEquals(6, c.getInt(0));
        }

        // Cancel order once -> restores 4 units -> stock = 10
        repository.cancelOrderSync(order.getOrderId());
        try (Cursor c = db.rawQuery("SELECT stock FROM medicines WHERE id = 'med_cancel'", null)) {
            assertTrue(c.moveToFirst());
            assertEquals("Stock should be restored to 10 after cancellation", 10, c.getInt(0));
        }

        // Cancel order second time -> should NOT restore again -> stock remains 10
        repository.cancelOrderSync(order.getOrderId());
        try (Cursor c = db.rawQuery("SELECT stock FROM medicines WHERE id = 'med_cancel'", null)) {
            assertTrue(c.moveToFirst());
            assertEquals("Stock must still be 10 and not restored twice", 10, c.getInt(0));
        }
    }

    // 4. FEFO: stock is deducted from earliest-expiring batch first, and order_items records batch used
    @Test
    public void testFefoBatchDeduction() throws Exception {
        insertTestMedicine("med_fefo", "Amoxicillin", "Amoxicillin", "500mg", 40.0, 50.0, 15, false);

        // Seed 2 batches: Batch A expires 2026-04, Batch B expires 2026-12
        SQLiteDatabase db = inMemoryDbHelper.getWritableDatabase();
        ContentValues b1 = new ContentValues();
        b1.put("id", "batch_1");
        b1.put("medicine_id", "med_fefo");
        b1.put("batch_no", "BATCH-EARLY-APR26");
        b1.put("expiry_date", "2026-04-01");
        b1.put("quantity", 5);
        db.insert("medicine_batches", null, b1);

        ContentValues b2 = new ContentValues();
        b2.put("id", "batch_2");
        b2.put("medicine_id", "med_fefo");
        b2.put("batch_no", "BATCH-LATE-DEC26");
        b2.put("expiry_date", "2026-12-01");
        b2.put("quantity", 10);
        db.insert("medicine_batches", null, b2);

        // Customer orders 3 units -> FEFO should deduct from BATCH-EARLY-APR26
        List<CartItem> cart = Collections.singletonList(new CartItem("med_fefo", "Amoxicillin", 40.0, 3, false));
        Order order = repository.placeOrderSync("usr_f", "Dave", "9876543210", "Street 2", cart);

        // Verify earliest batch reduced from 5 to 2
        try (Cursor c = db.rawQuery("SELECT quantity FROM medicine_batches WHERE batch_no = 'BATCH-EARLY-APR26'", null)) {
            assertTrue(c.moveToFirst());
            assertEquals("Earliest batch quantity should decrease by 3", 2, c.getInt(0));
        }

        // Verify later batch was untouched (remains 10)
        try (Cursor c = db.rawQuery("SELECT quantity FROM medicine_batches WHERE batch_no = 'BATCH-LATE-DEC26'", null)) {
            assertTrue(c.moveToFirst());
            assertEquals("Later batch quantity should remain 10", 10, c.getInt(0));
        }

        // Verify order_items records the batch used
        try (Cursor c = db.rawQuery("SELECT batch_no FROM order_items WHERE order_id = ?", new String[]{order.getOrderId()})) {
            assertTrue(c.moveToFirst());
            assertEquals("BATCH-EARLY-APR26", c.getString(0));
        }
    }

    // 5. An order containing a requires_rx medicine cannot be placed without a prescription row
    @Test
    public void testRequiresRxMedicineBlocksOrderWithoutPrescription() {
        insertTestMedicine("med_rx", "Amoxicillin 500", "Amoxicillin", "500mg", 45.0, 55.0, 20, true);

        String userId = "usr_rx_test";
        List<CartItem> cart = Collections.singletonList(new CartItem("med_rx", "Amoxicillin 500", 45.0, 1, true));

        // Attempt without prescription
        boolean threw = false;
        try {
            repository.placeOrderSync(userId, "Eve", "9876543210", "Street 3", cart);
        } catch (Exception e) {
            threw = true;
            assertTrue(e.getMessage().contains("Prescription is required"));
        }
        assertTrue("Order with requires_rx medicine must fail without prescription", threw);

        // Now insert valid prescription row
        SQLiteDatabase db = inMemoryDbHelper.getWritableDatabase();
        ContentValues rx = new ContentValues();
        rx.put("id", "rx_001");
        rx.put("user_id", userId);
        rx.put("file_path", "/data/prescriptions/rx1.jpg");
        rx.put("status", "Pending");
        rx.put("created_at", System.currentTimeMillis());
        db.insert("prescriptions", null, rx);

        // Place order again -> succeeds, initial status is Awaiting Rx Verification
        try {
            Order placed = repository.placeOrderSync(userId, "Eve", "9876543210", "Street 3", cart);
            assertNotNull(placed);
            assertEquals(OrderStateMachine.STATUS_AWAITING_RX, placed.getStatus());
        } catch (Exception e) {
            fail("Order with attached prescription must succeed: " + e.getMessage());
        }
    }

    // 6. Rx rejection by admin cancels order and restores stock
    @Test
    public void testRxRejectionByAdminCancelsOrderAndRestoresStock() throws Exception {
        insertTestMedicine("med_rx2", "Azithromycin 500", "Azithromycin", "500mg", 100.0, 120.0, 10, true);

        String userId = "usr_reject_test";
        SQLiteDatabase db = inMemoryDbHelper.getWritableDatabase();
        ContentValues rx = new ContentValues();
        rx.put("id", "rx_002");
        rx.put("user_id", userId);
        rx.put("file_path", "/data/prescriptions/rx2.jpg");
        rx.put("status", "Pending");
        rx.put("created_at", System.currentTimeMillis());
        db.insert("prescriptions", null, rx);

        List<CartItem> cart = Collections.singletonList(new CartItem("med_rx2", "Azithromycin 500", 100.0, 2, true));
        Order order = repository.placeOrderSync(userId, "Frank", "9876543210", "Street 4", cart);

        // Stock is now 8
        try (Cursor c = db.rawQuery("SELECT stock FROM medicines WHERE id = 'med_rx2'", null)) {
            assertTrue(c.moveToFirst());
            assertEquals(8, c.getInt(0));
        }

        // Admin rejects Rx
        repository.rejectPrescriptionSync(order.getOrderId(), "Prescription expired");

        // Verify order status cancelled
        try (Cursor c = db.rawQuery("SELECT status FROM orders WHERE id = ?", new String[]{order.getOrderId()})) {
            assertTrue(c.moveToFirst());
            assertEquals(OrderStateMachine.STATUS_CANCELLED, c.getString(0));
        }

        // Verify stock restored to 10
        try (Cursor c = db.rawQuery("SELECT stock FROM medicines WHERE id = 'med_rx2'", null)) {
            assertTrue(c.moveToFirst());
            assertEquals("Stock should be restored to 10 after Rx rejection", 10, c.getInt(0));
        }

        // Verify prescription status updated
        try (Cursor c = db.rawQuery("SELECT status, review_note FROM prescriptions WHERE order_id = ?", new String[]{order.getOrderId()})) {
            assertTrue(c.moveToFirst());
            assertEquals("Rejected", c.getString(0));
            assertEquals("Prescription expired", c.getString(1));
        }
    }

    // 7. Duplicate email registration is rejected, and wrong-password login fails
    @Test
    public void testDuplicateEmailRegistrationAndWrongPasswordLogin() throws Exception {
        repository.registerUserSync("Grace", "grace@example.com", "9876543210", "Password123", "customer");

        // Duplicate registration with same email
        boolean duplicateFailed = false;
        try {
            repository.registerUserSync("Grace Clone", "grace@example.com", "9876543210", "Password456", "customer");
        } catch (Exception e) {
            duplicateFailed = true;
            assertTrue(e.getMessage().contains("already exists"));
        }
        assertTrue("Duplicate email registration must be rejected", duplicateFailed);

        // Login with wrong password
        boolean wrongPasswordFailed = false;
        try {
            repository.loginUserSync("grace@example.com", "WrongPassword999");
        } catch (Exception e) {
            wrongPasswordFailed = true;
            assertTrue(e.getMessage().contains("Invalid email or password"));
        }
        assertTrue("Login with wrong password must fail", wrongPasswordFailed);

        // Login with correct password
        User loggedIn = repository.loginUserSync("grace@example.com", "Password123");
        assertNotNull(loggedIn);
        assertEquals("Grace", loggedIn.getName());
    }

    // 8. Symptom ranking: a medicine matching 2 selected symptoms ranks above one matching 1
    @Test
    public void testSymptomRankingRanksHigherMatchFirst() throws Exception {
        SQLiteDatabase db = inMemoryDbHelper.getWritableDatabase();

        // Symptoms
        db.execSQL("INSERT INTO symptoms(id, name) VALUES('sym_fever', 'Fever')");
        db.execSQL("INSERT INTO symptoms(id, name) VALUES('sym_headache', 'Headache')");

        // med_dual matches both Fever and Headache
        insertTestMedicine("med_dual", "Combiflam", "Ibuprofen", "400mg", 35.0, 40.0, 10, false);
        db.execSQL("INSERT INTO medicine_symptoms(medicine_id, symptom_id) VALUES('med_dual', 'sym_fever')");
        db.execSQL("INSERT INTO medicine_symptoms(medicine_id, symptom_id) VALUES('med_dual', 'sym_headache')");

        // med_single matches only Fever
        insertTestMedicine("med_single", "Paracetamol", "Paracetamol", "500mg", 20.0, 25.0, 10, false);
        db.execSQL("INSERT INTO medicine_symptoms(medicine_id, symptom_id) VALUES('med_single', 'sym_fever')");

        List<Medicine> results = repository.findMedicinesForSymptomsSync(
                Arrays.asList("sym_fever", "sym_headache"), "all", false, false);

        assertNotNull(results);
        assertTrue(results.size() >= 2);
        assertEquals("med_dual", results.get(0).getId());
        assertEquals(2, results.get(0).getMatchedSymptomsCount());
        assertEquals("med_single", results.get(1).getId());
        assertEquals(1, results.get(1).getMatchedSymptomsCount());
    }

    // 9. Substitutes query returns same generic_name and strength only, sorted by price, excluding self
    @Test
    public void testGenericSubstitutesQuery() throws Exception {
        // Target medicine
        insertTestMedicine("med_target", "Dolo 650", "Paracetamol", "650mg", 32.0, 35.0, 10, false);

        // Valid substitutes (same generic & strength, different prices)
        insertTestMedicine("med_sub1", "Calpol 650", "Paracetamol", "650mg", 26.0, 30.0, 10, false);
        insertTestMedicine("med_sub2", "P-650", "Paracetamol", "650mg", 29.0, 32.0, 10, false);

        // Invalid: different strength (500mg)
        insertTestMedicine("med_diff_str", "Crocin 500", "Paracetamol", "500mg", 18.0, 22.0, 10, false);

        // Invalid: different generic
        insertTestMedicine("med_diff_gen", "Brufen 650", "Ibuprofen", "650mg", 25.0, 30.0, 10, false);

        List<Medicine> substitutes = repository.getGenericSubstitutesSync("med_target");
        assertNotNull(substitutes);
        assertEquals("Should return exactly 2 substitutes", 2, substitutes.size());

        // Sorted by price ascending: Calpol 650 (26.0) then P-650 (29.0)
        assertEquals("med_sub1", substitutes.get(0).getId());
        assertEquals(26.0, substitutes.get(0).getPrice(), 0.001);
        assertEquals("med_sub2", substitutes.get(1).getId());
        assertEquals(29.0, substitutes.get(1).getPrice(), 0.001);

        // Target itself must be excluded
        for (Medicine m : substitutes) {
            assertNotEquals("Target medicine must not appear in its own substitutes", "med_target", m.getId());
        }
    }

    // 10. Interaction check flags a known interacting pair and does not flag a safe pair
    @Test
    public void testDrugInteractionCheck() throws Exception {
        SQLiteDatabase db = inMemoryDbHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("id", "int_1");
        cv.put("generic_a", "Aspirin");
        cv.put("generic_b", "Ibuprofen");
        cv.put("severity", "Severe");
        cv.put("message", "Concurrent NSAIDs increase GI bleeding risks.");
        db.insert("drug_interactions", null, cv);

        // Known interacting pair
        List<DrugInteraction> interactions = repository.checkCartInteractionsSync(Arrays.asList("Aspirin", "Ibuprofen"));
        assertNotNull(interactions);
        assertEquals(1, interactions.size());
        assertEquals("Severe", interactions.get(0).getSeverity());

        // Safe pair (Paracetamol + Cetirizine)
        List<DrugInteraction> safeInteractions = repository.checkCartInteractionsSync(Arrays.asList("Paracetamol", "Cetirizine"));
        assertNotNull(safeInteractions);
        assertTrue("Safe pair should have 0 interactions", safeInteractions.isEmpty());
    }

    // 11. CSV import: valid rows inserted, invalid rows rejected with reasons, whole import in one transaction
    @Test
    public void testCsvImportTransactional() throws Exception {
        String csvContent = "name,generic_name,manufacturer,strength,price,mrp,stock,requires_rx\n" +
                "Valid Med 1,Generic A,Mfg A,500mg,20.0,25.0,50,0\n" +
                "Invalid Med MRP,Generic B,Mfg B,500mg,30.0,25.0,50,0\n" + // price (30) > mrp (25) -> rejected
                "Valid Med 2,Generic C,Mfg C,250mg,15.0,20.0,10,0\n";

        CsvImportResult result = repository.importMedicinesFromCsvSync(csvContent);
        assertNotNull(result);
        assertEquals("Two valid rows should be inserted", 2, result.getInsertedCount());
        assertEquals("One invalid row should be rejected", 1, result.getRejectedCount());
        assertFalse(result.getRejectedReasons().isEmpty());
        assertTrue(result.getRejectedReasons().get(0).contains("cannot exceed MRP"));

        // Verify valid rows exist in DB
        SQLiteDatabase db = inMemoryDbHelper.getReadableDatabase();
        try (Cursor c = db.rawQuery("SELECT COUNT(*) FROM medicines WHERE name LIKE 'Valid Med%'", null)) {
            assertTrue(c.moveToFirst());
            assertEquals(2, c.getInt(0));
        }
    }

    // 12. Out-of-area pincode is rejected at checkout
    @Test
    public void testOutOfAreaPincodeRejectedAtCheckout() {
        // Seed valid pincodes
        SQLiteDatabase db = inMemoryDbHelper.getWritableDatabase();
        DBHelper.seedDefaultPincodes(db); // seeds 400001, 400050, etc.

        insertTestMedicine("med_pin", "Dolo 650", "Paracetamol", "650mg", 30.0, 35.0, 10, false);
        List<CartItem> cart = Collections.singletonList(new CartItem("med_pin", "Dolo 650", 30.0, 1, false));

        // In-area pincode (400001) -> succeeds
        try {
            Order order = repository.placeOrderSync("usr_pin", "Harry", "9876543210",
                    "Flat 101, Fort, Mumbai 400001", cart);
            assertNotNull(order);
        } catch (Exception e) {
            fail("Serviceable pincode 400001 should succeed: " + e.getMessage());
        }

        // Out-of-area pincode (999999) -> rejected
        boolean rejected = false;
        try {
            repository.placeOrderSync("usr_pin", "Harry", "9876543210",
                    "Village House, Remote Location 999999", cart);
        } catch (Exception e) {
            rejected = true;
            assertTrue(e.getMessage().contains("Delivery is not available in pincode 999999"));
        }
        assertTrue("Out-of-area pincode must be rejected at checkout", rejected);
    }

    // 13. Admin Add/Edit Medicine: validation, batch creation (FEFO), visibility & orderability
    @Test
    public void testAddMedicineValidationBatchCreationAndOrdering() throws Exception {
        SQLiteDatabase db = inMemoryDbHelper.getWritableDatabase();

        // Pre-seed a symptom so we can link the medicine to it
        ContentValues symCv = new ContentValues();
        symCv.put("id", "sym_fever");
        symCv.put("name", "Fever");
        symCv.put("red_flag_advice", "");
        db.insertWithOnConflict("symptoms", null, symCv, SQLiteDatabase.CONFLICT_IGNORE);

        // A. Attempt saving medicine with blank name — must fail validation
        Medicine invalidMed = new Medicine();
        invalidMed.setName("");
        invalidMed.setPrice(50.0);
        invalidMed.setStock(46);

        java.util.concurrent.CountDownLatch latch1 = new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.atomic.AtomicReference<String> errorRef = new java.util.concurrent.atomic.AtomicReference<>();
        repository.saveMedicine(invalidMed, new MediRepository.Callback<Void>() {
            @Override
            public void onSuccess(Void result) {
                latch1.countDown();
            }

            @Override
            public void onError(String error) {
                errorRef.set(error);
                latch1.countDown();
            }
        });
        assertTrue(latch1.await(5, java.util.concurrent.TimeUnit.SECONDS));
        assertNotNull("Saving with blank name must fail", errorRef.get());
        assertTrue("Error should mention 'name', was: " + errorRef.get(),
                errorRef.get().toLowerCase().contains("name"));

        // B. Save with valid name, price and a symptom link
        Medicine validMed = new Medicine();
        validMed.setName("dolo");
        validMed.setPrice(50.0);
        validMed.setStock(46);
        validMed.setActive(true);
        validMed.setSelectedSymptomIds(java.util.Collections.singletonList("sym_fever"));

        java.util.concurrent.CountDownLatch latch2 = new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.atomic.AtomicBoolean successBool = new java.util.concurrent.atomic.AtomicBoolean(false);
        repository.saveMedicine(validMed, new MediRepository.Callback<Void>() {
            @Override
            public void onSuccess(Void result) {
                successBool.set(true);
                latch2.countDown();
            }

            @Override
            public void onError(String error) {
                latch2.countDown();
            }
        });
        assertTrue(latch2.await(5, java.util.concurrent.TimeUnit.SECONDS));
        assertTrue("Saving valid medicine must succeed", successBool.get());

        // C. Verify batch row was created in medicine_batches
        try (Cursor c = db.rawQuery("SELECT batch_no, expiry_date, quantity FROM medicine_batches WHERE medicine_id = ?",
                new String[]{validMed.getId()})) {
            assertTrue("Batch must be created for new medicine with stock > 0", c.moveToFirst());
            String batchNo = c.getString(0);
            String expiry = c.getString(1);
            int qty = c.getInt(2);
            assertTrue("Batch no should start with INIT-, was: " + batchNo, batchNo.startsWith("INIT-"));
            assertNotNull("Expiry must be set", expiry);
            assertEquals("Batch quantity must equal medicine stock", 46, qty);
        }

        // D. Verify medicine appears in inventory
        java.util.concurrent.CountDownLatch latch3 = new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.atomic.AtomicReference<List<Medicine>> invRef = new java.util.concurrent.atomic.AtomicReference<>();
        repository.getMedicines(new MediRepository.Callback<List<Medicine>>() {
            @Override
            public void onSuccess(List<Medicine> result) {
                invRef.set(result);
                latch3.countDown();
            }

            @Override
            public void onError(String error) {
                latch3.countDown();
            }
        });
        assertTrue(latch3.await(5, java.util.concurrent.TimeUnit.SECONDS));
        boolean foundInInventory = false;
        for (Medicine m : invRef.get()) {
            if ("dolo".equalsIgnoreCase(m.getName())) {
                foundInInventory = true;
                break;
            }
        }
        assertTrue("Medicine 'dolo' must appear in inventory", foundInInventory);

        // E. Verify medicine appears in customer search
        java.util.concurrent.CountDownLatch latch4 = new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.atomic.AtomicReference<List<Medicine>> searchRef = new java.util.concurrent.atomic.AtomicReference<>();
        repository.searchMedicines("dolo", 20, 0, new MediRepository.Callback<List<Medicine>>() {
            @Override
            public void onSuccess(List<Medicine> result) {
                searchRef.set(result);
                latch4.countDown();
            }

            @Override
            public void onError(String error) {
                latch4.countDown();
            }
        });
        assertTrue(latch4.await(5, java.util.concurrent.TimeUnit.SECONDS));
        assertFalse("Search for 'dolo' must return results", searchRef.get().isEmpty());
        assertEquals("dolo", searchRef.get().get(0).getName());

        // F. Verify orderable and stock decreases
        DBHelper.seedDefaultPincodes(db);
        List<CartItem> cart = Collections.singletonList(
                new CartItem(validMed.getId(), validMed.getName(), validMed.getPrice(), 2, false)
        );
        Order placedOrder = repository.placeOrderSync("cust_dolo", "Customer", "9876543210",
                "Flat 1, Fort, Mumbai 400001", cart);
        assertNotNull(placedOrder);

        // Check medicine stock decreased by 2 -> 44
        try (Cursor c = db.rawQuery("SELECT stock FROM medicines WHERE id = ?", new String[]{validMed.getId()})) {
            assertTrue(c.moveToFirst());
            assertEquals("Medicine stock should decrease from 46 to 44", 44, c.getInt(0));
        }

        // Check batch quantity decreased by 2 -> 44
        try (Cursor c = db.rawQuery("SELECT quantity FROM medicine_batches WHERE medicine_id = ?",
                new String[]{validMed.getId()})) {
            assertTrue(c.moveToFirst());
            assertEquals("Batch quantity should decrease from 46 to 44", 44, c.getInt(0));
        }

        // G. Verify editing keeps search working
        validMed.setName("dolo 650 extra");
        java.util.concurrent.CountDownLatch latchEdit = new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.atomic.AtomicBoolean editSuccess = new java.util.concurrent.atomic.AtomicBoolean(false);
        repository.saveMedicine(validMed, new MediRepository.Callback<Void>() {
            @Override
            public void onSuccess(Void result) {
                editSuccess.set(true);
                latchEdit.countDown();
            }

            @Override
            public void onError(String error) {
                latchEdit.countDown();
            }
        });
        assertTrue(latchEdit.await(5, java.util.concurrent.TimeUnit.SECONDS));
        assertTrue("Editing medicine should succeed", editSuccess.get());

        // Search for updated name "extra"
        java.util.concurrent.CountDownLatch latch5 = new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.atomic.AtomicReference<List<Medicine>> searchUpdatedRef = new java.util.concurrent.atomic.AtomicReference<>();
        repository.searchMedicines("extra", 20, 0, new MediRepository.Callback<List<Medicine>>() {
            @Override
            public void onSuccess(List<Medicine> result) {
                searchUpdatedRef.set(result);
                latch5.countDown();
            }

            @Override
            public void onError(String error) {
                latch5.countDown();
            }
        });
        assertTrue(latch5.await(5, java.util.concurrent.TimeUnit.SECONDS));
        assertFalse("Search for updated name 'extra' must return results", searchUpdatedRef.get().isEmpty());
        assertEquals("dolo 650 extra", searchUpdatedRef.get().get(0).getName());
    }

    // 14. Fresh seeding succeeds and search for "para" finds seeded medicines
    @Test
    public void testSeedingAndSearchPara() throws Exception {
        SQLiteDatabase db = inMemoryDbHelper.getWritableDatabase();
        // Load seed data into in-memory DB
        SeedLoader.loadSeedData(context, db);

        // Verify count of medicines is 325+
        try (Cursor c = db.rawQuery("SELECT COUNT(*) FROM medicines", null)) {
            assertTrue(c.moveToFirst());
            int count = c.getInt(0);
            assertTrue("Expected 325+ seeded medicines, found: " + count, count >= 325);
        }

        // Search for "para"
        java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.atomic.AtomicReference<List<Medicine>> searchRef = new java.util.concurrent.atomic.AtomicReference<>();
        repository.searchMedicines("para", 30, 0, new MediRepository.Callback<List<Medicine>>() {
            @Override
            public void onSuccess(List<Medicine> result) {
                searchRef.set(result);
                latch.countDown();
            }

            @Override
            public void onError(String error) {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, java.util.concurrent.TimeUnit.SECONDS));
        assertNotNull(searchRef.get());
        assertFalse("Search for 'para' must return matching medicines", searchRef.get().isEmpty());
    }
}
