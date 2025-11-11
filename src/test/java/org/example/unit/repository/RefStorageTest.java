package org.example.unit.repository;

import org.example.repository.RefStorage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RefStorageTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldHandleBranchLifecycle() throws IOException {
        RefStorage refs = new RefStorage(tempDir);
        String commitHash = "a".repeat(40);

        refs.createBranch("master", commitHash);
        assertTrue(refs.branchExists("master"));
        assertEquals(commitHash, refs.getBranchCommit("master"));

        refs.setHead("master");
        assertEquals("master", refs.getCurrentBranch());
        assertEquals(commitHash, refs.getHeadCommit());

        String newCommit = "b".repeat(40);
        refs.updateBranch("master", newCommit);
        assertEquals(newCommit, refs.getBranchCommit("master"));

        List<String> branches = refs.listBranches();
        assertEquals(1, branches.size());
        assertEquals("master", branches.get(0));

        refs.createBranch("feature", commitHash);
        assertEquals(2, refs.listBranches().size());
        assertThrows(IOException.class, () -> refs.deleteBranch("master"));

        refs.deleteBranch("feature");
        assertFalse(refs.branchExists("feature"));
    }

    @Test
    void shouldHandleDetachedHead() throws IOException {
        RefStorage refs = new RefStorage(tempDir);
        String commitHash = "c".repeat(40);

        refs.setDetachedHead(commitHash);

        assertTrue(refs.isDetachedHead());
        assertNull(refs.getCurrentBranch());
        assertEquals(commitHash, refs.getHeadCommit());
    }

    @Test
    void shouldUpdateHeadCommit() throws IOException {
        RefStorage refs = new RefStorage(tempDir);

        refs.setDetachedHead("a".repeat(40));
        refs.updateHeadCommit("b".repeat(40));
        assertEquals("b".repeat(40), refs.getHeadCommit());

        refs.createBranch("master", "c".repeat(40));
        refs.setHead("master");
        refs.updateHeadCommit("d".repeat(40));
        assertEquals("d".repeat(40), refs.getHeadCommit());
        assertEquals("d".repeat(40), refs.getBranchCommit("master"));
    }
}