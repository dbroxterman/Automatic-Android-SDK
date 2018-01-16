package com.automatic.sample;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.WakefulBroadcastReceiver;
import android.util.Log;
import com.automatic.android.sdk.Automatic;
import com.automatic.net.servicebinding.ServiceEvents;

public class MyBroadcastReceiver extends WakefulBroadcastReceiver {
    public MyBroadcastReceiver() {}

    @Override
    public void onReceive(final Context context, Intent intent) {
        if (intent.getAction() != null && intent.getAction().equals(ServiceEvents.ACTION_PING)) {

            Log.i("SDKSampleApp", "Got ACTION_PING");
            if (Automatic.get().isLoggedIn()) {
                // start a service to handle binding and authentication
                Intent i = new Intent(context, DriveStateService.class);
                i.putExtra(ServiceEvents.ACTION_PING, true);
                WakefulBroadcastReceiver.startWakefulService(context, i);
            }
        }
    }
}
