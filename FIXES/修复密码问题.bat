@echo off
chcp 65001 >nul
echo ========================================
echo   密码问题诊断和修复工具
echo   生成时间: 2025-12-14
echo ========================================
echo.

set DB_HOST=localhost
set DB_PORT=3306
set DB_USER=root
set DB_PASS=root
set DB_NAME=sports_events

echo [步骤 1/4] 检查数据库连接...
echo.

mysql -h %DB_HOST% -P %DB_PORT% -u %DB_USER% -p%DB_PASS% -e "SELECT 1" >nul 2>&1
if %errorlevel% neq 0 (
    echo ✗ 数据库连接失败
    echo.
    echo 可能原因:
    echo   1. MySQL 服务未启动
    echo   2. 用户名或密码错误
    echo   3. MySQL 未添加到系统 PATH
    echo.
    echo 请先解决数据库连接问题
    pause
    exit /b 1
)

echo ✓ 数据库连接成功
echo.

echo [步骤 2/4] 查询当前 admin 用户信息...
echo.

mysql -h %DB_HOST% -P %DB_PORT% -u %DB_USER% -p%DB_PASS% %DB_NAME% -e "SELECT username, role, status, LEFT(password, 30) as password_preview FROM sys_user WHERE username = 'admin';"

echo.
echo [步骤 3/4] 更新密码为 123456...
echo.

mysql -h %DB_HOST% -P %DB_PORT% -u %DB_USER% -p%DB_PASS% %DB_NAME% < "D:\Sports-Events\fix_admin_password_multiple.sql"

if %errorlevel% equ 0 (
    echo ✓ 密码更新成功
) else (
    echo ✗ 密码更新失败
    pause
    exit /b 1
)

echo.
echo [步骤 4/4] 验证更新结果...
echo.

mysql -h %DB_HOST% -P %DB_PORT% -u %DB_USER% -p%DB_PASS% %DB_NAME% -e "SELECT username, role, LEFT(password, 30) as new_password_preview, update_time FROM sys_user WHERE username = 'admin';"

echo.
echo ========================================
echo   修复完成！
echo ========================================
echo.
echo 现在可以使用以下凭据登录:
echo   用户名: admin
echo   密码: 123456
echo.
echo 如果仍然无法登录，请尝试:
echo   1. 重启 Spring Boot 应用
echo   2. 清理浏览器缓存
echo   3. 运行: D:\Sports-Events\backend\src\test\java\com\sports\test\PasswordGenerator.java
echo      生成新的密码哈希
echo.
pause
