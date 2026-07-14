package com.zhangjintao.project.smart_lab_backend.modules.material.job;

import com.zhangjintao.project.smart_lab_backend.modules.material.service.MaterialAlertService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 低库存检测定时任务 — FR-0007
 * <p>
 * 每小时检查所有物资的当前库存是否低于安全库存，自动生成低库存预警记录并推送通知。
 *
 * @author linbowei
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LowStockCheckJob {

    private final MaterialAlertService materialAlertService;

    @Scheduled(cron = "${material.low-stock-check-cron:0 0 * * * ?}")
    public void checkLowStock() {
        log.info("LowStockCheckJob started");
        try {
            materialAlertService.checkLowStock();
        } catch (Exception e) {
            log.error("LowStockCheckJob failed", e);
        }
        log.info("LowStockCheckJob completed");
    }
}
