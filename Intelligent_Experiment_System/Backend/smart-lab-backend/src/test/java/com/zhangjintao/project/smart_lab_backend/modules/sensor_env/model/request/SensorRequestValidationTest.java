package com.zhangjintao.project.smart_lab_backend.modules.sensor_env.model.request;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SensorRequestValidationTest {

	private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

	@Test
	void createShouldAcceptCanonicalSensorNo() {
		CreateSensorRequest request = validRequest();
		assertThat(validator.validate(request)).isEmpty();
	}

	@Test
	void createShouldRejectLowerCaseSensorNo() {
		CreateSensorRequest request = validRequest();
		request.setSensorNo("sensor-001");
		assertThat(validator.validate(request))
				.anySatisfy(violation -> assertThat(violation.getMessage()).contains("大写字母"));
	}

	@Test
	void pageShouldRejectUnsupportedStatusCode() {
		SensorPageQueryRequest request = new SensorPageQueryRequest();
		request.setRunningStatus(4);
		assertThat(validator.validate(request))
				.anySatisfy(violation -> assertThat(violation.getMessage()).contains("1到3"));
	}

	private CreateSensorRequest validRequest() {
		CreateSensorRequest request = new CreateSensorRequest();
		request.setSensorNo("SENSOR-001");
		request.setSensorTypeId(1L);
		request.setTargetType(2);
		request.setTargetId(100L);
		return request;
	}
}
