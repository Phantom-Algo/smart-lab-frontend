package com.zhangjintao.project.smart_lab_backend.modules.personnel_management.service.impl;

import com.zhangjintao.project.smart_lab_backend.modules.personnel_management.dao.dataobject.DepartmentDO;
import com.zhangjintao.project.smart_lab_backend.modules.personnel_management.dao.dataobject.PersonDO;
import com.zhangjintao.project.smart_lab_backend.modules.personnel_management.dao.dataobject.PositionDO;
import com.zhangjintao.project.smart_lab_backend.modules.personnel_management.dao.repository.DepartmentRepository;
import com.zhangjintao.project.smart_lab_backend.modules.personnel_management.dao.repository.PersonRepository;
import com.zhangjintao.project.smart_lab_backend.modules.personnel_management.dao.repository.PositionRepository;
import com.zhangjintao.project.smart_lab_backend.modules.personnel_management.enums.DepartmentStatus;
import com.zhangjintao.project.smart_lab_backend.modules.personnel_management.enums.PersonType;
import com.zhangjintao.project.smart_lab_backend.modules.personnel_management.enums.PositionType;
import com.zhangjintao.project.smart_lab_backend.modules.personnel_management.exception.PersonnelManagementErrorCode;
import com.zhangjintao.project.smart_lab_backend.modules.personnel_management.exception.PersonnelManagementException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class PersonnelManagementSupport {

	private final DepartmentRepository departmentRepository;
	private final PositionRepository positionRepository;
	private final PersonRepository personRepository;

	public DepartmentDO requireDepartment(Long departmentId) {
		return departmentRepository.findById(departmentId)
				.orElseThrow(() -> new PersonnelManagementException(PersonnelManagementErrorCode.DEPARTMENT_NOT_FOUND));
	}

	public PositionDO requirePosition(Long positionId) {
		return positionRepository.findById(positionId)
				.orElseThrow(() -> new PersonnelManagementException(PersonnelManagementErrorCode.POSITION_NOT_FOUND));
	}

	public PersonDO requirePerson(Long personId) {
		return personRepository.findById(personId)
				.orElseThrow(() -> new PersonnelManagementException(PersonnelManagementErrorCode.PERSON_NOT_FOUND));
	}

	public void validatePersonIdentity(PersonDO personDO) {
		PersonType personType = PersonType.fromCode(personDO.getPersonType());
		if (personType == PersonType.STUDENT && !StringUtils.hasText(personDO.getStudentNo())) {
			throw new PersonnelManagementException(PersonnelManagementErrorCode.PERSON_REQUIRED_FIELD_MISSING, "学生必须填写学号");
		}
		if (personType != PersonType.STUDENT && !StringUtils.hasText(personDO.getEmployeeNo())) {
			throw new PersonnelManagementException(PersonnelManagementErrorCode.PERSON_REQUIRED_FIELD_MISSING, "教师或管理员必须填写工号");
		}
	}

	public void validatePositionAssignable(Integer personTypeCode, PositionDO positionDO, Long departmentId) {
		if (positionDO == null) {
			return;
		}
		if (!positionDO.getDepartmentId().equals(departmentId)) {
			throw new PersonnelManagementException(PersonnelManagementErrorCode.POSITION_TYPE_MISMATCH, "岗位不属于指定部门");
		}
		PersonType personType = PersonType.fromCode(personTypeCode);
		if (personType == PersonType.STUDENT && positionDO.getPositionType() != PositionType.STUDENT.getCode()) {
			throw new PersonnelManagementException(PersonnelManagementErrorCode.POSITION_TYPE_MISMATCH);
		}
		if (personType == PersonType.TEACHER && positionDO.getPositionType() == PositionType.STUDENT.getCode()) {
			throw new PersonnelManagementException(PersonnelManagementErrorCode.POSITION_TYPE_MISMATCH);
		}
		if (personType == PersonType.ADMIN && positionDO.getPositionType() == PositionType.STUDENT.getCode()) {
			throw new PersonnelManagementException(PersonnelManagementErrorCode.POSITION_TYPE_MISMATCH);
		}
	}

	public void validateDepartmentEnabled(DepartmentDO departmentDO) {
		if (departmentDO.getDepartmentStatus() != DepartmentStatus.ENABLED.getCode()) {
			throw new PersonnelManagementException(PersonnelManagementErrorCode.DEPARTMENT_IN_USE, "部门未启用");
		}
	}
}
