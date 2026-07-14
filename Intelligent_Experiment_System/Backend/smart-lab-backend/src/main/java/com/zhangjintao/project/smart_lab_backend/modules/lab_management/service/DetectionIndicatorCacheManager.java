package com.zhangjintao.project.smart_lab_backend.modules.lab_management.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.constants.LabManagementConstants;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.domain.entity.DetectionIndicator;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.function.Supplier;

/**
 * 检测指标配置二级缓存：Caffeine 本地缓存 + Redis 分布式缓存。
 */
@Component
public class DetectionIndicatorCacheManager {

	private static final TypeReference<List<DetectionIndicator>> INDICATOR_LIST_TYPE = new TypeReference<>() {
	};

	private final Cache<Long, List<DetectionIndicator>> detectionIndicatorListCaffeineCache;
	private final StringRedisTemplate stringRedisTemplate;
	private final ObjectMapper objectMapper;

	public DetectionIndicatorCacheManager(
			@Qualifier("detectionIndicatorListCaffeineCache")
			Cache<Long, List<DetectionIndicator>> detectionIndicatorListCaffeineCache,
			StringRedisTemplate stringRedisTemplate,
			ObjectMapper objectMapper) {
		this.detectionIndicatorListCaffeineCache = detectionIndicatorListCaffeineCache;
		this.stringRedisTemplate = stringRedisTemplate;
		this.objectMapper = objectMapper;
	}

	public List<DetectionIndicator> listByLabTypeId(Long labTypeId, Supplier<List<DetectionIndicator>> databaseLoader) {
		List<DetectionIndicator> localCachedIndicators = detectionIndicatorListCaffeineCache.getIfPresent(labTypeId);
		if (localCachedIndicators != null) {
			return localCachedIndicators;
		}

		String redisKey = LabManagementConstants.labIndicatorsRedisKey(labTypeId);
		String redisCachedJson = stringRedisTemplate.opsForValue().get(redisKey);
		if (StringUtils.hasText(redisCachedJson)) {
			List<DetectionIndicator> indicators = readIndicators(redisCachedJson);
			putLocalCache(labTypeId, indicators);
			return indicators;
		}

		List<DetectionIndicator> indicators = databaseLoader.get();
		putRedisCache(redisKey, indicators);
		putLocalCache(labTypeId, indicators);
		return indicators;
	}

	public void evictByLabTypeId(Long labTypeId) {
		stringRedisTemplate.delete(LabManagementConstants.labIndicatorsRedisKey(labTypeId));
		detectionIndicatorListCaffeineCache.invalidate(labTypeId);
	}

	private void putLocalCache(Long labTypeId, List<DetectionIndicator> indicators) {
		detectionIndicatorListCaffeineCache.put(labTypeId, List.copyOf(indicators));
	}

	private void putRedisCache(String redisKey, List<DetectionIndicator> indicators) {
		stringRedisTemplate.opsForValue().set(redisKey,
				writeIndicators(indicators), LabManagementConstants.LAB_INDICATORS_CACHE_TTL);
	}

	private List<DetectionIndicator> readIndicators(String json) {
		try {
			return objectMapper.readValue(json, INDICATOR_LIST_TYPE);
		}
		catch (JsonProcessingException ex) {
			throw new IllegalStateException("检测指标 Redis 缓存反序列化失败", ex);
		}
	}

	private String writeIndicators(List<DetectionIndicator> indicators) {
		try {
			return objectMapper.writeValueAsString(indicators);
		}
		catch (JsonProcessingException ex) {
			throw new IllegalStateException("检测指标 Redis 缓存序列化失败", ex);
		}
	}
}
