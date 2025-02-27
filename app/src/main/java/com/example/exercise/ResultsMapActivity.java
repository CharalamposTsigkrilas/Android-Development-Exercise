package com.example.exercise;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.example.exercise.databinding.ActivityResultsMapBinding;

import java.util.ArrayList;
import java.util.List;

public class ResultsMapActivity extends FragmentActivity implements OnMapReadyCallback, LocationListener {

    private GoogleMap mMap;
    private ActivityResultsMapBinding binding;

    private int session_id = -1; // Initialize session_id as a default invalid value to do checks
    private List<LatLng> circlesCenters = new ArrayList<>(); // Using a LatLng List to restore all the circles LatLngs from last session
    private List<EntryExitPointData> entryExitPointDataList = new ArrayList<>(); // Using a List that will help us see and separate entry and exit points on the map

    private LocationManager locationManager;
    private static final int REQUEST_LOCATION_PERMISSION_CODE = 1;

    private Intent serviceIntent = new Intent();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityResultsMapBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        // Initialize location manager
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        // Initialize buttons from layout that we need
        Button pauseButton = findViewById(R.id.pauseButton);
        Button restartButton = findViewById(R.id.restartButton);
        Button returnButton = findViewById(R.id.returnButton);

        pauseButton.setVisibility(View.VISIBLE); // Make pauseButton visible initially
        restartButton.setVisibility(View.INVISIBLE); // Make resumeButton invisible initially

        ContentResolver contentResolver = getContentResolver();

        // Initialize DBHelper and get the last session ID from the database
        DBHelper dbHelper = new DBHelper(getApplicationContext());
        session_id = dbHelper.getLastSessionId();

        // Ensure the session ID is valid
        if (session_id != -1){

            // Construct the URI for querying the content provider
            Uri uri = Uri.withAppendedPath(MyContentProvider.CONTENT_URI, DBHelper.TABLE_ENTRY_EXIT);

            // Define projection, selection, and selectionArgs for the query
            String[] projection = { DBHelper.COLUMN_LATITUDE, DBHelper.COLUMN_LONGITUDE, DBHelper.COLUMN_ENTRY_EXIT };
            String selection = DBHelper.COLUMN_SESSION_ID + " = ?";
            String[] selectionArgs = {String.valueOf(session_id)};

            // Query the ContentProvider to retrieve data based on session ID
            Cursor cursor = contentResolver.query(uri, projection, selection, selectionArgs, null);

            // Iterate through the cursor and retrieve latitude, longitude and entry-exit type to add to the list
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    double latitude = cursor.getDouble(cursor.getColumnIndexOrThrow(DBHelper.COLUMN_LATITUDE));
                    double longitude = cursor.getDouble(cursor.getColumnIndexOrThrow(DBHelper.COLUMN_LONGITUDE));
                    String entryExit = cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COLUMN_ENTRY_EXIT));

                    entryExitPointDataList.add(new EntryExitPointData(latitude, longitude, entryExit));
                }
                // Close the cursor to release resources
                cursor.close();
            }
        }

        // Ensure again that the session ID is valid
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

        // When pause button is clicked Service stops
        pauseButton.setOnClickListener(view -> {
            // Hide pauseButton
            pauseButton.setVisibility(View.INVISIBLE);
            // Show resumeButton
            restartButton.setVisibility(View.VISIBLE);

            serviceIntent.setClass(getApplicationContext(), MyService.class);
            stopService(serviceIntent);
            Toast.makeText(this, "Service paused!", Toast.LENGTH_SHORT).show();
        });

        // When restart button is clicked previous Service stops nad restarts again
        restartButton.setOnClickListener(view -> {
            // Hide resumeButton
            restartButton.setVisibility(View.INVISIBLE);
            // Show pauseButton
            pauseButton.setVisibility(View.VISIBLE);

            serviceIntent.setClass(getApplicationContext(), MyService.class);
            startService(serviceIntent);
            Toast.makeText(this, "Service restarted!", Toast.LENGTH_SHORT).show();
        });

        // When return button is clicked Map Activity finishes and we get back to main activity view
        returnButton.setOnClickListener(view -> {
            finish();
        });

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Check and request location permission if not granted
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION_PERMISSION_CODE);
            return;
        }

        // Enable showing current location on map
        mMap.setMyLocationEnabled(true);

        if(entryExitPointDataList.isEmpty() || entryExitPointDataList == null){
            Toast.makeText(this, "No Entry-Exit points recorded!", Toast.LENGTH_SHORT).show();
        }

        // For each entry-exit point we add and display a marker on the map
        for (EntryExitPointData entryExitPointData : entryExitPointDataList) {
            addMarkerToMap(entryExitPointData);
        }

        if(circlesCenters.isEmpty() || circlesCenters == null){
            Toast.makeText(this, "No circles recorded!", Toast.LENGTH_SHORT).show();
        }

        // For each circle we add and display a circle on the map
        for (LatLng circleCenter : circlesCenters){
            addCircle(circleCenter);
        }
    }

    // Display point as a marker on the map
    private void addMarkerToMap(EntryExitPointData pointData){

        LatLng entryExitPoint = new LatLng(pointData.getLatitude(), pointData.getLongitude());
        MarkerOptions markerOptions = new MarkerOptions();

        markerOptions.position(entryExitPoint);
        markerOptions.title(pointData.getEntryExit());

        mMap.addMarker(markerOptions);
    }

    // Display circle on the map
    private void addCircle(LatLng latLng) {

        CircleOptions circleOptions = new CircleOptions();
        circleOptions.center(latLng);
        circleOptions.radius(100);
        circleOptions.strokeWidth(3);
        circleOptions.strokeColor(Color.RED);
        circleOptions.fillColor(Color.argb(70, 255, 0, 0));
        circleOptions.visible(true);
        circleOptions.clickable(true);

        mMap.addCircle(circleOptions);
    }

    // We don't do anything when location is changed. We just need this Method for location listener implementation
    @Override
    public void onLocationChanged(Location location) {}

    // Request location permissions to use Google Maps, so location manager start taking the location updates
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_LOCATION_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                try {
                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);

                } catch (SecurityException e) {
                    Log.e("MapsActivity", "SecurityException: " + e.getMessage());
                }
            } else {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // Resume location updates when activity is resumed
    @Override
    protected void onResume() {
        super.onResume();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
        }
    }

    // Remove location updates when activity is paused
    @Override
    protected void onPause() {
        super.onPause();
        locationManager.removeUpdates(this);
    }

    // Remove location updates when activity is destroyed
    @Override
    protected void onDestroy(){
        super.onDestroy();
        locationManager.removeUpdates(this);
    }

    // An inside private class to keep latitude, longitude and entry-exit type
    // to separate markers displayed on the map.
    private static class EntryExitPointData {
        double latitude;
        double longitude;
        String entryExit;

        EntryExitPointData(double latitude, double longitude, String entryExit) {
            this.latitude = latitude;
            this.longitude = longitude;
            this.entryExit = entryExit;
        }

        public double getLatitude() {
            return latitude;
        }

        public double getLongitude() {
            return longitude;
        }

        public String getEntryExit() {
            return entryExit;
        }
    }
}