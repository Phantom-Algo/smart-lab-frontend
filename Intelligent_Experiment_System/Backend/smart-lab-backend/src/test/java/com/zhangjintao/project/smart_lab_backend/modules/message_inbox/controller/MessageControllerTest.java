package com.zhangjintao.project.smart_lab_backend.modules.message_inbox.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhangjintao.project.smart_lab_backend.common.result.ApiResult;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.enums.ContentFormat;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.enums.TargetType;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.model.dto.MessagePublishCommand;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.model.dto.MessagePublishResult;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.model.request.PublishMessageRequest;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.model.response.PublishMessageResponse;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.service.MessagePublisher;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class MessageControllerTest {

	@Test
	void publishWithDirectContentDelegatesToMessagePublisher() {
		RecordingMessagePublisher messagePublisher = new RecordingMessagePublisher();
		MessageController controller = new MessageController(messagePublisher);
		PublishMessageRequest request = new PublishMessageRequest();
		request.setMessageType("message_inbox.normal.admin_publish");
		request.setTargetType("all");
		request.setTitle("系统通知");
		request.setContent("实验室开放时间调整");
		request.setContentFormat(ContentFormat.MARKDOWN);
		request.setActionUrl("/messages");

		ApiResult<PublishMessageResponse> response = controller.publish(request);

		assertThat(response.getCode()).isEqualTo(ApiResult.SUCCESS_CODE);
		assertThat(response.getData().getEventNo()).isEqualTo(messagePublisher.command.getEventNo());
		assertThat(response.getData().getStreamRecordId()).isEqualTo("1700000000000-0");
		assertThat(messagePublisher.command.getEventNo()).startsWith("message_inbox.normal.admin_publish:");
		assertThat(messagePublisher.command.getSourceSystem()).isEqualTo("message_inbox");
		assertThat(messagePublisher.command.getMessageType()).isEqualTo("message_inbox.normal.admin_publish");
		assertThat(messagePublisher.command.getTargetType()).isEqualTo(TargetType.ALL);
		assertThat(messagePublisher.command.getTemplateCode()).isNull();
		assertThat(messagePublisher.command.getTempTemplate().getTitleTemplate()).isEqualTo("系统通知");
		assertThat(messagePublisher.command.getTempTemplate().getContentTemplate()).isEqualTo("实验室开放时间调整");
		assertThat(messagePublisher.command.getTempTemplate().getContentFormat()).isEqualTo(ContentFormat.MARKDOWN);
		assertThat(messagePublisher.command.getParams()).isNull();
		assertThat(messagePublisher.command.getActionUrl()).isEqualTo("/messages");
	}

	@Test
	void publishWithTemplateCodeDelegatesTemplateAndParamsToMessagePublisher() {
		RecordingMessagePublisher messagePublisher = new RecordingMessagePublisher();
		MessageController controller = new MessageController(messagePublisher);
		Map<String, Object> params = Map.of("labName", "计算机网络实验室");
		PublishMessageRequest request = new PublishMessageRequest();
		request.setMessageType("message_inbox.normal.admin_publish");
		request.setTargetType("multi");
		request.setTargetUserIds(List.of(1001L, 1002L));
		request.setTemplateCode("message_inbox.normal.lab_notice");
		request.setParams(params);

		controller.publish(request);

		assertThat(messagePublisher.command.getTargetType()).isEqualTo(TargetType.MULTI);
		assertThat(messagePublisher.command.getTargetUserIds()).containsExactly(1001L, 1002L);
		assertThat(messagePublisher.command.getTemplateCode()).isEqualTo("message_inbox.normal.lab_notice");
		assertThat(messagePublisher.command.getParams()).isEqualTo(params);
		assertThat(messagePublisher.command.getTempTemplate()).isNull();
	}

	@Test
	void publishRequiresAdminRole() throws NoSuchMethodException {
		Method publishMethod = MessageController.class.getMethod("publish", PublishMessageRequest.class);

		PreAuthorize preAuthorize = publishMethod.getAnnotation(PreAuthorize.class);

		assertThat(preAuthorize).isNotNull();
		assertThat(preAuthorize.value()).isEqualTo("hasRole('ADMIN')");
	}

	@Test
	void requestSupportsSnakeCaseTemplateCode() throws Exception {
		ObjectMapper objectMapper = new ObjectMapper();

		PublishMessageRequest request = objectMapper.readValue("""
				{
				  "message_type": "message_inbox.normal.admin_publish",
				  "target_type": "all",
				  "template_code": "message_inbox.normal.lab_notice",
				  "params": {
				    "labName": "计算机网络实验室"
				  }
				}
				""", PublishMessageRequest.class);

		assertThat(request.getMessageType()).isEqualTo("message_inbox.normal.admin_publish");
		assertThat(request.getTargetType()).isEqualTo("all");
		assertThat(request.getTemplateCode()).isEqualTo("message_inbox.normal.lab_notice");
		assertThat(request.getParams()).containsEntry("labName", "计算机网络实验室");
	}

	@Test
	void requestRejectsTemplateCodeAndDirectContentTogether() {
		Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
		PublishMessageRequest request = new PublishMessageRequest();
		request.setMessageType("message_inbox.normal.admin_publish");
		request.setTargetType("all");
		request.setTemplateCode("message_inbox.normal.lab_notice");
		request.setTitle("系统通知");
		request.setContent("实验室开放时间调整");

		assertThat(validator.validate(request))
				.extracting(violation -> violation.getMessage())
				.contains("templateCode 不能与 title/content 同时指定");
	}

	private static class RecordingMessagePublisher implements MessagePublisher {

		private MessagePublishCommand command;

		@Override
		public MessagePublishResult publish(MessagePublishCommand command) {
			this.command = command;
			return MessagePublishResult.builder()
					.eventNo(command.getEventNo())
					.streamRecordId("1700000000000-0")
					.build();
		}
	}
}
