package com.zhangjintao.project.smart_lab_backend.modules.equipment.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 智能化设备管理子系统配置
 *
 * @author linbowei
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "equipment")
public class EquipmentConfig {
    /** 设备编号前缀 */
    private String deviceNoPrefix = "DEV";
    /** 维修单号前缀 */
    private String repairNoPrefix = "RPR";
    /** 申请单号前缀 */
    private String applyNoPrefix = "APL";
    /** 健康分计算开关 */
    private boolean healthScoreEnabled = true;
}
