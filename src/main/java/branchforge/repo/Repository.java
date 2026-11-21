package branchforge.repo;
import branchforge.BranchForgeException;
import branchforge.model.*;
import branchforge.store.*;
import java.io.File;
public class Repository {
    private final File rootDir;
    private final File forgeDir;
    private final File objectsDir;
    private StateStore stateStore;
    private WorkingDirectory workingDir;
    private StagingArea staging;
    private CommitStore commitStore;
    private BlobStore blobStore;
    private BranchManager branchManager;
    private HeadPointer head;
    private RemoteManager remoteManager;

    public Repository(File rootDir) {
        if (rootDir == null || (rootDir.exists() && !rootDir.isDirectory())) {
            throw new BranchForgeException("Repository root must be a valid directory.");
        }
        this.rootDir = rootDir;
        this.forgeDir = new File(rootDir, ".branchforge");
        this.objectsDir = new File(forgeDir, "objects");
    }

    public void init() {
        if (forgeDir.exists()) {
            throw new BranchForgeException(
                    "A BranchForge version-control system already exists in the current directory.");
        }
        if (!objectsDir.exists() && !objectsDir.mkdirs()) {
            throw new BranchForgeException("Failed to create repository directories.");
        }
        this.workingDir = new WorkingDirectory(rootDir);
        this.stateStore = new StateStore(forgeDir);
        this.commitStore = new CommitStore(objectsDir);
        this.blobStore = new BlobStore(objectsDir);
        this.staging = new StagingArea();
        this.branchManager = new BranchManager();
        this.head = new HeadPointer("master");
        this.remoteManager = new RemoteManager();
        Commit initialCommit = CommitFactory.initialCommit();
        commitStore.save(initialCommit);
        branchManager.create("master", initialCommit.getId());
        persist();
    }

    public static Repository load(File rootDir) {
        Repository repo = new Repository(rootDir);
        if (!repo.isInitialized()) {
            throw new BranchForgeException("Not in an initialized BranchForge directory.");
        }
        repo.workingDir = new WorkingDirectory(rootDir);
        repo.stateStore = new StateStore(repo.forgeDir);
        repo.commitStore = new CommitStore(repo.objectsDir);
        repo.blobStore = new BlobStore(repo.objectsDir);

        repo.staging = repo.stateStore.load("staging", StagingArea.class);
        repo.branchManager = repo.stateStore.load("branches", BranchManager.class);
        repo.head = repo.stateStore.load("head", HeadPointer.class);
        repo.remoteManager = repo.stateStore.load("remotes", RemoteManager.class);

        return repo;
    }

    public boolean isInitialized() {
        return forgeDir.isDirectory()
                && objectsDir.isDirectory()
                && new File(forgeDir, "head").isFile()
                && new File(forgeDir, "branches").isFile()
                && new File(forgeDir, "staging").isFile()
                && new File(forgeDir, "remotes").isFile();
    }
    public void persist() {
        if (stateStore == null) {
            throw new BranchForgeException("Repository is not initialized or loaded.");
        }
        stateStore.save("staging", staging);
        stateStore.save("branches", branchManager);
        stateStore.save("head", head);
        stateStore.save("remotes", remoteManager);
    }
    private void ensureReady() {
        if (stateStore == null) {
            throw new BranchForgeException("Repository is not initialized or loaded.");
        }
    }

    public File getRootDir() { return rootDir; }
    public WorkingDirectory getWorkingDir() { ensureReady(); return workingDir; }
    public StagingArea getStaging() { ensureReady(); return staging; }
    public CommitStore getCommitStore() { ensureReady(); return commitStore; }
    public BlobStore getBlobStore() { ensureReady(); return blobStore; }
    public BranchManager getBranchManager() { ensureReady(); return branchManager; }
    public HeadPointer getHead() { ensureReady(); return head; }
    public RemoteManager getRemoteManager() { ensureReady(); return remoteManager; }


    public CommitGraph getGraph() {
        if (commitStore == null) {
            throw new BranchForgeException("Repository is not initialized or loaded.");
        }
        CommitGraph g = new CommitGraph();
        g.rebuild(commitStore.loadAll());
        return g;
    }
}
