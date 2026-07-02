@echo off
REM ===========================================================================
REM OculiX Douyin Comment Bot - ??????
REM ????????: mvn package -DskipTests
REM ===========================================================================

title OculiX Bot Packager
cd /d "%~dp0"

set VERSION=1.0.0
set JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-21.0.11.10-hotspot
set JPACKAGE="%JAVA_HOME%\bin\jpackage"
set M2_REPO=%USERPROFILE%\.m2\repository
set JFX_MOD_PATH=%M2_REPO%\org\openjfx\javafx-base\21.0.2\javafx-base-21.0.2-win.jar;%M2_REPO%\org\openjfx\javafx-controls\21.0.2\javafx-controls-21.0.2-win.jar;%M2_REPO%\org\openjfx\javafx-fxml\21.0.2\javafx-fxml-21.0.2-win.jar;%M2_REPO%\org\openjfx\javafx-graphics\21.0.2\javafx-graphics-21.0.2-win.jar

echo ============================================================
echo  OculiX Douyin Bot - Package Builder v%VERSION%
echo ============================================================
echo.

REM Step 1: Build
echo [1/5] Building fat JAR...
call mvn package -DskipTests -q
if %errorlevel% neq 0 (
    echo [ERROR] Build failed!
    pause
    exit /b 1
)
echo   OK

REM Step 2: Build custom JRE
echo [2/5] Building custom JRE (56MB)...
if exist dist\runtime rmdir /s /q dist\runtime
%JAVA_HOME%\bin\jlink --module-path "%JFX_MOD_PATH%;%JAVA_HOME%\jmods" --add-modules java.base,java.desktop,java.logging,java.net.http,java.management,java.naming,jdk.unsupported,javafx.controls,javafx.fxml,javafx.graphics --output dist\runtime --strip-debug --no-header-files --no-man-pages --compress=2 >nul 2>&1
echo   OK

REM Step 3: Assemble distribution
echo [3/5] Assembling distribution...
set DIST_DIR=dist\OculiX-Douyin-Bot-v%VERSION%
if exist %DIST_DIR% rmdir /s /q %DIST_DIR%
mkdir %DIST_DIR%\runtime %DIST_DIR%\app %DIST_DIR%\config %DIST_DIR%\resources\images %DIST_DIR%\logs

robocopy dist\runtime %DIST_DIR%\runtime /E /NJH /NJS /NDL /NFL /NP >nul
copy /y target\douyin-comment-bot-%VERSION%.jar %DIST_DIR%\app\ >nul
copy /y src\main\resources\config.json %DIST_DIR%\config\ >nul
echo   OK

REM Step 4: Create launcher
echo [4/5] Creating launcher...
(
echo @echo off
echo title OculiX Douyin Comment Bot
echo cd /d "%%%%~dp0"
echo if exist "runtime\bin\java.exe" (set JAVA=%%%%~dp0runtime\bin\java.exe) else (set JAVA=java)
echo echo Starting bot...
echo "%%%%JAVA%%%%" -jar "app\douyin-comment-bot-%VERSION%.jar"
echo pause
) > %DIST_DIR%\OculiX-Douyin-Bot.bat
echo   OK

REM Step 5: Create zip
echo [5/5] Creating zip archive...
if exist dist\OculiX-Douyin-Bot-v%VERSION%.zip del dist\OculiX-Douyin-Bot-v%VERSION%.zip
powershell -Command "Compress-Archive -Path '%DIST_DIR%\*' -DestinationPath 'dist\OculiX-Douyin-Bot-v%VERSION%.zip' -Force" >nul
echo   OK

echo.
echo ============================================================
echo  Package Complete!
echo ============================================================
echo.
echo Output:
echo   %DIST_DIR%\           (portable folder)
echo   dist\OculiX-Douyin-Bot-v%VERSION%.zip  (zip archive)
echo.
echo Size: ~300 MB (includes Java runtime)
echo.
pause
