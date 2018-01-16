package com.automatic.sample;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import com.automatic.android.sdk.Automatic;
import com.automatic.android.sdk.SdkError;
import com.automatic.android.sdk.ServiceBindingCallback;
import com.automatic.android.sdk.events.ConnectionStateListener;
import com.automatic.android.sdk.events.DriveStateListener;
import com.automatic.android.sdk.events.IgnitionEventListener;
import com.automatic.net.events.IgnitionEvent;
import com.automatic.net.servicebinding.DriveState;
import com.automatic.net.servicebinding.ServiceEvents;

public class DriveStateService extends Service implements DriveStateListener, ConnectionStateListener, IgnitionEventListener {

    private Automatic mAutomaticSdkInstance;

    public DriveStateService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        AppClass.eventLog.add("DriveStateService.onCreate()");
        mAutomaticSdkInstance = Automatic.get();
        mAutomaticSdkInstance.addDriveStateListener(this);
        mAutomaticSdkInstance.addConnectionStateListener(this);
        mAutomaticSdkInstance.addIgnitionEventListener(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        AppClass.eventLog.add("DriveStateService.onDestroy()");
        mAutomaticSdkInstance.removeDriveStateListener(this);
        mAutomaticSdkInstance.removeConnectionStateListener(this);
        mAutomaticSdkInstance.removeIgnitionEventListener(this);
    }

    @Override
    public void onDriveStateChanged(DriveState driveState) {
        AppClass.eventLog.add("* onDriveStateChanged: " + driveState);
        postNotification("Drive State Changed", "State is now: " + driveState);
    }

    @Override
    public void onDisconnect() {
        AppClass.eventLog.add("onDisconnect()");
    }

    @Override
    public void onConnect() {
        AppClass.eventLog.add("onConnect()");
    }

    @Override
    public void onLogout() {
        AppClass.eventLog.add("onLogout()");
    }

    @Override
    public void onIgnitionEvent(IgnitionEvent ignitionEvent) {
        String ignitionState = (ignitionEvent.ignitionStatus) ? "on" : "off";
        AppClass.eventLog.add("* onIgnitonEvent: " + ignitionState);
        postNotification("Ignition " + ignitionState.toUpperCase(), "Ignition is now " + ignitionState);
    }

    @Override
    public int onStartCommand(final Intent intent, int flags, int startId) {
        if (intent != null) {
            if (intent.hasExtra(ServiceEvents.ACTION_PING)) {
                Automatic.get().bindService(new ServiceBindingCallback() {
                    @Override
                    public void onBindingResponse(boolean success, SdkError sdkError) {
                        // talk to the car!

                        // release wake lock whenever you are done
                        MyBroadcastReceiver.completeWakefulIntent(intent);
                    }
                });
            }
        }
        return START_STICKY;
    }

    public void postNotification(String title, String message) {
        NotificationManager noteManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        Notification.Builder nb = new Notification.Builder(this);

        nb.setContentTitle(title)
                .setContentText(message)
                .setSmallIcon(com.automatic.android.sdk.R.mipmap.automatic_button_white)
                .setOngoing(false)
                .setAutoCancel(true)
                .setDefaults(Notification.DEFAULT_SOUND)
                .setTicker(title);

        noteManager.notify(5384, nb.build());
    }
}
