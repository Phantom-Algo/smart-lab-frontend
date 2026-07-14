package com.zhangjintao.project.smart_lab_backend.modules.sensor_env.utils;

import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.constants.SensorEnvConstants;

import java.util.concurrent.ThreadLocalRandom;

public final class SensorDataEventNoGenerator {

	private SensorDataEventNoGenerator() {
	}

	public static String generate() {
		StringBuilder suffix = new StringBuilder(SensorEnvConstants.EVENT_NO_RANDOM_DIGITS);
		for (int i = 0; i < SensorEnvConstants.EVENT_NO_RANDOM_DIGITS; i++) {
			suffix.append(ThreadLocalRandom.current().nextInt(10));
		}
		return SensorEnvConstants.SENSOR_DATA_REPORTED_EVENT_TYPE + ":" + suffix;
	}
}
