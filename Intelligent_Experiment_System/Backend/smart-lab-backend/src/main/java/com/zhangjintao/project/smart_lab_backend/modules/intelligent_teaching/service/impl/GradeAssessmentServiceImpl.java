package com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.service.impl;

import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.dao.dataobject.CourseDO;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.dao.dataobject.CourseGradePublishDO;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.dao.dataobject.GradeAssessmentDO;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.dao.dataobject.GradeItemDO;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.dao.dataobject.TaskSubmissionDO;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.dao.repository.CourseGradePublishRepository;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.dao.repository.GradeAssessmentRepository;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.dao.repository.GradeItemRepository;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.dao.repository.TaskSubmissionRepository;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.enums.CourseGradePublishStatus;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.enums.GradeAssessmentStatus;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.exception.IntelligentTeachingErrorCode;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.exception.IntelligentTeachingException;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.model.dto.CourseGradeItemDTO;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.model.dto.CourseGradeStatsDTO;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.model.dto.StudentGradeItemDTO;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.model.dto.StudentGradeSheetDTO;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.model.request.BatchGradeDraftRequest;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.model.request.GradeDraftUpsertRequest;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.model.response.CourseGradeStatsResponse;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.model.response.StudentGradeItemResponse;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.model.response.StudentGradeSheetResponse;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.service.CourseStudentDirectoryPort;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.service.GradeAssessmentService;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.service.GradeDirectoryPort;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.service.TeachingNotificationOrchestrator;
import com.zhangjintao.project.smart_lab_backend.modules.personnel_management.enums.PersonType;
import com.zhangjintao.project.smart_lab_backend.modules.personnel_management.model.dto.PersonDirectoryItemDTO;
import com.zhangjintao.project.smart_lab_backend.modules.personnel_management.service.PersonDirectoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GradeAssessmentServiceImpl implements GradeAssessmentService, GradeDirectoryPort {

	private final GradeItemRepository gradeItemRepository;
	private final GradeAssessmentRepository gradeAssessmentRepository;
	private final CourseGradePublishRepository courseGradePublishRepository;
	private final TaskSubmissionRepository taskSubmissionRepository;
	private final PersonDirectoryPort personDirectoryPort;
	private final CourseStudentDirectoryPort courseStudentDirectoryPort;
	private final TeachingNotificationOrchestrator teachingNotificationOrchestrator;
	private final TeachingSupport support;

	@Override
	@Transactional
	public void saveDraft(Long courseId, GradeDraftUpsertRequest request) {
		CourseDO courseDO = support.requireCourse(courseId);
		assertCourseOperator(courseDO, support.requireCurrentTeacherLike());
		assertGradeWritable(courseId);
		doSaveDraft(courseId, request, support.requireCurrentTeacherLike().getPersonId());
	}

	@Override
	@Transactional
	public void batchSaveDraft(Long courseId, BatchGradeDraftRequest request) {
		CourseDO courseDO = support.requireCourse(courseId);
		PersonDirectoryItemDTO actor = support.requireCurrentTeacherLike();
		assertCourseOperator(courseDO, actor);
		assertGradeWritable(courseId);
		for (GradeDraftUpsertRequest item : request.getItems()) {
			doSaveDraft(courseId, item, actor.getPersonId());
		}
	}

	@Override
	public List<StudentGradeSheetResponse> listCourseGradeSheets(Long courseId) {
		CourseDO courseDO = support.requireCourse(courseId);
		assertCourseOperator(courseDO, support.requireCurrentTeacherLike());
		return listStudentGradesByCourseId(courseId).stream().map(sheet -> toResponse(sheet, courseDO.getCourseName())).toList();
	}

	@Override
	public CourseGradeStatsResponse getCourseGradeStats(Long courseId) {
		CourseDO courseDO = support.requireCourse(courseId);
		assertCourseOperator(courseDO, support.requireCurrentTeacherLike());
		CourseGradeStatsDTO statsDTO = getCourseGradeStatsSummary(courseId);
		return CourseGradeStatsResponse.builder()
				.courseId(statsDTO.getCourseId())
				.studentCount(statsDTO.getStudentCount())
				.publishedStudentCount(statsDTO.getPublishedStudentCount())
				.draftStudentCount(statsDTO.getDraftStudentCount())
				.averageScore(statsDTO.getAverageScore())
				.highestScore(statsDTO.getHighestScore())
				.lowestScore(statsDTO.getLowestScore())
				.build();
	}

	@Override
	@Transactional
	public void publishCourseGrades(Long courseId) {
		CourseDO courseDO = support.requireCourse(courseId);
		PersonDirectoryItemDTO actor = support.requireCurrentTeacherLike();
		assertCourseOperator(courseDO, actor);
		assertPublishReady(courseId);
		List<GradeAssessmentDO> assessments = gradeAssessmentRepository.listByCourseId(courseId);
		LocalDateTime now = LocalDateTime.now();
		for (GradeAssessmentDO assessment : assessments) {
			assessment.setGradeStatus(GradeAssessmentStatus.PUBLISHED.getCode());
			assessment.setPublishedAt(now);
			gradeAssessmentRepository.update(assessment);
		}
		CourseGradePublishDO publishDO = courseGradePublishRepository.findByCourseId(courseId).orElse(null);
		if (publishDO == null) {
			publishDO = new CourseGradePublishDO();
			publishDO.setCourseId(courseId);
			publishDO.setPublishStatus(CourseGradePublishStatus.PUBLISHED.getCode());
			publishDO.setPublishedBy(actor.getPersonId());
			publishDO.setPublishedAt(now);
			courseGradePublishRepository.insert(publishDO);
		}
		else {
			publishDO.setPublishStatus(CourseGradePublishStatus.PUBLISHED.getCode());
			publishDO.setPublishedBy(actor.getPersonId());
			publishDO.setPublishedAt(now);
			courseGradePublishRepository.update(publishDO);
		}
		List<Long> targetUserIds = courseStudentDirectoryPort.listStudentsByCourseId(courseId).stream()
				.map(PersonDirectoryItemDTO::getUserId)
				.filter(Objects::nonNull)
				.distinct()
				.toList();
		teachingNotificationOrchestrator.notifyGradesPublished(courseDO, targetUserIds);
	}

	@Override
	public StudentGradeSheetResponse getMyGradeSheet(Long courseId) {
		PersonDirectoryItemDTO student = support.requireCurrentStudent();
		StudentGradeSheetDTO sheetDTO = getStudentGradeSheet(courseId, student.getPersonId())
				.orElseThrow(() -> new IntelligentTeachingException(IntelligentTeachingErrorCode.GRADE_SHEET_NOT_FOUND));
		if (!Objects.equals(sheetDTO.getPublishStatus(), CourseGradePublishStatus.PUBLISHED.getCode())
				&& !Objects.equals(sheetDTO.getPublishStatus(), CourseGradePublishStatus.LOCKED.getCode())) {
			throw new IntelligentTeachingException(IntelligentTeachingErrorCode.GRADE_SHEET_NOT_FOUND);
		}
		return toResponse(sheetDTO, support.requireCourse(courseId).getCourseName());
	}

	@Override
	public List<CourseGradeItemDTO> listItemsByCourseId(Long courseId) {
		support.requireCourse(courseId);
		return gradeItemRepository.listByCourseId(courseId).stream().map(this::toItemDto).toList();
	}

	@Override
	public List<StudentGradeSheetDTO> listStudentGradesByCourseId(Long courseId) {
		CourseDO courseDO = support.requireCourse(courseId);
		List<GradeItemDO> gradeItems = gradeItemRepository.listByCourseId(courseId);
		List<PersonDirectoryItemDTO> students = courseStudentDirectoryPort.listStudentsByCourseId(courseId);
		Map<Long, List<GradeAssessmentDO>> assessmentMap = gradeAssessmentRepository.listByCourseId(courseId).stream()
				.collect(Collectors.groupingBy(GradeAssessmentDO::getStudentId));
		Integer publishStatus = courseGradePublishRepository.findByCourseId(courseId)
				.map(CourseGradePublishDO::getPublishStatus)
				.orElse(CourseGradePublishStatus.UNPUBLISHED.getCode());
		return students.stream()
				.sorted(Comparator.comparing(PersonDirectoryItemDTO::getPersonName, Comparator.nullsLast(String::compareTo)))
				.map(student -> buildSheet(courseDO, student, gradeItems, assessmentMap.getOrDefault(student.getPersonId(), List.of()),
						publishStatus))
				.toList();
	}

	@Override
	public Optional<StudentGradeSheetDTO> getStudentGradeSheet(Long courseId, Long studentId) {
		CourseDO courseDO = support.requireCourse(courseId);
		PersonDirectoryItemDTO student = personDirectoryPort.getByPersonId(studentId)
				.orElseThrow(() -> new IntelligentTeachingException(IntelligentTeachingErrorCode.COURSE_STUDENT_INVALID));
		if (!courseStudentDirectoryPort.isCourseStudent(courseId, studentId)) {
			return Optional.empty();
		}
		List<GradeItemDO> gradeItems = gradeItemRepository.listByCourseId(courseId);
		List<GradeAssessmentDO> assessments = gradeAssessmentRepository.listByStudentIdAndCourseId(studentId, courseId);
		Integer publishStatus = courseGradePublishRepository.findByCourseId(courseId)
				.map(CourseGradePublishDO::getPublishStatus)
				.orElse(CourseGradePublishStatus.UNPUBLISHED.getCode());
		return Optional.of(buildSheet(courseDO, student, gradeItems, assessments, publishStatus));
	}

	@Override
	public CourseGradeStatsDTO getCourseGradeStatsSummary(Long courseId) {
		List<StudentGradeSheetDTO> sheets = listStudentGradesByCourseId(courseId);
		List<BigDecimal> totals = sheets.stream()
				.map(StudentGradeSheetDTO::getTotalScore)
				.filter(Objects::nonNull)
				.toList();
		BigDecimal average = totals.isEmpty()
				? BigDecimal.ZERO
				: totals.stream().reduce(BigDecimal.ZERO, BigDecimal::add)
						.divide(BigDecimal.valueOf(totals.size()), 2, RoundingMode.HALF_UP);
		BigDecimal highest = totals.stream().max(Comparator.naturalOrder()).orElse(BigDecimal.ZERO);
		BigDecimal lowest = totals.stream().min(Comparator.naturalOrder()).orElse(BigDecimal.ZERO);
		long publishedStudents = sheets.stream()
				.filter(sheet -> Objects.equals(sheet.getPublishStatus(), CourseGradePublishStatus.PUBLISHED.getCode())
						|| Objects.equals(sheet.getPublishStatus(), CourseGradePublishStatus.LOCKED.getCode()))
				.count();
		return CourseGradeStatsDTO.builder()
				.courseId(courseId)
				.studentCount(sheets.size())
				.publishedStudentCount(publishedStudents)
				.draftStudentCount(Math.max(0, sheets.size() - publishedStudents))
				.averageScore(average)
				.highestScore(highest)
				.lowestScore(lowest)
				.build();
	}

	private void doSaveDraft(Long courseId, GradeDraftUpsertRequest request, Long graderId) {
		GradeItemDO gradeItemDO = requireGradeItem(request.getGradeItemId());
		if (!Objects.equals(gradeItemDO.getCourseId(), courseId)) {
			throw new IntelligentTeachingException(IntelligentTeachingErrorCode.GRADE_ITEM_NOT_FOUND);
		}
		if (!courseStudentDirectoryPort.isCourseStudent(courseId, request.getStudentId())) {
			throw new IntelligentTeachingException(IntelligentTeachingErrorCode.COURSE_STUDENT_INVALID);
		}
		validateScore(request.getScore(), gradeItemDO.getFullScore());
		Long taskSubmissionId = request.getTaskSubmissionId();
		if (gradeItemDO.getRelatedTaskId() != null) {
			TaskSubmissionDO submissionDO = taskSubmissionId == null
					? taskSubmissionRepository.findCurrentByTaskIdAndStudentId(gradeItemDO.getRelatedTaskId(), request.getStudentId())
							.orElse(null)
					: taskSubmissionRepository.findById(taskSubmissionId).orElse(null);
			if (submissionDO == null || !Objects.equals(submissionDO.getStudentId(), request.getStudentId())
					|| !Objects.equals(submissionDO.getTaskId(), gradeItemDO.getRelatedTaskId())) {
				throw new IntelligentTeachingException(IntelligentTeachingErrorCode.GRADE_ITEM_RELATED_TASK_INVALID);
			}
			taskSubmissionId = submissionDO.getId();
			if (Objects.equals(submissionDO.getCurrentFlag(), 1) && !Objects.equals(submissionDO.getSubmitStatus(), 2)) {
				submissionDO.setSubmitStatus(3);
				taskSubmissionRepository.update(submissionDO);
			}
		}
		GradeAssessmentDO assessmentDO = gradeAssessmentRepository.findByGradeItemIdAndStudentId(request.getGradeItemId(),
				request.getStudentId()).orElse(null);
		if (assessmentDO == null) {
			assessmentDO = new GradeAssessmentDO();
			assessmentDO.setGradeItemId(request.getGradeItemId());
			assessmentDO.setStudentId(request.getStudentId());
			assessmentDO.setCourseId(courseId);
			assessmentDO.setGradeStatus(GradeAssessmentStatus.DRAFT.getCode());
			assessmentDO.setCreatedAt(LocalDateTime.now());
			assessmentDO.setUpdatedAt(LocalDateTime.now());
			assessmentDO.setTaskSubmissionId(taskSubmissionId);
			assessmentDO.setScore(request.getScore());
			assessmentDO.setComment(request.getComment());
			assessmentDO.setGraderId(graderId);
			assessmentDO.setGradedAt(LocalDateTime.now());
			gradeAssessmentRepository.insert(assessmentDO);
			return;
		}
		assessmentDO.setTaskSubmissionId(taskSubmissionId);
		assessmentDO.setScore(request.getScore());
		assessmentDO.setComment(request.getComment());
		assessmentDO.setGraderId(graderId);
		assessmentDO.setGradedAt(LocalDateTime.now());
		assessmentDO.setGradeStatus(GradeAssessmentStatus.DRAFT.getCode());
		assessmentDO.setPublishedAt(null);
		gradeAssessmentRepository.update(assessmentDO);
	}

	private void validateScore(BigDecimal score, BigDecimal fullScore) {
		if (score == null || fullScore == null || score.compareTo(BigDecimal.ZERO) < 0 || score.compareTo(fullScore) > 0) {
			throw new IntelligentTeachingException(IntelligentTeachingErrorCode.GRADE_SCORE_INVALID);
		}
	}

	private void assertPublishReady(Long courseId) {
		List<GradeItemDO> gradeItems = gradeItemRepository.listByCourseId(courseId);
		if (gradeItems.isEmpty()) {
			throw new IntelligentTeachingException(IntelligentTeachingErrorCode.GRADE_PUBLISH_NOT_READY);
		}
		BigDecimal totalWeight = gradeItems.stream()
				.map(GradeItemDO::getItemWeight)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
		if (totalWeight.compareTo(new BigDecimal("100.00")) != 0) {
			throw new IntelligentTeachingException(IntelligentTeachingErrorCode.GRADE_PUBLISH_NOT_READY);
		}
		List<PersonDirectoryItemDTO> students = courseStudentDirectoryPort.listStudentsByCourseId(courseId);
		Map<Long, List<GradeAssessmentDO>> assessmentsByStudent = gradeAssessmentRepository.listByCourseId(courseId).stream()
				.collect(Collectors.groupingBy(GradeAssessmentDO::getStudentId));
		for (PersonDirectoryItemDTO student : students) {
			List<GradeAssessmentDO> assessments = assessmentsByStudent.getOrDefault(student.getPersonId(), List.of());
			Map<Long, GradeAssessmentDO> assessmentMap = assessments.stream()
					.collect(Collectors.toMap(GradeAssessmentDO::getGradeItemId, Function.identity()));
			for (GradeItemDO item : gradeItems) {
				GradeAssessmentDO assessment = assessmentMap.get(item.getId());
				if (assessment == null || assessment.getScore() == null) {
					throw new IntelligentTeachingException(IntelligentTeachingErrorCode.GRADE_PUBLISH_NOT_READY);
				}
			}
		}
	}

	private void assertGradeWritable(Long courseId) {
		courseGradePublishRepository.findByCourseId(courseId).ifPresent(publish -> {
			if (publish.getPublishStatus() != null
					&& publish.getPublishStatus() != CourseGradePublishStatus.UNPUBLISHED.getCode()) {
				throw new IntelligentTeachingException(IntelligentTeachingErrorCode.GRADE_ALREADY_PUBLISHED);
			}
		});
	}

	private void assertCourseOperator(CourseDO courseDO, PersonDirectoryItemDTO actor) {
		if (Objects.equals(actor.getPersonType(), PersonType.ADMIN.getCode())) {
			return;
		}
		if (!Objects.equals(courseDO.getTeacherId(), actor.getPersonId())) {
			throw new IntelligentTeachingException(IntelligentTeachingErrorCode.CURRENT_USER_NOT_ALLOWED);
		}
	}

	private GradeItemDO requireGradeItem(Long gradeItemId) {
		return gradeItemRepository.findById(gradeItemId)
				.orElseThrow(() -> new IntelligentTeachingException(IntelligentTeachingErrorCode.GRADE_ITEM_NOT_FOUND));
	}

	private CourseGradeItemDTO toItemDto(GradeItemDO item) {
		return CourseGradeItemDTO.builder()
				.gradeItemId(item.getId())
				.courseId(item.getCourseId())
				.itemName(item.getItemName())
				.itemType(item.getItemType())
				.itemWeight(item.getItemWeight())
				.fullScore(item.getFullScore())
				.relatedTaskId(item.getRelatedTaskId())
				.sortNo(item.getSortNo())
				.build();
	}

	private StudentGradeSheetDTO buildSheet(CourseDO courseDO, PersonDirectoryItemDTO student, List<GradeItemDO> gradeItems,
			List<GradeAssessmentDO> assessments, Integer publishStatus) {
		Map<Long, GradeAssessmentDO> assessmentMap = assessments.stream()
				.collect(Collectors.toMap(GradeAssessmentDO::getGradeItemId, Function.identity(), (left, right) -> right, HashMap::new));
		List<StudentGradeItemDTO> items = new ArrayList<>();
		BigDecimal totalScore = BigDecimal.ZERO;
		boolean complete = !gradeItems.isEmpty();
		for (GradeItemDO item : gradeItems) {
			GradeAssessmentDO assessment = assessmentMap.get(item.getId());
			BigDecimal weighted = null;
			if (assessment != null && assessment.getScore() != null) {
				weighted = assessment.getScore()
						.divide(item.getFullScore(), 6, RoundingMode.HALF_UP)
						.multiply(item.getItemWeight())
						.setScale(2, RoundingMode.HALF_UP);
				totalScore = totalScore.add(weighted);
			}
			else {
				complete = false;
			}
			items.add(StudentGradeItemDTO.builder()
					.gradeItemId(item.getId())
					.itemName(item.getItemName())
					.itemType(item.getItemType())
					.itemWeight(item.getItemWeight())
					.fullScore(item.getFullScore())
					.score(assessment == null ? null : assessment.getScore())
					.weightedScore(weighted)
					.gradeStatus(assessment == null ? GradeAssessmentStatus.DRAFT.getCode() : assessment.getGradeStatus())
					.comment(assessment == null ? null : assessment.getComment())
					.build());
		}
		return StudentGradeSheetDTO.builder()
				.courseId(courseDO.getId())
				.studentId(student.getPersonId())
				.studentName(student.getPersonName())
				.studentNo(student.getStudentNo())
				.publishStatus(publishStatus)
				.totalScore(complete ? totalScore.setScale(2, RoundingMode.HALF_UP) : null)
				.items(items)
				.build();
	}

	private StudentGradeSheetResponse toResponse(StudentGradeSheetDTO dto, String courseName) {
		return StudentGradeSheetResponse.builder()
				.courseId(dto.getCourseId())
				.courseName(courseName)
				.studentId(dto.getStudentId())
				.studentName(dto.getStudentName())
				.studentNo(dto.getStudentNo())
				.publishStatus(dto.getPublishStatus())
				.totalScore(dto.getTotalScore())
				.items(dto.getItems().stream().map(item -> StudentGradeItemResponse.builder()
						.gradeItemId(item.getGradeItemId())
						.itemName(item.getItemName())
						.itemType(item.getItemType())
						.itemWeight(item.getItemWeight())
						.fullScore(item.getFullScore())
						.score(item.getScore())
						.weightedScore(item.getWeightedScore())
						.gradeStatus(item.getGradeStatus())
						.comment(item.getComment())
						.build()).toList())
				.build();
	}
}
