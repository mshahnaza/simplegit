package org.example.utils;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class SHA1HasherTest {

    @Test
    void should_hashEmptyArray() {
        byte[] empty = new byte[0];
        byte[] hash = SHA1Hasher.hash(empty);
        String hexHash = SHA1Hasher.toHex(hash);

        assertEquals(40, hexHash.length());
        assertEquals("da39a3ee5e6b4b0d3255bfef95601890afd80709", hexHash);
    }

    @Test
    void should_hashSimpleString() {
        byte[] data = "hello".getBytes(StandardCharsets.UTF_8);
        byte[] hash = SHA1Hasher.hash(data);
        String hexHash = SHA1Hasher.toHex(hash);

        assertEquals(40, hexHash.length());
        assertEquals("aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d", hexHash);
    }

    @Test
    void should_hashBinaryData() {
        byte[] binaryData = {(byte) 0xFF, (byte) 0xFE, 0x00, 0x01};
        byte[] hash = SHA1Hasher.hash(binaryData);
        String hexHash = SHA1Hasher.toHex(hash);

        assertEquals(40, hexHash.length());
        assertTrue(hexHash.matches("[0-9a-f]{40}"));
    }

    @Test
    void should_produceSameHashForSameInput() {
        byte[] data = "consistent".getBytes(StandardCharsets.UTF_8);

        byte[] hash1 = SHA1Hasher.hash(data);
        byte[] hash2 = SHA1Hasher.hash(data);

        assertArrayEquals(hash1, hash2);
    }

    @Test
    void should_produceDifferentHashForDifferentInput() {
        byte[] data1 = "input1".getBytes(StandardCharsets.UTF_8);
        byte[] data2 = "input2".getBytes(StandardCharsets.UTF_8);

        byte[] hash1 = SHA1Hasher.hash(data1);
        byte[] hash2 = SHA1Hasher.hash(data2);

        assertFalse(java.util.Arrays.equals(hash1, hash2));
    }

    @Test
    void should_handleNullInput() {
        assertThrows(NullPointerException.class, () -> SHA1Hasher.hash(null));
    }

    @Test
    void should_hashKnownGitObject() {
        String header = "blob 4\0";
        byte[] headerBytes = header.getBytes(StandardCharsets.UTF_8);
        byte[] content = "test".getBytes(StandardCharsets.UTF_8);

        byte[] data = new byte[headerBytes.length + content.length];
        System.arraycopy(headerBytes, 0, data, 0, headerBytes.length);
        System.arraycopy(content, 0, data, headerBytes.length, content.length);

        byte[] hash = SHA1Hasher.hash(data);
        String hexHash = SHA1Hasher.toHex(hash);

        assertEquals("30d74d258442c7c65512eafab474568dd706c430", hexHash);
    }
}
