package com.zhangjintao.project.smart_lab_backend.modules.lab_management.dao.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.dao.dataobject.LabDetectionLogDO;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.dao.mapper.LabDetectionLogMapper;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.dao.repository.LabDetectionLogRepository;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.domain.entity.LabDetectionLog;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class LabDetectionLogRepositoryImpl implements LabDetectionLogRepository {

	private final LabDetectionLogMapper labDetectionLogMapper;

	@Override
	public Optional<LabDetectionLog> findByEventNo(String eventNo) {
		if (!StringUtils.hasText(eventNo)) {
			return Optional.empty();
		}
		return Optional.ofNullable(labDetectionLogMapper.selectOne(new LambdaQueryWrapper<LabDetectionLogDO>()
						.eq(LabDetectionLogDO::getEventNo, eventNo)
						.last("limit 1")))
				.map(LabPersistenceConverter::toLabDetectionLog);
	}

	@Override
	public LabDetectionLog save(LabDetectionLog detectionLog) {
		LabDetectionLogDO detectionLogDO = LabPersistenceConverter.toLabDetectionLogDO(detectionLog);
		labDetectionLogMapper.insert(detectionLogDO);
		return Optional.ofNullable(labDetectionLogMapper.selectById(detectionLogDO.getId()))
				.map(LabPersistenceConverter::toLabDetectionLog)
				.orElseGet(() -> LabPersistenceConverter.toLabDetectionLog(detectionLogDO));
	}
}
