package com.zhangjintao.project.smart_lab_backend.modules.message_inbox.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhangjintao.project.smart_lab_backend.common.result.ApiResult;
import com.zhangjintao.project.smart_lab_backend.common.result.PageResult;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.enums.ContentFormat;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.model.request.CreateMessageTemplateRequest;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.model.request.MessageTemplatePageQueryRequest;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.model.request.UpdateMessageTemplateRequest;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.model.response.MessageTemplateResponse;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.service.MessageTemplateManagementService;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.reflect.Method;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MessageTemplateControllerTest {

	@Test
	void controllerRequiresAdminRole() {
		PreAuthorize preAuthorize = MessageTemplateController.class.getAnnotation(PreAuthorize.class);

		assertThat(preAuthorize).isNotNull();
		assertThat(preAuthorize.value()).isEqualTo("hasRole('ADMIN')");
	}

	@Test
	void createDelegatesToService() {
		RecordingMessageTemplateManagementService service = new RecordingMessageTemplateManagementService();
		MessageTemplateController controller = new MessageTemplateController(service);
		CreateMessageTemplateRequest request = createRequest();

		ApiResult<MessageTemplateResponse> result = controller.create(request);

		assertThat(service.createRequest).isSameAs(request);
		assertThat(result.getData().getId()).isEqualTo(1L);
	}

	@Test
	void getByIdDelegatesToService() {
		RecordingMessageTemplateManagementService service = new RecordingMessageTemplateManagementService();
		MessageTemplateController controller = new MessageTemplateController(service);

		controller.getById(7L);

		assertThat(service.getById).isEqualTo(7L);
	}

	@Test
	void pageDelegatesToService() {
		RecordingMessageTemplateManagementService service = new RecordingMessageTemplateManagementService();
		MessageTemplateController controller = new MessageTemplateController(service);
		MessageTemplatePageQueryRequest request = new MessageTemplatePageQueryRequest();
		request.setPageNum(2L);
		request.setPageSize(20L);

		ApiResult<PageResult<MessageTemplateResponse>> result = controller.page(request);

		assertThat(service.pageRequest).isSameAs(request);
		assertThat(result.getData().getPageNum()).isEqualTo(2L);
	}

	@Test
	void updateDelegatesToService() {
		RecordingMessageTemplateManagementService service = new RecordingMessageTemplateManagementService();
		MessageTemplateController controller = new MessageTemplateController(service);
		UpdateMessageTemplateRequest request = updateRequest();

		controller.update(3L, request);

		assertThat(service.updateId).isEqualTo(3L);
		assertThat(service.updateRequest).isSameAs(request);
	}

	@Test
	void deleteDelegatesToService() {
		RecordingMessageTemplateManagementService service = new RecordingMessageTemplateManagementService();
		MessageTemplateController controller = new MessageTemplateController(service);

		controller.delete(9L);

		assertThat(service.deleteId).isEqualTo(9L);
	}

	@Test
	void createRequestSupportsSnakeCaseFields() throws Exception {
		ObjectMapper objectMapper = new ObjectMapper();

		CreateMessageTemplateRequest request = objectMapper.readValue("""
				{
				  "template_code": "message_inbox.normal.lab_notice",
				  "template_name": "实验室通知",
				  "message_type": "message_inbox.normal.lab_notice",
				  "title_template": "实验室通知",
				  "content_template": "{{labName}} 开放时间调整",
				  "content_format": "MARKDOWN",
				  "param_schema": {
				    "properties": {
				      "labName": {
				        "type": "string"
				      }
				    }
				  }
				}
				""", CreateMessageTemplateRequest.class);

		assertThat(request.getTemplateCode()).isEqualTo("message_inbox.normal.lab_notice");
		assertThat(request.getTemplateName()).isEqualTo("实验室通知");
		assertThat(request.getMessageType()).isEqualTo("message_inbox.normal.lab_notice");
		assertThat(request.getTitleTemplate()).isEqualTo("实验室通知");
		assertThat(request.getContentTemplate()).isEqualTo("{{labName}} 开放时间调整");
		assertThat(request.getContentFormat()).isEqualTo(ContentFormat.MARKDOWN);
		assertThat(request.getParamSchema().path("properties").has("labName")).isTrue();
	}

	@Test
	void updateRequestSupportsPartialSnakeCaseFieldsAndParamSchemaPresence() throws Exception {
		ObjectMapper objectMapper = new ObjectMapper();

		UpdateMessageTemplateRequest partialRequest = objectMapper.readValue("""
				{
				  "template_name": "实验室通知-新版"
				}
				""", UpdateMessageTemplateRequest.class);
		UpdateMessageTemplateRequest clearSchemaRequest = objectMapper.readValue("""
				{
				  "param_schema": null
				}
				""", UpdateMessageTemplateRequest.class);

		assertThat(partialRequest.getTemplateName()).isEqualTo("实验室通知-新版");
		assertThat(partialRequest.getMessageType()).isNull();
		assertThat(partialRequest.isParamSchemaSpecified()).isFalse();
		assertThat(clearSchemaRequest.isParamSchemaSpecified()).isTrue();
		assertThat(clearSchemaRequest.getParamSchema().isNull()).isTrue();
	}

	@Test
	void hasCrudMethods() throws NoSuchMethodException {
		Method create = MessageTemplateController.class.getMethod("create", CreateMessageTemplateRequest.class);
		Method getById = MessageTemplateController.class.getMethod("getById", Long.class);
		Method page = MessageTemplateController.class.getMethod("page", MessageTemplatePageQueryRequest.class);
		Method update = MessageTemplateController.class.getMethod("update", Long.class, UpdateMessageTemplateRequest.class);
		Method delete = MessageTemplateController.class.getMethod("delete", Long.class);

		assertThat(create).isNotNull();
		assertThat(getById).isNotNull();
		assertThat(page).isNotNull();
		assertThat(update).isNotNull();
		assertThat(delete).isNotNull();
	}

	private CreateMessageTemplateRequest createRequest() {
		CreateMessageTemplateRequest request = new CreateMessageTemplateRequest();
		request.setTemplateCode("message_inbox.normal.lab_notice");
		request.setTemplateName("实验室通知");
		request.setMessageType("message_inbox.normal.lab_notice");
		request.setTitleTemplate("实验室通知");
		request.setContentTemplate("实验室开放时间调整");
		return request;
	}

	private UpdateMessageTemplateRequest updateRequest() {
		UpdateMessageTemplateRequest request = new UpdateMessageTemplateRequest();
		request.setTemplateName("实验室通知");
		request.setMessageType("message_inbox.normal.lab_notice");
		request.setTitleTemplate("实验室通知");
		request.setContentTemplate("实验室开放时间调整");
		return request;
	}

	private static class RecordingMessageTemplateManagementService implements MessageTemplateManagementService {

		private CreateMessageTemplateRequest createRequest;
		private Long getById;
		private MessageTemplatePageQueryRequest pageRequest;
		private Long updateId;
		private UpdateMessageTemplateRequest updateRequest;
		private Long deleteId;

		@Override
		public MessageTemplateResponse create(CreateMessageTemplateRequest request) {
			this.createRequest = request;
			return response(1L);
		}

		@Override
		public MessageTemplateResponse getById(Long id) {
			this.getById = id;
			return response(id);
		}

		@Override
		public PageResult<MessageTemplateResponse> page(MessageTemplatePageQueryRequest request) {
			this.pageRequest = request;
			return PageResult.of(List.of(response(1L)), 1, request.getPageNum(), request.getPageSize());
		}

		@Override
		public MessageTemplateResponse update(Long id, UpdateMessageTemplateRequest request) {
			this.updateId = id;
			this.updateRequest = request;
			return response(id);
		}

		@Override
		public void delete(Long id) {
			this.deleteId = id;
		}

		private MessageTemplateResponse response(Long id) {
			return MessageTemplateResponse.builder()
					.id(id)
					.templateCode("message_inbox.normal.lab_notice")
					.templateName("实验室通知")
					.messageType("message_inbox.normal.lab_notice")
					.titleTemplate("实验室通知")
					.contentTemplate("实验室开放时间调整")
					.contentFormat(ContentFormat.TEXT)
					.status(1)
					.build();
		}
	}
}
