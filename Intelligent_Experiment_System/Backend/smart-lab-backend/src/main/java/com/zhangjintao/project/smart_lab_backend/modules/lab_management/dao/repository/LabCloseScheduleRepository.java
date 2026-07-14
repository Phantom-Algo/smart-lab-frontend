package com.zhangjintao.project.smart_lab_backend.modules.lab_management.dao.repository;

import com.zhangjintao.project.smart_lab_backend.modules.lab_management.domain.entity.LabCloseSchedule;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.dto.LabCloseScheduleQueryCriteria;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface LabCloseScheduleRepository {

	boolean existsOverlap(Long labId, LocalDateTime startDateTime, LocalDateTime endDateTime);

	Optional<LabCloseSchedule> findByLabIdAndId(Long labId, Long scheduleId);

	LabCloseSchedule save(LabCloseSchedule closeSchedule);

	LabCloseSchedule update(LabCloseSchedule closeSchedule);

	void deleteById(Long scheduleId);

	List<LabCloseSchedule> listByLabId(Long labId, LabCloseScheduleQueryCriteria criteria);
}
