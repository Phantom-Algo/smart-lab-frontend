package com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.service.impl;

import com.zhangjintao.project.smart_lab_backend.common.exception.BusinessException;
import com.zhangjintao.project.smart_lab_backend.common.exception.CommonErrorCode;
import com.zhangjintao.project.smart_lab_backend.modules.auth_profile.model.dto.AuthenticatedUser;
import com.zhangjintao.project.smart_lab_backend.modules.auth_profile.utils.SecurityContextUtils;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.dao.dataobject.CourseDO;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.dao.dataobject.TeachingResourceDO;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.dao.dataobject.TeachingTaskDO;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.dao.repository.CourseRepository;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.dao.repository.TeachingResourceRepository;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.dao.repository.TeachingTaskRepository;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.enums.CourseStatus;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.exception.IntelligentTeachingErrorCode;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.exception.IntelligentTeachingException;
import com.zhangjintao.project.smart_lab_backend.modules.personnel_management.enums.PersonType;
import com.zhangjintao.project.smart_lab_backend.modules.personnel_management.model.dto.PersonDirectoryItemDTO;
import com.zhangjintao.project.smart_lab_backend.modules.personnel_management.service.PersonDirectoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class TeachingSupport {

	private final CourseRepository courseRepository;
	private final TeachingTaskRepository teachingTaskRepository;
	private final TeachingResourceRepository teachingResourceRepository;
	private final PersonDirectoryPort personDirectoryPort;

	public CourseDO requireCourse(Long courseId) {
		return courseRepository.findById(courseId)
				.orElseThrow(() -> new IntelligentTeachingException(IntelligentTeachingErrorCode.COURSE_NOT_FOUND));
	}

	public TeachingTaskDO requireTask(Long taskId) {
		return teachingTaskRepository.findById(taskId)
				.orElseThrow(() -> new IntelligentTeachingException(IntelligentTeachingErrorCode.TEACHING_TASK_NOT_FOUND));
	}

	public TeachingResourceDO requireResource(Long resourceId) {
		return teachingResourceRepository.findById(resourceId)
				.orElseThrow(() -> new IntelligentTeachingException(IntelligentTeachingErrorCode.TEACHING_RESOURCE_NOT_FOUND));
	}

	public PersonDirectoryItemDTO requireTeacherPerson(Long personId) {
		PersonDirectoryItemDTO person = personDirectoryPort.getByPersonId(personId)
				.orElseThrow(() -> new IntelligentTeachingException(IntelligentTeachingErrorCode.COURSE_TEACHER_INVALID));
		if (person.getPersonType() == null || person.getPersonType() != PersonType.TEACHER.getCode()) {
			throw new IntelligentTeachingException(IntelligentTeachingErrorCode.COURSE_TEACHER_INVALID);
		}
		return person;
	}

	public Map<Long, PersonDirectoryItemDTO> requireStudentMap(Collection<Long> studentIds) {
		List<PersonDirectoryItemDTO> students = personDirectoryPort.listByPersonIds(studentIds);
		Map<Long, PersonDirectoryItemDTO> map = students.stream()
				.filter(item -> item.getPersonType() != null && item.getPersonType() == PersonType.STUDENT.getCode())
				.collect(Collectors.toMap(PersonDirectoryItemDTO::getPersonId, Function.identity()));
		if (studentIds == null || studentIds.isEmpty() || map.size() != studentIds.size()) {
			throw new IntelligentTeachingException(IntelligentTeachingErrorCode.COURSE_STUDENT_INVALID);
		}
		return map;
	}

	public AuthenticatedUser requireAuthenticatedUser() {
		return SecurityContextUtils.getAuthenticatedUser()
				.orElseThrow(() -> new BusinessException(CommonErrorCode.UNAUTHORIZED));
	}

	public PersonDirectoryItemDTO requireCurrentTeacherLike() {
		AuthenticatedUser user = requireAuthenticatedUser();
		PersonDirectoryItemDTO person = personDirectoryPort.getByPersonId(user.getPersonId())
				.orElseThrow(() -> new BusinessException(CommonErrorCode.UNAUTHORIZED));
		if (person.getPersonType() == null || person.getPersonType() == PersonType.STUDENT.getCode()) {
			throw new IntelligentTeachingException(IntelligentTeachingErrorCode.CURRENT_USER_NOT_ALLOWED);
		}
		return person;
	}

	public PersonDirectoryItemDTO requireCurrentStudent() {
		AuthenticatedUser user = requireAuthenticatedUser();
		PersonDirectoryItemDTO person = personDirectoryPort.getByPersonId(user.getPersonId())
				.orElseThrow(() -> new BusinessException(CommonErrorCode.UNAUTHORIZED));
		if (person.getPersonType() == null || person.getPersonType() != PersonType.STUDENT.getCode()) {
			throw new IntelligentTeachingException(IntelligentTeachingErrorCode.CURRENT_USER_NOT_ALLOWED);
		}
		return person;
	}

	public void ensureCourseWritable(CourseDO courseDO) {
		if (courseDO.getCourseStatus() != null && courseDO.getCourseStatus() == CourseStatus.ARCHIVED.getCode()) {
			throw new IntelligentTeachingException(IntelligentTeachingErrorCode.TEACHING_TASK_STATUS_INVALID,
					"课程已归档，不允许继续修改教学内容");
		}
	}
}
