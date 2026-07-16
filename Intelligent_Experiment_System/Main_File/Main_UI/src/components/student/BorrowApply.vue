<template>
  <div class="borrow-page">
    <div class="page-header">
      <h2>借用与预约</h2>
      <p>查看设备借用与场地预约的审核进度</p>
    </div>

    <!-- 类型 + 状态筛选 -->
    <div class="filter-bar">
      <div class="filter-row">
        <div class="filter-group">
          <div
            v-for="t in typeFilters"
            :key="t.key"
            class="filter-tab"
            :class="{ active: activeType === t.key }"
            @click="activeType = t.key"
          >
            {{ t.label }}
            <span v-if="t.count !== undefined" class="filter-count">{{ t.count }}</span>
          </div>
        </div>
        <div class="filter-group" style="margin-left: 12px;">
          <div
            v-for="s in statusFilters"
            :key="s.key"
            class="filter-tab"
            :class="{ active: activeStatus === s.key }"
            @click="activeStatus = s.key"
          >
            {{ s.label }}
            <span v-if="s.count !== undefined" class="filter-count">{{ s.count }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 主体 -->
    <div class="borrow-body">
      <div class="apply-list-col">
        <div class="apply-list" v-if="filteredApplies.length > 0">
          <div
            v-for="apply in filteredApplies"
            :key="apply.id"
            class="apply-card"
            :class="'card-status-' + apply.status"
          >
            <!-- 状态 + 类型行 -->
            <div class="card-top">
              <div class="card-status-icon" :class="'icon-' + apply.status">
                <svg v-if="apply.status === 'pending'" viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/></svg>
                <svg v-else-if="apply.status === 'approved'" viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2"><path d="M22 11.08V12a10 10 0 11-5.93-9.14"/><polyline points="22 4 12 14.01 9 11.01"/></svg>
                <svg v-else-if="apply.status === 'rejected'" viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="15" y1="9" x2="9" y2="15"/><line x1="9" y1="9" x2="15" y2="15"/></svg>
                <svg v-else viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="8" y1="12" x2="16" y2="12"/></svg>
              </div>
              <div class="card-status-text">
                <span class="status-label">{{ statusLabel(apply.status) }}</span>
                <span class="type-badge" :class="'type-' + apply.type">{{ typeLabel(apply.type) }}</span>
                <span class="apply-no">{{ apply.applyNo }}</span>
              </div>
              <span class="card-date">{{ apply.createdAt }}</span>
            </div>

            <!-- 设备借用卡片内容 -->
            <template v-if="apply.type === 'device'">
              <div class="card-device">
                <div class="card-device-icon">
                  <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="1.6"><rect x="3" y="3" width="18" height="18" rx="3"/><circle cx="12" cy="12" r="4"/></svg>
                </div>
                <div class="card-device-info">
                  <span class="card-device-name">{{ apply.targetName }}</span>
                  <span class="card-device-no">{{ apply.targetNo }}</span>
                </div>
              </div>
              <div class="card-times">
                <div class="time-row">
                  <span class="time-label">预计使用</span>
                  <span class="time-value">{{ apply.startTime }} ~ {{ apply.endTime }}</span>
                </div>
                <div v-if="apply.approvedAt" class="time-row">
                  <span class="time-label">审批时间</span>
                  <span class="time-value">{{ apply.approvedAt }}</span>
                </div>
                <div v-if="apply.pickupHint" class="pickup-hint">
                  <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="12" y1="16" x2="12" y2="12"/><line x1="12" y1="8" x2="12.01" y2="8"/></svg>
                  <span>{{ apply.pickupHint }}</span>
                </div>
              </div>
            </template>

            <!-- 场地预约卡片内容 -->
            <template v-if="apply.type === 'lab'">
              <div class="card-device">
                <div class="card-device-icon lab-icon">
                  <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="1.6"><path d="M4 19.5A2.5 2.5 0 016.5 17H20"/><path d="M6.5 2H20v20H6.5A2.5 2.5 0 014 19.5v-15A2.5 2.5 0 016.5 2z"/></svg>
                </div>
                <div class="card-device-info">
                  <span class="card-device-name">{{ apply.targetName }}</span>
                  <span class="card-device-no">{{ apply.targetNo }}</span>
                </div>
              </div>
              <div class="card-times">
                <div class="time-row">
                  <span class="time-label">预约日期</span>
                  <span class="time-value">{{ apply.startTime }}</span>
                </div>
                <div class="time-row">
                  <span class="time-label">时间段</span>
                  <span class="time-value">{{ apply.endTime }}</span>
                </div>
                <div class="time-row" v-if="apply.peopleCount">
                  <span class="time-label">使用人数</span>
                  <span class="time-value">{{ apply.peopleCount }} 人</span>
                </div>
                <div v-if="apply.approvedAt" class="time-row">
                  <span class="time-label">审批时间</span>
                  <span class="time-value">{{ apply.approvedAt }}</span>
                </div>
                <div v-if="apply.pickupHint" class="pickup-hint lab-hint">
                  <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="12" y1="16" x2="12" y2="12"/><line x1="12" y1="8" x2="12.01" y2="8"/></svg>
                  <span>{{ apply.pickupHint }}</span>
                </div>
              </div>
            </template>

            <!-- 驳回原因 -->
            <div v-if="apply.status === 'rejected' && apply.rejectReason" class="reject-box">
              <div class="reject-label">驳回原因</div>
              <p class="reject-text">{{ apply.rejectReason }}</p>
            </div>

            <!-- 操作 -->
            <div class="card-actions">
              <button v-if="apply.status === 'pending'" class="action-btn danger" @click="cancelApply(apply.applyNo)">取消申请</button>
              <button v-if="apply.status === 'rejected'" class="action-btn primary" @click="reApply(apply)">重新申请</button>
              <span v-if="apply.status === 'approved'" class="action-hint">{{ apply.type === 'lab' ? '请按时使用实验室' : '请按时前往实验室领用设备' }}</span>
              <span v-if="apply.status === 'cancelled'" class="action-hint cancelled-text">申请已取消</span>
            </div>
          </div>
        </div>

        <div v-else class="empty-state">
          <svg viewBox="0 0 24 24" width="48" height="48" fill="none" stroke="#ccc" stroke-width="1.2"><path d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2"/></svg>
          <p v-if="activeStatus === 'all' && activeType === 'all'">暂无借用与预约记录</p>
          <p v-else>暂无符合条件的申请</p>
          <span class="empty-hint">可在"设备查询"或"实验室查询"中发起申请</span>
        </div>
      </div>

      <!-- 右侧统计面板 -->
      <div class="stats-panel">
        <div class="panel-card">
          <h4 class="panel-card-title">申请统计</h4>

          <div class="donut-wrapper">
            <div class="donut-ring" :style="{ background: donutGradient }">
              <div class="donut-hole">
                <span class="donut-total">{{ applies.length }}</span>
                <span class="donut-label">总申请数</span>
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
            <span class="sr-label">设备借用</span>
            <span class="sr-value">{{ deviceCount }} 项</span>
          </div>
          <div class="stat-row">
            <span class="sr-label">场地预约</span>
            <span class="sr-value">{{ labCount }} 项</span>
          </div>
          <div class="stat-row">
            <span class="sr-label">审批通过率</span>
            <span class="sr-value">{{ approvalRate }}%</span>
          </div>

          <div class="quick-links">
            <div class="quick-link" @click="goDeviceSearch">
              <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2"><circle cx="11" cy="11" r="8"/><path d="M21 21l-4.35-4.35"/></svg>
              <span>设备查询</span>
            </div>
            <div class="quick-link" @click="goLabSearch">
              <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2"><path d="M4 19.5A2.5 2.5 0 016.5 17H20"/><path d="M6.5 2H20v20H6.5A2.5 2.5 0 014 19.5v-15A2.5 2.5 0 016.5 2z"/></svg>
              <span>实验室查询</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onBeforeUnmount, onMounted } from 'vue'
