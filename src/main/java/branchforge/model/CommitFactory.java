package branchforge.model;
import branchforge.util.Utils;
import com.sun.source.tree.Tree;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class CommitFactory {

    private CommitFactory() { }

    public static Commit initialCommit(){
        Date ts = new Date(0);
        List<String> parents = new ArrayList<>();
        Map<String,String> files = new TreeMap<>();
        String msg = "initial commit" ;
        return new Commit(buildId(msg , ts , parents , files) , msg , ts , parents , files);
    }

    public static Commit formStaging(Commit parent , StagingArea staging , String msg , Date ts){
        TreeMap<String , String> files = new TreeMap<>(parent.getFileToBlob());
        files.putAll(staging.getStagedFiles());
        for(String f : staging.getRemovedFiles()){
            files.remove(f);
        }
        List<String> parents = new ArrayList<>();
        parents.add(parent.getId());
        return new Commit(buildId(msg,ts,parents,files),msg,ts,parents,files);
    }

    public static Commit fromMerge(String currentId , String targetId , Map<String,String> snapshot , String msg , Date ts){
        List<String> parents = new ArrayList<>();
        parents.add(currentId);
        parents.add(targetId);
        TreeMap<String,String> files = new TreeMap<>(snapshot);
        return new Commit(buildId(msg,ts,parents,files) , msg,ts,parents,files);
    }

    private static void writeString(DataOutputStream out, String s) throws IOException {
        byte[] b = s.getBytes(StandardCharsets.UTF_8);
        out.writeInt(b.length);
        out.write(b);
    }

    static byte[] encodeCanonical(String message, List<String> parents, Map<String, String> files, Date ts) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(bytes);
            writeString(out, message);
            out.writeLong(ts.getTime());
            out.writeInt(parents.size());
            for (String p : parents) {
                writeString(out, p);
            }
            TreeMap<String, String> sorted = new TreeMap<>(files);
            out.writeInt(sorted.size());
            for (Map.Entry<String, String> e : sorted.entrySet()) {
                writeString(out, e.getKey());
                writeString(out, e.getValue());
            }
            out.flush();
            return bytes.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String buildId(String message , Date timestamp, List<String> parents , Map<String,String> files){
        return Utils.sha1("commit" , encodeCanonical(message,parents,files,timestamp));
    }
}
