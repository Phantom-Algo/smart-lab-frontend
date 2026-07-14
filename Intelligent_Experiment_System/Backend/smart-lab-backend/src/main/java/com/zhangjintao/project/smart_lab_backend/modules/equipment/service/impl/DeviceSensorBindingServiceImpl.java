package com.zhangjintao.project.smart_lab_backend.modules.equipment.service.impl;

import com.zhangjintao.project.smart_lab_backend.modules.equipment.dao.dataobject.DeviceSensorBindingDO;
import com.zhangjintao.project.smart_lab_backend.modules.equipment.dao.repository.DeviceRepository;
import com.zhangjintao.project.smart_lab_backend.modules.equipment.dao.repository.DeviceSensorBindingRepository;
import com.zhangjintao.project.smart_lab_backend.modules.equipment.exception.EquipmentErrorCode;
import com.zhangjintao.project.smart_lab_backend.modules.equipment.exception.EquipmentException;
import com.zhangjintao.project.smart_lab_backend.modules.equipment.service.DeviceSensorBindingService;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.enums.SensorBindingStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class DeviceSensorBindingServiceImpl implements DeviceSensorBindingService {

	private final DeviceRepository deviceRepository;
	private final DeviceSensorBindingRepository bindingRepository;

	@Override
	@Transactional
	public void bind(Long sensorId, Long deviceId) {
		deviceRepository.findByIdForUpdate(deviceId)
				.orElseThrow(() -> new EquipmentException(EquipmentErrorCode.DEVICE_NOT_FOUND));
		LocalDateTime now = LocalDateTime.now();
		bindingRepository.findForUpdate(deviceId, sensorId).ifPresentOrElse(binding -> {
			if (binding.getBindStatus() == SensorBindingStatus.BOUND.getCode()) {
				throw bindingConflict(sensorId, deviceId);
			}
			if (bindingRepository.reactivate(binding.getId(), now) != 1) {
				throw bindingConflict(sensorId, deviceId);
			}
		}, () -> {
			DeviceSensorBindingDO binding = new DeviceSensorBindingDO();
			binding.setDeviceId(deviceId);
			binding.setSensorId(sensorId);
			binding.setBindStatus(SensorBindingStatus.BOUND.getCode());
			binding.setBoundAt(now);
			bindingRepository.insert(binding);
		});
	}

	@Override
	@Transactional
	public void unbind(Long sensorId, Long deviceId) {
		deviceRepository.findByIdForUpdate(deviceId)
				.orElseThrow(() -> new EquipmentException(EquipmentErrorCode.DEVICE_NOT_FOUND));
		if (bindingRepository.unbind(deviceId, sensorId, LocalDateTime.now()) != 1) {
			throw bindingConflict(sensorId, deviceId);
		}
	}

	@Override
	public boolean existsActiveBindingByDeviceId(Long deviceId) {
		return bindingRepository.existsActiveByDeviceId(deviceId);
	}

	private EquipmentException bindingConflict(Long sensorId, Long deviceId) {
		return new EquipmentException(EquipmentErrorCode.DEVICE_SENSOR_BINDING_CONFLICT,
				"设备传感器绑定关系与主记录不一致，sensorId=" + sensorId + ", deviceId=" + deviceId);
	}
}
