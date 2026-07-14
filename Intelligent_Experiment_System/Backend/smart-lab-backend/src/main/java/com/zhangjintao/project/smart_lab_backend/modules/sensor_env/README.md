# 物联网环境子系统（sensor_env）

## 当前实现范围

当前完成模块一”传感器类型与检测数据类型管理”、模块二”传感器管理”、模块三”传感器数据接收与记录”和模块四”数据路由转发”的物联网子系统生产端：

- 传感器类型创建、列表、详情、编辑、删除。
- 检测数据项创建、编辑、删除。
- 传感器类型目录 Caffeine + Redis 二级缓存。
- 删除类型前检查未软删除的传感器实例；无实例时级联删除检测数据项。
- 传感器注册、详情、分页筛选、编辑和软删除。
- 设备/实验室目标绑定、换绑和逻辑解绑。
- 管理员故障标记、解除故障和可配置的离线扫描。
- 供数据接收模块复用的传感器运行快照查询与心跳更新服务。
- 传感器状态变更后通知全部管理员。
- 模拟数据接收（POST /api/iot/sensor-data），含传感器校验、字段名校验、时间校验、事务写入心跳和日志。
- 数据接收事务提交后，通过 RocketMQ 顺序消息按绑定目标路由转发。
- 历史数据分页查询和最新数据查询。

下游消费端、告警判定及阈值管理不属于物联网子系统当前实现范围。

## 接口

除 `POST /api/iot/sensor-data`（开发阶段无需认证）外，其余接口仅允许 `ADMIN` 角色访问。

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/api/iot/sensor-types` | 创建传感器类型 |
| GET | `/api/iot/sensor-types` | 查询全部传感器类型摘要 |
| GET | `/api/iot/sensor-types/{sensorTypeId}` | 查询类型详情及检测数据项 |
| PUT | `/api/iot/sensor-types/{sensorTypeId}` | 编辑传感器类型 |
| DELETE | `/api/iot/sensor-types/{sensorTypeId}` | 删除类型并级联删除数据项 |
| POST | `/api/iot/sensor-types/{sensorTypeId}/data-types` | 添加检测数据项 |
| PUT | `/api/iot/data-types/{dataTypeId}` | 编辑检测数据项 |
| DELETE | `/api/iot/data-types/{dataTypeId}` | 删除检测数据项 |
| POST | `/api/iot/sensors` | 注册传感器并绑定目标 |
| GET | `/api/iot/sensors` | 分页查询传感器 |
| GET | `/api/iot/sensors/{sensorId}` | 查询传感器详情 |
| PUT | `/api/iot/sensors/{sensorId}` | 编辑传感器类型、目标和备注 |
| PATCH | `/api/iot/sensors/{sensorId}/status` | 标记或解除故障 |
| DELETE | `/api/iot/sensors/{sensorId}` | 逻辑解绑并软删除传感器 |
| POST | `/api/iot/sensor-data` | 接收模拟传感器检测数据（无需认证） |
| GET | `/api/iot/sensors/{sensorId}/logs` | 查询传感器历史数据（分页） |
| GET | `/api/iot/sensors/{sensorId}/logs/latest` | 查询传感器最新一条数据 |

## 核心规则

- `sensor_type_name` 全局唯一。
- 同一传感器类型下 `data_name` 唯一。
- `data_name` 强制使用 lower_snake_case，例如 `temperature`、`gas_concentration`。
- 类型有关联的未删除传感器实例时禁止删除。
- 类型无传感器实例时，在同一事务中先删除检测数据项，再删除类型。
- 逻辑外键的并发写入通过父级传感器类型行锁协调。
- `sensorNo` 注册后不可修改，软删除后不可复用。
- 未删除传感器必须绑定且只能绑定一个设备或实验室。
- 目标删除前检查有效绑定，存在绑定时拒绝删除。
- 超过阈值未上报自动离线；离线传感器下一次合法上报恢复正常。
- 故障由管理员标记，故障传感器拒绝数据上报。
- `detectedData` 允许上报已注册字段的子集；未注册字段返回 400。
- 工作日志统一写入 `status=1`（正常）；告警判定不在物联网子系统范围。
- 接收数据接口不返回 `alarmTriggered`——告警处理是下游子系统的职责。
- 数据接收在同一事务中更新心跳并写入日志，任一步失败整体回滚。
- RocketMQ Topic 固定为 `iot-sensor-data`，Tag 使用 `lab_sensor_data` 和 `device_sensor_data`。
- 转发消息使用顺序消息，同一传感器编号 `sensorNo` 作为 sharding key，保证同一传感器消息进入同一队列。
- MQ 消息体 JSON 字段统一使用 camelCase，消息体不增加 `status` 字段。

## 缓存

- Caffeine Key：`all`
- Redis Key：`sensor_env:sensor_type_catalog:all`
- TTL：30 分钟
- 任意类型或数据项写入成功后，在事务提交后清除两级缓存。
- Redis 缓存序列化异常直接抛出，不静默回源数据库。
- 传感器运行快照使用 `sensor_env:sensor_runtime:{sensorNo}`，TTL 为 10 分钟。
- 普通心跳不清理运行快照；目标或状态变化后在事务提交后清理。

## 离线检测配置

开发环境默认关闭：

```yaml
smart-lab:
  iot:
    sensor:
      status-detection-enabled: false
      offline-threshold: 5m
      offline-scan-fixed-delay: 1m
      offline-scan-batch-size: 200
```
