<template>
  <div class="cp-page">
    <div class="page-header">
      <h2>领用审批</h2>
      <p>审核耗材领用申请</p>
    </div>

    <div class="stats-row">
      <div class="stat-card"><div class="stat-num highlight">{{ pendingCount }}</div><div class="stat-label">待审核</div></div>
      <div class="stat-card"><div class="stat-num green">{{ todayApproved }}</div><div class="stat-label">今日通过</div></div>
      <div class="stat-card"><div class="stat-num">{{ monthTotal }}</div><div class="stat-label">本月申请</div></div>
      <div class="stat-card"><div class="stat-num" :class="approvalRate >= 80 ? 'green' : 'orange'">{{ approvalRate }}%</div><div class="stat-label">通过率</div></div>
    </div>

    <div class="filter-bar">
      <div class="filter-tabs">
        <div v-for="t in statusTabs" :key="t.key" class="filter-tab" :class="{ active: activeStatus === t.key }" @click="activeStatus = t.key; pageNum = 1">
          {{ t.label }}<span v-if="t.count !== undefined" class="tab-count">{{ t.count }}</span>
        </div>
      </div>
      <input v-model="searchNo" placeholder="搜索申请单号..." class="search-input" @keyup.enter="pageNum = 1" />
    </div>

    <div class="card-list" v-if="pagedList.length > 0">
      <div v-for="p in pagedList" :key="p.id" class="pick-card" :class="'card-' + p.status">
        <div class="card-left">
          <div class="card-header">
            <span class="status-badge" :class="'badge-' + p.status">{{ p.statusName }}</span>
            <span class="apply-no">{{ p.applyNo }}</span>
            <span class="apply-time">{{ p.createdAt }}</span>
          </div>
          <div class="card-body">
            <div class="card-row">
              <span class="ci-label">耗材</span>
              <span class="ci-value pick-link">{{ p.consumableName }}（{{ p.consumableCode }}）</span>
            </div>
            <div class="card-row">
              <span class="ci-label">申请人</span>
              <span class="ci-value">{{ p.applicantName }}
                <span class="person-type" :class="p.applicantType === 1 ? 'type-stu' : 'type-tea'">{{ p.applicantTypeName }}</span>
              </span>
              <span class="ci-sep">|</span>
              <span class="ci-label">数量</span>
              <span class="ci-value qty-val">{{ p.quantity }}{{ p.unit }}</span>
            </div>
            <div class="card-row">
              <span class="ci-label">用途</span>
              <span class="ci-value">{{ p.purpose }}</span>
            </div>
            <div class="card-row">
              <span class="ci-label">库存</span>
              <span class="stock-info" :class="p.currentStock >= p.quantity * 2 ? 'stock-ok' : p.currentStock >= p.quantity ? 'stock-low' : 'stock-out'">
                当前 {{ p.currentStock }}{{ p.unit }} {{ p.currentStock >= p.quantity * 2 ? '充足' : p.currentStock >= p.quantity ? '偏低' : '不足' }}
              </span>
            </div>
            <div v-if="p.status === 'rejected' && p.rejectReason" class="reject-reason">
              <span class="reason-label">驳回原因：</span>{{ p.rejectReason }}
            </div>
          </div>
        </div>
        <div class="card-right">
          <template v-if="p.status === 'pending'">
            <button class="btn-reject" @click="openReject(p)">驳回</button>
            <button class="btn-approve" @click="openApprove(p)">审核通过</button>
          </template>
          <template v-else-if="p.status === 'approved'">
            <button class="btn-revoke" @click="revokeApproval(p)">撤销审核</button>
          </template>
          <span v-else class="final-text">{{ p.status === 'rejected' ? '已驳回' : '已取消' }}</span>
        </div>
      </div>
    </div>

    <div v-else class="empty-state">
      <svg viewBox="0 0 24 24" width="48" height="48" fill="none" stroke="#ccc" stroke-width="1.2"><path d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2"/></svg>
      <p>暂无领用申请</p>
    </div>

    <div class="pagination" v-if="filteredList.length > pageSize">
      <button :disabled="pageNum <= 1" @click="pageNum--">上一页</button>
      <span>{{ pageNum }} / {{ totalPages }}</span>
      <button :disabled="pageNum >= totalPages" @click="pageNum++">下一页</button>
    </div>

    <!-- 审核通过弹窗 -->
    <teleport to="body">
      <transition name="modal-fade">
        <div v-if="showApprove" class="modal-overlay" @click.self="showApprove = false">
          <div class="modal-panel">
            <div class="modal-header">
              <h3>确认审核通过</h3>
              <button class="modal-close" @click="showApprove = false"><svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2"><path d="M18 6L6 18M6 6l12 12"/></svg></button>
            </div>
            <div class="modal-body">
              <div class="confirm-info">
                <div class="ci-row"><span>耗材</span><span>{{ currentPick?.consumableName }}（{{ currentPick?.consumableCode }}）</span></div>
                <div class="ci-row"><span>申请人</span><span>{{ currentPick?.applicantName }}</span></div>
                <div class="ci-row"><span>数量</span><span>{{ currentPick?.quantity }}{{ currentPick?.unit }}</span></div>
                <div class="ci-row"><span>库存变动</span><span>{{ currentPick?.currentStock }}{{ currentPick?.unit }} → {{ (currentPick?.currentStock || 0) - (currentPick?.quantity || 0) }}{{ currentPick?.unit }}</span></div>
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
                <textarea v-model="rejectForm.reason" class="form-input form-textarea" rows="3" placeholder="请填写驳回原因..." maxlength="300" />
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
import { getMaterialPage } from '../../utils/materialApi'
import { isMockMode } from '../../stores/authStore'

