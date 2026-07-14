package com.zhangjintao.project.smart_lab_backend.modules.lab_management.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.constants.LabManagementConstants;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.domain.entity.LabType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LabTypeCacheManagerTest {

	@Mock
	private StringRedisTemplate stringRedisTemplate;

	@Mock
	private ValueOperations<String, String> valueOperations;

	private Cache<String, List<LabType>> localCache;
	private ObjectMapper objectMapper;
	private LabTypeCacheManager labTypeCacheManager;

	@BeforeEach
	void setUp() {
		localCache = Caffeine.newBuilder().build();
		objectMapper = new ObjectMapper()
				.registerModule(new JavaTimeModule())
				.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
		labTypeCacheManager = new LabTypeCacheManager(localCache, stringRedisTemplate, objectMapper);
	}

	@Test
	void listAllShouldReturnLocalCacheFirst() {
		LabType labType = LabType.builder()
				.id(1L)
				.labTypeName("化学实验室")
				.build();
		localCache.put(LabManagementConstants.LAB_TYPES_ALL_LOCAL_CACHE_KEY, List.of(labType));

		List<LabType> result = labTypeCacheManager.listAll(() -> {
			throw new AssertionError("本地缓存命中时不应查询数据库");
		});

		assertThat(result).containsExactly(labType);
		verify(stringRedisTemplate, never()).opsForValue();
	}

	@Test
	void listAllShouldReadRedisAndBackfillLocalCache() throws Exception {
		LabType labType = LabType.builder()
				.id(1L)
				.labTypeName("化学实验室")
				.build();
		when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
		when(valueOperations.get(LabManagementConstants.LAB_TYPES_ALL_REDIS_KEY))
				.thenReturn(objectMapper.writeValueAsString(List.of(labType)));

		List<LabType> result = labTypeCacheManager.listAll(() -> {
			throw new AssertionError("Redis 缓存命中时不应查询数据库");
		});

		assertThat(result).extracting(LabType::getLabTypeName).containsExactly("化学实验室");
		assertThat(localCache.getIfPresent(LabManagementConstants.LAB_TYPES_ALL_LOCAL_CACHE_KEY))
				.extracting(LabType::getLabTypeName)
				.containsExactly("化学实验室");
	}

	@Test
	void listAllShouldLoadDatabaseAndWriteTwoLevelCacheWhenCacheMiss() {
		LabType labType = LabType.builder()
				.id(1L)
				.labTypeName("化学实验室")
				.build();
		when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
		when(valueOperations.get(LabManagementConstants.LAB_TYPES_ALL_REDIS_KEY)).thenReturn(null);

		List<LabType> result = labTypeCacheManager.listAll(() -> List.of(labType));

		assertThat(result).containsExactly(labType);
		assertThat(localCache.getIfPresent(LabManagementConstants.LAB_TYPES_ALL_LOCAL_CACHE_KEY)).containsExactly(labType);
		verify(valueOperations).set(eq(LabManagementConstants.LAB_TYPES_ALL_REDIS_KEY), anyString(),
				eq(LabManagementConstants.LAB_TYPES_CACHE_TTL));
	}

	@Test
	void evictAllShouldDeleteRedisAndInvalidateLocalCache() {
		LabType labType = LabType.builder()
				.id(1L)
				.labTypeName("化学实验室")
				.build();
		localCache.put(LabManagementConstants.LAB_TYPES_ALL_LOCAL_CACHE_KEY, List.of(labType));

		labTypeCacheManager.evictAll();

		verify(stringRedisTemplate).delete(LabManagementConstants.LAB_TYPES_ALL_REDIS_KEY);
		assertThat(localCache.getIfPresent(LabManagementConstants.LAB_TYPES_ALL_LOCAL_CACHE_KEY)).isNull();
	}
}
