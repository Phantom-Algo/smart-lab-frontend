package com.zhangjintao.project.smart_lab_backend.modules.auth_profile.model.response;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

@Value
@Builder
public class ResourcePermissionResponse {

	Long id;
	Integer resourceType;
	String resourceTypeName;
	Long resourceId;
	String actionCode;
	Integer permissionStatus;
	LocalDateTime effectiveAt;
	LocalDateTime expireAt;
	String description;
}
