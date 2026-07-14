package com.zhangjintao.project.smart_lab_backend.modules.lab_management.dao.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhangjintao.project.smart_lab_backend.common.result.PageResult;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.constants.LabManagementConstants;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.dao.dataobject.LabReservationDO;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.dao.mapper.LabReservationMapper;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.dao.repository.LabReservationRepository;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.domain.entity.LabReservation;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.enums.LabReservationStatus;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.dto.MyReservationQueryCriteria;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.dto.PendingReservationQueryCriteria;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class LabReservationRepositoryImpl implements LabReservationRepository {

	private final LabReservationMapper labReservationMapper;

	@Override
	public Optional<LabReservation> findByReservationNo(String reservationNo) {
		if (reservationNo == null) {
			return Optional.empty();
		}
		return Optional.ofNullable(labReservationMapper.selectOne(new LambdaQueryWrapper<LabReservationDO>()
						.eq(LabReservationDO::getReservationNo, reservationNo)
						.last("limit 1")))
				.map(LabPersistenceConverter::toLabReservation);
	}

	@Override
	public Optional<LabReservation> findByReservationNoForUpdate(String reservationNo) {
		if (reservationNo == null) {
			return Optional.empty();
		}
		return Optional.ofNullable(labReservationMapper.selectOne(new LambdaQueryWrapper<LabReservationDO>()
						.eq(LabReservationDO::getReservationNo, reservationNo)
						.last("limit 1 for update")))
				.map(LabPersistenceConverter::toLabReservation);
	}

	@Override
	public boolean existsByReservationNo(String reservationNo) {
		if (reservationNo == null) {
			return false;
		}
		return labReservationMapper.selectCount(new LambdaQueryWrapper<LabReservationDO>()
				.eq(LabReservationDO::getReservationNo, reservationNo)) > 0;
	}

	@Override
	public LabReservation save(LabReservation reservation) {
		LabReservationDO reservationDO = LabPersistenceConverter.toReservationDO(reservation);
		labReservationMapper.insert(reservationDO);
		return Optional.ofNullable(labReservationMapper.selectById(reservationDO.getId()))
				.map(LabPersistenceConverter::toLabReservation)
				.orElseGet(() -> LabPersistenceConverter.toLabReservation(reservationDO));
	}

	@Override
	public LabReservation update(LabReservation reservation) {
		labReservationMapper.updateById(LabPersistenceConverter.toReservationDO(reservation));
		return Optional.ofNullable(labReservationMapper.selectById(reservation.getId()))
				.map(LabPersistenceConverter::toLabReservation)
				.orElse(reservation);
	}

	@Override
	public List<LabReservation> listFuturePendingOrApprovedByLabId(Long labId, LocalDate today) {
		if (labId == null || today == null) {
			return List.of();
		}
		return labReservationMapper.selectList(new LambdaQueryWrapper<LabReservationDO>()
						.eq(LabReservationDO::getLabId, labId)
						.ge(LabReservationDO::getReservationDate, today)
						.in(LabReservationDO::getReservationStatus, LabReservationStatus.CANCELLABLE_BY_LAB_DELETED)
						.orderByAsc(LabReservationDO::getReservationDate)
						.orderByAsc(LabReservationDO::getStartTime))
				.stream()
				.map(LabPersistenceConverter::toLabReservation)
				.toList();
	}

	@Override
	public List<LabReservation> listActiveCheckedInByLabIdForUpdate(Long labId) {
		if (labId == null) {
			return List.of();
		}
		return labReservationMapper.selectList(new LambdaQueryWrapper<LabReservationDO>()
						.eq(LabReservationDO::getLabId, labId)
						.eq(LabReservationDO::getReservationStatus, LabReservationStatus.CHECKED_IN.getCode())
						.isNull(LabReservationDO::getCheckOutAt)
						.orderByAsc(LabReservationDO::getReservationDate)
						.orderByAsc(LabReservationDO::getStartTime)
						.last("for update"))
				.stream()
				.map(LabPersistenceConverter::toLabReservation)
				.toList();
	}

	@Override
	public List<LabReservation> listCurrentCheckedInByLabId(Long labId, LocalDateTime eventTime) {
		if (labId == null || eventTime == null) {
			return List.of();
		}
		return labReservationMapper.selectList(new LambdaQueryWrapper<LabReservationDO>()
						.eq(LabReservationDO::getLabId, labId)
						.eq(LabReservationDO::getReservationDate, eventTime.toLocalDate())
						.eq(LabReservationDO::getReservationStatus, LabReservationStatus.CHECKED_IN.getCode())
						.isNotNull(LabReservationDO::getCheckInAt)
						.isNull(LabReservationDO::getCheckOutAt)
						.le(LabReservationDO::getStartTime, eventTime.toLocalTime())
						.ge(LabReservationDO::getEndTime, eventTime.toLocalTime())
						.orderByAsc(LabReservationDO::getStartTime)
						.orderByAsc(LabReservationDO::getId))
				.stream()
				.map(LabPersistenceConverter::toLabReservation)
				.toList();
	}

	@Override
	public List<LabReservation> listPendingOrApprovedByLabIdAndDateRange(Long labId, LocalDate startDate,
			LocalDate endDate) {
		if (labId == null || startDate == null || endDate == null) {
			return List.of();
		}
		return labReservationMapper.selectList(new LambdaQueryWrapper<LabReservationDO>()
						.eq(LabReservationDO::getLabId, labId)
						.ge(LabReservationDO::getReservationDate, startDate)
						.le(LabReservationDO::getReservationDate, endDate)
						.in(LabReservationDO::getReservationStatus, LabReservationStatus.CANCELLABLE_BY_LAB_DELETED)
						.orderByAsc(LabReservationDO::getReservationDate)
						.orderByAsc(LabReservationDO::getStartTime))
				.stream()
				.map(LabPersistenceConverter::toLabReservation)
				.toList();
	}

	@Override
	public PageResult<LabReservation> pageMine(MyReservationQueryCriteria criteria) {
		long pageNum = criteria.getPage() == null ? LabManagementConstants.DEFAULT_PAGE : criteria.getPage();
		long pageSize = criteria.getPageSize() == null ? LabManagementConstants.DEFAULT_PAGE_SIZE : criteria.getPageSize();
		LambdaQueryWrapper<LabReservationDO> wrapper = new LambdaQueryWrapper<LabReservationDO>()
				.eq(LabReservationDO::getApplicantId, criteria.getApplicantId())
				.eq(criteria.getStatus() != null, LabReservationDO::getReservationStatus, criteria.getStatus())
				.orderByDesc(LabReservationDO::getReservationDate)
				.orderByDesc(LabReservationDO::getStartTime)
				.orderByDesc(LabReservationDO::getId);
		Page<LabReservationDO> page = labReservationMapper.selectPage(Page.of(pageNum, pageSize), wrapper);
		return PageResult.of(page.getRecords().stream()
						.map(LabPersistenceConverter::toLabReservation)
						.toList(),
				page.getTotal(), page.getCurrent(), page.getSize());
	}

	@Override
	public PageResult<LabReservation> pageAll(MyReservationQueryCriteria criteria) {
		long pageNum = criteria.getPage() == null ? LabManagementConstants.DEFAULT_PAGE : criteria.getPage();
		long pageSize = criteria.getPageSize() == null ? LabManagementConstants.DEFAULT_PAGE_SIZE : criteria.getPageSize();
		LambdaQueryWrapper<LabReservationDO> wrapper = new LambdaQueryWrapper<LabReservationDO>()
				.eq(criteria.getStatus() != null, LabReservationDO::getReservationStatus, criteria.getStatus())
				.orderByDesc(LabReservationDO::getReservationDate)
				.orderByDesc(LabReservationDO::getStartTime)
				.orderByDesc(LabReservationDO::getId);
		Page<LabReservationDO> page = labReservationMapper.selectPage(Page.of(pageNum, pageSize), wrapper);
		return PageResult.of(page.getRecords().stream()
						.map(LabPersistenceConverter::toLabReservation)
						.toList(),
				page.getTotal(), page.getCurrent(), page.getSize());
	}

	@Override
	public PageResult<LabReservation> pagePending(PendingReservationQueryCriteria criteria) {
		long pageNum = criteria.getPage() == null ? LabManagementConstants.DEFAULT_PAGE : criteria.getPage();
		long pageSize = criteria.getPageSize() == null ? LabManagementConstants.DEFAULT_PAGE_SIZE : criteria.getPageSize();
		LambdaQueryWrapper<LabReservationDO> wrapper = new LambdaQueryWrapper<LabReservationDO>()
				.eq(LabReservationDO::getReservationStatus, LabReservationStatus.PENDING_APPROVAL.getCode())
				.eq(criteria.getLabId() != null, LabReservationDO::getLabId, criteria.getLabId())
				.ge(criteria.getStartDate() != null, LabReservationDO::getReservationDate, criteria.getStartDate())
				.le(criteria.getEndDate() != null, LabReservationDO::getReservationDate, criteria.getEndDate())
				.orderByAsc(LabReservationDO::getReservationDate)
				.orderByAsc(LabReservationDO::getStartTime)
				.orderByAsc(LabReservationDO::getId);
		Page<LabReservationDO> page = labReservationMapper.selectPage(Page.of(pageNum, pageSize), wrapper);
		return PageResult.of(page.getRecords().stream()
						.map(LabPersistenceConverter::toLabReservation)
						.toList(),
				page.getTotal(), page.getCurrent(), page.getSize());
	}

	@Override
	public List<LabReservation> listOccupancy(Long labId, LocalDate date) {
		if (labId == null || date == null) {
			return List.of();
		}
		return labReservationMapper.selectList(new LambdaQueryWrapper<LabReservationDO>()
						.eq(LabReservationDO::getLabId, labId)
						.eq(LabReservationDO::getReservationDate, date)
						.in(LabReservationDO::getReservationStatus, LabReservationStatus.OCCUPANCY_STATUSES)
						.orderByAsc(LabReservationDO::getStartTime)
						.orderByAsc(LabReservationDO::getId))
				.stream()
				.map(LabPersistenceConverter::toLabReservation)
				.toList();
	}

	@Override
	public boolean existsActiveOverlap(Long labId, LocalDate reservationDate, LocalTime startTime, LocalTime endTime) {
		if (labId == null || reservationDate == null || startTime == null || endTime == null) {
			return false;
		}
		return labReservationMapper.selectCount(new LambdaQueryWrapper<LabReservationDO>()
				.eq(LabReservationDO::getLabId, labId)
				.eq(LabReservationDO::getReservationDate, reservationDate)
				.in(LabReservationDO::getReservationStatus, LabReservationStatus.OCCUPANCY_STATUSES)
				.lt(LabReservationDO::getStartTime, endTime)
				.gt(LabReservationDO::getEndTime, startTime)) > 0;
	}

	@Override
	public List<LabReservation> listApprovedUncheckedByDate(LocalDate date) {
		if (date == null) {
			return List.of();
		}
		return labReservationMapper.selectList(new LambdaQueryWrapper<LabReservationDO>()
						.eq(LabReservationDO::getReservationDate, date)
						.eq(LabReservationDO::getReservationStatus, LabReservationStatus.APPROVED.getCode())
						.isNull(LabReservationDO::getCheckInAt)
						.orderByAsc(LabReservationDO::getStartTime)
						.orderByAsc(LabReservationDO::getId))
				.stream()
				.map(LabPersistenceConverter::toLabReservation)
				.toList();
	}

	@Override
	public void updateBatch(List<LabReservation> reservations) {
		if (reservations == null || reservations.isEmpty()) {
			return;
		}
		reservations.stream()
				.map(LabPersistenceConverter::toReservationDO)
				.forEach(labReservationMapper::updateById);
	}
}
