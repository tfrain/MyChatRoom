package com.lucky.bean;

import java.util.Date;

public class FileMsg {

    private String fileName;
    private long fileSize;
    private Date date;

    public FileMsg(String fileName, long fileSize, Date date) {
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.date = date;
    }

    public String getFileName() {
        return fileName;
    }

    public long getFileSize() {
        return fileSize;
    }

    public Date getDate() {
        return date;
    }
}
