package com.zhangjintao.project.smart_lab_backend.modules.personnel_management.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.zhangjintao.project.smart_lab_backend.common.result.PageResult;
import com.zhangjintao.project.smart_lab_backend.modules.personnel_management.dao.dataobject.PositionDO;
import com.zhangjintao.project.smart_lab_backend.modules.personnel_management.dao.repository.PersonRepository;
import com.zhangjintao.project.smart_lab_backend.modules.personnel_management.dao.repository.PositionRepository;
import com.zhangjintao.project.smart_lab_backend.modules.personnel_management.exception.PersonnelManagementErrorCode;
import com.zhangjintao.project.smart_lab_backend.modules.personnel_management.exception.PersonnelManagementException;
import com.zhangjintao.project.smart_lab_backend.modules.personnel_management.model.converter.PersonnelManagementConverter;
import com.zhangjintao.project.smart_lab_backend.modules.personnel_management.model.request.PositionCreateRequest;
import com.zhangjintao.project.smart_lab_backend.modules.personnel_management.model.request.PositionUpdateRequest;
import com.zhangjintao.project.smart_lab_backend.modules.personnel_management.model.response.PositionResponse;
import com.zhangjintao.project.smart_lab_backend.modules.personnel_management.service.PositionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PositionServiceImpl implements PositionService {

	private final PositionRepository positionRepository;
	private final PersonRepository personRepository;
	private final PersonnelManagementSupport support;

	@Override
	@Transactional
	public PositionResponse create(PositionCreateRequest request) {
		support.validateDepartmentEnabled(support.requireDepartment(request.getDepartmentId()));
		validateUnique(request.getPositionNo(), request.getPositionCode(), null);
		PositionDO positionDO = new PositionDO();
		positionDO.setPositionNo(request.getPositionNo());
		positionDO.setPositionCode(request.getPositionCode());
		positionDO.setPositionName(request.getPositionName());
		positionDO.setPositionType(request.getPositionType());
		positionDO.setDepartmentId(request.getDepartmentId());
		positionDO.setPositionStatus(request.getPositionStatus());
		positionDO.setSortNo(request.getSortNo() == null ? 0 : request.getSortNo());
		positionDO.setDescription(request.getDescription());
		positionRepository.insert(positionDO);
		return PersonnelManagementConverter.toPositionResponse(positionDO);
	}

	@Override
	@Transactional
	public PositionResponse update(Long id, PositionUpdateRequest request) {
		PositionDO positionDO = requirePosition(id);
		support.validateDepartmentEnabled(support.requireDepartment(request.getDepartmentId()));
		positionDO.setPositionName(request.getPositionName());
		positionDO.setPositionType(request.getPositionType());
		positionDO.setDepartmentId(request.getDepartmentId());
		positionDO.setPositionStatus(request.getPositionStatus());
		positionDO.setSortNo(request.getSortNo() == null ? 0 : request.getSortNo());
		positionDO.setDescription(request.getDescription());
		positionRepository.update(positionDO);
		return PersonnelManagementConverter.toPositionResponse(positionDO);
	}

	@Override
	public PositionResponse getById(Long id) {
		return PersonnelManagementConverter.toPositionResponse(requirePosition(id));
	}

	@Override
	public PageResult<PositionResponse> page(Long pageNum, Long pageSize, Long departmentId, Integer positionStatus) {
		IPage<PositionDO> page = positionRepository.page(pageNum, pageSize, departmentId, positionStatus);
		return PageResult.of(page.getRecords().stream().map(PersonnelManagementConverter::toPositionResponse).toList(),
				page.getTotal(), pageNum, pageSize);
	}

	@Override
	@Transactional
	public void updateStatus(Long id, Integer positionStatus) {
		PositionDO positionDO = requirePosition(id);
		if (positionStatus == 2 && personRepository.countByPositionId(id) > 0) {
			throw new PersonnelManagementException(PersonnelManagementErrorCode.POSITION_IN_USE);
		}
		positionDO.setPositionStatus(positionStatus);
		positionRepository.update(positionDO);
	}

	private PositionDO requirePosition(Long id) {
		return positionRepository.findById(id)
				.orElseThrow(() -> new PersonnelManagementException(PersonnelManagementErrorCode.POSITION_NOT_FOUND));
	}

	private void validateUnique(String positionNo, String positionCode, Long excludeId) {
		if (positionRepository.existsByPositionNo(positionNo, excludeId)) {
			throw new PersonnelManagementException(PersonnelManagementErrorCode.PERSON_NO_DUPLICATE, "岗位编号重复");
		}
		if (positionRepository.existsByPositionCode(positionCode, excludeId)) {
			throw new PersonnelManagementException(PersonnelManagementErrorCode.PERSON_NO_DUPLICATE, "岗位编码重复");
		}
	}
}
