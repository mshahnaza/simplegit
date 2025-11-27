package org.example.repository;

import org.example.utils.SHA1Hasher;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class Index {
    private final Map<String, IndexEntry> entries;
    private final Path indexFile;

    public Index(Path indexFile) {
        this.indexFile = indexFile;
        this.entries = new LinkedHashMap<>();
    }

    public void add(IndexEntry entry) {
        entries.put(entry.getPath(), entry);
    }

    public void remove(String path) {
        entries.remove(path);
    }

    public IndexEntry getEntry(String path) {
        return entries.get(path);
    }

    public List<IndexEntry> getEntries() {
        List<IndexEntry> sorted = new ArrayList<>(entries.values());
        sorted.sort(Comparator.comparing(IndexEntry::getPath));
        return sorted;
    }

    public boolean contains(String path) {
        return entries.containsKey(path);
    }

    public void clear() {
        entries.clear();
    }

    public int size() {
        return entries.size();
    }

    public boolean isEmpty() {
        return entries.isEmpty();
    }

    public void save() throws IOException {
        ByteArrayOutputStream tempStream = new ByteArrayOutputStream();
        try (DataOutputStream tempOut = new DataOutputStream(tempStream)) {
            tempOut.writeInt(entries.size());
            for (IndexEntry entry : getEntries()) {
                writeEntry(tempOut, entry);
            }
        }

        byte[] data = tempStream.toByteArray();
        byte[] checksum = SHA1Hasher.hash(data);

        try (DataOutputStream out = new DataOutputStream(new FileOutputStream(indexFile.toFile()))) {
            out.write(data);
            out.write(checksum);
        }
    }

    private void writeEntry(DataOutputStream out, IndexEntry entry) throws IOException {
        out.writeUTF(entry.getPath());
        out.write(entry.getHash());
        out.writeInt(entry.getMode());
        out.writeInt(entry.getSize());
        out.writeLong(entry.getMtimeMillis());
    }

    public void load() throws IOException {
        entries.clear();

        if (!Files.exists(indexFile)) return;

        byte[] allData = Files.readAllBytes(indexFile);
        if (allData.length < 20) throw new IOException("File too short");

        byte[] data = Arrays.copyOfRange(allData, 0, allData.length - 20);
        byte[] expectedChecksum = Arrays.copyOfRange(allData, allData.length - 20, allData.length);
        byte[] actualChecksum = SHA1Hasher.hash(data);

        if (!Arrays.equals(expectedChecksum, actualChecksum)) {
            throw new IOException("Index corrupted");
        }

        ByteArrayInputStream byteStream = new ByteArrayInputStream(data);
        try (DataInputStream in = new DataInputStream(byteStream)) {
            int entryCount = in.readInt();

            for (int i = 0; i < entryCount; i++) {
                IndexEntry entry = readEntry(in);
                entries.put(entry.getPath(), entry);
            }
        }
    }

    private IndexEntry readEntry(DataInputStream in) throws IOException {
        String path = in.readUTF();
        byte[] hash = new byte[20];
        in.readFully(hash);
        int mode = in.readInt();
        int size = in.readInt();
        long mtimeMillis = in.readLong();

        return new IndexEntry(path, hash, mode, size, mtimeMillis);
    }
}
