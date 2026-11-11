<template>
  <main class="dashboard">
    <section class="hero glass-surface">
      <div class="hero-copy">
        <span class="eyebrow">STUDENT WORKSPACE</span>
        <h1>你好，{{ userName }}</h1>
        <p>{{ todayStr }} · {{ weekDay }}，继续完成你的实验学习计划。</p>
      </div>
      <div class="hero-side">
        <button class="refresh-button" :disabled="loading" title="刷新首页数据" @click="loadDashboard">
          <svg :class="{ spinning: loading }" viewBox="0 0 24 24" aria-hidden="true">
            <path d="M20 11a8 8 0 10-2.34 5.66M20 5v6h-6" />
          </svg>
          <span>{{ loading ? '更新中' : '刷新' }}</span>
        </button>
        <div class="stats" aria-label="学习数据概览">
          <div v-for="stat in stats" :key="stat.label" class="stat-item">
            <strong>{{ stat.value }}</strong>
            <span>{{ stat.label }}</span>
          </div>
        </div>
      </div>
    </section>

    <nav class="quick-actions" aria-label="快捷入口">
      <button
        v-for="action in quickActions"
        :key="action.key"
        class="quick-action glass-surface"
        @click="navigate(action.key, action.label)"
      >
        <span class="quick-icon" :style="{ '--accent': action.color, '--accent-soft': action.colorSoft }">
          <svg viewBox="0 0 24 24" aria-hidden="true"><path :d="action.iconPath" /></svg>
        </span>
        <span class="quick-copy">
          <strong>{{ action.label }}</strong>
          <small>{{ action.description }}</small>
        </span>
        <svg class="arrow-icon" viewBox="0 0 24 24" aria-hidden="true"><path d="m9 18 6-6-6-6" /></svg>
      </button>
    </nav>

    <div class="content-grid">
      <section class="panel glass-surface">
        <header class="panel-header">
          <div>
            <span class="section-kicker">TASKS</span>
            <h2>待完成任务</h2>
          </div>
          <button class="text-button" @click="navigate('my-task', '实验任务')">
            查看全部
            <svg viewBox="0 0 24 24" aria-hidden="true"><path d="m9 18 6-6-6-6" /></svg>
          </button>
        </header>

        <div v-if="loadErrors.tasks" class="state-box error-state">
          <svg viewBox="0 0 24 24" aria-hidden="true"><circle cx="12" cy="12" r="9"/><path d="M12 8v5m0 3h.01"/></svg>
          <span>{{ loadErrors.tasks }}</span>
        </div>
        <div v-else-if="loading && !hasLoaded" class="state-box">正在读取任务数据…</div>
        <div v-else-if="visibleTasks.length" class="task-list">
          <button
            v-for="task in visibleTasks"
            :key="task.taskId"
            class="task-row"
            @click="navigate('my-task', '实验任务')"
          >
            <span class="task-indicator" :class="deadlineClass(task.deadline)"></span>
            <span class="task-copy">
              <strong>{{ task.taskTitle }}</strong>
              <small>{{ task.courseName }} · {{ task.taskNo }}</small>
            </span>
            <time :class="deadlineClass(task.deadline)">{{ deadlineText(task.deadline) }}</time>
          </button>
        </div>
        <div v-else class="state-box success-state">
          <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M20 6 9 17l-5-5"/></svg>
          <span>当前没有待完成的实验任务</span>
        </div>
      </section>

      <section class="panel glass-surface">
        <header class="panel-header">
          <div>
            <span class="section-kicker">BORROWING</span>
            <h2>设备借用</h2>
          </div>
          <button class="text-button" @click="navigate('device-borrow', '借用申请')">
            申请记录
            <svg viewBox="0 0 24 24" aria-hidden="true"><path d="m9 18 6-6-6-6" /></svg>
          </button>
        </header>

        <div v-if="loadErrors.borrows" class="state-box error-state">
          <svg viewBox="0 0 24 24" aria-hidden="true"><circle cx="12" cy="12" r="9"/><path d="M12 8v5m0 3h.01"/></svg>
          <span>{{ loadErrors.borrows }}</span>
        </div>
        <div v-else-if="loading && !hasLoaded" class="state-box">正在读取借用数据…</div>
        <div v-else-if="visibleBorrows.length" class="borrow-list">
          <button
            v-for="item in visibleBorrows"
            :key="item.id"
            class="borrow-row"
            @click="navigate('device-borrow', '借用申请')"
          >
            <span class="borrow-icon">
              <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M5 8h14v10H5zM8 8V5h8v3m-5 5h2"/></svg>
            </span>
            <span class="borrow-copy">
              <strong>{{ item.deviceName }}</strong>
              <small>{{ item.deviceNo }} · {{ formatDateTime(item.expectedReturnTime) }} 前归还</small>
            </span>
            <span class="status-pill" :class="`status-${item.applyStatus}`">
              {{ item.applyStatusDesc || borrowStatusLabel(item.applyStatus) }}
            </span>
          </button>
        </div>
        <div v-else class="state-box">暂无设备借用申请</div>
      </section>
    </div>

    <section class="panel course-panel glass-surface">
      <header class="panel-header">
        <div>
          <span class="section-kicker">COURSES</span>
          <h2>我的课程概览</h2>
        </div>
        <button class="text-button" @click="navigate('my-course', '我的课程')">
          进入课程中心
          <svg viewBox="0 0 24 24" aria-hidden="true"><path d="m9 18 6-6-6-6" /></svg>
        </button>
      </header>

      <div v-if="loadErrors.courses" class="state-box error-state">
        <svg viewBox="0 0 24 24" aria-hidden="true"><circle cx="12" cy="12" r="9"/><path d="M12 8v5m0 3h.01"/></svg>
        <span>{{ loadErrors.courses }}</span>
      </div>
      <div v-else-if="loading && !hasLoaded" class="state-box">正在读取课程数据…</div>
      <div v-else-if="visibleCourses.length" class="course-grid">
        <button
          v-for="course in visibleCourses"
          :key="course.courseId"
          class="course-item"
          @click="navigate('my-course', '我的课程')"
        >
          <span class="course-topline">
            <small>{{ course.courseCode }}</small>
            <span :class="['course-status', `course-status-${course.courseStatus}`]">
              {{ courseStatusLabel(course.courseStatus) }}
            </span>
          </span>
          <strong>{{ course.courseName }}</strong>
          <span class="course-meta">{{ course.teacherName || '教师待设置' }} · {{ course.semester }}</span>
        </button>
      </div>
      <div v-else class="state-box">暂无已选课程，可前往课程中心选课</div>
    </section>

    <section class="panel explore-panel glass-surface">
      <header class="panel-header">
        <div>
          <span class="section-kicker">VIRTUAL LAB</span>
          <h2>3D 实验室漫游</h2>
        </div>
        <span class="panel-note">选择场景进入虚拟实验室</span>
      </header>
      <div class="lab-grid">
        <button
          v-for="lab in lab3dList"
          :key="lab.name"
          class="lab-item"
          @click="navigate('3d-show', lab.name)"
        >
          <span class="lab-icon" :style="{ '--lab-color': lab.color, '--lab-soft': lab.colorSoft }">
            <svg viewBox="0 0 24 24" aria-hidden="true"><path :d="lab.iconPath"/></svg>
          </span>
          <span><strong>{{ lab.name }}</strong><small>{{ lab.description }}</small></span>
        </button>
      </div>
    </section>
  </main>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { userName } from '../../stores/authStore'
