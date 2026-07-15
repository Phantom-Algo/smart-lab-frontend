import api from './api'
import type { ApiResult } from './api'

export interface SensorReading {
  type: string
  label: string
  value: number
  unit: string
  threshold: number
  status: 'normal' | 'warning' | 'alarm'
  barPct: number
}

export interface LabMonitorData {
  labId: number
  labName: string
  overallStatus: 'normal' | 'warning' | 'alarm'
  updateAgo: number
  sensors: SensorReading[]
}

/** 传感器离线检测：扫描并标记超时未上报的传感器为离线 */
export function detectOfflineSensors(): Promise<number> {
  return api.post<ApiResult<number>>('/iot/sensors/detect-offline')
    .then(res => res.data.data)
}

/** 获取所有实验室的传感器实时监测数据 */
export function getLabMonitorData(): Promise<LabMonitorData[]> {
  return api.get<ApiResult<LabMonitorData[]>>('/iot/labs/monitor')
    .then(res => res.data.data)
}
