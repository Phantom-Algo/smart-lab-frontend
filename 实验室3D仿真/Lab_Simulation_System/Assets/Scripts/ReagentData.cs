using System;
using System.Collections.Generic;
using UnityEngine;

// ============================================================
//  试剂 / 药材数据模型
// ============================================================

[Serializable]
public class ReagentData
{
    public int id;
    public string name;             // 试剂名称，如 "氯化钠"
    public string casNo;            // CAS 编号，如 "7647-14-5"
    public string spec;             // 规格，如 "AR 分析纯 / 500g"
    public string unit;             // 计量单位，如 "g" / "mL"
    public string category;         // 类别，如 "无机盐" / "有机溶剂"
    public float currentStock;      // 当前库存量
    public float safetyStock;       // 安全库存线（低于此值触发预警）
    public string location;         // 存放位置，如 "A-3-1 号柜"
    public string expireDateStr;    // 保质期（字符串，格式 yyyy-MM-dd）
    public int safetyLevel;         // 安全等级：1=普通 2=注意 3=危险

    // 派生属性（运行时计算，不参与序列化）
    [NonSerialized] public List<ReagentBatchData> batches;

    public bool IsLowStock => currentStock <= safetyStock;

    public int DaysUntilExpiry
    {
        get
        {
            if (DateTime.TryParse(expireDateStr, out DateTime expiry))
                return (expiry - DateTime.Today).Days;
            return int.MaxValue;
        }
    }

    public bool IsExpired => DaysUntilExpiry < 0;
    public bool IsExpiringSoon => !IsExpired && DaysUntilExpiry <= 30;

    /// <summary>状态标签（用于 UI 显示）</summary>
    public string StatusLabel
    {
        get
        {
            if (IsExpired)  return "已过期";
            if (IsLowStock) return "低库存";
            if (IsExpiringSoon) return "即将过期";
            return "正常";
        }
    }

    /// <summary>状态颜色 Hex（与 need-doc DS-002 颜色规范一致）</summary>
    public string StatusColor
    {
        get
        {
            if (IsExpired)  return "#E74C3C";
            if (IsLowStock) return "#F39C12";
            if (IsExpiringSoon) return "#F39C12";
            return "#27AE60";
        }
    }

    public string SafetyLevelLabel => safetyLevel switch
    {
        3 => "⚠ 危险",
        2 => "注意",
        _ => "普通"
    };
}

[Serializable]
public class ReagentBatchData
{
    public int id;
    public int reagentId;
    public string batchNo;          // 批次号，如 "B001"
    public string entryDateStr;     // 入库日期
    public float entryQuantity;     // 入库数量
    public float remainQuantity;    // 剩余数量
    public string status;           // "正常" / "已耗尽" / "过期"
}

// ============================================================
//  试剂领用申请数据模型
// ============================================================

[Serializable]
public class ReagentPickupRequest
{
    public int reagentId;
    public string reagentName;
    public float quantity;          // 领用数量
    public string unit;
    public string applicantName;
    public string pickupDateStr;
    public string purpose;
}

[Serializable]
public class ReagentPickupRecord
{
    public int id;
    public int reagentId;
    public string reagentName;
    public float quantity;
    public string unit;
    public string applicantName;
    public string pickupTimeStr;
    public string purpose;
    public string status;    // 审批中 / 已领用 / 已完成

    public string StatusColor => status switch
    {
        "审批中" => "#F39C12",
        "已完成" => "#27AE60",
        _ => "#5B9BD5"
    };
}

// ============================================================
//  设备数据模型（对应数据库 device 表）
// ============================================================

[Serializable]
public class DeviceData
{
    public int id;
    public string deviceNo;         // 设备编号，如 "EQ-2025-0042"
    public string name;             // 设备名称
    public string spec;             // 规格参数，如 "1000X / 双目 / LED光源"
    public string category;         // 设备类别，如 "光学仪器"
    public int labId;
    public string labName;          // 所属实验室名称
    public string purchaseDateStr;  // 购入日期
    public float purchasePrice;     // 购置价格
    public string expireDateStr;    // 保修到期日
    public string deviceStatus;     // 设备状态：闲置/借用中/维修中/报废/采购待入库
    public int healthScore;         // 健康分 0-100
    public string remark;

    // IoT 实时数据（来自 device_status_snapshot）
    [NonSerialized] public DeviceSensorData sensorData;

    public string StatusColor => deviceStatus switch
    {
        "闲置"     => "#27AE60",
        "借用中"   => "#5B9BD5",
        "维修中"   => "#F39C12",
        "报废"     => "#E74C3C",
        _          => "#7A8394"
    };

    public int DaysUntilExpiry
    {
        get
        {
            if (DateTime.TryParse(expireDateStr, out DateTime expiry))
                return (expiry - DateTime.Today).Days;
            return int.MaxValue;
        }
    }

