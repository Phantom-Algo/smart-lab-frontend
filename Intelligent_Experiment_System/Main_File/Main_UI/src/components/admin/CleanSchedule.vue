<template>
  <div class="cs-page">
    <div class="page-header">
      <h2>排班管理</h2>
      <p>值日排班、提醒通知</p>
    </div>

    <div class="stats-row">
      <div class="stat-card"><div class="stat-num blue">{{ weekScheduleCount }}</div><div class="stat-label">本周排班</div></div>
      <div class="stat-card"><div class="stat-num" style="color:#1a73e8">{{ todaySchedules.length }}</div><div class="stat-label">今日值日</div></div>
      <div class="stat-card"><div class="stat-num green">{{ weekSignedCount }}</div><div class="stat-label">已签到</div></div>
      <div class="stat-card"><div class="stat-num orange">{{ weekPendingCount }}</div><div class="stat-label">待提醒</div></div>
    </div>

    <div class="filter-bar">
      <div class="filter-tabs">
        <div v-for="t in statusTabs" :key="t.key" class="filter-tab" :class="{ active: activeStatus === t.key }" @click="activeStatus = t.key; pageNum = 1">
          {{ t.label }}<span class="tab-count">{{ t.count }}</span>
        </div>
      </div>
      <select v-model="filterLab" class="tool-select"><option value="">全部实验室</option><option v-for="l in labOptions" :key="l" :value="l">{{ l }}</option></select>
      <div class="week-picker">
        <button class="week-arrow" @click="prevWeek">&lt;</button>
        <span class="week-label">第 {{ currentWeek }} 周</span>
        <button class="week-arrow" @click="nextWeek">&gt;</button>
      </div>
      <button class="btn-add" @click="openCreate">+ 添加排班</button>
    </div>

    <div class="schedule-list" v-if="pagedGroups.length > 0">
      <div v-for="group in pagedGroups" :key="group.date" class="date-group">
        <div class="date-header">
          <span class="date-label">{{ group.dayLabel }} {{ group.date }}</span>
          <button class="btn-add-small" @click="openCreateForDate(group.date)">+ 添加</button>
        </div>
        <div class="duty-cards">
          <div v-for="s in group.items" :key="s.id" class="duty-card" :class="'card-' + s.status">
            <div class="card-body">
              <div class="card-main">
                <span class="lab-name">{{ s.labName }}</span>
                <span class="student-name">👤 {{ s.studentName }}</span>
                <span class="time-slot">🕐 {{ timeSlotLabel(s.timeSlot) }}</span>
              </div>
              <div class="card-meta">
                <span class="status-tag" :class="'st-' + s.status">{{ statusLabel(s.status) }}</span>
                <span class="sign-time" v-if="s.signTime">签到 {{ s.signTime }}</span>
                <span class="note-text" v-if="s.note">{{ s.note }}</span>
              </div>
            </div>
            <div class="card-actions">
              <button class="act-btn edit" @click="openEdit(s)">编辑</button>
              <button class="act-btn remind" v-if="s.status === 'scheduled'" @click="sendRemind(s)">提醒</button>
              <button class="act-btn del" @click="confirmDelete(s)">删除</button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div v-else class="empty-state">
      <svg viewBox="0 0 24 24" width="48" height="48" fill="none" stroke="#ccc" stroke-width="1.2"><rect x="3" y="4" width="18" height="18" rx="2"/><line x1="16" y1="2" x2="16" y2="6"/><line x1="8" y1="2" x2="8" y2="6"/><line x1="3" y1="10" x2="21" y2="10"/></svg>
      <p>暂无排班记录</p>
    </div>

    <div class="pagination" v-if="sortedGroups.length > pageSize">
      <button :disabled="pageNum <= 1" @click="pageNum--">上一页</button>
      <span>{{ pageNum }} / {{ totalPages }}</span>
      <button :disabled="pageNum >= totalPages" @click="pageNum++">下一页</button>
    </div>

    <!-- 创建/编辑弹窗 -->
    <teleport to="body">
      <transition name="modal-fade">
        <div v-if="showForm" class="modal-overlay" @click.self="showForm = false">
          <div class="modal-panel">
            <div class="modal-header"><h3>{{ isEditing ? '编辑排班' : '添加排班' }}</h3><button class="modal-close" @click="showForm = false"><svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2"><path d="M18 6L6 18M6 6l12 12"/></svg></button></div>
            <div class="modal-body">
              <div class="form-g"><label>实验室 <span class="required">*</span></label><select v-model="form.labName" class="form-input"><option value="" disabled>请选择</option><option v-for="l in labOptions" :key="l" :value="l">{{ l }}</option></select></div>
              <div class="form-g"><label>值日学生 <span class="required">*</span></label><select v-model="form.studentName" class="form-input"><option value="" disabled>请选择</option><option v-for="s in studentOptions" :key="s" :value="s">{{ s }}</option></select></div>
              <div class="form-g"><label>日期 <span class="required">*</span></label><input v-model="form.dutyDate" type="date" class="form-input" /></div>
              <div class="form-g"><label>时段 <span class="required">*</span></label>
                <div class="radio-row">
                  <label class="radio-item" :class="{ active: form.timeSlot === 'morning' }"><input type="radio" v-model="form.timeSlot" value="morning" /><span>上午 (8:00-12:00)</span></label>
                  <label class="radio-item" :class="{ active: form.timeSlot === 'afternoon' }"><input type="radio" v-model="form.timeSlot" value="afternoon" /><span>下午 (14:00-18:00)</span></label>
                </div>
              </div>
              <div class="form-g"><label>备注</label><input v-model="form.note" class="form-input" placeholder="可选" /></div>
              <label class="notify-check" v-if="!isEditing"><input type="checkbox" v-model="form.sendNotify" /><span>发送排班通知给学生</span></label>
            </div>
            <div class="modal-footer">
              <button class="modal-btn cancel" @click="showForm = false">取消</button>
              <button class="modal-btn primary" @click="doSave" :disabled="!form.labName || !form.studentName || !form.dutyDate || !form.timeSlot">{{ isEditing ? '保存修改' : '确认添加' }}</button>
            </div>
          </div>
        </div>
      </transition>
    </teleport>

    <!-- 删除确认弹窗 -->
    <teleport to="body">
      <transition name="modal-fade">
        <div v-if="showDelete" class="modal-overlay" @click.self="showDelete = false">
          <div class="modal-panel modal-small">
            <div class="modal-header"><h3>确认删除</h3><button class="modal-close" @click="showDelete = false"><svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2"><path d="M18 6L6 18M6 6l12 12"/></svg></button></div>
            <div class="modal-body">
              <p class="delete-warn">确定要删除 <strong>{{ deleteTarget?.labName }}</strong> 在 <strong>{{ deleteTarget?.dutyDate }}</strong> 的排班吗？</p>
            </div>
            <div class="modal-footer">
              <button class="modal-btn cancel" @click="showDelete = false">取消</button>
              <button class="modal-btn danger" @click="doDelete">确认删除</button>
            </div>
          </div>
        </div>
      </transition>
    </teleport>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, reactive } from 'vue'

