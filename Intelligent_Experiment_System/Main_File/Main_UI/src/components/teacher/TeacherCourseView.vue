<template>
  <div class="teacher-course">
    <div class="page-header">
      <h2>我的课程</h2>
      <p>管理所授实验课程</p>
    </div>

    <!-- 统计 -->
    <div class="stats-row">
      <div class="stat-card">
        <span class="stat-num">{{ courses.length }}</span>
        <span class="stat-label">授课课程</span>
      </div>
      <div class="stat-card">
        <span class="stat-num">{{ totalStudents }}</span>
        <span class="stat-label">学生总数</span>
      </div>
      <div class="stat-card">
        <span class="stat-num">{{ totalTasks }}</span>
        <span class="stat-label">实验任务</span>
      </div>
    </div>

    <!-- 周课表 -- 与学生端相同格式 -->
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
          <div v-for="day in weekDays" :key="day.key" class="day-col" :class="{ today: day.isToday }">
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
              :class="{ today: day.isToday }"
            >
              <div
                v-for="course in getSlotCourses(day.key, slot)"
                :key="course.id"
                class="course-block"
                :style="{ '--course-color': course.color }"
              >
                <span class="block-name">{{ course.shortName }}</span>
                <span class="block-room">{{ course.lab }}</span>
                <span class="block-class">{{ course.className }}</span>
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
    <div class="course-list">
      <div v-for="c in courses" :key="c.id" class="course-card" :style="{ '--course-color': c.color }">
        <div class="card-top">
          <div class="card-left">
            <div class="card-icon" :style="{ background: c.color }">
              <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="#fff" stroke-width="1.8">
                <path d="M4 19.5A2.5 2.5 0 016.5 17H20"/><path d="M6.5 2H20v20H6.5A2.5 2.5 0 014 19.5v-15A2.5 2.5 0 016.5 2z"/>
              </svg>
            </div>
            <div>
              <div class="card-name">{{ c.name }}</div>
              <div class="card-meta">{{ c.lab }} · {{ c.className }} · {{ c.studentCount }}人</div>
            </div>
          </div>
          <div class="card-tags">
            <span class="tag-item" v-for="(s, i) in c.schedule" :key="i">{{ s }}</span>
          </div>
        </div>
        <div class="card-progress">
          <div class="progress-item">
            <span class="progress-label">任务进度</span>
            <div class="progress-bar">
              <div class="progress-fill" :style="{ width: c.taskProgress + '%', background: c.color }"></div>
            </div>
            <span class="progress-text">{{ c.doneTasks }}/{{ c.totalTasks }}</span>
          </div>
          <div class="progress-item">
            <span class="progress-label">批改进度</span>
            <div class="progress-bar">
              <div class="progress-fill orange" :style="{ width: c.gradeProgress + '%' }"></div>
            </div>
            <span class="progress-text">{{ c.gradedCount }}/{{ c.submitCount }}</span>
          </div>
        </div>
        <div class="card-actions">
          <button class="action-btn" @click="$emit('navigate', { key: 'my-task', name: '实验任务' })">管理任务</button>
          <button class="action-btn primary" @click="$emit('navigate', { key: 'my-score', name: '成绩评定' })">批改成绩</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, onMounted } from 'vue'
import { getCoursePage, type CourseListItem } from '../../utils/teachingApi'
import { isMockMode } from '../../stores/authStore'

defineEmits<{ navigate: [payload: { key: string; name: string }] }>()

interface CourseDay { day: number; startSlot: number; endSlot: number }

interface Course {
  id: number; name: string; shortName: string; lab: string; className: string; studentCount: number
  schedule: string[]; days: CourseDay[]
  taskProgress: number; doneTasks: number; totalTasks: number
  gradeProgress: number; gradedCount: number; submitCount: number
  color: string; scheduleText: string
}

