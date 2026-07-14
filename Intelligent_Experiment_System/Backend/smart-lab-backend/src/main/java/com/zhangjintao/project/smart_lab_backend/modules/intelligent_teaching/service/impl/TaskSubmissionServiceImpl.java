package com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.service.impl;

import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.dao.dataobject.CourseDO;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.dao.dataobject.TaskSubmissionDO;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.dao.dataobject.TeachingTaskDO;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.dao.repository.TaskSubmissionRepository;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.enums.TaskSubmissionStatus;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.enums.TeachingTaskStatus;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.enums.TeachingTaskSubmitType;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.exception.IntelligentTeachingErrorCode;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.exception.IntelligentTeachingException;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.model.dto.SubmissionStatsDTO;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.model.dto.TaskSubmissionSummaryDTO;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.model.request.SubmissionReturnRequest;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.model.request.TaskSubmissionCreateRequest;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.model.response.TaskSubmissionDetailResponse;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.model.response.TaskSubmissionHistoryItemResponse;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.model.response.TaskSubmissionStatsResponse;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.service.CourseStudentDirectoryPort;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.service.TaskSubmissionDirectoryPort;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.service.TaskSubmissionReviewService;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.service.TaskSubmissionService;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.service.TeachingNotificationOrchestrator;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.utils.TeachingNoGenerator;
import com.zhangjintao.project.smart_lab_backend.modules.personnel_management.enums.PersonType;
import com.zhangjintao.project.smart_lab_backend.modules.personnel_management.model.dto.PersonDirectoryItemDTO;
import com.zhangjintao.project.smart_lab_backend.modules.personnel_management.service.PersonDirectoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TaskSubmissionServiceImpl
		implements TaskSubmissionService, TaskSubmissionReviewService, TaskSubmissionDirectoryPort {

	private final TaskSubmissionRepository taskSubmissionRepository;
	private final CourseStudentDirectoryPort courseStudentDirectoryPort;
	private final PersonDirectoryPort personDirectoryPort;
	private final TeachingNotificationOrchestrator teachingNotificationOrchestrator;
	private final TeachingSupport support;

	@Override
	@Transactional
	public TaskSubmissionDetailResponse submit(Long taskId, TaskSubmissionCreateRequest request) {
		PersonDirectoryItemDTO student = support.requireCurrentStudent();
		TeachingTaskDO taskDO = support.requireTask(taskId);
		CourseDO courseDO = support.requireCourse(taskDO.getCourseId());
		if (!courseStudentDirectoryPort.isCourseStudent(courseDO.getId(), student.getPersonId())) {
			throw new IntelligentTeachingException(IntelligentTeachingErrorCode.TASK_SUBMISSION_NOT_ALLOWED);
		}
		validateTaskSubmittable(taskDO);
		validateSubmissionContent(taskDO.getSubmitType(), request.getContentText(), request.getAttachments());

		TaskSubmissionDO previous = taskSubmissionRepository.findCurrentByTaskIdAndStudentId(taskId, student.getPersonId()).orElse(null);
		LocalDateTime now = LocalDateTime.now();
		if (previous != null) {
			previous.setCurrentFlag(0);
			taskSubmissionRepository.update(previous);
		}

		TaskSubmissionDO submissionDO = new TaskSubmissionDO();
		submissionDO.setSubmissionNo(TeachingNoGenerator.generateTaskNo().replace("TT", "TS"));
		submissionDO.setTaskId(taskId);
		submissionDO.setStudentId(student.getPersonId());
		submissionDO.setContentText(request.getContentText());
		submissionDO.setAttachments(request.getAttachments());
		submissionDO.setVersionNo(previous == null ? 1 : previous.getVersionNo() + 1);
		submissionDO.setParentSubmissionId(previous == null ? null : previous.getId());
		submissionDO.setCurrentFlag(1);
		submissionDO.setSubmitStatus(TaskSubmissionStatus.SUBMITTED.getCode());
		submissionDO.setIsLate(taskDO.getDeadline() != null && now.isAfter(taskDO.getDeadline()) ? 1 : 0);
		submissionDO.setSubmittedAt(now);
		taskSubmissionRepository.insert(submissionDO);
		return buildDetail(submissionDO, taskDO, student);
	}

	@Override
	public TaskSubmissionDetailResponse getMyCurrentSubmission(Long taskId) {
		PersonDirectoryItemDTO student = support.requireCurrentStudent();
		TaskSubmissionDO submissionDO = taskSubmissionRepository.findCurrentByTaskIdAndStudentId(taskId, student.getPersonId())
				.orElseThrow(() -> new IntelligentTeachingException(IntelligentTeachingErrorCode.TASK_SUBMISSION_NOT_FOUND));
		return buildDetail(submissionDO, support.requireTask(taskId), student);
	}

	@Override
	public List<TaskSubmissionHistoryItemResponse> listMyHistory(Long taskId) {
		PersonDirectoryItemDTO student = support.requireCurrentStudent();
		return taskSubmissionRepository.listHistoryByTaskIdAndStudentId(taskId, student.getPersonId()).stream()
				.map(this::toHistoryItem)
				.toList();
	}

	@Override
	public List<TaskSubmissionDetailResponse> listTaskCurrentSubmissions(Long taskId) {
		TeachingTaskDO taskDO = support.requireTask(taskId);
		assertCourseOperator(support.requireCourse(taskDO.getCourseId()), support.requireCurrentTeacherLike());
		Map<Long, PersonDirectoryItemDTO> studentMap = personDirectoryPort.listByPersonIds(
				taskSubmissionRepository.listCurrentByTaskId(taskId).stream().map(TaskSubmissionDO::getStudentId).toList()).stream()
				.collect(Collectors.toMap(PersonDirectoryItemDTO::getPersonId, Function.identity()));
		return taskSubmissionRepository.listCurrentByTaskId(taskId).stream()
				.map(item -> buildDetail(item, taskDO, studentMap.get(item.getStudentId())))
				.toList();
	}

	@Override
	public List<TaskSubmissionHistoryItemResponse> listStudentHistory(Long taskId, Long studentId) {
		TeachingTaskDO taskDO = support.requireTask(taskId);
		assertCourseOperator(support.requireCourse(taskDO.getCourseId()), support.requireCurrentTeacherLike());
		return taskSubmissionRepository.listHistoryByTaskIdAndStudentId(taskId, studentId).stream()
				.map(this::toHistoryItem)
				.toList();
	}

	@Override
	public TaskSubmissionStatsResponse getTaskStats(Long taskId) {
		return toStatsResponse(calculateTaskStats(taskId));
	}

	@Override
	@Transactional
	public TaskSubmissionDetailResponse returnSubmission(Long submissionId, SubmissionReturnRequest request) {
		TaskSubmissionDO submissionDO = taskSubmissionRepository.findById(submissionId)
				.orElseThrow(() -> new IntelligentTeachingException(IntelligentTeachingErrorCode.TASK_SUBMISSION_NOT_FOUND));
		TeachingTaskDO taskDO = support.requireTask(submissionDO.getTaskId());
		CourseDO courseDO = support.requireCourse(taskDO.getCourseId());
		assertCourseOperator(courseDO, support.requireCurrentTeacherLike());
		if (!Objects.equals(submissionDO.getCurrentFlag(), 1)) {
			throw new IntelligentTeachingException(IntelligentTeachingErrorCode.TASK_SUBMISSION_NOT_ALLOWED);
		}
		if (!StringUtils.hasText(request.getReturnedReason())) {
			throw new IntelligentTeachingException(IntelligentTeachingErrorCode.TASK_SUBMISSION_RETURN_REASON_REQUIRED);
		}
		submissionDO.setSubmitStatus(TaskSubmissionStatus.RETURNED.getCode());
		submissionDO.setReturnedReason(request.getReturnedReason().trim());
		submissionDO.setReturnedAt(LocalDateTime.now());
		taskSubmissionRepository.update(submissionDO);
		PersonDirectoryItemDTO student = personDirectoryPort.getByPersonId(submissionDO.getStudentId())
				.orElseThrow(() -> new IntelligentTeachingException(IntelligentTeachingErrorCode.COURSE_STUDENT_INVALID));
		teachingNotificationOrchestrator.notifySubmissionReturned(courseDO, student.getUserId(), taskDO.getTaskTitle(),
				submissionDO.getReturnedReason(), submissionDO.getId());
		return buildDetail(submissionDO, taskDO, student);
	}

	@Override
	public Optional<TaskSubmissionSummaryDTO> getLatestByTaskAndStudent(Long taskId, Long studentId) {
		return taskSubmissionRepository.findCurrentByTaskIdAndStudentId(taskId, studentId).map(this::toSummary);
	}

	@Override
	public List<TaskSubmissionSummaryDTO> listLatestByTaskId(Long taskId) {
		return taskSubmissionRepository.listCurrentByTaskId(taskId).stream().map(this::toSummary).toList();
	}

	@Override
	public List<TaskSubmissionSummaryDTO> listHistoryByTaskAndStudent(Long taskId, Long studentId) {
		return taskSubmissionRepository.listHistoryByTaskIdAndStudentId(taskId, studentId).stream().map(this::toSummary).toList();
	}

	@Override
	public SubmissionStatsDTO getTaskStatsSummary(Long taskId) {
		return calculateTaskStats(taskId);
	}

	private SubmissionStatsDTO calculateTaskStats(Long taskId) {
		TeachingTaskDO taskDO = support.requireTask(taskId);
		List<PersonDirectoryItemDTO> courseStudents = courseStudentDirectoryPort.listStudentsByCourseId(taskDO.getCourseId());
		List<TaskSubmissionDO> currentSubmissions = taskSubmissionRepository.listCurrentByTaskId(taskId);
		long expectedCount = courseStudents.size();
		long submittedCount = currentSubmissions.size();
		long returnedCount = currentSubmissions.stream()
				.filter(item -> Objects.equals(item.getSubmitStatus(), TaskSubmissionStatus.RETURNED.getCode()))
				.count();
		long lateCount = currentSubmissions.stream()
				.filter(item -> Objects.equals(item.getIsLate(), 1))
				.count();
		return SubmissionStatsDTO.builder()
				.taskId(taskId)
				.expectedCount(expectedCount)
				.submittedCount(submittedCount)
				.unsubmittedCount(Math.max(0, expectedCount - submittedCount))
				.returnedCount(returnedCount)
				.lateCount(lateCount)
				.build();
	}

	private void validateTaskSubmittable(TeachingTaskDO taskDO) {
		if (!Objects.equals(taskDO.getTaskStatus(), TeachingTaskStatus.PUBLISHED.getCode())) {
			throw new IntelligentTeachingException(IntelligentTeachingErrorCode.TASK_SUBMISSION_NOT_ALLOWED);
		}
	}

	private void validateSubmissionContent(Integer submitType, String contentText, String attachments) {
		boolean hasText = StringUtils.hasText(contentText);
		boolean hasAttachment = StringUtils.hasText(attachments);
		if (Objects.equals(submitType, TeachingTaskSubmitType.TEXT.getCode()) && !hasText) {
			throw new IntelligentTeachingException(IntelligentTeachingErrorCode.TASK_SUBMISSION_CONTENT_INVALID);
		}
		if (Objects.equals(submitType, TeachingTaskSubmitType.FILE.getCode()) && !hasAttachment) {
			throw new IntelligentTeachingException(IntelligentTeachingErrorCode.TASK_SUBMISSION_CONTENT_INVALID);
		}
		if (Objects.equals(submitType, TeachingTaskSubmitType.MIXED.getCode()) && !hasText && !hasAttachment) {
			throw new IntelligentTeachingException(IntelligentTeachingErrorCode.TASK_SUBMISSION_CONTENT_INVALID);
		}
	}

	private void assertCourseOperator(CourseDO courseDO, PersonDirectoryItemDTO actor) {
		if (Objects.equals(actor.getPersonType(), PersonType.ADMIN.getCode())) {
			return;
		}
		if (!Objects.equals(courseDO.getTeacherId(), actor.getPersonId())) {
			throw new IntelligentTeachingException(IntelligentTeachingErrorCode.CURRENT_USER_NOT_ALLOWED);
		}
	}

	private TaskSubmissionSummaryDTO toSummary(TaskSubmissionDO submissionDO) {
		return TaskSubmissionSummaryDTO.builder()
				.submissionId(submissionDO.getId())
				.submissionNo(submissionDO.getSubmissionNo())
				.taskId(submissionDO.getTaskId())
				.studentId(submissionDO.getStudentId())
				.versionNo(submissionDO.getVersionNo())
				.submitStatus(submissionDO.getSubmitStatus())
				.isLate(submissionDO.getIsLate())
				.submittedAt(submissionDO.getSubmittedAt())
				.build();
	}

	private TaskSubmissionHistoryItemResponse toHistoryItem(TaskSubmissionDO submissionDO) {
		return TaskSubmissionHistoryItemResponse.builder()
				.submissionId(submissionDO.getId())
				.versionNo(submissionDO.getVersionNo())
				.currentFlag(submissionDO.getCurrentFlag())
				.submitStatus(submissionDO.getSubmitStatus())
				.isLate(submissionDO.getIsLate())
				.returnedReason(submissionDO.getReturnedReason())
				.submittedAt(submissionDO.getSubmittedAt())
				.build();
	}

	private TaskSubmissionDetailResponse buildDetail(TaskSubmissionDO submissionDO, TeachingTaskDO taskDO,
			PersonDirectoryItemDTO student) {
		return TaskSubmissionDetailResponse.builder()
				.submissionId(submissionDO.getId())
				.submissionNo(submissionDO.getSubmissionNo())
				.taskId(submissionDO.getTaskId())
				.taskTitle(taskDO.getTaskTitle())
				.studentId(submissionDO.getStudentId())
				.studentName(student == null ? null : student.getPersonName())
				.versionNo(submissionDO.getVersionNo())
				.currentFlag(submissionDO.getCurrentFlag())
				.submitStatus(submissionDO.getSubmitStatus())
				.isLate(submissionDO.getIsLate())
				.contentText(submissionDO.getContentText())
				.attachments(submissionDO.getAttachments())
				.returnedReason(submissionDO.getReturnedReason())
				.returnedAt(submissionDO.getReturnedAt())
				.submittedAt(submissionDO.getSubmittedAt())
				.build();
	}

	private TaskSubmissionStatsResponse toStatsResponse(SubmissionStatsDTO statsDTO) {
		return TaskSubmissionStatsResponse.builder()
				.taskId(statsDTO.getTaskId())
				.expectedCount(statsDTO.getExpectedCount())
				.submittedCount(statsDTO.getSubmittedCount())
				.unsubmittedCount(statsDTO.getUnsubmittedCount())
				.returnedCount(statsDTO.getReturnedCount())
				.lateCount(statsDTO.getLateCount())
				.build();
	}
}
