@echo off
REM ===========================================================================
REM OculiX 抖音截流助手 - Portable Distribution Builder
REM ===========================================================================
REM 构建便携版 (含 JRE, 解压即用)
REM ===========================================================================
title OculiX Portable Builder

setlocal enabledelayedexpansion

set ROOT=%~dp0
set VERSION=2.0.0
set APP_NAME=抖音截流助手
set JFX_VERSION=21.0.2
set JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-21.0.11.10-hotspot
set MVN_HOME=C:\Users\18330\tools\apache-maven-3.9.9
set M2_REPO=%USERPROFILE%\.m2\repository
set JFX_MOD_PATH=%M2_REPO%\org\openjfx\javafx-base\%JFX_VERSION%\javafx-base-%JFX_VERSION%-win.jar;%M2_REPO%\org\openjfx\javafx-controls\%JFX_VERSION%\javafx-controls-%JFX_VERSION%-win.jar;%M2_REPO%\org\openjfx\javafx-fxml\%JFX_VERSION%\javafx-fxml-%JFX_VERSION%-win.jar;%M2_REPO%\org\openjfx\javafx-graphics\%JFX_VERSION%\javafx-graphics-%JFX_VERSION%-win.jar

set DIST_DIR=%ROOT%dist\OculiX-Douyin-Bot-v%VERSION%

echo ============================================================
echo  %APP_NAME% v%VERSION% - 便携版制作
echo ============================================================
echo.

REM Step 1: Build
echo [1/4] Building fat JAR...
cd /d "%ROOT%"
call "%MVN_HOME%\bin\mvn.cmd" clean package -DskipTests -q
if %errorlevel% neq 0 ( echo [ERROR] Build failed! & pause & exit /b 1 )
echo   [OK]

REM Step 2: jlink JRE
echo [2/4] Building custom JRE...
if exist "%DIST_DIR%\runtime" rmdir /s /q "%DIST_DIR%\runtime"
"%JAVA_HOME%\bin\jlink.exe" ^
    --module-path "%JFX_MOD_PATH%;%JAVA_HOME%\jmods" ^
    --add-modules java.base,java.desktop,java.logging,java.net.http,java.management,java.naming,jdk.unsupported,javafx.controls,javafx.fxml,javafx.graphics ^
    --output "%DIST_DIR%\runtime" ^
    --strip-debug --no-header-files --no-man-pages --compress=2
if %errorlevel% neq 0 ( echo [ERROR] jlink failed! & pause & exit /b 1 )
echo   [OK]

REM Step 3: Assemble distribution
echo [3/4] Assembling distribution...
if not exist "%DIST_DIR%\app" mkdir "%DIST_DIR%\app"
if not exist "%DIST_DIR%\config" mkdir "%DIST_DIR%\config"
if not exist "%DIST_DIR%\logs" mkdir "%DIST_DIR%\logs"

copy /y "%ROOT%target\douyin-comment-bot-%VERSION%.jar" "%DIST_DIR%\app\" >nul
echo   [OK]

REM Step 4: Create launcher
echo [4/4] Creating launcher...
(
echo @echo off
echo title OculiX 抖音截流助手
echo cd /d "%%%%~dp0"
echo.
echo "runtime\bin\java.exe" -jar "app\douyin-comment-bot-%VERSION%.jar"
echo pause
) > "%DIST_DIR%\启动抖音截流助手.bat"
echo   [OK]

echo.
echo ============================================================
echo  便携版制作完成!
echo ============================================================
echo.
echo 输出目录: %DIST_DIR%
echo.
echo 使用方法:
echo   1. 将 "%DIST_DIR%" 文件夹复制到目标电脑
echo   2. 双击 "启动抖音截流助手.bat" 运行
echo.
pause
