import api from './api'
import type { ApiResult, PageResult } from './api'

export type CourseStatus = 1 | 2 | 3
export interface CourseListItem {
  courseId: number; courseCode: string; courseName: string; semester: string
  teacherName: string; departmentName?: string | null; courseStatus: CourseStatus; studentCount: number
  // 兼容尚待迁移的旧页面，值均由真实响应映射。
  id: number; courseNo: string; status: CourseStatus; enrolledCount: number
  courseTypeId?: number; courseTypeName?: string; teacherId?: number | null; labId?: number | null
  labName?: string; weekday?: number; weekdayName?: string; timeSlot?: string; maxStudents?: number
  statusName?: string; description?: string | null; createdAt?: string
}
export interface CourseDetail {
  courseId: number; courseCode: string; courseName: string; semester: string; credit: number; totalHours: number
  teacherId: number; teacherName: string; departmentId?: number | null; departmentName?: string | null
  description?: string | null; syllabus?: string | null; courseStatus: CourseStatus; studentCount: number
}
export interface CoursePageQuery { pageNum?: number; pageSize?: number; keyword?: string; semester?: string; teacherId?: number; studentId?: number; departmentId?: number; courseStatus?: CourseStatus; courseName?: string; status?: CourseStatus }
export interface CourseSaveRequest { courseCode?: string; courseName: string; semester: string; credit: number; totalHours: number; teacherId: number; departmentId?: number; description?: string; syllabus?: string; courseStatus?: CourseStatus }
function courseCompat(raw: any): CourseListItem { return { ...raw, courseId: raw.courseId, courseCode: raw.courseCode, courseName: raw.courseName, semester: raw.semester, teacherName: raw.teacherName || '', departmentName: raw.departmentName, courseStatus: raw.courseStatus, studentCount: raw.studentCount || 0, id: raw.courseId, courseNo: raw.courseCode, status: raw.courseStatus, enrolledCount: raw.studentCount || 0, statusName: courseStatusLabel(raw.courseStatus) } }
export function courseStatusLabel(value: number): string { return ({ 1: '进行中', 2: '已结课', 3: '已归档' } as Record<number, string>)[value] || '未知' }
export function getCoursePage(query: CoursePageQuery = {}): Promise<PageResult<CourseListItem>> { const params = { ...query, keyword: query.keyword || query.courseName, courseStatus: query.courseStatus || query.status }; return api.get<ApiResult<PageResult<any>>>('/teaching/courses', { params }).then(r => ({ ...r.data.data, records: r.data.data.records.map(courseCompat) })) }
export function getAvailableCoursePage(query: CoursePageQuery = {}): Promise<PageResult<CourseListItem>> { const params = { ...query, keyword: query.keyword || query.courseName }; return api.get<ApiResult<PageResult<any>>>('/teaching/courses/available', { params }).then(r => ({ ...r.data.data, records: r.data.data.records.map(courseCompat) })) }
export function enrollCourse(id: number): Promise<void> { return api.post(`/teaching/courses/${id}/enroll`).then(() => {}) }
export function getCourseDetail(id: number): Promise<CourseDetail> { return api.get<ApiResult<CourseDetail>>(`/teaching/courses/${id}`).then(r => r.data.data) }
export function createCourse(data: CourseSaveRequest & { courseCode: string }): Promise<CourseDetail> { return api.post<ApiResult<CourseDetail>>('/teaching/courses', data).then(r => r.data.data) }
export function updateCourse(id: number, data: Omit<CourseSaveRequest, 'courseCode'>): Promise<CourseDetail> { return api.put<ApiResult<CourseDetail>>(`/teaching/courses/${id}`, data).then(r => r.data.data) }
export function archiveCourse(id: number): Promise<void> { return api.post(`/teaching/courses/${id}/archive`).then(() => {}) }
export function deleteCourse(id: number): Promise<void> { return api.delete(`/teaching/courses/${id}`).then(() => {}) }
export interface CourseStudent { personId: number; personName: string; studentNo?: string | null; className?: string | null; userId?: number | null; accountStatus?: number | null; enrollStatus: number }
export function getCourseStudents(id: number): Promise<CourseStudent[]> { return api.get<ApiResult<CourseStudent[]>>(`/teaching/courses/${id}/students`).then(r => r.data.data) }

