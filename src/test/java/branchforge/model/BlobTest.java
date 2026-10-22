package branchforge.model;
import branchforge.util.Utils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.*;
import static org.junit.jupiter.api.Assertions.*;

public class BlobTest {
    @Test
    void testBlobInitializationAndGetters(){
        byte[] content = "test it".getBytes();
        Blob blob = new Blob(content);
        assertArrayEquals(content, blob.getContent());
        assertNotNull(blob.getId());
        assertFalse(blob.getId().isEmpty());
    }
    @Test
    void testIdGenerationIsConsistent(){
        byte[] content= "test data".getBytes();
        Blob blob1= new Blob(content);
        Blob blob2= new Blob(content);
        assertEquals(blob1.getId(), blob2.getId());
        assertEquals(blob1.getId(), blob2.getId());
        assertEquals(Utils.sha1("blob", content), blob1.getId());
    }
    @Test
    void testSerialization(@TempDir File tempDir) {
        byte[] content = "test it".getBytes();
        Blob originalBlob = new Blob(content);
        File outFile = new File(tempDir, "blob.obj");
        Utils.writeObject(outFile, originalBlob);
        assertTrue(outFile.exists(), "Serialized file should be created");
        Blob deserializedBlob = Utils.readObject(outFile, Blob.class);
        assertNotNull(deserializedBlob, "Retrieved blob should not be null");
        assertEquals(originalBlob.getId(), deserializedBlob.getId(), "Blob IDs should match");
        assertArrayEquals(originalBlob.getContent(), deserializedBlob.getContent(), "Blob contents should match");
    }
    @Test
    void testConstructorEncapsulationFlaw() {
        byte[] content = "mutable".getBytes();
        Blob blob = new Blob(content);
        content[0] = 'X';
        assertNotEquals('X', blob.getContent()[0]);
    }
}
