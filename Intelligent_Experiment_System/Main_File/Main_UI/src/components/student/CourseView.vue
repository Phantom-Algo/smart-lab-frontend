<template>
  <div class="page">
    <header class="page-header">
      <div>
        <h2>我的课程</h2>
        <p>查看已选课程，并从进行中的课程中自主选课</p>
      </div>
    </header>

    <p v-if="message" :class="['message', messageType]">{{ message }}</p>

    <section class="course-section">
      <div class="section-header">
        <div>
          <h3>已选课程</h3>
          <p>共 {{ courses.length }} 门</p>
        </div>
      </div>
      <div v-if="loading" class="empty">正在加载已选课程…</div>
      <div v-else class="grid">
        <article v-for="course in courses" :key="course.courseId">
          <small>{{ course.courseCode }} · {{ course.semester }}</small>
          <h3>{{ course.courseName }}</h3>
          <p>{{ course.teacherName }} · {{ course.departmentName || '未设置部门' }}</p>
          <div class="card-footer">
            <span>{{ courseStatusLabel(course.courseStatus) }}</span>
            <button @click="view(course.courseId)">查看学习内容</button>
          </div>
        </article>
        <div v-if="courses.length === 0" class="empty full-width">暂无已选课程，可在下方选择课程</div>
      </div>
    </section>

    <section class="course-section available-section">
      <div class="section-header available-header">
        <div>
          <h3>选课中心</h3>
          <p>仅展示尚未选择且正在进行中的课程</p>
        </div>
        <div class="search-box">
          <input v-model.trim="keyword" placeholder="搜索课程名称或编码" @keyup.enter="loadAvailable" />
          <button @click="loadAvailable">查询</button>
        </div>
      </div>
      <div v-if="availableLoading" class="empty">正在加载可选课程…</div>
      <div v-else class="grid">
        <article v-for="course in availableCourses" :key="course.courseId">
          <small>{{ course.courseCode }} · {{ course.semester }}</small>
          <h3>{{ course.courseName }}</h3>
          <p>{{ course.teacherName }} · {{ course.departmentName || '未设置部门' }}</p>
          <div class="card-footer">
            <span>{{ course.studentCount }} 人已选</span>
            <button
              class="primary"
              :disabled="enrollingId === course.courseId"
              @click="enroll(course)"
            >
              {{ enrollingId === course.courseId ? '选课中…' : '选择课程' }}
            </button>
          </div>
        </article>
        <div v-if="availableCourses.length === 0" class="empty full-width">暂无符合条件的可选课程</div>
      </div>
    </section>

    <teleport to="body">
      <div v-if="dialog && detail" class="overlay" @click.self="dialog = false">
        <section class="modal">
          <h3>{{ detail.courseName }}</h3>
          <p>{{ detail.description || '暂无课程说明' }}</p>
          <div class="meta">
            <span>学分 {{ detail.credit }}</span>
            <span>总学时 {{ detail.totalHours }}</span>
            <span>{{ detail.teacherName }}</span>
          </div>
          <h4>已发布任务</h4>
          <div v-for="task in tasks" :key="task.taskId" class="row">
            {{ task.taskTitle }}
            <span>{{ task.deadline.replace('T', ' ').slice(0, 16) }}</span>
          </div>
          <div v-if="tasks.length === 0" class="modal-empty">暂无已发布任务</div>
          <h4>教学资源</h4>
          <a
            v-for="resource in resources"
            :key="resource.resourceId"
            class="row"
            :href="resource.fileUrl || resource.externalUrl || '#'"
            target="_blank"
            rel="noopener"
          >
            {{ resource.resourceName }}
            <span>{{ resourceTypeLabel(resource.resourceType) }}</span>
          </a>
          <div v-if="resources.length === 0" class="modal-empty">暂无教学资源</div>
          <footer><button @click="dialog = false">关闭</button></footer>
        </section>
      </div>
    </teleport>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import {
  courseStatusLabel,
  enrollCourse,
  getAvailableCoursePage,
  getCourseDetail,
  getCoursePage,
  getCourseTasks,
  getResourcePage,
  resourceTypeLabel,
  type CourseDetail,
  type CourseListItem,
  type ResourceItem,
  type TaskListItem,
} from '../../utils/teachingApi'

const courses = ref<CourseListItem[]>([])
const availableCourses = ref<CourseListItem[]>([])
const detail = ref<CourseDetail | null>(null)
const tasks = ref<TaskListItem[]>([])
const resources = ref<ResourceItem[]>([])
const loading = ref(false)
const availableLoading = ref(false)
const enrollingId = ref(0)
const dialog = ref(false)
const keyword = ref('')
const message = ref('')
const messageType = ref<'success' | 'error'>('error')

function errorMessage(error: unknown): string {
  const responseMessage = (error as { response?: { data?: { message?: string } } })?.response?.data?.message
  if (responseMessage) return responseMessage
  return error instanceof Error ? error.message : '操作失败'
}

