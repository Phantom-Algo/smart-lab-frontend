package com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.dao.repository;

import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.dao.dataobject.TaskSubmissionDO;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface TaskSubmissionRepository {

	Optional<TaskSubmissionDO> findById(Long id);

	Optional<TaskSubmissionDO> findCurrentByTaskIdAndStudentId(Long taskId, Long studentId);

	List<TaskSubmissionDO> listCurrentByTaskId(Long taskId);

	List<TaskSubmissionDO> listHistoryByTaskIdAndStudentId(Long taskId, Long studentId);

	List<TaskSubmissionDO> listCurrentByTaskIds(Collection<Long> taskIds);

	void insert(TaskSubmissionDO taskSubmissionDO);

	void update(TaskSubmissionDO taskSubmissionDO);
}
