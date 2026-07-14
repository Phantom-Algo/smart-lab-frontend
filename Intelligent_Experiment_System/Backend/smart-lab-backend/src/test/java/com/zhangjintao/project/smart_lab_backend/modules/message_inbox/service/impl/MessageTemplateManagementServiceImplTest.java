package com.zhangjintao.project.smart_lab_backend.modules.message_inbox.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhangjintao.project.smart_lab_backend.common.result.PageResult;
import com.zhangjintao.project.smart_lab_backend.modules.auth_profile.model.dto.AuthenticatedUser;
import com.zhangjintao.project.smart_lab_backend.modules.auth_profile.model.dto.UserAccountWithProfileDTO;
import com.zhangjintao.project.smart_lab_backend.modules.auth_profile.service.CurrentUserQueryService;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.dao.repository.MsgTemplateRepository;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.domain.entity.MessageTemplate;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.enums.ContentFormat;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.enums.TemplateStatus;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.exception.MessageInboxErrorCode;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.exception.MessageInboxException;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.model.request.CreateMessageTemplateRequest;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.model.request.MessageTemplatePageQueryRequest;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.model.request.UpdateMessageTemplateRequest;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.model.response.MessageTemplateResponse;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MessageTemplateManagementServiceImplTest {

	private final ObjectMapper objectMapper = new ObjectMapper();

	@Test
	void createUsesDefaultContentFormatStatusAndAuditUser() {
		RecordingMsgTemplateRepository repository = new RecordingMsgTemplateRepository();
		MessageTemplateManagementServiceImpl service = service(repository);
		CreateMessageTemplateRequest request = createRequest();

		MessageTemplateResponse response = service.create(request);

		assertThat(response.getId()).isEqualTo(1L);
		assertThat(response.getContentFormat()).isEqualTo(ContentFormat.TEXT);
		assertThat(response.getStatus()).isEqualTo(TemplateStatus.ENABLED.getCode());
		assertThat(response.getCreatedBy()).isEqualTo(77L);
		assertThat(response.getUpdatedBy()).isEqualTo(77L);
		assertThat(repository.insertedTemplate.getContentFormat()).isEqualTo(ContentFormat.TEXT);
		assertThat(repository.insertedTemplate.getStatus()).isEqualTo(TemplateStatus.ENABLED);
		assertThat(repository.insertedTemplate.getParamSchema()).isNull();
	}

	@Test
	void createRejectsDuplicateTemplateCode() {
		RecordingMsgTemplateRepository repository = new RecordingMsgTemplateRepository();
		repository.existsByCode = true;
		MessageTemplateManagementServiceImpl service = service(repository);

		MessageInboxException exception = assertThrows(MessageInboxException.class,
				() -> service.create(createRequest()));

		assertThat(exception.getCode()).isEqualTo(MessageInboxErrorCode.TEMPLATE_ALREADY_EXISTS.getCode());
	}

	@Test
	void updateMissingTemplateThrowsTemplateNotFound() {
		RecordingMsgTemplateRepository repository = new RecordingMsgTemplateRepository();
		MessageTemplateManagementServiceImpl service = service(repository);

		MessageInboxException exception = assertThrows(MessageInboxException.class,
				() -> service.update(99L, updateRequest()));

		assertThat(exception.getCode()).isEqualTo(MessageInboxErrorCode.TEMPLATE_NOT_FOUND.getCode());
	}

	@Test
	void updateMergesProvidedFieldsAndKeepsExistingValues() throws Exception {
		RecordingMsgTemplateRepository repository = new RecordingMsgTemplateRepository();
		repository.templates.put(5L, MessageTemplate.builder()
				.id(5L)
				.templateCode("message_inbox.normal.lab_notice")
				.templateName("实验室通知")
				.messageType("message_inbox.normal.lab_notice")
				.titleTemplate("{{labName}} 通知")
				.contentTemplate("实验室开放时间调整")
				.contentFormat(ContentFormat.MARKDOWN)
				.paramSchema(labNameParamSchemaText())
				.status(TemplateStatus.DISABLED)
				.createdBy(66L)
				.updatedBy(66L)
				.build());
		MessageTemplateManagementServiceImpl service = service(repository);
		UpdateMessageTemplateRequest request = new UpdateMessageTemplateRequest();
		request.setTemplateName("实验室通知-新版");

		MessageTemplateResponse response = service.update(5L, request);
		MessageTemplate updatedTemplate = repository.templates.get(5L);

		assertThat(response.getTemplateName()).isEqualTo("实验室通知-新版");
		assertThat(updatedTemplate.getTemplateName()).isEqualTo("实验室通知-新版");
		assertThat(updatedTemplate.getMessageType()).isEqualTo("message_inbox.normal.lab_notice");
		assertThat(updatedTemplate.getTitleTemplate()).isEqualTo("{{labName}} 通知");
		assertThat(updatedTemplate.getContentTemplate()).isEqualTo("实验室开放时间调整");
		assertThat(updatedTemplate.getContentFormat()).isEqualTo(ContentFormat.MARKDOWN);
		assertThat(updatedTemplate.getParamSchema()).contains("\"labName\"");
		assertThat(updatedTemplate.getStatus()).isEqualTo(TemplateStatus.DISABLED);
		assertThat(updatedTemplate.getCreatedBy()).isEqualTo(66L);
		assertThat(updatedTemplate.getUpdatedBy()).isEqualTo(77L);
	}

	@Test
	void updateRejectsClearingParamSchemaWhenTemplateStillHasPlaceholders() throws Exception {
		RecordingMsgTemplateRepository repository = new RecordingMsgTemplateRepository();
		repository.templates.put(5L, MessageTemplate.builder()
				.id(5L)
				.templateCode("message_inbox.normal.lab_notice")
				.templateName("实验室通知")
				.messageType("message_inbox.normal.lab_notice")
				.titleTemplate("{{labName}} 通知")
				.contentTemplate("实验室开放时间调整")
				.contentFormat(ContentFormat.TEXT)
				.paramSchema(labNameParamSchemaText())
				.status(TemplateStatus.ENABLED)
				.createdBy(77L)
				.updatedBy(77L)
				.build());
		MessageTemplateManagementServiceImpl service = service(repository);
		UpdateMessageTemplateRequest request = new UpdateMessageTemplateRequest();
		request.setParamSchema(null);

		MessageInboxException exception = assertThrows(MessageInboxException.class,
				() -> service.update(5L, request));

		assertThat(exception.getCode()).isEqualTo(MessageInboxErrorCode.TEMPLATE_PARAM_INVALID.getCode());
		assertThat(exception.getMessage()).contains("paramSchema 为空时不允许模板包含占位符");
	}

	@Test
	void deleteDisablesTemplateInsteadOfPhysicalDelete() {
		RecordingMsgTemplateRepository repository = new RecordingMsgTemplateRepository();
		repository.templates.put(9L, template(9L));
		MessageTemplateManagementServiceImpl service = service(repository);

		service.delete(9L);

		assertThat(repository.disabledId).isEqualTo(9L);
		assertThat(repository.disabledUpdatedBy).isEqualTo(77L);
		assertThat(repository.templates.get(9L).getStatus()).isEqualTo(TemplateStatus.DISABLED);
	}

	@Test
	void createRejectsPlaceholderNotDeclaredInParamSchema() throws Exception {
		RecordingMsgTemplateRepository repository = new RecordingMsgTemplateRepository();
		MessageTemplateManagementServiceImpl service = service(repository);
		CreateMessageTemplateRequest request = createRequest();
		request.setTitleTemplate("{{labName}} 通知");
		request.setParamSchema(objectMapper.readTree("""
				{
				  "properties": {
				  }
				}
				"""));

		MessageInboxException exception = assertThrows(MessageInboxException.class,
				() -> service.create(request));

		assertThat(exception.getCode()).isEqualTo(MessageInboxErrorCode.TEMPLATE_PARAM_INVALID.getCode());
		assertThat(exception.getMessage()).contains("模板占位符未在 paramSchema.properties 中声明");
	}

	@Test
	void createSerializesParamSchemaAndResponseReturnsJsonObject() throws Exception {
		RecordingMsgTemplateRepository repository = new RecordingMsgTemplateRepository();
		MessageTemplateManagementServiceImpl service = service(repository);
		CreateMessageTemplateRequest request = createRequest();
		request.setTitleTemplate("{{labName}} 通知");
		request.setParamSchema(objectMapper.readTree("""
				{
				  "properties": {
				    "labName": {
				      "type": "string"
				    }
				  },
				  "required": ["labName"]
				}
				"""));

		MessageTemplateResponse response = service.create(request);

		assertThat(repository.insertedTemplate.getParamSchema()).contains("\"labName\"");
		assertThat(response.getParamSchema().path("properties").has("labName")).isTrue();
	}

	@Test
	void pageConvertsDomainRecordsToResponsePage() {
		RecordingMsgTemplateRepository repository = new RecordingMsgTemplateRepository();
		repository.pageResult = PageResult.of(List.of(template(11L)), 1, 2, 20);
		MessageTemplateManagementServiceImpl service = service(repository);
		MessageTemplatePageQueryRequest request = new MessageTemplatePageQueryRequest();
		request.setPageNum(2L);
		request.setPageSize(20L);
		request.setStatus(TemplateStatus.ENABLED.getCode());

		PageResult<MessageTemplateResponse> response = service.page(request);

		assertThat(repository.pageNum).isEqualTo(2L);
		assertThat(repository.pageSize).isEqualTo(20L);
		assertThat(repository.pageStatus).isEqualTo(TemplateStatus.ENABLED);
		assertThat(response.getRecords()).hasSize(1);
		assertThat(response.getRecords().get(0).getId()).isEqualTo(11L);
	}

	private MessageTemplateManagementServiceImpl service(RecordingMsgTemplateRepository repository) {
		return new MessageTemplateManagementServiceImpl(objectMapper, repository, new FixedCurrentUserQueryService());
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

	private String labNameParamSchemaText() throws Exception {
		return objectMapper.writeValueAsString(objectMapper.readTree("""
				{
				  "properties": {
				    "labName": {
				      "type": "string"
				    }
				  },
				  "required": ["labName"]
				}
				"""));
	}

	private static MessageTemplate template(Long id) {
		return MessageTemplate.builder()
				.id(id)
				.templateCode("message_inbox.normal.lab_notice")
				.templateName("实验室通知")
				.messageType("message_inbox.normal.lab_notice")
				.titleTemplate("实验室通知")
				.contentTemplate("实验室开放时间调整")
				.contentFormat(ContentFormat.TEXT)
				.status(TemplateStatus.ENABLED)
				.createdBy(77L)
				.updatedBy(77L)
				.build();
	}

	private static class FixedCurrentUserQueryService implements CurrentUserQueryService {

		@Override
		public AuthenticatedUser requireCurrentUser() {
			return AuthenticatedUser.builder()
					.userId(77L)
					.username("admin")
					.build();
		}

		@Override
		public UserAccountWithProfileDTO requireCurrentUserWithProfile() {
			throw new UnsupportedOperationException("当前测试不需要该方法");
		}
	}

	private static class RecordingMsgTemplateRepository implements MsgTemplateRepository {

		private final Map<Long, MessageTemplate> templates = new LinkedHashMap<>();
		private boolean existsByCode;
		private long nextId = 1L;
		private MessageTemplate insertedTemplate;
		private Long disabledId;
		private Long disabledUpdatedBy;
		private PageResult<MessageTemplate> pageResult = PageResult.empty(1, 10);
		private long pageNum;
		private long pageSize;
		private TemplateStatus pageStatus;

		@Override
		public Optional<MessageTemplate> findEnabledById(Long id) {
			return Optional.ofNullable(templates.get(id))
					.filter(template -> template.getStatus() == TemplateStatus.ENABLED);
		}

		@Override
		public Optional<MessageTemplate> findEnabledByCode(String templateCode) {
			return templates.values().stream()
					.filter(template -> template.getStatus() == TemplateStatus.ENABLED)
					.filter(template -> template.getTemplateCode().equals(templateCode))
					.findFirst();
		}

		@Override
		public Optional<MessageTemplate> findById(Long id) {
			return Optional.ofNullable(templates.get(id));
		}

		@Override
		public Optional<MessageTemplate> findByCode(String templateCode) {
			return templates.values().stream()
					.filter(template -> template.getTemplateCode().equals(templateCode))
					.findFirst();
		}

		@Override
		public boolean existsByCode(String templateCode) {
			return existsByCode || findByCode(templateCode).isPresent();
		}

		@Override
		public PageResult<MessageTemplate> page(long pageNum, long pageSize, String templateCode, String templateName,
				String messageType, TemplateStatus status) {
			this.pageNum = pageNum;
			this.pageSize = pageSize;
			this.pageStatus = status;
			return pageResult;
		}

		@Override
		public MessageTemplate insert(MessageTemplate template) {
			MessageTemplate savedTemplate = MessageTemplate.builder()
					.id(nextId++)
					.templateCode(template.getTemplateCode())
					.templateName(template.getTemplateName())
					.messageType(template.getMessageType())
					.titleTemplate(template.getTitleTemplate())
					.contentTemplate(template.getContentTemplate())
					.contentFormat(template.getContentFormat())
					.paramSchema(template.getParamSchema())
					.status(template.getStatus())
					.createdBy(template.getCreatedBy())
					.updatedBy(template.getUpdatedBy())
					.build();
			this.insertedTemplate = savedTemplate;
			templates.put(savedTemplate.getId(), savedTemplate);
			return savedTemplate;
		}

		@Override
		public MessageTemplate update(MessageTemplate template) {
			templates.put(template.getId(), template);
			return template;
		}

		@Override
		public void disable(Long id, Long updatedBy) {
			this.disabledId = id;
			this.disabledUpdatedBy = updatedBy;
			MessageTemplate template = templates.get(id);
			templates.put(id, MessageTemplate.builder()
					.id(template.getId())
					.templateCode(template.getTemplateCode())
					.templateName(template.getTemplateName())
					.messageType(template.getMessageType())
					.titleTemplate(template.getTitleTemplate())
					.contentTemplate(template.getContentTemplate())
					.contentFormat(template.getContentFormat())
					.paramSchema(template.getParamSchema())
					.status(TemplateStatus.DISABLED)
					.createdBy(template.getCreatedBy())
					.updatedBy(updatedBy)
					.build());
		}
	}
}
