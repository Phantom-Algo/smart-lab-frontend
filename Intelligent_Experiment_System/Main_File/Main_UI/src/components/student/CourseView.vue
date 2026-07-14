<template>
  <div class="course-page">
    <div class="page-header">
      <h2>我的课程</h2>
      <p>查看所选实验课程与教学安排</p>
    </div>

    <!-- 周课表 -->
    <div class="schedule-card">
      <div class="card-header">
        <h3>课程表</h3>
        <div class="week-nav">
          <span class="week-label">第 {{ currentWeek }} 周</span>
        </div>
      </div>
      <div class="schedule-table">
        <div class="schedule-header">
          <div class="time-col"></div>
          <div v-for="day in weekDays" :key="day.key" class="day-col" :class="{ today: day.key === todayWeekday }">
            <span class="day-name">{{ day.label }}</span>
            <span class="day-date">{{ day.date }}</span>
          </div>
        </div>
        <div class="schedule-body">
          <div v-for="slot in timeSlots" :key="slot" class="schedule-row">
            <div class="time-col">{{ slot }}</div>
            <div
              v-for="day in weekDays"
              :key="day.key"
              class="day-col"
              :class="{ today: day.key === todayWeekday }"
            >
              <div
                v-for="course in getSlotCourses(day.key, slot)"
                :key="course.id"
                class="course-block"
                :style="{ '--course-color': course.color }"
                @click="openDetail(course)"
              >
                <span class="block-name">{{ course.name }}</span>
                <span class="block-room">{{ course.room }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 课程详情卡片 -->
    <div class="section-title-row">
      <h3>课程详情</h3>
    </div>
    <div class="course-cards">
      <div
        v-for="course in courses"
        :key="course.id"
        class="detail-card"
        :style="{ '--course-color': course.color }"
        @click="openDetail(course)"
      >
        <div class="detail-top">
          <div class="detail-icon" :style="{ background: course.color }">
            <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="#fff" stroke-width="1.8">
              <path d="M4 19.5A2.5 2.5 0 016.5 17H20"/><path d="M6.5 2H20v20H6.5A2.5 2.5 0 014 19.5v-15A2.5 2.5 0 016.5 2z"/>
            </svg>
          </div>
          <div class="detail-status" :style="{ color: course.color }">
            {{ course.status === 'ongoing' ? '进行中' : '即将开始' }}
          </div>
        </div>
        <h4 class="detail-name">{{ course.name }}</h4>
        <div class="detail-meta">
          <div class="meta-row">
            <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2"><path d="M20 21v-2a4 4 0 00-4-4H8a4 4 0 00-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>
            <span>{{ course.teacher }}</span>
          </div>
          <div class="meta-row">
            <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="4" width="18" height="18" rx="2"/><line x1="16" y1="2" x2="16" y2="6"/><line x1="8" y1="2" x2="8" y2="6"/><line x1="3" y1="10" x2="21" y2="10"/></svg>
            <span>{{ course.scheduleText }}</span>
          </div>
          <div class="meta-row">
            <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0118 0z"/><circle cx="12" cy="10" r="3"/></svg>
            <span>{{ course.room }}</span>
          </div>
        </div>
        <div class="detail-progress">
          <div class="progress-bar">
            <div class="progress-fill" :style="{ width: course.progress + '%', background: course.color }"></div>
          </div>
          <span class="progress-text">进度 {{ course.progress }}%</span>
        </div>
      </div>
    </div>

    <!-- 课程详情弹窗 -->
    <teleport to="body">
      <transition name="popup-fade">
        <div v-if="selectedCourse" class="popup-overlay" @click.self="closeDetail">
          <div class="popup-panel">
            <div class="popup-header">
              <div class="popup-header-left">
                <div class="popup-icon" :style="{ background: selectedCourse.color }">
                  <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="#fff" stroke-width="1.8"><path d="M4 19.5A2.5 2.5 0 016.5 17H20"/><path d="M6.5 2H20v20H6.5A2.5 2.5 0 014 19.5v-15A2.5 2.5 0 016.5 2z"/></svg>
                </div>
                <div>
                  <h3 class="popup-title">{{ selectedCourse.name }}</h3>
                  <p class="popup-subtitle">{{ selectedCourse.teacher }} | {{ selectedCourse.room }}</p>
                </div>
              </div>
              <button class="popup-close" @click="closeDetail">
                <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2"><path d="M18 6L6 18M6 6l12 12"/></svg>
              </button>
            </div>

            <div class="popup-body">
              <!-- 课程信息 -->
              <div class="popup-section">
                <h4 class="popup-section-title">课程信息</h4>
                <div class="popup-info-grid">
                  <div class="popup-info-item"><span class="pilabel">授课教师</span><span class="pivalue">{{ selectedCourse.teacher }}</span></div>
                  <div class="popup-info-item"><span class="pilabel">上课时间</span><span class="pivalue">{{ selectedCourse.scheduleText }}</span></div>
                  <div class="popup-info-item"><span class="pilabel">上课地点</span><span class="pivalue">{{ selectedCourse.room }}</span></div>
                  <div class="popup-info-item"><span class="pilabel">课程进度</span><span class="pivalue" :style="{ color: selectedCourse.color }">{{ selectedCourse.progress }}%</span></div>
                </div>
              </div>

              <!-- 实验任务 -->
              <div class="popup-section">
                <h4 class="popup-section-title">实验任务</h4>
                <div class="task-list">
                  <div v-for="task in currentTasks" :key="task.id" class="task-item">
                    <span class="task-status" :class="task.done ? 'done' : 'pending'">
                      {{ task.done ? '已完成' : '待完成' }}
                    </span>
                    <span class="task-name">{{ task.name }}</span>
                    <span class="task-deadline">{{ task.deadline }}</span>
                  </div>
                </div>
              </div>

              <!-- 教学资源 -->
              <div class="popup-section">
                <h4 class="popup-section-title">教学资源</h4>
                <div class="resource-list">
                  <div v-for="res in currentResources" :key="res.id" class="resource-item">
                    <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2"><path d="M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8z"/><polyline points="14 2 14 8 20 8"/></svg>
                    <span class="resource-name">{{ res.name }}</span>
                    <span class="resource-type">{{ res.type }}</span>
                  </div>
                </div>
              </div>

              <!-- 成绩记录 -->
              <div class="popup-section">
                <h4 class="popup-section-title">成绩记录</h4>
                <div class="grade-list" v-if="currentGrades.length > 0">
                  <div v-for="g in currentGrades" :key="g.id" class="grade-item">
                    <span class="grade-name">{{ g.name }}</span>
                    <span class="grade-score" :class="g.score >= 80 ? 'high' : g.score >= 60 ? 'mid' : 'low'">{{ g.score }}</span>
                  </div>
                </div>
                <div v-else class="empty-text">暂无成绩记录</div>
              </div>
            </div>

            <div class="popup-footer">
              <button class="popup-btn cancel" @click="closeDetail">关闭</button>
            </div>
          </div>
        </div>
      </transition>
    </teleport>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { getCoursePage, type CourseListItem } from '../../utils/teachingApi'
import { isMockMode } from '../../stores/authStore'

interface Course {
  id: number; name: string; teacher: string; room: string
  weekdays: number[]; startSlot: number; endSlot: number
  progress: number; status: string; color: string
  scheduleText: string
}

interface Task { id: number; name: string; deadline: string; done: boolean }
interface Resource { id: number; name: string; type: string }
interface Grade { id: number; name: string; score: number }

const selectedCourse = ref<Course | null>(null)
const courses = ref<Course[]>([])

const MOCK_COURSES: Course[] = [
  { id: 1, name: '有机化学实验', teacher: '李老师', room: 'A201有机化学实验室', weekdays: [1, 3], startSlot: 1, endSlot: 2, progress: 60, status: 'ongoing', color: '#4A90D9', scheduleText: '周一、周三 08:00-10:00' },
  { id: 2, name: '大学物理实验', teacher: '王老师', room: 'B103物理实验室', weekdays: [2, 4], startSlot: 1, endSlot: 2, progress: 30, status: 'ongoing', color: '#27AE60', scheduleText: '周二、周四 08:00-10:00' },
  { id: 3, name: '计算机实验', teacher: '张老师', room: 'C202计算机实验室', weekdays: [3], startSlot: 3, endSlot: 4, progress: 80, status: 'ongoing', color: '#E67E22', scheduleText: '周三 14:00-16:00' },
  { id: 4, name: '物理实验', teacher: '王老师', room: 'B103物理实验室', weekdays: [5], startSlot: 3, endSlot: 4, progress: 5, status: 'ongoing', color: '#8E44AD', scheduleText: '周五 14:00-16:00' },
]

async function loadCourses() {
  if (isMockMode.value) { courses.value = [...MOCK_COURSES]; return }
  try {
    const res = await getCoursePage({ pageNum: 1, pageSize: 50 })
    if (res.records.length > 0) {
      const colors = ['#4A90D9', '#27AE60', '#E67E22', '#8E44AD']
      courses.value = res.records.map((c, i) => ({
        id: c.id, name: c.courseName, teacher: c.teacherName || '未分配', room: c.labName || '未分配',
        weekdays: c.weekday ? [c.weekday] : [], startSlot: 1, endSlot: 2,
        progress: c.status === 2 ? 50 : c.status === 3 ? 100 : 5, status: c.status === 2 ? 'ongoing' : 'upcoming',
        color: colors[i % colors.length], scheduleText: c.timeSlot ? `${c.weekdayName || ''} ${c.timeSlot}` : ''
      }))
    } else courses.value = [...MOCK_COURSES]
  } catch { /* API 不可用，请检查后端服务 */ }
}

onMounted(() => loadCourses())

const courseExtras: Record<number, { tasks: Task[]; resources: Resource[]; grades: Grade[] }> = {
  1: {
    tasks: [
      { id: 1, name: '实验一：有机化合物的分离与纯化', deadline: '截止: 7月12日', done: false },
      { id: 2, name: '实验二：红外光谱分析', deadline: '截止: 7月20日', done: false },
      { id: 3, name: '实验安全预习报告', deadline: '截止: 7月8日', done: true },
    ],
    resources: [
      { id: 1, name: '有机化学实验指导书', type: 'PDF' },
      { id: 2, name: '实验一操作演示视频', type: 'MP4' },
      { id: 3, name: '有机化学实验安全规范', type: 'DOCX' },
    ],
    grades: [
      { id: 1, name: '实验一操作评分', score: 88 },
      { id: 2, name: '实验报告评分', score: 92 },
    ],
  },
  2: {
    tasks: [
      { id: 4, name: '实验一：力学基本测量', deadline: '截止: 7月14日', done: false },
      { id: 5, name: '实验二：电磁学实验', deadline: '截止: 7月22日', done: false },
    ],
    resources: [
      { id: 4, name: '大学物理实验教程', type: 'PDF' },
      { id: 5, name: '力学实验数据模板', type: 'XLSX' },
    ],
    grades: [
      { id: 3, name: '力学实验评分', score: 75 },
    ],
  },
  3: {
    tasks: [
      { id: 6, name: '实验一：Python 数据分析基础', deadline: '截止: 7月18日', done: false },
      { id: 7, name: '实验二：数据库设计与查询', deadline: '截止: 7月25日', done: false },
    ],
    resources: [
      { id: 6, name: 'Python 编程入门', type: 'PDF' },
      { id: 7, name: '实验数据集', type: 'ZIP' },
      { id: 8, name: '数据库设计规范', type: 'DOCX' },
    ],
    grades: [
      { id: 4, name: 'Python 编程作业', score: 95 },
      { id: 5, name: 'SQL 语句考核', score: 90 },
    ],
  },
  4: {
    tasks: [
      { id: 8, name: '实验一：光学基础实验', deadline: '截止: 7月16日', done: false },
    ],
    resources: [
      { id: 9, name: '物理实验基础教程', type: 'PDF' },
    ],
    grades: [],
  },
}

const currentTasks = computed(() => selectedCourse.value ? (courseExtras[selectedCourse.value.id]?.tasks || []) : [])
const currentResources = computed(() => selectedCourse.value ? (courseExtras[selectedCourse.value.id]?.resources || []) : [])
const currentGrades = computed(() => selectedCourse.value ? (courseExtras[selectedCourse.value.id]?.grades || []) : [])

function openDetail(course: Course): void { selectedCourse.value = course }
function closeDetail(): void { selectedCourse.value = null }

const weekDays = computed(() => {
  const semStart = new Date(2026, 1, 23) // 春季学期：2026-02-23
  const today = new Date()
  const dayOfWeek = today.getDay() || 7 // Sunday=7
  const monday = new Date(today)
  monday.setDate(today.getDate() - dayOfWeek + 1) // 本周一
  const names = ['周一', '周二', '周三', '周四', '周五']
  return [1, 2, 3, 4, 5].map((key, i) => {
    const d = new Date(monday)
    d.setDate(monday.getDate() + i)
    return { key, label: names[i], date: `${d.getMonth() + 1}/${d.getDate()}` }
  })
})

const currentWeek = computed(() => {
  const semStart = new Date(2026, 1, 23)
  const today = new Date()
  const diffMs = today.getTime() - semStart.getTime()
  return Math.max(1, Math.floor(diffMs / (7 * 24 * 60 * 60 * 1000)) + 1)
})

const timeSlots = ['08:00', '10:00', '12:00', '14:00', '16:00', '18:00']
const todayWeekday = new Date().getDay() || 7 // Sunday=0 -> 7, but we only show 1-5

function getSlotCourses(dayKey: number, slot: string): Course[] {
  const slotIndex = timeSlots.indexOf(slot)
  if (slotIndex === -1 || slotIndex >= timeSlots.length - 1) return []
  return courses.value.filter(c =>
    c.weekdays.includes(dayKey) &&
    slotIndex >= c.startSlot - 1 &&
    slotIndex < c.endSlot - 1
  )
}
</script>

<style scoped>
.course-page { height: 100%; overflow-y: auto; padding: 28px 36px; }

.page-header { margin-bottom: 24px; }
.page-header h2 { font-size: 22px; font-weight: 700; color: #1a1a2e; margin: 0 0 4px; }
.page-header p { font-size: 14px; color: #888; margin: 0; }

/* ===== 课表 ===== */
.schedule-card { background: rgba(255,255,255,.5); border-radius: 14px; padding: 20px 22px; margin-bottom: 28px; border: 1px solid rgba(0,0,0,.04); }

.card-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 16px; }
.card-header h3 { font-size: 15px; font-weight: 600; color: #1a1a2e; margin: 0; }

.week-nav { display: flex; align-items: center; gap: 12px; }
.week-label { font-size: 13px; color: #1a73e8; font-weight: 500; }

.schedule-header { display: flex; margin-bottom: 0; }
.time-col { width: 60px; flex-shrink: 0; }
.day-col { flex: 1; text-align: center; padding: 8px 4px 10px; }
.day-col.today { background: rgba(26,115,232,.06); border-radius: 8px; }
.day-name { display: block; font-size: 13px; font-weight: 600; color: #555; }
.day-date { display: block; font-size: 11px; color: #aaa; margin-top: 2px; }
.day-col.today .day-name { color: #1a73e8; }

.schedule-body { }
.schedule-row { display: flex; border-top: 1px solid rgba(0,0,0,.04); min-height: 64px; }
.schedule-row .time-col { font-size: 12px; color: #aaa; padding: 8px 0 0; }
.schedule-row .day-col { position: relative; padding: 4px; }

.course-block {
  padding: 6px 8px; border-radius: 6px; margin-bottom: 3px;
  background: color-mix(in srgb, var(--course-color) 12%, transparent);
  border-left: 3px solid var(--course-color);
  cursor: pointer; transition: all .15s;
}
.course-block:hover { filter: brightness(.95); }
.block-name { display: block; font-size: 12px; font-weight: 600; color: #333; }
.block-room { display: block; font-size: 10px; color: #999; margin-top: 1px; }

/* ===== 详情卡片 ===== */
.section-title-row { margin-bottom: 16px; }
.section-title-row h3 { font-size: 15px; font-weight: 600; color: #1a1a2e; margin: 0; }

.course-cards { display: grid; grid-template-columns: repeat(2, 1fr); gap: 14px; }

.detail-card { background: rgba(255,255,255,.5); border-radius: 14px; padding: 20px 22px; border: 1px solid rgba(0,0,0,.04); transition: all .18s; cursor: pointer; }
.detail-card:hover { background: rgba(255,255,255,.8); box-shadow: 0 4px 16px rgba(0,0,0,.06); }

.detail-top { display: flex; align-items: center; justify-content: space-between; margin-bottom: 12px; }

.detail-icon { width: 40px; height: 40px; border-radius: 10px; display: flex; align-items: center; justify-content: center; }

.detail-status { font-size: 12px; font-weight: 600; }

.detail-name { font-size: 16px; font-weight: 600; color: #1a1a2e; margin: 0 0 14px; }

.detail-meta { display: flex; flex-direction: column; gap: 8px; margin-bottom: 16px; }

.meta-row { display: flex; align-items: center; gap: 8px; font-size: 13px; color: #777; }

.detail-progress { display: flex; align-items: center; gap: 10px; }

.progress-bar { flex: 1; height: 6px; background: rgba(0,0,0,.06); border-radius: 3px; overflow: hidden; }
.progress-fill { height: 100%; border-radius: 3px; transition: width .3s; }
.progress-text { font-size: 12px; color: #999; white-space: nowrap; }

/* ===== 弹窗 ===== */
.popup-overlay { position: fixed; inset: 0; z-index: 1000; background: rgba(0,0,0,.3); backdrop-filter: blur(4px); -webkit-backdrop-filter: blur(4px); display: flex; align-items: center; justify-content: center; }

.popup-panel { width: 620px; max-width: 92vw; max-height: 85vh; background: #fff; border-radius: 18px; box-shadow: 0 20px 60px rgba(0,0,0,.15); overflow: hidden; display: flex; flex-direction: column; }

.popup-header { display: flex; align-items: flex-start; justify-content: space-between; padding: 24px 28px 0; }
.popup-header-left { display: flex; align-items: center; gap: 14px; }
.popup-icon { width: 44px; height: 44px; border-radius: 12px; display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
.popup-title { font-size: 18px; font-weight: 700; color: #1a1a2e; margin: 0 0 2px; }
.popup-subtitle { font-size: 13px; color: #888; margin: 0; }

.popup-close { width: 32px; height: 32px; display: flex; align-items: center; justify-content: center; border: none; background: none; border-radius: 8px; cursor: pointer; color: #999; flex-shrink: 0; }
.popup-close:hover { background: #f5f5f5; color: #333; }

.popup-body { padding: 20px 28px; overflow-y: auto; flex: 1; }

.popup-section { margin-bottom: 20px; }
.popup-section-title { font-size: 14px; font-weight: 600; color: #888; text-transform: uppercase; letter-spacing: .5px; margin: 0 0 12px; padding-bottom: 8px; border-bottom: 1px solid rgba(0,0,0,.05); }

.popup-info-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 10px 20px; }
.popup-info-item { display: flex; flex-direction: column; gap: 2px; }
.pilabel { font-size: 12px; color: #aaa; }
.pivalue { font-size: 14px; color: #333; font-weight: 500; }

/* 任务列表 */
.task-list { display: flex; flex-direction: column; gap: 8px; }
.task-item { display: flex; align-items: center; gap: 10px; }
.task-status { font-size: 11px; font-weight: 600; padding: 2px 8px; border-radius: 4px; white-space: nowrap; }
.task-status.pending { background: rgba(243,156,18,.1); color: #e67e22; }
.task-status.done { background: rgba(39,174,96,.1); color: #27ae60; }
.task-name { flex: 1; font-size: 13px; color: #333; }
.task-deadline { font-size: 11px; color: #bbb; white-space: nowrap; }

/* 资源列表 */
.resource-list { display: flex; flex-direction: column; gap: 8px; }
.resource-item { display: flex; align-items: center; gap: 10px; padding: 8px 12px; background: rgba(0,0,0,.02); border-radius: 8px; cursor: pointer; transition: background .15s; }
.resource-item:hover { background: rgba(0,0,0,.05); }
.resource-name { flex: 1; font-size: 13px; color: #333; }
.resource-type { font-size: 11px; color: #fff; background: #888; padding: 1px 6px; border-radius: 3px; font-weight: 600; }

/* 成绩 */
.grade-list { display: flex; flex-direction: column; gap: 6px; }
.grade-item { display: flex; align-items: center; justify-content: space-between; padding: 6px 0; }
.grade-name { font-size: 13px; color: #333; }
.grade-score { font-size: 16px; font-weight: 700; }
.grade-score.high { color: #27ae60; }
.grade-score.mid { color: #e67e22; }
.grade-score.low { color: #e74c3c; }

.empty-text { font-size: 13px; color: #ccc; padding: 8px 0; }

.popup-footer { padding: 12px 28px 20px; display: flex; justify-content: flex-end; }
.popup-btn { padding: 10px 28px; border-radius: 8px; font-size: 14px; cursor: pointer; border: none; transition: all .15s; }
.popup-btn.cancel { background: rgba(0,0,0,.04); color: #666; }
.popup-btn.cancel:hover { background: rgba(0,0,0,.08); }
</style>

<style>
.popup-fade-enter-active, .popup-fade-leave-active { transition: all .2s ease; }
.popup-fade-enter-from, .popup-fade-leave-to { opacity: 0; }
.popup-fade-enter-from .popup-panel, .popup-fade-leave-to .popup-panel { transform: scale(.95); }
</style>
