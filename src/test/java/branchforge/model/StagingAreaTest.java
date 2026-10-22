package branchforge.model;

import branchforge.util.Utils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;
public class StagingAreaTest {
    private StagingArea stagingArea;
    @BeforeEach
    void setUp() {
        stagingArea = new StagingArea();
    }
    @Test
    void testInitialStateIsEmpty() {
        assertTrue(stagingArea.isEmpty());
        assertTrue(stagingArea.getStagedFiles().isEmpty());
        assertTrue(stagingArea.getRemovedFiles().isEmpty());
    }
    @Test
    void testStageAdd(){
        stagingArea.stageAdd("file1.txt", "blob-123");
        assertFalse(stagingArea.isEmpty());
        assertEquals(1, stagingArea.getStagedFiles().size());
        assertEquals("blob-123", stagingArea.getStagedFiles().get("file1.txt"));
        assertTrue(stagingArea.getRemovedFiles().isEmpty());
    }
    @Test
    void testStageRemove() {
        stagingArea.stageRemove("file2.txt");
        assertFalse(stagingArea.isEmpty());
        assertEquals(1, stagingArea.getRemovedFiles().size());
        assertTrue(stagingArea.getRemovedFiles().contains("file2.txt"));
        assertTrue(stagingArea.getStagedFiles().isEmpty());
    }
    @Test
    void testStageAddOverridesStageRemove() {
        stagingArea.stageRemove("file.txt");
        stagingArea.stageAdd("file.txt", "blob-abc");
        assertTrue(stagingArea.getStagedFiles().containsKey("file.txt"));
        assertFalse(stagingArea.getRemovedFiles().contains("file.txt"));
    }
    @Test
    void testUnstage() {
        stagingArea.stageAdd("file-add.txt", "blob-1");
        stagingArea.stageRemove("file-remove.txt");
        stagingArea.unstage("file-add.txt");
        stagingArea.unstage("file-remove.txt");
        assertTrue(stagingArea.isEmpty());
    }
    @Test
    void testClear() {
        stagingArea.stageAdd("file1.txt", "blob-1");
        stagingArea.stageRemove("file2.txt");

        stagingArea.clear();

        assertTrue(stagingArea.isEmpty());
    }
    @Test
    void testCollectionsAreUnmodifiable() {
        stagingArea.stageAdd("file1.txt", "blob-1");
        stagingArea.stageRemove("file2.txt");

        assertThrows(UnsupportedOperationException.class, () -> {
            stagingArea.getStagedFiles().put("hacked.txt", "blob-x");
        });

        assertThrows(UnsupportedOperationException.class, () -> {
            stagingArea.getRemovedFiles().add("hacked.txt");
        });
    }
    @Test
    void testSerialization(@TempDir File tempDir) {
        stagingArea.stageAdd("added.txt", "blob-added");
        stagingArea.stageRemove("removed.txt");
        File outFile = new File(tempDir, "staging.obj");
        Utils.writeObject(outFile, stagingArea);
        assertTrue(outFile.exists());
        StagingArea deserializedStaging = Utils.readObject(outFile, StagingArea.class);
        assertNotNull(deserializedStaging);
        assertFalse(deserializedStaging.isEmpty());
        assertEquals("blob-added", deserializedStaging.getStagedFiles().get("added.txt"));
        assertTrue(deserializedStaging.getRemovedFiles().contains("removed.txt"));
    }
}