import { useMessageStore } from '../../stores/messageStore'
import {
  courseStatusLabel,
  getCoursePage,
  getMySubmission,
  getTaskPage,
  type CourseListItem,
  type TaskListItem,
} from '../../utils/teachingApi'
import { getMyBorrowApplies, type MyBorrowApplyItem } from '../../utils/deviceApi'

const emit = defineEmits<{
  navigate: [payload: { key: string; name: string }]
}>()

const { apiNewIdCount, fetchNewIds } = useMessageStore()
const courses = ref<CourseListItem[]>([])
const pendingTasks = ref<TaskListItem[]>([])
const borrowApplications = ref<MyBorrowApplyItem[]>([])
const loading = ref(false)
const hasLoaded = ref(false)
const loadErrors = reactive({ courses: '', tasks: '', borrows: '' })

const now = new Date()
const weekDays = ['星期日', '星期一', '星期二', '星期三', '星期四', '星期五', '星期六']
const todayStr = new Intl.DateTimeFormat('zh-CN', {
  year: 'numeric', month: 'long', day: 'numeric',
}).format(now)
const weekDay = weekDays[now.getDay()]

const quickActions = [
  { key: 'my-course', label: '我的课程', description: '查看课程与学习资料', color: '#2563eb', colorSoft: 'rgba(37, 99, 235, .12)', iconPath: 'M4 5.5A2.5 2.5 0 0 1 6.5 3H20v16H6.5A2.5 2.5 0 0 0 4 21.5zm0 0v16M8 7h8M8 11h6' },
  { key: 'device-borrow', label: '设备借用', description: '提交与跟踪借用申请', color: '#059669', colorSoft: 'rgba(5, 150, 105, .12)', iconPath: 'M5 8h14v11H5zM8 8V5h8v3m-5 5h2' },
  { key: 'lab-search', label: '实验室查询', description: '查看实验室开放信息', color: '#7c3aed', colorSoft: 'rgba(124, 58, 237, .12)', iconPath: 'M4 21h16M7 21V5l5-2 5 2v16M9 9h.01M15 9h.01M9 13h.01M15 13h.01M10 21v-4h4v4' },
]

