package com.ginxdroid.flamebrowseranddownloader.models;

public class SearchEngineItem {
    private String SEItemTitle;
    private String SEItemURL;
    private Integer SEIsDefault;
    private Integer SEItemIsCurrent;

    public SearchEngineItem() {
    }

    public String getSEItemTitle() {
        return SEItemTitle;
    }

    public void setSEItemTitle(String SEItemTitle) {
        this.SEItemTitle = SEItemTitle;
    }

    public String getSEItemURL() {
        return SEItemURL;
    }

    public void setSEItemURL(String SEItemURL) {
        this.SEItemURL = SEItemURL;
    }

    public Integer getSEIsDefault() {
        return SEIsDefault;
    }

    public void setSEIsDefault(Integer SEIsDefault) {
        this.SEIsDefault = SEIsDefault;
    }

    public Integer getSEItemIsCurrent() {
        return SEItemIsCurrent;
    }

    public void setSEItemIsCurrent(Integer SEItemIsCurrent) {
        this.SEItemIsCurrent = SEItemIsCurrent;
    }
}
