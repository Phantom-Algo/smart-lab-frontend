<template>
  <div class="da-page">
    <div class="page-header">
      <h2>借用审批</h2>
      <p>审核设备借用申请</p>
    </div>

    <!-- 统计卡片 -->
    <div class="stats-row">
      <div class="stat-card">
        <div class="stat-num highlight">{{ pendingCount }}</div>
        <div class="stat-label">待审批</div>
      </div>
      <div class="stat-card">
        <div class="stat-num green">{{ todayApproved }}</div>
        <div class="stat-label">今日通过</div>
      </div>
      <div class="stat-card">
        <div class="stat-num">{{ monthApproved }}</div>
        <div class="stat-label">本月通过</div>
      </div>
      <div class="stat-card">
        <div class="stat-num" :class="approvalRate >= 80 ? 'green' : 'orange'">{{ approvalRate }}%</div>
        <div class="stat-label">通过率</div>
      </div>
    </div>

    <!-- 筛选栏 -->
    <div class="filter-bar">
      <div class="filter-tabs">
        <div
          v-for="t in statusTabs"
          :key="t.key"
          class="filter-tab"
          :class="{ active: activeStatus === t.key }"
          @click="activeStatus = t.key; pageNum = 1"
        >
          {{ t.label }}
          <span v-if="t.count !== undefined" class="tab-count">{{ t.count }}</span>
        </div>
      </div>
      <input v-model="searchNo" placeholder="搜索申请单号..." class="search-input" @keyup.enter="pageNum = 1" />
    </div>

    <!-- 申请卡片列表 -->
    <div class="card-list" v-if="pagedList.length > 0">
      <div
        v-for="apply in pagedList"
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
            <!-- 驳回原因 -->
            <div v-if="apply.status === 'rejected' && apply.rejectReason" class="reject-reason">
              <span class="reason-label">驳回原因：</span>{{ apply.rejectReason }}
            </div>
            <!-- 领用提示 -->
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
            <button class="btn-revoke" @click="revokeApproval(apply)">撤销审批</button>
          </template>
          <span v-else class="card-final-text">
            {{ apply.status === 'rejected' ? '已处理' : '已取消' }}
          </span>
        </div>
      </div>
    </div>

    <div v-else class="empty-state">
      <svg viewBox="0 0 24 24" width="48" height="48" fill="none" stroke="#ccc" stroke-width="1.2"><path d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2"/></svg>
      <p>暂无{{ activeStatus !== 'all' ? statusLabel(activeStatus) + '的' : '' }}借用申请</p>
    </div>

    <div class="pagination" v-if="filteredList.length > pageSize">
      <button :disabled="pageNum <= 1" @click="pageNum--">上一页</button>
      <span>{{ pageNum }} / {{ totalPages }}</span>
      <button :disabled="pageNum >= totalPages" @click="pageNum++">下一页</button>
    </div>

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
  </div>
</template>

<script setup lang="ts">
import { ref, computed, reactive, onMounted } from 'vue'
import api from '../../utils/api'
import { isMockMode } from '../../stores/authStore'

type BorrowStatus = 'pending' | 'approved' | 'rejected' | 'cancelled'

interface BorrowApply {
  id: number; applyNo: string; deviceId: number; deviceName: string; deviceNo: string
  applicantId: number; applicantName: string; applicantType: number; applicantTypeName: string
  expectedStartTime: string; expectedReturnTime: string
  status: BorrowStatus; statusName: string
  rejectReason?: string; pickupHint?: string
  createdAt: string; approvedAt?: string
}

