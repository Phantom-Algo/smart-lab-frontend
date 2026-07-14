import api from './api'
import type { ApiResult, PageResult } from './api'

export interface SensorItem {
  id: number
  sensorNo: string
  sensorTypeName: string
  targetType: number
  targetId: number
  runningStatus: number
  runningStatusName: string
  lastReportAt: string | null
  remark: string | null
}

export function getSensorPage(query: { pageNum?: number; pageSize?: number; runningStatus?: number } = {}): Promise<PageResult<SensorItem>> {
  return api.get<ApiResult<PageResult<SensorItem>>>('/iot/sensors', { params: query })
    .then(res => res.data.data)
}
