package com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.service.impl;

import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.dao.dataobject.CourseDO;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.dao.dataobject.TeachingTaskDO;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.model.response.CourseStudentResponse;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.service.CourseStudentDirectoryPort;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.service.TeachingNotificationOrchestrator;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.enums.ContentFormat;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.enums.TargetType;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.model.dto.MessagePublishCommand;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.model.dto.TempMessageTemplate;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.service.MessagePublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TeachingNotificationOrchestratorImpl implements TeachingNotificationOrchestrator {

	private final MessagePublisher messagePublisher;
	private final CourseStudentDirectoryPort courseStudentDirectoryPort;

	@Override
	public void notifyTaskPublished(CourseDO courseDO, TeachingTaskDO teachingTaskDO) {
		List<Long> targetUserIds = courseStudentDirectoryPort.listCourseStudentResponses(courseDO.getId()).stream()
				.map(CourseStudentResponse::getUserId)
				.filter(java.util.Objects::nonNull)
				.distinct()
				.toList();
		if (targetUserIds.isEmpty()) {
			return;
		}
		messagePublisher.publish(MessagePublishCommand.builder()
				.eventNo("teaching-task-published-" + teachingTaskDO.getTaskNo())
				.sourceSystem("teaching")
				.messageType("teaching.task.published")
				.targetType(targetUserIds.size() == 1 ? TargetType.SINGLE : TargetType.MULTI)
				.targetUserIds(targetUserIds)
				.tempTemplate(TempMessageTemplate.builder()
						.titleTemplate("新教学任务：${taskTitle}")
						.contentTemplate("课程《${courseName}》已发布新任务，请在截止时间前完成提交。")
						.contentFormat(ContentFormat.TEXT)
						.build())
				.params(Map.of(
						"courseName", courseDO.getCourseName(),
						"taskTitle", teachingTaskDO.getTaskTitle(),
						"deadline", teachingTaskDO.getDeadline()))
				.actionUrl("/courses/" + courseDO.getId() + "/tasks/" + teachingTaskDO.getId())
				.occurredAt(LocalDateTime.now())
				.build());
	}

	@Override
	public void notifySubmissionReturned(CourseDO courseDO, Long studentUserId, String taskTitle, String returnedReason,
			Long submissionId) {
		if (studentUserId == null) {
			return;
		}
		messagePublisher.publish(MessagePublishCommand.builder()
				.eventNo("teaching-submission-returned-" + submissionId)
				.sourceSystem("teaching")
				.messageType("teaching.submission.returned")
				.targetType(TargetType.SINGLE)
				.targetUserIds(List.of(studentUserId))
				.tempTemplate(TempMessageTemplate.builder()
						.titleTemplate("作业被退回：${taskTitle}")
						.contentTemplate("课程《${courseName}》中的提交已被教师退回，原因：${returnedReason}")
						.contentFormat(ContentFormat.TEXT)
						.build())
				.params(Map.of(
						"courseName", courseDO.getCourseName(),
						"taskTitle", taskTitle,
						"returnedReason", returnedReason))
				.actionUrl("/courses/" + courseDO.getId() + "/submissions/" + submissionId)
				.occurredAt(LocalDateTime.now())
				.build());
	}

	@Override
	public void notifyGradesPublished(CourseDO courseDO, List<Long> targetUserIds) {
		if (targetUserIds == null || targetUserIds.isEmpty()) {
			return;
		}
		messagePublisher.publish(MessagePublishCommand.builder()
				.eventNo("teaching-grade-published-" + courseDO.getId() + "-" + System.currentTimeMillis())
				.sourceSystem("teaching")
				.messageType("teaching.grade.published")
				.targetType(targetUserIds.size() == 1 ? TargetType.SINGLE : TargetType.MULTI)
				.targetUserIds(targetUserIds)
				.tempTemplate(TempMessageTemplate.builder()
						.titleTemplate("成绩已发布：${courseName}")
						.contentTemplate("课程《${courseName}》的成绩已发布，请及时查看。")
						.contentFormat(ContentFormat.TEXT)
						.build())
				.params(Map.of("courseName", courseDO.getCourseName()))
				.actionUrl("/courses/" + courseDO.getId() + "/grades/me")
				.occurredAt(LocalDateTime.now())
				.build());
	}
}
