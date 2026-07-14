package com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreateLabRequest {

	@NotBlank(message = "实验室名称不能为空")
	@Size(max = 128, message = "实验室名称长度不能超过128")
	private String labName;

	@NotNull(message = "实验室类型不能为空")
	@Positive(message = "实验室类型ID必须为正数")
	private Long labTypeId;

	@Size(max = 64, message = "校区长度不能超过64")
	private String campus;

	@Size(max = 64, message = "楼栋长度不能超过64")
	private String building;

	@Min(value = 1, message = "楼层必须大于等于1")
	@Max(value = 99, message = "楼层不能超过99")
	private Integer floor;

	@Size(max = 32, message = "房间号长度不能超过32")
	private String roomNo;

	@Min(value = 0, message = "最大容纳人数不能小于0")
	private Integer capacity;

	@DecimalMin(value = "0.00", inclusive = false, message = "面积必须大于0")
	private BigDecimal areaM2;

	@Positive(message = "负责人用户ID必须为正数")
	private Long dutyUserId;

	@Size(max = 512, message = "描述长度不能超过512")
	private String description;
}
