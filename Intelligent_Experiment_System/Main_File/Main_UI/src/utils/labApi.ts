import api from './api'
import type { ApiResult, PageResult } from './api'

// ==================== 实验室场地管理 ====================

export type LabStatus = 1 | 2 | 3  // 1开放中 2维护中 3已关闭

export interface LabListItem {
  id: number
  labCode: string
  labName: string
  labTypeId: number
  labTypeName: string
  campus: string
  building: string
  floor: string
  roomNo: string
  capacity: number
  areaM2: number
  dutyUserId: number | null
  dutyUserName: string
  runningStatus: LabStatus
  runningStatusName: string
  description: string | null
  createdAt: string
  updatedAt: string
}

export interface LabPageQuery {
  pageNum?: number
  page?: number
  pageSize?: number
  labName?: string
  keyword?: string
  labTypeId?: number
  building?: string
  runningStatus?: LabStatus
}

export interface CreateLabRequest {
  labCode: string
  labName: string
  labTypeId: number
  campus?: string
  building?: string
  floor?: string
  roomNo?: string
  capacity?: number
  areaM2?: number
  dutyUserId?: number
  runningStatus?: LabStatus
  description?: string
}

export interface UpdateLabRequest {
  id: number
  labName?: string
  labTypeId?: number
  campus?: string
  building?: string
  floor?: string
  roomNo?: string
  capacity?: number
  areaM2?: number
  dutyUserId?: number
  runningStatus?: LabStatus
  description?: string
}

// 分页查询实验室
export function getLabPage(query: LabPageQuery): Promise<PageResult<LabListItem>> {
  return api.get<ApiResult<PageResult<LabListItem>>>('/lab-management/labs', {
    params: {
      ...query,
      page: query.page ?? query.pageNum,
      keyword: query.keyword ?? query.labName,
    },
  })
    .then(res => res.data.data)
}

// 查询实验室详情
export function getLabDetail(id: number): Promise<LabListItem> {
  return api.get<ApiResult<LabListItem>>(`/lab-management/labs/${id}`)
    .then(res => res.data.data)
}

// 新增实验室
export function createLab(data: CreateLabRequest): Promise<number> {
  return api.post<ApiResult<number>>('/lab-management/labs', data).then(res => res.data.data)
}

// 编辑实验室
export function updateLab(data: UpdateLabRequest): Promise<void> {
  return api.put<ApiResult<null>>(`/lab-management/labs/${data.id}`, data).then(() => {})
}

// ==================== 实验室预约审核 ====================

export type ReservationStatus = 'pending' | 'approved' | 'rejected' | 'cancelled'

export interface ReservationItem {
  id: number
  applyNo: string
  labId: number
  labName: string
  labCode: string
  applicantId: number
  applicantName: string
  applicantType: number
  applicantTypeName: string
  reserveDate: string
  timeSlot: string
  peopleCount: number
  purpose: string
  purposeTag: 'teaching' | 'research' | 'exam' | 'other'
  purposeTagName: string
  status: ReservationStatus
  statusName: string
  rejectReason?: string
  createdAt: string
  approvedAt?: string
}

export interface ReservationPageQuery {
  pageNum?: number
  page?: number
  pageSize?: number
  status?: ReservationStatus
  applyNo?: string
}

export interface ReservationApprovalRequest {
  reservationNo: string
  action: 'approve' | 'reject'
  rejectReason?: string
}

// 分页查询预约申请
export function getReservationPage(query: ReservationPageQuery): Promise<PageResult<ReservationItem>> {
  return api.get<ApiResult<PageResult<ReservationItem>>>('/lab-management/reservations/pending', {
    params: {
      ...query,
      page: query.page ?? query.pageNum,
    },
  })
    .then(res => res.data.data)
}

// 审批预约申请
export function approveReservation(data: ReservationApprovalRequest): Promise<void> {
  return api.patch<ApiResult<null>>(`/lab-management/reservations/${data.reservationNo}/approve`, {
    approved: data.action === 'approve',
    approveComment: data.rejectReason,
  }).then(() => {})
}
