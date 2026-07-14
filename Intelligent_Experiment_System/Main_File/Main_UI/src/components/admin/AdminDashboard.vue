<template>
  <div class="dashboard">
    <div class="welcome-bar">
      <div class="welcome-text">
        <h2 class="welcome-greeting">你好，{{ userName }}</h2>
        <p class="welcome-date">{{ todayStr }}</p>
      </div>
      <div class="welcome-stats">
        <div class="stat-item" @click="$emit('navigate', { key: 'person-manage', name: '人员档案' })">
          <span class="stat-num">{{ stats.totalPersons }}</span>
          <span class="stat-label">平台用户</span>
        </div>
        <div class="stat-item">
          <span class="stat-num green">{{ stats.activeDevices }}</span>
          <span class="stat-label">在线设备</span>
        </div>
        <div class="stat-item" @click="$emit('navigate', { key: 'msg-template', name: '消息模板' })">
          <span class="stat-num orange">{{ stats.pendingApprovals }}</span>
          <span class="stat-label">待处理审批</span>
        </div>
        <div class="stat-item highlight" @click="$emit('navigate', { key: 'safety-alarm', name: '报警记录' })">
          <span class="stat-num red">{{ stats.alarms }}</span>
          <span class="stat-label">今日告警</span>
        </div>
      </div>
    </div>

    <!-- 快捷入口 -->
    <div class="quick-actions">
      <div v-for="action in quickActions" :key="action.key" class="action-card" @click="$emit('navigate', { key: action.key, name: action.label })">
        <div class="action-icon" :style="{ background: action.color }" v-html="iconSvg(action.key)"></div>
        <div class="action-info">
          <span class="action-label">{{ action.label }}</span>
          <span class="action-desc">{{ action.desc }}</span>
        </div>
      </div>
    </div>

    <!-- 双栏：系统概览 + 最近操作 -->
    <div class="dashboard-row">
      <!-- 系统概览 -->
      <div class="dash-card overview-card">
        <div class="card-header clickable" @click="$emit('navigate', { key: 'efficiency', name: '效能看板' })"><h3>系统概览</h3></div>
        <div class="overview-grid">
          <div class="ov-item" v-for="item in overviewItems" :key="item.label">
            <span class="ov-label">{{ item.label }}</span>
            <span class="ov-value" :style="{ color: item.color }">{{ item.value }}</span>
          </div>
        </div>
      </div>

      <!-- 待处理事项 -->
      <div class="dash-card pending-card">
        <div class="card-header">
          <h3>待处理事项</h3>
          <span class="card-badge red-bg">{{ pendingItems.length }}</span>
        </div>
        <div class="pending-list" v-if="pendingItems.length > 0">
          <div v-for="item in pendingItems" :key="item.id" class="pending-item clickable" @click="$emit('navigate', { key: item.navKey, name: item.navName })">
            <span class="p-dot" :class="item.level"></span>
            <div class="p-body">
              <span class="p-title">{{ item.title }}</span>
              <span class="p-time">{{ item.time }}</span>
            </div>
          </div>
        </div>
        <div v-else class="empty-hint">暂无待处理事项</div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { userName } from '../../stores/authStore'
import { getDashboardStats, type DashboardStats } from '../../utils/dashboardApi'

defineEmits<{ navigate: [payload: { key: string; name: string }] }>()

const todayStr = computed(() => {
  const d = new Date()
  const weekDays = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']
  return `${d.getFullYear()}年${d.getMonth() + 1}月${d.getDate()}日 ${weekDays[d.getDay()]}`
})

const dashboardData = ref<DashboardStats | null>(null)

async function loadStats() {
  try {
    dashboardData.value = await getDashboardStats()
  } catch { /* API 不可用 */ }
}
onMounted(() => { loadStats() })

const stats = computed(() => ({
  totalPersons: dashboardData.value?.totalPersons ?? 0,
  activeDevices: dashboardData.value?.activeDevices ?? 0,
  pendingApprovals: dashboardData.value?.pendingApprovals ?? 0,
  alarms: dashboardData.value?.alarms ?? 0,
}))

