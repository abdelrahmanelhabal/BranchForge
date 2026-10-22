package branchforge.modle;
import branchforge.util.Utils;
import java.io.Serial;
import java.io.Serializable;

public class Blob implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L ;

    private final String id ;
    private final byte[] content;

    public Blob(byte[] content){
        this.content = content;
        this.id = Utils.sha1("blob",this.content);
    }

    public String getId(){
        return id ;
    }

    public byte[] getContent(){
        return content.clone();
    }

}
