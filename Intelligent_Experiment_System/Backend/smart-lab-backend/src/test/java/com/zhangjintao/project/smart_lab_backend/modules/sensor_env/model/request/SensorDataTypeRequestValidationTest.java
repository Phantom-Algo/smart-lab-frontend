package com.zhangjintao.project.smart_lab_backend.modules.sensor_env.model.request;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SensorDataTypeRequestValidationTest {

	private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

	@Test
	void createRequestShouldAcceptLowerSnakeCase() {
		CreateSensorDataTypeRequest request = new CreateSensorDataTypeRequest();
		request.setDataName("gas_concentration");
		request.setUnit("ppm");

		assertThat(validator.validate(request)).isEmpty();
	}

	@Test
	void createRequestShouldRejectNonLowerSnakeCase() {
		CreateSensorDataTypeRequest request = new CreateSensorDataTypeRequest();
		request.setDataName("GasConcentration");
		request.setUnit("ppm");

		assertThat(validator.validate(request))
				.anySatisfy(violation -> assertThat(violation.getMessage())
						.isEqualTo("检测数据名称必须使用lower_snake_case"));
	}

	@Test
	void updateRequestShouldRejectBlankUnit() {
		UpdateSensorDataTypeRequest request = new UpdateSensorDataTypeRequest();
		request.setDataName("temperature");
		request.setUnit(" ");

		assertThat(validator.validate(request))
				.anySatisfy(violation -> assertThat(violation.getMessage()).isEqualTo("检测数据单位不能为空"));
	}
}
