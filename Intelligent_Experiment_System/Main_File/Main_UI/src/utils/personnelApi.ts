import api from './api'
import type { ApiResult, PageResult } from './api'

export type PersonType = 1 | 2 | 3
export type PersonStatus = 1 | 2 | 3 | 4
export type GenderType = 0 | 1 | 2
export type AccountStatus = 1 | 2 | 3

export interface PersonAccountSummary {
  userId: number
  username: string
  userType: PersonType
  accountStatus: AccountStatus
  passwordResetRequired: boolean
  temporaryPassword?: string | null
}

export interface PersonDetail {
  id: number
  personNo: string
  personName: string
  personType: PersonType
  genderType: GenderType
  phone?: string | null
  email?: string | null
  idCardNoMasked?: string | null
  studentNo?: string | null
  employeeNo?: string | null
  departmentId: number
  departmentName: string
  positionId?: number | null
  positionName?: string | null
  className?: string | null
  titleName?: string | null
  personStatus: PersonStatus
  entryAt?: string | null
  leaveAt?: string | null
  sourceType: number
  description?: string | null
  account?: PersonAccountSummary | null
}

export interface PersonCreateRequest {
  personNo: string
  personName: string
  personType: PersonType
  genderType: GenderType
  phone?: string
  email?: string
  idCardNoMasked?: string
  studentNo?: string
  employeeNo?: string
  departmentId: number
  positionId?: number
  className?: string
  titleName?: string
  personStatus: PersonStatus
  entryAt?: string
  leaveAt?: string
  sourceType: number
  description?: string
}

export type PersonUpdateRequest = Omit<PersonCreateRequest, 'personNo' | 'sourceType'>

export interface PersonPageQuery {
  pageNum?: number
  pageSize?: number
  keyword?: string
  personTypes?: PersonType[]
  departmentId?: number
  positionId?: number
  personStatuses?: PersonStatus[]
  hasAccount?: boolean
  onlyEnabledDepartment?: boolean
}

export function getPersonPage(query: PersonPageQuery): Promise<PageResult<PersonDetail>> {
  return api.get<ApiResult<PageResult<PersonDetail>>>('/personnel/persons', {
    params: query,
    // Spring MVC 集合参数使用 personTypes=2&personTypes=3，不使用 personTypes[]=2。
    paramsSerializer: { indexes: null },
  })
    .then(response => response.data.data)
}

export function getPersonDetail(id: number): Promise<PersonDetail> {
  return api.get<ApiResult<PersonDetail>>(`/personnel/persons/${id}`)
    .then(response => response.data.data)
}

export function createPerson(data: PersonCreateRequest): Promise<PersonDetail> {
  return api.post<ApiResult<PersonDetail>>('/personnel/persons', data)
    .then(response => response.data.data)
}

export function updatePerson(id: number, data: PersonUpdateRequest): Promise<PersonDetail> {
  return api.put<ApiResult<PersonDetail>>(`/personnel/persons/${id}`, data)
    .then(response => response.data.data)
}

export function updatePersonStatus(id: number, personStatus: PersonStatus, description?: string): Promise<void> {
  return api.post<ApiResult<null>>(`/personnel/persons/${id}/status`, { personStatus, description }).then(() => {})
}

export function createPersonAccount(
  personId: number,
  data: { username?: string; initialPassword?: string; autoGeneratePassword: boolean },
): Promise<PersonAccountSummary> {
  return api.post<ApiResult<PersonAccountSummary>>(`/personnel/persons/${personId}/account`, data)
    .then(response => response.data.data)
}

export function enablePersonAccount(personId: number): Promise<void> {
  return api.post<ApiResult<null>>(`/personnel/persons/${personId}/account/enable`).then(() => {})
}

export function disablePersonAccount(personId: number, reason?: string): Promise<void> {
  return api.post<ApiResult<null>>(`/personnel/persons/${personId}/account/disable`, undefined, { params: { reason } })
    .then(() => {})
}

