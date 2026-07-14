package com.zhangjintao.project.smart_lab_backend.modules.material.controller;

import com.zhangjintao.project.smart_lab_backend.common.result.ApiResult;
import com.zhangjintao.project.smart_lab_backend.common.result.PageResult;
import com.zhangjintao.project.smart_lab_backend.modules.auth_profile.service.CurrentUserQueryService;
import com.zhangjintao.project.smart_lab_backend.modules.material.model.dto.MaterialCreateDTO;
import com.zhangjintao.project.smart_lab_backend.modules.material.model.dto.MaterialQueryDTO;
import com.zhangjintao.project.smart_lab_backend.modules.material.model.dto.MaterialUpdateDTO;
import com.zhangjintao.project.smart_lab_backend.modules.material.model.dto.StockInDTO;
import com.zhangjintao.project.smart_lab_backend.modules.material.model.vo.MaterialDetailVO;
import com.zhangjintao.project.smart_lab_backend.modules.material.model.vo.MaterialVO;
import com.zhangjintao.project.smart_lab_backend.modules.material.model.vo.StockInResultVO;
import com.zhangjintao.project.smart_lab_backend.modules.material.service.MaterialService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/material/materials")
@RequiredArgsConstructor
public class MaterialController {

    private final MaterialService materialService;
    private final CurrentUserQueryService currentUserQueryService;

    @GetMapping
    public ApiResult<PageResult<MaterialVO>> pageMaterials(@Valid MaterialQueryDTO query) {
        return ApiResult.success(materialService.pageMaterials(query));
    }

    @GetMapping("/{id}")
    public ApiResult<MaterialDetailVO> getMaterialDetail(@PathVariable Long id) {
        return ApiResult.success(materialService.getMaterialDetail(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResult<MaterialVO> createMaterial(@Valid @RequestBody MaterialCreateDTO dto) {
        Long operatorId = currentUserQueryService.requireCurrentUser().getPersonId();
        return ApiResult.success(materialService.createMaterial(dto, operatorId));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResult<MaterialVO> updateMaterial(@PathVariable Long id,
                                                 @Valid @RequestBody MaterialUpdateDTO dto) {
        return ApiResult.success(materialService.updateMaterial(id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResult<Void> deleteMaterial(@PathVariable Long id) {
        materialService.deleteMaterial(id);
        return ApiResult.success();
    }

    @PostMapping("/stock-in")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResult<StockInResultVO> stockIn(@Valid @RequestBody StockInDTO dto) {
        Long operatorId = currentUserQueryService.requireCurrentUser().getPersonId();
        return ApiResult.success(materialService.stockIn(dto, operatorId));
    }
}