const quickActions = [
  { key: 'person-manage', label: '人员档案', desc: '管理学生/教师/管理员', color: 'linear-gradient(135deg,#4A90D9,#357ABD)' },
  { key: 'device-manage', label: '设备台账', desc: '设备信息录入与管理', color: 'linear-gradient(135deg,#27AE60,#2ECC71)' },
  { key: 'lab-manage', label: '场地管理', desc: '实验室信息与预约', color: 'linear-gradient(135deg,#E67E22,#F39C12)' },
  { key: 'safety-check', label: '安全检查', desc: '创建检查任务与记录', color: 'linear-gradient(135deg,#E74C3C,#EC7063)' },
  { key: 'consumable-stock', label: '耗材库存', desc: '耗材台账与入库管理', color: 'linear-gradient(135deg,#8E44AD,#9B59B6)' },
  { key: 'iot-monitor', label: '物联监测', desc: '传感器数据实时查看', color: 'linear-gradient(135deg,#1ABC9C,#16A085)' },
  { key: 'efficiency', label: '效能看板', desc: '使用率与统计分析', color: 'linear-gradient(135deg,#2C3E50,#34495E)' },
  { key: 'msg-publish', label: '消息发布', desc: '向用户发送系统通知', color: 'linear-gradient(135deg,#3498DB,#2980B9)' },
]

function iconSvg(key: string): string {
  const icons: Record<string, string> = {
    'person-manage': `<svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="#fff" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><circle cx="9" cy="7" r="4"/><path d="M3 21v-2a4 4 0 0 1 4-4h4a4 4 0 0 1 4 4v2"/><circle cx="18" cy="8" r="3"/><path d="M20 21v-1a3 3 0 0 0-2.25-2.9"/></svg>`,
    'device-manage': `<svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="#fff" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="5" width="4" height="14" rx="1"/><rect x="10" y="7" width="4" height="12" rx="1"/><rect x="17" y="3" width="4" height="16" rx="1"/><circle cx="12" cy="14" r="7"/><path d="M9 14h6"/><path d="M12 11v6"/></svg>`,
    'lab-manage': `<svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="#fff" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M3 21h18"/><path d="M5 21V7l8-4v18"/><path d="M19 21V11l-6-4"/><path d="M9 9v.01"/><path d="M9 12v.01"/><path d="M9 15v.01"/><path d="M9 18v.01"/></svg>`,
    'safety-check': `<svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="#fff" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M12 2l7 4v5c0 5-3.2 8.7-7 10-3.8-1.3-7-5-7-10V6l7-4Z"/><path d="M9 12l2 2 4-4"/></svg>`,
    'consumable-stock': `<svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="#fff" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M9 3h6v4H9z"/><path d="M9 7v2.5c0 2.5-2 4.5-2 7.5 0 2 1.5 4 3 4h4c1.5 0 3-2 3-4 0-3-2-5-2-7.5V7"/><path d="M9 12h6"/><path d="M10 16h4"/></svg>`,
    'iot-monitor': `<svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="#fff" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="18" r="3"/><path d="M12 15v-4"/><path d="M8 10a4 4 0 0 1 8 0"/><path d="M6 6a8 8 0 0 1 12 0"/></svg>`,
    'efficiency': `<svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="#fff" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="13" width="4" height="8" rx="1"/><rect x="10" y="8" width="4" height="13" rx="1"/><rect x="17" y="3" width="4" height="18" rx="1"/></svg>`,
    'msg-publish': `<svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="#fff" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M5 8h10l4 4v8a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V10a2 2 0 0 1 2-2Z"/><path d="M7 8V6a5 5 0 0 1 10 0v2"/><path d="M3 14h18"/></svg>`,
  }
  return icons[key] ?? ''
}

const overviewItems = computed(() => {
  const d = dashboardData.value
  return [
    { label: '学生总数', value: `${d?.studentCount ?? 0} 人`, color: '#27AE60' },
    { label: '教师总数', value: `${d?.teacherCount ?? 0} 人`, color: '#4A90D9' },
    { label: '管理员', value: `${d?.adminCount ?? 0} 人`, color: '#E74C3C' },
    { label: '部门数', value: `${d?.departmentCount ?? 0} 个`, color: '#8E44AD' },
    { label: '实验室', value: `${d?.labCount ?? 0} 间`, color: '#E67E22' },
    { label: '设备总数', value: `${d?.deviceCount ?? 0} 台`, color: '#1ABC9C' },
    { label: '本月借用', value: `${d?.monthBorrowCount ?? 0} 次`, color: '#3498DB' },
    { label: '本月预约', value: `${d?.monthReservationCount ?? 0} 次`, color: '#F39C12' },
  ]
})

const pendingItems = computed(() => {
  return (dashboardData.value?.pendingItems ?? []).map((item, idx) => ({
    id: idx + 1,
    title: item.title,
    time: formatRelativeTime(item.time),
    level: item.level,
    navKey: item.navKey,
    navName: item.navName,
  }))
})