type PickStatus = 'pending' | 'approved' | 'rejected' | 'cancelled'

interface PickApply {
  id: number; applyNo: string; consumableId: number; consumableName: string; consumableCode: string
  unit: string; quantity: number; currentStock: number
  applicantId: number; applicantName: string; applicantType: number; applicantTypeName: string
  purpose: string; status: PickStatus; statusName: string
  rejectReason?: string; createdAt: string; approvedAt?: string
}

const MOCK_PICKS: PickApply[] = [
  { id:1, applyNo:'PICK-2024-001', consumableId:1, consumableName:'无水乙醇', consumableCode:'CON-001',
    unit:'瓶', quantity:2, currentStock:45,
    applicantId:301, applicantName:'张同学', applicantType:1, applicantTypeName:'学生',
    purpose:'有机化学实验 - 萃取操作', status:'pending', statusName:'待审核', createdAt:'2026-07-08 14:30' },
  { id:2, applyNo:'PICK-2024-002', consumableId:2, consumableName:'烧杯500ml', consumableCode:'CON-002',
    unit:'个', quantity:5, currentStock:32,
    applicantId:302, applicantName:'李老师', applicantType:2, applicantTypeName:'教师',
    purpose:'物理实验课程演示', status:'pending', statusName:'待审核', createdAt:'2026-07-08 16:00' },
  { id:3, applyNo:'PICK-2024-003', consumableId:3, consumableName:'丁腈手套', consumableCode:'CON-003',
    unit:'盒', quantity:10, currentStock:8,
    applicantId:303, applicantName:'王同学', applicantType:1, applicantTypeName:'学生',
    purpose:'生物实验防护使用', status:'rejected', statusName:'已驳回', createdAt:'2026-07-07 10:00',
    approvedAt:'2026-07-07 15:00', rejectReason:'当前库存不足，仅剩 8 盒，无法满足 10 盒的领用需求。建议分批领用或等待补货。' },
  { id:4, applyNo:'PICK-2024-004', consumableId:4, consumableName:'浓硫酸', consumableCode:'CON-004',
    unit:'瓶', quantity:1, currentStock:12,
    applicantId:304, applicantName:'孙老师', applicantType:2, applicantTypeName:'教师',
    purpose:'分析化学实验', status:'pending', statusName:'待审核', createdAt:'2026-07-09 09:00' },
  { id:5, applyNo:'PICK-2024-005', consumableId:5, consumableName:'pH试纸', consumableCode:'CON-005',
    unit:'盒', quantity:3, currentStock:60,
    applicantId:305, applicantName:'赵同学', applicantType:1, applicantTypeName:'学生',
    purpose:'水质检测实验', status:'approved', statusName:'已通过', createdAt:'2026-07-06 11:00',
    approvedAt:'2026-07-06 16:00' },
  { id:6, applyNo:'PICK-2024-006', consumableId:1, consumableName:'无水乙醇', consumableCode:'CON-001',
    unit:'瓶', quantity:1, currentStock:45,
    applicantId:306, applicantName:'陈同学', applicantType:1, applicantTypeName:'学生',
    purpose:'个人实验练习', status:'cancelled', statusName:'已取消', createdAt:'2026-07-05 08:00' },
]

