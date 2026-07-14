package com.zhangjintao.project.smart_lab_backend.modules.auth_profile.service.impl;

import com.zhangjintao.project.smart_lab_backend.modules.auth_profile.dao.dataobject.UserAccountDO;
import com.zhangjintao.project.smart_lab_backend.modules.auth_profile.dao.dataobject.UserProfileDO;
import com.zhangjintao.project.smart_lab_backend.modules.auth_profile.dao.repository.LoginSessionRepository;
import com.zhangjintao.project.smart_lab_backend.modules.auth_profile.dao.repository.UserAccountRepository;
import com.zhangjintao.project.smart_lab_backend.modules.auth_profile.dao.repository.UserProfileRepository;
import com.zhangjintao.project.smart_lab_backend.modules.auth_profile.enums.AccountStatus;
import com.zhangjintao.project.smart_lab_backend.modules.auth_profile.exception.AuthProfileErrorCode;
import com.zhangjintao.project.smart_lab_backend.modules.auth_profile.exception.AuthProfileException;
import com.zhangjintao.project.smart_lab_backend.modules.auth_profile.model.dto.AccountBindingDTO;
import com.zhangjintao.project.smart_lab_backend.modules.auth_profile.model.dto.CreatePersonAccountCommand;
import com.zhangjintao.project.smart_lab_backend.modules.auth_profile.model.dto.PersonProfileSyncCommand;
import com.zhangjintao.project.smart_lab_backend.modules.auth_profile.service.PersonAccountPort;
import com.zhangjintao.project.smart_lab_backend.modules.auth_profile.utils.RandomSecretUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PersonAccountPortImpl implements PersonAccountPort {

	private final UserAccountRepository userAccountRepository;
	private final UserProfileRepository userProfileRepository;
	private final LoginSessionRepository loginSessionRepository;
	private final PasswordEncoder passwordEncoder;

	@Override
	@Transactional
	public AccountBindingDTO createAccount(CreatePersonAccountCommand command) {
		userAccountRepository.findByPersonId(command.getPersonId()).ifPresent(account -> {
			throw new AuthProfileException(AuthProfileErrorCode.PERSON_ACCOUNT_ALREADY_BOUND);
		});
		UserAccountDO userAccountDO = new UserAccountDO();
		userAccountDO.setAccountNo(generateAccountNo(command.getPersonId()));
		userAccountDO.setUsername(command.getUsername());
		userAccountDO.setPersonId(command.getPersonId());
		userAccountDO.setUserType(command.getUserType());
		userAccountDO.setAccountStatus(AccountStatus.NORMAL.getCode());
		userAccountDO.setFailedLoginCount(0);
		userAccountDO.setIsPasswordResetRequired(1);
		String salt = RandomSecretUtils.randomToken(12);
		String initialPassword = resolveInitialPassword(command);
		userAccountDO.setPasswordSalt(salt);
		userAccountDO.setPasswordHash(passwordEncoder.encode(initialPassword + salt));
		userAccountDO.setLastPasswordChangedAt(LocalDateTime.now());
		userAccountRepository.insert(userAccountDO);

		UserProfileDO userProfileDO = new UserProfileDO();
		userProfileDO.setUserId(userAccountDO.getId());
		userProfileDO.setRealName(command.getRealName());
		userProfileDO.setGenderType(command.getGenderType());
		userProfileDO.setPhone(command.getPhone());
		userProfileDO.setEmail(command.getEmail());
		userProfileDO.setStudentNo(command.getStudentNo());
		userProfileDO.setEmployeeNo(command.getEmployeeNo());
		userProfileDO.setDepartmentName(command.getDepartmentName());
		userProfileDO.setClassName(command.getClassName());
		userProfileDO.setTitleName(command.getTitleName());
		userProfileDO.setProfileStatus(1);
		userProfileRepository.insert(userProfileDO);
		return toBinding(userAccountDO, initialPassword);
	}

	@Override
	@Transactional
	public void disableAccountByPersonId(Long personId, String reason) {
		Optional<UserAccountDO> accountOptional = userAccountRepository.findByPersonId(personId);
		if (accountOptional.isEmpty()) {
			return;
		}
		UserAccountDO userAccountDO = accountOptional.get();
		userAccountRepository.updateAccountStatusByPersonId(personId, AccountStatus.DISABLED.getCode());
		loginSessionRepository.invalidateAllByUserId(userAccountDO.getId(), null);
	}

	@Override
	public void enableAccountByPersonId(Long personId) {
		userAccountRepository.findByPersonId(personId)
				.orElseThrow(() -> new AuthProfileException(AuthProfileErrorCode.ACCOUNT_BINDING_NOT_FOUND));
		userAccountRepository.updateAccountStatusByPersonId(personId, AccountStatus.NORMAL.getCode());
	}

	@Override
	public Optional<AccountBindingDTO> getAccountBindingByPersonId(Long personId) {
		return userAccountRepository.findByPersonId(personId).map(account -> toBinding(account, null));
	}

	@Override
	public Optional<AccountBindingDTO> getAccountBindingByUserId(Long userId) {
		return userAccountRepository.findById(userId).map(account -> toBinding(account, null));
	}

	@Override
	public Map<Long, AccountBindingDTO> getAccountBindingsByPersonIds(Collection<Long> personIds) {
		List<Long> personIdList = personIds == null ? List.of() : personIds.stream().distinct().toList();
		return userAccountRepository.listByPersonIds(personIdList).stream()
				.map(account -> toBinding(account, null))
				.collect(Collectors.toMap(AccountBindingDTO::getPersonId, Function.identity()));
	}

	@Override
	@Transactional
	public void syncProfileByPerson(PersonProfileSyncCommand command) {
		UserAccountDO userAccountDO = userAccountRepository.findByPersonId(command.getPersonId())
				.orElseThrow(() -> new AuthProfileException(AuthProfileErrorCode.ACCOUNT_BINDING_NOT_FOUND));
		UserProfileDO userProfileDO = userProfileRepository.findByUserId(userAccountDO.getId())
				.orElseThrow(() -> new AuthProfileException(AuthProfileErrorCode.PROFILE_NOT_FOUND));
		userProfileDO.setRealName(command.getRealName());
		userProfileDO.setGenderType(command.getGenderType());
		userProfileDO.setPhone(command.getPhone());
		userProfileDO.setEmail(command.getEmail());
		userProfileDO.setStudentNo(command.getStudentNo());
		userProfileDO.setEmployeeNo(command.getEmployeeNo());
		userProfileDO.setDepartmentName(command.getDepartmentName());
		userProfileDO.setClassName(command.getClassName());
		userProfileDO.setTitleName(command.getTitleName());
		userProfileRepository.update(userProfileDO);
	}

	private AccountBindingDTO toBinding(UserAccountDO userAccountDO, String temporaryPassword) {
		return AccountBindingDTO.builder()
				.userId(userAccountDO.getId())
				.personId(userAccountDO.getPersonId())
				.username(userAccountDO.getUsername())
				.userType(userAccountDO.getUserType())
				.accountStatus(userAccountDO.getAccountStatus())
				.passwordResetRequired(userAccountDO.getIsPasswordResetRequired() != null
						&& userAccountDO.getIsPasswordResetRequired() == 1)
				.temporaryPassword(temporaryPassword)
				.build();
	}

	private String resolveInitialPassword(CreatePersonAccountCommand command) {
		if (Boolean.TRUE.equals(command.getAutoGeneratePassword())) {
			return RandomSecretUtils.randomToken(10);
		}
		if (!StringUtils.hasText(command.getInitialPassword())) {
			throw new AuthProfileException(AuthProfileErrorCode.PASSWORD_RULE_INVALID, "初始密码不能为空");
		}
		return command.getInitialPassword();
	}

	private String generateAccountNo(Long personId) {
		return "UA" + String.format("%010d", personId);
	}
}
