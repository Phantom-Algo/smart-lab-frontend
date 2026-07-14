package com.zhangjintao.project.smart_lab_backend.modules.message_inbox.dao.repository.impl;

import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.dao.mapper.UserInboxVisitStatMapper;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserInboxVisitStatRepositoryImplTest {

	@Test
	void findLastViewedAtReturnsOptionalValue() {
		UserInboxVisitStatMapper mapper = mock(UserInboxVisitStatMapper.class);
		UserInboxVisitStatRepositoryImpl repository = new UserInboxVisitStatRepositoryImpl(mapper);
		LocalDateTime lastViewedAt = LocalDateTime.now().minusMinutes(5);
		when(mapper.selectLastViewedAt(77L)).thenReturn(lastViewedAt);

		Optional<LocalDateTime> result = repository.findLastViewedAt(77L);

		assertThat(result).contains(lastViewedAt);
		verify(mapper).selectLastViewedAt(77L);
	}

	@Test
	void findLastViewedAtReturnsEmptyWhenMissing() {
		UserInboxVisitStatMapper mapper = mock(UserInboxVisitStatMapper.class);
		UserInboxVisitStatRepositoryImpl repository = new UserInboxVisitStatRepositoryImpl(mapper);
		when(mapper.selectLastViewedAt(77L)).thenReturn(null);

		Optional<LocalDateTime> result = repository.findLastViewedAt(77L);

		assertThat(result).isEmpty();
		verify(mapper).selectLastViewedAt(77L);
	}

	@Test
	void upsertLastViewedAtDelegatesToMapper() {
		UserInboxVisitStatMapper mapper = mock(UserInboxVisitStatMapper.class);
		UserInboxVisitStatRepositoryImpl repository = new UserInboxVisitStatRepositoryImpl(mapper);
		LocalDateTime lastViewedAt = LocalDateTime.now();
		when(mapper.upsertLastViewedAt(77L, lastViewedAt)).thenReturn(1);

		int affectedRows = repository.upsertLastViewedAt(77L, lastViewedAt);

		assertThat(affectedRows).isEqualTo(1);
		verify(mapper).upsertLastViewedAt(77L, lastViewedAt);
	}
}
