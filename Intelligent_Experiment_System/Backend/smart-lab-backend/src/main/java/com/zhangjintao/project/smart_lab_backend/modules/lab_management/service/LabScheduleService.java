package com.zhangjintao.project.smart_lab_backend.modules.lab_management.service;

import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.dto.LabScheduleSaveCommand;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.response.LabScheduleMutationResponse;
import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.response.LabScheduleResponse;

import java.util.List;

public interface LabScheduleService {

	LabScheduleMutationResponse save(Long labId, LabScheduleSaveCommand command);

	List<LabScheduleResponse> listByLabId(Long labId);

	LabScheduleMutationResponse delete(Long labId, Long scheduleId, boolean forceCancelConflictedReservations);
}
