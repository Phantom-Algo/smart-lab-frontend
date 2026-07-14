package com.zhangjintao.project.smart_lab_backend.modules.sensor_env.controller;

import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.model.dto.SensorDataTypeCreateCommand;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.model.dto.SensorDataTypeUpdateCommand;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.model.dto.SensorTypeCreateCommand;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.model.dto.SensorTypeUpdateCommand;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.model.request.CreateSensorDataTypeRequest;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.model.request.CreateSensorTypeRequest;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.model.request.UpdateSensorDataTypeRequest;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.model.request.UpdateSensorTypeRequest;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.model.response.SensorDataTypeResponse;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.model.response.SensorTypeDetailResponse;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.model.response.SensorTypeListItemResponse;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.service.SensorTypeService;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SensorTypeControllerTest {

	@Test
	void allEndpointsShouldRequireAdminRole() {
		PreAuthorize preAuthorize = SensorTypeController.class.getAnnotation(PreAuthorize.class);

		assertThat(preAuthorize).isNotNull();
		assertThat(preAuthorize.value()).isEqualTo("hasRole('ADMIN')");
	}

	@Test
	void controllerShouldDelegateEightOperations() {
		RecordingSensorTypeService service = new RecordingSensorTypeService();
		SensorTypeController controller = new SensorTypeController(service);

		CreateSensorTypeRequest createTypeRequest = new CreateSensorTypeRequest();
		createTypeRequest.setSensorTypeName("温湿度传感器");
		controller.create(createTypeRequest);
		controller.listAll();
		controller.getById(1L);

		UpdateSensorTypeRequest updateTypeRequest = new UpdateSensorTypeRequest();
		updateTypeRequest.setSensorTypeName("温湿度检测器");
		controller.update(1L, updateTypeRequest);
		controller.delete(1L);

		CreateSensorDataTypeRequest createDataTypeRequest = new CreateSensorDataTypeRequest();
		createDataTypeRequest.setDataName("temperature");
		createDataTypeRequest.setUnit("°C");
		controller.createDataType(1L, createDataTypeRequest);

		UpdateSensorDataTypeRequest updateDataTypeRequest = new UpdateSensorDataTypeRequest();
		updateDataTypeRequest.setDataName("room_temperature");
		updateDataTypeRequest.setUnit("°C");
		controller.updateDataType(2L, updateDataTypeRequest);
		controller.deleteDataType(2L);

		assertThat(service.operations).containsExactly(
				"create", "listAll", "getById:1", "update:1", "delete:1",
				"createDataType:1", "updateDataType:2", "deleteDataType:2");
	}

	private static class RecordingSensorTypeService implements SensorTypeService {

		private final java.util.ArrayList<String> operations = new java.util.ArrayList<>();

		@Override
		public SensorTypeDetailResponse create(SensorTypeCreateCommand command) {
			operations.add("create");
			return detail(1L, command.getSensorTypeName());
		}

		@Override
		public List<SensorTypeListItemResponse> listAll() {
			operations.add("listAll");
			return List.of();
		}

		@Override
		public SensorTypeDetailResponse getById(Long sensorTypeId) {
			operations.add("getById:" + sensorTypeId);
			return detail(sensorTypeId, "温湿度传感器");
		}

		@Override
		public SensorTypeDetailResponse update(Long sensorTypeId, SensorTypeUpdateCommand command) {
			operations.add("update:" + sensorTypeId);
			return detail(sensorTypeId, command.getSensorTypeName());
		}

		@Override
		public void delete(Long sensorTypeId) {
			operations.add("delete:" + sensorTypeId);
		}

		@Override
		public SensorDataTypeResponse createDataType(Long sensorTypeId, SensorDataTypeCreateCommand command) {
			operations.add("createDataType:" + sensorTypeId);
			return dataType(2L, sensorTypeId, command.getDataName());
		}

		@Override
		public SensorDataTypeResponse updateDataType(Long dataTypeId, SensorDataTypeUpdateCommand command) {
			operations.add("updateDataType:" + dataTypeId);
			return dataType(dataTypeId, 1L, command.getDataName());
		}

		@Override
		public void deleteDataType(Long dataTypeId) {
			operations.add("deleteDataType:" + dataTypeId);
		}

		private SensorTypeDetailResponse detail(Long id, String name) {
			return SensorTypeDetailResponse.builder()
					.id(id)
					.sensorTypeName(name)
					.dataTypes(List.of())
					.build();
		}

		private SensorDataTypeResponse dataType(Long id, Long sensorTypeId, String dataName) {
			return SensorDataTypeResponse.builder()
					.id(id)
					.sensorTypeId(sensorTypeId)
					.dataName(dataName)
					.build();
		}
	}
}
