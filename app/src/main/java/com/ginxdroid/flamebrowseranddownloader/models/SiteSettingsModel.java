package com.ginxdroid.flamebrowseranddownloader.models;

public class SiteSettingsModel {
    private Integer SsId;
    private Integer SsJavaScript;
    private Integer SsCookies;
    private Integer SsLocation;
    private Integer SsSaveSitesInHistory;
    private Integer SsSaveSearchHistory;
    private Integer SsIsChanged;

    public SiteSettingsModel() {
    }

    public Integer getSsId() {
        return SsId;
    }

    public void setSsId(Integer ssId) {
        SsId = ssId;
    }

    public Integer getSsJavaScript() {
        return SsJavaScript;
    }

    public void setSsJavaScript(Integer ssJavaScript) {
        SsJavaScript = ssJavaScript;
    }

    public Integer getSsCookies() {
        return SsCookies;
    }

    public void setSsCookies(Integer ssCookies) {
        SsCookies = ssCookies;
    }

    public Integer getSsLocation() {
        return SsLocation;
    }

    public void setSsLocation(Integer ssLocation) {
        SsLocation = ssLocation;
    }

    public Integer getSsSaveSitesInHistory() {
        return SsSaveSitesInHistory;
    }

    public void setSsSaveSitesInHistory(Integer ssSaveSitesInHistory) {
        SsSaveSitesInHistory = ssSaveSitesInHistory;
    }

    public Integer getSsSaveSearchHistory() {
        return SsSaveSearchHistory;
    }

    public void setSsSaveSearchHistory(Integer ssSaveSearchHistory) {
        SsSaveSearchHistory = ssSaveSearchHistory;
    }

    public Integer getSsIsChanged() {
        return SsIsChanged;
    }

    public void setSsIsChanged(Integer ssIsChanged) {
        SsIsChanged = ssIsChanged;
    }
}
