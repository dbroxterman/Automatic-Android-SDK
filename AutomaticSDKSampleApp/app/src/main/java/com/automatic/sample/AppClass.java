package com.automatic.sample;

import android.app.Application;
import android.databinding.ObservableArrayList;
import com.automatic.android.sdk.Automatic;
import com.automatic.android.sdk.LogLevel;
import com.automatic.net.Scope;

/**
 * Created by duncancarroll on 5/20/15.
 */
public class AppClass extends Application {

    public static final Scope[] scopes = {
            Scope.Public, Scope.VehicleVin, Scope.Trips, Scope.Location, Scope.VehicleEvents, Scope.VehicleProfile, Scope.UserProfile, Scope.Behavior,
            Scope.AdapterBasic
    };
    // Don't ever do this =)
    public static ObservableArrayList<String> eventLog = new ObservableArrayList<>();

    @Override
    public void onCreate() {
        super.onCreate();
        // Initialize Automatic SDK
        Automatic.initialize(new Automatic.Builder(this)
                .addScopes(scopes)
                .useServiceBinding()
                .logLevel(LogLevel.Full));
        // .autoLogIn(false) for web login
    }
}
