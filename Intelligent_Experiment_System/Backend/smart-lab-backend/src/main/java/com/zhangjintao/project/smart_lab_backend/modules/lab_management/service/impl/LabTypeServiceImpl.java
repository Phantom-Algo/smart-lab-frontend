package com.zhangjintao.project.smart_lab_backend.modules.lab_management.service.impl;

import com.zhangjintao.project.smart_lab_backend.modules.lab_management.dao.repository.LabRepository;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.dao.repository.LabTypeRepository;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.domain.entity.LabType;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.exception.LabManagementErrorCode;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.exception.LabManagementException;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.converter.LabManagementConverter;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.dto.LabTypeCreateCommand;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.dto.LabTypeUpdateCommand;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.response.LabTypeResponse;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.service.LabTypeCacheManager;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.service.LabTypeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LabTypeServiceImpl implements LabTypeService {

	private final LabTypeRepository labTypeRepository;
	private final LabRepository labRepository;
	private final LabTypeCacheManager labTypeCacheManager;

	@Override
	@Transactional
	public LabTypeResponse create(LabTypeCreateCommand command) {
		String labTypeName = normalizeLabTypeName(command.getLabTypeName());
		validateUniqueName(labTypeName, null);
		LabType savedLabType = labTypeRepository.save(LabType.builder()
				.labTypeName(labTypeName)
				.build());
		evictLabTypeCacheAfterCommit();
		return LabManagementConverter.toLabTypeResponse(savedLabType);
	}

	@Override
	@Transactional
	public LabTypeResponse update(Long typeId, LabTypeUpdateCommand command) {
		LabType labType = requireLabType(typeId);
		String labTypeName = normalizeLabTypeName(command.getLabTypeName());
		validateUniqueName(labTypeName, typeId);
		labType.applyUpdate(LabTypeUpdateCommand.builder()
				.labTypeName(labTypeName)
				.build());
		LabType updatedLabType = labTypeRepository.update(labType);
		evictLabTypeCacheAfterCommit();
		return LabManagementConverter.toLabTypeResponse(updatedLabType);
	}

	@Override
	@Transactional
	public void delete(Long typeId) {
		requireLabType(typeId);
		if (labRepository.existsByLabTypeId(typeId)) {
			throw new LabManagementException(LabManagementErrorCode.LAB_TYPE_IN_USE,
					"实验室类型已被实验室引用，typeId=" + typeId);
		}
		labTypeRepository.deleteById(typeId);
		evictLabTypeCacheAfterCommit();
	}

	@Override
	public List<LabTypeResponse> listAll() {
		return labTypeCacheManager.listAll(labTypeRepository::listAll)
				.stream()
				.map(LabManagementConverter::toLabTypeResponse)
				.toList();
	}

	private LabType requireLabType(Long typeId) {
		return labTypeRepository.findById(typeId)
				.orElseThrow(() -> new LabManagementException(LabManagementErrorCode.LAB_TYPE_NOT_FOUND,
						"实验室类型不存在，typeId=" + typeId));
	}

	private String normalizeLabTypeName(String labTypeName) {
		if (!StringUtils.hasText(labTypeName)) {
			throw new LabManagementException(LabManagementErrorCode.LAB_PARAM_INVALID, "实验室类型名称不能为空");
		}
		String normalizedName = labTypeName.trim();
		if (normalizedName.length() > 64) {
			throw new LabManagementException(LabManagementErrorCode.LAB_PARAM_INVALID, "实验室类型名称长度不能超过64");
		}
		return normalizedName;
	}

	private void validateUniqueName(String labTypeName, Long excludeId) {
		if (labTypeRepository.existsByName(labTypeName, excludeId)) {
			throw new LabManagementException(LabManagementErrorCode.LAB_TYPE_NAME_DUPLICATE,
					"实验室类型名称重复，labTypeName=" + labTypeName);
		}
	}

	private void evictLabTypeCacheAfterCommit() {
		if (!TransactionSynchronizationManager.isSynchronizationActive()) {
			labTypeCacheManager.evictAll();
			return;
		}
		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCommit() {
				labTypeCacheManager.evictAll();
			}
		});
	}
}
