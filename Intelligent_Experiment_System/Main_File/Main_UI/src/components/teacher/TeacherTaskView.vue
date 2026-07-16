<template>
  <div class="page">
    <header>
      <div>
        <h2>实验任务</h2>
        <p>创建、发布并跟踪本人课程任务</p>
      </div>
      <button @click="openCreate">新建任务</button>
    </header>

    <section class="filters">
      <select v-model="status">
        <option value="">全部状态</option>
        <option :value="1">草稿</option>
        <option :value="2">已发布</option>
        <option :value="3">已关闭</option>
      </select>
      <input v-model.trim="keyword" placeholder="搜索任务" />
      <button @click="load">查询</button>
    </section>

    <p v-if="message" class="error">{{ message }}</p>

    <section class="list">
      <article v-for="task in tasks" :key="task.taskId">
        <div>
          <small>{{ task.taskNo }} · {{ task.courseName }}</small>
          <h3>{{ task.taskTitle }}</h3>
          <p>截止 {{ fmt(task.deadline) }} · {{ taskStatusLabel(task.taskStatus) }}</p>
        </div>
        <div class="actions">
          <button v-if="task.taskStatus === 1" @click="edit(task.taskId)">编辑</button>
          <button v-if="task.taskStatus === 1" class="primary" @click="publish(task)">发布</button>
          <button v-if="task.taskStatus === 2" @click="close(task)">关闭</button>
          <button @click="openSubmissions(task)">查看提交</button>
        </div>
      </article>
      <div v-if="tasks.length === 0" class="empty">暂无任务</div>
    </section>

    <teleport to="body">
      <div v-if="dialog" class="overlay" @click.self="dialog = false">
        <section class="modal task-modal">
          <h3>{{ editingId ? '编辑任务' : '新建任务' }}</h3>
          <div class="grid">
            <label>
              所属课程
              <select v-model="form.courseId" :disabled="Boolean(editingId)">
                <option :value="0" disabled>请选择</option>
                <option v-for="course in courses" :key="course.courseId" :value="course.courseId">
                  {{ course.courseName }}
                </option>
              </select>
            </label>
            <label>任务标题<input v-model.trim="form.taskTitle" /></label>
            <label>
              提交方式
              <select v-model="form.submitType">
                <option :value="1">文本</option>
                <option :value="2">文件</option>
                <option :value="3">文本 + 文件</option>
              </select>
            </label>
            <label>最大文件大小（MB）<input v-model.number="form.maxFileSize" type="number" min="1" /></label>
            <label>截止时间<input v-model="form.deadline" type="datetime-local" /></label>
            <label>成绩权重<input v-model.number="form.taskWeight" type="number" min="0" max="100" /></label>
            <label class="full">任务内容<textarea v-model.trim="form.taskContent" rows="5" /></label>
          </div>
          <p v-if="formError" class="error">{{ formError }}</p>
          <footer>
            <button @click="dialog = false">取消</button>
            <button class="primary" @click="save">保存为草稿</button>
          </footer>
        </section>
      </div>

      <div v-if="submissionDialog" class="overlay" @click.self="closeSubmissions">
        <section class="modal submission-modal">
          <div class="submission-heading">
            <div>
              <small>{{ selectedTask?.taskNo }} · {{ selectedTask?.courseName }}</small>
              <h3>{{ selectedTask?.taskTitle }} - 学生提交</h3>
            </div>
            <button class="plain-button" @click="closeSubmissions">关闭</button>
          </div>

          <p v-if="submissionError" class="error">{{ submissionError }}</p>
          <div v-if="submissionLoading" class="empty">正在加载提交记录...</div>

          <template v-else-if="!submissionError">
            <section v-if="submissionStats" class="stats" aria-label="提交统计">
              <div><strong>{{ submissionStats.expectedCount }}</strong><span>应交人数</span></div>
              <div><strong>{{ submissionStats.submittedCount }}</strong><span>已交人数</span></div>
              <div><strong>{{ submissionStats.unsubmittedCount }}</strong><span>未交人数</span></div>
              <div><strong>{{ submissionStats.returnedCount }}</strong><span>已退回</span></div>
              <div><strong>{{ submissionStats.lateCount }}</strong><span>迟交人数</span></div>
            </section>

            <div class="submission-list">
              <article v-for="submission in submissions" :key="submission.submissionId" class="submission-card">
                <div class="submission-meta">
                  <div>
                    <h4>{{ submission.studentName || `学生 ${submission.studentId}` }}</h4>
                    <p>
                      {{ submission.submissionNo }} · 第 {{ submission.versionNo }} 版 ·
                      {{ fmt(submission.submittedAt) }}
                    </p>
                  </div>
                  <div class="badges">
                    <span :class="['badge', `status-${submission.submitStatus}`]">
                      {{ submissionStatusLabel(submission.submitStatus) }}
                    </span>
                    <span v-if="submission.isLate === 1" class="badge late">迟交</span>
                  </div>
                </div>

                <div class="submission-content">
                  <h5>提交正文</h5>
                  <p v-if="submission.contentText">{{ submission.contentText }}</p>
                  <p v-else class="muted">无文本内容</p>
                </div>

                <div v-if="submission.attachments" class="attachment-row">
                  <span>提交附件</span>
                  <a :href="submission.attachments" target="_blank" rel="noopener noreferrer">查看附件</a>
                </div>

                <div v-if="submission.returnedReason" class="returned-reason">
                  <strong>退回原因：</strong>{{ submission.returnedReason }}
                </div>
              </article>
              <div v-if="submissions.length === 0" class="empty">暂无学生提交</div>
            </div>
          </template>
        </section>
      </div>
    </teleport>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import {
  closeTask,
  createTask,
  getCoursePage,
  getSubmissionStats,
  getTaskDetail,
  getTaskPage,
  getTaskSubmissions,
  publishTask,
  taskStatusLabel,
  updateTask,
  type CourseListItem,
  type SubmissionItem,
  type SubmissionStats,
  type TaskListItem,
  type TaskStatus,
  type SubmitType,
} from '../../utils/teachingApi'

