@echo off
chcp 65001 >nul
echo ========================================
echo   批量重置所有用户密码
echo   生成时间: 2025-12-14
echo ========================================
echo.

set DB_HOST=localhost
set DB_PORT=3306
set DB_USER=root
set DB_PASS=root
set DB_NAME=sports_events
set SQL_FILE=D:\Sports-Events\reset_all_passwords.sql

echo 准备重置以下用户的密码:
echo   - admin (系统管理员)
echo   - teacher01 (张老师)
echo   - student01 (李明)
echo   - student02 (王芳)
echo   - student03 (张伟)
echo.
echo 所有密码将统一重置为: 123456
echo.

pause

echo.
echo 正在执行密码重置...
echo.

mysql -h %DB_HOST% -P %DB_PORT% -u %DB_USER% -p%DB_PASS% %DB_NAME% < "%SQL_FILE%"

if %errorlevel% equ 0 (
    echo.
    echo ========================================
    echo   ✓ 密码重置成功！
    echo ========================================
    echo.
    echo 所有用户登录信息:
    echo ----------------------------------------
    echo 用户名        角色            密码
    echo ----------------------------------------
    echo admin         系统管理员      123456
    echo teacher01     教师            123456
    echo student01     学生            123456
    echo student02     学生            123456
    echo student03     学生            123456
    echo ----------------------------------------
    echo.
    echo 请重启 Spring Boot 应用后测试登录
    echo.
) else (
    echo.
    echo ✗ 密码重置失败
    echo.
    echo 请检查:
    echo   1. MySQL 服务是否运行
    echo   2. 数据库连接信息是否正确
    echo   3. 是否有足够的权限
    echo.
    echo 或手动在 MySQL 客户端执行:
    echo   %SQL_FILE%
    echo.
)

pause
