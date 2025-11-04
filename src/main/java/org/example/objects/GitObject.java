package org.example.objects;

public abstract class GitObject {
    protected String sha;
    protected String type;

    public abstract String serialize();
    public abstract void deserialize(String data);

    public String getType() {
        return type;
    }

    public String getSha() {
        if(sha == null) {
            sha = computeSha();
        }
        return sha;
    }

    protected abstract String computeSha();

}
