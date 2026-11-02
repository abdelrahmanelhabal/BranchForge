package branchforge.command;

import branchforge.BranchForgeException;
import branchforge.model.*;
import branchforge.store.CommitStore;

import java.util.Queue;
import java.util.LinkedList;
import java.util.Set;
import java.util.HashSet;

public class LogCommand implements Command {
    private final CommitStore commitStore;
    private final BranchManager branches;
    private final HeadPointer head;

    public LogCommand(CommitStore commitStore, BranchManager branches, HeadPointer head) {
        this.commitStore = commitStore;
        this.branches = branches;
        this.head = head;
    }

    @Override
    public void execute(String[] args) {
        if (args.length > 0) {
            throw new BranchForgeException("Incorrect operands.");
        }

        String startId = head.getCurrentCommitId(branches);
        if (startId == null) return;

        Queue<String> queue = new LinkedList<>();
        Set<String> visited = new HashSet<>();

        queue.add(startId);

        while (!queue.isEmpty()) {
            String currentId = queue.poll();

            if (visited.contains(currentId)) continue;

            visited.add(currentId);
            Commit c = commitStore.load(currentId);

            CommitPrinter.print(c);

            for (String parentId : c.getParentIds()) {
                if (!visited.contains(parentId)) {
                    queue.add(parentId);
                }
            }
        }
    }
}