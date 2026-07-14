package com.zhangjintao.project.smart_lab_backend.modules.message_inbox.service.impl;

import com.zhangjintao.project.smart_lab_backend.common.result.PageResult;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.dao.repository.MsgTemplateRepository;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.domain.entity.MessageTemplate;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.enums.ContentFormat;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.enums.TemplateStatus;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.model.dto.MessagePublishCommand;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.model.dto.MessageTemplateSnapshot;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class MessageTemplateServiceImplTest {

	@Test
	void resolveTemplateUsesMessageTypeWhenTemplateCodeIsBlank() {
		RecordingMsgTemplateRepository msgTemplateRepository = new RecordingMsgTemplateRepository();
		MessageTemplateServiceImpl service = new MessageTemplateServiceImpl(msgTemplateRepository);
		String messageType = "equipment.normal.loan.approved";
		msgTemplateRepository.template = template(123L, messageType);

		MessageTemplateSnapshot snapshot = service.resolveTemplate(MessagePublishCommand.builder()
				.messageType(messageType)
				.build());

		assertThat(snapshot.getTemplateId()).isEqualTo(123L);
		assertThat(snapshot.getContentFormat()).isEqualTo(ContentFormat.TEXT);
		assertThat(msgTemplateRepository.lastTemplateCode).isEqualTo(messageType);
		assertThat(msgTemplateRepository.lastTemplateId).isNull();
	}

	@Test
	void resolveTemplateUsesExplicitTemplateCodeFirst() {
		RecordingMsgTemplateRepository msgTemplateRepository = new RecordingMsgTemplateRepository();
		MessageTemplateServiceImpl service = new MessageTemplateServiceImpl(msgTemplateRepository);
		String templateCode = "equipment.normal.loan.rejected";
		msgTemplateRepository.template = template(456L, templateCode);

		MessageTemplateSnapshot snapshot = service.resolveTemplate(MessagePublishCommand.builder()
				.messageType("equipment.normal.loan.approved")
				.templateCode(templateCode)
				.build());

		assertThat(snapshot.getTemplateId()).isEqualTo(456L);
		assertThat(msgTemplateRepository.lastTemplateCode).isEqualTo(templateCode);
	}

	private MessageTemplate template(Long id, String templateCode) {
		return MessageTemplate.builder()
				.id(id)
				.templateCode(templateCode)
				.templateName("设备借用审批结果")
				.messageType(templateCode)
				.titleTemplate("审批结果")
				.contentTemplate("你的设备借用申请已有结果")
				.contentFormat(ContentFormat.TEXT)
				.status(TemplateStatus.ENABLED)
				.build();
	}

	private static class RecordingMsgTemplateRepository implements MsgTemplateRepository {

		private MessageTemplate template;

		private Long lastTemplateId;

		private String lastTemplateCode;

		@Override
		public Optional<MessageTemplate> findEnabledById(Long id) {
			this.lastTemplateId = id;
			return Optional.ofNullable(template);
		}

		@Override
		public Optional<MessageTemplate> findEnabledByCode(String templateCode) {
			this.lastTemplateCode = templateCode;
			return Optional.ofNullable(template);
		}

		@Override
		public Optional<MessageTemplate> findById(Long id) {
			throw new UnsupportedOperationException("当前测试不需要该方法");
		}

		@Override
		public Optional<MessageTemplate> findByCode(String templateCode) {
			throw new UnsupportedOperationException("当前测试不需要该方法");
		}

		@Override
		public boolean existsByCode(String templateCode) {
			throw new UnsupportedOperationException("当前测试不需要该方法");
		}

		@Override
		public PageResult<MessageTemplate> page(long pageNum, long pageSize, String templateCode, String templateName,
				String messageType, TemplateStatus status) {
			throw new UnsupportedOperationException("当前测试不需要该方法");
		}

		@Override
		public MessageTemplate insert(MessageTemplate template) {
			throw new UnsupportedOperationException("当前测试不需要该方法");
		}

		@Override
		public MessageTemplate update(MessageTemplate template) {
			throw new UnsupportedOperationException("当前测试不需要该方法");
		}

		@Override
		public void disable(Long id, Long updatedBy) {
			throw new UnsupportedOperationException("当前测试不需要该方法");
		}
	}
}
