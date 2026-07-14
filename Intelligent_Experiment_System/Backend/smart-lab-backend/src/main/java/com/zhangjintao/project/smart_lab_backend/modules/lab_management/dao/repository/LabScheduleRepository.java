package com.zhangjintao.project.smart_lab_backend.modules.lab_management.dao.repository;

import com.zhangjintao.project.smart_lab_backend.modules.lab_management.domain.value.LabWeeklySchedule;

import java.util.List;
import java.util.Optional;

public interface LabScheduleRepository {

	List<LabWeeklySchedule> listByLabId(Long labId);

	Optional<LabWeeklySchedule> findByLabIdAndDayOfWeek(Long labId, Integer dayOfWeek);

	Optional<LabWeeklySchedule> findByLabIdAndId(Long labId, Long scheduleId);

	void upsertBatch(List<LabWeeklySchedule> schedules);

	void deleteById(Long scheduleId);
}
