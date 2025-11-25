package org.example.objects;

import org.example.utils.SHA1Hasher;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class Blob extends GitObject {
    private byte[] content;

    public Blob(byte[] content) {
        if (content == null) {
            throw new IllegalArgumentException("Content cannot be null");
        }
        this.content = content;
        this.type = "blob";
    }

    public Blob() {
        this.type = "blob";
        this.content = new byte[0];
    }

    @Override
    public byte[] serialize() {
        return Arrays.copyOf(content, content.length);
    }

    @Override
    public void deserialize(byte[] data) {
        this.content = Arrays.copyOf(data, data.length);
        this.hash = null;
    }

    @Override
    protected byte[] computeHash() {
        String header = "blob " +  content.length + "\0";
        byte[] headerBytes = header.getBytes(StandardCharsets.UTF_8);

        byte[] sha = new byte[headerBytes.length + content.length];
        System.arraycopy(headerBytes, 0, sha, 0, headerBytes.length);
        System.arraycopy(content, 0, sha, headerBytes.length, content.length);

        return SHA1Hasher.hash(sha);
    }
}
