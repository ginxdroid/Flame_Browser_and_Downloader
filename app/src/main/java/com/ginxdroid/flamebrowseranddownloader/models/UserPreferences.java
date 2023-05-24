package com.ginxdroid.flamebrowseranddownloader.models;

public class UserPreferences {

    private Integer upKeyId;
    private Integer currentThemeID;
    private Integer isDarkWebUI;

    private Integer darkTheme;

    private String homePageURL;
    private String searchEngineURL;

    private Integer isSaveRecentTabs;


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
}
