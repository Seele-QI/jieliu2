package com.oculix.douyin.agent;

import com.oculix.douyin.model.Config;
import com.oculix.douyin.service.ConfigManager;
import com.oculix.douyin.util.AppLogger;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 视觉AI控制核心 —— 截图 → MiniMax-VL 看图分析 → 执行操作 → 循环
 *
 * 工作流程:
 * 1. 截取当前屏幕（压缩至1280px宽）
 * 2. base64 编码后发给 MiniMax-VL
 * 3. LLM 返回 JSON 格式的操作指令
 * 4. 解析为 VisionAction
 * 5. ActionExecutor 执行鼠标/键盘操作
 * 6. 回到步骤1
 */
public class VisionAgent {
    private static VisionAgent instance;
    private final Config config;
    private final HttpClient httpClient;
    private final ActionExecutor executor;
    private Robot robot;
    private volatile boolean running = false;
    private volatile boolean paused = false;

    // 循环控制
    private static final int MAX_CONSECUTIVE_FAILURES = 5;
    private static final int MAX_STEPS = 300;
    private static final int SCREENSHOT_WIDTH = 1280;

    // 评论相关状态
    private int commentsSentInSession = 0;
    private int videosProcessedInSession = 0;

    public interface AgentCallback {
        void onLog(String msg);
        void onAction(VisionAction action);
        void onStatus(String status);
        void onComplete(String summary);
    }
    private AgentCallback callback;

    private VisionAgent() {
        this.config = ConfigManager.getInstance().getConfig();
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();
        this.executor = ActionExecutor.getInstance();
        try {
            this.robot = new Robot();
        } catch (AWTException e) {
            AppLogger.error("Robot init failed: " + e.getMessage());
        }
    }

    public static synchronized VisionAgent getInstance() {
        if (instance == null) instance = new VisionAgent();
        return instance;
    }

    public void setCallback(AgentCallback cb) { this.callback = cb; }
    public void setPaused(boolean p) { this.paused = p; executor.setPaused(p); }
    public boolean isPaused() { return paused; }
    public boolean isRunning() { return running; }

    // ======== 对外接口 ========

    public void start(String goal) {
        if (running) {
            AppLogger.warn("Agent already running");
            return;
        }

        running = true;
        paused = false;
        executor.setPaused(false);
        commentsSentInSession = 0;
        videosProcessedInSession = 0;
        notifyStatus("启动中");
        AppLogger.info("=== 视觉AI自动化启动 ===");
        AppLogger.info("目标: " + goal);

        // 给用户3秒时间切换到 Chrome 窗口
        AppLogger.info("请确保 Chrome（抖音已登录）窗口在前台可见");
        AppLogger.info("3秒后开始工作...");
        sleep(3000);

        new Thread(() -> {
            try {
                runLoop(goal);
            } catch (Exception e) {
                AppLogger.error("Agent error: " + e.getMessage());
                e.printStackTrace();
            } finally {
                running = false;
                notifyStatus("已停止");
                String summary = String.format(
                    "本次运行总结:\n  - 已处理视频: %d\n  - 已发送评论: %d",
                    videosProcessedInSession, commentsSentInSession);
                AppLogger.info("=== 视觉AI自动化结束 ===");
                AppLogger.info(summary);
                if (callback != null) callback.onComplete(summary);
            }
        }, "vision-agent").start();
    }

    public void stop() {
        running = false;
        paused = false;
        executor.setPaused(false);
    }

