package com.zhangjintao.project.smart_lab_backend.modules.message_inbox.service.impl;

import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.enums.TargetType;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.exception.MessageInboxException;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.model.dto.MessagePublishCommand;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.model.dto.TempMessageTemplate;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MessagePublishValidatorImplTest {

	private final MessagePublishValidatorImpl validator = new MessagePublishValidatorImpl();

	@Test
	void validateAllowsMessageTypeAsDefaultTemplateCode() {
		MessagePublishCommand command = baseCommandBuilder().build();

		assertDoesNotThrow(() -> validator.validate(command));
	}

	@Test
	void validateRejectsTemplateIdAndTemplateCodeTogether() {
		MessagePublishCommand command = baseCommandBuilder()
				.templateId(1L)
				.templateCode("equipment.normal.loan.approved")
				.build();

		MessageInboxException exception = assertThrows(MessageInboxException.class,
				() -> validator.validate(command));

		assertThat(exception.getMessage()).contains("templateId 与 templateCode 不能同时指定");
	}

	@Test
	void validateRejectsTempTemplateWithFormalTemplateSource() {
		MessagePublishCommand command = baseCommandBuilder()
				.templateCode("equipment.normal.loan.approved")
				.tempTemplate(TempMessageTemplate.builder()
						.titleTemplate("审批通过")
						.contentTemplate("你的借用申请已通过")
						.build())
				.build();

		MessageInboxException exception = assertThrows(MessageInboxException.class,
				() -> validator.validate(command));

		assertThat(exception.getMessage()).contains("tempTemplate 不能与 templateId/templateCode 同时指定");
	}

	private MessagePublishCommand.MessagePublishCommandBuilder baseCommandBuilder() {
		return MessagePublishCommand.builder()
				.eventNo("equipment.normal.loan.approved:2026070800000001")
				.sourceSystem("equipment")
				.messageType("equipment.normal.loan.approved")
				.targetType(TargetType.ALL);
	}
}
