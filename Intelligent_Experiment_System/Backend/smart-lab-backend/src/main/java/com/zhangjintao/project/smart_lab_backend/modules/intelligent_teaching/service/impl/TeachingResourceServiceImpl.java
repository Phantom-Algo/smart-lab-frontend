package com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.service.impl;

import com.zhangjintao.project.smart_lab_backend.common.result.PageResult;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.dao.dataobject.CourseDO;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.dao.dataobject.TeachingResourceDO;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.dao.repository.TeachingResourceRepository;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.enums.TeachingResourceType;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.exception.IntelligentTeachingErrorCode;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.exception.IntelligentTeachingException;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.model.converter.IntelligentTeachingConverter;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.model.dto.TeachingResourceQuery;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.model.request.ResourceUploadPrepareRequest;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.model.request.TeachingResourceCreateRequest;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.model.request.TeachingResourceQueryRequest;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.model.request.TeachingResourceReorderRequest;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.model.request.TeachingResourceUpdateRequest;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.model.response.ResourceUploadPrepareResponse;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.model.response.TeachingResourceResponse;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.service.TeachingResourceService;
import com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.utils.TeachingNoGenerator;
import com.zhangjintao.project.smart_lab_backend.modules.personnel_management.enums.PersonType;
import com.zhangjintao.project.smart_lab_backend.modules.personnel_management.model.dto.PersonDirectoryItemDTO;
import com.zhangjintao.project.smart_lab_backend.modules.personnel_management.service.PersonDirectoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TeachingResourceServiceImpl implements TeachingResourceService {

	private final TeachingResourceRepository teachingResourceRepository;
	private final PersonDirectoryPort personDirectoryPort;
	private final TeachingSupport support;

	@Override
	@Transactional
	public TeachingResourceResponse create(TeachingResourceCreateRequest request) {
		PersonDirectoryItemDTO actor = support.requireCurrentTeacherLike();
		CourseDO courseDO = support.requireCourse(request.getCourseId());
		assertCourseOperator(courseDO, actor);
		support.ensureCourseWritable(courseDO);
		validateResourceContent(request.getResourceType(), request.getFileUrl(), request.getExternalUrl());
		TeachingResourceDO resourceDO = new TeachingResourceDO();
		resourceDO.setResourceNo(TeachingNoGenerator.generateResourceNo());
		resourceDO.setCourseId(request.getCourseId());
		resourceDO.setResourceName(request.getResourceName());
		resourceDO.setResourceType(request.getResourceType());
		resourceDO.setFileUrl(request.getFileUrl());
		resourceDO.setExternalUrl(request.getExternalUrl());
		resourceDO.setFileSize(request.getFileSize());
		resourceDO.setSortNo(request.getSortNo() == null ? 0 : request.getSortNo());
		resourceDO.setUploadedBy(actor.getPersonId());
		teachingResourceRepository.insert(resourceDO);
		return buildResourceResponse(resourceDO);
	}

	@Override
	@Transactional
	public TeachingResourceResponse update(Long id, TeachingResourceUpdateRequest request) {
		PersonDirectoryItemDTO actor = support.requireCurrentTeacherLike();
		TeachingResourceDO resourceDO = support.requireResource(id);
		CourseDO courseDO = support.requireCourse(resourceDO.getCourseId());
		assertCourseOperator(courseDO, actor);
		support.ensureCourseWritable(courseDO);
		validateResourceContent(request.getResourceType(), request.getFileUrl(), request.getExternalUrl());
		resourceDO.setResourceName(request.getResourceName());
		resourceDO.setResourceType(request.getResourceType());
		resourceDO.setFileUrl(request.getFileUrl());
		resourceDO.setExternalUrl(request.getExternalUrl());
		resourceDO.setFileSize(request.getFileSize());
		resourceDO.setSortNo(request.getSortNo() == null ? 0 : request.getSortNo());
		teachingResourceRepository.update(resourceDO);
		return buildResourceResponse(resourceDO);
	}

	@Override
	public TeachingResourceResponse getById(Long id) {
		return buildResourceResponse(support.requireResource(id));
	}

	@Override
	public PageResult<TeachingResourceResponse> page(TeachingResourceQueryRequest request) {
		var page = teachingResourceRepository.page(
				request.getPageNum() == null ? 1L : request.getPageNum(),
				request.getPageSize() == null ? 10L : request.getPageSize(),
				TeachingResourceQuery.builder()
						.courseId(request.getCourseId())
						.resourceType(request.getResourceType())
						.keyword(request.getKeyword())
						.uploadedBy(request.getUploadedBy())
						.pageNum(request.getPageNum())
						.pageSize(request.getPageSize())
						.build());
		List<TeachingResourceResponse> records = page.getRecords().stream().map(this::buildResourceResponse).toList();
		return PageResult.of(records, page.getTotal(), page.getCurrent(), page.getSize());
	}

	@Override
	@Transactional
	public void delete(Long id) {
		PersonDirectoryItemDTO actor = support.requireCurrentTeacherLike();
		TeachingResourceDO resourceDO = support.requireResource(id);
		CourseDO courseDO = support.requireCourse(resourceDO.getCourseId());
		assertCourseOperator(courseDO, actor);
		teachingResourceRepository.deleteById(id);
	}

	@Override
	public ResourceUploadPrepareResponse prepareUpload(ResourceUploadPrepareRequest request) {
		support.requireCurrentTeacherLike();
		return ResourceUploadPrepareResponse.builder()
				.uploadMode("EXTERNAL_REFERENCE")
				.uploadToken(null)
				.objectKey("teaching/" + LocalDate.now() + "/" + request.getFileName())
				.callbackField("fileUrl")
				.note("文件服务暂未接入，请先上传到外部文件系统后回填 fileUrl")
				.build();
	}

	@Override
	@Transactional
	public void reorder(TeachingResourceReorderRequest request) {
		PersonDirectoryItemDTO actor = support.requireCurrentTeacherLike();
		for (TeachingResourceReorderRequest.Item item : request.getItems()) {
			if (item.getResourceId() == null) {
				continue;
			}
			TeachingResourceDO resourceDO = support.requireResource(item.getResourceId());
			assertCourseOperator(support.requireCourse(resourceDO.getCourseId()), actor);
			resourceDO.setSortNo(item.getSortNo() == null ? 0 : item.getSortNo());
			teachingResourceRepository.update(resourceDO);
		}
	}

	private void validateResourceContent(Integer resourceType, String fileUrl, String externalUrl) {
		boolean fileBased = Objects.equals(resourceType, TeachingResourceType.PPT.getCode())
				|| Objects.equals(resourceType, TeachingResourceType.WORD.getCode())
				|| Objects.equals(resourceType, TeachingResourceType.PDF.getCode())
				|| Objects.equals(resourceType, TeachingResourceType.OTHER.getCode());
		boolean linkBased = Objects.equals(resourceType, TeachingResourceType.VIDEO_LINK.getCode())
				|| Objects.equals(resourceType, TeachingResourceType.REFERENCE_LINK.getCode());
		if (fileBased && !StringUtils.hasText(fileUrl)) {
			throw new IntelligentTeachingException(IntelligentTeachingErrorCode.TEACHING_RESOURCE_CONTENT_INVALID);
		}
		if (linkBased && !StringUtils.hasText(externalUrl)) {
			throw new IntelligentTeachingException(IntelligentTeachingErrorCode.TEACHING_RESOURCE_CONTENT_INVALID);
		}
	}

	private void assertCourseOperator(CourseDO courseDO, PersonDirectoryItemDTO actor) {
		if (Objects.equals(actor.getPersonType(), PersonType.ADMIN.getCode())) {
			return;
		}
		if (!Objects.equals(courseDO.getTeacherId(), actor.getPersonId())) {
			throw new IntelligentTeachingException(IntelligentTeachingErrorCode.CURRENT_USER_NOT_ALLOWED);
		}
	}

	private TeachingResourceResponse buildResourceResponse(TeachingResourceDO resourceDO) {
		CourseDO courseDO = support.requireCourse(resourceDO.getCourseId());
		String uploadedByName = personDirectoryPort.getByPersonId(resourceDO.getUploadedBy())
				.map(PersonDirectoryItemDTO::getPersonName)
				.orElse(null);
		return IntelligentTeachingConverter.toTeachingResourceResponse(resourceDO, courseDO.getCourseName(), uploadedByName);
	}
}