type DutyStatus = 'scheduled' | 'signed' | 'done'
type TimeSlot = 'morning' | 'afternoon'

interface Schedule {
  id: number; labName: string; studentName: string; dutyDate: string
  timeSlot: TimeSlot; status: DutyStatus; signTime: string | null; note: string | null
}

const labOptions = ['A305 分析化学实验室', 'B103 物理实验室', 'C202 计算机实验室', 'D108 生物培养实验室']
const studentOptions = ['张同学', '赵同学', '李同学', '王同学', '刘同学', '陈同学']

function todayStr() { return '2026-07-09' }
function weekStartStr() { return '2026-07-07' }

const schedules = ref<Schedule[]>([
  { id:1, labName:'A305 分析化学实验室', studentName:'张同学', dutyDate:'2026-07-07', timeSlot:'morning', status:'signed', signTime:'08:05', note:'重点检查通风橱' },
  { id:2, labName:'B103 物理实验室', studentName:'赵同学', dutyDate:'2026-07-07', timeSlot:'afternoon', status:'signed', signTime:'14:02', note:null },
  { id:3, labName:'C202 计算机实验室', studentName:'李同学', dutyDate:'2026-07-08', timeSlot:'morning', status:'scheduled', signTime:null, note:null },
  { id:4, labName:'D108 生物培养实验室', studentName:'王同学', dutyDate:'2026-07-08', timeSlot:'afternoon', status:'signed', signTime:'14:10', note:'灭菌设备检查' },
  { id:5, labName:'A305 分析化学实验室', studentName:'赵同学', dutyDate:'2026-07-09', timeSlot:'morning', status:'signed', signTime:'08:00', note:null },
  { id:6, labName:'B103 物理实验室', studentName:'刘同学', dutyDate:'2026-07-09', timeSlot:'afternoon', status:'scheduled', signTime:null, note:null },
  { id:7, labName:'C202 计算机实验室', studentName:'陈同学', dutyDate:'2026-07-10', timeSlot:'morning', status:'scheduled', signTime:null, note:'机房清洁' },
  { id:8, labName:'D108 生物培养实验室', studentName:'张同学', dutyDate:'2026-07-10', timeSlot:'afternoon', status:'scheduled', signTime:null, note:null },
  { id:9, labName:'A305 分析化学实验室', studentName:'李同学', dutyDate:'2026-07-11', timeSlot:'morning', status:'done', signTime:'08:03', note:null },
  { id:10, labName:'B103 物理实验室', studentName:'王同学', dutyDate:'2026-07-11', timeSlot:'afternoon', status:'done', signTime:'14:00', note:'已完成检查评价' },
])

