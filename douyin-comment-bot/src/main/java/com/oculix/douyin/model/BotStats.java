package com.oculix.douyin.model;

import java.time.LocalDateTime;

/**
 * Statistics tracking for the bot's work summary.
 */
public class BotStats {
    private int totalVideosFound;
    private int totalCommentsAttempted;
    private int totalCommentsSuccess;
    private int totalCommentsFailed;
    private int totalCommentsSkipped;
    private int totalLLMGenerated;
    private int totalFallbackUsed;
    private LocalDateTime startTime;
    private LocalDateTime lastActivityTime;
    private String currentStatus = "Idle";

    public BotStats() {
        this.startTime = LocalDateTime.now();
    }

    public synchronized void recordFoundVideo() {
        totalVideosFound++;
        lastActivityTime = LocalDateTime.now();
    }

    public synchronized void recordCommentAttempt() {
        totalCommentsAttempted++;
        lastActivityTime = LocalDateTime.now();
    }

    public synchronized void recordCommentSuccess() {
        totalCommentsSuccess++;
        lastActivityTime = LocalDateTime.now();
    }

    public synchronized void recordCommentFailed() {
        totalCommentsFailed++;
        lastActivityTime = LocalDateTime.now();
    }

    public synchronized void recordCommentSkipped() {
        totalCommentsSkipped++;
        lastActivityTime = LocalDateTime.now();
    }

    public synchronized void recordLLMGenerated() {
        totalLLMGenerated++;
    }

    public synchronized void recordFallbackUsed() {
        totalFallbackUsed++;
    }

    public synchronized void setCurrentStatus(String status) {
        this.currentStatus = status;
    }

    // Getters
    public int getTotalVideosFound() { return totalVideosFound; }
    public int getTotalCommentsAttempted() { return totalCommentsAttempted; }
    public int getTotalCommentsSuccess() { return totalCommentsSuccess; }
    public int getTotalCommentsFailed() { return totalCommentsFailed; }
    public int getTotalCommentsSkipped() { return totalCommentsSkipped; }
    public int getTotalLLMGenerated() { return totalLLMGenerated; }
    public int getTotalFallbackUsed() { return totalFallbackUsed; }
    public LocalDateTime getStartTime() { return startTime; }
    public LocalDateTime getLastActivityTime() { return lastActivityTime; }
    public String getCurrentStatus() { return currentStatus; }

    public double getSuccessRate() {
        if (totalCommentsAttempted == 0) return 0;
        return (double) totalCommentsSuccess / totalCommentsAttempted * 100;
    }

    public long getRunningMinutes() {
        return java.time.Duration.between(startTime, LocalDateTime.now()).toMinutes();
    }

    public synchronized void reset() {
        totalVideosFound = 0;
        totalCommentsAttempted = 0;
        totalCommentsSuccess = 0;
        totalCommentsFailed = 0;
        totalCommentsSkipped = 0;
        totalLLMGenerated = 0;
        totalFallbackUsed = 0;
        startTime = LocalDateTime.now();
        lastActivityTime = null;
        currentStatus = "Idle";
    }
}
