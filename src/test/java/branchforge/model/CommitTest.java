package branchforge.model;

import branchforge.util.Utils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
public class CommitTest {
    @Test
    void testBasicInitializationAndAllGetters() {
        String id = "commit-hash-123";
        String message = "Add user login";
        Date date = new Date(1600000000000L);
        List<String> parents = List.of("parent-hash-456");
        Map<String, String> files = Map.of("Login.java", "blob-abc", "User.java", "blob-def");

        Commit commit = new Commit(id, message, date, parents, files);

        assertEquals(id, commit.getId());
        assertEquals(message, commit.getMessage());
        assertEquals(date, commit.getTimeStamp());

        assertEquals(parents, commit.getParentIds());
        assertEquals(files, commit.getFileToBlob());

        Set<String> expectedFiles = Set.of("Login.java", "User.java");
        assertEquals(expectedFiles, commit.getFiles());

        assertEquals("blob-abc", commit.getBlobId("Login.java"));
        assertEquals("blob-def", commit.getBlobId("User.java"));
        assertNull(commit.getBlobId("Missing.java"));
    }
    @Test
    void testCannotHaveMoreThanTwoParents() {
        List<String> tooManyParents = List.of("parent-1", "parent-2", "parent-3");
        Map<String, String> files = new HashMap<>();
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            new Commit("id-123", "Too many parents", new Date(), tooManyParents, files);
        });
        assertEquals("A commit cannot have more than 2 parents.", exception.getMessage());
    }

    @Test
    void testConstructorDefensiveCopying() {
        Date mutableDate = new Date(1000000L);
        List<String> mutableParents = new ArrayList<>();
        mutableParents.add("parent-1");
        Map<String, String> mutableFiles = new HashMap<>();
        mutableFiles.put("test.txt", "blob-1");

        Commit commit = new Commit("id", "msg", mutableDate, mutableParents, mutableFiles);

        // Mutate the original objects passed into the constructor
        mutableDate.setTime(9999999L);
        mutableParents.add("parent-2");
        mutableFiles.put("hacked.txt", "blob-evil");

        // Verify the commit's internal state remained completely unchanged
        assertNotEquals(mutableDate.getTime(), commit.getTimeStamp().getTime(), "Date should be defensively copied");
        assertEquals(1, commit.getParentIds().size(), "Parents list should be defensively copied");
        assertEquals(1, commit.getFileToBlob().size(), "Files map should be defensively copied");
        assertNull(commit.getBlobId("hacked.txt"), "Internal map should not contain the hacked file");
    }

    @Test
    void testGetterDefensiveCopying() {
        Commit commit = new Commit("id", "msg", new Date(), List.of("p1"), Map.of("f1.txt", "b1"));

        // 1. Test Date getter cloning
        Date retrievedDate = commit.getTimeStamp();
        retrievedDate.setTime(0L);
        assertNotEquals(0L, commit.getTimeStamp().getTime(), "Modifying returned Date should not affect the Commit");

        // 2. Test unmodifiable lists and maps
        assertThrows(UnsupportedOperationException.class, () -> {
            commit.getParentIds().add("new-parent");
        }, "Should not be able to modify the returned parents list");

        assertThrows(UnsupportedOperationException.class, () -> {
            commit.getFileToBlob().put("new.txt", "blob-2");
        }, "Should not be able to modify the returned files map");

        assertThrows(UnsupportedOperationException.class, () -> {
            commit.getFiles().add("new.txt");
        }, "Should not be able to modify the returned files set");
    }

    @Test
    void testSerialization(@TempDir File tempDir) {
        Date date = new Date();
        List<String> parents = List.of("parent-abc");
        Map<String, String> files = Map.of("file1.txt", "blob-xyz");
        Commit originalCommit = new Commit("commit-hash", "Fix bug", date, parents, files);
        File outFile = new File(tempDir, "commit.obj");
        Utils.writeObject(outFile, originalCommit);
        assertTrue(outFile.exists());
        Commit deserializedCommit = Utils.readObject(outFile, Commit.class);
        assertNotNull(deserializedCommit);
        assertEquals(originalCommit.getId(), deserializedCommit.getId());
        assertEquals(originalCommit.getMessage(), deserializedCommit.getMessage());
        assertEquals(originalCommit.getTimeStamp(), deserializedCommit.getTimeStamp());
        assertEquals(originalCommit.getParentIds(), deserializedCommit.getParentIds());
        assertEquals(originalCommit.getFileToBlob(), deserializedCommit.getFileToBlob());
    }
}
