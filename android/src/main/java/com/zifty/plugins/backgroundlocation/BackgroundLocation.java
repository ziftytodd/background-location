package com.zifty.plugins.backgroundlocation;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.net.ConnectivityManager;

import com.getcapacitor.JSObject;
import com.getcapacitor.Logger;
import com.getcapacitor.annotation.CapacitorPlugin;
import com.getcapacitor.annotation.Permission;
import com.getcapacitor.annotation.PermissionCallback;
import com.getcapacitor.PermissionState;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.android.BuildConfig;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;

import org.json.JSONObject;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

@CapacitorPlugin(
    name = "BackgroundLocation",
    permissions = {
        @Permission(
            alias = "location",
            strings = {
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            }
        ),
        @Permission(
            alias = "background-location",
            strings = { Manifest.permission.ACCESS_BACKGROUND_LOCATION }
        ),
        @Permission(
            alias = "battery",
            strings = { Manifest.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS }
        )
    }
)
public class BackgroundLocation extends Plugin {
    private PluginCall callPendingPermissions = null;
    private Boolean stoppedWithoutPermissions = false;

    @PluginMethod(returnType=PluginMethod.RETURN_CALLBACK)
    public void addWatcher(final PluginCall call) {
        if (service == null) {
            call.reject("Service not running.");
            return;
        }
        call.setKeepAlive(true);
        if (getPermissionState("location") != PermissionState.GRANTED) {
            if (call.getBoolean("requestPermissions", true)) {
                callPendingPermissions = call;
                requestPermissionForAlias("location", call, "locationPermsCallback");
            } else {
                call.reject("Permission not granted.", "NOT_AUTHORIZED");
            }
        } else {
            if (!isLocationEnabled(getContext())) {
                call.reject("Location services turned off.", "OFF");
            }
        }
        if (call.getBoolean("stale", false)) {
            LocationServices.getFusedLocationProviderClient(
                    getContext()
            ).getLastLocation().addOnSuccessListener(
                    getActivity(),
                    new OnSuccessListener<Location>() {
                        @Override
                        public void onSuccess(Location location) {
                            if (location != null) {
                                call.resolve(formatLocation(location));
                            }
                        }
                    }
            );
        }
        Notification backgroundNotification = null;
        String backgroundMessage = call.getString("backgroundMessage");
        if (backgroundMessage != null) {
            Notification.Builder builder = new Notification.Builder(getContext())
                    .setContentTitle(
                            call.getString(
                                    "backgroundTitle",
                                    "Using your location"
                            )
                    )
                    .setContentText(backgroundMessage)
                    .setOngoing(true)
                    .setPriority(Notification.PRIORITY_HIGH)
                    .setWhen(System.currentTimeMillis());

            try {
                String name = getAppString(
                        "capacitor_background_geolocation_notification_icon",
                        "mipmap/ic_launcher"
                );
                String[] parts = name.split("/");
                // It is actually necessary to set a valid icon for the notification to behave
                // correctly when tapped. If there is no icon specified, tapping it will open the
                // app's settings, rather than bringing the application to the foreground.
                builder.setSmallIcon(
                        getAppResourceIdentifier(parts[1], parts[0])
                );
            } catch (Exception e) {
                Logger.error("Could not set notification icon", e);
            }

            Intent launchIntent = getContext().getPackageManager().getLaunchIntentForPackage(
                    getContext().getPackageName()
            );
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                builder.setContentIntent(
                        PendingIntent.getActivity(
                                getContext(),
                                0,
                                launchIntent,
                                PendingIntent.FLAG_CANCEL_CURRENT
                        )
                );
            }

            // Set the Channel ID for Android O.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                builder.setChannelId(BackgroundGeolocationService.class.getPackage().getName());
            }

