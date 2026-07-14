package com.zhangjintao.project.smart_lab_backend.modules.lab_management.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.constants.LabManagementConstants;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.domain.entity.DetectionIndicator;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.domain.entity.LabType;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.domain.value.LabWeeklySchedule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class LabManagementCacheConfig {

	@Bean
	public Cache<String, List<LabType>> labTypeListCaffeineCache() {
		return Caffeine.newBuilder()
				.maximumSize(1)
				.expireAfterWrite(LabManagementConstants.LAB_TYPES_CACHE_TTL)
				.build();
	}

	@Bean
	public Cache<Long, List<DetectionIndicator>> detectionIndicatorListCaffeineCache() {
		return Caffeine.newBuilder()
				.maximumSize(512)
				.expireAfterWrite(LabManagementConstants.LAB_INDICATORS_CACHE_TTL)
				.build();
	}

	@Bean
	public Cache<Long, List<LabWeeklySchedule>> labScheduleListCaffeineCache() {
		return Caffeine.newBuilder()
				.maximumSize(512)
				.expireAfterWrite(LabManagementConstants.LAB_SCHEDULE_CACHE_TTL)
				.build();
	}
}
