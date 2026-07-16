<template>
  <div class="da-page">
    <div class="page-header">
      <h2>借用审批</h2>
      <p>审核设备借用申请 · 领用登记 · 归还登记</p>
    </div>

    <!-- 统计卡片 -->
    <div class="stats-row">
      <div class="stat-card">
        <div class="stat-num highlight">{{ pendingCount }}</div>
        <div class="stat-label">待审批</div>
      </div>
      <div class="stat-card">
        <div class="stat-num green">{{ checkedOutCount }}</div>
        <div class="stat-label">已领用</div>
      </div>
      <div class="stat-card">
        <div class="stat-num">{{ returnedCount }}</div>
        <div class="stat-label">已归还</div>
      </div>
      <div class="stat-card">
        <div class="stat-num red">{{ overdueCount }}</div>
        <div class="stat-label">逾期未还</div>
      </div>
    </div>

    <!-- 顶部导航 Tab -->
    <div class="section-tabs">
      <div
        class="section-tab"
        :class="{ active: activeSection === 'apply' }"
        @click="switchSection('apply')"
      >借用申请（{{ applyCount }}）</div>
      <div
        class="section-tab"
        :class="{ active: activeSection === 'log' }"
        @click="switchSection('log')"
      >借用记录（{{ logCount }}）</div>
    </div>

    <!-- ==================== 借用申请列表 ==================== -->
    <template v-if="activeSection === 'apply'">
      <div class="action-bar">
        <div class="filter-tabs">
          <div
            v-for="t in applyStatusTabs"
            :key="t.key"
            class="filter-tab"
            :class="{ active: activeApplyStatus === t.key }"
            @click="activeApplyStatus = t.key; pageNum = 1"
          >
            {{ t.label }}
            <span v-if="t.count !== undefined" class="tab-count">{{ t.count }}</span>
          </div>
        </div>
        <input v-model="searchNo" placeholder="搜索申请单号..." class="search-input" @keyup.enter="pageNum = 1" />
      </div>

      <div class="card-list" v-if="pagedApplies.length > 0 && !loadError">
        <div
          v-for="apply in pagedApplies"
          :key="apply.id"
          class="apply-card"
          :class="'card-' + apply.status"
        >
          <div class="card-left">
            <div class="card-header">
              <span class="status-badge" :class="'badge-' + apply.status">{{ apply.statusName }}</span>
              <span class="apply-no">{{ apply.applyNo }}</span>
              <span class="apply-time">{{ apply.createdAt }}</span>
            </div>
            <div class="card-body">
              <div class="card-info-row">
                <span class="info-label">设备</span>
                <span class="info-value device-link" @click="viewDevice(apply.deviceId)">{{ apply.deviceName }}（{{ apply.deviceNo }}）</span>
              </div>
              <div class="card-info-row">
                <span class="info-label">申请人</span>
                <span class="info-value">
                  {{ apply.applicantName }}
                  <span class="applicant-type" :class="apply.applicantType === 1 ? 'type-stu' : 'type-tea'">{{ apply.applicantTypeName }}</span>
                </span>
              </div>
              <div class="card-info-row">
                <span class="info-label">使用时段</span>
                <span class="info-value">{{ apply.expectedStartTime }} ~ {{ apply.expectedReturnTime }}</span>
              </div>
              <div v-if="apply.purpose" class="card-info-row">
                <span class="info-label">用途</span>
                <span class="info-value purpose-text">{{ apply.purpose }}</span>
              </div>
              <div v-if="apply.status === 'rejected' && apply.rejectReason" class="reject-reason">
                <span class="reason-label">驳回原因：</span>{{ apply.rejectReason }}
              </div>
              <div v-if="apply.status === 'approved' && apply.pickupHint" class="pickup-hint">
                <span class="hint-label">领用提示：</span>{{ apply.pickupHint }}
              </div>
            </div>
          </div>
          <div class="card-right">
            <template v-if="apply.status === 'pending'">
              <button class="btn-reject" @click="openReject(apply)">驳回</button>
              <button class="btn-approve" @click="openApprove(apply)">✓ 审批通过</button>
            </template>
            <template v-else-if="apply.status === 'approved'">
              <button class="btn-checkout" @click="doCheckOut(apply)">领用登记</button>
            </template>
            <span v-else class="card-final-text">
              {{ apply.status === 'rejected' ? '已驳回' : '已取消' }}
            </span>
          </div>
        </div>
      </div>

      <div v-else-if="loadError" class="empty-state" style="gap:12px">
        <svg viewBox="0 0 24 24" width="48" height="48" fill="none" stroke="#e74c3c" stroke-width="1.2"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>
        <p style="color:#e74c3c;margin:0">数据加载失败</p>
        <span style="font-size:12px;color:#999">请检查后端服务是否正常</span>
        <button class="retry-btn" @click="loadApplies();loadLogs()" style="padding:8px 20px;border:1px solid #1a73e8;border-radius:8px;background:#fff;color:#1a73e8;font-size:13px;cursor:pointer">重新加载</button>
      </div>

      <div v-else class="empty-state">
        <svg viewBox="0 0 24 24" width="48" height="48" fill="none" stroke="#ccc" stroke-width="1.2"><path d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2"/></svg>
        <p>暂无{{ activeApplyStatus !== 'all' ? applyStatusLabel(activeApplyStatus) + '的' : '' }}借用申请</p>
      </div>

      <div class="pagination" v-if="filteredApplies.length > pageSize">
        <button :disabled="pageNum <= 1" @click="pageNum--">上一页</button>
        <span>{{ pageNum }} / {{ applyTotalPages }}</span>
        <button :disabled="pageNum >= applyTotalPages" @click="pageNum++">下一页</button>
      </div>
    </template>

    <!-- ==================== 借用记录 ==================== -->
    <template v-if="activeSection === 'log'">
      <div class="action-bar">
        <div class="filter-tabs">
          <div
            v-for="t in logStatusTabs"
            :key="t.key"
            class="filter-tab"
            :class="{ active: activeLogStatus === t.key }"
            @click="activeLogStatus = t.key; logPageNum = 1"
          >
            {{ t.label }}
            <span v-if="t.count !== undefined" class="tab-count">{{ t.count }}</span>
          </div>
        </div>
        <div class="toolbar-right">
          <button class="refresh-btn" @click="refreshLogs" :class="{ spinning: logRefreshing }">
            <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2"><polyline points="23 4 23 10 17 10"/><path d="M20.49 15a9 9 0 1 1-2.12-9.36L23 10"/></svg>
            刷新
          </button>
          <button class="btn-overdue" @click="doDetectOverdue">
            ⏰ 逾期检测
          </button>
        </div>
      </div>

      <div class="data-table-wrap" v-if="pagedLogs.length > 0">
        <table class="data-table">
          <thead>
            <tr>
              <th>设备</th>
              <th>借用人</th>
              <th>预计归还</th>
              <th>实际领出</th>
              <th>实际归还</th>
              <th>状态</th>
              <th>备注</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="log in pagedLogs" :key="log.id">
              <td><span class="device-name">{{ log.deviceName }}</span></td>
              <td>{{ log.borrowerName || '用户' + log.borrowerId }}</td>
              <td>{{ formatTime(log.expectedReturnTime) }}</td>
              <td>{{ formatTime(log.actualOutTime) }}</td>
              <td>{{ formatTime(log.actualReturnTime) }}</td>
              <td>
                <span class="log-status-tag" :class="'ls-' + log.status">{{ log.statusDesc }}</span>
                <span v-if="log.isOverdue === 1" class="overdue-tag">逾期</span>
              </td>
              <td class="remark-cell">{{ log.returnRemark || '--' }}</td>
              <td class="action-cell">
                <button
                  v-if="log.status === 1 || log.status === 3"
                  class="row-btn checkin-btn"
                  @click="openCheckIn(log)"
                >归还登记</button>
                <span v-else class="done-text">已归还</span>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <div v-else class="empty-state">
        <svg viewBox="0 0 24 24" width="48" height="48" fill="none" stroke="#ccc" stroke-width="1.2"><path d="M12 8v4l3 3M12 22a10 10 0 100-20 10 10 0 000 20z"/></svg>
        <p style="margin:0">暂无借用记录</p>
        <span style="font-size:12px;color:#bbb">借用记录在"审批通过 → 领用登记"后自动产生</span>
      </div>

      <div class="pagination" v-if="filteredLogs.length > logPageSize">
        <button :disabled="logPageNum <= 1" @click="logPageNum--">上一页</button>
        <span>{{ logPageNum }} / {{ logTotalPages }}</span>
        <button :disabled="logPageNum >= logTotalPages" @click="logPageNum++">下一页</button>
      </div>
    </template>

    <!-- 审批通过弹窗 -->
    <teleport to="body">
      <transition name="modal-fade">
        <div v-if="showApprove" class="modal-overlay" @click.self="showApprove = false">
          <div class="modal-panel">
            <div class="modal-header">
              <h3>确认审批通过</h3>
              <button class="modal-close" @click="showApprove = false"><svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2"><path d="M18 6L6 18M6 6l12 12"/></svg></button>
            </div>
            <div class="modal-body">
              <div class="confirm-info">
                <div class="ci-row"><span>设备</span><span>{{ currentApply?.deviceName }}</span></div>
                <div class="ci-row"><span>申请人</span><span>{{ currentApply?.applicantName }}</span></div>
                <div class="ci-row"><span>使用时段</span><span>{{ currentApply?.expectedStartTime }} ~ {{ currentApply?.expectedReturnTime }}</span></div>
              </div>
              <div class="form-g">
                <label>领用提示（可选）</label>
                <textarea v-model="approveForm.pickupHint" class="form-input form-textarea" rows="2" placeholder="如：请到 A201 实验室联系管理员领用设备" maxlength="200" />
              </div>
            </div>
            <div class="modal-footer">
              <button class="modal-btn cancel" @click="showApprove = false">取消</button>
              <button class="modal-btn primary" @click="doApprove">确认通过</button>
            </div>
          </div>
        </div>
      </transition>
    </teleport>

    <!-- 驳回弹窗 -->
    <teleport to="body">
      <transition name="modal-fade">
        <div v-if="showReject" class="modal-overlay" @click.self="showReject = false">
          <div class="modal-panel">
            <div class="modal-header">
              <h3>驳回申请</h3>
              <button class="modal-close" @click="showReject = false"><svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2"><path d="M18 6L6 18M6 6l12 12"/></svg></button>
            </div>
            <div class="modal-body">
              <div class="form-g">
                <label>驳回原因 <span class="required">*</span></label>
                <textarea v-model="rejectForm.reason" class="form-input form-textarea" rows="3" placeholder="请填写驳回原因，系统将通知申请人..." maxlength="300" />
              </div>
              <div class="quick-reasons">
                <span class="qr-label">快捷填写：</span>
                <button v-for="r in quickReasons" :key="r" class="qr-btn" @click="rejectForm.reason = r">{{ r }}</button>
              </div>
            </div>
            <div class="modal-footer">
              <button class="modal-btn cancel" @click="showReject = false">取消</button>
              <button class="modal-btn danger" @click="doReject" :disabled="!rejectForm.reason.trim()">确认驳回</button>
            </div>
          </div>
        </div>
      </transition>
    </teleport>

    <!-- 归还登记弹窗 -->
    <teleport to="body">
      <transition name="modal-fade">
        <div v-if="showCheckIn" class="modal-overlay" @click.self="showCheckIn = false">
          <div class="modal-panel">
            <div class="modal-header">
              <h3>归还登记</h3>
              <button class="modal-close" @click="showCheckIn = false"><svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2"><path d="M18 6L6 18M6 6l12 12"/></svg></button>
            </div>
            <div class="modal-body">
              <div class="confirm-info">
                <div class="ci-row"><span>设备</span><span>{{ currentLog?.deviceName }}</span></div>
                <div class="ci-row"><span>预计归还</span><span>{{ formatTime(currentLog?.expectedReturnTime) }}</span></div>
                <div class="ci-row"><span>实际领出</span><span>{{ formatTime(currentLog?.actualOutTime) }}</span></div>
              </div>
              <div class="form-g">
                <label>归还备注</label>
                <textarea v-model="checkInForm.remark" class="form-input form-textarea" rows="2" placeholder="如：设备外观完好，功能正常" maxlength="200" />
              </div>
            </div>
            <div class="modal-footer">
              <button class="modal-btn cancel" @click="showCheckIn = false">取消</button>
              <button class="modal-btn primary green" @click="doCheckIn">确认归还</button>
            </div>
          </div>
        </div>
      </transition>
    </teleport>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, reactive, onMounted } from 'vue'
