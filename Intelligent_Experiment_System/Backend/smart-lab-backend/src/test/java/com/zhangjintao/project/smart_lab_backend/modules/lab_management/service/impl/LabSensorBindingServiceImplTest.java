package com.zhangjintao.project.smart_lab_backend.modules.lab_management.service.impl;

import com.zhangjintao.project.smart_lab_backend.modules.lab_management.dao.repository.LabRepository;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.dao.repository.LabSensorBindingRepository;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.domain.entity.Lab;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LabSensorBindingServiceImplTest {

	@Mock private LabRepository labRepository;
	@Mock private LabSensorBindingRepository bindingRepository;
	@InjectMocks private LabSensorBindingServiceImpl service;

	@Test
	void bindShouldLockLabAndInsertBinding() {
		when(labRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(Lab.builder().id(100L).build()));
		when(bindingRepository.findForUpdate(100L, 10L)).thenReturn(Optional.empty());

		service.bind(10L, 100L);

		var captor = ArgumentCaptor.forClass(
				com.zhangjintao.project.smart_lab_backend.modules.lab_management.dao.dataobject.LabSensorBindingDO.class);
		verify(bindingRepository).insert(captor.capture());
		assertThat(captor.getValue().getSensorId()).isEqualTo(10L);
		assertThat(captor.getValue().getLabId()).isEqualTo(100L);
		assertThat(captor.getValue().getBindStatus()).isEqualTo(1);
	}
}
