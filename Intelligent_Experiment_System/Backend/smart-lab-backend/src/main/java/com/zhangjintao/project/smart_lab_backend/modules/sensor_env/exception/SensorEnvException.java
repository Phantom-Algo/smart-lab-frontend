package com.zhangjintao.project.smart_lab_backend.modules.sensor_env.exception;

import com.zhangjintao.project.smart_lab_backend.common.exception.BusinessException;

public class SensorEnvException extends BusinessException {

	public SensorEnvException(SensorEnvErrorCode errorCode) {
		super(errorCode);
	}

	public SensorEnvException(SensorEnvErrorCode errorCode, String message) {
		super(errorCode, message);
	}
}
