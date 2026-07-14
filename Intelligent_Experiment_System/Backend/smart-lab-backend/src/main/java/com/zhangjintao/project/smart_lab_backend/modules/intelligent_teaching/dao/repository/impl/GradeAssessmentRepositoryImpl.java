package com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.dao.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.dao.dataobject.GradeAssessmentDO;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.dao.mapper.GradeAssessmentMapper;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.dao.repository.GradeAssessmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class GradeAssessmentRepositoryImpl implements GradeAssessmentRepository {

	private final GradeAssessmentMapper gradeAssessmentMapper;

	@Override
	public Optional<GradeAssessmentDO> findById(Long id) {
		return Optional.ofNullable(gradeAssessmentMapper.selectById(id));
	}

	@Override
	public Optional<GradeAssessmentDO> findByGradeItemIdAndStudentId(Long gradeItemId, Long studentId) {
		return Optional.ofNullable(gradeAssessmentMapper.selectOne(new LambdaQueryWrapper<GradeAssessmentDO>()
				.eq(GradeAssessmentDO::getGradeItemId, gradeItemId)
				.eq(GradeAssessmentDO::getStudentId, studentId)
				.last("limit 1")));
	}

	@Override
	public List<GradeAssessmentDO> listByCourseId(Long courseId) {
		return gradeAssessmentMapper.selectList(new LambdaQueryWrapper<GradeAssessmentDO>()
				.eq(GradeAssessmentDO::getCourseId, courseId)
				.orderByAsc(GradeAssessmentDO::getStudentId, GradeAssessmentDO::getGradeItemId));
	}

	@Override
	public List<GradeAssessmentDO> listByCourseIdAndStudentIds(Long courseId, Collection<Long> studentIds) {
		if (studentIds == null || studentIds.isEmpty()) {
			return List.of();
		}
		return gradeAssessmentMapper.selectList(new LambdaQueryWrapper<GradeAssessmentDO>()
				.eq(GradeAssessmentDO::getCourseId, courseId)
				.in(GradeAssessmentDO::getStudentId, studentIds));
	}

	@Override
	public List<GradeAssessmentDO> listByStudentIdAndCourseId(Long studentId, Long courseId) {
		return gradeAssessmentMapper.selectList(new LambdaQueryWrapper<GradeAssessmentDO>()
				.eq(GradeAssessmentDO::getStudentId, studentId)
				.eq(GradeAssessmentDO::getCourseId, courseId)
				.orderByAsc(GradeAssessmentDO::getGradeItemId));
	}

	@Override
	public void insert(GradeAssessmentDO gradeAssessmentDO) {
		gradeAssessmentMapper.insert(gradeAssessmentDO);
	}

	@Override
	public void update(GradeAssessmentDO gradeAssessmentDO) {
		gradeAssessmentMapper.updateById(gradeAssessmentDO);
	}
}
