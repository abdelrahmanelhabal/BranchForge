package branchforge.command;

import branchforge.BranchForgeException;
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

class RmCommandTest {

    @TempDir
    Path tempDir;

    private Repository repo;
    private AddCommand addCommand;
    private RmCommand rmCommand;

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
        rmCommand = new RmCommand(
                repo.getWorkingDir(),
                repo.getStaging(),
                repo.getCommitStore(),
                repo.getBranchManager(),
                repo.getHead()
        );
    }

    @Test
    void execute_unstagesUntrackedStagedFileWithoutDeletingFromWorkingDir() throws Exception {
        writeFile("new.txt", "hello");
        addCommand.execute(new String[]{"new.txt"});

        rmCommand.execute(new String[]{"new.txt"});

        assertFalse(repo.getStaging().getStagedFiles().containsKey("new.txt"));
        assertFalse(repo.getStaging().getRemovedFiles().contains("new.txt"));
        assertTrue(Files.exists(tempDir.resolve("new.txt")));
    }

    @Test
    void execute_stagesTrackedFileForRemovalAndDeletesFromWorkingDir() throws Exception {
        writeFile("a.txt", "v1");
        addCommand.execute(new String[]{"a.txt"});
        commitCurrentStaging("commit v1");

        rmCommand.execute(new String[]{"a.txt"});

        assertTrue(repo.getStaging().getRemovedFiles().contains("a.txt"));
        assertFalse(Files.exists(tempDir.resolve("a.txt")));
    }

    @Test
    void execute_unstagesAndStagesForRemovalWhenFileIsBothTrackedAndStaged() throws Exception {
        writeFile("a.txt", "v1");
        addCommand.execute(new String[]{"a.txt"});
        commitCurrentStaging("commit v1");

        writeFile("a.txt", "v2");
        addCommand.execute(new String[]{"a.txt"});
        assertTrue(repo.getStaging().getStagedFiles().containsKey("a.txt"));

        rmCommand.execute(new String[]{"a.txt"});

        assertFalse(repo.getStaging().getStagedFiles().containsKey("a.txt"));
        assertTrue(repo.getStaging().getRemovedFiles().contains("a.txt"));
        assertFalse(Files.exists(tempDir.resolve("a.txt")));
    }

    @Test
    void execute_stagesTrackedFileForRemovalEvenIfAlreadyDeletedFromWorkingDir() throws Exception {
        writeFile("a.txt", "v1");
        addCommand.execute(new String[]{"a.txt"});
        commitCurrentStaging("commit v1");

        Files.delete(tempDir.resolve("a.txt"));

        rmCommand.execute(new String[]{"a.txt"});

        assertTrue(repo.getStaging().getRemovedFiles().contains("a.txt"));
    }

    @Test
    void execute_throwsWhenFileIsNeitherTrackedNorStaged() throws Exception {
        writeFile("untracked.txt", "data");

        BranchForgeException ex = assertThrows(
                BranchForgeException.class,
                () -> rmCommand.execute(new String[]{"untracked.txt"})
        );
        assertEquals("No reason to remove the file.", ex.getMessage());
    }

    @Test
    void execute_throwsWhenOperandCountIsInvalid() {
        assertThrows(BranchForgeException.class, () -> rmCommand.execute(new String[]{}));
        assertThrows(BranchForgeException.class, () -> rmCommand.execute(new String[]{"a.txt", "b.txt"}));
    }
    @Test
    void execute_throwsBranchForgeExceptionWhenArgsIsNull() {
        assertThrows(BranchForgeException.class, () -> rmCommand.execute(null));
    }

    @Test
    void execute_throwsBranchForgeExceptionWhenFileNameIsNullOrBlank() {
        assertThrows(BranchForgeException.class, () -> rmCommand.execute(new String[]{null}));
        assertThrows(BranchForgeException.class, () -> rmCommand.execute(new String[]{""}));
        assertThrows(BranchForgeException.class, () -> rmCommand.execute(new String[]{"   "}));
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