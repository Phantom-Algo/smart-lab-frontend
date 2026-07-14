package com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.dao.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.dao.dataobject.TaskSubmissionDO;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.dao.mapper.TaskSubmissionMapper;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.dao.repository.TaskSubmissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class TaskSubmissionRepositoryImpl implements TaskSubmissionRepository {

	private final TaskSubmissionMapper taskSubmissionMapper;

	@Override
	public Optional<TaskSubmissionDO> findById(Long id) {
		return Optional.ofNullable(taskSubmissionMapper.selectById(id));
	}

	@Override
	public Optional<TaskSubmissionDO> findCurrentByTaskIdAndStudentId(Long taskId, Long studentId) {
		return Optional.ofNullable(taskSubmissionMapper.selectOne(new LambdaQueryWrapper<TaskSubmissionDO>()
				.eq(TaskSubmissionDO::getTaskId, taskId)
				.eq(TaskSubmissionDO::getStudentId, studentId)
				.eq(TaskSubmissionDO::getCurrentFlag, 1)
				.last("limit 1")));
	}

	@Override
	public List<TaskSubmissionDO> listCurrentByTaskId(Long taskId) {
		return taskSubmissionMapper.selectList(new LambdaQueryWrapper<TaskSubmissionDO>()
				.eq(TaskSubmissionDO::getTaskId, taskId)
				.eq(TaskSubmissionDO::getCurrentFlag, 1)
				.orderByDesc(TaskSubmissionDO::getSubmittedAt, TaskSubmissionDO::getId));
	}

	@Override
	public List<TaskSubmissionDO> listHistoryByTaskIdAndStudentId(Long taskId, Long studentId) {
		return taskSubmissionMapper.selectList(new LambdaQueryWrapper<TaskSubmissionDO>()
				.eq(TaskSubmissionDO::getTaskId, taskId)
				.eq(TaskSubmissionDO::getStudentId, studentId)
				.orderByDesc(TaskSubmissionDO::getVersionNo, TaskSubmissionDO::getId));
	}

	@Override
	public List<TaskSubmissionDO> listCurrentByTaskIds(Collection<Long> taskIds) {
		if (taskIds == null || taskIds.isEmpty()) {
			return List.of();
		}
		return taskSubmissionMapper.selectList(new LambdaQueryWrapper<TaskSubmissionDO>()
				.in(TaskSubmissionDO::getTaskId, taskIds)
				.eq(TaskSubmissionDO::getCurrentFlag, 1));
	}

	@Override
	public void insert(TaskSubmissionDO taskSubmissionDO) {
		taskSubmissionMapper.insert(taskSubmissionDO);
	}

	@Override
	public void update(TaskSubmissionDO taskSubmissionDO) {
		taskSubmissionMapper.updateById(taskSubmissionDO);
	}
}
