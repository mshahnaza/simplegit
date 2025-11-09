package org.example.repository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class RefStorage {
    private final Path gitDir;

    public RefStorage(Path gitDir) {
        this.gitDir = gitDir;
    }

    private Path refsDir() {
        return gitDir.resolve("refs").resolve("heads");
    }

    private Path headFile() {
        return gitDir.resolve("HEAD");
    }

    public String getHeadRef() throws IOException {
        if (!Files.exists(headFile())) {
            return null;
        }
        String content = Files.readString(headFile()).trim();
        if(content.startsWith("ref: ")) return content.substring(5);
        return null;
    }

    public String getCurrentBranch() throws IOException {
        String ref = getHeadRef();
        if (ref != null && ref.startsWith("refs/heads/")) {
            return ref.substring("refs/heads/".length());
        }
        return null;
    }

    public String getHeadCommit() throws IOException {
        if (!Files.exists(headFile())) {
            return null;
        }
        String ref = getHeadRef();
        if (ref == null) {
            String content = Files.readString(headFile()).trim();
            return content.matches("[0-9a-f]{40}") ? content : null;
        }
        Path refPath = gitDir.resolve(ref);
        if (Files.exists(refPath)) return Files.readString(refPath).trim();
        return null;
    }

    public void updateHeadCommit(String commitHash) throws IOException {
        if (commitHash == null || !commitHash.matches("[0-9a-f]{40}")) {
            throw new IllegalArgumentException("Invalid commit hash: " + commitHash);
        }
        String ref = getHeadRef();
        if (ref == null) Files.writeString(headFile(), commitHash + "\n");
        else {
            Path refPath = gitDir.resolve(ref);
            Files.writeString(refPath, commitHash + "\n");
        }
    }

    public void createBranch(String branchName, String commitHash) throws IOException {
        if (branchName == null || branchName.isEmpty()) {
            throw new IllegalArgumentException("Branch name cannot be empty");
        }
        if (commitHash == null || !commitHash.matches("[0-9a-f]{40}")) {
            throw new IllegalArgumentException("Invalid commit hash: " + commitHash);
        }
        Path branchPath = refsDir().resolve(branchName);
        if (Files.exists(branchPath)) {
            throw new IOException("Branch already exists: " + branchName);
        }
        Files.createDirectories(branchPath.getParent());
        Files.writeString(branchPath, commitHash + "\n");
    }

    public void deleteBranch(String branchName) throws IOException {
        if (branchName == null || branchName.isEmpty()) {
            throw new IllegalArgumentException("Branch name cannot be empty");
        }

        String currentBranch = getCurrentBranch();
        if (branchName.equals(currentBranch)) {
            throw new IOException("Cannot delete current branch: " + branchName);
        }

        Path branchPath = refsDir().resolve(branchName);

        if (!Files.exists(branchPath)) {
            throw new IOException("Branch does not exist: " + branchName);
        }

        Files.delete(branchPath);
    }

    public List<String> listBranches() throws IOException {
        if (!Files.exists(refsDir())) {
            return List.of();
        }

        try (Stream<Path> paths = Files.walk(refsDir())) {
            return paths
                    .filter(Files::isRegularFile)
                    .map(refsDir()::relativize)
                    .map(Path::toString)
                    .collect(Collectors.toList());
        }
    }

    public void updateBranch(String branchName, String commitHash) throws IOException {
        if (branchName == null || branchName.isEmpty()) {
            throw new IllegalArgumentException("Branch name cannot be empty");
        }
        if (commitHash == null || !commitHash.matches("[0-9a-f]{40}")) {
            throw new IllegalArgumentException("Invalid commit hash: " + commitHash);
        }

        Path branchPath = refsDir().resolve(branchName);
        Files.createDirectories(branchPath.getParent());
        Files.writeString(branchPath, commitHash + "\n");
    }

    public void setHead(String branchName) throws IOException {
        if (branchName == null || branchName.isEmpty()) {
            throw new IllegalArgumentException("Branch name cannot be empty");
        }

        Path branchPath = refsDir().resolve(branchName);
        if (!Files.exists(branchPath)) {
            throw new IOException("Branch does not exist: " + branchName);
        }

        String ref = "refs/heads/" + branchName;
        Files.writeString(headFile(), "ref: " + ref + "\n");
    }

    public void setDetachedHead(String commitHash) throws IOException {
        if (commitHash == null || !commitHash.matches("[0-9a-f]{40}")) {
            throw new IllegalArgumentException("Invalid commit hash: " + commitHash);
        }

        Files.writeString(headFile(), commitHash + "\n");
    }

    public boolean isDetachedHead() throws IOException {
        return getHeadRef() == null && getHeadCommit() != null;
    }

    public String getBranchCommit(String branchName) throws IOException {
        Path branchPath = refsDir().resolve(branchName);

        if (!Files.exists(branchPath)) {
            return null;
        }

        return Files.readString(branchPath).trim();
    }

    public boolean branchExists(String branchName) {
        Path branchPath = refsDir().resolve(branchName);
        return Files.exists(branchPath);
    }
}
