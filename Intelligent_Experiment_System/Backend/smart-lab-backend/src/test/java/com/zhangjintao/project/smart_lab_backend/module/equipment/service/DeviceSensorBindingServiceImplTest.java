package com.zhangjintao.project.smart_lab_backend.module.equipment.service;

import com.zhangjintao.project.smart_lab_backend.modules.equipment.dao.dataobject.DeviceDO;
import com.zhangjintao.project.smart_lab_backend.modules.equipment.dao.repository.DeviceRepository;
import com.zhangjintao.project.smart_lab_backend.modules.equipment.dao.repository.DeviceSensorBindingRepository;
import com.zhangjintao.project.smart_lab_backend.modules.equipment.service.impl.DeviceSensorBindingServiceImpl;
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
class DeviceSensorBindingServiceImplTest {

	@Mock private DeviceRepository deviceRepository;
	@Mock private DeviceSensorBindingRepository bindingRepository;
	@InjectMocks private DeviceSensorBindingServiceImpl service;

	@Test
	void bindShouldLockDeviceAndInsertBinding() {
		DeviceDO device = new DeviceDO();
		device.setId(200L);
		when(deviceRepository.findByIdForUpdate(200L)).thenReturn(Optional.of(device));
		when(bindingRepository.findForUpdate(200L, 10L)).thenReturn(Optional.empty());

		service.bind(10L, 200L);

		var captor = ArgumentCaptor.forClass(
				com.zhangjintao.project.smart_lab_backend.modules.equipment.dao.dataobject.DeviceSensorBindingDO.class);
		verify(bindingRepository).insert(captor.capture());
		assertThat(captor.getValue().getSensorId()).isEqualTo(10L);
		assertThat(captor.getValue().getDeviceId()).isEqualTo(200L);
		assertThat(captor.getValue().getBindStatus()).isEqualTo(1);
	}
}