const picks = ref<PickApply[]>([])

async function loadData() {
  if (isMockMode.value) { picks.value = [...MOCK_PICKS]; return }
  try {
    // 同时请求领用记录和物资详情，交叉关联
    const [receivesRes, materialsRes] = await Promise.all([
      api.get('/material/receives', { params: { pageNum: 1, pageSize: 100 } }),
      getMaterialPage({ pageNum: 1, pageSize: 500 })
    ])

    // 构建物资ID → 物资详情的映射表
    const materialMap = new Map<number, any>()
    if (materialsRes?.records) {
      materialsRes.records.forEach((m: any) => materialMap.set(m.id, m))
    }

    const records: any[] = receivesRes.data?.data?.records || []
    if (records.length) {
      picks.value = records.map((r: any) => {
        const mat = materialMap.get(r.materialId) || {}
        return {
          id: r.id,
          applyNo: r.applyNo || '',
          consumableId: r.materialId || 0,
          consumableName: r.materialName || '',
          consumableCode: mat.materialNo || `MAT-${r.materialId}`,
          unit: mat.unit || '—',
          quantity: r.expectedQuantity || 0,
          currentStock: mat.currentStock ?? 0,
          applicantId: r.applicantId || 0,
          applicantName: r.applicantName || `用户#${r.applicantId || '未知'}`,
          applicantType: r.applicantType || 1,
          applicantTypeName: r.applicantType === 2 ? '教师' : '学生',
          purpose: r.purpose || '',
          status: (r.applyStatus === 1 ? 'pending' : r.applyStatus === 2 ? 'approved' : r.applyStatus === 3 ? 'rejected' : 'cancelled') as PickStatus,
          statusName: r.applyStatusDesc || '待审核',
          rejectReason: r.rejectReason || undefined,
          createdAt: r.createdAt || '',
          approvedAt: r.updatedAt || r.approvedAt || undefined
        }
      })
    }
  } catch { /* 请求失败时保持空列表 */ }
}

onMounted(() => loadData())

const MOCK_PICKS2 = null as any
const activeStatus = ref<string>('all')
const searchNo = ref('')
const pageNum = ref(1)
const pageSize = 6

const statusTabs = computed(() => [
  { key:'all' as const, label:'全部', count:picks.value.length },
  { key:'pending' as const, label:'待审核', count:picks.value.filter(p => p.status === 'pending').length },
  { key:'approved' as const, label:'已通过', count:picks.value.filter(p => p.status === 'approved').length },
  { key:'rejected' as const, label:'已驳回', count:picks.value.filter(p => p.status === 'rejected').length },
  { key:'cancelled' as const, label:'已取消', count:picks.value.filter(p => p.status === 'cancelled').length },
])

const filteredList = computed(() => {
  let list = picks.value
  if (activeStatus.value !== 'all') list = list.filter(p => p.status === activeStatus.value)
  if (searchNo.value) list = list.filter(p => p.applyNo.toLowerCase().includes(searchNo.value.toLowerCase()))
  return list
})
const totalPages = computed(() => Math.ceil(filteredList.value.length / pageSize) || 1)
const pagedList = computed(() => filteredList.value.slice((pageNum.value - 1) * pageSize, pageNum.value * pageSize))

const pendingCount = computed(() => picks.value.filter(p => p.status === 'pending').length)
const todayApproved = computed(() => {
  const today = new Date().toISOString().slice(0, 10)
  return picks.value.filter(p => p.status === 'approved' && p.approvedAt?.startsWith(today)).length
})
const monthTotal = computed(() => picks.value.length)
const approvalRate = computed(() => {
  const decided = picks.value.filter(p => p.status === 'approved' || p.status === 'rejected').length
  return decided === 0 ? 0 : Math.round(picks.value.filter(p => p.status === 'approved').length / decided * 100)
})