import api from '../../utils/api'

interface BorrowApply {
  id: number
  applyNo: string
  type: 'device' | 'lab'
  targetName: string
  targetNo: string
  status: 'pending' | 'approved' | 'rejected' | 'cancelled'
  startTime: string
  endTime: string
  createdAt: string
  approvedAt?: string
  pickupHint?: string
  rejectReason?: string
  peopleCount?: number
}

function fmtDt(dt: string | undefined | null): string {
  if (!dt) return ''
  return dt.replace('T', ' ').slice(0, 16)
}

const activeType = ref('all')
const activeStatus = ref('all')

const emit = defineEmits(['navigate'])

const typeFilters = computed(() => [
  { key: 'all', label: '全部', count: applies.value.length },
  { key: 'device', label: '设备借用', count: applies.value.filter(a => a.type === 'device').length },
  { key: 'lab', label: '场地预约', count: applies.value.filter(a => a.type === 'lab').length },
])

const statusFilters = computed(() => [
  { key: 'all', label: '全部', count: applies.value.length },
  { key: 'pending', label: '待审核', count: applies.value.filter(a => a.status === 'pending').length },
  { key: 'approved', label: '审批通过', count: applies.value.filter(a => a.status === 'approved').length },
  { key: 'rejected', label: '已驳回', count: applies.value.filter(a => a.status === 'rejected').length },
])

