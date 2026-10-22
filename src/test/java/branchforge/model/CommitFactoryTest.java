package branchforge.model;

import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class CommitFactoryTest {
    @Test
    void testInitialCommit(){
        Commit commit = CommitFactory.initialCommit();
        assertEquals("initial commit", commit.getMessage());
        assertEquals(0L, commit.getTimeStamp().getTime(), "Initial commit should use epoch time 0");
        assertTrue(commit.getParentIds().isEmpty(), "Initial commit should have no parents");
        assertTrue(commit.getFileToBlob().isEmpty(), "Initial commit should have no files");
        assertNotNull(commit.getId(), "Commit ID should be generated");
    }
    @Test
    void testFormStagingAddsAndRemovesFilesCorrectly() {
        Map<String, String> parentFiles = new HashMap<>();
        parentFiles.put("keep.txt", "blob-1");
        parentFiles.put("remove.txt", "blob-2");
        Commit parent = new Commit("parent-id", "first commit", new Date(), new ArrayList<>(), parentFiles);

        StagingArea staging = new StagingArea();
        staging.stageRemove("remove.txt");
        staging.stageAdd("new.txt", "blob-3");

        Date now = new Date();
        Commit newCommit = CommitFactory.formStaging(parent, staging, "second commit", now);

        assertEquals("second commit", newCommit.getMessage());
        assertEquals(now, newCommit.getTimeStamp());

        assertEquals(1, newCommit.getParentIds().size());
        assertEquals("parent-id", newCommit.getParentIds().get(0));

        Set<String> finalFiles = newCommit.getFiles();
        assertEquals(2, finalFiles.size());
        assertTrue(finalFiles.contains("keep.txt"));
        assertTrue(finalFiles.contains("new.txt"));
        assertFalse(finalFiles.contains("remove.txt"));

        assertEquals("blob-1", newCommit.getBlobId("keep.txt"));
        assertEquals("blob-3", newCommit.getBlobId("new.txt"));
    }
    @Test
    void testFromMerge() {
        Map<String, String> snapshot = Map.of("merged.txt", "blob-m");
        Date now = new Date();
        Commit mergeCommit = CommitFactory.fromMerge("branch1-id", "branch2-id", snapshot, "Merge branches", now);
        assertEquals("Merge branches", mergeCommit.getMessage());
        assertEquals(now, mergeCommit.getTimeStamp());

        assertEquals(2, mergeCommit.getParentIds().size());
        assertEquals("branch1-id", mergeCommit.getParentIds().get(0));
        assertEquals("branch2-id", mergeCommit.getParentIds().get(1));

        assertEquals(1, mergeCommit.getFiles().size());
        assertEquals("blob-m", mergeCommit.getBlobId("merged.txt"));
    }
    @Test
    void testEncodeCanonicalIsDeterministic() {
        Map<String, String> files1 = new LinkedHashMap<>();
        files1.put("z.txt", "blob-z");
        files1.put("a.txt", "blob-a");
        Map<String, String> files2 = new LinkedHashMap<>();
        files2.put("a.txt", "blob-a");
        files2.put("z.txt", "blob-z");
        Date ts = new Date(10000L);
        List<String> parents = List.of("parent1");
        byte[] bytes1 = CommitFactory.encodeCanonical("msg", parents, files1, ts);
        byte[] bytes2 = CommitFactory.encodeCanonical("msg", parents, files2, ts);
        assertArrayEquals(bytes1, bytes2, "The byte arrays must be identical regardless of Map insertion order");
    }
}
