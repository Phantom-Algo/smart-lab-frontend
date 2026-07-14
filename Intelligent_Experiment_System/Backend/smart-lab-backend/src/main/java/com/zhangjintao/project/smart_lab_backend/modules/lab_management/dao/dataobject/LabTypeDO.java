package com.zhangjintao.project.smart_lab_backend.modules.lab_management.dao.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("lab_type")
public class LabTypeDO {

	@TableId(type = IdType.AUTO)
	private Long id;
	private String labTypeName;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;
}
