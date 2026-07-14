package com.zhangjintao.project.smart_lab_backend.modules.equipment.dao.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.zhangjintao.project.smart_lab_backend.modules.equipment.dao.dataobject.DeviceSensorBindingDO;
import com.zhangjintao.project.smart_lab_backend.modules.equipment.dao.mapper.DeviceSensorBindingMapper;
import com.zhangjintao.project.smart_lab_backend.modules.equipment.dao.repository.DeviceSensorBindingRepository;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.enums.SensorBindingStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class DeviceSensorBindingRepositoryImpl implements DeviceSensorBindingRepository {

	private final DeviceSensorBindingMapper mapper;

	@Override
	public Optional<DeviceSensorBindingDO> findForUpdate(Long deviceId, Long sensorId) {
		return Optional.ofNullable(mapper.selectOne(new LambdaQueryWrapper<DeviceSensorBindingDO>()
				.eq(DeviceSensorBindingDO::getDeviceId, deviceId)
				.eq(DeviceSensorBindingDO::getSensorId, sensorId)
				.last("LIMIT 1 FOR UPDATE")));
	}

	@Override
	public void insert(DeviceSensorBindingDO binding) {
		if (mapper.insert(binding) != 1) {
			throw new IllegalStateException("新增设备传感器绑定行数异常");
		}
	}

	@Override
	public int reactivate(Long bindingId, LocalDateTime boundAt) {
		return mapper.update(null, new LambdaUpdateWrapper<DeviceSensorBindingDO>()
				.eq(DeviceSensorBindingDO::getId, bindingId)
				.eq(DeviceSensorBindingDO::getBindStatus, SensorBindingStatus.UNBOUND.getCode())
				.set(DeviceSensorBindingDO::getBindStatus, SensorBindingStatus.BOUND.getCode())
				.set(DeviceSensorBindingDO::getBoundAt, boundAt)
				.set(DeviceSensorBindingDO::getUnboundAt, null));
	}

	@Override
	public int unbind(Long deviceId, Long sensorId, LocalDateTime unboundAt) {
		return mapper.update(null, new LambdaUpdateWrapper<DeviceSensorBindingDO>()
				.eq(DeviceSensorBindingDO::getDeviceId, deviceId)
				.eq(DeviceSensorBindingDO::getSensorId, sensorId)
				.eq(DeviceSensorBindingDO::getBindStatus, SensorBindingStatus.BOUND.getCode())
				.set(DeviceSensorBindingDO::getBindStatus, SensorBindingStatus.UNBOUND.getCode())
				.set(DeviceSensorBindingDO::getUnboundAt, unboundAt));
	}

	@Override
	public boolean existsActiveByDeviceId(Long deviceId) {
		return mapper.selectCount(new LambdaQueryWrapper<DeviceSensorBindingDO>()
				.eq(DeviceSensorBindingDO::getDeviceId, deviceId)
				.eq(DeviceSensorBindingDO::getBindStatus, SensorBindingStatus.BOUND.getCode())) > 0;
	}
}
