<template>
  <div class="tce-page">
    <div class="page-header">
      <div>
        <h2>检查评价</h2>
        <p>对学生值日情况打分评价</p>
      </div>
      <button class="back-link" @click="goBack">
        <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2"><path d="M19 12H5M12 19l-7-7 7-7"/></svg>
        返回值日安排
      </button>
    </div>

    <!-- 统计卡片 -->
    <div class="stats-row">
      <div class="stat-card">
        <div class="stat-icon pending-icon">
          <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2"><path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/><path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"/></svg>
        </div>
        <div class="stat-body">
          <span class="stat-num orange">{{ pendingList.length }}</span>
          <span class="stat-label">待评价</span>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon avg-icon">
          <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2"><polyline points="22 12 18 12 15 21 9 3 6 12 2 12"/></svg>
        </div>
        <div class="stat-body">
          <span class="stat-num blue">{{ avgScore }}</span>
          <span class="stat-label">本周平均分</span>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon done-icon">
          <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/><polyline points="22 4 12 14.01 9 11.01"/></svg>
        </div>
        <div class="stat-body">
          <span class="stat-num green">{{ historyList.length }}</span>
          <span class="stat-label">已评价</span>
        </div>
      </div>
      <div class="stat-filter">
        <span class="filter-label">实验室</span>
        <select v-model="labFilter" class="filter-select">
          <option value="">全部</option>
          <option v-for="lab in labOptions" :key="lab" :value="lab">{{ lab }}</option>
        </select>
      </div>
    </div>

    <!-- 待评价列表 -->
    <div class="section">
      <div class="section-header">
        <h3>待评价</h3>
        <span class="section-count">{{ filteredPending.length }} 条</span>
      </div>

      <div v-if="filteredPending.length === 0" class="empty-state">暂无待评价记录</div>

      <div v-for="item in filteredPending" :key="item.id" class="eval-card">
        <div class="eval-info">
          <div class="eval-student">
            <span class="student-name">{{ item.studentName }}</span>
            <span class="student-class">{{ item.className }}</span>
          </div>
          <div class="eval-meta">
            <span>{{ item.labName }}</span>
            <span class="meta-sep">|</span>
            <span>{{ item.date }} {{ item.weekday }}</span>
            <span class="meta-sep">|</span>
            <span>{{ item.timeSlot }}</span>
            <span class="meta-sep">|</span>
            <span>签到 {{ item.checkInTime }}</span>
          </div>
        </div>

        <div class="eval-actions">
          <div class="score-area">
            <span class="score-label">卫生评分</span>
            <div class="score-btns">
              <button
                v-for="s in quickScores"
                :key="s"
                class="score-btn"
                :class="{ active: scores[item.id] === s }"
                @click="scores[item.id] = s"
              >{{ s }}</button>
            </div>
            <input
              type="number"
              v-model.number="scores[item.id]"
              class="score-input"
              min="0"
              max="100"
              placeholder="自定义"
              @change="clampScore(item.id)"
            />
            <span class="score-unit">分</span>
            <span v-if="scores[item.id]" class="score-level" :class="scoreClass(scores[item.id])">{{ scoreLevel(scores[item.id]) }}</span>
          </div>
          <div class="comment-area">
            <input
              v-model="comments[item.id]"
              type="text"
              class="comment-input"
              placeholder="评语（选填）"
              maxlength="200"
            />
            <button class="submit-btn" @click="submitEval(item)" :disabled="!scores[item.id] || submittingId === item.id">
              {{ submittingId === item.id ? '提交中...' : '提交评价' }}
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- 已评价记录 -->
    <div class="section">
      <div class="section-header toggle-header" @click="showHistory = !showHistory">
        <h3>已评价记录</h3>
        <span class="section-count">{{ historyList.length }} 条</span>
        <span class="toggle-icon">{{ showHistory ? '收起' : '展开' }}</span>
      </div>

      <div v-if="showHistory && historyList.length === 0" class="empty-state">暂无评价记录</div>

      <div v-if="showHistory" class="history-list">
        <div v-for="rec in historyList" :key="rec.id" class="history-row">
          <div class="history-info">
            <span class="history-name">{{ rec.studentName }}</span>
            <span class="history-class">{{ rec.className }}</span>
            <span class="history-lab">{{ rec.labName }}</span>
            <span class="history-date">{{ rec.date }}</span>
          </div>
          <div class="history-score" :class="scoreClass(rec.score)">{{ rec.score }} 分</div>
          <div class="history-comment" v-if="rec.comment">{{ rec.comment }}</div>
          <div class="history-time">{{ rec.evaluatedAt }}</div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed } from 'vue'

