package com.zhangjintao.project.smart_lab_backend.modules.material.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 物资管理子系统配置
 *
 * @author linbowei
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "material")
public class MaterialConfig {
    /** 物资编号前缀 */
    private String materialNoPrefix = "MTL";
    /** 批次编号前缀 */
    private String batchNoPrefix = "BTC";
    /** 领用申请编号前缀 */
    private String applyNoPrefix = "MRP";
    /** 入库单号前缀 */
    private String stockInNoPrefix = "MSI";
    /** 临期预警阈值（天） */
    private int nearExpiryDays = 30;
    /** 超量领用阈值 */
    private int overReceiveThreshold = 100;
}
