package com.oculix.douyin.model;

import java.time.LocalDateTime;

/**
 * Represents a comment task with result tracking.
 */
public class CommentTask {
    private final VideoInfo video;
    private String commentText;
    private TaskStatus status;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private String errorMessage;
    private boolean generatedByLLM;

    public enum TaskStatus {
        PENDING, IN_PROGRESS, SUCCESS, FAILED, SKIPPED
    }

    public CommentTask(VideoInfo video) {
        this.video = video;
        this.status = TaskStatus.PENDING;
        this.startedAt = LocalDateTime.now();
    }

    public VideoInfo getVideo() { return video; }
    
    public String getCommentText() { return commentText; }
    public void setCommentText(String commentText) { this.commentText = commentText; }
    
    public TaskStatus getStatus() { return status; }
    public void setStatus(TaskStatus status) { this.status = status; }
    
    public LocalDateTime getStartedAt() { return startedAt; }
    
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
    
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    
    public boolean isGeneratedByLLM() { return generatedByLLM; }
    public void setGeneratedByLLM(boolean generatedByLLM) { this.generatedByLLM = generatedByLLM; }

    @Override
    public String toString() {
        return "CommentTask{video='" + video.getTitle() + "', status=" + status + "}";
    }
}
