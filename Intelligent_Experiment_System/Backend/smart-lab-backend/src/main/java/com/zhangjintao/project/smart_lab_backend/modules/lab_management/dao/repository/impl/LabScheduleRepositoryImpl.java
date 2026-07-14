package com.zhangjintao.project.smart_lab_backend.modules.lab_management.dao.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.dao.dataobject.LabScheduleDO;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.dao.mapper.LabScheduleMapper;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.dao.repository.LabScheduleRepository;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.domain.value.LabWeeklySchedule;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class LabScheduleRepositoryImpl implements LabScheduleRepository {

	private final LabScheduleMapper labScheduleMapper;

	@Override
	public List<LabWeeklySchedule> listByLabId(Long labId) {
		if (labId == null) {
			return List.of();
		}
		return labScheduleMapper.selectList(new LambdaQueryWrapper<LabScheduleDO>()
						.eq(LabScheduleDO::getLabId, labId)
						.orderByAsc(LabScheduleDO::getDayOfWeek))
				.stream()
				.map(LabPersistenceConverter::toLabWeeklySchedule)
				.toList();
	}

	@Override
	public Optional<LabWeeklySchedule> findByLabIdAndDayOfWeek(Long labId, Integer dayOfWeek) {
		if (labId == null || dayOfWeek == null) {
			return Optional.empty();
		}
		return Optional.ofNullable(labScheduleMapper.selectOne(new LambdaQueryWrapper<LabScheduleDO>()
						.eq(LabScheduleDO::getLabId, labId)
						.eq(LabScheduleDO::getDayOfWeek, dayOfWeek)
						.last("limit 1")))
				.map(LabPersistenceConverter::toLabWeeklySchedule);
	}

	@Override
	public Optional<LabWeeklySchedule> findByLabIdAndId(Long labId, Long scheduleId) {
		if (labId == null || scheduleId == null) {
			return Optional.empty();
		}
		return Optional.ofNullable(labScheduleMapper.selectOne(new LambdaQueryWrapper<LabScheduleDO>()
						.eq(LabScheduleDO::getLabId, labId)
						.eq(LabScheduleDO::getId, scheduleId)
						.last("limit 1")))
				.map(LabPersistenceConverter::toLabWeeklySchedule);
	}

	@Override
	public void upsertBatch(List<LabWeeklySchedule> schedules) {
		if (schedules == null || schedules.isEmpty()) {
			return;
		}
		labScheduleMapper.upsertBatch(schedules.stream()
				.map(LabPersistenceConverter::toLabScheduleDO)
				.toList());
	}

	@Override
	public void deleteById(Long scheduleId) {
		if (scheduleId == null) {
			return;
		}
		labScheduleMapper.deleteById(scheduleId);
	}
}
