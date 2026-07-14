package com.zhangjintao.project.smart_lab_backend.modules.personnel_management.model.response;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class PositionResponse {

	Long id;
	String positionNo;
	String positionCode;
	String positionName;
	Integer positionType;
	Long departmentId;
	Integer positionStatus;
	Integer sortNo;
	String description;
}
