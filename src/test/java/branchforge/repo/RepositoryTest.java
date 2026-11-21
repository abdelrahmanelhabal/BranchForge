package branchforge.repo;

import branchforge.BranchForgeException;
import branchforge.model.Commit;
import branchforge.model.CommitGraph;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;
import branchforge.model.CommitFactory;
import branchforge.repo.Repository;
class RepositoryTest {

    @TempDir
    Path tempDir;

    @Test
    void init_createsDirectoryStructureAndInitialState() {
        File root = tempDir.toFile();
        Repository repo = new Repository(root);

        assertFalse(repo.isInitialized());
        repo.init();

        assertTrue(repo.isInitialized());
        assertTrue(new File(root, ".branchforge").isDirectory());
        assertTrue(new File(root, ".branchforge/objects").isDirectory());
        assertTrue(new File(root, ".branchforge/head").isFile());

        assertEquals(root, repo.getRootDir());
        assertNotNull(repo.getWorkingDir());
        assertNotNull(repo.getStaging());
        assertNotNull(repo.getCommitStore());
        assertNotNull(repo.getBlobStore());
        assertNotNull(repo.getBranchManager());
        assertNotNull(repo.getHead());
        assertNotNull(repo.getRemoteManager());

        assertEquals("master", repo.getHead().getCurrentBranch());
        String masterCommitId = repo.getBranchManager().get("master");
        assertNotNull(masterCommitId);

        Commit initial = repo.getCommitStore().load(masterCommitId);
        assertNotNull(initial);
        assertEquals("initial commit", initial.getMessage());
    }

    @Test
    void init_throwsWhenAlreadyInitialized() {
        Repository repo = new Repository(tempDir.toFile());
        repo.init();

        assertThrows(BranchForgeException.class, repo::init);
    }

    @Test
    void isInitialized_returnsFalseIfHeadFileIsMissing() {
        File forgeDir = new File(tempDir.toFile(), ".branchforge");
        assertTrue(forgeDir.mkdirs());

        Repository repo = new Repository(tempDir.toFile());
        assertFalse(repo.isInitialized());
    }

    @Test
    void load_throwsWhenDirectoryIsNotInitialized() {
        assertThrows(BranchForgeException.class, () -> Repository.load(tempDir.toFile()));
    }

    @Test
    void load_restoresPersistedRepositoryState() {
        File root = tempDir.toFile();
        Repository original = new Repository(root);
        original.init();
        Repository loaded = Repository.load(root);
        assertTrue(loaded.isInitialized());
        assertEquals(root, loaded.getRootDir());
        assertEquals("master", loaded.getHead().getCurrentBranch());
        assertNotNull(loaded.getStaging());
        assertNotNull(loaded.getBranchManager());
        assertNotNull(loaded.getRemoteManager());
        assertFalse(loaded.getCommitStore().loadAll().isEmpty());
    }

    @Test
    void persist_savesChangesAcrossReload() {
        File root = tempDir.toFile();
        Repository repo = new Repository(root);
        repo.init();
        String initialCommitId = repo.getBranchManager().get("master");
        repo.getBranchManager().create("feature", initialCommitId);
        repo.getHead().setCurrentBranch("feature");
        repo.persist();
        Repository reloaded = Repository.load(root);
        assertEquals("feature", reloaded.getHead().getCurrentBranch());
        assertEquals(initialCommitId, reloaded.getBranchManager().get("feature"));
    }

    @Test
    void getGraph_reflectsNewCommitsSavedAfterFirstCall() {
        Repository repo = new Repository(tempDir.toFile());
        repo.init();
        CommitGraph graphBefore = repo.getGraph();
        String masterId = repo.getBranchManager().get("master");
        Commit parent = repo.getCommitStore().load(masterId);
        Commit second = CommitFactory.fromStaging(parent, repo.getStaging(), "second commit", new Date());
        repo.getCommitStore().save(second);
        CommitGraph graphAfter = repo.getGraph();
        assertTrue(graphAfter.contains(second.getId()), "CommitGraph should include commits saved after getGraph() was first called");
    }

    @Test
    void isInitialized_returnsFalseWhenRepositoryIsCorruptedOrIncomplete() throws Exception {
        File forgeDir = new File(tempDir.toFile(), ".branchforge");
        assertTrue(forgeDir.mkdirs());
        assertTrue(new File(forgeDir, "head").createNewFile());

        Repository repo = new Repository(tempDir.toFile());

        assertFalse(repo.isInitialized(), "Incomplete .branchforge directory should not be considered initialized");
    }

    @Test
    void persist_onUninitializedRepo_throwsBranchForgeExceptionInsteadOfNpe() {
        Repository repo = new Repository(tempDir.toFile());
        assertThrows(BranchForgeException.class, repo::persist);
    }

    @Test
    void getGraph_onUninitializedRepo_throwsBranchForgeExceptionInsteadOfNpe() {
        Repository repo = new Repository(tempDir.toFile());
        assertThrows(BranchForgeException.class, repo::getGraph);
    }

    @Test
    void constructor_throwsWhenRootDirIsNullOrNotADirectory() throws Exception {
        File regularFile = new File(tempDir.toFile(), "not-a-dir.txt");
        assertTrue(regularFile.createNewFile());
        assertThrows(BranchForgeException.class, () -> new Repository(null));
        assertThrows(BranchForgeException.class, () -> new Repository(regularFile));
    }
    @Test
    void getters_onUninitializedRepo_throwBranchForgeExceptionInsteadOfReturningNull() {
        Repository repo = new Repository(tempDir.toFile());
        assertThrows(BranchForgeException.class, repo::getStaging);
        assertThrows(BranchForgeException.class, repo::getBranchManager);
        assertThrows(BranchForgeException.class, repo::getHead);
        assertThrows(BranchForgeException.class, repo::getCommitStore);
        assertThrows(BranchForgeException.class, repo::getBlobStore);
    }

    @Test
    void init_throwsWhenForgeDirAlreadyExistsEvenIfIncomplete() {
        File forgeDir = new File(tempDir.toFile(), ".branchforge");
        assertTrue(forgeDir.mkdirs());
        Repository repo = new Repository(tempDir.toFile());
        assertThrows(BranchForgeException.class, repo::init);
    }

    @Test
    void load_onCorruptedStateFiles_throwsBranchForgeException() throws Exception {
        File forgeDir = new File(tempDir.toFile(), ".branchforge");
        File objectsDir = new File(forgeDir, "objects");
        assertTrue(objectsDir.mkdirs());
        assertTrue(new File(forgeDir, "head").createNewFile());
        assertTrue(new File(forgeDir, "branches").createNewFile());
        assertTrue(new File(forgeDir, "staging").createNewFile());
        assertTrue(new File(forgeDir, "remotes").createNewFile());
        assertThrows(BranchForgeException.class, () -> Repository.load(tempDir.toFile()));
    }
}