package com.example.gasmonitoringsystem;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class  GasMonitoringService extends Service {
    private static final String CHANNEL_ID = "GasMonitoringServiceChannel";
    private static final int NOTIFICATION_ID = 1;
    private static final float WEIGHT_THRESHOLD = 50.0f;
    private static final int GAS_LEAK_THRESHOLD = 200;



    private DatabaseReference mDatabase;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        mDatabase = FirebaseDatabase.getInstance().getReference();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForegroundService();
        monitorGasData();
        return START_STICKY;
    }

    private void startForegroundService() {
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Gas Monitoring Service")
                .setContentText("Monitoring gas and weight data")
                .setSmallIcon(R.drawable.ic_gas)
                .build();

        startForeground(NOTIFICATION_ID, notification);
    }

    private void monitorGasData() {
        mDatabase.child("gasData").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    float weight = dataSnapshot.child("weight").getValue(Float.class);
                    int gasRaw = dataSnapshot.child("gasRaw").getValue(Integer.class);

                    checkAndSendNotifications(weight, gasRaw);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("GasMonitoringService", "Database error: " + databaseError.getMessage());
            }
        });
    }

    private void checkAndSendNotifications(float weight, int gasRaw) {
        if (weight < WEIGHT_THRESHOLD) {
            sendNotification("Weight Alert",
                    "Warning: Weight level reached to " + weight + "g",
                    NotificationManager.IMPORTANCE_HIGH);
        }

        if (gasRaw > GAS_LEAK_THRESHOLD) {
            sendNotification("Gas Leak Alert",
                    "Warning: Gas level reached to " + gasRaw + " PPM",
                    NotificationManager.IMPORTANCE_HIGH);
        }
    }

    private void sendNotification(String title, String content, int importance) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_gas)
                .setContentTitle(title)
                .setContentText(content)
                .setPriority(importance)
                .setAutoCancel(true);

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        notificationManager.notify(getNotificationId(), builder.build());
    }

    private int getNotificationId() {
        return (int) System.currentTimeMillis();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Gas Monitoring Service Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            );

            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Cleanup if needed
    }
}