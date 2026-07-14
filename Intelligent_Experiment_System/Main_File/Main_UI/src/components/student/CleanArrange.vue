<template>
  <div class="clean-page">
    <div class="page-header">
      <h2>值日安排</h2>
      <p>查看值日排班表</p>
    </div>

    <!-- 周课表 -->
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
          :class="{ today: day.isToday, rest: !day.duty }"
        >
          <div class="cell-header">
            <span class="cell-day">{{ day.label }}</span>
            <span class="cell-date">{{ day.date }}</span>
          </div>
          <div class="cell-body" v-if="day.duty">
            <span class="cell-lab">{{ day.duty.labShort }}</span>
            <span class="cell-time">{{ day.duty.timeSlot }}</span>
            <span class="cell-dot" :class="'dot-' + day.duty.status"></span>
          </div>
          <div class="cell-body rest-text" v-else>
            <span>休息</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 今日详情 + 值日列表 -->
    <div class="section-title-row">
      <h3>值日详情</h3>
    </div>

    <div class="duty-list" v-if="dutyItems.length > 0">
      <div
        v-for="duty in dutyItems"
        :key="duty.id"
        class="duty-card"
        :class="'card-' + duty.status"
      >
        <div class="card-left">
          <div class="card-date-box" :class="{ today: duty.isToday }">
            <span class="date-month">{{ duty.month }}月</span>
            <span class="date-day">{{ duty.day }}</span>
            <span class="date-weekday">{{ duty.weekday }}</span>
          </div>
        </div>
        <div class="card-body">
          <div class="card-top">
            <div>
              <span class="card-status-text" :class="'text-' + duty.status">{{ statusLabel(duty.status) }}</span>
              <span class="card-lab-name">{{ duty.labName }}</span>
            </div>
            <span class="card-time">{{ duty.timeSlot }}</span>
          </div>
          <div class="card-tasks" v-if="duty.tasks">
            <span class="tasks-label">值日任务：</span>{{ duty.tasks }}
          </div>
          <div class="card-group" v-if="duty.groupMates">
            <span class="group-label">同组：</span>{{ duty.groupMates }}
          </div>
          <div class="card-score" v-if="duty.score !== undefined">
            <span class="score-label">检查评分：</span>
            <span class="score-value" :class="duty.score >= 80 ? 'green' : duty.score >= 60 ? 'orange' : 'red'">{{ duty.score }} 分</span>
            <span v-if="duty.comment" class="score-comment">— {{ duty.comment }}</span>
          </div>
        </div>
        <div class="card-right" v-if="duty.status === 'pending'">
          <button class="checkin-btn" @click="checkIn(duty.id)">签到打卡</button>
        </div>
      </div>
    </div>

    <div v-else class="empty-state">
      <svg viewBox="0 0 24 24" width="48" height="48" fill="none" stroke="#ccc" stroke-width="1.2"><rect x="3" y="4" width="18" height="18" rx="2"/><line x1="16" y1="2" x2="16" y2="6"/><line x1="8" y1="2" x2="8" y2="6"/><line x1="3" y1="10" x2="21" y2="10"/></svg>
      <p>暂无值日安排</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'

interface DutyDay {
  key: number; label: string; date: string; isToday: boolean
  duty?: { labShort: string; timeSlot: string; status: string }
}

interface DutyItem {
  id: number; labName: string; status: 'pending' | 'done' | 'missed'
  month: number; day: number; weekday: string; timeSlot: string; isToday: boolean
  tasks: string; groupMates: string
  score?: number; comment?: string
}

const weekOffset = ref(0)

// 以 7月8日(周一)作为基准周
const baseMonday = new Date(2026, 6, 6) // July 6, 2026 is a Monday
const weekDayLabels = ['周一', '周二', '周三', '周四', '周五', '周六', '周日']

const currentWeek = computed(() => {
  const now = new Date()
  const startOfYear = new Date(now.getFullYear(), 0, 1)
  const weekNum = Math.ceil(((now.getTime() + weekOffset.value * 7 * 86400000 - startOfYear.getTime()) / 86400000 + startOfYear.getDay() + 1) / 7)
  return weekNum
})