const showApprove = ref(false)
const showReject = ref(false)
const currentPick = ref<PickApply | null>(null)
const rejectForm = reactive({ reason: '' })
const quickReasons = ['当前库存不足，无法满足领用需求', '领用用途不明确，请补充说明', '领用数量超出合理范围', '其他原因，请手动填写']

function openApprove(p: PickApply) { currentPick.value = p; showApprove.value = true }
function openReject(p: PickApply) { currentPick.value = p; rejectForm.reason = ''; showReject.value = true }
async function doApprove() {
  if (!currentPick.value) return
  if (isMockMode.value) {
    const p = picks.value.find(x => x.id === currentPick.value!.id)
    if (p) { p.status = 'approved'; p.statusName = '已通过'; p.approvedAt = new Date().toISOString().slice(0, 16); p.currentStock -= p.quantity }
    showApprove.value = false; return
  }
  try {
    await api.post(`/material/receives/${currentPick.value.applyNo}/approve`, {})
    showApprove.value = false; loadData()
  } catch (e: any) { alert(e.message || '操作失败') }
}
async function doReject() {
  if (!currentPick.value || !rejectForm.reason.trim()) return
  if (isMockMode.value) {
    const p = picks.value.find(x => x.id === currentPick.value!.id)
    if (p) { p.status = 'rejected'; p.statusName = '已驳回'; p.rejectReason = rejectForm.reason.trim() }
    showReject.value = false; return
  }
  try {
    await api.post(`/material/receives/${currentPick.value.applyNo}/reject`, { reason: rejectForm.reason.trim() })
    showReject.value = false; loadData()
  } catch (e: any) { alert(e.message || '操作失败') }
}
function revokeApproval(p: PickApply) {
  if (confirm(`确定撤销对「${p.consumableName}」的审批通过吗？库存将恢复。`)) { p.status = 'pending'; p.statusName = '待审核'; p.approvedAt = undefined; p.currentStock += p.quantity }
}
</script>

