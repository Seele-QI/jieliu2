package com.oculix.douyin.model;

import java.time.LocalDateTime;

/**
 * Represents a video found during monitoring.
 */
public class VideoInfo {
    private String url;
    private String title;
    private String description;
    private String authorName;
    private String authorId;
    private LocalDateTime foundAt;
    private boolean hasCommented;

    public VideoInfo() {
        this.foundAt = LocalDateTime.now();
    }

    public VideoInfo(String url, String title, String authorName) {
        this();
        this.url = url;
        this.title = title;
        this.authorName = authorName;
    }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getAuthorName() { return authorName; }
    public void setAuthorName(String authorName) { this.authorName = authorName; }

    public String getAuthorId() { return authorId; }
    public void setAuthorId(String authorId) { this.authorId = authorId; }

    public LocalDateTime getFoundAt() { return foundAt; }
    public void setFoundAt(LocalDateTime foundAt) { this.foundAt = foundAt; }

    public boolean isHasCommented() { return hasCommented; }
    public void setHasCommented(boolean hasCommented) { this.hasCommented = hasCommented; }

    @Override
    public String toString() {
        return "VideoInfo{title='" + title + "', author='" + authorName + "', url='" + url + "'}";
    }
}
