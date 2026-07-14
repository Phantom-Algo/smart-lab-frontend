package com.zhangjintao.project.smart_lab_backend.modules.sensor_env.config;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Data
@Validated
@Component
@ConfigurationProperties(prefix = "smart-lab.iot.sensor")
public class SensorProperties {

	private boolean statusDetectionEnabled = false;

	@NotNull
	private Duration offlineThreshold = Duration.ofMinutes(5);

	@NotNull
	private Duration offlineScanFixedDelay = Duration.ofMinutes(1);

	@Min(1)
	@Max(1000)
	private int offlineScanBatchSize = 200;

	@AssertTrue(message = "传感器离线阈值和扫描间隔必须为正数")
	public boolean isDurationConfigurationValid() {
		return offlineThreshold != null && !offlineThreshold.isZero() && !offlineThreshold.isNegative()
				&& offlineScanFixedDelay != null && !offlineScanFixedDelay.isZero()
				&& !offlineScanFixedDelay.isNegative();
	}
}
