import api from './api'
import type { ApiResult, PageResult } from './api'

// ==================== 课程管理 ====================

export type CourseStatus = 1 | 2 | 3  // 1未开始 2进行中 3已结课

export interface CourseListItem {
  id: number
  courseNo: string
  courseName: string
  courseTypeId: number
  courseTypeName: string
  semester: string
  teacherId: number | null
  teacherName: string
  labId: number | null
  labName: string
  weekday: number          // 1-5 周一~周五
  weekdayName: string
  timeSlot: string
  maxStudents: number
  enrolledCount: number
  status: CourseStatus
  statusName: string
  description: string | null
  createdAt: string
}

export interface CoursePageQuery {
  pageNum?: number
  pageSize?: number
  courseName?: string
  courseTypeId?: number
  status?: CourseStatus
  semester?: string
}

export interface CreateCourseRequest {
  courseNo: string
  courseName: string
  courseTypeId: number
  semester: string
  teacherId?: number
  labId?: number
  weekday?: number
  timeSlot?: string
  maxStudents?: number
  description?: string
}

// 分页查询课程
export function getCoursePage(query: CoursePageQuery): Promise<PageResult<CourseListItem>> {
  return api.get<ApiResult<PageResult<CourseListItem>>>('/courses', { params: query })
    .then(res => res.data.data)
}

// 查询课程详情
export function getCourseDetail(id: number): Promise<CourseListItem> {
  return api.get<ApiResult<CourseListItem>>(`/courses/${id}`)
    .then(res => res.data.data)
}

// 新增课程
export function createCourse(data: CreateCourseRequest): Promise<number> {
  return api.post<ApiResult<number>>('/courses', data).then(res => res.data.data)
}
