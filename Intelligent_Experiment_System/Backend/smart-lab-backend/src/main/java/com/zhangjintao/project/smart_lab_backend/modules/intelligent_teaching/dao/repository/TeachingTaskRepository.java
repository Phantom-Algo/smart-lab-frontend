package com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.dao.repository;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.dao.dataobject.TeachingTaskDO;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.model.dto.TeachingTaskQuery;

import java.util.List;
import java.util.Optional;

public interface TeachingTaskRepository {

	Optional<TeachingTaskDO> findById(Long id);

	IPage<TeachingTaskDO> page(Long current, Long size, TeachingTaskQuery query);

	List<TeachingTaskDO> listByCourseId(Long courseId);

	void insert(TeachingTaskDO teachingTaskDO);

	void update(TeachingTaskDO teachingTaskDO);
}
