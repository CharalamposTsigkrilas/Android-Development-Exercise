package com.example.exercise;

import android.annotation.SuppressLint;
import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.List;

public class MyContentProvider extends ContentProvider {

    // All Uri variables we need for each table access
    public static final String AUTHORITY = "com.example.exercise";
    public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY);
    private UriMatcher uriMatcher;
    public static final int CIRCLES_URI_CODE = 1;
    public static final int ENTRIES_EXITS_URI_CODE = 2;

    // Also a DBHelper we use for our provider methods
    private DBHelper dbHelper;

    // On Content Provider creation we match all the Uris for each table in our database.
    // Also we initialize our DBHelper object and return if our database has been initialized
    @Override
    public boolean onCreate() {
        uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        uriMatcher.addURI(AUTHORITY, DBHelper.TABLE_CIRCLE, CIRCLES_URI_CODE);
        uriMatcher.addURI(AUTHORITY, DBHelper.TABLE_ENTRY_EXIT, ENTRIES_EXITS_URI_CODE);

        dbHelper = new DBHelper(getContext().getApplicationContext());

        // Get writable database instance
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        return db != null;
    }

    // Our query method. We return a cursor that shows
    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection,
                        @Nullable String[] selectionArgs, @Nullable String sortOrder) {
        // Get readable database instance
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor;

        // Determine the URI type and perform appropriate query
        switch (uriMatcher.match(uri)) {

            case CIRCLES_URI_CODE:

                // Query circles table
                cursor = db.query(DBHelper.TABLE_CIRCLE, projection, selection, selectionArgs, null, null, sortOrder);
                break;

            case ENTRIES_EXITS_URI_CODE:

                // Query entries_exits table
                cursor = db.query(DBHelper.TABLE_ENTRY_EXIT, projection, selection, selectionArgs, null, null, sortOrder);
                break;

            default:

                // If the URI doesn't match known patterns, throw an exception
                throw new IllegalArgumentException("Unknown URI: " + uri);

        }

        // Set notification URI on the cursor, so it can be automatically updated if the data at this URI changes
        cursor.setNotificationUri(getContext().getContentResolver(), uri);

        return cursor;
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues values) {
        // Get writable database instance
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        long rowId;

        // Determine the URI type and perform appropriate insert
        switch (uriMatcher.match(uri)) {

            case CIRCLES_URI_CODE:

                // Insert into circles table
                rowId = db.insert(DBHelper.TABLE_CIRCLE, null, values);
                break;

            case ENTRIES_EXITS_URI_CODE:

                // Insert into entries_exits table
                rowId = db.insert(DBHelper.TABLE_ENTRY_EXIT, null, values);
                break;

            default:

                // If the URI doesn't match known patterns, throw an exception
                throw new IllegalArgumentException("Unknown URI: " + uri);

        }

        // If insertion was successful, return the URI with the appended row ID and notify content resolver
        if (rowId != -1) {
            Uri newUri = ContentUris.withAppendedId(uri, rowId);
            getContext().getContentResolver().notifyChange(newUri, null);

            return newUri;
        }

        // If insertion failed, throw an exception
        throw new SQLException("Failed to insert row into " + uri);
    }



    // Other methods Content Provider needs to implement but we don't use in our project
    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        return null;
    }

    @Override
    public int delete(@NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs) {

        return 0;
    }

    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues values, @Nullable String selection,
                      @Nullable String[] selectionArgs) {
        return 0;
    }
}