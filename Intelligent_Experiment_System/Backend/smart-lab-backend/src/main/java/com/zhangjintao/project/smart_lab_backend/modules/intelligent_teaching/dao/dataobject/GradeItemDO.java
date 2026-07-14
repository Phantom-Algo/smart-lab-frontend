package com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.dao.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("grade_item")
public class GradeItemDO {

	@TableId(type = IdType.AUTO)
	private Long id;
	private Long courseId;
	private String itemName;
	private Integer itemType;
	private BigDecimal itemWeight;
	private BigDecimal fullScore;
	private Long relatedTaskId;
	private Integer sortNo;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;
}
