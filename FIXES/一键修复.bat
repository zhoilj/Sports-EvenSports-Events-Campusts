@echo off
chcp 65001 >nul
echo ========================================
echo   实体类修复 - 一键复制脚本
echo   生成时间: 2025-12-14
echo ========================================
echo.

set FIXES_DIR=D:\Sports-Events\FIXES
set BACKEND_DIR=D:\Sports-Events\backend\src\main\java\com\sports\modules

echo [1/4] 正在复制 Registration.java...
copy /Y "%FIXES_DIR%\Registration.java" "%BACKEND_DIR%\registration\entity\Registration.java"
if %errorlevel% equ 0 (
    echo ✓ Registration.java 复制成功
) else (
    echo ✗ Registration.java 复制失败
)

echo.
echo [2/4] 正在复制 Schedule.java...
copy /Y "%FIXES_DIR%\Schedule.java" "%BACKEND_DIR%\schedule\entity\Schedule.java"
if %errorlevel% equ 0 (
    echo ✓ Schedule.java 复制成功
) else (
    echo ✗ Schedule.java 复制失败
)

echo.
echo [3/4] 正在复制 News.java...
copy /Y "%FIXES_DIR%\News.java" "%BACKEND_DIR%\news\entity\News.java"
if %errorlevel% equ 0 (
    echo ✓ News.java 复制成功
) else (
    echo ✗ News.java 复制失败
)

echo.
echo [4/4] 正在复制 Result.java...
copy /Y "%FIXES_DIR%\Result.java" "%BACKEND_DIR%\result\entity\Result.java"
if %errorlevel% equ 0 (
    echo ✓ Result.java 复制成功
) else (
    echo ✗ Result.java 复制失败
)

echo.
echo ========================================
echo   复制完成！
echo ========================================
echo.
echo 下一步操作:
echo 1. 在 VSCode 中刷新文件（右键 → Refresh）
echo 2. 检查文件是否已更新
echo 3. 执行数据库脚本: fix_database_schema.sql
echo 4. 运行: mvn clean compile
echo.
pause
