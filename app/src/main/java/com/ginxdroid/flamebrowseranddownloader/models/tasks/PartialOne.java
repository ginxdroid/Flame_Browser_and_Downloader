package com.ginxdroid.flamebrowseranddownloader.models.tasks;

public class PartialOne {
    private Integer CurrentStatus;
    private Integer CurrentProgress;
    private Integer ChunkMode;

    public PartialOne() {
    }

    public Integer getCurrentStatus() {
        return CurrentStatus;
    }

    public void setCurrentStatus(Integer currentStatus) {
        CurrentStatus = currentStatus;
    }

    public Integer getCurrentProgress() {
        return CurrentProgress;
    }

    public void setCurrentProgress(Integer currentProgress) {
        CurrentProgress = currentProgress;
    }

    public Integer getChunkMode() {
        return ChunkMode;
    }

    public void setChunkMode(Integer chunkMode) {
        ChunkMode = chunkMode;
    }
}