const currentWeek = ref(27)
const activeStatus = ref<DutyStatus | 'all'>('all')
const filterLab = ref('')
const pageNum = ref(1)
const pageSize = 5

function prevWeek() { if (currentWeek.value > 1) { currentWeek.value--; pageNum.value = 1 } }
function nextWeek() { currentWeek.value++; pageNum.value = 1 }

// 根据周次计算该周的日期范围（以周27 = 7月7日周一为基准）
const weekDates = computed(() => {
  const baseMonday = new Date('2026-07-07') // 第27周周一
  const diff = currentWeek.value - 27
  const monday = new Date(baseMonday)
  monday.setDate(monday.getDate() + diff * 7)
  const days: { date: string; label: string }[] = []
  const dayLabels = ['周一','周二','周三','周四','周五','周六','周日']
  for (let i = 0; i < 7; i++) {
    const d = new Date(monday)
    d.setDate(d.getDate() + i)
    const dateStr = d.toISOString().slice(0, 10)
    days.push({ date: dateStr, label: dayLabels[i] })
  }
  return days
})

const weekDateSet = computed(() => new Set(weekDates.value.map(d => d.date)))

const todaySchedules = computed(() => schedules.value.filter(s => s.dutyDate === todayStr()))
const weekScheduleCount = computed(() => filteredList.value.length)
const weekSignedCount = computed(() => filteredList.value.filter(s => s.status === 'signed' || s.status === 'done').length)
const weekPendingCount = computed(() => filteredList.value.filter(s => s.status === 'scheduled').length)

const statusTabs = computed(() => [
  { key:'all' as const, label:'全部', count:schedules.value.length },
  { key:'scheduled' as const, label:'已排班', count:schedules.value.filter(s => s.status === 'scheduled').length },
  { key:'signed' as const, label:'已签到', count:schedules.value.filter(s => s.status === 'signed').length },
  { key:'done' as const, label:'已完成', count:schedules.value.filter(s => s.status === 'done').length },
])

const filteredList = computed(() => {
  let list = schedules.value
  if (activeStatus.value !== 'all') list = list.filter(s => s.status === activeStatus.value)
  if (filterLab.value) list = list.filter(s => s.labName === filterLab.value)
  // 按当前周过滤
  list = list.filter(s => weekDateSet.value.has(s.dutyDate))
  return list
})

// 按日期分组，按日期排序
const sortedGroups = computed(() => {
  const map = new Map<string, Schedule[]>()
  for (const s of filteredList.value) {
    const arr = map.get(s.dutyDate) || []
    arr.push(s)
    map.set(s.dutyDate, arr)
  }
  // 按当前周的日期顺序排列
  const orderMap = new Map(weekDates.value.map((d, i) => [d.date, i]))
  const entries = [...map.entries()].sort((a, b) => (orderMap.get(a[0]) ?? 99) - (orderMap.get(b[0]) ?? 99))
  return entries.map(([date, items]) => ({ date, items, dayLabel: weekDates.value.find(d => d.date === date)?.label || '' }))
})
const totalPages = computed(() => Math.ceil(sortedGroups.value.length / pageSize) || 1)
const pagedGroups = computed(() => sortedGroups.value.slice((pageNum.value - 1) * pageSize, pageNum.value * pageSize))

