package com.example.doubtbridge.models;

import com.google.firebase.firestore.PropertyName;

public class Doubt {
    private String id;
    private String title;
    private String description;
    private String userName;
    private String userId;
    private String subject;
    private String department;
    private String className;
    private String status;
    private String answer;
    private String answeredBy;
    
    @PropertyName("isImportant")
    private boolean isImportant;
    
    @PropertyName("isBookmarked")
    private boolean isBookmarked;
    
    private long frequency;
    private long timestamp;
    private long answeredAt;
    private String sourceNoteId;
    private String imageUrl;

    public Doubt() {}

    public Doubt(String title, String description, String userName, String userId,
                 String subject, String department, String className) {
        this(title, description, userName, userId, subject, department, className, null);
    }

    public Doubt(String title, String description, String userName, String userId,
                 String subject, String department, String className, String imageUrl) {
        this.title = title;
        this.description = description;
        this.userName = userName;
        this.userId = userId;
        this.subject = subject;
        this.department = department;
        this.className = className;
        this.imageUrl = imageUrl;
        this.status = "pending";
        this.isImportant = false;
        this.isBookmarked = false;
        this.frequency = 1;
        this.timestamp = System.currentTimeMillis();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    
    public String getDescription() { return description; }
    public void setDescription(String desc) { this.description = desc; }
    
    public String getUserName() { return userName; }
    public void setUserName(String n) { this.userName = n; }
    
    public String getUserId() { return userId; }
    public void setUserId(String uid) { this.userId = uid; }
    
    public String getSubject() { return subject; }
    public void setSubject(String s) { this.subject = s; }
    
    public String getDepartment() { return department; }
    public void setDepartment(String d) { this.department = d; }
    
    public String getClassName() { return className; }
    public void setClassName(String c) { this.className = c; }
    
    public String getStatus() { return status; }
    public void setStatus(String s) { this.status = s; }
    
    public String getAnswer() { return answer; }
    public void setAnswer(String a) { this.answer = a; }
    
    public String getAnsweredBy() { return answeredBy; }
    public void setAnsweredBy(String a) { this.answeredBy = a; }

    @PropertyName("isImportant")
    public boolean isImportant() { return isImportant; }
    
    @PropertyName("isImportant")
    public void setImportant(boolean v) { this.isImportant = v; }

    @PropertyName("isBookmarked")
    public boolean isBookmarked() { return isBookmarked; }
    
    @PropertyName("isBookmarked")
    public void setBookmarked(boolean v) { this.isBookmarked = v; }

    public long getFrequency() { return frequency; }
    public void setFrequency(long f) { this.frequency = f; }
    
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long t) { this.timestamp = t; }

    public long getAnsweredAt() { return answeredAt; }
    public void setAnsweredAt(long a) { this.answeredAt = a; }

    public String getSourceNoteId() { return sourceNoteId; }
    public void setSourceNoteId(String id) { this.sourceNoteId = id; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
}
