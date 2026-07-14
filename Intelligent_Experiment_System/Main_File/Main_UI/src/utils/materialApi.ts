import api from './api'
import type { ApiResult, PageResult } from './api'

export interface MaterialItem {
  id: number
  materialNo: string
  materialName: string
  labId: number
  unit: string
  safetyStock: number
  currentStock: number
  createdAt: string
  updatedAt: string
}

export function getMaterialPage(query: { pageNum?: number; pageSize?: number; materialName?: string } = {}): Promise<PageResult<MaterialItem>> {
  return api.get<ApiResult<PageResult<MaterialItem>>>('/material/materials', { params: query })
    .then(res => res.data.data)
}
