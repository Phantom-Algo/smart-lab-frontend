package com.zhangjintao.project.smart_lab_backend.modules.equipment.dao.repository;

import com.zhangjintao.project.smart_lab_backend.modules.equipment.dao.dataobject.DeviceSensorBindingDO;

import java.time.LocalDateTime;
import java.util.Optional;

public interface DeviceSensorBindingRepository {
	Optional<DeviceSensorBindingDO> findForUpdate(Long deviceId, Long sensorId);
	void insert(DeviceSensorBindingDO binding);
	int reactivate(Long bindingId, LocalDateTime boundAt);
	int unbind(Long deviceId, Long sensorId, LocalDateTime unboundAt);
	boolean existsActiveByDeviceId(Long deviceId);
}