const MOCK_APPLIES: BorrowApply[] = [
  { id:1, applyNo:'JY-DEV-001', deviceId:1, deviceName:'数字示波器', deviceNo:'EQ-2024-002',
    applicantId:101, applicantName:'张同学', applicantType:1, applicantTypeName:'学生',
    expectedStartTime:'07-09 09:00', expectedReturnTime:'07-10 18:00',
    status:'pending', statusName:'待审核', createdAt:'2026-07-08 10:30' },
  { id:2, applyNo:'JY-DEV-002', deviceId:2, deviceName:'高速离心机', deviceNo:'EQ-2024-001',
    applicantId:102, applicantName:'李老师', applicantType:2, applicantTypeName:'教师',
    expectedStartTime:'07-12 08:00', expectedReturnTime:'07-14 17:00',
    status:'pending', statusName:'待审核', createdAt:'2026-07-08 14:20' },
  { id:3, applyNo:'JY-DEV-003', deviceId:3, deviceName:'紫外分光光度计', deviceNo:'EQ-2022-008',
    applicantId:103, applicantName:'王同学', applicantType:1, applicantTypeName:'学生',
    expectedStartTime:'07-10 08:00', expectedReturnTime:'07-12 17:00',
    status:'pending', statusName:'待审核', createdAt:'2026-07-08 09:00' },
  { id:4, applyNo:'JY-DEV-004', deviceId:4, deviceName:'PCR扩增仪', deviceNo:'EQ-2024-010',
    applicantId:104, applicantName:'孙老师', applicantType:2, applicantTypeName:'教师',
    expectedStartTime:'07-15 13:00', expectedReturnTime:'07-16 17:00',
    status:'approved', statusName:'已通过', createdAt:'2026-07-07 08:00',
    approvedAt:'2026-07-07 16:30', pickupHint:'请到 A201 化学实验室联系实验室管理员领用设备' },
  { id:5, applyNo:'JY-DEV-005', deviceId:5, deviceName:'电子天平', deviceNo:'EQ-2024-003',
    applicantId:105, applicantName:'赵同学', applicantType:1, applicantTypeName:'学生',
    expectedStartTime:'07-11 09:00', expectedReturnTime:'07-11 12:00',
    status:'rejected', statusName:'已驳回', createdAt:'2026-07-07 11:00',
    approvedAt:'2026-07-07 15:00', rejectReason:'设备正在维修中，预计 7月15日恢复使用。请届时重新提交申请。' },
  { id:6, applyNo:'JY-DEV-006', deviceId:6, deviceName:'显微镜', deviceNo:'EQ-2022-005',
    applicantId:106, applicantName:'陈同学', applicantType:1, applicantTypeName:'学生',
    expectedStartTime:'07-08 14:00', expectedReturnTime:'07-08 18:00',
    status:'cancelled', statusName:'已取消', createdAt:'2026-07-06 10:00' },
  { id:7, applyNo:'JY-DEV-007', deviceId:7, deviceName:'服务器机柜', deviceNo:'EQ-2023-020',
    applicantId:107, applicantName:'周老师', applicantType:2, applicantTypeName:'教师',
    expectedStartTime:'07-18 08:00', expectedReturnTime:'07-20 18:00',
    status:'approved', statusName:'已通过', createdAt:'2026-07-06 16:00',
    approvedAt:'2026-07-07 09:00' },
  { id:8, applyNo:'JY-DEV-008', deviceId:2, deviceName:'高速离心机', deviceNo:'EQ-2024-001',
    applicantId:108, applicantName:'刘同学', applicantType:1, applicantTypeName:'学生',
    expectedStartTime:'07-16 09:00', expectedReturnTime:'07-16 17:00',
    status:'rejected', statusName:'已驳回', createdAt:'2026-07-05 14:00',
    approvedAt:'2026-07-06 10:00', rejectReason:'该时段设备已被其他教师预约占用，请调整时间后重新提交。' },
]

const applies = ref<BorrowApply[]>([])