<style scoped>
.cp-page { height:100%; overflow-y:auto; padding:20px 24px }
.page-header { margin-bottom:16px }
.page-header h2 { font-size:20px; font-weight:600; color:#1a1a2e; margin:0 }
.page-header p { margin:4px 0 0; font-size:13px; color:#999 }

.stats-row { display:flex; gap:12px; margin-bottom:16px; flex-wrap:wrap }
.stat-card { background:rgba(255,255,255,.65); backdrop-filter:blur(10px); border:1px solid rgba(0,0,0,.06); border-radius:12px; padding:14px 20px; min-width:100px; text-align:center }
.stat-num { font-size:24px; font-weight:700; color:#1a1a2e; line-height:1.1 }
.stat-label { font-size:12px; color:#999; margin-top:2px }
.highlight { color:#1a73e8 } .green { color:#27ae60 } .orange { color:#f39c12 }

.filter-bar { display:flex; align-items:center; justify-content:space-between; margin-bottom:18px; gap:12px; flex-wrap:wrap }
.filter-tabs { display:flex; gap:4px; background:rgba(255,255,255,.4); border-radius:10px; padding:4px }
.filter-tab { padding:8px 16px; border-radius:8px; font-size:13px; color:#666; cursor:pointer; transition:all .15s; display:flex; align-items:center; gap:6px; user-select:none; white-space:nowrap }
.filter-tab:hover { color:#333; background:rgba(0,0,0,.03) }
.filter-tab.active { background:#fff; color:#1a73e8; font-weight:600; box-shadow:0 1px 3px rgba(0,0,0,.08) }
.tab-count { font-size:11px; background:rgba(0,0,0,.06); padding:1px 7px; border-radius:10px; color:#888 }
.filter-tab.active .tab-count { background:rgba(26,115,232,.1); color:#1a73e8 }
.search-input { padding:8px 12px; border:1px solid rgba(0,0,0,.1); border-radius:10px; font-size:13px; outline:none; width:180px; background:rgba(255,255,255,.7) }

.card-list { display:flex; flex-direction:column; gap:12px }
.pick-card { background:rgba(255,255,255,.55); backdrop-filter:blur(10px); border-radius:12px; border:1px solid rgba(0,0,0,.04); display:flex; align-items:stretch; overflow:hidden }
.pick-card.card-pending { border-left:3px solid #1a73e8 }
.pick-card.card-approved { border-left:3px solid #27ae60 }
.pick-card.card-rejected { border-left:3px solid #e74c3c }
.pick-card.card-cancelled { border-left:3px solid #ccc; opacity:.65 }

.card-left { flex:1; padding:16px 20px; min-width:0 }
.card-right { display:flex; flex-direction:column; align-items:flex-end; justify-content:center; gap:8px; padding:16px 20px; flex-shrink:0; min-width:130px; border-left:1px solid rgba(0,0,0,.04) }

.card-header { display:flex; align-items:center; gap:10px; margin-bottom:10px }
.status-badge { padding:2px 8px; border-radius:4px; font-size:11px; font-weight:600 }
.badge-pending { background:rgba(26,115,232,.1); color:#1a73e8 }
.badge-approved { background:rgba(39,174,96,.1); color:#27ae60 }
.badge-rejected { background:rgba(231,76,60,.1); color:#e74c3c }
.badge-cancelled { background:rgba(0,0,0,.05); color:#999 }
.apply-no { font-size:13px; font-weight:600; color:#1a1a2e }
.apply-time { font-size:12px; color:#999; margin-left:auto }

.card-body { display:flex; flex-direction:column; gap:5px }
.card-row { display:flex; align-items:baseline; gap:6px; font-size:13px; flex-wrap:wrap }
.ci-label { color:#999; min-width:40px; flex-shrink:0 }
.ci-value { color:#333 }
.pick-link { color:#1a73e8; font-weight:500 }
.person-type { padding:1px 6px; border-radius:3px; font-size:11px; margin-left:4px }
.type-stu { background:rgba(76,175,80,.1); color:#2e7d32 }
.type-tea { background:rgba(26,115,232,.1); color:#1557b0 }
.ci-sep { color:#ddd }
.qty-val { font-weight:600; color:#e67e22 }

.stock-info { padding:2px 8px; border-radius:4px; font-size:11px; font-weight:500 }
.stock-ok { background:rgba(39,174,96,.08); color:#27ae60 }
.stock-low { background:rgba(243,156,18,.08); color:#e67e22 }
.stock-out { background:rgba(231,76,60,.08); color:#e74c3c }

.reject-reason { margin-top:6px; padding:8px 12px; background:rgba(231,76,60,.04); border-radius:8px; font-size:12px; color:#c0392b; line-height:1.5 }
.reason-label { font-weight:600 }

.btn-approve { padding:9px 20px; border:none; border-radius:10px; background:linear-gradient(135deg,#27ae60,#2ecc71); color:#fff; font-size:13px; font-weight:500; cursor:pointer; white-space:nowrap }
.btn-approve:hover { box-shadow:0 4px 12px rgba(39,174,96,.3) }
.btn-reject { padding:9px 20px; border:1px solid rgba(231,76,60,.3); border-radius:10px; background:#fff; color:#e74c3c; font-size:13px; cursor:pointer; white-space:nowrap }
.btn-reject:hover { background:rgba(231,76,60,.05); border-color:#e74c3c }
.btn-revoke { padding:8px 16px; border:1px solid rgba(0,0,0,.1); border-radius:8px; background:#fff; color:#888; font-size:12px; cursor:pointer }
.btn-revoke:hover { border-color:#e74c3c; color:#e74c3c }
.final-text { font-size:12px; color:#bbb }

.empty-state { display:flex; flex-direction:column; align-items:center; padding:60px 0; color:#ccc; gap:8px }
.empty-state p { margin:0; font-size:15px; color:#bbb }

.pagination { display:flex; align-items:center; justify-content:center; gap:10px; margin-top:18px }
.pagination button { padding:7px 16px; border:1px solid rgba(0,0,0,.1); border-radius:8px; background:#fff; font-size:13px; cursor:pointer; color:#555 }
.pagination button:disabled { opacity:.4; cursor:not-allowed }
</style>

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
.form-input { padding:10px 14px; border:1px solid rgba(0,0,0,.1); border-radius:10px; font-size:14px; outline:none; background:rgba(255,255,255,.7); font-family:inherit; width:100%; box-sizing:border-box }
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
