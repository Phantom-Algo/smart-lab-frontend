package com.zhangjintao.project.smart_lab_backend.modules.lab_management.controller;

import com.zhangjintao.project.smart_lab_backend.common.result.ApiResult;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.dto.LabScheduleSaveCommand;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.request.SetLabSchedulesRequest;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.response.LabScheduleMutationResponse;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.response.LabScheduleResponse;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.service.LabScheduleService;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.reflect.Method;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class LabScheduleControllerTest {

	@Test
	void writeMethodsShouldRequireAdminRole() throws NoSuchMethodException {
		Method save = LabScheduleController.class.getMethod("save", Long.class, SetLabSchedulesRequest.class);
		Method delete = LabScheduleController.class.getMethod("delete", Long.class, Long.class, boolean.class);
		Method list = LabScheduleController.class.getMethod("listByLabId", Long.class);

		assertThat(save.getAnnotation(PreAuthorize.class).value()).isEqualTo("hasRole('ADMIN')");
		assertThat(delete.getAnnotation(PreAuthorize.class).value()).isEqualTo("hasRole('ADMIN')");
		assertThat(list.getAnnotation(PreAuthorize.class)).isNull();
	}

	@Test
	void saveShouldDelegateToService() {
		RecordingLabScheduleService service = new RecordingLabScheduleService();
		LabScheduleController controller = new LabScheduleController(service);
		SetLabSchedulesRequest request = new SetLabSchedulesRequest();
		SetLabSchedulesRequest.ScheduleItem item = new SetLabSchedulesRequest.ScheduleItem();
		item.setDayOfWeek(1);
		item.setOpenTime(LocalTime.of(8, 0));
		item.setCloseTime(LocalTime.of(18, 0));
		request.setSchedules(List.of(item));
		request.setForceCancelConflictedReservations(true);

		ApiResult<LabScheduleMutationResponse> result = controller.save(100L, request);

		assertThat(service.saveLabId).isEqualTo(100L);
		assertThat(service.saveCommand.getSchedules().getFirst().getDayOfWeek()).isEqualTo(1);
		assertThat(service.saveCommand.isForceCancelConflictedReservations()).isTrue();
		assertThat(result.getData().isApplied()).isTrue();
	}

	private static class RecordingLabScheduleService implements LabScheduleService {

		private Long saveLabId;
		private LabScheduleSaveCommand saveCommand;

		@Override
		public LabScheduleMutationResponse save(Long labId, LabScheduleSaveCommand command) {
			this.saveLabId = labId;
			this.saveCommand = command;
			return LabScheduleMutationResponse.builder()
					.applied(true)
					.schedules(List.of())
					.conflictedReservations(List.of())
					.build();
		}

		@Override
		public List<LabScheduleResponse> listByLabId(Long labId) {
			return List.of();
		}

		@Override
		public LabScheduleMutationResponse delete(Long labId, Long scheduleId, boolean forceCancelConflictedReservations) {
			return LabScheduleMutationResponse.builder()
					.applied(true)
					.schedules(List.of())
					.conflictedReservations(List.of())
					.build();
		}
	}
}