const lab3dList = [
  { name: '教学实验室', description: '标准教学环境', color: '#2563eb', colorSoft: 'rgba(37,99,235,.1)', iconPath: 'M4 6h16M4 10h16M4 14h16M4 18h16M9 3v18' },
  { name: '通用实验室', description: '综合实验空间', color: '#059669', colorSoft: 'rgba(5,150,105,.1)', iconPath: 'M4 4h16v16H4zM4 10h16M10 4v16' },
  { name: '计算机实验室', description: '计算机与网络实验', color: '#ea580c', colorSoft: 'rgba(234,88,12,.1)', iconPath: 'M4 5h16v12H4zM8 21h8M12 17v4' },
  { name: '小组研讨室', description: '协作与讨论空间', color: '#7c3aed', colorSoft: 'rgba(124,58,237,.1)', iconPath: 'M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2M9 11a4 4 0 1 0 0-8 4 4 0 0 0 0 8zm9-3a3 3 0 0 1 0 6m4 7v-2a4 4 0 0 0-3-3.87' },
  { name: '器材借用室', description: '器材领用管理', color: '#dc2626', colorSoft: 'rgba(220,38,38,.1)', iconPath: 'm12 3 8 4.5v9L12 21l-8-4.5v-9zm0 9 8-4.5M12 12 4 7.5M12 12v9' },
]

const stats = computed(() => [
  { label: '已选课程', value: courses.value.length },
  { label: '待完成', value: pendingTasks.value.length },
  { label: '借用申请', value: borrowApplications.value.filter(item => item.applyStatus === 1 || item.applyStatus === 2).length },
  { label: '未读消息', value: apiNewIdCount.value },
])

const visibleTasks = computed(() => pendingTasks.value.slice(0, 4))
const visibleBorrows = computed(() => borrowApplications.value.slice(0, 4))
const visibleCourses = computed(() => courses.value.slice(0, 4))

function navigate(key: string, name: string): void {
  emit('navigate', { key, name })
}

function errorMessage(error: unknown, fallback: string): string {
  const responseMessage = (error as { response?: { data?: { message?: string } } })?.response?.data?.message
  return responseMessage || (error instanceof Error ? error.message : fallback)
}

async function loadCourses(): Promise<void> {
  try {
    const result = await getCoursePage({ pageNum: 1, pageSize: 100 })
    courses.value = result.records || []
    loadErrors.courses = ''
  } catch (error) {
    loadErrors.courses = errorMessage(error, '课程数据加载失败')
  }
}

