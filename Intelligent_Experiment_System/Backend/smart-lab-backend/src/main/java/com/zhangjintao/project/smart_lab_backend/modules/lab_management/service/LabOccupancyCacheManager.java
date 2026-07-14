package com.zhangjintao.project.smart_lab_backend.modules.lab_management.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.constants.LabManagementConstants;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.response.LabOccupancyResponse;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.function.Supplier;

@Component
public class LabOccupancyCacheManager {

	private static final TypeReference<List<LabOccupancyResponse.OccupiedTime>> OCCUPIED_TIME_LIST_TYPE =
			new TypeReference<>() {
			};

	private final StringRedisTemplate stringRedisTemplate;
	private final ObjectMapper objectMapper;

	public LabOccupancyCacheManager(StringRedisTemplate stringRedisTemplate, ObjectMapper objectMapper) {
		this.stringRedisTemplate = stringRedisTemplate;
		this.objectMapper = objectMapper;
	}

	public List<LabOccupancyResponse.OccupiedTime> listOccupiedTimes(Long labId, LocalDate date,
			Supplier<List<LabOccupancyResponse.OccupiedTime>> databaseLoader) {
		String redisKey = LabManagementConstants.labOccupancyRedisKey(labId, date);
		String cachedJson = stringRedisTemplate.opsForValue().get(redisKey);
		if (StringUtils.hasText(cachedJson)) {
			return readOccupiedTimes(cachedJson);
		}
		List<LabOccupancyResponse.OccupiedTime> occupiedTimes = databaseLoader.get();
		stringRedisTemplate.opsForValue().set(redisKey, writeOccupiedTimes(occupiedTimes),
				LabManagementConstants.LAB_OCCUPANCY_CACHE_TTL);
		return occupiedTimes;
	}

	public void evict(Long labId, LocalDate date) {
		if (labId == null || date == null) {
			return;
		}
		stringRedisTemplate.delete(LabManagementConstants.labOccupancyRedisKey(labId, date));
	}

	private List<LabOccupancyResponse.OccupiedTime> readOccupiedTimes(String json) {
		try {
			return objectMapper.readValue(json, OCCUPIED_TIME_LIST_TYPE);
		}
		catch (JsonProcessingException ex) {
			throw new IllegalStateException("实验室占用 Redis 缓存反序列化失败", ex);
		}
	}

	private String writeOccupiedTimes(List<LabOccupancyResponse.OccupiedTime> occupiedTimes) {
		try {
			return objectMapper.writeValueAsString(occupiedTimes);
		}
		catch (JsonProcessingException ex) {
			throw new IllegalStateException("实验室占用 Redis 缓存序列化失败", ex);
		}
	}
}
