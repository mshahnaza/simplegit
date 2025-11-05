package org.example.utils;

import java.math.BigInteger;
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
        BigInteger number = new BigInteger(1, bytes);
        String hex = number.toString(16);
        while (hex.length() < 40) {
            hex = "0" + hex;
        }
        return hex;
    }
}
