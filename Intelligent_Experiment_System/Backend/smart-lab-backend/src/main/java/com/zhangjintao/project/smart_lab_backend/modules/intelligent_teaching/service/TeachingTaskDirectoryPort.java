package com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.service;

import com.zhangjintao.project.smart_lab_backend.common.result.PageResult;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.model.dto.TeachingTaskQuery;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.model.dto.TeachingTaskSummaryDTO;

import java.util.List;
import java.util.Optional;

public interface TeachingTaskDirectoryPort {

	Optional<TeachingTaskSummaryDTO> getByTaskId(Long taskId);

	PageResult<TeachingTaskSummaryDTO> page(TeachingTaskQuery query);

	List<TeachingTaskSummaryDTO> listSummariesByCourseId(Long courseId);
}
