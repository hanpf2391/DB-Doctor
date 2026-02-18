@echo off
REM ============================================
REM DB-Doctor Docker Build and Push Script
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

set "IMAGE_NAME=hanpf23/db-doctor"

echo.
echo ============================================
echo   DB-Doctor Docker Build and Push
echo ============================================
echo.
echo Version: %VERSION%
echo Image: %IMAGE_NAME%:%VERSION%
echo.

REM Check Docker is running
docker --version >nul 2>&1
if %errorlevel% neq 0 (
    echo [ERROR] Docker is not running or not installed
    echo Please install Docker Desktop: https://www.docker.com/products/docker-desktop
    pause
    exit /b 1
)

REM Check if logged in to Docker Hub
echo [1/4] Checking Docker Hub login...
docker info | findstr "Username" >nul 2>&1
if %errorlevel% neq 0 (
    echo [WARN] Not logged in to Docker Hub
    echo.
    echo Please login first:
    echo   docker login -u hanpf23
    echo.
    pause
    docker login -u hanpf23
)
echo [OK] Docker Hub login checked
echo.

REM Build Docker image
echo [2/4] Building Docker image...
echo [INFO] This may take 10-15 minutes for first build...
echo.
docker build -t %IMAGE_NAME%:%VERSION% .
if %errorlevel% neq 0 (
    echo.
    echo [ERROR] Docker build failed
    pause
    exit /b 1
)
echo [OK] Image built successfully
echo.

REM Tag as latest
echo [3/4] Tagging as latest...
docker tag %IMAGE_NAME%:%VERSION% %IMAGE_NAME%:latest
echo [OK] Tagged as latest
echo.

REM Push to Docker Hub
echo [4/4] Pushing to Docker Hub...
echo.
echo Pushing %IMAGE_NAME%:%VERSION%...
docker push %IMAGE_NAME%:%VERSION%
if %errorlevel% neq 0 (
    echo.
    echo [ERROR] Push failed for %VERSION%
    pause
    exit /b 1
)
echo.
echo Pushing %IMAGE_NAME%:latest...
docker push %IMAGE_NAME%:latest
if %errorlevel% neq 0 (
    echo.
    echo [WARN] Push failed for latest (but %VERSION% succeeded)
)
echo.

REM Display results
echo ============================================
echo   Docker images pushed successfully!
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
