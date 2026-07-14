package com.zhangjintao.project.smart_lab_backend.modules.lab_management.utils;

import com.zhangjintao.project.smart_lab_backend.modules.lab_management.constants.LabManagementConstants;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.exception.LabManagementErrorCode;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.exception.LabManagementException;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class LabAlertMessageEventNoGenerator {

	private static final Pattern IOT_EVENT_NO_PATTERN =
			Pattern.compile("^iot\\.sensor\\.data\\.report:(\\d{16})$");

	private LabAlertMessageEventNoGenerator() {
	}

	public static String generate(String iotEventNo) {
		Matcher matcher = IOT_EVENT_NO_PATTERN.matcher(iotEventNo == null ? "" : iotEventNo);
		if (!matcher.matches()) {
			throw new LabManagementException(LabManagementErrorCode.LAB_SENSOR_DATA_INVALID,
					"IoT事件编号格式不合法，eventNo=" + iotEventNo);
		}
		return LabManagementConstants.ENVIRONMENT_ALERT_MESSAGE_TYPE + ":" + matcher.group(1);
	}
}