const MOCK_COURSES: Course[] = [
  { id: 1, name: '有机化学实验', shortName: '有机化学', lab: 'A201', className: '化工2401班', studentCount: 32, schedule: ['周三 08:00-10:00', '周五 14:00-17:00'], days: [{ day: 3, startSlot: 1, endSlot: 2 }, { day: 5, startSlot: 4, endSlot: 5 }], taskProgress: 75, doneTasks: 6, totalTasks: 8, gradeProgress: 60, gradedCount: 9, submitCount: 15, color: '#4A90D9', scheduleText: '周三 08:00-10:00、周五 14:00-17:00' },
  { id: 2, name: '有机化学实验', shortName: '有机化学', lab: 'A201', className: '化工2402班', studentCount: 30, schedule: ['周一 08:00-10:00', '周四 14:00-17:00'], days: [{ day: 1, startSlot: 1, endSlot: 2 }, { day: 4, startSlot: 4, endSlot: 5 }], taskProgress: 60, doneTasks: 5, totalTasks: 8, gradeProgress: 45, gradedCount: 7, submitCount: 16, color: '#4A90D9', scheduleText: '周一 08:00-10:00、周四 14:00-17:00' },
  { id: 3, name: '有机化学实验', shortName: '有机化学', lab: 'A201', className: '应化2401班', studentCount: 28, schedule: ['周二 08:00-10:00', '周五 08:00-10:00'], days: [{ day: 2, startSlot: 1, endSlot: 2 }, { day: 5, startSlot: 1, endSlot: 2 }], taskProgress: 80, doneTasks: 6, totalTasks: 8, gradeProgress: 70, gradedCount: 10, submitCount: 14, color: '#4A90D9', scheduleText: '周二 08:00-10:00、周五 08:00-10:00' },
]

const courses = ref<Course[]>([])

console.log('[TeacherCourseView] 组件已加载')

async function loadCourses() {
  console.log('[TeacherCourseView] loadCourses 开始, isMockMode:', isMockMode.value)
  if (isMockMode.value) { courses.value = [...MOCK_COURSES]; return }
  try {
    const res = await getCoursePage({ pageNum: 1, pageSize: 100 })
    console.log('[TeacherCourseView] API 返回:', res.records.length, '条课程')
    if (res.records.length > 0) {
      const colors = ['#4A90D9', '#27AE60', '#E67E22', '#8E44AD']
      courses.value = res.records.map((c, i) => ({
        id: c.id,
        name: c.courseName,
        shortName: c.courseName.length > 4 ? c.courseName.slice(0, 4) : c.courseName,
        lab: c.labName || '未分配',
        className: c.departmentName || '',
        studentCount: c.enrolledCount || 0,
        schedule: c.timeSlot ? [`${c.weekdayName || ''} ${c.timeSlot}`] : [],
        days: c.weekday ? [{ day: c.weekday, startSlot: 1, endSlot: 2 }] : [],
        taskProgress: 50, doneTasks: 4, totalTasks: 8,
        gradeProgress: 40, gradedCount: 5, submitCount: c.enrolledCount || 0,
        color: colors[i % colors.length],
        scheduleText: c.timeSlot ? `${c.weekdayName || ''} ${c.timeSlot}` : ''
      }))
      console.log('[TeacherCourseView] 已映射', courses.value.length, '条课程')
    } else {
      console.log('[TeacherCourseView] API 返回空列表，使用 Mock 数据')
      courses.value = [...MOCK_COURSES]
    }
  } catch (e) {
    console.error('[TeacherCourseView] API 调用失败:', e)
  }
}

onMounted(() => loadCourses())

const totalStudents = computed(() => courses.value.reduce((s, c) => s + c.studentCount, 0))
const totalTasks = computed(() => courses.value.reduce((s, c) => s + c.totalTasks, 0))

// 课表数据
const weekDays = computed(() => {
  const today = new Date()
  const dayOfWeek = today.getDay() || 7
  const monday = new Date(today)
  monday.setDate(today.getDate() - dayOfWeek + 1)
  const names = ['周一', '周二', '周三', '周四', '周五']
  return [1, 2, 3, 4, 5].map((key, i) => {
    const d = new Date(monday)
    d.setDate(monday.getDate() + i)
    return { key, label: names[i], date: `${d.getMonth() + 1}/${d.getDate()}`, isToday: today.getDay() === key }
  })
})

const currentWeek = computed(() => {
  const semStart = new Date(2026, 1, 23)
  const today = new Date()
  const diffMs = today.getTime() - semStart.getTime()
  return Math.max(1, Math.floor(diffMs / (7 * 24 * 60 * 60 * 1000)) + 1)
})

const timeSlots = ['08:00', '10:00', '12:00', '14:00', '16:00', '18:00']

function getSlotCourses(dayKey: number, slot: string): Course[] {
  const slotIndex = timeSlots.indexOf(slot)
  if (slotIndex === -1 || slotIndex >= timeSlots.length - 1) return []
  return courses.value.filter(c => {
    return c.days.some(d => d.day === dayKey && slotIndex === d.startSlot - 1)
  })
}
</script>

<style scoped>
.teacher-course { height: 100%; overflow-y: auto; padding: 28px 36px; }

