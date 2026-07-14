# FlywayDB 使用规范

## 放置位置

所有 Flyway migration 脚本统一放置在项目相对路径：

`src/main/resources/db/migration`

对应 Spring 配置为：

`spring.flyway.locations: classpath:db/migration`

---

## 迁移脚本命名规范

当前项目默认只使用 Flyway 的 Versioned Migration，命名格式为：

`VyyyyMMddHHmmss__lower_snake_case_description.sql`

例如：

`V20260707123645__create_user_table.sql`

命名规则：

1. `V` 是固定前缀，表示 Versioned Migration。
2. 版本号使用 14 位时间戳：`yyyyMMddHHmmss`。
3. 版本号与描述之间必须使用两个下划线：`__`。
4. 描述部分使用小写英文和下划线，尽量准确表达脚本用途。
5. 文件必须以 `.sql` 结尾。
6. 不允许新增空 migration 文件。

---

## 迁移规则

1. 已经合并到主干分支的 migration 脚本，不允许修改内容、删除文件或重命名文件。

2. 已经在共享环境、测试环境、生产环境，或其他成员本地执行过的 migration 脚本，不允许再修改内容、删除文件或重命名文件。

3. 如果需要修正已经执行过的数据库结构，必须新增一个版本号更大的 migration 脚本，通过 `ALTER TABLE`、`CREATE INDEX` 等方式向前演进。

4. 仅在个人分支内、尚未合并主干、且尚未被其他人执行的 migration 脚本，可以在合并前调整版本号或内容。

5. 合并到主干分支前，必须基于最新 `origin/main` 检查 migration 版本号：

   - 先更新本地的 `origin/main`。
   - 查看主干分支当前最大的 migration 版本号。
   - 确保本分支新增的所有 migration 版本号都大于主干分支当前最大的 migration 版本号。
   - 如果本分支有多个 migration，也要保证它们之间的版本顺序符合真实执行顺序。

6. 如果本分支 migration 版本号小于或等于主干分支最新版本号，必须在合并前修改本分支 migration 的版本号，使其大于主干分支最新版本号。

7. 如果某个 migration 在本地开发库中已经执行过，之后又在个人分支内修改了文件名、版本号或内容，推荐直接删除并重建本地开发数据库，然后重新执行全部 migration。

8. 不要在共享环境、测试环境、生产环境随意执行 `flyway repair`。`repair` 只能用于明确知道原因的本地开发库修复，不能用来掩盖已经执行过的 migration 被修改的问题。

9. 默认不要使用 `R__xxx.sql` 形式的 Repeatable Migration。确实需要使用时，必须先经过团队确认，避免重复执行带来不可预期的数据或结构变更。
