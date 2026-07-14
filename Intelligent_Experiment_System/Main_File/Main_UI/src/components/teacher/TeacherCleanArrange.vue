<template>
  <div class="tca-page">
    <div class="page-header">
      <h2>值日安排</h2>
      <p>查看学生值日排班与完成情况</p>
    </div>

    <!-- 统计卡片 + 实验室筛选 -->
    <div class="stats-row">
      <div class="stat-card">
        <div class="stat-icon today-icon">
          <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="4" width="18" height="18" rx="2"/><line x1="16" y1="2" x2="16" y2="6"/><line x1="8" y1="2" x2="8" y2="6"/><line x1="3" y1="10" x2="21" y2="10"/></svg>
        </div>
        <div class="stat-body">
          <span class="stat-num">{{ stats.todayTotal }}</span>
          <span class="stat-label">今日值日</span>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon done-icon">
          <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/><polyline points="22 4 12 14.01 9 11.01"/></svg>
        </div>
        <div class="stat-body">
          <span class="stat-num green">{{ stats.doneCount }}</span>
          <span class="stat-label">已完成</span>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon pending-icon">
          <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/></svg>
        </div>
        <div class="stat-body">
          <span class="stat-num orange">{{ stats.pendingCount }}</span>
          <span class="stat-label">未签到</span>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon eval-icon">
          <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2"><path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/><path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"/></svg>
        </div>
        <div class="stat-body">
          <span class="stat-num blue">{{ stats.unevaluatedCount }}</span>
          <span class="stat-label">待评价</span>
        </div>
      </div>
      <div class="stat-filter">
        <span class="filter-label">实验室</span>
        <select v-model="selectedLab" class="filter-select">
          <option value="">全部</option>
          <option v-for="lab in labOptions" :key="lab" :value="lab">{{ lab }}</option>
        </select>
      </div>
    </div>

    <!-- 周视图日历 -->
    <div class="week-card">
      <div class="week-nav">
        <button class="week-arrow" @click="prevWeek">&lt;</button>
        <span class="week-label">第 {{ currentWeek }} 周（{{ weekRange }}）</span>
        <button class="week-arrow" @click="nextWeek">&gt;</button>
      </div>
      <div class="week-grid">
        <div
          v-for="day in weekDays"
          :key="day.key"
          class="week-cell"
          :class="{ today: day.isToday, active: selectedDay === day.key }"
          @click="selectDay(day.key)"
        >
          <div class="cell-header">
            <span class="cell-day">{{ day.label }}</span>
            <span class="cell-date">{{ day.date }}</span>
          </div>
          <div class="cell-body" v-if="day.students.length > 0">
            <div v-for="s in day.students.slice(0, 3)" :key="s.name" class="cell-student">
              <span class="cell-name">{{ s.name }}</span>
              <span class="cell-status-dot" :class="'dot-' + s.status"></span>
            </div>
            <span v-if="day.students.length > 3" class="cell-more">+{{ day.students.length - 3 }}人</span>
          </div>
          <div class="cell-body rest-text" v-else>
            <span>无值日</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 当日详情 -->
    <div class="detail-section">
      <div class="section-header">
        <h3>{{ selectedDayLabel }} 值日详情</h3>
      </div>

      <div v-if="selectedDayStudents.length === 0" class="empty-state">该日无值日安排</div>

      <div v-for="group in groupedStudents" :key="group.lab" class="detail-group">
        <div class="group-header">
          <span class="group-lab">{{ group.lab }}</span>
          <span class="group-time">{{ group.timeSlot }}</span>
        </div>
        <div class="student-list">
          <div v-for="s in group.students" :key="s.name" class="student-row">
            <div class="student-info">
              <span class="student-name">{{ s.name }}</span>
              <span class="student-class" v-if="s.className">{{ s.className }}</span>
            </div>
            <div class="student-status-area">
              <span class="status-tag" :class="'tag-' + s.status">{{ statusLabel(s.status) }}</span>
              <span class="checkin-time" v-if="s.checkInTime">{{ s.checkInTime }} 签到</span>
            </div>
            <button
              v-if="s.status === 'done' || s.status === 'unevaluated'"
              class="eval-btn"
              @click="goEvaluate(s.name)"
            >检查评价</button>
          </div>
        </div>
      </div>
    </div>

    <!-- 本周统计 -->
    <div class="week-summary">
      <span class="summary-item">本周已签到 <strong class="green">{{ weekStats.done }}</strong> 次</span>
      <span class="summary-divider">|</span>
      <span class="summary-item">缺勤 <strong class="red">{{ weekStats.missed }}</strong> 次</span>
      <span class="summary-divider">|</span>
      <span class="summary-item">待评价 <strong class="blue">{{ weekStats.unevaluated }}</strong> 次</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'

