import api from './api'
import type { ApiResult, PageResult } from './api'

// ==================== 课程 ====================
export type CourseStatus = 1 | 2 | 3

/** 匹配后端 CoursePageItemResponse 实际返回字段 */
export interface CourseListItem {
  id: number               // JSON: courseId → 映射为 id
  courseNo: string         // JSON: courseCode
  courseName: string       // JSON: courseName
  semester: string         // JSON: semester
  teacherName: string      // JSON: teacherName
  departmentName: string   // JSON: departmentName
  status: CourseStatus     // JSON: courseStatus
  enrolledCount: number    // JSON: studentCount
  // 以下为前端扩展字段（API 返回或详情接口提供）
  courseTypeId?: number
  courseTypeName?: string
  teacherId?: number | null
  labId?: number | null
  labName?: string
  weekday?: number
  weekdayName?: string
  timeSlot?: string
  maxStudents?: number
  statusName?: string
  description?: string | null
  createdAt?: string
}

export interface CourseDetail extends CourseListItem {
  students?: { personId: number; studentNo: string; realName: string; className: string }[]
  labs?: { labId: number; labName: string }[]
}

export interface CoursePageQuery {
  pageNum?: number; pageSize?: number; courseName?: string
  courseTypeId?: number; status?: CourseStatus; semester?: string
}

export function getCoursePage(query: CoursePageQuery): Promise<PageResult<CourseListItem>> {
  return api.get<ApiResult<PageResult<any>>>('/teaching/courses', { params: query }).then(r => {
    const page = r.data?.data
    if (!page) return { records: [], total: 0, pageNum: query.pageNum || 1, pageSize: query.pageSize || 20, pages: 0 }
    return {
      ...page,
      records: (page.records || []).map((raw: any) => ({
        id: raw.courseId ?? raw.id,
        courseNo: raw.courseCode ?? raw.courseNo ?? '',
        courseName: raw.courseName ?? '',
        semester: raw.semester ?? '',
        teacherName: raw.teacherName ?? '',
        departmentName: raw.departmentName ?? '',
        status: raw.courseStatus ?? raw.status ?? 1,
        enrolledCount: raw.studentCount ?? raw.enrolledCount ?? 0,
      }))
    }
  })
}
export function getCourseDetail(id: number): Promise<CourseDetail> {
  return api.get<ApiResult<CourseDetail>>(`/teaching/courses/${id}`).then(r => r.data.data)
}
export function createCourse(data: Partial<CourseListItem>): Promise<CourseDetail> {
  return api.post<ApiResult<CourseDetail>>('/teaching/courses', data).then(r => r.data.data)
}
export function updateCourse(id: number, data: Partial<CourseListItem>): Promise<CourseDetail> {
  return api.put<ApiResult<CourseDetail>>(`/teaching/courses/${id}`, data).then(r => r.data.data)
}
export function deleteCourse(id: number): Promise<void> {
  return api.delete(`/teaching/courses/${id}`).then(() => {})
}

// ==================== 教学任务 ====================
export interface TaskListItem {
  id: number; taskNo: string; taskName: string; courseId: number
	  courseName: string; taskType: string; publishStatus: string
	  startTime: string; endTime: string; createdAt: string
	  description?: string
	  submitCount?: number
	  gradedCount?: number
	}

export interface TaskDetail extends TaskListItem {
  description: string
}

export interface TaskPageQuery {
  pageNum?: number; pageSize?: number; taskName?: string; publishStatus?: string
}

export function getTaskPage(query: TaskPageQuery): Promise<PageResult<TaskListItem>> {
  return api.get<ApiResult<PageResult<TaskListItem>>>('/teaching/tasks', { params: query }).then(r => r.data.data)
}
export function getTaskDetail(id: number): Promise<TaskDetail> {
  return api.get<ApiResult<TaskDetail>>(`/teaching/tasks/${id}`).then(r => r.data.data)
}
export function getCourseTasks(courseId: number): Promise<TaskListItem[]> {
  return api.get<ApiResult<TaskListItem[]>>(`/teaching/courses/${courseId}/tasks`).then(r => r.data.data)
}
export function createTask(data: Partial<TaskDetail> & { courseId: number }): Promise<TaskDetail> {
  return api.post<ApiResult<TaskDetail>>('/teaching/tasks', data).then(r => r.data.data)
}
export function publishTask(id: number): Promise<TaskDetail> {
  return api.post<ApiResult<TaskDetail>>(`/teaching/tasks/${id}/publish`).then(r => r.data.data)
}

