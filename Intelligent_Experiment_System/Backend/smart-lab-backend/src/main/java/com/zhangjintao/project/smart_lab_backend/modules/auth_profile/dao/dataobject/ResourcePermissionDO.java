package com.zhangjintao.project.smart_lab_backend.modules.auth_profile.dao.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("resource_permission")
public class ResourcePermissionDO {

	@TableId(type = IdType.AUTO)
	private Long id;

	private Long userId;

	private Integer resourceType;

	private Long resourceId;

	private String actionCode;

	private Integer permissionStatus;

	private LocalDateTime effectiveAt;

	private LocalDateTime expireAt;

	private String description;

	private String extraData;

	private LocalDateTime createdAt;

	private LocalDateTime updatedAt;

	private LocalDateTime deletedAt;
}
