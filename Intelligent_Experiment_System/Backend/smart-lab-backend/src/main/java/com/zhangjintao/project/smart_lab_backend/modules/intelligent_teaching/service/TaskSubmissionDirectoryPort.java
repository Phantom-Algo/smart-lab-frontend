package com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.service;

import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.model.dto.SubmissionStatsDTO;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.model.dto.TaskSubmissionSummaryDTO;

import java.util.List;
import java.util.Optional;

public interface TaskSubmissionDirectoryPort {

	Optional<TaskSubmissionSummaryDTO> getLatestByTaskAndStudent(Long taskId, Long studentId);

	List<TaskSubmissionSummaryDTO> listLatestByTaskId(Long taskId);

	List<TaskSubmissionSummaryDTO> listHistoryByTaskAndStudent(Long taskId, Long studentId);

	SubmissionStatsDTO getTaskStatsSummary(Long taskId);
}
