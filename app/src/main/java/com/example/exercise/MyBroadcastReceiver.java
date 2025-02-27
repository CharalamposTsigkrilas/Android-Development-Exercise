package com.example.exercise;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.LocationManager;
import android.os.Build;
import android.widget.Toast;

public class MyBroadcastReceiver extends BroadcastReceiver {

    private Intent serviceIntent;
    private LocationManager locationManager;

    @Override
    public void onReceive(Context context, Intent intent) {
        // Check if the received intent is for provider change action
        if (intent.getAction() != null && intent.getAction().equals(LocationManager.PROVIDERS_CHANGED_ACTION)) {
            // Check if GPS is enabled
            if (isGpsEnabled(context)) {
                // If GPS is enabled, start the location tracking service
                startLocationService(context);
            } else {
                // If GPS is disabled, stop the location tracking service
                stopLocationService(context);
            }
        }
    }

    // Method to check if GPS is enabled
    private boolean isGpsEnabled(Context context) {
        locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        return locationManager != null && locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

    // Method to start the location tracking service
    private void startLocationService(Context context) {
        serviceIntent = new Intent(context, MyService.class);
        // Start foreground service for Android Oreo and above, and regular service for below Android Oreo
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent);
        } else {
            context.startService(serviceIntent);
        }
        Toast.makeText(context.getApplicationContext(), "GPS signal is enabled! Service started!", Toast.LENGTH_SHORT).show();
    }

    // Method to stop the location tracking service
    private void stopLocationService(Context context) {
        serviceIntent = new Intent(context, MyService.class);
        context.stopService(serviceIntent);
        Toast.makeText(context.getApplicationContext(), "GPS signal lost! Service stopped!", Toast.LENGTH_SHORT).show();
    }
}