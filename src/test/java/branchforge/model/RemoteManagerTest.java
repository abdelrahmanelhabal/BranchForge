package branchforge.model;

import branchforge.BranchForgeException;
import branchforge.util.Utils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class RemoteManagerTest {
    private RemoteManager remoteManager;

    @BeforeEach
    void setUp() {
        remoteManager = new RemoteManager();
    }

    @Test
    void testInitialStateIsEmpty() {
        assertTrue(remoteManager.list().isEmpty());
    }
    @Test
    void testAddAndGetRemote() {
        remoteManager.add("origin", "https://github.com/user/repo.git");
        assertEquals("https://github.com/user/repo.git", remoteManager.get("origin"));
        assertEquals(1, remoteManager.list().size());
        assertTrue(remoteManager.list().contains("origin"));
    }

    @Test
    void testAddDuplicateRemoteThrowsException() {
        remoteManager.add("origin", "https://github.com/user/repo.git");
        Exception exception = assertThrows(BranchForgeException.class, () -> {
            remoteManager.add("origin", "https://github.com/user/different.git");
        });
        assertEquals("A remote with that name already exists.", exception.getMessage());
    }
    @Test
    void testRemoveRemoteSuccessfully() {
        remoteManager.add("origin", "https://github.com/user/repo.git");
        remoteManager.remove("origin");
        assertTrue(remoteManager.list().isEmpty());
    }
    @Test
    void testRemoveNonExistentRemoteThrowsException() {
        Exception exception = assertThrows(BranchForgeException.class, () -> {
            remoteManager.remove("missing-remote");
        });
        assertEquals("A remote with that name does not exist.", exception.getMessage());
    }
    @Test
    void testGetNonExistentRemoteThrowsException() {
        Exception exception = assertThrows(BranchForgeException.class, () -> {
            remoteManager.get("fake-remote");
        });
        assertEquals("A remote with that name does not exist.", exception.getMessage());
    }
    @Test
    void testListIsUnmodifiable() {
        remoteManager.add("origin", "https://github.com/user/repo.git");
        Set<String> remotes = remoteManager.list();
        assertThrows(UnsupportedOperationException.class, () -> {
            remotes.add("upstream");
        });
        assertThrows(UnsupportedOperationException.class, () -> {
            remotes.remove("origin");
        });
    }
    @Test
    void testSerialization(@TempDir File tempDir) {
        remoteManager.add("origin", "https://github.com/user/repo.git");
        remoteManager.add("upstream", "https://github.com/upstream/repo.git");

        File outFile = new File(tempDir, "remotes.obj");

        Utils.writeObject(outFile, remoteManager);
        assertTrue(outFile.exists());

        RemoteManager deserializedManager = Utils.readObject(outFile, RemoteManager.class);

        assertNotNull(deserializedManager);
        assertEquals(2, deserializedManager.list().size());
        assertEquals("https://github.com/user/repo.git", deserializedManager.get("origin"));
        assertEquals("https://github.com/upstream/repo.git", deserializedManager.get("upstream"));
    }
}
