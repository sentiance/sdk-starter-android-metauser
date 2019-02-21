package com.sentiance.sdkstarter;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.sentiance.sdk.MetaUserLinker;
import com.sentiance.sdk.OnInitCallback;
import com.sentiance.sdk.OnSdkStatusUpdateHandler;
import com.sentiance.sdk.OnStartFinishedHandler;
import com.sentiance.sdk.SdkConfig;
import com.sentiance.sdk.SdkStatus;
import com.sentiance.sdk.Sentiance;
import com.sentiance.sdk.Token;
import com.sentiance.sdk.TokenResultCallback;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class SentianceWrapper implements MetaUserLinker, OnSdkStatusUpdateHandler, OnInitCallback, OnStartFinishedHandler {

    private static final String URL = "http://www.example.com/link-user";
    private static final String TAG = "SentianceWrapper";
    private static final String APP_ID = "SENTIANCE_APP_ID";
    public static final String ACTION_SDK_STATUS_UPDATED = "com.sentiance.ACTION_SDK_STATUS_UPDATED";
    public static final String ACTION_INIT_SUCCEEDED = "com.sentiance.ACTION_INIT_SUCCEEDED";
    public static final String ACTION_INIT_FAILED = "com.sentiance.ACTION_INIT_FAILED";

    private Context mContext;
    private Cache mCache;

    public SentianceWrapper (Context context) {
        mContext = context.getApplicationContext();
        mCache = new Cache(context);
    }

    public void initializeSentianceSdk () {

        // In this sample implementation, the user's id and Sentiance secret
        // are stored in a cache after a successful login and Sentiance app secret
        // retrieval.
        // See the LoginActivity.LoginTask class for more details.

        if (mCache.getUserId() == null) {
            // Cannot initialize the SDK since the user has not logged in yet.
            return;
        }

        if (mCache.getAppSecret() == null) {
            // Cannot initialize the SDK since the app secret is missing.
            return;
        }

        // Create the config.
        SdkConfig config = new SdkConfig.Builder(APP_ID, mCache.getAppSecret(), createNotification())
                .setOnSdkStatusUpdateHandler(this)
                .setMetaUserLinker(this)  // pass your implementation of the linker here
                .build();

        // Initialize the Sentiance SDK.
        Sentiance.getInstance(mContext).init(config, this);
    }

    @Override
    public void onInitSuccess () {
        printInitSuccessLogStatements();

        // Sentiance SDK was successfully initialized, we can now start it.
        Sentiance.getInstance(mContext).start(this);
        LocalBroadcastManager.getInstance(mContext).sendBroadcast(new Intent(ACTION_INIT_SUCCEEDED));
    }

    @Override
    public void onInitFailure (OnInitCallback.InitIssue initIssue, @Nullable Throwable throwable) {
        LocalBroadcastManager.getInstance(mContext).sendBroadcast(new Intent(ACTION_INIT_FAILED));
        Log.e(TAG, "Could not initialize SDK: " + initIssue);

        switch (initIssue) {
            case INVALID_CREDENTIALS:
                Log.e(TAG, "Make sure SENTIANCE_APP_ID and SENTIANCE_SECRET are set correctly.");
                break;
            case CHANGED_CREDENTIALS:
                Log.e(TAG, "The app ID and secret have changed; this is not supported. If you meant to change the app credentials, please uninstall the app and try again.");
                break;
            case SERVICE_UNREACHABLE:
                Log.e(TAG, "The Sentiance API could not be reached. Double-check your internet connection and try again.");
                break;
            case LINK_FAILED:
                Log.e(TAG, "An issue was encountered trying to link the installation ID to the metauser.");
                break;
            case INITIALIZATION_ERROR:
                Log.e(TAG, "An unexpected exception or an error occurred during initialization.", throwable);
                break;
        }
    }

    @Override
    public void onStartFinished (SdkStatus sdkStatus) {
        Log.i(TAG, "SDK start finished with status: " + sdkStatus.startStatus);
    }

    @Override
    public void onSdkStatusUpdate (SdkStatus sdkStatus) {
        Log.i(TAG, "SDK status updated: " + sdkStatus.toString());

        // The status update is broadcast internally; this is so the other components of the app
        // (specifically MainActivity) can react on this.
        LocalBroadcastManager.getInstance(mContext).sendBroadcast(new Intent(ACTION_SDK_STATUS_UPDATED));
    }

    private Notification createNotification () {
        // PendingIntent that will start your application's MainActivity
        Intent intent = new Intent(mContext, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(mContext, 0, intent, 0);

        // On Oreo and above, you must create a notification channel
        String channelId = "trips";
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId, "Trips", NotificationManager.IMPORTANCE_MIN);
            channel.setShowBadge(false);
            NotificationManager notificationManager = (NotificationManager)mContext.getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.createNotificationChannel(channel);
        }

        return new NotificationCompat.Builder(mContext, channelId)
                .setContentTitle(mContext.getString(R.string.app_name) + " is running")
                .setContentText("Touch to open.")
                .setContentIntent(pendingIntent)
                .setShowWhen(false)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .build();
    }

    private void printInitSuccessLogStatements () {
        Log.i(TAG, "Sentiance SDK initialized, version: " + Sentiance.getInstance(mContext).getVersion());
        Log.i(TAG, "Sentiance platform user id for this install: " + Sentiance.getInstance(mContext).getUserId());
        Sentiance.getInstance(mContext).getUserAccessToken(new TokenResultCallback() {
            @Override
            public void onSuccess (Token token) {
                Log.i(TAG, "Access token to query the HTTP API: Bearer " + token.getTokenId());
                // Using this token, you can query the Sentiance API.
            }

            @Override
            public void onFailure () {
                Log.e(TAG, "Couldn't get access token");
            }
        });
    }

    @Override
    public boolean link (String installId) {
        // installId is a random ID assigned to this installation, not yet linked to any
        // user. Use installId to link this installation to your own user and to a
        // (possibly existing) Sentiance user.
        //
        // The following example sends both user IDs to a dummy server, which in turn sends
        // a linking request to the Sentiance platform. The server returns 200 after linking
        // with the Sentiance platform has succeeded.
        //
        // To obtain the Sentiance user id associated with this installation, use
        // Sentiance.getUserId() after initialization completes.
        //
        // For further details, see: https://developers.sentiance.com/docs/sdk/android/metausers

        String jsonBody = "{\"email\": \"" + mCache.getUserId() + "\", \"sentiance_id\": \"" + installId + "\"}";
        Request request = new Request.Builder()
                .url(URL)
                .header("Authorization", getAuthHeader())
                .post(RequestBody.create(MediaType.parse("application/json"), jsonBody))
                .build();

        try {
            Response response = getClient().newCall(request).execute();
            return response.isSuccessful();
        } catch (IOException e) {
            Log.e(TAG, e.getMessage() + "\n" + Log.getStackTraceString(e));
        }
        return false;
    }

    private String getAuthHeader() {
        return "Basic ICBhdXRob3JpemF0aW9uICA=";  // replace with your app's authorization token
    }

    private OkHttpClient getClient() {
        return new OkHttpClient.Builder()
                .readTimeout(60, TimeUnit.SECONDS)
                .build();
    }
}
