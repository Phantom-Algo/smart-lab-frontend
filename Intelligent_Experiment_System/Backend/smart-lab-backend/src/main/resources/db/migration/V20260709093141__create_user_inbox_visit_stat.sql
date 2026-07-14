CREATE TABLE `user_inbox_visit_stat` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '收件箱查看状态主键ID',
    `user_id` BIGINT UNSIGNED NOT NULL COMMENT '用户ID，统一关联登录鉴权子系统的 user_id',
    `last_viewed_at` DATETIME(3) NOT NULL COMMENT '最近一次查看收件箱时间',
    `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_inbox_visit_stat_user_id` (`user_id`)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='用户收件箱查看状态表';
