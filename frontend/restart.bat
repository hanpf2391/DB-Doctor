@echo off
echo 正在清除前端缓存...
echo.

REM 删除 Vite 缓存
if exist "node_modules\.vite" (
    echo 删除 node_modules\.vite
    rmdir /s /q "node_modules\.vite"
)

REM 删除 dist 目录
if exist "dist" (
    echo 删除 dist
    rmdir /s /q "dist"
)

REM 删除 .vite 缓存目录（如果有）
if exist ".vite" (
    echo 删除 .vite
    rmdir /s /q ".vite"
)

echo.
echo 清除完成！
echo.
echo 现在运行: npm run dev
pause
