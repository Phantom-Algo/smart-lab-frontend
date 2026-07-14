package com.zhangjintao.project.smart_lab_backend.modules.lab_management.dao.repository;

import com.zhangjintao.project.smart_lab_backend.common.result.PageResult;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.domain.entity.LabReservation;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.dto.MyReservationQueryCriteria;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.dto.PendingReservationQueryCriteria;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public interface LabReservationRepository {

	Optional<LabReservation> findByReservationNo(String reservationNo);

	Optional<LabReservation> findByReservationNoForUpdate(String reservationNo);

	boolean existsByReservationNo(String reservationNo);

	LabReservation save(LabReservation reservation);

	LabReservation update(LabReservation reservation);

	List<LabReservation> listFuturePendingOrApprovedByLabId(Long labId, LocalDate today);

	List<LabReservation> listActiveCheckedInByLabIdForUpdate(Long labId);

	List<LabReservation> listCurrentCheckedInByLabId(Long labId, LocalDateTime eventTime);

	List<LabReservation> listPendingOrApprovedByLabIdAndDateRange(Long labId, LocalDate startDate, LocalDate endDate);

	PageResult<LabReservation> pageMine(MyReservationQueryCriteria criteria);

	PageResult<LabReservation> pageAll(MyReservationQueryCriteria criteria);

	PageResult<LabReservation> pagePending(PendingReservationQueryCriteria criteria);

	List<LabReservation> listOccupancy(Long labId, LocalDate date);

	boolean existsActiveOverlap(Long labId, LocalDate reservationDate, LocalTime startTime, LocalTime endTime);

	List<LabReservation> listApprovedUncheckedByDate(LocalDate date);

	void updateBatch(List<LabReservation> reservations);
}
