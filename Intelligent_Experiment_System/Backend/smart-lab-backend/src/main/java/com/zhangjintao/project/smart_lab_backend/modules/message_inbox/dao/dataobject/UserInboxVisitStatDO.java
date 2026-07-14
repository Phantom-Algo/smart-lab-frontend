package com.zhangjintao.project.smart_lab_backend.modules.message_inbox.dao.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user_inbox_visit_stat")
public class UserInboxVisitStatDO {

	@TableId(type = IdType.AUTO)
	private Long id;

	private Long userId;

	private LocalDateTime lastViewedAt;

	private LocalDateTime createdAt;

	private LocalDateTime updatedAt;
}
