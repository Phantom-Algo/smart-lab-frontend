import api from './api'
import type { ApiResult, PageResult } from './api'

/** 设备/环境告警记录 */
export interface DeviceAlertRecord {
  id: number
  deviceId: number
  deviceName: string
  sensorId: number | null
  paramName: string
  actualValue: string
  safeScope: string
  exceedDirection: number        // 1超标 2低于
  alertTime: string
  status: number                 // 0待处理 1已处理
  statusDesc: string
  handlerId: number | null
  handleResult: string | null
  createdAt: string
}

/** 耗材预警记录 */
export interface MaterialAlertRecord {
  id: number
  materialId: number
  materialName: string
  batchId: number | null
  batchNo: string
  alertType: number
  alertTypeDesc: string
  alertContent: string
  status: number                 // 0待处理 1已处理
  statusDesc: string
  handledBy: number | null
  alertTime: string
  handledAt: string | null
  createdAt: string
}

/** 前端统一告警展示模型 */
export interface UnifiedAlert {
  id: string
  alarmNo: string
  alarmType: 'iot' | 'consumable'
  alarmLevel: 'high' | 'normal'
  labName: string
  alarmObject: string
  alarmContent: string
  alarmValue: string
  thresholdValue: string
  alarmTime: string
  handleStatus: 'pending' | 'done'
  handlerName: string | null
  handleResult: string | null
  remark: string | null
}

/** 分页查询设备告警 */
export function getDeviceAlerts(params: {
  pageNum?: number
  pageSize?: number
  status?: number
}): Promise<PageResult<DeviceAlertRecord>> {
  return api.get<ApiResult<PageResult<DeviceAlertRecord>>>('/equipment/alerts', { params })
    .then(res => res.data.data)
}

/** 处理设备告警 */
export function handleDeviceAlert(id: number, result: string): Promise<void> {
  return api.post<ApiResult<null>>(`/equipment/alerts/${id}/handle`, { result })
    .then(() => {})
}

/** 分页查询耗材预警 */
export function getMaterialAlerts(params: {
  pageNum?: number
  pageSize?: number
  status?: number
}): Promise<PageResult<MaterialAlertRecord>> {
  return api.get<ApiResult<PageResult<MaterialAlertRecord>>>('/material/alerts', { params })
    .then(res => res.data.data)
}

/** 处理耗材预警 */
export function handleMaterialAlert(id: number, result?: string): Promise<void> {
  return api.post<ApiResult<null>>(`/material/alerts/${id}/handle`, { result })
    .then(() => {})
}

/** 统一告警查询（合并设备+耗材）+ 分页 */
export async function getUnifiedAlerts(params: {
  pageNum?: number
  pageSize?: number
  type?: 'iot' | 'consumable' | ''
  status?: number
}): Promise<{ records: UnifiedAlert[]; total: number }> {
  const pageNum = params.pageNum || 1
  const pageSize = params.pageSize || 20
  const statusFilter = params.status !== undefined ? params.status : undefined

  const calls: Promise<any>[] = []
  if (!params.type || params.type === 'iot') {
    calls.push(getDeviceAlerts({ pageNum: 1, pageSize: 200, status: statusFilter }).catch(() => ({ records: [], total: 0, pageNum: 1, pageSize: 200, pages: 0 } as PageResult<DeviceAlertRecord>)))
  } else {
    calls.push(Promise.resolve({ records: [], total: 0, pageNum: 1, pageSize: 200, pages: 0 } as PageResult<DeviceAlertRecord>))
  }
  if (!params.type || params.type === 'consumable') {
    calls.push(getMaterialAlerts({ pageNum: 1, pageSize: 200, status: statusFilter }).catch(() => ({ records: [], total: 0, pageNum: 1, pageSize: 200, pages: 0 } as PageResult<MaterialAlertRecord>)))
  } else {
    calls.push(Promise.resolve({ records: [], total: 0, pageNum: 1, pageSize: 200, pages: 0 } as PageResult<MaterialAlertRecord>))
  }

  const [deviceResult, materialResult] = await Promise.all(calls)

  const unified: UnifiedAlert[] = [
    ...deviceResult.records.map((r: DeviceAlertRecord) => deviceToUnified(r)),
    ...materialResult.records.map((r: MaterialAlertRecord) => materialToUnified(r)),
  ]

  // 按时间倒序排列
  unified.sort((a, b) => b.alarmTime.localeCompare(a.alarmTime))

  const total = unified.length
  const start = (pageNum - 1) * pageSize
  const records = unified.slice(start, start + pageSize)

  return { records, total }
}

function deviceToUnified(r: DeviceAlertRecord): UnifiedAlert {
  const isOver = r.exceedDirection === 1
  const isHigh = Math.abs(parseFloat(r.actualValue) - parseFloat(r.safeScope.replace(/[^0-9.]/g, ''))) > 10
  return {
    id: `dev-${r.id}`,
    alarmNo: `ALM-DEV-${String(r.id).padStart(5, '0')}`,
    alarmType: 'iot',
    alarmLevel: isHigh ? 'high' : 'normal',
    labName: r.deviceName ? `${r.deviceName}传感器` : '设备告警',
    alarmObject: r.paramName || '传感器',
    alarmContent: `${r.paramName}${isOver ? '超标' : '低于安全范围'}（${r.safeScope}）`,
    alarmValue: r.actualValue,
    thresholdValue: r.safeScope,
    alarmTime: r.alertTime?.replace('T', ' ').slice(0, 16) || r.createdAt?.replace('T', ' ').slice(0, 16) || '',
    handleStatus: r.status === 0 ? 'pending' : 'done',
    handlerName: r.handlerId ? String(r.handlerId) : null,
    handleResult: r.handleResult || null,
    remark: null,
  }
}

function materialToUnified(r: MaterialAlertRecord): UnifiedAlert {
  return {
    id: `mat-${r.id}`,
    alarmNo: `ALM-MAT-${String(r.id).padStart(5, '0')}`,
    alarmType: 'consumable',
    alarmLevel: r.alertType === 3 ? 'high' : 'normal',
    labName: r.materialName || '耗材',
    alarmObject: r.batchNo ? `${r.materialName} (${r.batchNo})` : r.materialName,
    alarmContent: r.alertContent,
    alarmValue: r.alertTypeDesc,
    thresholdValue: '',
    alarmTime: r.alertTime?.replace('T', ' ').slice(0, 16) || '',
    handleStatus: r.status === 0 ? 'pending' : 'done',
    handlerName: r.handledBy ? String(r.handledBy) : null,
    handleResult: null,
    remark: null,
  }
}
