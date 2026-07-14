<template>
  <div class="report-page">
    <div class="page-header">
      <h2>我的上报</h2>
      <p>查看上报记录与处理进度</p>
    </div>

    <!-- 状态筛选 -->
    <div class="filter-bar">
      <div
        v-for="f in statusFilters"
        :key="f.key"
        class="filter-tab"
        :class="{ active: activeFilter === f.key }"
        @click="activeFilter = f.key"
      >
        {{ f.label }}
        <span class="filter-count">{{ f.count }}</span>
      </div>
    </div>

    <div class="report-body">
      <!-- 左侧上报列表 -->
      <div class="report-list-col">
        <div class="report-list" v-if="filteredReports.length > 0">
          <div
            v-for="report in filteredReports"
            :key="report.id"
            class="report-card"
            :class="'status-' + report.status"
          >
            <div class="card-top">
              <div class="card-status-icon" :class="'icon-' + report.status">
                <svg v-if="report.status === 'pending'" viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/></svg>
                <svg v-else-if="report.status === 'processing'" viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/></svg>
                <svg v-else viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2"><path d="M22 11.08V12a10 10 0 11-5.93-9.14"/><polyline points="22 4 12 14.01 9 11.01"/></svg>
              </div>
              <div class="card-header-info">
                <div class="card-title-row">
                  <span class="card-title">{{ report.title }}</span>
                  <span class="level-badge" :class="'level-' + report.level">{{ levelLabel(report.level) }}</span>
                </div>
                <span class="card-lab">{{ report.labName }}</span>
              </div>
              <span class="status-tag" :class="'tag-' + report.status">{{ statusLabel(report.status) }}</span>
            </div>

            <!-- 时间线 -->
            <div class="card-timeline">
              <div class="tl-item">
                <span class="tl-label">上报时间</span>
                <span class="tl-value">{{ report.createdAt }}</span>
              </div>
              <div class="tl-item" v-if="report.deadline">
                <span class="tl-label">整改期限</span>
                <span class="tl-value" :class="{ 'text-danger': isOverdue(report) }">{{ report.deadline }}</span>
              </div>
              <div class="tl-item" v-if="report.responsiblePerson">
                <span class="tl-label">责任人</span>
                <span class="tl-value">{{ report.responsiblePerson }}</span>
              </div>
              <div class="tl-item" v-if="report.status === 'completed' && report.completedAt">
                <span class="tl-label">完成时间</span>
                <span class="tl-value" style="color: #27ae60;">{{ report.completedAt }}</span>
              </div>
            </div>

            <!-- 复核意见 -->
            <div v-if="report.reviewComment" class="card-review">
              <div class="review-label">复核意见</div>
              <p class="review-text">{{ report.reviewComment }}</p>
            </div>
          </div>
        </div>

        <div v-else class="empty-state">
          <svg viewBox="0 0 24 24" width="48" height="48" fill="none" stroke="#ccc" stroke-width="1.2"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>
          <p v-if="activeFilter === 'all'">暂无隐患上报记录</p>
          <p v-else>暂无{{ statusLabel(activeFilter) }}的上报</p>
        </div>
      </div>

      <!-- 右侧统计面板 -->
      <div class="stats-panel">
        <div class="panel-card">
          <h4 class="panel-card-title">上报统计</h4>

          <div class="donut-wrapper">
            <div class="donut-ring" :style="{ background: donutGradient }">
              <div class="donut-hole">
                <span class="donut-total">{{ reports.length }}</span>
                <span class="donut-label">总上报数</span>
              </div>
            </div>
          </div>

          <div class="donut-legend">
            <div v-for="item in legendItems" :key="item.label" class="legend-item">
              <span class="legend-dot" :style="{ background: item.color }"></span>
              <span class="legend-name">{{ item.label }}</span>
              <span class="legend-count">{{ item.count }}</span>
            </div>
          </div>

          <div class="stat-row">
            <span class="sr-label">完成率</span>
            <span class="sr-value" :class="completionRate >= 80 ? 'text-green' : completionRate >= 40 ? 'text-orange' : 'text-red'">{{ completionRate }}%</span>
          </div>
          <div class="stat-row">
            <span class="sr-label">重要隐患</span>
            <span class="sr-value">{{ importantCount }} 项</span>
          </div>
          <div class="stat-row">
            <span class="sr-label">超期未完成</span>
            <span class="sr-value text-red">{{ overdueCount }} 项</span>
          </div>

          <div class="quick-links">
            <div class="quick-link" @click="goReport">
              <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>
              <span>新建上报</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'

