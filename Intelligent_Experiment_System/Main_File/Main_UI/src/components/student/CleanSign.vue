<template>
  <div class="sign-page">
    <div class="page-header">
      <h2>我的签到</h2>
      <p>值日签到打卡</p>
    </div>

    <div class="sign-layout">
      <!-- 左侧：月历 -->
      <div class="sign-left">
        <div class="calendar-card">
          <div class="calendar-nav">
            <button class="cal-arrow" @click="prevMonth">&lt;</button>
            <span class="cal-month-label">{{ year }}年 {{ month }}月</span>
            <button class="cal-arrow" @click="nextMonth">&gt;</button>
          </div>
          <div class="calendar-grid">
            <div class="cal-weekday" v-for="w in weekHeaders" :key="w">{{ w }}</div>
            <div
              v-for="(cell, idx) in calendarCells"
              :key="idx"
              class="cal-cell"
              :class="{
                'cal-other': !cell.isCurrentMonth,
                'cal-today': cell.isToday,
                'cal-done': cell.status === 'done',
                'cal-missed': cell.status === 'missed',
                'cal-pending': cell.status === 'pending',
              }"
            >
              <span class="cal-day-num">{{ cell.day }}</span>
              <span class="cal-dot" v-if="cell.status && cell.isCurrentMonth" :class="'dot-' + cell.status"></span>
            </div>
          </div>
          <div class="calendar-legend">
            <span class="legend-item"><span class="leg-dot dot-done"></span>已签到</span>
            <span class="legend-item"><span class="leg-dot dot-pending"></span>待签到</span>
            <span class="legend-item"><span class="leg-dot dot-missed"></span>缺勤</span>
          </div>
        </div>

        <!-- 统计卡片 -->
        <div class="stats-row">
          <div class="stat-item">
            <span class="stat-num blue">{{ stats.weekDone }}</span>
            <span class="stat-label">本周已签</span>
          </div>
          <div class="stat-item">
            <span class="stat-num green">{{ stats.streak }}</span>
            <span class="stat-label">连续签到</span>
          </div>
          <div class="stat-item">
            <span class="stat-num orange">{{ stats.rate }}%</span>
            <span class="stat-label">签到率</span>
          </div>
        </div>
      </div>

      <!-- 右侧：今日签到 + 时间线 -->
      <div class="sign-right">
        <!-- 今日待签到 -->
        <div class="today-card" v-if="todayTask">
          <div class="today-header">
            <span class="today-badge">今日签到</span>
            <span class="today-date">{{ todayTask.month }}月{{ todayTask.day }}日 {{ todayTask.weekday }}</span>
          </div>
          <div class="today-body">
            <div class="today-info">
              <div class="today-lab">{{ todayTask.labName }}</div>
              <div class="today-row">
                <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="#888" stroke-width="2"><circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/></svg>
                <span>{{ todayTask.timeSlot }}</span>
              </div>
              <div class="today-row" v-if="todayTask.groupMates">
                <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="#888" stroke-width="2"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M23 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/></svg>
                <span>{{ todayTask.groupMates }}</span>
              </div>
            </div>
            <div class="today-actions">
              <div v-if="todayTask.status === 'pending'" class="today-status-text pending">待签到</div>
              <div v-else-if="todayTask.status === 'done'" class="today-status-text done">✓ 已签到</div>
              <button
                v-if="todayTask.status === 'pending'"
                class="sign-btn"
                @click="doSignIn"
              >
                <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2.5"><path d="M12 2l2.4 7.2h7.6l-6 4.8 2.4 7.2-6-4.8-6 4.8 2.4-7.2-6-4.8h7.6z"/></svg>
                签到打卡
              </button>
            </div>
          </div>
          <div class="today-tasks" v-if="todayTask.tasks">
            <span class="tasks-label">值日任务：</span>{{ todayTask.tasks }}
          </div>
        </div>

        <!-- 签到历史时间线 -->
        <div class="history-section">
          <h3 class="section-title">签到记录</h3>
          <div class="timeline" v-if="historyItems.length > 0">
            <div
              v-for="(item, idx) in historyItems"
              :key="idx"
              class="tl-item"
              :class="'tl-' + item.status"
            >
              <div class="tl-dot-wrap">
                <div class="tl-dot" :class="'dot-' + item.status"></div>
                <div class="tl-line" v-if="idx < historyItems.length - 1"></div>
              </div>
              <div class="tl-content">
                <div class="tl-top">
                  <span class="tl-date">{{ item.month }}月{{ item.day }}日 {{ item.weekday }}</span>
                  <span class="tl-status-tag" :class="'tag-' + item.status">{{ statusText(item.status) }}</span>
                </div>
                <div class="tl-lab">{{ item.labName }}</div>
                <div class="tl-time">{{ item.timeSlot }}</div>
                <div class="tl-score" v-if="item.score !== undefined">
                  检查评分：<span class="score-num" :class="scoreClass(item.score)">{{ item.score }} 分</span>
                  <span v-if="item.comment" class="score-comment">— {{ item.comment }}</span>
                </div>
              </div>
            </div>
          </div>
          <div v-else class="empty-timeline">暂无签到记录</div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, reactive } from 'vue'

