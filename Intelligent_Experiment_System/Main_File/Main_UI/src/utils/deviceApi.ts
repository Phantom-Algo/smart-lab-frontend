import api from './api'
import type { ApiResult, PageResult } from './api'

// ==================== 设备台账 ====================

/** 设备状态：1采购待入库 2闲置 3借用中 4维修中 5报废 */
export type DeviceStatus = 1 | 2 | 3 | 4 | 5

export interface DeviceListItem {
  id: number
  deviceNo: string
  name: string
  spec: Record<string, unknown> | null
  labId: number
  labName: string
  purchaseDate: string | null
  purchasePrice: number | null
  expireDate: string | null
  deviceStatus: DeviceStatus
  deviceStatusName: string
  healthScore: number
  sensorId: number | null
  remark: string | null
  createdAt: string
  updatedAt: string
}

export interface DeviceDetail extends DeviceListItem {
  // 扩展：借用/维修历史摘要
  recentLoans?: { applyNo: string; applicantName: string; startTime: string; endTime: string; status: string }[]
  recentRepairs?: { repairNo: string; cost: number; reportTime: string; status: string }[]
}

export interface DevicePageQuery {
  pageNum?: number
  pageSize?: number
  deviceNo?: string
  name?: string
  deviceStatus?: DeviceStatus
  labId?: number
}

export interface CreateDeviceRequest {
  deviceNo: string
  name: string
  spec?: Record<string, unknown>
  labId: number
  purchaseDate?: string
  purchasePrice?: number
  expireDate?: string
  remark?: string
}

export interface UpdateDeviceRequest {
  id: number
  name?: string
  spec?: Record<string, unknown>
  labId?: number
  purchaseDate?: string
  purchasePrice?: number
  expireDate?: string
  deviceStatus?: DeviceStatus
  remark?: string
}

// 分页查询设备
export function getDevicePage(query: DevicePageQuery): Promise<PageResult<DeviceListItem>> {
  return api.get<ApiResult<PageResult<DeviceListItem>>>('/equipment/devices', { params: query })
    .then(res => res.data.data)
}

// 查询设备详情
export function getDeviceDetail(id: number): Promise<DeviceDetail> {
  return api.get<ApiResult<DeviceDetail>>(`/equipment/devices/${id}`)
    .then(res => res.data.data)
}

// 新增设备
export function createDevice(data: CreateDeviceRequest): Promise<number> {
  return api.post<ApiResult<number>>('/equipment/devices', data)
    .then(res => res.data.data)
}

// 编辑设备
export function updateDevice(data: UpdateDeviceRequest): Promise<void> {
  return api.put<ApiResult<null>>(`/equipment/devices/${data.id}`, data).then(() => {})
}

// ==================== 设备借用审批 ====================

export type BorrowStatus = 'pending' | 'approved' | 'rejected' | 'cancelled'

export interface BorrowApplyItem {
  id: number
  applyNo: string
  deviceId: number
  deviceName: string
  deviceNo: string
  applicantId: number
  applicantName: string
  applicantType: number       // 1学生 2教师
  applicantTypeName: string
  expectedStartTime: string
  expectedReturnTime: string
  status: BorrowStatus
  statusName: string
  rejectReason?: string
  pickupHint?: string
  createdAt: string
  approvedAt?: string
}

export interface BorrowApprovalRequest {
  applyNo: string
  action: 'approve' | 'reject'
  rejectReason?: string
}

export interface BorrowPageQuery {
  pageNum?: number
  pageSize?: number
  status?: BorrowStatus
  applyStatus?: number
  applyNo?: string
}

// 分页查询借用申请
export function getBorrowApplies(query: BorrowPageQuery): Promise<PageResult<BorrowApplyItem>> {
  return api.get<ApiResult<PageResult<BorrowApplyItem>>>('/equipment/borrow-applies', { params: query })
    .then(res => res.data.data)
}

// 审批借用申请
export function approveBorrowApply(data: BorrowApprovalRequest): Promise<void> {
  return api.post<ApiResult<null>>(`/equipment/borrows/${data.applyNo}/approve`, {
    approved: data.action === 'approve',
    rejectReason: data.rejectReason,
  }).then(() => {})
}

// ==================== 设备维修管理 ====================

export type RepairStatus = 'pending' | 'processing' | 'fixed' | 'accepted' | 'cancelled'

export interface RepairItem {
  id: number
  repairNo: string
  deviceId: number
  deviceName: string
  deviceNo: string
  labName: string
  reporterName: string
  reporterType: number
  reporterTypeName: string
  handlerName: string | null
  faultDescription: string
  reportTime: string
  status: RepairStatus
  statusName: string
  totalCost: number
  costItems: { amount: number; remark: string; createdAt: string }[]
  createdAt: string
  updatedAt: string
}

export interface RepairPageQuery {
  pageNum?: number
  pageSize?: number
  status?: RepairStatus
  repairNo?: string
}

export interface UpdateRepairRequest {
  repairNo: string
  action: 'start' | 'add-cost' | 'mark-fixed' | 'accept' | 'cancel'
  costAmount?: number
  costRemark?: string
}

export interface CreateRepairRequest {
  deviceId: number
  reporterName: string
  faultDescription: string
}

// 分页查询维修记录
export function getRepairPage(query: RepairPageQuery): Promise<PageResult<RepairItem>> {
  return api.get<ApiResult<PageResult<RepairItem>>>('/equipment/repairs', { params: query })
    .then(res => res.data.data)
}

// 更新维修状态
export function updateRepairStatus(data: UpdateRepairRequest): Promise<void> {
  if (data.action === 'start') {
    return api.post<ApiResult<null>>(`/equipment/repairs/${data.repairNo}/start`, {}).then(() => {})
  }
  if (data.action === 'mark-fixed') {
    return api.post<ApiResult<null>>(`/equipment/repairs/${data.repairNo}/complete`, {
      repairDesc: data.costRemark || '维修完成',
    }).then(() => {})
  }
  if (data.action === 'accept') {
    return api.post<ApiResult<null>>(`/equipment/repairs/${data.repairNo}/accept`).then(() => {})
  }
  if (data.action === 'add-cost') {
    return api.post<ApiResult<null>>(`/equipment/repairs/${data.repairNo}/cost`, {
      cost: data.costAmount,
      remark: data.costRemark,
    }).then(() => {})
  }
  return Promise.reject(new Error('后端暂不支持取消维修单'))
}

// 管理员登记维修
export function createRepair(data: CreateRepairRequest): Promise<number> {
  return api.post<ApiResult<number>>('/equipment/repairs', data).then(res => res.data.data)
}
