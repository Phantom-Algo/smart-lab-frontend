package com.zhangjintao.project.smart_lab_backend.modules.lab_management.service.impl;

import com.zhangjintao.project.smart_lab_backend.modules.lab_management.dao.repository.LabRepository;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.dao.repository.LabTypeRepository;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.domain.entity.LabType;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.exception.LabManagementErrorCode;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.exception.LabManagementException;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.dto.LabTypeCreateCommand;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.dto.LabTypeUpdateCommand;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.response.LabTypeResponse;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.service.LabTypeCacheManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LabTypeServiceImplTest {

	@Mock
	private LabTypeRepository labTypeRepository;

	@Mock
	private LabRepository labRepository;

	@Mock
	private LabTypeCacheManager labTypeCacheManager;

	@InjectMocks
	private LabTypeServiceImpl labTypeService;

	@Test
	void createSuccessShouldNormalizeNameAndEvictCache() {
		when(labTypeRepository.existsByName("化学实验室", null)).thenReturn(false);
		when(labTypeRepository.save(any(LabType.class))).thenReturn(LabType.builder()
				.id(1L)
				.labTypeName("化学实验室")
				.build());

		LabTypeResponse response = labTypeService.create(LabTypeCreateCommand.builder()
				.labTypeName(" 化学实验室 ")
				.build());

		assertThat(response.getId()).isEqualTo(1L);
		assertThat(response.getLabTypeName()).isEqualTo("化学实验室");
		ArgumentCaptor<LabType> labTypeCaptor = ArgumentCaptor.forClass(LabType.class);
		verify(labTypeRepository).save(labTypeCaptor.capture());
		assertThat(labTypeCaptor.getValue().getLabTypeName()).isEqualTo("化学实验室");
		verify(labTypeCacheManager).evictAll();
	}

	@Test
	void createDuplicateNameShouldThrowBusinessException() {
		when(labTypeRepository.existsByName("化学实验室", null)).thenReturn(true);

		assertThatThrownBy(() -> labTypeService.create(LabTypeCreateCommand.builder()
				.labTypeName("化学实验室")
				.build()))
				.isInstanceOfSatisfying(LabManagementException.class, ex ->
						assertThat(ex.getCode()).isEqualTo(LabManagementErrorCode.LAB_TYPE_NAME_DUPLICATE.getCode()));

		verify(labTypeRepository, never()).save(any());
		verify(labTypeCacheManager, never()).evictAll();
	}

	@Test
	void updateNotFoundShouldThrowBusinessException() {
		when(labTypeRepository.findById(1L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> labTypeService.update(1L, LabTypeUpdateCommand.builder()
				.labTypeName("物理实验室")
				.build()))
				.isInstanceOfSatisfying(LabManagementException.class, ex ->
						assertThat(ex.getCode()).isEqualTo(LabManagementErrorCode.LAB_TYPE_NOT_FOUND.getCode()));

		verify(labTypeRepository, never()).update(any());
		verify(labTypeCacheManager, never()).evictAll();
	}

	@Test
	void updateDuplicateNameShouldThrowBusinessException() {
		when(labTypeRepository.findById(1L)).thenReturn(Optional.of(LabType.builder()
				.id(1L)
				.labTypeName("旧类型")
				.build()));
		when(labTypeRepository.existsByName("物理实验室", 1L)).thenReturn(true);

		assertThatThrownBy(() -> labTypeService.update(1L, LabTypeUpdateCommand.builder()
				.labTypeName("物理实验室")
				.build()))
				.isInstanceOfSatisfying(LabManagementException.class, ex ->
						assertThat(ex.getCode()).isEqualTo(LabManagementErrorCode.LAB_TYPE_NAME_DUPLICATE.getCode()));

		verify(labTypeRepository, never()).update(any());
		verify(labTypeCacheManager, never()).evictAll();
	}

	@Test
	void deleteInUseShouldThrowBusinessException() {
		when(labTypeRepository.findById(1L)).thenReturn(Optional.of(LabType.builder()
				.id(1L)
				.labTypeName("化学实验室")
				.build()));
		when(labRepository.existsByLabTypeId(1L)).thenReturn(true);

		assertThatThrownBy(() -> labTypeService.delete(1L))
				.isInstanceOfSatisfying(LabManagementException.class, ex ->
						assertThat(ex.getCode()).isEqualTo(LabManagementErrorCode.LAB_TYPE_IN_USE.getCode()));

		verify(labTypeRepository, never()).deleteById(any());
		verify(labTypeCacheManager, never()).evictAll();
	}

	@Test
	void deleteSuccessShouldEvictCache() {
		when(labTypeRepository.findById(1L)).thenReturn(Optional.of(LabType.builder()
				.id(1L)
				.labTypeName("化学实验室")
				.build()));
		when(labRepository.existsByLabTypeId(1L)).thenReturn(false);

		labTypeService.delete(1L);

		verify(labTypeRepository).deleteById(1L);
		verify(labTypeCacheManager).evictAll();
	}
}
