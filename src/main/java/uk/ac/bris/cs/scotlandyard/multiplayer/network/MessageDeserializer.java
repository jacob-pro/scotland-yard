package uk.ac.bris.cs.scotlandyard.multiplayer.network;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.google.gson.typeadapters.RuntimeTypeAdapterFactory;

import java.util.Optional;

class MessageDeserializer {

    private static final RuntimeTypeAdapterFactory<Message> typeFactory = RuntimeTypeAdapterFactory
            .of(Message.class, "messageType")
            .registerSubtype(Handshake.class, Message.MessageType.HANDSHAKE.toString())
            .registerSubtype(Request.class, Message.MessageType.REQUEST.toString())
            .registerSubtype(Response.class, Message.MessageType.RESPONSE.toString())
            .registerSubtype(Notification.class, Message.MessageType.NOTIFICATION.toString());

    private Gson gson = new GsonBuilder().registerTypeAdapterFactory(typeFactory).create();

    Optional<Message> deserialize(String s) {
        try {
            Message message = gson.fromJson(s, Message.class);
            return Optional.of(message);
        } catch (JsonParseException e) {
            return Optional.empty();
        }
    }

}
