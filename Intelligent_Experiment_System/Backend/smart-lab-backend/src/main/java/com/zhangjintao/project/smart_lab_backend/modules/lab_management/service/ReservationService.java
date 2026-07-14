package com.zhangjintao.project.smart_lab_backend.modules.lab_management.service;

import com.zhangjintao.project.smart_lab_backend.common.result.PageResult;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.dto.MyReservationQueryCriteria;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.dto.PendingReservationQueryCriteria;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.dto.ReservationApproveCommand;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.dto.ReservationCancelCommand;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.dto.ReservationCreateCommand;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.response.LabOccupancyResponse;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.response.ReservationResponse;

import java.time.LocalDate;

public interface ReservationService {

	ReservationResponse create(ReservationCreateCommand command);

	ReservationResponse approve(ReservationApproveCommand command);

	ReservationResponse cancel(ReservationCancelCommand command);

	ReservationResponse checkIn(String reservationNo, Long applicantId);

	ReservationResponse adminCheckIn(String reservationNo, Long operatorId);

	ReservationResponse checkOut(String reservationNo, Long applicantId);

	PageResult<ReservationResponse> pageMine(MyReservationQueryCriteria criteria);

	PageResult<ReservationResponse> pageAll(MyReservationQueryCriteria criteria);

	PageResult<ReservationResponse> pagePending(PendingReservationQueryCriteria criteria);

	LabOccupancyResponse getOccupancy(Long labId, LocalDate date);

	void markAbsentReservations();
}