import {
  getBorrowApplies,
  approveBorrowApply,
  checkOutDevice,
  checkInDevice,
  getBorrowLogs,
  detectOverdue,
  type BorrowApplyItem,
  type BorrowLogItem,
  type BorrowStatus,
} from '../../utils/deviceApi'

type ApplyStatus = BorrowStatus | 'all'
type LogFilterStatus = 'all' | 1 | 2 | 3

// ==================== 数据 ====================
const applies = ref<BorrowApplyItem[]>([])
const logs = ref<BorrowLogItem[]>([])
const loadError = ref(false)
const logRefreshing = ref(false)

async function loadApplies() {
  try {
    const res = await getBorrowApplies({ pageNum: 1, pageSize: 100 })
    applies.value = (res.records || []).map((r: Record<string, any>) => ({
      ...r,
      id: r.id,
      applyNo: r.applyNo || '',
      deviceId: r.deviceId,
      deviceName: r.deviceName || '',
      deviceNo: r.deviceNo || '',
      applicantId: r.applicantId,
      applicantName: r.applicantName || '',
      applicantType: r.applicantType ?? 1,
      applicantTypeName: r.applicantType === 2 ? '教师' : '学生',
      expectedStartTime: formatLocalDt(r.expectedStartTime),
      expectedReturnTime: formatLocalDt(r.expectedReturnTime),
      purpose: r.purpose || '',
      status: mapStatus(r.applyStatus),
      statusName: r.applyStatusDesc || mapStatusName(mapStatus(r.applyStatus)),
      rejectReason: r.rejectReason || '',
      createdAt: formatLocalDt(r.createdAt),
    }))
  } catch (e) {
    console.error('加载借用申请失败:', e)
    loadError.value = true
  }
}

