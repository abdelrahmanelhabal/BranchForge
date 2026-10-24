package branchforge.store;

import branchforge.BranchForgeException;
import branchforge.model.Blob;
import branchforge.model.Commit;
import branchforge.model.StagingArea;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Date;
import java.util.Set;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.*;
public class WorkingDirectoryTest {
    @TempDir
    Path tempDir;

    private File rootDir;
    private WorkingDirectory workingDir;
    private BlobStore blobStore;

    @BeforeEach
    void setUp() {
        rootDir = tempDir.resolve("workspace").toFile();
        rootDir.mkdirs();
        workingDir = new WorkingDirectory(rootDir);

        File branchforgeDir = new File(rootDir, ".branchforge");
        blobStore = new BlobStore(new File(branchforgeDir, "objects"));
    }

    private Commit createDummyCommit(String id, String message, TreeMap<String, String> files) {
        return new Commit(id, message, new Date(), Collections.emptyList(), files);
    }
    @Test
    void testWriteReadExistsAndDelete() {
        String file = "hello.txt";
        byte[] content = "world".getBytes(StandardCharsets.UTF_8);

        assertFalse(workingDir.exists(file));

        workingDir.write(file, content);
        assertTrue(workingDir.exists(file));
        assertArrayEquals(content, workingDir.read(file));

        workingDir.delete(file);
        assertFalse(workingDir.exists(file));
    }
    @Test
    void testReadThrowsOnNonExistentFile() {
        assertThrows(BranchForgeException.class, () -> workingDir.read("ghost.txt"));
    }
    @Test
    void testValidateFileNameBlocksInvalidPaths() {
        byte[] content = "data".getBytes();

        assertThrows(IllegalArgumentException.class, () -> workingDir.write(null, content));
        assertThrows(IllegalArgumentException.class, () -> workingDir.write("   ", content));
        assertThrows(IllegalArgumentException.class, () -> workingDir.write("subdir/file.txt", content));
        assertThrows(IllegalArgumentException.class, () -> workingDir.write("..", content));
        assertThrows(IllegalArgumentException.class, () -> workingDir.write(".", content));
    }
    @Test
    void testListFilesIgnoresDirectoriesAndSpecialNames() throws Exception {
        workingDir.write("a.txt", "a".getBytes(StandardCharsets.UTF_8));
        workingDir.write("b.txt", "b".getBytes(StandardCharsets.UTF_8));
        new File(rootDir, "subdir").mkdirs();
        new File(rootDir, ".branchforge").createNewFile();
        Set<String> files = workingDir.listFiles();
        assertEquals(2, files.size());
        assertTrue(files.contains("a.txt"));
        assertTrue(files.contains("b.txt"));
        assertFalse(files.contains(".branchforge"));
    }


    @Test
    void testCheckUntrackedOverwriteThrowsWhenUntrackedFileInTarget() {
        workingDir.write("conflict.txt", "untracked content".getBytes(StandardCharsets.UTF_8));

        Commit current = createDummyCommit("1111111111111111111111111111111111111111", "current", new TreeMap<>());

        TreeMap<String, String> targetFiles = new TreeMap<>();
        targetFiles.put("conflict.txt", "blob_id_1");
        Commit target = createDummyCommit("2222222222222222222222222222222222222222", "target", targetFiles);

        StagingArea staging = new StagingArea();

        assertThrows(BranchForgeException.class, () -> {
            workingDir.checkUntrackedOverwrite(target, current, staging);
        });
    }
    @Test
    void testCheckUntrackedOverwriteAllowsTrackedOrStagedFiles() {
        workingDir.write("staged.txt", "staged content".getBytes(StandardCharsets.UTF_8));
        workingDir.write("tracked.txt", "tracked content".getBytes(StandardCharsets.UTF_8));

        TreeMap<String, String> currentFiles = new TreeMap<>();
        currentFiles.put("tracked.txt", "blob_old");
        Commit current = createDummyCommit("1111111111111111111111111111111111111111", "current", currentFiles);

        TreeMap<String, String> targetFiles = new TreeMap<>();
        targetFiles.put("staged.txt", "blob_new_1");
        targetFiles.put("tracked.txt", "blob_new_2");
        Commit target = createDummyCommit("2222222222222222222222222222222222222222", "target", targetFiles);

        StagingArea staging = new StagingArea();
        staging.stageAdd("staged.txt", "blob_staged");

        assertDoesNotThrow(() -> {
            workingDir.checkUntrackedOverwrite(target, current, staging);
        });
    }
    @Test
    void testRestoreFromCommit() {
        byte[] content = "restored data".getBytes(StandardCharsets.UTF_8);
        Blob blob = new Blob(content);
        blobStore.save(blob);

        TreeMap<String, String> targetFiles = new TreeMap<>();
        targetFiles.put("restored.txt", blob.getId());
        Commit target = createDummyCommit("1111111111111111111111111111111111111111", "target", targetFiles);

        assertFalse(workingDir.exists("restored.txt"));

        workingDir.restoreFromCommit(target, blobStore);

        assertTrue(workingDir.exists("restored.txt"));
        assertArrayEquals(content, workingDir.read("restored.txt"));
    }

    @Test
    void testRemoveFilesNotInDeletesCorrectly() {
        workingDir.write("keep.txt", "keep me".getBytes(StandardCharsets.UTF_8));
        workingDir.write("remove.txt", "remove me".getBytes(StandardCharsets.UTF_8));
        workingDir.write("untracked.txt", "ignore me".getBytes(StandardCharsets.UTF_8));

        TreeMap<String, String> currentFiles = new TreeMap<>();
        currentFiles.put("keep.txt", "blob_1");
        currentFiles.put("remove.txt", "blob_2");
        Commit current = createDummyCommit("1111111111111111111111111111111111111111", "current", currentFiles);
        TreeMap<String, String> targetFiles = new TreeMap<>();
        targetFiles.put("keep.txt", "blob_1");
        Commit target = createDummyCommit("2222222222222222222222222222222222222222", "target", targetFiles);

        workingDir.removeFilesNotIn(target, current);

        assertTrue(workingDir.exists("keep.txt"), "Should keep files present in the target commit.");
        assertFalse(workingDir.exists("remove.txt"), "Should remove files tracked in current but missing from target.");
        assertTrue(workingDir.exists("untracked.txt"), "Should not delete untracked files.");
    }
}