const tasks = ref<TaskListItem[]>([])
const courses = ref<CourseListItem[]>([])
const status = ref<TaskStatus | ''>('')
const keyword = ref('')
const dialog = ref(false)
const editingId = ref(0)
const message = ref('')
const formError = ref('')
const submissionDialog = ref(false)
const submissionLoading = ref(false)
const submissionError = ref('')
const selectedTask = ref<TaskListItem | null>(null)
const submissions = ref<SubmissionItem[]>([])
const submissionStats = ref<SubmissionStats | null>(null)

const form = reactive({
  courseId: 0,
  taskTitle: '',
  taskContent: '',
  submitType: 1 as SubmitType,
  maxFileSize: 20,
  deadline: '',
  taskWeight: 0,
})

const err = (error: unknown) => error instanceof Error ? error.message : '操作失败'
const fmt = (value: string) => value?.replace('T', ' ').slice(0, 16) || '—'

function submissionStatusLabel(submitStatus: number) {
  return ({ 1: '已提交', 2: '已退回', 3: '已评分' } as Record<number, string>)[submitStatus] || '未知状态'
}

async function load() {
  message.value = ''
  try {
    tasks.value = (await getTaskPage({
      pageNum: 1,
      pageSize: 500,
      keyword: keyword.value || undefined,
      taskStatus: status.value === '' ? undefined : status.value,
    })).records
  } catch (error) {
    message.value = err(error)
  }
}

function clear() {
  Object.assign(form, {
    courseId: 0,
    taskTitle: '',
    taskContent: '',
    submitType: 1,
    maxFileSize: 20,
    deadline: '',
    taskWeight: 0,
  })
  formError.value = ''
}

function openCreate() {
  clear()
  editingId.value = 0
  dialog.value = true
}

async function edit(id: number) {
  try {
    const detail = await getTaskDetail(id)
    Object.assign(form, {
      courseId: detail.courseId,
      taskTitle: detail.taskTitle,
      taskContent: detail.taskContent,
      submitType: detail.submitType,
      maxFileSize: detail.maxFileSize,
      deadline: detail.deadline.slice(0, 16),
      taskWeight: detail.taskWeight || 0,
    })
    editingId.value = id
    dialog.value = true
  } catch (error) {
    message.value = err(error)
  }
}

async function save() {
  if (!form.courseId || !form.taskTitle || !form.taskContent || !form.deadline) {
    formError.value = '请完整填写任务信息'
    return
  }
  const data = {
    taskTitle: form.taskTitle,
    taskContent: form.taskContent,
    submitType: form.submitType,
    maxFileSize: form.maxFileSize,
    deadline: form.deadline,
    taskWeight: form.taskWeight || undefined,
  }
  try {
    if (editingId.value) await updateTask(editingId.value, data)
    else await createTask({ courseId: form.courseId, ...data })
    dialog.value = false
    await load()
  } catch (error) {
    formError.value = err(error)
  }
}

async function publish(task: TaskListItem) {
  if (!confirm(`确认发布“${task.taskTitle}”？`)) return
  try {
    await publishTask(task.taskId)
    await load()
  } catch (error) {
    message.value = err(error)
  }
}

async function close(task: TaskListItem) {
  if (!confirm(`确认关闭“${task.taskTitle}”？`)) return
  try {
    await closeTask(task.taskId)
    await load()
  } catch (error) {
    message.value = err(error)
  }
}

async function openSubmissions(task: TaskListItem) {
  selectedTask.value = task
  submissions.value = []
  submissionStats.value = null
  submissionError.value = ''
  submissionLoading.value = true
  submissionDialog.value = true
  try {
    const [submissionList, stats] = await Promise.all([
      getTaskSubmissions(task.taskId),
      getSubmissionStats(task.taskId),
    ])
    submissions.value = submissionList
    submissionStats.value = stats
  } catch (error) {
    submissionError.value = err(error)
  } finally {
    submissionLoading.value = false
  }
}

function closeSubmissions() {
  submissionDialog.value = false
}