async function loadLogs() {
  try {
    const res = await getBorrowLogs({ pageNum: 1, pageSize: 200 })
    logs.value = (res.records || []).map(r => ({
      ...r,
      expectedReturnTime: r.expectedReturnTime ? formatLocalDt(r.expectedReturnTime) : null,
      actualOutTime: r.actualOutTime ? formatLocalDt(r.actualOutTime) : null,
      actualReturnTime: r.actualReturnTime ? formatLocalDt(r.actualReturnTime) : null,
      createdAt: formatLocalDt(r.createdAt),
      updatedAt: formatLocalDt(r.updatedAt),
    }))
  } catch (e) {
    console.error('加载借用记录失败:', e)
  }
}

onMounted(() => {
  loadApplies()
  loadLogs()
})

// ==================== Tab 切换 ====================
const activeSection = ref<'apply' | 'log'>('apply')
function switchSection(s: 'apply' | 'log') { activeSection.value = s }

const applyCount = computed(() => applies.value.length)
const logCount = computed(() => logs.value.length)

// ==================== 统计 ====================
const pendingCount = computed(() => applies.value.filter(a => a.status === 'pending').length)
const checkedOutCount = computed(() => logs.value.filter(l => l.status === 1).length)
const returnedCount = computed(() => logs.value.filter(l => l.status === 2).length)
const overdueCount = computed(() => logs.value.filter(l => l.status === 3).length)

