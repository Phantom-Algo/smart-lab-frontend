package com.zhangjintao.project.smart_lab_backend.modules.sensor_env.constants;

import java.time.Duration;
import java.util.regex.Pattern;

public final class SensorEnvConstants {

	private SensorEnvConstants() {
	}

	public static final String SENSOR_TYPE_CATALOG_REDIS_KEY = "sensor_env:sensor_type_catalog:all";
	public static final String SENSOR_TYPE_CATALOG_LOCAL_CACHE_KEY = "all";
	public static final Duration SENSOR_TYPE_CATALOG_CACHE_TTL = Duration.ofMinutes(30);
	public static final String DATA_NAME_REGEX = "^[a-z][a-z0-9_]{0,63}$";
	public static final Pattern DATA_NAME_PATTERN = Pattern.compile(DATA_NAME_REGEX);
	public static final String SENSOR_NO_REGEX = "^[A-Z0-9][A-Z0-9_-]{0,63}$";
	public static final Pattern SENSOR_NO_PATTERN = Pattern.compile(SENSOR_NO_REGEX);
	public static final String SENSOR_RUNTIME_REDIS_KEY_PREFIX = "sensor_env:sensor_runtime:";
	public static final Duration SENSOR_RUNTIME_CACHE_TTL = Duration.ofMinutes(10);
	public static final long DEFAULT_PAGE = 1L;
	public static final long DEFAULT_PAGE_SIZE = 20L;
	public static final String SOURCE_SYSTEM = "iot";
	public static final String SENSOR_STATUS_CHANGED_MESSAGE_TYPE = "iot.sensor.status.changed";
	public static final String SENSOR_DATA_REPORTED_EVENT_TYPE = "iot.sensor.data.report";
	public static final int EVENT_NO_RANDOM_DIGITS = 16;

	public static String sensorRuntimeRedisKey(String sensorNo) {
		return SENSOR_RUNTIME_REDIS_KEY_PREFIX + sensorNo;
	}
}
