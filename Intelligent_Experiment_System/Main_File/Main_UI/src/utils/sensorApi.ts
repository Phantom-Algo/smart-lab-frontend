import api from './api'
import type { ApiResult, PageResult } from './api'

export const SENSOR_TARGET_LAB = 2

export type SensorRunningStatus = 1 | 2 | 3

export interface SensorDataTypeItem {
  id: number
  sensorTypeId: number
  dataName: string
  unit: string
  createdAt?: string
  updatedAt?: string
}

export interface SensorTypeItem {
  id: number
  sensorTypeName: string
  description: string | null
  dataTypeCount: number
  createdAt?: string
  updatedAt?: string
}

export interface SensorTypeDetail extends Omit<SensorTypeItem, 'dataTypeCount'> {
  dataTypes: SensorDataTypeItem[]
}

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

export interface SensorItem {
  id: number
  sensorNo: string
  sensorTypeId: number
  sensorTypeName: string
  targetType: number
  targetTypeDescription: string
  targetId: number
  runningStatus: SensorRunningStatus
  runningStatusDescription: string
  lastReportAt: string | null
  remark: string | null
  createdAt: string
  updatedAt: string
}

export interface SensorPageQuery {
  page?: number
  pageNum?: number
  pageSize?: number
  keyword?: string
  sensorTypeId?: number
  targetType?: number
  runningStatus?: SensorRunningStatus
  sortField?: string
  sortOrder?: 'asc' | 'desc'
}

export interface CreateSensorTypePayload {
  sensorTypeName: string
  description?: string | null
}

export interface UpdateSensorTypePayload {
  sensorTypeName: string
  description?: string | null
}

export interface CreateSensorDataTypePayload {
  dataName: string
  unit: string
}

export interface UpdateSensorDataTypePayload {
  dataName: string
  unit: string
}

export interface CreateSensorPayload {
  sensorNo: string
  sensorTypeId: number
  targetType: number
  targetId: number
  remark?: string | null
}

export interface UpdateSensorPayload {
  sensorTypeId: number
  targetType: number
  targetId: number
  remark?: string | null
}

export interface UpdateSensorStatusPayload {
  runningStatus: SensorRunningStatus
  reason?: string | null
}

export function listSensorTypes(): Promise<SensorTypeItem[]> {
  return api.get<ApiResult<SensorTypeItem[]>>('/iot/sensor-types')
    .then(res => res.data.data)
}

export function getSensorTypeDetail(sensorTypeId: number): Promise<SensorTypeDetail> {
  return api.get<ApiResult<SensorTypeDetail>>(`/iot/sensor-types/${sensorTypeId}`)
    .then(res => res.data.data)
}

export function createSensorType(data: CreateSensorTypePayload): Promise<SensorTypeDetail> {
  return api.post<ApiResult<SensorTypeDetail>>('/iot/sensor-types', data)
    .then(res => res.data.data)
}

export function updateSensorType(sensorTypeId: number, data: UpdateSensorTypePayload): Promise<SensorTypeDetail> {
  return api.put<ApiResult<SensorTypeDetail>>(`/iot/sensor-types/${sensorTypeId}`, data)
    .then(res => res.data.data)
}

export function deleteSensorType(sensorTypeId: number): Promise<void> {
  return api.delete(`/iot/sensor-types/${sensorTypeId}`).then(() => {})
}

export function createSensorDataType(
  sensorTypeId: number,
  data: CreateSensorDataTypePayload,
): Promise<SensorDataTypeItem> {
  return api.post<ApiResult<SensorDataTypeItem>>(`/iot/sensor-types/${sensorTypeId}/data-types`, data)
    .then(res => res.data.data)
}

export function updateSensorDataType(
  dataTypeId: number,
  data: UpdateSensorDataTypePayload,
): Promise<SensorDataTypeItem> {
  return api.put<ApiResult<SensorDataTypeItem>>(`/iot/data-types/${dataTypeId}`, data)
    .then(res => res.data.data)
}

export function deleteSensorDataType(dataTypeId: number): Promise<void> {
  return api.delete(`/iot/data-types/${dataTypeId}`).then(() => {})
}

export function getSensorPage(query: SensorPageQuery = {}): Promise<PageResult<SensorItem>> {
  return api.get<ApiResult<PageResult<SensorItem>>>('/iot/sensors', {
    params: {
      ...query,
      page: query.page ?? query.pageNum,
    },
  })
    .then(res => res.data.data)
}

export function getSensorDetail(sensorId: number): Promise<SensorItem> {
  return api.get<ApiResult<SensorItem>>(`/iot/sensors/${sensorId}`)
    .then(res => res.data.data)
}

export function createSensor(data: CreateSensorPayload): Promise<SensorItem> {
  return api.post<ApiResult<SensorItem>>('/iot/sensors', data)
    .then(res => res.data.data)
}

export function updateSensor(sensorId: number, data: UpdateSensorPayload): Promise<SensorItem> {
  return api.put<ApiResult<SensorItem>>(`/iot/sensors/${sensorId}`, data)
    .then(res => res.data.data)
}

export function updateSensorStatus(
  sensorId: number,
  data: UpdateSensorStatusPayload,
): Promise<SensorItem> {
  return api.patch<ApiResult<SensorItem>>(`/iot/sensors/${sensorId}/status`, data)
    .then(res => res.data.data)
}

export function deleteSensor(sensorId: number): Promise<void> {
  return api.delete(`/iot/sensors/${sensorId}`).then(() => {})
}
