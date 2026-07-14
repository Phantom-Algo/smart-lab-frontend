package com.zhangjintao.project.smart_lab_backend.modules.message_inbox.model.converter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.dao.dataobject.MsgDeadLetterDO;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.dao.dataobject.MsgEventDO;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.dao.dataobject.MsgTemplateDO;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.dao.dataobject.UserInboxMessageDO;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.domain.entity.MessageDeadLetter;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.domain.entity.MessageEvent;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.domain.entity.MessageTemplate;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.domain.entity.UserInboxMessage;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.enums.ContentFormat;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.enums.DeadLetterStatus;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.enums.DispatchStatus;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.enums.FailureStage;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.enums.MessagePriority;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.enums.TargetType;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.enums.TemplateStatus;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.model.dto.TempMessageTemplate;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class MessageInboxConverterTest {

	private final ObjectMapper objectMapper = new ObjectMapper();

	@Test
	void convertMsgEventDoAndDomainWithJsonFields() throws Exception {
		LocalDateTime now = LocalDateTime.of(2026, 7, 8, 10, 0);
		MsgEventDO eventDO = new MsgEventDO();
		eventDO.setId(88L);
		eventDO.setEventNo("equipment.normal.loan.approved:2026070800000001");
		eventDO.setSourceSystem("equipment");
		eventDO.setMessageType("equipment.normal.loan.approved");
		eventDO.setPriority(MessagePriority.NORMAL.getCode());
		eventDO.setOccurredAt(now);
		eventDO.setTargetType(TargetType.MULTI.getCode());
		eventDO.setTargets("[1001,1002]");
		eventDO.setTemplateId(123L);
		eventDO.setTempTemplate("""
				{"titleTemplate":"审批结果","contentTemplate":"{{deviceName}} 已通过","contentFormat":"TEXT","paramSchema":"{}"}
				""");
		eventDO.setParams("""
				{"deviceName":"示波器","loanId":9}
				""");
		eventDO.setTitle("审批通过");
		eventDO.setContent("示波器 已通过");
		eventDO.setContentFormat(ContentFormat.TEXT.getCode());
		eventDO.setActionUrl("/equipment/loans/9");
		eventDO.setDispatchStatus(DispatchStatus.COMPLETED.getCode());
		eventDO.setRetryCount(1);
		eventDO.setProcessedAt(now);
		eventDO.setCreatedAt(now);
		eventDO.setUpdatedAt(now);

		MessageEvent event = MessageInboxConverter.toMessageEvent(eventDO, objectMapper);

		assertThat(event.getPriority()).isEqualTo(MessagePriority.NORMAL);
		assertThat(event.getTargetType()).isEqualTo(TargetType.MULTI);
		assertThat(event.getTargetUserIds()).containsExactly(1001L, 1002L);
		assertThat(event.getTempTemplate().getContentTemplate()).isEqualTo("{{deviceName}} 已通过");
		assertThat(event.getParams()).containsEntry("deviceName", "示波器");
		assertThat(event.getContentFormat()).isEqualTo(ContentFormat.TEXT);
		assertThat(event.isCompleted()).isTrue();

		MsgEventDO convertedDO = MessageInboxConverter.toMsgEventDO(event, objectMapper);
		assertThat(convertedDO.getPriority()).isEqualTo(MessagePriority.NORMAL.getCode());
		assertThat(convertedDO.getTargetType()).isEqualTo(TargetType.MULTI.getCode());
		assertThat(objectMapper.readValue(convertedDO.getTargets(), new TypeReference<List<Long>>() {
		})).containsExactly(1001L, 1002L);
		assertThat(objectMapper.readValue(convertedDO.getParams(), new TypeReference<Map<String, Object>>() {
		})).containsEntry("deviceName", "示波器");
	}

	@Test
	void convertTemplateDoToDomain() {
		MsgTemplateDO templateDO = new MsgTemplateDO();
		templateDO.setId(123L);
		templateDO.setTemplateCode("equipment.normal.loan.approved");
		templateDO.setTemplateName("设备借用审批结果");
		templateDO.setMessageType("equipment.normal.loan.approved");
		templateDO.setTitleTemplate("审批结果");
		templateDO.setContentTemplate("你的设备借用申请已有结果");
		templateDO.setContentFormat(ContentFormat.MARKDOWN.getCode());
		templateDO.setParamSchema("{\"version\":1}");
		templateDO.setStatus(TemplateStatus.ENABLED.getCode());

		MessageTemplate template = MessageInboxConverter.toMessageTemplate(templateDO);

		assertThat(template.getId()).isEqualTo(123L);
		assertThat(template.getContentFormat()).isEqualTo(ContentFormat.MARKDOWN);
		assertThat(template.getStatus()).isEqualTo(TemplateStatus.ENABLED);
		assertThat(template.getParamSchema()).isEqualTo("{\"version\":1}");
	}

	@Test
	void convertInboxMessageBooleanFieldsToDatabaseCode() {
		LocalDateTime receivedAt = LocalDateTime.of(2026, 7, 8, 10, 30);
		UserInboxMessage unreadMessage = UserInboxMessage.unread(88L, 1001L, receivedAt);

		UserInboxMessageDO unreadMessageDO = MessageInboxConverter.toUserInboxMessageDO(unreadMessage);

		assertThat(unreadMessageDO.getMessageId()).isEqualTo(88L);
		assertThat(unreadMessageDO.getUserId()).isEqualTo(1001L);
		assertThat(unreadMessageDO.getIsRead()).isZero();
		assertThat(unreadMessageDO.getIsDeleted()).isZero();
		assertThat(unreadMessageDO.getReceivedAt()).isEqualTo(receivedAt);

		UserInboxMessage deletedReadMessage = UserInboxMessage.builder()
				.messageId(88L)
				.userId(1002L)
				.read(true)
				.deleted(true)
				.receivedAt(receivedAt)
				.build();
		UserInboxMessageDO deletedReadMessageDO = MessageInboxConverter.toUserInboxMessageDO(deletedReadMessage);

		assertThat(deletedReadMessageDO.getIsRead()).isEqualTo(1);
		assertThat(deletedReadMessageDO.getIsDeleted()).isEqualTo(1);
	}

	@Test
	void convertDeadLetterDomainAndDo() {
		MessageDeadLetter deadLetter = MessageDeadLetter.builder()
				.eventId(88L)
				.eventNo("equipment.normal.loan.approved:2026070800000001")
				.failureStage(FailureStage.RENDER)
				.errorCode("60002")
				.errorMessage("参数错误")
				.rawPayload("{\"eventNo\":\"equipment.normal.loan.approved:2026070800000001\"}")
				.retryable(true)
				.status(DeadLetterStatus.PENDING)
				.build();

		MsgDeadLetterDO deadLetterDO = MessageInboxConverter.toMsgDeadLetterDO(deadLetter);

		assertThat(deadLetterDO.getFailureStage()).isEqualTo(FailureStage.RENDER.name());
		assertThat(deadLetterDO.getRetryable()).isEqualTo(1);
		assertThat(deadLetterDO.getStatus()).isEqualTo(DeadLetterStatus.PENDING.getCode());

		MessageDeadLetter convertedDeadLetter = MessageInboxConverter.toMessageDeadLetter(deadLetterDO);

		assertThat(convertedDeadLetter.getFailureStage()).isEqualTo(FailureStage.RENDER);
		assertThat(convertedDeadLetter.isRetryable()).isTrue();
		assertThat(convertedDeadLetter.getStatus()).isEqualTo(DeadLetterStatus.PENDING);
	}

	@Test
	void convertTempTemplateToEventDoJson() throws Exception {
		MessageEvent event = MessageEvent.builder()
				.eventNo("equipment.normal.loan.approved:2026070800000001")
				.sourceSystem("equipment")
				.messageType("equipment.normal.loan.approved")
				.priority(MessagePriority.NORMAL)
				.occurredAt(LocalDateTime.of(2026, 7, 8, 11, 0))
				.targetType(TargetType.SINGLE)
				.targetUserIds(List.of(1001L))
				.tempTemplate(TempMessageTemplate.builder()
						.titleTemplate("审批结果")
						.contentTemplate("{{deviceName}} 已通过")
						.contentFormat(ContentFormat.TEXT)
						.paramSchema("{\"version\":1}")
						.build())
				.params(Map.of("deviceName", "示波器"))
				.contentFormat(ContentFormat.TEXT)
				.dispatchStatus(DispatchStatus.PROCESSING)
				.retryCount(0)
				.build();

		MsgEventDO eventDO = MessageInboxConverter.toMsgEventDO(event, objectMapper);
		TempMessageTemplate tempTemplate = objectMapper.readValue(eventDO.getTempTemplate(), TempMessageTemplate.class);

		assertThat(tempTemplate.getTitleTemplate()).isEqualTo("审批结果");
		assertThat(tempTemplate.getContentFormat()).isEqualTo(ContentFormat.TEXT);
	}
}
