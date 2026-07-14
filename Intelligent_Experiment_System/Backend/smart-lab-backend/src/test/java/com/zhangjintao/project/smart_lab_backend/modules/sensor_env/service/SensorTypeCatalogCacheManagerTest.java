package com.zhangjintao.project.smart_lab_backend.modules.sensor_env.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.constants.SensorEnvConstants;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.domain.entity.SensorDataType;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.domain.entity.SensorType;
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
class SensorTypeCatalogCacheManagerTest {

	@Mock
	private StringRedisTemplate stringRedisTemplate;

	@Mock
	private ValueOperations<String, String> valueOperations;

	private Cache<String, List<SensorType>> localCache;
	private ObjectMapper objectMapper;
	private SensorTypeCatalogCacheManager cacheManager;

	@BeforeEach
	void setUp() {
		localCache = Caffeine.newBuilder().build();
		objectMapper = new ObjectMapper()
				.registerModule(new JavaTimeModule())
				.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
		cacheManager = new SensorTypeCatalogCacheManager(localCache, stringRedisTemplate, objectMapper);
	}

	@Test
	void listAllShouldReturnLocalCacheFirst() {
		SensorType sensorType = sensorType();
		localCache.put(SensorEnvConstants.SENSOR_TYPE_CATALOG_LOCAL_CACHE_KEY, List.of(sensorType));

		List<SensorType> result = cacheManager.listAll(() -> {
			throw new AssertionError("本地缓存命中时不应查询数据库");
		});

		assertThat(result).containsExactly(sensorType);
		verify(stringRedisTemplate, never()).opsForValue();
	}

	@Test
	void listAllShouldReadRedisAndBackfillLocalCache() throws Exception {
		when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
		when(valueOperations.get(SensorEnvConstants.SENSOR_TYPE_CATALOG_REDIS_KEY))
				.thenReturn(objectMapper.writeValueAsString(List.of(sensorType())));

		List<SensorType> result = cacheManager.listAll(() -> {
			throw new AssertionError("Redis 缓存命中时不应查询数据库");
		});

		assertThat(result).singleElement().satisfies(sensorType ->
				assertThat(sensorType.getDataTypes()).extracting(SensorDataType::getDataName)
						.containsExactly("temperature"));
		assertThat(localCache.getIfPresent(SensorEnvConstants.SENSOR_TYPE_CATALOG_LOCAL_CACHE_KEY)).isNotNull();
	}

	@Test
	void listAllShouldLoadDatabaseAndWriteTwoLevelCacheWhenCacheMiss() {
		when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
		when(valueOperations.get(SensorEnvConstants.SENSOR_TYPE_CATALOG_REDIS_KEY)).thenReturn(null);

		List<SensorType> result = cacheManager.listAll(() -> List.of(sensorType()));

		assertThat(result).hasSize(1);
		verify(valueOperations).set(eq(SensorEnvConstants.SENSOR_TYPE_CATALOG_REDIS_KEY), anyString(),
				eq(SensorEnvConstants.SENSOR_TYPE_CATALOG_CACHE_TTL));
		assertThat(localCache.getIfPresent(SensorEnvConstants.SENSOR_TYPE_CATALOG_LOCAL_CACHE_KEY)).hasSize(1);
	}

	@Test
	void evictAllShouldDeleteRedisAndInvalidateLocalCache() {
		localCache.put(SensorEnvConstants.SENSOR_TYPE_CATALOG_LOCAL_CACHE_KEY, List.of(sensorType()));

		cacheManager.evictAll();

		verify(stringRedisTemplate).delete(SensorEnvConstants.SENSOR_TYPE_CATALOG_REDIS_KEY);
		assertThat(localCache.getIfPresent(SensorEnvConstants.SENSOR_TYPE_CATALOG_LOCAL_CACHE_KEY)).isNull();
	}

	private SensorType sensorType() {
		return SensorType.builder()
				.id(1L)
				.sensorTypeName("温湿度传感器")
				.dataTypes(List.of(SensorDataType.builder()
						.id(2L)
						.sensorTypeId(1L)
						.dataName("temperature")
						.unit("°C")
						.build()))
				.build();
	}
}