const emit = defineEmits<{ navigate: [payload: { key: string; name: string }] }>()

// ========== 筛选 ==========
const selectedLab = ref('')
const labOptions = ['A201 有机化学实验室', 'B103 物理实验室', 'C202 计算机实验室']
const selectedDay = ref(3) // 默认周三(今天)
const weekOffset = ref(0)

// ========== 周视图计算 ==========
const baseMonday = new Date(2026, 6, 6)
const weekDayLabels = ['周一', '周二', '周三', '周四', '周五', '周六', '周日']

const currentWeek = computed(() => {
  const now = new Date()
  const startOfYear = new Date(now.getFullYear(), 0, 1)
  return Math.ceil(((now.getTime() + weekOffset.value * 7 * 86400000 - startOfYear.getTime()) / 86400000 + startOfYear.getDay() + 1) / 7)
})

const weekRange = computed(() => {
  const monday = new Date(baseMonday)
  monday.setDate(monday.getDate() + weekOffset.value * 7)
  const sunday = new Date(monday)
  sunday.setDate(sunday.getDate() + 6)
  const fmt = (d: Date) => `${d.getMonth() + 1}/${d.getDate()}`
  return `${fmt(monday)} - ${fmt(sunday)}`
})

interface DayStudent {
  name: string; status: 'done' | 'pending' | 'missed' | 'unevaluated'
  lab: string; timeSlot: string; className?: string; checkInTime?: string
}

interface WeekDay {
  key: number; label: string; date: string; isToday: boolean; students: DayStudent[]
}

// Mock 周数据
const allWeekData: WeekDay[] = [
  { key: 1, label: '周一', date: '7/6', isToday: false, students: [
    { name: '张同学', status: 'done', lab: 'A201', timeSlot: '08:00-10:00', className: '计科2401', checkInTime: '08:05' },
  ]},
  { key: 2, label: '周二', date: '7/7', isToday: false, students: [
    { name: '李同学', status: 'done', lab: 'A201', timeSlot: '08:00-10:00', className: '计科2401', checkInTime: '08:12' },
    { name: '赵同学', status: 'unevaluated', lab: 'B103', timeSlot: '14:00-16:00', className: '软工2402', checkInTime: '14:03' },
  ]},
  { key: 3, label: '周三', date: '7/8', isToday: true, students: [
    { name: '王同学', status: 'pending', lab: 'A201', timeSlot: '08:00-10:00', className: '计科2401' },
    { name: '张同学', status: 'done', lab: 'A201', timeSlot: '08:00-10:00', className: '计科2401', checkInTime: '08:15' },
    { name: '李同学', status: 'done', lab: 'B103', timeSlot: '14:00-16:00', className: '软工2402', checkInTime: '14:05' },
    { name: '刘同学', status: 'done', lab: 'C202', timeSlot: '10:00-12:00', className: '计科2401', checkInTime: '10:02' },
  ]},
  { key: 4, label: '周四', date: '7/9', isToday: false, students: [
    { name: '张同学', status: 'unevaluated', lab: 'A201', timeSlot: '08:00-10:00', className: '计科2401', checkInTime: '08:08' },
    { name: '李同学', status: 'done', lab: 'B103', timeSlot: '14:00-16:00', className: '软工2402', checkInTime: '14:10' },
  ]},
  { key: 5, label: '周五', date: '7/10', isToday: false, students: [
    { name: '王同学', status: 'done', lab: 'A201', timeSlot: '08:00-10:00', className: '计科2401', checkInTime: '08:00' },
    { name: '赵同学', status: 'missed', lab: 'B103', timeSlot: '14:00-16:00', className: '软工2402' },
  ]},
  { key: 6, label: '周六', date: '7/11', isToday: false, students: [] },
  { key: 7, label: '周日', date: '7/12', isToday: false, students: [] },
]

const weekDays = computed(() => allWeekData)

// 当日数据
const selectedDayLabel = computed(() => {
  const d = weekDays.value.find(w => w.key === selectedDay.value)
  return d ? `${d.label} ${d.date}` : ''
})

