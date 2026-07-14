package com.zhangjintao.project.smart_lab_backend.modules.sensor_env.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.constants.SensorEnvConstants;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.model.dto.SensorRuntimeSnapshot;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Optional;
import java.util.function.Supplier;

@Component
public class SensorRuntimeCacheManager {

	private final Cache<String, SensorRuntimeSnapshot> localCache;
	private final StringRedisTemplate stringRedisTemplate;
	private final ObjectMapper objectMapper;

	public SensorRuntimeCacheManager(
			@Qualifier("sensorRuntimeCaffeineCache") Cache<String, SensorRuntimeSnapshot> localCache,
			StringRedisTemplate stringRedisTemplate, ObjectMapper objectMapper) {
		this.localCache = localCache;
		this.stringRedisTemplate = stringRedisTemplate;
		this.objectMapper = objectMapper;
	}

	public Optional<SensorRuntimeSnapshot> get(String sensorNo,
			Supplier<Optional<SensorRuntimeSnapshot>> databaseLoader) {
		SensorRuntimeSnapshot local = localCache.getIfPresent(sensorNo);
		if (local != null) {
			return Optional.of(local);
		}
		String redisJson = stringRedisTemplate.opsForValue().get(SensorEnvConstants.sensorRuntimeRedisKey(sensorNo));
		if (StringUtils.hasText(redisJson)) {
			SensorRuntimeSnapshot snapshot = read(redisJson);
			localCache.put(sensorNo, snapshot);
			return Optional.of(snapshot);
		}
		Optional<SensorRuntimeSnapshot> loaded = databaseLoader.get();
		loaded.ifPresent(snapshot -> put(sensorNo, snapshot));
		return loaded;
	}

	public void evict(String sensorNo) {
		stringRedisTemplate.delete(SensorEnvConstants.sensorRuntimeRedisKey(sensorNo));
		localCache.invalidate(sensorNo);
	}

	private void put(String sensorNo, SensorRuntimeSnapshot snapshot) {
		stringRedisTemplate.opsForValue().set(SensorEnvConstants.sensorRuntimeRedisKey(sensorNo),
				write(snapshot), SensorEnvConstants.SENSOR_RUNTIME_CACHE_TTL);
		localCache.put(sensorNo, snapshot);
	}

	private SensorRuntimeSnapshot read(String json) {
		try {
			return objectMapper.readValue(json, SensorRuntimeSnapshot.class);
		}
		catch (JsonProcessingException ex) {
			throw new IllegalStateException("传感器运行快照 Redis 缓存反序列化失败", ex);
		}
	}

	private String write(SensorRuntimeSnapshot snapshot) {
		try {
			return objectMapper.writeValueAsString(snapshot);
		}
		catch (JsonProcessingException ex) {
			throw new IllegalStateException("传感器运行快照 Redis 缓存序列化失败", ex);
		}
	}
}
