package com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.dao.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("task_submission")
public class TaskSubmissionDO {

	@TableId(type = IdType.AUTO)
	private Long id;
	private String submissionNo;
	private Long taskId;
	private Long studentId;
	private String contentText;
	private String attachments;
	private Integer versionNo;
	private Long parentSubmissionId;
	private Integer currentFlag;
	private Integer submitStatus;
	private Integer isLate;
	private LocalDateTime submittedAt;
	private String returnedReason;
	private LocalDateTime returnedAt;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;
}
