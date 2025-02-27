package com.example.exercise;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
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
import com.example.exercise.databinding.ActivityMapBinding;

import java.util.ArrayList;
import java.util.List;

public class MapActivity extends FragmentActivity implements OnMapReadyCallback, LocationListener {

    private GoogleMap mMap;
    private ActivityMapBinding binding;
    private LocationManager locationManager;
    private static final int REQUEST_LOCATION_PERMISSION_CODE = 1;

    private List<Circle> circles = new ArrayList<>(); // Using a Circle List to keep all the circles displayed on the map
    private Intent serviceIntent = new Intent();
    private int session_id = -1; // Initialize session_id as a default invalid value to do checks

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMapBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Obtain the SupportMapFragment and get notified when the map is ready to be used
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        // Initialize location manager
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        // Initialize buttons from layout that we need
        Button cancelButton = findViewById(R.id.cancelButton);
        Button startButton = findViewById(R.id.startButton);

        // A click listener for each button

        // When cancel button is clicked Map Activity finishes and we get back to main activity view
        cancelButton.setOnClickListener(view -> {
            finish();
        });

        // When start button is clicked, all the circles shown on the map, are stored in the database.
        // If at least 1 circle is on map and has been stored in db, we start the service
        startButton.setOnClickListener(view -> {
            if (circles.size() > 0 && circles != null) {
                if(saveLocationsToDatabase()){
                    startLocationService();
                }
            } else {
                Toast.makeText(MapActivity.this, "You haven't set any location", Toast.LENGTH_SHORT).show();
            }
        });
    }

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

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Check and request location permission if not granted
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION_PERMISSION_CODE);
            return;
        }

        // Enable showing current location on map
        mMap.setMyLocationEnabled(true);

        // Set long click listener to add circle to the map
        mMap.setOnMapLongClickListener(latLng -> {
            addCircle(latLng);
        });

        // Set circle click listener to remove circle from the map
        mMap.setOnCircleClickListener(circle ->{
            removeCircle(circle);
        });
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

    // We don't do anything when location is changed. We just need this Method for location listener implementation
    @Override
    public void onLocationChanged(Location location) {}

    // Add circle to the map and to the List with center a LatLng location that was long clicked
    private void addCircle(LatLng latLng) {

        CircleOptions circleOptions = new CircleOptions();
        circleOptions.center(latLng);
        circleOptions.radius(100);
        circleOptions.strokeWidth(3);
        circleOptions.strokeColor(Color.RED);
        circleOptions.fillColor(Color.argb(70, 255, 0, 0));
        circleOptions.visible(true);
        circleOptions.clickable(true);

        Circle currentCircle = mMap.addCircle(circleOptions);
        circles.add(currentCircle);
    }

    // Remove circle from the map and from the List when a location inside a circle was clicked
    private void removeCircle(Circle circle) {
        circle.remove();
        circles.remove(circle);
    }

    // Save locations to database
    private boolean saveLocationsToDatabase() {

            // If circles List is not empty we start the insertion process
            if (circles != null && !circles.isEmpty()) {

                // Uri for 'circle' table
                Uri uri = Uri.withAppendedPath(MyContentProvider.CONTENT_URI, DBHelper.TABLE_CIRCLE);

                // Taking the last session id from the Database with the 'getLastSessionId()' method
                DBHelper dbHelper = new DBHelper(getApplicationContext());
                session_id = dbHelper.getLastSessionId() + 1; // We increase last session id by 1 (new session)

                if (session_id == -1){
                    return false;
                }

                // Insert each circle into the database
                for (Circle circle : circles) {

                    LatLng latLng = circle.getCenter();
                    ContentValues values = new ContentValues();

                    // Specifically we insert latitude, longitude and session_id of current (new) session
                    values.put(DBHelper.COLUMN_LATITUDE, latLng.latitude);
                    values.put(DBHelper.COLUMN_LONGITUDE, latLng.longitude);
                    values.put(DBHelper.COLUMN_SESSION_ID, session_id);

                    // Using Content Resolver to communicate with the Content Provider insert Method
                    ContentResolver contentResolver = getContentResolver();
                    contentResolver.insert(uri, values);
                }

                Toast.makeText(this, "Locations recorded!", Toast.LENGTH_SHORT).show();
                return true;
            }

        Toast.makeText(this, "Error: No location recorded!", Toast.LENGTH_SHORT).show();
        return false;
    }

    // When the insert process finishes we start service. If already a service is activated we stop it.
    private void startLocationService() {

        serviceIntent.setClass(getApplicationContext(), MyService.class);
        stopService(serviceIntent);
        startService(serviceIntent);
        Toast.makeText(MapActivity.this, "Service Started!", Toast.LENGTH_SHORT).show();
    }

}