.page-header { margin-bottom: 18px; }
.page-header h2 { font-size: 22px; font-weight: 700; color: #1a1a2e; margin: 0 0 4px; }
.page-header p { font-size: 14px; color: #888; margin: 0; }

/* ===== 统计 ===== */
.stats-row { display: flex; gap: 12px; margin-bottom: 20px; }
.stat-card { flex: 1; background: rgba(255,255,255,.5); border-radius: 12px; padding: 16px 20px; text-align: center; border: 1px solid rgba(0,0,0,.04); }
.stat-num { font-size: 26px; font-weight: 700; color: #1a73e8; display: block; line-height: 1.2; }
.stat-label { font-size: 12px; color: #999; }

/* ===== 课表 ===== */
.schedule-card { background: rgba(255,255,255,.5); border-radius: 14px; padding: 20px 22px; margin-bottom: 20px; border: 1px solid rgba(0,0,0,.04); }

.card-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 16px; }
.card-header h3 { font-size: 15px; font-weight: 600; color: #1a1a2e; margin: 0; }
.week-nav { display: flex; align-items: center; gap: 12px; }
.week-label { font-size: 13px; color: #1a73e8; font-weight: 500; }

.schedule-header { display: flex; margin-bottom: 0; }
.time-col { width: 60px; flex-shrink: 0; }
.day-col { flex: 1; text-align: center; padding: 8px 4px 10px; }
.day-col.today { background: rgba(26,115,232,.06); border-radius: 8px; }
.day-name { display: block; font-size: 13px; font-weight: 600; color: #555; }
.day-date { display: block; font-size: 11px; color: #888; margin-top: 2px; }
.day-col.today .day-name { color: #1a73e8; }

.schedule-body { }
.schedule-row { display: flex; border-top: 1px solid rgba(0,0,0,.04); min-height: 68px; }
.schedule-row .time-col { font-size: 12px; color: #aaa; padding: 8px 0 0; }
.schedule-row .day-col { position: relative; padding: 4px; }

.course-block {
  padding: 7px 9px; border-radius: 6px;
  background: color-mix(in srgb, var(--course-color) 12%, transparent);
  border-left: 3px solid var(--course-color);
  cursor: default; transition: all .15s;
  display: flex; flex-direction: column; gap: 1px;
}
.course-block:hover { filter: brightness(.95); }
.block-name { font-size: 12px; font-weight: 600; color: #333; line-height: 1.3; }
.block-room { font-size: 10px; color: #888; }
.block-class { font-size: 10px; color: #aaa; }

/* ===== 课程详情列表 ===== */
.section-title-row { margin-bottom: 14px; }
.section-title-row h3 { font-size: 15px; font-weight: 600; color: #1a1a2e; margin: 0; }

.course-list { display: flex; flex-direction: column; gap: 14px; }
.course-card { background: rgba(255,255,255,.5); border-radius: 14px; border: 1px solid rgba(0,0,0,.04); padding: 18px 22px; border-left: 4px solid var(--course-color); }

.card-top { display: flex; align-items: flex-start; justify-content: space-between; margin-bottom: 14px; }
.card-left { display: flex; align-items: center; gap: 12px; }
.card-icon { width: 42px; height: 42px; border-radius: 10px; display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
.card-name { font-size: 16px; font-weight: 700; color: #1a1a2e; margin-bottom: 3px; }
.card-meta { font-size: 12px; color: #999; }

.card-tags { display: flex; gap: 6px; flex-wrap: wrap; }
.tag-item { font-size: 11px; padding: 3px 10px; border-radius: 6px; background: rgba(26,115,232,.08); color: #1a73e8; }

.card-progress { display: flex; flex-direction: column; gap: 8px; margin-bottom: 14px; padding: 12px; background: rgba(0,0,0,.015); border-radius: 8px; }
.progress-item { display: flex; align-items: center; gap: 10px; }
.progress-label { font-size: 12px; color: #888; width: 56px; flex-shrink: 0; }
.progress-bar { flex: 1; height: 6px; background: rgba(0,0,0,.06); border-radius: 3px; overflow: hidden; }
.progress-fill { height: 100%; border-radius: 3px; transition: width .3s; }
.progress-fill.orange { background: #e67e22; }
.progress-text { font-size: 11px; color: #aaa; width: 36px; text-align: right; }

.card-actions { display: flex; gap: 10px; justify-content: flex-end; }
.action-btn { padding: 8px 20px; border: 1px solid rgba(0,0,0,.1); border-radius: 8px; background: #fff; font-size: 13px; cursor: pointer; color: #555; transition: all .15s; }
.action-btn:hover { border-color: #1a73e8; color: #1a73e8; }
.action-btn.primary { background: linear-gradient(135deg,#1a73e8,#4a90d9); color: #fff; border: none; }
.action-btn.primary:hover { box-shadow: 0 4px 12px rgba(26,115,232,.3); }
</style>