async function loadTasks(): Promise<void> {
  try {
    const result = await getTaskPage({ pageNum: 1, pageSize: 100, taskStatus: 2 })
    const tasks = result.records || []
    const submissionResults = await Promise.allSettled(tasks.map(task => getMySubmission(task.taskId)))
    const unexpectedFailure = submissionResults.find(result =>
      result.status === 'rejected'
      && !(result.reason instanceof Error && result.reason.message.includes('任务提交不存在'))
    )
    if (unexpectedFailure?.status === 'rejected') throw unexpectedFailure.reason
    pendingTasks.value = tasks
      .filter((_task, index) => {
        const submission = submissionResults[index]
        return submission.status === 'rejected' || submission.value.submitStatus === 2
      })
      .sort((left, right) => new Date(left.deadline).getTime() - new Date(right.deadline).getTime())
    loadErrors.tasks = ''
  } catch (error) {
    loadErrors.tasks = errorMessage(error, '任务数据加载失败')
  }
}

async function loadBorrows(): Promise<void> {
  try {
    const result = await getMyBorrowApplies({ pageNum: 1, pageSize: 100 })
    borrowApplications.value = (result.records || [])
      .slice()
      .sort((left, right) => new Date(right.createdAt).getTime() - new Date(left.createdAt).getTime())
    loadErrors.borrows = ''
  } catch (error) {
    loadErrors.borrows = errorMessage(error, '借用数据加载失败')
  }
}

async function loadDashboard(): Promise<void> {
  if (loading.value) return
  loading.value = true
  await Promise.allSettled([loadCourses(), loadTasks(), loadBorrows(), fetchNewIds()])
  hasLoaded.value = true
  loading.value = false
}

function formatDateTime(value: string): string {
  return value ? value.replace('T', ' ').slice(0, 16) : '时间待定'
}

function deadlineText(value: string): string {
  if (!value) return '截止时间待定'
  const date = new Date(value)
  if (!Number.isFinite(date.getTime())) return formatDateTime(value)
  const prefix = date.getTime() < Date.now() ? '已截止' : '截止'
  return `${prefix} ${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')} ${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`
}

function deadlineClass(value: string): string {
  const time = new Date(value).getTime()
  if (!Number.isFinite(time)) return 'normal'
  if (time < Date.now()) return 'overdue'
  return time - Date.now() <= 48 * 60 * 60 * 1000 ? 'urgent' : 'normal'
}

function borrowStatusLabel(status: number): string {
  return ({ 1: '待审批', 2: '已通过', 3: '已驳回', 4: '已取消' } as Record<number, string>)[status] || '未知状态'
}

onMounted(loadDashboard)
</script>

<style scoped>
.dashboard {
  height: 100%;
  overflow-y: auto;
  box-sizing: border-box;
  padding: clamp(20px, 2.4vw, 36px);
  color: #172033;
}

.glass-surface {
  background: rgba(255, 255, 255, .68);
  border: 1px solid rgba(255, 255, 255, .74);
  box-shadow: 0 14px 38px rgba(70, 101, 143, .09);
  backdrop-filter: blur(18px);
  -webkit-backdrop-filter: blur(18px);
}

.hero {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 30px;
  padding: 26px 30px;
  border-radius: 22px;
}

.eyebrow, .section-kicker {
  display: block;
  color: #2563eb;
  font-size: 10px;
  font-weight: 750;
  letter-spacing: .16em;
}

.hero h1 {
  margin: 7px 0 6px;
  color: #111827;
  font-size: clamp(24px, 2vw, 32px);
  line-height: 1.2;
}

.hero p {
  margin: 0;
  color: #718096;
  font-size: 13px;
}

.hero-side {
  display: flex;
  align-items: center;
  gap: 24px;
}

.refresh-button, .text-button {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  border: 0;
  background: transparent;
  color: #64748b;
  cursor: pointer;
  font: inherit;
}

.refresh-button {
  padding: 8px 10px;
  border: 1px solid rgba(148, 163, 184, .22);
  border-radius: 10px;
  font-size: 12px;
}

.refresh-button:disabled { cursor: default; opacity: .65; }
.refresh-button svg, .text-button svg { width: 15px; height: 15px; fill: none; stroke: currentColor; stroke-width: 1.8; }
.spinning { animation: spin .9s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }

