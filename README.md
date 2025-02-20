# Android OPSEC POC
An Android WebView App with Enhanced Data Collection & Encrypted Communication.

Created with ChatGPT.

**⚠️ This project is intended only for security research and OPSEC testing. The authors are NOT responsible for any misuse of this tool.**

## 📌 Overview

Evil Webview is an Android application that uses WebView to display web content while gathering device-specific information. It securely transmits collected data using ECDH key exchange and AES encryption over a custom TCP socket connection.

## 🚀 Features

👉 Device Fingerprinting - Collects detailed hardware & system data

👉 Network Information Gathering - Captures WiFi SSID, BSSID, IP, and MAC address

👉 Advanced Cryptographic Security - Uses ECDH (Elliptic Curve Diffie-Hellman) for key exchange

👉 End-to-End Encryption - Encrypts data with AES-256-CBC before transmission

👉 Stealth Mode - Runs as a background service

## 🛠️ Installation

### 📱 Android

1. Clone the Repository ```git clone https://github.com/merezal/Android-OPSEC-POC```

2. Open in Android Studio

3. Open Android Studio

4. Select Open an Existing Project

5. Choose the cloned git repository

6. Configure the server location in ```src/main/res/values/strings.xml``` 

7. Build & Install APK

### 🐍 Python Server

The server listens for incoming connections and securely decrypts received data.

1. Create python virtual environment if one hasn't already been created. ```python -m venv env```
2. Activate python the virtual environment. ```source env/bin/activate```
3. Install dependencies to the python virtual environment. ```pip install -r requirements.txt```
4. Start the Python server: ```python server.py```

5. Ensure port 4444 is open for inbound connections. Data is named by timestamp and stored as JSON in a local ```/data``` directory.

## 🔐 Security & Data Collection

### The app collects the following device data in JSON format:

🔹 Device Information - Manufacturer, Model, Android Version, Android ID

🔹 Network Information - WiFi SSID, BSSID, IP Address, MAC Address

🔹 Hardware Info - Screen size, available sensors, battery stats

🔹 Location Data - (if permissions granted) GPS coordinates, network location

🔹 Running Processes & Installed Apps - (depending on device restrictions)

This data is encrypted using AES-256-CBC and sent via a secure TCP socket to a remote server.
