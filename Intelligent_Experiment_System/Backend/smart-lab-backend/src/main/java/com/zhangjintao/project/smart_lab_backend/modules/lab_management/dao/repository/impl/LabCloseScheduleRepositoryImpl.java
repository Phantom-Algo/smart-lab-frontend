package com.zhangjintao.project.smart_lab_backend.modules.lab_management.dao.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.dao.dataobject.LabCloseScheduleDO;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.dao.mapper.LabCloseScheduleMapper;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.dao.repository.LabCloseScheduleRepository;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.domain.entity.LabCloseSchedule;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.dto.LabCloseScheduleQueryCriteria;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class LabCloseScheduleRepositoryImpl implements LabCloseScheduleRepository {

	private final LabCloseScheduleMapper labCloseScheduleMapper;

	@Override
	public boolean existsOverlap(Long labId, LocalDateTime startDateTime, LocalDateTime endDateTime) {
		if (labId == null || startDateTime == null || endDateTime == null) {
			return false;
		}
		return labCloseScheduleMapper.selectCount(new LambdaQueryWrapper<LabCloseScheduleDO>()
				.eq(LabCloseScheduleDO::getLabId, labId)
				.lt(LabCloseScheduleDO::getStartTime, endDateTime)
				.gt(LabCloseScheduleDO::getEndTime, startDateTime)) > 0;
	}

	@Override
	public Optional<LabCloseSchedule> findByLabIdAndId(Long labId, Long scheduleId) {
		if (labId == null || scheduleId == null) {
			return Optional.empty();
		}
		return Optional.ofNullable(labCloseScheduleMapper.selectOne(new LambdaQueryWrapper<LabCloseScheduleDO>()
						.eq(LabCloseScheduleDO::getLabId, labId)
						.eq(LabCloseScheduleDO::getId, scheduleId)
						.last("limit 1")))
				.map(LabPersistenceConverter::toLabCloseSchedule);
	}

	@Override
	public LabCloseSchedule save(LabCloseSchedule closeSchedule) {
		LabCloseScheduleDO closeScheduleDO = LabPersistenceConverter.toLabCloseScheduleDO(closeSchedule);
		labCloseScheduleMapper.insert(closeScheduleDO);
		return Optional.ofNullable(labCloseScheduleMapper.selectById(closeScheduleDO.getId()))
				.map(LabPersistenceConverter::toLabCloseSchedule)
				.orElseGet(() -> LabPersistenceConverter.toLabCloseSchedule(closeScheduleDO));
	}

	@Override
	public LabCloseSchedule update(LabCloseSchedule closeSchedule) {
		labCloseScheduleMapper.updateById(LabPersistenceConverter.toLabCloseScheduleDO(closeSchedule));
		return Optional.ofNullable(labCloseScheduleMapper.selectById(closeSchedule.getId()))
				.map(LabPersistenceConverter::toLabCloseSchedule)
				.orElse(closeSchedule);
	}

	@Override
	public void deleteById(Long scheduleId) {
		if (scheduleId == null) {
			return;
		}
		labCloseScheduleMapper.deleteById(scheduleId);
	}

	@Override
	public List<LabCloseSchedule> listByLabId(Long labId, LabCloseScheduleQueryCriteria criteria) {
		if (labId == null) {
			return List.of();
		}
		LabCloseScheduleQueryCriteria safeCriteria = criteria == null
				? LabCloseScheduleQueryCriteria.builder().build()
				: criteria;
		LambdaQueryWrapper<LabCloseScheduleDO> wrapper = new LambdaQueryWrapper<LabCloseScheduleDO>()
				.eq(LabCloseScheduleDO::getLabId, labId)
				.eq(safeCriteria.getCloseType() != null, LabCloseScheduleDO::getCloseType, safeCriteria.getCloseType());
		if (safeCriteria.getStartDate() != null) {
			wrapper.gt(LabCloseScheduleDO::getEndTime, safeCriteria.getStartDate().atStartOfDay());
		}
		if (safeCriteria.getEndDate() != null) {
			wrapper.lt(LabCloseScheduleDO::getStartTime, safeCriteria.getEndDate().plusDays(1).atStartOfDay());
		}
		return labCloseScheduleMapper.selectList(wrapper
						.orderByAsc(LabCloseScheduleDO::getStartTime)
						.orderByAsc(LabCloseScheduleDO::getId))
				.stream()
				.map(LabPersistenceConverter::toLabCloseSchedule)
				.toList();
	}
}
