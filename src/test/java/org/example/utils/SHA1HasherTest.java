package org.example.utils;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.nio.charset.StandardCharsets;

class SHA1HasherTest {

    @Test
    void should_hashEmptyArray() {
        byte[] empty = new byte[0];
        String hash = SHA1Hasher.hash(empty);

        assertEquals(40, hash.length());
        assertEquals("da39a3ee5e6b4b0d3255bfef95601890afd80709", hash);
    }

    @Test
    void should_hashSimpleString() {
        byte[] data = "hello".getBytes(StandardCharsets.UTF_8);
        String hash = SHA1Hasher.hash(data);

        assertEquals(40, hash.length());
        assertEquals("aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d", hash);
    }

    @Test
    void should_hashBinaryData() {
        byte[] binaryData = {(byte) 0xFF, (byte) 0xFE, 0x00, 0x01};
        String hash = SHA1Hasher.hash(binaryData);

        assertEquals(40, hash.length());
        assertTrue(hash.matches("[0-9a-f]{40}"));
    }

    @Test
    void should_produceSameHashForSameInput() {
        byte[] data = "consistent".getBytes(StandardCharsets.UTF_8);

        String hash1 = SHA1Hasher.hash(data);
        String hash2 = SHA1Hasher.hash(data);

        assertEquals(hash1, hash2);
    }

    @Test
    void should_produceDifferentHashForDifferentInput() {
        byte[] data1 = "input1".getBytes(StandardCharsets.UTF_8);
        byte[] data2 = "input2".getBytes(StandardCharsets.UTF_8);

        String hash1 = SHA1Hasher.hash(data1);
        String hash2 = SHA1Hasher.hash(data2);

        assertNotEquals(hash1, hash2);
    }

    @Test
    void should_handleNullInput() {
        assertThrows(NullPointerException.class, () -> {
            SHA1Hasher.hash(null);
        });
    }

    @Test
    void should_hashKnownGitObject() {
        String header = "blob 4\0";
        byte[] headerBytes = header.getBytes(StandardCharsets.UTF_8);
        byte[] content = "test".getBytes(StandardCharsets.UTF_8);

        byte[] data = new byte[headerBytes.length + content.length];
        System.arraycopy(headerBytes, 0, data, 0, headerBytes.length);
        System.arraycopy(content, 0, data, headerBytes.length, content.length);

        String hash = SHA1Hasher.hash(data);

        assertEquals("30d74d258442c7c65512eafab474568dd706c430", hash);
    }
}