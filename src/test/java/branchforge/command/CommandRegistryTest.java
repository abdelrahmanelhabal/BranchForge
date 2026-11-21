package branchforge.command;

import branchforge.BranchForgeException;
import branchforge.repo.Repository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class CommandRegistryTest {

    @TempDir
    Path tempDir;

    private Repository repo;
    private CommandRegistry registry;

    @BeforeEach
    void setUp() {
        repo = new Repository(tempDir.toFile());
        repo.init();
        registry = new CommandRegistry(repo);
    }

    @Test
    void isKnown_returnsTrueForAllRegisteredCommands() {
        assertTrue(CommandRegistry.isKnown("add"));
        assertTrue(CommandRegistry.isKnown("commit"));
        assertTrue(CommandRegistry.isKnown("rm"));
        assertTrue(CommandRegistry.isKnown("log"));
        assertTrue(CommandRegistry.isKnown("global-log"));
        assertTrue(CommandRegistry.isKnown("find"));
    }

    @Test
    void isKnown_returnsFalseForUnknownOrWrongCaseCommands() {
        assertFalse(CommandRegistry.isKnown("unknown"));
        assertFalse(CommandRegistry.isKnown(""));
        assertFalse(CommandRegistry.isKnown("ADD"));
    }

    @Test
    void get_returnsCorrectCommandInstanceForEachRegisteredName() {
        assertInstanceOf(AddCommand.class, registry.get("add"));
        assertInstanceOf(CommitCommand.class, registry.get("commit"));
        assertInstanceOf(RmCommand.class, registry.get("rm"));
        assertInstanceOf(LogCommand.class, registry.get("log"));
        assertInstanceOf(GlobalLogCommand.class, registry.get("global-log"));
        assertInstanceOf(FindCommand.class, registry.get("find"));
    }

    @Test
    void get_throwsBranchForgeExceptionForUnknownCommand() {
        assertThrows(BranchForgeException.class, () -> registry.get("nonexistent"));
    }


    @Test
    void isKnown_returnsFalseWhenNameIsNull() {
        assertFalse(CommandRegistry.isKnown(null));
    }

    @Test
    void get_throwsBranchForgeExceptionWhenNameIsNull() {
        assertThrows(BranchForgeException.class, () -> registry.get(null));
    }

    @Test
    void constructor_throwsBranchForgeExceptionWhenRepositoryIsNull() {
        assertThrows(BranchForgeException.class, () -> new CommandRegistry(null));
    }
}