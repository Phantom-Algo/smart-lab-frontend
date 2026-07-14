package com.zhangjintao.project.smart_lab_backend.modules.lab_management.controller;

import com.zhangjintao.project.smart_lab_backend.common.result.PageResult;
import com.zhangjintao.project.smart_lab_backend.modules.auth_profile.model.dto.AuthenticatedUser;
import com.zhangjintao.project.smart_lab_backend.modules.auth_profile.service.CurrentUserQueryService;
import com.zhangjintao.project.smart_lab_backend.modules.auth_profile.model.dto.UserAccountWithProfileDTO;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.dto.MyReservationQueryCriteria;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.dto.PendingReservationQueryCriteria;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.dto.ReservationApproveCommand;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.dto.ReservationCancelCommand;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.dto.ReservationCreateCommand;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.request.ApproveReservationRequest;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.request.CancelReservationRequest;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.request.CreateReservationRequest;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.request.MyReservationPageQueryRequest;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.request.PendingReservationPageQueryRequest;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.response.LabOccupancyResponse;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.response.ReservationResponse;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.service.ReservationService;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.reflect.Method;
import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

class ReservationControllerTest {

	@Test
	void roleAnnotationsShouldMatchReservationPlan() throws NoSuchMethodException {
		Method create = ReservationController.class.getMethod("create", CreateReservationRequest.class);
		Method approve = ReservationController.class.getMethod("approve", String.class, ApproveReservationRequest.class);
		Method pagePending = ReservationController.class.getMethod("pagePending", PendingReservationPageQueryRequest.class);
		Method cancel = ReservationController.class.getMethod("cancel", String.class, CancelReservationRequest.class);
		Method adminCheckIn = ReservationController.class.getMethod("adminCheckIn", String.class);

		assertThat(create.getAnnotation(PreAuthorize.class).value()).isEqualTo("hasAnyRole('STUDENT','TEACHER')");
		assertThat(approve.getAnnotation(PreAuthorize.class).value()).isEqualTo("hasRole('ADMIN')");
		assertThat(pagePending.getAnnotation(PreAuthorize.class).value()).isEqualTo("hasRole('ADMIN')");
		assertThat(adminCheckIn.getAnnotation(PreAuthorize.class).value()).isEqualTo("hasRole('ADMIN')");
		assertThat(cancel.getAnnotation(PreAuthorize.class)).isNull();
	}

	@Test
	void createShouldUseCurrentUserAsApplicant() {
		RecordingReservationService reservationService = new RecordingReservationService();
		ReservationController controller = new ReservationController(reservationService, new StubCurrentUserQueryService());
		CreateReservationRequest request = new CreateReservationRequest();
		request.setLabId(100L);
		request.setReservationDate(LocalDate.of(2026, 7, 10));
		request.setStartTime(LocalTime.of(9, 0));
		request.setEndTime(LocalTime.of(10, 0));

		controller.create(request);

		assertThat(reservationService.createCommand.getApplicantId()).isEqualTo(10L);
		assertThat(reservationService.createCommand.getLabId()).isEqualTo(100L);
	}

	@Test
	void pageMineShouldUseCurrentUserAsApplicant() {
		RecordingReservationService reservationService = new RecordingReservationService();
		ReservationController controller = new ReservationController(reservationService, new StubCurrentUserQueryService());
		MyReservationPageQueryRequest request = new MyReservationPageQueryRequest();
		request.setStatus(2);

		controller.pageMine(request);

		assertThat(reservationService.myCriteria.getApplicantId()).isEqualTo(10L);
		assertThat(reservationService.myCriteria.getStatus()).isEqualTo(2);
	}

	@Test
	void adminCheckInShouldUseCurrentUserAsOperator() {
		RecordingReservationService reservationService = new RecordingReservationService();
		ReservationController controller = new ReservationController(reservationService, new StubCurrentUserQueryService());

		controller.adminCheckIn("LAB_RESV-20260710123456");

		assertThat(reservationService.adminCheckInReservationNo).isEqualTo("LAB_RESV-20260710123456");
		assertThat(reservationService.adminCheckInOperatorId).isEqualTo(10L);
	}

	private static class RecordingReservationService implements ReservationService {

		private ReservationCreateCommand createCommand;
		private MyReservationQueryCriteria myCriteria;
		private String adminCheckInReservationNo;
		private Long adminCheckInOperatorId;

		@Override
		public ReservationResponse create(ReservationCreateCommand command) {
			this.createCommand = command;
			return response();
		}

		@Override
		public ReservationResponse approve(ReservationApproveCommand command) {
			return response();
		}

		@Override
		public ReservationResponse cancel(ReservationCancelCommand command) {
			return response();
		}

		@Override
		public ReservationResponse checkIn(String reservationNo, Long applicantId) {
			return response();
		}

		@Override
		public ReservationResponse adminCheckIn(String reservationNo, Long operatorId) {
			this.adminCheckInReservationNo = reservationNo;
			this.adminCheckInOperatorId = operatorId;
			return response();
		}

		@Override
		public ReservationResponse checkOut(String reservationNo, Long applicantId) {
			return response();
		}

		@Override
		public PageResult<ReservationResponse> pageMine(MyReservationQueryCriteria criteria) {
			this.myCriteria = criteria;
			return PageResult.of(java.util.List.of(response()), 1, 1, 20);
		}

		@Override
		public PageResult<ReservationResponse> pagePending(PendingReservationQueryCriteria criteria) {
			return PageResult.empty(1, 20);
		}

		@Override
		public LabOccupancyResponse getOccupancy(Long labId, LocalDate date) {
			return LabOccupancyResponse.builder().labId(labId).date(date).occupiedTimes(java.util.List.of()).build();
		}

		@Override
		public void markAbsentReservations() {
		}

		private ReservationResponse response() {
			return ReservationResponse.builder()
					.reservationNo("LAB_RESV-20260710123456")
					.build();
		}
	}

	private static class StubCurrentUserQueryService implements CurrentUserQueryService {

		@Override
		public AuthenticatedUser requireCurrentUser() {
			return AuthenticatedUser.builder()
					.userId(10L)
					.build();
		}

		@Override
		public UserAccountWithProfileDTO requireCurrentUserWithProfile() {
			throw new UnsupportedOperationException("测试不需要用户资料");
		}
	}
}