const weekHeaders = ['日', '一', '二', '三', '四', '五', '六']

// --- 月历逻辑 ---
const now = new Date()
const viewYear = ref(now.getFullYear())
const viewMonth = ref(now.getMonth() + 1) // 1-based

const year = computed(() => viewYear.value)
const month = computed(() => viewMonth.value)

interface CalCell {
  day: number; isCurrentMonth: boolean; isToday: boolean
  status: 'done' | 'missed' | 'pending' | null
}

// 模拟签到数据：日期 -> 状态
const signData: Record<string, 'done' | 'missed' | 'pending'> = {
  '2026-07-03': 'done',
  '2026-07-04': 'done',
  '2026-07-08': 'pending',
  '2026-07-01': 'missed',
  '2026-06-27': 'done',
  '2026-06-26': 'done',
  '2026-06-25': 'done',
  '2026-06-24': 'missed',
  '2026-06-20': 'done',
  '2026-06-19': 'done',
  '2026-06-18': 'done',
  '2026-06-17': 'done',
}

function dateKey(y: number, m: number, d: number): string {
  const mm = String(m).padStart(2, '0')
  const dd = String(d).padStart(2, '0')
  return `${y}-${mm}-${dd}`
}

const calendarCells = computed((): CalCell[] => {
  const y = viewYear.value
  const m = viewMonth.value
  const firstDay = new Date(y, m - 1, 1)
  const startDayOfWeek = firstDay.getDay() // 0=Sun
  const daysInMonth = new Date(y, m, 0).getDate()
  const daysInPrevMonth = new Date(y, m - 1, 0).getDate()

  const today = new Date()
  const todayKey = dateKey(today.getFullYear(), today.getMonth() + 1, today.getDate())

  const cells: CalCell[] = []

  // 上月补齐
  for (let i = startDayOfWeek - 1; i >= 0; i--) {
    const d = daysInPrevMonth - i
    const key = dateKey(m === 1 ? y - 1 : y, m === 1 ? 12 : m - 1, d)
    cells.push({ day: d, isCurrentMonth: false, isToday: false, status: signData[key] || null })
  }

  // 当月
  for (let d = 1; d <= daysInMonth; d++) {
    const key = dateKey(y, m, d)
    cells.push({
      day: d, isCurrentMonth: true,
      isToday: key === todayKey,
      status: signData[key] || null,
    })
  }

  // 下月补齐
  const remaining = 7 - (cells.length % 7)
  if (remaining < 7) {
    for (let d = 1; d <= remaining; d++) {
      const key = dateKey(m === 12 ? y + 1 : y, m === 12 ? 1 : m + 1, d)
      cells.push({ day: d, isCurrentMonth: false, isToday: false, status: signData[key] || null })
    }
  }

  return cells
})

function prevMonth(): void {
  if (viewMonth.value === 1) { viewYear.value--; viewMonth.value = 12 }
  else { viewMonth.value-- }
}
function nextMonth(): void {
  if (viewMonth.value === 12) { viewYear.value++; viewMonth.value = 1 }
  else { viewMonth.value++ }
}

// --- 统计 ---
const stats = reactive({
  weekDone: 3,
  streak: 5,
  rate: 85,
})

// --- 今日任务 ---
interface TodayTask {
  labName: string; month: number; day: number; weekday: string
  timeSlot: string; groupMates: string; tasks: string
  status: 'pending' | 'done' | 'missed'
}

const todayTask = ref<TodayTask>({
  labName: 'B103 物理实验室', month: 7, day: 8, weekday: '周一',
  timeSlot: '17:00 ~ 18:00', groupMates: '李明、王芳',
  tasks: '清扫地面、整理实验台、擦拭设备表面、倾倒垃圾',
  status: 'pending',
})

