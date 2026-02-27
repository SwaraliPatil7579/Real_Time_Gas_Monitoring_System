package com.example.gasbooking;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "gas_booking.db";
    private static final int DATABASE_VERSION = 1;

    private static final String TABLE_USERS = "users";
    private static final String TABLE_BOOKINGS = "bookings";

    // Users Table Columns
    private static final String COLUMN_USER_ID = "id";
    private static final String COLUMN_USERNAME = "username";
    private static final String COLUMN_PASSWORD = "password";

    // Bookings Table Columns
    private static final String COLUMN_BOOKING_ID = "id";
    private static final String COLUMN_BOOKING_USER_ID = "user_id";
    private static final String COLUMN_BOOKING_DATE = "date";
    private static final String COLUMN_BOOKING_TIME = "time";
    private static final String COLUMN_BOOKING_QUANTITY = "quantity";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createUsersTable = "CREATE TABLE " + TABLE_USERS + " (" +
                COLUMN_USER_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_USERNAME + " TEXT, " +
                COLUMN_PASSWORD + " TEXT)";
        db.execSQL(createUsersTable);

        String createBookingsTable = "CREATE TABLE " + TABLE_BOOKINGS + " (" +
                COLUMN_BOOKING_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_BOOKING_USER_ID + " INTEGER, " +
                COLUMN_BOOKING_DATE + " TEXT, " +
                COLUMN_BOOKING_TIME + " TEXT, " +
                COLUMN_BOOKING_QUANTITY + " TEXT, " +
                "delivery_date TEXT, " + // Existing column for delivery date
                "delivery_status TEXT, " + // Existing column for delivery status
                "address TEXT, " + // New column for address
                "FOREIGN KEY(" + COLUMN_BOOKING_USER_ID + ") REFERENCES " + TABLE_USERS + "(" + COLUMN_USER_ID + "))";
        db.execSQL(createBookingsTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_BOOKINGS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS);
        onCreate(db);
    }

    // User Registration
    public void registerUser (String username, String password) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_USERNAME, username);
        values.put(COLUMN_PASSWORD, password);
        db.insert(TABLE_USERS, null, values);
        db.close();
    }

    // User Login
    public boolean loginUser (String username, String password) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_USERS, null, COLUMN_USERNAME + "=? AND " + COLUMN_PASSWORD + "=?",
                new String[]{username, password}, null, null, null);
        boolean exists = cursor.getCount() > 0;
        cursor.close();
        db.close();
        return exists;
    }

    // Add Booking

    public void addBooking(int userId, String quantity, String address) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_BOOKING_USER_ID, userId);
        values.put(COLUMN_BOOKING_QUANTITY, quantity);
        values.put("address", address); // Set address

        // Get current date and time
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd"); // Date format
        SimpleDateFormat sdfTime = new SimpleDateFormat("HH:mm"); // Time format
        String currentDate = sdfDate.format(calendar.getTime());
        String currentTime = sdfTime.format(calendar.getTime());

        values.put(COLUMN_BOOKING_DATE, currentDate); // Set current date
        values.put(COLUMN_BOOKING_TIME, currentTime); // Set current time

        // Calculate delivery date (2 days after the booking date)
        calendar.add(Calendar.DAY_OF_MONTH, 2); // Add 2 days
        String deliveryDate = sdfDate.format(calendar.getTime());
        values.put("delivery_date", deliveryDate); // Set delivery date

        // Set delivery status to "Delivered"
        values.put("delivery_status", "Delivered");

        long result = db.insert(TABLE_BOOKINGS, null, values);
        if (result == -1) {
            Log.e("DatabaseHelper", "Failed to insert booking");
        } else {
            Log.d("DatabaseHelper", "Booking inserted successfully");
        }
        db.close();
    }
    // Get Bookings for a User
    public List<GasBooking> getBookings(int userId) {
        List<GasBooking> bookings = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        // Query to get all bookings for the specified user ID
        Cursor cursor = db.query(TABLE_BOOKINGS, null, COLUMN_BOOKING_USER_ID + "=?",
                new String[]{String.valueOf(userId)}, null, null, null);

        if (cursor.moveToFirst()) {
            do {
                String date = cursor.getString(cursor.getColumnIndex(COLUMN_BOOKING_DATE));
                String time = cursor.getString(cursor.getColumnIndex(COLUMN_BOOKING_TIME));
                String quantity = cursor.getString(cursor.getColumnIndex(COLUMN_BOOKING_QUANTITY));
                String deliveryDate = cursor.getString(cursor.getColumnIndex("delivery_date")); // Get delivery date
                String deliveryStatus = cursor.getString(cursor.getColumnIndex("delivery_status")); // Get delivery status
                String address = cursor.getString(cursor.getColumnIndex("address")); // Get address

                // Create a new GasBooking object with the retrieved data
                bookings.add(new GasBooking(date, time, quantity, deliveryDate, deliveryStatus, address));
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();
        return bookings;
    }

    // Get All Users
    public List<User> getAllUsers() {
        List<User> users = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_USERS, null, null, null, null, null, null);
        if (cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(cursor.getColumnIndex(COLUMN_USER_ID));
                String username = cursor.getString(cursor.getColumnIndex(COLUMN_USERNAME));
                String password = cursor.getString(cursor.getColumnIndex(COLUMN_PASSWORD));
                users.add(new User(id, username, password));
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return users;
    }

    public User getUserById(int userId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_USERS, null, COLUMN_USER_ID + "=?",
                new String[]{String.valueOf(userId)}, null, null, null);

        if (cursor != null && cursor.moveToFirst()) {
            String username = cursor.getString(cursor.getColumnIndex(COLUMN_USERNAME));
            String password = cursor.getString(cursor.getColumnIndex(COLUMN_PASSWORD));
            cursor.close();
            return new User(userId, username, password); // Return the User object
        }

        if (cursor != null) {
            cursor.close();
        }
        return null; // Return null if user not found
    }
}