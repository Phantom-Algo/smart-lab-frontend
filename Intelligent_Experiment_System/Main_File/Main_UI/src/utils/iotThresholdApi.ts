import api from './api'
import type { ApiResult, PageResult } from './api'

export interface ThresholdLabItem {
  id: number
  labName: string
  labTypeId: number
  labTypeName: string
}

export interface DetectionIndicatorItem {
  id: number
  labTypeId: number
  indicatorName: string
  unit: string
  safeRange: Array<Array<number | null>>
  createdAt?: string
  updatedAt?: string
}

export interface DetectionIndicatorPayload {
  labTypeId?: number
  indicatorName: string
  unit: string
  safeRange: Array<Array<number | null>>
}

export interface SensorDataTypeItem {
  id: number
  sensorTypeId: number
  dataName: string
  unit: string
}

export interface LabSensorBindingItem {
  bindingId: number
  labId: number
  sensorId: number
  sensorNo: string
  sensorTypeId: number
  sensorTypeName: string
  bindStatus: number
  bindStatusDesc: string
  boundAt: string | null
  unboundAt: string | null
}

export interface AvailableLabSensorItem {
  sensorId: number
  sensorNo: string
  sensorTypeId: number
  sensorTypeName: string
  dataTypes: SensorDataTypeItem[]
}

export function searchThresholdLabs(keyword = ''): Promise<PageResult<ThresholdLabItem>> {
  return api.get<ApiResult<PageResult<ThresholdLabItem>>>('/lab-management/labs', {
    params: {
      keyword,
      page: 1,
      pageSize: 10,
      sortField: 'lab_name',
      sortOrder: 'asc',
    },
  }).then(res => res.data.data)
}

export function getDetectionIndicators(labTypeId: number): Promise<DetectionIndicatorItem[]> {
  return api.get<ApiResult<DetectionIndicatorItem[]>>(`/lab-management/lab-types/${labTypeId}/indicators`)
    .then(res => res.data.data)
}

export function createDetectionIndicator(data: DetectionIndicatorPayload): Promise<DetectionIndicatorItem> {
  return api.post<ApiResult<DetectionIndicatorItem>>('/lab-management/detection-indicators', data)
    .then(res => res.data.data)
}

export function updateDetectionIndicator(
  indicatorId: number,
  data: DetectionIndicatorPayload,
): Promise<DetectionIndicatorItem> {
  return api.put<ApiResult<DetectionIndicatorItem>>(`/lab-management/detection-indicators/${indicatorId}`, data)
    .then(res => res.data.data)
}

export function deleteDetectionIndicator(indicatorId: number): Promise<void> {
  return api.delete(`/lab-management/detection-indicators/${indicatorId}`).then(() => {})
}

export function clearDetectionIndicators(labTypeId: number): Promise<void> {
  return api.delete(`/lab-management/lab-types/${labTypeId}/indicators`).then(() => {})
}

export function getLabSensorBindings(labId: number): Promise<LabSensorBindingItem[]> {
  return api.get<ApiResult<LabSensorBindingItem[]>>(`/lab-management/labs/${labId}/sensor-bindings`)
    .then(res => res.data.data)
}

export function searchAvailableLabSensors(
  labId: number,
  keyword = '',
): Promise<PageResult<AvailableLabSensorItem>> {
  return api.get<ApiResult<PageResult<AvailableLabSensorItem>>>(`/lab-management/labs/${labId}/available-sensors`, {
    params: {
      keyword,
      page: 1,
      pageSize: 20,
    },
  }).then(res => res.data.data)
}

export function bindLabSensor(labId: number, sensorId: number): Promise<void> {
  return api.post(`/lab-management/labs/${labId}/sensor-bindings`, { sensorId }).then(() => {})
}

export function unbindLabSensor(labId: number, sensorId: number): Promise<void> {
  return api.delete(`/lab-management/labs/${labId}/sensor-bindings/${sensorId}`).then(() => {})
}
