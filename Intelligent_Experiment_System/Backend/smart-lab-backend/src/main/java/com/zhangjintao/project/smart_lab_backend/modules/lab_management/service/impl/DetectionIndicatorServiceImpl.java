package com.zhangjintao.project.smart_lab_backend.modules.lab_management.service.impl;

import com.zhangjintao.project.smart_lab_backend.modules.lab_management.dao.repository.DetectionIndicatorRepository;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.dao.repository.LabTypeRepository;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.domain.entity.DetectionIndicator;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.domain.value.DetectionSafeRange;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.domain.value.DetectionSafeRangeSegment;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.exception.LabManagementErrorCode;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.exception.LabManagementException;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.converter.LabManagementConverter;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.dto.DetectionIndicatorCreateCommand;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.dto.DetectionIndicatorUpdateCommand;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.response.DetectionIndicatorResponse;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.service.DetectionIndicatorCacheManager;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.service.DetectionIndicatorService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class DetectionIndicatorServiceImpl implements DetectionIndicatorService {

	private final DetectionIndicatorRepository detectionIndicatorRepository;
	private final LabTypeRepository labTypeRepository;
	private final DetectionIndicatorCacheManager detectionIndicatorCacheManager;

	@Override
	@Transactional
	public DetectionIndicatorResponse create(DetectionIndicatorCreateCommand command) {
		Long labTypeId = command.getLabTypeId();
		requireLabType(labTypeId);
		String indicatorName = normalizeIndicatorName(command.getIndicatorName());
		String unit = normalizeUnit(command.getUnit());
		DetectionSafeRange safeRange = buildSafeRange(command.getSafeRange());
		validateUniqueName(labTypeId, indicatorName, null);
		DetectionIndicator savedIndicator = detectionIndicatorRepository.save(DetectionIndicator.builder()
				.labTypeId(labTypeId)
				.indicatorName(indicatorName)
				.unit(unit)
				.safeRange(safeRange)
				.build());
		evictIndicatorCacheAfterCommit(List.of(labTypeId));
		return LabManagementConverter.toDetectionIndicatorResponse(savedIndicator);
	}

	@Override
	@Transactional
	public DetectionIndicatorResponse update(Long indicatorId, DetectionIndicatorUpdateCommand command) {
		DetectionIndicator indicator = requireIndicator(indicatorId);
		Long oldLabTypeId = indicator.getLabTypeId();
		Long newLabTypeId = command.getLabTypeId() == null ? oldLabTypeId : command.getLabTypeId();
		requireLabType(newLabTypeId);
		String newIndicatorName = command.getIndicatorName() == null
				? indicator.getIndicatorName()
				: normalizeIndicatorName(command.getIndicatorName());
		String newUnit = command.getUnit() == null ? indicator.getUnit() : normalizeUnit(command.getUnit());
		DetectionSafeRange newSafeRange = command.getSafeRange() == null
				? indicator.getSafeRange()
				: buildSafeRange(command.getSafeRange());
		validateUniqueName(newLabTypeId, newIndicatorName, indicatorId);

		indicator.applyUpdate(newLabTypeId, newIndicatorName, newUnit, newSafeRange);
		DetectionIndicator updatedIndicator = detectionIndicatorRepository.update(indicator);
		evictIndicatorCacheAfterCommit(List.of(oldLabTypeId, newLabTypeId));
		return LabManagementConverter.toDetectionIndicatorResponse(updatedIndicator);
	}

	@Override
	@Transactional
	public void delete(Long indicatorId) {
		DetectionIndicator indicator = requireIndicator(indicatorId);
		detectionIndicatorRepository.deleteById(indicatorId);
		evictIndicatorCacheAfterCommit(List.of(indicator.getLabTypeId()));
	}

	@Override
	public List<DetectionIndicatorResponse> listByLabTypeId(Long labTypeId) {
		requireLabType(labTypeId);
		return detectionIndicatorCacheManager.listByLabTypeId(labTypeId,
						() -> detectionIndicatorRepository.listByLabTypeId(labTypeId))
				.stream()
				.map(LabManagementConverter::toDetectionIndicatorResponse)
				.toList();
	}

	private DetectionIndicator requireIndicator(Long indicatorId) {
		return detectionIndicatorRepository.findById(indicatorId)
				.orElseThrow(() -> new LabManagementException(LabManagementErrorCode.DETECTION_INDICATOR_NOT_FOUND,
						"检测指标不存在，indicatorId=" + indicatorId));
	}

	private void requireLabType(Long labTypeId) {
		if (labTypeId == null) {
			throw new LabManagementException(LabManagementErrorCode.LAB_PARAM_INVALID, "实验室类型ID不能为空");
		}
		labTypeRepository.findById(labTypeId)
				.orElseThrow(() -> new LabManagementException(LabManagementErrorCode.LAB_TYPE_NOT_FOUND,
						"实验室类型不存在，labTypeId=" + labTypeId));
	}

	private String normalizeIndicatorName(String indicatorName) {
		if (!StringUtils.hasText(indicatorName)) {
			throw new LabManagementException(LabManagementErrorCode.LAB_PARAM_INVALID, "指标名称不能为空");
		}
		String normalizedName = indicatorName.trim();
		if (normalizedName.length() > 64) {
			throw new LabManagementException(LabManagementErrorCode.LAB_PARAM_INVALID, "指标名称长度不能超过64");
		}
		return normalizedName;
	}

	private String normalizeUnit(String unit) {
		if (!StringUtils.hasText(unit)) {
			throw new LabManagementException(LabManagementErrorCode.LAB_PARAM_INVALID, "指标单位不能为空");
		}
		String normalizedUnit = unit.trim();
		if (normalizedUnit.length() > 16) {
			throw new LabManagementException(LabManagementErrorCode.LAB_PARAM_INVALID, "指标单位长度不能超过16");
		}
		return normalizedUnit;
	}

	private DetectionSafeRange buildSafeRange(List<List<BigDecimal>> rawSafeRange) {
		if (rawSafeRange == null || rawSafeRange.isEmpty()) {
			throw invalidSafeRange("安全区间不能为空");
		}
		return DetectionSafeRange.builder()
				.segments(rawSafeRange.stream()
						.map(this::buildSafeRangeSegment)
						.toList())
				.build();
	}

	private DetectionSafeRangeSegment buildSafeRangeSegment(List<BigDecimal> rawSegment) {
		if (rawSegment == null || rawSegment.size() != 2) {
			throw invalidSafeRange("安全区间每段必须是长度为2的数组");
		}
		BigDecimal min = rawSegment.get(0);
		BigDecimal max = rawSegment.get(1);
		if (min == null && max == null) {
			throw invalidSafeRange("安全区间每段至少需要一个边界");
		}
		if (min != null && max != null && min.compareTo(max) > 0) {
			throw invalidSafeRange("安全区间下限不能大于上限");
		}
		return DetectionSafeRangeSegment.builder()
				.min(min)
				.max(max)
				.build();
	}

	private LabManagementException invalidSafeRange(String message) {
		return new LabManagementException(LabManagementErrorCode.DETECTION_SAFE_RANGE_INVALID, message);
	}

	private void validateUniqueName(Long labTypeId, String indicatorName, Long excludeId) {
		if (detectionIndicatorRepository.existsByLabTypeIdAndIndicatorName(labTypeId, indicatorName, excludeId)) {
			throw new LabManagementException(LabManagementErrorCode.DETECTION_INDICATOR_NAME_DUPLICATE,
					"同一实验室类型下检测指标名称重复，labTypeId=" + labTypeId + ", indicatorName=" + indicatorName);
		}
	}

	private void evictIndicatorCacheAfterCommit(Collection<Long> labTypeIds) {
		Set<Long> distinctLabTypeIds = new LinkedHashSet<>(labTypeIds);
		if (!TransactionSynchronizationManager.isSynchronizationActive()) {
			distinctLabTypeIds.forEach(detectionIndicatorCacheManager::evictByLabTypeId);
			return;
		}
		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCommit() {
				distinctLabTypeIds.forEach(detectionIndicatorCacheManager::evictByLabTypeId);
			}
		});
	}
}
