package com.zhangjintao.project.smart_lab_backend.modules.auth_profile.service.impl;

import com.zhangjintao.project.smart_lab_backend.modules.auth_profile.dao.repository.ResourcePermissionRepository;
import com.zhangjintao.project.smart_lab_backend.modules.auth_profile.model.converter.AuthProfileConverter;
import com.zhangjintao.project.smart_lab_backend.modules.auth_profile.model.response.ResourcePermissionResponse;
import com.zhangjintao.project.smart_lab_backend.modules.auth_profile.service.CurrentUserQueryService;
import com.zhangjintao.project.smart_lab_backend.modules.auth_profile.service.ResourcePermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ResourcePermissionServiceImpl implements ResourcePermissionService {

	private final CurrentUserQueryService currentUserQueryService;
	private final ResourcePermissionRepository resourcePermissionRepository;

	@Override
	public List<ResourcePermissionResponse> listCurrentUserPermissions(Integer resourceType) {
		Long userId = currentUserQueryService.requireCurrentUser().getUserId();
		if (resourceType == null) {
			return resourcePermissionRepository.listActiveByUserId(userId).stream()
					.map(AuthProfileConverter::toResourcePermissionResponse)
					.toList();
		}
		return resourcePermissionRepository.listActiveByUserIdAndResourceType(userId, resourceType).stream()
				.map(AuthProfileConverter::toResourcePermissionResponse)
				.toList();
	}

	@Override
	public boolean hasPermission(Long userId, Integer resourceType, Long resourceId, String actionCode) {
		return resourcePermissionRepository.existsActivePermission(userId, resourceType, resourceId, actionCode);
	}
}