export type TaskStatus = 1 | 2 | 3
export type SubmitType = 1 | 2 | 3
export interface TaskListItem {
  taskId: number; taskNo: string; courseId: number; courseName: string; taskTitle: string; taskStatus: TaskStatus
  deadline: string; publishedAt?: string | null; createdByName?: string | null
  id: number; taskName: string; publishStatus: string; endTime: string; taskType: string; startTime: string; createdAt: string; description?: string; submitCount?: number; gradedCount?: number
}
export interface TaskDetail { taskId: number; taskNo: string; courseId: number; courseName: string; taskTitle: string; taskContent: string; submitType: SubmitType; maxFileSize: number; deadline: string; taskWeight?: number | null; taskStatus: TaskStatus; publishedAt?: string | null; createdBy: number; createdByName?: string | null }
export interface TaskPageQuery { pageNum?: number; pageSize?: number; courseId?: number; keyword?: string; taskStatus?: TaskStatus; taskName?: string; publishStatus?: string }
export interface TaskSaveRequest { courseId?: number; taskTitle: string; taskContent: string; submitType: SubmitType; maxFileSize: number; deadline: string; taskWeight?: number }
export function taskStatusLabel(value: number): string { return ({ 1: '草稿', 2: '已发布', 3: '已关闭' } as Record<number, string>)[value] || '未知' }
function taskCompat(raw: any): TaskListItem { return { ...raw, id: raw.taskId, taskName: raw.taskTitle, publishStatus: taskStatusLabel(raw.taskStatus), endTime: raw.deadline, taskType: '', startTime: raw.publishedAt || '', createdAt: raw.publishedAt || '', description: '' } }
export function getTaskPage(query: TaskPageQuery = {}): Promise<PageResult<TaskListItem>> { const params = { ...query, keyword: query.keyword || query.taskName }; return api.get<ApiResult<PageResult<any>>>('/teaching/tasks', { params }).then(r => ({ ...r.data.data, records: r.data.data.records.map(taskCompat) })) }
export function getTaskDetail(id: number): Promise<TaskDetail> { return api.get<ApiResult<TaskDetail>>(`/teaching/tasks/${id}`).then(r => r.data.data) }
export function getCourseTasks(courseId: number): Promise<TaskListItem[]> { return api.get<ApiResult<any[]>>(`/teaching/courses/${courseId}/tasks`).then(r => r.data.data.map(taskCompat)) }
export function createTask(data: TaskSaveRequest & { courseId: number }): Promise<TaskDetail> { return api.post<ApiResult<TaskDetail>>('/teaching/tasks', data).then(r => r.data.data) }
export function updateTask(id: number, data: Omit<TaskSaveRequest, 'courseId'>): Promise<TaskDetail> { return api.put<ApiResult<TaskDetail>>(`/teaching/tasks/${id}`, data).then(r => r.data.data) }
export async function publishTask(id: number, deadline?: string): Promise<TaskDetail> {
  const task = await getTaskDetail(id)
  if (task.taskStatus !== 1) throw new Error('任务已不是草稿状态，请刷新任务列表')
  const effectiveDeadline = deadline || task.deadline
  const deadlineTime = new Date(effectiveDeadline).getTime()
  if (!Number.isFinite(deadlineTime) || deadlineTime <= Date.now()) {
    throw new Error('任务截止时间已过，请先编辑任务并设置未来的截止时间')
  }
  return api.post<ApiResult<TaskDetail>>(`/teaching/tasks/${id}/publish`, { deadline: effectiveDeadline })
    .then(r => r.data.data)
}
export function closeTask(id: number): Promise<void> { return api.post(`/teaching/tasks/${id}/close`).then(() => {}) }

export type ResourceType = 1 | 2 | 3 | 4 | 5 | 6
export interface ResourceItem { resourceId: number; resourceNo: string; courseId: number; courseName: string; resourceName: string; resourceType: ResourceType; fileUrl?: string | null; externalUrl?: string | null; fileSize?: number | null; sortNo: number; uploadedBy: number; uploadedByName?: string | null; createdAt: string }
export interface ResourcePageQuery { pageNum?: number; pageSize?: number; courseId?: number; resourceType?: ResourceType; keyword?: string }
export interface ResourceSaveRequest { courseId?: number; resourceName: string; resourceType: ResourceType; fileUrl?: string; externalUrl?: string; fileSize?: number; sortNo?: number }
export function resourceTypeLabel(value: number): string { return ({ 1: 'PPT', 2: 'Word', 3: 'PDF', 4: '视频链接', 5: '参考链接', 6: '其他' } as Record<number, string>)[value] || '未知' }
export function getResourcePage(query: ResourcePageQuery = {}): Promise<PageResult<ResourceItem>> { return api.get<ApiResult<PageResult<ResourceItem>>>('/teaching/resources', { params: query }).then(r => r.data.data) }
export function createResource(data: ResourceSaveRequest & { courseId: number }): Promise<ResourceItem> { return api.post<ApiResult<ResourceItem>>('/teaching/resources', data).then(r => r.data.data) }
export function updateResource(id: number, data: Omit<ResourceSaveRequest, 'courseId'>): Promise<ResourceItem> { return api.put<ApiResult<ResourceItem>>(`/teaching/resources/${id}`, data).then(r => r.data.data) }
export function deleteResource(id: number): Promise<void> { return api.delete(`/teaching/resources/${id}`).then(() => {}) }

