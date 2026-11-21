package branchforge.command;

import branchforge.BranchForgeException;
import branchforge.model.*;
import branchforge.store.*;

public class RmCommand implements Command {
    private final WorkingDirectory workingDir;
    private final StagingArea staging;
    private final CommitStore commitStore;
    private final BranchManager branches;
    private final HeadPointer head;

    public RmCommand(WorkingDirectory workingDir, StagingArea staging, CommitStore commitStore,
                     BranchManager branches, HeadPointer head) {
        this.workingDir = workingDir;
        this.staging = staging;
        this.commitStore = commitStore;
        this.branches = branches;
        this.head = head;
    }

    @Override
    public void execute(String[] args) {
        if (args==null || args.length != 1) {
            throw new BranchForgeException("Incorrect operands.");
        }
        String file = args[0];
        validateFileName(file);
        Commit currentCommit = commitStore.load(head.getCurrentCommitId(branches));
        boolean isTracked = currentCommit.getBlobId(file) != null;
        boolean isStaged = staging.getStagedFiles().containsKey(file);

        if (!isTracked && !isStaged) {
            throw new BranchForgeException("No reason to remove the file.");
        }

        if (isStaged) {
            staging.unstage(file);
        }

        if (isTracked) {
            staging.stageRemove(file);
            if (workingDir.exists(file)) {
                workingDir.delete(file);
            }
        }
    }
    private void validateFileName(String file) {
        if (file == null || file.isBlank()) {
            throw new BranchForgeException("No reason to remove the file.");
        }
    }
}