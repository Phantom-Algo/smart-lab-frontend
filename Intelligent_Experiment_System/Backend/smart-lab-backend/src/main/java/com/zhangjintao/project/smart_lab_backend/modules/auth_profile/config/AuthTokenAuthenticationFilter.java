package com.zhangjintao.project.smart_lab_backend.modules.auth_profile.config;

import com.zhangjintao.project.smart_lab_backend.modules.auth_profile.constants.AuthProfileConstants;
import com.zhangjintao.project.smart_lab_backend.modules.auth_profile.dao.dataobject.LoginSessionDO;
import com.zhangjintao.project.smart_lab_backend.modules.auth_profile.dao.dataobject.UserAccountDO;
import com.zhangjintao.project.smart_lab_backend.modules.auth_profile.dao.dataobject.UserProfileDO;
import com.zhangjintao.project.smart_lab_backend.modules.auth_profile.dao.repository.LoginSessionRepository;
import com.zhangjintao.project.smart_lab_backend.modules.auth_profile.dao.repository.UserAccountRepository;
import com.zhangjintao.project.smart_lab_backend.modules.auth_profile.dao.repository.UserProfileRepository;
import com.zhangjintao.project.smart_lab_backend.modules.auth_profile.enums.AccountStatus;
import com.zhangjintao.project.smart_lab_backend.modules.auth_profile.enums.UserType;
import com.zhangjintao.project.smart_lab_backend.modules.auth_profile.model.dto.AuthenticatedUser;
import com.zhangjintao.project.smart_lab_backend.modules.auth_profile.utils.TokenHashUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class AuthTokenAuthenticationFilter extends OncePerRequestFilter {

	private final LoginSessionRepository loginSessionRepository;
	private final UserAccountRepository userAccountRepository;
	private final UserProfileRepository userProfileRepository;

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		String token = resolveBearerToken(request);
		if (StringUtils.hasText(token)) {
			loginSessionRepository.findActiveByAccessTokenHash(TokenHashUtils.sha256Hex(token))
					.flatMap(this::buildAuthentication)
					.ifPresent(authentication -> SecurityContextHolder.getContext().setAuthentication(authentication));
		}
		filterChain.doFilter(request, response);
	}

	private java.util.Optional<UsernamePasswordAuthenticationToken> buildAuthentication(LoginSessionDO loginSessionDO) {
		UserAccountDO userAccountDO = userAccountRepository.findById(loginSessionDO.getUserId()).orElse(null);
		if (userAccountDO == null || userAccountDO.getAccountStatus() != AccountStatus.NORMAL.getCode()) {
			return java.util.Optional.empty();
		}
		UserProfileDO userProfileDO = userProfileRepository.findByUserId(userAccountDO.getId()).orElse(null);
		UserType userType = UserType.fromCode(userAccountDO.getUserType());
		AuthenticatedUser authenticatedUser = AuthenticatedUser.builder()
				.userId(userAccountDO.getId())
				.profileId(userProfileDO == null ? null : userProfileDO.getId())
				.personId(userAccountDO.getPersonId())
				.username(userAccountDO.getUsername())
				.userType(userType)
				.sessionId(loginSessionDO.getId())
				.sessionNo(loginSessionDO.getSessionNo())
				.authorities(List.of(userType.getAuthority()))
				.build();
		UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
				authenticatedUser,
				null,
				List.of(new SimpleGrantedAuthority(userType.getAuthority())));
		authentication.setDetails(loginSessionDO);
		return java.util.Optional.of(authentication);
	}

	private String resolveBearerToken(HttpServletRequest request) {
		String authorization = request.getHeader(AuthProfileConstants.AUTHORIZATION_HEADER);
		if (!StringUtils.hasText(authorization) || !authorization.startsWith(AuthProfileConstants.BEARER_PREFIX)) {
			return null;
		}
		return authorization.substring(AuthProfileConstants.BEARER_PREFIX.length()).trim();
	}
}
