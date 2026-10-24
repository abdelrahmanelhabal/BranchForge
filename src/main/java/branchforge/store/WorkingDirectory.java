package branchforge.store;

import branchforge.BranchForgeException;
import branchforge.model.Blob;
import branchforge.model.Commit;
import branchforge.model.StagingArea;
import branchforge.util.Utils;

import java.io.File;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public class WorkingDirectory {

    private final File root;

    public WorkingDirectory(File root) {
        if (root == null) {
            throw new IllegalArgumentException("Working directory root cannot be null.");
        }
        this.root = root;
    }

    public boolean exists(String file) {
        validateFileName(file);
        File target = new File(root, file);
        return target.exists() && target.isFile();
    }

    public byte[] read(String file) {
        if (!exists(file)) {
            throw new BranchForgeException("File does not exist: " + file);
        }
        return Utils.readContents(new File(root, file));
    }


    public void write(String file, byte[] content) {
        validateFileName(file);
        if (content == null) {
            throw new IllegalArgumentException("File content cannot be null.");
        }
        if (!root.exists() && !root.mkdirs()) {
            throw new BranchForgeException("Could not create working directory: " + root);
        }
        Utils.writeContents(new File(root, file), content);
    }

    public void delete(String file) {
        validateFileName(file);
        File target = new File(root, file);
        if (target.exists() && target.isFile()) {
            if (!target.delete()) {
                throw new BranchForgeException("Could not delete file: " + file);
            }
        }
    }
    public Set<String> listFiles() {
        if (!root.exists() || !root.isDirectory()) {
            return Collections.emptySet();
        }
        File[] files = root.listFiles(File::isFile);
        if (files == null || files.length == 0) {
            return Collections.emptySet();
        }
        Set<String> names = new TreeSet<>();
        for (File f : files) {
            String name = f.getName();
            if (!name.equals(".branchforge")) {
                names.add(name);
            }
        }
        return Collections.unmodifiableSet(names);
    }

    public boolean isTracked(String file, Commit commit) {
        validateFileName(file);
        if (commit == null) {
            return false;
        }
        return commit.getFiles().contains(file);
    }

    public void checkUntrackedOverwrite(Commit target, Commit current, StagingArea staging) {
        if (target == null || current == null || staging == null) {
            throw new IllegalArgumentException("Target commit, current commit, and staging area cannot be null.");
        }
        for (String file : listFiles()) {
            boolean trackedInCurrent = isTracked(file, current);
            boolean stagedForAdd = staging.getStagedFiles().containsKey(file);
            boolean stagedForRemoval = staging.getRemovedFiles().contains(file);

            boolean isUntracked = (!trackedInCurrent && !stagedForAdd) || stagedForRemoval;

            if (isUntracked && isTracked(file, target)) {
                throw new BranchForgeException(
                        "There is an untracked file in the way; delete it, or add and commit it first."
                );
            }
        }
    }

    public void restoreFromCommit(Commit commit, BlobStore blobStore) {
        if (commit == null || blobStore == null) {
            throw new IllegalArgumentException("Commit and BlobStore cannot be null.");
        }
        Map<String, byte[]> filesToWrite = new LinkedHashMap<>();
        for (String file : commit.getFiles()) {
            String blobId = commit.getBlobId(file);
            Blob blob = blobStore.load(blobId);
            filesToWrite.put(file, blob.getContent());
        }

        for (Map.Entry<String, byte[]> entry : filesToWrite.entrySet()) {
            write(entry.getKey(), entry.getValue());
        }
    }
    public void removeFilesNotIn(Commit target, Commit current) {
        if (target == null || current == null) {
            throw new IllegalArgumentException("Target and current commits cannot be null.");
        }
        Set<String> targetFiles = target.getFiles();
        for (String file : current.getFiles()) {
            if (!targetFiles.contains(file)) {
                delete(file);
            }
        }
    }
    private void validateFileName(String file) {
        if (file == null || file.isBlank()) {
            throw new IllegalArgumentException("File name cannot be null or blank.");
        }
        if (file.contains("/") || file.contains("\\") || file.equals(".") || file.equals("..")) {
            throw new IllegalArgumentException("Invalid file name (subdirectories are not supported): " + file);
        }
    }
}