function formatRelativeTime(timeStr: string): string {
  if (!timeStr) return ''
  try {
    const d = new Date(timeStr)
    const now = new Date()
    const diffMs = now.getTime() - d.getTime()
    const diffMin = Math.floor(diffMs / 60000)
    if (diffMin < 1) return '刚刚'
    if (diffMin < 60) return `${diffMin}分钟前`
    const diffHour = Math.floor(diffMin / 60)
    if (diffHour < 24) return `${diffHour}小时前`
    const diffDay = Math.floor(diffHour / 24)
    if (diffDay === 1) return '昨天'
    if (diffDay < 7) return `${diffDay}天前`
    return timeStr.slice(0, 10)
  } catch { return timeStr.slice(0, 10) }
}
</script>

<style scoped>
.dashboard { height: 100%; overflow-y: auto; padding: 28px 36px; }

/* ===== 欢迎区 ===== */
.welcome-bar { display: flex; align-items: center; justify-content: space-between; margin-bottom: 20px; }
.welcome-text { }
.welcome-greeting { font-size: 22px; font-weight: 700; color: #1a1a2e; margin: 0 0 2px; }
.welcome-date { font-size: 13px; color: #999; margin: 0; }

.welcome-stats { display: flex; gap: 12px; }
.stat-item { background: rgba(255,255,255,.5); border-radius: 12px; padding: 12px 18px; text-align: center; cursor: pointer; border: 1px solid rgba(0,0,0,.04); transition: all .15s; min-width: 76px; }
.stat-item:hover { background: rgba(255,255,255,.75); transform: translateY(-2px); box-shadow: 0 4px 12px rgba(0,0,0,.06); }
.stat-num { font-size: 24px; font-weight: 700; color: #1a1a2e; display: block; line-height: 1.2; }
.stat-num.green { color: #27ae60; }
.stat-num.orange { color: #e67e22; }
.stat-num.red { color: #e74c3c; }
.stat-label { font-size: 11px; color: #999; }

/* ===== 快捷操作 ===== */
.quick-actions { display: grid; grid-template-columns: repeat(4, 1fr); gap: 10px; margin-bottom: 20px; }
.action-card { display: flex; align-items: center; gap: 10px; padding: 13px 16px; background: rgba(255,255,255,.5); border-radius: 12px; border: 1px solid rgba(0,0,0,.04); cursor: pointer; transition: all .15s; }
.action-card:hover { background: rgba(255,255,255,.75); transform: translateY(-2px); box-shadow: 0 4px 12px rgba(0,0,0,.06); }
.action-icon { width: 42px; height: 42px; border-radius: 10px; display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
.action-icon :deep(svg) { display: block; }
.action-info { display: flex; flex-direction: column; gap: 2px; }
.action-label { font-size: 13px; font-weight: 600; color: #333; }
.action-desc { font-size: 11px; color: #aaa; }

/* ===== 双栏 ===== */
.dashboard-row { display: flex; gap: 16px; }
.dash-card { background: rgba(255,255,255,.5); border-radius: 14px; border: 1px solid rgba(0,0,0,.04); padding: 18px 20px; }
.overview-card { flex: 1; }
.pending-card { flex: 1; }

.card-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 14px; }
.card-header.clickable { cursor: pointer; user-select: none; }
.card-header.clickable:hover h3 { color: #1a73e8; }
.card-header h3 { font-size: 14px; font-weight: 600; color: #1a1a2e; margin: 0; }
.card-badge { font-size: 12px; font-weight: 600; padding: 2px 10px; border-radius: 10px; background: rgba(0,0,0,.05); color: #888; }
.card-badge.red-bg { background: rgba(231,76,60,.12); color: #e74c3c; }

/* 概览网格 */
.overview-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 10px; }
.ov-item { display: flex; flex-direction: column; align-items: center; padding: 12px 8px; background: rgba(0,0,0,.015); border-radius: 8px; }
.ov-label { font-size: 11px; color: #999; margin-bottom: 4px; }
.ov-value { font-size: 16px; font-weight: 700; }

/* 待处理 */
.pending-list { display: flex; flex-direction: column; gap: 8px; }
.pending-item { display: flex; align-items: center; gap: 10px; padding: 9px 10px; background: rgba(0,0,0,.015); border-radius: 8px; }
.pending-item.clickable { cursor: pointer; transition: background .12s; }
.pending-item.clickable:hover { background: rgba(26,115,232,.04); }
.p-dot { width: 8px; height: 8px; border-radius: 50%; flex-shrink: 0; }
.p-dot.warn { background: #e67e22; }
.p-dot.alarm { background: #e74c3c; }
.p-dot.info { background: #3498db; }
.p-body { flex: 1; display: flex; flex-direction: column; gap: 2px; }
.p-title { font-size: 13px; color: #555; }
.p-time { font-size: 11px; color: #bbb; }

.empty-hint { text-align: center; padding: 30px 0; font-size: 13px; color: #ccc; }
</style>
