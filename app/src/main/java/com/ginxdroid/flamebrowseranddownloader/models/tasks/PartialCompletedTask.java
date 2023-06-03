package com.ginxdroid.flamebrowseranddownloader.models.tasks;

public class PartialCompletedTask {
    private String FileName;
    private String TimeLeft;
    private String DownloadSpeed;

    public PartialCompletedTask() {
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
}
