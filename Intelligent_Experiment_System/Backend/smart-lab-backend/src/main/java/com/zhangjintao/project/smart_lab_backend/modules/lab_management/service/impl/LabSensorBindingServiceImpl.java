package com.zhangjintao.project.smart_lab_backend.modules.lab_management.service.impl;

import com.zhangjintao.project.smart_lab_backend.modules.lab_management.dao.dataobject.LabSensorBindingDO;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.dao.repository.LabRepository;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.dao.repository.LabSensorBindingRepository;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.exception.LabManagementErrorCode;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.exception.LabManagementException;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.service.LabSensorBindingService;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.enums.SensorBindingStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class LabSensorBindingServiceImpl implements LabSensorBindingService {

	private final LabRepository labRepository;
	private final LabSensorBindingRepository bindingRepository;

	@Override
	@Transactional
	public void bind(Long sensorId, Long labId) {
		labRepository.findByIdForUpdate(labId)
				.orElseThrow(() -> new LabManagementException(LabManagementErrorCode.LAB_NOT_FOUND));
		LocalDateTime now = LocalDateTime.now();
		bindingRepository.findForUpdate(labId, sensorId).ifPresentOrElse(binding -> {
			if (binding.getBindStatus() == SensorBindingStatus.BOUND.getCode()) {
				throw bindingConflict(sensorId, labId);
			}
			if (bindingRepository.reactivate(binding.getId(), now) != 1) {
				throw bindingConflict(sensorId, labId);
			}
		}, () -> {
			LabSensorBindingDO binding = new LabSensorBindingDO();
			binding.setLabId(labId);
			binding.setSensorId(sensorId);
			binding.setBindStatus(SensorBindingStatus.BOUND.getCode());
			binding.setBoundAt(now);
			bindingRepository.insert(binding);
		});
	}

	@Override
	@Transactional
	public void unbind(Long sensorId, Long labId) {
		labRepository.findByIdForUpdate(labId)
				.orElseThrow(() -> new LabManagementException(LabManagementErrorCode.LAB_NOT_FOUND));
		if (bindingRepository.unbind(labId, sensorId, LocalDateTime.now()) != 1) {
			throw bindingConflict(sensorId, labId);
		}
	}

	@Override
	public boolean existsActiveBindingByLabId(Long labId) {
		return bindingRepository.existsActiveByLabId(labId);
	}

	private LabManagementException bindingConflict(Long sensorId, Long labId) {
		return new LabManagementException(LabManagementErrorCode.LAB_SENSOR_BINDING_CONFLICT,
				"实验室传感器绑定关系与主记录不一致，sensorId=" + sensorId + ", labId=" + labId);
	}
}
