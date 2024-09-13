package org.vaadin.marcus.semantickernel;

public class Conversation {
    private String user;
    private String responseIntent;

    public Conversation(String user, String responseIntent) {
        this.user = user;
        this.responseIntent = responseIntent;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getResponseIntent() {
        return responseIntent;
    }

    public void setResponseIntent(String responseIntent) {
        this.responseIntent = responseIntent;
    }

    @Override
    public String toString() {
        return "Conversation{" +
                "user='" + user + '\'' +
                ", responseIntent='" + responseIntent + '\'' +
                '}';
    }
}
