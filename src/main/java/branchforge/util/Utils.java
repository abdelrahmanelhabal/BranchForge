package branchforge.util;

import branchforge.BranchForgeException;

import java.io.*;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Utils {
    private Utils(){}
    public static String sha1(byte[] contents){
        return sha1((Object) contents);
    }
    public static String sha1(Object... vals){
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            for (Object val : vals){
                if (val instanceof byte[]){
                    md.update((byte[]) val);
                }else if (val instanceof String){
                    md.update(((String) val).getBytes(StandardCharsets.UTF_8));
                }else {
                    throw new IllegalArgumentException("Unsupported type for SHA-1: " + val.getClass());
                }
            }
            return java.util.HexFormat.of().formatHex(md.digest());
        } catch (NoSuchAlgorithmException e){
            throw new BranchForgeException("System does not support SHA-1 algorithm.");
        }
    }
    public static byte[] readContents(File f){
        try {
            return Files.readAllBytes(f.toPath());
        } catch (IOException e){
            throw new BranchForgeException("Could not read file: " + f.getName());
        }
    }
    public static void writeContents(File f, byte[] contents){
        try {
            Files.write(f.toPath(), contents);
        } catch (IOException e){
            throw new BranchForgeException("Could not write to file: " + f.getName());
        }
    }
    public static void writeObject(File f , Serializable obj){
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(f))) {
            out.writeObject(obj);
        } catch (IOException e) {
            throw new BranchForgeException("Could not serialize to file: " + f.getName());
        }
    }
    public static <T extends Serializable> T readObject(File f, Class<T> expectedClass){
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(f))) {
            return expectedClass.cast(in.readObject());
        } catch (IOException | ClassNotFoundException | ClassCastException e) {
            throw new BranchForgeException("Could not read object from file: " + f.getName());
        }
    }
}
