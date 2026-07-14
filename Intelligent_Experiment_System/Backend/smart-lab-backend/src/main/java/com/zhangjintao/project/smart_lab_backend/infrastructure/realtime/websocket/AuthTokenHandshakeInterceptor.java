package com.zhangjintao.project.smart_lab_backend.infrastructure.realtime.websocket;

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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuthTokenHandshakeInterceptor implements HandshakeInterceptor {

	private final LoginSessionRepository loginSessionRepository;
	private final UserAccountRepository userAccountRepository;
	private final UserProfileRepository userProfileRepository;

	@Override
	public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
			WebSocketHandler wsHandler, Map<String, Object> attributes) {
		String token = extractToken(request);
		if (!StringUtils.hasText(token)) {
			response.setStatusCode(HttpStatus.UNAUTHORIZED);
			return false;
		}
		LoginSessionDO loginSession = loginSessionRepository.findActiveByAccessTokenHash(TokenHashUtils.sha256Hex(token))
				.orElse(null);
		if (loginSession == null) {
			response.setStatusCode(HttpStatus.UNAUTHORIZED);
			return false;
		}
		UserAccountDO userAccountDO = userAccountRepository.findById(loginSession.getUserId()).orElse(null);
		if (userAccountDO == null || userAccountDO.getAccountStatus() != AccountStatus.NORMAL.getCode()) {
			response.setStatusCode(HttpStatus.UNAUTHORIZED);
			return false;
		}
		UserProfileDO userProfileDO = userProfileRepository.findByUserId(userAccountDO.getId()).orElse(null);
		UserType userType = UserType.fromCode(userAccountDO.getUserType());
		AuthenticatedUser authenticatedUser = AuthenticatedUser.builder()
				.userId(userAccountDO.getId())
				.profileId(userProfileDO == null ? null : userProfileDO.getId())
				.personId(userAccountDO.getPersonId())
				.username(userAccountDO.getUsername())
				.userType(userType)
				.sessionId(loginSession.getId())
				.sessionNo(loginSession.getSessionNo())
				.authorities(List.of(userType.getAuthority()))
				.build();
		attributes.put(RealtimeWebSocketAuth.AUTH_USER_ATTR, authenticatedUser);
		attributes.put(RealtimeWebSocketAuth.USER_ID_ATTR, authenticatedUser.getUserId());
		return true;
	}

	@Override
	public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
			WebSocketHandler wsHandler, Exception exception) {
		// no-op
	}

	private String extractToken(ServerHttpRequest request) {
		if (!(request instanceof ServletServerHttpRequest servletRequest)) {
			return null;
		}
		HttpServletRequest httpServletRequest = servletRequest.getServletRequest();
		String token = httpServletRequest.getParameter("token");
		if (!StringUtils.hasText(token)) {
			return null;
		}
		return URLDecoder.decode(token, StandardCharsets.UTF_8);
	}
}