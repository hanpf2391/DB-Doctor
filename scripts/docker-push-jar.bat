@echo off
REM ============================================
REM DB-Doctor Docker Build Script (JAR only)
REM ============================================

SETLOCAL EnableDelayedExpansion

REM Switch to project root directory
cd /d "%~dp0.."

REM Read version from pom.xml
for /f "tokens=1,2,3,4,5,6 delims=<>" %%a in ('findstr /n "<version>" pom.xml ^| findstr "^17:"') do (
    set "VERSION=%%c"
    goto :found_version
)
:found_version

set "IMAGE_NAME=hanpf23/db-doctor"

echo.
echo ============================================
echo   DB-Doctor Docker Build (JAR Mode)
echo ============================================
echo.
echo Version: %VERSION%
echo Image: %IMAGE_NAME%:%VERSION%
echo.
echo This will use the pre-built JAR file.
echo.

REM Check Docker is running
docker --version >nul 2>&1
if %errorlevel% neq 0 (
    echo [ERROR] Docker is not running or not installed
    pause
    exit /b 1
)

REM Check JAR file exists
echo [1/4] Checking JAR file...
if not exist "target\db-doctor-*.jar" (
    echo [ERROR] JAR file not found in target directory
    echo Please run build-release.bat first
    pause
    exit /b 1
)
echo [OK] JAR file found
echo.

REM Check if logged in to Docker Hub
echo [2/4] Checking Docker Hub login...
docker info | findstr "Username" >nul 2>&1
if %errorlevel% neq 0 (
    echo [WARN] Not logged in to Docker Hub
    echo.
    pause
    docker login -u hanpf23
)
echo [OK] Docker Hub login checked
echo.

REM Build Docker image using JAR-only Dockerfile
echo [3/4] Building Docker image...
echo [INFO] This is faster than full build (2-3 minutes)...
echo.
docker build -f Dockerfile.jaronly -t %IMAGE_NAME%:%VERSION% .
if %errorlevel% neq 0 (
    echo.
    echo [ERROR] Docker build failed
    pause
    exit /b 1
)
echo [OK] Image built successfully
echo.

REM Tag as latest
echo [4/4] Tagging and pushing...
docker tag %IMAGE_NAME%:%VERSION% %IMAGE_NAME%:latest
echo [OK] Tagged as latest
echo.
echo Pushing to Docker Hub...
echo Pushing %IMAGE_NAME%:%VERSION%...
docker push %IMAGE_NAME%:%VERSION%
echo.
echo Pushing %IMAGE_NAME%:latest...
docker push %IMAGE_NAME%:latest || echo [WARN] Push failed for latest
echo.

REM Display results
echo ============================================
echo   Docker image pushed successfully!
echo ============================================
echo.
echo Image URLs:
echo   Docker Hub: https://hub.docker.com/r/hanpf23/db-doctor
echo.
echo Pull commands:
echo   docker pull %IMAGE_NAME%:%VERSION%
echo   docker pull %IMAGE_NAME%:latest
echo.
echo Run command:
echo   docker run -d -p 8080:8080 --name db-doctor %IMAGE_NAME%:%VERSION%
echo.
pause
