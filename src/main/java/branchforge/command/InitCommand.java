package branchforge.command;

import branchforge.BranchForgeException;
import branchforge.repo.Repository;
import java.io.File;

public class InitCommand implements Command {
    private final File rootDir;

    public InitCommand(File rootDir) {
        this.rootDir = rootDir;
    }

    @Override
    public void execute(String[] args) {
        if (args.length > 0) {
            throw new BranchForgeException("Incorrect operands.");
        }
        Repository repo = new Repository(rootDir);
        repo.init();
    }
}