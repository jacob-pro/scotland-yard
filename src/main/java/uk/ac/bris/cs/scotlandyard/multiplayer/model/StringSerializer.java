package uk.ac.bris.cs.scotlandyard.multiplayer.model;

import java.io.*;
import java.util.Base64;

public class StringSerializer {

    public static String serializeObject(Serializable obj) {
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
            objectOutputStream.writeObject(obj);
            objectOutputStream.flush();
            return Base64.getEncoder().encodeToString(byteArrayOutputStream.toByteArray());
        } catch (Exception e) {
            return null;
        }
    }

    public static Object deserializeObject(String string) {
        try {
            byte[] bytes = Base64.getDecoder().decode(string);
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bytes);
            ObjectInputStream objectInputStream = new ObjectInputStream(byteArrayInputStream);
            return objectInputStream.readObject();
        } catch (Exception e) {
            return null;
        }
    }

}
