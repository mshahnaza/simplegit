package org.example.objects;

import org.example.utils.SHA1Hasher;

public abstract class GitObject {
    protected byte[] hash;
    protected String type;

    public abstract byte[] serialize();
    public abstract void deserialize(byte[] data);

    public String getType() {
        return type;
    }

    public String getHexhash() {
        if(hash == null) {
            hash = computeHash();
        }
        return SHA1Hasher.toHex(hash);
    }

    public byte[] getHash() {
        if(hash == null) {
            hash = computeHash();
        }
        return hash;
    }

    protected abstract byte[] computeHash();

}
