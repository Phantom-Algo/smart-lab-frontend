package com.zhangjintao.project.smart_lab_backend.modules.lab_management.exception;

import com.zhangjintao.project.smart_lab_backend.common.exception.ErrorCode;
import org.springframework.http.HttpStatus;

public enum LabManagementErrorCode implements ErrorCode {

	LAB_NOT_FOUND(63000, "实验室不存在", HttpStatus.NOT_FOUND),
	LAB_TYPE_NOT_FOUND(63001, "实验室类型不存在", HttpStatus.NOT_FOUND),
	LAB_LOCATION_DUPLICATE(63002, "同一位置已存在实验室", HttpStatus.CONFLICT),
	DUTY_USER_NOT_FOUND(63003, "实验室负责人不存在", HttpStatus.BAD_REQUEST),
	LAB_PARAM_INVALID(63004, "实验室参数错误", HttpStatus.BAD_REQUEST),
	LAB_SORT_FIELD_INVALID(63005, "实验室排序字段不支持", HttpStatus.BAD_REQUEST),
	LAB_TYPE_NAME_DUPLICATE(63006, "实验室类型名称重复", HttpStatus.CONFLICT),
	LAB_TYPE_IN_USE(63007, "实验室类型已被实验室引用", HttpStatus.CONFLICT),
	DETECTION_INDICATOR_NOT_FOUND(63008, "检测指标不存在", HttpStatus.NOT_FOUND),
	DETECTION_INDICATOR_NAME_DUPLICATE(63009, "检测指标名称重复", HttpStatus.CONFLICT),
	DETECTION_SAFE_RANGE_INVALID(63010, "检测指标安全区间不合法", HttpStatus.BAD_REQUEST),
	RESERVATION_NOT_FOUND(63011, "实验室预约不存在", HttpStatus.NOT_FOUND),
	RESERVATION_PERMISSION_DENIED(63012, "无权操作该预约", HttpStatus.FORBIDDEN),
	RESERVATION_TIME_INVALID(63013, "预约时间不合法", HttpStatus.BAD_REQUEST),
	RESERVATION_OUT_OF_OPEN_SCHEDULE(63014, "预约时间不在实验室开放时间内", HttpStatus.BAD_REQUEST),
	RESERVATION_CLOSE_SCHEDULE_CONFLICT(63015, "预约时间与实验室不开放时段冲突", HttpStatus.CONFLICT),
	RESERVATION_TIME_CONFLICT(63016, "预约时间段已被占用", HttpStatus.CONFLICT),
	RESERVATION_STATUS_INVALID(63017, "预约状态不允许当前操作", HttpStatus.CONFLICT),
	RESERVATION_CHECK_IN_WINDOW_INVALID(63018, "当前时间不在允许签到窗口内", HttpStatus.BAD_REQUEST),
	RESERVATION_CHECK_OUT_TIME_INVALID(63019, "当前时间不满足签退要求", HttpStatus.BAD_REQUEST),
	LAB_STATUS_TRANSITION_INVALID(63020, "实验室状态不允许当前操作", HttpStatus.CONFLICT),
	RESERVATION_NO_GENERATE_FAILED(63021, "预约单号生成失败", HttpStatus.INTERNAL_SERVER_ERROR),
	RESERVATION_RELATED_USER_NOT_FOUND(63022, "预约关联用户不存在", HttpStatus.BAD_REQUEST),
	LAB_SCHEDULE_NOT_FOUND(63023, "实验室开放时间不存在", HttpStatus.NOT_FOUND),
	LAB_SCHEDULE_PARAM_INVALID(63024, "实验室开放时间参数错误", HttpStatus.BAD_REQUEST),
	LAB_CLOSE_SCHEDULE_NOT_FOUND(63025, "实验室临时关闭时间段不存在", HttpStatus.NOT_FOUND),
	LAB_CLOSE_SCHEDULE_PARAM_INVALID(63026, "实验室临时关闭参数错误", HttpStatus.BAD_REQUEST),
	LAB_SENSOR_BINDING_CONFLICT(63027, "实验室传感器绑定关系冲突", HttpStatus.CONFLICT),
	LAB_HAS_ACTIVE_SENSOR_BINDING(63028, "实验室存在有效传感器绑定", HttpStatus.CONFLICT),
	LAB_SENSOR_DATA_INVALID(63029, "实验室传感器数据不合法", HttpStatus.BAD_REQUEST),
	LAB_DETECTION_LOG_NOT_FOUND(63030, "实验室检测日志不存在", HttpStatus.NOT_FOUND),
	LAB_ENVIRONMENT_ALERT_RECIPIENT_NOT_FOUND(63031, "实验室环境预警接收人不存在", HttpStatus.BAD_REQUEST);

	private final int code;
	private final String message;
	private final HttpStatus httpStatus;

	LabManagementErrorCode(int code, String message, HttpStatus httpStatus) {
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
