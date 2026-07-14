package com.zhangjintao.project.smart_lab_backend.modules.equipment.controller;

import com.zhangjintao.project.smart_lab_backend.modules.equipment.model.dto.*;
import com.zhangjintao.project.smart_lab_backend.modules.equipment.model.vo.DeviceTypeVO;
import com.zhangjintao.project.smart_lab_backend.modules.equipment.model.vo.DeviceVO;
import com.zhangjintao.project.smart_lab_backend.modules.equipment.service.DeviceService;
import com.zhangjintao.project.smart_lab_backend.modules.equipment.service.DeviceTypeService;
import com.zhangjintao.project.smart_lab_backend.common.result.ApiResult;
import com.zhangjintao.project.smart_lab_backend.common.result.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/equipment")
@RequiredArgsConstructor
public class DeviceController {

    private final DeviceService deviceService;
    private final DeviceTypeService deviceTypeService;

    // ============ 设备 CRUD ============

    @GetMapping("/devices")
    public ApiResult<PageResult<DeviceVO>> listDevices(@Valid DeviceQueryDTO query) {
        return ApiResult.success(deviceService.listDevices(query));
    }

    @GetMapping("/devices/{id}")
    public ApiResult<DeviceVO> getDevice(@PathVariable Long id) {
        return ApiResult.success(deviceService.getDevice(id));
    }

    @PostMapping("/devices")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResult<DeviceVO> createDevice(@Valid @RequestBody DeviceCreateDTO dto) {
        return ApiResult.success(deviceService.createDevice(dto));
    }

    @PutMapping("/devices/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResult<DeviceVO> updateDevice(@PathVariable Long id, @Valid @RequestBody DeviceUpdateDTO dto) {
        return ApiResult.success(deviceService.updateDevice(id, dto));
    }

    @DeleteMapping("/devices/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResult<Void> deleteDevice(@PathVariable Long id) {
        deviceService.deleteDevice(id);
        return ApiResult.success();
    }

    // ============ 设备类型管理 ============

    @GetMapping("/types")
    public ApiResult<List<DeviceTypeVO>> listTypes() {
        return ApiResult.success(deviceTypeService.listTypes());
    }

    @GetMapping("/types/{id}")
    public ApiResult<DeviceTypeVO> getType(@PathVariable Long id) {
        return ApiResult.success(deviceTypeService.getType(id));
    }

    @PostMapping("/types")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResult<DeviceTypeVO> createType(@Valid @RequestBody DeviceTypeCreateDTO dto) {
        return ApiResult.success(deviceTypeService.createType(dto));
    }

    @PutMapping("/types/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResult<DeviceTypeVO> updateType(@PathVariable Long id, @Valid @RequestBody DeviceTypeUpdateDTO dto) {
        return ApiResult.success(deviceTypeService.updateType(id, dto));
    }

    @DeleteMapping("/types/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResult<Void> deleteType(@PathVariable Long id) {
        deviceTypeService.deleteType(id);
        return ApiResult.success();
    }
}
