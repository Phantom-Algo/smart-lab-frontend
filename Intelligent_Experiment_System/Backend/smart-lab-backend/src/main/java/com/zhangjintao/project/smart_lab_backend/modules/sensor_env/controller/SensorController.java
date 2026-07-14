package com.zhangjintao.project.smart_lab_backend.modules.sensor_env.controller;

import com.zhangjintao.project.smart_lab_backend.common.result.ApiResult;
import com.zhangjintao.project.smart_lab_backend.common.result.PageResult;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.model.converter.SensorEnvConverter;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.model.request.CreateSensorRequest;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.model.request.SensorPageQueryRequest;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.model.request.UpdateSensorRequest;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.model.request.UpdateSensorStatusRequest;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.model.response.SensorResponse;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.service.SensorService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/iot/sensors")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class SensorController {

	private final SensorService sensorService;

	@PostMapping
	public ApiResult<SensorResponse> create(@Valid @RequestBody CreateSensorRequest request) {
		return ApiResult.success(sensorService.create(SensorEnvConverter.toCreateCommand(request)));
	}

	@GetMapping("/{sensorId}")
	public ApiResult<SensorResponse> getById(
			@PathVariable @Positive(message = "传感器ID必须为正数") Long sensorId) {
		return ApiResult.success(sensorService.getById(sensorId));
	}

	@GetMapping
	public ApiResult<PageResult<SensorResponse>> page(@Valid @ModelAttribute SensorPageQueryRequest request) {
		return ApiResult.success(sensorService.page(SensorEnvConverter.toCriteria(request)));
	}

	@PutMapping("/{sensorId}")
	public ApiResult<SensorResponse> update(
			@PathVariable @Positive(message = "传感器ID必须为正数") Long sensorId,
			@Valid @RequestBody UpdateSensorRequest request) {
		return ApiResult.success(sensorService.update(sensorId, SensorEnvConverter.toUpdateCommand(request)));
	}

	@PatchMapping("/{sensorId}/status")
	public ApiResult<SensorResponse> updateStatus(
			@PathVariable @Positive(message = "传感器ID必须为正数") Long sensorId,
			@Valid @RequestBody UpdateSensorStatusRequest request) {
		return ApiResult.success(sensorService.updateStatus(sensorId,
				SensorEnvConverter.toStatusUpdateCommand(request)));
	}

	@DeleteMapping("/{sensorId}")
	public ApiResult<Void> delete(
			@PathVariable @Positive(message = "传感器ID必须为正数") Long sensorId) {
		sensorService.delete(sensorId);
		return ApiResult.success();
	}
}
