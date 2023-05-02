package com.ginxdroid.flamebrowseranddownloader.models;

public class QuickLinkModel {

    private String qlURL;
    private String qlTitle;
    private String qlFaviconPath;
    private Integer qlVisiblePosition;

    private Integer qlKeyId;

    public QuickLinkModel() {
    }

    public String getQlURL() {
        return qlURL;
    }

    public void setQlURL(String qlURL) {
        this.qlURL = qlURL;
    }

    public String getQlTitle() {
        return qlTitle;
    }

    public void setQlTitle(String qlTitle) {
        this.qlTitle = qlTitle;
    }

    public String getQlFaviconPath() {
        return qlFaviconPath;
    }

    public void setQlFaviconPath(String qlFaviconPath) {
        this.qlFaviconPath = qlFaviconPath;
    }

    public Integer getQlVisiblePosition() {
        return qlVisiblePosition;
    }

    public void setQlVisiblePosition(Integer qlVisiblePosition) {
        this.qlVisiblePosition = qlVisiblePosition;
    }

    public Integer getQlKeyId() {
        return qlKeyId;
    }

    public void setQlKeyId(Integer qlKeyId) {
        this.qlKeyId = qlKeyId;
    }
}
