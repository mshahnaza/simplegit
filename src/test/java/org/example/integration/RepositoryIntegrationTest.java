package org.example.integration;

import org.example.objects.Commit;
import org.example.repository.Repository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RepositoryIntegrationTest {

    @TempDir
    Path tempDir;

    private Repository repo;

    @BeforeEach
    void setUp() {
        repo = new Repository(tempDir);
    }

    @Test
    void shouldInitializeRepository() throws IOException {
        repo.init();

        assertTrue(Files.exists(tempDir.resolve(".git")));
        assertTrue(Files.exists(tempDir.resolve(".git/objects")));
        assertTrue(Files.exists(tempDir.resolve(".git/refs/heads")));
        assertTrue(Files.exists(tempDir.resolve(".git/HEAD")));
    }

    @Test
    void shouldCreateAndRetrieveCommits() throws IOException {
        repo.init();

        Path testFile = tempDir.resolve("test.txt");
        Files.write(testFile, "Hello, Git!".getBytes());
        repo.add("test.txt");

        String commitHash = repo.commit("init commit", "Test User <test@example.com>");

        assertNotNull(commitHash);
        assertEquals(40, commitHash.length());

        List<Commit> log = repo.log();
        assertEquals(1, log.size());
        assertEquals("init commit", log.get(0).getMessage());
        assertTrue(log.get(0).getAuthor().startsWith("Test User <test@example.com>"));
    }

    @Test
    void shouldManageBranches() throws IOException {
        repo.init();

        Path testFile = tempDir.resolve("test.txt");
        Files.write(testFile, "content".getBytes());
        repo.add("test.txt");
        repo.commit("init commit", "Test User <test@example.com>");

        repo.createBranch("feature");
        repo.createBranch("develop");

        List<String> branches = repo.listBranches();
        assertTrue(branches.contains("feature"));
        assertTrue(branches.contains("develop"));

        repo.checkout("feature");
        assertEquals("feature", repo.getCurrentBranch());

        repo.checkout("develop");
        assertEquals("develop", repo.getCurrentBranch());
    }

    @Test
    void shouldHandleFileLifecycle() throws IOException {
        repo.init();

        Path testFile = tempDir.resolve("test.txt");

        Files.write(testFile, "content".getBytes());
        repo.add("test.txt");
        repo.commit("Add file", "Test User <test@example.com>");

        Files.write(testFile, "modified content".getBytes());
        repo.add("test.txt");
        repo.commit("Modified file", "Test User <test@example.com>");

        repo.remove("test.txt", false, false);
        repo.commit("Remove file", "Test User <test@example.com>");

        List<Commit> log = repo.log();
        assertEquals(3, log.size());
        assertEquals("Remove file", log.get(0).getMessage());
        assertEquals("Modified file", log.get(1).getMessage());
        assertEquals("Add file", log.get(2).getMessage());
    }

    @Test
    void shouldDetectRepositoryState() throws IOException {
        repo.init();

        assertDoesNotThrow(() -> repo.status());

        Path testFile = tempDir.resolve("test.txt");
        Files.write(testFile, "content".getBytes());
        repo.add("test.txt");

        assertDoesNotThrow(() -> repo.status());

        repo.commit("Initial commit", "Test User <test@example.com>");
        assertDoesNotThrow(() -> repo.status());
    }

    @Test
    void shouldHandleComplexWorkflows() throws IOException {
        repo.init();

        Files.createDirectories(tempDir.resolve("src/main"));
        Files.createDirectories(tempDir.resolve("src/test"));

        Files.write(tempDir.resolve("src/main/Main.java"), "class Main {}".getBytes());
        Files.write(tempDir.resolve("src/test/Test.java"), "class Test {}".getBytes());
        Files.write(tempDir.resolve("README.md"), "# Project".getBytes());

        repo.addAll();
        String commitHash = repo.commit("Initial project structure", "Test User <test@example.com>");

        repo.createBranch("feature");
        repo.checkout("feature");

        Files.write(tempDir.resolve("src/main/Main.java"), "class Main { void feature() {} }".getBytes());
        repo.add("src/main/Main.java");
        repo.commit("Add feature method", "Test User <test@example.com>");

        repo.checkout("master");

        String content = Files.readString(tempDir.resolve("src/main/Main.java"));
        assertEquals("class Main {}", content);

        repo.checkout("feature");
        content = Files.readString(tempDir.resolve("src/main/Main.java"));
        assertEquals("class Main { void feature() {} }", content);
    }

    @Test
    void shouldPreventDataLoss() throws IOException {
        repo.init();

        Path testFile = tempDir.resolve("test.txt");
        Files.write(testFile, "content".getBytes());
        repo.add("test.txt");
        repo.commit("Initial commit", "Test User <test@example.com>");

        Files.write(testFile, "modified first time".getBytes());
        repo.add("test.txt");
        Files.write(testFile, "modified second time, not in index".getBytes());

        repo.createBranch("feature");

        assertThrows(IOException.class,
                () -> repo.checkout("feature"),
                "Should prevent checkout with uncommitted changes");
    }

    @Test
    void shouldMaintainCommitHistory() throws IOException {
        repo.init();

        Path file = tempDir.resolve("file.txt");

        for (int i = 1; i <= 5; i++) {
            Files.write(file, ("v" + i).getBytes());
            repo.add("file.txt");
            repo.commit("Commit " + i, "User <user@example.com>");
        }

        List<Commit> log = repo.log();

        assertEquals(5, log.size());

        for (int i = 0; i < 5; i++) {
            assertEquals("Commit " + (5 - i), log.get(i).getMessage());
        }
    }

    @Test
    void shouldHandleEmptyRepository() throws IOException {
        repo.init();

        assertTrue(repo.listBranches().isEmpty());
        assertTrue(repo.log().isEmpty());
        assertDoesNotThrow(() -> repo.status());

        assertThrows(IllegalStateException.class, () -> repo.createBranch("feature"));
    }
}