const applies = ref<BorrowApply[]>([])

async function loadData() {
  const [deviceResult, labResult] = await Promise.allSettled([
    api.get('/equipment/borrow-applies/my', { params: { pageNum: 1, pageSize: 100 } }),
    api.get('/lab-management/reservations/my', { params: { page: 1, pageSize: 100 } })
  ])
  const items: BorrowApply[] = []

  if (deviceResult.status === 'fulfilled') {
    const records = deviceResult.value.data?.data?.records || []
    records.forEach((r: any) => {
      const statusMap: Record<number, BorrowApply['status']> = { 1: 'pending', 2: 'approved', 3: 'rejected', 4: 'cancelled' }
      items.push({
        id: r.id,
        applyNo: r.applyNo || `BORROW-${r.id}`,
        type: 'device' as const,
        targetName: r.deviceName || '',
        targetNo: r.deviceNo || `DEV-${r.deviceId || ''}`,
        status: statusMap[r.applyStatus] || 'pending',
        startTime: fmtDt(r.expectedStartTime),
        endTime: fmtDt(r.expectedReturnTime),
        createdAt: fmtDt(r.createdAt),
        approvedAt: r.applyStatus && r.applyStatus !== 1 ? fmtDt(r.updatedAt) : undefined,
        pickupHint: r.applyStatus === 2 ? '申请已通过，请等待管理员办理出库。' : undefined,
        rejectReason: r.rejectReason || undefined
      })
    })
  } else {
    console.error('设备借用记录加载失败：', deviceResult.reason)
  }

  if (labResult.status === 'fulfilled') {
    const records = labResult.value.data?.data?.records || []
    records.forEach((r: any) => {
      const statusMap: Record<number, string> = { 1: 'pending', 2: 'approved', 3: 'rejected', 4: 'cancelled', 5: 'approved', 6: 'cancelled' }
      items.push({
        id: r.id,
        applyNo: r.reservationNo || '',
        type: 'lab' as const,
        targetName: r.labName || '',
        targetNo: r.labCode || `LAB-${r.labId || ''}`,
        status: (statusMap[r.reservationStatus] || 'pending') as BorrowApply['status'],
        startTime: r.reservationDate || '',
        endTime: r.startTime && r.endTime ? `${r.startTime} ~ ${r.endTime}` : '',
        createdAt: fmtDt(r.createdAt),
        approvedAt: r.approveTime ? fmtDt(r.approveTime) : undefined,
        peopleCount: r.peopleCount || undefined
      })
    })
  } else {
    console.error('场地预约记录加载失败：', labResult.reason)
  }

  applies.value = items
}

function handleRefreshRequest(): void {
  void loadData()
}

onMounted(() => {
  void loadData()
  window.addEventListener('refresh-reservation-list', handleRefreshRequest)
})

onBeforeUnmount(() => {
  window.removeEventListener('refresh-reservation-list', handleRefreshRequest)
})

const filteredApplies = computed(() => {
  let list = applies.value
  if (activeType.value !== 'all') list = list.filter(a => a.type === activeType.value)
  if (activeStatus.value !== 'all') list = list.filter(a => a.status === activeStatus.value)
  return list
})

function statusLabel(s: string): string {
  const m: Record<string, string> = { pending: '待审核', approved: '审批通过', rejected: '已驳回', cancelled: '已取消' }
  return m[s] || s
}
function typeLabel(t: string): string {
  return t === 'device' ? '设备借用' : '场地预约'
}

async function cancelApply(applyNo: string): Promise<void> {
  const a = applies.value.find(x => x.applyNo === applyNo)
  if (!a || !confirm(`确定要取消对「${a.targetName}」的申请吗？`)) return
  try {
    if (a.type === 'device') {
      await api.post(`/equipment/borrows/${encodeURIComponent(applyNo)}/cancel`)
    } else {
      await api.patch(`/lab-management/reservations/${encodeURIComponent(applyNo)}/cancel`, { reason: '用户主动取消' })
    }
    a.status = 'cancelled'
  } catch (e: any) {
    alert(e?.response?.data?.message || e?.message || '取消失败')
  }
}
function reApply(apply: BorrowApply): void {
  const key = apply.type === 'device' ? 'device-search' : 'lab-search'
  const name = apply.type === 'device' ? '设备查询' : '实验室查询'
  emit('navigate', { key, name })
}
function goDeviceSearch(): void { emit('navigate', { key: 'device-search', name: '设备查询' }) }
function goLabSearch(): void { emit('navigate', { key: 'lab-search', name: '实验室查询' }) }

