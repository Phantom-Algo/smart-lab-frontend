package com.zhangjintao.project.smart_lab_backend.modules.personnel_management.dao.repository;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.zhangjintao.project.smart_lab_backend.modules.personnel_management.dao.dataobject.PersonDO;
import com.zhangjintao.project.smart_lab_backend.modules.personnel_management.model.dto.PersonDirectoryQuery;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PersonRepository {

	Optional<PersonDO> findById(Long id);

	List<PersonDO> listByIds(Collection<Long> ids);

	IPage<PersonDO> page(Long current, Long size, PersonDirectoryQuery query);

	List<PersonDO> list(PersonDirectoryQuery query);

	boolean existsByPersonNo(String personNo, Long excludeId);

	boolean existsByStudentNo(String studentNo, Long excludeId);

	boolean existsByEmployeeNo(String employeeNo, Long excludeId);

	long countByDepartmentId(Long departmentId);

	long countByPositionId(Long positionId);

	void insert(PersonDO personDO);

	void update(PersonDO personDO);
}
