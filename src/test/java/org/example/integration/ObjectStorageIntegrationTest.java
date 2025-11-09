package org.example.integration;

import org.example.objects.Blob;
import org.example.objects.Commit;
import org.example.objects.Tree;
import org.example.repository.ObjectStorage;
import org.example.utils.SHA1Hasher;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ObjectStorageIntegrationTest {
    @TempDir
    Path tempDir;


    @BeforeEach
    void setUp() {

    }

    @Test
    void shouldTestCommitWithSingleFile() throws IOException {
        ObjectStorage objectStorage = new ObjectStorage(tempDir);

        Blob blob = new Blob("content".getBytes(StandardCharsets.UTF_8));
        Tree rootTree = new Tree();
        rootTree.addFile("file.txt", blob.getHash());

        String author = "Author <author@example.com> 1700000000 +0000";
        String comitter = "Comitter <comitter@example.com> 1700000000 +0000";
        Commit commit = new Commit(rootTree.getHash(), author, comitter, "commit");

        objectStorage.store(blob);
        objectStorage.store(rootTree);
        objectStorage.store(commit);

        assertTrue(objectStorage.exists(blob.getHexhash()));
        assertTrue(objectStorage.exists(rootTree.getHexhash()));
        assertTrue(objectStorage.exists(commit.getHexhash()));

        Commit loadedCommit = (Commit) objectStorage.load(commit.getHexhash());
        assertEquals(commit.getAuthor(), loadedCommit.getAuthor());
        assertEquals(commit.getCommitter(), loadedCommit.getCommitter());
        assertArrayEquals(commit.getTreeHash(), loadedCommit.getTreeHash());

        Tree loadedTree = (Tree) objectStorage.load(rootTree.getHexhash());
        assertArrayEquals(rootTree.getHash(), loadedTree.getHash());
        assertEquals(rootTree.getEntries().size(), loadedTree.getEntries().size());

        Tree.Entry fileEntry = loadedTree.getEntries().get(0);
        Blob loadedBlob = (Blob) objectStorage.load(fileEntry.getHexHash());
        assertEquals("content", new String(loadedBlob.serialize()));
        assertEquals(blob.getHexhash(), loadedBlob.getHexhash());
    }

    @Test
    void shouldHandleLinearCommitHistory() throws IOException {
        ObjectStorage objectStorage = new ObjectStorage(tempDir);

        String author = "Author <author@example.com> 1700000000 +0000";
        String comitter = "Comitter <comitter@example.com> 1700000000 +0000";

        Blob blob1 = new Blob("content1".getBytes(StandardCharsets.UTF_8));
        Tree tree1 = new Tree();
        tree1.addFile("file.txt", blob1.getHash());
        Commit commit1 = new Commit(tree1.getHash(), author, comitter, "commit1");

        Blob blob2 = new Blob("content2".getBytes(StandardCharsets.UTF_8));
        Tree tree2 = new Tree();
        tree2.addFile("file.txt", blob2.getHash());
        Commit commit2 = new Commit(tree2.getHash(), Arrays.asList(commit1.getHash()), author, comitter, "commit2");

        Blob blob3 = new Blob("content3".getBytes(StandardCharsets.UTF_8));
        Tree tree3 = new Tree();
        tree3.addFile("file.txt", blob3.getHash());
        Commit commit3 = new Commit(tree3.getHash(), Arrays.asList(commit2.getHash()), author, comitter, "commit3");

        objectStorage.store(blob1);
        objectStorage.store(tree1);
        objectStorage.store(commit1);
        objectStorage.store(blob2);
        objectStorage.store(tree2);
        objectStorage.store(commit2);
        objectStorage.store(blob3);
        objectStorage.store(tree3);
        objectStorage.store(commit3);

        assertTrue(commit1.isRootCommit());

        Commit loadedCommit3 = (Commit) objectStorage.load(commit3.getHexhash());
        assertEquals(1, loadedCommit3.getParentHashes().size());

        Commit loadedCommit2 = (Commit) objectStorage.load(SHA1Hasher.toHex(loadedCommit3.getParentHashes().get(0)));
        assertEquals("commit2", loadedCommit2.getMessage());

        Commit loadedCommit1 = (Commit) objectStorage.load(SHA1Hasher.toHex(loadedCommit2.getParentHashes().get(0)));
        assertEquals("commit1", loadedCommit1.getMessage());

    }

    @Test
    void shouldHandleNestedTreeStructure() throws IOException {
        ObjectStorage objectStorage = new ObjectStorage(tempDir);
        String author = "Author <author@example.com> 1700000000 +0000";
        String comitter = "Comitter <comitter@example.com> 1700000000 +0000";

        Blob srcBlob = new Blob("src blob".getBytes(StandardCharsets.UTF_8));
        Tree srcTree = new Tree();
        srcTree.addFile("src_file.txt", srcBlob.getHash());

        Blob rootBlob = new Blob("root blob".getBytes(StandardCharsets.UTF_8));
        Tree rootTree = new Tree();
        rootTree.addFile("root_file.txt", rootBlob.getHash());
        rootTree.addDirectory("src", srcTree.getHash());

        Commit commit = new Commit(rootTree.getHash(), author, comitter, "commit");

        objectStorage.store(srcBlob);
        objectStorage.store(srcTree);
        objectStorage.store(rootBlob);
        objectStorage.store(rootTree);
        objectStorage.store(commit);

        Commit loadedCommit = (Commit) objectStorage.load(commit.getHexhash());
        assertEquals("commit", loadedCommit.getMessage());

        Tree loadedRootTree = (Tree) objectStorage.load(rootTree.getHexhash());
        assertEquals(2, loadedRootTree.getEntries().size());

        Tree.Entry srcEntry = loadedRootTree.getEntries().stream()
                .filter(entry -> "src".equals(entry.getName()))
                .findFirst().orElseThrow(() -> new RuntimeException("No src entry found"));
        Tree loadedSrcTree = (Tree) objectStorage.load(srcEntry.getHexHash());
        assertEquals("tree", srcEntry.getType());
        assertEquals(1, loadedSrcTree.getEntries().size());

        Tree.Entry srcFileEntry = loadedSrcTree.getEntries().get(0);
        assertEquals("blob", srcFileEntry.getType());
        assertEquals("src_file.txt", srcFileEntry.getName());

        Tree.Entry rootFileEntry = loadedRootTree.getEntries().stream()
                .filter(entry -> "root_file.txt".equals(entry.getName()))
                .findFirst().orElseThrow(() -> new RuntimeException("No root file entry found"));
        assertEquals("blob", rootFileEntry.getType());
    }

    @Test
    void shouldTrackFileChangesAcrossCommits() throws IOException {
        ObjectStorage objectStorage = new ObjectStorage(tempDir);
        String author = "Author <author@example.com> 1700000000 +0000";
        String comitter = "Comitter <comitter@example.com> 1700000000 +0000";

        Blob blob1 = new Blob("content".getBytes(StandardCharsets.UTF_8));
        Tree tree1 = new Tree();
        tree1.addFile("file.txt", blob1.getHash());
        Commit commit1 = new Commit(tree1.getHash(), author, comitter, "first commit");

        Blob blob2 = new Blob("changed content".getBytes(StandardCharsets.UTF_8));
        Tree tree2 = new Tree();
        tree2.addFile("file.txt", blob2.getHash());
        Commit commit2  = new Commit(tree2.getHash(), Arrays.asList(commit1.getHash()), author, comitter, "second commit");

        objectStorage.store(blob1);
        objectStorage.store(blob2);
        objectStorage.store(tree1);
        objectStorage.store(tree2);
        objectStorage.store(commit1);
        objectStorage.store(commit2);

        Commit loadedSecondCommit = (Commit) objectStorage.load(commit2.getHexhash());
        Tree loadedSecondTree = (Tree) objectStorage.load(SHA1Hasher.toHex(loadedSecondCommit.getTreeHash()));
        Tree.Entry loadedSecondFileEntry = loadedSecondTree.getEntries().get(0);
        Blob loadedSecondBlob = (Blob) objectStorage.load(loadedSecondFileEntry.getHexHash());

        assertEquals("second commit", loadedSecondCommit.getMessage());
        assertEquals("changed content", new String(loadedSecondBlob.serialize()));

        Commit loadedFirstCommit = (Commit) objectStorage.load(SHA1Hasher.toHex(loadedSecondCommit.getParentHashes().get(0)));
        Tree loadedFirstTree = (Tree) objectStorage.load(SHA1Hasher.toHex(loadedFirstCommit.getTreeHash()));
        Tree.Entry fileEntry = loadedFirstTree.getEntries().get(0);
        Blob loadedFirstBlob = (Blob) objectStorage.load(fileEntry.getHexHash());

        assertEquals("first commit", loadedFirstCommit.getMessage());
        assertEquals("content", new String(loadedFirstBlob.serialize()));

        assertTrue(objectStorage.exists(blob1.getHexhash()));
        assertTrue(objectStorage.exists(blob2.getHexhash()));
        assertNotEquals(commit1.getHexhash(), commit2.getHexhash());
        assertNotEquals(loadedFirstCommit.getHash(), loadedSecondCommit.getHash());
    }

}
