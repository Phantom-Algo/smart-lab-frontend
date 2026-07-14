package com.zhangjintao.project.smart_lab_backend.modules.message_inbox.model.converter;

import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.dao.dataobject.MsgTemplateDO;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.domain.entity.MessageTemplate;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.enums.ContentFormat;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.enums.TemplateStatus;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MessageTemplateConverterTest {

	@Test
	void toMsgTemplateDOMapsDomainFields() {
		MessageTemplate template = MessageTemplate.builder()
				.id(1L)
				.templateCode("message_inbox.normal.lab_notice")
				.templateName("实验室通知")
				.messageType("message_inbox.normal.lab_notice")
				.titleTemplate("实验室通知")
				.contentTemplate("实验室开放时间调整")
				.contentFormat(ContentFormat.MARKDOWN)
				.paramSchema("{\"properties\":{}}")
				.status(TemplateStatus.ENABLED)
				.createdBy(77L)
				.updatedBy(88L)
				.build();

		MsgTemplateDO templateDO = MessageInboxConverter.toMsgTemplateDO(template);

		assertThat(templateDO.getId()).isEqualTo(1L);
		assertThat(templateDO.getTemplateCode()).isEqualTo("message_inbox.normal.lab_notice");
		assertThat(templateDO.getTemplateName()).isEqualTo("实验室通知");
		assertThat(templateDO.getMessageType()).isEqualTo("message_inbox.normal.lab_notice");
		assertThat(templateDO.getTitleTemplate()).isEqualTo("实验室通知");
		assertThat(templateDO.getContentTemplate()).isEqualTo("实验室开放时间调整");
		assertThat(templateDO.getContentFormat()).isEqualTo(ContentFormat.MARKDOWN.getCode());
		assertThat(templateDO.getParamSchema()).isEqualTo("{\"properties\":{}}");
		assertThat(templateDO.getStatus()).isEqualTo(TemplateStatus.ENABLED.getCode());
		assertThat(templateDO.getCreatedBy()).isEqualTo(77L);
		assertThat(templateDO.getUpdatedBy()).isEqualTo(88L);
	}
}
