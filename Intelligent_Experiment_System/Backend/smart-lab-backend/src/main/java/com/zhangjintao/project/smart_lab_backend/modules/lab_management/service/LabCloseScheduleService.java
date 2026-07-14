package com.zhangjintao.project.smart_lab_backend.modules.lab_management.service;

import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.dto.LabCloseScheduleCreateCommand;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.dto.LabCloseScheduleQueryCriteria;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.dto.LabCloseScheduleUpdateCommand;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.response.LabCloseScheduleMutationResponse;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.response.LabCloseScheduleResponse;

import java.util.List;

public interface LabCloseScheduleService {

	LabCloseScheduleMutationResponse create(Long labId, LabCloseScheduleCreateCommand command);

	LabCloseScheduleMutationResponse update(Long labId, Long scheduleId, LabCloseScheduleUpdateCommand command);

	void delete(Long labId, Long scheduleId);

	List<LabCloseScheduleResponse> listByLabId(Long labId, LabCloseScheduleQueryCriteria criteria);
}
