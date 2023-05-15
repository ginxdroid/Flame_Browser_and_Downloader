package com.ginxdroid.flamebrowseranddownloader.models;

public class BookmarkItem {
    private Integer BKeyId;
    private String BFaviconPath;
    private String BTitle;
    private String BURL;

    public BookmarkItem() {
    }

    public Integer getBKeyId() {
        return BKeyId;
    }

    public void setBKeyId(Integer BKeyId) {
        this.BKeyId = BKeyId;
    }

    public String getBFaviconPath() {
        return BFaviconPath;
    }

    public void setBFaviconPath(String BFaviconPath) {
        this.BFaviconPath = BFaviconPath;
    }

    public String getBTitle() {
        return BTitle;
    }

    public void setBTitle(String BTitle) {
        this.BTitle = BTitle;
    }

    public String getBURL() {
        return BURL;
    }

    public void setBURL(String BURL) {
        this.BURL = BURL;
    }
}
