package com.ginxdroid.flamebrowseranddownloader.models;

public class HistoryItem {
    private Integer HiKeyId;
    private String HiFaviconPath;
    private String HiTitle;
    private String HiURL;
    private String HiDate;
    private Integer HiType;

    public HistoryItem() {
    }

    public Integer getHiKeyId() {
        return HiKeyId;
    }

    public void setHiKeyId(Integer hiKeyId) {
        HiKeyId = hiKeyId;
    }

    public String getHiFaviconPath() {
        return HiFaviconPath;
    }

    public void setHiFaviconPath(String hiFaviconPath) {
        HiFaviconPath = hiFaviconPath;
    }

    public String getHiTitle() {
        return HiTitle;
    }

    public void setHiTitle(String hiTitle) {
        HiTitle = hiTitle;
    }

    public String getHiURL() {
        return HiURL;
    }

    public void setHiURL(String hiURL) {
        HiURL = hiURL;
    }

    public String getHiDate() {
        return HiDate;
    }

    public void setHiDate(String hiDate) {
        HiDate = hiDate;
    }

    public Integer getHiType() {
        return HiType;
    }

    public void setHiType(Integer hiType) {
        HiType = hiType;
    }
}
