package branchforge.model;
import java.io.Serial;
import java.io.Serializable;
import java.util.*;

public class Commit implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L ;

    private final String id ;
    private final String message ;
    private final Date timeStamp ;
    private final List<String> parentIds;
    private final TreeMap<String,String> fileToBlob ;

    public Commit(String id , String message , Date timeStamp , List<String> parentIds , Map<String,String> files){
        this.id = Objects.requireNonNull(id);
        this.message = Objects.requireNonNull(message);
        this.timeStamp = new Date( Objects.requireNonNull(timeStamp).getTime() );
        this.parentIds = validateParents(parentIds);
        this.fileToBlob = new TreeMap<>( Objects.requireNonNull(files) );
    }

    private List<String> validateParents(List<String> parentIds) {
        Objects.requireNonNull(parentIds);
        if (parentIds.size() > 2) {
            throw new IllegalArgumentException( "A commit cannot have more than 2 parents." );
        }
        if (new HashSet<>(parentIds).size() != parentIds.size()) {
            throw new IllegalArgumentException("A commit cannot have duplicate parents.");
        }
        return new ArrayList<>(parentIds);
    }

    public String getId(){
        return id;
    }

    public String getMessage(){
        return message;
    }

    public Date getTimeStamp(){
        return new Date(timeStamp.getTime());
    }

    public List<String> getParentIds(){
        return Collections.unmodifiableList(parentIds);
    }

    public Map<String,String> getFileToBlob(){
        return Collections.unmodifiableMap(fileToBlob);
    }

    public Set<String> getFiles(){
        return Collections.unmodifiableSet(fileToBlob.keySet());
    }

    public String getBlobId(String file){
        return fileToBlob.get(file);
    }

}
