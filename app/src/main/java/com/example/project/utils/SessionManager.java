package com.example.project.utils;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Manages the user authentication session.
 * Stores only the userId in SharedPreferences as required.
 */
public final class SessionManager {

    private static final String PREF_NAME = "medi_store_session";
    private static final String KEY_USER_ID = "user_id";

    private final SharedPreferences preferences;

    public SessionManager(Context context) {
        this.preferences = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public void saveUserId(String userId) {
        preferences.edit().putString(KEY_USER_ID, userId).apply();
    }

    public String getUserId() {
        return preferences.getString(KEY_USER_ID, null);
    }

    public boolean isLoggedIn() {
        return getUserId() != null;
    }

    public void clear() {
        preferences.edit().clear().apply();
    }
}
