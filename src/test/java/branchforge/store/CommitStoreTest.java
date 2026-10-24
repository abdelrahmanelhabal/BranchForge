package branchforge.store;

import branchforge.BranchForgeException;
import branchforge.model.Commit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.*;

class CommitStoreTest {

    @TempDir
    Path tempDir;

    private File objectsDir;
    private CommitStore commitStore;

    private static final String ID_1 = "1111111111111111111111111111111111111111";
    private static final String ID_2 = "2222222222222222222222222222222222222222";
    private static final String ID_AMBIGUOUS_A = "abcde11111111111111111111111111111111111";
    private static final String ID_AMBIGUOUS_B = "abcde22222222222222222222222222222222222";

    @BeforeEach
    void setUp() {
        objectsDir = tempDir.resolve("objects").toFile();
        commitStore = new CommitStore(objectsDir);
    }

    private Commit createDummyCommit(String id, String message) {
        return new Commit(id, message, new Date(), Collections.emptyList(), new TreeMap<>());
    }


    @Test
    void constructorAppendsCommitsToDirectory() {
        Commit c = createDummyCommit(ID_1, "Init");
        commitStore.save(c);

        File expectedCommitsDir = new File(objectsDir, "commits");
        assertTrue(expectedCommitsDir.exists() && expectedCommitsDir.isDirectory());
        assertTrue(new File(expectedCommitsDir, ID_1).exists());
    }

    @Test
    void saveAndExistsExactMatch() {
        assertFalse(commitStore.exists(ID_1));

        commitStore.save(createDummyCommit(ID_1, "Init"));
        assertTrue(commitStore.exists(ID_1));
    }

    @Test
    void loadSuccessfullyRetrievesCommit() {
        Commit original = createDummyCommit(ID_1, "Initial Commit");
        commitStore.save(original);

        Commit loaded = commitStore.load(ID_1);
        assertEquals(ID_1, loaded.getId());
        assertEquals("Initial Commit", loaded.getMessage());
    }

    @Test
    void existsAndLoadSupportAbbreviatedIds() {
        commitStore.save(createDummyCommit(ID_1, "Init"));

        String shortId = "11111111"; // 8 characters
        assertTrue(commitStore.exists(shortId), "Should return true for a unique abbreviated ID.");

        Commit loaded = commitStore.load(shortId);
        assertEquals(ID_1, loaded.getId());
    }

    @Test
    void existsAndLoadHandleAmbiguousAbbreviatedIds() {
        commitStore.save(createDummyCommit(ID_AMBIGUOUS_A, "A"));
        commitStore.save(createDummyCommit(ID_AMBIGUOUS_B, "B"));

        String ambiguousPrefix = "abcde";
        assertFalse(commitStore.exists(ambiguousPrefix), "exists() should return false if prefix matches multiple commits.");

        assertThrows(BranchForgeException.class, () -> {
            commitStore.load(ambiguousPrefix);
        }, "load() should throw BranchForgeException for ambiguous prefix.");
    }

    @Test
    void existsAndLoadHandleIdsTooLong() {
        commitStore.save(createDummyCommit(ID_1, "Init"));

        String tooLongId = ID_1 + "A"; // 41 characters
        assertFalse(commitStore.exists(tooLongId));
        assertThrows(BranchForgeException.class, () -> commitStore.load(tooLongId));
    }


    @Test
    void listAllAndLoadAllReturnEverythingSorted() {
        commitStore.save(createDummyCommit(ID_2, "Second"));
        commitStore.save(createDummyCommit(ID_1, "First"));

        List<String> ids = commitStore.listAll();
        assertEquals(2, ids.size());
        assertEquals(ID_1, ids.get(0), "IDs should be sorted alphabetically.");
        assertEquals(ID_2, ids.get(1));

        List<Commit> commits = commitStore.loadAll();
        assertEquals(2, commits.size());
        assertEquals("First", commits.get(0).getMessage());
        assertEquals("Second", commits.get(1).getMessage());
    }

    @Test
    void validateIdBlocksPathTraversal() {
        assertThrows(IllegalArgumentException.class, () -> commitStore.exists("."));
        assertThrows(IllegalArgumentException.class, () -> commitStore.load("../sneaky"));
    }

    @Test
    void saveThrowsWhenTargetFileAlreadyExistsAsADirectory() {
        File commitsDir = new File(objectsDir, "commits");
        commitsDir.mkdirs();
        assertTrue(new File(commitsDir, ID_1).mkdirs(), "Setup: Failed to create dummy directory");

        assertThrows(Exception.class, () -> {
            commitStore.save(createDummyCommit(ID_1, "Conflict"));
        });
    }
}