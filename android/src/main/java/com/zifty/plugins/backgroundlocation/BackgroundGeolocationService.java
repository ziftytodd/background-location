package com.zifty.plugins.backgroundlocation;

import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.os.Binder;
import android.os.IBinder;

import com.getcapacitor.Logger;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationAvailability;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import java.util.HashSet;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

// A bound and started service that is promoted to a foreground service when
// location updates have been requested and the main activity is stopped.
//
// When an activity is bound to this service, frequent location updates are
// permitted. When the activity is removed from the foreground, the service
// promotes itself to a foreground service, and location updates continue. When
// the activity comes back to the foreground, the foreground service stops, and
// the notification associated with that service is removed.
public class BackgroundGeolocationService extends Service {
    static final String ACTION_BROADCAST = (
            BackgroundGeolocationService.class.getPackage().getName() + ".broadcast"
    );
    private final IBinder binder = new LocalBinder();

    // Must be unique for this application.
    private static final int NOTIFICATION_ID = 28351;

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    private class Watcher {
        public FusedLocationProviderClient client;
        public LocationRequest locationRequest;
        public LocationCallback locationCallback;
        public Notification backgroundNotification;
        public int minMillis;
    }

    private Watcher mainWatcher;

    Notification getNotification() {
        if ((mainWatcher != null) && (mainWatcher.backgroundNotification != null)) {
            return mainWatcher.backgroundNotification;
        }
        return null;
    }

    // Handles requests from the activity.
    public class LocalBinder extends Binder {
        void startWatcher(
                Notification backgroundNotification,
                float distanceFilter,
                final int minMillis
        ) {
            if (mainWatcher != null) return;

            FusedLocationProviderClient client = LocationServices.getFusedLocationProviderClient(
                BackgroundGeolocationService.this
            );
            LocationRequest locationRequest = new LocationRequest();
            locationRequest.setMaxWaitTime(1000);
            locationRequest.setInterval(Math.max(minMillis, 1000));
            locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
            locationRequest.setSmallestDisplacement(distanceFilter);

            LocationCallback callback = new LocationCallback(){
                private long lastTime = 0;

                @Override
                public void onLocationResult(LocationResult locationResult) {
                    Location location = locationResult.getLastLocation();
                    long time = System.currentTimeMillis();
                    location.setTime(time);
                    if ((lastTime == 0) || ((time - lastTime) > minMillis)) {
                        lastTime = time;
                        Intent intent = new Intent(ACTION_BROADCAST);
                        intent.putExtra("location", location);
                        LocalBroadcastManager.getInstance(
                                getApplicationContext()
                        ).sendBroadcast(intent);
                    }
                }
                @Override
                public void onLocationAvailability(LocationAvailability availability) {
                    if (!availability.isLocationAvailable()) {
                        Logger.debug("Location not available");
                    }
                }
            };

            mainWatcher = new Watcher();
            mainWatcher.client = client;
            mainWatcher.locationRequest = locationRequest;
            mainWatcher.locationCallback = callback;
            mainWatcher.backgroundNotification = backgroundNotification;
            mainWatcher.minMillis = minMillis;

            mainWatcher.client.requestLocationUpdates(
                    mainWatcher.locationRequest,
                    mainWatcher.locationCallback,
                    null
            );
        }

        void stopWatcher() {
            if (mainWatcher != null) {
                mainWatcher.client.removeLocationUpdates(mainWatcher.locationCallback);
                mainWatcher = null;
                if (getNotification() == null) {
                    stopForeground(true);
                }
            }
        }

        void onPermissionsGranted() {
            // If permissions were granted while the app was in the background, for example in
            // the Settings app, the watchers need restarting.
            if (mainWatcher != null) {
                mainWatcher.client.removeLocationUpdates(mainWatcher.locationCallback);
                mainWatcher.client.requestLocationUpdates(
                        mainWatcher.locationRequest,
                        mainWatcher.locationCallback,
                        null
                );
            }
        }

        void onActivityStarted() {
            stopForeground(true);
        }

        void onActivityStopped() {
            Notification notification = getNotification();
            if (notification != null) {
                startForeground(NOTIFICATION_ID, notification);
            }
        }

        void stopService() {
            BackgroundGeolocationService.this.stopSelf();
        }
    }
}
