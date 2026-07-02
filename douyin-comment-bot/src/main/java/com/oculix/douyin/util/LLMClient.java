package com.oculix.douyin.util;

import com.oculix.douyin.model.Config;
import com.oculix.douyin.service.ConfigManager;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Random;

/**
 * Client for MiniMax LLM API to generate dynamic comments.
 */
public class LLMClient {
    private static LLMClient instance;
    private final Config config;
    private final HttpClient httpClient;
    private final Random random;

    private LLMClient() {
        this.config = ConfigManager.getInstance().getConfig();
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();
        this.random = new Random();
    }

    public static synchronized LLMClient getInstance() {
        if (instance == null) {
            instance = new LLMClient();
        }
        return instance;
    }

    /**
     * Generate a comment based on the video context.
     */
    public String generateComment(String videoTitle, String videoDescription, String authorName) {
        var llmConfig = config.getComment().getLlm();
        if (llmConfig == null || !llmConfig.isEnabled()) {
            return getFallbackComment();
        }

        try {
            String prompt = buildPrompt(videoTitle, videoDescription, authorName);
            String response = callLLM(prompt);
            
            if (response != null && !response.isBlank()) {
                return sanitizeComment(response);
            }
        } catch (Exception e) {
            AppLogger.warn("LLM comment generation failed: " + e.getMessage());
        }

        return getFallbackComment();
    }

    private String buildPrompt(String videoTitle, String videoDescription, String authorName) {
        String framework = config.getComment().getFramework();
        
        return String.format("""
            你是一个热心的抖音用户，正在观看一个短视频。
            请根据以下视频信息，写一条简短自然的评论（10-30字）。
            
            视频标题：%s
            视频描述：%s
            作者：%s
            
            要求：
            - 评论要自然、口语化，像一个真实用户的留言
            - 不要过于营销或广告化
            - 语气诚恳友好，可以适当夸赞
            - 不要复制粘贴指令或提示
            - 只输出评论内容，不要额外文字
            
            参考句式框架：%s
            """,
            videoTitle != null ? videoTitle : "",
            videoDescription != null ? videoDescription : "",
            authorName != null ? authorName : "",
            framework != null ? framework : "觉得不错！"
        );
    }

    private String callLLM(String prompt) throws Exception {
        var llmConfig = config.getComment().getLlm();
        String apiUrl = llmConfig.getApiUrl();
        String apiKey = llmConfig.getApiKey();
        String model = llmConfig.getModel();
        double temperature = llmConfig.getTemperature();
        
        if (apiKey == null || apiKey.isEmpty()) {
            AppLogger.warn("LLM API key not configured, using fallback comments");
            return null;
        }

        String requestBody = String.format("""
            {
                "model": "%s",
                "messages": [{"role": "user", "content": %s}],
                "temperature": %.1f,
                "max_tokens": %d
            }
            """,
            model,
            JSON.serializeString(prompt),
            temperature,
            llmConfig.getMaxTokens()
        );

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(apiUrl))
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer " + apiKey)
            .POST(HttpRequest.BodyPublishers.ofString(requestBody))
            .timeout(Duration.ofSeconds(30))
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() == 200) {
            return extractCommentFromResponse(response.body());
        } else {
            AppLogger.warn("LLM API returned status: " + response.statusCode());
            return null;
        }
    }

    private String extractCommentFromResponse(String responseBody) {
        // Parse MiniMax response format
        try {
            // Simple JSON parsing - extract content
            if (responseBody.contains("\"content\"")) {
                int start = responseBody.indexOf("\"content\"") + 10;
                start = responseBody.indexOf("\"", start) + 1;
                int end = responseBody.indexOf("\"", start);
                return responseBody.substring(start, end);
            }
        } catch (Exception e) {
            AppLogger.warn("Failed to parse LLM response: " + e.getMessage());
        }
        return null;
    }

    private String getFallbackComment() {
        List<String> variants = config.getComment().getVariants();
        if (variants != null && !variants.isEmpty()) {
            return variants.get(random.nextInt(variants.size()));
        }
        return "内容不错，支持一下！";
    }

    private String sanitizeComment(String comment) {
        // Remove quotes, trim, limit length
        return comment.replaceAll("[\"'\"']", "")
            .trim()
            .substring(0, Math.min(comment.length(), 200));
    }

    /**
     * Simple JSON utility for building request bodies.
     */
    private static class JSON {
        static String serializeString(String s) {
            return "\"" + s.replace("\\", "\\\\\\\\")
                .replace("\"", "\\\\\\\"")
                .replace("\n", "\\\\n")
                .replace("\r", "\\\\r")
                .replace("\t", "\\\\t") + "\"";
        }
    }
}