// ==================== 申请筛选 ====================
const activeApplyStatus = ref<ApplyStatus>('all')
const searchNo = ref('')
const pageNum = ref(1)
const pageSize = 6

const applyStatusTabs = computed(() => [
  { key: 'all' as const, label: '全部', count: applies.value.length },
  { key: 'pending' as const, label: '待审核', count: applies.value.filter(a => a.status === 'pending').length },
  { key: 'approved' as const, label: '已通过', count: applies.value.filter(a => a.status === 'approved').length },
  { key: 'rejected' as const, label: '已驳回', count: applies.value.filter(a => a.status === 'rejected').length },
  { key: 'cancelled' as const, label: '已取消', count: applies.value.filter(a => a.status === 'cancelled').length },
])

const filteredApplies = computed(() => {
  let list = applies.value
  if (activeApplyStatus.value !== 'all') list = list.filter(a => a.status === activeApplyStatus.value)
  if (searchNo.value) list = list.filter(a => a.applyNo.toLowerCase().includes(searchNo.value.toLowerCase()))
  return list
})
const applyTotalPages = computed(() => Math.ceil(filteredApplies.value.length / pageSize) || 1)
const pagedApplies = computed(() => {
  const s = (pageNum.value - 1) * pageSize
  return filteredApplies.value.slice(s, s + pageSize)
})

function applyStatusLabel(s: string): string {
  const m: Record<string, string> = { pending: '待审核', approved: '已通过', rejected: '已驳回', cancelled: '已取消' }
  return m[s] || s
}

function mapStatus(s: number | string): BorrowStatus {
  if (typeof s === 'number') {
    return ({ 1: 'pending', 2: 'approved', 3: 'rejected', 4: 'cancelled' } as Record<number, BorrowStatus>)[s] || 'pending'
  }
  return (['pending', 'approved', 'rejected', 'cancelled'].includes(s) ? s : 'pending') as BorrowStatus
}
function mapStatusName(s: BorrowStatus): string {
  const m: Record<BorrowStatus, string> = { pending: '待审核', approved: '已通过', rejected: '已驳回', cancelled: '已取消' }
  return m[s] || '待审核'
}

// ==================== 借用记录筛选 ====================
const activeLogStatus = ref<LogFilterStatus>('all')
const logPageNum = ref(1)
const logPageSize = 10

