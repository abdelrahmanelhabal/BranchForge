package branchforge.util;

import java.io.Serializable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.File;
import java.io.Serializable;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class UtilsTest {
    static class DummyCommit implements Serializable{
        String message;
        int id;
        DummyCommit(String message,int id){
            this.message = message;
            this.id = id;
        }
    }
    @Test
    void testSha1GeneratesCorrectAndConsistentHash(){
        String data1 = "hello";
        String data2 = "World";
        String hash1 = Utils.sha1(data1, data2);
        String hash2 = Utils.sha1(data1, data2);
        String diffHash = Utils.sha1("Different Data");
        assertEquals(40, hash1.length(), "SHA-1 hash must be exactly 40 characters long");
        assertEquals(hash1, hash2, "Identical inputs must produce identical hashes");
        assertNotEquals(hash1, diffHash, "Different inputs must produce different hashes");
    }
    @Test
    void testRawContentsRoundTrip(@TempDir File tempDir){
        File outFile = new File(tempDir,"raw_data.txt");
        byte[] originalData = "BranchForge Raw Data Test".getBytes();
        Utils.writeContents(outFile, originalData);
        assertTrue(outFile.exists(), "File should be created on the disk");
        byte[] retrievedData = Utils.readContents(outFile);
        assertArrayEquals(originalData, retrievedData, "Read data should match written data exactly");
    }
    @Test
    void testSerializationRoundTrip(@TempDir File tempDir){
        File outFile = new File(tempDir, "dummy_commit.obj");
        DummyCommit originalObj = new DummyCommit("Initial Commit", 100);
        Utils.writeObject(outFile , originalObj);
        assertTrue(outFile.exists(), "Serialized file should be created");
        DummyCommit retrievedObj = Utils.readObject(outFile, DummyCommit.class);
        assertNotNull(retrievedObj, "Retrieved object should not be null");
        assertEquals(originalObj.message, retrievedObj.message, "Object messages should match");
        assertEquals(originalObj.id, retrievedObj.id, "Object IDs should match");
    }
}
