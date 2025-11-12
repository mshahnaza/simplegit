package org.example.unit.repository;

import org.example.repository.Index;
import org.example.repository.IndexEntry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class IndexTest {

    @TempDir
    Path tempDir;

    private Index index;
    private Path indexFile;

    @BeforeEach
    void setUp() {
        indexFile = tempDir.resolve("index");
        index = new Index(indexFile);
    }

    @Test
    void addShouldStoreEntry() {
        byte[] hash = new byte[20];
        IndexEntry entry = new IndexEntry("file.txt", hash, 0100644, 100, 1000L, 500000);

        index.add(entry);

        assertTrue(index.contains("file.txt"));
        assertEquals(entry, index.getEntry("file.txt"));
    }

    @Test
    void removeShouldDeleteEntry() {
        byte[] hash = new byte[20];
        IndexEntry entry = new IndexEntry("file.txt", hash, 0100644, 100, 1000L, 500000);

        index.add(entry);
        assertTrue(index.contains("file.txt"));

        index.remove("file.txt");
        assertFalse(index.contains("file.txt"));
        assertNull(index.getEntry("file.txt"));
    }

    @Test
    void getEntriesShouldReturnSortedList() {
        byte[] hash = new byte[20];
        IndexEntry entry1 = new IndexEntry("b.txt", hash, 0100644, 100, 1000L, 500000);
        IndexEntry entry2 = new IndexEntry("a.txt", hash, 0100644, 100, 1000L, 500000);
        IndexEntry entry3 = new IndexEntry("c.txt", hash, 0100644, 100, 1000L, 500000);

        index.add(entry1);
        index.add(entry2);
        index.add(entry3);

        List<IndexEntry> entries = index.getEntries();

        assertEquals(3, entries.size());
        assertEquals("a.txt", entries.get(0).getPath());
        assertEquals("b.txt", entries.get(1).getPath());
        assertEquals("c.txt", entries.get(2).getPath());
    }

    @Test
    void saveAndLoadEmptyIndexShouldWork() throws IOException {
        index.save();

        Index loadedIndex = new Index(indexFile);
        loadedIndex.load();

        assertTrue(loadedIndex.isEmpty());
        assertEquals(0, loadedIndex.size());
    }

    @Test
    void saveAndLoadWithMultipleEntriesShouldPreserveAllData() throws IOException {
        byte[] hash1 = new byte[20];
        byte[] hash2 = new byte[20];
        hash2[0] = 1;

        IndexEntry entry1 = new IndexEntry("file1.txt", hash1, 0100644, 100, 1000L, 500000);
        IndexEntry entry2 = new IndexEntry("file2.txt", hash2, 0100755, 200, 2000L, 750000);

        index.add(entry1);
        index.add(entry2);
        index.save();

        Index loadedIndex = new Index(indexFile);
        loadedIndex.load();

        assertEquals(2, loadedIndex.size());

        IndexEntry loaded1 = loadedIndex.getEntry("file1.txt");
        IndexEntry loaded2 = loadedIndex.getEntry("file2.txt");

        assertIndexEntriesEqual(entry1, loaded1);
        assertIndexEntriesEqual(entry2, loaded2);
    }

    @Test
    void loadWhenFileDoesNotExistShouldNotFail() throws IOException {
        Files.deleteIfExists(indexFile);

        Index newIndex = new Index(indexFile);
        newIndex.load();

        assertTrue(newIndex.isEmpty());
    }

    @Test
    void loadWithCorruptedSignatureShouldThrow() throws IOException {
        Files.write(indexFile, "INVALID_SIGNATURE".getBytes());

        assertThrows(IOException.class, () -> index.load());
    }

    @Test
    void loadWithInvalidVersionShouldThrow() throws IOException {
        byte[] invalidData = new byte[100];
        System.arraycopy("DIRC".getBytes(), 0, invalidData, 0, 4);
        invalidData[4] = 0; invalidData[5] = 0; invalidData[6] = 0; invalidData[7] = 99;

        Files.write(indexFile, invalidData);

        assertThrows(IOException.class, () -> index.load());
    }

    @Test
    void loadWithInvalidChecksumShouldThrow() throws IOException {
        index.add(new IndexEntry("test.txt", new byte[20], 0100644, 100, 1000L, 500000));
        index.save();

        Files.write(indexFile, new byte[]{0x00}, java.nio.file.StandardOpenOption.APPEND);

        assertThrows(IOException.class, () -> index.load());
    }

    @Test
    void clearShouldRemoveAllEntries() {
        byte[] hash = new byte[20];
        index.add(new IndexEntry("file1.txt", hash, 0100644, 100, 1000L, 500000));
        index.add(new IndexEntry("file2.txt", hash, 0100644, 100, 1000L, 500000));

        assertEquals(2, index.size());

        index.clear();

        assertTrue(index.isEmpty());
        assertEquals(0, index.size());
    }

    @Test
    void containsShouldWorkCorrectly() {
        byte[] hash = new byte[20];
        IndexEntry entry = new IndexEntry("file.txt", hash, 0100644, 100, 1000L, 500000);

        assertFalse(index.contains("file.txt"));

        index.add(entry);

        assertTrue(index.contains("file.txt"));
        assertFalse(index.contains("nonexistent.txt"));
    }

    @Test
    void saveShouldCreateValidIndexFileStructure() throws IOException {
        byte[] hash = new byte[20];
        IndexEntry entry = new IndexEntry("test.txt", hash, 0100644, 123, 1700000000L, 500000000);

        index.add(entry);
        index.save();

        assertTrue(Files.exists(indexFile));
        assertTrue(Files.size(indexFile) > 0);

        byte[] data = Files.readAllBytes(indexFile);
        assertTrue(data.length >= 12 + 62 + "test.txt".length() + 20);
    }

    private void assertIndexEntriesEqual(IndexEntry expected, IndexEntry actual) {
        assertEquals(expected.getPath(), actual.getPath());
        assertArrayEquals(expected.getHash(), actual.getHash());
        assertEquals(expected.getMode(), actual.getMode());
        assertEquals(expected.getSize(), actual.getSize());
        assertEquals(expected.getMtimeSec(), actual.getMtimeSec());
        assertEquals(expected.getMtimeNano(), actual.getMtimeNano());
    }
}