package com.zhangjintao.project.smart_lab_backend.modules.lab_management.controller;

import com.zhangjintao.project.smart_lab_backend.common.result.ApiResult;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.dto.DetectionIndicatorCreateCommand;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.dto.DetectionIndicatorUpdateCommand;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.request.CreateDetectionIndicatorRequest;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.request.UpdateDetectionIndicatorRequest;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.response.DetectionIndicatorResponse;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.service.DetectionIndicatorService;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DetectionIndicatorControllerTest {

	@Test
	void adminWriteMethodsRequireAdminRole() throws NoSuchMethodException {
		Method create = DetectionIndicatorController.class.getMethod("create", CreateDetectionIndicatorRequest.class);
		Method update = DetectionIndicatorController.class.getMethod("update", Long.class,
				UpdateDetectionIndicatorRequest.class);
		Method delete = DetectionIndicatorController.class.getMethod("delete", Long.class);

		assertThat(create.getAnnotation(PreAuthorize.class).value()).isEqualTo("hasRole('ADMIN')");
		assertThat(update.getAnnotation(PreAuthorize.class).value()).isEqualTo("hasRole('ADMIN')");
		assertThat(delete.getAnnotation(PreAuthorize.class).value()).isEqualTo("hasRole('ADMIN')");
	}

	@Test
	void listByLabTypeIdDoesNotRequireAdminRole() throws NoSuchMethodException {
		Method listByLabTypeId = DetectionIndicatorController.class.getMethod("listByLabTypeId", Long.class);

		assertThat(listByLabTypeId.getAnnotation(PreAuthorize.class)).isNull();
	}

	@Test
	void createDelegatesToService() {
		RecordingDetectionIndicatorService service = new RecordingDetectionIndicatorService();
		DetectionIndicatorController controller = new DetectionIndicatorController(service);
		CreateDetectionIndicatorRequest request = new CreateDetectionIndicatorRequest();
		request.setLabTypeId(1L);
		request.setIndicatorName(" temperature ");
		request.setUnit(" ℃ ");
		request.setSafeRange(List.of(Arrays.asList(null, BigDecimal.TEN)));

		ApiResult<DetectionIndicatorResponse> result = controller.create(request);

		assertThat(service.createCommand.getLabTypeId()).isEqualTo(1L);
		assertThat(service.createCommand.getIndicatorName()).isEqualTo("temperature");
		assertThat(service.createCommand.getUnit()).isEqualTo("℃");
		assertThat(service.createCommand.getSafeRange()).containsExactly(Arrays.asList(null, BigDecimal.TEN));
		assertThat(result.getData().getId()).isEqualTo(1L);
	}

	@Test
	void updateDelegatesToService() {
		RecordingDetectionIndicatorService service = new RecordingDetectionIndicatorService();
		DetectionIndicatorController controller = new DetectionIndicatorController(service);
		UpdateDetectionIndicatorRequest request = new UpdateDetectionIndicatorRequest();
		request.setLabTypeId(2L);
		request.setIndicatorName(" humidity ");
		request.setUnit("%");
		request.setSafeRange(List.of(List.of(BigDecimal.ZERO, BigDecimal.valueOf(80))));

		ApiResult<DetectionIndicatorResponse> result = controller.update(11L, request);

		assertThat(service.updateIndicatorId).isEqualTo(11L);
		assertThat(service.updateCommand.getLabTypeId()).isEqualTo(2L);
		assertThat(service.updateCommand.getIndicatorName()).isEqualTo("humidity");
		assertThat(result.getData().getId()).isEqualTo(11L);
	}

	@Test
	void deleteDelegatesToService() {
		RecordingDetectionIndicatorService service = new RecordingDetectionIndicatorService();
		DetectionIndicatorController controller = new DetectionIndicatorController(service);

		controller.delete(11L);

		assertThat(service.deleteIndicatorId).isEqualTo(11L);
	}

	@Test
	void listByLabTypeIdDelegatesToService() {
		RecordingDetectionIndicatorService service = new RecordingDetectionIndicatorService();
		DetectionIndicatorController controller = new DetectionIndicatorController(service);

		ApiResult<List<DetectionIndicatorResponse>> result = controller.listByLabTypeId(1L);

		assertThat(service.listLabTypeId).isEqualTo(1L);
		assertThat(result.getData()).extracting(DetectionIndicatorResponse::getIndicatorName)
				.containsExactly("temperature");
	}

	private static class RecordingDetectionIndicatorService implements DetectionIndicatorService {

		private DetectionIndicatorCreateCommand createCommand;
		private Long updateIndicatorId;
		private DetectionIndicatorUpdateCommand updateCommand;
		private Long deleteIndicatorId;
		private Long listLabTypeId;

		@Override
		public DetectionIndicatorResponse create(DetectionIndicatorCreateCommand command) {
			this.createCommand = command;
			return response(1L, command.getLabTypeId(), command.getIndicatorName(), command.getUnit(), command.getSafeRange());
		}

		@Override
		public DetectionIndicatorResponse update(Long indicatorId, DetectionIndicatorUpdateCommand command) {
			this.updateIndicatorId = indicatorId;
			this.updateCommand = command;
			return response(indicatorId, command.getLabTypeId(), command.getIndicatorName(), command.getUnit(),
					command.getSafeRange());
		}

		@Override
		public void delete(Long indicatorId) {
			this.deleteIndicatorId = indicatorId;
		}

		@Override
		public List<DetectionIndicatorResponse> listByLabTypeId(Long labTypeId) {
			this.listLabTypeId = labTypeId;
			return List.of(response(1L, labTypeId, "temperature", "℃",
					List.of(List.of(BigDecimal.ZERO, BigDecimal.TEN))));
		}

		private DetectionIndicatorResponse response(Long id, Long labTypeId, String indicatorName, String unit,
				List<List<BigDecimal>> safeRange) {
			return DetectionIndicatorResponse.builder()
					.id(id)
					.labTypeId(labTypeId)
					.indicatorName(indicatorName)
					.unit(unit)
					.safeRange(safeRange)
					.build();
		}
	}
}
