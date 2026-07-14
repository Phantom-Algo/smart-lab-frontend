package com.zhangjintao.project.smart_lab_backend.modules.lab_management.service;

import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.dto.DetectionIndicatorCreateCommand;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.dto.DetectionIndicatorUpdateCommand;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.response.DetectionIndicatorResponse;

import java.util.List;

public interface DetectionIndicatorService {

	DetectionIndicatorResponse create(DetectionIndicatorCreateCommand command);

	DetectionIndicatorResponse update(Long indicatorId, DetectionIndicatorUpdateCommand command);

	void delete(Long indicatorId);

	List<DetectionIndicatorResponse> listByLabTypeId(Long labTypeId);
}
