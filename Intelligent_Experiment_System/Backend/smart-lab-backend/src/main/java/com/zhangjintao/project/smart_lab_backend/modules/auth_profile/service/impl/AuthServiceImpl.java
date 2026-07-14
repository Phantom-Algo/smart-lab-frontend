package com.zhangjintao.project.smart_lab_backend.modules.auth_profile.service.impl;

import com.zhangjintao.project.smart_lab_backend.modules.auth_profile.config.AuthProfileProperties;
import com.zhangjintao.project.smart_lab_backend.modules.auth_profile.dao.dataobject.LoginSessionDO;
import com.zhangjintao.project.smart_lab_backend.modules.auth_profile.dao.dataobject.UserAccountDO;
import com.zhangjintao.project.smart_lab_backend.modules.auth_profile.dao.repository.LoginSessionRepository;
import com.zhangjintao.project.smart_lab_backend.modules.auth_profile.dao.repository.UserAccountRepository;
import com.zhangjintao.project.smart_lab_backend.modules.auth_profile.enums.AccountStatus;
import com.zhangjintao.project.smart_lab_backend.modules.auth_profile.enums.SessionStatus;
import com.zhangjintao.project.smart_lab_backend.modules.auth_profile.enums.UserType;
import com.zhangjintao.project.smart_lab_backend.modules.auth_profile.exception.AuthProfileErrorCode;
import com.zhangjintao.project.smart_lab_backend.modules.auth_profile.exception.AuthProfileException;
import com.zhangjintao.project.smart_lab_backend.modules.auth_profile.model.converter.AuthProfileConverter;
import com.zhangjintao.project.smart_lab_backend.modules.auth_profile.model.dto.AuthenticatedUser;
import com.zhangjintao.project.smart_lab_backend.modules.auth_profile.model.dto.UserAccountWithProfileDTO;
import com.zhangjintao.project.smart_lab_backend.modules.auth_profile.model.request.ChangePasswordRequest;
import com.zhangjintao.project.smart_lab_backend.modules.auth_profile.model.request.LoginRequest;
import com.zhangjintao.project.smart_lab_backend.modules.auth_profile.model.response.LoginResponse;
import com.zhangjintao.project.smart_lab_backend.modules.auth_profile.model.response.SecurityOverviewResponse;
import com.zhangjintao.project.smart_lab_backend.modules.auth_profile.service.AuthService;
import com.zhangjintao.project.smart_lab_backend.modules.auth_profile.service.CurrentUserQueryService;
import com.zhangjintao.project.smart_lab_backend.modules.auth_profile.utils.RandomSecretUtils;
import com.zhangjintao.project.smart_lab_backend.modules.auth_profile.utils.SessionNoGenerator;
import com.zhangjintao.project.smart_lab_backend.modules.auth_profile.utils.TokenHashUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

	private final UserAccountRepository userAccountRepository;
	private final LoginSessionRepository loginSessionRepository;
	private final CurrentUserQueryService currentUserQueryService;
	private final AuthProfileProperties authProfileProperties;
	private final PasswordEncoder passwordEncoder;

	@Override
	@Transactional
	public LoginResponse login(LoginRequest request, HttpServletRequest httpServletRequest) {
		loginSessionRepository.markExpiredSessions();
		UserAccountDO userAccountDO = userAccountRepository.findByUsername(request.getUsername())
				.orElseThrow(() -> new AuthProfileException(AuthProfileErrorCode.INVALID_CREDENTIALS));
		ensureAccountAvailableForLogin(userAccountDO);
		if (!matchesPassword(request.getPassword(), userAccountDO)) {
			handleLoginFailure(userAccountDO);
			throw new AuthProfileException(AuthProfileErrorCode.INVALID_CREDENTIALS);
		}
		userAccountRepository.resetFailedLoginState(userAccountDO.getId());
		LocalDateTime now = LocalDateTime.now();
		LocalDateTime expireAt = now.plusSeconds(authProfileProperties.getSession().getAccessTokenTtlSeconds());
		String accessToken = RandomSecretUtils.randomToken(32);
		LoginSessionDO loginSessionDO = new LoginSessionDO();
		loginSessionDO.setUserId(userAccountDO.getId());
		loginSessionDO.setSessionNo(SessionNoGenerator.generate());
		loginSessionDO.setAccessTokenHash(TokenHashUtils.sha256Hex(accessToken));
		loginSessionDO.setSessionStatus(SessionStatus.ONLINE.getCode());
		loginSessionDO.setLoginAt(now);
		loginSessionDO.setExpireAt(expireAt);
		loginSessionDO.setIpAddress(resolveClientIp(httpServletRequest));
		loginSessionDO.setDeviceType(resolveDeviceType(httpServletRequest.getHeader("User-Agent")));
		loginSessionDO.setUserAgent(trimToNull(httpServletRequest.getHeader("User-Agent")));
		loginSessionRepository.insert(loginSessionDO);
		userAccountRepository.updateLoginSuccess(userAccountDO.getId(), loginSessionDO.getIpAddress(), now);
		UserType userType = UserType.fromCode(userAccountDO.getUserType());
		return LoginResponse.builder()
				.userId(userAccountDO.getId())
				.username(userAccountDO.getUsername())
				.userType(userType.getCode())
				.userTypeName(userType.getDescription())
				.accessToken(accessToken)
				.tokenType("Bearer")
				.expireAt(expireAt)
				.passwordResetRequired(userAccountDO.getIsPasswordResetRequired() != null
						&& userAccountDO.getIsPasswordResetRequired() == 1)
				.build();
	}

	@Override
	@Transactional
	public void logoutCurrentSession() {
		AuthenticatedUser currentUser = currentUserQueryService.requireCurrentUser();
		loginSessionRepository.markLoggedOut(currentUser.getSessionId());
	}

	@Override
	@Transactional
	public void changePassword(ChangePasswordRequest request) {
		if (!request.getNewPassword().equals(request.getConfirmPassword())) {
			throw new AuthProfileException(AuthProfileErrorCode.PASSWORD_RULE_INVALID, "新密码与确认密码不一致");
		}
		validatePasswordRule(request.getNewPassword());
		UserAccountWithProfileDTO currentUser = currentUserQueryService.requireCurrentUserWithProfile();
		UserAccountDO userAccountDO = currentUser.getUserAccount();
		if (!matchesPassword(request.getOldPassword(), userAccountDO)) {
			throw new AuthProfileException(AuthProfileErrorCode.OLD_PASSWORD_MISMATCH);
		}
		if (matchesPassword(request.getNewPassword(), userAccountDO)) {
			throw new AuthProfileException(AuthProfileErrorCode.PASSWORD_RULE_INVALID, "新密码不能与原密码相同");
		}
		String newSalt = RandomSecretUtils.randomToken(12);
		String encodedPassword = passwordEncoder.encode(request.getNewPassword() + newSalt);
		userAccountRepository.updatePassword(userAccountDO.getId(), encodedPassword, newSalt, LocalDateTime.now());
		loginSessionRepository.invalidateAllByUserId(userAccountDO.getId(), currentUserQueryService.requireCurrentUser().getSessionId());
		loginSessionRepository.markLoggedOut(currentUserQueryService.requireCurrentUser().getSessionId());
	}

	@Override
	public SecurityOverviewResponse getCurrentSecurityOverview() {
		UserAccountWithProfileDTO currentUser = currentUserQueryService.requireCurrentUserWithProfile();
		UserAccountDO userAccountDO = currentUser.getUserAccount();
		return SecurityOverviewResponse.builder()
				.failedLoginCount(userAccountDO.getFailedLoginCount())
				.accountStatus(userAccountDO.getAccountStatus())
				.lockExpiresAt(userAccountDO.getLockExpiresAt())
				.lastLoginAt(userAccountDO.getLastLoginAt())
				.lastLoginIp(userAccountDO.getLastLoginIp())
				.lastPasswordChangedAt(userAccountDO.getLastPasswordChangedAt())
				.passwordResetRequired(userAccountDO.getIsPasswordResetRequired() != null
						&& userAccountDO.getIsPasswordResetRequired() == 1)
				.recentSessions(loginSessionRepository.listRecentByUserId(
								userAccountDO.getId(), authProfileProperties.getSession().getRecentSessionLimit())
						.stream()
						.map(AuthProfileConverter::toLoginSessionItemResponse)
						.toList())
				.build();
	}

	private void ensureAccountAvailableForLogin(UserAccountDO userAccountDO) {
		AccountStatus accountStatus = AccountStatus.fromCode(userAccountDO.getAccountStatus());
		if (accountStatus == AccountStatus.DISABLED) {
			throw new AuthProfileException(AuthProfileErrorCode.ACCOUNT_DISABLED);
		}
		if (accountStatus == AccountStatus.LOCKED) {
			if (userAccountDO.getLockExpiresAt() != null && userAccountDO.getLockExpiresAt().isBefore(LocalDateTime.now())) {
				userAccountRepository.unlockAccount(userAccountDO.getId());
				return;
			}
			throw new AuthProfileException(AuthProfileErrorCode.ACCOUNT_LOCKED,
					"账号已锁定，解锁时间：" + userAccountDO.getLockExpiresAt());
		}
	}

	private void handleLoginFailure(UserAccountDO userAccountDO) {
		userAccountRepository.increaseFailedLoginCount(userAccountDO.getId());
		int nextFailedCount = (userAccountDO.getFailedLoginCount() == null ? 0 : userAccountDO.getFailedLoginCount()) + 1;
		if (nextFailedCount >= authProfileProperties.getPassword().getMaxFailedAttempts()) {
			userAccountRepository.lockAccount(userAccountDO.getId(),
					LocalDateTime.now().plusMinutes(authProfileProperties.getPassword().getLockDurationMinutes()));
		}
	}

	private boolean matchesPassword(String rawPassword, UserAccountDO userAccountDO) {
		return passwordEncoder.matches(rawPassword + userAccountDO.getPasswordSalt(), userAccountDO.getPasswordHash());
	}

	private void validatePasswordRule(String password) {
		if (!StringUtils.hasText(password) || password.length() < authProfileProperties.getPassword().getMinLength()) {
			throw new AuthProfileException(AuthProfileErrorCode.PASSWORD_RULE_INVALID,
					"密码长度不能少于" + authProfileProperties.getPassword().getMinLength() + "位");
		}
	}

	private String resolveClientIp(HttpServletRequest request) {
		String forwardedFor = request.getHeader("X-Forwarded-For");
		if (StringUtils.hasText(forwardedFor)) {
			return forwardedFor.split(",")[0].trim();
		}
		String realIp = request.getHeader("X-Real-IP");
		if (StringUtils.hasText(realIp)) {
			return realIp.trim();
		}
		return request.getRemoteAddr();
	}

	private String resolveDeviceType(String userAgent) {
		if (!StringUtils.hasText(userAgent)) {
			return "UNKNOWN";
		}
		String lowerCaseUserAgent = userAgent.toLowerCase();
		if (lowerCaseUserAgent.contains("mobile") || lowerCaseUserAgent.contains("android") || lowerCaseUserAgent.contains("iphone")) {
			return "MOBILE";
		}
		return "PC";
	}

	private String trimToNull(String value) {
		return StringUtils.hasText(value) ? value.trim() : null;
	}
}
