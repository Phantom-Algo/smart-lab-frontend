package com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.service;

import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.model.request.BatchGradeDraftRequest;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.model.request.GradeDraftUpsertRequest;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.model.response.CourseGradeStatsResponse;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.model.response.StudentGradeSheetResponse;

import java.util.List;

public interface GradeAssessmentService {

	void saveDraft(Long courseId, GradeDraftUpsertRequest request);

	void batchSaveDraft(Long courseId, BatchGradeDraftRequest request);

	List<StudentGradeSheetResponse> listCourseGradeSheets(Long courseId);

	CourseGradeStatsResponse getCourseGradeStats(Long courseId);

	void publishCourseGrades(Long courseId);

	StudentGradeSheetResponse getMyGradeSheet(Long courseId);
}
