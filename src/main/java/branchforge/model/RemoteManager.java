package branchforge.model;
import branchforge.BranchForgeException;
import java.io.Serial;
import java.io.Serializable;
import java.util.Collections;
import java.util.Set;
import java.util.TreeMap;

public class RemoteManager implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L ;

    private TreeMap<String,String> remotes = new TreeMap<>();

    public void add(String name , String url){
        if(remotes.containsKey(name)){
            throw new BranchForgeException("A remote with that name already exists.");
        }
        remotes.put(name , url);
    }

    public void remove(String name){
        if(!remotes.containsKey(name)){
            throw new BranchForgeException("A remote with that name does not exist.");
        }
        remotes.remove(name);
    }

    public String get(String name){
        return remotes.get(name);
    }

    public Set<String> list(){
        return Collections.unmodifiableSet(remotes.keySet());
    }
}
