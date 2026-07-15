import api from './api'
import type { ApiResult } from './api'

export interface DashboardStats {
  totalPersons: number
  activeDevices: number
  pendingApprovals: number
  alarms: number
  studentCount: number
  teacherCount: number
  adminCount: number
  departmentCount: number
  labCount: number
  deviceCount: number
  monthBorrowCount: number
  monthReservationCount: number
  pendingItems: PendingItem[]
  // 效能看板
  labUsageList: { name: string; count: number }[]
  deviceStatusBreakdown: { status: number; count: number }[]
  monthlyBorrowTrend: { month: string; count: number }[]
  monthlyReservationTrend: { month: string; count: number }[]
  totalDeviceCount: number
  borrowedDeviceCount: number
  repairDeviceCount: number
  totalRepairCount: number
}

export interface PendingItem {
  type: string
  title: string
  time: string
  level: string
  navKey: string
  navName: string
}

export interface DeviceUsageItem {
  deviceId: number
  deviceName: string
  deviceNo: string
  borrowCount: number
  usageRate: number
}

export function getDashboardStats(): Promise<DashboardStats> {
  return api.get<ApiResult<DashboardStats>>('/equipment/stats/dashboard')
    .then(res => res.data.data)
}

/** 获取设备使用率 TOP5 */
export function getDeviceUsageTop(): Promise<DeviceUsageItem[]> {
  return api.get<ApiResult<DeviceUsageItem[]>>('/equipment/stats/device-usage-top')
    .then(res => res.data.data)
}

/** 维修成本统计 */
export interface CostStatsVo {
  totalRepairs: number
  totalCost: number
  avgCost: number
  maxCost: number
}

/** 获取维修成本统计（按时间段） */
export function getCostStats(start: string, end: string, labId?: number): Promise<CostStatsVo> {
  return api.get<ApiResult<CostStatsVo>>('/equipment/stats/cost', {
    params: { start, end, labId }
  }).then(res => res.data.data)
}
