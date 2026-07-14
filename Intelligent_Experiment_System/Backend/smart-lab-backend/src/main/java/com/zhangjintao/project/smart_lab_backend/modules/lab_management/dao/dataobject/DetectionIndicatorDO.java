package com.zhangjintao.project.smart_lab_backend.modules.lab_management.dao.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("lab_detection_indicator")
public class DetectionIndicatorDO {

	@TableId(type = IdType.AUTO)
	private Long id;
	private Long labTypeId;
	private String indicatorName;
	private String unit;
	private String safeRange;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;
}
