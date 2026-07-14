package com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.dao.repository;

import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.dao.dataobject.GradeAssessmentDO;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface GradeAssessmentRepository {

	Optional<GradeAssessmentDO> findById(Long id);

	Optional<GradeAssessmentDO> findByGradeItemIdAndStudentId(Long gradeItemId, Long studentId);

	List<GradeAssessmentDO> listByCourseId(Long courseId);

	List<GradeAssessmentDO> listByCourseIdAndStudentIds(Long courseId, Collection<Long> studentIds);

	List<GradeAssessmentDO> listByStudentIdAndCourseId(Long studentId, Long courseId);

	void insert(GradeAssessmentDO gradeAssessmentDO);

	void update(GradeAssessmentDO gradeAssessmentDO);
}