export interface SubmissionItem { submissionId: number; submissionNo: string; taskId: number; taskTitle: string; studentId: number; studentName: string; versionNo: number; currentFlag: number; submitStatus: number; isLate: number; contentText?: string | null; attachments?: string | null; returnedReason?: string | null; returnedAt?: string | null; submittedAt: string; id: number; content: string; status: string }
function submissionCompat(raw: any): SubmissionItem { return { ...raw, id: raw.submissionId, content: raw.contentText || '', status: String(raw.submitStatus) } }
export interface SubmissionStats { taskId: number; expectedCount: number; submittedCount: number; unsubmittedCount: number; returnedCount: number; lateCount: number; totalStudents?: number }
export function getTaskSubmissions(taskId: number): Promise<SubmissionItem[]> { return api.get<ApiResult<any[]>>(`/teaching/tasks/${taskId}/submissions`).then(r => r.data.data.map(submissionCompat)) }
export function getMySubmission(taskId: number): Promise<SubmissionItem> { return api.get<ApiResult<any>>(`/teaching/tasks/${taskId}/submissions/me`).then(r => submissionCompat(r.data.data)) }
export function submitTask(taskId: number, data: { contentText?: string; attachments?: string; content?: string }): Promise<SubmissionItem> { return api.post<ApiResult<any>>(`/teaching/tasks/${taskId}/submissions`, { contentText: data.contentText ?? data.content, attachments: data.attachments }).then(r => submissionCompat(r.data.data)) }
export function returnSubmission(submissionId: number, returnedReason: string): Promise<SubmissionItem> { return api.post<ApiResult<any>>(`/teaching/submissions/${submissionId}/return`, { returnedReason }).then(r => submissionCompat(r.data.data)) }
export function getSubmissionStats(taskId: number): Promise<SubmissionStats> { return api.get<ApiResult<SubmissionStats>>(`/teaching/tasks/${taskId}/submission-stats`).then(r => ({ ...r.data.data, totalStudents: r.data.data.expectedCount })) }

export interface GradeItem { gradeItemId: number; courseId: number; itemName: string; itemType: number; itemWeight: number; fullScore: number; relatedTaskId?: number | null; sortNo: number; id?: number; taskId?: number; studentId?: number; studentName?: string; studentNo?: string; score?: number | null; comment?: string | null }
export interface StudentGradeItem { gradeItemId: number; itemName: string; itemType: number; itemWeight: number; fullScore: number; score?: number | null; weightedScore?: number | null; gradeStatus?: number | null; comment?: string | null }
export interface StudentGradeSheet { courseId: number; courseName: string; studentId: number; studentName: string; studentNo?: string | null; publishStatus: number; totalScore?: number | null; items: StudentGradeItem[] }
export function getGradeItems(courseId: number): Promise<GradeItem[]> { return api.get<ApiResult<GradeItem[]>>(`/teaching/courses/${courseId}/grade-items`).then(r => r.data.data) }
export function createGradeItem(courseId: number, data: Omit<GradeItem, 'gradeItemId' | 'courseId'>): Promise<GradeItem> { return api.post<ApiResult<GradeItem>>(`/teaching/courses/${courseId}/grade-items`, data).then(r => r.data.data) }
export function getCourseGradeSheets(courseId: number): Promise<StudentGradeSheet[]> { return api.get<ApiResult<StudentGradeSheet[]>>(`/teaching/courses/${courseId}/grades`).then(r => r.data.data) }
export function saveGradeDrafts(courseId: number, items: { gradeItemId: number; studentId: number; taskSubmissionId?: number; score: number; comment?: string }[]): Promise<void> { return api.post(`/teaching/courses/${courseId}/grades/draft/batch`, { items }).then(() => {}) }
export function publishCourseGrades(courseId: number): Promise<void> { return api.post(`/teaching/courses/${courseId}/grades/publish`).then(() => {}) }
export function getMyCourseGrade(courseId: number): Promise<StudentGradeSheet> { return api.get<ApiResult<StudentGradeSheet>>(`/teaching/courses/${courseId}/grades/me`).then(r => r.data.data) }
export function submitGrades(_taskId: number, _grades: { studentId: number; score: number; comment?: string }[]): Promise<void> { return Promise.reject(new Error('请通过课程成绩项保存成绩')) }
export async function getMyGrades(): Promise<{ taskName: string; courseName: string; score: number; comment?: string | null }[]> { const courses = await getCoursePage({ pageNum: 1, pageSize: 200 }); const sheets = await Promise.all(courses.records.map(course => getMyCourseGrade(course.courseId).catch(() => null))); return sheets.flatMap(sheet => sheet ? sheet.items.map(item => ({ taskName: item.itemName, courseName: sheet.courseName, score: item.score || 0, comment: item.comment })) : []) }
