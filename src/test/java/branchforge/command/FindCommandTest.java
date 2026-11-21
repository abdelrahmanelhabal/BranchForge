package branchforge.command;

import branchforge.BranchForgeException;
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

import static org.junit.jupiter.api.Assertions.*;

class FindCommandTest {

    @TempDir
    Path tempDir;

    private Repository repo;
    private AddCommand addCommand;
    private CommitCommand commitCommand;
    private FindCommand findCommand;

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
        commitCommand = new CommitCommand(
                repo.getStaging(),
                repo.getCommitStore(),
                repo.getBranchManager(),
                repo.getHead()
        );
        findCommand = new FindCommand(repo.getCommitStore());

        originalOut = System.out;
        outContent = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outContent, true, StandardCharsets.UTF_8));
    }

    @AfterEach
    void tearDown() {
        System.setOut(originalOut);
    }


    @Test
    void execute_printsCommitIdForSingleMatchingCommit() throws Exception {
        String id = makeCommit("a.txt", "v1", "add feature A");

        findCommand.execute(new String[]{"add feature A"});

        String output = outContent.toString(StandardCharsets.UTF_8).trim();
        assertEquals(id, output);
    }

    @Test
    void execute_printsAllCommitIdsWhenMultipleCommitsShareMessage() throws Exception {
        String id1 = makeCommit("a.txt", "v1", "fix bug");
        String id2 = makeCommit("a.txt", "v2", "fix bug");
        String otherId = makeCommit("a.txt", "v3", "unrelated");

        findCommand.execute(new String[]{"fix bug"});

        String output = outContent.toString(StandardCharsets.UTF_8);
        assertTrue(output.contains(id1));
        assertTrue(output.contains(id2));
        assertFalse(output.contains(otherId));
    }

    @Test
    void execute_requiresExactMessageMatch() throws Exception {
        makeCommit("a.txt", "v1", "fix bug");

        assertThrows(BranchForgeException.class, () -> findCommand.execute(new String[]{"fix"}));
        assertThrows(BranchForgeException.class, () -> findCommand.execute(new String[]{"FIX BUG"}));
    }

    @Test
    void execute_throwsWhenNoCommitMatchesMessage() {
        BranchForgeException ex = assertThrows(
                BranchForgeException.class,
                () -> findCommand.execute(new String[]{"does not exist"})
        );
        assertEquals("Found no commit with that message.", ex.getMessage());
    }

    @Test
    void execute_throwsWhenOperandCountIsInvalid() {
        assertThrows(BranchForgeException.class, () -> findCommand.execute(new String[]{}));
        assertThrows(BranchForgeException.class, () -> findCommand.execute(new String[]{"msg1", "msg2"}));
    }

    @Test
    void execute_throwsBranchForgeExceptionWhenArgsIsNull() {
        assertThrows(BranchForgeException.class, () -> findCommand.execute(null));
    }

    @Test
    void constructor_throwsIllegalArgumentExceptionWhenCommitStoreIsNull() {
        assertThrows(IllegalArgumentException.class, () -> new FindCommand(null));
    }


    private String makeCommit(String fileName, String content, String message) throws Exception {
        Files.writeString(tempDir.resolve(fileName), content, StandardCharsets.UTF_8);
        addCommand.execute(new String[]{fileName});
        commitCommand.execute(new String[]{message});
        return repo.getHead().getCurrentCommitId(repo.getBranchManager());
    }
}