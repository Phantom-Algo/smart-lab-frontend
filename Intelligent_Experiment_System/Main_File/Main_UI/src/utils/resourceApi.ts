import api from './api'
import type { ApiResult, PageResult } from './api'

export interface ResourceItem {
  id: number
  name: string
  fileType: string
  courseId: number | null
  courseName: string
  fileName: string | null
  fileSize: number | null
  description: string | null
  uploadedAt: string
}

export function getResourcePage(query: { pageNum?: number; pageSize?: number; courseId?: number } = {}): Promise<PageResult<ResourceItem>> {
  return api.get<ApiResult<PageResult<ResourceItem>>>('/teaching/resources', { params: query })
    .then(res => res.data.data)
}
