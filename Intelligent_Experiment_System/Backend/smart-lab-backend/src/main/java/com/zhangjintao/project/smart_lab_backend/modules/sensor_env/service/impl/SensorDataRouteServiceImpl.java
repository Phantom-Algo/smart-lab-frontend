package com.zhangjintao.project.smart_lab_backend.modules.sensor_env.service.impl;

import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.model.dto.SensorDataRouteCommand;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.service.SensorDataMessagePublisher;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.service.SensorDataRouteService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
@RequiredArgsConstructor
public class SensorDataRouteServiceImpl implements SensorDataRouteService {

	private final SensorDataMessagePublisher publisher;

	@Override
	public void publishAfterCommit(SensorDataRouteCommand command) {
		Runnable task = () -> publisher.publish(command);
		if (!TransactionSynchronizationManager.isSynchronizationActive()) {
			task.run();
			return;
		}
		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCommit() {
				task.run();
			}
		});
	}
}
