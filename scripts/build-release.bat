@echo off
REM ============================================
REM DB-Doctor Release Build Script (Frontend + Backend)
REM ============================================

SETLOCAL EnableDelayedExpansion

REM Switch to project root directory
cd /d "%~dp0.."

REM Read version from pom.xml - get line 17 (project version)
for /f "tokens=1,2,3,4,5,6 delims=<>" %%a in ('findstr /n "<version>" pom.xml ^| findstr "^17:"') do (
    set "VERSION=%%c"
    goto :found_version
)
:found_version

echo.
echo ============================================
echo   DB-Doctor Release Build Tool
echo ============================================
echo.
echo Version: %VERSION%
echo.

REM 1. Clean old build files
echo [1/8] Cleaning old build files...
call mvn clean -q
if exist frontend\dist rmdir /s /q frontend\dist
if exist dist rmdir /s /q dist
echo [OK] Clean completed
echo.

REM 2. Check frontend dependencies
echo [2/8] Checking frontend dependencies...
cd frontend
if not exist "node_modules" (
    echo [INFO] Frontend dependencies not installed, installing...
    call npm install
)
echo [OK] Frontend dependencies check completed
echo.

REM 3. Build frontend
echo [3/8] Building frontend (Vue 3)...
echo [INFO] Building frontend, please wait...
call npm run build
if not exist "dist" (
    echo [ERROR] Frontend build failed
    pause
    exit /b 1
)
echo [OK] Frontend build completed
echo.

REM 4. Copy frontend resources to backend
echo [4/8] Copying frontend resources to backend...
cd ..
if exist "src\main\resources\static" rmdir /s /q "src\main\resources\static"
mkdir "src\main\resources\static"
xcopy /e /i /y "frontend\dist\*" "src\main\resources\static\" >nul
echo [OK] Frontend resources copied
echo.

REM 5. Build backend (skip tests)
echo [5/8] Building backend (Spring Boot)...
echo [INFO] Packaging, please wait...
REM Temporarily rename test directory to avoid test compilation
if exist "src\test" (
    move "src\test" "src\test.bak" >nul
)
call mvn package -DskipTests -Dmaven.test.skip=true -q
REM Restore test directory
if exist "src\test.bak" (
    move "src\test.bak" "src\test" >nul
)
if not exist "target\db-doctor-%VERSION%.jar" (
    echo [ERROR] Backend build failed: JAR file not found
    echo [INFO] Checking target directory...
    dir target\*.jar
    pause
    exit /b 1
)
echo [OK] Backend build completed: target\db-doctor-%VERSION%.jar
echo.

REM 6. Create release directory
echo [6/8] Creating release directory...
set "RELEASE_DIR=DB-Doctor-v%VERSION%"
set "DIST_DIR=dist"
if exist "%DIST_DIR%" rmdir /s /q "%DIST_DIR%"
mkdir "%DIST_DIR%\%RELEASE_DIR%"
echo [OK] Created directory: %DIST_DIR%\%RELEASE_DIR%
echo.

REM 7. Copy files
echo [7/8] Copying files to release directory...

REM Copy JAR file
copy "target\db-doctor-%VERSION%.jar" "%DIST_DIR%\%RELEASE_DIR%\db-doctor.jar" >nul
echo   [OK] db-doctor.jar

REM Generate SHA256 checksum
cd "%DIST_DIR%\%RELEASE_DIR%"
certutil -hashfile db-doctor.jar SHA256 > db-doctor.jar.sha256
cd ..\..
echo   [OK] db-doctor.jar.sha256

REM Create config directory
mkdir "%DIST_DIR%\%RELEASE_DIR%\config" 2>nul
if exist "src\main\resources\application-local.yml.template" (
    copy "src\main\resources\application-local.yml.template" "%DIST_DIR%\%RELEASE_DIR%\config\" >nul
) else (
    echo # Config template > "%DIST_DIR%\%RELEASE_DIR%\config\application-local.yml.template"
)
echo   [OK] config/application-local.yml.template

