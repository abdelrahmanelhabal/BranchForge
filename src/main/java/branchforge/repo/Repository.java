package branchforge.repo;
import branchforge.BranchForgeException;
import branchforge.model.*;
import branchforge.store.*;
import java.io.File;
import java.util.Date;
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
    private CommitGraph graph;
    private RemoteManager remoteManager;

    public Repository(File rootDir) {
        this.rootDir = rootDir;
        this.forgeDir = new File(rootDir, ".branchforge");
        this.objectsDir = new File(forgeDir, "objects");
    }

    public void init() {
        if (isInitialized()) {
            throw new BranchForgeException(
                    "A BranchForge version-control system already exists in the current directory.");
        }
        forgeDir.mkdirs();
        objectsDir.mkdirs();

        this.workingDir = new WorkingDirectory(rootDir);
        this.stateStore = new StateStore(forgeDir);
        this.commitStore = new CommitStore(objectsDir);
        this.blobStore = new BlobStore(objectsDir);
        this.staging = new StagingArea();
        this.branchManager = new BranchManager();
        this.head = new HeadPointer("master");
        this.remoteManager = new RemoteManager();
        Commit initialCommit = CommitFactory.fromStaging(null, staging, "initial commit", new Date(0));
        commitStore.save(initialCommit);
        branchManager.create("master", initialCommit.getId());
        head.setCurrentBranch("master");
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
        return forgeDir.isDirectory() && new File(forgeDir, "head").isFile();
    }

    public void persist() {
        stateStore.save("staging", staging);
        stateStore.save("branches", branchManager);
        stateStore.save("head", head);
        stateStore.save("remotes", remoteManager);
    }

    public File getRootDir() { return rootDir; }
    public WorkingDirectory getWorkingDir() { return workingDir; }
    public StagingArea getStaging() { return staging; }
    public CommitStore getCommitStore() { return commitStore; }
    public BlobStore getBlobStore() { return blobStore; }
    public BranchManager getBranchManager() { return branchManager; }
    public HeadPointer getHead() { return head; }
    public RemoteManager getRemoteManager() { return remoteManager; }

    public CommitGraph getGraph() {
        if (graph == null) {
            graph = new CommitGraph();
            graph.rebuild(commitStore.loadAll());
        }
        return graph;
    }
}
