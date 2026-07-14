package com.zhangjintao.project.smart_lab_backend.modules.lab_management.dao.repository;

import com.zhangjintao.project.smart_lab_backend.modules.lab_management.domain.entity.LabStatusChangeLog;

public interface LabStatusChangeLogRepository {

	void save(LabStatusChangeLog log);
}
