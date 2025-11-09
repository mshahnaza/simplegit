package org.example.unit.objects;

import org.example.objects.Tree;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TreeTest {

    private Tree tree;
    private byte[] blobHash1;
    private byte[] blobHash2;
    private byte[] treeHash;

    @BeforeEach
    void setUp() {
        tree = new Tree();

        blobHash1 = hexToBytes("a1b2c3d4e5f6a1b2c3d4e5a1b2c3d4e5f6a1b2c3");
        blobHash2 = hexToBytes("f6e5d4c3b2a1f6e5d4c3b2a1f6e5d4c3b2a1f6e5");
        treeHash = hexToBytes("1234567890abcdef1234567890abcdef12345678");
    }

    @Test
    void shouldCreateEmptyTree() {
        assertNotNull(tree);
        assertEquals("tree", tree.getType());
        assertTrue(tree.getEntries().isEmpty());
    }

    @Test
    void shouldSerializeAndDeserializeEmptyTree() {
        Tree emptyTree = new Tree();

        byte[] serialized = emptyTree.serialize();
        Tree deserializedTree = new Tree();
        deserializedTree.deserialize(serialized);

        assertNotNull(serialized);
        assertEquals(0, serialized.length);
        assertTrue(deserializedTree.getEntries().isEmpty());
    }

    @Test
    void shouldSerializeAndDeserializeTreeWithFiles() {
        Tree originalTree = new Tree();
        originalTree.addFile("README.md", blobHash1);
        originalTree.addFile("Main.java", blobHash2);

        byte[] serialized = originalTree.serialize();
        Tree deserializedTree = new Tree();
        deserializedTree.deserialize(serialized);

        assertNotNull(serialized);
        assertTrue(serialized.length > 0);

        List<Tree.Entry> originalEntries = originalTree.getEntries();
        List<Tree.Entry> deserializedEntries = deserializedTree.getEntries();

        assertEquals(originalEntries.size(), deserializedEntries.size());

        for (int i = 0; i < originalEntries.size(); i++) {
            Tree.Entry originalEntry = originalEntries.get(i);
            Tree.Entry deserializedEntry = deserializedEntries.get(i);

            assertEquals(originalEntry.getMode(), deserializedEntry.getMode());
            assertEquals(originalEntry.getName(), deserializedEntry.getName());
            assertArrayEquals(originalEntry.getHash(), deserializedEntry.getHash());
        }
    }

    @Test
    void shouldSerializeAndDeserializeTreeWithDirectories() {
        Tree originalTree = new Tree();
        originalTree.addFile("config.txt", blobHash1);
        originalTree.addDirectory("src", treeHash);
        originalTree.addDirectory("docs", blobHash2);

        byte[] serialized = originalTree.serialize();
        Tree deserializedTree = new Tree();
        deserializedTree.deserialize(serialized);

        assertEquals(3, deserializedTree.getEntries().size());

        assertEquals("config.txt", deserializedTree.getEntries().get(0).getName());
        assertEquals("docs", deserializedTree.getEntries().get(1).getName());
        assertEquals("src", deserializedTree.getEntries().get(2).getName());

        assertEquals("blob", deserializedTree.getEntries().get(0).getType());
        assertEquals("tree", deserializedTree.getEntries().get(1).getType());
        assertEquals("tree", deserializedTree.getEntries().get(2).getType());
    }

    @Test
    void shouldMaintainConsistentHashAfterSerialization() {
        Tree tree1 = new Tree();
        tree1.addFile("test.txt", blobHash1);
        tree1.addDirectory("src", treeHash);

        byte[] serialized = tree1.serialize();
        Tree tree2 = new Tree();
        tree2.deserialize(serialized);

        byte[] hash1 = tree1.getHash();
        byte[] hash2 = tree2.getHash();

        assertArrayEquals(hash1, hash2, "Hash should be consistent after serialization/deserialization");
    }

    @Test
    void shouldHandleBinaryDataInSerialization() {
        Tree tree = new Tree();
        byte[] binaryHash = new byte[20];
        for (int i = 0; i < 20; i++) {
            binaryHash[i] = (byte) (i * 7);
        }
        tree.addFile("binary.dat", binaryHash);

        byte[] serialized = tree.serialize();
        Tree deserialized = new Tree();
        deserialized.deserialize(serialized);

        assertArrayEquals(binaryHash, deserialized.getEntries().get(0).getHash());
    }

    @Test
    void shouldPreserveEntryOrderAfterDeserialization() {
        Tree tree = new Tree();
        tree.addFile("z_file.txt", blobHash1);
        tree.addFile("a_file.txt", blobHash2);
        tree.addDirectory("m_dir", treeHash);

        byte[] serialized = tree.serialize();
        Tree deserialized = new Tree();
        deserialized.deserialize(serialized);

        List<Tree.Entry> entries = deserialized.getEntries();
        assertEquals("a_file.txt", entries.get(0).getName());
        assertEquals("m_dir", entries.get(1).getName());
        assertEquals("z_file.txt", entries.get(2).getName());
    }

    @Test
    void shouldHandleDeserializeWithInvalidData() {
        Tree tree = new Tree();
        byte[] invalidData = "invalid tree data".getBytes(StandardCharsets.UTF_8);

        assertDoesNotThrow(() -> tree.deserialize(invalidData));
        assertTrue(tree.getEntries().isEmpty());
    }

    @Test
    void shouldHandleDeserializeWithPartialData() {
        byte[] partialData = "100644 file.txt\0".getBytes(StandardCharsets.UTF_8);
        Tree tree = new Tree();

        tree.deserialize(partialData);

        assertTrue(tree.getEntries().isEmpty());
    }

    private byte[] hexToBytes(String hex) {
        byte[] bytes = new byte[20];
        for (int i = 0; i < 20; i++) {
            int index = i * 2;
            int value = Integer.parseInt(hex.substring(index, index + 2), 16);
            bytes[i] = (byte) value;
        }
        return bytes;
    }
}