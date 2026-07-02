@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion

set JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-21.0.11.10-hotspot
set BOT_DIR=%~dp0

set M2_REPO=%USERPROFILE%\.m2\repository\org\openjfx
set JFX_VER=21.0.2

:: 使用 Windows 平台的 javafx jar（含 module-info.class）
set JFX_DIR=%TEMP%\oculix-javafx
if not exist "%JFX_DIR%" mkdir "%JFX_DIR%"
if not exist "%JFX_DIR%\javafx.base.jar" copy "%M2_REPO%\javafx-base\%JFX_VER%\javafx-base-%JFX_VER%-win.jar" "%JFX_DIR%\javafx.base.jar" >nul
if not exist "%JFX_DIR%\javafx.controls.jar" copy "%M2_REPO%\javafx-controls\%JFX_VER%\javafx-controls-%JFX_VER%-win.jar" "%JFX_DIR%\javafx.controls.jar" >nul
if not exist "%JFX_DIR%\javafx.fxml.jar" copy "%M2_REPO%\javafx-fxml\%JFX_VER%\javafx-fxml-%JFX_VER%-win.jar" "%JFX_DIR%\javafx.fxml.jar" >nul
if not exist "%JFX_DIR%\javafx.graphics.jar" copy "%M2_REPO%\javafx-graphics\%JFX_VER%\javafx-graphics-%JFX_VER%-win.jar" "%JFX_DIR%\javafx.graphics.jar" >nul

:: 启动
"%JAVA_HOME%\bin\java.exe" ^
    --module-path "%JFX_DIR%" ^
    --add-modules javafx.controls,javafx.fxml ^
    -cp "%BOT_DIR%target\douyin-comment-bot-2.0.1.jar;%BOT_DIR%target\douyin-comment-bot-2.0.0.jar" ^
    com.oculix.douyin.App

endlocal
