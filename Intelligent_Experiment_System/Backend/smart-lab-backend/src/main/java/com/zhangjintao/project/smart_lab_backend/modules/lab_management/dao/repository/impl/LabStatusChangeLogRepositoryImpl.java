package com.zhangjintao.project.smart_lab_backend.modules.lab_management.dao.repository.impl;

import com.zhangjintao.project.smart_lab_backend.modules.lab_management.dao.mapper.LabStatusChangeLogMapper;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.dao.repository.LabStatusChangeLogRepository;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.domain.entity.LabStatusChangeLog;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class LabStatusChangeLogRepositoryImpl implements LabStatusChangeLogRepository {

	private final LabStatusChangeLogMapper labStatusChangeLogMapper;

	@Override
	public void save(LabStatusChangeLog log) {
		labStatusChangeLogMapper.insert(LabPersistenceConverter.toLabStatusChangeLogDO(log));
	}
}
