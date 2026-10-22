# Medi Store - Firebase Configuration and Security Rules

This document outlines the setup steps for connecting **Medi Store** to your Firebase project and enforcing Cloud Firestore Security Rules.

---

## 1. Firebase Setup Instructions

1. Go to the [Firebase Console](https://console.firebase.google.com/).
2. Click **Add project** and create a project named `Medi Store`.
3. In the project dashboard, click **Add app** and select **Android**.
4. Register the app with:
   - **Android package name:** `com.example.project`
   - **App nickname:** Medi Store
5. Download `google-services.json`.
6. Place `google-services.json` directly into the `app/` folder:
   `D:/Mad proj/project/app/google-services.json`
7. Enable **Firebase Authentication**:
   - Navigation: **Build > Authentication > Sign-in method**
   - Enable **Email/Password**.
8. Enable **Cloud Firestore**:
   - Navigation: **Build > Firestore Database**
   - Click **Create database**.
   - Choose production mode or test mode.

---

## 2. Cloud Firestore Security Rules

Copy and publish these security rules in **Cloud Firestore > Rules**:

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {

    // Helper functions
    function isAuthenticated() {
      return request.auth != null;
    }

    function isUser(userId) {
      return isAuthenticated() && request.auth.uid == userId;
    }

    function isAdmin() {
      return isAuthenticated() && 
        exists(/databases/$(database)/documents/users/$(request.auth.uid)) &&
        get(/databases/$(database)/documents/users/$(request.auth.uid)).data.role == "admin";
    }

    // Users Collection
    match /users/{userId} {
      allow read: if isAuthenticated();
      allow create: if isAuthenticated() && request.auth.uid == userId;
      allow update: if isUser(userId) && (!request.resource.data.diff(resource.data).affectedKeys().hasAny(['role']));
      allow write: if isAdmin();
    }

    // Medicines Collection (Public Read, Admin Write)
    match /medicines/{medicineId} {
      allow read: if true;
      allow write: if isAdmin();
    }

    // Symptom Categories
    match /symptomCategories/{categoryId} {
      allow read: if true;
      allow write: if isAdmin();
    }

    // Carts Collection (Customer Own Cart Only)
    match /carts/{userId} {
      allow read, write: if isUser(userId);
    }

    // Orders Collection
    match /orders/{orderId} {
      // Customer can read their own orders; Admin can read all
      allow read: if isAuthenticated() && (resource.data.userId == request.auth.uid || isAdmin());
      // Customer can create an order for themselves
      allow create: if isAuthenticated() && request.resource.data.userId == request.auth.uid;
      // Admin can update order status
      allow update: if isAdmin();
    }
  }
}
```

---

## 3. How to Test Customer and Admin Flows

### Customer Flow:
1. Launch the app (`MainActivity`).
2. Login or click **Don't have an account? Register** to create a customer account.
3. On the Home screen, browse medicines by symptom (Fever, Cold, Headache, Acidity) or search the catalogue.
4. Click any medicine to open details, adjust quantity, and tap **Add to Cart**.
5. Open the Shopping Cart, review items and subtotal, and tap **Proceed to Checkout**.
6. Enter delivery address and tap **PLACE ORDER**.
7. View the generated Tax Invoice, tap **View / Generate PDF Invoice** or **Share Invoice PDF**.
8. Go to **My Orders** from the bottom bar to view order history.

### Admin / Shopkeeper Flow:
1. On the login screen, click **Shopkeeper / Admin Login** (or launch `AdminLoginActivity`).
2. Login with Shopkeeper ID (e.g., `admin`).
3. On the Admin Dashboard, inspect key statistics:
   - Active Medicines
   - Low Stock (<10)
   - Out of Stock
   - Pending Orders
4. Tap **Manage Inventory & Stock** to view medicines, add new medicines, edit prices, or refill stock (+ or =).
5. Tap **View Customer Orders** to review orders and update order statuses (Pending -> Confirmed -> Preparing -> Completed / Cancelled).
6. When an order is cancelled, stock is automatically restored.