const selectedDayStudents = computed(() => {
  const d = weekDays.value.find(w => w.key === selectedDay.value)
  let list = d ? d.students : []
  if (selectedLab.value) list = list.filter(s => s.lab === selectedLab.value)
  return list
})

const groupedStudents = computed(() => {
  const groups: { lab: string; timeSlot: string; students: DayStudent[] }[] = []
  for (const s of selectedDayStudents.value) {
    let g = groups.find(g => g.lab === s.lab && g.timeSlot === s.timeSlot)
    if (!g) { g = { lab: s.lab, timeSlot: s.timeSlot, students: [] }; groups.push(g) }
    g.students.push(s)
  }
  return groups
})

// 统计
const stats = computed(() => {
  const today = weekDays.value.find(w => w.isToday)
  const list = today ? today.students : []
  return {
    todayTotal: list.length,
    doneCount: list.filter(s => s.status === 'done').length,
    pendingCount: list.filter(s => s.status === 'pending').length,
    unevaluatedCount: list.filter(s => s.status === 'unevaluated').length,
  }
})

const weekStats = computed(() => {
  let done = 0, missed = 0, unevaluated = 0
  for (const d of weekDays.value) {
    for (const s of d.students) {
      if (s.status === 'done') done++
      else if (s.status === 'missed') missed++
      else if (s.status === 'unevaluated') unevaluated++
    }
  }
  return { done, missed, unevaluated }
})

// ========== 方法 ==========
function prevWeek() { weekOffset.value-- }
function nextWeek() { weekOffset.value++ }
function selectDay(key: number) { selectedDay.value = key }

function statusLabel(s: string) {
  const m: Record<string, string> = { done: '已完成', pending: '未签到', missed: '缺勤', unevaluated: '待评价' }
  return m[s] || s
}

function goEvaluate(_studentName: string) {
  emit('navigate', { key: 'clean-evaluate', name: '检查评价' })
}
</script>

<style scoped>
.tca-page { height:100%; display:flex; flex-direction:column; overflow-y:auto; padding:20px 24px }