interface HazardReport {
  id: number
  title: string
  labName: string
  level: 'normal' | 'important'
  status: 'pending' | 'processing' | 'completed'
  createdAt: string
  deadline?: string
  completedAt?: string
  responsiblePerson?: string
  reviewComment?: string
}

const activeFilter = ref('all')
const emit = defineEmits(['navigate'])

const statusFilters = computed(() => [
  { key: 'all', label: '全部', count: reports.length },
  { key: 'pending', label: '待处理', count: reports.filter(r => r.status === 'pending').length },
  { key: 'processing', label: '整改中', count: reports.filter(r => r.status === 'processing').length },
  { key: 'completed', label: '已完成', count: reports.filter(r => r.status === 'completed').length },
])

const reports: HazardReport[] = [
  {
    id: 1, title: '化学实验室通风橱排风故障', labName: 'A201 化学实验室', level: 'important',
    status: 'pending', createdAt: '2026-07-08 14:00', deadline: '2026-07-10',
  },
  {
    id: 2, title: 'B103 实验室天花板漏水', labName: 'B103 物理实验室', level: 'important',
    status: 'processing', createdAt: '2026-07-05 09:00', deadline: '2026-07-12',
    responsiblePerson: '王老师',
  },
  {
    id: 3, title: '信息楼消防通道堆放杂物', labName: 'C202 计算机实验室', level: 'normal',
    status: 'completed', createdAt: '2026-07-01 10:00', completedAt: '2026-07-03',
    reviewComment: '已清理完毕，消防通道畅通。提醒物业加强日常巡查。',
  },
  {
    id: 4, title: 'D305 实验室洗眼器水压不足', labName: 'D305 生物实验室', level: 'normal',
    status: 'processing', createdAt: '2026-07-06 16:00', deadline: '2026-07-09',
    responsiblePerson: '赵老师',
  },
  {
    id: 5, title: 'E108 电子实验室电路老化', labName: 'E108 电子实验室', level: 'important',
    status: 'completed', createdAt: '2026-06-28 08:00', deadline: '2026-07-02',
    completedAt: '2026-07-01', responsiblePerson: '陈老师',
    reviewComment: '已更换老化线路，经检测电路安全达标。定期检查制度已建立。',
  },
  {
    id: 6, title: 'A305 实验室气瓶固定松动', labName: 'A305 分析化学实验室', level: 'important',
    status: 'processing', createdAt: '2026-07-04 11:00', deadline: '2026-07-06',
    responsiblePerson: '李老师',
  },
]

const filteredReports = computed(() => {
  if (activeFilter.value === 'all') return reports
  return reports.filter(r => r.status === activeFilter.value)
})

function statusLabel(s: string): string {
  const m: Record<string, string> = { pending: '待处理', processing: '整改中', completed: '已完成' }
  return m[s] || s
}
function levelLabel(l: string): string { return l === 'important' ? '重要隐患' : '一般隐患' }
function isOverdue(r: HazardReport): boolean {
  if (!r.deadline || r.status === 'completed') return false
  return new Date(r.deadline) < new Date()
}
function goReport(): void { emit('navigate', { key: 'hazard-report', name: '隐患上报' }) }

// ===== 统计 =====
const statusColors: Record<string, string> = { pending: '#e67e22', processing: '#1a73e8', completed: '#27ae60' }
const statusLabels: Record<string, string> = { pending: '待处理', processing: '整改中', completed: '已完成' }

