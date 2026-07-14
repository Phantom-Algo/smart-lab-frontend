package com.zhangjintao.project.smart_lab_backend.modules.personnel_management.service;

import com.zhangjintao.project.smart_lab_backend.modules.personnel_management.model.dto.PersonAccountSummaryDTO;
import com.zhangjintao.project.smart_lab_backend.modules.personnel_management.model.request.PersonCreateAccountRequest;

public interface PersonAccountOrchestrator {

	PersonAccountSummaryDTO createAccount(Long personId, PersonCreateAccountRequest request);

	void disableAccount(Long personId, String reason);

	PersonAccountSummaryDTO getAccount(Long personId);
}
