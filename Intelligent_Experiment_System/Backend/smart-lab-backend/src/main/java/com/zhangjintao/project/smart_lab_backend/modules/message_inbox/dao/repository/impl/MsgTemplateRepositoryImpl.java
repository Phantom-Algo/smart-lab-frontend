package com.zhangjintao.project.smart_lab_backend.modules.message_inbox.dao.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhangjintao.project.smart_lab_backend.common.result.PageResult;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.dao.dataobject.MsgTemplateDO;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.dao.mapper.MsgTemplateMapper;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.dao.repository.MsgTemplateRepository;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.domain.entity.MessageTemplate;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.enums.TemplateStatus;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.model.converter.MessageInboxConverter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Optional;

/**
 * 消息模板仓储实现
 */
@Repository
@RequiredArgsConstructor
public class MsgTemplateRepositoryImpl implements MsgTemplateRepository {

	private final MsgTemplateMapper msgTemplateMapper;

	@Override
	public Optional<MessageTemplate> findEnabledById(Long id) {
		if (id == null) {
			return Optional.empty();
		}
		return Optional.ofNullable(msgTemplateMapper.selectOne(new LambdaQueryWrapper<MsgTemplateDO>()
				.eq(MsgTemplateDO::getId, id)
				.eq(MsgTemplateDO::getStatus, TemplateStatus.ENABLED.getCode())))
				.map(MessageInboxConverter::toMessageTemplate);
	}

	@Override
	public Optional<MessageTemplate> findEnabledByCode(String templateCode) {
		return Optional.ofNullable(msgTemplateMapper.selectOne(new LambdaQueryWrapper<MsgTemplateDO>()
				.eq(MsgTemplateDO::getTemplateCode, templateCode)
				.eq(MsgTemplateDO::getStatus, TemplateStatus.ENABLED.getCode())))
				.map(MessageInboxConverter::toMessageTemplate);
	}

	@Override
	public Optional<MessageTemplate> findById(Long id) {
		if (id == null) {
			return Optional.empty();
		}
		return Optional.ofNullable(msgTemplateMapper.selectById(id))
				.map(MessageInboxConverter::toMessageTemplate);
	}

	@Override
	public Optional<MessageTemplate> findByCode(String templateCode) {
		if (!StringUtils.hasText(templateCode)) {
			return Optional.empty();
		}
		return Optional.ofNullable(msgTemplateMapper.selectOne(new LambdaQueryWrapper<MsgTemplateDO>()
				.eq(MsgTemplateDO::getTemplateCode, templateCode)))
				.map(MessageInboxConverter::toMessageTemplate);
	}

	@Override
	public boolean existsByCode(String templateCode) {
		if (!StringUtils.hasText(templateCode)) {
			return false;
		}
		Long count = msgTemplateMapper.selectCount(new LambdaQueryWrapper<MsgTemplateDO>()
				.eq(MsgTemplateDO::getTemplateCode, templateCode));
		return count != null && count > 0;
	}

	@Override
	public PageResult<MessageTemplate> page(long pageNum, long pageSize, String templateCode, String templateName,
			String messageType, TemplateStatus status) {
		Page<MsgTemplateDO> page = msgTemplateMapper.selectPage(new Page<>(pageNum, pageSize),
				new LambdaQueryWrapper<MsgTemplateDO>()
						.like(StringUtils.hasText(templateCode), MsgTemplateDO::getTemplateCode, templateCode)
						.like(StringUtils.hasText(templateName), MsgTemplateDO::getTemplateName, templateName)
						.eq(StringUtils.hasText(messageType), MsgTemplateDO::getMessageType, messageType)
						.eq(status != null, MsgTemplateDO::getStatus, status == null ? null : status.getCode())
						.orderByDesc(MsgTemplateDO::getUpdatedAt)
						.orderByDesc(MsgTemplateDO::getId));
		List<MessageTemplate> records = page.getRecords().stream()
				.map(MessageInboxConverter::toMessageTemplate)
				.toList();
		return PageResult.of(records, page.getTotal(), page.getCurrent(), page.getSize());
	}

	@Override
	public MessageTemplate insert(MessageTemplate template) {
		MsgTemplateDO templateDO = MessageInboxConverter.toMsgTemplateDO(template);
		msgTemplateMapper.insert(templateDO);
		return findById(templateDO.getId()).orElseGet(() -> MessageInboxConverter.toMessageTemplate(templateDO));
	}

	@Override
	public MessageTemplate update(MessageTemplate template) {
		MsgTemplateDO templateDO = MessageInboxConverter.toMsgTemplateDO(template);
		msgTemplateMapper.updateById(templateDO);
		return findById(template.getId()).orElseGet(() -> MessageInboxConverter.toMessageTemplate(templateDO));
	}

	@Override
	public void disable(Long id, Long updatedBy) {
		MsgTemplateDO templateDO = new MsgTemplateDO();
		templateDO.setId(id);
		templateDO.setStatus(TemplateStatus.DISABLED.getCode());
		templateDO.setUpdatedBy(updatedBy);
		msgTemplateMapper.updateById(templateDO);
	}
}
