package com.zhangjintao.project.smart_lab_backend.modules.material;

import com.zhangjintao.project.smart_lab_backend.modules.material.dao.dataobject.*;
import com.zhangjintao.project.smart_lab_backend.modules.material.dao.repository.*;
import com.zhangjintao.project.smart_lab_backend.modules.material.model.dto.*;
import com.zhangjintao.project.smart_lab_backend.modules.material.model.vo.*;
import com.zhangjintao.project.smart_lab_backend.modules.material.service.MaterialAlertService;
import com.zhangjintao.project.smart_lab_backend.modules.material.service.MaterialReceiveService;
import com.zhangjintao.project.smart_lab_backend.modules.material.service.MaterialService;
import com.zhangjintao.project.smart_lab_backend.modules.message_inbox.service.MessagePublisher;
import com.zhangjintao.project.smart_lab_backend.modules.personnel_management.model.dto.PersonDirectoryItemDTO;
import com.zhangjintao.project.smart_lab_backend.modules.personnel_management.service.PersonDirectoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 物资管理子系统 — 集成测试（H2 内存数据库）
 * <p>
 * 覆盖：完整领用流程、FIFO多批次扣减、低库存预警、预警去重、人员解析
 *
 * @author linbowei
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class MaterialIntegrationTest {

    @Autowired private MaterialService materialService;
    @Autowired private MaterialReceiveService receiveService;
    @Autowired private MaterialAlertService alertService;

    @Autowired private MaterialRepository materialRepository;
    @Autowired private MaterialBatchRepository batchRepository;
    @Autowired private MaterialReceiveApplyRepository applyRepository;
    @Autowired private MaterialReceiveLogRepository logRepository;
    @Autowired private MaterialReceiveBatchDetailRepository detailRepository;
    @Autowired private MaterialAlertRecordRepository alertRecordRepository;
    @Autowired private PersonDirectoryPort personDirectoryPort;

    @Autowired private JdbcTemplate jdbc;

    @MockBean private MessagePublisher messagePublisher;

    private Long materialId;

    @BeforeEach
    void setUp() {
        // 创建物资
        MaterialDO m = new MaterialDO();
        m.setMaterialNo("MTL-INTEGRATION");
        m.setMaterialName("集成测试试剂");
        m.setLabId(1L);
        m.setUnit("瓶");
        m.setSafetyStock(20);
        m.setCurrentStock(0);
        materialRepository.insert(m);
        materialId = m.getId();
    }

    // ==================== 1. 完整领用流程 ====================

    @Test @DisplayName("完整流程：入库→申请→审批出库→验证库存和记录")
    void test01_fullReceiveFlow() {
        // ---- 入库 50 ----
        StockInDTO stockIn = new StockInDTO();
        stockIn.setMaterialId(materialId);
        stockIn.setQuantity(50);
        stockIn.setExpiryDate(LocalDate.of(2027, 6, 30));
        StockInResultVO stockInResult = materialService.stockIn(stockIn, 100L);

        assertThat(stockInResult).isNotNull();
        assertThat(stockInResult.getQuantity()).isEqualTo(50);
        assertThat(stockInResult.getNewCurrentStock()).isEqualTo(50);

        // 验证批次表
        List<MaterialBatchDO> batches = batchRepository.findByMaterialId(materialId);
        assertThat(batches).hasSize(1);
        assertThat(batches.get(0).getRemaining()).isEqualTo(50);
        assertThat(batches.get(0).getBatchStatus()).isEqualTo(1);

        // 验证入库记录
        assertThat(stockInResult.getBatchNo()).isNotNull();
        assertThat(stockInResult.getStockInNo()).isNotNull();

        // ---- 申请 20 ----
        ReceiveApplyDTO applyDTO = new ReceiveApplyDTO();
        applyDTO.setMaterialId(materialId);
        applyDTO.setExpectedQuantity(20);
        applyDTO.setPurpose("集成测试");
        ReceiveApplyVO applyVO = receiveService.apply(applyDTO, 200L);

        assertThat(applyVO.getApplyNo()).startsWith("MRP");
        assertThat(applyVO.getApplyStatus()).isEqualTo(1); // 待审核
        assertThat(applyVO.getExpectedQuantity()).isEqualTo(20);

        // ---- 审批出库 ----
        ReceiveLogVO logVO = receiveService.approve(applyVO.getApplyNo(), 300L);
        assertThat(logVO.getTotalQuantity()).isEqualTo(20);
        assertThat(logVO.getReceiverId()).isEqualTo(200L);
        assertThat(logVO.getOutOperatorId()).isEqualTo(300L);

        // ---- 验证库存已扣减 ----
        MaterialDO updated = materialRepository.findById(materialId).orElseThrow();
        assertThat(updated.getCurrentStock()).isEqualTo(30); // 50-20

        // ---- 验证批次余量 ----
        List<MaterialBatchDO> updatedBatches = batchRepository.findByMaterialId(materialId);
        assertThat(updatedBatches.get(0).getRemaining()).isEqualTo(30); // 50-20

        // ---- 验证出库记录 ----
        MaterialReceiveLogDO logDO = logRepository.findByApplyId(applyVO.getId()).orElseThrow();
        assertThat(logDO.getTotalQuantity()).isEqualTo(20);

        // ---- 验证批次扣减明细 ----
        List<MaterialReceiveBatchDetailDO> details = detailRepository.findByReceiveLogId(logDO.getId());
        assertThat(details).hasSize(1);
        assertThat(details.get(0).getDeductedQuantity()).isEqualTo(20);

        // ---- 验证申请状态变更 ----
        MaterialReceiveApplyDO updatedApply = applyRepository.findById(applyVO.getId()).orElseThrow();
        assertThat(updatedApply.getApplyStatus()).isEqualTo(2); // 审批通过
        assertThat(updatedApply.getApproverId()).isEqualTo(300L);
    }

    // ==================== 2. 多批次 FIFO 扣减 ====================

    @Test @DisplayName("FIFO选批：3批总量100，申请80 → 前2批清空第3批剩20")
    void test02_multiBatchFifoDeduction() {
        // 先入库一批50
        materialService.stockIn(stockInDTO(50, LocalDate.of(2027, 6, 30)), 1L);
        // 再入库一批30（有效期更早）
        materialService.stockIn(stockInDTO(30, LocalDate.of(2026, 12, 31)), 1L);
        // 再入库一批20
        materialService.stockIn(stockInDTO(20, LocalDate.of(2027, 9, 30)), 1L);

        // 当前总库存 100（50+30+20），有效期顺序：批2(26/12/31) → 批1(27/6/30) → 批3(27/9/30)
        MaterialDO m = materialRepository.findById(materialId).orElseThrow();
        assertThat(m.getCurrentStock()).isEqualTo(100);

        // 申请80
        ReceiveApplyDTO applyDTO = new ReceiveApplyDTO();
        applyDTO.setMaterialId(materialId);
        applyDTO.setExpectedQuantity(80);
        ReceiveApplyVO applyVO = receiveService.apply(applyDTO, 200L);
        receiveService.approve(applyVO.getApplyNo(), 300L);

        // 验证库存：100 - 80 = 20
        MaterialDO after = materialRepository.findById(materialId).orElseThrow();
        assertThat(after.getCurrentStock()).isEqualTo(20);

        // 验证3个批次的状态
        // 入库顺序：批1(50,27/6/30) → 批2(30,26/12/31) → 批3(20,27/9/30)
        // 有效期排序：批2(26/12/31) → 批1(27/6/30) → 批3(27/9/30)
        // 扣减顺序（临期优先）：批2(30)清空 → 批1(50)清空 → toDeduct=0, 批3不动
        // 最终：批1余0、批2余0、批3余20
        List<MaterialBatchDO> batches = batchRepository.findByMaterialId(materialId);
        assertThat(batches).hasSize(3);

        long totalRemaining = batches.stream().mapToInt(b -> b.getRemaining()).sum();
        assertThat(totalRemaining).isEqualTo(20);

        // 有且仅有一个批次余量>0
        List<MaterialBatchDO> nonEmpty = batches.stream()
                .filter(b -> b.getRemaining() > 0).collect(Collectors.toList());
        assertThat(nonEmpty).hasSize(1);
        assertThat(nonEmpty.get(0).getRemaining()).isEqualTo(20);
    }

    // ==================== 3. 低库存预警 ====================

    @Test @DisplayName("低库存预警：库存低于安全库存生成预警")
    void test03_lowStockAlert() {
        // 当前safetyStock=20, currentStock=0(初始)
        // 入库 10 个 → currentStock=10，低于安全库存20
        materialService.stockIn(stockInDTO(10, LocalDate.of(2027, 6, 30)), 1L);

        // 执行低库存检测
        alertService.checkLowStock();

        // 验证预警记录
        List<MaterialAlertRecordDO> alerts = queryAlerts();
        assertThat(alerts).isNotEmpty();
        MaterialAlertRecordDO alert = alerts.get(0);
        assertThat(alert.getMaterialId()).isEqualTo(materialId);
        assertThat(alert.getAlertType()).isEqualTo(1); // 低库存
        assertThat(alert.getStatus()).isEqualTo(0); // 待处理
    }

    @Test @DisplayName("预警去重：同一物资同一天低库存只生成一条预警")
    void test04_alertDedup() {
        materialService.stockIn(stockInDTO(10, LocalDate.of(2027, 6, 30)), 1L);

        // 第一次检测 → 生成预警
        alertService.checkLowStock();
        assertThat(queryAlerts()).hasSize(1);

        // 第二次检测 → 不应重复生成
        alertService.checkLowStock();
        assertThat(queryAlerts()).hasSize(1);
    }

    @Test @DisplayName("库存充足时不生成低库存预警")
    void test05_noAlertWhenStockSufficient() {
        // 入库 50 个 → currentStock=50 > safetyStock=20
        materialService.stockIn(stockInDTO(50, LocalDate.of(2027, 6, 30)), 1L);

        alertService.checkLowStock();
        assertThat(queryAlerts()).isEmpty();
    }

    // ==================== 4. 超量领用预警 ====================

    @Test @DisplayName("超量领用：超过阈值生成预警")
    void test06_overReceiveAlert() {
        // 入库200个（必须>=101以满足库存校验）
        materialService.stockIn(stockInDTO(200, LocalDate.of(2027, 6, 30)), 1L);

        // 申请101个（超出阈值100）
        ReceiveApplyDTO applyDTO = new ReceiveApplyDTO();
        applyDTO.setMaterialId(materialId);
        applyDTO.setExpectedQuantity(101);
        ReceiveApplyVO applyVO = receiveService.apply(applyDTO, 200L);
        receiveService.approve(applyVO.getApplyNo(), 300L);

        // 验证有超量领用预警
        List<MaterialAlertRecordDO> alerts = queryAlerts();
        boolean hasOverReceive = alerts.stream().anyMatch(a -> a.getAlertType() == 4);
        assertThat(hasOverReceive).isTrue();
    }

    // ==================== 5. 人员解析 ====================

    @Test @DisplayName("personId→userId解析（真实链路）")
    void test07_personIdToUserId() {
        // 需要先插入 person 和 user_account 数据
        Long personId = insertPerson("MAT-TEST-1", "物资测试员");
        Long userId = insertUserAccount("MAT-UA-1", "matuser", personId);

        PersonDirectoryItemDTO p = personDirectoryPort.getByPersonId(personId).orElse(null);
        assertThat(p).isNotNull();
        assertThat(p.getUserId()).isEqualTo(userId);
        assertThat(p.getPersonName()).isEqualTo("物资测试员");

        // 不存在的 personId
        assertThat(personDirectoryPort.getByPersonId(99999L)).isEmpty();
    }

    // ==================== 6. 预警处理 ====================

    @Test @DisplayName("预警处理：待处理→已处理")
    void test08_handleAlert() {
        // 先触发低库存预警
        materialService.stockIn(stockInDTO(10, LocalDate.of(2027, 6, 30)), 1L);
        alertService.checkLowStock();

        MaterialAlertRecordDO alert = queryAlerts().get(0);
        Long alertId = alert.getId();
        assertThat(alert.getStatus()).isEqualTo(0);

        // 处理预警
        alertService.handleAlert(alertId, 500L, "已采购入库");
        MaterialAlertRecordDO handled = alertRecordRepository.findById(alertId).orElseThrow();
        assertThat(handled.getStatus()).isEqualTo(1);
        assertThat(handled.getHandledBy()).isEqualTo(500L);
    }

    // ==================== 辅助方法 ====================

    private StockInDTO stockInDTO(int quantity, LocalDate expiryDate) {
        StockInDTO dto = new StockInDTO();
        dto.setMaterialId(materialId);
        dto.setQuantity(quantity);
        dto.setExpiryDate(expiryDate);
        return dto;
    }

    private List<MaterialAlertRecordDO> queryAlerts() {
        return jdbc.query("SELECT * FROM material_alert_record WHERE material_id = ? ORDER BY id",
                (rs, rowNum) -> {
                    MaterialAlertRecordDO r = new MaterialAlertRecordDO();
                    r.setId(rs.getLong("id"));
                    r.setMaterialId(rs.getLong("material_id"));
                    r.setBatchId(rs.getLong("batch_id"));
                    if (rs.wasNull()) r.setBatchId(null);
                    r.setAlertType(rs.getInt("alert_type"));
                    r.setAlertContent(rs.getString("alert_content"));
                    r.setStatus(rs.getInt("status"));
                    r.setHandledBy(rs.getLong("handled_by"));
                    if (rs.wasNull()) r.setHandledBy(null);
                    r.setAlertTime(rs.getTimestamp("alert_time") != null
                            ? rs.getTimestamp("alert_time").toLocalDateTime() : null);
                    return r;
                },
                materialId);
    }

    private Long insertPerson(String no, String name) {
        jdbc.update("INSERT INTO person (person_no, person_name, person_type, person_status, source_type, created_at, updated_at) VALUES (?,?,2,1,1,NOW(),NOW())",
                no, name);
        return jdbc.queryForObject("SELECT id FROM person WHERE person_no = ?", Long.class, no);
    }

    private Long insertUserAccount(String no, String username, Long pid) {
        jdbc.update("INSERT INTO user_account (account_no, username, password_hash, user_type, person_id, account_status, created_at, updated_at) VALUES (?,?,'hash',2,?,1,NOW(),NOW())",
                no, username, pid);
        return jdbc.queryForObject("SELECT id FROM user_account WHERE account_no = ?", Long.class, no);
    }
}
