package com.example.gasbooking;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private DatabaseHelper databaseHelper;
    private EditText usernameEditText, passwordEditText, addressEditText, quantityEditText;
    private Button registerButton, loginButton,check, bookGasButton, viewBookingsButton;
    //private TextView bookingStatusTextView, bookingsTextView;
    private GridLayout bookingsGridLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        databaseHelper = new DatabaseHelper(this);
        usernameEditText = findViewById(R.id.usernameEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        addressEditText = findViewById(R.id.addressEditText);
        quantityEditText = findViewById(R.id.quantityEditText);
        registerButton = findViewById(R.id.registerButton);
        loginButton = findViewById(R.id.loginButton);
        bookGasButton = findViewById(R.id.bookGasButton);
        viewBookingsButton = findViewById(R.id.viewBookingsButton);
        /*bookingStatusTextView = findViewById(R.id.bookingStatusTextView);
        bookingsTextView = findViewById(R.id.bookingsTextView);*/
        bookingsGridLayout = findViewById(R.id.bookingsGridLayout);
        check= findViewById(R.id.check);

        check.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkUserDetails();
            }
        });

        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String username = usernameEditText.getText().toString();
                String password = passwordEditText.getText().toString();
                databaseHelper.registerUser (username, password);
                Toast.makeText(MainActivity.this, "User  registered successfully!", Toast.LENGTH_SHORT).show();
            }
        });

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String username = usernameEditText.getText().toString();
                String password = passwordEditText.getText().toString();
                if (databaseHelper.loginUser (username, password)) {
                    Toast.makeText(MainActivity.this, "Login successful!", Toast.LENGTH_SHORT).show();
                    //bookingStatusTextView.setVisibility(View.VISIBLE);
                    addressEditText.setVisibility(View.VISIBLE);
                    quantityEditText.setVisibility(View.VISIBLE);
                    bookGasButton.setVisibility(View.VISIBLE);
                    viewBookingsButton.setVisibility(View.VISIBLE); // Show the View Bookings button
                    //bookingsTextView.setVisibility(View.GONE); // Hide bookings text view initially

                    // Get user ID
                    int userId = getUserId(username);

                    bookGasButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            String quantity = quantityEditText.getText().toString();
                            String address = addressEditText.getText().toString(); // Get address
                            databaseHelper.addBooking(userId, quantity, address); // Call with address
                            Toast.makeText(MainActivity.this, "Booking successful!", Toast.LENGTH_SHORT).show();
                        }
                    });

                    // Set up the View Bookings button
                    viewBookingsButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            displayBookings(userId); // Display bookings for the logged-in user
                        }
                    });
                } else {
                    Toast.makeText(MainActivity.this, "Invalid username or password.", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private int getUserId(String username) {
        SQLiteDatabase db = databaseHelper.getReadableDatabase();
        Cursor cursor = db.query("users", new String[]{"id"}, "username=?", new String[]{username}, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            int userId = cursor.getInt(cursor.getColumnIndex("id"));
            cursor.close();
            return userId;
        }
        return -1; // User not found
    }

    /*private void displayBookings(int userId) {
        List<GasBooking> bookings = databaseHelper.getBookings(userId);
        StringBuilder bookingsText = new StringBuilder();
        bookingsText.append("Your Bookings:\n");
        bookingsText.append(String.format("%-10s %-8s %-5s %-15s %-15s %-30s%n", "Date", "Time", "Qnty", "Delivery On", "Status", "Address"));
        for (GasBooking booking : bookings) {
            bookingsText.append(String.format("%-10s %-8s %-5s %-15s %-15s %-30s%n",
                    booking.getDate(),
                    booking.getTime(),
                    booking.getQuantity(),
                    booking.getDeliveryDate(),
                    booking.getDeliveryStatus(),
                    booking.getAddress())); // Display address
        }
        bookingsTextView.setText(bookingsText.toString());
        bookingsTextView.setVisibility(View.VISIBLE); // Show the bookings text view
    }*/
    private void displayBookings(int userId) {
        List<GasBooking> bookings = databaseHelper.getBookings(userId);
        bookingsGridLayout.removeAllViews(); // Clear previous views

        // Add header row
        addHeaderRow();
        for (GasBooking booking : bookings) {
            addBookingToGrid(booking);
        }

        bookingsGridLayout.setVisibility(View.VISIBLE); // Show the grid layout
    }

    private void checkUserDetails()
    {
            DatabaseHelper databaseHelper = new DatabaseHelper(this);
            User user = databaseHelper.getUserById(1); // Get user with userId = 1

            if (user != null) {
                // Display user details
                Log.d("User Details", "User  ID: " + user.getUserId());
                Log.d("User Details", "Username: " + user.getUsername());
                // You can also display this information in a TextView or Toast
                Toast.makeText(this, ":User  " + user.getUsername(), Toast.LENGTH_SHORT).show();
            } else {
                Log.d("User Details", "User  not found");
            }
        }


private void addBookingToGrid(GasBooking booking) {
    // Create TextViews for each booking detail
    TextView dateTextView = new TextView(this);
    dateTextView.setText(booking.getDate());
    dateTextView.setPadding(8, 8, 8, 8);
    bookingsGridLayout.addView(dateTextView);

    TextView timeTextView = new TextView(this);
    timeTextView.setText(booking.getTime());
    timeTextView.setPadding(8, 8, 8, 8);
    bookingsGridLayout.addView(timeTextView);

    TextView quantityTextView = new TextView(this);
    quantityTextView.setText(booking.getQuantity());
    quantityTextView.setPadding(8, 8, 8, 8);
    bookingsGridLayout.addView(quantityTextView);

    TextView deliveryDateTextView = new TextView(this);
    deliveryDateTextView.setText(booking.getDeliveryDate());
    deliveryDateTextView.setPadding(8, 8, 8, 8);
    bookingsGridLayout.addView(deliveryDateTextView);

    TextView deliveryStatusTextView = new TextView(this);
    deliveryStatusTextView.setText(booking.getDeliveryStatus());
    deliveryStatusTextView.setPadding(8, 8, 8, 8);
    bookingsGridLayout.addView(deliveryStatusTextView);

    TextView addressTextView = new TextView(this);
    addressTextView.setText(booking.getAddress());
    addressTextView.setPadding(8, 8, 8, 8);
    bookingsGridLayout.addView(addressTextView);
}
    private void addHeaderRow() {
        // Create TextViews for each header
        String[] headers = {"Date", "Time", "Quantity", "Delivery Date", "Delivery Status", "Address"};

        for (String header : headers) {
            TextView headerTextView = new TextView(this);
            headerTextView.setText(header);
            headerTextView.setPadding(8, 8, 8, 8);
            headerTextView.setTypeface(null, Typeface.BOLD); // Make text bold
            headerTextView.setBackgroundColor(getResources().getColor(android.R.color.darker_gray));
            headerTextView.setTextColor(getResources().getColor(android.R.color.white));
            bookingsGridLayout.addView(headerTextView);
        }
    }


}