    public bool IsWarrantyExpired => DaysUntilExpiry < 0;

    public string HealthLabel
    {
        get
        {
            if (healthScore >= 80) return "良好";
            if (healthScore >= 50) return "一般";
            return "较差";
        }
    }

    public string HealthColor
    {
        get
        {
            if (healthScore >= 80) return "#27AE60";
            if (healthScore >= 50) return "#F39C12";
            return "#E74C3C";
        }
    }
}

// ============================================================
//  设备借用申请数据模型（对应 device_borrow_apply 表）
// ============================================================

[Serializable]
public class DeviceBorrowRequest
{
    public int deviceId;
    public string deviceName;
    public string applicantName;
    public string expectedStartStr;
    public string expectedReturnStr;
    public string purpose;
}

// ============================================================
//  设备借用记录（对应 device_borrow_log 表）
// ============================================================

[Serializable]
public class DeviceBorrowRecord
{
    public int id;
    public string deviceName;
    public string borrowerName;
    public string actualOutTimeStr;
    public string actualReturnTimeStr;
    public int status;  // 1=借出中 2=已归还 3=逾期未还
    public string statusLabel;
    public string statusColor;

    public string StatusLabel => !string.IsNullOrEmpty(statusLabel) ? statusLabel : status switch
    {
        1 => "借出中",
        2 => "已归还",
        3 => "逾期未还",
        _ => "未知"
    };

    public string StatusColor => !string.IsNullOrEmpty(statusColor) ? statusColor : status switch
    {
        1 => "#5B9BD5",
        2 => "#27AE60",
        3 => "#E74C3C",
        _ => "#7A8394"
    };
}

[Serializable]
public class DeviceRepairRecord
{
    public int id;
    public int deviceId;
    public string deviceName;
    public string reportTimeStr;
    public string description;
    public string handlerName;
    public string repairStatus;   // 待处理 / 维修中 / 已修复

    public string StatusColor => repairStatus switch
    {
        "待处理" => "#F39C12",
        "已修复" => "#27AE60",
        _ => "#5B9BD5"
    };
}

// ============================================================
//  设备传感器实时数据（对应 DS-002 IoT 数据结构）
// ============================================================

[Serializable]
public class DeviceSensorData
{
    public int deviceId;
    public float temperature;   // 温度 °C
    public float humidity;      // 湿度 %
    public float battery;       // 电量 %
    public float signalStrength;// 信号强度 %
    public string lastReportStr;
    public string status;       // "normal" / "warning" / "alarm"
}

// ============================================================
//  实验室环境 IoT 数据（对应需求文档 DS-002）
// ============================================================

[Serializable]
public class LabIoTData
{
    public int labId;
    public float temperature;   // 温度 °C
    public float humidity;      // 湿度 %
    public float smoke;         // 烟雾浓度 ppm
    public float gas;           // 气体浓度 ppm
    public float battery;       // 网关/设备平均电量 %
    public float signalStrength;// 网络信号强度 %
    public string lastUpdateStr;// 最后更新时间
    public string status;       // "normal" / "warning" / "alarm"

    public string TemperatureStatus
    {
        get
        {
            if (temperature > 40f) return "alarm";
            if (temperature > 30f) return "warning";
            return "normal";
        }
    }

    public string HumidityStatus
    {
        get
        {
            if (humidity < 30f || humidity > 70f) return "warning";
            return "normal";
        }
    }

    public string BatteryStatus
    {
        get
        {
            if (battery < 20f) return "alarm";
            if (battery < 50f) return "warning";
            return "normal";
        }
    }

    public string SignalStatus
    {
        get
        {
            if (signalStrength < 25f) return "alarm";
            if (signalStrength < 60f) return "warning";
            return "normal";
        }
    }

    public string SignalStrengthLabel
    {
        get
        {
            if (signalStrength >= 75f) return "强";
            if (signalStrength >= 45f) return "中";
            return "弱";
        }
    }

    public string StatusColor => status switch
    {
        "alarm"   => "#E74C3C",
        "warning" => "#F39C12",
        _         => "#27AE60"
    };
}

// ============================================================
//  告警记录（用于监控面板展示）
// ============================================================

[Serializable]
public class AlertRecord
{
    public string timeStr;
    public string deviceName;
    public string message;
    public string level;    // "alarm" / "warning" / "resolved"

    public string LevelColor => level switch
    {
        "alarm"    => "#E74C3C",
        "warning"  => "#F39C12",
        "resolved" => "#27AE60",
        _          => "#7A8394"
    };

    public string LevelIcon => level switch
    {
        "alarm"    => "●",
        "warning"  => "▲",
        "resolved" => "✓",
        _          => "○"
    };
}
