package com.example.xposeddemo;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class DataProvider extends ContentProvider {
    private static final String AUTHORITY = "com.example.studentprovider";
    private static final String DATABASE_NAME = "student.db";
    private static final int DATABASE_VERSION = 1;
    private static final String STUDENT_TABLE = "student";

    private static final UriMatcher uriMatcher;
    private static final int STUDENT_ALL = 1;
    private static final int STUDENT_SINGLE = 2;

    static {
        uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        uriMatcher.addURI(AUTHORITY, STUDENT_TABLE, STUDENT_ALL);
        uriMatcher.addURI(AUTHORITY, STUDENT_TABLE + "/#", STUDENT_SINGLE);
    }

    private SQLiteDatabase db;

    @Override
    public boolean onCreate() {
        DBHelper dbHelper = new DBHelper(getContext(), DATABASE_NAME, null, DATABASE_VERSION);
        db = dbHelper.getWritableDatabase();
        return db != null;
    }

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection, @Nullable String[] selectionArgs, @Nullable String sortOrder) {
        Cursor cursor = null;
        switch (uriMatcher.match(uri)) {
            case STUDENT_ALL:
                cursor = db.query(STUDENT_TABLE, projection, selection, selectionArgs, null, null, sortOrder);
                break;
            case STUDENT_SINGLE:
                String id = uri.getPathSegments().get(1);
                cursor = db.query(STUDENT_TABLE, projection, "_id=?", new String[]{id}, null, null, sortOrder);
                break;
            default:
                throw new IllegalArgumentException("Unsupported URI: " + uri);
        }
        cursor.setNotificationUri(getContext().getContentResolver(), uri);
        return cursor;
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        switch (uriMatcher.match(uri)) {
            case STUDENT_ALL:
                return "vnd.android.cursor.dir/vnd.com.example.studentprovider.student";
            case STUDENT_SINGLE:
                return "vnd.android.cursor.item/vnd.com.example.studentprovider.student";
            default:
                throw new IllegalArgumentException("Unsupported URI: " + uri);
        }
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues values) {
        long rowId = db.insert(STUDENT_TABLE, null, values);
        if (rowId > 0) {
            Uri rowUri = ContentUris.withAppendedId(uri, rowId);
            getContext().getContentResolver().notifyChange(rowUri, null);
            return rowUri;
        }
        throw new SQLException("Failed to insert row into " + uri);
    }

    @Override
    public int delete(@NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs) {
        int count = 0;
        switch (uriMatcher.match(uri)) {
            case STUDENT_ALL:
                count = db.delete(STUDENT_TABLE, selection, selectionArgs);
                break;
            case STUDENT_SINGLE:
                String id = uri.getPathSegments().get(1);
                count = db.delete(STUDENT_TABLE, "_id=?", new String[]{id});
                break;
            default:
                throw new IllegalArgumentException("Unsupported URI: " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }

    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues values, @Nullable String selection, @Nullable String[] selectionArgs) {
        int count = 0;
        switch (uriMatcher.match(uri)) {
            case STUDENT_ALL:
                count = db.update(STUDENT_TABLE, values, selection, selectionArgs);
                break;
            case STUDENT_SINGLE:
                String id = uri.getPathSegments().get(1);
                count = db.update(STUDENT_TABLE, values, "_id=?", new String[]{id});
                break;
            default:
                throw new IllegalArgumentException("Unsupported URI: " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }

    private static class DBHelper extends SQLiteOpenHelper {
        private static final String CREATE_STUDENT_TABLE = "CREATE TABLE " + STUDENT_TABLE + "(_id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT, age INTEGER)";

        public DBHelper(@Nullable Context context, @Nullable String name, @Nullable SQLiteDatabase.CursorFactory factory, int version) {
            super(context, null, factory, version);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL(CREATE_STUDENT_TABLE);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        }
    }
}
