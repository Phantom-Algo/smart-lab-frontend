package com.zhangjintao.project.smart_lab_backend.modules.lab_management.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.constants.LabManagementConstants;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.domain.value.LabWeeklySchedule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LabScheduleCacheManagerTest {

	@Mock
	private StringRedisTemplate stringRedisTemplate;

	@Mock
	private ValueOperations<String, String> valueOperations;

	private Cache<Long, List<LabWeeklySchedule>> localCache;
	private ObjectMapper objectMapper;
	private LabScheduleCacheManager labScheduleCacheManager;

	@BeforeEach
	void setUp() {
		localCache = Caffeine.newBuilder().build();
		objectMapper = new ObjectMapper()
				.registerModule(new JavaTimeModule())
				.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
		labScheduleCacheManager = new LabScheduleCacheManager(localCache, stringRedisTemplate, objectMapper);
	}

	@Test
	void listByLabIdShouldReturnLocalCacheFirst() {
		LabWeeklySchedule schedule = schedule();
		localCache.put(100L, List.of(schedule));

		List<LabWeeklySchedule> result = labScheduleCacheManager.listByLabId(100L, () -> {
			throw new AssertionError("本地缓存命中时不应查询数据库");
		});

		assertThat(result).containsExactly(schedule);
		verify(stringRedisTemplate, never()).opsForValue();
	}

	@Test
	void listByLabIdShouldReadRedisAndBackfillLocalCache() throws Exception {
		LabWeeklySchedule schedule = schedule();
		when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
		when(valueOperations.get(LabManagementConstants.labScheduleRedisKey(100L)))
				.thenReturn(objectMapper.writeValueAsString(List.of(schedule)));

		List<LabWeeklySchedule> result = labScheduleCacheManager.listByLabId(100L, () -> {
			throw new AssertionError("Redis 缓存命中时不应查询数据库");
		});

		assertThat(result).extracting(LabWeeklySchedule::getDayOfWeek).containsExactly(1);
		assertThat(localCache.getIfPresent(100L)).extracting(LabWeeklySchedule::getDayOfWeek).containsExactly(1);
	}

	@Test
	void listByLabIdShouldLoadDatabaseAndWriteTwoLevelCacheWhenCacheMiss() {
		LabWeeklySchedule schedule = schedule();
		when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
		when(valueOperations.get(LabManagementConstants.labScheduleRedisKey(100L))).thenReturn(null);

		List<LabWeeklySchedule> result = labScheduleCacheManager.listByLabId(100L, () -> List.of(schedule));

		assertThat(result).containsExactly(schedule);
		assertThat(localCache.getIfPresent(100L)).containsExactly(schedule);
		verify(valueOperations).set(eq(LabManagementConstants.labScheduleRedisKey(100L)), anyString(),
				eq(LabManagementConstants.LAB_SCHEDULE_CACHE_TTL));
	}

	@Test
	void evictShouldDeleteRedisAndInvalidateLocalCache() {
		localCache.put(100L, List.of(schedule()));

		labScheduleCacheManager.evict(100L);

		verify(stringRedisTemplate).delete(LabManagementConstants.labScheduleRedisKey(100L));
		assertThat(localCache.getIfPresent(100L)).isNull();
	}

	private LabWeeklySchedule schedule() {
		return LabWeeklySchedule.builder()
				.id(1L)
				.labId(100L)
				.dayOfWeek(1)
				.openTime(LocalTime.of(8, 0))
				.closeTime(LocalTime.of(18, 0))
				.build();
	}
}
