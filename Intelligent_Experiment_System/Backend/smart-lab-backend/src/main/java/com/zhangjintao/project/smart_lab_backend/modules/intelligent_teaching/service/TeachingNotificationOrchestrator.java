package com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.service;

import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.dao.dataobject.CourseDO;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.dao.dataobject.TeachingTaskDO;

public interface TeachingNotificationOrchestrator {

	void notifyTaskPublished(CourseDO courseDO, TeachingTaskDO teachingTaskDO);

	void notifySubmissionReturned(CourseDO courseDO, Long studentUserId, String taskTitle, String returnedReason, Long submissionId);

	void notifyGradesPublished(CourseDO courseDO, java.util.List<Long> targetUserIds);
}
