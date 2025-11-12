package org.example.repository;

import org.example.utils.SHA1Hasher;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class Index {
    private static final byte[] SIGNATURE = "DIRC".getBytes(StandardCharsets.UTF_8);
    private static final int VERSION = 2;

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
        ByteArrayOutputStream dataStream = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(dataStream)) {
            out.write(SIGNATURE);
            out.writeInt(VERSION);
            out.writeInt(entries.size());

            List<IndexEntry> sorted = getEntries();
            for (IndexEntry entry : sorted) {
                writeEntry(out, entry);
            }
        }

        byte[] data = dataStream.toByteArray();
        byte[] checksum = SHA1Hasher.hash(data);

        try (FileOutputStream fos = new FileOutputStream(indexFile.toFile());
             BufferedOutputStream bos = new BufferedOutputStream(fos)) {
            bos.write(data);
            bos.write(checksum);
        }
    }

    private void writeEntry(DataOutputStream out, IndexEntry entry) throws IOException {
        out.writeInt((int) entry.getMtimeSec());
        out.writeInt(entry.getMtimeNano());
        out.writeInt((int) entry.getMtimeSec());
        out.writeInt(entry.getMtimeNano());

        out.writeInt(0);
        out.writeInt(0);

        out.writeInt(entry.getMode());

        out.writeInt(0);
        out.writeInt(0);

        out.writeInt(entry.getSize());

        out.write(entry.getHash());

        byte[] pathBytes = entry.getPath().getBytes(StandardCharsets.UTF_8);
        if (pathBytes.length > 0xFFF) {
            throw new IOException("Path too long: " + entry.getPath());
        }
        short flags = (short) pathBytes.length;
        out.writeShort(flags);

        out.write(pathBytes);

        int entrySize = 62 + pathBytes.length;
        int padding = (8 - (entrySize % 8)) % 8;
        if (padding > 0) {
            out.write(new byte[padding]);
        }
    }

    public void load() throws IOException {
        entries.clear();

        if (!Files.exists(indexFile)) {
            return;
        }

        byte[] allData = Files.readAllBytes(indexFile);
        if (allData.length < 12 + 20) {
            throw new IOException("Index file too short");
        }

        byte[] fileDataWithoutChecksum = Arrays.copyOfRange(allData, 0, allData.length - 20);
        byte[] expectedChecksum = Arrays.copyOfRange(allData, allData.length - 20, allData.length);
        byte[] actualChecksum = SHA1Hasher.hash(fileDataWithoutChecksum);

        if (!Arrays.equals(expectedChecksum, actualChecksum)) {
            throw new IOException("Index file checksum mismatch");
        }

        ByteArrayInputStream byteStream = new ByteArrayInputStream(fileDataWithoutChecksum);
        try (DataInputStream in = new DataInputStream(byteStream)) {

            byte[] sig = new byte[4];
            in.readFully(sig);
            if (!Arrays.equals(sig, SIGNATURE)) {
                throw new IOException("Invalid index signature");
            }

            int version = in.readInt();
            if (version != VERSION) {
                throw new IOException("Unsupported index version: " + version);
            }

            int entryCount = in.readInt();

            for (int i = 0; i < entryCount; i++) {
                IndexEntry entry = readEntry(in);
                entries.put(entry.getPath(), entry);
            }
        }
    }

    private IndexEntry readEntry(DataInputStream in) throws IOException {
        int ctimeSec = in.readInt();
        int ctimeNano = in.readInt();
        int mtimeSec = in.readInt();
        int mtimeNano = in.readInt();

        in.skipBytes(8);

        int mode = in.readInt();

        in.skipBytes(8);

        int size = in.readInt();

        byte[] hash = new byte[20];
        in.readFully(hash);

        short flags = in.readShort();
        int pathLength = flags & 0xFFF;

        if (pathLength < 0 || pathLength > 0xFFF) {
            throw new IOException("Invalid path length in index entry: " + pathLength);
        }

        byte[] pathBytes = new byte[pathLength];
        in.readFully(pathBytes);
        String path = new String(pathBytes, StandardCharsets.UTF_8);

        int entrySize = 62 + pathLength;
        int padding = (8 - (entrySize % 8)) % 8;
        if (padding > 0) {
            in.skipBytes(padding);
        }

        return new IndexEntry(path, hash, mode, size, mtimeSec, mtimeNano);
    }
}