export function resetPersonPassword(
  personId: number,
  data: { initialPassword?: string; autoGeneratePassword: boolean },
): Promise<PersonAccountSummary> {
  return api.post<ApiResult<PersonAccountSummary>>(`/personnel/persons/${personId}/account/reset-password`, data)
    .then(response => response.data.data)
}

export type DepartmentStatus = 1 | 2

export interface DepartmentItem {
  id: number
  departmentNo: string
  departmentCode: string
  departmentName: string
  parentId?: number | null
  leaderPersonId?: number | null
  leaderPersonName?: string | null
  departmentStatus: DepartmentStatus
  sortNo: number
  description?: string | null
  children?: DepartmentItem[]
}

export interface DepartmentCreateRequest {
  departmentNo: string
  departmentCode: string
  departmentName: string
  parentId?: number
  leaderPersonId?: number
  departmentStatus: DepartmentStatus
  sortNo?: number
  description?: string
}

export type DepartmentUpdateRequest = Omit<DepartmentCreateRequest, 'departmentNo' | 'departmentCode'>

export function getDepartmentTree(): Promise<DepartmentItem[]> {
  return api.get<ApiResult<DepartmentItem[]>>('/personnel/departments/tree')
    .then(response => response.data.data)
}

export function getDepartmentDetail(id: number): Promise<DepartmentItem> {
  return api.get<ApiResult<DepartmentItem>>(`/personnel/departments/${id}`)
    .then(response => response.data.data)
}

export function createDepartment(data: DepartmentCreateRequest): Promise<DepartmentItem> {
  return api.post<ApiResult<DepartmentItem>>('/personnel/departments', data)
    .then(response => response.data.data)
}

export function updateDepartment(id: number, data: DepartmentUpdateRequest): Promise<DepartmentItem> {
  return api.put<ApiResult<DepartmentItem>>(`/personnel/departments/${id}`, data)
    .then(response => response.data.data)
}

export function updateDepartmentStatus(id: number, departmentStatus: DepartmentStatus): Promise<void> {
  return api.post<ApiResult<null>>(`/personnel/departments/${id}/status`, undefined, { params: { departmentStatus } })
    .then(() => {})
}

export type PositionType = 1 | 2 | 3
export type PositionStatus = 1 | 2

export interface PositionItem {
  id: number
  positionNo: string
  positionCode: string
  positionName: string
  positionType: PositionType
  departmentId: number
  positionStatus: PositionStatus
  sortNo: number
  description?: string | null
}

export interface PositionCreateRequest {
  positionNo: string
  positionCode: string
  positionName: string
  positionType: PositionType
  departmentId: number
  positionStatus: PositionStatus
  sortNo?: number
  description?: string
}

export type PositionUpdateRequest = Omit<PositionCreateRequest, 'positionNo' | 'positionCode'>

export function getPositions(query: {
  pageNum?: number
  pageSize?: number
  departmentId?: number
  positionStatus?: PositionStatus
} = {}): Promise<PageResult<PositionItem>> {
  return api.get<ApiResult<PageResult<PositionItem>>>('/personnel/positions', { params: query })
    .then(response => response.data.data)
}

export function getPositionDetail(id: number): Promise<PositionItem> {
  return api.get<ApiResult<PositionItem>>(`/personnel/positions/${id}`)
    .then(response => response.data.data)
}

export function createPosition(data: PositionCreateRequest): Promise<PositionItem> {
  return api.post<ApiResult<PositionItem>>('/personnel/positions', data)
    .then(response => response.data.data)
}

export function updatePosition(id: number, data: PositionUpdateRequest): Promise<PositionItem> {
  return api.put<ApiResult<PositionItem>>(`/personnel/positions/${id}`, data)
    .then(response => response.data.data)
}

export function updatePositionStatus(id: number, positionStatus: PositionStatus): Promise<void> {
  return api.post<ApiResult<null>>(`/personnel/positions/${id}/status`, undefined, { params: { positionStatus } })
    .then(() => {})
}
