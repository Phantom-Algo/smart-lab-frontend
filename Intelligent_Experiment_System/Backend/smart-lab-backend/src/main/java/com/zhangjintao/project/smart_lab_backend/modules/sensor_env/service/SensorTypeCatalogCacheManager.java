package com.zhangjintao.project.smart_lab_backend.modules.sensor_env.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.constants.SensorEnvConstants;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.domain.entity.SensorType;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.function.Supplier;

/**
 * 传感器类型目录二级缓存：Caffeine 本地缓存 + Redis 分布式缓存。
 */
@Component
public class SensorTypeCatalogCacheManager {

	private static final TypeReference<List<SensorType>> SENSOR_TYPE_CATALOG_TYPE = new TypeReference<>() {
	};

	private final Cache<String, List<SensorType>> sensorTypeCatalogCaffeineCache;
	private final StringRedisTemplate stringRedisTemplate;
	private final ObjectMapper objectMapper;

	public SensorTypeCatalogCacheManager(
			@Qualifier("sensorTypeCatalogCaffeineCache") Cache<String, List<SensorType>> sensorTypeCatalogCaffeineCache,
			StringRedisTemplate stringRedisTemplate,
			ObjectMapper objectMapper) {
		this.sensorTypeCatalogCaffeineCache = sensorTypeCatalogCaffeineCache;
		this.stringRedisTemplate = stringRedisTemplate;
		this.objectMapper = objectMapper;
	}

	public List<SensorType> listAll(Supplier<List<SensorType>> databaseLoader) {
		List<SensorType> localCatalog = sensorTypeCatalogCaffeineCache
				.getIfPresent(SensorEnvConstants.SENSOR_TYPE_CATALOG_LOCAL_CACHE_KEY);
		if (localCatalog != null) {
			return localCatalog;
		}

		String redisJson = stringRedisTemplate.opsForValue()
				.get(SensorEnvConstants.SENSOR_TYPE_CATALOG_REDIS_KEY);
		if (StringUtils.hasText(redisJson)) {
			List<SensorType> catalog = readCatalog(redisJson);
			putLocalCache(catalog);
			return catalog;
		}

		List<SensorType> catalog = List.copyOf(databaseLoader.get());
		putRedisCache(catalog);
		putLocalCache(catalog);
		return catalog;
	}

	public void evictAll() {
		stringRedisTemplate.delete(SensorEnvConstants.SENSOR_TYPE_CATALOG_REDIS_KEY);
		sensorTypeCatalogCaffeineCache.invalidate(SensorEnvConstants.SENSOR_TYPE_CATALOG_LOCAL_CACHE_KEY);
	}

	private void putLocalCache(List<SensorType> catalog) {
		sensorTypeCatalogCaffeineCache.put(SensorEnvConstants.SENSOR_TYPE_CATALOG_LOCAL_CACHE_KEY,
				List.copyOf(catalog));
	}

	private void putRedisCache(List<SensorType> catalog) {
		stringRedisTemplate.opsForValue().set(SensorEnvConstants.SENSOR_TYPE_CATALOG_REDIS_KEY,
				writeCatalog(catalog), SensorEnvConstants.SENSOR_TYPE_CATALOG_CACHE_TTL);
	}

	private List<SensorType> readCatalog(String json) {
		try {
			return List.copyOf(objectMapper.readValue(json, SENSOR_TYPE_CATALOG_TYPE));
		}
		catch (JsonProcessingException ex) {
			throw new IllegalStateException("传感器类型目录 Redis 缓存反序列化失败", ex);
		}
	}

	private String writeCatalog(List<SensorType> catalog) {
		try {
			return objectMapper.writeValueAsString(catalog);
		}
		catch (JsonProcessingException ex) {
			throw new IllegalStateException("传感器类型目录 Redis 缓存序列化失败", ex);
		}
	}
}
