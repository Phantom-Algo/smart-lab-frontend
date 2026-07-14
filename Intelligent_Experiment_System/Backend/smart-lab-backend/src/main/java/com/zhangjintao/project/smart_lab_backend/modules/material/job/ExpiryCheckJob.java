package com.zhangjintao.project.smart_lab_backend.modules.material.job;

import com.zhangjintao.project.smart_lab_backend.modules.material.service.MaterialAlertService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 临期/过期检测定时任务 — FR-0007
 * <p>
 * 每日凌晨2点检查批次有效期，自动生成临期/过期预警记录并推送通知。
 * 过期批次自动标记为报废并回收库存。
 *
 * @author linbowei
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ExpiryCheckJob {

    private final MaterialAlertService materialAlertService;

    @Scheduled(cron = "${material.expiry-check-cron:0 0 2 * * ?}")
    public void checkExpiry() {
        log.info("ExpiryCheckJob started");
        try {
            materialAlertService.checkExpiry();
        } catch (Exception e) {
            log.error("ExpiryCheckJob failed", e);
        }
        log.info("ExpiryCheckJob completed");
    }
}