            backgroundNotification = builder.build();
        }
        service.addWatcher(
                call.getCallbackId(),
                backgroundNotification,
                call.getFloat("distanceFilter", 0f),
                call.getInt("minMillisBetweenUpdates", 0)
        );
    }

    @PermissionCallback
    private void locationPermsCallback(PluginCall call) {
        if (getPermissionState("location") == PermissionState.GRANTED) {
            callPendingPermissions.resolve(new JSObject().put("status", "whenInUse"));
        } else {
            callPendingPermissions.resolve(new JSObject().put("status", "denied"));
        }
    }

    @PermissionCallback
    private void backgroundlocationPermsCallback(PluginCall call) {
        if (getPermissionState("background-location") == PermissionState.GRANTED) {
            callPendingPermissions.resolve(new JSObject().put("status", "always"));
        } else {
            callPendingPermissions.resolve(new JSObject().put("status", "denied"));
        }
    }

    @Override
    protected void handleRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.handleRequestPermissionsResult(requestCode, permissions, grantResults);

        if (callPendingPermissions == null) {
            return;
        }

        for(int result : grantResults) {
            if (result == PackageManager.PERMISSION_DENIED) {
                callPendingPermissions.reject("User denied location permission", "NOT_AUTHORIZED");
                break;
            }
        }
        callPendingPermissions = null;
        if (service != null) {
            service.onPermissionsGranted();
        }
    }

    @PluginMethod()
    public void removeWatcher(PluginCall call) {
        String callbackId = call.getString("id");
        if (callbackId == null) {
            call.reject("Missing id.");
            return;
        }
        service.removeWatcher(callbackId);
        PluginCall savedCall = bridge.getSavedCall(callbackId);
        if (savedCall != null) {
            savedCall.release(bridge);
        }
        call.resolve();
    }

    @PluginMethod()
    public void openSettings(PluginCall call) {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", getContext().getPackageName(), null);
        intent.setData(uri);
        getContext().startActivity(intent);
        call.resolve();
    }

    @PluginMethod()
    public void doCheckPermissions(PluginCall call) {
        String status = "notDetermined";
        boolean available = false;
        boolean useSettingsPage = false;

        if (checkSinglePermission(Manifest.permission.ACCESS_FINE_LOCATION) &&
            checkSinglePermission(Manifest.permission.ACCESS_COARSE_LOCATION)) {
            status = "whenInUse";
            available = true;
            if (checkSinglePermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION)) {
                status = "always";
            } else if (!ActivityCompat.shouldShowRequestPermissionRationale(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION)) {
                useSettingsPage = true;
            }
        } else if (!ActivityCompat.shouldShowRequestPermissionRationale(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION)) {
            status = "denied";
            useSettingsPage = true;
        }

        // { dataSaver: string, ignoreBatteryOptimization: string }
        boolean batt = true;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PowerManager pm = (PowerManager) getContext().getSystemService(Context.POWER_SERVICE);
            if (pm.isIgnoringBatteryOptimizations(getContext().getPackageName())) {
                batt = true;
            } else {
                batt = false;
            }
        }

        boolean whitelisted = true;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            ConnectivityManager connMgr = (ConnectivityManager) getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
            switch (connMgr.getRestrictBackgroundStatus()) {
                case ConnectivityManager.RESTRICT_BACKGROUND_STATUS_ENABLED:
                    whitelisted = false;
                    // data = "false";
                    break;

                case ConnectivityManager.RESTRICT_BACKGROUND_STATUS_WHITELISTED:
                    whitelisted = true;
                    // data = "true";
                    break;

                case ConnectivityManager.RESTRICT_BACKGROUND_STATUS_DISABLED:
                    whitelisted = true;
                    // data = "disabled";
                    break;
            }
        }

        call.resolve(new JSObject()
                .put("status", status)
                .put("available", available)
                .put("useSettingsPage", useSettingsPage)
                .put("appWhitelisted", whitelisted)
                .put("ignoreBatteryOptimizations", batt)
        );
    }

    @PluginMethod()
    public void doRequestPermissions(PluginCall call) {
        callPendingPermissions = call;
        if (!(checkSinglePermission(Manifest.permission.ACCESS_FINE_LOCATION) &&
              checkSinglePermission(Manifest.permission.ACCESS_COARSE_LOCATION))) {
            requestPermissionForAlias("location", call, "locationPermsCallback");
        } else {
            requestPermissionForAlias("background-location", call, "backgroundlocationPermsCallback");
        }
    }

    @PluginMethod()
    public void doRequestWhenInUsePermission(PluginCall call) {
        if (!(checkSinglePermission(Manifest.permission.ACCESS_FINE_LOCATION) &&
                checkSinglePermission(Manifest.permission.ACCESS_COARSE_LOCATION))) {
            callPendingPermissions = call;
            requestPermissionForAlias("location", call, "locationPermsCallback");
        } else {
            call.resolve(new JSObject().put("status", "granted"));
        }
    }

    @PluginMethod()
    public void doRequestAlwaysPermission(PluginCall call) {
        if (!(checkSinglePermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION))) {
            callPendingPermissions = call;
            requestPermissionForAlias("background-location", call, "backgroundlocationPermsCallback");
        } else {
            call.resolve(new JSObject().put("status", "granted"));
        }
    }

    @PluginMethod()
    public void doRequestIgnoreBatteryOptimization(PluginCall call) {
        boolean success = false;
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Intent intent = new Intent();
                intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.setData(Uri.parse("package:" + getContext().getPackageName()));
                getContext().startActivity(intent);

                call.resolve(new JSObject().put("status", "granted"));
            }else {
                call.resolve(new JSObject().put("status", "denied"));
            }
        } catch (Exception e) {
            call.reject("Battery optimizations failed");
        }
    }

    @PluginMethod()
    public void doRequestIgnoreDataSaver(PluginCall call) {
        boolean success = false;
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                Intent intent = new Intent();
                ConnectivityManager connMgr = (ConnectivityManager) getContext().getSystemService(Context.CONNECTIVITY_SERVICE);

                intent.setAction(Settings.ACTION_IGNORE_BACKGROUND_DATA_RESTRICTIONS_SETTINGS);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.setData(Uri.parse("package:" + getContext().getPackageName()));
                getContext().startActivity(intent);

                call.resolve(new JSObject().put("status", "granted"));
            } else {
                call.resolve(new JSObject().put("status", "denied"));
            }
        } catch (Exception e) {
            call.reject("Data Saver failed");
        }
    }

    @PluginMethod()
    public void stayAwake(PluginCall call) {
        Log.d("TreadsX", "Starting stayAwake");

        final WebView wb = this.getBridge().getWebView();

        Thread thread = new Thread() {
            public void run() {
                try {
                    Thread.sleep(1000);

                    // Will this "resume timers"?
                    Log.d("TreadsX", "staying awake");
                    wb.post(new Runnable() {
                        @Override
                        public void run() {
                            Log.d("TreadsX", "Actual stayAwake");
                            wb.resumeTimers();
                            wb.dispatchWindowVisibilityChanged(View.VISIBLE);
                        }
                    });
                } catch (InterruptedException e) {
                    // do nothing
                }
            }
        };

        thread.start();

        call.resolve();
    }

    // Checks if device-wide location services are disabled
    private static Boolean isLocationEnabled(Context context)
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            LocationManager lm = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
            return lm != null && lm.isLocationEnabled();
        } else {
            return  (
                    Settings.Secure.getInt(
                            context.getContentResolver(),
                            Settings.Secure.LOCATION_MODE,
                            Settings.Secure.LOCATION_MODE_OFF
                    ) != Settings.Secure.LOCATION_MODE_OFF
            );

        }
    }

    private static JSObject formatLocation(Location location) {
        JSObject obj = new JSObject();
        obj.put("latitude", location.getLatitude());
        obj.put("longitude", location.getLongitude());
        obj.put("accuracy", location.hasAccuracy() ? location.getAccuracy() : -1);
        obj.put("altitude", location.hasAltitude() ? location.getAltitude() : -1);
        if (Build.VERSION.SDK_INT >= 26 && location.hasVerticalAccuracy()) {
            obj.put("altitudeAccuracy", location.getVerticalAccuracyMeters());
        } else {
            obj.put("altitudeAccuracy", -1);
        }
        obj.put("speed", location.hasSpeed() ? location.getSpeed() : 0);
        obj.put("bearing", location.hasBearing() ? location.getBearing() : -1);
        obj.put("heading", location.hasBearing() ? location.getBearing() : -1);
        obj.put("time", location.getTime());
        return obj;
    }

    // Sends messages to the service.
    private BackgroundGeolocationService.LocalBinder service = null;

    // Receives messages from the service.
    private class ServiceReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String id = intent.getStringExtra("id");
            PluginCall call = bridge.getSavedCall(id);
            if (call == null) {
                return;
            }
            Location location = intent.getParcelableExtra("location");
            if (location == null) {
                if (BuildConfig.DEBUG) {
                    call.error("No locations received");
                }
                return;
            }
            call.success(formatLocation(location));
        }
    }

    // Gets the identifier of the app's resource by name, returning 0 if not found.
    private int getAppResourceIdentifier(String name, String defType) {
        return getContext().getResources().getIdentifier(
                name,
                defType,
                getContext().getPackageName()
        );
    }

    // Gets a string from the app's strings.xml file, resorting to a fallback if it is not defined.
    private String getAppString(String name, String fallback) {
        int id = getAppResourceIdentifier(name, "string");
        return id == 0 ? fallback : getContext().getString(id);
    }

    @Override
    public void load() {
        super.load();

        // Android O requires a Notification Channel.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager = (NotificationManager) getContext().getSystemService(
                    Context.NOTIFICATION_SERVICE
            );
            NotificationChannel channel = new NotificationChannel(
                    BackgroundGeolocationService.class.getPackage().getName(),
                    getAppString(
                            "capacitor_background_geolocation_notification_channel_name",
                            "Background Tracking"
                    ),
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            channel.enableLights(false);
            channel.enableVibration(false);
            channel.setSound(null, null);
            manager.createNotificationChannel(channel);
        }

        this.getContext().bindService(
                new Intent(this.getContext(), BackgroundGeolocationService.class),
                new ServiceConnection() {
                    @Override
                    public void onServiceConnected(ComponentName name, IBinder binder) {
                        BackgroundLocation.this.service = (BackgroundGeolocationService.LocalBinder) binder;
                    }

                    @Override
                    public void onServiceDisconnected(ComponentName name) {
                    }
                },
                Context.BIND_AUTO_CREATE
        );

        LocalBroadcastManager.getInstance(this.getContext()).registerReceiver(
                new ServiceReceiver(),
                new IntentFilter(BackgroundGeolocationService.ACTION_BROADCAST)
        );
    }

    @Override
    protected void handleOnStart() {
        if (service != null) {
            service.onActivityStarted();
            if (stoppedWithoutPermissions && hasRequiredPermissions()) {
                service.onPermissionsGranted();
            }
        }
        super.handleOnStart();
    }

    @Override
    protected void handleOnStop() {
        if (service != null) {
            service.onActivityStopped();
        }
        stoppedWithoutPermissions = !hasRequiredPermissions();
        super.handleOnStop();
    }

    @Override
    protected void handleOnDestroy() {
        if (service != null) {
            service.stopService();
        }
        super.handleOnDestroy();
    }

    private boolean checkSinglePermission(String permission) {
        return ContextCompat.checkSelfPermission(getContext(), permission) == PackageManager.PERMISSION_GRANTED;
    }
}
