import api from './api'
import type { ApiResult, PageResult } from './api'

// ========== 物资 (Material) ==========

export interface MaterialItem {
  id: number
  materialNo: string
  materialName: string
  labId: number
  unit: string
  safetyStock: number
  currentStock: number
  batchCount: number
  earliestExpiryDate: string | null
  earliestBatchNo: string | null
  createdAt: string
  updatedAt: string
}

/** 分页查询物资 */
export function getMaterialPage(query: { pageNum?: number; pageSize?: number; materialName?: string; labId?: number } = {}): Promise<PageResult<MaterialItem>> {
  return api.get<ApiResult<PageResult<MaterialItem>>>('/material/materials', { params: query })
    .then(res => res.data.data)
}

export interface MaterialDetail {
  id: number
  materialNo: string
  materialName: string
  labId: number
  unit: string
  safetyStock: number
  currentStock: number
  batches: MaterialBatch[]
  createdAt: string
  updatedAt: string
}

/** 物资详情（含批次） */
export function getMaterialDetail(id: number): Promise<MaterialDetail> {
  return api.get<ApiResult<MaterialDetail>>(`/material/materials/${id}`)
    .then(res => res.data.data)
}

/** 创建物资 */
export interface CreateMaterialParams {
  materialName: string
  labId: number
  unit: string
  safetyStock?: number
}
export function createMaterial(data: CreateMaterialParams): Promise<MaterialItem> {
  return api.post<ApiResult<MaterialItem>>('/material/materials', data)
    .then(res => res.data.data)
}

/** 更新物资 */
export interface UpdateMaterialParams {
  materialName?: string
  labId?: number
  unit?: string
  safetyStock?: number
}
export function updateMaterial(id: number, data: UpdateMaterialParams): Promise<MaterialItem> {
  return api.put<ApiResult<MaterialItem>>(`/material/materials/${id}`, data)
    .then(res => res.data.data)
}

/** 删除物资 */
export function deleteMaterial(id: number): Promise<void> {
  return api.delete<ApiResult<null>>(`/material/materials/${id}`).then(() => {})
}

// ========== 物资批次 (MaterialBatch) ==========

export interface MaterialBatch {
  id: number
  batchNo: string
  materialId: number
  materialName: string
  quantity: number
  remaining: number
  expiryDate: string
  stockInTime: string
  batchStatus: number
  batchStatusDesc: string
  operatorId: number
  remark: string | null
  createdAt: string
  updatedAt: string
}

/** 查某物资的所有批次 */
export function getBatches(materialId: number): Promise<MaterialBatch[]> {
  return api.get<ApiResult<MaterialBatch[]>>('/material/batches', { params: { materialId } })
    .then(res => res.data.data)
}

/** 报废某批次 */
export function scrapBatch(batchId: number): Promise<void> {
  return api.put<ApiResult<null>>(`/material/batches/${batchId}/scrap`).then(() => {})
}

// ========== 入库 (StockIn) ==========

export interface StockInParams {
  materialId: number
  quantity: number
  expiryDate: string
  remark?: string
}
export interface StockInResult {
  id: number
  batchNo: string
  materialId: number
  quantity: number
  remaining: number
  expiryDate: string
}

/** 物资入库 */
export function stockIn(data: StockInParams): Promise<StockInResult> {
  return api.post<ApiResult<StockInResult>>('/material/materials/stock-in', data)
    .then(res => res.data.data)
}

// ========== 领用申请 (ReceiveApply) ==========

export interface ReceiveApplyItem {
  id: number
  applyNo: string
  materialId: number
  materialName: string
  applicantId: number
  approverId: number | null
  expectedQuantity: number
  purpose: string | null
  applyStatus: number      // 1-待审核 2-已通过 3-已驳回 4-已取消
  applyStatusDesc: string
  rejectReason: string | null
  createdAt: string
  updatedAt: string
}

export interface ApplyParams {
  materialId: number
  expectedQuantity: number
  purpose?: string
}

/** 提交领用申请 */
export function applyReceive(data: ApplyParams): Promise<ReceiveApplyItem> {
  return api.post<ApiResult<ReceiveApplyItem>>('/material/receives/apply', data)
    .then(res => res.data.data)
}

/** 管理员分页查所有领用申请 */
export function listAllReceives(query: { pageNum?: number; pageSize?: number; applyStatus?: number } = {}): Promise<PageResult<ReceiveApplyItem>> {
  return api.get<ApiResult<PageResult<ReceiveApplyItem>>>('/material/receives', { params: query })
    .then(res => res.data.data)
}

/** 教师查自己的领用申请 */
export function listMyReceives(query: { pageNum?: number; pageSize?: number } = {}): Promise<PageResult<ReceiveApplyItem>> {
  return api.get<ApiResult<PageResult<ReceiveApplyItem>>>('/material/receives/my', { params: query })
    .then(res => res.data.data)
}

/** 领用详情 */
export function getReceiveDetail(applyNo: string): Promise<any> {
  return api.get<ApiResult<any>>(`/material/receives/${applyNo}`)
    .then(res => res.data.data)
}

/** 撤单 */
export function cancelReceive(applyNo: string): Promise<void> {
  return api.put<ApiResult<null>>(`/material/receives/${applyNo}/cancel`).then(() => {})
}

/** 审批通过 */
export function approveReceive(applyNo: string): Promise<any> {
  return api.post<ApiResult<any>>(`/material/receives/${applyNo}/approve`)
    .then(res => res.data.data)
}

/** 驳回 */
export function rejectReceive(applyNo: string, reason: string): Promise<void> {
  return api.post<ApiResult<null>>(`/material/receives/${applyNo}/reject`, { reason })
    .then(() => {})
}

// ========== 预警 (MaterialAlert) ==========

export interface MaterialAlertItem {
  id: number
  materialId: number
  materialName: string
  batchId: number | null
  batchNo: string | null
  alertType: number       // 1-低库存 2-临期 3-过期
  alertTypeDesc: string
  alertContent: string
  status: number          // 0-未处理 1-已处理
  statusDesc: string
  handledBy: number | null
  alertTime: string
  handledAt: string | null
  createdAt: string
}

/** 分页查预警 */
export function listAlerts(query: { pageNum?: number; pageSize?: number; alertType?: number; status?: number } = {}): Promise<PageResult<MaterialAlertItem>> {
  return api.get<ApiResult<PageResult<MaterialAlertItem>>>('/material/alerts', { params: query })
    .then(res => res.data.data)
}

/** 处理预警 */
export function handleAlert(id: number, result?: string): Promise<void> {
  return api.post<ApiResult<null>>(`/material/alerts/${id}/handle`, result ? { result } : {})
    .then(() => {})
}
