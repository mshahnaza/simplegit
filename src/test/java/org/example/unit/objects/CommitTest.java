package org.example.unit.objects;

import org.example.objects.Commit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class CommitTest {

    private Commit commit;
    private byte[] treeHash;
    private byte[] parentHash1;
    private byte[] parentHash2;

    @BeforeEach
    void setUp() {
        treeHash = hexToBytes("1234567890abcdef1234567890abcdef12345678");
        parentHash1 = hexToBytes("a1b2c3d4e5f6a1b2c3d4e5a1b2c3d4e5f6a1b2c3");
        parentHash2 = hexToBytes("f6e5d4c3b2a1f6e5d4c3b2a1f6e5d4c3b2a1f6e5");
    }

    @Test
    void shouldCreateRootCommit() {
        String author = "John Doe <john@example.com> 1700000000 +0000";
        String committer = "Jane Smith <jane@example.com> 1700000000 +0000";

        commit = new Commit(treeHash, Arrays.asList(), author, committer, "Initial commit");

        assertNotNull(commit);
        assertEquals("commit", commit.getType());
        assertArrayEquals(treeHash, commit.getTreeHash());
        assertTrue(commit.getParentHashes().isEmpty());
        assertEquals(author, commit.getAuthor());
        assertEquals(committer, commit.getCommitter());
        assertEquals("Initial commit", commit.getMessage());
        assertTrue(commit.isRootCommit());
    }

    @Test
    void shouldCreateCommitWithParents() {
        String author = "Alice <alice@example.com> 1700000001 +0000";
        String committer = "Bob <bob@example.com> 1700000001 +0000";

        commit = new Commit(treeHash, Arrays.asList(parentHash1, parentHash2), author, committer, "Merge commit");

        assertEquals(2, commit.getParentHashes().size());
        assertArrayEquals(parentHash1, commit.getParentHashes().get(0));
        assertArrayEquals(parentHash2, commit.getParentHashes().get(1));
        assertFalse(commit.isRootCommit());
    }

    @Test
    void shouldSerializeAndDeserializeCommit() {
        String author = "Test Author <test@example.com> 1700000002 +0000";
        String committer = "Test Committer <test@example.com> 1700000002 +0000";
        String message = "Test commit message\nWith multiple lines";

        Commit original = new Commit(treeHash, Arrays.asList(parentHash1), author, committer, message);

        byte[] serialized = original.serialize();
        Commit deserialized = new Commit();
        deserialized.deserialize(serialized);

        assertArrayEquals(original.getTreeHash(), deserialized.getTreeHash());
        assertEquals(original.getParentHashes().size(), deserialized.getParentHashes().size());
        assertArrayEquals(original.getParentHashes().get(0), deserialized.getParentHashes().get(0));
        assertEquals(original.getAuthor(), deserialized.getAuthor());
        assertEquals(original.getCommitter(), deserialized.getCommitter());
        assertEquals(original.getMessage(), deserialized.getMessage());
    }

    @Test
    void shouldMaintainConsistentHashAfterSerialization() {
        String author = "Hash Test <hash@test.com> 1700000003 +0000";
        String committer = "Hash Test <hash@test.com> 1700000003 +0000";

        Commit commit1 = new Commit(treeHash, Arrays.asList(parentHash1), author, committer, "Hash test");

        byte[] serialized = commit1.serialize();
        Commit commit2 = new Commit();
        commit2.deserialize(serialized);

        String hash1 = commit1.getHexhash();
        String hash2 = commit2.getHexhash();

        assertEquals(hash1, hash2, "Hash should be consistent after serialization/deserialization");
    }

    @Test
    void shouldHandleEmptyCommit() {
        Commit empty = new Commit();

        assertNotNull(empty);
        assertEquals("commit", empty.getType());
        assertNull(empty.getTreeHash());
        assertTrue(empty.getParentHashes().isEmpty());
        assertNull(empty.getAuthor());
        assertNull(empty.getCommitter());
        assertEquals("", empty.getMessage());
        assertTrue(empty.isRootCommit());
    }

    @Test
    void shouldSerializeWithCorrectFormat() {
        String author = "Format Test <format@test.com> 1700000004 +0000";
        String committer = "Format Test <format@test.com> 1700000004 +0000";

        commit = new Commit(treeHash, Arrays.asList(parentHash1), author, committer, "Format test");

        byte[] serialized = commit.serialize();
        String serializedString = new String(serialized, StandardCharsets.UTF_8);

        assertTrue(serializedString.startsWith("tree "));
        assertTrue(serializedString.contains("parent "));
        assertTrue(serializedString.contains("author " + author));
        assertTrue(serializedString.contains("committer " + committer));
        assertTrue(serializedString.contains("Format test"));
    }

    @Test
    void shouldDeserializeFromGitFormat() {
        String gitCommit = "tree 1234567890abcdef1234567890abcdef12345678\n" +
                "parent a1b2c3d4e5f6a1b2c3d4e5a1b2c3d4e5f6a1b2c3\n" +
                "author John Doe <john@example.com> 1700000000 +0000\n" +
                "committer Jane Smith <jane@example.com> 1700000000 +0000\n" +
                "\n" +
                "Commit message with\n" +
                "multiple lines";

        Commit commit = new Commit();
        commit.deserialize(gitCommit.getBytes(StandardCharsets.UTF_8));

        assertArrayEquals(hexToBytes("1234567890abcdef1234567890abcdef12345678"), commit.getTreeHash());
        assertEquals(1, commit.getParentHashes().size());
        assertArrayEquals(hexToBytes("a1b2c3d4e5f6a1b2c3d4e5a1b2c3d4e5f6a1b2c3"), commit.getParentHashes().get(0));
        assertEquals("John Doe <john@example.com> 1700000000 +0000", commit.getAuthor());
        assertEquals("Jane Smith <jane@example.com> 1700000000 +0000", commit.getCommitter());
        assertEquals("Commit message with\nmultiple lines", commit.getMessage());
    }

    @Test
    void shouldHandleDeserializeWithEmptyData() {
        Commit commit = new Commit();

        assertDoesNotThrow(() -> commit.deserialize(null));
        assertDoesNotThrow(() -> commit.deserialize(new byte[0]));
    }

    @Test
    void shouldHandleDeserializeWithMissingFields() {
        String minimalCommit = "tree 1234567890abcdef1234567890abcdef12345678\n" +
                "\n" +
                "Minimal commit";

        Commit commit = new Commit();
        commit.deserialize(minimalCommit.getBytes(StandardCharsets.UTF_8));

        assertArrayEquals(hexToBytes("1234567890abcdef1234567890abcdef12345678"), commit.getTreeHash());
        assertTrue(commit.getParentHashes().isEmpty());
        assertNull(commit.getAuthor());
        assertNull(commit.getCommitter());
        assertEquals("Minimal commit", commit.getMessage());
    }

    @Test
    void shouldValidateTreeHashLength() {
        String author = "Test <test@test.com> 1700000000 +0000";
        String committer = "Test <test@test.com> 1700000000 +0000";

        byte[] invalidHash = new byte[19];
        assertThrows(IllegalArgumentException.class,
                () -> new Commit(invalidHash, Arrays.asList(), author, committer, "Test"));
    }

    @Test
    void shouldValidateParentHashLength() {
        String author = "Test <test@test.com> 1700000000 +0000";
        String committer = "Test <test@test.com> 1700000000 +0000";

        byte[] invalidParentHash = new byte[19];
        assertThrows(IllegalArgumentException.class,
                () -> new Commit(treeHash, Arrays.asList(invalidParentHash), author, committer, "Test"));
    }

    @Test
    void shouldUpdateHashAfterModification() {
        String author = "Original <original@test.com> 1700000000 +0000";
        String committer = "Original <original@test.com> 1700000000 +0000";

        commit = new Commit(treeHash, Arrays.asList(), author, committer, "Original");
        String originalHash = commit.getHexhash();

        commit.setMessage("Modified");
        String modifiedHash = commit.getHexhash();

        assertNotEquals(originalHash, modifiedHash, "Hash should change after modification");
    }

    @Test
    void shouldHandleMultiLineMessageInDeserialize() {
        String gitCommit = "tree 1234567890abcdef1234567890abcdef12345678\n" +
                "author Test <test@test.com> 1700000000 +0000\n" +
                "committer Test <test@test.com> 1700000000 +0000\n" +
                "\n" +
                "Line 1\n" +
                "Line 2\n" +
                "Line 3\n" +
                "Line 4";

        Commit commit = new Commit();
        commit.deserialize(gitCommit.getBytes(StandardCharsets.UTF_8));

        assertEquals("Line 1\nLine 2\nLine 3\nLine 4", commit.getMessage());
    }

    @Test
    void shouldCreateCommitWithSimpleConstructor() {
        String author = "Simple <simple@test.com> 1700000000 +0000";
        String committer = "Simple <simple@test.com> 1700000000 +0000";

        Commit commit = new Commit(treeHash, author, committer, "Simple commit");

        assertNotNull(commit);
        assertArrayEquals(treeHash, commit.getTreeHash());
        assertTrue(commit.getParentHashes().isEmpty());
        assertEquals(author, commit.getAuthor());
        assertEquals(committer, commit.getCommitter());
        assertEquals("Simple commit", commit.getMessage());
    }

    @Test
    void shouldHandleNullMessage() {
        String author = "Null Test <null@test.com> 1700000000 +0000";
        String committer = "Null Test <null@test.com> 1700000000 +0000";

        Commit commit = new Commit(treeHash, Arrays.asList(), author, committer, null);

        assertEquals("", commit.getMessage());
    }

    @Test
    void shouldHandleNullInGetters() {
        Commit commit = new Commit();

        assertNull(commit.getTreeHash());
        assertTrue(commit.getParentHashes().isEmpty());
        assertNull(commit.getAuthor());
        assertNull(commit.getCommitter());
        assertEquals("", commit.getMessage());
    }

    @Test
    void shouldComputeValidHash() {
        String author = "Hash Test <hash@test.com> 1700000000 +0000";
        String committer = "Hash Test <hash@test.com> 1700000000 +0000";

        commit = new Commit(treeHash, Arrays.asList(), author, committer, "Test commit");

        String hash = commit.getHexhash();

        assertNotNull(hash);
        assertEquals(40, hash.length());
        assertTrue(hash.matches("[0-9a-f]{40}"), "Hash should be valid hex string");
    }

    private byte[] hexToBytes(String hex) {
        if (hex == null || hex.length() != 40) {
            throw new IllegalArgumentException("Hex string must be exactly 40 characters for SHA-1: '" + hex + "' (length: " + (hex != null ? hex.length() : "null") + ")");
        }

        if (!hex.matches("[0-9a-fA-F]{40}")) {
            throw new IllegalArgumentException("Invalid hex characters in: " + hex);
        }

        byte[] bytes = new byte[20];
        for (int i = 0; i < 20; i++) {
            int index = i * 2;
            int value = Integer.parseInt(hex.substring(index, index + 2), 16);
            bytes[i] = (byte) value;
        }
        return bytes;
    }

    @Test
    void shouldHandleSerializationWithNullTreeHash() {
        String author = "Test <test@test.com> 1700000000 +0000";
        String committer = "Test <test@test.com> 1700000000 +0000";

        Commit commit = new Commit(null, Arrays.asList(), author, committer, "Test");

        assertDoesNotThrow(() -> commit.serialize());

        byte[] serialized = commit.serialize();
        String serializedString = new String(serialized, StandardCharsets.UTF_8);

        assertFalse(serializedString.contains("tree "));
    }

    @Test
    void shouldHandleEmptyMessageInSerialization() {
        String author = "Test <test@test.com> 1700000000 +0000";
        String committer = "Test <test@test.com> 1700000000 +0000";

        Commit commit = new Commit(treeHash, Arrays.asList(), author, committer, "");

        byte[] serialized = commit.serialize();
        String serializedString = new String(serialized, StandardCharsets.UTF_8);

        assertTrue(serializedString.endsWith("\n\n"));
    }

    @Test
    void shouldValidateAuthorFormat() {
        assertThrows(IllegalArgumentException.class,
                () -> new Commit(treeHash, Arrays.asList(), "Invalid author", "Valid <valid@test.com> 1700000000 +0000", "Test"));

        Commit commit = new Commit();
        assertThrows(IllegalArgumentException.class,
                () -> commit.setAuthor("Invalid author"));

        assertDoesNotThrow(() -> commit.setAuthor("Name <email@test.com> 1700000000 +0000"));
        assertDoesNotThrow(() -> commit.setAuthor("John Doe <john.doe@example.com> 1700000000 -0500"));
    }

    @Test
    void shouldValidateCommitterFormat() {
        Commit commit = new Commit();
        assertThrows(IllegalArgumentException.class,
                () -> commit.setCommitter("Invalid committer"));

        assertDoesNotThrow(() -> commit.setCommitter("Name <email@test.com> 1700000000 +0000"));
    }
}