package branchforge.model;
import java.io.Serial;
import java.io.Serializable;

public class HeadPointer implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L ;

    private String currentBranch ;

    public HeadPointer(String initialBranch){
        this.currentBranch = initialBranch;
    }

    public String getCurrentBranch(){
        return currentBranch;
    }

    public void setCurrentBranch(String name){
        this.currentBranch = name ;
    }

    public String getCurrentCommitId(BranchManager branchManager){
        return branchManager.get(currentBranch);
    }
}
