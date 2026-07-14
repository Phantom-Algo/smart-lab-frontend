package com.zhangjintao.project.smart_lab_backend.modules.message_inbox.dao.repository.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhangjintao.project.smart_lab_backend.common.result.PageResult;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.dao.mapper.UserInboxMessageMapper;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.domain.entity.UserInboxMessage;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.enums.MessagePriority;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.model.dto.InboxMessageQueryDTO;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserInboxMessageRepositoryImplTest {

	@Test
	@SuppressWarnings({"unchecked", "rawtypes"})
	void pageUsesMapperPageAndConvertsFilters() {
		UserInboxMessageMapper mapper = mock(UserInboxMessageMapper.class);
		UserInboxMessageRepositoryImpl repository = new UserInboxMessageRepositoryImpl(mapper);
		Page<InboxMessageQueryDTO> mapperPage = new Page<>(2, 20);
		mapperPage.setRecords(List.of(message()));
		mapperPage.setTotal(31);
		when(mapper.selectInboxMessagePage(any(Page.class), eq(77L), eq(2),
				eq("message_inbox.system.lab_notice"), eq(0))).thenReturn(mapperPage);

		PageResult<InboxMessageQueryDTO> result = repository.page(77L, 2, 20, MessagePriority.SYSTEM,
				"message_inbox.system.lab_notice", false);

		assertThat(result.getPageNum()).isEqualTo(2);
		assertThat(result.getPageSize()).isEqualTo(20);
		assertThat(result.getTotal()).isEqualTo(31);
		assertThat(result.getRecords()).hasSize(1);
		ArgumentCaptor<Page> pageCaptor = ArgumentCaptor.forClass(Page.class);
		verify(mapper).selectInboxMessagePage(pageCaptor.capture(), eq(77L), eq(2),
				eq("message_inbox.system.lab_notice"), eq(0));
		assertThat(pageCaptor.getValue().getCurrent()).isEqualTo(2);
		assertThat(pageCaptor.getValue().getSize()).isEqualTo(20);
	}

	@Test
	void insertIgnoreBatchReturnsZeroForEmptyList() {
		UserInboxMessageMapper mapper = mock(UserInboxMessageMapper.class);
		UserInboxMessageRepositoryImpl repository = new UserInboxMessageRepositoryImpl(mapper);

		int insertedRows = repository.insertIgnoreBatch(List.of());

		assertThat(insertedRows).isZero();
		verify(mapper, never()).insertIgnoreBatch(any());
	}

	@Test
	void insertIgnoreBatchConvertsDomainMessages() {
		UserInboxMessageMapper mapper = mock(UserInboxMessageMapper.class);
		UserInboxMessageRepositoryImpl repository = new UserInboxMessageRepositoryImpl(mapper);
		when(mapper.insertIgnoreBatch(any())).thenReturn(1);

		int insertedRows = repository.insertIgnoreBatch(List.of(UserInboxMessage.unread(100L, 77L,
				LocalDateTime.now())));

		assertThat(insertedRows).isEqualTo(1);
		verify(mapper).insertIgnoreBatch(any());
	}

	@Test
	void listNewInboxMessageIdsPassesLastViewedAtToMapper() {
		UserInboxMessageMapper mapper = mock(UserInboxMessageMapper.class);
		UserInboxMessageRepositoryImpl repository = new UserInboxMessageRepositoryImpl(mapper);
		LocalDateTime lastViewedAt = LocalDateTime.now().minusMinutes(5);
		when(mapper.selectNewInboxMessageIds(77L, lastViewedAt)).thenReturn(List.of(10L, 11L));

		List<Long> ids = repository.listNewInboxMessageIds(77L, lastViewedAt);

		assertThat(ids).containsExactly(10L, 11L);
		verify(mapper).selectNewInboxMessageIds(77L, lastViewedAt);
	}

	@Test
	void listNewInboxMessageIdsSupportsNullLastViewedAt() {
		UserInboxMessageMapper mapper = mock(UserInboxMessageMapper.class);
		UserInboxMessageRepositoryImpl repository = new UserInboxMessageRepositoryImpl(mapper);
		when(mapper.selectNewInboxMessageIds(77L, null)).thenReturn(List.of(10L, 11L, 12L));

		List<Long> ids = repository.listNewInboxMessageIds(77L, null);

		assertThat(ids).containsExactly(10L, 11L, 12L);
		verify(mapper).selectNewInboxMessageIds(77L, null);
	}

	@Test
	void softDeleteBatchReturnsZeroForEmptyList() {
		UserInboxMessageMapper mapper = mock(UserInboxMessageMapper.class);
		UserInboxMessageRepositoryImpl repository = new UserInboxMessageRepositoryImpl(mapper);

		int affectedRows = repository.softDeleteBatch(77L, List.of(), LocalDateTime.now());

		assertThat(affectedRows).isZero();
		verify(mapper, never()).softDeleteBatch(any(), any(), any());
	}

	private InboxMessageQueryDTO message() {
		InboxMessageQueryDTO message = new InboxMessageQueryDTO();
		message.setInboxMessageId(10L);
		message.setMessageId(100L);
		message.setTitle("实验室通知");
		message.setPriority(MessagePriority.SYSTEM.getCode());
		message.setMessageType("message_inbox.system.lab_notice");
		message.setReadFlag(0);
		return message;
	}
}
