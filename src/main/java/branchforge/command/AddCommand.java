package branchforge.command;

import branchforge.BranchForgeException;
import branchforge.model.*;
import branchforge.store.*;

public class AddCommand implements Command {
    private final WorkingDirectory workingDir;
    private final StagingArea staging;
    private final BlobStore blobStore;
    private final CommitStore commitStore;
    private final BranchManager branches;
    private final HeadPointer head;

    public AddCommand(WorkingDirectory workingDir, StagingArea staging, BlobStore blobStore,
                      CommitStore commitStore, BranchManager branches, HeadPointer head) {
        this.workingDir = workingDir;
        this.staging = staging;
        this.blobStore = blobStore;
        this.commitStore = commitStore;
        this.branches = branches;
        this.head = head;
    }

    @Override
    public void execute(String[] args) {
        if (args.length != 1) {
            throw new BranchForgeException("Incorrect operands.");
        }
        String file = args[0];
        validateExists(file);
        stageFile(file);
    }

    private void validateExists(String file) {
        if (!workingDir.exists(file)) {
            throw new BranchForgeException("File does not exist.");
        }
    }

    private void stageFile(String file) {
        Blob blob = new Blob(workingDir.read(file));
        String currentBranch = head.getCurrentBranch();
        String currentBlobIdInCommit = null;
        if (currentBranch != null && branches.exists(currentBranch)) {
            String headCommitId = head.getCurrentCommitId(branches);
            if (headCommitId != null) {
                Commit currentCommit = commitStore.load(headCommitId);
                currentBlobIdInCommit = currentCommit.getBlobId(file);
            }
        }
        if (blob.getId().equals(currentBlobIdInCommit)) {
            staging.unstage(file);
        } else {
            blobStore.save(blob);
            staging.stageAdd(file, blob.getId());
        }
    }
}