@echo off
REM ============================================
REM DB-Doctor Startup Script (Windows)
REM ============================================

SETLOCAL EnableDelayedExpansion

REM Change to script's parent directory (where db-doctor.jar is located)
cd /d "%~dp0.."

echo.
echo ============================================
echo   DB-Doctor MySQL Slow Query Monitor
echo   Version: v0.1.0
echo ============================================
echo.

REM Check Java environment
echo [1/5] Checking Java environment...
java -version >nul 2>&1
if %errorlevel% neq 0 (
    echo ERROR: Java not found
    echo.
    echo Please install Java 17 or higher:
    echo https://www.oracle.com/java/technologies/downloads/
    echo.
    pause
    exit /b 1
)

REM Check Java version
for /f "tokens=3" %%i in ('java -version 2^>^&1 ^| findstr /i "version"') do set JAVA_VERSION=%%i
set JAVA_VERSION=%JAVA_VERSION:"=%
for /f "tokens=1,2 delims=." %%a in ("%JAVA_VERSION%") do set MAJOR_VERSION=%%a

echo OK: Java version %JAVA_VERSION%
echo.

REM Check JAR file
echo [2/5] Checking application files...
if not exist "db-doctor.jar" (
    echo ERROR: db-doctor.jar not found
    echo.
    echo Please ensure this script is in the correct directory:
    echo DB-Doctor-v3.1.0/
    echo   |-- db-doctor.jar
    echo   |-- scripts/
    echo        |-- start.bat
    echo.
    pause
    exit /b 1
)
echo OK: Found db-doctor.jar
echo.

REM Check configuration file
echo [3/5] Checking configuration...
if not exist "config\application-local.yml" (
    if exist "config\application-local.yml.template" (
        echo WARNING: Config file not found, creating from template...
        copy "config\application-local.yml.template" "config\application-local.yml" >nul
        echo OK: Created config\application-local.yml
    ) else (
        echo WARNING: No config file found, using defaults
    )
) else (
    echo OK: Configuration file exists
)
echo.

REM Create necessary directories
echo [4/5] Creating directories...
if not exist "data" mkdir data
if not exist "logs" mkdir logs
echo OK: Directories ready
echo.

REM Start application
echo [5/5] Starting DB-Doctor...
echo.
echo ============================================
echo   Starting, please wait...
echo   URL: http://localhost:8080
echo   Login: dbdoctor / dbdoctor
echo ============================================
echo.
echo Press Ctrl+C to stop the server
echo.

REM Start Spring Boot application
java -Xms512m -Xmx1024m -jar db-doctor.jar

REM If application exits abnormally
if %errorlevel% neq 0 (
    echo.
    echo ERROR: DB-Doctor exited with error code %errorlevel%
    echo.
    echo Please check the log file: logs\db-doctor.log
    echo.
    pause
)