const emit = defineEmits<{ navigate: [payload: { key: string; name: string }] }>()

// ========== 筛选 ==========
const labFilter = ref('')
const labOptions = ['A201 有机化学实验室', 'B103 物理实验室', 'C202 计算机实验室']
const showHistory = ref(false)

// ========== 评分状态 ==========
const scores = reactive<Record<number, number>>({})
const comments = reactive<Record<number, string>>({})
const submittingId = ref<number | null>(null)
const quickScores = [60, 70, 80, 90, 100]

// ========== Mock 待评价 ==========
interface PendingItem {
  id: number; studentName: string; className: string; labName: string
  date: string; weekday: string; timeSlot: string; checkInTime: string
}

const pendingList = ref<PendingItem[]>([
  { id: 1, studentName: '张同学', className: '计科2401', labName: 'A201 有机化学实验室',
    date: '7/8', weekday: '周三', timeSlot: '08:00-10:00', checkInTime: '08:15' },
  { id: 2, studentName: '赵同学', className: '软工2402', labName: 'B103 物理实验室',
    date: '7/7', weekday: '周二', timeSlot: '14:00-16:00', checkInTime: '14:03' },
  { id: 3, studentName: '张同学', className: '计科2401', labName: 'A201 有机化学实验室',
    date: '7/9', weekday: '周四', timeSlot: '08:00-10:00', checkInTime: '08:08' },
])

const filteredPending = computed(() => {
  if (!labFilter.value) return pendingList.value
  return pendingList.value.filter(p => p.labName === labFilter.value)
})

// ========== Mock 已评价 ==========
interface HistoryItem {
  id: number; studentName: string; className: string; labName: string
  date: string; score: number; comment: string; evaluatedAt: string
}

const historyList = ref<HistoryItem[]>([
  { id: 101, studentName: '李同学', className: '计科2401', labName: 'A201 有机化学实验室',
    date: '7/7', score: 88, comment: '台面擦拭干净，器材归位整齐', evaluatedAt: '7/7 10:30' },
  { id: 102, studentName: '王同学', className: '软工2402', labName: 'B103 物理实验室',
    date: '7/6', score: 75, comment: '地面清扫不够彻底，注意角落', evaluatedAt: '7/6 16:20' },
  { id: 103, studentName: '刘同学', className: '计科2401', labName: 'C202 计算机实验室',
    date: '7/6', score: 92, comment: '完成质量高，继续保持', evaluatedAt: '7/6 12:10' },
  { id: 104, studentName: '李同学', className: '软工2402', labName: 'A201 有机化学实验室',
    date: '7/5', score: 80, comment: '', evaluatedAt: '7/5 10:45' },
])

// ========== 统计 ==========
const avgScore = computed(() => {
  if (historyList.value.length === 0) return '--'
  const sum = historyList.value.reduce((a, b) => a + b.score, 0)
  return Math.round(sum / historyList.value.length)
})

// ========== 方法 ==========
function clampScore(id: number) {
  const v = scores[id]
  if (!Number.isFinite(v) || v < 0) scores[id] = 0
  else if (v > 100) scores[id] = 100
}

function scoreClass(s: number) {
  if (s >= 80) return 'green'
  if (s >= 60) return 'orange'
  return 'red'
}

