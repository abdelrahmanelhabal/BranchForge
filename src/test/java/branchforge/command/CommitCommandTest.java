package branchforge.command;

import branchforge.BranchForgeException;
import branchforge.model.Commit;
import branchforge.repo.Repository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class CommitCommandTest {

    @TempDir
    Path tempDir;

    private Repository repo;
    private AddCommand addCommand;
    private RmCommand rmCommand;
    private CommitCommand commitCommand;

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
        commitCommand = new CommitCommand(
                repo.getStaging(),
                repo.getCommitStore(),
                repo.getBranchManager(),
                repo.getHead()
        );
    }


    @Test
    void execute_createsCommitSavesItAdvancesBranchAndClearsStaging() throws Exception {
        String initialHeadId = repo.getHead().getCurrentCommitId(repo.getBranchManager());
        writeFile("a.txt", "hello");
        addCommand.execute(new String[]{"a.txt"});

        commitCommand.execute(new String[]{"first commit"});

        String newHeadId = repo.getHead().getCurrentCommitId(repo.getBranchManager());
        assertNotEquals(initialHeadId, newHeadId);
        assertTrue(repo.getCommitStore().exists(newHeadId));
        assertTrue(repo.getStaging().isEmpty());

        Commit committed = repo.getCommitStore().load(newHeadId);
        assertEquals("first commit", committed.getMessage());
        assertNotNull(committed.getBlobId("a.txt"));
    }

    @Test
    void execute_preservesUntouchedFilesAndRemovesStagedRemovals() throws Exception {
        writeFile("keep.txt", "stay");
        writeFile("drop.txt", "bye");
        addCommand.execute(new String[]{"keep.txt"});
        addCommand.execute(new String[]{"drop.txt"});
        commitCommand.execute(new String[]{"add two files"});

        rmCommand.execute(new String[]{"drop.txt"});
        commitCommand.execute(new String[]{"remove drop.txt"});

        String headId = repo.getHead().getCurrentCommitId(repo.getBranchManager());
        Commit latest = repo.getCommitStore().load(headId);
        assertNotNull(latest.getBlobId("keep.txt"));
        assertNull(latest.getBlobId("drop.txt"));
        assertTrue(repo.getStaging().isEmpty());
    }

    @Test
    void execute_throwsWhenStagingAreaIsEmpty() {
        BranchForgeException ex = assertThrows(
                BranchForgeException.class,
                () -> commitCommand.execute(new String[]{"nothing staged"})
        );
        assertEquals("No changes added to the commit.", ex.getMessage());
    }

    @Test
    void execute_throwsWhenMessageIsMissingBlankOrTooManyArgs() {
        assertThrows(BranchForgeException.class, () -> commitCommand.execute(new String[]{}));
        assertThrows(BranchForgeException.class, () -> commitCommand.execute(new String[]{""}));
        assertThrows(BranchForgeException.class, () -> commitCommand.execute(new String[]{"   "}));
        assertThrows(BranchForgeException.class, () -> commitCommand.execute(new String[]{"msg1", "msg2"}));
    }


    @Test
    void execute_throwsBranchForgeExceptionWhenArgsIsNull() {
        assertThrows(BranchForgeException.class, () -> commitCommand.execute(null));
    }

    @Test
    void execute_throwsBranchForgeExceptionWhenMessageIsNull() {
        assertThrows(BranchForgeException.class, () -> commitCommand.execute(new String[]{null}));
    }

    private void writeFile(String name, String content) throws Exception {
        Files.writeString(tempDir.resolve(name), content, StandardCharsets.UTF_8);
    }
}