package com.zhangjintao.project.smart_lab_backend.modules.lab_management.dao.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.dao.dataobject.DetectionIndicatorDO;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.dao.mapper.DetectionIndicatorMapper;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.dao.repository.DetectionIndicatorRepository;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.domain.entity.DetectionIndicator;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.exception.LabManagementErrorCode;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.exception.LabManagementException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class DetectionIndicatorRepositoryImpl implements DetectionIndicatorRepository {

	private final DetectionIndicatorMapper detectionIndicatorMapper;
	private final ObjectMapper objectMapper;

	@Override
	public Optional<DetectionIndicator> findById(Long id) {
		if (id == null) {
			return Optional.empty();
		}
		return Optional.ofNullable(detectionIndicatorMapper.selectById(id))
				.map(indicatorDO -> LabPersistenceConverter.toDetectionIndicator(indicatorDO, objectMapper));
	}

	@Override
	public List<DetectionIndicator> listByLabTypeId(Long labTypeId) {
		return detectionIndicatorMapper.selectList(new LambdaQueryWrapper<DetectionIndicatorDO>()
						.eq(DetectionIndicatorDO::getLabTypeId, labTypeId)
						.orderByAsc(DetectionIndicatorDO::getId))
				.stream()
				.map(indicatorDO -> LabPersistenceConverter.toDetectionIndicator(indicatorDO, objectMapper))
				.toList();
	}

	@Override
	public boolean existsByLabTypeIdAndIndicatorName(Long labTypeId, String indicatorName, Long excludeId) {
		if (labTypeId == null || !StringUtils.hasText(indicatorName)) {
			return false;
		}
		LambdaQueryWrapper<DetectionIndicatorDO> wrapper = new LambdaQueryWrapper<DetectionIndicatorDO>()
				.eq(DetectionIndicatorDO::getLabTypeId, labTypeId)
				.eq(DetectionIndicatorDO::getIndicatorName, indicatorName);
		if (excludeId != null) {
			wrapper.ne(DetectionIndicatorDO::getId, excludeId);
		}
		return detectionIndicatorMapper.selectCount(wrapper) > 0;
	}

	@Override
	public DetectionIndicator save(DetectionIndicator indicator) {
		DetectionIndicatorDO indicatorDO = LabPersistenceConverter.toDetectionIndicatorDO(indicator, objectMapper);
		try {
			detectionIndicatorMapper.insert(indicatorDO);
		}
		catch (DuplicateKeyException ex) {
			throw duplicateNameException(indicator.getLabTypeId(), indicator.getIndicatorName());
		}
		return findById(indicatorDO.getId())
				.orElseGet(() -> LabPersistenceConverter.toDetectionIndicator(indicatorDO, objectMapper));
	}

	@Override
	public DetectionIndicator update(DetectionIndicator indicator) {
		try {
			detectionIndicatorMapper.updateById(LabPersistenceConverter.toDetectionIndicatorDO(indicator, objectMapper));
		}
		catch (DuplicateKeyException ex) {
			throw duplicateNameException(indicator.getLabTypeId(), indicator.getIndicatorName());
		}
		return findById(indicator.getId()).orElse(indicator);
	}

	@Override
	public void deleteById(Long id) {
		detectionIndicatorMapper.deleteById(id);
	}

	private LabManagementException duplicateNameException(Long labTypeId, String indicatorName) {
		return new LabManagementException(LabManagementErrorCode.DETECTION_INDICATOR_NAME_DUPLICATE,
				"同一实验室类型下检测指标名称重复，labTypeId=" + labTypeId + ", indicatorName=" + indicatorName);
	}
}
