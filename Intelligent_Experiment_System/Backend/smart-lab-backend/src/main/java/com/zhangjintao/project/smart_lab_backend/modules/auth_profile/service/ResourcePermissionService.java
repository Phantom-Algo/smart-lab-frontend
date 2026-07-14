package com.zhangjintao.project.smart_lab_backend.modules.auth_profile.service;

import com.zhangjintao.project.smart_lab_backend.modules.auth_profile.model.response.ResourcePermissionResponse;

import java.util.List;

public interface ResourcePermissionService {

	List<ResourcePermissionResponse> listCurrentUserPermissions(Integer resourceType);

	boolean hasPermission(Long userId, Integer resourceType, Long resourceId, String actionCode);
}