async function loadData() {
  if (isMockMode.value) { applies.value = [...MOCK_APPLIES]; return }
  try {
    const res = await api.get('/equipment/borrow-applies', { params: { pageNum: 1, pageSize: 100 } })
    const records = res.data?.data?.records || []
    applies.value = records.map((r: any) => ({
        id: r.id || 0, applyNo: r.applyNo || '', deviceId: r.deviceId || 0, deviceName: r.deviceName || '', deviceNo: r.deviceNo || '',
        applicantId: r.applicantId || 0, applicantName: r.applicantName || '', applicantType: r.applicantType || 1, applicantTypeName: r.applicantType === 2 ? '教师' : '学生',
        expectedStartTime: r.expectedStartTime || '', expectedReturnTime: r.expectedReturnTime || '',
        status: mapBorrowStatus(r.applyStatus ?? r.status), statusName: r.applyStatusDesc || r.statusName || '待审核',
        rejectReason: r.rejectReason, pickupHint: r.pickupHint, createdAt: r.createdAt || '',
        approvedAt: r.approvedAt || (r.applyStatus && r.applyStatus !== 1 ? r.updatedAt : undefined)
    }))
  } catch { /* API 不可用，请检查后端服务 */ }
}

onMounted(() => loadData())

// ========== 筛选 & 分页 ==========
const activeStatus = ref<BorrowStatus | 'all'>('all')
const searchNo = ref('')
const pageNum = ref(1)
const pageSize = 6

const statusTabs = computed(() => [
  { key: 'all' as const, label: '全部', count: applies.value.length },
  { key: 'pending' as const, label: '待审核', count: applies.value.filter(a => a.status === 'pending').length },
  { key: 'approved' as const, label: '已通过', count: applies.value.filter(a => a.status === 'approved').length },
  { key: 'rejected' as const, label: '已驳回', count: applies.value.filter(a => a.status === 'rejected').length },
  { key: 'cancelled' as const, label: '已取消', count: applies.value.filter(a => a.status === 'cancelled').length },
])

const filteredList = computed(() => {
  let list = applies.value
  if (activeStatus.value !== 'all') list = list.filter(a => a.status === activeStatus.value)
  if (searchNo.value) list = list.filter(a => a.applyNo.toLowerCase().includes(searchNo.value.toLowerCase()))
  return list
})
const totalPages = computed(() => Math.ceil(filteredList.value.length / pageSize) || 1)
const pagedList = computed(() => {
  const s = (pageNum.value - 1) * pageSize
  return filteredList.value.slice(s, s + pageSize)
})

const pendingCount = computed(() => applies.value.filter(a => a.status === 'pending').length)
const todayApproved = computed(() => {
  const today = new Date().toISOString().slice(0, 10)
  return applies.value.filter(a => a.status === 'approved' && a.approvedAt?.startsWith(today)).length
})
const monthApproved = computed(() => applies.value.filter(a => a.status === 'approved').length)
const approvalRate = computed(() => {
  const decided = applies.value.filter(a => a.status === 'approved' || a.status === 'rejected').length
  if (decided === 0) return 0
  return Math.round((applies.value.filter(a => a.status === 'approved').length / decided) * 100)
})

// ========== 审批弹窗 ==========
const showApprove = ref(false)
const showReject = ref(false)
const currentApply = ref<BorrowApply | null>(null)
const approveForm = reactive({ pickupHint: '' })
const rejectForm = reactive({ reason: '' })

const quickReasons = ['设备正在维修中', '该时段已被预约', '申请理由不充分', '设备暂不可借用', '其他原因请手动填写']

function mapBorrowStatus(status: number | string): BorrowStatus {
  const numericMap: Record<number, BorrowStatus> = { 1: 'pending', 2: 'approved', 3: 'rejected', 4: 'cancelled' }
  if (typeof status === 'number') return numericMap[status] || 'pending'
  return (status || 'pending') as BorrowStatus
}

