# Medi Store - Android Pharmacy Management Application

Medi Store is a complete PharmEasy-style pharmacy and medicine ordering Android application built with Java, SQLite, and Material 3 DayNight theme (minSdk 24).

> [!WARNING]
> **Demo Data Disclaimer:**  
> Demo data generated for a college project. Verify against official sources before any real use. Drug details and interactions are for demonstration purposes only.

---

## Architecture & Technology Stack

- **Platform:** Native Android (minSdk 24, targetSdk 34, Java 17/21)
- **Local Database:** SQLite via `DBHelper extends SQLiteOpenHelper` (`medistore.db`)
  - Offline-first architecture: No Firebase, no Room, no external network dependencies.
  - Foreign key enforcement enabled (`PRAGMA foreign_keys=ON`).
  - Full-Text Search 4 (FTS4) virtual table (`medicines_fts`) with automated content-sync triggers for instant medicine searching with prefix matching.
  - Multi-table operations (placing orders, cancellations, stock updates) executed within strict atomic SQLite transactions (`beginTransaction()`, `setTransactionSuccessful()`, `endTransaction()`).
  - Parameterized queries throughout the application to eliminate SQL injection vulnerabilities.
- **Repository Pattern:** `MediRepository` singleton executing DB tasks on a background single-thread `ExecutorService` and dispatching callbacks on the Android main looper (`Handler(Looper.getMainLooper())`).
- **MVVM Architecture:** Jetpack `ViewModel` and `LiveData` powering reactive UI updates across customer and admin views (`HomeViewModel`, `MedicineListViewModel`, `CartViewModel`, `OrderHistoryViewModel`, `SymptomsViewModel`).
- **Lists & Diffing:** `ListAdapter` with `DiffUtil` for smooth animations and zero full-dataset rebinds.
- **Security:** Password hashing with `PBKDF2WithHmacSHA256`, 16-byte cryptographically secure random salt, and 120,000 iterations. Plaintext passwords are never stored.

---

## Default Admin Credentials

> [!IMPORTANT]
> The database automatically seeds one default administrator user on creation:
> - **Email:** `admin@medistore.com`
> - **Default Password:** `admin123!`
> 
> **SECURITY NOTICE:** This default password must be changed immediately upon first login! Log in via the Admin Portal, access the profile/security section, and update your password.

---

## Features

### 1. Authentication & Session Management
- **Registration:**
  - Validates full name, email format, Indian phone number format (`^[6-9][0-9]{9}$`), and password policy (min 8 characters with at least one letter and one number).
  - Enforces unique email constraint with user-friendly error handling.
  - Assigns role `"customer"` to all self-registered accounts.
- **Login:**
  - Verifies passwords against salted PBKDF2 hashes.
  - No backdoor credentials or auto-account creation.
- **Role Guard:**
  - All admin activities (`AdminDashboardActivity`, `InventoryActivity`, `AddEditMedicineActivity`, `AdminOrdersActivity`) enforce an active admin session check, redirecting unauthorized users or customers to the login portal.
- **Session:**
  - `SessionManager` stores only `userId` in SharedPreferences.
  - `SplashActivity` acts as the launcher: directs logged-in admins to `AdminDashboardActivity`, logged-in customers to `HomeActivity`, and unauthenticated users to `MainActivity` (Customer Login).
  - Profile screens (`ProfileActivity`, `EditProfileActivity`, `ChangePasswordActivity`) permit editing contact information and securely changing passwords.
  - Logout clears active session state and resets the activity task backstack.

### 2. Catalogue & Symptom Checker v2
- **Realistic Indian Pharmacy Catalogue:**
  - 325+ commonly sold OTC and prescription medicines across 21 therapeutic categories (Analgesics, Antipyretics, Antacids, Antibiotics, Antihistamines, Diabetes, BP, Skin, First Aid, etc.).
  - Realistic pricing, pack sizes, manufacturers, dosages, side effects, and Schedule H prescription requirement flags (`requires_rx`).
