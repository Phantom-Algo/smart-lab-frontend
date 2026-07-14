package com.zhangjintao.project.smart_lab_backend.modules.lab_management.utils;

import com.zhangjintao.project.smart_lab_backend.modules.lab_management.exception.LabManagementErrorCode;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.exception.LabManagementException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LabAlertMessageEventNoGeneratorTest {

	@Test
	void generateShouldReuseIotEventSerialAndUseLabMessageTypePrefix() {
		String eventNo = LabAlertMessageEventNoGenerator.generate("iot.sensor.data.report:1234567890123456");

		assertThat(eventNo).isEqualTo("lab.warning.environment_alert:1234567890123456");
	}

	@Test
	void generateInvalidIotEventNoShouldThrowBusinessException() {
		assertThatThrownBy(() -> LabAlertMessageEventNoGenerator.generate("iot.sensor.data.report:abc"))
				.isInstanceOfSatisfying(LabManagementException.class, ex ->
						assertThat(ex.getCode()).isEqualTo(LabManagementErrorCode.LAB_SENSOR_DATA_INVALID.getCode()));
	}
}
