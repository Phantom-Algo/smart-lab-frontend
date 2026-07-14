package com.zhangjintao.project.smart_lab_backend.modules.personnel_management.dao.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("department")
public class DepartmentDO {

	@TableId(type = IdType.AUTO)
	private Long id;
	private String departmentNo;
	private String departmentCode;
	private String departmentName;
	private Long parentId;
	private Long leaderPersonId;
	private Integer departmentStatus;
	private Integer sortNo;
	private String description;
	private String extraData;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;
	private LocalDateTime deletedAt;
}
