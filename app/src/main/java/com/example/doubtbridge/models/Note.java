package com.example.doubtbridge.models;

import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;
import java.util.List;

public class Note {
    private String id;
    private String title;
    private String content;       // extracted text
    private String subject;
    private String department;
    private String className;
    private String uploadedBy;
    private String uploaderName;
    private String fileUrl;       // (Optional) for external links
    private String fileBase64;    // Alternative to Storage: Stores document in Firestore
    private String fileType;      // "text","pdf","ppt"
    private List<String> keywords;
    @ServerTimestamp
    private Date timestamp;

    public Note() {}

    public Note(String title, String content, String subject, String department,
                String className, String uploadedBy, String uploaderName,
                List<String> keywords, String fileUrl, String fileType) {
        this.title = title;
        this.content = content;
        this.subject = subject;
        this.department = department;
        this.className = className;
        this.uploadedBy = uploadedBy;
        this.uploaderName = uploaderName;
        this.keywords = keywords;
        this.fileUrl = fileUrl;
        this.fileType = fileType;
    }

    public Note(String title, String content, String subject, String department,
                String className, String uploadedBy, String uploaderName,
                List<String> keywords, String fileUrl, String fileType, String fileBase64) {
        this.title = title;
        this.content = content;
        this.subject = subject;
        this.department = department;
        this.className = className;
        this.uploadedBy = uploadedBy;
        this.uploaderName = uploaderName;
        this.keywords = keywords;
        this.fileUrl = fileUrl;
        this.fileType = fileType;
        this.fileBase64 = fileBase64;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String t) { this.title = t; }
    public String getContent() { return content; }
    public void setContent(String c) { this.content = c; }
    public String getSubject() { return subject; }
    public void setSubject(String s) { this.subject = s; }
    public String getDepartment() { return department; }
    public void setDepartment(String d) { this.department = d; }
    public String getClassName() { return className; }
    public void setClassName(String c) { this.className = c; }
    public String getUploadedBy() { return uploadedBy; }
    public void setUploadedBy(String u) { this.uploadedBy = u; }
    public String getUploaderName() { return uploaderName; }
    public void setUploaderName(String n) { this.uploaderName = n; }
    public String getFileUrl() { return fileUrl; }
    public void setFileUrl(String u) { this.fileUrl = u; }
    public String getFileBase64() { return fileBase64; }
    public void setFileBase64(String b) { this.fileBase64 = b; }
    public String getFileType() { return fileType; }
    public void setFileType(String t) { this.fileType = t; }
    public List<String> getKeywords() { return keywords; }
    public void setKeywords(List<String> k) { this.keywords = k; }
    public Date getTimestamp() { return timestamp; }
    public void setTimestamp(Date t) { this.timestamp = t; }
}
