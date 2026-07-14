package com.zhangjintao.project.smart_lab_backend.modules.sensor_env.exception;

import com.zhangjintao.project.smart_lab_backend.common.exception.ErrorCode;
import org.springframework.http.HttpStatus;

public enum SensorEnvErrorCode implements ErrorCode {

	SENSOR_TYPE_NOT_FOUND(64000, "传感器类型不存在", HttpStatus.NOT_FOUND),
	SENSOR_DATA_TYPE_NOT_FOUND(64001, "检测数据项不存在", HttpStatus.NOT_FOUND),
	SENSOR_TYPE_NAME_DUPLICATE(64002, "传感器类型名称重复", HttpStatus.CONFLICT),
	SENSOR_DATA_TYPE_NAME_DUPLICATE(64003, "同一传感器类型下检测数据名称重复", HttpStatus.CONFLICT),
	SENSOR_TYPE_IN_USE(64004, "传感器类型已被传感器实例引用", HttpStatus.CONFLICT),
	SENSOR_ENV_PARAM_INVALID(64005, "物联网环境参数错误", HttpStatus.BAD_REQUEST),
	SENSOR_NOT_FOUND(64006, "传感器不存在", HttpStatus.NOT_FOUND),
	SENSOR_NO_DUPLICATE(64007, "传感器编号已存在", HttpStatus.CONFLICT),
	SENSOR_TARGET_NOT_FOUND(64008, "传感器绑定目标不存在", HttpStatus.NOT_FOUND),
	SENSOR_BINDING_CONFLICT(64009, "传感器绑定关系冲突", HttpStatus.CONFLICT),
	SENSOR_STATUS_TRANSITION_INVALID(64010, "传感器状态不允许当前操作", HttpStatus.CONFLICT),
	SENSOR_SORT_FIELD_INVALID(64011, "传感器排序字段不支持", HttpStatus.BAD_REQUEST),
	SENSOR_DATA_FIELD_MISMATCH(64012, "上报的检测数据字段名未在传感器类型中注册", HttpStatus.BAD_REQUEST),
	DETECTED_AT_INVALID(64013, "检测时间超出合法范围", HttpStatus.BAD_REQUEST),
	SENSOR_DATA_ROUTE_FAILED(64014, "传感器数据路由转发失败", HttpStatus.INTERNAL_SERVER_ERROR);

	private final int code;
	private final String message;
	private final HttpStatus httpStatus;

	SensorEnvErrorCode(int code, String message, HttpStatus httpStatus) {
		this.code = code;
		this.message = message;
		this.httpStatus = httpStatus;
	}

	@Override
	public int getCode() {
		return code;
	}

	@Override
	public String getMessage() {
		return message;
	}

	@Override
	public HttpStatus getHttpStatus() {
		return httpStatus;
	}
}
