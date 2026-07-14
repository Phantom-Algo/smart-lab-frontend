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

export type ReservationStatus = 'pending' | 'approved' | 'rejected' | 'cancelled' | 'checkedIn' | 'absent'
export type ReservationPurposeTag = 'teaching' | 'research' | 'exam' | 'other'

const STATUS_CODE_TO_KEY: Record<number, ReservationStatus> = {
  1: 'pending',
  2: 'approved',
  3: 'rejected',
  4: 'cancelled',
  5: 'checkedIn',
  6: 'absent',
}

const STATUS_KEY_TO_CODE: Record<ReservationStatus, number> = {
  pending: 1,
  approved: 2,
  rejected: 3,
  cancelled: 4,
  checkedIn: 5,
  absent: 6,
}

const STATUS_LABELS: Record<ReservationStatus, string> = {
  pending: '待审核',
  approved: '已通过',
  rejected: '已驳回',
  cancelled: '已取消',
  checkedIn: '已签到',
  absent: '已爽约',
}

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
  purposeTag: ReservationPurposeTag
  purposeTagName: string
  status: ReservationStatus
  statusName: string
  rejectReason?: string
  createdAt: string
  approvedAt?: string
}

export interface ReservationStats {
  pendingCount: number
  statusCounts: Partial<Record<ReservationStatus, number>>
  todayApproved: number
  monthReservations: number
  approvalRate: number
}

interface ApiReservationResponse {
  id?: number
  reservationNo?: string
  labId?: number
  labName?: string
  applicantId?: number
  applicantName?: string
  applicantType?: number
  applicantTypeDesc?: string
  reservationDate?: string
  startTime?: string
  endTime?: string
  purpose?: string
  courseName?: string
  reservationStatus?: number
  reservationStatusDesc?: string
  approveTime?: string
  approveComment?: string
  cancelReason?: string
  createdAt?: string
}

interface ApiReservationStatsResponse {
  pendingCount?: number
  statusCounts?: Record<string, number>
  todayApproved?: number
  monthReservations?: number
  approvalRate?: number
}

export interface ReservationPageQuery {
  pageNum?: number
  page?: number
  pageSize?: number
  status?: ReservationStatus | number
  applyNo?: string
  reservationNo?: string
  labId?: number
  startDate?: string
  endDate?: string
}

export interface ReservationApprovalRequest {
  reservationNo: string
  action: 'approve' | 'reject'
  rejectReason?: string
}

export function reservationStatusToCode(status?: ReservationStatus | number): number | undefined {
  if (typeof status === 'number') return status
  return status ? STATUS_KEY_TO_CODE[status] : undefined
}

// 分页查询预约申请
export function getReservationPage(query: ReservationPageQuery): Promise<PageResult<ReservationItem>> {
  const status = reservationStatusToCode(query.status)
  return api.get<ApiResult<PageResult<ApiReservationResponse>>>('/lab-management/reservations', {
    params: {
      page: query.page ?? query.pageNum,
      pageSize: query.pageSize,
      status,
      reservationNo: query.reservationNo ?? query.applyNo,
      labId: query.labId,
      startDate: query.startDate,
      endDate: query.endDate,
    },
  }).then(res => {
    const page = res.data.data
    return {
      ...page,
      records: page.records.map(mapReservationItem),
    }
  })
}

export function getReservationStats(): Promise<ReservationStats> {
  return api.get<ApiResult<ApiReservationStatsResponse>>('/lab-management/reservations/stats')
    .then(res => mapReservationStats(res.data.data))
}

// 审批预约申请
export function approveReservation(data: ReservationApprovalRequest): Promise<void> {
  return api.patch<ApiResult<unknown>>(`/lab-management/reservations/${data.reservationNo}/approve`, {
    approved: data.action === 'approve',
    approveComment: data.action === 'reject' ? data.rejectReason : '审核通过',
  }).then(() => {})
}

function mapReservationItem(r: ApiReservationResponse): ReservationItem {
  const status = STATUS_CODE_TO_KEY[r.reservationStatus ?? 1] ?? 'cancelled'
  const purpose = cleanPurpose(r.purpose)
  const purposeTag = inferPurposeTag(`${r.courseName ?? ''} ${r.purpose ?? ''}`)
  return {
    id: r.id ?? 0,
    applyNo: r.reservationNo ?? '',
    labId: r.labId ?? 0,
    labName: r.labName ?? '',
    labCode: '',
    applicantId: r.applicantId ?? 0,
    applicantName: r.applicantName ?? '',
    applicantType: r.applicantType ?? 1,
    applicantTypeName: r.applicantTypeDesc ?? (r.applicantType === 2 ? '教师' : '学生'),
    reserveDate: r.reservationDate ?? '',
    timeSlot: `${formatTime(r.startTime)}~${formatTime(r.endTime)}`,
    peopleCount: parsePeopleCount(r.purpose),
    purpose,
    purposeTag,
    purposeTagName: purposeTagName(purposeTag),
    status,
    statusName: r.reservationStatusDesc ?? STATUS_LABELS[status],
    rejectReason: r.approveComment ?? r.cancelReason,
    createdAt: formatDateTime(r.createdAt),
    approvedAt: formatDateTime(r.approveTime),
  }
}

function mapReservationStats(stats: ApiReservationStatsResponse): ReservationStats {
  const statusCounts: Partial<Record<ReservationStatus, number>> = {}
  Object.entries(stats.statusCounts ?? {}).forEach(([code, count]) => {
    const status = STATUS_CODE_TO_KEY[Number(code)]
    if (status) statusCounts[status] = Number(count)
  })
  return {
    pendingCount: Number(stats.pendingCount ?? statusCounts.pending ?? 0),
    statusCounts,
    todayApproved: Number(stats.todayApproved ?? 0),
    monthReservations: Number(stats.monthReservations ?? 0),
    approvalRate: Number(stats.approvalRate ?? 0),
  }
}

function parsePeopleCount(purpose: string | undefined): number {
  if (!purpose) return 1
  const matched = purpose.match(/使用人数[:：]\s*(\d+)/)
  return matched ? Number(matched[1]) : 1
}

function cleanPurpose(purpose: string | undefined): string {
  return (purpose ?? '')
    .replace(/[;；]?\s*使用人数[:：]\s*\d+\s*人?\s*$/u, '')
    .trim()
}

function inferPurposeTag(text: string): ReservationPurposeTag {
  if (/教学|课程|上课|实验课/.test(text)) return 'teaching'
  if (/科研|课题|研究|论文/.test(text)) return 'research'
  if (/考试|竞赛|答辩|测验/.test(text)) return 'exam'
  return 'other'
}

function purposeTagName(tag: ReservationPurposeTag): string {
  const names: Record<ReservationPurposeTag, string> = {
    teaching: '教学',
    research: '科研',
    exam: '考试',
    other: '其他',
  }
  return names[tag]
}

function formatTime(value: string | undefined): string {
  return value ? value.slice(0, 5) : ''
}

function formatDateTime(value: string | undefined): string {
  return value ? value.replace('T', ' ').slice(0, 16) : ''
}
