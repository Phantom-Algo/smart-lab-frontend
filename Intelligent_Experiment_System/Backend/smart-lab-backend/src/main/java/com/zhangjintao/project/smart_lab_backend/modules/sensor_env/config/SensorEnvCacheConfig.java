package com.zhangjintao.project.smart_lab_backend.modules.sensor_env.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.constants.SensorEnvConstants;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.domain.entity.SensorType;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.model.dto.SensorRuntimeSnapshot;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SensorEnvCacheConfig {

	@Bean
	public Cache<String, List<SensorType>> sensorTypeCatalogCaffeineCache() {
		return Caffeine.newBuilder()
				.maximumSize(1)
				.expireAfterWrite(SensorEnvConstants.SENSOR_TYPE_CATALOG_CACHE_TTL)
				.build();
	}

	@Bean
	public Cache<String, SensorRuntimeSnapshot> sensorRuntimeCaffeineCache() {
		return Caffeine.newBuilder()
				.maximumSize(10_000)
				.expireAfterWrite(SensorEnvConstants.SENSOR_RUNTIME_CACHE_TTL)
				.build();
	}
}
