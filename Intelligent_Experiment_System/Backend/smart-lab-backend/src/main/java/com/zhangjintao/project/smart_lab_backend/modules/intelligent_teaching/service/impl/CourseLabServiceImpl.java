package com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.service.impl;

import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.dao.dataobject.CourseDO;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.dao.dataobject.CourseLabDO;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.dao.repository.CourseLabRepository;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.enums.CourseLabBindStatus;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.exception.IntelligentTeachingErrorCode;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.exception.IntelligentTeachingException;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.model.converter.IntelligentTeachingConverter;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.model.dto.LabSummaryDTO;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.model.request.CourseBindLabRequest;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.model.response.CourseLabBindingResponse;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.service.CourseLabService;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.service.LabDirectoryPort;
import com.zhangjintao.project.smart_lab_backend.modules.personnel_management.enums.PersonType;
import com.zhangjintao.project.smart_lab_backend.modules.personnel_management.model.dto.PersonDirectoryItemDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class CourseLabServiceImpl implements CourseLabService {

	private final CourseLabRepository courseLabRepository;
	private final TeachingSupport support;
	private final LabDirectoryPort labDirectoryPort;

	@Override
	public List<CourseLabBindingResponse> listByCourseId(Long courseId) {
		support.requireCourse(courseId);
		List<CourseLabDO> bindings = courseLabRepository.listByCourseId(courseId);
		Map<Long, LabSummaryDTO> labMap = labDirectoryPort.getByLabIds(bindings.stream().map(CourseLabDO::getLabId).toList());
		return bindings.stream()
				.map(binding -> IntelligentTeachingConverter.toCourseLabBindingResponse(binding, labMap.get(binding.getLabId())))
				.toList();
	}

	@Override
	@Transactional
	public CourseLabBindingResponse bind(Long courseId, CourseBindLabRequest request) {
		CourseDO courseDO = support.requireCourse(courseId);
		assertCourseOperator(courseDO, support.requireCurrentTeacherLike());
		support.ensureCourseWritable(courseDO);
		LabSummaryDTO labSummary = requireBindableLab(request.getLabId());
		CourseLabDO binding = courseLabRepository.findByCourseIdAndLabId(courseId, request.getLabId()).orElse(null);
		if (binding != null && binding.getBindStatus() != null
				&& binding.getBindStatus() == CourseLabBindStatus.BOUND.getCode()) {
			throw new IntelligentTeachingException(IntelligentTeachingErrorCode.COURSE_LAB_ALREADY_BOUND);
		}
		if (binding == null) {
			binding = new CourseLabDO();
			binding.setCourseId(courseId);
			binding.setLabId(request.getLabId());
			binding.setBoundAt(LocalDateTime.now());
			binding.setBindStatus(CourseLabBindStatus.BOUND.getCode());
			courseLabRepository.insert(binding);
		}
		else {
			binding.setBindStatus(CourseLabBindStatus.BOUND.getCode());
			binding.setBoundAt(LocalDateTime.now());
			binding.setUnboundAt(null);
			courseLabRepository.update(binding);
		}
		return IntelligentTeachingConverter.toCourseLabBindingResponse(binding, labSummary);
	}

	@Override
	@Transactional
	public void unbind(Long courseId, Long labId) {
		CourseDO courseDO = support.requireCourse(courseId);
		assertCourseOperator(courseDO, support.requireCurrentTeacherLike());
		support.ensureCourseWritable(courseDO);
		courseLabRepository.findByCourseIdAndLabId(courseId, labId).ifPresent(binding -> {
			binding.setBindStatus(CourseLabBindStatus.UNBOUND.getCode());
			binding.setUnboundAt(LocalDateTime.now());
			courseLabRepository.update(binding);
		});
	}

	private LabSummaryDTO requireBindableLab(Long labId) {
		LabSummaryDTO labSummary = labDirectoryPort.getByLabId(labId)
				.orElseThrow(() -> new IntelligentTeachingException(IntelligentTeachingErrorCode.COURSE_LAB_NOT_FOUND));
		if (!Boolean.TRUE.equals(labSummary.getAvailable())) {
			throw new IntelligentTeachingException(IntelligentTeachingErrorCode.COURSE_LAB_UNAVAILABLE);
		}
		return labSummary;
	}

	private void assertCourseOperator(CourseDO courseDO, PersonDirectoryItemDTO actor) {
		if (Objects.equals(actor.getPersonType(), PersonType.ADMIN.getCode())) {
			return;
		}
		if (!Objects.equals(courseDO.getTeacherId(), actor.getPersonId())) {
			throw new IntelligentTeachingException(IntelligentTeachingErrorCode.CURRENT_USER_NOT_ALLOWED);
		}
	}
}
