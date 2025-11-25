package org.example.objects;

import org.example.utils.SHA1Hasher;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class Tree extends GitObject {

    public static class Entry {
        private String mode;
        private byte[] hash;
        private String name;

        public Entry(String mode, byte[] hash, String name) {
            this.mode = mode;
            this.hash = hash;
            this.name = name;
        }

        public String getMode() { return mode; }
        public byte[] getHash() { return hash; }
        public String getHexHash() { return SHA1Hasher.toHex(hash); }
        public String getName() { return name; }
        public String getType() {
            if ("100644".equals(mode) || "100755".equals(mode)) {
                return "blob";
            } else if ("040000".equals(mode)) {
                return "tree";
            } else {
                return "unknown";
            }
        }
    }

    private List<Entry> entries;

    public Tree() {
        type = "tree";
        entries = new ArrayList<>();
    }

    public void addEntry(Entry entry) {
        entries.add(entry);
        entries.sort(Comparator.comparing(Entry::getName));
        this.hash = null;
    }

    public void addFile(String filename, byte[] hash) {
        addEntry(new Entry("100644", hash, filename));
    }

    public void addDirectory(String directory, byte[] hash) {
        addEntry(new Entry("040000", hash, directory));
    }

    @Override
    public byte[] serialize() {
        List<byte[]> parts = new ArrayList<>();
        int totalLength = 0;

        for (Entry entry : entries) {
            byte[] modeName = (entry.getMode() + " " + entry.getName() + "\0").getBytes();
            byte[] hash = entry.getHash();
            byte[] combined = new byte[modeName.length + hash.length];

            System.arraycopy(modeName, 0, combined, 0, modeName.length);
            System.arraycopy(hash, 0, combined, modeName.length, hash.length);

            parts.add(combined);
            totalLength += combined.length;
        }

        byte[] result = new byte[totalLength];
        int pos = 0;
        for (byte[] part : parts) {
            System.arraycopy(part, 0, result, pos, part.length);
            pos += part.length;
        }

        return result;
    }

    @Override
    public void deserialize(byte[] data) {
        entries.clear();

        if (data == null || data.length == 0) {
            return;
        }

        int position = 0;
        while (position < data.length) {
            int nullBytePos = -1;
            for (int i = position; i < data.length; i++) {
                if (data[i] == 0) {
                    nullBytePos = i;
                    break;
                }
            }

            if (nullBytePos == -1) {
                break;
            }

            String modeAndName = new String(data, position, nullBytePos - position);
            String[] parts = modeAndName.split(" ", 2);
            if (parts.length != 2) {
                throw new IllegalArgumentException("Invalid tree entry format: " + modeAndName);
            }

            String mode = parts[0];
            String name = parts[1];

            position = nullBytePos + 1;

            if (position + 20 > data.length) {
                break;
            }

            byte[] hash = new byte[20];
            System.arraycopy(data, position, hash, 0, 20);
            position += 20;

            String type = ("100644".equals(mode) || "100755".equals(mode)) ? "blob" : "tree";

            entries.add(new Entry(mode, hash, name));
        }

        entries.sort(Comparator.comparing(Entry::getName));
        this.hash = null;
    }

    @Override
    protected byte[] computeHash() {
        byte[] content = serialize();
        String header = "tree " + content.length + "\0";
        byte[] data = new byte[header.length() + content.length];

        System.arraycopy(header.getBytes(), 0, data, 0, header.length());
        System.arraycopy(content, 0, data, header.length(), content.length);

        return SHA1Hasher.hash(data);
    }

    public List<Entry> getEntries() {
        return entries;
    }
}
