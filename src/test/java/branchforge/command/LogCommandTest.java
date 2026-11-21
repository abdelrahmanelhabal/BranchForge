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

class LogCommandTest {

    @TempDir
    Path tempDir;

    private Repository repo;
    private AddCommand addCommand;
    private CommitCommand commitCommand;
    private LogCommand logCommand;

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
        logCommand = new LogCommand(
                repo.getCommitStore(),
                repo.getBranchManager(),
                repo.getHead()
        );

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

        logCommand.execute(new String[]{});

        String output = outContent.toString(StandardCharsets.UTF_8);
        assertTrue(output.contains("commit " + initialId));
        assertTrue(output.contains("==="));
    }

    @Test
    void execute_printsCommitHistoryFromHeadToInitialCommit() throws Exception {
        String initialId = repo.getHead().getCurrentCommitId(repo.getBranchManager());
        String firstId = makeCommit("a.txt", "v1", "first commit");
        String secondId = makeCommit("a.txt", "v2", "second commit");

        logCommand.execute(new String[]{});

        String output = outContent.toString(StandardCharsets.UTF_8);
        int secondIdx = output.indexOf("commit " + secondId);
        int firstIdx = output.indexOf("commit " + firstId);
        int initialIdx = output.indexOf("commit " + initialId);

        assertTrue(secondIdx != -1 && firstIdx != -1 && initialIdx != -1);
        assertTrue(secondIdx < firstIdx && firstIdx < initialIdx);
        assertTrue(output.contains("second commit"));
        assertTrue(output.contains("first commit"));
    }

    @Test
    void execute_throwsWhenExtraOperandsProvided() {
        BranchForgeException ex = assertThrows(
                BranchForgeException.class,
                () -> logCommand.execute(new String[]{"extra"})
        );
        assertEquals("Incorrect operands.", ex.getMessage());
    }


    @Test
    void execute_throwsBranchForgeExceptionWhenArgsIsNull() {
        assertThrows(BranchForgeException.class, () -> logCommand.execute(null));
    }

    @Test
    void constructor_throwsIllegalArgumentExceptionWhenDependenciesAreNull() {
        assertThrows(IllegalArgumentException.class,
                () -> new LogCommand(null, repo.getBranchManager(), repo.getHead()));
        assertThrows(IllegalArgumentException.class,
                () -> new LogCommand(repo.getCommitStore(), null, repo.getHead()));
        assertThrows(IllegalArgumentException.class,
                () -> new LogCommand(repo.getCommitStore(), repo.getBranchManager(), null));
    }

    private String makeCommit(String fileName, String content, String message) throws Exception {
        Files.writeString(tempDir.resolve(fileName), content, StandardCharsets.UTF_8);
        addCommand.execute(new String[]{fileName});
        commitCommand.execute(new String[]{message});
        return repo.getHead().getCurrentCommitId(repo.getBranchManager());
    }
}