function openApprove(apply: BorrowApply) {
  currentApply.value = apply
  approveForm.pickupHint = ''
  showApprove.value = true
}
function openReject(apply: BorrowApply) {
  currentApply.value = apply
  rejectForm.reason = ''
  showReject.value = true
}
async function doApprove() {
  if (!currentApply.value) return
  if (isMockMode.value) {
    const a = applies.value.find(x => x.id === currentApply.value!.id)
    if (a) { a.status = 'approved'; a.statusName = '已通过'; a.approvedAt = new Date().toISOString().slice(0, 16); a.pickupHint = approveForm.pickupHint.trim() || undefined }
    showApprove.value = false; return
  }
  try {
    await api.post(`/equipment/borrows/${currentApply.value.applyNo}/approve`, { approved: true })
    showApprove.value = false
    activeStatus.value = 'approved'
    await loadData()
  } catch (e: any) { alert(e?.response?.data?.message || e?.message || '操作失败') }
}
async function doReject() {
  if (!currentApply.value || !rejectForm.reason.trim()) return
  if (isMockMode.value) {
    const a = applies.value.find(x => x.id === currentApply.value!.id)
    if (a) { a.status = 'rejected'; a.statusName = '已驳回'; a.rejectReason = rejectForm.reason.trim() }
    showReject.value = false; return
  }
  try {
    await api.post(`/equipment/borrows/${currentApply.value.applyNo}/approve`, {
      approved: false,
      rejectReason: rejectForm.reason.trim()
    })
    showReject.value = false
    activeStatus.value = 'rejected'
    await loadData()
  } catch (e: any) { alert(e?.response?.data?.message || e?.message || '操作失败') }
}
function revokeApproval(apply: BorrowApply) {
  if (confirm(`确定撤销对「${apply.deviceName}」的审批通过吗？该申请将退回待审核状态。`)) {
    apply.status = 'pending'
    apply.statusName = '待审核'
    apply.approvedAt = undefined
    apply.pickupHint = undefined
  }
}

function viewDevice(deviceId: number) {
  alert('查看设备详情：deviceId=' + deviceId + '（可跳转到设备台账查看页面）')
}

function statusLabel(s: string): string {
  const m: Record<string, string> = { pending: '待审核', approved: '已通过', rejected: '已驳回', cancelled: '已取消' }
  return m[s] || s
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
.orange { color:#f39c12 }

/* 筛选栏 */
.filter-bar { display:flex; align-items:center; justify-content:space-between; margin-bottom:18px; gap:12px; flex-wrap:wrap }
.filter-tabs { display:flex; gap:4px; background:rgba(255,255,255,.4); border-radius:10px; padding:4px }
.filter-tab { padding:8px 16px; border-radius:8px; font-size:13px; color:#666; cursor:pointer; transition:all .15s; display:flex; align-items:center; gap:6px; user-select:none; white-space:nowrap }
.filter-tab:hover { color:#333; background:rgba(0,0,0,.03) }
.filter-tab.active { background:#fff; color:#1a73e8; font-weight:600; box-shadow:0 1px 3px rgba(0,0,0,.08) }
.tab-count { font-size:11px; background:rgba(0,0,0,.06); padding:1px 7px; border-radius:10px; color:#888 }
.filter-tab.active .tab-count { background:rgba(26,115,232,.1); color:#1a73e8 }
.search-input { padding:8px 12px; border:1px solid rgba(0,0,0,.1); border-radius:10px; font-size:13px; outline:none; width:180px; background:rgba(255,255,255,.7) }

/* 卡片列表 */
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
.btn-revoke { padding:8px 16px; border:1px solid rgba(0,0,0,.1); border-radius:8px; background:#fff; color:#888; font-size:12px; cursor:pointer }
.btn-revoke:hover { border-color:#e74c3c; color:#e74c3c }
.card-final-text { font-size:12px; color:#bbb }

.empty-state { display:flex; flex-direction:column; align-items:center; padding:60px 0; color:#ccc; gap:8px }
.empty-state p { margin:0; font-size:15px; color:#bbb }

.pagination { display:flex; align-items:center; justify-content:center; gap:10px; margin-top:18px }
.pagination button { padding:7px 16px; border:1px solid rgba(0,0,0,.1); border-radius:8px; background:#fff; font-size:13px; cursor:pointer; color:#555 }
.pagination button:disabled { opacity:.4; cursor:not-allowed }
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
