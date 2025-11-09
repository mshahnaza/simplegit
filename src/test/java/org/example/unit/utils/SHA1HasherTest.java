package org.example.unit.utils;

import org.example.utils.SHA1Hasher;
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
    void should_convertToHexAndBack() {
        byte[] originalData = "test data".getBytes(StandardCharsets.UTF_8);
        byte[] hash = SHA1Hasher.hash(originalData);
        String hex = SHA1Hasher.toHex(hash);
        byte[] restored = SHA1Hasher.fromHex(hex);

        assertArrayEquals(hash, restored);
    }

    @Test
    void should_validateHexLength() {
        assertThrows(IllegalArgumentException.class,
                () -> SHA1Hasher.fromHex("short"));
    }

    @Test
    void should_handleNullInput() {
        assertThrows(IllegalArgumentException.class, () -> SHA1Hasher.toHex(null));
        assertThrows(IllegalArgumentException.class, () -> SHA1Hasher.fromHex(null));
    }
}