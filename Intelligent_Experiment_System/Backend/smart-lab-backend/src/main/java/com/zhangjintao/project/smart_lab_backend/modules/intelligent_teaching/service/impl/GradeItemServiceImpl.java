package com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.service.impl;

import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.dao.dataobject.CourseDO;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.dao.dataobject.GradeItemDO;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.dao.dataobject.TeachingTaskDO;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.dao.repository.CourseGradePublishRepository;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.dao.repository.GradeItemRepository;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.enums.CourseGradePublishStatus;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.exception.IntelligentTeachingErrorCode;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.exception.IntelligentTeachingException;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.model.request.GradeItemCreateRequest;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.model.request.GradeItemReorderRequest;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.model.request.GradeItemUpdateRequest;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.model.response.GradeItemResponse;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.service.GradeItemService;
import com.zhangjintao.project.smart_lab_backend.modules.personnel_management.enums.PersonType;
import com.zhangjintao.project.smart_lab_backend.modules.personnel_management.model.dto.PersonDirectoryItemDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class GradeItemServiceImpl implements GradeItemService {

	private final GradeItemRepository gradeItemRepository;
	private final CourseGradePublishRepository courseGradePublishRepository;
	private final TeachingSupport support;

	@Override
	@Transactional
	public GradeItemResponse create(Long courseId, GradeItemCreateRequest request) {
		CourseDO courseDO = support.requireCourse(courseId);
		assertCourseOperator(courseDO, support.requireCurrentTeacherLike());
		support.ensureCourseWritable(courseDO);
		assertGradeWritable(courseId);
		validateGradeItem(courseId, request.getItemName(), request.getRelatedTaskId(), request.getItemWeight(), null);
		GradeItemDO gradeItemDO = new GradeItemDO();
		gradeItemDO.setCourseId(courseId);
		fillGradeItem(gradeItemDO, request.getItemName(), request.getItemType(), request.getItemWeight(), request.getFullScore(),
				request.getRelatedTaskId(), request.getSortNo());
		gradeItemRepository.insert(gradeItemDO);
		return toResponse(gradeItemDO);
	}

	@Override
	@Transactional
	public GradeItemResponse update(Long id, GradeItemUpdateRequest request) {
		GradeItemDO gradeItemDO = requireGradeItem(id);
		CourseDO courseDO = support.requireCourse(gradeItemDO.getCourseId());
		assertCourseOperator(courseDO, support.requireCurrentTeacherLike());
		assertGradeWritable(courseDO.getId());
		validateGradeItem(courseDO.getId(), request.getItemName(), request.getRelatedTaskId(), request.getItemWeight(), id);
		fillGradeItem(gradeItemDO, request.getItemName(), request.getItemType(), request.getItemWeight(), request.getFullScore(),
				request.getRelatedTaskId(), request.getSortNo());
		gradeItemRepository.update(gradeItemDO);
		return toResponse(gradeItemDO);
	}

	@Override
	@Transactional
	public void delete(Long id) {
		GradeItemDO gradeItemDO = requireGradeItem(id);
		CourseDO courseDO = support.requireCourse(gradeItemDO.getCourseId());
		assertCourseOperator(courseDO, support.requireCurrentTeacherLike());
		assertGradeWritable(courseDO.getId());
		gradeItemRepository.deleteById(id);
	}

	@Override
	public List<GradeItemResponse> listByCourseId(Long courseId) {
		support.requireCourse(courseId);
		return gradeItemRepository.listByCourseId(courseId).stream().map(this::toResponse).toList();
	}

	@Override
	@Transactional
	public void reorder(Long courseId, GradeItemReorderRequest request) {
		CourseDO courseDO = support.requireCourse(courseId);
		assertCourseOperator(courseDO, support.requireCurrentTeacherLike());
		assertGradeWritable(courseId);
		for (GradeItemReorderRequest.Item item : request.getItems()) {
			if (item.getGradeItemId() == null) {
				continue;
			}
			GradeItemDO gradeItemDO = requireGradeItem(item.getGradeItemId());
			if (!Objects.equals(gradeItemDO.getCourseId(), courseId)) {
				throw new IntelligentTeachingException(IntelligentTeachingErrorCode.GRADE_ITEM_NOT_FOUND);
			}
			gradeItemDO.setSortNo(item.getSortNo() == null ? 0 : item.getSortNo());
			gradeItemRepository.update(gradeItemDO);
		}
	}

	private GradeItemDO requireGradeItem(Long id) {
		return gradeItemRepository.findById(id)
				.orElseThrow(() -> new IntelligentTeachingException(IntelligentTeachingErrorCode.GRADE_ITEM_NOT_FOUND));
	}

	private void validateGradeItem(Long courseId, String itemName, Long relatedTaskId, BigDecimal itemWeight, Long excludeId) {
		if (gradeItemRepository.existsByCourseIdAndItemName(courseId, itemName, excludeId)) {
			throw new IntelligentTeachingException(IntelligentTeachingErrorCode.GRADE_ITEM_NAME_DUPLICATE);
		}
		if (relatedTaskId != null) {
			TeachingTaskDO taskDO = support.requireTask(relatedTaskId);
			if (!Objects.equals(taskDO.getCourseId(), courseId) || gradeItemRepository.existsByRelatedTaskId(relatedTaskId, excludeId)) {
				throw new IntelligentTeachingException(IntelligentTeachingErrorCode.GRADE_ITEM_RELATED_TASK_INVALID);
			}
		}
		BigDecimal totalWeight = gradeItemRepository.sumWeightByCourseId(courseId, excludeId, itemWeight);
		if (totalWeight.compareTo(new BigDecimal("100.00")) > 0) {
			throw new IntelligentTeachingException(IntelligentTeachingErrorCode.GRADE_ITEM_WEIGHT_INVALID);
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

	private void fillGradeItem(GradeItemDO gradeItemDO, String itemName, Integer itemType, BigDecimal itemWeight,
			BigDecimal fullScore, Long relatedTaskId, Integer sortNo) {
		gradeItemDO.setItemName(itemName);
		gradeItemDO.setItemType(itemType);
		gradeItemDO.setItemWeight(itemWeight);
		gradeItemDO.setFullScore(fullScore);
		gradeItemDO.setRelatedTaskId(relatedTaskId);
		gradeItemDO.setSortNo(sortNo == null ? 0 : sortNo);
	}

	private GradeItemResponse toResponse(GradeItemDO gradeItemDO) {
		return GradeItemResponse.builder()
				.gradeItemId(gradeItemDO.getId())
				.courseId(gradeItemDO.getCourseId())
				.itemName(gradeItemDO.getItemName())
				.itemType(gradeItemDO.getItemType())
				.itemWeight(gradeItemDO.getItemWeight())
				.fullScore(gradeItemDO.getFullScore())
				.relatedTaskId(gradeItemDO.getRelatedTaskId())
				.sortNo(gradeItemDO.getSortNo())
				.build();
	}
}
