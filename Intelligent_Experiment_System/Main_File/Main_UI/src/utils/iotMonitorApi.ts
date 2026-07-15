import api from './api'
import type { ApiResult, PageResult } from './api'

export interface LabTypeOption {
  id: number
  labTypeName: string
}

export interface LabFilterOptions {
  campuses: string[]
  buildings: string[]
  buildingByCampus: Record<string, string[]>
}

export interface IotMonitorOverviewQuery {
  campus?: string
  building?: string
  labTypeId?: number
  labName?: string
  historyLimit?: number
}

export interface IotMonitorPoint {
  logId: number
  value: number
  status: number
  exceedDirection: string | null
  detectedAt: string
}

export interface IotMonitorIndicator {
  indicatorId: number
  dataName: string
  displayName: string
  unit: string
  safeRange: Array<Array<number | null>>
  latestValue: number | null
  latestStatus: number | null
  latestStatusDesc: string
  exceedDirection: string | null
  detectedAt: string | null
  points: IotMonitorPoint[]
}

export type IotLabMonitorStatus = 'NORMAL' | 'ALERT' | 'NO_SENSOR' | 'NO_DATA'

export interface IotMonitorLabCard {
  labId: number
  labName: string
  labTypeId: number
  labTypeName: string
  campus: string | null
  building: string | null
  floor: number | null
  roomNo: string | null
  monitorStatus: IotLabMonitorStatus
  monitorStatusDesc: string
  boundSensorCount: number
  onlineSensorCount: number
  latestDetectedAt: string | null
  indicators: IotMonitorIndicator[]
}

export interface IotMonitorOverview {
  totalLabs: number
  onlineMonitorLabs: number
  normalLabs: number
  alertLabs: number
  totalSensors: number
  onlineSensors: number
  sensorOnlineRate: number
  updatedAt: string
  labs: IotMonitorLabCard[]
}

export interface IotMonitorLogQuery {
  dataName?: string
  startTime?: string
  endTime?: string
  page?: number
  pageSize?: number
}

export interface IotMonitorLog {
  id: number
  labId: number
  sensorId: number | null
  sensorNo: string | null
  indicatorId: number | null
  dataName: string
  value: number
  unit: string
  safeRange: Array<Array<number | null>>
  status: number
  statusDesc: string
  exceedDirection: string | null
  detectedAt: string
  eventNo: string
  messageId: string | null
  createdAt: string
}

export function getLabFilterOptions(): Promise<LabFilterOptions> {
  return api.get<ApiResult<LabFilterOptions>>('/lab-management/labs/filter-options')
    .then(res => res.data.data)
}

export function getMonitorLabTypes(): Promise<LabTypeOption[]> {
  return api.get<ApiResult<LabTypeOption[]>>('/lab-management/lab-types')
    .then(res => res.data.data)
}

export function getIotMonitorOverview(query: IotMonitorOverviewQuery): Promise<IotMonitorOverview> {
  return api.get<ApiResult<IotMonitorOverview>>('/lab-management/iot-monitor/overview', {
    params: query,
  }).then(res => res.data.data)
}

export function getIotMonitorLogs(
  labId: number,
  query: IotMonitorLogQuery,
): Promise<PageResult<IotMonitorLog>> {
  return api.get<ApiResult<PageResult<IotMonitorLog>>>(`/lab-management/iot-monitor/labs/${labId}/logs`, {
    params: query,
  }).then(res => res.data.data)
}
