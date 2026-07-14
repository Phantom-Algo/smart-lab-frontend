package com.zhangjintao.project.smart_lab_backend.modules.personnel_management.dao.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("position")
public class PositionDO {

	@TableId(type = IdType.AUTO)
	private Long id;
	private String positionNo;
	private String positionCode;
	private String positionName;
	private Integer positionType;
	private Long departmentId;
	private Integer positionStatus;
	private Integer sortNo;
	private String description;
	private String extraData;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;
	private LocalDateTime deletedAt;
}
