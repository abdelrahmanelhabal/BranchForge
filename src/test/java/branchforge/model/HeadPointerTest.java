package branchforge.model;

import branchforge.BranchForgeException;
import branchforge.util.Utils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

public class HeadPointerTest {
    private HeadPointer head;
    private BranchManager branchManager;

    @BeforeEach
    void setUp(){
        head = new HeadPointer("main");
        branchManager = new BranchManager();
    }
    @Test
    void testInitialBranch(){
        assertEquals("main", head.getCurrentBranch(), "The initial branch should be 'main'");
    }
    @Test
    void testSetCurrentBranch() {
        head.setCurrentBranch("feature-login");
        assertEquals("feature-login", head.getCurrentBranch(), "The current branch should be updated");
    }
    @Test
    void testGetCurrentCommitId() {
        branchManager.create("main", "commit-abc-123");
        String commitId = head.getCurrentCommitId(branchManager);
        assertEquals("commit-abc-123", commitId, "It should retrieve the correct commit ID from the BranchManager");
    }
    @Test
    void testGetCurrentCommitIdForMissingBranchThrowsException() {
        Exception exception = assertThrows(BranchForgeException.class, () -> {
            head.getCurrentCommitId(branchManager);
        });
        assertEquals("A branch with that name does not exist.", exception.getMessage());
    }
    @Test
    void testSerialization(@TempDir File tempDir) {
        head.setCurrentBranch("hotfix-1");
        File outFile = new File(tempDir, "HEAD.obj");
        Utils.writeObject(outFile, head);
        assertTrue(outFile.exists(), "Serialized file should be created");
        HeadPointer deserializedHead = Utils.readObject(outFile, HeadPointer.class);
        assertNotNull(deserializedHead, "Retrieved HeadPointer should not be null");
        assertEquals("hotfix-1", deserializedHead.getCurrentBranch(), "The branch name should be preserved after deserialization");
    }
}
