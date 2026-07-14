package com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.dao.repository;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.dao.dataobject.TeachingResourceDO;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.model.dto.TeachingResourceQuery;

import java.util.List;
import java.util.Optional;

public interface TeachingResourceRepository {

	Optional<TeachingResourceDO> findById(Long id);

	IPage<TeachingResourceDO> page(Long current, Long size, TeachingResourceQuery query);

	List<TeachingResourceDO> listByCourseId(Long courseId);

	void insert(TeachingResourceDO teachingResourceDO);

	void update(TeachingResourceDO teachingResourceDO);

	void deleteById(Long id);
}
