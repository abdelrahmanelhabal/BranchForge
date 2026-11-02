package branchforge.command;

import branchforge.BranchForgeException;
import branchforge.repo.Repository;
import java.util.Map;
import java.util.function.Function;
public class CommandRegistry {
    private static final Map<String, Function<Repository, Command>> FACTORIES = Map.of(
            "add",        r -> new AddCommand(r.getWorkingDir(), r.getStaging(), r.getBlobStore(),
                    r.getCommitStore(), r.getBranchManager(), r.getHead()),
            "commit",     r -> new CommitCommand(r.getStaging(), r.getCommitStore(), r.getBranchManager(), r.getHead()),
            "rm",         r -> new RmCommand(r.getWorkingDir(), r.getStaging(), r.getCommitStore(),
                    r.getBranchManager(), r.getHead()),
            "log",        r -> new LogCommand(r.getCommitStore(), r.getBranchManager(), r.getHead()),
            "global-log", r -> new GlobalLogCommand(r.getCommitStore()),
            "find",       r -> new FindCommand(r.getCommitStore())
    );
    private final Repository repo;
    public CommandRegistry(Repository repo) {
        this.repo = repo;
    }
    public static boolean isKnown(String name) {
        return FACTORIES.containsKey(name);
    }
    public Command get(String name) {
        Function<Repository, Command> f = FACTORIES.get(name);
        if (f == null) {
            throw new BranchForgeException("No command with that name exists.");
        }
        return f.apply(repo);
    }
}

