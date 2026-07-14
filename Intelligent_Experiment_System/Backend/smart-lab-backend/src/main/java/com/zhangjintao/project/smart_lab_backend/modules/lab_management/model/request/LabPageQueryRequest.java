package com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.request;

import com.zhangjintao.project.smart_lab_backend.modules.lab_management.constants.LabManagementConstants;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class LabPageQueryRequest {

	@Size(max = 128, message = "搜索关键词长度不能超过128")
	private String keyword;

	@Size(max = 64, message = "校区长度不能超过64")
	private String campus;

	@Size(max = 64, message = "楼栋长度不能超过64")
	private String building;

	@Min(value = 1, message = "楼层必须大于等于1")
	@Max(value = 99, message = "楼层不能超过99")
	private Integer floor;

	@Size(max = 32, message = "房间号长度不能超过32")
	private String roomNo;

	@Positive(message = "实验室类型ID必须为正数")
	private Long labTypeId;

	@Min(value = 1, message = "使用状态只能是1到4")
	@Max(value = 4, message = "使用状态只能是1到4")
	private Integer runningStatus;

	@Min(value = 1, message = "开放状态只能是1到3")
	@Max(value = 3, message = "开放状态只能是1到3")
	private Integer openStatus;

	@Min(value = 1, message = "页码必须大于等于1")
	private Long page = LabManagementConstants.DEFAULT_PAGE;

	@Min(value = 1, message = "每页条数必须大于等于1")
	@Max(value = 100, message = "每页条数不能超过100")
	private Long pageSize = LabManagementConstants.DEFAULT_PAGE_SIZE;

	@Size(max = 32, message = "排序字段长度不能超过32")
	private String sortField = "created_at";

	@Pattern(regexp = "(?i)asc|desc", message = "排序方向只能是asc或desc")
	private String sortOrder = "desc";
}