REM Create scripts directory
mkdir "%DIST_DIR%\%RELEASE_DIR%\scripts" 2>nul
if exist "scripts\start.bat" copy "scripts\start.bat" "%DIST_DIR%\%RELEASE_DIR%\scripts\" >nul
if exist "scripts\start.sh" copy "scripts\start.sh" "%DIST_DIR%\%RELEASE_DIR%\scripts\" >nul
echo   [OK] scripts/start.bat
echo   [OK] scripts/start.sh

REM Copy documentation
if exist "README.md" copy "README.md" "%DIST_DIR%\%RELEASE_DIR%" >nul
if exist "README_EN.md" copy "README_EN.md" "%DIST_DIR%\%RELEASE_DIR%" >nul
if exist "LICENSE" copy "LICENSE" "%DIST_DIR%\%RELEASE_DIR%" >nul
echo   [OK] README.md
echo   [OK] README_EN.md
echo   [OK] LICENSE
echo.

REM 8. Generate quick start guide
echo [8/8] Generating quick start guide...
(
echo # DB-Doctor Quick Start Guide
echo.
echo ## System Requirements
echo.
echo - Java 17 or higher
echo - OS: Windows / Linux / macOS
echo - Memory: At least 2GB available memory
echo - Disk: At least 500MB free space
echo - MySQL 5.7+ / 8.0+
echo.
echo ## Quick Start
echo.
echo ### For Windows Users
echo.
echo 1. Double-click `scripts\start.bat`
echo 2. Wait for startup to complete
echo 3. Open browser: http://localhost:8080
echo.
echo ### For Linux/Mac Users
echo.
echo ```bash
echo # 1. Grant execute permissions
echo chmod +x scripts/*.sh
echo.
echo # 2. Run startup script
echo ./scripts/start.sh
echo.
echo # 3. Open browser
echo open http://localhost:8080
echo ```
echo.
echo ## First-Time Configuration
echo.
echo After startup, access the Web UI for configuration:
echo.
echo 1. Click "Configuration Center" -^> "Data Source Configuration"
echo    - Fill in MySQL database connection information
echo.
echo 2. Click "Configure AI" -^> Select AI Model
echo    - DeepSeek (Recommended, cost-effective)
echo    - Ollama Local Model (Data stays on-premise)
echo    - Qwen, OpenAI, etc.
echo.
echo 3. Fill in API Key or local model address
echo.
echo 4. Click "Test Connection" to verify configuration
echo.
echo 5. Click "Reload Configuration" to apply
echo.
echo ## FAQ
echo.
echo ### Q: Java not found error
echo A: Please install Java 17 or higher
echo.
echo ### Q: Port 8080 already in use
echo A: Change the server.port configuration
echo.
echo ### Q: How to view logs
echo A: Check the logs/db-doctor.log file
echo.
echo ### Q: Where is the configuration file
echo A: All configurations are managed through Web UI with hot reload support
echo.
echo ## Technical Support
echo.
echo - GitHub: https://github.com/hanpf2391/DB-Doctor
echo - Issues: https://github.com/hanpf2391/DB-Doctor/issues
) > "%DIST_DIR%\%RELEASE_DIR%\QUICKSTART.md"
echo   [OK] QUICKSTART.md
echo.

REM 9. Display results
echo ============================================
echo   Build Completed!
echo ============================================
echo.
echo Release Package Location
echo.
echo JAR file (for GitHub Release):
echo   %DIST_DIR%\%RELEASE_DIR%\db-doctor.jar
echo.
echo Full release package:
echo   %DIST_DIR%\%RELEASE_DIR%\
echo.
echo ============================================
echo   Next Steps
echo ============================================
echo.
echo 1. Test JAR package:
echo    cd %DIST_DIR%\%RELEASE_DIR%
echo    java -jar db-doctor.jar
echo.
echo 2. Visit GitHub to create Release:
echo    https://github.com/hanpf2391/DB-Doctor/releases/new
echo.
echo 3. Fill in Release information:
echo    Tag: v%VERSION%
echo    Title: DB-Doctor v%VERSION% Release
echo.
echo 4. Upload files:
echo    Upload %DIST_DIR%\%RELEASE_DIR%\db-doctor.jar
echo    Rename to: db-doctor.jar
echo.
echo 5. Publish
echo    Click "Publish release"
echo.
pause
