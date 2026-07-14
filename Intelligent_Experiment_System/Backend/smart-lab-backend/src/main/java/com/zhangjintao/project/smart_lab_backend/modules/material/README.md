# 物资管理子系统

> **包路径**：`com.zhangjintao.project.smart_lab_backend.modules.material`
> **版本**：V1.0
> **负责人**：林伯炜

## 模块概述

物资管理子系统负责实验室耗材与药剂的全生命周期管理，覆盖物资从采购入库、批次追踪、库存监控、领用审批出库到报废处置的全链路流转。

## 目录结构

```
material/
├── controller/          # 控制器层 (MaterialController, ReceiveController, AlertController, BatchController)
├── service/             # 服务层接口
│   └── impl/            # 服务层实现
├── dao/                 # 数据访问层
│   ├── dataobject/      # DO：7个表映射对象
│   ├── mapper/          # Mapper：7个MyBatis-Plus接口
│   └── repository/      # Repository：7接口+7实现
├── model/               # 数据模型
│   ├── request/         # 请求对象
│   ├── response/        # 响应对象
│   ├── dto/             # 数据传输对象
│   └── converter/       # 数据转换器
├── domain/              # 领域模型
│   ├── entity/          # 领域实体
│   └── value/           # 值对象
├── enums/               # 枚举类
├── constants/           # 常量类
├── exception/           # 异常类 + ErrorCode
├── utils/               # 工具类
├── config/              # 配置类
└── README.md            # 本文件
```

## 数据库表（7张）

| 表名 | 说明 |
|------|------|
| material | 物资表 |
| material_batch | 物资批次表 |
| material_stock_in | 入库记录表 |
| material_receive_apply | 领用申请表 |
| material_receive_log | 领用出库日志表 |
| material_receive_batch_detail | 出库批次扣减明细表 |
| material_alert_record | 预警记录表 |

## 跨模块依赖

- **auth_profile**：CurrentUserQueryService（获取当前用户personId）
- **personnel_management**：PersonDirectoryPort（人员校验 + personId→userId映射）
- **message_inbox**：MessagePublisher（消息通知推送）

## 相关文档

- PRD：`.ga/docs/prd/物资管理系统功能需求设计.md`
- TDD：`.ga/docs/tdd/物资管理系统技术实现设计.md`
- Flyway：`src/main/resources/db/migration/V20260710150000__create_material_schema.sql`
