package com.zhangjintao.project.smart_lab_backend.modules.lab_management.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.constants.LabManagementConstants;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.domain.value.LabWeeklySchedule;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.function.Supplier;

/**
 * 实验室开放时间二级缓存：Caffeine 本地缓存 + Redis 分布式缓存。
 */
@Component
public class LabScheduleCacheManager {

	private static final TypeReference<List<LabWeeklySchedule>> LAB_SCHEDULE_LIST_TYPE = new TypeReference<>() {
	};

	private final Cache<Long, List<LabWeeklySchedule>> labScheduleListCaffeineCache;
	private final StringRedisTemplate stringRedisTemplate;
	private final ObjectMapper objectMapper;

	public LabScheduleCacheManager(
			@Qualifier("labScheduleListCaffeineCache") Cache<Long, List<LabWeeklySchedule>> labScheduleListCaffeineCache,
			StringRedisTemplate stringRedisTemplate,
			ObjectMapper objectMapper) {
		this.labScheduleListCaffeineCache = labScheduleListCaffeineCache;
		this.stringRedisTemplate = stringRedisTemplate;
		this.objectMapper = objectMapper;
	}

	public List<LabWeeklySchedule> listByLabId(Long labId, Supplier<List<LabWeeklySchedule>> databaseLoader) {
		List<LabWeeklySchedule> localCachedSchedules = labScheduleListCaffeineCache.getIfPresent(labId);
		if (localCachedSchedules != null) {
			return localCachedSchedules;
		}

		String redisKey = LabManagementConstants.labScheduleRedisKey(labId);
		String redisCachedJson = stringRedisTemplate.opsForValue().get(redisKey);
		if (StringUtils.hasText(redisCachedJson)) {
			List<LabWeeklySchedule> schedules = readSchedules(redisCachedJson);
			putLocalCache(labId, schedules);
			return schedules;
		}

		List<LabWeeklySchedule> schedules = databaseLoader.get();
		putRedisCache(redisKey, schedules);
		putLocalCache(labId, schedules);
		return schedules;
	}

	public void evict(Long labId) {
		if (labId == null) {
			return;
		}
		stringRedisTemplate.delete(LabManagementConstants.labScheduleRedisKey(labId));
		labScheduleListCaffeineCache.invalidate(labId);
	}

	private void putLocalCache(Long labId, List<LabWeeklySchedule> schedules) {
		labScheduleListCaffeineCache.put(labId, List.copyOf(schedules));
	}

	private void putRedisCache(String redisKey, List<LabWeeklySchedule> schedules) {
		stringRedisTemplate.opsForValue().set(redisKey,
				writeSchedules(schedules), LabManagementConstants.LAB_SCHEDULE_CACHE_TTL);
	}

	private List<LabWeeklySchedule> readSchedules(String json) {
		try {
			return objectMapper.readValue(json, LAB_SCHEDULE_LIST_TYPE);
		}
		catch (JsonProcessingException ex) {
			throw new IllegalStateException("实验室开放时间 Redis 缓存反序列化失败", ex);
		}
	}

	private String writeSchedules(List<LabWeeklySchedule> schedules) {
		try {
			return objectMapper.writeValueAsString(schedules);
		}
		catch (JsonProcessingException ex) {
			throw new IllegalStateException("实验室开放时间 Redis 缓存序列化失败", ex);
		}
	}
}
