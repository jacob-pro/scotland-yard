package uk.ac.bris.cs.scotlandyard.server.messaging;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

public class StringSerializer {

	public static String SerializeObject(Serializable obj) {
		try {
			ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
			ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
			objectOutputStream.writeObject(obj);
			objectOutputStream.flush();
			return byteArrayOutputStream.toString();
		} catch (Exception e) {
			return null;
		}
	}

}