async function loadSelected(): Promise<void> {
  loading.value = true
  try {
    courses.value = (await getCoursePage({ pageNum: 1, pageSize: 500 })).records
  } catch (error) {
    messageType.value = 'error'
    message.value = errorMessage(error)
  } finally {
    loading.value = false
  }
}

async function loadAvailable(): Promise<void> {
  availableLoading.value = true
  try {
    availableCourses.value = (await getAvailableCoursePage({
      pageNum: 1,
      pageSize: 500,
      keyword: keyword.value || undefined,
    })).records
  } catch (error) {
    messageType.value = 'error'
    message.value = errorMessage(error)
  } finally {
    availableLoading.value = false
  }
}

async function enroll(course: CourseListItem): Promise<void> {
  if (!confirm(`确认选择“${course.courseName}”？`)) return
  enrollingId.value = course.courseId
  try {
    await enrollCourse(course.courseId)
    messageType.value = 'success'
    message.value = `选课成功，“${course.courseName}”已加入我的课程`
    await Promise.all([loadSelected(), loadAvailable()])
  } catch (error) {
    messageType.value = 'error'
    message.value = errorMessage(error)
  } finally {
    enrollingId.value = 0
  }
}

async function view(id: number): Promise<void> {
  try {
    const [courseDetail, courseTasks, courseResources] = await Promise.all([
      getCourseDetail(id),
      getCourseTasks(id),
      getResourcePage({ pageNum: 1, pageSize: 500, courseId: id }),
    ])
    detail.value = courseDetail
    tasks.value = courseTasks
    resources.value = courseResources.records
    dialog.value = true
  } catch (error) {
    messageType.value = 'error'
    message.value = errorMessage(error)
  }
}

onMounted(() => Promise.all([loadSelected(), loadAvailable()]))
</script>

<style scoped>
.page { height: 100%; overflow: auto; padding: 28px 36px; color: #172033; }
.page-header { display: flex; align-items: center; justify-content: space-between; }
h2, h3 { margin: 0; }
.page-header p, .section-header p { margin: 5px 0 0; color: #7b8495; }
.message { margin: 16px 0 0; border-radius: 8px; padding: 10px 12px; }
.message.error { color: #b42336; background: #fff1f2; }
.message.success { color: #167044; background: #ecfdf3; }
.course-section { margin-top: 22px; }
.available-section { border-top: 1px solid #e3e8ef; padding-top: 22px; }
.section-header { display: flex; align-items: flex-end; justify-content: space-between; margin-bottom: 12px; }
.section-header p { font-size: 13px; }
.available-header { gap: 20px; }
.search-box { display: flex; gap: 8px; }
.search-box input { width: 240px; border: 1px solid #d9e0e9; border-radius: 8px; padding: 9px 11px; font: inherit; }
.search-box button, .card-footer button, .modal button { border: 0; border-radius: 7px; padding: 8px 12px; background: #eef1f6; color: #273248; cursor: pointer; }
.grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 12px; }
.grid article { display: flex; flex-direction: column; min-height: 152px; border: 1px solid #e7ebf1; border-radius: 12px; padding: 16px; background: #fff; }
.grid h3 { margin: 7px 0; }
.grid article > p, .grid small { margin: 0; color: #7b8495; font-size: 12px; }
.card-footer { display: flex; align-items: center; justify-content: space-between; margin-top: auto; border-top: 1px solid #eef1f5; padding-top: 12px; }
.card-footer span { color: #687386; font-size: 13px; }
.card-footer .primary { background: #2563eb; color: #fff; }
.card-footer button:disabled { cursor: not-allowed; opacity: .6; }
.empty { padding: 38px; text-align: center; color: #8992a2; }
.full-width { grid-column: 1 / -1; border: 1px dashed #d9e0e9; border-radius: 12px; background: #fafbfc; }
.overlay { position: fixed; inset: 0; z-index: 1200; display: flex; align-items: center; justify-content: center; background: #0f172a6b; }
.modal { width: min(650px, 90%); max-height: 82vh; overflow: auto; border-radius: 14px; padding: 22px; background: #fff; }
.modal > p { color: #687386; }
.meta { display: flex; gap: 16px; margin: 14px 0; color: #687386; font-size: 13px; }
.modal h4 { margin: 20px 0 8px; }
.row { display: flex; justify-content: space-between; border-bottom: 1px solid #eef1f5; padding: 10px 0; color: inherit; text-decoration: none; }
.row span { color: #7b8495; font-size: 12px; }
.modal-empty { color: #8992a2; font-size: 13px; }
.modal footer { display: flex; justify-content: flex-end; margin-top: 20px; }
@media (max-width: 750px) {
  .page { padding: 22px 18px; }
  .grid { grid-template-columns: 1fr; }
  .available-header { align-items: stretch; flex-direction: column; }
  .search-box input { width: 100%; }
}
</style>
