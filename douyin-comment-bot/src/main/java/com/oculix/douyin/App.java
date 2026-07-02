package com.oculix.douyin;

import com.oculix.douyin.gui.MainController;
import com.oculix.douyin.service.ConfigManager;
import com.oculix.douyin.util.AppLogger;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class App extends Application {

    @Override
    public void start(Stage primaryStage) {
        try {
            AppLogger.init("./logs/bot.log");
            AppLogger.info("========================================");
            AppLogger.info("OculiX 抖音截流助手 v2.0.0");
            AppLogger.info("视觉AI自动化 - LLM驱动");
            AppLogger.info("========================================");

            ConfigManager.getInstance().loadConfig();

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/main.fxml"));
            Scene scene = new Scene(loader.load(), 960, 680);

            MainController controller = loader.getController();
            controller.setStage(primaryStage);

            primaryStage.setTitle("OculiX 抖音截流助手?");
            primaryStage.setScene(scene);
            primaryStage.setMinWidth(800);
            primaryStage.setMinHeight(600);
            primaryStage.setOnCloseRequest(e -> {
                com.oculix.douyin.agent.VisionAgent.getInstance().stop();
                Platform.exit();
                System.exit(0);
            });

            primaryStage.show();
            AppLogger.info("UI启动完成");

        } catch (Exception e) {
            System.err.println("启动失败: " + e.getMessage());
            e.printStackTrace();
            Platform.exit();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
