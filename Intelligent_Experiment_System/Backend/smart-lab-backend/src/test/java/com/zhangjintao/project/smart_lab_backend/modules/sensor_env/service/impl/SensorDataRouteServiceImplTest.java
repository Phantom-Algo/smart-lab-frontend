package com.zhangjintao.project.smart_lab_backend.modules.sensor_env.service.impl;

import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.model.dto.SensorDataRouteCommand;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.service.SensorDataMessagePublisher;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SensorDataRouteServiceImplTest {

	@Mock
	private SensorDataMessagePublisher publisher;

	@AfterEach
	void tearDown() {
		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			TransactionSynchronizationManager.clearSynchronization();
		}
	}

	@Test
	void shouldPublishImmediatelyWhenNoTransactionActive() {
		SensorDataRouteCommand command = SensorDataRouteCommand.builder().sensorNo("SENSOR-001").build();
		SensorDataRouteServiceImpl service = new SensorDataRouteServiceImpl(publisher);

		service.publishAfterCommit(command);

		verify(publisher).publish(command);
	}

	@Test
	void shouldPublishAfterTransactionCommit() {
		SensorDataRouteCommand command = SensorDataRouteCommand.builder().sensorNo("SENSOR-001").build();
		SensorDataRouteServiceImpl service = new SensorDataRouteServiceImpl(publisher);
		TransactionSynchronizationManager.initSynchronization();

		service.publishAfterCommit(command);

		verify(publisher, never()).publish(command);
		for (TransactionSynchronization synchronization : TransactionSynchronizationManager.getSynchronizations()) {
			synchronization.afterCommit();
		}
		verify(publisher).publish(command);
	}
}
