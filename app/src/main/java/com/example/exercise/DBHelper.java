package com.example.exercise;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.Nullable;

public class DBHelper extends SQLiteOpenHelper {

    // Database's name and version
    private static final String DATABASE_NAME = "geofence.db";
    private static final int DATABASE_VERSION = 1;

    // Database tables' names
    public static final String TABLE_CIRCLE = "circle";
    public static final String TABLE_ENTRY_EXIT ="entry_exit";

    // Tables columns' names
    public static final String COLUMN_SESSION_ID = "session_id";  //session_id is not a primary key
    public static final String COLUMN_LATITUDE = "latitude";
    public static final String COLUMN_LONGITUDE = "longitude";
    public static final String COLUMN_ENTRY_EXIT = "entry_or_exit";

    // Create table commands
    public static final String CREATE_TABLE_CIRCLE = createTableCircle();
    public static final String CREATE_TABLE_ENTRY_EXIT = createTableEntryExit();


    public DBHelper(@Nullable Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    // Create table 'circle' Method
    private static String createTableCircle(){
        String tableCreationToString = "CREATE TABLE " + TABLE_CIRCLE + " (" + COLUMN_SESSION_ID + " INTEGER, " + COLUMN_LATITUDE + " REAL, " + COLUMN_LONGITUDE + " REAL);";
        return tableCreationToString;
    }

    // Create table 'entry_exit' Method
    private static String createTableEntryExit() {
        String tableCreationToString = "CREATE TABLE " + TABLE_ENTRY_EXIT + " (" + COLUMN_SESSION_ID + " INTEGER, " + COLUMN_LATITUDE + " REAL, " + COLUMN_LONGITUDE + " REAL, " + COLUMN_ENTRY_EXIT + " TEXT);";
        return tableCreationToString;
    }

    // On database creation we create our 2 tables that we need
    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_CIRCLE);
        db.execSQL(CREATE_TABLE_ENTRY_EXIT);
    }

    // On database upgrade we drop tables and create new ones
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_CIRCLE);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_ENTRY_EXIT);
        onCreate(db);
    }

    // Getting the last session id that has been saved into the 'circle' table.
    // If there is nothing in the 'circle' table, we return zero '0' id as a first.
    public int getLastSessionId(){

        // Get readable database instance
        SQLiteDatabase db = this.getReadableDatabase();

        // Query for the maximum session ID in the circles table
        String query = "SELECT MAX("+COLUMN_SESSION_ID+") FROM "+TABLE_CIRCLE;
        Cursor cursor = db.rawQuery(query, null);

        int lastSessionId = 0;

        // Retrieve the last session ID from the cursor if available
        if (cursor != null && cursor.moveToFirst()) {
            lastSessionId = cursor.getInt(0);
        }

        // Close the cursor to release resources
        if (cursor != null) {
            cursor.close();
        }

        return lastSessionId;
    }
}
