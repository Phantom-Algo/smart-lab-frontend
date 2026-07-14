package com.zhangjintao.project.smart_lab_backend.modules.sensor_env.controller;

import com.zhangjintao.project.smart_lab_backend.common.result.PageResult;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.model.request.CreateSensorRequest;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.model.request.SensorPageQueryRequest;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.model.request.UpdateSensorRequest;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.model.request.UpdateSensorStatusRequest;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.model.response.SensorResponse;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.service.SensorService;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SensorControllerTest {

	@Test
	void allEndpointsShouldRequireAdminRole() {
		PreAuthorize preAuthorize = SensorController.class.getAnnotation(PreAuthorize.class);
		assertThat(preAuthorize).isNotNull();
		assertThat(preAuthorize.value()).isEqualTo("hasRole('ADMIN')");
	}

	@Test
	void controllerShouldDelegateSixOperations() {
		SensorService service = mock(SensorService.class);
		SensorController controller = new SensorController(service);
		when(service.page(any())).thenReturn(PageResult.empty(1, 20));

		CreateSensorRequest create = new CreateSensorRequest();
		create.setSensorNo("SENSOR-001");
		create.setSensorTypeId(1L);
		create.setTargetType(2);
		create.setTargetId(100L);
		controller.create(create);
		controller.getById(10L);
		controller.page(new SensorPageQueryRequest());

		UpdateSensorRequest update = new UpdateSensorRequest();
		update.setSensorTypeId(1L);
		update.setTargetType(1);
		update.setTargetId(200L);
		controller.update(10L, update);

		UpdateSensorStatusRequest status = new UpdateSensorStatusRequest();
		status.setRunningStatus(3);
		status.setReason("损坏");
		controller.updateStatus(10L, status);
		controller.delete(10L);

		verify(service).create(any());
		verify(service).getById(10L);
		verify(service).page(any());
		verify(service).update(any(), any());
		verify(service).updateStatus(any(), any());
		verify(service).delete(10L);
	}
}
