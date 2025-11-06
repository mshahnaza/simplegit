package org.example.objects;

import org.example.utils.SHA1Hasher;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class Commit extends GitObject {
    private byte[] treeHash;
    private List<byte[]> parentHashes = new ArrayList<>();
    private String author;
    private String committer;
    private String message;

    public Commit(byte[] treeHash, List<byte[]> parentHashes, String author, String committer, String message) {
        this.type = "commit";
        setTreeHash(treeHash);
        for (byte[] parentHash : parentHashes) {
            addParent(parentHash);
        }
        setAuthor(author);
        setCommitter(committer);
        setMessage(message);
    }

    public Commit(byte[] treeHash, String author, String committer, String message) {
        this(treeHash, new ArrayList<>(), author, committer, message);
    }

    public Commit() {
        this.type = "commit";
        this.treeHash = null;
        this.parentHashes = new ArrayList<>();
        this.author = null;
        this.committer = null;
        this.message = "";
    }

    public byte[] getTreeHash() { return treeHash == null ? null : treeHash.clone(); }
    public List<byte[]> getParentHashes() {
        List<byte[]> copy = new ArrayList<>();
        for (byte[] parentHash : parentHashes) {
            copy.add(parentHash == null ? null : parentHash.clone());
        }
        return copy;
    }
    public String getAuthor() { return author; }
    public String getCommitter() { return committer; }
    public String getMessage() { return message; }

    public void setTreeHash(byte[] treeHash) {
        if (treeHash == null) {
            this.treeHash = null;
        } else {
            if (treeHash.length != 20) {
                throw new IllegalArgumentException("treeHash must be 20 bytes (SHA-1 raw).");
            }
            this.treeHash = treeHash.clone();
        }
        this.hash = null;
    }
    public void addParent(byte[] parentHash) {
        if (parentHash != null && parentHash.length != 20) {
            throw new IllegalArgumentException("parentHash must be 20 bytes (SHA-1 raw) or null.");
        }
        parentHashes.add(parentHash == null ? null : parentHash.clone());
        this.hash = null;
    }
    public void setMessage(String message) {
        this.message = message == null ? "" : message;
        this.hash = null;
    }
    public void setAuthor(String author) {
        if (author != null && !isValidSignatureFormat(author)) {
            throw new IllegalArgumentException("Invalid author format: " + author);
        }
        this.author = author;
        this.hash = null;
    }

    public void setCommitter(String committer) {
        if (committer != null && !isValidSignatureFormat(committer)) {
            throw new IllegalArgumentException("Invalid committer format: " + committer);
        }
        this.committer = committer;
        this.hash = null;
    }

    private boolean isValidSignatureFormat(String signature) {
        if (signature == null) return true;
        return signature.matches("^.+\\s\\d+\\s[+-]\\d{4}$");
    }

    @Override
    public byte[] serialize() {
        StringBuilder sb = new StringBuilder();

        if (treeHash != null) {
            sb.append("tree ").append(SHA1Hasher.toHex(treeHash)).append("\n");
        }

        for (byte[] parentHash : parentHashes) {
            if (parentHash != null) {
                sb.append("parent ").append(SHA1Hasher.toHex(parentHash)).append("\n");
            }
        }

        if (author != null) {
            sb.append("author ").append(author).append("\n");
        }
        if (committer != null) {
            sb.append("committer ").append(committer).append("\n");
        }
        sb.append("\n");
        sb.append(message);
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public void deserialize(byte[] data) {
        treeHash = null;
        parentHashes.clear();
        author = null;
        committer = null;
        message = "";

        if(data == null || data.length == 0) {
            return;
        }

        String content = new String(data, StandardCharsets.UTF_8);
        int index = content.indexOf("\n\n");
        String header;
        String body;
        if (index == -1) {
            header = content;
            body = "";
        } else {
            header = content.substring(0, index);
            body = content.substring(index + 2);
        }

        String[] parts = header.split("\n");

        for (String part : parts) {
            if (part.startsWith("tree ")) {
                this.treeHash = SHA1Hasher.fromHex(part.substring("tree ".length()));
            } else if (part.startsWith("parent ")) {
                this.parentHashes.add(SHA1Hasher.fromHex(part.substring("parent ".length())));
            } else if (part.startsWith("author ")) {
                this.author = part.substring("author ".length());
            } else if (part.startsWith("committer ")) {
                this.committer = part.substring("committer ".length());
            }
        }
        this.message = body;
    }

    @Override
    protected byte[] computeHash() {
        byte[] content = serialize();
        String header = "commit " + content.length + "\0";
        byte[] headerBytes = header.getBytes(StandardCharsets.UTF_8);

        byte[] data = new byte[content.length + headerBytes.length];
        System.arraycopy(headerBytes, 0, data, 0, headerBytes.length);
        System.arraycopy(content, 0, data, headerBytes.length, content.length);
        return SHA1Hasher.hash(data);
    }

    public boolean isRootCommit() {
        return parentHashes.isEmpty();
    }
}
