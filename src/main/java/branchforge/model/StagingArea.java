package branchforge.model;
import java.io.Serial;
import java.io.Serializable;
import java.util.*;

public class StagingArea implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L ;

    private final TreeMap<String,String> stagedFiles = new TreeMap<>();
    private final TreeSet<String> removedFiles = new TreeSet<>();

    public void stageAdd(String file , String blobId){
        removedFiles.remove(file);
        stagedFiles.put(file,blobId);
    }

    public void stageRemove(String file){
        removedFiles.add(file);
        stagedFiles.remove(file);
    }

    public void unstage(String file){
        removedFiles.remove(file);
        stagedFiles.remove(file);
    }

    public void clear(){
        removedFiles.clear();
        stagedFiles.clear();
    }

    public boolean isEmpty(){
        return removedFiles.isEmpty() && stagedFiles.isEmpty();
    }

    public Map<String,String> getStagedFiles(){
        return Collections.unmodifiableMap(stagedFiles);
    }

    public Set<String> getRemovedFiles(){
        return Collections.unmodifiableSet(removedFiles);
    }

}
