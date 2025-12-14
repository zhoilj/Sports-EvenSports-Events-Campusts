@echo off
chcp 65001 >nul
echo ========================================
echo   修复验证和编译脚本
echo   生成时间: 2025-12-14
echo ========================================
echo.

echo [步骤 1/3] 验证实体类文件...
echo.
echo 检查 Registration.java...
findstr "@TableField" "D:\Sports-Events\backend\src\main\java\com\sports\modules\registration\entity\Registration.java" >nul
if %errorlevel% equ 0 (echo ✓ Registration.java 已包含字段映射) else (echo ✗ Registration.java 可能未正确更新)

echo 检查 Schedule.java...
findstr "@TableField" "D:\Sports-Events\backend\src\main\java\com\sports\modules\schedule\entity\Schedule.java" >nul
if %errorlevel% equ 0 (echo ✓ Schedule.java 已包含字段映射) else (echo ✗ Schedule.java 可能未正确更新)

echo 检查 News.java...
findstr "@TableField" "D:\Sports-Events\backend\src\main\java\com\sports\modules\news\entity\News.java" >nul
if %errorlevel% equ 0 (echo ✓ News.java 已包含字段映射) else (echo ✗ News.java 可能未正确更新)

echo 检查 Result.java...
findstr "@TableField" "D:\Sports-Events\backend\src\main\java\com\sports\modules\result\entity\Result.java" >nul
if %errorlevel% equ 0 (echo ✓ Result.java 已包含字段映射) else (echo ✗ Result.java 可能未正确更新)

echo.
echo ========================================
echo [步骤 2/3] 清理和编译...
echo ========================================
echo.

cd /d D:\Sports-Events\backend
echo 正在执行: mvn clean compile
call mvn clean compile

if %errorlevel% equ 0 (
    echo.
    echo ✓ 编译成功！
) else (
    echo.
    echo ✗ 编译失败，请检查错误信息
    pause
    exit /b 1
)

echo.
echo ========================================
echo [步骤 3/3] 下一步操作提示
echo ========================================
echo.
echo 编译成功！现在需要：
echo.
echo 1. 执行数据库脚本（如果还未执行）:
echo    mysql -u root -proot sports_events ^< D:\Sports-Events\fix_database_schema.sql
echo.
echo 2. 启动应用测试:
echo    mvn spring-boot:run
echo.
echo 3. 测试以下功能:
echo    - 用户登录
echo    - 赛事查询
echo    - 报名功能
echo.
pause