const weekDays = computed((): DutyDay[] => {
  const monday = new Date(baseMonday)
  monday.setDate(monday.getDate() + weekOffset.value * 7)
  const today = new Date()
  const schedule: Record<number, { labShort: string; timeSlot: string; status: string }> = {
    1: { labShort: 'B103 物理', timeSlot: '17:00-18:00', status: 'pending' },
    2: { labShort: 'A201 化学', timeSlot: '17:00-18:00', status: 'pending' },
    4: { labShort: 'C202 计算机', timeSlot: '17:00-18:00', status: 'done' },
    5: { labShort: 'D305 生物', timeSlot: '17:00-18:00', status: 'done' },
  }
  return Array.from({ length: 7 }, (_, i) => {
    const d = new Date(monday)
    d.setDate(monday.getDate() + i)
    const key = i + 1
    const dateStr = `${d.getMonth() + 1}/${d.getDate()}`
    const isToday = d.toDateString() === today.toDateString()
    return {
      key, label: weekDayLabels[i], date: dateStr, isToday,
      duty: schedule[key],
    }
  })
})

const weekRange = computed(() => {
  const monday = new Date(baseMonday)
  monday.setDate(monday.getDate() + weekOffset.value * 7)
  const sunday = new Date(monday)
  sunday.setDate(monday.getDate() + 6)
  return `${monday.getMonth() + 1}/${monday.getDate()} - ${sunday.getMonth() + 1}/${sunday.getDate()}`
})

const dutyItems: DutyItem[] = [
  { id: 1, labName: 'B103 物理实验室', status: 'pending', month: 7, day: 8, weekday: '周一', timeSlot: '17:00 ~ 18:00', isToday: true,
    tasks: '清扫地面、整理实验台、擦拭设备表面、倾倒垃圾', groupMates: '李明、王芳' },
  { id: 2, labName: 'A201 化学实验室', status: 'pending', month: 7, day: 9, weekday: '周二', timeSlot: '17:00 ~ 18:00', isToday: false,
    tasks: '清理通风橱、整理试剂架、清洗水槽、拖地', groupMates: '赵磊、陈静' },
  { id: 3, labName: 'C202 计算机实验室', status: 'done', month: 7, day: 3, weekday: '周四', timeSlot: '17:00 ~ 18:00', isToday: false,
    tasks: '清扫地面、擦拭显示器与键盘、整理桌椅', groupMates: '李明、王芳',
    score: 95, comment: '打扫非常干净，设备摆放整齐' },
  { id: 4, labName: 'D305 生物实验室', status: 'done', month: 7, day: 4, weekday: '周五', timeSlot: '17:00 ~ 18:00', isToday: false,
    tasks: '清理实验废弃物、消毒操作台、清洗培养皿', groupMates: '赵磊',
    score: 82, comment: '基本完成，培养皿清洗需加强' },
  { id: 5, labName: 'B103 物理实验室', status: 'missed', month: 7, day: 1, weekday: '周二', timeSlot: '17:00 ~ 18:00', isToday: false,
    tasks: '清扫地面、整理实验台', groupMates: '李明',
    score: 0, comment: '未按时签到，请下次注意' },
]

function prevWeek(): void { weekOffset.value-- }
function nextWeek(): void { weekOffset.value++ }

function statusLabel(s: string): string {
  const m: Record<string, string> = { pending: '待签到', done: '已完成', missed: '已缺勤' }
  return m[s] || s
}

function checkIn(id: number): void {
  const d = dutyItems.find(x => x.id === id)
  if (d && confirm(`确认完成「${d.labName}」的值日签到吗？`)) {
    d.status = 'done'
    d.score = 90
    d.comment = '已完成签到，等待检查评分'
  }
}
</script>

<style scoped>
.clean-page { height: 100%; overflow-y: auto; padding: 28px 36px; }

