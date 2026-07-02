package com.oculix.douyin.gui;

import com.oculix.douyin.model.Config;
import com.oculix.douyin.service.ConfigManager;
import com.oculix.douyin.util.AppLogger;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

public class SettingsDialog {

    @FXML private Label dialogTitle;
    @FXML private Label dialogDesc;
    @FXML private Label labelApiUrl;
    @FXML private Label labelApiUrlHint;
    @FXML private Label labelApiKey;
    @FXML private Label labelModel;
    @FXML private Button btnReset;
    @FXML private Button btnCancel;
    @FXML private Button btnSave;

    @FXML private TextField apiUrlField;
    @FXML private PasswordField apiKeyField;
    @FXML private TextField modelNameField;

    private Stage dialogStage;

    @FXML
    public void initialize() {
        dialogTitle.setText("LLM 模型设置");
        dialogDesc.setText("配置视觉AI模型。默认使用 MiniMax-VL（支持看图）。也可换其他 OpenAI 兼容模型。");
        labelApiUrl.setText("API 接口地址（默认 MiniMax）");
        labelApiUrlHint.setText("默认已填好 MiniMax 接口地址。如果你用其他模型（DeepSeek/通义千问），替换成对应地址即可。");
        labelApiKey.setText("API Key");
        labelModel.setText("模型名称（默认 MiniMax-VL）");
        btnReset.setText("恢复默认");
        btnCancel.setText("取消");
        btnSave.setText("保存");

        loadCurrentSettings();
    }

    public static void show(Window owner) {
        try {
            FXMLLoader loader = new FXMLLoader(
                SettingsDialog.class.getResource("/settings_dialog.fxml"));
            Scene scene = new Scene(loader.load());

            SettingsDialog controller = loader.getController();

            Stage stage = new Stage();
            stage.setTitle("LLM 模型设置");
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initOwner(owner);
            stage.setScene(scene);
            stage.setResizable(false);

            controller.dialogStage = stage;
            stage.showAndWait();

        } catch (Exception e) {
            AppLogger.error("打开设置窗口失败: " + e.getMessage());
        }
    }

    private void loadCurrentSettings() {
        Config cfg = ConfigManager.getInstance().getConfig();
        if (cfg.getComment() != null && cfg.getComment().getLlm() != null) {
            var llm = cfg.getComment().getLlm();
            apiUrlField.setText(llm.getApiUrl() != null ? llm.getApiUrl() : "https://api.minimax.chat/v1/text/chatcompletion_v2");
            apiKeyField.setText(llm.getApiKey() != null ? llm.getApiKey() : "");
            modelNameField.setText(llm.getModel() != null ? llm.getModel() : "MiniMax-VL");
        } else {
            apiUrlField.setText("https://api.minimax.chat/v1/text/chatcompletion_v2");
            apiKeyField.setText("");
            modelNameField.setText("MiniMax-VL");
        }
    }

    @FXML
    private void handleSave() {
        String url = apiUrlField.getText().trim();
        String key = apiKeyField.getText().trim();
        String model = modelNameField.getText().trim();

        if (url.isEmpty()) url = "https://api.minimax.chat/v1/text/chatcompletion_v2";
        if (model.isEmpty()) model = "MiniMax-VL";

        Config cfg = ConfigManager.getInstance().getConfig();
        if (cfg.getComment() == null) cfg.setComment(new Config.CommentConfig());
        if (cfg.getComment().getLlm() == null) cfg.getComment().setLlm(new Config.CommentConfig.LLMConfig());

        var llm = cfg.getComment().getLlm();
        llm.setApiUrl(url);
        llm.setApiKey(key);
        llm.setModel(model);
        llm.setEnabled(true);

        boolean saved = ConfigManager.getInstance().saveConfig();
        if (saved) {
            AppLogger.info("LLM 设置已保存 - API: " + url + " | 模型: " + model);
            if (!key.isEmpty()) {
                AppLogger.info("API Key 已配置 (" + key.substring(0, Math.min(8, key.length())) + "...)");
            } else {
                AppLogger.warn("API Key 为空，将使用固定文案库");
            }
        } else {
            AppLogger.error("保存 LLM 设置失败");
        }

        dialogStage.close();
    }

    @FXML
    private void handleReset() {
        apiUrlField.setText("https://api.minimax.chat/v1/text/chatcompletion_v2");
        apiKeyField.setText("");
        modelNameField.setText("MiniMax-VL");
    }

    @FXML
    private void handleCancel() {
        dialogStage.close();
    }
}