function doSignIn(): void {
  if (todayTask.value && confirm(`确认完成「${todayTask.value.labName}」的值日签到吗？`)) {
    todayTask.value.status = 'done'
    const key = dateKey(now.getFullYear(), now.getMonth() + 1, now.getDate())
    signData[key] = 'done'
    stats.weekDone++
    stats.streak++
  }
}

// --- 历史时间线 ---
interface HistoryItem {
  labName: string; month: number; day: number; weekday: string
  timeSlot: string; status: 'done' | 'missed'
  score?: number; comment?: string
}

const historyItems: HistoryItem[] = [
  { labName: 'D305 生物实验室', month: 7, day: 4, weekday: '周五', timeSlot: '17:00 ~ 18:00', status: 'done', score: 82, comment: '基本完成，培养皿清洗需加强' },
  { labName: 'C202 计算机实验室', month: 7, day: 3, weekday: '周四', timeSlot: '17:00 ~ 18:00', status: 'done', score: 95, comment: '打扫非常干净，设备摆放整齐' },
  { labName: 'B103 物理实验室', month: 7, day: 1, weekday: '周二', timeSlot: '17:00 ~ 18:00', status: 'missed', score: 0, comment: '未按时签到，请下次注意' },
  { labName: 'D305 生物实验室', month: 6, day: 27, weekday: '周五', timeSlot: '17:00 ~ 18:00', status: 'done', score: 88, comment: '整体良好' },
  { labName: 'C202 计算机实验室', month: 6, day: 26, weekday: '周四', timeSlot: '17:00 ~ 18:00', status: 'done', score: 91, comment: '干净整洁' },
]

function statusText(s: string): string {
  return s === 'done' ? '已签到' : s === 'missed' ? '缺勤' : s
}
function scoreClass(s: number): string {
  return s >= 90 ? 'green' : s >= 60 ? 'orange' : 'red'
}
</script>

<style scoped>
.sign-page { height: 100%; overflow-y: auto; padding: 28px 36px; }

