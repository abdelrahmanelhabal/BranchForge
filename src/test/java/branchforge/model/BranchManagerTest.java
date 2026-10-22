package branchforge.model;

import branchforge.BranchForgeException;
import branchforge.util.Utils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class BranchManagerTest {
    private BranchManager branchManager;
    @BeforeEach
    void setUp(){
        branchManager = new BranchManager();
    }
    @Test
    void testCreateAndGetBranch() {
        branchManager.create("main", "commit-123");
        assertTrue(branchManager.exists("main"));
        assertEquals("commit-123", branchManager.get("main"));
    }
    @Test
    void testCreateDuplicateBranchThrowsException(){
        branchManager.create("feature-x", "commit-123");
        Exception exception = assertThrows(BranchForgeException.class, () -> {
            branchManager.create("feature-x", "commit-456");
        });
        assertEquals("A branch with that name already exists.", exception.getMessage());
    }
    @Test
    void testDeleteBranchSuccessfully(){
        branchManager.create("temp-branch", "commit-123");
        assertTrue(branchManager.exists("temp-branch"));
        branchManager.delete("temp-branch");
        assertFalse(branchManager.exists("temp-branch"));
    }
    @Test
    void testDeleteNonExistentBranchThrowsException(){
        Exception exception =assertThrows(BranchForgeException.class , ()->{
            branchManager.delete("missing-branch");
        });
        assertEquals("A branch with that name does not exist.", exception.getMessage());
    }
    @Test
    void testListReturnsAllBranches() {
        branchManager.create("main", "commit-1");
        branchManager.create("dev", "commit-2");
        branchManager.create("feature", "commit-3");
        Set<String> branches = branchManager.list();
        assertEquals(3, branches.size());
        assertTrue(branches.contains("main"));
        assertTrue(branches.contains("dev"));
        assertTrue(branches.contains("feature"));
    }
    @Test
    void testListIsUnmodifiable() {
        branchManager.create("main", "commit-1");
        Set<String> branches = branchManager.list();
        assertThrows(UnsupportedOperationException.class, () -> {
            branches.add("hacked-branch");
        });
        assertThrows(UnsupportedOperationException.class, () -> {
            branches.remove("main");
        });
    }
    @Test
    void testSerialization(@TempDir File tempDir) {
        branchManager.create("main", "commit-abc");
        branchManager.create("patch-1", "commit-xyz");

        File outFile = new File(tempDir, "branches.obj");
        Utils.writeObject(outFile, branchManager);
        assertTrue(outFile.exists());

        BranchManager deserializedManager = Utils.readObject(outFile, BranchManager.class);

        assertNotNull(deserializedManager);
        assertEquals(2, deserializedManager.list().size());
        assertTrue(deserializedManager.exists("main"));
        assertEquals("commit-xyz", deserializedManager.get("patch-1"));
    }
}
