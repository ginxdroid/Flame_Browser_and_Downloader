package com.ginxdroid.flamebrowseranddownloader.models.tasks;

public class PartialBindDownloadTask {
    private Integer KeyId;
    private String FileName;
    private String TimeLeft;
    private String DownloadSpeed;
    private String PauseResumeSupported;
    private Integer CurrentStatus;

    public PartialBindDownloadTask() {
    }

    public Integer getKeyId() {
        return KeyId;
    }

    public void setKeyId(Integer keyId) {
        KeyId = keyId;
    }

    public String getFileName() {
        return FileName;
    }

    public void setFileName(String fileName) {
        FileName = fileName;
    }

    public String getTimeLeft() {
        return TimeLeft;
    }

    public void setTimeLeft(String timeLeft) {
        TimeLeft = timeLeft;
    }

    public String getDownloadSpeed() {
        return DownloadSpeed;
    }

    public void setDownloadSpeed(String downloadSpeed) {
        DownloadSpeed = downloadSpeed;
    }

    public String getPauseResumeSupported() {
        return PauseResumeSupported;
    }

    public void setPauseResumeSupported(String pauseResumeSupported) {
        PauseResumeSupported = pauseResumeSupported;
    }

    public Integer getCurrentStatus() {
        return CurrentStatus;
    }

    public void setCurrentStatus(Integer currentStatus) {
        CurrentStatus = currentStatus;
    }
}
