package com.zhangjintao.project.smart_lab_backend.modules.lab_management.controller;

import com.zhangjintao.project.smart_lab_backend.common.result.ApiResult;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.dto.LabCloseScheduleCreateCommand;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.dto.LabCloseScheduleQueryCriteria;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.dto.LabCloseScheduleUpdateCommand;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.request.CreateLabCloseScheduleRequest;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.request.LabCloseScheduleQueryRequest;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.request.UpdateLabCloseScheduleRequest;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.response.LabCloseScheduleMutationResponse;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.response.LabCloseScheduleResponse;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.service.LabCloseScheduleService;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class LabCloseScheduleControllerTest {

	@Test
	void writeMethodsShouldRequireAdminRole() throws NoSuchMethodException {
		Method create = LabCloseScheduleController.class.getMethod("create", Long.class, CreateLabCloseScheduleRequest.class);
		Method update = LabCloseScheduleController.class.getMethod("update", Long.class, Long.class,
				UpdateLabCloseScheduleRequest.class);
		Method delete = LabCloseScheduleController.class.getMethod("delete", Long.class, Long.class);
		Method list = LabCloseScheduleController.class.getMethod("listByLabId", Long.class,
				LabCloseScheduleQueryRequest.class);

		assertThat(create.getAnnotation(PreAuthorize.class).value()).isEqualTo("hasRole('ADMIN')");
		assertThat(update.getAnnotation(PreAuthorize.class).value()).isEqualTo("hasRole('ADMIN')");
		assertThat(delete.getAnnotation(PreAuthorize.class).value()).isEqualTo("hasRole('ADMIN')");
		assertThat(list.getAnnotation(PreAuthorize.class)).isNull();
	}

	@Test
	void createShouldDelegateToService() {
		RecordingLabCloseScheduleService service = new RecordingLabCloseScheduleService();
		LabCloseScheduleController controller = new LabCloseScheduleController(service);
		CreateLabCloseScheduleRequest request = new CreateLabCloseScheduleRequest();
		request.setCloseType(2);
		request.setStartTime(LocalDateTime.of(2026, 7, 10, 9, 0));
		request.setEndTime(LocalDateTime.of(2026, 7, 10, 11, 0));
		request.setReason("检修");
		request.setForceCancelConflictedReservations(true);

		ApiResult<LabCloseScheduleMutationResponse> result = controller.create(100L, request);

		assertThat(service.createLabId).isEqualTo(100L);
		assertThat(service.createCommand.getCloseType()).isEqualTo(2);
		assertThat(service.createCommand.isForceCancelConflictedReservations()).isTrue();
		assertThat(result.getData().isApplied()).isTrue();
	}

	private static class RecordingLabCloseScheduleService implements LabCloseScheduleService {

		private Long createLabId;
		private LabCloseScheduleCreateCommand createCommand;

		@Override
		public LabCloseScheduleMutationResponse create(Long labId, LabCloseScheduleCreateCommand command) {
			this.createLabId = labId;
			this.createCommand = command;
			return mutationResponse();
		}

		@Override
		public LabCloseScheduleMutationResponse update(Long labId, Long scheduleId,
				LabCloseScheduleUpdateCommand command) {
			return mutationResponse();
		}

		@Override
		public void delete(Long labId, Long scheduleId) {
		}

		@Override
		public List<LabCloseScheduleResponse> listByLabId(Long labId, LabCloseScheduleQueryCriteria criteria) {
			return List.of();
		}

		private LabCloseScheduleMutationResponse mutationResponse() {
			return LabCloseScheduleMutationResponse.builder()
					.applied(true)
					.conflictedReservations(List.of())
					.build();
		}
	}
}
