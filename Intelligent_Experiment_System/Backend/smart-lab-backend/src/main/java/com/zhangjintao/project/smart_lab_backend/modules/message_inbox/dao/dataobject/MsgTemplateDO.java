package com.zhangjintao.project.smart_lab_backend.modules.message_inbox.dao.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("msg_template")
public class MsgTemplateDO {

	@TableId(type = IdType.AUTO)
	private Long id;

	private String templateCode;

	private String templateName;

	private String messageType;

	private String titleTemplate;

	private String contentTemplate;

	private String contentFormat;

	private String paramSchema;

	private Integer status;

	private Long createdBy;

	private Long updatedBy;

	private LocalDateTime createdAt;

	private LocalDateTime updatedAt;
}
