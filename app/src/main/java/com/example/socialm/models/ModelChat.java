package com.example.socialm.models;

import com.google.firebase.database.PropertyName;

public class ModelChat {

    String name, message, receiver, sender, timestamp, type;
    boolean isSeen;

    public ModelChat() {
    }



    public ModelChat(String name, String message, String receiver, String sender, String timestamp, String type, boolean isSeen) {
        this.name = name;
        this.message = message;
        this.receiver = receiver;
        this.sender = sender;
        this.timestamp = timestamp;
        this.type = type;
        this.isSeen = isSeen;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getReceiver() {
        return receiver;
    }

    public void setReceiver(String receiver) {
        this.receiver = receiver;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    @PropertyName("isSeen")
    public boolean isSeen() {
        return isSeen;
    }


    @PropertyName("isSeen")
    public void setSeen(boolean seen) {
        isSeen = seen;
    }
}
