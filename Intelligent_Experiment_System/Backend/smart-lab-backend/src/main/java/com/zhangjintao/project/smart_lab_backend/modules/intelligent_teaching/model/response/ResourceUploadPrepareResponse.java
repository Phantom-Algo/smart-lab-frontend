package com.zhangjintao.project.smart_lab_backend.modules.intelligent_teaching.model.response;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ResourceUploadPrepareResponse {

	String uploadMode;
	String uploadToken;
	String objectKey;
	String callbackField;
	String note;
}
