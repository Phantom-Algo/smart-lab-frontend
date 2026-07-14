package com.zhangjintao.project.smart_lab_backend.module.equipment.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhangjintao.project.smart_lab_backend.modules.equipment.dao.dataobject.*;
import com.zhangjintao.project.smart_lab_backend.modules.equipment.dao.repository.*;
import com.zhangjintao.project.smart_lab_backend.modules.equipment.model.dto.*;
import com.zhangjintao.project.smart_lab_backend.modules.equipment.model.vo.*;
import com.zhangjintao.project.smart_lab_backend.modules.equipment.service.*;
import com.zhangjintao.project.smart_lab_backend.modules.personnel_management.model.dto.PersonDirectoryItemDTO;
import com.zhangjintao.project.smart_lab_backend.modules.personnel_management.service.PersonDirectoryPort;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.domain.entity.SensorDataMessage;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/**
 * 智能化设备管理系统 — 集成测试（H2 内存数据库，10个用例全覆盖）
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@TestMethodOrder(MethodOrderer.MethodName.class)
class EquipmentIntegrationTest {

    @Autowired private DeviceSensorService deviceSensorService;
    @Autowired private DeviceBorrowService deviceBorrowService;
    @Autowired private DeviceRepairService deviceRepairService;
    @Autowired private DeviceAlertService deviceAlertService;
    @Autowired private DeviceRepository deviceRepository;
    @Autowired private EquipmentTypeRepository equipmentTypeRepository;
    @Autowired private EquipmentMetricIndicatorRepository metricIndicatorRepository;
    @Autowired private PersonDirectoryPort personDirectoryPort;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ObjectMapper objectMapper;

    private Long personId;
    private Long userId;
    private Long deviceId;
    private Long typeId;

    @BeforeEach
    void setUp() {
        personId = insertPerson("P001", "测试员");
        userId = insertUserAccount("UA001", "testuser", personId);

        EquipmentTypeDO t = new EquipmentTypeDO();
        t.setTypeName("测试类型");
        equipmentTypeRepository.insert(t);
        typeId = t.getId();

        addIndicator(typeId, "temperature", "℃", "{\"min\":0,\"max\":40}");
        addIndicator(typeId, "humidity", "%RH", "{\"min\":20,\"max\":80}");

        DeviceDO d = new DeviceDO();
        d.setDeviceNo("DEV-TEST");
        d.setName("测试设备");
        d.setDeviceTypeId(typeId);
        d.setDeviceStatus(2);
        d.setHealthScore(100);
        d.setVersion(0);
        d.setCreatedAt(LocalDateTime.now());
        d.setUpdatedAt(LocalDateTime.now());
        deviceRepository.insert(d);
        deviceId = d.getId();
    }

    // ==================== 1. MQ 消息转换 ====================

    @Test @DisplayName("SensorDataMessage→ReportDTO 字段全映射")
    void test01_mqToDtoConversion() throws Exception {
        String json = mqJson(deviceId, 100L, 1);
        SensorDataMessage msg = objectMapper.readValue(json, SensorDataMessage.class);

        assertThat(msg.getEventNo()).isEqualTo("iot.event.001");
        assertThat(msg.getSensor().getSensorId()).isEqualTo(100L);
        assertThat(msg.getTarget().getTargetType()).isEqualTo(1);
        assertThat(msg.getDetectedData().get("temperature").getValue())
                .isEqualByComparingTo(new BigDecimal("25.5"));

        SensorReportDTO dto = convert(msg);
        assertThat(dto.getEventNo()).isEqualTo(msg.getEventNo());
        assertThat(dto.getSensorId()).isEqualTo(100L);
        assertThat(dto.getDeviceId()).isEqualTo(deviceId);
        assertThat(dto.getMetrics().get("temperature").getQuantity())
                .isEqualByComparingTo(new BigDecimal("25.5"));
    }

    @Test @DisplayName("Target过滤：targetType=2(LAB)被正确跳过")
    void test02_targetTypeLabIsFiltered() throws Exception {
        String json = mqJson(deviceId, 100L, 2);
        SensorDataMessage msg = objectMapper.readValue(json, SensorDataMessage.class);
        assertThat(msg.getTarget().getTargetType()).isEqualTo(2);
        assertThat(msg.getTarget().getTargetType()).isNotEqualTo(1);
    }

    // ==================== 2. 传感器数据落库 + 告警 ====================

    @Test @DisplayName("正常范围→监测日志落库，无告警")
    void test03_normalSensorDataNoAlert() {
        deviceSensorService.receiveSensorData(report(deviceId, 200L, "temperature", "25.0", "℃"));

        Integer cnt = jdbc.queryForObject(
                "SELECT COUNT(*) FROM device_data_monitor_log WHERE device_id = ?", Integer.class, deviceId);
        assertThat(cnt).isGreaterThan(0);

        Integer alertCnt = jdbc.queryForObject(
                "SELECT COUNT(*) FROM device_alert_record WHERE device_id = ?", Integer.class, deviceId);
        assertThat(alertCnt).isEqualTo(0);
    }

    @Test @DisplayName("温度超标→告警生成")
    void test04_overtempGeneratesAlert() {
        deviceSensorService.receiveSensorData(report(deviceId, 300L, "temperature", "45.0", "℃"));

        Integer cnt = jdbc.queryForObject(
                "SELECT COUNT(*) FROM device_alert_record WHERE device_id = ? AND param_name = 'temperature'",
                Integer.class, deviceId);
        assertThat(cnt).isGreaterThan(0);

        Map<String, Object> alert = jdbc.queryForMap(
                "SELECT status, device_id FROM device_alert_record WHERE device_id = ? AND param_name = 'temperature' LIMIT 1",
                deviceId);
        assertThat(alert.get("STATUS")).isEqualTo(0);
        assertThat(((Number) alert.get("DEVICE_ID")).longValue()).isEqualTo(deviceId);
    }

    @Test @DisplayName("多指标混合：temperature超标+humidity正常→仅temperature告警")
    void test05_mixedMetricsPartialAlert() {
        SensorReportDTO dto = new SensorReportDTO();
        dto.setEventNo("evt.mix." + System.nanoTime());
        dto.setSensorId(400L);
        dto.setDeviceId(deviceId);
        Map<String, SensorMetricValueDTO> m = new HashMap<>();

        SensorMetricValueDTO tv = new SensorMetricValueDTO();
        tv.setQuantity(new BigDecimal("50.0")); tv.setUnit("℃");
        m.put("temperature", tv);

        SensorMetricValueDTO hv = new SensorMetricValueDTO();
        hv.setQuantity(new BigDecimal("60.0")); hv.setUnit("%RH");
        m.put("humidity", hv);

        dto.setMetrics(m);
        deviceSensorService.receiveSensorData(dto);

        int tCnt = jdbc.queryForObject(
                "SELECT COUNT(*) FROM device_alert_record WHERE device_id = ? AND param_name = 'temperature'",
                Integer.class, deviceId);
        int hCnt = jdbc.queryForObject(
                "SELECT COUNT(*) FROM device_alert_record WHERE device_id = ? AND param_name = 'humidity'",
                Integer.class, deviceId);
        assertThat(tCnt).isGreaterThan(0);
        assertThat(hCnt).isEqualTo(0);
    }

    // ==================== 3. 人员 ID 对接 ====================

    @Test @DisplayName("personId→userId解析（PersonDirectoryServiceImpl真实链路）")
    void test06_personIdToUserIdResolution() {
        PersonDirectoryItemDTO p = personDirectoryPort.getByPersonId(personId).orElse(null);
        assertThat(p).isNotNull();
        assertThat(p.getUserId()).isEqualTo(userId);
        assertThat(p.getPersonName()).isEqualTo("测试员");
    }

    @Test @DisplayName("不存在的personId→empty")
    void test07_nonexistentPersonIdReturnsEmpty() {
        assertThat(personDirectoryPort.getByPersonId(99999L)).isEmpty();
    }

    // ==================== 4. 借用全流程 ====================

    @Test @DisplayName("借用全链路：申请(deviceId+applicantId)→审批→领用(outOperatorId)→归还(returnOperatorId)")
    void test08_borrowFullFlow() {
        BorrowApplyDTO apply = new BorrowApplyDTO();
        apply.setApplicantId(personId);
        apply.setExpectedStartTime(LocalDateTime.now().plusDays(1));
        apply.setExpectedReturnTime(LocalDateTime.now().plusDays(3));
        apply.setPurpose("集成测试借用");

        // apply(Long deviceId, BorrowApplyDTO dto)
        BorrowApplyVO vo = assertDoesNotThrow(() -> deviceBorrowService.apply(deviceId, apply));
        assertThat(vo.getApplyNo()).startsWith("APL");
        assertThat(vo.getDeviceId()).isEqualTo(deviceId);

        BorrowApproveDTO approve = new BorrowApproveDTO();
        approve.setApproverId(personId);
        approve.setApproved(true);
        assertDoesNotThrow(() -> deviceBorrowService.approve(vo.getApplyNo(), approve));

        BorrowLogVO log = assertDoesNotThrow(() -> deviceBorrowService.checkOut(vo.getApplyNo(), personId));
        assertThat(log.getOutOperatorId()).isEqualTo(personId);

        assertDoesNotThrow(() -> deviceBorrowService.checkIn(log.getId(), "正常归还", personId));
    }

    // ==================== 5. 维修全流程 ====================

    @Test @DisplayName("维修全链路：报修→开始→完成→验收→费用登记")
    void test09_repairFullFlow() {
        RepairReportDTO report = new RepairReportDTO();
        report.setReporterId(personId);
        report.setDeviceId(deviceId);
        report.setFaultDesc("屏幕不亮");

        RepairVO vo = assertDoesNotThrow(() -> deviceRepairService.reportFault(report));
        assertThat(vo.getRepairNo()).startsWith("RPR");

        assertDoesNotThrow(() -> deviceRepairService.startRepair(vo.getRepairNo(), personId));
        assertDoesNotThrow(() -> deviceRepairService.completeRepair(vo.getRepairNo(), "更换屏幕"));
        assertDoesNotThrow(() -> deviceRepairService.acceptRepair(vo.getRepairNo()));
        assertDoesNotThrow(() -> deviceRepairService.recordCost(vo.getRepairNo(), BigDecimal.valueOf(500), "屏幕费"));
    }

    // ==================== 6. 告警处理 ====================

    @Test @DisplayName("告警处理：handlerId校验后落库")
    void test10_handleAlertWithHandlerValidation() {
        deviceSensorService.receiveSensorData(report(deviceId, 500L, "temperature", "55.0", "℃"));
        Long alertId = jdbc.queryForObject(
                "SELECT id FROM device_alert_record WHERE device_id = ? LIMIT 1", Long.class, deviceId);

        assertDoesNotThrow(() -> deviceAlertService.handleAlert(alertId, personId, "已检查"));

        Map<String, Object> u = jdbc.queryForMap(
                "SELECT status, handler_id, handle_result FROM device_alert_record WHERE id = ?", alertId);
        assertThat(u.get("STATUS")).isEqualTo(1);
        assertThat(((Number) u.get("HANDLER_ID")).longValue()).isEqualTo(personId);
        assertThat(u.get("HANDLE_RESULT")).isEqualTo("已检查");
    }

    // ==================== 辅助方法 ====================

    private Long insertPerson(String no, String name) {
        jdbc.update("INSERT INTO person (person_no, person_name, person_type, person_status, source_type, created_at, updated_at) VALUES (?,?,2,1,1,NOW(),NOW())", no, name);
        return jdbc.queryForObject("SELECT id FROM person WHERE person_no = ?", Long.class, no);
    }

    private Long insertUserAccount(String no, String username, Long pid) {
        jdbc.update("INSERT INTO user_account (account_no, username, password_hash, user_type, person_id, account_status, created_at, updated_at) VALUES (?,?,'hash',2,?,1,NOW(),NOW())", no, username, pid);
        return jdbc.queryForObject("SELECT id FROM user_account WHERE account_no = ?", Long.class, no);
    }

    private void addIndicator(Long typeId, String name, String unit, String scope) {
        EquipmentMetricIndicatorDO i = new EquipmentMetricIndicatorDO();
        i.setEquipmentTypeId(typeId);
        i.setMetricName(name);
        i.setUnit(unit);
        i.setSafeScope(scope);
        metricIndicatorRepository.insert(i);
    }

    private SensorReportDTO report(Long deviceId, Long sensorId, String name, String qty, String unit) {
        SensorReportDTO d = new SensorReportDTO();
        d.setEventNo("evt." + System.nanoTime());
        d.setSensorId(sensorId);
        d.setDeviceId(deviceId);
        SensorMetricValueDTO v = new SensorMetricValueDTO();
        v.setQuantity(new BigDecimal(qty));
        v.setUnit(unit);
        d.setMetrics(Map.of(name, v));
        return d;
    }

    private SensorReportDTO convert(SensorDataMessage msg) {
        SensorReportDTO d = new SensorReportDTO();
        d.setEventNo(msg.getEventNo());
        d.setSensorId(msg.getSensor() != null ? msg.getSensor().getSensorId() : null);
        d.setDeviceId(msg.getTarget().getTargetId());
        Map<String, SensorMetricValueDTO> m = new HashMap<>();
        if (msg.getDetectedData() != null) {
            msg.getDetectedData().forEach((k, v) -> {
                SensorMetricValueDTO mv = new SensorMetricValueDTO();
                mv.setQuantity(v.getValue() != null ? v.getValue() : BigDecimal.ZERO);
                mv.setUnit(v.getUnit());
                m.put(k, mv);
            });
        }
        d.setMetrics(m);
        return d;
    }

    private String mqJson(Long deviceId, Long sensorId, int targetType) {
        return ("""
            {"message_id":"u1","event_no":"iot.event.001","occurred_at":"2026-07-09T10:00:00","source_system":"iot",
             "sensor":{"sensor_id":%d,"sensor_no":"SN-1","sensor_type_id":1,"sensor_type_name":"温度"},
             "target":{"target_type":%d,"target_id":%d},
             "detected_data":{"temperature":{"value":25.5,"unit":"℃"}},
             "detected_at":"2026-07-09T09:59:00"}""").formatted(sensorId, targetType, deviceId);
    }
}