function timeSlotLabel(t: TimeSlot) { return t === 'morning' ? '上午 8:00-12:00' : '下午 14:00-18:00' }
function statusLabel(s: DutyStatus) { const m: Record<string,string> = { scheduled:'已排班', signed:'已签到', done:'已完成' }; return m[s] || s }

// 创建/编辑弹窗
const showForm = ref(false)
const isEditing = ref(false)
const editId = ref(0)
const form = reactive({ labName:'', studentName:'', dutyDate:'', timeSlot:'morning' as TimeSlot, note:'', sendNotify:true })

function openCreate() { openCreateForDate('') }
function openCreateForDate(date: string) { isEditing.value = false; editId.value = 0; form.labName = ''; form.studentName = ''; form.dutyDate = date; form.timeSlot = 'morning'; form.note = ''; form.sendNotify = true; showForm.value = true }
function openEdit(s: Schedule) { isEditing.value = true; editId.value = s.id; form.labName = s.labName; form.studentName = s.studentName; form.dutyDate = s.dutyDate; form.timeSlot = s.timeSlot; form.note = s.note || ''; showForm.value = true }
function doSave() {
  if (!form.labName || !form.studentName || !form.dutyDate || !form.timeSlot) return
  if (isEditing.value) {
    const s = schedules.value.find(s => s.id === editId.value)
    if (s) { s.labName = form.labName; s.studentName = form.studentName; s.dutyDate = form.dutyDate; s.timeSlot = form.timeSlot; s.note = form.note || null }
  } else {
    const newId = Math.max(...schedules.value.map(s => s.id), 0) + 1
    schedules.value.push({ id: newId, labName: form.labName, studentName: form.studentName, dutyDate: form.dutyDate, timeSlot: form.timeSlot, status: 'scheduled', signTime: null, note: form.note || null })
  }
  showForm.value = false
}

// 删除
const showDelete = ref(false)
const deleteTarget = ref<Schedule | null>(null)
function confirmDelete(s: Schedule) { deleteTarget.value = s; showDelete.value = true }
function doDelete() {
  if (!deleteTarget.value) return
  const idx = schedules.value.findIndex(s => s.id === deleteTarget.value!.id)
  if (idx !== -1) schedules.value.splice(idx, 1)
  showDelete.value = false; deleteTarget.value = null
}

// 提醒
function sendRemind(s: Schedule) {
  alert(`已向「${s.studentName}」发送值日提醒：${s.dutyDate} ${timeSlotLabel(s.timeSlot)} 在 ${s.labName} 值日，请按时签到。`)
}
</script>

<style scoped>
.cs-page { height:100%; overflow-y:auto; padding:20px 24px }