const logStatusTabs = computed(() => [
  { key: 'all' as const, label: '全部', count: logs.value.length },
  { key: 1 as const, label: '借出中', count: logs.value.filter(l => l.status === 1).length },
  { key: 2 as const, label: '已归还', count: logs.value.filter(l => l.status === 2).length },
  { key: 3 as const, label: '逾期未还', count: logs.value.filter(l => l.status === 3).length },
])

const filteredLogs = computed(() => {
  if (activeLogStatus.value === 'all') return logs.value
  return logs.value.filter(l => l.status === activeLogStatus.value)
})
const logTotalPages = computed(() => Math.ceil(filteredLogs.value.length / logPageSize) || 1)
const pagedLogs = computed(() => {
  const s = (logPageNum.value - 1) * logPageSize
  return filteredLogs.value.slice(s, s + logPageSize)
})

function formatTime(t: string | null | undefined): string {
  if (!t) return '--'
  return t.replace('T', ' ').slice(0, 16)
}

function refreshLogs() {
  logRefreshing.value = true
  loadLogs().finally(() => { logRefreshing.value = false })
}

// ==================== 审批操作 ====================
const showApprove = ref(false)
const showReject = ref(false)
const currentApply = ref<BorrowApplyItem | null>(null)
const approveForm = reactive({ pickupHint: '' })
const rejectForm = reactive({ reason: '' })
const quickReasons = ['设备正在维修中', '该时段已被预约', '申请理由不充分', '设备暂不可借用', '其他原因请手动填写']

function openApprove(apply: BorrowApplyItem) {
  currentApply.value = apply
  approveForm.pickupHint = ''
  showApprove.value = true
}
function openReject(apply: BorrowApplyItem) {
  currentApply.value = apply
  rejectForm.reason = ''
  showReject.value = true
}

async function doApprove() {
  if (!currentApply.value) return
  try {
    // 后端 BorrowApproveDTO 仅有 approved/rejectReason 字段，无 pickupHint
    // 领用提示暂存前端 localStorage 以备展示
    if (approveForm.pickupHint.trim()) {
      localStorage.setItem(`pickup_hint_${currentApply.value.applyNo}`, approveForm.pickupHint.trim())
    }
    await approveBorrowApply({
      applyNo: currentApply.value.applyNo,
      action: 'approve',
    })
    showApprove.value = false
    activeApplyStatus.value = 'approved'
    await loadApplies()
    await loadLogs()
  } catch (e: any) {
    alert(e?.response?.data?.message || e?.message || '审批失败')
  }
}

async function doReject() {
  if (!currentApply.value || !rejectForm.reason.trim()) return
  try {
    await approveBorrowApply({
      applyNo: currentApply.value.applyNo,
      action: 'reject',
      rejectReason: rejectForm.reason.trim(),
    })
    showReject.value = false
    activeApplyStatus.value = 'rejected'
    await loadApplies()
  } catch (e: any) {
    alert(e?.response?.data?.message || e?.message || '驳回失败')
  }
}

// ==================== 领用登记 ====================
async function doCheckOut(apply: BorrowApplyItem) {
  if (!confirm(`确认将「${apply.deviceName}」领出给申请人「${apply.applicantName}」吗？`)) return
  try {
    await checkOutDevice(apply.applyNo)
    alert(`领用登记成功！设备「${apply.deviceName}」已标记为借用中。`)
    await loadApplies()
    await loadLogs()
  } catch (e: any) {
    alert(e?.response?.data?.message || e?.message || '领用登记失败')
  }
}

// ==================== 归还登记 ====================
const showCheckIn = ref(false)
const currentLog = ref<BorrowLogItem | null>(null)
const checkInForm = reactive({ remark: '' })

function openCheckIn(log: BorrowLogItem) {
  currentLog.value = log
  checkInForm.remark = ''
  showCheckIn.value = true
}

async function doCheckIn() {
  if (!currentLog.value) return
  try {
    await checkInDevice(currentLog.value.id, checkInForm.remark.trim() || undefined)
    alert('归还登记成功！')
    showCheckIn.value = false
    await loadLogs()
    await loadApplies()
  } catch (e: any) {
    alert(e?.response?.data?.message || e?.message || '归还登记失败')
  }
}

