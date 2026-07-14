package com.zhangjintao.project.smart_lab_backend.modules.material.controller;

import com.zhangjintao.project.smart_lab_backend.common.result.ApiResult;
import com.zhangjintao.project.smart_lab_backend.modules.auth_profile.service.CurrentUserQueryService;
import com.zhangjintao.project.smart_lab_backend.modules.material.model.vo.MaterialBatchVO;
import com.zhangjintao.project.smart_lab_backend.modules.material.service.MaterialBatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/material/batches")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class BatchController {
    private final MaterialBatchService batchService;
    private final CurrentUserQueryService currentUserQueryService;

    @GetMapping
    public ApiResult<List<MaterialBatchVO>> listBatches(@RequestParam Long materialId) {
        return ApiResult.success(batchService.listBatchesByMaterial(materialId));
    }

    @PutMapping("/{batchId}/scrap")
    public ApiResult<Void> scrapBatch(@PathVariable Long batchId) {
        Long operatorId = currentUserQueryService.requireCurrentUser().getPersonId();
        batchService.scrapBatch(batchId, operatorId);
        return ApiResult.success();
    }
}
