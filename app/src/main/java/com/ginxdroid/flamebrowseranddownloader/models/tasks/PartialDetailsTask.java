package com.ginxdroid.flamebrowseranddownloader.models.tasks;

public class PartialDetailsTask {
    private String FileName;
    private String Url;
    private String PageURL;
    private String DirPath;
    private String PauseResumeSupported;
    private Integer ChunkMode;
    private Long TotalBytes;
    private Long DownloadedBytes;
    private Integer CurrentStatus;
    private Integer segmentsForDownloadTask;

    public PartialDetailsTask() {
    }

    public String getFileName() {
        return FileName;
    }

    public void setFileName(String fileName) {
        FileName = fileName;
    }

    public String getUrl() {
        return Url;
    }

    public void setUrl(String url) {
        Url = url;
    }

    public String getPageURL() {
        return PageURL;
    }

    public void setPageURL(String pageURL) {
        PageURL = pageURL;
    }

    public String getDirPath() {
        return DirPath;
    }

    public void setDirPath(String dirPath) {
        DirPath = dirPath;
    }

    public String getPauseResumeSupported() {
        return PauseResumeSupported;
    }

    public void setPauseResumeSupported(String pauseResumeSupported) {
        PauseResumeSupported = pauseResumeSupported;
    }

    public Integer getChunkMode() {
        return ChunkMode;
    }

    public void setChunkMode(Integer chunkMode) {
        ChunkMode = chunkMode;
    }

    public Long getTotalBytes() {
        return TotalBytes;
    }

    public void setTotalBytes(Long totalBytes) {
        TotalBytes = totalBytes;
    }

    public Long getDownloadedBytes() {
        return DownloadedBytes;
    }

    public void setDownloadedBytes(Long downloadedBytes) {
        DownloadedBytes = downloadedBytes;
    }

    public Integer getCurrentStatus() {
        return CurrentStatus;
    }

    public void setCurrentStatus(Integer currentStatus) {
        CurrentStatus = currentStatus;
    }

    public Integer getSegmentsForDownloadTask() {
        return segmentsForDownloadTask;
    }

    public void setSegmentsForDownloadTask(Integer segmentsForDownloadTask) {
        this.segmentsForDownloadTask = segmentsForDownloadTask;
    }
}
