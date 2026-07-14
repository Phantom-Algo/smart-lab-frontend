package com.zhangjintao.project.smart_lab_backend.modules.lab_management.constants;

import java.time.Duration;

public final class LabManagementConstants {

	private LabManagementConstants() {
	}

	public static final long DEFAULT_PAGE = 1L;
	public static final long DEFAULT_PAGE_SIZE = 20L;
	public static final long MAX_PAGE_SIZE = 100L;

	public static final String SOURCE_SYSTEM = "lab_management";
	public static final String ENVIRONMENT_ALERT_SOURCE_SYSTEM = "lab";
	public static final String RESERVATION_PENDING_REVIEW_MESSAGE_TYPE = "lab_management.system.reservation_pending_review";
	public static final String RESERVATION_CANCELLED_MESSAGE_TYPE = "lab_management.system.reservation_cancelled";
	public static final String RESERVATION_APPROVED_MESSAGE_TYPE = "lab_management.system.reservation_approved";
	public static final String RESERVATION_REJECTED_MESSAGE_TYPE = "lab_management.system.reservation_rejected";
	public static final String RESERVATION_ABSENT_MESSAGE_TYPE = "lab_management.system.reservation_absent";
	public static final String RESERVATION_FORCED_CHECKOUT_MESSAGE_TYPE = "lab_management.system.reservation_forced_checkout";
	public static final String LAB_STATUS_CHANGED_MESSAGE_TYPE = "lab_management.system.lab_status_changed";
	public static final String ENVIRONMENT_ALERT_MESSAGE_TYPE = "lab.warning.environment_alert";
	public static final int RESERVATION_CHECK_IN_BEFORE_START_MINUTES = 5;
	public static final int RESERVATION_CHECK_IN_AFTER_START_MINUTES = 30;
	public static final int RESERVATION_NO_RANDOM_DIGITS = 6;
	public static final int RESERVATION_NO_MAX_RETRY = 5;

	public static final String LAB_TYPES_ALL_REDIS_KEY = "lab:types:all";
	public static final String LAB_TYPES_ALL_LOCAL_CACHE_KEY = "all";
	public static final Duration LAB_TYPES_CACHE_TTL = Duration.ofMinutes(30);

	public static final String LAB_INDICATORS_REDIS_KEY_PREFIX = "lab:indicators:";
	public static final Duration LAB_INDICATORS_CACHE_TTL = Duration.ofMinutes(30);
	public static final String LAB_OCCUPANCY_REDIS_KEY_PREFIX = "lab:occupancy:";
	public static final Duration LAB_OCCUPANCY_CACHE_TTL = Duration.ofMinutes(5);
	public static final String LAB_SCHEDULE_REDIS_KEY_PREFIX = "lab:schedule:";
	public static final Duration LAB_SCHEDULE_CACHE_TTL = Duration.ofMinutes(30);
	public static final String LAB_SENSOR_MESSAGE_PROCESSED_REDIS_KEY_PREFIX = "lab:sensor:message:processed:";
	public static final Duration LAB_SENSOR_MESSAGE_PROCESSED_TTL = Duration.ofDays(7);
	public static final String LAB_ENV_ALERT_COOLDOWN_REDIS_KEY_PREFIX = "lab:env:alert:cooldown:";
	public static final Duration LAB_ENV_ALERT_COOLDOWN_TTL = Duration.ofMinutes(5);

	public static String labIndicatorsRedisKey(Long labTypeId) {
		return LAB_INDICATORS_REDIS_KEY_PREFIX + labTypeId;
	}

	public static String labOccupancyRedisKey(Long labId, java.time.LocalDate date) {
		return LAB_OCCUPANCY_REDIS_KEY_PREFIX + labId + ":" + date;
	}

	public static String labScheduleRedisKey(Long labId) {
		return LAB_SCHEDULE_REDIS_KEY_PREFIX + labId;
	}

	public static String labSensorMessageProcessedRedisKey(String eventNo) {
		return LAB_SENSOR_MESSAGE_PROCESSED_REDIS_KEY_PREFIX + eventNo;
	}

	public static String labEnvironmentAlertCooldownRedisKey(Long labId, Long sensorId, String indicatorName,
			String exceedDirection) {
		return LAB_ENV_ALERT_COOLDOWN_REDIS_KEY_PREFIX + labId + ":" + sensorId + ":" + indicatorName + ":"
				+ exceedDirection;
	}
}
