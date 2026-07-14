package com.zhangjintao.project.smart_lab_backend.modules.sensor_env.dao.repository;

import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.domain.entity.SensorDataType;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.domain.entity.SensorType;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.service.SensorTypeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class SensorTypePersistenceIntegrationTest {

	@Autowired
	private SensorTypeRepository sensorTypeRepository;

	@Autowired
	private SensorDataTypeRepository sensorDataTypeRepository;

	@Autowired
	private SensorTypeService sensorTypeService;

	@Test
	void updateShouldAllowClearingDescription() {
		SensorType sensorType = sensorTypeRepository.save(SensorType.builder()
				.sensorTypeName(uniqueTypeName())
				.description("待清空描述")
				.build());

		sensorType.setDescription(null);
		SensorType updated = sensorTypeRepository.update(sensorType);

		assertThat(updated.getDescription()).isNull();
	}

	@Test
	void deleteTypeShouldCascadeDataTypesWhenNoSensorReferencesIt() {
		SensorType sensorType = sensorTypeRepository.save(SensorType.builder()
				.sensorTypeName(uniqueTypeName())
				.build());
		sensorDataTypeRepository.save(SensorDataType.builder()
				.sensorTypeId(sensorType.getId())
				.dataName("integration_value")
				.unit("unit")
				.build());

		sensorTypeService.delete(sensorType.getId());

		assertThat(sensorTypeRepository.findById(sensorType.getId())).isEmpty();
		assertThat(sensorDataTypeRepository.listBySensorTypeIds(List.of(sensorType.getId()))).isEmpty();
	}

	private String uniqueTypeName() {
		return "集成测试-" + UUID.randomUUID().toString().substring(0, 8);
	}
}