function scoreLevel(s: number) {
  if (s >= 90) return '优秀'
  if (s >= 80) return '良好'
  if (s >= 60) return '合格'
  return '不合格'
}

async function submitEval(item: PendingItem) {
  if (!scores[item.id] || submittingId.value) return
  submittingId.value = item.id
  // 模拟提交延迟
  await new Promise(r => setTimeout(r, 400))
  // 加入已评价
  historyList.value.unshift({
    id: Date.now(),
    studentName: item.studentName,
    className: item.className,
    labName: item.labName,
    date: item.date,
    score: scores[item.id],
    comment: comments[item.id] || '',
    evaluatedAt: new Date().toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' }),
  })
  // 从待评价移除
  const idx = pendingList.value.findIndex(p => p.id === item.id)
  if (idx >= 0) pendingList.value.splice(idx, 1)
  delete scores[item.id]
  delete comments[item.id]
  submittingId.value = null
}

function goBack() {
  emit('navigate', { key: 'clean-arrange', name: '值日安排' })
}
</script>

<style scoped>
.tce-page { height:100%; display:flex; flex-direction:column; overflow-y:auto; padding:20px 24px }

.page-header { display:flex; align-items:center; justify-content:space-between; margin-bottom:16px; flex-shrink:0 }
.page-header h2 { margin:0; font-size:20px; font-weight:600; color:#1a1a2e }
.page-header p { margin:4px 0 0; font-size:13px; color:#999 }
.back-link { display:flex; align-items:center; gap:6px; background:none; border:none; color:#1a73e8; font-size:13px; cursor:pointer; padding:8px 12px; border-radius:8px; transition:.15s; flex-shrink:0 }
.back-link:hover { background:rgba(26,115,232,.06) }

/* ===== 统计卡片 ===== */
.stats-row { display:flex; align-items:center; gap:12px; margin-bottom:18px; flex-shrink:0; flex-wrap:wrap }
.stat-card { display:flex; align-items:center; gap:10px; background:rgba(255,255,255,.65); backdrop-filter:blur(10px); border:1px solid rgba(0,0,0,.06); border-radius:12px; padding:12px 18px; min-width:120px }
.stat-icon { width:36px; height:36px; border-radius:10px; display:flex; align-items:center; justify-content:center; flex-shrink:0 }
.pending-icon { background:rgba(243,156,18,.1); color:#f39c12 }
.avg-icon { background:rgba(26,115,232,.1); color:#1a73e8 }
.done-icon { background:rgba(39,174,96,.1); color:#27ae60 }
.stat-body { display:flex; flex-direction:column }
.stat-num { font-size:20px; font-weight:700; color:#1a1a2e; line-height:1.1 }
.stat-num.orange { color:#f39c12 }
.stat-num.blue { color:#1a73e8 }
.stat-num.green { color:#27ae60 }
.stat-label { font-size:12px; color:#999 }

.stat-filter { display:flex; align-items:center; gap:8px; margin-left:auto }
.filter-label { font-size:13px; color:#888 }
.filter-select { padding:8px 12px; border:1px solid rgba(0,0,0,.1); border-radius:10px; font-size:13px; color:#555; outline:none; background:rgba(255,255,255,.7); backdrop-filter:blur(10px); cursor:pointer }

/* ===== 分区 ===== */
.section { margin-bottom:18px; flex-shrink:0 }
.section-header { display:flex; align-items:center; gap:10px; margin-bottom:10px }
.section-header h3 { margin:0; font-size:15px; font-weight:600; color:#1a1a2e }
.section-count { font-size:12px; color:#aaa }
.toggle-header { cursor:pointer; user-select:none; transition:.15s }
.toggle-header:hover { opacity:.7 }
.toggle-icon { font-size:12px; color:#1a73e8; margin-left:auto }

/* ===== 待评价卡片 ===== */
.eval-card { background:rgba(255,255,255,.65); backdrop-filter:blur(10px); border:1px solid rgba(0,0,0,.06); border-radius:14px; padding:16px 20px; margin-bottom:10px }
.eval-info { margin-bottom:14px }
.eval-student { display:flex; align-items:center; gap:8px; margin-bottom:4px }
.student-name { font-size:15px; font-weight:600; color:#1a1a2e }
.student-class { font-size:12px; color:#aaa }
.eval-meta { font-size:12px; color:#888; display:flex; align-items:center; gap:6px; flex-wrap:wrap }
.meta-sep { color:#ddd }

.eval-actions { display:flex; flex-direction:column; gap:10px }

/* 评分区 */
.score-area { display:flex; align-items:center; gap:10px; flex-wrap:wrap }
.score-label { font-size:13px; font-weight:500; color:#555; white-space:nowrap }
.score-btns { display:flex; gap:6px }
.score-btn { padding:6px 14px; border:1px solid rgba(0,0,0,.12); border-radius:8px; background:#fff; font-size:13px; font-weight:500; color:#555; cursor:pointer; transition:.15s }
.score-btn:hover { border-color:#1a73e8; color:#1a73e8 }
.score-btn.active { background:#1a73e8; color:#fff; border-color:#1a73e8 }
.score-input { width:60px; padding:6px 8px; border:1px solid rgba(0,0,0,.12); border-radius:8px; font-size:13px; text-align:center; outline:none; -moz-appearance:textfield; background:rgba(255,255,255,.7) }
.score-input::-webkit-inner-spin-button,.score-input::-webkit-outer-spin-button { -webkit-appearance:none }
.score-unit { font-size:13px; color:#888 }
.score-level { font-size:12px; font-weight:600; padding:2px 8px; border-radius:4px }
.score-level.green { background:rgba(39,174,96,.12); color:#1e8449 }
.score-level.orange { background:rgba(243,156,18,.12); color:#b8730a }
.score-level.red { background:rgba(231,76,60,.12); color:#b03a2e }

/* 评语区 */
.comment-area { display:flex; align-items:center; gap:10px }
.comment-input { flex:1; padding:8px 12px; border:1px solid rgba(0,0,0,.1); border-radius:10px; font-size:13px; outline:none; background:rgba(255,255,255,.7); transition:.15s }
.comment-input:focus { border-color:#1a73e8 }
.submit-btn { padding:8px 24px; border:none; border-radius:10px; background:linear-gradient(135deg,#1a73e8,#4a90d9); color:#fff; font-size:13px; font-weight:500; cursor:pointer; transition:all .2s; white-space:nowrap }
.submit-btn:hover:not(:disabled) { box-shadow:0 4px 12px rgba(26,115,232,.3) }
.submit-btn:disabled { opacity:.4; cursor:not-allowed }

/* ===== 已评价记录 ===== */
.history-list { display:flex; flex-direction:column; gap:6px }
.history-row { display:flex; align-items:center; gap:12px; padding:10px 16px; background:rgba(255,255,255,.5); border-radius:10px; flex-wrap:wrap }
.history-info { display:flex; align-items:center; gap:8px; flex:1; min-width:0; flex-wrap:wrap }
.history-name { font-size:14px; font-weight:500; color:#222 }
.history-class { font-size:11px; color:#aaa }
.history-lab { font-size:12px; color:#888 }
.history-date { font-size:11px; color:#bbb }
.history-score { font-size:14px; font-weight:700; white-space:nowrap }
.history-score.green { color:#27ae60 }
.history-score.orange { color:#f39c12 }
.history-score.red { color:#e74c3c }
.history-comment { font-size:12px; color:#666; max-width:200px; white-space:nowrap; overflow:hidden; text-overflow:ellipsis }
.history-time { font-size:11px; color:#bbb; margin-left:auto; white-space:nowrap }

/* ===== 空状态 ===== */
.empty-state { padding:40px 20px; text-align:center; color:#bbb; font-size:13px }

.green { color:#27ae60 }
.orange { color:#f39c12 }
.red { color:#e74c3c }
.blue { color:#1a73e8 }
</style>
