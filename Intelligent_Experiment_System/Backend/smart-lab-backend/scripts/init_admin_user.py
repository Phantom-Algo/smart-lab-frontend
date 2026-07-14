#!/usr/bin/env python3
"""
默认管理员账号初始化脚本
直接操作 MySQL 数据库，插入默认的 admin 用户、人员档案和用户资料。

密码哈希方案（与 Java 端 AuthServiceImpl 保持一致）:
    - salt = Base64UrlNoPadding(random_bytes(12))   # 16 字符
    - hash = BCrypt(原始密码 + salt)                  # Spring Security BCryptPasswordEncoder

使用方法:
    python3 scripts/init_admin_user.py

依赖:
    pip install pymysql bcrypt
"""

import os
import secrets
import sys
import base64

import bcrypt
import pymysql

# ============================================================
# 数据库连接配置（开发环境）
# ============================================================
DB_CONFIG = {
    "host": "127.0.0.1",
    "port": 3306,
    "user": "test",
    "password": "123456",
    "database": "smart_lab",
    "charset": "utf8mb4",
}

# ============================================================
# 默认管理员配置
# ============================================================
ADMIN_USERNAME = "admin"           # 登录用户名
ADMIN_PASSWORD = "123456"          # 登录密码
ADMIN_REAL_NAME = "系统管理员"       # 真实姓名
ADMIN_DEPT_ID = 1                  # 所属部门：智慧实验室管理中心
ADMIN_POSITION_ID = 15             # 岗位：中心主任
ADMIN_EMAIL = "admin@smart-lab.com"

# 编号前缀
PERSON_NO_PREFIX = "ADMIN-"
ACCOUNT_NO_PREFIX = "ACC-ADMIN-"


def generate_salt() -> str:
    """生成与 Java RandomSecretUtils.randomToken(12) 等效的密码盐值。
    Java: Base64.getUrlEncoder().withoutPadding().encodeToString(12 random bytes)
    """
    random_bytes = secrets.token_bytes(12)
    return base64.urlsafe_b64encode(random_bytes).decode("ascii").rstrip("=")


def bcrypt_hash(password: str, salt: str) -> str:
    """使用 BCrypt 对 (密码 + 盐) 进行哈希。
    与 Spring Security BCryptPasswordEncoder 保持一致（$2a$ 前缀，10 rounds）。
    """
    combined = (password + salt).encode("utf-8")
    # bcrypt.hashpw 默认使用 $2b$ 前缀，Spring Security 使用 $2a$
    # 将 $2b$ 替换为 $2a$ 确保兼容
    hashed = bcrypt.hashpw(combined, bcrypt.gensalt(rounds=10))
    return hashed.decode("utf-8").replace("$2b$", "$2a$")


def main():
    connection = pymysql.connect(**DB_CONFIG)

    try:
        with connection.cursor() as cursor:
            # -------------------------------------------------------
            # 1. 检查部门是否存在
            # -------------------------------------------------------
            cursor.execute("SELECT id FROM department WHERE id = %s", (ADMIN_DEPT_ID,))
            if cursor.fetchone() is None:
                print(f"[错误] 部门 id={ADMIN_DEPT_ID} 不存在，请先执行 DML 初始化脚本。")
                sys.exit(1)
            print("[OK] 部门数据已存在")

            # -------------------------------------------------------
            # 2. 检查岗位是否存在
            # -------------------------------------------------------
            cursor.execute("SELECT id FROM position WHERE id = %s", (ADMIN_POSITION_ID,))
            if cursor.fetchone() is None:
                print(f"[错误] 岗位 id={ADMIN_POSITION_ID} 不存在，请先执行 DML 初始化脚本。")
                sys.exit(1)
            print("[OK] 岗位数据已存在")

            # -------------------------------------------------------
            # 3. 检查管理员账号是否已存在（幂等）
            # -------------------------------------------------------
            cursor.execute("SELECT id FROM user_account WHERE username = %s", (ADMIN_USERNAME,))
            existing = cursor.fetchone()
            if existing is not None:
                print(f"[跳过] 管理员账号 '{ADMIN_USERNAME}' 已存在 (user_account.id={existing[0]})，无需重复创建。")
                return

            # -------------------------------------------------------
            # 4. 生成人员编号 & 账号编号
            # -------------------------------------------------------
            now_ts = __import__("time").strftime("%Y%m%d%H%M%S")
            person_no = f"{PERSON_NO_PREFIX}{now_ts}"
            account_no = f"{ACCOUNT_NO_PREFIX}{now_ts}"

            # -------------------------------------------------------
            # 5. 插入 person 表（人员档案）
            # -------------------------------------------------------
            cursor.execute(
                """INSERT INTO person
                   (person_no, person_name, person_type, gender_type, phone, email,
                    department_id, position_id, person_status, source_type, description)
                   VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)""",
                (person_no, ADMIN_REAL_NAME, 3, 1, None, ADMIN_EMAIL,
                 ADMIN_DEPT_ID, ADMIN_POSITION_ID, 1, 1, "系统初始化默认管理员"),
            )
            person_id = cursor.lastrowid
            print(f"[OK] 插入 person: id={person_id}, person_no={person_no}")

            # -------------------------------------------------------
            # 6. 生成密码盐值与哈希
            # -------------------------------------------------------
            salt = generate_salt()
            password_hash = bcrypt_hash(ADMIN_PASSWORD, salt)
            print(f"[OK] 密码哈希生成完成: salt={salt}, hash_prefix={password_hash[:29]}...")

            # -------------------------------------------------------
            # 7. 插入 user_account 表
            # -------------------------------------------------------
            cursor.execute(
                """INSERT INTO user_account
                   (account_no, username, person_id, password_hash, password_salt,
                    user_type, account_status, failed_login_count,
                    is_password_reset_required, lock_expires_at)
                   VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s)""",
                (account_no, ADMIN_USERNAME, person_id, password_hash, salt,
                 3, 1, 0, 0, None),  # user_type=3(管理员), account_status=1(正常), 首次登录不需要改密
            )
            user_id = cursor.lastrowid
            print(f"[OK] 插入 user_account: id={user_id}, username={ADMIN_USERNAME}")

            # -------------------------------------------------------
            # 8. 插入 user_profile 表（个人信息）
            # -------------------------------------------------------
            cursor.execute(
                """INSERT INTO user_profile
                   (user_id, real_name, gender_type, email, profile_status)
                   VALUES (%s, %s, %s, %s, %s)""",
                (user_id, ADMIN_REAL_NAME, 1, ADMIN_EMAIL, 1),
            )
            print(f"[OK] 插入 user_profile: user_id={user_id}")

        # 提交事务
        connection.commit()
        print("\n========================================")
        print(f"  管理员账号创建成功！")
        print(f"  用户名: {ADMIN_USERNAME}")
        print(f"  密码:   {ADMIN_PASSWORD}")
        print("========================================")

    except Exception as e:
        connection.rollback()
        print(f"[错误] 执行失败: {e}")
        sys.exit(1)
    finally:
        connection.close()


if __name__ == "__main__":
    main()