// ==================== 教学资源 ====================
export interface ResourceItem {
  id: number; resourceName: string; resourceType: string; fileSize: number
  courseId: number; courseName: string; downloadUrl: string; createdAt: string
}

export interface ResourcePageQuery {
  pageNum?: number; pageSize?: number; resourceName?: string; courseId?: number
}

export function getResourcePage(query: ResourcePageQuery): Promise<PageResult<ResourceItem>> {
  return api.get<ApiResult<PageResult<ResourceItem>>>('/teaching/resources', { params: query }).then(r => r.data.data)
}
export function deleteResource(id: number): Promise<void> {
  return api.delete(`/teaching/resources/${id}`).then(() => {})
}

// ==================== 作业提交 ====================
export interface SubmissionItem {
  id: number; taskId: number; studentId: number; studentName: string
  content: string; status: string; submittedAt: string
}

export interface SubmissionStats {
  totalStudents: number; submittedCount: number; unsubmittedCount: number
}

export function getTaskSubmissions(taskId: number): Promise<SubmissionItem[]> {
  return api.get<ApiResult<SubmissionItem[]>>(`/teaching/tasks/${taskId}/submissions`).then(r => r.data.data)
}
export function getMySubmission(taskId: number): Promise<SubmissionItem> {
  return api.get<ApiResult<SubmissionItem>>(`/teaching/tasks/${taskId}/submissions/me`).then(r => r.data.data)
}
export function submitTask(taskId: number, data: { content?: string }): Promise<SubmissionItem> {
  return api.post<ApiResult<SubmissionItem>>(`/teaching/tasks/${taskId}/submissions`, data).then(r => r.data.data)
}
export function returnSubmission(submissionId: number, comment: string): Promise<SubmissionItem> {
  return api.post<ApiResult<SubmissionItem>>(`/teaching/submissions/${submissionId}/return`, { comment }).then(r => r.data.data)
}
export function getSubmissionStats(taskId: number): Promise<SubmissionStats> {
  return api.get<ApiResult<SubmissionStats>>(`/teaching/tasks/${taskId}/submission-stats`).then(r => r.data.data)
}

// ==================== 成绩评定 ====================
export interface GradeItem {
  id: number; taskId: number; studentId: number; studentName: string; studentNo: string
  score: number | null; comment: string | null
}

export function getTaskGrades(taskId: number): Promise<GradeItem[]> {
  return api.get<ApiResult<GradeItem[]>>(`/teaching/tasks/${taskId}/grades`).then(r => r.data.data)
}
export function submitGrades(taskId: number, grades: { studentId: number; score: number; comment?: string }[]): Promise<void> {
  return api.post(`/teaching/tasks/${taskId}/grades`, { grades }).then(() => {})
}
export function getMyGrades(): Promise<{ taskName: string; courseName: string; score: number; comment?: string | null }[]> {
  // 获取学生课程列表，再逐个查成绩
  return getCoursePage({ pageNum: 1, pageSize: 50 }).then(async page => {
    const results: { taskName: string; courseName: string; score: number; comment?: string | null }[] = []
    for (const course of page.records) {
      try {
        const sheet = await api.get<ApiResult<any>>(`/teaching/courses/${course.id}/grades/me`)
        if (sheet.data?.data?.items) {
          for (const item of sheet.data.data.items) {
            results.push({ taskName: item.itemName || '', courseName: course.courseName, score: item.score ?? 0, comment: item.comment ?? null })
          }
        }
      } catch { /* course may have no grades yet */ }
    }
    return results
  })
}
