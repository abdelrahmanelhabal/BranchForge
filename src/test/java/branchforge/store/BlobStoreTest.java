package branchforge.store;

import branchforge.BranchForgeException;
import branchforge.model.Blob;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class BlobStoreTest {
    @TempDir
    Path tempDir;

    private File objectsDir;
    private BlobStore blobStore;

    @BeforeEach
    void setUp() {
        objectsDir = tempDir.resolve("objects").toFile();
        blobStore = new BlobStore(objectsDir);
    }
    @Test
    void constructorThrowsOnNullDirectory() {
        assertThrows(IllegalArgumentException.class, () -> new BlobStore(null));
    }
    @Test
    void constructorAppendsBlobsToDirectoryIfNotAlreadyNamedBlobs() {
        Blob blob = new Blob("test data".getBytes(StandardCharsets.UTF_8));
        blobStore.save(blob);

        File expectedBlobsDir = new File(objectsDir, "blobs");
        assertTrue(expectedBlobsDir.exists() && expectedBlobsDir.isDirectory(),
                "Constructor should have appended 'blobs' to the directory path.");

        File savedBlobFile = new File(expectedBlobsDir, blob.getId());
        assertTrue(savedBlobFile.exists(), "Blob should be saved inside the 'blobs' subdirectory.");
    }
    @Test
    void constructorDoesNotAppendBlobsIfAlreadyNamedBlobs() {
        File explicitlyNamedBlobsDir = tempDir.resolve("blobs").toFile();
        BlobStore specificStore = new BlobStore(explicitlyNamedBlobsDir);

        Blob blob = new Blob("test data".getBytes(StandardCharsets.UTF_8));
        specificStore.save(blob);
        File nestedBlobsDir = new File(explicitlyNamedBlobsDir, "blobs");
        assertFalse(nestedBlobsDir.exists(), "Constructor should not append 'blobs' if the directory is already named 'blobs'.");

        File savedBlobFile = new File(explicitlyNamedBlobsDir, blob.getId());
        assertTrue(savedBlobFile.exists(), "Blob should be saved directly in the 'blobs' directory.");
    }
    @Test
    void saveAndExistsWorkingAsExpected() {
        Blob blob = new Blob("hello branchforge".getBytes(StandardCharsets.UTF_8));
        String id = blob.getId();

        assertFalse(blobStore.exists(id));

        blobStore.save(blob);
        assertTrue(blobStore.exists(id));
    }
    @Test
    void loadSuccessfullyRetrievesBlob() {
        byte[] content = "unique content for blob".getBytes(StandardCharsets.UTF_8);
        Blob originalBlob = new Blob(content);
        blobStore.save(originalBlob);

        Blob loadedBlob = blobStore.load(originalBlob.getId());

        assertNotNull(loadedBlob);
        assertEquals(originalBlob.getId(), loadedBlob.getId());
        assertArrayEquals(content, loadedBlob.getContent());
    }

    @Test
    void saveThrowsOnNullBlob() {
        assertThrows(IllegalArgumentException.class, () -> blobStore.save(null));
    }

    @Test
    void saveDoesNotOverwriteExistingBlob() {
        Blob blob1 = new Blob("content".getBytes(StandardCharsets.UTF_8));
        blobStore.save(blob1);

        File savedFile = new File(new File(objectsDir, "blobs"), blob1.getId());
        long lastModified = savedFile.lastModified();

        blobStore.save(blob1);

        assertEquals(lastModified, savedFile.lastModified(), "Save should return early if the blob already exists.");
    }

    @Test
    void loadThrowsOnNonExistentBlob() {
        assertThrows(BranchForgeException.class, () -> blobStore.load("nonexistent_id"));
    }

    @Test
    void existsAndLoadThrowOnInvalidId() {
        // Null ID
        assertThrows(IllegalArgumentException.class, () -> blobStore.exists(null));
        assertThrows(IllegalArgumentException.class, () -> blobStore.load(null));

        // Blank ID
        assertThrows(IllegalArgumentException.class, () -> blobStore.exists("   "));
        assertThrows(IllegalArgumentException.class, () -> blobStore.load(""));
    }

    // ============================================================
    // Advanced Edge Cases & Flaw Tests
    // ============================================================

    @Test
    void validateIdBlocksPathTraversal() {
//        Blob blob = new Blob("test content".getBytes(StandardCharsets.UTF_8));
        assertThrows(IllegalArgumentException.class, () -> {
            blobStore.exists(".");
        }, "An ID of '.' should be blocked.");

        assertThrows(IllegalArgumentException.class, () -> {
            blobStore.load("..");
        }, "An ID of '..' should be blocked.");

        assertThrows(IllegalArgumentException.class, () -> {
            blobStore.exists("../sneaky_file");
        }, "Path traversal sequences should be blocked.");

        assertThrows(IllegalArgumentException.class, () -> {
            blobStore.load("/absolute/path/file");
        }, "Absolute paths should be blocked.");
    }

    @Test
    void saveThrowsWhenTargetFileAlreadyExistsAsADirectory() {
        Blob blob = new Blob("test content".getBytes(StandardCharsets.UTF_8));
        String id = blob.getId();
        File blobsDir = new File(objectsDir, "blobs");
        blobsDir.mkdirs();
        File maliciousDir = new File(blobsDir, id);
        assertTrue(maliciousDir.mkdirs(), "Setup: Failed to create a directory matching the blob ID");
        assertThrows(Exception.class, () -> {
            blobStore.save(blob);
        }, "Should throw an exception when trying to overwrite a directory with a blob file.");
    }

    @Test
    void saveThrowsWhenDirectoryIsReadOnly() {
        Blob blob = new Blob("test content".getBytes(StandardCharsets.UTF_8));
        File blobsDir = new File(objectsDir, "blobs");
        blobsDir.mkdirs();
        assertTrue(blobsDir.setWritable(false), "Setup: Failed to make directory read-only");
        try {
            assertThrows(Exception.class, () -> {
                blobStore.save(blob);
            }, "Should throw an exception if the program lacks write permissions.");
        } finally {
            blobsDir.setWritable(true);
        }
    }
}
