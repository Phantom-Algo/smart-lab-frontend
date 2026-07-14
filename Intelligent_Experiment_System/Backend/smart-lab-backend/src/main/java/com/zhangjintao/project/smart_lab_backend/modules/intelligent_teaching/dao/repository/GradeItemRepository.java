package com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.dao.repository;

import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.dao.dataobject.GradeItemDO;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface GradeItemRepository {

	Optional<GradeItemDO> findById(Long id);

	List<GradeItemDO> listByCourseId(Long courseId);

	boolean existsByCourseIdAndItemName(Long courseId, String itemName, Long excludeId);

	boolean existsByRelatedTaskId(Long relatedTaskId, Long excludeId);

	BigDecimal sumWeightByCourseId(Long courseId, Long excludeId, BigDecimal replacingWeight);

	void insert(GradeItemDO gradeItemDO);

	void update(GradeItemDO gradeItemDO);

	void deleteById(Long id);
}
