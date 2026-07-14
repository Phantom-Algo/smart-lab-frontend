package com.zhangjintao.project.smart_lab_backend.modules.auth_profile.utils;

import com.zhangjintao.project.smart_lab_backend.modules.auth_profile.model.dto.AuthenticatedUser;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

public final class SecurityContextUtils {

	private SecurityContextUtils() {
	}

	public static Optional<AuthenticatedUser> getAuthenticatedUser() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !authentication.isAuthenticated()
				|| authentication instanceof AnonymousAuthenticationToken
				|| !(authentication.getPrincipal() instanceof AuthenticatedUser authenticatedUser)) {
			return Optional.empty();
		}
		return Optional.of(authenticatedUser);
	}
}
