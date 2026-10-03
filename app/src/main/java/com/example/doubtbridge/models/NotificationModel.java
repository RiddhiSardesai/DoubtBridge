package com.example.doubtbridge.models;

public class NotificationModel {
    private String id;
    private String userId;
    private String title;
    private String message;
    private String type;
    private String doubtId;
    private boolean isRead;
    private long timestamp;

    public NotificationModel() {}

    public NotificationModel(String userId, String title, String message, String type, String doubtId) {
        this.userId    = userId;
        this.title     = title;
        this.message   = message;
        this.type      = type;
        this.doubtId   = doubtId;
        this.isRead    = false;
        this.timestamp = System.currentTimeMillis();
    }

    public String getId()            { return id; }
    public void   setId(String id)   { this.id = id; }
    public String getUserId()        { return userId; }
    public void   setUserId(String u){ this.userId = u; }
    public String getTitle()         { return title; }
    public void   setTitle(String t) { this.title = t; }
    public String getMessage()       { return message; }
    public void   setMessage(String m){ this.message = m; }
    public String getType()          { return type; }
    public void   setType(String t)  { this.type = t; }
    public String getDoubtId()       { return doubtId; }
    public void   setDoubtId(String d){ this.doubtId = d; }
    // Use standard getter name — Firestore reads field "isRead" correctly
    public boolean isRead()          { return isRead; }
    public void    setRead(boolean r){ this.isRead = r; }
    public long  getTimestamp()      { return timestamp; }
    public void  setTimestamp(long t){ this.timestamp = t; }
}