.page-header { margin-bottom: 18px; }
.page-header h2 { font-size: 22px; font-weight: 700; color: #1a1a2e; margin: 0 0 4px; }
.page-header p { font-size: 14px; color: #888; margin: 0; }

/* ===== 周课表 ===== */
.week-card { background: rgba(255,255,255,.5); border-radius: 14px; border: 1px solid rgba(0,0,0,.04); padding: 18px 20px 14px; margin-bottom: 24px; }

.week-nav { display: flex; align-items: center; justify-content: center; gap: 16px; margin-bottom: 14px; }
.week-arrow { width: 30px; height: 30px; border: 1px solid rgba(0,0,0,.08); border-radius: 8px; background: #fff; cursor: pointer; font-size: 14px; color: #888; display: flex; align-items: center; justify-content: center; transition: all .15s; }
.week-arrow:hover { border-color: #1a73e8; color: #1a73e8; }
.week-label { font-size: 14px; font-weight: 600; color: #1a1a2e; }

.week-grid { display: grid; grid-template-columns: repeat(7, 1fr); gap: 6px; }

.week-cell { border-radius: 10px; padding: 10px 6px; text-align: center; transition: all .15s; }
.week-cell.rest { opacity: .4; }
.week-cell.today { background: rgba(26,115,232,.06); }

.cell-header { margin-bottom: 6px; }
.cell-day { display: block; font-size: 12px; color: #666; }
.cell-date { display: block; font-size: 11px; color: #888; margin-top: 1px; }
.week-cell.today .cell-day { color: #1a73e8; font-weight: 600; }

.cell-body { display: flex; flex-direction: column; align-items: center; gap: 3px; }
.cell-lab { font-size: 12px; font-weight: 600; color: #333; }
.cell-time { font-size: 10px; color: #aaa; }
.cell-dot { width: 6px; height: 6px; border-radius: 50%; margin-top: 2px; }
.dot-pending { background: #1a73e8; }
.dot-done { background: #27ae60; }
.dot-missed { background: #e74c3c; }
.rest-text { font-size: 12px; color: #ccc; padding: 6px 0; }

/* ===== 详情列表 ===== */
.section-title-row { margin-bottom: 14px; }
.section-title-row h3 { font-size: 15px; font-weight: 600; color: #1a1a2e; margin: 0; }

.duty-list { display: flex; flex-direction: column; gap: 12px; }

.duty-card { display: flex; align-items: stretch; gap: 14px; background: rgba(255,255,255,.5); border-radius: 14px; border: 1px solid rgba(0,0,0,.04); padding: 16px 18px; }
.duty-card.card-pending { border-left: 3px solid #1a73e8; }
.duty-card.card-done { border-left: 3px solid #27ae60; opacity: .75; }
.duty-card.card-missed { border-left: 3px solid #e74c3c; opacity: .7; }

/* 日期方块 */
.card-left { flex-shrink: 0; }
.card-date-box { display: flex; flex-direction: column; align-items: center; padding: 6px 12px; border-radius: 10px; background: rgba(0,0,0,.02); min-width: 56px; }
.card-date-box.today { background: rgba(26,115,232,.08); }
.date-month { font-size: 10px; color: #aaa; }
.date-day { font-size: 20px; font-weight: 700; color: #1a1a2e; line-height: 1.2; }
.date-weekday { font-size: 11px; color: #999; }
.card-date-box.today .date-day { color: #1a73e8; }

.card-body { flex: 1; min-width: 0; }
.card-top { display: flex; align-items: center; justify-content: space-between; gap: 10px; margin-bottom: 8px; }
.card-status-text { font-size: 13px; font-weight: 600; margin-right: 6px; }
.text-pending { color: #1a73e8; }
.text-done { color: #27ae60; }
.text-missed { color: #e74c3c; }
.card-lab-name { font-size: 14px; font-weight: 600; color: #1a1a2e; }
.card-time { font-size: 13px; color: #888; white-space: nowrap; }

.card-tasks { font-size: 12px; color: #888; line-height: 1.5; margin-bottom: 4px; }
.tasks-label { color: #aaa; }
.card-group { font-size: 12px; color: #888; }
.group-label { color: #aaa; }

.card-score { margin-top: 8px; padding: 6px 10px; background: rgba(0,0,0,.015); border-radius: 6px; font-size: 12px; }
.score-label { color: #aaa; }
.score-value { font-weight: 700; }
.score-value.green { color: #27ae60; }
.score-value.orange { color: #e67e22; }
.score-value.red { color: #e74c3c; }
.score-comment { color: #aaa; }

/* 签到按钮 */
.card-right { flex-shrink: 0; display: flex; align-items: center; }
.checkin-btn { padding: 10px 22px; border: none; border-radius: 10px; background: linear-gradient(135deg, #1a73e8, #4a90d9); color: #fff; font-size: 14px; font-weight: 600; cursor: pointer; transition: all .15s; white-space: nowrap; }
.checkin-btn:hover { box-shadow: 0 4px 14px rgba(26,115,232,.35); }

.empty-state { display: flex; flex-direction: column; align-items: center; padding: 80px 0; color: #ccc; }
.empty-state p { font-size: 14px; margin-top: 12px; }
</style>
