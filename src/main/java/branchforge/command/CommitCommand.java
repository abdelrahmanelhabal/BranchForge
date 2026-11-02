package branchforge.command;

import branchforge.BranchForgeException;
import branchforge.model.*;
import branchforge.store.CommitStore;
import java.util.Date;

public class CommitCommand implements Command {
    private final StagingArea staging;
    private final CommitStore commitStore;
    private final BranchManager branches;
    private final HeadPointer head;

    public CommitCommand(StagingArea staging, CommitStore commitStore,
                         BranchManager branches, HeadPointer head) {
        this.staging = staging;
        this.commitStore = commitStore;
        this.branches = branches;
        this.head = head;
    }

    @Override
    public void execute(String[] args) {
        if (args.length != 1 || args[0].trim().isEmpty()) {
            throw new BranchForgeException("Please enter a commit message.");
        }
        createCommit(args[0]);
    }

    private void createCommit(String msg) {
        if (staging.isEmpty()) {
            throw new BranchForgeException("No changes added to the commit.");
        }

        String currentBranch = head.getCurrentBranch();
        if (currentBranch == null) {
            throw new BranchForgeException("Cannot commit without an active branch.");
        }

        Commit parentCommit = null;

        if (branches.exists(currentBranch)) {
            String headCommitId = head.getCurrentCommitId(branches);
            parentCommit = commitStore.load(headCommitId);
        }

        Commit newCommit = CommitFactory.fromStaging(parentCommit, staging, msg, new Date());
        commitStore.save(newCommit);

        if (branches.exists(currentBranch)) {
            branches.set(currentBranch, newCommit.getId());
        } else {
            branches.create(currentBranch, newCommit.getId());
        }

        staging.clear();
    }
}