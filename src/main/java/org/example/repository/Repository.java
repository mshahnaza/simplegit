package org.example.repository;

import org.example.objects.Blob;
import org.example.objects.Commit;
import org.example.objects.Tree;
import org.example.utils.SHA1Hasher;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class Repository {
    private final Path workingDir;
    private final Path gitDir;
    private final ObjectStorage objectStorage;
    private final RefStorage refStorage;
    private final Index index;

    public Repository(Path workingDir) {
        this.workingDir = workingDir.toAbsolutePath().normalize();
        this.gitDir = workingDir.resolve(".git");
        this.objectStorage = new ObjectStorage(gitDir);
        this.refStorage = new RefStorage(gitDir);
        this.index = new Index(gitDir.resolve("index"));
    }

    public void init() throws IOException {
        if (Files.exists(gitDir)) {
            throw new IllegalStateException("Repository already initialized");
        }

        Files.createDirectories(gitDir.resolve("objects"));
        Files.createDirectories(gitDir.resolve("refs").resolve("heads"));
        Files.createDirectories(gitDir.resolve("refs").resolve("tags"));

        Files.writeString(gitDir.resolve("HEAD"), "ref: refs/heads/master\n");

        index.clear();
        index.save();

        System.out.println("Initialized empty Git repository in " + gitDir);
    }

    public boolean isRepository() {
        return java.nio.file.Files.exists(gitDir) &&
                java.nio.file.Files.isDirectory(gitDir);
    }

    public void add(String filePath) throws IOException {
        index.load();
        if (filePath == null || filePath.trim().isEmpty()) {
            throw new IllegalArgumentException("File path cannot be empty");
        }
        String normalizedPath = normalizePath(filePath);
        if (normalizedPath.startsWith(".git/")) {
            return;
        }
        Map<String, byte[]> headFiles = getHeadFiles();

        Path file = workingDir.resolve(normalizedPath);

        if (!Files.exists(file)) {
            throw new IOException("File does not exist: " + filePath);
        }

        if (Files.isDirectory(file)) {
            try (var stream = Files.walk(file)) {
                stream.filter(Files::isRegularFile)
                        .forEach(subFile -> {
                            try {
                                String relativePath = normalizePath(subFile);
                                addFile(relativePath, headFiles);
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        });
            }
        } else {
            addFile(normalizedPath, headFiles);
        }
        index.save();
    }

    private void addFile(String filePath, Map<String, byte[]> headFiles) throws IOException {
        if (filePath.startsWith(".git/")) {
            return;
        }
        Path file = workingDir.resolve(filePath);
        byte[] content = Files.readAllBytes(file);
        Blob blob = new Blob(content);

        IndexEntry existingEntry = index.getEntry(filePath);
        boolean inHead = headFiles.containsKey(filePath);
        byte[] headHash = headFiles.get(filePath);

        if (existingEntry != null && Arrays.equals(existingEntry.getHash(), blob.getHash())) {
            return;
        }

        if (inHead && headHash != null && Arrays.equals(headHash, blob.getHash())) {
            return;
        }

        objectStorage.store(blob);
        IndexEntry entry = IndexEntry.fromFile(filePath, blob.getHash(), file);
        index.add(entry);

        System.out.println("add '" + filePath + "'");
    }

    public void addAll() throws IOException {
        index.load();
        try (var stream = Files.walk(workingDir)) {
            stream.filter(Files::isRegularFile)
                    .forEach(file -> {
                        try {
                            String relativePath = normalizePath(file);

                            if (relativePath.startsWith(".git")) {
                                return;
                            }

                            add(relativePath);
                        } catch (IOException e) {
                            System.err.println("Failed to add " + file + ": " + e.getMessage());
                        }
                    });
        }
        index.save();
    }


    public void remove(String filePath, boolean cached, boolean force) throws IOException {
        index.load();
        String normalizedPath = normalizePath(filePath);
        Path file = workingDir.resolve(normalizedPath);

        IndexEntry indexEntry = index.getEntry(normalizedPath);
        boolean fileExistsInWorkingDir = Files.exists(file);
        Map<String, byte[]> headFiles = getHeadFiles();
        boolean fileExistsInHead = headFiles.containsKey(normalizedPath);

        if (indexEntry == null && !fileExistsInWorkingDir && !fileExistsInHead) {
            throw new IOException("path '" + filePath + "' did not match any files");
        }

        if (!force && fileExistsInWorkingDir) {
            if (indexEntry != null && indexEntry.isModified(file)) {
                throw new IOException("the following file has local modifications:\n    " + filePath +
                        "\n(use --force to force removal)");
            }
            if (indexEntry == null && fileExistsInHead) {
                byte[] headHash = headFiles.get(normalizedPath);
                byte[] workingHash = getWorkingFiles().get(normalizedPath);
                if (headHash != null && workingHash != null && !Arrays.equals(headHash, workingHash)) {
                    throw new IOException("the following file has local modifications:\n    " + filePath +
                            "\n(use --force to force removal)");
                }
            }
        }

        if (!cached && fileExistsInWorkingDir) {
            try {
                Files.delete(file);
            } catch (IOException e) {
                throw new IOException("unable to remove '" + normalizedPath + "': " + e.getMessage());
            }
        }

        if (fileExistsInHead && !cached) {
            IndexEntry removalEntry = createRemovalEntry(normalizedPath);
            index.add(removalEntry);
        }

        if (indexEntry != null) {
            index.remove(normalizedPath);
        }
        index.save();
        if (cached) {
            System.out.println("removed from index: " + filePath);
        } else {
            System.out.println("removed: " + filePath);
        }
    }

    private IndexEntry createRemovalEntry(String filePath) {
        return new IndexEntry(filePath, new byte[20], 0, 0, System.currentTimeMillis());
    }

    public String commit(String message, String author) throws IOException {
        index.load();
        if (message == null || message.trim().isEmpty()) {
            throw new IllegalArgumentException("Commit message cannot be empty");
        }
        if (author == null || author.trim().isEmpty()) {
            throw new IllegalArgumentException("Author cannot be empty");
        }

        if(index.isEmpty()) throw new IllegalStateException("nothing to commit, working tree clean");

        Tree rootTree = buildTreeFromIndex();
        objectStorage.store(rootTree);

        String parentHash = refStorage.getHeadCommit();
        List<byte[]> parents = new ArrayList<>();
        if(parentHash != null) parents.add(SHA1Hasher.fromHex(parentHash));

        String timestamp = String.valueOf(System.currentTimeMillis() / 1000);
        String fullAuthor = author + " " + timestamp + " +0000";

        Commit commit = new Commit(
                rootTree.getHash(),
                parents,
                fullAuthor,
                fullAuthor,
                message
        );

        objectStorage.store(commit);

        refStorage.updateHeadCommit(commit.getHexhash());

        index.clear();
        index.save();

        String branch = refStorage.getCurrentBranch();
        if (branch == null) branch = "detached HEAD";

        System.out.println("[" + branch + " " + commit.getHexhash().substring(0, 7) + "] " + message);

        return commit.getHexhash();
    }

    private Tree buildTreeFromIndex() throws IOException {
        Map<String, List<IndexEntry>> entriesByDir = new HashMap<>();
        entriesByDir.put("", new ArrayList<>());

        for (IndexEntry entry : index.getEntries()) {
            if (entry.getMode() == 0) {
                continue;
            }
            String path = entry.getPath();
            String fileDir = getDirectoryPath(path);
            entriesByDir.computeIfAbsent(fileDir, k -> new ArrayList<>()).add(entry);
        }

        Set<String> allDirs = new HashSet<>(entriesByDir.keySet());
        for (String dir : new ArrayList<>(allDirs)) {
            String parent = getDirectoryPath(dir);
            while (!parent.isEmpty() && !allDirs.contains(parent)) {
                entriesByDir.put(parent, new ArrayList<>());
                allDirs.add(parent);
                parent = getDirectoryPath(parent);
            }
        }

        List<String> directories = new ArrayList<>(entriesByDir.keySet());
        directories.sort((a, b) -> Integer.compare(countSlashes(b), countSlashes(a)));


        Map<String, Tree> trees = new HashMap<>();

        for (String dir : directories) {
            Tree tree = new Tree();

            for (IndexEntry entry : entriesByDir.get(dir)) {
                String entryDir = getDirectoryPath(entry.getPath());
                if (dir.equals(entryDir)) {
                    String fileName = getFileName(entry.getPath());
                    String mode = (entry.getMode() == IndexEntry.MODE_EXECUTABLE) ? "100755" : "100644";
                    tree.addFile(fileName, entry.getHash());
                }
            }

            for (String subDir : trees.keySet()) {
                if (isDirectChild(dir, subDir)) {
                    Tree subTree = trees.get(subDir);

                    String dirName = getFileName(subDir);
                    tree.addDirectory(dirName, subTree.getHash());
                }
            }

            trees.put(dir, tree);
            objectStorage.store(tree);
        }

        Tree rootTree = trees.get("");
        if (rootTree == null) {
            rootTree = new Tree();
        }

        for (Map.Entry<String, Tree> entry : trees.entrySet()) {
            String dirPath = entry.getKey();
            Tree tree = entry.getValue();

            if (!dirPath.isEmpty() && !dirPath.contains("/")) {
                rootTree.addDirectory(dirPath, tree.getHash());
            }
        }

        objectStorage.store(rootTree);
        return rootTree;
    }

    private String getDirectoryPath(String path) {
        int lastSlash = path.lastIndexOf('/');
        return lastSlash == -1 ? "" : path.substring(0, lastSlash);
    }

    private String getFileName(String path) {
        int lastSlash = path.lastIndexOf('/');
        return lastSlash == -1 ? path : path.substring(lastSlash + 1);
    }

    private int countSlashes(String path) {
        return (int) path.chars().filter(ch -> ch == '/').count();
    }

    private boolean isDirectChild(String parent, String child) {
        if (parent.isEmpty()) {
            return !child.isEmpty() && !child.contains("/");
        }

        if (!child.startsWith(parent + "/")) {
            return false;
        }

        String afterParent = child.substring(parent.length() + 1);
        return !afterParent.contains("/");
    }

    public void createBranch(String branch) throws IOException {
        String headCommit = refStorage.getHeadCommit();
        if(headCommit == null) throw new IllegalStateException("Cannot create branch - no commits yet");
        refStorage.createBranch(branch, headCommit);
        System.out.println("Created branch '" + branch + "'");
    }

    public void deleteBranch(String branchName) throws IOException {
        refStorage.deleteBranch(branchName);
        System.out.println("Deleted branch '" + branchName + "'");
    }

    public List<String> listBranches() throws IOException {
        return refStorage.listBranches();
    }

    public String getCurrentBranch() throws IOException {
        return refStorage.getCurrentBranch();
    }

    public void checkout(String branch) throws IOException {
        if(refStorage.branchExists(branch)) {
            checkoutBranch(branch);
        } else if (branch.matches("[0-9a-f]{40}")) {
            checkoutCommit(branch);
        } else {
            throw new IllegalArgumentException(
                    "error: pathspec '" + branch + "' did not match any file(s) known to git\n" +
                            "Did you mean to create a new branch? Use: git checkout -b " + branch
            );
        }
    }

    private void checkoutBranch(String branch) throws IOException {
        String commitHash = refStorage.getBranchCommit(branch);
        Commit commit = (Commit) objectStorage.load(commitHash);

        clearWorkingDirectory();

        restoreTree(commit.getTreeHash(), workingDir);

        refStorage.setHead(branch);
        System.out.println("Switched to branch '" + branch + "'");
    }

    private void checkoutCommit(String commitHash) throws IOException {
        if (!commitHash.matches("[0-9a-f]{40}")) {
            throw new IllegalArgumentException("Invalid commit hash: " + commitHash);
        }

        if (!objectStorage.exists(commitHash)) {
            throw new IOException("Commit not found: " + commitHash);
        }

        Commit commit = (Commit) objectStorage.load(commitHash);

        clearWorkingDirectory();
        restoreTree(commit.getTreeHash(), workingDir);
        refStorage.setDetachedHead(commitHash);

        System.out.println("Note: switching to detached HEAD state");
        System.out.println("HEAD is now at " + commitHash.substring(0, 7));
    }

    private void restoreTree(byte[] treeHash, Path currDir) throws IOException {
        Tree tree = (Tree) objectStorage.load(SHA1Hasher.toHex(treeHash));

        for(Tree.Entry entry : tree.getEntries()) {
            if(entry.getType().equals("blob")) {
                Blob blob = (Blob) objectStorage.load(entry.getHexHash());
                Path filePath = currDir.resolve(entry.getName());

                Path parent = filePath.getParent();
                if (parent != null) {
                    Files.createDirectories(parent);
                }
                Files.write(filePath, blob.serialize());
            } else if(entry.getType().equals("tree")) {
                Path newDir = currDir.resolve(entry.getName());
                Files.createDirectories(newDir);
                restoreTree(entry.getHash(), newDir);
            }
        }
    }

    private void clearWorkingDirectory() throws IOException {
        if (hasUncommittedChanges()) {
            throw new IOException(
                    "Your local changes would be lost. Please commit or stash them first."
            );
        }

        Map<String, byte[]> headFiles = getHeadFiles();
        for (String filePath : headFiles.keySet()) {
            Path file = workingDir.resolve(filePath);
            if (Files.exists(file)) {
                try {
                    Files.delete(file);
                    deleteEmptyParentDirectories(file.getParent());
                } catch (IOException e) {
                }
            }
        }
        try (var stream = Files.walk(workingDir)) {
            stream.filter(Files::isRegularFile)
                    .filter(path -> !path.startsWith(gitDir))
                    .forEach(file -> {
                        try {
                            String relativePath = normalizePath(file);
                            if (!headFiles.containsKey(relativePath)) {
                                Files.delete(file);
                                deleteEmptyParentDirectories(file.getParent());
                            }
                        } catch (IOException e) {
                        }
                    });
        }
    }

    private void deleteEmptyParentDirectories(Path dir) throws IOException {
        while (dir != null && !dir.equals(workingDir) && Files.exists(dir)) {
            try (var stream = Files.list(dir)) {
                if (!stream.findAny().isPresent()) {
                    Files.delete(dir);
                    dir = dir.getParent();
                } else {
                    break;
                }
            }
        }
    }

    private boolean hasUncommittedChanges() throws IOException {
        index.load();

        for (IndexEntry entry : index.getEntries()) {
            Path file = workingDir.resolve(entry.getPath());
            if (!Files.exists(file)) {
                return true;
            }
            if (entry.isModified(file)) {
                return true;
            }
        }

        return false;
    }

    public List<Commit> log() throws IOException {
        List<Commit> result = new ArrayList<>();

        String headHash = refStorage.getHeadCommit();
        if (headHash == null) return result;

        List<Commit> allCommits = new ArrayList<>();
        collectAllCommits(headHash, allCommits, new HashSet<>());

        allCommits.sort((c1, c2) -> Long.compare(
                extractTimestamp(c2),
                extractTimestamp(c1)
        ));

        return allCommits;
    }

    private void collectAllCommits(String commitHash, List<Commit> result, Set<String> visited)
            throws IOException {
        if (visited.contains(commitHash) || !objectStorage.exists(commitHash)) {
            return;
        }

        Commit commit = (Commit) objectStorage.load(commitHash);
        result.add(commit);
        visited.add(commitHash);

        for (byte[] parentHash : commit.getParentHashes()) {
            if (parentHash != null) {
                collectAllCommits(SHA1Hasher.toHex(parentHash), result, visited);
            }
        }
    }

    private long extractTimestamp(Commit commit) {
        String signature = commit.getCommitter() != null ? commit.getCommitter() : commit.getAuthor();
        if (signature == null) return 0L;

        String[] parts = signature.split(" ");
        for (int i = parts.length - 2; i >= 0; i--) {
            try {
                return Long.parseLong(parts[i]);
            } catch (NumberFormatException ignored) {}
        }
        return 0L;
    }

    public void status() throws IOException {
        index.load();
        String branch = refStorage.getCurrentBranch();
        if (branch == null) branch = "detached HEAD";

        System.out.println("On branch " + branch);
        System.out.println();

        Map<String, byte[]> rawH = getHeadFiles();
        Map<String, byte[]> rawI = getIndexFiles();
        Map<String, byte[]> rawW = getWorkingFiles();

        Map<String, byte[]> H = normalizePaths(rawH);
        Map<String, byte[]> I = normalizePaths(rawI);
        Map<String, byte[]> W = normalizePaths(rawW);

        Set<String> allPaths = new TreeSet<>();
        allPaths.addAll(H.keySet());
        allPaths.addAll(I.keySet());
        allPaths.addAll(W.keySet());

        Set<String> stagedAdded = new LinkedHashSet<>();
        Set<String> stagedModified = new LinkedHashSet<>();
        Set<String> stagedDeleted = new LinkedHashSet<>();

        Set<String> unstagedModified = new LinkedHashSet<>();
        Set<String> unstagedDeleted = new LinkedHashSet<>();

        Set<String> untracked = new LinkedHashSet<>();

        for (String path : allPaths) {
            byte[] h = H.get(path);
            byte[] i = I.get(path);
            byte[] w = W.get(path);

            boolean inHead = h != null;
            boolean inIndex = i != null;
            boolean inWork = w != null;

            if (!inHead && inIndex) {
                stagedAdded.add(path);
                if (inWork && !hashesEqual(i, w)) {
                    unstagedModified.add(path);
                }
                continue;
            }

            if (!inHead && !inIndex && inWork) {
                untracked.add(path);
                continue;
            }

            if (inHead) {
                if (!inIndex && !inWork) {
                    stagedDeleted.add(path);
                    continue;
                }

                if (inIndex && !inWork) {
                    if (hashesEqual(h, i)) {
                        unstagedDeleted.add(path);
                    } else {
                        stagedDeleted.add(path);
                    }
                    continue;
                }

                if (!inIndex && inWork) {
                    if (!hashesEqual(h, w)) {
                        unstagedModified.add(path);
                    }
                    continue;
                }

                if (inIndex && inWork) {
                    if (!hashesEqual(i, h)) {
                        stagedModified.add(path);
                    }
                    if (!hashesEqual(w, i)) {
                        unstagedModified.add(path);
                    }
                    continue;
                }
            }
        }

        printStatusDetailed(stagedAdded, stagedModified, stagedDeleted,
                unstagedModified, unstagedDeleted, untracked);
    }

    private Map<String, byte[]> normalizePaths(Map<String, byte[]> input) {
        Map<String, byte[]> out = new HashMap<>();
        for (Map.Entry<String, byte[]> e : input.entrySet()) {
            String normalized = e.getKey().replace(File.separatorChar, '/');
            out.put(normalized, e.getValue());
        }
        return out;
    }

    private boolean hashesEqual(byte[] a, byte[] b) {
        if (a == b) return true;
        if (a == null || b == null) return false;
        return Arrays.equals(a, b);
    }

    private void printStatusDetailed(Set<String> stagedAdded,
                                     Set<String> stagedModified,
                                     Set<String> stagedDeleted,
                                     Set<String> unstagedModified,
                                     Set<String> unstagedDeleted,
                                     Set<String> untracked) {

        boolean hasStaged = !stagedAdded.isEmpty() || !stagedModified.isEmpty() || !stagedDeleted.isEmpty();
        boolean hasUnstaged = !unstagedModified.isEmpty() || !unstagedDeleted.isEmpty();
        boolean hasUntracked = !untracked.isEmpty();

        if (hasStaged) {
            System.out.println("Changes to be committed:");
            System.out.println("  (use \"git restore --staged <file>...\" to unstage)");
            stagedAdded.forEach(p -> System.out.println("    new file:   " + p));
            stagedModified.forEach(p -> System.out.println("    modified:   " + p));
            stagedDeleted.forEach(p -> System.out.println("    deleted:    " + p));
            System.out.println();
        }

        if (hasUnstaged) {
            System.out.println("Changes not staged for commit:");
            System.out.println("  (use \"git add <file>...\" to update what will be committed)");
            System.out.println("  (use \"git restore <file>...\" to discard changes in working directory)");
            unstagedModified.forEach(p -> System.out.println("    modified:   " + p));
            unstagedDeleted.forEach(p -> System.out.println("    deleted:    " + p));
            System.out.println();
        }

        if (hasUntracked) {
            System.out.println("Untracked files:");
            System.out.println("  (use \"git add <file>...\" to include in what will be committed)");
            untracked.forEach(p -> System.out.println("    " + p));
            System.out.println();
        }

        if (!hasStaged && !hasUnstaged && !hasUntracked) {
            System.out.println("nothing to commit, working tree clean");
        }
    }

    private Map<String, byte[]> getIndexFiles() {
        Map<String, byte[]> indexFiles = new HashMap<>();

        for (IndexEntry entry : index.getEntries()) {
            indexFiles.put(entry.getPath(), entry.getHash());
        }

        return indexFiles;
    }

    private Map<String, byte[]> getWorkingFiles() throws IOException {
        Map<String, byte[]> workingFiles = new HashMap<>();

        try (var stream = Files.walk(workingDir)) {
            stream.filter(Files::isRegularFile)
                    .filter(path -> !path.startsWith(gitDir))
                    .forEach(file -> {
                        try {
                            String relativePath = normalizePath(file);
                            if (!relativePath.startsWith(".git")) {
                                byte[] content = Files.readAllBytes(file);
                                Blob blob = new Blob(content);
                                workingFiles.put(relativePath, blob.getHash());
                            }
                        } catch (IOException e) {
                        }
                    });
        }

        return workingFiles;
    }

    private Map<String, byte[]> getHeadFiles() throws IOException {
        Map<String, byte[]> headFiles = new HashMap<>();

        String headCommitHash = refStorage.getHeadCommit();
        if (headCommitHash == null) {
            return headFiles;
        }

        Commit headCommit = (Commit) objectStorage.load(headCommitHash);
        Tree headTree = (Tree) objectStorage.load(SHA1Hasher.toHex(headCommit.getTreeHash()));

        collectFilesFromTree(headTree, "", headFiles);

        return headFiles;
    }

    private void collectFilesFromTree(Tree tree, String currentPath, Map<String, byte[]> files) throws IOException {
        for (Tree.Entry entry : tree.getEntries()) {
            String fullPath = currentPath.isEmpty() ? entry.getName() : currentPath + "/" + entry.getName();

            if ("blob".equals(entry.getType())) {
                files.put(fullPath, entry.getHash());
            } else if ("tree".equals(entry.getType())) {
                Tree subTree = (Tree) objectStorage.load(entry.getHexHash());
                collectFilesFromTree(subTree, fullPath, files);
            }
        }
    }

    private boolean isFileInHead(String filePath) throws IOException {
        Map<String, byte[]> headFiles = getHeadFiles();
        return headFiles.containsKey(filePath);
    }

    private String normalizePath(String path) {
        Path resolved = workingDir.resolve(path);
        return workingDir.relativize(resolved).toString().replace(File.separatorChar, '/');
    }

    private String normalizePath(Path path) {
        return workingDir.relativize(path).toString().replace(File.separatorChar, '/');
    }
}
