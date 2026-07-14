package com.zhangjintao.project.smart_lab_backend.modules.message_inbox.service.impl;

import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.dao.repository.MsgEventRepository;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.domain.entity.MessageEvent;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.domain.value.MessageContentSnapshot;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.enums.ContentFormat;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.enums.TargetType;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.model.dto.MessageDispatchResult;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.model.dto.MessagePublishCommand;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.model.dto.MessageTemplateSnapshot;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.model.dto.RenderedMessage;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.service.InboxDeliveryService;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.service.MessageTemplateService;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.service.RecipientResolveService;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.service.TemplateRenderService;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class MessageDispatchProcessorImplTest {

	@Test
	void processPersistsResolvedTemplateIdWhenCommandUsesTemplateCode() {
		RecordingMsgEventRepository msgEventRepository = new RecordingMsgEventRepository();
		FixedMessageTemplateService messageTemplateService = new FixedMessageTemplateService();
		FixedTemplateRenderService templateRenderService = new FixedTemplateRenderService();
		FixedRecipientResolveService recipientResolveService = new FixedRecipientResolveService();
		RecordingInboxDeliveryService inboxDeliveryService = new RecordingInboxDeliveryService();
		MessageDispatchProcessorImpl processor = new MessageDispatchProcessorImpl(msgEventRepository,
				command -> { }, messageTemplateService, templateRenderService, recipientResolveService, inboxDeliveryService);
		Map<String, Object> params = Map.of("equipmentName", "示波器");
		MessagePublishCommand command = MessagePublishCommand.builder()
				.eventNo("equipment.normal.loan.approved:2026070800000001")
				.sourceSystem("equipment")
				.messageType("equipment.normal.loan.approved")
				.targetType(TargetType.SINGLE)
				.targetUserIds(List.of(1001L))
				.templateCode("equipment.normal.loan.approved")
				.params(params)
				.build();
		messageTemplateService.template = MessageTemplateSnapshot.builder()
				.templateId(123L)
				.titleTemplate("审批通过")
				.contentTemplate("{{equipmentName}} 借用申请已通过")
				.contentFormat(ContentFormat.TEXT)
				.build();
		templateRenderService.renderedMessage = RenderedMessage.builder()
				.title("审批通过")
				.content("示波器 借用申请已通过")
				.contentFormat(ContentFormat.TEXT)
				.build();
		recipientResolveService.recipientUserIds = List.of(1001L);

		MessageDispatchResult result = processor.process(command);

		assertThat(msgEventRepository.insertedEvent.getTemplateId()).isEqualTo(123L);
		assertThat(result.isDuplicateCompleted()).isFalse();
		assertThat(result.getEventId()).isEqualTo(88L);
		assertThat(msgEventRepository.markProcessingId).isEqualTo(88L);
		assertThat(inboxDeliveryService.messageId).isEqualTo(88L);
		assertThat(inboxDeliveryService.recipientUserIds).containsExactly(1001L);
		assertThat(msgEventRepository.markCompletedId).isEqualTo(88L);
		assertThat(msgEventRepository.completedTitle).isEqualTo("审批通过");
		assertThat(msgEventRepository.completedContent).isEqualTo("示波器 借用申请已通过");
		assertThat(msgEventRepository.completedContentFormat).isEqualTo(ContentFormat.TEXT);
	}

	private static class RecordingMsgEventRepository implements MsgEventRepository {

		private MessageEvent insertedEvent;

		private Long markProcessingId;

		private Long markCompletedId;

		private String completedTitle;

		private String completedContent;

		private ContentFormat completedContentFormat;

		@Override
		public Optional<MessageEvent> findByEventNo(String eventNo) {
			return Optional.empty();
		}

		@Override
		public MessageEvent insert(MessageEvent messageEvent) {
			messageEvent.setId(88L);
			this.insertedEvent = messageEvent;
			return messageEvent;
		}

		@Override
		public void markProcessing(Long id) {
			this.markProcessingId = id;
		}

		@Override
		public void markCompleted(Long id, MessageContentSnapshot contentSnapshot) {
			this.markCompletedId = id;
			this.completedTitle = contentSnapshot.getTitle();
			this.completedContent = contentSnapshot.getContent();
			this.completedContentFormat = contentSnapshot.getContentFormat();
		}

		@Override
		public void markFailed(Long id) {
		}
	}

	private static class FixedMessageTemplateService implements MessageTemplateService {

		private MessageTemplateSnapshot template;

		@Override
		public MessageTemplateSnapshot resolveTemplate(MessagePublishCommand command) {
			return template;
		}
	}

	private static class FixedTemplateRenderService implements TemplateRenderService {

		private RenderedMessage renderedMessage;

		@Override
		public RenderedMessage render(MessageTemplateSnapshot template, Map<String, Object> params) {
			return renderedMessage;
		}
	}

	private static class FixedRecipientResolveService implements RecipientResolveService {

		private List<Long> recipientUserIds;

		@Override
		public List<Long> resolveRecipients(MessagePublishCommand command) {
			return recipientUserIds;
		}
	}

	private static class RecordingInboxDeliveryService implements InboxDeliveryService {

		private Long messageId;

		private List<Long> recipientUserIds;

		@Override
		public int deliver(Long messageId, List<Long> recipientUserIds) {
			this.messageId = messageId;
			this.recipientUserIds = recipientUserIds;
			return recipientUserIds.size();
		}
	}
}
