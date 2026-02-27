package com.example.gasmonitoringsystem;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.telephony.TelephonyManager;
import android.text.InputType;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.database.DataSnapshot;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import android.media.MediaPlayer;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {
    private TextView weightValue, gasValue;
    private TextView weightLastUpdatedText, gasLastUpdatedText;
    private TextView weightAlertText, gasAlertText;
    private CardView weightAlertCard, gasAlertCard;
    private LineChart sensorChart;
    private Button reconnectButton, playSoundButton;

    private ArrayList<Entry> weightEntries = new ArrayList<>();
    private ArrayList<Entry> gasEntries = new ArrayList<>();
    private static final float WEIGHT_THRESHOLD = 50.0f;
    private static final int GAS_LEAK_THRESHOLD = 200;

    private static final int MAX_SMS_COUNT = 3;

    private boolean hasBookedGas = false;
    private int smsSentCount = 0;
    private int leakSmsCount = 0;
    private DatabaseReference mDatabase;
    private MediaPlayer mediaPlayer;
    private boolean isSoundOn = true;

    private static final int SMS_PERMISSION_CODE = 101;
    private static final int PHONE_STATE_PERMISSION_CODE = 102;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Check if this is the first run and prompt for phone number
        if (isFirstRun()) {
            promptForPhoneNumber();

            // Save that first run is completed
            SharedPreferences prefs = getSharedPreferences("GasMonitoringPrefs", MODE_PRIVATE);
            prefs.edit().putBoolean("first_run", false).apply();
        }

        playSoundButton = findViewById(R.id.playSoundButton);

        initializeViews();
        setupChart();

        // Firebase Realtime Database reference
        mDatabase = FirebaseDatabase.getInstance().getReference();

        reconnectButton.setOnClickListener(v -> fetchDataFromFirebase());

        playSoundButton.setOnClickListener(v -> {
            isSoundOn = !isSoundOn;
            updatePlaySoundButton();
        });

        // Initial data fetch
        fetchDataFromFirebase();
    }


    private void updatePlaySoundButton() {
        playSoundButton.setText(isSoundOn ? "Sound On" : "Sound Off");
        int textColor = isSoundOn ? Color.parseColor("#4CAF50") : Color.parseColor("#F44336");

        playSoundButton.setTextColor(textColor);
    }

    private void initializeViews() {
        weightValue = findViewById(R.id.weightValue);
        gasValue = findViewById(R.id.gasValue);
        weightLastUpdatedText = findViewById(R.id.weightLastUpdatedText);
        gasLastUpdatedText = findViewById(R.id.gasLastUpdatedText);
        weightAlertText = findViewById(R.id.weightAlertText);
        gasAlertText = findViewById(R.id.gasAlertText);
        weightAlertCard = findViewById(R.id.weightAlertCard);
        gasAlertCard = findViewById(R.id.gasAlertCard);
        sensorChart = findViewById(R.id.sensorChart);
        reconnectButton = findViewById(R.id.reconnectButton);
    }

    private void setupChart() {
        sensorChart.getDescription().setEnabled(false);
        sensorChart.setTouchEnabled(true);
        sensorChart.setDragEnabled(true);
        sensorChart.setScaleEnabled(true);
        sensorChart.setPinchZoom(true);

        LineDataSet weightDataSet = new LineDataSet(weightEntries, "Weight (g)");
        weightDataSet.setColor(Color.BLUE);
        weightDataSet.setCircleColor(Color.BLUE);

        LineDataSet gasDataSet = new LineDataSet(gasEntries, "Gas Level");
        gasDataSet.setColor(Color.YELLOW);
        gasDataSet.setCircleColor(Color.YELLOW);
        gasDataSet.setDrawCircles(false); // Disable the yellow dots

        LineData lineData = new LineData(weightDataSet, gasDataSet);
        sensorChart.setData(lineData);

        XAxis xAxis = sensorChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);

        YAxis leftAxis = sensorChart.getAxisLeft();
        leftAxis.setDrawGridLines(true);

        sensorChart.getAxisRight().setEnabled(false);
    }


    public void fetchDataFromFirebase() {
        mDatabase.child("gasData").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    float weight = dataSnapshot.child("weight").getValue(Float.class);
                    int gasRaw = dataSnapshot.child("gasRaw").getValue(Integer.class);
                    int gasNormalized = dataSnapshot.child("gasNormalized").getValue(Integer.class);
                    long timestamp = dataSnapshot.child("timestamp").getValue(Long.class);

                    // Log the fetched data
                    Log.d("FirebaseData", "Weight: " + weight + ", Gas Raw: " + gasRaw + ", Gas Normalized: " + gasNormalized + ", Timestamp: " + timestamp);

                    updateUI(weight, gasRaw, gasNormalized, timestamp);
                    updateChart(weight, gasNormalized, timestamp);
                    trackAlerts(weight, gasRaw, timestamp);
                    send(weight);
                    createweightSms(weight);
                    createLeakSms(gasRaw);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                // Handle possible errors (e.g., network error)
            }
        });
    }

    private void updateUI(float weight, int gasRaw, int gasNormalized, long timestamp) {
        runOnUiThread(() -> {
            // Update sensor values
            weightValue.setText(String.format(Locale.getDefault(), "%.1f g", weight));
            gasValue.setText(String.format(Locale.getDefault(), "%d PPM", gasRaw));

            // Update timestamps
            String timeString = new SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                    .format(new Date());
            weightLastUpdatedText.setText("Last updated: " + timeString);
            gasLastUpdatedText.setText("Last updated: " + timeString);

            // Update alerts
            updateWeightAlert(weight);
            updateGasAlert(gasRaw);
        });
    }

    private void updateWeightAlert(float weight) {
        if (weight < WEIGHT_THRESHOLD) {
            weightAlertCard.setCardBackgroundColor(Color.parseColor("#FFEBEE"));
            weightAlertText.setTextColor(Color.parseColor("#D32F2F"));
            weightAlertText.setText("Warning: Weight below threshold!");

        } else {
            weightAlertCard.setCardBackgroundColor(Color.parseColor("#E8F5E9"));
            weightAlertText.setTextColor(Color.parseColor("#2E7D32"));
            weightAlertText.setText("Weight within normal range");
        }
    }

    private void updateGasAlert(int gasRaw) {
        if (gasRaw > GAS_LEAK_THRESHOLD) {
            gasAlertCard.setCardBackgroundColor(Color.parseColor("#FFEBEE"));
            gasAlertText.setTextColor(Color.parseColor("#D32F2F"));
            gasAlertText.setText("Warning: Gas leak detected!");
        } else {
            gasAlertCard.setCardBackgroundColor(Color.parseColor("#E8F5E9"));
            gasAlertText.setTextColor(Color.parseColor("#2E7D32"));
            gasAlertText.setText("No gas leak detected");
        }
    }

    private void updateChart(float weight, int gasNormalized, long timestamp) {
        runOnUiThread(() -> {
            float timePoint = timestamp / 1000f; // Convert to seconds

            weightEntries.add(new Entry(timePoint, weight));
            gasEntries.add(new Entry(timePoint, gasNormalized));

            if (weightEntries.size() > 50) {
                weightEntries.remove(0);
                gasEntries.remove(0);
            }

            LineData lineData = new LineData(
                    new LineDataSet(weightEntries, "Weight (g)"),
                    new LineDataSet(gasEntries, "Gas Level")
            );
            sensorChart.setData(lineData);
            sensorChart.invalidate();
        });
    }

    private void trackAlerts(float weight, int gasRaw, long timestamp) {
        boolean weightLow = weight < WEIGHT_THRESHOLD;
        boolean gasLeak = gasRaw > GAS_LEAK_THRESHOLD;

        if (weightLow || gasLeak) {
            if (isSoundOn) {
                playAlertSound();
            }
        }
    }

    // Play alert sound
    private void playAlertSound() {
        if (mediaPlayer == null) {
            mediaPlayer = MediaPlayer.create(this, R.raw.buzzer); // Ensure alert_sound.mp3 is in res/raw/
            mediaPlayer.setOnCompletionListener(mp -> stopAlertSound());
        }

        if (!mediaPlayer.isPlaying()) {
            mediaPlayer.start();
        }
    }

    // Stop alert sound if needed
    private void stopAlertSound() {
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    private void send(float weight) {
        if (weight < WEIGHT_THRESHOLD && !hasBookedGas) {
            // Create an explicit intent to the receiver
            Intent intent = new Intent("com.example.gasbooking.BOOK_GAS");
            intent.putExtra("quantity", "1");
            intent.putExtra("address", "123 DYP");
            intent.setPackage("com.example.gasbooking"); // Set the package name of the receiving app

            // Send the broadcast
            sendBroadcast(intent);
            Log.d("Intent", "Gas booking intent sent!");

            // Set the flag to true to prevent multiple bookings
            hasBookedGas = true;

            // Show a toast notification
            Toast.makeText(this, "Gas cylinder booking initiated automatically!", Toast.LENGTH_SHORT).show();
        } else if (weight >= WEIGHT_THRESHOLD && hasBookedGas) {
            // Reset the flag if weight returns to normal
            hasBookedGas = false;
            Log.d("Intent", "Gas level normal, booking flag reset");
        }
    }

    private void createweightSms(float weight) {
        if (weight < WEIGHT_THRESHOLD && smsSentCount < MAX_SMS_COUNT) {
            String phoneNumber = getDevicePhoneNumber();
            if (phoneNumber != null) {
                String message = "Gas Monitoring Alert #" + (smsSentCount + 1) + ": Gas weight is below threshold! Current weight: " + weight + " g. A new cylinder has been automatically booked.";
                sendSms(phoneNumber, message);
                smsSentCount++;
            }
        } else if (weight >= WEIGHT_THRESHOLD) {
            // Reset SMS count when weight returns to normal
            smsSentCount = 0;
        }
    }

    private void sendSms(String phoneNumber, String message) {
        // Check for SMS permission
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.SEND_SMS}, SMS_PERMISSION_CODE);
        } else {
            // Send SMS
            SmsManager smsManager = SmsManager.getDefault();
            smsManager.sendTextMessage(phoneNumber, null, message, null, null);
            Toast.makeText(this, "SMS sent successfully!", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == SMS_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, you can send SMS
                Toast.makeText(this, "SMS permission granted", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "SMS permission denied! Alerts cannot be sent.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void createLeakSms(int gasRaw) {
        boolean isGasLeaking = gasRaw > GAS_LEAK_THRESHOLD;

        if (isGasLeaking && leakSmsCount < MAX_SMS_COUNT) {
            String phoneNumber = getDevicePhoneNumber();
            if (phoneNumber != null) {
                String message = "Gas Monitoring Alert #" + (leakSmsCount + 1) + ": Gas Leak!!! Take Action!!!";
                sendSms(phoneNumber, message);
                leakSmsCount++;
            }
        } else if (!isGasLeaking) {
            // Reset counter when gas level returns to normal
            leakSmsCount = 0;
        }
    }
    private String getDevicePhoneNumber() {
        SharedPreferences prefs = getSharedPreferences("GasMonitoringPrefs", MODE_PRIVATE);
        String phoneNumber = prefs.getString("phone_number", null);

        // If no phone number is saved, prompt the user
        if (phoneNumber == null || phoneNumber.isEmpty()) {
            promptForPhoneNumber();
            return null;
        }

        return phoneNumber;
    }

    private void promptForPhoneNumber() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Enter Phone Number");
        builder.setMessage("Please enter your phone number to receive alerts for gas leaks and low cylinder weight");
        builder.setCancelable(false); // User must enter a number

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_PHONE);
        input.setHint("Enter your phone number");
        builder.setView(input);

        builder.setPositiveButton("Save", (dialog, which) -> {
            String phoneNumber = input.getText().toString();
            if (!phoneNumber.isEmpty()) {
                // Save the phone number
                SharedPreferences prefs = getSharedPreferences("GasMonitoringPrefs", MODE_PRIVATE);
                prefs.edit().putString("phone_number", phoneNumber).apply();

                Toast.makeText(MainActivity.this, "Phone number saved successfully", Toast.LENGTH_SHORT).show();
            } else {
                // If empty, show the dialog again
                Toast.makeText(MainActivity.this, "Phone number cannot be empty", Toast.LENGTH_SHORT).show();
                promptForPhoneNumber();
            }
        });

        // Only allow cancellation if it's not first run
        if (!isFirstRun()) {
            builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        }

        builder.show();
    }
    private boolean isFirstRun() {
        SharedPreferences prefs = getSharedPreferences("GasMonitoringPrefs", MODE_PRIVATE);
        return prefs.getBoolean("first_run", true);
    }
}
