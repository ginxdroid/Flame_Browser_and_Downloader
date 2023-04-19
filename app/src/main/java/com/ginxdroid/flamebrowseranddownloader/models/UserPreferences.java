package com.ginxdroid.flamebrowseranddownloader.models;

public class UserPreferences {

    private Integer upKeyId;
    private Integer currentThemeID;
    private Integer isDarkWebUI;

    private Integer darkTheme;


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
}
