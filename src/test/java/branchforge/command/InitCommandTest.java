package branchforge.command;

import branchforge.BranchForgeException;
import branchforge.repo.Repository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class InitCommandTest {

    @TempDir
    Path tempDir;

    private InitCommand initCommand;

    @BeforeEach
    void setUp() {
        initCommand = new InitCommand(tempDir.toFile());
    }

    @Test
    void execute_initializesRepositoryInRootDir() {
        initCommand.execute(new String[]{});

        Repository repo = new Repository(tempDir.toFile());
        assertTrue(repo.isInitialized());
    }

    @Test
    void execute_throwsWhenRepositoryAlreadyInitialized() {
        initCommand.execute(new String[]{});

        assertThrows(BranchForgeException.class, () -> initCommand.execute(new String[]{}));
    }

    @Test
    void execute_throwsWhenExtraOperandsProvided() {
        BranchForgeException ex = assertThrows(
                BranchForgeException.class,
                () -> initCommand.execute(new String[]{"extra"})
        );
        assertEquals("Incorrect operands.", ex.getMessage());
    }

    @Test
    void execute_throwsBranchForgeExceptionWhenArgsIsNull() {
        assertThrows(BranchForgeException.class, () -> initCommand.execute(null));
    }

    @Test
    void constructor_throwsIllegalArgumentExceptionWhenRootDirIsNull() {
        assertThrows(IllegalArgumentException.class, () -> new InitCommand(null));
    }
}