.stats { display: flex; gap: clamp(18px, 2.5vw, 34px); }
.stat-item { min-width: 54px; text-align: center; }
.stat-item strong { display: block; color: #0f172a; font-size: 25px; line-height: 1.15; }
.stat-item span { color: #8793a5; font-size: 11px; }

.quick-actions {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
  margin: 16px 0;
}

.quick-action {
  display: flex;
  align-items: center;
  gap: 13px;
  min-width: 0;
  padding: 16px 18px;
  border-radius: 16px;
  color: inherit;
  text-align: left;
  cursor: pointer;
  transition: transform .18s ease, box-shadow .18s ease, background .18s ease;
}

.quick-action:hover { transform: translateY(-2px); background: rgba(255,255,255,.84); box-shadow: 0 16px 34px rgba(70,101,143,.13); }
.quick-icon, .lab-icon, .borrow-icon { display: grid; place-items: center; flex: 0 0 auto; }
.quick-icon { width: 42px; height: 42px; border-radius: 12px; color: var(--accent); background: var(--accent-soft); }
.quick-icon svg { width: 21px; height: 21px; fill: none; stroke: currentColor; stroke-width: 1.65; stroke-linecap: round; stroke-linejoin: round; }
.quick-copy { display: flex; flex: 1; min-width: 0; flex-direction: column; gap: 3px; }
.quick-copy strong { font-size: 14px; }
.quick-copy small { overflow: hidden; color: #8a96a8; font-size: 11px; text-overflow: ellipsis; white-space: nowrap; }
.arrow-icon { width: 17px; height: 17px; fill: none; stroke: #9aa5b5; stroke-width: 1.7; }

.content-grid { display: grid; grid-template-columns: minmax(0, 1.15fr) minmax(0, .85fr); gap: 16px; }
.panel { min-width: 0; padding: 22px; border-radius: 20px; }
.course-panel, .explore-panel { margin-top: 16px; }
.panel-header { display: flex; align-items: flex-start; justify-content: space-between; gap: 16px; margin-bottom: 16px; }
.panel-header h2 { margin: 4px 0 0; color: #172033; font-size: 16px; }
.text-button { padding: 3px 0; color: #64748b; font-size: 11px; }
.text-button:hover { color: #2563eb; }
.panel-note { color: #8a96a8; font-size: 11px; }

.task-list, .borrow-list { display: flex; flex-direction: column; }
.task-row, .borrow-row, .course-item, .lab-item {
  border: 0;
  background: transparent;
  color: inherit;
  font: inherit;
  text-align: left;
  cursor: pointer;
}
.task-row, .borrow-row { display: flex; align-items: center; gap: 12px; min-width: 0; padding: 12px 4px; border-top: 1px solid rgba(148,163,184,.16); }
.task-row:first-child, .borrow-row:first-child { border-top: 0; }
.task-row:hover strong, .borrow-row:hover strong { color: #2563eb; }
.task-indicator { width: 7px; height: 7px; flex: 0 0 auto; border-radius: 50%; background: #3b82f6; box-shadow: 0 0 0 4px rgba(59,130,246,.1); }
.task-indicator.urgent { background: #f59e0b; box-shadow: 0 0 0 4px rgba(245,158,11,.1); }
.task-indicator.overdue { background: #ef4444; box-shadow: 0 0 0 4px rgba(239,68,68,.1); }
.task-copy, .borrow-copy { display: flex; flex: 1; min-width: 0; flex-direction: column; gap: 4px; }
.task-copy strong, .borrow-copy strong { overflow: hidden; font-size: 13px; text-overflow: ellipsis; white-space: nowrap; transition: color .15s; }
.task-copy small, .borrow-copy small { overflow: hidden; color: #8a96a8; font-size: 10px; text-overflow: ellipsis; white-space: nowrap; }
.task-row time { flex: 0 0 auto; color: #64748b; font-size: 10px; }
.task-row time.urgent { color: #b45309; }
.task-row time.overdue { color: #dc2626; }

.borrow-icon { width: 34px; height: 34px; border-radius: 10px; color: #2563eb; background: rgba(37,99,235,.09); }
.borrow-icon svg { width: 18px; height: 18px; fill: none; stroke: currentColor; stroke-width: 1.6; stroke-linecap: round; stroke-linejoin: round; }
.status-pill { flex: 0 0 auto; padding: 4px 8px; border-radius: 999px; background: #f1f5f9; color: #64748b; font-size: 9px; font-weight: 650; }
.status-1 { background: #fff7ed; color: #c2410c; }
.status-2 { background: #ecfdf5; color: #047857; }
.status-3 { background: #fef2f2; color: #b91c1c; }

.course-grid { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 12px; }
.course-item { display: flex; min-width: 0; flex-direction: column; gap: 9px; padding: 15px; border: 1px solid rgba(148,163,184,.16); border-radius: 14px; background: rgba(255,255,255,.34); transition: border-color .18s ease, transform .18s ease, background .18s ease; }
.course-item:hover { transform: translateY(-2px); border-color: rgba(37,99,235,.28); background: rgba(255,255,255,.66); }
.course-topline { display: flex; align-items: center; justify-content: space-between; gap: 8px; }
.course-topline small { overflow: hidden; color: #7b8798; font-size: 9px; text-overflow: ellipsis; }
.course-item > strong { overflow: hidden; color: #1e293b; font-size: 13px; text-overflow: ellipsis; white-space: nowrap; }
.course-meta { overflow: hidden; color: #8a96a8; font-size: 10px; text-overflow: ellipsis; white-space: nowrap; }
.course-status { flex: 0 0 auto; padding: 3px 7px; border-radius: 999px; background: #f1f5f9; color: #64748b; font-size: 8px; }
.course-status-1 { background: #eff6ff; color: #1d4ed8; }
.course-status-2 { background: #f8fafc; color: #64748b; }
.course-status-3 { background: #f1f5f9; color: #475569; }

.lab-grid { display: grid; grid-template-columns: repeat(5, minmax(0, 1fr)); gap: 10px; }
.lab-item { display: flex; align-items: center; gap: 10px; min-width: 0; padding: 11px; border-radius: 13px; transition: background .18s ease, transform .18s ease; }
.lab-item:hover { transform: translateY(-1px); background: rgba(255,255,255,.48); }
.lab-icon { width: 36px; height: 36px; border-radius: 10px; color: var(--lab-color); background: var(--lab-soft); }
.lab-icon svg { width: 18px; height: 18px; fill: none; stroke: currentColor; stroke-width: 1.55; stroke-linecap: round; stroke-linejoin: round; }
.lab-item > span:last-child { display: flex; min-width: 0; flex-direction: column; gap: 3px; }
.lab-item strong, .lab-item small { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.lab-item strong { color: #334155; font-size: 11px; }
.lab-item small { color: #94a3b8; font-size: 9px; }

.state-box { display: flex; min-height: 92px; align-items: center; justify-content: center; gap: 8px; color: #94a3b8; font-size: 12px; text-align: center; }
.state-box svg { width: 19px; height: 19px; fill: none; stroke: currentColor; stroke-width: 1.7; stroke-linecap: round; stroke-linejoin: round; }
.error-state { color: #b91c1c; }
.success-state { color: #047857; }

@media (max-width: 1120px) {
  .hero { align-items: flex-start; }
  .hero-side { align-items: flex-end; flex-direction: column-reverse; gap: 12px; }
  .course-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .lab-grid { grid-template-columns: repeat(3, minmax(0, 1fr)); }
}

@media (max-width: 800px) {
  .hero { flex-direction: column; }
  .hero-side { width: 100%; align-items: stretch; flex-direction: column-reverse; }
  .stats { justify-content: space-between; }
  .refresh-button { align-self: flex-start; }
  .quick-actions, .content-grid { grid-template-columns: 1fr; }
  .lab-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
}

@media (max-width: 520px) {
  .dashboard { padding: 14px; }
  .hero, .panel { padding: 18px; border-radius: 17px; }
  .stats { gap: 10px; }
  .stat-item strong { font-size: 20px; }
  .quick-copy small, .task-row time { display: none; }
  .course-grid, .lab-grid { grid-template-columns: 1fr; }
  .panel-header { align-items: flex-end; }
}
</style>
