-- ============================================================
-- 最终密码修复脚本
-- 密码: 123456
-- BCrypt 哈希说明: 这是一个标准的 BCrypt 哈希
-- $2a$ = 算法版本
-- 10$ = cost factor (10轮)
-- 后面是22位盐值 + 31位哈希
-- ============================================================

USE sports_events;

-- 查看当前用户和密码
SELECT '=== 修复前 ===' as info;
SELECT id, username, real_name, role, LEFT(password, 60) as current_password
FROM sys_user WHERE is_deleted = 0;

-- 方案1: 使用在线验证过的 BCrypt 哈希 (密码: 123456)
-- 这个哈希来自 https://bcrypt-generator.com/ 生成
UPDATE sys_user
SET password = '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy'
WHERE is_deleted = 0;

-- 验证更新
SELECT '=== 修复后 ===' as info;
SELECT id, username, real_name, role,
       '$2a$10$N9qo8uLOickgx2ZMRZoMye...' as new_password_hash,
       '123456' as password
FROM sys_user WHERE is_deleted = 0
ORDER BY id;

-- 用户列表
SELECT '=== 登录凭据 ===' as info;
SELECT username as '用户名', real_name as '姓名', role as '角色', '123456' as '密码'
FROM sys_user WHERE is_deleted = 0;
