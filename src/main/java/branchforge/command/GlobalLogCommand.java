package branchforge.command;

import branchforge.BranchForgeException;
import branchforge.model.Commit;
import branchforge.store.CommitStore;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class GlobalLogCommand implements Command {
    private final CommitStore commitStore;

    public GlobalLogCommand(CommitStore commitStore) {
        this.commitStore = commitStore;
    }

    @Override
    public void execute(String[] args) {
        if (args.length > 0) {
            throw new BranchForgeException("Incorrect operands.");
        }

        List<Commit> all = new ArrayList<>(commitStore.loadAll());

        all.sort(Comparator.comparing(Commit::getTimeStamp)
                .thenComparing(Commit::getId)
                .reversed());

        all.forEach(CommitPrinter::print);
    }
}