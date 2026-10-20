package branchforge.modle;
import branchforge.BranchForgeException;
import java.io.Serial;
import java.io.Serializable;
import java.util.Collections;
import java.util.Set;
import java.util.TreeMap;

public class BranchManager implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L ;

    private final TreeMap<String,String> branches = new TreeMap<>();

    public void create(String name , String commitId){
        if(branches.containsKey(name)){
            throw new BranchForgeException("A branch with that name already exists.");
        }
        branches.put(name,commitId);
    }

    public void delete(String name){
        if(!branches.containsKey(name)){
            throw new BranchForgeException("A branch with that name does not exist.");
        }
        branches.remove(name);
    }

    public String get(String name){
        if(!branches.containsKey(name)){
            throw new BranchForgeException("A branch with that name does not exist.");
        }
        return branches.get(name);
    }

    public boolean exists(String name){
        return branches.containsKey(name);
    }

    public Set<String> list(){
        return Collections.unmodifiableSet(branches.keySet());
    }
}
