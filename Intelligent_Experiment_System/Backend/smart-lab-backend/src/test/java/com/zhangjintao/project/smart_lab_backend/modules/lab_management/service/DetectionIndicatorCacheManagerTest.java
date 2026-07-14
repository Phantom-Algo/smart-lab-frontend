package com.zhangjintao.project.smart_lab_backend.modules.lab_management.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.constants.LabManagementConstants;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.domain.entity.DetectionIndicator;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.domain.value.DetectionSafeRange;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.domain.value.DetectionSafeRangeSegment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DetectionIndicatorCacheManagerTest {

	@Mock
	private StringRedisTemplate stringRedisTemplate;

	@Mock
	private ValueOperations<String, String> valueOperations;

	private Cache<Long, List<DetectionIndicator>> localCache;
	private ObjectMapper objectMapper;
	private DetectionIndicatorCacheManager detectionIndicatorCacheManager;

	@BeforeEach
	void setUp() {
		localCache = Caffeine.newBuilder().build();
		objectMapper = new ObjectMapper()
				.registerModule(new JavaTimeModule())
				.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
		detectionIndicatorCacheManager = new DetectionIndicatorCacheManager(localCache, stringRedisTemplate, objectMapper);
	}

	@Test
	void listByLabTypeIdShouldReturnLocalCacheFirst() {
		DetectionIndicator indicator = indicator(1L, 1L, "temperature");
		localCache.put(1L, List.of(indicator));

		List<DetectionIndicator> result = detectionIndicatorCacheManager.listByLabTypeId(1L, () -> {
			throw new AssertionError("本地缓存命中时不应查询数据库");
		});

		assertThat(result).containsExactly(indicator);
		verify(stringRedisTemplate, never()).opsForValue();
	}

	@Test
	void listByLabTypeIdShouldReadRedisAndBackfillLocalCache() throws Exception {
		DetectionIndicator indicator = indicator(1L, 1L, "temperature");
		when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
		when(valueOperations.get(LabManagementConstants.labIndicatorsRedisKey(1L)))
				.thenReturn(objectMapper.writeValueAsString(List.of(indicator)));

		List<DetectionIndicator> result = detectionIndicatorCacheManager.listByLabTypeId(1L, () -> {
			throw new AssertionError("Redis 缓存命中时不应查询数据库");
		});

		assertThat(result).extracting(DetectionIndicator::getIndicatorName).containsExactly("temperature");
		assertThat(localCache.getIfPresent(1L))
				.extracting(DetectionIndicator::getIndicatorName)
				.containsExactly("temperature");
	}

	@Test
	void listByLabTypeIdShouldLoadDatabaseAndWriteTwoLevelCacheWhenCacheMiss() {
		DetectionIndicator indicator = indicator(1L, 1L, "temperature");
		when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
		when(valueOperations.get(LabManagementConstants.labIndicatorsRedisKey(1L))).thenReturn(null);

		List<DetectionIndicator> result = detectionIndicatorCacheManager.listByLabTypeId(1L, () -> List.of(indicator));

		assertThat(result).containsExactly(indicator);
		assertThat(localCache.getIfPresent(1L)).containsExactly(indicator);
		verify(valueOperations).set(eq(LabManagementConstants.labIndicatorsRedisKey(1L)), anyString(),
				eq(LabManagementConstants.LAB_INDICATORS_CACHE_TTL));
	}

	@Test
	void evictByLabTypeIdShouldDeleteRedisAndInvalidateLocalCache() {
		DetectionIndicator indicator = indicator(1L, 1L, "temperature");
		localCache.put(1L, List.of(indicator));

		detectionIndicatorCacheManager.evictByLabTypeId(1L);

		verify(stringRedisTemplate).delete(LabManagementConstants.labIndicatorsRedisKey(1L));
		assertThat(localCache.getIfPresent(1L)).isNull();
	}

	private DetectionIndicator indicator(Long id, Long labTypeId, String indicatorName) {
		return DetectionIndicator.builder()
				.id(id)
				.labTypeId(labTypeId)
				.indicatorName(indicatorName)
				.unit("℃")
				.safeRange(DetectionSafeRange.builder()
						.segments(List.of(DetectionSafeRangeSegment.builder()
								.min(BigDecimal.ZERO)
								.max(BigDecimal.TEN)
								.build()))
						.build())
				.build();
	}
}