const donutGradient = computed(() => {
  const counts: Record<string, number> = { pending: 0, processing: 0, completed: 0 }
  reports.forEach(r => { counts[r.status] = (counts[r.status] || 0) + 1 })
  const total = reports.length
  if (total === 0) return 'conic-gradient(#eee 0deg, #eee 360deg)'
  const order = ['completed', 'processing', 'pending']
  let acc = 0; const parts: string[] = []
  order.forEach(s => { if (counts[s] === 0) return; const pct = counts[s] / total; const start = acc; acc += pct; parts.push(`${statusColors[s]} ${start * 360}deg ${acc * 360}deg`) })
  return `conic-gradient(${parts.join(', ')})`
})
const legendItems = computed(() => {
  const counts: Record<string, number> = {}
  reports.forEach(r => { counts[r.status] = (counts[r.status] || 0) + 1 })
  return ['completed', 'processing', 'pending'].filter(s => counts[s] > 0).map(s => ({ label: statusLabels[s], count: counts[s], color: statusColors[s] }))
})

const completionRate = computed(() => {
  if (reports.length === 0) return 0
  return Math.round((reports.filter(r => r.status === 'completed').length / reports.length) * 100)
})
const importantCount = computed(() => reports.filter(r => r.level === 'important').length)
const overdueCount = computed(() => reports.filter(r => r.status !== 'completed' && r.deadline && new Date(r.deadline) < new Date()).length)
</script>

<style scoped>
.report-page { height: 100%; overflow-y: auto; padding: 28px 36px; }

