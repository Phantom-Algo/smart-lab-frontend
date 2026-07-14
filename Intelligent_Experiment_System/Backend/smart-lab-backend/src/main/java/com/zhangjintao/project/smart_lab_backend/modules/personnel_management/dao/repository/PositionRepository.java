package com.zhangjintao.project.smart_lab_backend.modules.personnel_management.dao.repository;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.zhangjintao.project.smart_lab_backend.modules.personnel_management.dao.dataobject.PositionDO;

import java.util.List;
import java.util.Optional;

public interface PositionRepository {

	Optional<PositionDO> findById(Long id);

	IPage<PositionDO> page(Long current, Long size, Long departmentId, Integer positionStatus);

	List<PositionDO> listByDepartmentId(Long departmentId);

	boolean existsByPositionNo(String positionNo, Long excludeId);

	boolean existsByPositionCode(String positionCode, Long excludeId);

	long countByDepartmentId(Long departmentId);

	void insert(PositionDO positionDO);

	void update(PositionDO positionDO);
}
