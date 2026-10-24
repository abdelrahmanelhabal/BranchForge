package branchforge.store;

import branchforge.BranchForgeException;
import branchforge.model.Commit;
import branchforge.util.Utils;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CommitStore {

    private static final int FULL_ID_LENGTH = 40;
    private final File objectsDir;

    public CommitStore(File objectsDir) {
        if (objectsDir == null) {
            throw new IllegalArgumentException("objectsDir cannot be null.");
        }
        this.objectsDir = objectsDir.getName().equals("commits")
                ? objectsDir
                : new File(objectsDir, "commits");
    }
    public void save(Commit c) {
        if (c == null) {
            throw new IllegalArgumentException("Commit cannot be null.");
        }
        String id = c.getId();
        validateId(id);
        if (!objectsDir.exists()) {
            objectsDir.mkdirs();
        }
        File target = new File(objectsDir, id);
        if (target.exists() && target.isFile()) {
            return;
        }
        Utils.writeObject(target, c);
    }

    public Commit load(String id) {
        String resolvedId = resolveId(id);
        File target = new File(objectsDir, resolvedId);
        return Utils.readObject(target, Commit.class);
    }

    public boolean exists(String id) {
        validateId(id);
        if (id.length() > FULL_ID_LENGTH) {
            return false;
        }
        if (id.length() == FULL_ID_LENGTH) {
            File target = new File(objectsDir, id);
            return target.exists() && target.isFile();
        }
        return findMatchingIds(id).size() == 1;
    }

    public List<String> listAll() {
        if (!objectsDir.exists() || !objectsDir.isDirectory()) {
            return Collections.emptyList();
        }
        File[] files = objectsDir.listFiles(File::isFile);
        if (files == null || files.length == 0) {
            return Collections.emptyList();
        }
        List<String> ids = new ArrayList<>(files.length);
        for (File f : files) {
            ids.add(f.getName());
        }
        Collections.sort(ids);
        return ids;
    }

    public List<Commit> loadAll() {
        List<String> ids = listAll();
        List<Commit> commits = new ArrayList<>(ids.size());
        for (String id : ids) {
            File target = new File(objectsDir, id);
            commits.add(Utils.readObject(target, Commit.class));
        }
        return commits;
    }

    private String resolveId(String id) {
        validateId(id);
        if (id.length() > FULL_ID_LENGTH) {
            throw new BranchForgeException("No commit with that id exists.");
        }
        if (id.length() == FULL_ID_LENGTH) {
            File target = new File(objectsDir, id);
            if (!target.exists() || !target.isFile()) {
                throw new BranchForgeException("No commit with that id exists.");
            }
            return id;
        }

        List<String> matches = findMatchingIds(id);
        if (matches.isEmpty()) {
            throw new BranchForgeException("No commit with that id exists.");
        }
        if (matches.size() > 1) {
            throw new BranchForgeException("Ambiguous commit id prefix: " + id);
        }
        return matches.get(0);
    }

    private List<String> findMatchingIds(String prefix) {
        List<String> matches = new ArrayList<>();
        for (String commitId : listAll()) {
            if (commitId.startsWith(prefix)) {
                matches.add(commitId);
                if (matches.size() > 1) {
                    break;
                }
            }
        }
        return matches;
    }

    private void validateId(String id) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Commit ID cannot be null or blank.");
        }
        if (id.contains("/") || id.contains("\\") || id.equals(".") || id.contains("..")) {
            throw new IllegalArgumentException("Invalid Commit ID (path traversal detected): " + id);
        }
    }
}