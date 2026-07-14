package com.zhangjintao.project.smart_lab_backend.modules.personnel_management.service.impl;

import com.zhangjintao.project.smart_lab_backend.common.result.PageResult;
import com.zhangjintao.project.smart_lab_backend.modules.personnel_management.dao.dataobject.DepartmentDO;
import com.zhangjintao.project.smart_lab_backend.modules.personnel_management.dao.dataobject.PersonDO;
import com.zhangjintao.project.smart_lab_backend.modules.personnel_management.dao.dataobject.PositionDO;
import com.zhangjintao.project.smart_lab_backend.modules.personnel_management.dao.repository.PersonRepository;
import com.zhangjintao.project.smart_lab_backend.modules.personnel_management.enums.PersonStatus;
import com.zhangjintao.project.smart_lab_backend.modules.personnel_management.exception.PersonnelManagementErrorCode;
import com.zhangjintao.project.smart_lab_backend.modules.personnel_management.exception.PersonnelManagementException;
import com.zhangjintao.project.smart_lab_backend.modules.personnel_management.model.converter.PersonnelManagementConverter;
import com.zhangjintao.project.smart_lab_backend.modules.personnel_management.model.dto.PersonDirectoryQuery;
import com.zhangjintao.project.smart_lab_backend.modules.personnel_management.model.request.PersonCreateRequest;
import com.zhangjintao.project.smart_lab_backend.modules.personnel_management.model.request.PersonQueryRequest;
import com.zhangjintao.project.smart_lab_backend.modules.personnel_management.model.request.PersonStatusChangeRequest;
import com.zhangjintao.project.smart_lab_backend.modules.personnel_management.model.request.PersonUpdateRequest;
import com.zhangjintao.project.smart_lab_backend.modules.personnel_management.model.response.PersonDetailResponse;
import com.zhangjintao.project.smart_lab_backend.modules.personnel_management.service.PersonDirectoryPort;
import com.zhangjintao.project.smart_lab_backend.modules.personnel_management.service.PersonService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PersonServiceImpl implements PersonService {

	private final PersonRepository personRepository;
	private final PersonnelManagementSupport support;
	private final PersonAccountOrchestratorImpl personAccountOrchestrator;
	private final PersonDirectoryPort personDirectoryPort;

	@Override
	@Transactional
	public PersonDetailResponse create(PersonCreateRequest request) {
		validateUnique(request.getPersonNo(), request.getStudentNo(), request.getEmployeeNo(), null);
		DepartmentDO departmentDO = support.requireDepartment(request.getDepartmentId());
		support.validateDepartmentEnabled(departmentDO);
		PositionDO positionDO = request.getPositionId() == null ? null : support.requirePosition(request.getPositionId());
		support.validatePositionAssignable(request.getPersonType(), positionDO, request.getDepartmentId());
		PersonDO personDO = new PersonDO();
		fillPerson(personDO, request);
		support.validatePersonIdentity(personDO);
		personRepository.insert(personDO);
		return buildDetail(personDO);
	}

	@Override
	@Transactional
	public PersonDetailResponse update(Long id, PersonUpdateRequest request) {
		PersonDO personDO = support.requirePerson(id);
		validateUnique(personDO.getPersonNo(), request.getStudentNo(), request.getEmployeeNo(), id);
		DepartmentDO departmentDO = support.requireDepartment(request.getDepartmentId());
		support.validateDepartmentEnabled(departmentDO);
		PositionDO positionDO = request.getPositionId() == null ? null : support.requirePosition(request.getPositionId());
		support.validatePositionAssignable(request.getPersonType(), positionDO, request.getDepartmentId());
		fillPerson(personDO, request);
		support.validatePersonIdentity(personDO);
		personRepository.update(personDO);
		personAccountOrchestrator.syncProfile(personDO, departmentDO, positionDO);
		if (personDO.getPersonStatus() != PersonStatus.NORMAL.getCode()) {
			personAccountOrchestrator.disableAccount(personDO.getId(), request.getDescription());
		}
		return buildDetail(personDO);
	}

	@Override
	public PersonDetailResponse getById(Long id) {
		return buildDetail(support.requirePerson(id));
	}

	@Override
	public PageResult<PersonDetailResponse> page(PersonQueryRequest request) {
		PersonDirectoryQuery query = toDirectoryQuery(request);
		PageResult<com.zhangjintao.project.smart_lab_backend.modules.personnel_management.model.dto.PersonDirectoryItemDTO> page =
				personDirectoryPort.page(query);
		return PageResult.of(page.getRecords().stream()
				.map(item -> buildDetail(support.requirePerson(item.getPersonId())))
				.toList(), page.getTotal(), page.getPageNum(), page.getPageSize());
	}

	@Override
	@Transactional
	public void updateStatus(Long id, PersonStatusChangeRequest request) {
		PersonDO personDO = support.requirePerson(id);
		personDO.setPersonStatus(request.getPersonStatus());
		personDO.setDescription(request.getDescription());
		personRepository.update(personDO);
		if (request.getPersonStatus() != PersonStatus.NORMAL.getCode()) {
			personAccountOrchestrator.disableAccount(id, request.getDescription());
		}
	}

	private PersonDetailResponse buildDetail(PersonDO personDO) {
		DepartmentDO departmentDO = support.requireDepartment(personDO.getDepartmentId());
		PositionDO positionDO = personDO.getPositionId() == null ? null : support.requirePosition(personDO.getPositionId());
		return PersonnelManagementConverter.toPersonDetailResponse(personDO, departmentDO, positionDO,
				personAccountOrchestrator.getAccount(personDO.getId()));
	}

	private void validateUnique(String personNo, String studentNo, String employeeNo, Long excludeId) {
		if (personRepository.existsByPersonNo(personNo, excludeId)) {
			throw new PersonnelManagementException(PersonnelManagementErrorCode.PERSON_NO_DUPLICATE);
		}
		if (personRepository.existsByStudentNo(studentNo, excludeId)) {
			throw new PersonnelManagementException(PersonnelManagementErrorCode.STUDENT_NO_DUPLICATE);
		}
		if (personRepository.existsByEmployeeNo(employeeNo, excludeId)) {
			throw new PersonnelManagementException(PersonnelManagementErrorCode.EMPLOYEE_NO_DUPLICATE);
		}
	}

	private void fillPerson(PersonDO personDO, PersonCreateRequest request) {
		personDO.setPersonNo(request.getPersonNo());
		fillPersonCommon(personDO, request.getPersonName(), request.getPersonType(), request.getGenderType(), request.getPhone(),
				request.getEmail(), request.getIdCardNoMasked(), request.getStudentNo(), request.getEmployeeNo(),
				request.getDepartmentId(), request.getPositionId(), request.getClassName(), request.getTitleName(),
				request.getPersonStatus(), request.getEntryAt(), request.getLeaveAt(), request.getDescription());
		personDO.setSourceType(request.getSourceType());
	}

	private void fillPerson(PersonDO personDO, PersonUpdateRequest request) {
		fillPersonCommon(personDO, request.getPersonName(), request.getPersonType(), request.getGenderType(), request.getPhone(),
				request.getEmail(), request.getIdCardNoMasked(), request.getStudentNo(), request.getEmployeeNo(),
				request.getDepartmentId(), request.getPositionId(), request.getClassName(), request.getTitleName(),
				request.getPersonStatus(), request.getEntryAt(), request.getLeaveAt(), request.getDescription());
	}

	private void fillPersonCommon(PersonDO personDO, String personName, Integer personType, Integer genderType, String phone, String email,
			String idCardNoMasked, String studentNo, String employeeNo, Long departmentId, Long positionId, String className,
			String titleName, Integer personStatus, java.time.LocalDateTime entryAt, java.time.LocalDateTime leaveAt, String description) {
		personDO.setPersonName(personName);
		personDO.setPersonType(personType);
		personDO.setGenderType(genderType);
		personDO.setPhone(phone);
		personDO.setEmail(email);
		personDO.setIdCardNoMasked(idCardNoMasked);
		personDO.setStudentNo(studentNo);
		personDO.setEmployeeNo(employeeNo);
		personDO.setDepartmentId(departmentId);
		personDO.setPositionId(positionId);
		personDO.setClassName(className);
		personDO.setTitleName(titleName);
		personDO.setPersonStatus(personStatus);
		personDO.setEntryAt(entryAt);
		personDO.setLeaveAt(leaveAt);
		personDO.setDescription(description);
	}

	private PersonDirectoryQuery toDirectoryQuery(PersonQueryRequest request) {
		return PersonDirectoryQuery.builder()
				.personTypes(request.getPersonTypes())
				.departmentId(request.getDepartmentId())
				.positionId(request.getPositionId())
				.personStatuses(request.getPersonStatuses())
				.keyword(request.getKeyword())
				.hasAccount(request.getHasAccount())
				.onlyEnabledDepartment(request.getOnlyEnabledDepartment())
				.pageNum(request.getPageNum())
				.pageSize(request.getPageSize())
				.build();
	}
}
