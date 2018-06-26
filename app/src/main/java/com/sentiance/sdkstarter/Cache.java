package com.sentiance.sdkstarter;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.Nullable;

class Cache {
    private static final String PREF_NAME = "app";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_APP_SECRET = "app_secret";

    private Context mContext;

    Cache (Context context) {
        mContext = context;
    }

    @Nullable
    String getUserId() {
        SharedPreferences sharedPreferences = mContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return sharedPreferences.getString(KEY_USER_ID, null);
    }

    void setUserId(String userId) {
        SharedPreferences sharedPreferences = mContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        sharedPreferences.edit().putString(KEY_USER_ID, userId).apply();
    }

    @Nullable
    String getAppSecret() {
        SharedPreferences sharedPreferences = mContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return sharedPreferences.getString(KEY_APP_SECRET, null);
    }

    void setAppSecret(String secret) {
        SharedPreferences sharedPreferences = mContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        sharedPreferences.edit().putString(KEY_APP_SECRET, secret).apply();
    }
}
