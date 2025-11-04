package org.example.objects;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

class BlobTest {

    private byte[] testContent;
    private Blob blob;

    @BeforeEach
    void setUp() {
        testContent = "Hello, Git!".getBytes(StandardCharsets.UTF_8);
        blob = new Blob(testContent);
    }

    @Test
    void should_createBlobWithContent() {
        assertNotNull(blob);
        assertArrayEquals(testContent, blob.serialize());
    }

    @Test
    void should_createEmptyBlob() {
        Blob emptyBlob = new Blob();
        assertNotNull(emptyBlob);
        assertArrayEquals(new byte[0], emptyBlob.serialize());
    }

    @Test
    void should_setTypeToBlob() {
        assertEquals("blob", blob.getType());
    }

    @Test
    void should_deserializeContent() {
        byte[] newContent = "New content".getBytes(StandardCharsets.UTF_8);
        blob.deserialize(newContent);

        assertArrayEquals(newContent, blob.serialize());
    }

    @Test
    void should_computeCorrectSha1Hash() {
        byte[] content = "test".getBytes(StandardCharsets.UTF_8);
        Blob testBlob = new Blob(content);
        String expectedHash = "30d74d258442c7c65512eafab474568dd706c430";

        String actualHash = testBlob.computeSha();
        assertEquals(expectedHash, actualHash);
    }

    @Test
    void should_handleEmptyContent() {
        Blob emptyBlob = new Blob(new byte[0]);

        assertNotNull(emptyBlob.computeSha());
        assertEquals(40, emptyBlob.computeSha().length());
    }

    @Test
    void should_handleBinaryContent() {
        byte[] binaryContent = {(byte) 0xFF, (byte) 0xFE, 0x00, 0x01, (byte) 0x80};
        Blob binaryBlob = new Blob(binaryContent);

        assertArrayEquals(binaryContent, binaryBlob.serialize());
        assertNotNull(binaryBlob.computeSha());
    }

    @Test
    void should_handleLargeContent() {
        byte[] largeContent = new byte[10000];
        Arrays.fill(largeContent, (byte) 'A');

        Blob largeBlob = new Blob(largeContent);

        assertArrayEquals(largeContent, largeBlob.serialize());
        assertNotNull(largeBlob.computeSha());
    }

    @Test
    void should_computeHashWithCorrectFormat() {
        byte[] content = "test".getBytes(StandardCharsets.UTF_8);
        Blob testBlob = new Blob(content);

        String hash = testBlob.computeSha();

        assertEquals(40, hash.length());
        assertTrue(hash.matches("[0-9a-f]{40}"));
    }
}