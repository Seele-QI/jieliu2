package com.oculix.douyin.gui;

import com.oculix.douyin.agent.VisionAction;
import com.oculix.douyin.agent.VisionAgent;
import com.oculix.douyin.model.BotStats;
import com.oculix.douyin.model.Config;
import com.oculix.douyin.service.ConfigManager;
import com.oculix.douyin.util.AppLogger;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 主界面控制器
 * 负责 UI 交互、配置管理、启动/暂停/停止视觉AI
 */
public class MainController {
    @FXML private Button btnStart;
    @FXML private Button btnPause;
    @FXML private Button btnStop;
    @FXML private Button btnSettings;
    @FXML private Button btnSaveAll;
    @FXML private Button btnApplyKeywords;

    // Keywords tab
    @FXML private TextArea keywordsArea;

    // Content template tab
    @FXML private ComboBox<String> strategyCombo;
    @FXML private VBox llmTemplatePanel;
    @FXML private TextArea llmPromptArea;
    @FXML private VBox fixedCommentPanel;
    @FXML private TextArea fixedCommentArea;

    // Status
    @FXML private Label statusLabel;
    @FXML private Label videosFoundLabel;
    @FXML private Label commentsSuccessLabel;
    @FXML private Label commentsFailedLabel;
    @FXML private Label commentsSkippedLabel;
    @FXML private Label successRateLabel;
    @FXML private Label runningTimeLabel;
    @FXML private Label llmGeneratedLabel;
    @FXML private TextArea logArea;

    private static MainController instance;
    private Stage primaryStage;
    private final BotStats stats = new BotStats();
    private long startTime = 0;

    private static final String STRATEGY_AI = "AI 生成（推荐）";
    private static final String STRATEGY_FIXED = "固定文案库";
    private static final String STRATEGY_MIX = "AI + 固定混合";

    public MainController() { instance = this; }
    public static MainController getInstance() { return instance; }

    @FXML
    public void initialize() {
        // 填充策略选择器
        strategyCombo.getItems().addAll(STRATEGY_AI, STRATEGY_FIXED, STRATEGY_MIX);

        btnStart.setOnAction(e -> handleStart());
        btnPause.setOnAction(e -> handlePause());
        btnStop.setOnAction(e -> handleStop());
        btnSettings.setOnAction(e -> handleSettings());
        btnSaveAll.setOnAction(e -> handleSaveAll());
        btnApplyKeywords.setOnAction(e -> handleApplyKeywords());

        // Strategy combo listener
        strategyCombo.getSelectionModel().selectedItemProperty().addListener((obs, old, val) -> {
            boolean isAi = STRATEGY_AI.equals(val) || STRATEGY_MIX.equals(val);
            boolean isFixed = STRATEGY_FIXED.equals(val) || STRATEGY_MIX.equals(val);
            llmTemplatePanel.setVisible(isAi);
            llmTemplatePanel.setManaged(isAi);
            fixedCommentPanel.setVisible(isFixed);
            fixedCommentPanel.setManaged(isFixed);
        });

        AppLogger.setUICallback(this::appendLog);
        loadConfigIntoUI();

        String configPath = ConfigManager.getInstance().getConfigPath();
        appendLog("配置文件: " + configPath);
        appendLog("就绪。请设置截流关键词和内容模板后点击开始运行。");
        appendLog("提示：请先在 Chrome 中登录抖音并保持窗口可见");
    }

    public void setStage(Stage stage) { this.primaryStage = stage; }

    // ======== 加载配置 ========

    private void loadConfigIntoUI() {
        Config cfg = ConfigManager.getInstance().getConfig();
        if (cfg.getMonitor() != null && cfg.getMonitor().getKeywords() != null) {
            keywordsArea.setText(String.join("\n", cfg.getMonitor().getKeywords()));
        }
        String strategy = STRATEGY_AI; // default
        if (cfg.getComment() != null) {
            if (cfg.getComment().getLlm() != null && cfg.getComment().getLlm().isEnabled()) {
                strategy = cfg.getComment().getVariants() != null && !cfg.getComment().getVariants().isEmpty()
                    ? STRATEGY_MIX : STRATEGY_AI;
            } else {
                strategy = STRATEGY_FIXED;
            }
            llmPromptArea.setText(cfg.getComment().getFramework() != null ? cfg.getComment().getFramework()
                : "你是用户，正在看一个短视频。请写一条自然的口语评论，表达认可和共鸣，语气诚恳。");
            fixedCommentArea.setText(cfg.getComment().getVariants() != null
                ? String.join("\n", cfg.getComment().getVariants())
                : "说得太对了！\n学到了，感谢分享！\n这个观点我支持！\n很有帮助，收藏了！");
        }
        strategyCombo.getSelectionModel().select(strategy);
    }

    // ======== 操作按钮 ========