- **Symptom Checker v2:**
  - 32 detailed symptoms with clinically informed "red flag" medical warnings.
  - Multi-select searchable `ChipGroup` dynamically populated from the database.
  - Safety filters: Age group (Child < 12, Adult 12-65, Senior 65+), Pregnancy / Breastfeeding safety (`avoid_in_pregnancy`), and prescription exclusion (`requires_rx`).
  - Matches medicines and displays a badge showing matching symptom count.
- **Home Screen Integration:**
  - Horizontal carousel of popular symptoms fetched directly from SQLite. Tapping any symptom launches the symptom checker with that symptom pre-selected.

### 3. Shopping Cart, Checkout & Pricing Rules
- **Centralized Pricing (`PricingUtils`):**
  - Free delivery threshold: Free for subtotal > ₹500, else ₹50 flat delivery fee.
  - GST calculations: 5% or 12% computed per medicine according to Indian pharmaceutical tax slabs.
- **Atomic Checkout (`placeOrder`):**
  - Verifies real-time stock availability for every cart item.
  - Atomically deducts stock from `medicines` table.
  - Inserts order record into `orders`, line items into `order_items`, initial status into `order_status_history`, and clears `cart_items`.
- **Order Cancellation & Stock Restoration:**
  - Changing an order status to `"Cancelled"` automatically restocks the corresponding items back into inventory in an atomic transaction.
- **Invoices:**
  - Instant PDF generation and viewing using Android's native `PdfDocument` API.

---

## Build & Run

### Prerequisites
- Android SDK 34 (minSdk 24)
- Java 17 or Java 21 JDK configured (`JAVA_HOME`)

### Building the Project
```bash
./gradlew assembleDebug
```
The debug APK will be generated at:
```
app/build/outputs/apk/debug/app-debug.apk
```

---

## Manual Verification Steps

1. **Database Seeding & First Launch:**
   - Launch app on clean install or after clearing app storage.
   - Splash screen seeds `medistore.db` (325+ medicines, 32 symptoms, 21 categories, 1 admin account) in ~1-2 seconds.
   - Unauthenticated state redirects to `MainActivity` (Customer Login).
2. **Customer Registration & Login:**
   - Click "Register". Try invalid phone (e.g., `12345`) or weak password -> verify validation messages.
   - Register with valid Indian number (e.g., `9876543210`) and password (e.g., `Pass1234`).
   - Log in. Greeting on `HomeActivity` reflects registered user's real name.
3. **Session Persistence:**
   - Force close / kill app from task manager. Relaunch -> `SplashActivity` routes directly to `HomeActivity` without re-authenticating.
4. **Symptom Checker:**
   - Tap "Fever" on the popular symptoms carousel or open Symptom Checker from navigation.
   - Select "Fever" and "Headache". Check "Pregnant or Breastfeeding Safe Only".
   - Verify red-flag warning appears and results display filtered medicines with matching badge.
5. **Cart, Pricing & Checkout:**
   - Add medicines with subtotal < ₹500 -> verify ₹50 delivery fee is applied.
   - Add medicines with subtotal > ₹500 -> verify delivery fee is ₹0.00 (Free).
   - Proceed to Checkout -> pre-fills customer name and phone. Place order.
   - Stock is deducted immediately.
   - Navigate to "My Order History" -> newly placed order is displayed with status "Pending".
6. **Admin Portal & Role Guard:**
   - Log out from customer profile.
   - Try directly accessing any admin route or log in with non-admin credentials -> rejected.
   - Log in via Pharmacist / Admin Portal (`admin@medistore.com` / `admin123!`).
   - Admin Dashboard loads statistics, low stock counts, and orders.
   - Open Orders -> change order status from "Pending" to "Confirmed" / "Preparing" / "Completed".
   - Cancel an order -> verify inventory stock is automatically restored.
