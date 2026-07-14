package com.zhangjintao.project.smart_lab_backend.modules.message_inbox.dao.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("msg_event")
public class MsgEventDO {

	@TableId(type = IdType.AUTO)
	private Long id;

	private String eventNo;

	private String sourceSystem;

	private String messageType;

	private Integer priority;

	private LocalDateTime occurredAt;

	private String targetType;

	private String targets;

	private Long templateId;

	private String tempTemplate;

	private String params;

	private String title;

	private String content;

	private String contentFormat;

	private String actionUrl;

	private Integer dispatchStatus;

	private Integer retryCount;

	private LocalDateTime processedAt;

	private LocalDateTime createdAt;

	private LocalDateTime updatedAt;
}
