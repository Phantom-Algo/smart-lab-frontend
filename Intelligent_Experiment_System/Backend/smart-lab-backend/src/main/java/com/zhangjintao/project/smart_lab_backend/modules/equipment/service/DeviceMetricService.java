package com.zhangjintao.project.smart_lab_backend.modules.equipment.service;

import com.zhangjintao.project.smart_lab_backend.modules.equipment.model.dto.MetricIndicatorConfigDTO;
import com.zhangjintao.project.smart_lab_backend.modules.equipment.model.vo.MetricIndicatorVO;

import java.util.List;

/**
 * 设备监测指标管理服务接口
 *
 * @author linbowei
 */
public interface DeviceMetricService {

    /**
     * 查询某设备类型下的所有监测指标
     */
    List<MetricIndicatorVO> getMetricsByTypeId(Long typeId);

    /**
     * 配置/更新监测指标（管理员）
     * 同一类型下指标名唯一：存在则更新，不存在则新增
     */
    MetricIndicatorVO configureMetric(MetricIndicatorConfigDTO dto);

    /**
     * 删除监测指标（管理员）
     */
    void deleteMetric(Long id);
}
