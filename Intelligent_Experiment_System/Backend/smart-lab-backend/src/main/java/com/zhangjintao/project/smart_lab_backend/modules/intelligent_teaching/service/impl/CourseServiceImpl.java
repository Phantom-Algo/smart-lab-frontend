package com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.service.impl;

import com.zhangjintao.project.smart_lab_backend.common.result.PageResult;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.dao.dataobject.CourseDO;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.dao.dataobject.CourseStudentDO;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.dao.repository.CourseRepository;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.dao.repository.CourseStudentRepository;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.enums.CourseStatus;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.enums.CourseStudentEnrollStatus;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.exception.IntelligentTeachingErrorCode;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.exception.IntelligentTeachingException;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.model.converter.IntelligentTeachingConverter;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.model.dto.CourseQuery;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.model.dto.CourseSummaryDTO;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.model.request.CourseBatchEnrollRequest;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.model.request.CourseCreateRequest;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.model.request.CourseQueryRequest;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.model.request.CourseUpdateRequest;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.model.response.CourseDetailResponse;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.model.response.CoursePageItemResponse;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.model.response.CourseStudentResponse;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.service.CourseService;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.service.CourseStudentDirectoryPort;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.service.TeachingCourseDirectoryPort;
import com.zhangjintao.project.smart_lab_backend.modules.personnel_management.enums.PersonType;
import com.zhangjintao.project.smart_lab_backend.modules.personnel_management.model.dto.DepartmentSummaryDTO;
import com.zhangjintao.project.smart_lab_backend.modules.personnel_management.model.dto.PersonDirectoryItemDTO;
import com.zhangjintao.project.smart_lab_backend.modules.personnel_management.service.DepartmentDirectoryPort;
import com.zhangjintao.project.smart_lab_backend.modules.personnel_management.service.PersonDirectoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CourseServiceImpl implements CourseService, TeachingCourseDirectoryPort, CourseStudentDirectoryPort {

	private final CourseRepository courseRepository;
	private final CourseStudentRepository courseStudentRepository;
	private final PersonDirectoryPort personDirectoryPort;
	private final DepartmentDirectoryPort departmentDirectoryPort;
	private final TeachingSupport support;

	@Override
	@Transactional
	public CourseDetailResponse create(CourseCreateRequest request) {
		validateCourseUniqueness(request.getCourseCode(), request.getSemester(), request.getCourseName(), null);
		support.requireCurrentTeacherLike();
		support.requireTeacherPerson(request.getTeacherId());
		validateDepartment(request.getDepartmentId());
		CourseDO courseDO = new CourseDO();
		courseDO.setCourseCode(request.getCourseCode());
		fillCourse(courseDO, request.getCourseName(), request.getSemester(), request.getCredit(), request.getTotalHours(),
				request.getTeacherId(), request.getDepartmentId(), request.getDescription(), request.getSyllabus());
		courseDO.setCourseStatus(CourseStatus.ONGOING.getCode());
		courseRepository.insert(courseDO);
		return buildDetail(courseDO);
	}

	@Override
	@Transactional
	public CourseDetailResponse update(Long id, CourseUpdateRequest request) {
		CourseDO courseDO = support.requireCourse(id);
		PersonDirectoryItemDTO actor = support.requireCurrentTeacherLike();
		assertCourseOperator(courseDO, actor);
		validateCourseUniqueness(courseDO.getCourseCode(), request.getSemester(), request.getCourseName(), id);
		support.requireTeacherPerson(request.getTeacherId());
		validateDepartment(request.getDepartmentId());
		fillCourse(courseDO, request.getCourseName(), request.getSemester(), request.getCredit(), request.getTotalHours(),
				request.getTeacherId(), request.getDepartmentId(), request.getDescription(), request.getSyllabus());
		if (request.getCourseStatus() != null) {
			courseDO.setCourseStatus(request.getCourseStatus());
		}
		courseRepository.update(courseDO);
		return buildDetail(courseDO);
	}

	@Override
	public CourseDetailResponse getById(Long id) {
		return buildDetail(support.requireCourse(id));
	}

	@Override
	public PageResult<CoursePageItemResponse> page(CourseQueryRequest request) {
		PageResult<CourseSummaryDTO> page = page(toQuery(request));
		List<CoursePageItemResponse> records = page.getRecords().stream()
				.map(summary -> IntelligentTeachingConverter.toCoursePageItemResponse(
						support.requireCourse(summary.getCourseId()),
						summary.getTeacherName(),
						summary.getDepartmentName(),
						countEnrolledStudents(summary.getCourseId())))
				.toList();
		return PageResult.of(records, page.getTotal(), page.getPageNum(), page.getPageSize());
	}

	@Override
	@Transactional
	public void archive(Long id) {
		CourseDO courseDO = support.requireCourse(id);
		assertCourseOperator(courseDO, support.requireCurrentTeacherLike());
		courseDO.setCourseStatus(CourseStatus.ARCHIVED.getCode());
		courseRepository.update(courseDO);
	}

	@Override
	@Transactional
	public void delete(Long id) {
		CourseDO courseDO = support.requireCourse(id);
		assertCourseOperator(courseDO, support.requireCurrentTeacherLike());
		courseDO.setDeletedAt(LocalDateTime.now());
		courseRepository.update(courseDO);
	}

	@Override
	public List<CourseStudentResponse> listStudents(Long courseId) {
		support.requireCourse(courseId);
		return listCourseStudentResponses(courseId);
	}

	@Override
	@Transactional
	public void batchEnroll(Long courseId, CourseBatchEnrollRequest request) {
		if (request.getStudentIds() == null || request.getStudentIds().isEmpty()) {
			throw new IntelligentTeachingException(IntelligentTeachingErrorCode.COURSE_STUDENT_EMPTY);
		}
		CourseDO courseDO = support.requireCourse(courseId);
		support.ensureCourseWritable(courseDO);
		assertCourseOperator(courseDO, support.requireCurrentTeacherLike());
		Map<Long, PersonDirectoryItemDTO> studentMap = support.requireStudentMap(request.getStudentIds());
		LocalDateTime now = LocalDateTime.now();
		for (Long studentId : studentMap.keySet()) {
			CourseStudentDO relation = courseStudentRepository.findByCourseIdAndStudentId(courseId, studentId).orElse(null);
			if (relation == null) {
				relation = new CourseStudentDO();
				relation.setCourseId(courseId);
				relation.setStudentId(studentId);
				relation.setEnrolledAt(now);
				relation.setEnrollStatus(CourseStudentEnrollStatus.ENROLLED.getCode());
				courseStudentRepository.insert(relation);
				continue;
			}
			relation.setEnrollStatus(CourseStudentEnrollStatus.ENROLLED.getCode());
			relation.setWithdrawnAt(null);
			if (relation.getEnrolledAt() == null) {
				relation.setEnrolledAt(now);
			}
			courseStudentRepository.update(relation);
		}
	}

	@Override
	@Transactional
	public void batchWithdraw(Long courseId, CourseBatchEnrollRequest request) {
		if (request.getStudentIds() == null || request.getStudentIds().isEmpty()) {
			throw new IntelligentTeachingException(IntelligentTeachingErrorCode.COURSE_STUDENT_EMPTY);
		}
		CourseDO courseDO = support.requireCourse(courseId);
		support.ensureCourseWritable(courseDO);
		assertCourseOperator(courseDO, support.requireCurrentTeacherLike());
		LocalDateTime now = LocalDateTime.now();
		for (Long studentId : request.getStudentIds()) {
			courseStudentRepository.findByCourseIdAndStudentId(courseId, studentId).ifPresent(relation -> {
				relation.setEnrollStatus(CourseStudentEnrollStatus.WITHDRAWN.getCode());
				relation.setWithdrawnAt(now);
				courseStudentRepository.update(relation);
			});
		}
	}

	@Override
	public Optional<CourseSummaryDTO> getByCourseId(Long courseId) {
		return courseRepository.findById(courseId).map(this::toSummary);
	}

	@Override
	public PageResult<CourseSummaryDTO> page(CourseQuery query) {
		if (query == null) {
			query = CourseQuery.builder().pageNum(1L).pageSize(10L).build();
		}
		List<CourseDO> candidates = query.getStudentId() == null
				? courseRepository.list(query)
				: filterCoursesForStudent(courseRepository.list(query), query.getStudentId());
		List<CourseSummaryDTO> summaries = candidates.stream()
				.sorted(Comparator.comparing(CourseDO::getUpdatedAt, Comparator.nullsLast(Comparator.reverseOrder()))
						.thenComparing(CourseDO::getId, Comparator.reverseOrder()))
				.map(this::toSummary)
				.toList();
		return pageSlice(summaries, query.getPageNum(), query.getPageSize());
	}

	@Override
	public List<CourseSummaryDTO> listByTeacherId(Long teacherId) {
		return courseRepository.list(CourseQuery.builder().teacherId(teacherId).build()).stream()
				.map(this::toSummary)
				.toList();
	}

	@Override
	public List<CourseSummaryDTO> listByStudentId(Long studentId) {
		return filterCoursesForStudent(courseRepository.list(CourseQuery.builder().build()), studentId).stream()
				.map(this::toSummary)
				.toList();
	}

	@Override
	public List<PersonDirectoryItemDTO> listStudentsByCourseId(Long courseId) {
		List<CourseStudentDO> relations = activeRelations(courseStudentRepository.listByCourseId(courseId));
		return personDirectoryPort.listByPersonIds(relations.stream().map(CourseStudentDO::getStudentId).toList());
	}

	@Override
	public List<CourseStudentResponse> listCourseStudentResponses(Long courseId) {
		List<CourseStudentDO> relations = activeRelations(courseStudentRepository.listByCourseId(courseId));
		Map<Long, Integer> statusMap = relations.stream()
				.collect(Collectors.toMap(CourseStudentDO::getStudentId, CourseStudentDO::getEnrollStatus));
		return personDirectoryPort.listByPersonIds(statusMap.keySet()).stream()
				.filter(item -> Objects.equals(item.getPersonType(), PersonType.STUDENT.getCode()))
				.map(item -> IntelligentTeachingConverter.toCourseStudentResponse(item, statusMap.get(item.getPersonId())))
				.toList();
	}

	@Override
	public boolean isCourseStudent(Long courseId, Long studentId) {
		return courseStudentRepository.findByCourseIdAndStudentId(courseId, studentId)
				.map(item -> Objects.equals(item.getEnrollStatus(), CourseStudentEnrollStatus.ENROLLED.getCode()))
				.orElse(false);
	}

	@Override
	public boolean isCourseTeacher(Long courseId, Long teacherId) {
		return courseRepository.findById(courseId)
				.map(course -> Objects.equals(course.getTeacherId(), teacherId))
				.orElse(false);
	}

	private void validateCourseUniqueness(String courseCode, String semester, String courseName, Long excludeId) {
		if (courseRepository.existsByCourseCode(courseCode, excludeId)) {
			throw new IntelligentTeachingException(IntelligentTeachingErrorCode.COURSE_CODE_DUPLICATE);
		}
		if (courseRepository.existsBySemesterAndCourseName(semester, courseName, excludeId)) {
			throw new IntelligentTeachingException(IntelligentTeachingErrorCode.COURSE_NAME_DUPLICATE_IN_SEMESTER);
		}
	}

	private void validateDepartment(Long departmentId) {
		if (departmentId == null) {
			return;
		}
		if (departmentDirectoryPort.getById(departmentId).isEmpty()) {
			throw new IntelligentTeachingException(IntelligentTeachingErrorCode.COURSE_DEPARTMENT_INVALID);
		}
	}

	private void fillCourse(CourseDO courseDO, String courseName, String semester, Integer credit, Integer totalHours, Long teacherId,
			Long departmentId, String description, String syllabus) {
		courseDO.setCourseName(courseName);
		courseDO.setSemester(semester);
		courseDO.setCredit(credit);
		courseDO.setTotalHours(totalHours);
		courseDO.setTeacherId(teacherId);
		courseDO.setDepartmentId(departmentId);
		courseDO.setDescription(description);
		courseDO.setSyllabus(syllabus);
	}

	private CourseDetailResponse buildDetail(CourseDO courseDO) {
		PersonDirectoryItemDTO teacher = support.requireTeacherPerson(courseDO.getTeacherId());
		String departmentName = Optional.ofNullable(courseDO.getDepartmentId())
				.flatMap(departmentDirectoryPort::getById)
				.map(DepartmentSummaryDTO::getDepartmentName)
				.orElse(null);
		return IntelligentTeachingConverter.toCourseDetailResponse(courseDO, teacher.getPersonName(), departmentName,
				countEnrolledStudents(courseDO.getId()));
	}

	private CourseSummaryDTO toSummary(CourseDO courseDO) {
		PersonDirectoryItemDTO teacher = support.requireTeacherPerson(courseDO.getTeacherId());
		String departmentName = Optional.ofNullable(courseDO.getDepartmentId())
				.flatMap(departmentDirectoryPort::getById)
				.map(DepartmentSummaryDTO::getDepartmentName)
				.orElse(null);
		return CourseSummaryDTO.builder()
				.courseId(courseDO.getId())
				.courseCode(courseDO.getCourseCode())
				.courseName(courseDO.getCourseName())
				.semester(courseDO.getSemester())
				.teacherId(courseDO.getTeacherId())
				.teacherName(teacher.getPersonName())
				.departmentId(courseDO.getDepartmentId())
				.departmentName(departmentName)
				.courseStatus(courseDO.getCourseStatus())
				.build();
	}

	private long countEnrolledStudents(Long courseId) {
		return activeRelations(courseStudentRepository.listByCourseId(courseId)).size();
	}

	private void assertCourseOperator(CourseDO courseDO, PersonDirectoryItemDTO actor) {
		if (Objects.equals(actor.getPersonType(), PersonType.ADMIN.getCode())) {
			return;
		}
		if (!Objects.equals(courseDO.getTeacherId(), actor.getPersonId())) {
			throw new IntelligentTeachingException(IntelligentTeachingErrorCode.CURRENT_USER_NOT_ALLOWED);
		}
	}

	private List<CourseDO> filterCoursesForStudent(List<CourseDO> courses, Long studentId) {
		Set<Long> enrolledCourseIds = courseStudentRepository.listByStudentId(studentId).stream()
				.filter(item -> Objects.equals(item.getEnrollStatus(), CourseStudentEnrollStatus.ENROLLED.getCode()))
				.map(CourseStudentDO::getCourseId)
				.collect(Collectors.toSet());
		return courses.stream()
				.filter(course -> enrolledCourseIds.contains(course.getId()))
				.toList();
	}

	private List<CourseStudentDO> activeRelations(List<CourseStudentDO> relations) {
		return relations.stream()
				.filter(item -> Objects.equals(item.getEnrollStatus(), CourseStudentEnrollStatus.ENROLLED.getCode()))
				.toList();
	}

	private CourseQuery toQuery(CourseQueryRequest request) {
		return CourseQuery.builder()
				.keyword(request.getKeyword())
				.semester(request.getSemester())
				.teacherId(request.getTeacherId())
				.studentId(request.getStudentId())
				.departmentId(request.getDepartmentId())
				.courseStatus(request.getCourseStatus())
				.pageNum(request.getPageNum())
				.pageSize(request.getPageSize())
				.build();
	}

	private <T> PageResult<T> pageSlice(List<T> source, Long pageNum, Long pageSize) {
		long current = pageNum == null || pageNum < 1 ? 1 : pageNum;
		long size = pageSize == null || pageSize < 1 ? 10 : pageSize;
		int fromIndex = (int) Math.min((current - 1) * size, source.size());
		int toIndex = (int) Math.min(fromIndex + size, source.size());
		return PageResult.of(source.subList(fromIndex, toIndex), source.size(), current, size);
	}
}
