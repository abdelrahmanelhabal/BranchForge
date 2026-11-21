package branchforge.command;

import branchforge.BranchForgeException;
import branchforge.model.Commit;
import branchforge.model.CommitFactory;
import branchforge.repo.Repository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class GlobalLogCommandTest {

    @TempDir
    Path tempDir;

    private Repository repo;
    private AddCommand addCommand;
    private GlobalLogCommand globalLogCommand;

    private PrintStream originalOut;
    private ByteArrayOutputStream outContent;

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
        globalLogCommand = new GlobalLogCommand(repo.getCommitStore());

        originalOut = System.out;
        outContent = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outContent, true, StandardCharsets.UTF_8));
    }

    @AfterEach
    void tearDown() {
        System.setOut(originalOut);
    }


    @Test
    void execute_printsInitialCommitOnFreshRepository() {
        String initialId = repo.getHead().getCurrentCommitId(repo.getBranchManager());

        globalLogCommand.execute(new String[]{});

        String output = outContent.toString(StandardCharsets.UTF_8);
        assertTrue(output.contains("commit " + initialId));
        assertTrue(output.contains("==="));
    }

    @Test
    void execute_printsAllCommitsInReverseChronologicalOrder() throws Exception {
        String initialId = repo.getHead().getCurrentCommitId(repo.getBranchManager());
        Commit initial = repo.getCommitStore().load(initialId);

        Commit older = saveCommit(initial, "a.txt", "v1", "older commit", new Date(10_000L));
        Commit newer = saveCommit(older, "a.txt", "v2", "newer commit", new Date(20_000L));

        globalLogCommand.execute(new String[]{});

        String output = outContent.toString(StandardCharsets.UTF_8);
        int newerIdx = output.indexOf("commit " + newer.getId());
        int olderIdx = output.indexOf("commit " + older.getId());
        int initialIdx = output.indexOf("commit " + initialId);

        assertTrue(newerIdx != -1 && olderIdx != -1 && initialIdx != -1);
        assertTrue(newerIdx < olderIdx);
        assertTrue(olderIdx < initialIdx);
    }

    @Test
    void execute_throwsWhenExtraOperandsProvided() {
        BranchForgeException ex = assertThrows(
                BranchForgeException.class,
                () -> globalLogCommand.execute(new String[]{"extra"})
        );
        assertEquals("Incorrect operands.", ex.getMessage());
    }

    @Test
    void execute_throwsBranchForgeExceptionWhenArgsIsNull() {
        assertThrows(BranchForgeException.class, () -> globalLogCommand.execute(null));
    }

    @Test
    void constructor_throwsIllegalArgumentExceptionWhenCommitStoreIsNull() {
        assertThrows(IllegalArgumentException.class, () -> new GlobalLogCommand(null));
    }


    private Commit saveCommit(Commit parent, String fileName, String content, String message, Date timestamp) throws Exception {
        Files.writeString(tempDir.resolve(fileName), content, StandardCharsets.UTF_8);
        addCommand.execute(new String[]{fileName});
        Commit commit = CommitFactory.fromStaging(parent, repo.getStaging(), message, timestamp);
        repo.getCommitStore().save(commit);
        repo.getBranchManager().set(repo.getHead().getCurrentBranch(), commit.getId());
        repo.getStaging().clear();
        return commit;
    }
}