    /**
     * 主循环: 截图 → LLM → 执行 → 重复
     */
    private void runLoop(String goal) {
        int stepCount = 0;
        int consecutiveFailures = 0;

        while (running && stepCount < MAX_STEPS) {
            if (paused) { sleep(500); consecutiveFailures = 0; continue; }

            stepCount++;
            AppLogger.info("--- 步骤 " + stepCount + " ---");

            // 1. 截屏
            notifyStatus("正在截屏...");
            String base64Image = captureScreenResized();
            if (base64Image == null) {
                AppLogger.error("截屏失败");
                sleep(2000);
                consecutiveFailures++;
                if (consecutiveFailures >= MAX_CONSECUTIVE_FAILURES) {
                    AppLogger.error("连续失败" + MAX_CONSECUTIVE_FAILURES + "次，停止运行");
                    break;
                }
                continue;
            }

            // 2. 调 MiniMax-VL 看图
            notifyStatus("LLM 分析屏幕...");
            String llmResponse = askMiniMax(goal, base64Image, stepCount);
            if (llmResponse == null) {
                AppLogger.warn("LLM 未返回有效指令，等待后重试...");
                sleep(3000);
                consecutiveFailures++;
                if (consecutiveFailures >= MAX_CONSECUTIVE_FAILURES) {
                    AppLogger.error("LLM 连续失败" + MAX_CONSECUTIVE_FAILURES + "次，停止运行");
                    break;
                }
                continue;
            }
            consecutiveFailures = 0;

            // 3. 解析指令
            VisionAction action = parseAction(llmResponse);
            if (action == null) {
                AppLogger.warn("无法解析 LLM 返回的指令");
                AppLogger.info("LLM 回复: " + llmResponse.substring(0, Math.min(300, llmResponse.length())));
                sleep(2000);
                continue;
            }

            // 4. 通知 UI
            if (callback != null) callback.onAction(action);
            AppLogger.info("LLM 指令: " + action.type + " | " + (action.reasoning != null ? action.reasoning : ""));

            // 5. 如果是 done 则结束
            if (action.isDone()) {
                AppLogger.success("任务完成: " + action.reasoning);
                break;
            }

            // 6. 执行操作
            boolean success = executor.execute(action);

            // 7. 记录统计
            if (action.type.equals("type") && action.text != null && !action.text.isEmpty()) {
                // 发了评论
                if (success) {
                    commentsSentInSession++;
                    AppLogger.success("评论已发送 (#" + commentsSentInSession + ")");
                }
            }

            // 8. 操作间随机延迟（防检测）
            int delayMs = 1500 + new java.util.Random().nextInt(3000);
            sleep(delayMs);
        }

        if (stepCount >= MAX_STEPS) {
            AppLogger.info("达到最大步骤数 " + MAX_STEPS + "，自动停止");
        }
    }

    // ======== 截屏 ========