.page-header { margin-bottom:16px }
.page-header h2 { font-size:20px; font-weight:600; color:#1a1a2e; margin:0 }
.page-header p { margin:4px 0 0; font-size:13px; color:#999 }

.stats-row { display:flex; gap:12px; margin-bottom:16px; flex-wrap:wrap }
.stat-card { background:rgba(255,255,255,.65); backdrop-filter:blur(10px); border:1px solid rgba(0,0,0,.06); border-radius:12px; padding:14px 20px; min-width:90px; text-align:center }
.stat-num { font-size:24px; font-weight:700; color:#1a1a2e; line-height:1.1 }
.stat-label { font-size:12px; color:#999; margin-top:2px }
.blue { color:#1a73e8 } .green { color:#27ae60 } .orange { color:#f39c12 }

.filter-bar { display:flex; align-items:center; gap:10px; margin-bottom:18px; flex-wrap:wrap }
.filter-tabs { display:flex; gap:4px; background:rgba(255,255,255,.4); border-radius:10px; padding:4px }
.filter-tab { padding:8px 16px; border-radius:8px; font-size:13px; color:#666; cursor:pointer; transition:all .15s; display:flex; align-items:center; gap:6px; user-select:none; white-space:nowrap }
.filter-tab:hover { color:#333; background:rgba(0,0,0,.03) }
.filter-tab.active { background:#fff; color:#1a73e8; font-weight:600; box-shadow:0 1px 3px rgba(0,0,0,.08) }
.tab-count { font-size:11px; background:rgba(0,0,0,.06); padding:1px 7px; border-radius:10px; color:#888 }
.filter-tab.active .tab-count { background:rgba(26,115,232,.1); color:#1a73e8 }
.tool-select { padding:8px 12px; border:1px solid rgba(0,0,0,.1); border-radius:10px; font-size:13px; background:rgba(255,255,255,.7); outline:none; cursor:pointer }

.week-picker { display:flex; align-items:center; gap:6px; background:rgba(255,255,255,.5); border:1px solid rgba(0,0,0,.08); border-radius:10px; padding:4px 8px }
.week-arrow { background:none; border:none; cursor:pointer; color:#666; font-size:16px; padding:2px 6px; border-radius:6px; line-height:1 }
.week-arrow:hover { background:rgba(0,0,0,.04); color:#333 }
.week-label { font-size:13px; color:#555; font-weight:500; min-width:60px; text-align:center }

.btn-add { padding:9px 18px; border:none; border-radius:10px; background:linear-gradient(135deg,#1a73e8,#4a90d9); color:#fff; font-size:13px; font-weight:500; cursor:pointer; white-space:nowrap; margin-left:auto }
.btn-add:hover { box-shadow:0 4px 12px rgba(26,115,232,.3) }

.schedule-list { display:flex; flex-direction:column; gap:16px }
.date-group { background:rgba(255,255,255,.45); backdrop-filter:blur(10px); border-radius:12px; border:1px solid rgba(0,0,0,.04); overflow:hidden }
.date-header { display:flex; align-items:center; justify-content:space-between; padding:12px 16px; background:rgba(0,0,0,.02); border-bottom:1px solid rgba(0,0,0,.04) }
.date-label { font-size:14px; font-weight:600; color:#1a1a2e }
.btn-add-small { padding:5px 14px; border:1px solid rgba(26,115,232,.25); border-radius:8px; background:rgba(26,115,232,.04); color:#1a73e8; font-size:12px; cursor:pointer }
.btn-add-small:hover { background:rgba(26,115,232,.1) }

.duty-cards { display:flex; flex-direction:column; gap:0 }
.duty-card { display:flex; align-items:center; justify-content:space-between; padding:12px 16px; border-bottom:1px solid rgba(0,0,0,.03); transition:background .12s }
.duty-card:last-child { border-bottom:none }
.duty-card:hover { background:rgba(0,0,0,.01) }
.duty-card.card-scheduled { border-left:3px solid #f39c12 }
.duty-card.card-signed { border-left:3px solid #1a73e8 }
.duty-card.card-done { border-left:3px solid #27ae60 }

.card-body { display:flex; flex-direction:column; gap:4px; min-width:0; flex:1 }
.card-main { display:flex; align-items:center; gap:12px; flex-wrap:wrap }
.lab-name { font-weight:600; color:#1a1a2e; font-size:14px }
.student-name { color:#555; font-size:13px }
.time-slot { color:#888; font-size:12px }
.card-meta { display:flex; align-items:center; gap:10px }

.status-tag { padding:1px 8px; border-radius:4px; font-size:11px; font-weight:500 }
.st-scheduled { background:rgba(243,156,18,.1); color:#e67e22 }
.st-signed { background:rgba(26,115,232,.1); color:#1a73e8 }
.st-done { background:rgba(39,174,96,.1); color:#27ae60 }
.sign-time { font-size:12px; color:#888 }
.note-text { font-size:12px; color:#999; max-width:200px; overflow:hidden; text-overflow:ellipsis; white-space:nowrap }

.card-actions { display:flex; gap:6px; flex-shrink:0 }
.act-btn { padding:5px 12px; border:1px solid rgba(0,0,0,.1); border-radius:8px; background:#fff; font-size:12px; cursor:pointer; color:#555 }
.act-btn:hover { border-color:#aaa }
.act-btn.edit:hover { border-color:#1a73e8; color:#1a73e8 }
.act-btn.remind { color:#e67e22; border-color:rgba(243,156,18,.25) }
.act-btn.remind:hover { border-color:#e67e22; background:rgba(243,156,18,.04) }
.act-btn.del:hover { border-color:#e74c3c; color:#e74c3c; background:rgba(231,76,60,.04) }

.empty-state { display:flex; flex-direction:column; align-items:center; padding:60px 0; color:#ccc; gap:8px }
.empty-state p { margin:0; font-size:15px; color:#bbb }

.pagination { display:flex; align-items:center; justify-content:center; gap:10px; margin-top:18px }
.pagination button { padding:7px 16px; border:1px solid rgba(0,0,0,.1); border-radius:8px; background:#fff; font-size:13px; cursor:pointer; color:#555 }
.pagination button:disabled { opacity:.4; cursor:not-allowed }

.radio-row { display:flex; gap:8px }
.radio-item { flex:1; padding:10px 12px; border:1px solid rgba(0,0,0,.1); border-radius:10px; font-size:13px; cursor:pointer; display:flex; align-items:center; gap:6px; color:#666; transition:.15s }
.radio-item input { display:none }
.radio-item.active { border-color:#1a73e8; background:rgba(26,115,232,.04); color:#1a73e8; font-weight:500 }
.notify-check { display:flex; align-items:center; gap:6px; font-size:13px; color:#666; cursor:pointer }
.notify-check input { accent-color:#1a73e8 }
.delete-warn { font-size:14px; color:#555; line-height:1.6; margin:0 }
</style>

<style>
.modal-overlay { position:fixed; inset:0; background:rgba(0,0,0,.35); display:flex; align-items:center; justify-content:center; z-index:1100; padding:20px }
.modal-panel { background:#fff; border-radius:16px; width:100%; max-width:480px; max-height:85vh; display:flex; flex-direction:column; box-shadow:0 16px 48px rgba(0,0,0,.15) }
.modal-small { max-width:400px }
.modal-header { display:flex; align-items:center; justify-content:space-between; padding:20px 24px 0 }
.modal-header h3 { margin:0; font-size:17px; font-weight:600; color:#1a1a2e }
.modal-close { background:none; border:none; cursor:pointer; color:#aaa; padding:4px; border-radius:6px }
.modal-close:hover { background:rgba(0,0,0,.05); color:#333 }
.modal-body { padding:20px 24px; overflow-y:auto; flex:1; display:flex; flex-direction:column; gap:14px }
.modal-footer { display:flex; justify-content:flex-end; gap:10px; padding:16px 24px 20px; border-top:1px solid rgba(0,0,0,.06) }
.modal-btn { padding:10px 24px; border-radius:10px; font-size:14px; cursor:pointer; border:none; font-weight:500; transition:.2s }
.modal-btn.cancel { background:rgba(0,0,0,.04); color:#666 }
.modal-btn.cancel:hover { background:rgba(0,0,0,.08) }
.modal-btn.primary { background:linear-gradient(135deg,#1a73e8,#4a90d9); color:#fff }
.modal-btn.primary:hover:not(:disabled) { box-shadow:0 4px 12px rgba(26,115,232,.3) }
.modal-btn:disabled { opacity:.4; cursor:not-allowed }
.modal-btn.danger { background:linear-gradient(135deg,#e74c3c,#c0392b); color:#fff }
.modal-btn.danger:hover { box-shadow:0 4px 12px rgba(231,76,60,.3) }
.form-g { display:flex; flex-direction:column; gap:4px }
.form-g label { font-size:13px; font-weight:500; color:#555 }
.required { color:#e74c3c }
.form-input { padding:10px 14px; border:1px solid rgba(0,0,0,.1); border-radius:10px; font-size:14px; color:#333; outline:none; background:rgba(255,255,255,.7); transition:.15s; font-family:inherit; width:100%; box-sizing:border-box }
.form-input:focus { border-color:#1a73e8 }
.modal-fade-enter-active { transition:all .2s ease }
.modal-fade-leave-active { transition:all .15s ease }
.modal-fade-enter-from,.modal-fade-leave-to { opacity:0 }
.modal-fade-enter-from .modal-panel { transform:scale(.94) translateY(12px) }
</style>