onMounted(async () => {
  try {
    courses.value = (await getCoursePage({ pageNum: 1, pageSize: 500 })).records
  } catch (error) {
    message.value = err(error)
  }
  await load()
})
</script>

<style scoped>
.page { height: 100%; overflow: auto; padding: 28px 36px; background: transparent; color: #172033; }
header { display: flex; justify-content: space-between; align-items: center; }
h2 { margin: 0; }
header p { margin: 5px 0; color: #7b8495; }
header button, .primary, .filters button { border: 0; border-radius: 8px; padding: 9px 15px; background: #2563eb; color: #fff; }
.filters { display: flex; gap: 10px; margin: 18px 0; padding: 14px; background: #fff; border: 1px solid #e7ebf1; border-radius: 12px; }
.filters input, .filters select, .grid input, .grid select, .grid textarea { border: 1px solid #dfe4ec; border-radius: 8px; padding: 9px; font: inherit; }
.list { display: flex; flex-direction: column; gap: 10px; }
.list > article { display: flex; justify-content: space-between; align-items: center; padding: 16px 18px; background: #fff; border: 1px solid #e7ebf1; border-radius: 12px; }
.list h3 { margin: 5px 0; }
.list p, .list small { color: #7b8495; font-size: 12px; }
.actions { display: flex; gap: 7px; }
.actions button, .modal footer button, .plain-button { border: 0; border-radius: 7px; padding: 7px 11px; background: #eef1f6; color: #172033; cursor: pointer; }
.actions .primary, .modal footer .primary { background: #2563eb; color: #fff; }
.empty { text-align: center; padding: 50px; color: #8992a2; }
.error { color: #b42336; }
.overlay { position: fixed; inset: 0; z-index: 1200; display: flex; align-items: center; justify-content: center; padding: 24px; background: #0f172a6b; }
.modal { padding: 22px; background: #fff; border-radius: 14px; box-shadow: 0 18px 48px #0f172a2b; }
.task-modal { width: min(700px, 90%); }
.grid { display: grid; grid-template-columns: 1fr 1fr; gap: 14px; }
.grid label { display: flex; flex-direction: column; gap: 6px; font-size: 12px; color: #687386; }
.full { grid-column: 1 / -1; }
.modal footer { display: flex; justify-content: flex-end; gap: 8px; margin-top: 16px; }
.submission-modal { width: min(920px, 94vw); max-height: 86vh; overflow: auto; }
.submission-heading { position: sticky; top: -22px; z-index: 2; display: flex; justify-content: space-between; align-items: flex-start; margin: -22px -22px 0; padding: 22px; border-bottom: 1px solid #edf0f5; background: #fff; border-radius: 14px 14px 0 0; }
.submission-heading small { color: #7b8495; }
.submission-heading h3 { margin: 6px 0 0; }
.stats { display: grid; grid-template-columns: repeat(5, 1fr); gap: 10px; margin: 20px 0; }
.stats div { display: flex; flex-direction: column; align-items: center; gap: 5px; padding: 14px 8px; border: 1px solid #e4e9f1; border-radius: 10px; background: #f8fafc; }
.stats strong { color: #1d4ed8; font-size: 22px; }
.stats span { color: #6f798b; font-size: 12px; }
.submission-list { display: flex; flex-direction: column; gap: 12px; }
.submission-card { padding: 17px; border: 1px solid #e4e9f1; border-radius: 12px; background: #fff; }
.submission-meta { display: flex; justify-content: space-between; align-items: flex-start; gap: 16px; }
.submission-meta h4 { margin: 0 0 5px; }
.submission-meta p { margin: 0; color: #7b8495; font-size: 12px; }
.badges { display: flex; gap: 6px; flex-shrink: 0; }
.badge { padding: 4px 8px; border-radius: 999px; background: #dcfce7; color: #18713b; font-size: 12px; }
.badge.status-2 { background: #fee2e2; color: #a52222; }
.badge.status-3 { background: #dbeafe; color: #1d4ed8; }
.badge.late { background: #fff3cd; color: #8a5b00; }
.submission-content { margin-top: 15px; padding: 13px 14px; border-radius: 9px; background: #f8fafc; }
.submission-content h5 { margin: 0 0 8px; color: #657084; font-size: 12px; }
.submission-content p { margin: 0; line-height: 1.7; white-space: pre-wrap; overflow-wrap: anywhere; }
.submission-content .muted { color: #929aaa; }
.attachment-row { display: flex; align-items: center; gap: 14px; margin-top: 12px; font-size: 13px; }
.attachment-row span { color: #657084; }
.attachment-row a { color: #2563eb; text-decoration: none; }
.returned-reason { margin-top: 12px; padding: 10px 12px; border-radius: 8px; background: #fff1f2; color: #9f2334; font-size: 13px; }

@media (max-width: 700px) {
  .page { padding: 20px; }
  .grid { grid-template-columns: 1fr; }
  .full { grid-column: auto; }
  .list > article { align-items: flex-start; flex-direction: column; gap: 12px; }
  .actions { flex-wrap: wrap; }
  .stats { grid-template-columns: repeat(2, 1fr); }
  .submission-meta { flex-direction: column; }
}
</style>