    /**
     * 截屏并压缩至 SCREENSHOT_WIDTH 宽度，返回 JPEG base64
     */
    private String captureScreenResized() {
        try {
            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            BufferedImage fullScreen = robot.createScreenCapture(
                new Rectangle(0, 0, screenSize.width, screenSize.height));

            // 压缩宽度至 1280px，保持宽高比
            int targetW = SCREENSHOT_WIDTH;
            int targetH = (int) ((double) screenSize.height / screenSize.width * targetW);

            BufferedImage resized = new BufferedImage(targetW, targetH, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = resized.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(fullScreen, 0, 0, targetW, targetH, null);
            g.dispose();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(resized, "jpg", baos);
            byte[] bytes = baos.toByteArray();
            AppLogger.info("截屏完成: " + targetW + "x" + targetH + " (" + (bytes.length / 1024) + "KB)");
            return Base64.getEncoder().encodeToString(bytes);
        } catch (Exception e) {
            AppLogger.error("截屏失败: " + e.getMessage());
            return null;
        }
    }

    // ======== MiniMax-VL API 调用 ========

    /**
     * 调用 MiniMax-VL 看图模型
     * MiniMax-VL 使用 OpenAI 兼容格式的 content array，支持 image_url
     */
    private String askMiniMax(String goal, String base64Image, int step) {
        var llm = config.getComment().getLlm();
        if (llm == null || llm.getApiKey() == null || llm.getApiKey().isEmpty()) {
            AppLogger.error("未配置 API Key，请在设置中填写");
            return null;
        }

        // MiniMax-VL 看图接口
        String apiUrl = llm.getApiUrl();
        if (apiUrl == null || apiUrl.isEmpty()) {
            apiUrl = "https://api.minimax.chat/v1/text/chatcompletion_v2";
        }
        String model = llm.getModel();
        if (model == null || model.isEmpty()) {
            model = "MiniMax-VL";
        }
        double temperature = 0.1;
        int maxTokens = 500;

        try {
            String requestBody = buildRequestBody(goal, base64Image, model, temperature, step, maxTokens);
            AppLogger.info("调用 MiniMax-VL (模型: " + model + ") ...");

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + llm.getApiKey())
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .timeout(Duration.ofSeconds(60))
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                String content = extractContentFromMinimax(response.body());
                return content;
            } else {
                String errorBody = response.body();
                AppLogger.warn("MiniMax API 返回 " + response.statusCode()
                    + ": " + (errorBody.length() > 200 ? errorBody.substring(0, 200) : errorBody));
                return null;
            }
        } catch (Exception e) {
            AppLogger.error("MiniMax API 调用异常: " + e.getMessage());
            return null;
        }
    }

    /**
     * 构建 MiniMax-VL 请求体
     * MiniMax 的 chatcompletion_v2 接口使用 OpenAI 兼容格式
     */
    private String buildRequestBody(String goal, String base64Image, String model,
                                     double temp, int step, int maxTokens) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"model\": \"").append(escapeJson(model)).append("\",\n");
        sb.append("  \"messages\": [\n");

        // System prompt - 明确告诉 LLM 它的角色和输出格式
        String systemPrompt = "你是OculiX抖音自动评论机器人的视觉控制AI。"
            + "你正在看电脑屏幕的截图，屏幕上运行着抖音网页版。"
            + "你的任务是根据用户的截流目标，决定鼠标要点击哪里、输入什么内容。"
            + "\n\n请分析当前截图，输出下一步操作指令（JSON格式，不要markdown包裹）："
            + "\n{\"type\":\"操作类型\", \"x\":横坐标, \"y\":纵坐标, \"text\":\"要输入的文本\", \"amount\":滚动量, \"delayMs\":等待毫秒, \"reasoning\":\"为什么做这个操作\"}"
            + "\n\n可用的操作类型："
            + "\n- click: 在 (x,y) 位置点击鼠标左键"
            + "\n- type: 在当前位置输入 text 内容（用于输入评论）"
            + "\n- scroll: 滚动 amount 像素（正数向下，负数向上）"
            + "\n- wait: 等待 delayMs 毫秒"
            + "\n- done: 任务完成，结束自动化"
            + "\n\n重要规则："
            + "\n1. 坐标(x,y)是屏幕上实际的像素坐标"
            + "\n2. 截图已压缩至1280px宽，但坐标要用实际屏幕坐标（你的截图比例是缩放的，但坐标请直接按实际屏幕像素）"
            + "\n3. 只能返回纯JSON，不要Markdown代码块"
            + "\n4. 务必返回type字段"
            + "\n5. 如果当前页面上没有找到目标，就返回 wait 让系统等待或滚动";

        sb.append("    {\"role\": \"system\", \"content\": \"");
        sb.append(escapeJson(systemPrompt));
        sb.append("\"},\n");

        // User message - 当前目标和截图
        String userPrompt = "当前任务目标：" + goal
            + "\n\n这是第 " + step + " 步操作。请分析截图，输出下一步要做的操作。";

        sb.append("    {\"role\": \"user\", \"content\": [\n");
        sb.append("      {\"type\": \"text\", \"text\": \"");
        sb.append(escapeJson(userPrompt));
        sb.append("\"},\n");
        sb.append("      {\"type\": \"image_url\", \"image_url\": {\"url\": \"data:image/jpeg;base64,");
        sb.append(base64Image);
        sb.append("\"}}\n");
        sb.append("    ]}\n");
        sb.append("  ],\n");
        sb.append("  \"temperature\": ").append(temp).append(",\n");
        sb.append("  \"max_tokens\": ").append(maxTokens).append(",\n");
        sb.append("  \"top_p\": 0.9\n");
        sb.append("}\n");

        return sb.toString();
    }

    /**
     * 从 MiniMax-VL 响应中提取 content 文本
     * MiniMax 响应格式类似 OpenAI: {"choices":[{"message":{"content":"..."}}]}
     */
    private String extractContentFromMinimax(String responseBody) {
        try {
            // 先尝试找 "content": "..." 的模式
            int choicesIdx = responseBody.indexOf("\"choices\"");
            if (choicesIdx < 0) {
                // 尝试直接找 content
                return extractSimpleContent(responseBody);
            }
            int msgIdx = responseBody.indexOf("\"message\"", choicesIdx);
            if (msgIdx < 0) return extractSimpleContent(responseBody);
            int contentIdx = responseBody.indexOf("\"content\"", msgIdx);
            if (contentIdx < 0) return null;

            // 找到 content 字段的值
            int colonIdx = responseBody.indexOf(':', contentIdx + 9);
            if (colonIdx < 0) return null;

            // 跳过空白
            int start = colonIdx + 1;
            while (start < responseBody.length() && responseBody.charAt(start) == ' ') start++;
            if (start >= responseBody.length()) return null;

            // 如果是字符串（以引号开头）
            if (responseBody.charAt(start) == '"') {
                start++; // 跳过开头的引号
                StringBuilder content = new StringBuilder();
                for (int i = start; i < responseBody.length(); i++) {
                    char c = responseBody.charAt(i);
                    if (c == '\\') {
                        i++;
                        if (i < responseBody.length()) {
                            char next = responseBody.charAt(i);
                            switch (next) {
                                case 'n': content.append('\n'); break;
                                case 'r': content.append('\r'); break;
                                case 't': content.append('\t'); break;
                                case '"': content.append('"'); break;
                                case '\\': content.append('\\'); break;
                                default: content.append(next); break;
                            }
                        }
                    } else if (c == '"') {
                        break;
                    } else {
                        content.append(c);
                    }
                }
                String result = content.toString().trim();
                if (result.isEmpty()) return null;
                return result;
            }

            return null;
        } catch (Exception e) {
            AppLogger.warn("解析 MiniMax 响应失败: " + e.getMessage());
            return null;
        }
    }

    /**
     * 简易 content 提取（备用）
     */
    private String extractSimpleContent(String body) {
        int idx = body.indexOf("\"content\"");
        if (idx < 0) return null;
        idx = body.indexOf(':', idx + 9);
        if (idx < 0) return null;
        int start = body.indexOf('"', idx + 1);
        if (start < 0) return null;
        start++;
        int end = body.indexOf('"', start);
        if (end < 0) return null;
        String raw = body.substring(start, end);
        // 转义处理
        raw = raw.replace("\\n", "\n").replace("\\\"", "\"").replace("\\\\", "\\");
        return raw.trim();
    }

    // ======== 解析 LLM 输出 ========

    /**
     * 将 LLM 返回的 JSON 文本解析为 VisionAction
     * 支持纯 JSON 或 markdown 包裹的 JSON
     */
    private VisionAction parseAction(String llmText) {
        if (llmText == null || llmText.isEmpty()) return null;

        try {
            // 提取 JSON（去掉可能的 markdown 包裹）
            String json = extractJsonBlock(llmText);
            if (json == null) return null;

            // 解析字段
            String type = extractJsonField(json, "type");
            if (type == null || type.isEmpty()) return null;

            VisionAction action = new VisionAction();
            action.type = type.trim().toLowerCase();
            action.reasoning = extractJsonField(json, "reasoning");

            String xs = extractJsonField(json, "x");
            String ys = extractJsonField(json, "y");
            if (xs != null) {
                try { action.x = Integer.parseInt(xs.trim()); } catch (NumberFormatException e) { }
            }
            if (ys != null) {
                try { action.y = Integer.parseInt(ys.trim()); } catch (NumberFormatException e) { }
            }

            String text = extractJsonField(json, "text");
            if (text != null && !text.isEmpty()) {
                // 处理 Unicode 转义
                action.text = unescapeUnicode(text);
            }

            String amount = extractJsonField(json, "amount");
            if (amount != null) {
                try { action.amount = Integer.parseInt(amount.trim()); } catch (NumberFormatException e) { }
            }

            String delay = extractJsonField(json, "delayMs");
            if (delay != null) {
                try { action.delayMs = Integer.parseInt(delay.trim()); } catch (NumberFormatException e) { }
            }

            return action;
        } catch (Exception e) {
            AppLogger.warn("解析 LLM 指令失败: " + e.getMessage());
            return null;
        }
    }

    /**
     * 从文本中提取 JSON 块（去掉 markdown ```json ... ``` 包裹）
     */
    private String extractJsonBlock(String text) {
        // 尝试匹配 ```json ... ```
        Pattern mdPattern = Pattern.compile("```(?:json)?\\s*\\n?([\\s\\S]*?)```", Pattern.CASE_INSENSITIVE);
        Matcher mdMatcher = mdPattern.matcher(text);
        if (mdMatcher.find()) {
            String inner = mdMatcher.group(1).trim();
            if (inner.startsWith("{") && inner.endsWith("}")) {
                return inner;
            }
        }

        // 直接找 { ... }
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return text.substring(start, end + 1);
        }
        return null;
    }

    /**
     * 从 JSON 中提取命名字段的值（支持字符串和数字）
     */
    private String extractJsonField(String json, String field) {
        // 匹配字符串值: "field": "value"
        String strPattern = "\"" + field + "\"\\s*:\\s*\"";
        Pattern p = Pattern.compile(strPattern + "([^\"]*?)\"");
        Matcher m = p.matcher(json);
        if (m.find()) return m.group(1).trim();

        // 匹配数字值: "field": 123 或 "field": -123
        String numPattern = "\"" + field + "\"\\s*:\\s*(-?\\d+)";
        Pattern pn = Pattern.compile(numPattern);
        Matcher mn = pn.matcher(json);
        if (mn.find()) return mn.group(1).trim();

        return null;
    }

    /**
     * 处理 Unicode 转义字符 (例如 u0041 代表 A)
     */
    private String unescapeUnicode(String text) {
        if (text == null || !text.contains("\\u")) return text;
        StringBuilder sb = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\\' && i + 5 < text.length() && text.charAt(i + 1) == 'u') {
                try {
                    String hex = text.substring(i + 2, i + 6);
                    int code = Integer.parseInt(hex, 16);
                    sb.append((char) code);
                    i += 5;
                } catch (NumberFormatException e) {
                    sb.append(c);
                }
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * 转义字符串用于嵌入 JSON（只转义必要的字符）
     */
    private String escapeJson(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder(s.length());
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default: sb.append(c);
            }
        }
        return sb.toString();
    }

    private void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    private void notifyStatus(String s) {
        if (callback != null) callback.onStatus(s);
    }
}
