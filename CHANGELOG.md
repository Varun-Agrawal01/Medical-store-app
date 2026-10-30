# Changelog

All notable changes to the MediStore project are documented in this file.

## [1.0.0] - 2026-10-29

### Phase 1 to 5: Core Architecture & Symptom Checker
- Architected local offline-first SQLite database schema with foreign key constraints, indexes, and full-text search (FTS4) synchronization triggers.
- Implemented MVVM architecture with `MediRepository` background thread execution and MainThread LiveData dispatch.
- Developed Symptom Checker v2 allowing multi-symptom OTC medicine recommendations ranked by match count, in-stock status, and price, with pregnancy and pediatric filters and red-flag advisories.

### Phase 6: Navigation, Vector Assets & Accessibility
- Removed legacy raster PNG back arrows and transitioned all screen navigation to vector drawables (`ic_arrow_back.xml`).
- Added Material Symbols vector drawables (home, medicines, cart, orders, profile, search, logout, prescription).
- Replaced custom bottom bar with `BottomNavigationView` across primary customer destinations.
- Ensured minimum 48dp touch targets and dark mode color attribute compliance.

### Phase 7: Cart, Addresses & Checkout Realism
- Tied shopping cart directly to individual customer sessions in the `cart_items` table.
- Added saved address management with default address support.
- Added `service_pincodes` table for Mumbai pincodes (400001–400104 range & Navi Mumbai), blocking unserviceable deliveries.
- Replaced timestamp-based IDs with an atomic `order_counter` generating sequential numbers in `MS-YYYY-XXXXXX` format.
- Standardized phone number validation to 10-digit Indian mobile regex (`^[6-9]\d{9}$`).

### Phase 8: Prescription Enforcement & Review Queue
- Enforced mandatory prescription attachments for all `requires_rx = 1` medicines before checkout.
- Added `ActivityResultContracts` capture with internal file storage sandboxing.
- Introduced `Awaiting Rx Verification` order status.
- Added Admin Prescription Queue with transactional approval and rejection (with rejection note and stock restoration).

### Phase 9: Order Lifecycle State Machine & Tracking
- Implemented `OrderStateMachine` with strict transition rules; terminal states (`Delivered`, `Cancelled`) reject all further changes.
- Added `order_status_history` logging every status update with notes and timestamps.
- Added customer order detail timeline and order cancellation with stock restoration.
- Implemented past-order reordering workflow.

### Phase 10: Tax Invoice PDF Generation
- Upgraded `PdfGenerator` to output compliant Indian tax invoices.
- Added shop settings configuration (GSTIN, DL number, pharmacist registration).
- Calculated CGST/SGST tax split and converted grand totals into Indian numbering amount-in-words format.

### Phase 11: Admin Inventory & FEFO Batch Management
- Added `medicine_batches` table tracking batch numbers, quantities, and expiration dates.
- Implemented First-Expired, First-Out (FEFO) batch allocation in checkout transactions, stamping batch numbers on `order_items`.
- Built transactional CSV bulk import parser with row-level validation and rejection reporting.
- Added Admin Dashboard metrics for sales, pending orders, low stock (<10), out-of-stock, and near-expiry items.

### Phase 12: Differentiator Features
- Added generic substitutes engine returning same generic formulation and strength sorted by ascending price.
- Added `drug_interactions` table and real-time contraindication warning checks for shopping cart contents.

### Phase 13 & 14: Comprehensive Unit & Instrumented Testing
- Added pure JVM unit tests (`src/test`):
  - `PricingUtilsTest`: delivery fee boundary conditions (just below, at, above Rs 500), CGST/SGST split, rounding, and amount-in-words.
  - `OrderStateMachineTest`: validated all allowed transitions, terminal states, and rejection of illegal backward/forward transitions.
  - `PasswordHasherTest`: verified PBKDF2 uniqueness across random salts, correct vs. incorrect password validation, and rejection of empty/null inputs.
  - `ValidatorsTest`: tested Indian phone numbers, 6-digit postal pincodes, RFC emails, and password strength rules.
  - Rewrote legacy `MediStoreUnitTest` to invoke real production business logic.
- Added in-memory SQLite instrumented tests (`src/androidTest`):
  - `placeOrder`: validated stock deduction and complete transaction rollback upon short stock.
  - Order numbers: verified consecutive orders receive distinct sequential IDs.
  - `cancelOrder`: validated stock is restored exactly once without duplicate restorations.
  - FEFO: verified stock is deducted from the earliest-expiring batch first.
  - Prescription enforcement: verified Rx orders fail without attached prescriptions.
  - Admin rejection: verified Rx rejection moves order to Cancelled and restores inventory.
  - Duplicate email & wrong password: verified rejection.
  - Symptom ranking: verified 2 matched symptoms rank above 1 match.
  - Generic substitutes: verified query returns matching formulation/strength only, sorted by price and excluding self.
  - Interaction check: verified warnings for conflicting drug pairs and silent clearance for safe pairs.
  - CSV import: verified atomic import of valid lines and detailed rejection of invalid lines in one transaction.
  - Pincode check: verified out-of-area delivery rejection.
- Cleaned up example tests (`ExampleUnitTest`, `ExampleInstrumentedTest`).
- Added GitHub Actions CI configuration (`.github/workflows/ci.yml`).

### Phase 15: Repository Hygiene & Release Readiness
- Updated `.gitignore` to ignore build artifacts, logs, crash dumps, and IDE caches; untracked previously committed log files.
- Hardened `AndroidManifest.xml` (`android:allowBackup="false"`, single exported launcher activity, removed unused backup rules).
- Enabled R8 minification and resource shrinking for the release build type with comprehensive ProGuard keep rules.
- Documented schema migration history and safe non-destructive `onUpgrade` path in `DBHelper`.
- Resolved all Android Lint errors (0 errors).
- Added comprehensive `README.md` with ER and architecture diagrams in Mermaid.