.page-header { margin-bottom: 18px; }
.page-header h2 { font-size: 22px; font-weight: 700; color: #1a1a2e; margin: 0 0 4px; }
.page-header p { font-size: 14px; color: #888; margin: 0; }

/* ===== 左右布局 ===== */
.sign-layout { display: flex; gap: 24px; align-items: flex-start; }

/* ===== 左侧 ===== */
.sign-left { width: 340px; flex-shrink: 0; display: flex; flex-direction: column; gap: 16px; }

/* 月历卡片 */
.calendar-card { background: rgba(255,255,255,.5); border-radius: 14px; border: 1px solid rgba(0,0,0,.04); padding: 16px 18px; }

.calendar-nav { display: flex; align-items: center; justify-content: space-between; margin-bottom: 12px; }
.cal-arrow { width: 28px; height: 28px; border: 1px solid rgba(0,0,0,.08); border-radius: 8px; background: #fff; cursor: pointer; font-size: 13px; color: #888; display: flex; align-items: center; justify-content: center; transition: all .15s; }
.cal-arrow:hover { border-color: #1a73e8; color: #1a73e8; }
.cal-month-label { font-size: 14px; font-weight: 600; color: #1a1a2e; }

.calendar-grid { display: grid; grid-template-columns: repeat(7, 1fr); gap: 2px; text-align: center; }

.cal-weekday { font-size: 11px; color: #999; padding: 6px 0; font-weight: 500; }

.cal-cell { display: flex; flex-direction: column; align-items: center; padding: 6px 0; border-radius: 8px; position: relative; transition: background .15s; cursor: default; }
.cal-cell.cal-other { opacity: .3; }
.cal-cell.cal-today { background: rgba(26,115,232,.08); }
.cal-cell.cal-today .cal-day-num { color: #1a73e8; font-weight: 700; }

.cal-day-num { font-size: 13px; color: #444; line-height: 1.4; }
.cal-dot { width: 5px; height: 5px; border-radius: 50%; margin-top: 2px; }
.dot-done { background: #27ae60; }
.dot-pending { background: #1a73e8; }
.dot-missed { background: #e74c3c; }

.calendar-legend { display: flex; gap: 14px; margin-top: 12px; padding-top: 10px; border-top: 1px solid rgba(0,0,0,.05); }
.legend-item { display: flex; align-items: center; gap: 4px; font-size: 11px; color: #999; }
.leg-dot { width: 6px; height: 6px; border-radius: 50%; }

/* 统计卡片 */
.stats-row { display: flex; gap: 10px; }
.stat-item { flex: 1; background: rgba(255,255,255,.5); border-radius: 10px; padding: 14px 10px; text-align: center; border: 1px solid rgba(0,0,0,.04); display: flex; flex-direction: column; gap: 2px; }
.stat-num { font-size: 22px; font-weight: 700; }
.stat-num.blue { color: #1a73e8; }
.stat-num.green { color: #27ae60; }
.stat-num.orange { color: #e67e22; }
.stat-label { font-size: 11px; color: #999; }

/* ===== 右侧 ===== */
.sign-right { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 20px; }

/* 今日签到卡片 */
.today-card { background: rgba(255,255,255,.5); border-radius: 14px; border: 1px solid rgba(0,0,0,.04); padding: 20px 24px; }

.today-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 16px; }
.today-badge { background: linear-gradient(135deg, #1a73e8, #4a90d9); color: #fff; font-size: 12px; font-weight: 600; padding: 4px 14px; border-radius: 20px; }
.today-date { font-size: 13px; color: #888; }

.today-body { display: flex; align-items: center; justify-content: space-between; gap: 20px; margin-bottom: 14px; }
.today-info { flex: 1; }
.today-lab { font-size: 18px; font-weight: 700; color: #1a1a2e; margin-bottom: 8px; }
.today-row { display: flex; align-items: center; gap: 6px; font-size: 13px; color: #888; margin-bottom: 3px; }

.today-actions { display: flex; flex-direction: column; align-items: flex-end; gap: 8px; flex-shrink: 0; }
.today-status-text { font-size: 13px; font-weight: 600; padding: 4px 14px; border-radius: 20px; }
.today-status-text.pending { background: rgba(26,115,232,.1); color: #1a73e8; }
.today-status-text.done { background: rgba(39,174,96,.1); color: #27ae60; }

.sign-btn { display: flex; align-items: center; gap: 8px; padding: 14px 32px; border: none; border-radius: 14px; background: linear-gradient(135deg, #1a73e8, #4a90d9); color: #fff; font-size: 16px; font-weight: 700; cursor: pointer; transition: all .2s; white-space: nowrap; box-shadow: 0 2px 8px rgba(26,115,232,.2); }
.sign-btn:hover { box-shadow: 0 6px 20px rgba(26,115,232,.4); transform: translateY(-1px); }
.sign-btn:active { transform: translateY(0); }

.today-tasks { font-size: 13px; color: #888; padding-top: 12px; border-top: 1px solid rgba(0,0,0,.04); line-height: 1.5; }
.tasks-label { color: #aaa; }

/* 签到记录 */
.history-section { }
.section-title { font-size: 15px; font-weight: 600; color: #1a1a2e; margin: 0 0 12px; }

/* 时间线 */
.timeline { display: flex; flex-direction: column; }
.tl-item { display: flex; gap: 14px; }
.tl-dot-wrap { display: flex; flex-direction: column; align-items: center; width: 20px; flex-shrink: 0; }
.tl-dot { width: 10px; height: 10px; border-radius: 50%; margin-top: 6px; flex-shrink: 0; }
.tl-dot.dot-done { background: #27ae60; }
.tl-dot.dot-missed { background: #e74c3c; }
.tl-line { width: 2px; flex: 1; background: rgba(0,0,0,.06); margin-top: 4px; min-height: 30px; }

.tl-content { flex: 1; min-width: 0; padding: 4px 0 16px; }
.tl-top { display: flex; align-items: center; gap: 10px; margin-bottom: 4px; }
.tl-date { font-size: 13px; color: #555; font-weight: 500; }
.tl-status-tag { font-size: 11px; font-weight: 600; padding: 1px 8px; border-radius: 10px; }
.tag-done { background: rgba(39,174,96,.1); color: #27ae60; }
.tag-missed { background: rgba(231,76,60,.1); color: #e74c3c; }

.tl-lab { font-size: 13px; color: #888; }
.tl-time { font-size: 12px; color: #aaa; margin-bottom: 2px; }
.tl-score { font-size: 12px; color: #888; margin-top: 4px; padding: 4px 8px; background: rgba(0,0,0,.015); border-radius: 6px; display: inline-block; }
.score-num { font-weight: 700; }
.score-num.green { color: #27ae60; }
.score-num.orange { color: #e67e22; }
.score-num.red { color: #e74c3c; }
.score-comment { color: #aaa; }

.empty-timeline { text-align: center; padding: 40px 0; color: #ccc; font-size: 13px; }
</style>
