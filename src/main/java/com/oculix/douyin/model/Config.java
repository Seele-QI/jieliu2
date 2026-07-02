package com.oculix.douyin.model;

import java.util.List;

public class Config {
    private MonitorConfig monitor;
    private CommentConfig comment;
    private AntiDetectionConfig antiDetection;
    private AuditConfig audit;

    public MonitorConfig getMonitor() { return monitor; }
    public void setMonitor(MonitorConfig monitor) { this.monitor = monitor; }
    public CommentConfig getComment() { return comment; }
    public void setComment(CommentConfig comment) { this.comment = comment; }
    public AntiDetectionConfig getAntiDetection() { return antiDetection; }
    public void setAntiDetection(AntiDetectionConfig antiDetection) { this.antiDetection = antiDetection; }
    public AuditConfig getAudit() { return audit; }
    public void setAudit(AuditConfig audit) { this.audit = audit; }

    public static class MonitorConfig {
        private boolean enabled = true;
        private List<String> keywords;
        private List<String> topics;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public List<String> getKeywords() { return keywords; }
        public void setKeywords(List<String> keywords) { this.keywords = keywords; }
        public List<String> getTopics() { return topics; }
        public void setTopics(List<String> topics) { this.topics = topics; }
    }

    public static class CommentConfig {
        private String framework;
        private List<String> variants;
        private LLMConfig llm;

        public String getFramework() { return framework; }
        public void setFramework(String framework) { this.framework = framework; }
        public List<String> getVariants() { return variants; }
        public void setVariants(List<String> variants) { this.variants = variants; }
        public LLMConfig getLlm() { return llm; }
        public void setLlm(LLMConfig llm) { this.llm = llm; }

        public static class LLMConfig {
            private boolean enabled = true;
            private String apiUrl;
            private String apiKey;
            private String model;
            private double temperature = 0.1;
            private int maxTokens = 300;

            public boolean isEnabled() { return enabled; }
            public void setEnabled(boolean enabled) { this.enabled = enabled; }
            public String getApiUrl() { return apiUrl; }
            public void setApiUrl(String apiUrl) { this.apiUrl = apiUrl; }
            public String getApiKey() { return apiKey; }
            public void setApiKey(String apiKey) { this.apiKey = apiKey; }
            public String getModel() { return model; }
            public void setModel(String model) { this.model = model; }
            public double getTemperature() { return temperature; }
            public void setTemperature(double temperature) { this.temperature = temperature; }
            public int getMaxTokens() { return maxTokens; }
            public void setMaxTokens(int maxTokens) { this.maxTokens = maxTokens; }
        }
    }

    public static class AntiDetectionConfig {
        private int minDelaySeconds = 3;
        private int maxDelaySeconds = 8;
        private MouseSpeed mouseSpeed;

        public int getMinDelaySeconds() { return minDelaySeconds; }
        public void setMinDelaySeconds(int minDelaySeconds) { this.minDelaySeconds = minDelaySeconds; }
        public int getMaxDelaySeconds() { return maxDelaySeconds; }
        public void setMaxDelaySeconds(int maxDelaySeconds) { this.maxDelaySeconds = maxDelaySeconds; }
        public MouseSpeed getMouseSpeed() { return mouseSpeed; }
        public void setMouseSpeed(MouseSpeed mouseSpeed) { this.mouseSpeed = mouseSpeed; }

        public static class MouseSpeed {
            private int min = 80;
            private int max = 150;
            public int getMin() { return min; }
            public void setMin(int min) { this.min = min; }
            public int getMax() { return max; }
            public void setMax(int max) { this.max = max; }
        }
    }

    public static class AuditConfig {
        private String logPath = "./logs/audit.jsonl";
        private int maxFileSizeMB = 10;
        public String getLogPath() { return logPath; }
        public void setLogPath(String logPath) { this.logPath = logPath; }
        public int getMaxFileSizeMB() { return maxFileSizeMB; }
        public void setMaxFileSizeMB(int maxFileSizeMB) { this.maxFileSizeMB = maxFileSizeMB; }
    }
}
