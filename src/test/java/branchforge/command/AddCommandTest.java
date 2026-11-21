package branchforge.command;

import branchforge.BranchForgeException;
import branchforge.model.Blob;
import branchforge.model.Commit;
import branchforge.model.CommitFactory;
import branchforge.repo.Repository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class AddCommandTest {

    @TempDir
    Path tempDir;

    private Repository repo;
    private AddCommand addCommand;

    @BeforeEach
    void setUp() {
        repo = new Repository(tempDir.toFile());
        repo.init();
        addCommand = new AddCommand(
                repo.getWorkingDir(),
                repo.getStaging(),
                repo.getBlobStore(),
                repo.getCommitStore(),
                repo.getBranchManager(),
                repo.getHead()
        );
    }


    @Test
    void execute_stagesNewFileAndSavesBlob() throws Exception {
        writeFile("hello.txt", "hello world");
        addCommand.execute(new String[]{"hello.txt"});
        String expectedBlobId = new Blob("hello world".getBytes(StandardCharsets.UTF_8)).getId();
        assertEquals(expectedBlobId, repo.getStaging().getStagedFiles().get("hello.txt"));
        assertTrue(repo.getBlobStore().exists(expectedBlobId));
    }

    @Test
    void execute_overwritesPreviouslyStagedVersionWhenFileChanges() throws Exception {
        writeFile("a.txt", "v1");
        addCommand.execute(new String[]{"a.txt"});

        writeFile("a.txt", "v2");
        addCommand.execute(new String[]{"a.txt"});

        String v2Id = new Blob("v2".getBytes(StandardCharsets.UTF_8)).getId();
        assertEquals(v2Id, repo.getStaging().getStagedFiles().get("a.txt"));
    }

    @Test
    void execute_unstagesFileWhenContentMatchesCurrentCommit() throws Exception {
        writeFile("a.txt", "v1");
        addCommand.execute(new String[]{"a.txt"});
        commitCurrentStaging("commit v1");

        writeFile("a.txt", "v2");
        addCommand.execute(new String[]{"a.txt"});
        assertTrue(repo.getStaging().getStagedFiles().containsKey("a.txt"));

        writeFile("a.txt", "v1");
        addCommand.execute(new String[]{"a.txt"});

        assertFalse(repo.getStaging().getStagedFiles().containsKey("a.txt"));
    }

    @Test
    void execute_unstagesRemovalWhenFileMatchesCurrentCommit() throws Exception {
        writeFile("a.txt", "v1");
        addCommand.execute(new String[]{"a.txt"});
        commitCurrentStaging("commit v1");

        repo.getStaging().stageRemove("a.txt");
        assertTrue(repo.getStaging().getRemovedFiles().contains("a.txt"));

        addCommand.execute(new String[]{"a.txt"});

        assertFalse(repo.getStaging().getRemovedFiles().contains("a.txt"));
        assertFalse(repo.getStaging().getStagedFiles().containsKey("a.txt"));
    }

    @Test
    void execute_throwsWhenFileDoesNotExist() {
        assertThrows(BranchForgeException.class, () -> addCommand.execute(new String[]{"missing.txt"}));
    }

    @Test
    void execute_throwsWhenOperandCountIsInvalid() {
        assertThrows(BranchForgeException.class, () -> addCommand.execute(new String[]{}));
        assertThrows(BranchForgeException.class, () -> addCommand.execute(new String[]{"a.txt", "b.txt"}));
    }

    @Test
    void execute_throwsBranchForgeExceptionWhenArgsIsNull() {
        assertThrows(BranchForgeException.class, () -> addCommand.execute(null));
    }

    @Test
    void execute_throwsBranchForgeExceptionWhenFileNameIsNullOrBlank() {
        assertThrows(BranchForgeException.class, () -> addCommand.execute(new String[]{null}));
        assertThrows(BranchForgeException.class, () -> addCommand.execute(new String[]{""}));
        assertThrows(BranchForgeException.class, () -> addCommand.execute(new String[]{"   "}));
    }

    @Test
    void execute_throwsBranchForgeExceptionWhenTargetIsADirectory() throws Exception {
        Files.createDirectory(tempDir.resolve("subdir"));
        assertThrows(BranchForgeException.class, () -> addCommand.execute(new String[]{"subdir"}));
    }

    @Test
    void execute_removesFileFromStagedRemovalsWhenModifiedAndReAdded() throws Exception {
        writeFile("a.txt", "v1");
        addCommand.execute(new String[]{"a.txt"});
        commitCurrentStaging("commit v1");

        repo.getStaging().stageRemove("a.txt");

        writeFile("a.txt", "v2");
        addCommand.execute(new String[]{"a.txt"});

        assertTrue(repo.getStaging().getStagedFiles().containsKey("a.txt"));
        assertFalse(repo.getStaging().getRemovedFiles().contains("a.txt"));
    }

    private void writeFile(String name, String content) throws Exception {
        Files.writeString(tempDir.resolve(name), content, StandardCharsets.UTF_8);
    }

    private void commitCurrentStaging(String message) {
        String headId = repo.getHead().getCurrentCommitId(repo.getBranchManager());
        Commit parent = repo.getCommitStore().load(headId);
        Commit next = CommitFactory.fromStaging(parent, repo.getStaging(), message, new Date());
        repo.getCommitStore().save(next);
        repo.getBranchManager().set(repo.getHead().getCurrentBranch(), next.getId());
        repo.getStaging().clear();
    }
}