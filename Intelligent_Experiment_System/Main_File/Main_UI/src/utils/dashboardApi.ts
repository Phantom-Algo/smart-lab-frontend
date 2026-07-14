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

export function getDashboardStats(): Promise<DashboardStats> {
  return api.get<ApiResult<DashboardStats>>('/dashboard/stats')
    .then(res => res.data.data)
}
