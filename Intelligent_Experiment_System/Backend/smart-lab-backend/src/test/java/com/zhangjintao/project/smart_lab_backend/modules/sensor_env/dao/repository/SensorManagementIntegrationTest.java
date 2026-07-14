package com.zhangjintao.project.smart_lab_backend.modules.sensor_env.dao.repository;

import com.zhangjintao.project.smart_lab_backend.modules.equipment.dao.dataobject.DeviceDO;
import com.zhangjintao.project.smart_lab_backend.modules.equipment.dao.mapper.DeviceMapper;
import com.zhangjintao.project.smart_lab_backend.modules.equipment.dao.repository.DeviceSensorBindingRepository;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.dao.dataobject.LabDO;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.dao.mapper.LabMapper;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.dao.repository.LabSensorBindingRepository;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.domain.entity.SensorType;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.model.dto.SensorCreateCommand;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.model.dto.SensorUpdateCommand;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.service.SensorService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class SensorManagementIntegrationTest {

	@Autowired private SensorTypeRepository sensorTypeRepository;
	@Autowired private SensorRepository sensorRepository;
	@Autowired private SensorService sensorService;
	@Autowired private LabMapper labMapper;
	@Autowired private DeviceMapper deviceMapper;
	@Autowired private LabSensorBindingRepository labBindingRepository;
	@Autowired private DeviceSensorBindingRepository deviceBindingRepository;

	@Test
	void registerRebindAndDeleteShouldKeepMainRecordAndBindingsConsistent() {
		SensorType sensorType = sensorTypeRepository.save(SensorType.builder()
				.sensorTypeName("集成传感器-" + suffix())
				.build());
		LabDO lab = new LabDO();
		lab.setLabName("集成实验室-" + suffix());
		lab.setLabTypeId(1L);
		lab.setCapacity(10);
		lab.setRunningStatus(1);
		lab.setOpenStatus(1);
		labMapper.insert(lab);
		DeviceDO device = new DeviceDO();
		device.setDeviceNo("DEV-IT-" + suffix());
		device.setName("集成设备");
		device.setDeviceTypeId(1L);
		device.setDeviceStatus(2);
		device.setHealthScore(100);
		device.setVersion(1);
		deviceMapper.insert(device);

		var created = sensorService.create(SensorCreateCommand.builder()
				.sensorNo("SENSOR-IT-" + suffix().toUpperCase())
				.sensorTypeId(sensorType.getId())
				.targetType(2)
				.targetId(lab.getId())
				.build());
		assertThat(labBindingRepository.existsActiveByLabId(lab.getId())).isTrue();

		sensorService.update(created.getId(), SensorUpdateCommand.builder()
				.sensorTypeId(sensorType.getId())
				.targetType(1)
				.targetId(device.getId())
				.build());
		assertThat(labBindingRepository.existsActiveByLabId(lab.getId())).isFalse();
		assertThat(deviceBindingRepository.existsActiveByDeviceId(device.getId())).isTrue();

		sensorService.delete(created.getId());
		assertThat(sensorRepository.findById(created.getId())).isEmpty();
		assertThat(deviceBindingRepository.existsActiveByDeviceId(device.getId())).isFalse();
	}

	private String suffix() {
		return UUID.randomUUID().toString().substring(0, 8);
	}
}
