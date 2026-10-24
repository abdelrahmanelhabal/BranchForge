package branchforge.store;

import branchforge.BranchForgeException;
import branchforge.util.Utils;

import java.io.File;
import java.io.Serializable;

public class StateStore {

    private final File branchforgeDir;

    public StateStore(File branchforgeDir) {
        if (branchforgeDir == null) {
            throw new IllegalArgumentException("branchforgeDir cannot be null.");
        }
        if (branchforgeDir.exists() && !branchforgeDir.isDirectory()) {
            throw new IllegalArgumentException("branchforgeDir exists but is not a directory.");
        }
        this.branchforgeDir = branchforgeDir;
    }

    public void save(String name, Serializable state) {
        validateName(name);
        if (state == null) {
            throw new IllegalArgumentException("State object cannot be null.");
        }
        if (!branchforgeDir.exists()) {
            if (!branchforgeDir.mkdirs()) {
                throw new BranchForgeException("Failed to create directory: " + branchforgeDir.getAbsolutePath());
            }
        }
        File target = new File(branchforgeDir, name);
        Utils.writeObject(target, state);
    }

    public <T extends Serializable> T load(String name, Class<T> type) {
        validateName(name);
        if (type == null) {
            throw new IllegalArgumentException("Target type class cannot be null.");
        }
        File target = new File(branchforgeDir, name);
        if (!target.exists() || !target.isFile()) {
            throw new BranchForgeException("State file does not exist: " + name);
        }
        return Utils.readObject(target, type);
    }

    public boolean exists(String name) {
        validateName(name);
        File target = new File(branchforgeDir, name);
        return target.exists() && target.isFile();
    }

    private void validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("State file name cannot be null or blank.");
        }
        if (name.contains("/") || name.contains("\\") || name.equals(".") || name.contains("..")) {
            throw new IllegalArgumentException("Invalid state file name: " + name);
        }
    }
}