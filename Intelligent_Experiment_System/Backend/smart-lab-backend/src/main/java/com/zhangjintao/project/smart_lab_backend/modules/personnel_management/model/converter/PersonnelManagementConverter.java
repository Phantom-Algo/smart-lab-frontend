package com.zhangjintao.project.smart_lab_backend.modules.personnel_management.model.converter;

import com.zhangjintao.project.smart_lab_backend.modules.auth_profile.model.dto.AccountBindingDTO;
import com.zhangjintao.project.smart_lab_backend.modules.personnel_management.dao.dataobject.DepartmentDO;
import com.zhangjintao.project.smart_lab_backend.modules.personnel_management.dao.dataobject.PersonDO;
import com.zhangjintao.project.smart_lab_backend.modules.personnel_management.dao.dataobject.PositionDO;
import com.zhangjintao.project.smart_lab_backend.modules.personnel_management.model.dto.PersonAccountSummaryDTO;
import com.zhangjintao.project.smart_lab_backend.modules.personnel_management.model.dto.PersonDirectoryItemDTO;
import com.zhangjintao.project.smart_lab_backend.modules.personnel_management.model.response.DepartmentResponse;
import com.zhangjintao.project.smart_lab_backend.modules.personnel_management.model.response.DepartmentTreeNodeResponse;
import com.zhangjintao.project.smart_lab_backend.modules.personnel_management.model.response.PersonDetailResponse;
import com.zhangjintao.project.smart_lab_backend.modules.personnel_management.model.response.PositionResponse;

import java.util.List;

public final class PersonnelManagementConverter {

	private PersonnelManagementConverter() {
	}

	public static DepartmentResponse toDepartmentResponse(DepartmentDO departmentDO) {
		return DepartmentResponse.builder()
				.id(departmentDO.getId())
				.departmentNo(departmentDO.getDepartmentNo())
				.departmentCode(departmentDO.getDepartmentCode())
				.departmentName(departmentDO.getDepartmentName())
				.parentId(departmentDO.getParentId())
				.leaderPersonId(departmentDO.getLeaderPersonId())
				.departmentStatus(departmentDO.getDepartmentStatus())
				.sortNo(departmentDO.getSortNo())
				.description(departmentDO.getDescription())
				.build();
	}

	public static DepartmentTreeNodeResponse toDepartmentTreeNodeResponse(DepartmentDO departmentDO,
			List<DepartmentTreeNodeResponse> children) {
		return DepartmentTreeNodeResponse.builder()
				.id(departmentDO.getId())
				.departmentNo(departmentDO.getDepartmentNo())
				.departmentName(departmentDO.getDepartmentName())
				.departmentCode(departmentDO.getDepartmentCode())
				.departmentStatus(departmentDO.getDepartmentStatus())
				.sortNo(departmentDO.getSortNo())
				.leaderPersonName(null)
				.children(children)
				.build();
	}

	public static PositionResponse toPositionResponse(PositionDO positionDO) {
		return PositionResponse.builder()
				.id(positionDO.getId())
				.positionNo(positionDO.getPositionNo())
				.positionCode(positionDO.getPositionCode())
				.positionName(positionDO.getPositionName())
				.positionType(positionDO.getPositionType())
				.departmentId(positionDO.getDepartmentId())
				.positionStatus(positionDO.getPositionStatus())
				.sortNo(positionDO.getSortNo())
				.description(positionDO.getDescription())
				.build();
	}

	public static PersonAccountSummaryDTO toPersonAccountSummaryDTO(AccountBindingDTO accountBindingDTO) {
		if (accountBindingDTO == null) {
			return null;
		}
		return PersonAccountSummaryDTO.builder()
				.userId(accountBindingDTO.getUserId())
				.username(accountBindingDTO.getUsername())
				.userType(accountBindingDTO.getUserType())
				.accountStatus(accountBindingDTO.getAccountStatus())
				.passwordResetRequired(accountBindingDTO.getPasswordResetRequired())
				.temporaryPassword(accountBindingDTO.getTemporaryPassword())
				.build();
	}

	public static PersonDetailResponse toPersonDetailResponse(PersonDO personDO, DepartmentDO departmentDO, PositionDO positionDO,
			PersonAccountSummaryDTO accountSummaryDTO) {
		return PersonDetailResponse.builder()
				.id(personDO.getId())
				.personNo(personDO.getPersonNo())
				.personName(personDO.getPersonName())
				.personType(personDO.getPersonType())
				.genderType(personDO.getGenderType())
				.phone(personDO.getPhone())
				.email(personDO.getEmail())
				.idCardNoMasked(personDO.getIdCardNoMasked())
				.studentNo(personDO.getStudentNo())
				.employeeNo(personDO.getEmployeeNo())
				.departmentId(personDO.getDepartmentId())
				.departmentName(departmentDO == null ? null : departmentDO.getDepartmentName())
				.positionId(personDO.getPositionId())
				.positionName(positionDO == null ? null : positionDO.getPositionName())
				.className(personDO.getClassName())
				.titleName(personDO.getTitleName())
				.personStatus(personDO.getPersonStatus())
				.entryAt(personDO.getEntryAt())
				.leaveAt(personDO.getLeaveAt())
				.sourceType(personDO.getSourceType())
				.description(personDO.getDescription())
				.account(accountSummaryDTO)
				.build();
	}

	public static PersonDirectoryItemDTO toPersonDirectoryItemDTO(PersonDO personDO, DepartmentDO departmentDO, PositionDO positionDO,
			AccountBindingDTO accountBindingDTO) {
		return PersonDirectoryItemDTO.builder()
				.personId(personDO.getId())
				.personNo(personDO.getPersonNo())
				.personName(personDO.getPersonName())
				.personType(personDO.getPersonType())
				.genderType(personDO.getGenderType())
				.phone(personDO.getPhone())
				.email(personDO.getEmail())
				.studentNo(personDO.getStudentNo())
				.employeeNo(personDO.getEmployeeNo())
				.departmentId(personDO.getDepartmentId())
				.departmentName(departmentDO == null ? null : departmentDO.getDepartmentName())
				.positionId(personDO.getPositionId())
				.positionName(positionDO == null ? null : positionDO.getPositionName())
				.className(personDO.getClassName())
				.titleName(personDO.getTitleName())
				.personStatus(personDO.getPersonStatus())
				.userId(accountBindingDTO == null ? null : accountBindingDTO.getUserId())
				.accountStatus(accountBindingDTO == null ? null : accountBindingDTO.getAccountStatus())
				.username(accountBindingDTO == null ? null : accountBindingDTO.getUsername())
				.build();
	}
}