    private void handleStart() {
        if (VisionAgent.getInstance().isRunning()) {
            VisionAgent.getInstance().setPaused(false);
            btnStart.setDisable(true);
            btnPause.setText("暂停");
            btnPause.setDisable(false);
            appendLog("继续运行");
            return;
        }

        // 构建目标描述
        String keywords = keywordsArea.getText().trim();
        String goal;
        if (!keywords.isEmpty()) {
            goal = "在抖音上搜索关键词" + keywords + "，浏览相关的短视频，在评论区发截流评论。";
            appendLog("目标: " + goal);
        } else {
            goal = "浏览抖音视频，在评论区发截流评论。";
            appendLog("[提示] 未设置关键词，AI 会自由浏览抖音页面");
        }

        startTime = System.currentTimeMillis();

        // 注册 VisionAgent 回调
        VisionAgent.getInstance().setCallback(new VisionAgent.AgentCallback() {
            @Override
            public void onLog(String msg) {
                appendLog("[Agent] " + msg);
            }

            @Override
            public void onAction(VisionAction action) {
                // 记录操作到日志
                String log = "操作: " + action.type
                    + (action.x >= 0 ? " (" + action.x + "," + action.y + ")" : "")
                    + (action.text != null && !action.text.isEmpty() ? " 文本: " + action.text : "")
                    + (action.reasoning != null && !action.reasoning.isEmpty() ? " 理由: " + action.reasoning : "");
                appendLog(log);
            }

            @Override
            public void onStatus(String status) {
                Platform.runLater(() -> statusLabel.setText(status));
            }

            @Override
            public void onComplete(String summary) {
                Platform.runLater(() -> {
                    btnStart.setDisable(false);
                    btnPause.setDisable(true);
                    btnStop.setDisable(true);
                    updateStats();
                    showSummary();
                });
            }
        });

        btnStart.setDisable(true);
        btnPause.setDisable(false);
        btnStop.setDisable(false);

        VisionAgent.getInstance().start(goal);
    }

    private void handlePause() {
        VisionAgent agent = VisionAgent.getInstance();
        if (agent.isRunning()) {
            agent.setPaused(!agent.isPaused());
            btnPause.setText(agent.isPaused() ? "继续" : "暂停");
            appendLog(agent.isPaused() ? "已暂停" : "已继续");
        }
    }

    private void handleStop() {
        VisionAgent.getInstance().stop();
        btnStart.setDisable(false);
        btnPause.setDisable(true);
        btnStop.setDisable(true);
        btnPause.setText("暂停");
        stats.setCurrentStatus("已停止");
        statusLabel.setText("已停止");
        appendLog("用户手动停止运行");
    }

    @FXML private void handleSettings() {
        SettingsDialog.show(btnSettings.getScene().getWindow());
        // 设置保存后重新加载配置
        ConfigManager.getInstance().reloadConfig();
        appendLog("设置已更新");
    }

    // ======== 保存/应用配置 ========

    private void handleApplyKeywords() {
        Config cfg = ConfigManager.getInstance().getConfig();
        if (cfg.getMonitor() == null) cfg.setMonitor(new Config.MonitorConfig());
        cfg.getMonitor().setKeywords(parseLines(keywordsArea.getText()));
        if (ConfigManager.getInstance().saveConfig()) {
            appendLog("关键词已应用: " + cfg.getMonitor().getKeywords());
        }
    }

    private void handleSaveAll() {
        Config cfg = ConfigManager.getInstance().getConfig();
        if (cfg.getMonitor() == null) cfg.setMonitor(new Config.MonitorConfig());
        cfg.getMonitor().setKeywords(parseLines(keywordsArea.getText()));
        if (cfg.getComment() == null) cfg.setComment(new Config.CommentConfig());
        if (cfg.getComment().getLlm() == null) cfg.getComment().setLlm(new Config.CommentConfig.LLMConfig());

        String strategy = strategyCombo.getSelectionModel().getSelectedItem();
        cfg.getComment().getLlm().setEnabled(STRATEGY_AI.equals(strategy) || STRATEGY_MIX.equals(strategy));
        if (STRATEGY_AI.equals(strategy) || STRATEGY_MIX.equals(strategy)) {
            cfg.getComment().setFramework(llmPromptArea.getText().trim());
        }
        if (STRATEGY_FIXED.equals(strategy) || STRATEGY_MIX.equals(strategy)) {
            cfg.getComment().setVariants(parseLines(fixedCommentArea.getText()));
        }

        if (ConfigManager.getInstance().saveConfig()) {
            appendLog("所有配置已保存，策略: " + strategy);
        }
    }

    // ======== UI 更新 ========

    public void appendLog(String msg) {
        if (logArea != null) Platform.runLater(() -> {
            logArea.appendText(msg + "\n");
            logArea.setScrollTop(Double.MAX_VALUE);
        });
    }

    public void updateStatus(String s) {
        Platform.runLater(() -> statusLabel.setText(s));
    }

    private void updateStats() {
        Platform.runLater(() -> {
            if (videosFoundLabel != null) videosFoundLabel.setText(String.valueOf(stats.getTotalVideosFound()));
            if (commentsSuccessLabel != null) commentsSuccessLabel.setText(String.valueOf(stats.getTotalCommentsSuccess()));
            if (commentsFailedLabel != null) commentsFailedLabel.setText(String.valueOf(stats.getTotalCommentsFailed()));
            if (commentsSkippedLabel != null) commentsSkippedLabel.setText(String.valueOf(stats.getTotalCommentsSkipped()));
            if (successRateLabel != null) successRateLabel.setText(String.format("%.1f%%", stats.getSuccessRate()));
            if (llmGeneratedLabel != null) llmGeneratedLabel.setText(String.valueOf(stats.getTotalLLMGenerated()));
            if (runningTimeLabel != null && startTime > 0) {
                long mins = (System.currentTimeMillis() - startTime) / 60000;
                runningTimeLabel.setText(mins + "分");
            }
        });
    }

    private void showSummary() {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("运行总结");
            alert.setHeaderText("本次自动化运行结束");
            long mins = startTime > 0 ? (System.currentTimeMillis() - startTime) / 60000 : 0;
            alert.setContentText(String.format("""
                运行时长:   %d 分
                评论成功:   %d
                失败:       %d
                跳过:       %d
                """, mins, stats.getTotalCommentsSuccess(),
                stats.getTotalCommentsFailed(), stats.getTotalCommentsSkipped()));
            alert.show();
        });
    }

    private List<String> parseLines(String raw) {
        return Arrays.stream(raw.split("\n"))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .collect(Collectors.toList());
    }
}
