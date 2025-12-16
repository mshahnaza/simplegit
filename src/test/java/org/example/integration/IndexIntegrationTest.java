package org.example.integration;

import org.example.objects.Blob;
import org.example.objects.Commit;
import org.example.objects.Tree;
import org.example.repository.Index;
import org.example.repository.IndexEntry;
import org.example.repository.ObjectStorage;
import org.example.repository.RefStorage;
import org.example.utils.SHA1Hasher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class IndexIntegrationTest {

    @TempDir
    Path tempDir;

    private Path gitDir;
    private Path workspace;
    private Index index;
    private ObjectStorage objectStorage;
    private RefStorage refStorage;

    @BeforeEach
    void setUp() throws IOException {
        gitDir = tempDir.resolve(".git");
        workspace = tempDir.resolve("workspace");

        Files.createDirectories(gitDir);
        Files.createDirectories(workspace);

        Path indexFile = gitDir.resolve("index");
        index = new Index(indexFile);
        objectStorage = new ObjectStorage(gitDir);
        refStorage = new RefStorage(gitDir);
    }

    @Test
    void shouldTrackFilesAndCreateCommit() throws IOException {
        Path readmeFile = workspace.resolve("README.md");
        Path srcFile = workspace.resolve("src/Main.java");

        Files.createDirectories(srcFile.getParent());
        Files.write(readmeFile, "# My Project".getBytes());
        Files.write(srcFile, "public class Main {}".getBytes());

        byte[] readmeHash = SHA1Hasher.hash(Files.readAllBytes(readmeFile));
        byte[] srcHash = SHA1Hasher.hash(Files.readAllBytes(srcFile));

        IndexEntry readmeEntry = IndexEntry.fromFile("README.md", readmeHash, readmeFile);
        IndexEntry srcEntry = IndexEntry.fromFile("src/Main.java", srcHash, srcFile);

        index.add(readmeEntry);
        index.add(srcEntry);
        index.save();

        assertTrue(index.contains("README.md"));
        assertTrue(index.contains("src/Main.java"));
        assertEquals(2, index.size());

        Blob readmeBlob = new Blob(Files.readAllBytes(readmeFile));
        Blob srcBlob = new Blob(Files.readAllBytes(srcFile));

        objectStorage.store(readmeBlob);
        objectStorage.store(srcBlob);

        Tree rootTree = new Tree();
        Tree srcTree = new Tree();
        srcTree.addFile("Main.java", srcBlob.getHash());
        rootTree.addFile("README.md", readmeBlob.getHash());
        rootTree.addDirectory("src", srcTree.getHash());
        objectStorage.store(srcTree);
        objectStorage.store(rootTree);

        String author = "Test Author <author@test.com> 1700000000 +0000";
        String committer = "Test Committer <committer@test.com> 1700000000 +0000";
        Commit commit = new Commit(rootTree.getHash(), author, committer, "Initial commit");
        objectStorage.store(commit);

        refStorage.updateHeadCommit(commit.getHexhash());

        assertTrue(objectStorage.exists(readmeBlob.getHexhash()));
        assertTrue(objectStorage.exists(srcBlob.getHexhash()));
        assertTrue(objectStorage.exists(rootTree.getHexhash()));
        assertTrue(objectStorage.exists(srcTree.getHexhash()));
        assertTrue(objectStorage.exists(commit.getHexhash()));

        String headCommit = refStorage.getHeadCommit();
        assertEquals(commit.getHexhash(), headCommit);
    }

    @Test
    void shouldHandleFileModifications() throws IOException, InterruptedException {
        Path file = workspace.resolve("config.txt");
        Files.write(file, "version1".getBytes());

        byte[] hash1 = SHA1Hasher.hash(Files.readAllBytes(file));
        IndexEntry entry1 = IndexEntry.fromFile("config.txt", hash1, file);
        index.add(entry1);
        index.save();

        Files.write(file, "version2".getBytes());
        Thread.sleep(30);

        assertTrue(entry1.isModified(file));

        byte[] hash2 = SHA1Hasher.hash(Files.readAllBytes(file));
        IndexEntry entry2 = IndexEntry.fromFile("config.txt", hash2, file);
        index.add(entry2);
        index.save();

        Index loadedIndex = new Index(gitDir.resolve("index"));
        loadedIndex.load();

        IndexEntry loadedEntry = loadedIndex.getEntry("config.txt");
        assertArrayEquals(hash2, loadedEntry.getHash());
        assertFalse(loadedEntry.isModified(file));
    }

    @Test
    void shouldHandleMultipleCommitsWithIndexChanges() throws IOException {
        Path file1 = workspace.resolve("file1.txt");
        Files.write(file1, "content1".getBytes());

        byte[] hash1 = SHA1Hasher.hash(Files.readAllBytes(file1));
        IndexEntry entry1 = IndexEntry.fromFile("file1.txt", hash1, file1);
        index.add(entry1);
        index.save();

        Blob blob1 = new Blob(Files.readAllBytes(file1));
        Tree tree1 = new Tree();
        tree1.addFile("file1.txt", blob1.getHash());

        objectStorage.store(blob1);
        objectStorage.store(tree1);

        String author = "Author <author@test.com> 1700000000 +0000";
        Commit commit1 = new Commit(tree1.getHash(), author, author, "First commit");
        objectStorage.store(commit1);

        Path file2 = workspace.resolve("file2.txt");
        Files.write(file2, "content2".getBytes());

        byte[] hash2 = SHA1Hasher.hash(Files.readAllBytes(file2));
        IndexEntry entry2 = IndexEntry.fromFile("file2.txt", hash2, file2);
        index.add(entry2);
        index.save();

        Blob blob2 = new Blob(Files.readAllBytes(file2));
        Tree tree2 = new Tree();
        tree2.addFile("file1.txt", blob1.getHash());
        tree2.addFile("file2.txt", blob2.getHash());

        objectStorage.store(blob2);
        objectStorage.store(tree2);

        Commit commit2 = new Commit(tree2.getHash(), List.of(commit1.getHash()), author, author, "Second commit");
        objectStorage.store(commit2);

        Commit loadedCommit2 = (Commit) objectStorage.load(commit2.getHexhash());
        assertEquals("Second commit", loadedCommit2.getMessage());
        assertEquals(1, loadedCommit2.getParentHashes().size());

        Commit loadedCommit1 = (Commit) objectStorage.load(SHA1Hasher.toHex(loadedCommit2.getParentHashes().get(0)));
        assertEquals("First commit", loadedCommit1.getMessage());

        Index currentIndex = new Index(gitDir.resolve("index"));
        currentIndex.load();
        assertEquals(2, currentIndex.size());
        assertTrue(currentIndex.contains("file1.txt"));
        assertTrue(currentIndex.contains("file2.txt"));
    }

    @Test
    void shouldHandleFileDeletions() throws IOException {
        Path file1 = workspace.resolve("keep.txt");
        Path file2 = workspace.resolve("delete.txt");

        Files.write(file1, "keep this".getBytes());
        Files.write(file2, "delete this".getBytes());

        byte[] hash1 = SHA1Hasher.hash(Files.readAllBytes(file1));
        byte[] hash2 = SHA1Hasher.hash(Files.readAllBytes(file2));

        IndexEntry entry1 = IndexEntry.fromFile("keep.txt", hash1, file1);
        IndexEntry entry2 = IndexEntry.fromFile("delete.txt", hash2, file2);

        index.add(entry1);
        index.add(entry2);
        index.save();

        Files.delete(file2);
        index.remove("delete.txt");
        index.save();

        Blob blob1 = new Blob(Files.readAllBytes(file1));
        Tree tree = new Tree();
        tree.addFile("keep.txt", blob1.getHash());

        objectStorage.store(blob1);
        objectStorage.store(tree);

        String author = "Author <author@test.com> 1700000000 +0000";
        Commit commit = new Commit(tree.getHash(), author, author, "Remove delete.txt");
        objectStorage.store(commit);

        Index finalIndex = new Index(gitDir.resolve("index"));
        finalIndex.load();

        assertEquals(1, finalIndex.size());
        assertTrue(finalIndex.contains("keep.txt"));
        assertFalse(finalIndex.contains("delete.txt"));

        Tree loadedTree = (Tree) objectStorage.load(tree.getHexhash());
        assertEquals(1, loadedTree.getEntries().size());
        assertEquals("keep.txt", loadedTree.getEntries().get(0).getName());
    }

    @Test
    void shouldDetectChangesAfterCommit() throws IOException {
        Path file = workspace.resolve("data.txt");
        Files.write(file, "initial data".getBytes());

        byte[] hash1 = SHA1Hasher.hash(Files.readAllBytes(file));
        IndexEntry entry = IndexEntry.fromFile("data.txt", hash1, file);
        index.add(entry);
        index.save();

        Blob blob = new Blob(Files.readAllBytes(file));
        Tree tree = new Tree();
        tree.addFile("data.txt", blob.getHash());

        objectStorage.store(blob);
        objectStorage.store(tree);

        String author = "Author <author@test.com> 1700000000 +0000";
        Commit commit = new Commit(tree.getHash(), author, author, "Initial data");
        objectStorage.store(commit);

        Files.write(file, "modified data".getBytes());

        IndexEntry currentEntry = index.getEntry("data.txt");
        assertTrue(currentEntry.isModified(file));

        byte[] hash2 = SHA1Hasher.hash(Files.readAllBytes(file));
        IndexEntry newEntry = IndexEntry.fromFile("data.txt", hash2, file);
        index.add(newEntry);
        index.save();

        Blob newBlob = new Blob(Files.readAllBytes(file));
        Tree newTree = new Tree();
        newTree.addFile("data.txt", newBlob.getHash());

        objectStorage.store(newBlob);
        objectStorage.store(newTree);

        Commit newCommit = new Commit(newTree.getHash(), List.of(commit.getHash()), author, author, "Update data");
        objectStorage.store(newCommit);

        Commit loadedNewCommit = (Commit) objectStorage.load(newCommit.getHexhash());
        Commit loadedOldCommit = (Commit) objectStorage.load(SHA1Hasher.toHex(loadedNewCommit.getParentHashes().get(0)));

        assertNotEquals(loadedOldCommit.getTreeHash(), loadedNewCommit.getTreeHash());

        Tree oldTree = (Tree) objectStorage.load(SHA1Hasher.toHex(loadedOldCommit.getTreeHash()));
        Tree newTreeLoaded = (Tree) objectStorage.load(SHA1Hasher.toHex(loadedNewCommit.getTreeHash()));

        assertNotEquals(oldTree.getHash(), newTreeLoaded.getHash());
    }
}