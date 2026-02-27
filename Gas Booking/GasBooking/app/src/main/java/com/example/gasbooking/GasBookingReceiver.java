package com.example.gasbooking;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class GasBookingReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals("com.example.gasbooking.BOOK_GAS")) {
            int quantity = intent.getIntExtra("quantity", 1); // Default to 1 if not provided
            String address = intent.getStringExtra("address");

            // Log the received data
            Log.d("GasBookingReceiver", "Received booking request: Quantity = " + quantity + ", Address = " + address);

            // Book gas automatically
            DatabaseHelper databaseHelper = new DatabaseHelper(context);
            databaseHelper.addBooking(1, String.valueOf(quantity), address); // Assuming user ID is 1
        }
    }
}