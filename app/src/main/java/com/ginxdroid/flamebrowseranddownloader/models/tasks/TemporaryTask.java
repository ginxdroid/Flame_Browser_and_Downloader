package com.ginxdroid.flamebrowseranddownloader.models.tasks;

public class TemporaryTask {
    private String URL;
    private String UserAgent;
    private String ContentDisposition;
    private String MimeType;
    private String PageURL;
    private long ContentLength;
    private String Name;

    public TemporaryTask() {
    }

    public String getURL() {
        return URL;
    }

    public void setURL(String URL) {
        this.URL = URL;
    }

    public String getUserAgent() {
        return UserAgent;
    }

    public void setUserAgent(String userAgent) {
        UserAgent = userAgent;
    }

    public String getContentDisposition() {
        return ContentDisposition;
    }

    public void setContentDisposition(String contentDisposition) {
        ContentDisposition = contentDisposition;
    }

    public String getMimeType() {
        return MimeType;
    }

    public void setMimeType(String mimeType) {
        MimeType = mimeType;
    }

    public String getPageURL() {
        return PageURL;
    }

    public void setPageURL(String pageURL) {
        PageURL = pageURL;
    }

    public long getContentLength() {
        return ContentLength;
    }

    public void setContentLength(long contentLength) {
        ContentLength = contentLength;
    }

    public String getName() {
        return Name;
    }

    public void setName(String name) {
        Name = name;
    }
}
