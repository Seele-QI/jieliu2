@echo off
REM ===========================================================================
REM OculiX Douyin Comment Bot - Build Installer Script
REM ===========================================================================
REM This script builds the portable distribution and optionally creates
REM a setup.exe installer using Inno Setup (auto-downloaded if needed).
REM ===========================================================================

echo ============================================================
echo  OculiX Douyin Comment Bot - Build Installer
echo ============================================================

setlocal enabledelayedexpansion
set ROOT=%~dp0
set DIST=%ROOT%dist
set JRE_DIR=%ROOT%runtime
set BUILD_DIR=%DIST%\OculiX-Douyin-Bot
set VERSION=1.0.0

echo Step 1: Build fat JAR...
cd /d "%ROOT%"
call mvn package -DskipTests -q
if %errorlevel% neq 0 (
    echo [ERROR] Maven build failed
    pause
    exit /b 1
)
echo [OK] Build complete

echo Step 2: Prepare distribution directory...
if exist "%BUILD_DIR%" rmdir /s /q "%BUILD_DIR%"
mkdir "%BUILD_DIR%\app"
mkdir "%BUILD_DIR%\config"
mkdir "%BUILD_DIR%\resources\images"
mkdir "%BUILD_DIR%\logs"

copy /y "%ROOT%target\douyin-comment-bot-%VERSION%.jar" "%BUILD_DIR%\app\"
copy /y "%ROOT%src\main\resources\config.json" "%BUILD_DIR%\config\"
echo [OK] Distribution prepared

echo Step 3: Create launcher script...
(
echo @echo off
echo REM OculiX Douyin Comment Bot Launcher
echo setlocal enabledelayedexpansion
echo cd /d "%%~dp0"
echo.
echo REM Check Java
echo java -version ^>nul 2^>^&1
echo if errorlevel 1 (
echo     echo [ERROR] Java is not installed. Please install Java 17+.
echo     echo Download: https://adoptium.net/
echo     pause
echo     exit /b 1
echo )
echo.
echo echo Starting OculiX Douyin Comment Bot...
echo java ^
echo     -Dlogback.configurationFile=config/logback.xml ^
echo     --module-path "app/javafx-lib" --add-modules javafx.controls,javafx.fxml ^
echo     -jar "app/douyin-comment-bot-%VERSION%.jar" ^
echo     --config "config/config.json"
echo pause
) > "%BUILD_DIR%\run.bat"

(
echo @echo off
echo REM OculiX Douyin Bot - Start minimized
echo start /min "" "%%~dp0run.bat"
) > "%BUILD_DIR%\start.bat"

echo [OK] Launcher scripts created

echo Step 4: Create config file with instructions...
(
echo {
echo   "_instructions": "Edit this file to configure the bot",
echo   "browser": {
echo     "profilePath": "C:/Users/YOUR_USERNAME/AppData/Local/Google/Chrome/User Data/Profile 1",
echo     "headless": false,
echo     "url": "https://www.douyin.com"
echo   },
echo   "monitor": {
echo     "keywords": ["??", "??", "????"],
echo     "intervalSeconds": 60
echo   },
echo   "comment": {
echo     "framework": "???????????????~",
echo     "variants": ["?????????", "????????"],
echo     "llm": {
echo       "enabled": false,
echo       "apiKey": "YOUR_MINIMAX_API_KEY"
echo     }
echo   }
echo }
) > "%BUILD_DIR%\config\config.json"
echo [OK] Default config created

echo Step 5: Create README...
(
echo OculiX Douyin Comment Bot v%VERSION%
echo ===============================
echo.
echo [How to use]
echo 1. Edit config\config.json - set your Chrome profile path
echo 2. Double-click run.bat to start
echo 3. The bot will open Douyin, wait for you to log in
echo 4. Once logged in, monitoring starts automatically
echo.
echo [Requirements]
echo - Windows 10/11
echo - Java 17+ (https://adoptium.net/)
echo - Chrome browser (with existing login)
echo.
echo [Configuration]
echo Edit config/config.json to set:
echo   - browser.profilePath: Your Chrome user data path
echo   - monitor.keywords: Search keywords
echo   - comment.llm.apiKey: MiniMax API key (optional)
echo.
echo [Need help?]
echo See full documentation: README.md
) > "%BUILD_DIR%\README.txt"
echo [OK] README created

echo Step 6: Create zip archive...
if exist "%DIST%\OculiX-Douyin-Bot-v%VERSION%.zip" del "%DIST%\OculiX-Douyin-Bot-v%VERSION%.zip"
cd /d "%DIST%"
powershell -Command "Compress-Archive -Path '%BUILD_DIR%\*' -DestinationPath 'OculiX-Douyin-Bot-v%VERSION%.zip' -Force"
if %errorlevel% neq 0 (
    echo [WARN] Zip creation failed, distribution is in %BUILD_DIR%
) else (
    echo [OK] Zip archive created: OculiX-Douyin-Bot-v%VERSION%.zip
)

echo.
echo ============================================================
echo  Build Complete!
echo ============================================================
echo.
echo Output:
echo   %BUILD_DIR%\  (portable distribution)
echo   %DIST%\OculiX-Douyin-Bot-v%VERSION%.zip  (zip archive)
echo.
echo To deploy to another computer:
echo   1. Copy the zip file
echo   2. Install Java 17+ on target machine
echo   3. Edit config\config.json with Chrome profile path
echo   4. Run run.bat
echo.
pause
