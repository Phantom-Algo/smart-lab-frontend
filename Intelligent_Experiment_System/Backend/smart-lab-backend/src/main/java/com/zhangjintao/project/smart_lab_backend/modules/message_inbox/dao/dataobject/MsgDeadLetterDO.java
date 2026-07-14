package com.zhangjintao.project.smart_lab_backend.modules.message_inbox.dao.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("msg_dead_letter")
public class MsgDeadLetterDO {

	@TableId(type = IdType.AUTO)
	private Long id;

	private Long eventId;

	private String eventNo;

	private String failureStage;

	private String errorCode;

	private String errorMessage;

	private String rawPayload;

	private Integer retryable;

	private Integer status;

	private LocalDateTime createdAt;

	private LocalDateTime updatedAt;
}
