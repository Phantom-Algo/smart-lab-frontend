package com.zhangjintao.project.smart_lab_backend.modules.personnel_management.service.impl;

import com.zhangjintao.project.smart_lab_backend.modules.auth_profile.model.dto.AccountBindingDTO;
import com.zhangjintao.project.smart_lab_backend.modules.auth_profile.model.dto.CreatePersonAccountCommand;
import com.zhangjintao.project.smart_lab_backend.modules.auth_profile.model.dto.PersonProfileSyncCommand;
import com.zhangjintao.project.smart_lab_backend.modules.auth_profile.service.PersonAccountPort;
import com.zhangjintao.project.smart_lab_backend.modules.auth_profile.enums.UserType;
import com.zhangjintao.project.smart_lab_backend.modules.personnel_management.dao.dataobject.DepartmentDO;
import com.zhangjintao.project.smart_lab_backend.modules.personnel_management.dao.dataobject.PersonDO;
import com.zhangjintao.project.smart_lab_backend.modules.personnel_management.dao.dataobject.PositionDO;
import com.zhangjintao.project.smart_lab_backend.modules.personnel_management.enums.PersonStatus;
import com.zhangjintao.project.smart_lab_backend.modules.personnel_management.enums.PersonType;
import com.zhangjintao.project.smart_lab_backend.modules.personnel_management.exception.PersonnelManagementErrorCode;
import com.zhangjintao.project.smart_lab_backend.modules.personnel_management.exception.PersonnelManagementException;
import com.zhangjintao.project.smart_lab_backend.modules.personnel_management.model.converter.PersonnelManagementConverter;
import com.zhangjintao.project.smart_lab_backend.modules.personnel_management.model.dto.PersonAccountSummaryDTO;
import com.zhangjintao.project.smart_lab_backend.modules.personnel_management.model.request.PersonCreateAccountRequest;
import com.zhangjintao.project.smart_lab_backend.modules.personnel_management.service.PersonAccountOrchestrator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class PersonAccountOrchestratorImpl implements PersonAccountOrchestrator {

	private final PersonnelManagementSupport support;
	private final PersonAccountPort personAccountPort;

	@Override
	@Transactional
	public PersonAccountSummaryDTO createAccount(Long personId, PersonCreateAccountRequest request) {
		PersonDO personDO = support.requirePerson(personId);
		if (personDO.getPersonStatus() != PersonStatus.NORMAL.getCode()) {
			throw new PersonnelManagementException(PersonnelManagementErrorCode.PERSON_STATUS_NOT_ALLOW_CREATE_ACCOUNT);
		}
		if (personAccountPort.getAccountBindingByPersonId(personId).isPresent()) {
			throw new PersonnelManagementException(PersonnelManagementErrorCode.PERSON_ACCOUNT_ALREADY_BOUND);
		}
		DepartmentDO departmentDO = support.requireDepartment(personDO.getDepartmentId());
		CreatePersonAccountCommand command = CreatePersonAccountCommand.builder()
				.personId(personId)
				.username(resolveUsername(personDO, request))
				.initialPassword(request.getInitialPassword())
				.autoGeneratePassword(request.getAutoGeneratePassword())
				.userType(mapUserType(personDO.getPersonType()).getCode())
				.realName(personDO.getPersonName())
				.genderType(personDO.getGenderType())
				.phone(personDO.getPhone())
				.email(personDO.getEmail())
				.studentNo(personDO.getStudentNo())
				.employeeNo(personDO.getEmployeeNo())
				.departmentName(departmentDO.getDepartmentName())
				.className(personDO.getClassName())
				.titleName(personDO.getTitleName())
				.build();
		AccountBindingDTO accountBindingDTO = personAccountPort.createAccount(command);
		return PersonnelManagementConverter.toPersonAccountSummaryDTO(accountBindingDTO);
	}

	@Override
	@Transactional
	public void disableAccount(Long personId, String reason) {
		personAccountPort.disableAccountByPersonId(personId, reason);
	}

	@Override
	public PersonAccountSummaryDTO getAccount(Long personId) {
		return personAccountPort.getAccountBindingByPersonId(personId)
				.map(PersonnelManagementConverter::toPersonAccountSummaryDTO)
				.orElse(null);
	}

	public void syncProfile(PersonDO personDO, DepartmentDO departmentDO, PositionDO positionDO) {
		if (personAccountPort.getAccountBindingByPersonId(personDO.getId()).isEmpty()) {
			return;
		}
		personAccountPort.syncProfileByPerson(PersonProfileSyncCommand.builder()
				.personId(personDO.getId())
				.realName(personDO.getPersonName())
				.genderType(personDO.getGenderType())
				.phone(personDO.getPhone())
				.email(personDO.getEmail())
				.studentNo(personDO.getStudentNo())
				.employeeNo(personDO.getEmployeeNo())
				.departmentName(departmentDO.getDepartmentName())
				.className(personDO.getClassName())
				.titleName(personDO.getTitleName())
				.build());
	}

	private String resolveUsername(PersonDO personDO, PersonCreateAccountRequest request) {
		if (StringUtils.hasText(request.getUsername())) {
			return request.getUsername().trim();
		}
		if (StringUtils.hasText(personDO.getStudentNo())) {
			return personDO.getStudentNo();
		}
		if (StringUtils.hasText(personDO.getEmployeeNo())) {
			return personDO.getEmployeeNo();
		}
		return personDO.getPersonNo();
	}

	private UserType mapUserType(Integer personType) {
		PersonType value = PersonType.fromCode(personType);
		return switch (value) {
			case STUDENT -> UserType.STUDENT;
			case TEACHER -> UserType.TEACHER;
			case ADMIN -> UserType.ADMIN;
		};
	}
}
