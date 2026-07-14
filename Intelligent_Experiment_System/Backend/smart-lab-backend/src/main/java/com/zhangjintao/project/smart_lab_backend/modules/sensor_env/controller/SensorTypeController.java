package com.zhangjintao.project.smart_lab_backend.modules.sensor_env.controller;

import com.zhangjintao.project.smart_lab_backend.common.result.ApiResult;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.model.converter.SensorEnvConverter;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.model.request.CreateSensorDataTypeRequest;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.model.request.CreateSensorTypeRequest;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.model.request.UpdateSensorDataTypeRequest;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.model.request.UpdateSensorTypeRequest;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.model.response.SensorDataTypeResponse;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.model.response.SensorTypeDetailResponse;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.model.response.SensorTypeListItemResponse;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.service.SensorTypeService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/iot")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class SensorTypeController {

	private final SensorTypeService sensorTypeService;

	@PostMapping("/sensor-types")
	public ApiResult<SensorTypeDetailResponse> create(@Valid @RequestBody CreateSensorTypeRequest request) {
		return ApiResult.success(sensorTypeService.create(SensorEnvConverter.toCreateCommand(request)));
	}

	@GetMapping("/sensor-types")
	public ApiResult<List<SensorTypeListItemResponse>> listAll() {
		return ApiResult.success(sensorTypeService.listAll());
	}

	@GetMapping("/sensor-types/{sensorTypeId}")
	public ApiResult<SensorTypeDetailResponse> getById(
			@PathVariable @Positive(message = "传感器类型ID必须为正数") Long sensorTypeId) {
		return ApiResult.success(sensorTypeService.getById(sensorTypeId));
	}

	@PutMapping("/sensor-types/{sensorTypeId}")
	public ApiResult<SensorTypeDetailResponse> update(
			@PathVariable @Positive(message = "传感器类型ID必须为正数") Long sensorTypeId,
			@Valid @RequestBody UpdateSensorTypeRequest request) {
		return ApiResult.success(sensorTypeService.update(sensorTypeId, SensorEnvConverter.toUpdateCommand(request)));
	}

	@DeleteMapping("/sensor-types/{sensorTypeId}")
	public ApiResult<Void> delete(
			@PathVariable @Positive(message = "传感器类型ID必须为正数") Long sensorTypeId) {
		sensorTypeService.delete(sensorTypeId);
		return ApiResult.success();
	}

	@PostMapping("/sensor-types/{sensorTypeId}/data-types")
	public ApiResult<SensorDataTypeResponse> createDataType(
			@PathVariable @Positive(message = "传感器类型ID必须为正数") Long sensorTypeId,
			@Valid @RequestBody CreateSensorDataTypeRequest request) {
		return ApiResult.success(sensorTypeService.createDataType(sensorTypeId,
				SensorEnvConverter.toCreateCommand(request)));
	}

	@PutMapping("/data-types/{dataTypeId}")
	public ApiResult<SensorDataTypeResponse> updateDataType(
			@PathVariable @Positive(message = "检测数据项ID必须为正数") Long dataTypeId,
			@Valid @RequestBody UpdateSensorDataTypeRequest request) {
		return ApiResult.success(sensorTypeService.updateDataType(dataTypeId,
				SensorEnvConverter.toUpdateCommand(request)));
	}

	@DeleteMapping("/data-types/{dataTypeId}")
	public ApiResult<Void> deleteDataType(
			@PathVariable @Positive(message = "检测数据项ID必须为正数") Long dataTypeId) {
		sensorTypeService.deleteDataType(dataTypeId);
		return ApiResult.success();
	}
}