.page-header { margin-bottom:16px; flex-shrink:0 }
.page-header h2 { margin:0; font-size:20px; font-weight:600; color:#1a1a2e }
.page-header p { margin:4px 0 0; font-size:13px; color:#999 }

/* ===== 统计卡片行 ===== */
.stats-row { display:flex; align-items:center; gap:12px; margin-bottom:16px; flex-shrink:0; flex-wrap:wrap }
.stat-card { display:flex; align-items:center; gap:10px; background:rgba(255,255,255,.65); backdrop-filter:blur(10px); border:1px solid rgba(0,0,0,.06); border-radius:12px; padding:12px 18px; min-width:130px }
.stat-icon { width:36px; height:36px; border-radius:10px; display:flex; align-items:center; justify-content:center; flex-shrink:0 }
.today-icon { background:rgba(26,115,232,.1); color:#1a73e8 }
.done-icon { background:rgba(39,174,96,.1); color:#27ae60 }
.pending-icon { background:rgba(243,156,18,.1); color:#f39c12 }
.eval-icon { background:rgba(26,115,232,.1); color:#1a73e8 }
.stat-body { display:flex; flex-direction:column; align-items:flex-start }
.stat-num { font-size:20px; font-weight:700; color:#1a1a2e; line-height:1.1 }
.stat-num.green { color:#27ae60 }
.stat-num.orange { color:#f39c12 }
.stat-num.blue { color:#1a73e8 }
.stat-label { font-size:12px; color:#999 }

.stat-filter { display:flex; align-items:center; gap:8px; margin-left:auto }
.filter-label { font-size:13px; color:#888 }
.filter-select { padding:8px 12px; border:1px solid rgba(0,0,0,.1); border-radius:10px; font-size:13px; color:#555; outline:none; background:rgba(255,255,255,.7); backdrop-filter:blur(10px); cursor:pointer }

/* ===== 周视图日历 ===== */
.week-card { background:rgba(255,255,255,.65); backdrop-filter:blur(10px); border:1px solid rgba(0,0,0,.06); border-radius:14px; padding:16px 20px; margin-bottom:16px; flex-shrink:0 }
.week-nav { display:flex; align-items:center; justify-content:center; gap:16px; margin-bottom:12px }
.week-arrow { width:32px; height:32px; border:1px solid rgba(0,0,0,.1); border-radius:8px; background:#fff; font-size:14px; cursor:pointer; display:flex; align-items:center; justify-content:center; color:#555; transition:.15s }
.week-arrow:hover { border-color:#1a73e8; color:#1a73e8 }
.week-label { font-size:14px; font-weight:500; color:#333 }

.week-grid { display:grid; grid-template-columns:repeat(7,1fr); gap:6px }
.week-cell { background:rgba(0,0,0,.02); border-radius:10px; padding:10px; text-align:center; cursor:pointer; transition:.15s; border:2px solid transparent; min-height:90px }
.week-cell:hover { background:rgba(0,0,0,.04) }
.week-cell.today { background:rgba(26,115,232,.04); border-color:rgba(26,115,232,.15) }
.week-cell.active { border-color:#1a73e8 }

.cell-header { margin-bottom:6px }
.cell-day { font-size:12px; font-weight:500; color:#555; display:block }
.cell-date { font-size:11px; color:#aaa }

.cell-body { display:flex; flex-direction:column; gap:3px }
.cell-student { display:flex; align-items:center; justify-content:center; gap:4px; font-size:12px }
.cell-name { color:#333; font-weight:500; white-space:nowrap; overflow:hidden; text-overflow:ellipsis; max-width:60px }
.cell-status-dot { width:6px; height:6px; border-radius:50%; flex-shrink:0 }
.dot-done { background:#27ae60 }
.dot-pending { background:#f39c12 }
.dot-missed { background:#e74c3c }
.dot-unevaluated { background:#1a73e8 }
.cell-more { font-size:11px; color:#1a73e8; cursor:pointer }
.rest-text { font-size:12px; color:#ccc; padding:8px 0 }

/* ===== 详情区 ===== */
.detail-section { flex:1; background:rgba(255,255,255,.65); backdrop-filter:blur(10px); border:1px solid rgba(0,0,0,.06); border-radius:14px; padding:16px 20px; margin-bottom:12px; overflow-y:auto }
.section-header { margin-bottom:12px }
.section-header h3 { margin:0; font-size:15px; font-weight:600; color:#1a1a2e }

.detail-group { margin-bottom:14px }
.group-header { display:flex; align-items:center; gap:10px; margin-bottom:8px; padding-bottom:8px; border-bottom:1px solid rgba(0,0,0,.05) }
.group-lab { font-size:14px; font-weight:600; color:#1a73e8 }
.group-time { font-size:12px; color:#888 }

.student-list { display:flex; flex-direction:column; gap:6px }
.student-row { display:flex; align-items:center; justify-content:space-between; padding:10px 14px; background:rgba(0,0,0,.02); border-radius:10px; gap:12px }
.student-info { display:flex; align-items:center; gap:8px; flex:1; min-width:0 }
.student-name { font-size:14px; font-weight:500; color:#222 }
.student-class { font-size:11px; color:#aaa }
.student-status-area { display:flex; align-items:center; gap:8px }
.status-tag { font-size:11px; font-weight:600; padding:3px 10px; border-radius:10px; white-space:nowrap }
.tag-done { background:rgba(39,174,96,.12); color:#1e8449 }
.tag-pending { background:rgba(243,156,18,.12); color:#b8730a }
.tag-missed { background:rgba(231,76,60,.12); color:#b03a2e }
.tag-unevaluated { background:rgba(26,115,232,.12); color:#1557b0 }
.checkin-time { font-size:11px; color:#aaa }

.eval-btn { padding:6px 16px; border:1px solid rgba(26,115,232,.3); border-radius:8px; background:rgba(26,115,232,.04); color:#1a73e8; font-size:12px; cursor:pointer; transition:.15s; white-space:nowrap }
.eval-btn:hover { background:rgba(26,115,232,.1) }

.empty-state { padding:30px; text-align:center; color:#bbb; font-size:13px }

/* ===== 本周统计 ===== */
.week-summary { display:flex; align-items:center; justify-content:center; gap:12px; padding:12px 20px; background:rgba(255,255,255,.5); border-radius:12px; font-size:13px; color:#888; flex-shrink:0 }
.summary-divider { color:#ddd }
.summary-item strong { font-weight:600; margin:0 2px }
.green { color:#27ae60 }
.red { color:#e74c3c }
.blue { color:#1a73e8 }
</style>
