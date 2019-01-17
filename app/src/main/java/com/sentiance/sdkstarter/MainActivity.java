package com.sentiance.sdkstarter;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.text.format.Formatter;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.sentiance.sdk.InitState;
import com.sentiance.sdk.SdkStatus;
import com.sentiance.sdk.Sentiance;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private final BroadcastReceiver statusUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive (Context context, Intent intent) {
            refreshStatus();
        }
    };

    private ListView statusList;
    private boolean permCheckPostponed;

    @Override
    protected void onCreate (Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (new Cache(this).getUserId() == null) {
            startActivity(new Intent(this, LoginActivity.class));
            permCheckPostponed = true;
        }
        else {
            permCheck();
        }

        setContentView(R.layout.activity_main);

        statusList = (ListView)findViewById(R.id.statusList);
    }

    private void permCheck() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            startActivity(new Intent(this, PermissionCheckActivity.class));
        }
    }

    @Override
    protected void onResume () {
        super.onResume();

        if (!permCheckPostponed) {
            permCheck();
        }
        permCheckPostponed = false;

        // Register a receiver so we are notified by MyApplication when the Sentiance SDK status was updated.
        LocalBroadcastManager.getInstance(this).registerReceiver(statusUpdateReceiver, new IntentFilter(SentianceWrapper.ACTION_SDK_STATUS_UPDATED));
        refreshStatus();
    }

    @Override
    protected void onPause () {
        super.onPause();

        LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(statusUpdateReceiver);
    }

    private void refreshStatus () {
        List<String> statusItems = new ArrayList<>();

        if (Sentiance.getInstance(this).getInitState() == InitState.INITIALIZED) {
            statusItems.add("SDK version: " + Sentiance.getInstance(this).getVersion());
            statusItems.add("User ID: " + Sentiance.getInstance(this).getUserId());

            SdkStatus sdkStatus = Sentiance.getInstance(this).getSdkStatus();

            statusItems.add("Start status: " + sdkStatus.startStatus.name());
            statusItems.add("Can detect: " + String.valueOf(sdkStatus.canDetect));
            statusItems.add("Remote enabled: " + String.valueOf(sdkStatus.isRemoteEnabled));
            statusItems.add("Location perm granted: " + String.valueOf(sdkStatus.isLocationPermGranted));
            statusItems.add("Location setting: " + sdkStatus.locationSetting.name());

            statusItems.add(formatQuota("Wi-Fi", sdkStatus.wifiQuotaStatus, Sentiance.getInstance(this).getWiFiQuotaUsage(), Sentiance.getInstance(this).getWiFiQuotaLimit()));
            statusItems.add(formatQuota("Mobile data", sdkStatus.mobileQuotaStatus, Sentiance.getInstance(this).getMobileQuotaUsage(), Sentiance.getInstance(this).getMobileQuotaLimit()));
            statusItems.add(formatQuota("Disk", sdkStatus.diskQuotaStatus, Sentiance.getInstance(this).getDiskQuotaUsage(), Sentiance.getInstance(this).getDiskQuotaLimit()));
        }
        else {
            statusItems.add("SDK not initialized");
        }

        statusList.setAdapter(new ArrayAdapter<>(this, R.layout.list_item_status, R.id.textView, statusItems));
    }

    private String formatQuota (String name, SdkStatus.QuotaStatus status, long bytesUsed, long bytesLimit) {
        return String.format(Locale.US, "%s quota: %s / %s (%s)",
                name,
                Formatter.formatShortFileSize(this, bytesUsed),
                Formatter.formatShortFileSize(this, bytesLimit),
                status.name());
    }

}
