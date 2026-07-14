package com.zhangjintao.project.smart_lab_backend.modules.lab_management.dao.repository;

import com.zhangjintao.project.smart_lab_backend.modules.lab_management.dao.dataobject.LabSensorBindingDO;

import java.time.LocalDateTime;
import java.util.Optional;

public interface LabSensorBindingRepository {
	Optional<LabSensorBindingDO> findForUpdate(Long labId, Long sensorId);
	void insert(LabSensorBindingDO binding);
	int reactivate(Long bindingId, LocalDateTime boundAt);
	int unbind(Long labId, Long sensorId, LocalDateTime unboundAt);
	boolean existsActiveByLabId(Long labId);
}
