package com.lucky.bean;

import java.util.Date;

public class FileMsg {

    private String usr;
    private String word;
    private Date date;

    public FileMsg(String usr, String word, Date date) {
        this.usr = usr;
        this.word = word;
        this.date = date;
    }

    public String getUsr() {
        return usr;
    }

    public String getWord() {
        return word;
    }

    public Date getDate() {
        return date;
    }
}
