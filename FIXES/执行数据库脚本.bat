@echo off
chcp 65001 >nul
echo ========================================
echo   数据库修复脚本执行工具
echo   生成时间: 2025-12-14
echo ========================================
echo.

set DB_HOST=localhost
set DB_PORT=3306
set DB_USER=root
set DB_PASS=root
set DB_NAME=sports_events
set SQL_FILE=D:\Sports-Events\fix_database_schema.sql

echo 数据库连接信息:
echo   主机: %DB_HOST%:%DB_PORT%
echo   用户: %DB_USER%
echo   数据库: %DB_NAME%
echo   脚本: %SQL_FILE%
echo.

echo 正在执行数据库脚本...
echo.

mysql -h %DB_HOST% -P %DB_PORT% -u %DB_USER% -p%DB_PASS% %DB_NAME% < "%SQL_FILE%"

if %errorlevel% equ 0 (
    echo.
    echo ✓ 数据库脚本执行成功！
    echo.
    echo 已完成:
    echo   - 添加 sport_item.current_participants 字段
    echo   - 初始化现有数据的报名人数
    echo.
) else (
    echo.
    echo ✗ 数据库脚本执行失败
    echo.
    echo 可能的原因:
    echo   1. MySQL 未安装或未添加到 PATH
    echo   2. 数据库连接信息不正确
    echo   3. 数据库权限问题
    echo.
    echo 手动执行方法:
    echo   1. 打开 MySQL Workbench 或 Navicat
    echo   2. 连接到 sports_events 数据库
    echo   3. 打开并执行: %SQL_FILE%
    echo.
)

pause
