package org.example.repository;

import org.example.utils.SHA1Hasher;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.Arrays;

public class IndexEntry {
    public static final int MODE_FILE = 0100644;
    public static final int MODE_EXECUTABLE = 0100755;

    private final String path;
    private final byte[] hash;
    private final int mode;
    private final int size;
    private final long mtimeSec;
    private final int mtimeNano;

    public IndexEntry(String path, byte[] hash, int mode, int size, long mtimeSec, int mtimeNano) {
        if(path == null || hash == null) throw new IllegalArgumentException("Path and hash cannot be null");
        if(hash.length != 20) throw new IllegalArgumentException("Hash length must be 20 bytes");
        this.path = path;
        this.hash = hash;
        this.mode = mode;
        this.size = size;
        this.mtimeSec = mtimeSec;
        this.mtimeNano = mtimeNano;
    }

    public IndexEntry(String indexPath, byte[] hash, int size, long mtimeMillis) {
        this(indexPath, hash, MODE_FILE, size, mtimeMillis / 1000,
                (int)((mtimeMillis % 1000) * 1_000_000));
    }

    public String getPath() { return path; }
    public byte[] getHash() { return hash.clone(); }
    public int getMode() { return mode; }
    public int getSize() { return size; }
    public long getMtimeSec() { return mtimeSec; }
    public int getMtimeNano() { return mtimeNano; }

    public static IndexEntry fromFile(String path, byte[] hash, Path file) throws IOException {
        long size = Files.size(file);
        FileTime mtime = Files.getLastModifiedTime(file);
        boolean executable = Files.isExecutable(file);
        int mode = executable ? MODE_EXECUTABLE : MODE_FILE;
        long mtimeMillis = mtime.toMillis();

        return new IndexEntry(path, hash, mode, (int)size,
                mtimeMillis / 1000,
                (int)((mtimeMillis % 1000) * 1_000_000));
    }

    public long getMtimeMillis() {
        return mtimeSec * 1000L + mtimeNano / 1_000_000;
    }

    public boolean isModified(Path file) throws IOException {
        if (!Files.exists(file)) {
            return true;
        }

        FileTime currentMtime = Files.getLastModifiedTime(file);
        long currentSize = Files.size(file);

        return currentMtime.toMillis() != getMtimeMillis() ||
                currentSize != size;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof IndexEntry that)) return false;

        return mode == that.mode &&
                size == that.size &&
                mtimeSec == that.mtimeSec &&
                mtimeNano == that.mtimeNano &&
                path.equals(that.path) &&
                Arrays.equals(hash, that.hash);
    }

    @Override
    public int hashCode() {
        int result = path.hashCode();
        result = 31 * result + Arrays.hashCode(hash);
        result = 31 * result + mode;
        result = 31 * result + size;
        result = 31 * result + Long.hashCode(mtimeSec);
        result = 31 * result + mtimeNano;
        return result;
    }

    @Override
    public String toString() {
        String modeStr = String.format("%06o", mode);
        String hashStr = SHA1Hasher.toHex(hash);
        return String.format("%s %s %s", modeStr, hashStr, path);
    }
}
