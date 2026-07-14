package com.zhangjintao.project.smart_lab_backend.modules.lab_management.dao.repository;

import com.zhangjintao.project.smart_lab_backend.modules.lab_management.domain.entity.LabDetectionLog;

import java.util.Optional;

public interface LabDetectionLogRepository {

	Optional<LabDetectionLog> findByEventNo(String eventNo);

	LabDetectionLog save(LabDetectionLog detectionLog);
}
