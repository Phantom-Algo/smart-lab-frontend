package com.zhangjintao.project.smart_lab_backend.modules.personnel_management.service.impl;

import com.zhangjintao.project.smart_lab_backend.modules.personnel_management.dao.dataobject.DepartmentDO;
import com.zhangjintao.project.smart_lab_backend.modules.personnel_management.dao.repository.DepartmentRepository;
import com.zhangjintao.project.smart_lab_backend.modules.personnel_management.dao.repository.PersonRepository;
import com.zhangjintao.project.smart_lab_backend.modules.personnel_management.dao.repository.PositionRepository;
import com.zhangjintao.project.smart_lab_backend.modules.personnel_management.exception.PersonnelManagementErrorCode;
import com.zhangjintao.project.smart_lab_backend.modules.personnel_management.exception.PersonnelManagementException;
import com.zhangjintao.project.smart_lab_backend.modules.personnel_management.model.converter.PersonnelManagementConverter;
import com.zhangjintao.project.smart_lab_backend.modules.personnel_management.model.request.DepartmentCreateRequest;
import com.zhangjintao.project.smart_lab_backend.modules.personnel_management.model.request.DepartmentUpdateRequest;
import com.zhangjintao.project.smart_lab_backend.modules.personnel_management.model.response.DepartmentResponse;
import com.zhangjintao.project.smart_lab_backend.modules.personnel_management.model.response.DepartmentTreeNodeResponse;
import com.zhangjintao.project.smart_lab_backend.modules.personnel_management.service.DepartmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DepartmentServiceImpl implements DepartmentService {

	private final DepartmentRepository departmentRepository;
	private final PositionRepository positionRepository;
	private final PersonRepository personRepository;

	@Override
	@Transactional
	public DepartmentResponse create(DepartmentCreateRequest request) {
		validateUnique(request.getDepartmentNo(), request.getDepartmentCode(), null);
		validateParent(request.getParentId(), null);
		DepartmentDO departmentDO = new DepartmentDO();
		departmentDO.setDepartmentNo(request.getDepartmentNo());
		departmentDO.setDepartmentCode(request.getDepartmentCode());
		departmentDO.setDepartmentName(request.getDepartmentName());
		departmentDO.setParentId(request.getParentId());
		departmentDO.setLeaderPersonId(request.getLeaderPersonId());
		departmentDO.setDepartmentStatus(request.getDepartmentStatus());
		departmentDO.setSortNo(request.getSortNo() == null ? 0 : request.getSortNo());
		departmentDO.setDescription(request.getDescription());
		departmentRepository.insert(departmentDO);
		return PersonnelManagementConverter.toDepartmentResponse(departmentDO);
	}

	@Override
	@Transactional
	public DepartmentResponse update(Long id, DepartmentUpdateRequest request) {
		DepartmentDO departmentDO = requireDepartment(id);
		validateParent(request.getParentId(), id);
		departmentDO.setDepartmentName(request.getDepartmentName());
		departmentDO.setParentId(request.getParentId());
		departmentDO.setLeaderPersonId(request.getLeaderPersonId());
		departmentDO.setDepartmentStatus(request.getDepartmentStatus());
		departmentDO.setSortNo(request.getSortNo() == null ? 0 : request.getSortNo());
		departmentDO.setDescription(request.getDescription());
		departmentRepository.update(departmentDO);
		return PersonnelManagementConverter.toDepartmentResponse(departmentDO);
	}

	@Override
	public DepartmentResponse getById(Long id) {
		return PersonnelManagementConverter.toDepartmentResponse(requireDepartment(id));
	}

	@Override
	public List<DepartmentTreeNodeResponse> getTree() {
		List<DepartmentDO> allDepartments = departmentRepository.listAll();
		return buildTree(null, allDepartments);
	}

	@Override
	@Transactional
	public void updateStatus(Long id, Integer departmentStatus) {
		DepartmentDO departmentDO = requireDepartment(id);
		if (departmentStatus == 2 && (departmentRepository.hasChildren(id)
				|| positionRepository.countByDepartmentId(id) > 0
				|| personRepository.countByDepartmentId(id) > 0)) {
			throw new PersonnelManagementException(PersonnelManagementErrorCode.DEPARTMENT_IN_USE);
		}
		departmentDO.setDepartmentStatus(departmentStatus);
		departmentRepository.update(departmentDO);
	}

	private DepartmentDO requireDepartment(Long id) {
		return departmentRepository.findById(id)
				.orElseThrow(() -> new PersonnelManagementException(PersonnelManagementErrorCode.DEPARTMENT_NOT_FOUND));
	}

	private void validateUnique(String departmentNo, String departmentCode, Long excludeId) {
		if (departmentRepository.existsByDepartmentNo(departmentNo, excludeId)) {
			throw new PersonnelManagementException(PersonnelManagementErrorCode.PERSON_NO_DUPLICATE, "部门编号重复");
		}
		if (departmentRepository.existsByDepartmentCode(departmentCode, excludeId)) {
			throw new PersonnelManagementException(PersonnelManagementErrorCode.PERSON_NO_DUPLICATE, "部门编码重复");
		}
	}

	private void validateParent(Long parentId, Long currentId) {
		if (parentId == null) {
			return;
		}
		if (currentId != null && currentId.equals(parentId)) {
			throw new PersonnelManagementException(PersonnelManagementErrorCode.INVALID_PARENT_DEPARTMENT);
		}
		requireDepartment(parentId);
	}

	private List<DepartmentTreeNodeResponse> buildTree(Long parentId, List<DepartmentDO> allDepartments) {
		return allDepartments.stream()
				.filter(item -> parentId == null ? item.getParentId() == null : parentId.equals(item.getParentId()))
				.map(item -> PersonnelManagementConverter.toDepartmentTreeNodeResponse(item, buildTree(item.getId(), allDepartments)))
				.toList();
	}
}
