package com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.model.converter;

import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.dao.dataobject.CourseDO;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.dao.dataobject.CourseLabDO;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.dao.dataobject.TeachingResourceDO;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.dao.dataobject.TeachingTaskDO;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.model.dto.LabSummaryDTO;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.model.response.CourseDetailResponse;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.model.response.CourseLabBindingResponse;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.model.response.CoursePageItemResponse;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.model.response.CourseStudentResponse;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.model.response.TeachingResourceResponse;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.model.response.TeachingTaskDetailResponse;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.model.response.TeachingTaskPageItemResponse;
import com.zhangjintao.project.smart_lab_backend.modules.personnel_management.model.dto.PersonDirectoryItemDTO;

public final class IntelligentTeachingConverter {

	private IntelligentTeachingConverter() {
	}

	public static CourseDetailResponse toCourseDetailResponse(CourseDO courseDO, String teacherName, String departmentName,
			long studentCount) {
		return CourseDetailResponse.builder()
				.courseId(courseDO.getId())
				.courseCode(courseDO.getCourseCode())
				.courseName(courseDO.getCourseName())
				.semester(courseDO.getSemester())
				.credit(courseDO.getCredit())
				.totalHours(courseDO.getTotalHours())
				.teacherId(courseDO.getTeacherId())
				.teacherName(teacherName)
				.departmentId(courseDO.getDepartmentId())
				.departmentName(departmentName)
				.description(courseDO.getDescription())
				.syllabus(courseDO.getSyllabus())
				.courseStatus(courseDO.getCourseStatus())
				.studentCount(studentCount)
				.build();
	}

	public static CoursePageItemResponse toCoursePageItemResponse(CourseDO courseDO, String teacherName, String departmentName,
			long studentCount) {
		return CoursePageItemResponse.builder()
				.courseId(courseDO.getId())
				.courseCode(courseDO.getCourseCode())
				.courseName(courseDO.getCourseName())
				.semester(courseDO.getSemester())
				.teacherName(teacherName)
				.departmentName(departmentName)
				.courseStatus(courseDO.getCourseStatus())
				.studentCount(studentCount)
				.build();
	}

	public static CourseStudentResponse toCourseStudentResponse(PersonDirectoryItemDTO person, Integer enrollStatus) {
		return CourseStudentResponse.builder()
				.personId(person.getPersonId())
				.personName(person.getPersonName())
				.studentNo(person.getStudentNo())
				.className(person.getClassName())
				.userId(person.getUserId())
				.accountStatus(person.getAccountStatus())
				.enrollStatus(enrollStatus)
				.build();
	}

	public static CourseLabBindingResponse toCourseLabBindingResponse(CourseLabDO courseLabDO, LabSummaryDTO labSummaryDTO) {
		return CourseLabBindingResponse.builder()
				.labId(courseLabDO.getLabId())
				.bindStatus(courseLabDO.getBindStatus())
				.labDetailAvailable(labSummaryDTO != null)
				.labName(labSummaryDTO == null ? null : labSummaryDTO.getLabName())
				.labNo(labSummaryDTO == null ? null : labSummaryDTO.getLabNo())
				.campus(labSummaryDTO == null ? null : labSummaryDTO.getCampus())
				.building(labSummaryDTO == null ? null : labSummaryDTO.getBuilding())
				.floor(labSummaryDTO == null ? null : labSummaryDTO.getFloor())
				.roomNo(labSummaryDTO == null ? null : labSummaryDTO.getRoomNo())
				.capacity(labSummaryDTO == null ? null : labSummaryDTO.getCapacity())
				.runningStatus(labSummaryDTO == null ? null : labSummaryDTO.getRunningStatus())
				.runningStatusDesc(labSummaryDTO == null ? null : labSummaryDTO.getRunningStatusDesc())
				.openStatus(labSummaryDTO == null ? null : labSummaryDTO.getOpenStatus())
				.openStatusDesc(labSummaryDTO == null ? null : labSummaryDTO.getOpenStatusDesc())
				.available(labSummaryDTO == null ? null : labSummaryDTO.getAvailable())
				.boundAt(courseLabDO.getBoundAt())
				.unboundAt(courseLabDO.getUnboundAt())
				.build();
	}

	public static TeachingTaskDetailResponse toTeachingTaskDetailResponse(TeachingTaskDO teachingTaskDO, String courseName,
			String createdByName) {
		return TeachingTaskDetailResponse.builder()
				.taskId(teachingTaskDO.getId())
				.taskNo(teachingTaskDO.getTaskNo())
				.courseId(teachingTaskDO.getCourseId())
				.courseName(courseName)
				.taskTitle(teachingTaskDO.getTaskTitle())
				.taskContent(teachingTaskDO.getTaskContent())
				.submitType(teachingTaskDO.getSubmitType())
				.maxFileSize(teachingTaskDO.getMaxFileSize())
				.deadline(teachingTaskDO.getDeadline())
				.taskWeight(teachingTaskDO.getTaskWeight())
				.taskStatus(teachingTaskDO.getTaskStatus())
				.publishedAt(teachingTaskDO.getPublishedAt())
				.createdBy(teachingTaskDO.getCreatedBy())
				.createdByName(createdByName)
				.build();
	}

	public static TeachingTaskPageItemResponse toTeachingTaskPageItemResponse(TeachingTaskDO teachingTaskDO, String courseName,
			String createdByName) {
		return TeachingTaskPageItemResponse.builder()
				.taskId(teachingTaskDO.getId())
				.taskNo(teachingTaskDO.getTaskNo())
				.courseId(teachingTaskDO.getCourseId())
				.courseName(courseName)
				.taskTitle(teachingTaskDO.getTaskTitle())
				.taskStatus(teachingTaskDO.getTaskStatus())
				.deadline(teachingTaskDO.getDeadline())
				.publishedAt(teachingTaskDO.getPublishedAt())
				.createdByName(createdByName)
				.build();
	}

	public static TeachingResourceResponse toTeachingResourceResponse(TeachingResourceDO teachingResourceDO, String courseName,
			String uploadedByName) {
		return TeachingResourceResponse.builder()
				.resourceId(teachingResourceDO.getId())
				.resourceNo(teachingResourceDO.getResourceNo())
				.courseId(teachingResourceDO.getCourseId())
				.courseName(courseName)
				.resourceName(teachingResourceDO.getResourceName())
				.resourceType(teachingResourceDO.getResourceType())
				.fileUrl(teachingResourceDO.getFileUrl())
				.externalUrl(teachingResourceDO.getExternalUrl())
				.fileSize(teachingResourceDO.getFileSize())
				.sortNo(teachingResourceDO.getSortNo())
				.uploadedBy(teachingResourceDO.getUploadedBy())
				.uploadedByName(uploadedByName)
				.createdAt(teachingResourceDO.getCreatedAt())
				.build();
	}
}