// ===== 环形图 =====
const statusColors: Record<string, string> = {
  pending: '#1a73e8', approved: '#27ae60', rejected: '#e74c3c', cancelled: '#bbb',
}
const statusLabels: Record<string, string> = {
  pending: '待审核', approved: '审批通过', rejected: '已驳回', cancelled: '已取消',
}
const donutGradient = computed(() => {
  const counts: Record<string, number> = { approved: 0, pending: 0, rejected: 0, cancelled: 0 }
  applies.value.forEach(a => { counts[a.status] = (counts[a.status] || 0) + 1 })
  const total = applies.value.length
  if (total === 0) return 'conic-gradient(#eee 0deg, #eee 360deg)'
  const order = ['approved', 'pending', 'rejected', 'cancelled']
  let acc = 0; const parts: string[] = []
  order.forEach(s => { if (counts[s] === 0) return; const pct = counts[s] / total; const start = acc; acc += pct; parts.push(`${statusColors[s]} ${start * 360}deg ${acc * 360}deg`) })
  return `conic-gradient(${parts.join(', ')})`
})
const legendItems = computed(() => {
  const counts: Record<string, number> = {}
  applies.value.forEach(a => { counts[a.status] = (counts[a.status] || 0) + 1 })
  return ['approved', 'pending', 'rejected', 'cancelled'].filter(s => counts[s] > 0).map(s => ({ label: statusLabels[s], count: counts[s], color: statusColors[s] }))
})

const deviceCount = computed(() => applies.value.filter(a => a.type === 'device').length)
const labCount = computed(() => applies.value.filter(a => a.type === 'lab').length)
const totalDecided = computed(() => applies.value.filter(a => a.status === 'approved' || a.status === 'rejected').length)
const approvalRate = computed(() => { if (totalDecided.value === 0) return 0; return Math.round((applies.value.filter(a => a.status === 'approved').length / totalDecided.value) * 100) })
</script>

