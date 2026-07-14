package com.zhangjintao.project.smart_lab_backend.modules.personnel_management.dao.repository;

import com.zhangjintao.project.smart_lab_backend.modules.personnel_management.dao.dataobject.DepartmentDO;

import java.util.List;
import java.util.Optional;

public interface DepartmentRepository {

	Optional<DepartmentDO> findById(Long id);

	List<DepartmentDO> listAll();

	List<DepartmentDO> listByParentId(Long parentId);

	boolean existsByDepartmentNo(String departmentNo, Long excludeId);

	boolean existsByDepartmentCode(String departmentCode, Long excludeId);

	boolean hasChildren(Long id);

	void insert(DepartmentDO departmentDO);

	void update(DepartmentDO departmentDO);
}
