package Trabalho;

import java.io.Serializable;

public class Message implements Serializable {
    private String src;
    private String text;
    private Action action;

    public String getSrc() {
        return src;
    }

    public void setSrc(String src) {
        this.src = src;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public Action getAction() {
        return action;
    }

    public void setAction(Action action) {
        this.action = action;
    }
    
    public enum Action {
        CONNECT, MESSAGE, PING, PING_BACK, ASK, IP
    }
}
