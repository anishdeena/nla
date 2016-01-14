package com.example.anish.testchat.other;

/**
 * Created by anish on 31/10/15.
 */
public class Message {
    private String fromName, message, payload;
    private boolean isSelf;

    public Message() {
    }

    public Message(String fromName, String message, boolean isSelf) {
        this.fromName = fromName;
        this.message = message;
        this.isSelf = isSelf;
    }

    public Message(String fromName, String message, boolean isSelf, String payload) {
        this.fromName = fromName;
        this.message = message;
        this.isSelf = isSelf;
        this.payload = payload;

    }

    public String getFromName() {
        return fromName;
    }

    public void setFromName(String fromName) {
        this.fromName = fromName;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean isSelf() {
        return isSelf;
    }

    public void setSelf(boolean isSelf) {
        this.isSelf = isSelf;
    }

    public String getPayload() { return payload; }

}
