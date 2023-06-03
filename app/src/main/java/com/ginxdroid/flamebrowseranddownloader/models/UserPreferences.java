package com.ginxdroid.flamebrowseranddownloader.models;

public class UserPreferences {

    private Integer upKeyId;
    private Integer currentThemeID;
    private Integer isDarkWebUI;

    private Integer darkTheme;

    private String homePageURL;
    private String searchEngineURL;

    private Integer isSaveRecentTabs;
    private Integer browserTutorialInfo;

    private String downloadPath;
    private Integer autoResumeStatus;
    private Integer simultaneousTasks;
    private Integer defaultSegments;
    private Integer directDownload;
    private Integer showOptimization;


    public UserPreferences() {
    }

    public Integer getUpKeyId() {
        return upKeyId;
    }

    public void setUpKeyId(Integer upKeyId) {
        this.upKeyId = upKeyId;
    }

    public Integer getCurrentThemeID() {
        return currentThemeID;
    }

    public void setCurrentThemeID(Integer currentThemeID) {
        this.currentThemeID = currentThemeID;
    }

    public Integer getIsDarkWebUI() {
        return isDarkWebUI;
    }

    public void setIsDarkWebUI(Integer isDarkWebUI) {
        this.isDarkWebUI = isDarkWebUI;
    }

    public Integer getDarkTheme() {
        return darkTheme;
    }

    public void setDarkTheme(Integer darkTheme) {
        this.darkTheme = darkTheme;
    }

    public String getHomePageURL() {
        return homePageURL;
    }

    public void setHomePageURL(String homePageURL) {
        this.homePageURL = homePageURL;
    }

    public String getSearchEngineURL() {
        return searchEngineURL;
    }

    public void setSearchEngineURL(String searchEngineURL) {
        this.searchEngineURL = searchEngineURL;
    }


    public Integer getIsSaveRecentTabs() {
        return isSaveRecentTabs;
    }

    public void setIsSaveRecentTabs(Integer isSaveRecentTabs) {
        this.isSaveRecentTabs = isSaveRecentTabs;
    }

    public Integer getBrowserTutorialInfo() {
        return browserTutorialInfo;
    }

    public void setBrowserTutorialInfo(Integer browserTutorialInfo) {
        this.browserTutorialInfo = browserTutorialInfo;
    }

    public String getDownloadPath() {
        return downloadPath;
    }

    public void setDownloadPath(String downloadPath) {
        this.downloadPath = downloadPath;
    }

    public Integer getAutoResumeStatus() {
        return autoResumeStatus;
    }

    public void setAutoResumeStatus(Integer autoResumeStatus) {
        this.autoResumeStatus = autoResumeStatus;
    }

    public Integer getSimultaneousTasks() {
        return simultaneousTasks;
    }

    public void setSimultaneousTasks(Integer simultaneousTasks) {
        this.simultaneousTasks = simultaneousTasks;
    }

    public Integer getDefaultSegments() {
        return defaultSegments;
    }

    public void setDefaultSegments(Integer defaultSegments) {
        this.defaultSegments = defaultSegments;
    }

    public Integer getDirectDownload() {
        return directDownload;
    }

    public void setDirectDownload(Integer directDownload) {
        this.directDownload = directDownload;
    }

    public Integer getShowOptimization() {
        return showOptimization;
    }

    public void setShowOptimization(Integer showOptimization) {
        this.showOptimization = showOptimization;
    }
}
