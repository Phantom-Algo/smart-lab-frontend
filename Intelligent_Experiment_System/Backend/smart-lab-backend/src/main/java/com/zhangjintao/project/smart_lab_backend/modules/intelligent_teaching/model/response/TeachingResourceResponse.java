package com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.model.response;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

@Value
@Builder
public class TeachingResourceResponse {

	Long resourceId;
	String resourceNo;
	Long courseId;
	String courseName;
	String resourceName;
	Integer resourceType;
	String fileUrl;
	String externalUrl;
	Long fileSize;
	Integer sortNo;
	Long uploadedBy;
	String uploadedByName;
	LocalDateTime createdAt;
}
