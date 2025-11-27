package org.example.repository;

import org.example.objects.Blob;
import org.example.objects.Commit;
import org.example.objects.GitObject;
import org.example.objects.Tree;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

public class ObjectStorage {
    private final Path objectsDir;

    public ObjectStorage(Path objectsDir) {
        this.objectsDir = objectsDir.resolve("objects");
    }

    public void store(GitObject object) throws IOException {
        String hash = object.getHexhash();
        if (exists(hash)) {
            return;
        }
        byte[] serialized = object.serialize();
        String header = object.getType() + " " + serialized.length + "\0";
        byte[] headerBytes = header.getBytes();

        byte[] data = new byte[headerBytes.length + serialized.length];
        System.arraycopy(headerBytes, 0, data, 0, headerBytes.length);
        System.arraycopy(serialized, 0, data, headerBytes.length, serialized.length);

        byte[] compressed = compress(data);
        storeRaw(hash, compressed);
    }

    public GitObject load(String hash) throws IOException {
        byte[] compressed = loadRaw(hash);
        byte[] fullData = decompress(compressed);

        int nullByteIndex = findNullByteIndex(fullData);
        if (nullByteIndex == -1) {
            throw new IOException("Invalid object format");
        }
        String header = new String(fullData, 0, nullByteIndex);
        String[] parts = header.split(" ");
        if (parts.length != 2) {
            throw new IOException("Invalid object format");
        }
        String type = parts[0];
        int contentLength = Integer.parseInt(parts[1]);

        byte[] content = new byte[contentLength];
        int contentStart = nullByteIndex + 1;
        if (contentStart + contentLength > fullData.length) {
            throw new IOException("Object data truncated");
        }
        System.arraycopy(fullData, contentStart, content, 0, contentLength);

        return createObject(type, hash, content);
    }

    public boolean delete(String hash) throws IOException {
        Path objectPath = getObjectPath(hash);
        return Files.deleteIfExists(objectPath);
    }

    public boolean exists(String hash) {
        Path objectPath = getObjectPath(hash);
        return Files.exists(objectPath);
    }

    private void storeRaw(String hash, byte[] data) throws IOException {
        Path objectPath = getObjectPath(hash);
        Path objectDir = objectPath.getParent();

        if(!Files.exists(objectDir)) {
            Files.createDirectories(objectDir);
        }

        Files.write(objectPath, data);
    }

    private byte[] loadRaw(String hash) throws IOException {
        Path objectPath = getObjectPath(hash);
        if(!Files.exists(objectPath)) {
            throw new IOException("File does not exist: " + objectPath);
        }
        return Files.readAllBytes(objectPath);
    }

    private byte[] compress(byte[] data) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try(DeflaterOutputStream dos = new DeflaterOutputStream(baos)) {
            dos.write(data);
        }
        return baos.toByteArray();
    }

    private byte[] decompress(byte[] data) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try(InflaterInputStream iis = new InflaterInputStream(new ByteArrayInputStream(data))) {
            byte[] buffer = new byte[4096];
            int read;
            while((read = iis.read(buffer)) != -1) {
                baos.write(buffer, 0, read);
            }
        }
        return baos.toByteArray();
    }

    private GitObject createObject(String type, String hash, byte[] content) throws IOException {
        GitObject obj;
        switch (type) {
            case "blob":
                obj = new Blob();
                break;
            case "tree":
                obj = new Tree();
                break;
            case "commit":
                obj = new Commit();
                break;
            default:
                throw new IOException("Unknown object type: " + type);
        }

        obj.deserialize(content);

        String actualHash = obj.getHexhash();
        if (!actualHash.equals(hash)) {
            throw new IllegalStateException(
                    String.format("Hash mismatch! Expected: %s, Actual: %s",
                            hash, actualHash));
        }

        return obj;
    }

    private int findNullByteIndex(byte[] data) {
        for(int i = 0; i < data.length; i++) {
            if(data[i] == 0) {
                return i;
            }
        }
        return -1;
    }

    private Path getObjectPath(String hash) {
        if (hash == null) {
            throw new IllegalArgumentException("Hash cannot be null");
        }
        if (hash.length() != 40) {
            throw new IllegalArgumentException("SHA-1 hash must be 40 characters long: " + hash);
        }
        if (!hash.matches("[0-9a-fA-F]+")) {
            throw new IllegalArgumentException("Hash contains invalid characters: " + hash);
        }
        return objectsDir.resolve(hash.substring(0, 2)).resolve(hash.substring(2));
    }
}
