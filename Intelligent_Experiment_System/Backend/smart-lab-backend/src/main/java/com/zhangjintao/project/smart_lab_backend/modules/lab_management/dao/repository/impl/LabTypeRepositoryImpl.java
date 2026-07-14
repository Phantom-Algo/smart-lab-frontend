package com.zhangjintao.project.smart_lab_backend.modules.lab_management.dao.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.dao.dataobject.LabTypeDO;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.dao.mapper.LabTypeMapper;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.dao.repository.LabTypeRepository;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.domain.entity.LabType;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.exception.LabManagementErrorCode;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.exception.LabManagementException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class LabTypeRepositoryImpl implements LabTypeRepository {

	private final LabTypeMapper labTypeMapper;

	@Override
	public Optional<LabType> findById(Long id) {
		if (id == null) {
			return Optional.empty();
		}
		return Optional.ofNullable(labTypeMapper.selectById(id))
				.map(LabPersistenceConverter::toLabType);
	}

	@Override
	public List<LabType> listAll() {
		return labTypeMapper.selectList(new LambdaQueryWrapper<LabTypeDO>()
						.orderByAsc(LabTypeDO::getId))
				.stream()
				.map(LabPersistenceConverter::toLabType)
				.toList();
	}

	@Override
	public Map<Long, LabType> mapByIds(Collection<Long> ids) {
		if (ids == null || ids.isEmpty()) {
			return Map.of();
		}
		return labTypeMapper.selectList(new LambdaQueryWrapper<LabTypeDO>()
						.in(LabTypeDO::getId, ids))
				.stream()
				.map(LabPersistenceConverter::toLabType)
				.collect(Collectors.toMap(LabType::getId, Function.identity()));
	}

	@Override
	public boolean existsByName(String labTypeName, Long excludeId) {
		if (!StringUtils.hasText(labTypeName)) {
			return false;
		}
		LambdaQueryWrapper<LabTypeDO> wrapper = new LambdaQueryWrapper<LabTypeDO>()
				.eq(LabTypeDO::getLabTypeName, labTypeName);
		if (excludeId != null) {
			wrapper.ne(LabTypeDO::getId, excludeId);
		}
		return labTypeMapper.selectCount(wrapper) > 0;
	}

	@Override
	public LabType save(LabType labType) {
		LabTypeDO labTypeDO = LabPersistenceConverter.toLabTypeDO(labType);
		try {
			labTypeMapper.insert(labTypeDO);
		}
		catch (DuplicateKeyException ex) {
			throw duplicateNameException(labType.getLabTypeName());
		}
		return findById(labTypeDO.getId()).orElseGet(() -> LabPersistenceConverter.toLabType(labTypeDO));
	}

	@Override
	public LabType update(LabType labType) {
		try {
			labTypeMapper.updateById(LabPersistenceConverter.toLabTypeDO(labType));
		}
		catch (DuplicateKeyException ex) {
			throw duplicateNameException(labType.getLabTypeName());
		}
		return findById(labType.getId()).orElse(labType);
	}

	@Override
	public void deleteById(Long id) {
		labTypeMapper.deleteById(id);
	}

	private LabManagementException duplicateNameException(String labTypeName) {
		return new LabManagementException(LabManagementErrorCode.LAB_TYPE_NAME_DUPLICATE,
				"实验室类型名称重复，labTypeName=" + labTypeName);
	}
}
