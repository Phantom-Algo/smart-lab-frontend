package com.zhangjintao.project.smart_lab_backend.modules.sensor_env.service.impl;

import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.dao.repository.SensorDataTypeRepository;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.dao.repository.SensorRepository;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.dao.repository.SensorTypeRepository;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.domain.entity.SensorDataType;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.domain.entity.SensorType;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.exception.SensorEnvErrorCode;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.exception.SensorEnvException;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.model.dto.SensorDataTypeCreateCommand;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.model.dto.SensorDataTypeUpdateCommand;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.model.dto.SensorTypeCreateCommand;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.model.response.SensorTypeDetailResponse;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.service.SensorTypeCatalogCacheManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SensorTypeServiceImplTest {

	@Mock
	private SensorTypeRepository sensorTypeRepository;

	@Mock
	private SensorDataTypeRepository sensorDataTypeRepository;

	@Mock
	private SensorRepository sensorRepository;

	@Mock
	private SensorTypeCatalogCacheManager cacheManager;

	@InjectMocks
	private SensorTypeServiceImpl sensorTypeService;

	@Test
	void createShouldNormalizeFieldsAndEvictCache() {
		when(sensorTypeRepository.existsByName("温湿度传感器", null)).thenReturn(false);
		when(sensorTypeRepository.save(any(SensorType.class))).thenReturn(SensorType.builder()
				.id(1L)
				.sensorTypeName("温湿度传感器")
				.description("环境温湿度")
				.build());

		SensorTypeDetailResponse response = sensorTypeService.create(SensorTypeCreateCommand.builder()
				.sensorTypeName(" 温湿度传感器 ")
				.description(" 环境温湿度 ")
				.build());

		assertThat(response.getId()).isEqualTo(1L);
		ArgumentCaptor<SensorType> captor = ArgumentCaptor.forClass(SensorType.class);
		verify(sensorTypeRepository).save(captor.capture());
		assertThat(captor.getValue().getSensorTypeName()).isEqualTo("温湿度传感器");
		assertThat(captor.getValue().getDescription()).isEqualTo("环境温湿度");
		verify(cacheManager).evictAll();
	}

	@Test
	void createDuplicateNameShouldThrowConflict() {
		when(sensorTypeRepository.existsByName("烟雾传感器", null)).thenReturn(true);

		assertThatThrownBy(() -> sensorTypeService.create(SensorTypeCreateCommand.builder()
				.sensorTypeName("烟雾传感器")
				.build()))
				.isInstanceOfSatisfying(SensorEnvException.class, ex ->
						assertThat(ex.getCode()).isEqualTo(SensorEnvErrorCode.SENSOR_TYPE_NAME_DUPLICATE.getCode()));

		verify(sensorTypeRepository, never()).save(any());
		verify(cacheManager, never()).evictAll();
	}

	@Test
	void listAndDetailShouldUseSameCatalogWithDifferentResponses() {
		SensorType sensorType = sensorTypeWithDataType();
		when(cacheManager.listAll(any())).thenReturn(List.of(sensorType));

		assertThat(sensorTypeService.listAll())
				.singleElement()
				.satisfies(item -> {
					assertThat(item.getId()).isEqualTo(1L);
					assertThat(item.getDataTypeCount()).isEqualTo(1);
				});
		assertThat(sensorTypeService.getById(1L).getDataTypes())
				.extracting("dataName")
				.containsExactly("temperature");
	}

	@Test
	void getByIdNotFoundShouldThrowNotFound() {
		when(cacheManager.listAll(any())).thenReturn(List.of());

		assertThatThrownBy(() -> sensorTypeService.getById(99L))
				.isInstanceOfSatisfying(SensorEnvException.class, ex ->
						assertThat(ex.getCode()).isEqualTo(SensorEnvErrorCode.SENSOR_TYPE_NOT_FOUND.getCode()));
	}

	@Test
	void deleteInUseShouldNotDeleteTypeOrDataTypes() {
		when(sensorTypeRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(sensorTypeWithDataType()));
		when(sensorRepository.existsBySensorTypeId(1L)).thenReturn(true);

		assertThatThrownBy(() -> sensorTypeService.delete(1L))
				.isInstanceOfSatisfying(SensorEnvException.class, ex ->
						assertThat(ex.getCode()).isEqualTo(SensorEnvErrorCode.SENSOR_TYPE_IN_USE.getCode()));

		verify(sensorDataTypeRepository, never()).deleteBySensorTypeId(any());
		verify(sensorTypeRepository, never()).deleteById(any());
		verify(cacheManager, never()).evictAll();
	}

	@Test
	void deleteWithoutSensorShouldDeleteDataTypesBeforeType() {
		when(sensorTypeRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(sensorTypeWithDataType()));
		when(sensorRepository.existsBySensorTypeId(1L)).thenReturn(false);

		sensorTypeService.delete(1L);

		InOrder inOrder = inOrder(sensorDataTypeRepository, sensorTypeRepository);
		inOrder.verify(sensorDataTypeRepository).deleteBySensorTypeId(1L);
		inOrder.verify(sensorTypeRepository).deleteById(1L);
		verify(cacheManager).evictAll();
	}

	@Test
	void createDataTypeShouldRejectNonLowerSnakeCase() {
		when(sensorTypeRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(sensorTypeWithDataType()));

		assertThatThrownBy(() -> sensorTypeService.createDataType(1L, SensorDataTypeCreateCommand.builder()
				.dataName("Temperature")
				.unit("°C")
				.build()))
				.isInstanceOfSatisfying(SensorEnvException.class, ex ->
						assertThat(ex.getCode()).isEqualTo(SensorEnvErrorCode.SENSOR_ENV_PARAM_INVALID.getCode()));

		verify(sensorDataTypeRepository, never()).save(any());
	}

	@Test
	void updateDataTypeShouldLockParentAndDataTypeThenUpdate() {
		SensorDataType dataType = SensorDataType.builder()
				.id(2L)
				.sensorTypeId(1L)
				.dataName("temperature")
				.unit("°C")
				.build();
		when(sensorDataTypeRepository.findById(2L)).thenReturn(Optional.of(dataType));
		when(sensorTypeRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(sensorTypeWithDataType()));
		when(sensorDataTypeRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(dataType));
		when(sensorDataTypeRepository.existsByName(1L, "room_temperature", 2L)).thenReturn(false);
		when(sensorDataTypeRepository.update(any(SensorDataType.class))).thenAnswer(invocation -> invocation.getArgument(0));

		sensorTypeService.updateDataType(2L, SensorDataTypeUpdateCommand.builder()
				.dataName("room_temperature")
				.unit("°C")
				.build());

		verify(sensorTypeRepository).findByIdForUpdate(1L);
		verify(sensorDataTypeRepository).findByIdForUpdate(2L);
		verify(sensorDataTypeRepository).update(dataType);
		assertThat(dataType.getDataName()).isEqualTo("room_temperature");
		verify(cacheManager).evictAll();
	}

	private SensorType sensorTypeWithDataType() {
		return SensorType.builder()
				.id(1L)
				.sensorTypeName("温湿度传感器")
				.description("环境温湿度")
				.dataTypes(List.of(SensorDataType.builder()
						.id(2L)
						.sensorTypeId(1L)
						.dataName("temperature")
						.unit("°C")
						.build()))
				.build();
	}
}
