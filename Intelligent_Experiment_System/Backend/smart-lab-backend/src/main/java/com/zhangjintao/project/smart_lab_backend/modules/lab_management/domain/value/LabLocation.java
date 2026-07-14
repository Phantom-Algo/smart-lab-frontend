package com.zhangjintao.project.smart_lab_backend.modules.lab_management.domain.value;

import lombok.Builder;
import lombok.Value;
import org.springframework.util.StringUtils;

@Value
@Builder
public class LabLocation {

	String campus;
	String building;
	Integer floor;
	String roomNo;

	public boolean isCompleteForUniqueCheck() {
		return StringUtils.hasText(campus)
				&& StringUtils.hasText(building)
				&& floor != null
				&& StringUtils.hasText(roomNo);
	}
}
