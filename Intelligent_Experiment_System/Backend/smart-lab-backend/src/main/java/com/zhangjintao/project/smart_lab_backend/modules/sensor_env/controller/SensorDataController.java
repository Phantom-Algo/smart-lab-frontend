package com.zhangjintao.project.smart_lab_backend.modules.sensor_env.controller;

import com.zhangjintao.project.smart_lab_backend.common.result.ApiResult;
import com.zhangjintao.project.smart_lab_backend.common.result.PageResult;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.constants.SensorEnvConstants;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.model.converter.SensorEnvConverter;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.model.dto.SensorWorkLogQueryCriteria;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.model.request.ReceiveSensorDataRequest;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.model.request.SensorWorkLogQueryRequest;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.model.response.LabIotSummaryResponse;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.model.response.SensorDataReceiveResponse;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.model.response.SensorWorkLogResponse;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.service.SensorDataService;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.service.LabIotSummaryService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 传感器数据控制器。
 * <p>
 * 提供模拟数据接收（开发阶段无需认证）和历史数据查询（仅管理员）接口。
 * </p>
 *
 * @author 张津涛
 * @since 2026-07-10
 */
@RestController
@RequestMapping("/api/iot")
@RequiredArgsConstructor
public class SensorDataController {

	private final SensorDataService sensorDataService;	private final LabIotSummaryService labIotSummaryService;
	/**
	 * 接收模拟传感器数据。
	 * <p>
	 * 开发阶段无需认证，供外部模拟程序直接调用。
	 * 生产环境应在此增加 API Key 校验。
	 * </p>
	 */
	@PostMapping("/sensor-data")
	public ApiResult<SensorDataReceiveResponse> receiveData(@Valid @RequestBody ReceiveSensorDataRequest request) {
		SensorDataReceiveResponse response = sensorDataService.receive(
				SensorEnvConverter.toDataReceiveCommand(request));
		return ApiResult.success(response);
	}

	/**
	 * 查询传感器历史数据（分页）。
	 * 仅管理员可访问。
	 */
	@GetMapping("/sensors/{sensorId}/logs")
	@PreAuthorize("hasRole('ADMIN')")
	public ApiResult<PageResult<SensorWorkLogResponse>> queryLogs(
			@PathVariable Long sensorId,
			@Valid SensorWorkLogQueryRequest request) {
		SensorWorkLogQueryCriteria criteria = buildCriteria(sensorId, request);
		PageResult<SensorWorkLogResponse> page = sensorDataService.queryLogs(criteria);
		return ApiResult.success(page);
	}

	/**
	 * 查询传感器最新一条工作日志。
	 * 仅管理员可访问。
	 */
	@GetMapping("/sensors/{sensorId}/logs/latest")
	@PreAuthorize("hasRole('ADMIN')")
	public ApiResult<SensorWorkLogResponse> getLatestLog(@PathVariable Long sensorId) {
		SensorWorkLogResponse response = sensorDataService.getLatestLog(sensorId);
		return ApiResult.success(response);
	}

	/**
	 * 查询实验室 IoT 数据汇总（供 Unity 3D 场景展示）。
	 * 公开接口，仅需登录即可访问（无需管理员权限），确保 Unity WebGL 内嵌页面可正常调用。
	 */
	@GetMapping("/labs/{labId}/summary")
	public ApiResult<LabIotSummaryResponse> getLabIotSummary(
			@PathVariable @Positive(message = "实验室ID必须为正数") Long labId) {
		return ApiResult.success(labIotSummaryService.buildSummary(labId));
	}

	/**
	 * 将请求参数转换为查询条件，设置默认值。
	 */
	private SensorWorkLogQueryCriteria buildCriteria(Long sensorId, SensorWorkLogQueryRequest request) {
		LocalDateTime now = LocalDateTime.now();
		LocalDateTime startTime = parseTimeOrDefault(request.getStartTime(), now.minusHours(24));
		LocalDateTime endTime = parseTimeOrDefault(request.getEndTime(), now);
		long page = request.getPage() != null ? request.getPage() : SensorEnvConstants.DEFAULT_PAGE;
		long pageSize = request.getPageSize() != null ? request.getPageSize() : SensorEnvConstants.DEFAULT_PAGE_SIZE;

		return SensorWorkLogQueryCriteria.builder()
				.sensorId(sensorId)
				.startTime(startTime)
				.endTime(endTime)
				.page(page)
				.pageSize(pageSize)
				.build();
	}

	private LocalDateTime parseTimeOrDefault(String timeStr, LocalDateTime defaultValue) {
		if (timeStr == null || timeStr.isBlank()) {
			return defaultValue;
		}
		return LocalDateTime.parse(timeStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
	}
}
