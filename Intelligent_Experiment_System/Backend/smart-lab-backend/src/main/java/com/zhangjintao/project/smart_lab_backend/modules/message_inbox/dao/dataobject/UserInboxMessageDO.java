package com.zhangjintao.project.smart_lab_backend.modules.message_inbox.dao.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user_inbox_message")
public class UserInboxMessageDO {

	@TableId(type = IdType.AUTO)
	private Long id;

	private Long userId;

	private Long messageId;

	private Integer isRead;

	private LocalDateTime readAt;

	private Integer isDeleted;

	private LocalDateTime deletedAt;

	private LocalDateTime receivedAt;
}
