package com.sentiance.sdkstarter;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

public class LoginActivity extends AppCompatActivity implements View.OnClickListener {

    EditText externalId;
    Button login;

    BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive (Context context, Intent intent) {
            if (intent.getAction().equals(SentianceWrapper.ACTION_INIT_SUCCEEDED)) {
                finish();
            } else {
                Toast.makeText(LoginActivity.this, "SDK init failed", Toast.LENGTH_SHORT).show();
                login.setText("Login");
                login.setEnabled(true);
            }
        }
    };

    @Override
    protected void onCreate (Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        externalId = findViewById(R.id.externalId);
        login = findViewById(R.id.login);

        login.setOnClickListener(this);

        LocalBroadcastManager.getInstance(this).registerReceiver(receiver,
                new IntentFilter(SentianceWrapper.ACTION_INIT_FAILED));
        LocalBroadcastManager.getInstance(this).registerReceiver(receiver,
                new IntentFilter(SentianceWrapper.ACTION_INIT_SUCCEEDED));
    }

    @Override
    public void onClick (View v) {
        if (TextUtils.isEmpty(externalId.getText().toString())) {
            return;
        }

        login.setEnabled(false);
        login.setText("Logging in");

        new LoginTask().execute((Void[])null);
    }

    @Override
    protected void onDestroy () {
        super.onDestroy();

        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver);
    }

    /**
     * This class simulates a login request that returns the Sentiance app
     * secret. In your own app, make sure you take proper security measures
     * to prevent exposing the secret (e.g. secure connection, certificate
     * pinning, Android SafetyNet).
     */
    private class LoginTask extends AsyncTask<Void, Void, Boolean> {

        String extId = "";

        @Override
        protected void onPreExecute () {
            extId = externalId.getText().toString();
        }

        @Override
        protected Boolean doInBackground (Void... voids) {
            String secret = null;

            try {
                Thread.sleep(3000);  // Simulate login request
                secret = "SENTIANCE_APP_SECRET_YOU_RECEIVED";
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            if (secret != null) {
                Cache cache = new Cache(LoginActivity.this);

                // Save the user id so we can initialize the Sentiance SDK.
                cache.setUserId(extId);

                // Save the app secret. This example stores the secret as is. In your
                // own app, make sure the secret is stored in a secure manner (e.g.
                // encrypted with the Android Keystore system).
                cache.setAppSecret(secret);
            }

            return true;
        }

        @Override
        protected void onPostExecute (Boolean success) {
            if (success) {
                new SentianceWrapper(LoginActivity.this).initializeSentianceSdk();
            }
            else {
                Toast.makeText(LoginActivity.this, "Login failed", Toast.LENGTH_SHORT).show();
                login.setEnabled(true);
                login.setText("Login");
            }
        }
    }
}
