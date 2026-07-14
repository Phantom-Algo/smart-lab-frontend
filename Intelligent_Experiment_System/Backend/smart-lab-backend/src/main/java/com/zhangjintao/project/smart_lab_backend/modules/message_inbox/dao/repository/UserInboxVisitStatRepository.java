package com.zhangjintao.project.smart_lab_backend.modules.message_inbox.dao.repository;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 用户收件箱查看状态仓储接口。
 */
public interface UserInboxVisitStatRepository {

	Optional<LocalDateTime> findLastViewedAt(Long userId);

	int upsertLastViewedAt(Long userId, LocalDateTime lastViewedAt);
}
