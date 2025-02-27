package com.example.exercise;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    Intent intent = new Intent();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize buttons from layout that we need
        Button mapButton = findViewById(R.id.mapButton);
        Button stopTrackingButton = findViewById(R.id.stopTrackingButton);
        Button viewResultsButton = findViewById(R.id.viewResultsButton);


        // A click listener for each button

        // When map button is clicked Map Activity is starting
        mapButton.setOnClickListener(view -> {
            intent.setClass(getApplicationContext(), MapActivity.class);
            startActivity(intent);
        });

        // When stop tracking button is clicked Service stops
        stopTrackingButton.setOnClickListener(view -> {
            intent.setClass(getApplicationContext(), MyService.class);
            stopService(intent);
            Toast.makeText(this, "Service stopped!", Toast.LENGTH_SHORT).show();
        });

        // view results button is clicked Results Map Activity is starting
        viewResultsButton.setOnClickListener(view -> {
            intent.setClass(getApplicationContext(), ResultsMapActivity.class);
            startActivity(intent);
        });
    }

}