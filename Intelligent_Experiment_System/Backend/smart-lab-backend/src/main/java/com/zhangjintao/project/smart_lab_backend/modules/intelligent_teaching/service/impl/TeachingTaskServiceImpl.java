package com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.service.impl;

import com.zhangjintao.project.smart_lab_backend.common.result.PageResult;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.dao.dataobject.CourseDO;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.dao.dataobject.TeachingTaskDO;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.dao.repository.CourseRepository;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.dao.repository.TeachingTaskRepository;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.enums.TeachingTaskStatus;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.exception.IntelligentTeachingErrorCode;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.exception.IntelligentTeachingException;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.model.converter.IntelligentTeachingConverter;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.model.dto.TeachingTaskQuery;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.model.dto.TeachingTaskSummaryDTO;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.model.request.TeachingTaskCreateRequest;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.model.request.TeachingTaskPublishRequest;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.model.request.TeachingTaskQueryRequest;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.model.request.TeachingTaskUpdateRequest;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.model.response.TeachingTaskDetailResponse;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.model.response.TeachingTaskPageItemResponse;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.service.TeachingNotificationOrchestrator;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.service.TeachingTaskDirectoryPort;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.service.TeachingTaskService;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.utils.TeachingNoGenerator;
import com.zhangjintao.project.smart_lab_backend.modules.personnel_management.enums.PersonType;
import com.zhangjintao.project.smart_lab_backend.modules.personnel_management.model.dto.PersonDirectoryItemDTO;
import com.zhangjintao.project.smart_lab_backend.modules.personnel_management.service.PersonDirectoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TeachingTaskServiceImpl implements TeachingTaskService, TeachingTaskDirectoryPort {

	private final TeachingTaskRepository teachingTaskRepository;
	private final CourseRepository courseRepository;
	private final PersonDirectoryPort personDirectoryPort;
	private final TeachingSupport support;
	private final TeachingNotificationOrchestrator teachingNotificationOrchestrator;

	@Override
	@Transactional
	public TeachingTaskDetailResponse create(TeachingTaskCreateRequest request) {
		PersonDirectoryItemDTO actor = support.requireCurrentTeacherLike();
		CourseDO courseDO = support.requireCourse(request.getCourseId());
		assertCourseOperator(courseDO, actor);
		support.ensureCourseWritable(courseDO);
		validateDeadline(request.getDeadline());
		TeachingTaskDO taskDO = new TeachingTaskDO();
		taskDO.setTaskNo(TeachingNoGenerator.generateTaskNo());
		taskDO.setCourseId(request.getCourseId());
		taskDO.setTaskTitle(request.getTaskTitle());
		taskDO.setTaskContent(request.getTaskContent());
		taskDO.setSubmitType(request.getSubmitType());
		taskDO.setMaxFileSize(request.getMaxFileSize());
		taskDO.setDeadline(request.getDeadline());
		taskDO.setTaskWeight(request.getTaskWeight());
		taskDO.setTaskStatus(TeachingTaskStatus.DRAFT.getCode());
		taskDO.setCreatedBy(actor.getPersonId());
		teachingTaskRepository.insert(taskDO);
		return buildDetail(taskDO);
	}

	@Override
	@Transactional
	public TeachingTaskDetailResponse update(Long id, TeachingTaskUpdateRequest request) {
		PersonDirectoryItemDTO actor = support.requireCurrentTeacherLike();
		TeachingTaskDO taskDO = support.requireTask(id);
		CourseDO courseDO = support.requireCourse(taskDO.getCourseId());
		assertCourseOperator(courseDO, actor);
		support.ensureCourseWritable(courseDO);
		if (!Objects.equals(taskDO.getTaskStatus(), TeachingTaskStatus.DRAFT.getCode())) {
			throw new IntelligentTeachingException(IntelligentTeachingErrorCode.TEACHING_TASK_STATUS_INVALID);
		}
		validateDeadline(request.getDeadline());
		taskDO.setTaskTitle(request.getTaskTitle());
		taskDO.setTaskContent(request.getTaskContent());
		taskDO.setSubmitType(request.getSubmitType());
		taskDO.setMaxFileSize(request.getMaxFileSize());
		taskDO.setDeadline(request.getDeadline());
		taskDO.setTaskWeight(request.getTaskWeight());
		teachingTaskRepository.update(taskDO);
		return buildDetail(taskDO);
	}

	@Override
	public TeachingTaskDetailResponse getById(Long id) {
		return buildDetail(support.requireTask(id));
	}

	@Override
	public PageResult<TeachingTaskPageItemResponse> page(TeachingTaskQueryRequest request) {
		PageResult<TeachingTaskSummaryDTO> page = page(toQuery(request));
		List<TeachingTaskPageItemResponse> records = page.getRecords().stream()
				.map(item -> buildPageItem(support.requireTask(item.getTaskId())))
				.toList();
		return PageResult.of(records, page.getTotal(), page.getPageNum(), page.getPageSize());
	}

	@Override
	public List<TeachingTaskPageItemResponse> listByCourseId(Long courseId) {
		return teachingTaskRepository.listByCourseId(courseId).stream()
				.map(this::buildPageItem)
				.toList();
	}

	@Override
	@Transactional
	public TeachingTaskDetailResponse publish(Long id, TeachingTaskPublishRequest request) {
		PersonDirectoryItemDTO actor = support.requireCurrentTeacherLike();
		TeachingTaskDO taskDO = support.requireTask(id);
		CourseDO courseDO = support.requireCourse(taskDO.getCourseId());
		assertCourseOperator(courseDO, actor);
		support.ensureCourseWritable(courseDO);
		if (!Objects.equals(taskDO.getTaskStatus(), TeachingTaskStatus.DRAFT.getCode())) {
			throw new IntelligentTeachingException(IntelligentTeachingErrorCode.TEACHING_TASK_STATUS_INVALID);
		}
		if (request != null && request.getDeadline() != null) {
			validateDeadline(request.getDeadline());
			taskDO.setDeadline(request.getDeadline());
		}
		else {
			validateDeadline(taskDO.getDeadline());
		}
		taskDO.setTaskStatus(TeachingTaskStatus.PUBLISHED.getCode());
		taskDO.setPublishedAt(LocalDateTime.now());
		teachingTaskRepository.update(taskDO);
		teachingNotificationOrchestrator.notifyTaskPublished(courseDO, taskDO);
		return buildDetail(taskDO);
	}

	@Override
	@Transactional
	public void close(Long id) {
		PersonDirectoryItemDTO actor = support.requireCurrentTeacherLike();
		TeachingTaskDO taskDO = support.requireTask(id);
		CourseDO courseDO = support.requireCourse(taskDO.getCourseId());
		assertCourseOperator(courseDO, actor);
		taskDO.setTaskStatus(TeachingTaskStatus.CLOSED.getCode());
		teachingTaskRepository.update(taskDO);
	}

	@Override
	public Optional<TeachingTaskSummaryDTO> getByTaskId(Long taskId) {
		return teachingTaskRepository.findById(taskId).map(this::toSummary);
	}

	@Override
	public PageResult<TeachingTaskSummaryDTO> page(TeachingTaskQuery query) {
		if (query == null) {
			query = TeachingTaskQuery.builder().pageNum(1L).pageSize(10L).build();
		}
		var page = teachingTaskRepository.page(
				query.getPageNum() == null ? 1L : query.getPageNum(),
				query.getPageSize() == null ? 10L : query.getPageSize(),
				query);
		List<TeachingTaskSummaryDTO> records = page.getRecords().stream().map(this::toSummary).toList();
		return PageResult.of(records, page.getTotal(), page.getCurrent(), page.getSize());
	}

	@Override
	public List<TeachingTaskSummaryDTO> listSummariesByCourseId(Long courseId) {
		return teachingTaskRepository.listByCourseId(courseId).stream().map(this::toSummary).toList();
	}

	private TeachingTaskQuery toQuery(TeachingTaskQueryRequest request) {
		return TeachingTaskQuery.builder()
				.courseId(request.getCourseId())
				.createdBy(request.getCreatedBy())
				.keyword(request.getKeyword())
				.taskStatus(request.getTaskStatus())
				.pageNum(request.getPageNum())
				.pageSize(request.getPageSize())
				.build();
	}

	private void validateDeadline(LocalDateTime deadline) {
		if (deadline == null || !deadline.isAfter(LocalDateTime.now())) {
			throw new IntelligentTeachingException(IntelligentTeachingErrorCode.TEACHING_TASK_DEADLINE_INVALID);
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

	private TeachingTaskSummaryDTO toSummary(TeachingTaskDO taskDO) {
		return TeachingTaskSummaryDTO.builder()
				.taskId(taskDO.getId())
				.taskNo(taskDO.getTaskNo())
				.courseId(taskDO.getCourseId())
				.taskTitle(taskDO.getTaskTitle())
				.taskStatus(taskDO.getTaskStatus())
				.deadline(taskDO.getDeadline())
				.publishedAt(taskDO.getPublishedAt())
				.build();
	}

	private TeachingTaskDetailResponse buildDetail(TeachingTaskDO taskDO) {
		CourseDO courseDO = support.requireCourse(taskDO.getCourseId());
		Map<Long, PersonDirectoryItemDTO> personMap = personDirectoryPort.listByPersonIds(List.of(taskDO.getCreatedBy())).stream()
				.collect(Collectors.toMap(PersonDirectoryItemDTO::getPersonId, Function.identity()));
		return IntelligentTeachingConverter.toTeachingTaskDetailResponse(taskDO, courseDO.getCourseName(),
				Optional.ofNullable(personMap.get(taskDO.getCreatedBy())).map(PersonDirectoryItemDTO::getPersonName).orElse(null));
	}

	private TeachingTaskPageItemResponse buildPageItem(TeachingTaskDO taskDO) {
		CourseDO courseDO = support.requireCourse(taskDO.getCourseId());
		String createdByName = personDirectoryPort.getByPersonId(taskDO.getCreatedBy())
				.map(PersonDirectoryItemDTO::getPersonName)
				.orElse(null);
		return IntelligentTeachingConverter.toTeachingTaskPageItemResponse(taskDO, courseDO.getCourseName(), createdByName);
	}
}
