package com.zhangjintao.project.smart_lab_backend.modules.lab_management.dao.repository;

import com.zhangjintao.project.smart_lab_backend.modules.lab_management.domain.entity.DetectionIndicator;

import java.util.List;
import java.util.Optional;

public interface DetectionIndicatorRepository {

	Optional<DetectionIndicator> findById(Long id);

	List<DetectionIndicator> listByLabTypeId(Long labTypeId);

	boolean existsByLabTypeIdAndIndicatorName(Long labTypeId, String indicatorName, Long excludeId);

	DetectionIndicator save(DetectionIndicator indicator);

	DetectionIndicator update(DetectionIndicator indicator);

	void deleteById(Long id);
}
