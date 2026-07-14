package com.zhangjintao.project.smart_lab_backend.modules.message_inbox.dao.repository;

import com.zhangjintao.project.smart_lab_backend.common.result.PageResult;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.domain.entity.MessageTemplate;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.enums.TemplateStatus;

import java.util.Optional;

/**
 * 消息模板仓储接口
 */
public interface MsgTemplateRepository {

	/**
	 * 根据ID查询已启用的消息模板
	 *
	 * @param id 模板ID
	 * @return 消息模板
	 */
	Optional<MessageTemplate> findEnabledById(Long id);

	/**
	 * 根据模板编码查询已启用的消息模板
	 *
	 * @param templateCode 模板编码
	 * @return 消息模板
	 */
	Optional<MessageTemplate> findEnabledByCode(String templateCode);

	Optional<MessageTemplate> findById(Long id);

	Optional<MessageTemplate> findByCode(String templateCode);

	boolean existsByCode(String templateCode);

	PageResult<MessageTemplate> page(long pageNum, long pageSize, String templateCode, String templateName,
			String messageType, TemplateStatus status);

	MessageTemplate insert(MessageTemplate template);

	MessageTemplate update(MessageTemplate template);

	void disable(Long id, Long updatedBy);
}