.page-header { margin-bottom: 20px; }
.page-header h2 { font-size: 22px; font-weight: 700; color: #1a1a2e; margin: 0 0 4px; }
.page-header p { font-size: 14px; color: #888; margin: 0; }

/* 筛选栏 */
.filter-bar { display: flex; gap: 4px; background: rgba(255,255,255,.4); border-radius: 10px; padding: 4px; margin-bottom: 18px; width: fit-content; }
.filter-tab { padding: 8px 18px; border-radius: 8px; font-size: 13px; color: #666; cursor: pointer; transition: all .15s; display: flex; align-items: center; gap: 6px; user-select: none; }
.filter-tab:hover { color: #333; background: rgba(0,0,0,.03); }
.filter-tab.active { background: #fff; color: #1a73e8; font-weight: 600; box-shadow: 0 1px 3px rgba(0,0,0,.08); }
.filter-count { font-size: 11px; background: rgba(0,0,0,.06); padding: 1px 7px; border-radius: 10px; color: #888; }
.filter-tab.active .filter-count { background: rgba(26,115,232,.1); color: #1a73e8; }

/* 两栏 */
.report-body { display: flex; gap: 24px; align-items: flex-start; }
.report-list-col { flex: 1; min-width: 0; }
.report-list { display: flex; flex-direction: column; gap: 12px; }

/* 卡片 */
.report-card { background: rgba(255,255,255,.5); border-radius: 14px; border: 1px solid rgba(0,0,0,.04); padding: 18px 20px; }
.report-card.status-pending { border-left: 3px solid #e67e22; }
.report-card.status-processing { border-left: 3px solid #1a73e8; }
.report-card.status-completed { border-left: 3px solid #27ae60; opacity: .75; }

.card-top { display: flex; align-items: flex-start; gap: 10px; margin-bottom: 12px; }
.card-status-icon { width: 32px; height: 32px; border-radius: 50%; display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
.icon-pending { background: rgba(230,126,34,.1); color: #e67e22; }
.icon-processing { background: rgba(26,115,232,.1); color: #1a73e8; }
.icon-completed { background: rgba(39,174,96,.1); color: #27ae60; }

.card-header-info { flex: 1; min-width: 0; }
.card-title-row { display: flex; align-items: center; gap: 8px; margin-bottom: 2px; }
.card-title { font-size: 14px; font-weight: 600; color: #1a1a2e; }
.level-badge { font-size: 10px; font-weight: 600; padding: 2px 6px; border-radius: 3px; white-space: nowrap; }
.level-important { background: rgba(231,76,60,.1); color: #e74c3c; }
.level-normal { background: rgba(230,126,34,.1); color: #e67e22; }
.card-lab { font-size: 12px; color: #aaa; }

.status-tag { font-size: 11px; font-weight: 600; padding: 3px 10px; border-radius: 4px; white-space: nowrap; }
.tag-pending { background: rgba(230,126,34,.1); color: #e67e22; }
.tag-processing { background: rgba(26,115,232,.1); color: #1a73e8; }
.tag-completed { background: rgba(39,174,96,.1); color: #27ae60; }

/* 时间线 */
.card-timeline { display: flex; flex-wrap: wrap; gap: 6px 16px; margin-bottom: 8px; }
.tl-item { display: flex; align-items: center; gap: 6px; font-size: 12px; }
.tl-label { color: #aaa; }
.tl-value { color: #555; }
.text-danger { color: #e74c3c; font-weight: 600; }

/* 复核意见 */
.card-review { padding: 10px 14px; background: rgba(39,174,96,.04); border-radius: 8px; border-left: 3px solid #27ae60; }
.review-label { font-size: 11px; font-weight: 600; color: #27ae60; margin-bottom: 3px; }
.review-text { font-size: 13px; color: #555; margin: 0; line-height: 1.6; }

.empty-state { display: flex; flex-direction: column; align-items: center; padding: 80px 0; color: #ccc; }
.empty-state p { font-size: 14px; margin: 10px 0 4px; }

/* 右侧面板 */
.stats-panel { width: 300px; flex-shrink: 0; position: sticky; top: 0; }
.panel-card { background: rgba(255,255,255,.55); border-radius: 14px; border: 1px solid rgba(0,0,0,.04); padding: 24px 22px; }
.panel-card-title { font-size: 14px; font-weight: 600; color: #888; text-transform: uppercase; letter-spacing: .5px; margin: 0 0 20px; text-align: center; }

.donut-wrapper { display: flex; justify-content: center; margin-bottom: 14px; }
.donut-ring { width: 160px; height: 160px; border-radius: 50%; position: relative; display: flex; align-items: center; justify-content: center; }
.donut-hole { width: 106px; height: 106px; border-radius: 50%; background: #fff; display: flex; flex-direction: column; align-items: center; justify-content: center; z-index: 1; }
.donut-total { font-size: 26px; font-weight: 700; color: #1a1a2e; line-height: 1; }
.donut-label { font-size: 11px; color: #aaa; margin-top: 2px; }

.donut-legend { display: flex; flex-direction: column; gap: 6px; margin-bottom: 14px; }
.legend-item { display: flex; align-items: center; gap: 8px; }
.legend-dot { width: 8px; height: 8px; border-radius: 50%; flex-shrink: 0; }
.legend-name { flex: 1; font-size: 12px; color: #555; }
.legend-count { font-size: 12px; color: #aaa; font-weight: 600; }

.stat-row { display: flex; align-items: center; justify-content: space-between; padding: 6px 0; }
.stat-row + .stat-row { border-top: 1px solid rgba(0,0,0,.03); }
.sr-label { font-size: 13px; color: #888; }
.sr-value { font-size: 14px; font-weight: 600; color: #333; }
.text-green { color: #27ae60; }
.text-orange { color: #e67e22; }
.text-red { color: #e74c3c; }

.quick-links { margin-top: 14px; padding-top: 12px; border-top: 1px solid rgba(0,0,0,.06); }
.quick-link { display: flex; align-items: center; gap: 8px; padding: 8px 12px; border-radius: 8px; cursor: pointer; color: #e67e22; font-size: 13px; transition: background .15s; }
.quick-link:hover { background: rgba(230,126,34,.05); }
</style>
