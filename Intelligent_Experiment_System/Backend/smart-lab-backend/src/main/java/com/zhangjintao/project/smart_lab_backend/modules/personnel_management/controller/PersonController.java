package com.zhangjintao.project.smart_lab_backend.modules.personnel_management.controller;

import com.zhangjintao.project.smart_lab_backend.common.result.ApiResult;
import com.zhangjintao.project.smart_lab_backend.common.result.PageResult;
import com.zhangjintao.project.smart_lab_backend.modules.personnel_management.model.dto.PersonAccountSummaryDTO;
import com.zhangjintao.project.smart_lab_backend.modules.personnel_management.model.request.PersonCreateAccountRequest;
import com.zhangjintao.project.smart_lab_backend.modules.personnel_management.model.request.PersonCreateRequest;
import com.zhangjintao.project.smart_lab_backend.modules.personnel_management.model.request.PersonQueryRequest;
import com.zhangjintao.project.smart_lab_backend.modules.personnel_management.model.request.PersonStatusChangeRequest;
import com.zhangjintao.project.smart_lab_backend.modules.personnel_management.model.request.PersonUpdateRequest;
import com.zhangjintao.project.smart_lab_backend.modules.personnel_management.model.response.PersonDetailResponse;
import com.zhangjintao.project.smart_lab_backend.modules.personnel_management.service.PersonAccountOrchestrator;
import com.zhangjintao.project.smart_lab_backend.modules.personnel_management.service.PersonService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/personnel/persons")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class PersonController {

	private final PersonService personService;
	private final PersonAccountOrchestrator personAccountOrchestrator;

	@PostMapping
	public ApiResult<PersonDetailResponse> create(@Valid @RequestBody PersonCreateRequest request) {
		return ApiResult.success(personService.create(request));
	}

	@PutMapping("/{id}")
	public ApiResult<PersonDetailResponse> update(@PathVariable Long id, @Valid @RequestBody PersonUpdateRequest request) {
		return ApiResult.success(personService.update(id, request));
	}

	@GetMapping("/{id}")
	public ApiResult<PersonDetailResponse> getById(@PathVariable Long id) {
		return ApiResult.success(personService.getById(id));
	}

	@GetMapping
	public ApiResult<PageResult<PersonDetailResponse>> page(PersonQueryRequest request) {
		return ApiResult.success(personService.page(request));
	}

	@PostMapping("/{id}/status")
	public ApiResult<Void> updateStatus(@PathVariable Long id, @Valid @RequestBody PersonStatusChangeRequest request) {
		personService.updateStatus(id, request);
		return ApiResult.success();
	}

	@PostMapping("/{id}/account")
	public ApiResult<PersonAccountSummaryDTO> createAccount(@PathVariable Long id,
			@RequestBody(required = false) PersonCreateAccountRequest request) {
		return ApiResult.success(personAccountOrchestrator.createAccount(id,
				request == null ? new PersonCreateAccountRequest() : request));
	}

	@PostMapping("/{id}/account/disable")
	public ApiResult<Void> disableAccount(@PathVariable Long id, @RequestParam(required = false) String reason) {
		personAccountOrchestrator.disableAccount(id, reason);
		return ApiResult.success();
	}

	@GetMapping("/{id}/account")
	public ApiResult<PersonAccountSummaryDTO> getAccount(@PathVariable Long id) {
		return ApiResult.success(personAccountOrchestrator.getAccount(id));
	}
}
