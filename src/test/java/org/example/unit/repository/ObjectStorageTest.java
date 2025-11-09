package org.example.unit.repository;

import org.example.objects.Blob;
import org.example.objects.Commit;
import org.example.objects.Tree;
import org.example.repository.ObjectStorage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class ObjectStorageTest {

    @TempDir
    Path tempDir;

    private ObjectStorage storage;

    @BeforeEach
    void setUp() {
        storage = new ObjectStorage(tempDir);
    }

    @Test
    void shouldStoreAndLoadBlob() throws IOException {
        Blob original = new Blob("Hello, World!".getBytes());

        storage.store(original);
        Blob loaded = (Blob) storage.load(original.getHexhash());

        assertArrayEquals(original.serialize(), loaded.serialize());
        assertEquals(original.getHexhash(), loaded.getHexhash());
    }

    @Test
    void shouldStoreAndLoadTree() throws IOException {
        Tree original = new Tree();
        original.addFile("README.md", hexToBytes("a1b2c3d4e5f6a1b2c3d4e5a1b2c3d4e5f6a1b2c3"));
        original.addDirectory("src", hexToBytes("f6e5d4c3b2a1f6e5d4c3b2a1f6e5d4c3b2a1f6e5"));

        storage.store(original);
        Tree loaded = (Tree) storage.load(original.getHexhash());

        assertEquals(original.getEntries().size(), loaded.getEntries().size());
        assertEquals(original.getHexhash(), loaded.getHexhash());
    }

    @Test
    void shouldStoreAndLoadCommit() throws IOException {
        String author = "Test <test@test.com> 1700000000 +0000";
        String committer = "Test <test@test.com> 1700000000 +0000";
        byte[] treeHash = hexToBytes("1234567890abcdef1234567890abcdef12345678");

        Commit original = new Commit(treeHash, Arrays.asList(), author, committer, "Test commit");

        storage.store(original);
        Commit loaded = (Commit) storage.load(original.getHexhash());

        assertEquals(original.getMessage(), loaded.getMessage());
        assertEquals(original.getHexhash(), loaded.getHexhash());
        assertEquals(original.getAuthor(), loaded.getAuthor());
    }

    @Test
    void shouldCheckObjectExistence() throws IOException {
        Blob blob = new Blob("Test content".getBytes());
        String hash = blob.getHexhash();

        assertFalse(storage.exists(hash));
        storage.store(blob);
        assertTrue(storage.exists(hash));
    }

    @Test
    void shouldThrowWhenLoadingNonExistentObject() {
        assertThrows(IOException.class, () -> storage.load("a1b2c3d4e5f6a1b2c3d4e5a1b2c3d4e5f6a1b2c3"));
    }

    @Test
    void shouldDeleteObject() throws IOException {
        Blob blob = new Blob("To be deleted".getBytes());
        String hash = blob.getHexhash();

        storage.store(blob);
        assertTrue(storage.exists(hash));

        assertTrue(storage.delete(hash));
        assertFalse(storage.exists(hash));
    }

    @Test
    void shouldReturnFalseWhenDeletingNonExistentObject() throws IOException {
        String nonExistentHash = "1111111111111111111111111111111111111111";
        assertFalse(storage.delete(nonExistentHash));
    }

    @Test
    void shouldThrowOnInvalidHashInDelete() {
        assertThrows(IllegalArgumentException.class,
                () -> storage.delete("short"));

        assertThrows(IllegalArgumentException.class,
                () -> storage.delete("nonexistent123456789012345678901234567890"));

        assertThrows(IllegalArgumentException.class,
                () -> storage.delete(null));

        assertThrows(IllegalArgumentException.class,
                () -> storage.delete("ZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZ"));
    }

    @Test
    void shouldThrowOnInvalidHashInExists() {
        assertThrows(IllegalArgumentException.class,
                () -> storage.exists("short"));

        assertThrows(IllegalArgumentException.class,
                () -> storage.exists(null));

        assertThrows(IllegalArgumentException.class,
                () -> storage.exists("ZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZ"));
    }

    @Test
    void shouldHandleEmptyBlob() throws IOException {
        Blob emptyBlob = new Blob(new byte[0]);

        storage.store(emptyBlob);
        Blob loaded = (Blob) storage.load(emptyBlob.getHexhash());

        assertArrayEquals(new byte[0], loaded.serialize());
        assertEquals(emptyBlob.getHexhash(), loaded.getHexhash());
    }

    @Test
    void shouldHandleLargeBlob() throws IOException {
        byte[] largeData = new byte[1024 * 1024];
        Arrays.fill(largeData, (byte) 'A');

        Blob largeBlob = new Blob(largeData);

        storage.store(largeBlob);
        Blob loaded = (Blob) storage.load(largeBlob.getHexhash());

        assertArrayEquals(largeData, loaded.serialize());
    }

    @Test
    void shouldMaintainHashConsistencyAfterStorage() throws IOException {
        Blob original = new Blob("Consistency test".getBytes());
        String originalHash = original.getHexhash();

        storage.store(original);
        Blob loaded = (Blob) storage.load(originalHash);

        assertEquals(originalHash, loaded.getHexhash());
    }

    @Test
    void shouldThrowOnInvalidHashFormat() {
        assertThrows(IllegalArgumentException.class, () -> storage.load("short"));

        assertThrows(IllegalArgumentException.class, () -> storage.load("invalid!characters@in#hash$value%invalid"));

        assertThrows(IllegalArgumentException.class, () -> storage.load(null));

        assertThrows(IllegalArgumentException.class, () -> storage.load("1234567890"));
    }

    @Test
    void shouldHandleMultipleObjects() throws IOException {
        Blob blob1 = new Blob("Content 1".getBytes());
        Blob blob2 = new Blob("Content 2".getBytes());
        Blob blob3 = new Blob("Content 3".getBytes());

        storage.store(blob1);
        storage.store(blob2);
        storage.store(blob3);

        assertTrue(storage.exists(blob1.getHexhash()));
        assertTrue(storage.exists(blob2.getHexhash()));
        assertTrue(storage.exists(blob3.getHexhash()));

        Blob loaded1 = (Blob) storage.load(blob1.getHexhash());
        Blob loaded2 = (Blob) storage.load(blob2.getHexhash());
        Blob loaded3 = (Blob) storage.load(blob3.getHexhash());

        assertArrayEquals(blob1.serialize(), loaded1.serialize());
        assertArrayEquals(blob2.serialize(), loaded2.serialize());
        assertArrayEquals(blob3.serialize(), loaded3.serialize());
    }

    @Test
    void shouldCreateCorrectDirectoryStructure() throws IOException {
        Blob blob = new Blob("Test".getBytes());
        String hash = blob.getHexhash();

        storage.store(blob);

        Path expectedDir = tempDir.resolve("objects").resolve(hash.substring(0, 2));
        Path expectedFile = expectedDir.resolve(hash.substring(2));

        assertTrue(Files.exists(expectedDir));
        assertTrue(Files.exists(expectedFile));
        assertTrue(Files.isDirectory(expectedDir));
        assertTrue(Files.isRegularFile(expectedFile));
    }

    @Test
    void shouldCompressData() throws IOException {
        StringBuilder content = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            content.append("This is some repetitive content that compresses well ");
        }

        Blob blob = new Blob(content.toString().getBytes());

        storage.store(blob);

        Path objectPath = tempDir.resolve("objects")
                .resolve(blob.getHexhash().substring(0, 2))
                .resolve(blob.getHexhash().substring(2));

        byte[] storedData = Files.readAllBytes(objectPath);

        String header = blob.getType() + " " + blob.serialize().length + "\0";
        byte[] headerBytes = header.getBytes();
        byte[] originalData = blob.serialize();
        byte[] uncompressedData = new byte[headerBytes.length + originalData.length];
        System.arraycopy(headerBytes, 0, uncompressedData, 0, headerBytes.length);
        System.arraycopy(originalData, 0, uncompressedData, headerBytes.length, originalData.length);

        assertTrue(storedData.length < uncompressedData.length,
                "Compressed data should be smaller than uncompressed. " +
                        "Compressed: " + storedData.length + " bytes, " +
                        "Uncompressed: " + uncompressedData.length + " bytes");
    }

    private byte[] hexToBytes(String hex) {
        byte[] bytes = new byte[20];
        for (int i = 0; i < 20; i++) {
            int index = i * 2;
            int value = Integer.parseInt(hex.substring(index, index + 2), 16);
            bytes[i] = (byte) value;
        }
        return bytes;
    }
}