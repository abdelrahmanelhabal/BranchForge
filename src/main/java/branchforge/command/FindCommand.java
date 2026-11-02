package branchforge.command;

import branchforge.BranchForgeException;
import branchforge.model.Commit;
import branchforge.store.CommitStore;
import java.util.List;

public class FindCommand implements Command {
    private final CommitStore commitStore;

    public FindCommand(CommitStore commitStore) {
        this.commitStore = commitStore;
    }

    @Override
    public void execute(String[] args) {
        if (args.length != 1) {
            throw new BranchForgeException("Incorrect operands.");
        }
        String searchMessage = args[0];

        List<Commit> allCommits = commitStore.loadAll();
        boolean found = false;

        for (Commit commit : allCommits) {
            if (commit.getMessage().equals(searchMessage)) {
                System.out.println(commit.getId());
                found = true;
            }
        }

        if (!found) {
            throw new BranchForgeException("Found no commit with that message.");
        }
    }
}