<style scoped>
.borrow-page { height: 100%; overflow-y: auto; padding: 28px 36px; }
.page-header { margin-bottom: 20px; }
.page-header h2 { font-size: 22px; font-weight: 700; color: #1a1a2e; margin: 0 0 4px; }
.page-header p { font-size: 14px; color: #888; margin: 0; }

/* 筛选栏 */
.filter-bar { margin-bottom: 20px; }
.filter-row { display: flex; align-items: center; gap: 0; }
.filter-group { display: flex; gap: 4px; background: rgba(255,255,255,.4); border-radius: 10px; padding: 4px; }
.filter-tab { padding: 8px 16px; border-radius: 8px; font-size: 13px; color: #666; cursor: pointer; transition: all .15s; display: flex; align-items: center; gap: 6px; user-select: none; white-space: nowrap; }
.filter-tab:hover { color: #333; background: rgba(0,0,0,.03); }
.filter-tab.active { background: #fff; color: #1a73e8; font-weight: 600; box-shadow: 0 1px 3px rgba(0,0,0,.08); }
.filter-count { font-size: 11px; background: rgba(0,0,0,.06); padding: 1px 7px; border-radius: 10px; color: #888; }
.filter-tab.active .filter-count { background: rgba(26,115,232,.1); color: #1a73e8; }

/* 两栏 */
.borrow-body { display: flex; gap: 24px; align-items: flex-start; }
.apply-list-col { flex: 1; min-width: 0; }
.apply-list { display: flex; flex-direction: column; gap: 14px; }

/* 卡片 */
.apply-card { background: rgba(255,255,255,.5); border-radius: 14px; border: 1px solid rgba(0,0,0,.04); padding: 20px 22px; transition: border-color .2s; }
.apply-card.card-status-pending { border-left: 3px solid #1a73e8; }
.apply-card.card-status-approved { border-left: 3px solid #27ae60; }
.apply-card.card-status-rejected { border-left: 3px solid #e74c3c; }
.apply-card.card-status-cancelled { border-left: 3px solid #ccc; opacity: .7; }

.card-top { display: flex; align-items: center; gap: 10px; margin-bottom: 14px; }
.card-status-icon { width: 32px; height: 32px; border-radius: 50%; display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
.icon-pending { background: rgba(26,115,232,.1); color: #1a73e8; }
.icon-approved { background: rgba(39,174,96,.1); color: #27ae60; }
.icon-rejected { background: rgba(231,76,60,.1); color: #e74c3c; }
.icon-cancelled { background: rgba(0,0,0,.05); color: #bbb; }

.card-status-text { flex: 1; display: flex; align-items: center; gap: 8px; }
.status-label { font-size: 14px; font-weight: 600; color: #1a1a2e; }
.type-badge { font-size: 10px; font-weight: 600; padding: 2px 8px; border-radius: 3px; }
.type-device { background: rgba(26,115,232,.08); color: #1a73e8; }
.type-lab { background: rgba(142,68,173,.08); color: #8e44ad; }
.apply-no { font-size: 11px; color: #bbb; font-family: 'SF Mono', 'Consolas', monospace; }
.card-date { font-size: 12px; color: #aaa; }

/* 设备/实验室信息 */
.card-device { display: flex; align-items: center; gap: 10px; margin-bottom: 12px; padding: 10px 14px; background: rgba(0,0,0,.015); border-radius: 8px; }
.card-device-icon { width: 34px; height: 34px; border-radius: 8px; background: rgba(0,0,0,.04); display: flex; align-items: center; justify-content: center; color: #999; flex-shrink: 0; }
.card-device-icon.lab-icon { background: rgba(142,68,173,.06); color: #8e44ad; }
.card-device-info { display: flex; flex-direction: column; gap: 1px; }
.card-device-name { font-size: 14px; font-weight: 600; color: #1a1a2e; }
.card-device-no { font-size: 11px; color: #bbb; font-family: 'SF Mono', 'Consolas', monospace; }

.card-times { margin-bottom: 12px; }
.time-row { display: flex; align-items: center; gap: 10px; margin-bottom: 4px; }
.time-label { font-size: 12px; color: #aaa; min-width: 56px; }
.time-value { font-size: 13px; color: #555; }

.pickup-hint { display: flex; align-items: flex-start; gap: 6px; margin-top: 6px; padding: 8px 12px; background: rgba(39,174,96,.05); border-radius: 6px; color: #27ae60; font-size: 12px; line-height: 1.5; }
.pickup-hint.lab-hint { background: rgba(142,68,173,.05); color: #8e44ad; }
.pickup-hint svg { flex-shrink: 0; margin-top: 1px; }

.reject-box { padding: 12px 14px; background: rgba(231,76,60,.04); border-radius: 8px; border-left: 3px solid #e74c3c; margin-bottom: 12px; }
.reject-label { font-size: 11px; font-weight: 600; color: #e74c3c; text-transform: uppercase; letter-spacing: .5px; margin-bottom: 4px; }
.reject-text { font-size: 13px; color: #666; margin: 0; line-height: 1.6; }

.card-actions { display: flex; align-items: center; justify-content: flex-end; gap: 10px; padding-top: 12px; border-top: 1px solid rgba(0,0,0,.04); }
.action-btn { padding: 7px 20px; border-radius: 8px; font-size: 13px; cursor: pointer; border: none; transition: all .15s; font-weight: 500; }
.action-btn.danger { background: rgba(231,76,60,.06); color: #e74c3c; }
.action-btn.danger:hover { background: rgba(231,76,60,.12); }
.action-btn.primary { background: rgba(26,115,232,.06); color: #1a73e8; }
.action-btn.primary:hover { background: rgba(26,115,232,.12); }
.action-hint { font-size: 12px; color: #27ae60; }
.action-hint.cancelled-text { color: #bbb; }

.empty-state { display: flex; flex-direction: column; align-items: center; justify-content: center; padding: 80px 0; color: #ccc; }
.empty-state p { font-size: 14px; margin: 12px 0 4px; }
.empty-hint { font-size: 12px; color: #ddd; }

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

.quick-links { margin-top: 14px; padding-top: 12px; border-top: 1px solid rgba(0,0,0,.06); display: flex; flex-direction: column; gap: 4px; }
.quick-link { display: flex; align-items: center; gap: 8px; padding: 8px 12px; border-radius: 8px; cursor: pointer; color: #1a73e8; font-size: 13px; transition: background .15s; }
.quick-link:hover { background: rgba(26,115,232,.05); }
</style>
