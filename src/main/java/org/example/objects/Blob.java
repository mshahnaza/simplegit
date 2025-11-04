package org.example.objects;

import org.example.utils.SHA256Hasher;

import java.nio.charset.StandardCharsets;

public class Blob extends GitObject {
    private byte[] content;

    public Blob(byte[] content) {
        this.content = content;
        this.type = "blob";
    }

    public Blob() {
        this.type = "blob";
        this.content = new byte[0];
    }

    @Override
    public String serialize() {
        return new String(content, StandardCharsets.UTF_8);
    }

    @Override
    public void deserialize(String data) {
        this.content = data.getBytes();
    }

    @Override
    protected String computeSha() {
        String header = "blob\0" +  content.length + "\0";
        byte[] headerBytes = header.getBytes(StandardCharsets.UTF_8);

        byte[] sha = new byte[headerBytes.length + content.length];
        System.arraycopy(headerBytes, 0, sha, 0, headerBytes.length);
        System.arraycopy(content, 0, sha, headerBytes.length, content.length);

        return SHA256Hasher.hash(sha);
    }
}
