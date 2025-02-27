package com.example.exercise;

import static android.content.ContentValues.TAG;

import android.Manifest;
import android.app.Activity;
import android.app.Service;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.List;

public class MyService extends Service implements LocationListener {

    // Minimum distance and time for a location update
    private static final long MIN_DISTANCE_UPDATE = 50;
    private static final long MIN_TIME_UPDATE = 5000;

    private ContentResolver contentResolver;
    private LocationManager locationManager;
    private Location lastLocation;

    private int session_id = -1; // Initialize session_id as a default invalid value to do checks
    private List<LatLng> circlesCenters = new ArrayList<>(); // Using a LatLng List to restore all the circles' LatLngs from last session
    private float CIRCLE_RADIUS = 100;  // Circle radius

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        // Initialize Content Resolver
        contentResolver = getContentResolver();

        // Initialize Location Manager
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        // Check location permission if not granted
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, MIN_TIME_UPDATE, MIN_DISTANCE_UPDATE, this);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        // Initialize DBHelper and get the last session ID from the database
        DBHelper dbHelper = new DBHelper(getApplicationContext());
        session_id = dbHelper.getLastSessionId();

        // Ensure the session ID is valid
        if (session_id != -1) {

            // Construct the URI for querying the content provider
            Uri uri = Uri.withAppendedPath(MyContentProvider.CONTENT_URI, DBHelper.TABLE_CIRCLE);

            // Define projection, selection, and selectionArgs for the query
            String[] projection = {DBHelper.COLUMN_LATITUDE, DBHelper.COLUMN_LONGITUDE};
            String selection = DBHelper.COLUMN_SESSION_ID + " = ?";
            String[] selectionArgs = {String.valueOf(session_id)};

            // Query the ContentProvider to retrieve data based on session ID
            Cursor cursor = contentResolver.query(uri, projection, selection, selectionArgs, null);

            // Iterate through the cursor and retrieve latitude and longitude to add to the list
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    double latitude = cursor.getDouble(cursor.getColumnIndexOrThrow(DBHelper.COLUMN_LATITUDE));
                    double longitude = cursor.getDouble(cursor.getColumnIndexOrThrow(DBHelper.COLUMN_LONGITUDE));

                    circlesCenters.add(new LatLng(latitude, longitude));
                }

            }
            // Close the cursor to release resources
            cursor.close();
        }
        // Return START_STICKY to indicate that the service should be restarted if it gets terminated
        return START_STICKY;
    }

    // Remove location updates when service is destroyed
    @Override
    public void onDestroy() {
        super.onDestroy();
            locationManager.removeUpdates(this);
    }


    @Override
    public void onLocationChanged(Location location) {

        // Check if the last location is null or if the time difference between
        // the current and last location is greater than or equal to 5 seconds
        if (lastLocation == null || location.getTime() - lastLocation.getTime() >= MIN_TIME_UPDATE) {

            // Calculate the distance between the last location and the current location
            float distance;
            if (lastLocation == null) {
                distance = 0;
                lastLocation = location;
            } else {
                distance = lastLocation.distanceTo(location);
            }

            // Check if the distance is greater than or equal to 50 meters to start checking
            // for entry and exit points for circles of the current session
            if (distance >= MIN_DISTANCE_UPDATE) {
                checkLocation(location);
            }
        }
    }

    private void checkLocation(Location location) {

        // Check if circlesCenters list is null or empty
        if (circlesCenters == null || circlesCenters.isEmpty()){
            return;
        }

        // Get LatLng objects for previous and current points
        LatLng previousPoint = new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude());
        LatLng currentPoint = new LatLng(location.getLatitude(), location.getLongitude());

        // Initialize flags to track if previous point was inside any circle and if current point is inside any circlee any circle
        boolean currentIsInsideCircle = false;
        boolean previousWasInsideCircle = false;

        boolean entry = false;
        boolean exit = false;

        // Iterate through circlesCenters list and check distance from previous and current points to circle centers
        for (LatLng circleCenter : circlesCenters) {

            float[] distanceForPrevious = new float[1];
            float[] distanceForCurrent = new float[1];

            // Calculate distance from previous point to circle center
            Location.distanceBetween(previousPoint.latitude, previousPoint.longitude, circleCenter.latitude, circleCenter.longitude, distanceForPrevious);
            // Calculate distance from current point to circle center
            Location.distanceBetween(currentPoint.latitude, currentPoint.longitude, circleCenter.latitude, circleCenter.longitude, distanceForCurrent);

            // Check if previous point was inside the circle
            if (distanceForPrevious[0] < CIRCLE_RADIUS) {
                previousWasInsideCircle = true;
            }

            // Check if current point is inside the circle
            if (distanceForCurrent[0] < CIRCLE_RADIUS) {
                currentIsInsideCircle = true;
            }

            // Check if the previous location was outside a circle and the current location is inside
            if (!previousWasInsideCircle && currentIsInsideCircle){

                // If so, it indicates an entry into a circle, so we break out of the loop to optimize performance
                entry = true;
                break;

            // Check if the previous location was inside a circle but the current location is outside one
            } else if (previousWasInsideCircle && !currentIsInsideCircle){

                // If so, it indicates an exit from a circle, so we break out of the loop to optimize performance
                exit = true;
                break;

            }

        }

        LatLng locationToInsert;
        String entryOrExit;

        if (entry) {
            // Previous point was outside, current point is inside - insert as entry point
            locationToInsert=currentPoint;
            entryOrExit="entry";

        }else if (exit) {
            // Previous point was inside, current point is outside - insert as exit point
            locationToInsert=previousPoint;
            entryOrExit="exit";

        } else {
            // If both points are inside or outside circles, no action is needed
            lastLocation = location;
            return;
        }

        ContentValues values = new ContentValues();

        // Put location details, session id and entry/exit type into content values
        values.put(DBHelper.COLUMN_SESSION_ID, session_id);
        values.put(DBHelper.COLUMN_LATITUDE, locationToInsert.latitude);
        values.put(DBHelper.COLUMN_LONGITUDE, locationToInsert.longitude);
        values.put(DBHelper.COLUMN_ENTRY_EXIT, entryOrExit);

        // Construct URI for inserting data into the content provider
        Uri uri = Uri.withAppendedPath(MyContentProvider.CONTENT_URI, DBHelper.TABLE_ENTRY_EXIT);
        // Insert data into the content provider
        contentResolver.insert(uri, values);

        // Update lastLocation to the current location
        lastLocation = location;
    }

}