package com.zhangjintao.project.smart_lab_backend.modules.lab_management.service;

import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.dto.LabTypeCreateCommand;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.dto.LabTypeUpdateCommand;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.response.LabTypeResponse;

import java.util.List;

public interface LabTypeService {

	LabTypeResponse create(LabTypeCreateCommand command);

	LabTypeResponse update(Long typeId, LabTypeUpdateCommand command);

	void delete(Long typeId);

	List<LabTypeResponse> listAll();
}
