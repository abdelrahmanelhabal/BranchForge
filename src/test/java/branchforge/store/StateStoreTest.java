package branchforge.store;
import branchforge.BranchForgeException;
import branchforge.model.HeadPointer;
import branchforge.model.StagingArea;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
public class StateStoreTest {
    @TempDir
    Path tempDir;
    private File branchforgeDir;
    private StateStore stateStore;
    @BeforeEach
    void setUp() {
        branchforgeDir = tempDir.resolve(".branchforge").toFile();
        stateStore = new StateStore(branchforgeDir);
    }
    @Test
    void constructorThrowsOnNullDirectory() {
        assertThrows(IllegalArgumentException.class, () -> new StateStore(null));
    }
    @Test
    void constructorThrowsIfBranchforgeDirIsAFile() throws Exception {
        File fakeDir = tempDir.resolve(".branchforge_file").toFile();
        fakeDir.createNewFile();
        assertThrows(IllegalArgumentException.class, () -> {
            new StateStore(fakeDir);
        }, "Should reject initialization if the target path is a standard file.");
    }

    @Test
    void saveAndExistsWorkingAsExpected() {
        assertFalse(stateStore.exists("head"));
        HeadPointer head = new HeadPointer("main");
        head.setCurrentBranch("main");
        stateStore.save("head", head);
        assertTrue(stateStore.exists("head"));
    }
    @Test
    void loadSuccessfullyRetrievesState() {
        HeadPointer head = new HeadPointer("main");
        head.setCurrentBranch("development");
        stateStore.save("head", head);
        HeadPointer loaded = stateStore.load("head", HeadPointer.class);
        assertNotNull(loaded);
        assertEquals("development", loaded.getCurrentBranch());
    }
    @Test
    void loadThrowsOnInvalidArgumentsOrMissingFile() {
        HeadPointer head = new HeadPointer("main");
        stateStore.save("head", head);

        // Null name
        assertThrows(IllegalArgumentException.class, () -> stateStore.load(null, HeadPointer.class));

        // Null type
        assertThrows(IllegalArgumentException.class, () -> stateStore.load("head", null));

        // File does not exist
        assertThrows(BranchForgeException.class, () -> stateStore.load("missing_file", HeadPointer.class));
    }
    @Test
    void existsThrowsOnInvalidName() {
        assertThrows(IllegalArgumentException.class, () -> stateStore.exists(null));
        assertThrows(IllegalArgumentException.class, () -> stateStore.exists(""));
    }

    @Test
    void loadWrongTypeThrowsClassCastException() {
        HeadPointer head = new HeadPointer("main");
        head.setCurrentBranch("main");
        stateStore.save("head", head);

        assertThrows(Exception.class, () -> {
            stateStore.load("head", StagingArea.class);
        });
    }
    @Test
    void validateNamePreventsDirectoryTraversal() {
        HeadPointer head = new HeadPointer("main");
        head.setCurrentBranch("main");
        assertThrows(IllegalArgumentException.class, () -> {
            stateStore.save("../head", head);
        });

        assertThrows(IllegalArgumentException.class, () -> {
            stateStore.save("subdir/head", head);
        });

        assertThrows(IllegalArgumentException.class, () -> {
            stateStore.exists("..\\windows_traversal");
        });
    }


    @Test
    void saveThrowsIfCannotCreateDirectory() throws Exception {
        File conflictingFile = tempDir.resolve("conflict_dir").toFile();
        conflictingFile.createNewFile();

        StateStore brokenStore = new StateStore(new File(tempDir.toFile(), "conflict_dir/nested"));
        HeadPointer head = new HeadPointer("main");

        assertThrows(BranchForgeException.class, () -> {
            brokenStore.save("head", head);
        }, "Should throw BranchForgeException when mkdirs() fails.");
    }

    @Test
    void mutatingObjectAfterSaveDoesNotAffectStoredState() {
        HeadPointer head = new HeadPointer("main");

        stateStore.save("head", head);

        head.setCurrentBranch("hacked-branch");

        HeadPointer loaded = stateStore.load("head", HeadPointer.class);

        assertEquals("main", loaded.getCurrentBranch(), "Stored state should not reflect in-memory mutations made after saving.");
        assertNotSame(head, loaded, "Loaded object should be a distinct instance from the saved object.");
    }

    @Test
    void mutatingLoadedObjectDoesNotAffectStoredState() {
        HeadPointer original = new HeadPointer("main");
        stateStore.save("head", original);

        HeadPointer firstLoad = stateStore.load("head", HeadPointer.class);
        firstLoad.setCurrentBranch("feature-branch");

        HeadPointer secondLoad = stateStore.load("head", HeadPointer.class);
        assertEquals("main", secondLoad.getCurrentBranch(), "Stored state should not be affected by mutations to previously loaded instances.");
        assertNotSame(firstLoad, secondLoad, "Each load should return a completely new instance.");
    }

    @Test
    void validateNameBlocksCurrentDirectoryAndHiddenPaths() {
        HeadPointer head = new HeadPointer("main");
        assertThrows(IllegalArgumentException.class, () -> {
            stateStore.save(".", head);
        }, "Saving a file named '.' should be blocked.");

        assertThrows(IllegalArgumentException.class, () -> {
            stateStore.save("..", head);
        });
    }

    @Test
    void saveThrowsWhenTargetFileAlreadyExistsAsADirectory() {
        branchforgeDir.mkdirs();
        File headDir = new File(branchforgeDir, "head");
        assertTrue(headDir.mkdirs(), "Setup: Failed to create 'head' directory");
        HeadPointer head = new HeadPointer("main");
        assertThrows(Exception.class, () -> {
            stateStore.save("head", head);
        }, "Should throw an exception when trying to overwrite a directory with a state file.");
    }

    @Test
    void saveThrowsWhenDirectoryIsReadOnly() {
        branchforgeDir.mkdirs();
        assertTrue(branchforgeDir.setWritable(false), "Setup: Failed to make directory read-only");
        HeadPointer head = new HeadPointer("main");
        try {
            assertThrows(Exception.class, () -> {
                stateStore.save("head", head);
            }, "Should throw an exception if the program lacks write permissions.");
        } finally {
            // Restore write permissions so JUnit can clean up the temp directory after the test
            branchforgeDir.setWritable(true);
        }
    }
}
