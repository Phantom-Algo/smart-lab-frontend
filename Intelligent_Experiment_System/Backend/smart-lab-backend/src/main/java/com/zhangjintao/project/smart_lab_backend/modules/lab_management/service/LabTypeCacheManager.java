package com.zhangjintao.project.smart_lab_backend.modules.lab_management.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.constants.LabManagementConstants;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.domain.entity.LabType;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.function.Supplier;

/**
 * 实验室类型二级缓存：Caffeine 本地缓存 + Redis 分布式缓存。
 */
@Component
public class LabTypeCacheManager {

	private static final TypeReference<List<LabType>> LAB_TYPE_LIST_TYPE = new TypeReference<>() {
	};

	private final Cache<String, List<LabType>> labTypeListCaffeineCache;
	private final StringRedisTemplate stringRedisTemplate;
	private final ObjectMapper objectMapper;

	public LabTypeCacheManager(
			@Qualifier("labTypeListCaffeineCache") Cache<String, List<LabType>> labTypeListCaffeineCache,
			StringRedisTemplate stringRedisTemplate,
			ObjectMapper objectMapper) {
		this.labTypeListCaffeineCache = labTypeListCaffeineCache;
		this.stringRedisTemplate = stringRedisTemplate;
		this.objectMapper = objectMapper;
	}

	public List<LabType> listAll(Supplier<List<LabType>> databaseLoader) {
		List<LabType> localCachedLabTypes = labTypeListCaffeineCache
				.getIfPresent(LabManagementConstants.LAB_TYPES_ALL_LOCAL_CACHE_KEY);
		if (localCachedLabTypes != null) {
			return localCachedLabTypes;
		}

		String redisCachedJson = stringRedisTemplate.opsForValue().get(LabManagementConstants.LAB_TYPES_ALL_REDIS_KEY);
		if (StringUtils.hasText(redisCachedJson)) {
			List<LabType> labTypes = readLabTypes(redisCachedJson);
			putLocalCache(labTypes);
			return labTypes;
		}

		List<LabType> labTypes = databaseLoader.get();
		putRedisCache(labTypes);
		putLocalCache(labTypes);
		return labTypes;
	}

	public void evictAll() {
		stringRedisTemplate.delete(LabManagementConstants.LAB_TYPES_ALL_REDIS_KEY);
		labTypeListCaffeineCache.invalidate(LabManagementConstants.LAB_TYPES_ALL_LOCAL_CACHE_KEY);
	}

	private void putLocalCache(List<LabType> labTypes) {
		labTypeListCaffeineCache.put(LabManagementConstants.LAB_TYPES_ALL_LOCAL_CACHE_KEY, List.copyOf(labTypes));
	}

	private void putRedisCache(List<LabType> labTypes) {
		stringRedisTemplate.opsForValue().set(LabManagementConstants.LAB_TYPES_ALL_REDIS_KEY,
				writeLabTypes(labTypes), LabManagementConstants.LAB_TYPES_CACHE_TTL);
	}

	private List<LabType> readLabTypes(String json) {
		try {
			return objectMapper.readValue(json, LAB_TYPE_LIST_TYPE);
		}
		catch (JsonProcessingException ex) {
			throw new IllegalStateException("实验室类型 Redis 缓存反序列化失败", ex);
		}
	}

	private String writeLabTypes(List<LabType> labTypes) {
		try {
			return objectMapper.writeValueAsString(labTypes);
		}
		catch (JsonProcessingException ex) {
			throw new IllegalStateException("实验室类型 Redis 缓存序列化失败", ex);
		}
	}
}
