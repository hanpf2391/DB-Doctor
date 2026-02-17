@echo off
REM ============================================
REM DB-Doctor 发布打包脚本 (Windows)
REM ============================================

SETLOCAL EnableDelayedExpansion

REM 进入项目根目录
cd /d "%~dp0.."

echo.
echo ============================================
echo   DB-Doctor Release Package Tool
echo ============================================
echo.

REM 使用默认版本号
set VERSION=v0.1.0

echo Version: %VERSION%
echo.

REM 1. Clean old build
echo [1/7] Cleaning old build files...
call mvn clean -q
if %errorlevel% neq 0 (
    echo ERROR: Clean failed
    pause
    exit /b 1
)
echo OK: Clean completed
echo.

REM 2. Build JAR
echo [2/7] Building JAR package...
call mvn clean package -Dmaven.test.skip=true -q
if %errorlevel% neq 0 (
    echo ERROR: Build failed
    pause
    exit /b 1
)

if not exist "target\db-doctor-3.0.0.jar" (
    echo ERROR: JAR file not found
    pause
    exit /b 1
)
echo OK: Build completed - target\db-doctor-3.0.0.jar
echo.

REM 3. Create release directory
echo [3/7] Creating release directory...
set RELEASE_DIR=DB-Doctor-%VERSION%
set DIST_DIR=dist
if exist "%DIST_DIR%" rd /s /q "%DIST_DIR%"
mkdir "%DIST_DIR%\%RELEASE_DIR%"
echo OK: Created directory - %DIST_DIR%\%RELEASE_DIR%
echo.

REM 4. Copy files
echo [4/7] Copying files to release directory...

REM Copy JAR
copy "target\db-doctor-3.0.0.jar" "%DIST_DIR%\%RELEASE_DIR%\db-doctor.jar" >nul
echo   OK: db-doctor.jar

REM Create config directory
mkdir "%DIST_DIR%\%RELEASE_DIR%\config" 2>nul
if exist "src\main\resources\application-local.yml.template" (
    copy "src\main\resources\application-local.yml.template" "%DIST_DIR%\%RELEASE_DIR%\config\" >nul
    echo   OK: config\application-local.yml.template
)

REM Create scripts directory
mkdir "%DIST_DIR%\%RELEASE_DIR%\scripts" 2>nul
if exist "scripts\start.sh" (
    copy "scripts\start.sh" "%DIST_DIR%\%RELEASE_DIR%\scripts\" >nul
    echo   OK: scripts\start.sh
)
if exist "scripts\start.bat" (
    copy "scripts\start.bat" "%DIST_DIR%\%RELEASE_DIR%\scripts\" >nul
    echo   OK: scripts\start.bat
)

REM Copy docs
if exist "README.md" (
    copy "README.md" "%DIST_DIR%\%RELEASE_DIR%\" >nul
    echo   OK: README.md
)
if exist "LICENSE" (
    copy "LICENSE" "%DIST_DIR%\%RELEASE_DIR%\" >nul
    echo   OK: LICENSE
)
if exist "docs\快速开始.md" (
    xcopy "docs" "%DIST_DIR%\%RELEASE_DIR%\docs\" /E /I /Q >nul
    echo   OK: docs\
)
echo.

REM 5. Create QUICKSTART
echo [5/7] Generating QUICKSTART.md...
(
echo # DB-Doctor Quick Start Guide
echo.
echo ## System Requirements
echo.
echo - Java 17 or higher
echo - OS: Windows / Linux / macOS
echo - Memory: 2GB+
echo - Disk: 500MB+
echo.
echo ## Quick Start
echo.
echo ### Windows Users
echo.
echo 1. Double-click `scripts\start.bat`
echo 2. Visit: http://localhost:8080
echo 3. Login: dbdoctor / dbdoctor
echo.
echo ### Linux/Mac Users
echo.
echo ```bash
echo chmod +x scripts/*.sh
echo ./scripts/start.sh
echo ```
echo.
echo ## Configuration
echo.
echo Edit `config\application-local.yml` to configure:
echo.
echo - Database connection
echo - AI service API key
echo - Email notification settings
echo.
echo ## Support
echo.
echo - GitHub: https://github.com/hanpf2391/DB-Doctor
echo.
) > "%DIST_DIR%\%RELEASE_DIR%\QUICKSTART.md"
echo OK: QUICKSTART.md
echo.

REM 6. Create ZIP package
echo [6/7] Creating ZIP package...
cd "%DIST_DIR%"

echo   Creating ZIP file...
powershell -Command "Compress-Archive -Path '%RELEASE_DIR%' -DestinationPath '%RELEASE_DIR%.zip' -Force"
echo   OK: %RELEASE_DIR%.zip

cd .. >nul
echo.

REM 7. Show result
echo [7/7] Package completed!
echo.
echo ============================================
echo   Release Package Location
echo ============================================
echo.
echo Package: %DIST_DIR%\%RELEASE_DIR%.zip
echo.
echo Directory: %DIST_DIR%\%RELEASE_DIR%\
echo.
echo ============================================
echo   Next Steps
echo ============================================
echo.
echo 1. Test package:
echo    cd %DIST_DIR%\%RELEASE_DIR%
echo    scripts\start.bat
echo.
echo 2. Upload to GitHub Release:
echo    https://github.com/hanpf2391/DB-Doctor/releases/new
echo.
echo.

pause
