import api from './api'
import type { ApiResult, PageResult } from './api'

// ==================== 人员档案 ====================

export interface PersonListItem {
  id: number
  personNo: string
  personName: string
  personType: number
  personTypeName: string
  genderType: number
  departmentName: string
  positionName: string
  personStatus: number
  personStatusName: string
  phone: string
  email: string
}

export interface PersonDetail extends PersonListItem {
  studentNo: string
  employeeNo: string
  className: string
  titleName: string
  extraData: Record<string, unknown>
  accountInfo?: {
    userId: number
    username: string
    accountStatus: number
    accountStatusName: string
  }
}

export interface PersonCreateRequest {
  personName: string
  personType: number
  genderType?: number
  departmentId?: number
  positionId?: number
  phone?: string
  email?: string
  studentNo?: string
  employeeNo?: string
  className?: string
  titleName?: string
}

export interface PersonPageQuery {
  pageNum?: number
  pageSize?: number
  personName?: string
  personType?: number
  departmentId?: number
  personStatus?: number
}

// 分页查询人员
export function getPersonPage(query: PersonPageQuery): Promise<PageResult<PersonListItem>> {
  return api.get<ApiResult<PageResult<PersonListItem>>>('/personnel/persons', { params: query })
    .then(res => res.data.data)
}

// 获取人员详情
export function getPersonDetail(id: number): Promise<PersonDetail> {
  return api.get<ApiResult<PersonDetail>>(`/personnel/persons/${id}`)
    .then(res => res.data.data)
}

// 新增人员
export function createPerson(data: PersonCreateRequest): Promise<PersonDetail> {
  return api.post<ApiResult<PersonDetail>>('/personnel/persons', data)
    .then(res => res.data.data)
}

// 编辑人员
export function updatePerson(id: number, data: Partial<PersonCreateRequest>): Promise<PersonDetail> {
  return api.put<ApiResult<PersonDetail>>(`/personnel/persons/${id}`, data)
    .then(res => res.data.data)
}

// 变更人员状态
export function updatePersonStatus(id: number, newStatus: number, reason?: string): Promise<void> {
  return api.post(`/personnel/persons/${id}/status`, { newStatus, reason }).then(() => {})
}

// 为人员创建账号
export function createPersonAccount(personId: number): Promise<void> {
  return api.post(`/personnel/persons/${personId}/account`).then(() => {})
}

// 停用人员账号
export function disablePersonAccount(personId: number): Promise<void> {
  return api.post(`/personnel/persons/${personId}/account/disable`).then(() => {})
}

// ==================== 部门管理 ====================

export interface DepartmentItem {
  id: number
  departmentNo: string
  departmentCode: string
  departmentName: string
  parentId: number | null
  leaderPersonId: number | null
  leaderPersonName: string
  description?: string | null
  departmentStatus: number
  children?: DepartmentItem[]
}

export function getDepartmentTree(): Promise<DepartmentItem[]> {
  return api.get<ApiResult<DepartmentItem[]>>('/personnel/departments/tree')
    .then(res => res.data.data)
}

export function getDepartments(): Promise<PageResult<DepartmentItem>> {
  return api.get<ApiResult<PageResult<DepartmentItem>>>('/personnel/departments')
    .then(res => res.data.data)
}

export function createDepartment(data: { departmentName: string; departmentCode?: string; parentId?: number }): Promise<void> {
  return api.post('/personnel/departments', data).then(() => {})
}

export function updateDepartment(id: number, data: { departmentName?: string; parentId?: number; leaderPersonId?: number }): Promise<void> {
  return api.put(`/personnel/departments/${id}`, data).then(() => {})
}

// ==================== 岗位管理 ====================

export interface PositionItem {
  id: number
  positionNo: string
  positionName: string
  positionType: number
  positionTypeName: string
  departmentId: number
  departmentName: string
  positionStatus: number
}

export function getPositions(departmentId?: number): Promise<PageResult<PositionItem>> {
  return api.get<ApiResult<PageResult<PositionItem>>>('/personnel/positions', { params: { departmentId, pageNum: 1, pageSize: 100 } })
    .then(res => res.data.data)
}

export function createPosition(data: { positionName: string; positionType: number; departmentId: number }): Promise<void> {
  return api.post('/personnel/positions', data).then(() => {})
}
