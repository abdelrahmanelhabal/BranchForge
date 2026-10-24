package branchforge.store;

import branchforge.BranchForgeException;
import branchforge.model.Blob;
import branchforge.util.Utils;

import java.io.File;
public class BlobStore {
    private final File objectsDir;
    public BlobStore(File objectsDir) {
        if (objectsDir == null) {
            throw new IllegalArgumentException("objectsDir cannot be null.");
        }
        this.objectsDir = objectsDir.getName().equals("blobs") ? objectsDir : new File(objectsDir, "blobs");
    }
    public void save(Blob b) {
        if (b == null) {
            throw new IllegalArgumentException("Blob cannot be null.");
        }
        String id = b.getId();
        validateId(id);
        if (exists(id)) {
            return;
        }
        if (!objectsDir.exists()) {
            if (!objectsDir.mkdirs() && !objectsDir.exists()) {
                throw new BranchForgeException("Failed to create directory: " + objectsDir.getAbsolutePath());
            }
        }
        File target = new File(objectsDir, id);
        Utils.writeObject(target, b);
    }
    public Blob load(String id) {
        if (!exists(id)) {
            throw new BranchForgeException("Blob does not exist: " + id);
        }
        File target = new File(objectsDir, id);
        return Utils.readObject(target, Blob.class);
    }
    public boolean exists(String id) {
        validateId(id);
        File target = new File(objectsDir, id);
        return target.exists() && target.isFile();
    }
    private void validateId(String id) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Blob ID cannot be null or blank.");
        }
        if (id.contains("/") || id.contains("\\") || id.equals(".") || id.contains("..")) {
            throw new IllegalArgumentException("Invalid Blob ID (path traversal detected): " + id);
        }
    }

}
