package com.zhangjintao.project.smart_lab_backend.modules.lab_management.dao.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.dao.dataobject.LabSensorBindingDO;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.dao.mapper.LabSensorBindingMapper;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.dao.repository.LabSensorBindingRepository;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.enums.SensorBindingStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class LabSensorBindingRepositoryImpl implements LabSensorBindingRepository {

	private final LabSensorBindingMapper mapper;

	@Override
	public Optional<LabSensorBindingDO> findForUpdate(Long labId, Long sensorId) {
		return Optional.ofNullable(mapper.selectOne(new LambdaQueryWrapper<LabSensorBindingDO>()
				.eq(LabSensorBindingDO::getLabId, labId)
				.eq(LabSensorBindingDO::getSensorId, sensorId)
				.last("LIMIT 1 FOR UPDATE")));
	}

	@Override
	public void insert(LabSensorBindingDO binding) {
		if (mapper.insert(binding) != 1) {
			throw new IllegalStateException("新增实验室传感器绑定行数异常");
		}
	}

	@Override
	public int reactivate(Long bindingId, LocalDateTime boundAt) {
		return mapper.update(null, new LambdaUpdateWrapper<LabSensorBindingDO>()
				.eq(LabSensorBindingDO::getId, bindingId)
				.eq(LabSensorBindingDO::getBindStatus, SensorBindingStatus.UNBOUND.getCode())
				.set(LabSensorBindingDO::getBindStatus, SensorBindingStatus.BOUND.getCode())
				.set(LabSensorBindingDO::getBoundAt, boundAt)
				.set(LabSensorBindingDO::getUnboundAt, null));
	}

	@Override
	public int unbind(Long labId, Long sensorId, LocalDateTime unboundAt) {
		return mapper.update(null, new LambdaUpdateWrapper<LabSensorBindingDO>()
				.eq(LabSensorBindingDO::getLabId, labId)
				.eq(LabSensorBindingDO::getSensorId, sensorId)
				.eq(LabSensorBindingDO::getBindStatus, SensorBindingStatus.BOUND.getCode())
				.set(LabSensorBindingDO::getBindStatus, SensorBindingStatus.UNBOUND.getCode())
				.set(LabSensorBindingDO::getUnboundAt, unboundAt));
	}

	@Override
	public boolean existsActiveByLabId(Long labId) {
		return mapper.selectCount(new LambdaQueryWrapper<LabSensorBindingDO>()
				.eq(LabSensorBindingDO::getLabId, labId)
				.eq(LabSensorBindingDO::getBindStatus, SensorBindingStatus.BOUND.getCode())) > 0;
	}
}
