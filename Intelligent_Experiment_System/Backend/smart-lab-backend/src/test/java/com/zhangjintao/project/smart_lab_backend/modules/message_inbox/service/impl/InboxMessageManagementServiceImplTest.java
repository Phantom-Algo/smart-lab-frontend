package com.zhangjintao.project.smart_lab_backend.modules.message_inbox.service.impl;

import com.zhangjintao.project.smart_lab_backend.common.result.PageResult;
import com.zhangjintao.project.smart_lab_backend.modules.auth_profile.enums.UserType;
import com.zhangjintao.project.smart_lab_backend.modules.auth_profile.model.dto.AuthenticatedUser;
import com.zhangjintao.project.smart_lab_backend.modules.auth_profile.model.dto.UserAccountWithProfileDTO;
import com.zhangjintao.project.smart_lab_backend.modules.auth_profile.service.CurrentUserQueryService;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.dao.repository.UserInboxMessageRepository;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.dao.repository.UserInboxVisitStatRepository;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.enums.ContentFormat;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.enums.MessagePriority;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.exception.MessageInboxErrorCode;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.exception.MessageInboxException;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.model.dto.InboxMessageQueryDTO;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.model.request.BatchDeleteInboxMessageRequest;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.model.request.InboxMessagePageQueryRequest;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.model.response.InboxMessageDetailResponse;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.model.response.InboxMessageListItemResponse;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InboxMessageManagementServiceImplTest {

	@Test
	void pageMyInboxUsesCurrentUserAndConvertsFilters() {
		RecordingUserInboxMessageRepository repository = new RecordingUserInboxMessageRepository();
		repository.pageResult = PageResult.of(List.of(message(10L, 100L, false)), 1, 2, 20);
		InboxMessageManagementServiceImpl service = service(repository);
		InboxMessagePageQueryRequest request = new InboxMessagePageQueryRequest();
		request.setPageNum(2L);
		request.setPageSize(20L);
		request.setPriority(MessagePriority.NORMAL.getCode());
		request.setMessageType("message_inbox.normal.lab_notice");
		request.setIsRead(0);

		PageResult<InboxMessageListItemResponse> result = service.pageMyInbox(request);

		assertThat(repository.pageUserId).isEqualTo(77L);
		assertThat(repository.pagePriority).isEqualTo(MessagePriority.NORMAL);
		assertThat(repository.pageMessageType).isEqualTo("message_inbox.normal.lab_notice");
		assertThat(repository.pageRead).isFalse();
		assertThat(result.getRecords().get(0).getPriorityDescription()).isEqualTo("普通通知");
	}

	@Test
	void listMyNewInboxMessageIdsUsesCurrentUserAndLastViewedAt() {
		RecordingUserInboxMessageRepository repository = new RecordingUserInboxMessageRepository();
		repository.newInboxMessageIds = List.of(10L, 11L);
		RecordingUserInboxVisitStatRepository visitStatRepository = new RecordingUserInboxVisitStatRepository();
		LocalDateTime lastViewedAt = LocalDateTime.now().minusMinutes(5);
		visitStatRepository.lastViewedAt = Optional.of(lastViewedAt);
		InboxMessageManagementServiceImpl service = service(repository, visitStatRepository);

		List<Long> newIds = service.listMyNewInboxMessageIds();

		assertThat(visitStatRepository.findLastViewedAtUserId).isEqualTo(77L);
		assertThat(repository.listNewUserId).isEqualTo(77L);
		assertThat(repository.listNewLastViewedAt).isEqualTo(lastViewedAt);
		assertThat(newIds).containsExactly(10L, 11L);
	}

	@Test
	void listMyNewInboxMessageIdsPassesNullWhenUserNeverViewedInbox() {
		RecordingUserInboxMessageRepository repository = new RecordingUserInboxMessageRepository();
		repository.newInboxMessageIds = List.of(10L, 11L, 12L);
		RecordingUserInboxVisitStatRepository visitStatRepository = new RecordingUserInboxVisitStatRepository();
		visitStatRepository.lastViewedAt = Optional.empty();
		InboxMessageManagementServiceImpl service = service(repository, visitStatRepository);

		List<Long> newIds = service.listMyNewInboxMessageIds();

		assertThat(repository.listNewUserId).isEqualTo(77L);
		assertThat(repository.listNewLastViewedAt).isNull();
		assertThat(newIds).containsExactly(10L, 11L, 12L);
	}

	@Test
	void recordMyInboxViewedUsesCurrentUserAndServerTime() {
		RecordingUserInboxVisitStatRepository visitStatRepository = new RecordingUserInboxVisitStatRepository();
		InboxMessageManagementServiceImpl service = service(new RecordingUserInboxMessageRepository(), visitStatRepository);
		LocalDateTime before = LocalDateTime.now();

		service.recordMyInboxViewed();

		LocalDateTime after = LocalDateTime.now();
		assertThat(visitStatRepository.upsertUserId).isEqualTo(77L);
		assertThat(visitStatRepository.upsertLastViewedAt).isBetween(before, after);
	}

	@Test
	void getMyUnreadMessageMarksReadAndReturnsReadDetail() {
		RecordingUserInboxMessageRepository repository = new RecordingUserInboxMessageRepository();
		repository.details.put(key(77L, 10L), message(10L, 100L, false));
		InboxMessageManagementServiceImpl service = service(repository);

		InboxMessageDetailResponse response = service.getMyMessage(10L);

		assertThat(repository.markReadUserId).isEqualTo(77L);
		assertThat(repository.markReadInboxMessageId).isEqualTo(10L);
		assertThat(response.isRead()).isTrue();
		assertThat(response.getReadAt()).isNotNull();
	}

	@Test
	void getUserMessageAsAdminDoesNotMarkRead() {
		RecordingUserInboxMessageRepository repository = new RecordingUserInboxMessageRepository();
		repository.details.put(key(99L, 10L), message(10L, 100L, false));
		InboxMessageManagementServiceImpl service = service(repository);

		InboxMessageDetailResponse response = service.getUserMessage(99L, 10L);

		assertThat(response.isRead()).isFalse();
		assertThat(repository.markReadInboxMessageId).isNull();
	}

	@Test
	void deleteMyMessageUsesCurrentUser() {
		RecordingUserInboxMessageRepository repository = new RecordingUserInboxMessageRepository();
		InboxMessageManagementServiceImpl service = service(repository);

		service.deleteMyMessage(10L);

		assertThat(repository.softDeleteUserId).isEqualTo(77L);
		assertThat(repository.softDeleteInboxMessageId).isEqualTo(10L);
	}

	@Test
	void deleteUserMessagesDeduplicatesAndDeletesAllValidMessages() {
		RecordingUserInboxMessageRepository repository = new RecordingUserInboxMessageRepository();
		repository.countActiveReturn = 2;
		InboxMessageManagementServiceImpl service = service(repository);
		BatchDeleteInboxMessageRequest request = new BatchDeleteInboxMessageRequest();
		request.setInboxMessageIds(List.of(10L, 10L, 11L));

		service.deleteUserMessages(99L, request);

		assertThat(repository.countActiveUserId).isEqualTo(99L);
		assertThat(repository.countActiveIds).containsExactly(10L, 11L);
		assertThat(repository.softDeleteBatchIds).containsExactly(10L, 11L);
	}

	@Test
	void deleteUserMessagesRejectsInvalidIdsBeforeDelete() {
		RecordingUserInboxMessageRepository repository = new RecordingUserInboxMessageRepository();
		repository.countActiveReturn = 1;
		InboxMessageManagementServiceImpl service = service(repository);
		BatchDeleteInboxMessageRequest request = new BatchDeleteInboxMessageRequest();
		request.setInboxMessageIds(List.of(10L, 11L));

		MessageInboxException exception = assertThrows(MessageInboxException.class,
				() -> service.deleteUserMessages(99L, request));

		assertThat(exception.getCode()).isEqualTo(MessageInboxErrorCode.INBOX_MESSAGE_NOT_FOUND.getCode());
		assertThat(repository.softDeleteBatchIds).isNull();
	}

	@Test
	void getMissingMessageThrowsInboxMessageNotFound() {
		RecordingUserInboxMessageRepository repository = new RecordingUserInboxMessageRepository();
		InboxMessageManagementServiceImpl service = service(repository);

		MessageInboxException exception = assertThrows(MessageInboxException.class,
				() -> service.getMyMessage(404L));

		assertThat(exception.getCode()).isEqualTo(MessageInboxErrorCode.INBOX_MESSAGE_NOT_FOUND.getCode());
	}

	private InboxMessageManagementServiceImpl service(RecordingUserInboxMessageRepository repository) {
		return service(repository, new RecordingUserInboxVisitStatRepository());
	}

	private InboxMessageManagementServiceImpl service(RecordingUserInboxMessageRepository repository,
			RecordingUserInboxVisitStatRepository visitStatRepository) {
		return new InboxMessageManagementServiceImpl(repository, visitStatRepository, new FixedCurrentUserQueryService());
	}

	private static InboxMessageQueryDTO message(Long inboxMessageId, Long messageId, boolean read) {
		InboxMessageQueryDTO message = new InboxMessageQueryDTO();
		message.setInboxMessageId(inboxMessageId);
		message.setMessageId(messageId);
		message.setTitle("实验室通知");
		message.setPriority(MessagePriority.NORMAL.getCode());
		message.setMessageType("message_inbox.normal.lab_notice");
		message.setSentAt(LocalDateTime.now().minusMinutes(10));
		message.setReceivedAt(LocalDateTime.now());
		message.setReadFlag(read ? 1 : 0);
		message.setReadAt(read ? LocalDateTime.now() : null);
		message.setContent("实验室开放时间调整");
		message.setContentFormat(ContentFormat.TEXT.getCode());
		message.setActionUrl("/messages");
		return message;
	}

	private static String key(Long userId, Long inboxMessageId) {
		return userId + ":" + inboxMessageId;
	}

	private static class FixedCurrentUserQueryService implements CurrentUserQueryService {

		@Override
		public AuthenticatedUser requireCurrentUser() {
			return AuthenticatedUser.builder()
					.userId(77L)
					.username("student")
					.userType(UserType.STUDENT)
					.build();
		}

		@Override
		public UserAccountWithProfileDTO requireCurrentUserWithProfile() {
			throw new UnsupportedOperationException("当前测试不需要该方法");
		}
	}

	private static class RecordingUserInboxMessageRepository implements UserInboxMessageRepository {

		private final Map<String, InboxMessageQueryDTO> details = new HashMap<>();
		private PageResult<InboxMessageQueryDTO> pageResult = PageResult.empty(1, 10);
		private Long pageUserId;
		private MessagePriority pagePriority;
		private String pageMessageType;
		private Boolean pageRead;
		private Long listNewUserId;
		private LocalDateTime listNewLastViewedAt;
		private List<Long> newInboxMessageIds = List.of();
		private Long markReadUserId;
		private Long markReadInboxMessageId;
		private Long softDeleteUserId;
		private Long softDeleteInboxMessageId;
		private long countActiveReturn;
		private Long countActiveUserId;
		private List<Long> countActiveIds;
		private List<Long> softDeleteBatchIds;

		@Override
		public int insertIgnoreBatch(List<com.zhangjintao.project.smart_lab_backend.modules.message_inbox.domain.entity.UserInboxMessage> inboxMessages) {
			throw new UnsupportedOperationException("当前测试不需要该方法");
		}

		@Override
		public PageResult<InboxMessageQueryDTO> page(Long userId, long pageNum, long pageSize,
				MessagePriority priority, String messageType, Boolean read) {
			this.pageUserId = userId;
			this.pagePriority = priority;
			this.pageMessageType = messageType;
			this.pageRead = read;
			return pageResult;
		}

		@Override
		public List<Long> listNewInboxMessageIds(Long userId, LocalDateTime lastViewedAt) {
			this.listNewUserId = userId;
			this.listNewLastViewedAt = lastViewedAt;
			return newInboxMessageIds;
		}

		@Override
		public Optional<InboxMessageQueryDTO> findActiveDetail(Long userId, Long inboxMessageId) {
			return Optional.ofNullable(details.get(key(userId, inboxMessageId)));
		}

		@Override
		public long countActiveByUserIdAndIds(Long userId, List<Long> inboxMessageIds) {
			this.countActiveUserId = userId;
			this.countActiveIds = inboxMessageIds;
			return countActiveReturn;
		}

		@Override
		public int markRead(Long userId, Long inboxMessageId, LocalDateTime readAt) {
			this.markReadUserId = userId;
			this.markReadInboxMessageId = inboxMessageId;
			InboxMessageQueryDTO message = details.get(key(userId, inboxMessageId));
			message.setReadFlag(1);
			message.setReadAt(readAt);
			return 1;
		}

		@Override
		public int softDelete(Long userId, Long inboxMessageId, LocalDateTime deletedAt) {
			this.softDeleteUserId = userId;
			this.softDeleteInboxMessageId = inboxMessageId;
			return 1;
		}

		@Override
		public int softDeleteBatch(Long userId, List<Long> inboxMessageIds, LocalDateTime deletedAt) {
			this.softDeleteBatchIds = inboxMessageIds;
			return inboxMessageIds.size();
		}
	}

	private static class RecordingUserInboxVisitStatRepository implements UserInboxVisitStatRepository {

		private Optional<LocalDateTime> lastViewedAt = Optional.empty();
		private Long findLastViewedAtUserId;
		private Long upsertUserId;
		private LocalDateTime upsertLastViewedAt;

		@Override
		public Optional<LocalDateTime> findLastViewedAt(Long userId) {
			this.findLastViewedAtUserId = userId;
			return lastViewedAt;
		}

		@Override
		public int upsertLastViewedAt(Long userId, LocalDateTime lastViewedAt) {
			this.upsertUserId = userId;
			this.upsertLastViewedAt = lastViewedAt;
			return 1;
		}
	}
}
