package com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.dao.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("grade_assessment")
public class GradeAssessmentDO {

	@TableId(type = IdType.AUTO)
	private Long id;
	private Long gradeItemId;
	private Long studentId;
	private Long courseId;
	private Long taskSubmissionId;
	private BigDecimal score;
	private Long graderId;
	private LocalDateTime gradedAt;
	private String comment;
	private Integer gradeStatus;
	private LocalDateTime publishedAt;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;
}