// ==================== 逾期检测 ====================
async function doDetectOverdue() {
  const activeLogs = logs.value.filter(l => l.status === 1)
  if (activeLogs.length === 0) {
    alert('当前没有正在借出中的设备记录，无需检测逾期。\n请先审批通过→领用登记，产生借用记录后再执行此操作。')
    return
  }
  if (!confirm(`将扫描 ${activeLogs.length} 条借出中记录，标记超期未还的设备，确认执行？`)) return
  try {
    await detectOverdue()
    alert(`逾期检测完成！共检查 ${activeLogs.length} 条借出记录。`)
    await loadLogs()
  } catch (e: any) {
    alert(e?.response?.data?.message || e?.message || '逾期检测失败')
  }
}

// ==================== 工具 ====================
function formatLocalDt(dt: string | undefined | null): string {
  if (!dt) return ''
  return dt.replace('T', ' ').slice(0, 16)
}

function viewDevice(deviceId: number) {
  alert('查看设备详情：deviceId=' + deviceId + '（可在设备台账页面查看）')
}
</script>

<style scoped>
.da-page { height:100%; overflow-y:auto; padding:20px 24px }
.page-header { margin-bottom:16px }
.page-header h2 { font-size:20px; font-weight:600; color:#1a1a2e; margin:0 }
.page-header p { margin:4px 0 0; font-size:13px; color:#999 }

.stats-row { display:flex; gap:12px; margin-bottom:16px; flex-wrap:wrap }
.stat-card { background:rgba(255,255,255,.65); backdrop-filter:blur(10px); border:1px solid rgba(0,0,0,.06); border-radius:12px; padding:14px 20px; min-width:100px; text-align:center }
.stat-num { font-size:24px; font-weight:700; color:#1a1a2e; line-height:1.1 }
.stat-label { font-size:12px; color:#999; margin-top:2px }
.highlight { color:#1a73e8 }
.green { color:#27ae60 }
.red { color:#e74c3c }

/* 顶部 Tab */
.section-tabs { display:flex; gap:0; margin-bottom:16px; background:rgba(255,255,255,.4); border-radius:10px; padding:4px }
.section-tab { padding:9px 24px; border-radius:8px; font-size:14px; font-weight:500; color:#666; cursor:pointer; transition:all .15s; user-select:none }
.section-tab:hover { color:#333; background:rgba(0,0,0,.03) }
.section-tab.active { background:#fff; color:#1a73e8; font-weight:600; box-shadow:0 1px 3px rgba(0,0,0,.08) }

/* 操作栏 */
.action-bar { display:flex; align-items:center; justify-content:space-between; margin-bottom:18px; gap:12px; flex-wrap:wrap }
.filter-tabs { display:flex; gap:4px; background:rgba(255,255,255,.4); border-radius:10px; padding:4px }
.filter-tab { padding:8px 16px; border-radius:8px; font-size:13px; color:#666; cursor:pointer; transition:all .15s; display:flex; align-items:center; gap:6px; user-select:none; white-space:nowrap }
.filter-tab:hover { color:#333; background:rgba(0,0,0,.03) }
.filter-tab.active { background:#fff; color:#1a73e8; font-weight:600; box-shadow:0 1px 3px rgba(0,0,0,.08) }
.tab-count { font-size:11px; background:rgba(0,0,0,.06); padding:1px 7px; border-radius:10px; color:#888 }
.filter-tab.active .tab-count { background:rgba(26,115,232,.1); color:#1a73e8 }
.search-input { padding:8px 12px; border:1px solid rgba(0,0,0,.1); border-radius:10px; font-size:13px; outline:none; width:180px; background:rgba(255,255,255,.7) }
.toolbar-right { display:flex; align-items:center; gap:10px }
.refresh-btn { display:flex; align-items:center; gap:5px; padding:7px 14px; border:1px solid rgba(0,0,0,.1); border-radius:8px; background:#fff; font-size:12px; color:#555; cursor:pointer; transition:.2s }
.refresh-btn:hover { border-color:#1a73e8; color:#1a73e8 }
.refresh-btn.spinning svg { animation:spin .6s linear }
@keyframes spin { to { transform:rotate(360deg) } }

/* 申请卡片列表 */
.card-list { display:flex; flex-direction:column; gap:12px }
.apply-card { background:rgba(255,255,255,.55); backdrop-filter:blur(10px); border-radius:12px; border:1px solid rgba(0,0,0,.04); display:flex; align-items:stretch; overflow:hidden; transition:border-color .2s }
.apply-card.card-pending { border-left:3px solid #1a73e8 }
.apply-card.card-approved { border-left:3px solid #27ae60 }
.apply-card.card-rejected { border-left:3px solid #e74c3c }
.apply-card.card-cancelled { border-left:3px solid #ccc; opacity:.65 }

.card-left { flex:1; padding:16px 20px; min-width:0 }
.card-right { display:flex; flex-direction:column; align-items:flex-end; justify-content:center; gap:8px; padding:16px 20px; flex-shrink:0; min-width:140px; border-left:1px solid rgba(0,0,0,.04) }

.card-header { display:flex; align-items:center; gap:10px; margin-bottom:12px }
.status-badge { padding:2px 8px; border-radius:4px; font-size:11px; font-weight:600 }
.badge-pending { background:rgba(26,115,232,.1); color:#1a73e8 }
.badge-approved { background:rgba(39,174,96,.1); color:#27ae60 }
.badge-rejected { background:rgba(231,76,60,.1); color:#e74c3c }
.badge-cancelled { background:rgba(0,0,0,.05); color:#999 }
.apply-no { font-size:13px; font-weight:600; color:#1a1a2e }
.apply-time { font-size:12px; color:#999; margin-left:auto }

.card-body { display:flex; flex-direction:column; gap:6px }
.card-info-row { display:flex; align-items:baseline; gap:8px; font-size:13px }
.info-label { color:#999; min-width:56px; flex-shrink:0 }
.info-value { color:#333 }
.purpose-text { color:#666; max-width:300px; overflow:hidden; text-overflow:ellipsis; white-space:nowrap }
.device-link { color:#1a73e8; cursor:pointer; font-weight:500 }
.device-link:hover { text-decoration:underline }
.applicant-type { padding:1px 6px; border-radius:3px; font-size:11px; margin-left:6px }
.type-stu { background:rgba(76,175,80,.1); color:#2e7d32 }
.type-tea { background:rgba(26,115,232,.1); color:#1557b0 }

.reject-reason { margin-top:6px; padding:8px 12px; background:rgba(231,76,60,.04); border-radius:8px; font-size:12px; color:#c0392b; line-height:1.5 }
.reason-label { font-weight:600 }
.pickup-hint { margin-top:6px; padding:8px 12px; background:rgba(39,174,96,.04); border-radius:8px; font-size:12px; color:#1e8449; line-height:1.5 }
.hint-label { font-weight:600 }

/* 操作按钮 */
.btn-approve { padding:9px 20px; border:none; border-radius:10px; background:linear-gradient(135deg,#27ae60,#2ecc71); color:#fff; font-size:13px; font-weight:500; cursor:pointer; white-space:nowrap; transition:.15s }
.btn-approve:hover { box-shadow:0 4px 12px rgba(39,174,96,.3) }
.btn-reject { padding:9px 20px; border:1px solid rgba(231,76,60,.3); border-radius:10px; background:#fff; color:#e74c3c; font-size:13px; cursor:pointer; white-space:nowrap; transition:.15s }
.btn-reject:hover { background:rgba(231,76,60,.05); border-color:#e74c3c }
.btn-checkout { padding:9px 20px; border:none; border-radius:10px; background:linear-gradient(135deg,#1a73e8,#4a90d9); color:#fff; font-size:13px; font-weight:500; cursor:pointer; white-space:nowrap; transition:.15s }
.btn-checkout:hover { box-shadow:0 4px 12px rgba(26,115,232,.3) }
.btn-overdue { padding:8px 16px; border:1px solid rgba(231,76,60,.3); border-radius:8px; background:#fff; color:#e74c3c; font-size:13px; cursor:pointer; white-space:nowrap }
.btn-overdue:hover { background:rgba(231,76,60,.05) }
.card-final-text { font-size:12px; color:#bbb }

.empty-state { display:flex; flex-direction:column; align-items:center; padding:60px 0; color:#ccc; gap:8px }
.empty-state p { margin:0; font-size:15px; color:#bbb }

.pagination { display:flex; align-items:center; justify-content:center; gap:10px; margin-top:18px }
.pagination button { padding:7px 16px; border:1px solid rgba(0,0,0,.1); border-radius:8px; background:#fff; font-size:13px; cursor:pointer; color:#555 }
.pagination button:disabled { opacity:.4; cursor:not-allowed }

/* 借用记录表格 */
.data-table-wrap { background:rgba(255,255,255,.5); border-radius:12px; border:1px solid rgba(0,0,0,.04); overflow-x:auto }
.data-table { width:100%; border-collapse:collapse; min-width:800px }
.data-table th,.data-table td { padding:11px 14px; text-align:left; font-size:13px; border-bottom:1px solid rgba(0,0,0,.04); white-space:nowrap }
.data-table th { font-weight:600; color:#666; background:rgba(0,0,0,.015); font-size:12px }
.device-name { font-weight:600; color:#1a1a2e }
.remark-cell { max-width:140px; overflow:hidden; text-overflow:ellipsis; color:#888; font-size:12px }

.log-status-tag { padding:2px 8px; border-radius:4px; font-size:11px; font-weight:600 }
.ls-1 { background:rgba(26,115,232,.1); color:#1a73e8 }
.ls-2 { background:rgba(39,174,96,.1); color:#27ae60 }
.ls-3 { background:rgba(231,76,60,.1); color:#e74c3c }
.overdue-tag { margin-left:4px; padding:1px 6px; border-radius:3px; background:rgba(231,76,60,.15); color:#e74c3c; font-size:10px; font-weight:600 }

.action-cell { display:flex; gap:6px }
.row-btn { padding:4px 12px; border:1px solid rgba(0,0,0,.1); border-radius:6px; background:#fff; font-size:11px; color:#555; cursor:pointer }
.row-btn:hover { border-color:#1a73e8; color:#1a73e8 }
.checkin-btn { border-color:#27ae60; color:#27ae60 }
.checkin-btn:hover { background:rgba(39,174,96,.06) }
.done-text { font-size:12px; color:#999 }
</style>

<!-- 弹窗样式 -->
<style>
.modal-overlay { position:fixed; inset:0; background:rgba(0,0,0,.35); display:flex; align-items:center; justify-content:center; z-index:1100; padding:20px }
.modal-panel { background:#fff; border-radius:16px; width:100%; max-width:480px; max-height:85vh; display:flex; flex-direction:column; box-shadow:0 16px 48px rgba(0,0,0,.15) }
.modal-header { display:flex; align-items:center; justify-content:space-between; padding:20px 24px 0 }
.modal-header h3 { margin:0; font-size:17px; font-weight:600; color:#1a1a2e }
.modal-close { background:none; border:none; cursor:pointer; color:#aaa; padding:4px; border-radius:6px }
.modal-close:hover { background:rgba(0,0,0,.05); color:#333 }
.modal-body { padding:20px 24px; overflow-y:auto; flex:1; display:flex; flex-direction:column; gap:14px }
.modal-footer { display:flex; justify-content:flex-end; gap:10px; padding:16px 24px 20px; border-top:1px solid rgba(0,0,0,.06) }
.modal-btn { padding:10px 24px; border-radius:10px; font-size:14px; cursor:pointer; border:none; font-weight:500; transition:.2s }
.modal-btn.cancel { background:rgba(0,0,0,.04); color:#666 }
.modal-btn.cancel:hover { background:rgba(0,0,0,.08) }
.modal-btn.primary { background:linear-gradient(135deg,#27ae60,#2ecc71); color:#fff }
.modal-btn.primary:hover { box-shadow:0 4px 12px rgba(39,174,96,.3) }
.modal-btn.primary.green { background:linear-gradient(135deg,#27ae60,#2ecc71) }
.modal-btn.danger { background:linear-gradient(135deg,#e74c3c,#ec7063); color:#fff }
.modal-btn.danger:hover:not(:disabled) { box-shadow:0 4px 12px rgba(231,76,60,.3) }
.modal-btn:disabled { opacity:.4; cursor:not-allowed }

.confirm-info { background:rgba(0,0,0,.02); border-radius:10px; padding:12px 16px }
.ci-row { display:flex; justify-content:space-between; padding:4px 0; font-size:13px; color:#555 }
.ci-row span:first-child { color:#999 }

.form-g { display:flex; flex-direction:column; gap:4px }
.form-g label { font-size:13px; font-weight:500; color:#555 }
.required { color:#e74c3c }
.form-input { padding:10px 14px; border:1px solid rgba(0,0,0,.1); border-radius:10px; font-size:14px; color:#333; outline:none; background:rgba(255,255,255,.7); transition:.15s; font-family:inherit; width:100%; box-sizing:border-box }
.form-input:focus { border-color:#1a73e8 }
.form-textarea { resize:vertical; min-height:60px }

.quick-reasons { display:flex; align-items:center; gap:6px; flex-wrap:wrap }
.qr-label { font-size:11px; color:#999; white-space:nowrap }
.qr-btn { padding:5px 10px; border:1px solid rgba(0,0,0,.1); border-radius:6px; background:#fff; font-size:11px; color:#666; cursor:pointer; white-space:nowrap }
.qr-btn:hover { border-color:#1a73e8; color:#1a73e8 }

.modal-fade-enter-active { transition:all .2s ease }
.modal-fade-leave-active { transition:all .15s ease }
.modal-fade-enter-from,.modal-fade-leave-to { opacity:0 }
.modal-fade-enter-from .modal-panel { transform:scale(.94) translateY(12px) }
</style>
