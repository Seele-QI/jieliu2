package com.oculix.douyin.model;

import java.util.HashSet;
import java.util.Set;

/**
 * Tracks bot progress for resume-after-restart support.
 */
public class ResumeState {
    private Set<String> processedUrls = new HashSet<>();
    private int totalProcessed;
    private int successCount;

    public Set<String> getProcessedUrls() { return processedUrls; }
    public void setProcessedUrls(Set<String> processedUrls) { this.processedUrls = processedUrls; }

    public int getTotalProcessed() { return totalProcessed; }
    public void setTotalProcessed(int totalProcessed) { this.totalProcessed = totalProcessed; }

    public int getSuccessCount() { return successCount; }
    public void setSuccessCount(int successCount) { this.successCount = successCount; }

    public void markProcessed(String url) {
        processedUrls.add(url);
        totalProcessed++;
    }

    public boolean isProcessed(String url) {
        return processedUrls.contains(url);
    }

    public void clear() {
        processedUrls.clear();
        totalProcessed = 0;
        successCount = 0;
    }
}
