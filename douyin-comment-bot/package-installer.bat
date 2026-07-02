@echo off
REM ===========================================================================
REM OculiX 抖音截流助手 - jpackage Installer Builder
REM ===========================================================================
REM 构建链条: mvn package -> jlink (custom JRE) -> jpackage (app-image) -> jpackage (exe)
REM ===========================================================================
REM WiX 依赖: 生成 .exe 安装包需要 WiX Toolset 3.14+
REM   如果无法在线下载 WiX，请手动从以下地址下载 wix314.exe:
REM   https://github.com/wixtoolset/wix3/releases/tag/wix3141rtm
REM   然后用管理员身份安装
REM ===========================================================================
title OculiX Installer Builder

setlocal enabledelayedexpansion

set ROOT=%~dp0
set VERSION=2.0.0
set APP_NAME=抖音截流助手
set APP_DESC=OculiX 抖音评论引流助手 - 视觉AI自动化
set VENDOR=OculiX
set OUTPUT_DIR=%ROOT%dist\installer
set BUILD_DIR=%ROOT%dist\build
set JFX_VERSION=21.0.2

REM === JDK 路径 ===
set JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-21.0.11.10-hotspot

REM === Maven 路径 ===
set MVN_HOME=C:\Users\18330\tools\apache-maven-3.9.9

REM === JavaFX 模块路径 (Windows platform jars) ===
set M2_REPO=%USERPROFILE%\.m2\repository
set JFX_MOD_PATH=%M2_REPO%\org\openjfx\javafx-base\%JFX_VERSION%\javafx-base-%JFX_VERSION%-win.jar;%M2_REPO%\org\openjfx\javafx-controls\%JFX_VERSION%\javafx-controls-%JFX_VERSION%-win.jar;%M2_REPO%\org\openjfx\javafx-fxml\%JFX_VERSION%\javafx-fxml-%JFX_VERSION%-win.jar;%M2_REPO%\org\openjfx\javafx-graphics\%JFX_VERSION%\javafx-graphics-%JFX_VERSION%-win.jar

echo ============================================================
echo  %APP_NAME% v%VERSION% - 安装包制作
echo ============================================================
echo.

REM Step 1: Maven Build
echo [1/5] Building fat JAR...
cd /d "%ROOT%"
call "%MVN_HOME%\bin\mvn.cmd" clean package -DskipTests -q
if %errorlevel% neq 0 ( echo [ERROR] Maven build failed! & pause & exit /b 1 )
echo   [OK]

REM Step 2: Prepare input directory
echo [2/5] Preparing input directory...
if exist "%BUILD_DIR%" rmdir /s /q "%BUILD_DIR%"
mkdir "%BUILD_DIR%\app" "%BUILD_DIR%\config" "%BUILD_DIR%\logs"
copy /y "%ROOT%target\douyin-comment-bot-%VERSION%.jar" "%BUILD_DIR%\app\" >nul
echo   [OK]

REM Step 3: Build custom JRE with jlink
echo [3/5] Building custom JRE (jlink)...
if exist "%BUILD_DIR%\runtime" rmdir /s /q "%BUILD_DIR%\runtime"
"%JAVA_HOME%\bin\jlink.exe" --module-path "%JFX_MOD_PATH%;%JAVA_HOME%\jmods" --add-modules java.base,java.desktop,java.logging,java.net.http,java.management,java.naming,jdk.unsupported,javafx.controls,javafx.fxml,javafx.graphics --output "%BUILD_DIR%\runtime" --strip-debug --no-header-files --no-man-pages --compress=2
if %errorlevel% neq 0 ( echo [ERROR] jlink failed! & pause & exit /b 1 )
echo   [OK]

REM Step 4: Build app-image
echo [4/5] Building app-image...
if exist "%OUTPUT_DIR%" rmdir /s /q "%OUTPUT_DIR%"
mkdir "%OUTPUT_DIR%"
"%JAVA_HOME%\bin\jpackage.exe" --type app-image --input "%BUILD_DIR%\app" --runtime-image "%BUILD_DIR%\runtime" --main-jar "douyin-comment-bot-%VERSION%.jar" --main-class com.oculix.douyin.App --name "%APP_NAME%" --app-version %VERSION% --vendor "%VENDOR%" --description "%APP_DESC%" --dest "%OUTPUT_DIR%" --java-options "-Dfile.encoding=UTF-8"
if %errorlevel% neq 0 ( echo [ERROR] jpackage app-image failed! & pause & exit /b 1 )
xcopy /e /i /y /q "%BUILD_DIR%\config" "%OUTPUT_DIR%\%APP_NAME%\config" >nul
xcopy /e /i /y /q "%BUILD_DIR%\logs" "%OUTPUT_DIR%\%APP_NAME%\logs" >nul
echo   [OK]

REM Step 5: Try building exe installer
echo [5/5] Attempting exe installer (requires WiX Toolset)...

REM Check if WiX is available
where candle.exe >nul 2>&1
if %errorlevel% equ 0 (
    "%JAVA_HOME%\bin\jpackage.exe" --type exe --app-image "%OUTPUT_DIR%\%APP_NAME%" --name "%APP_NAME%" --app-version %VERSION% --vendor "%VENDOR%" --description "%APP_DESC%" --dest "%OUTPUT_DIR%" --win-menu --win-menu-group "OculiX" --win-shortcut --win-shortcut-prompt --win-dir-chooser --about-url "https://github.com/oculix-org/Oculix"
    if %errorlevel% equ 0 (
        echo   [OK] EXE installer created!
        goto :done
    )
)

echo   [WARN] WiX Toolset not found, skipping .exe installer.
echo.
echo   The app-image is ready for portable use:
echo     %OUTPUT_DIR%\%APP_NAME%\
echo.
echo   To generate a .exe installer with setup wizard:
echo   Method 1 - Online: Run as Admin in PowerShell:
echo     winget install WiXToolset.WiXToolset
echo.
echo   Method 2 - Offline: Download from another computer with GitHub access:
echo     https://github.com/wixtoolset/wix3/releases/tag/wix3141rtm
echo     (download wix314.exe, transfer via USB, install as Admin)
echo.
echo   Then re-run this script to generate the .exe installer.
echo.

:done
echo.
echo ============================================================
echo  Build Complete!
echo ============================================================
echo.
echo  App-image (portable): %OUTPUT_DIR%\%APP_NAME%\
dir "%OUTPUT_DIR%\*.exe" 2>nul
echo.
pause
