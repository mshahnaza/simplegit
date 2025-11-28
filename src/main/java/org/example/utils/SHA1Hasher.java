package org.example.utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class SHA1Hasher {
    public static byte[] hash(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            return digest.digest(bytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public static String toHex(byte[] bytes) {
        if (bytes == null) {
            throw new IllegalArgumentException("Bytes array cannot be null");
        }
        StringBuilder hex = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            hex.append(String.format("%02x", b & 0xFF));
        }
        return hex.toString();
    }

    public static byte[] fromHex(String hex) {
        if (hex == null) {
            throw new IllegalArgumentException("Hex string cannot be null");
        }
        if (hex.length() != 40) {
            throw new IllegalArgumentException("SHA-1 hash must be 40 characters long: " + hex);
        }

        byte[] bytes = new byte[20];
        for (int i = 0; i < 20; i++) {
            int index = i * 2;
            int value = Integer.parseInt(hex.substring(index, index + 2), 16);
            bytes[i] = (byte) value;
        }
        return bytes;
    }
}
