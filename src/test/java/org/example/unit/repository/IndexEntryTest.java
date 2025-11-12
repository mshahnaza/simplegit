package org.example.unit.repository;

import org.example.repository.IndexEntry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;

import static org.junit.jupiter.api.Assertions.*;

class IndexEntryTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldValidateInput() {
        byte[] hash = new byte[20];

        assertThrows(IllegalArgumentException.class,
                () -> new IndexEntry(null, hash, 0100644, 100, 1000L, 500000));

        assertThrows(IllegalArgumentException.class,
                () -> new IndexEntry("file.txt", null, 0100644, 100, 1000L, 500000));

        assertThrows(IllegalArgumentException.class,
                () -> new IndexEntry("file.txt", new byte[19], 0100644, 100, 1000L, 500000));
    }

    @Test
    void constructorWithMillisShouldConvertCorrectly() {
        byte[] hash = new byte[20];
        long mtimeMillis = 1700000000123L;

        IndexEntry entry = new IndexEntry("file.txt", hash, 100, mtimeMillis);

        assertEquals(1700000000L, entry.getMtimeSec());
        assertEquals(123000000, entry.getMtimeNano());
    }

    @Test
    void fromFileShouldCreateEntryWithCorrectMetadata() throws IOException {
        Path testFile = tempDir.resolve("test.txt");
        Files.write(testFile, "content".getBytes());
        Files.setLastModifiedTime(testFile, FileTime.fromMillis(1700000000000L));

        testFile.toFile().setExecutable(false);

        byte[] hash = new byte[20];
        IndexEntry entry = IndexEntry.fromFile("test.txt", hash, testFile);

        boolean executable = Files.isExecutable(testFile);
        int expectedMode = executable ? IndexEntry.MODE_EXECUTABLE : IndexEntry.MODE_FILE;
        assertEquals("test.txt", entry.getPath());
        assertArrayEquals(hash, entry.getHash());
        assertEquals(expectedMode, entry.getMode());
        assertEquals(7, entry.getSize());
        assertEquals(1700000000L, entry.getMtimeSec());
    }

    @Test
    void fromFileShouldDetectExecutableFiles() throws IOException {
        Path testFile = tempDir.resolve("script.sh");
        Files.write(testFile, "#!/bin/bash".getBytes());
        testFile.toFile().setExecutable(true);

        byte[] hash = new byte[20];
        IndexEntry entry = IndexEntry.fromFile("script.sh", hash, testFile);

        assertEquals(IndexEntry.MODE_EXECUTABLE, entry.getMode());
    }

    @Test
    void isModifiedWhenFileChangedShouldReturnTrue() throws IOException {
        Path testFile = tempDir.resolve("test.txt");
        Files.write(testFile, "content".getBytes());
        Files.setLastModifiedTime(testFile, FileTime.fromMillis(1700000000000L));

        byte[] hash = new byte[20];
        IndexEntry entry = IndexEntry.fromFile("test.txt", hash, testFile);

        Files.write(testFile, "modified content".getBytes());
        Files.setLastModifiedTime(testFile, FileTime.fromMillis(1700000001000L));

        assertTrue(entry.isModified(testFile));
    }

    @Test
    void isModifiedWhenFileUnchangedShouldReturnFalse() throws IOException {
        Path testFile = tempDir.resolve("test.txt");
        Files.write(testFile, "content".getBytes());
        Files.setLastModifiedTime(testFile, FileTime.fromMillis(1700000000000L));

        byte[] hash = new byte[20];
        IndexEntry entry = IndexEntry.fromFile("test.txt", hash, testFile);

        assertFalse(entry.isModified(testFile));
    }

    @Test
    void isModifiedWhenFileDeletedShouldReturnTrue() throws IOException {
        Path testFile = tempDir.resolve("test.txt");
        Files.write(testFile, "content".getBytes());

        byte[] hash = new byte[20];
        IndexEntry entry = IndexEntry.fromFile("test.txt", hash, testFile);

        Files.delete(testFile);

        assertTrue(entry.isModified(testFile));
    }

    @Test
    void getMtimeMillisShouldConvertCorrectly() {
        byte[] hash = new byte[20];
        IndexEntry entry = new IndexEntry("file.txt", hash, 0100644, 100, 1700000000L, 500000000);

        assertEquals(1700000000500L, entry.getMtimeMillis());
    }

    @Test
    void equalsShouldWorkCorrectly() {
        byte[] hash1 = new byte[20];
        byte[] hash2 = new byte[20];
        hash2[0] = 1;

        IndexEntry entry1 = new IndexEntry("file.txt", hash1, 0100644, 100, 1000L, 500000);
        IndexEntry entry2 = new IndexEntry("file.txt", hash1, 0100644, 100, 1000L, 500000);
        IndexEntry entry3 = new IndexEntry("other.txt", hash1, 0100644, 100, 1000L, 500000);
        IndexEntry entry4 = new IndexEntry("file.txt", hash2, 0100644, 100, 1000L, 500000);

        assertEquals(entry1, entry2);
        assertNotEquals(entry1, entry3);
        assertNotEquals(entry1, entry4);
        assertNotEquals(entry1, null);
        assertNotEquals(entry1, "not an IndexEntry");
    }

    @Test
    void hashCodeShouldBeConsistent() {
        byte[] hash = new byte[20];
        IndexEntry entry = new IndexEntry("file.txt", hash, 0100644, 100, 1000L, 500000);

        int hash1 = entry.hashCode();
        int hash2 = entry.hashCode();

        assertEquals(hash1, hash2);
    }

    @Test
    void toStringShouldFormatCorrectly() {
        byte[] hash = new byte[20];
        hash[0] = (byte) 0x12;
        hash[1] = (byte) 0xab;

        IndexEntry entry = new IndexEntry("file.txt", hash, 0100644, 100, 1000L, 500000);
        String result = entry.toString();

        assertTrue(result.contains("100644"));
        assertTrue(result.contains("file.txt"));
        assertTrue(result.contains("12ab"));
    }
}