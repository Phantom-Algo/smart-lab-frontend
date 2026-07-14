package com.zhangjintao.project.smart_lab_backend.modules.message_inbox.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhangjintao.project.smart_lab_backend.common.result.ApiResult;
import com.zhangjintao.project.smart_lab_backend.common.result.PageResult;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.enums.ContentFormat;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.model.request.BatchDeleteInboxMessageRequest;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.model.request.InboxMessagePageQueryRequest;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.model.response.InboxMessageDetailResponse;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.model.response.InboxMessageListItemResponse;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.service.InboxMessageManagementService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class InboxMessageControllerTest {

	@Test
	void pageDelegatesToService() {
		RecordingInboxMessageManagementService service = new RecordingInboxMessageManagementService();
		InboxMessageController controller = new InboxMessageController(service);
		InboxMessagePageQueryRequest request = new InboxMessagePageQueryRequest();
		request.setPageNum(2L);
		request.setPageSize(20L);

		ApiResult<PageResult<InboxMessageListItemResponse>> result = controller.page(request);

		assertThat(service.pageMyInboxRequest).isSameAs(request);
		assertThat(result.getData().getPageNum()).isEqualTo(2L);
	}

	@Test
	void listNewIdsDelegatesToService() {
		RecordingInboxMessageManagementService service = new RecordingInboxMessageManagementService();
		InboxMessageController controller = new InboxMessageController(service);

		ApiResult<List<Long>> result = controller.listNewIds();

		assertThat(service.listMyNewInboxMessageIdsCalled).isTrue();
		assertThat(result.getData()).containsExactly(10L, 11L);
	}

	@Test
	void recordViewedDelegatesToService() {
		RecordingInboxMessageManagementService service = new RecordingInboxMessageManagementService();
		InboxMessageController controller = new InboxMessageController(service);

		ApiResult<Void> result = controller.recordViewed();

		assertThat(service.recordMyInboxViewedCalled).isTrue();
		assertThat(result.getData()).isNull();
	}

	@Test
	void getDetailDelegatesToService() {
		RecordingInboxMessageManagementService service = new RecordingInboxMessageManagementService();
		InboxMessageController controller = new InboxMessageController(service);

		ApiResult<InboxMessageDetailResponse> result = controller.getDetail(10L);

		assertThat(service.getMyMessageId).isEqualTo(10L);
		assertThat(result.getData().getInboxMessageId()).isEqualTo(10L);
	}

	@Test
	void deleteDelegatesToService() {
		RecordingInboxMessageManagementService service = new RecordingInboxMessageManagementService();
		InboxMessageController controller = new InboxMessageController(service);

		controller.delete(11L);

		assertThat(service.deleteMyMessageId).isEqualTo(11L);
	}

	@Test
	void deleteBatchDelegatesToService() {
		RecordingInboxMessageManagementService service = new RecordingInboxMessageManagementService();
		InboxMessageController controller = new InboxMessageController(service);
		BatchDeleteInboxMessageRequest request = new BatchDeleteInboxMessageRequest();
		request.setInboxMessageIds(List.of(1L, 2L));

		controller.deleteBatch(request);

		assertThat(service.deleteMyMessagesRequest).isSameAs(request);
	}

	@Test
	void batchDeleteRequestSupportsSnakeCaseFields() throws Exception {
		ObjectMapper objectMapper = new ObjectMapper();

		BatchDeleteInboxMessageRequest request = objectMapper.readValue("""
				{
				  "inbox_message_ids": [1, 2, 3]
				}
				""", BatchDeleteInboxMessageRequest.class);

		assertThat(request.getInboxMessageIds()).containsExactly(1L, 2L, 3L);
	}

	static class RecordingInboxMessageManagementService implements InboxMessageManagementService {

		InboxMessagePageQueryRequest pageMyInboxRequest;
		Long getMyMessageId;
		Long deleteMyMessageId;
		BatchDeleteInboxMessageRequest deleteMyMessagesRequest;
		boolean listMyNewInboxMessageIdsCalled;
		boolean recordMyInboxViewedCalled;
		Long pageUserId;
		InboxMessagePageQueryRequest pageUserInboxRequest;
		Long getUserId;
		Long getUserInboxMessageId;
		Long deleteUserId;
		Long deleteUserInboxMessageId;
		Long deleteBatchUserId;
		BatchDeleteInboxMessageRequest deleteUserMessagesRequest;

		@Override
		public PageResult<InboxMessageListItemResponse> pageMyInbox(InboxMessagePageQueryRequest request) {
			this.pageMyInboxRequest = request;
			return PageResult.of(List.of(listItem(1L)), 1, request.getPageNum(), request.getPageSize());
		}

		@Override
		public List<Long> listMyNewInboxMessageIds() {
			this.listMyNewInboxMessageIdsCalled = true;
			return List.of(10L, 11L);
		}

		@Override
		public void recordMyInboxViewed() {
			this.recordMyInboxViewedCalled = true;
		}

		@Override
		public InboxMessageDetailResponse getMyMessage(Long inboxMessageId) {
			this.getMyMessageId = inboxMessageId;
			return detail(inboxMessageId);
		}

		@Override
		public void deleteMyMessage(Long inboxMessageId) {
			this.deleteMyMessageId = inboxMessageId;
		}

		@Override
		public void deleteMyMessages(BatchDeleteInboxMessageRequest request) {
			this.deleteMyMessagesRequest = request;
		}

		@Override
		public PageResult<InboxMessageListItemResponse> pageUserInbox(Long userId, InboxMessagePageQueryRequest request) {
			this.pageUserId = userId;
			this.pageUserInboxRequest = request;
			return PageResult.of(List.of(listItem(1L)), 1, request.getPageNum(), request.getPageSize());
		}

		@Override
		public InboxMessageDetailResponse getUserMessage(Long userId, Long inboxMessageId) {
			this.getUserId = userId;
			this.getUserInboxMessageId = inboxMessageId;
			return detail(inboxMessageId);
		}

		@Override
		public void deleteUserMessage(Long userId, Long inboxMessageId) {
			this.deleteUserId = userId;
			this.deleteUserInboxMessageId = inboxMessageId;
		}

		@Override
		public void deleteUserMessages(Long userId, BatchDeleteInboxMessageRequest request) {
			this.deleteBatchUserId = userId;
			this.deleteUserMessagesRequest = request;
		}

		private InboxMessageListItemResponse listItem(Long id) {
			return InboxMessageListItemResponse.builder()
					.inboxMessageId(id)
					.messageId(100L)
					.title("实验室通知")
					.priority(3)
					.priorityDescription("普通通知")
					.messageType("message_inbox.normal.lab_notice")
					.read(false)
					.build();
		}

		private InboxMessageDetailResponse detail(Long id) {
			return InboxMessageDetailResponse.builder()
					.inboxMessageId(id)
					.messageId(100L)
					.title("实验室通知")
					.priority(3)
					.priorityDescription("普通通知")
					.messageType("message_inbox.normal.lab_notice")
					.read(false)
					.content("实验室开放时间调整")
					.contentFormat(ContentFormat.TEXT)
					.actionUrl("/messages")
					.build();
		}
	}
}
