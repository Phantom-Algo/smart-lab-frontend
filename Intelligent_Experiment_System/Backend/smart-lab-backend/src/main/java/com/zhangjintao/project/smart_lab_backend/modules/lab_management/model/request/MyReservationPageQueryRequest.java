package com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.request;

import com.zhangjintao.project.smart_lab_backend.modules.lab_management.constants.LabManagementConstants;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class MyReservationPageQueryRequest {

	@Min(value = 1, message = "预约状态只能是1到6")
	@Max(value = 6, message = "预约状态只能是1到6")
	private Integer status;

	@Min(value = 1, message = "页码必须大于等于1")
	private Long page = LabManagementConstants.DEFAULT_PAGE;

	@Min(value = 1, message = "每页条数必须大于等于1")
	@Max(value = 100, message = "每页条数不能超过100")
	private Long pageSize = LabManagementConstants.DEFAULT_PAGE_SIZE;
}
