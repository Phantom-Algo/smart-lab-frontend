<template>
  <div class="sh-page">
    <div class="page-header">
      <h2>隐患管理</h2>
      <p>隐患分派、整改复核、闭环管理</p>
    </div>

    <div class="stats-row">
      <div class="stat-card"><div class="stat-num blue">{{ hazards.length }}</div><div class="stat-label">总数</div></div>
      <div class="stat-card"><div class="stat-num red">{{ statusCount('pending') }}</div><div class="stat-label">待分派</div></div>
      <div class="stat-card"><div class="stat-num orange">{{ statusCount('rectifying') }}</div><div class="stat-label">整改中</div></div>
      <div class="stat-card"><div class="stat-num green">{{ statusCount('reviewing') }}</div><div class="stat-label">待复核</div></div>
    </div>

    <div class="filter-bar">
      <div class="filter-tabs">
        <div v-for="t in statusTabs" :key="t.key" class="filter-tab" :class="{ active: activeStatus === t.key }" @click="activeStatus = t.key; pageNum = 1">
          {{ t.label }}<span v-if="t.count !== undefined" class="tab-count">{{ t.count }}</span>
        </div>
      </div>
      <select v-model="filterLevel" class="tool-select"><option value="">全部等级</option><option value="high">高</option><option value="medium">中</option><option value="low">低</option></select>
    </div>

    <div class="card-list" v-if="pagedList.length > 0">
      <div v-for="h in pagedList" :key="h.id" class="hazard-card" :class="'card-' + h.status">
        <div class="card-left">
          <div class="card-header">
            <span class="level-tag" :class="'level-' + h.level">{{ levelLabel(h.level) }}</span>
            <span class="status-badge" :class="'badge-' + h.status">{{ h.statusName }}</span>
            <span class="hazard-no">{{ h.hazardNo }}</span>
            <span class="hazard-time">{{ h.reportTime }}</span>
          </div>
          <div class="card-body">
            <div class="card-row title-row"><span class="hazard-title">{{ h.title }}</span></div>
            <div class="card-row">
              <span class="ci-label">实验室</span><span class="ci-value">{{ h.labName }}</span>
              <span class="ci-sep">|</span>
              <span class="ci-label">上报人</span><span class="ci-value">{{ h.reporterName }}</span>
              <span class="ci-sep">|</span>
              <span class="ci-label">期限</span><span class="ci-value deadline">{{ h.deadline || '未设置' }}</span>
            </div>
            <div class="card-row desc-row" v-if="h.description"><span class="ci-label">描述</span><span class="ci-value desc-text">{{ h.description }}</span></div>
            <div class="card-row" v-if="h.handlerName">
              <span class="ci-label">处理人</span><span class="ci-value">{{ h.handlerName }}</span>
              <span class="ci-sep" v-if="h.rectifyNote">|</span>
              <span class="ci-value rectify-note" v-if="h.rectifyNote">整改说明：{{ h.rectifyNote }}</span>
            </div>
            <div class="card-row" v-if="h.status === 'closed' && h.reviewerName">
              <span class="ci-label">复核人</span><span class="ci-value">{{ h.reviewerName }}</span>
              <span class="ci-sep">|</span>
              <span class="ci-value">复核意见：{{ h.reviewOpinion || '验收合格' }}</span>
            </div>
          </div>
        </div>
        <div class="card-right">
          <template v-if="h.status === 'pending'">
            <button class="btn-primary" @click="openAssign(h)">分派整改人</button>
          </template>
          <template v-else-if="h.status === 'rectifying'">
            <button class="btn-primary green" @click="submitReview(h)">提交复核</button>
          </template>
          <template v-else-if="h.status === 'reviewing'">
            <button class="btn-outline orange" @click="returnRectify(h)">退回整改</button>
            <button class="btn-primary green" @click="approveClose(h)">复核通过</button>
          </template>
          <span v-else class="final-text">✅ 已闭环</span>
        </div>
      </div>
    </div>

    <div v-else class="empty-state">
      <svg viewBox="0 0 24 24" width="48" height="48" fill="none" stroke="#ccc" stroke-width="1.2"><path d="M10.29 3.86L1.82 18a2 2 0 001.71 3h16.94a2 2 0 001.71-3L13.71 3.86a2 2 0 00-3.42 0z"/><line x1="12" y1="9" x2="12" y2="13"/><line x1="12" y1="17" x2="12.01" y2="17"/></svg>
      <p>暂无隐患记录</p>
    </div>

    <div class="pagination" v-if="filteredList.length > pageSize">
      <button :disabled="pageNum <= 1" @click="pageNum--">上一页</button>
      <span>{{ pageNum }} / {{ totalPages }}</span>
      <button :disabled="pageNum >= totalPages" @click="pageNum++">下一页</button>
    </div>

    <!-- 分派弹窗 -->
    <teleport to="body">
      <transition name="modal-fade">
        <div v-if="showAssign" class="modal-overlay" @click.self="showAssign = false">
          <div class="modal-panel">
            <div class="modal-header"><h3>分派整改人</h3><button class="modal-close" @click="showAssign = false"><svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2"><path d="M18 6L6 18M6 6l12 12"/></svg></button></div>
            <div class="modal-body">
              <div class="confirm-info"><div class="ci-row"><span>隐患</span><span>{{ currentHazard?.hazardNo }} {{ currentHazard?.title }}</span></div></div>
              <div class="form-g"><label>处理人 <span class="required">*</span></label><select v-model="assignForm.handlerId" class="form-input"><option :value="0" disabled>请选择</option><option v-for="p in handlerOptions" :key="p.id" :value="p.id">{{ p.name }}</option></select></div>
              <div class="form-g"><label>整改期限 <span class="required">*</span></label><input v-model="assignForm.deadline" type="date" class="form-input" /></div>
              <div class="form-g"><label>分派说明</label><input v-model="assignForm.note" class="form-input" placeholder="可选" /></div>
            </div>
            <div class="modal-footer">
              <button class="modal-btn cancel" @click="showAssign = false">取消</button>
              <button class="modal-btn primary" @click="doAssign" :disabled="!assignForm.handlerId">确认分派</button>
            </div>
          </div>
        </div>
      </transition>
    </teleport>

    <!-- 提交复核 / 退回 / 复核通过 确认弹窗 -->
    <teleport to="body">
      <transition name="modal-fade">
        <div v-if="showConfirm" class="modal-overlay" @click.self="showConfirm = false">
          <div class="modal-panel">
            <div class="modal-header"><h3>{{ confirmTitle }}</h3><button class="modal-close" @click="showConfirm = false"><svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2"><path d="M18 6L6 18M6 6l12 12"/></svg></button></div>
            <div class="modal-body">
              <div class="confirm-info"><div class="ci-row"><span>隐患</span><span>{{ currentHazard?.hazardNo }} {{ currentHazard?.title }}</span></div></div>
              <div class="form-g"><label>备注</label><input v-model="confirmNote" class="form-input" placeholder="可选" /></div>
            </div>
            <div class="modal-footer">
              <button class="modal-btn cancel" @click="showConfirm = false">取消</button>
              <button class="modal-btn primary" :class="confirmAction === 'approveClose' ? '' : confirmAction === 'returnRectify' ? 'orange-btn' : ''" @click="doConfirm">{{ confirmButtonText }}</button>
            </div>
          </div>
        </div>
      </transition>
    </teleport>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, reactive } from 'vue'

type HazardStatus = 'pending' | 'rectifying' | 'reviewing' | 'closed'

interface Hazard {
  id: number; hazardNo: string; title: string; level: string; labName: string
  reporterName: string; handlerName: string | null; reviewerName: string | null
  status: HazardStatus; statusName: string; reportTime: string; deadline: string | null
  description: string | null; rectifyNote: string | null; reviewOpinion: string | null
}

const handlerOptions = [{ id:1, name:'李老师' }, { id:2, name:'孙老师' }, { id:3, name:'周老师' }]

const hazards = ref<Hazard[]>([
  { id:1, hazardNo:'HZ-2024-001', title:'浓硫酸存放柜锁具损坏', level:'high', labName:'A305 分析化学实验室', reporterName:'王管理员', handlerName:null, reviewerName:null, status:'pending', statusName:'待分派', reportTime:'2026-07-08 10:30', deadline:null, description:'锁具损坏，未执行双人双锁管理制度，存在危化品流失风险', rectifyNote:null, reviewOpinion:null },
  { id:2, hazardNo:'HZ-2024-002', title:'物理实验室插座过热', level:'medium', labName:'B103 物理实验室', reporterName:'张同学', handlerName:null, reviewerName:null, status:'pending', statusName:'待分派', reportTime:'2026-07-08 14:00', deadline:null, description:'西侧墙面插座使用时有焦糊味，外壳变色', rectifyNote:null, reviewOpinion:null },
  { id:3, hazardNo:'HZ-2024-003', title:'通风橱风量不足', level:'medium', labName:'A305 分析化学实验室', reporterName:'孙老师', handlerName:'李老师', reviewerName:null, status:'rectifying', statusName:'整改中', reportTime:'2026-07-05 09:00', deadline:'2026-07-12', description:'通风橱排风量低于标准值', rectifyNote:'已联系厂家检测风机，待更换配件', reviewOpinion:null },
  { id:4, hazardNo:'HZ-2024-004', title:'生物实验室紫外线灯故障', level:'medium', labName:'D108 生物培养实验室', reporterName:'周老师', handlerName:'孙老师', reviewerName:null, status:'rectifying', statusName:'整改中', reportTime:'2026-07-06 11:00', deadline:'2026-07-10', description:'紫外灭菌灯管不亮', rectifyNote:null, reviewOpinion:null },
  { id:5, hazardNo:'HZ-2024-005', title:'计算机实验室网线裸露', level:'low', labName:'C202 计算机实验室', reporterName:'赵同学', handlerName:'李老师', reviewerName:null, status:'rectifying', statusName:'整改中', reportTime:'2026-07-07 08:00', deadline:'2026-07-09', description:'地面走线槽破损，网线外露', rectifyNote:'已用线槽盖板临时覆盖，待更换新线槽', reviewOpinion:null },
  { id:6, hazardNo:'HZ-2024-006', title:'灭火器过期', level:'high', labName:'B103 物理实验室', reporterName:'王管理员', handlerName:'周老师', reviewerName:null, status:'reviewing', statusName:'待复核', reportTime:'2026-07-03 15:00', deadline:'2026-07-05', description:'2具干粉灭火器已过有效期', rectifyNote:'已更换全新灭火器2具', reviewOpinion:null },
  { id:7, hazardNo:'HZ-2024-007', title:'洗眼器水压不足', level:'low', labName:'A305 分析化学实验室', reporterName:'李老师', handlerName:'孙老师', reviewerName:'王管理员', status:'closed', statusName:'已闭环', reportTime:'2026-07-01 10:00', deadline:'2026-07-03', description:'紧急洗眼器出水压力偏低', rectifyNote:'已检修管道并更换阀门', reviewOpinion:'验收合格，水压恢复正常' },
])

const activeStatus = ref<HazardStatus | 'all'>('all')
const filterLevel = ref('')
const pageNum = ref(1)
const pageSize = 6

const statusTabs = computed(() => [
  { key:'all' as const, label:'全部', count:hazards.value.length },
  { key:'pending' as const, label:'待分派', count:hazards.value.filter(h => h.status === 'pending').length },
  { key:'rectifying' as const, label:'整改中', count:hazards.value.filter(h => h.status === 'rectifying').length },
  { key:'reviewing' as const, label:'待复核', count:hazards.value.filter(h => h.status === 'reviewing').length },
  { key:'closed' as const, label:'已闭环', count:hazards.value.filter(h => h.status === 'closed').length },
])

const filteredList = computed(() => {
  let list = hazards.value
  if (activeStatus.value !== 'all') list = list.filter(h => h.status === activeStatus.value)
  if (filterLevel.value) list = list.filter(h => h.level === filterLevel.value)
  return list
})
const totalPages = computed(() => Math.ceil(filteredList.value.length / pageSize) || 1)
const pagedList = computed(() => filteredList.value.slice((pageNum.value - 1) * pageSize, pageNum.value * pageSize))

function statusCount(s: HazardStatus) { return hazards.value.filter(h => h.status === s).length }
function levelLabel(l: string) { const m: Record<string,string> = { high:'高', medium:'中', low:'低' }; return m[l] || l }

// 分派弹窗
const showAssign = ref(false)
const currentHazard = ref<Hazard | null>(null)
const assignForm = reactive({ handlerId:0, deadline:'', note:'' })
function openAssign(h: Hazard) { currentHazard.value = h; assignForm.handlerId = 0; assignForm.deadline = ''; assignForm.note = ''; showAssign.value = true }
function doAssign() {
  if (!currentHazard.value || !assignForm.handlerId) return
  const h = currentHazard.value
  h.handlerName = handlerOptions.find(p => p.id === assignForm.handlerId)?.name || ''
  h.deadline = assignForm.deadline; h.status = 'rectifying'; h.statusName = '整改中'
  showAssign.value = false
}

// 确认弹窗（提交复核/退回/复核通过 共用）
const showConfirm = ref(false)
const confirmAction = ref('')
const confirmNote = ref('')
const confirmTitle = computed(() => confirmAction.value === 'submitReview' ? '提交复核' : confirmAction.value === 'returnRectify' ? '退回整改' : '复核通过')
const confirmButtonText = computed(() => confirmAction.value === 'submitReview' ? '提交复核' : confirmAction.value === 'returnRectify' ? '确认退回' : '确认闭环')

function submitReview(h: Hazard) { currentHazard.value = h; confirmAction.value = 'submitReview'; confirmNote.value = ''; showConfirm.value = true }
function returnRectify(h: Hazard) { currentHazard.value = h; confirmAction.value = 'returnRectify'; confirmNote.value = ''; showConfirm.value = true }
function approveClose(h: Hazard) { currentHazard.value = h; confirmAction.value = 'approveClose'; confirmNote.value = ''; showConfirm.value = true }
function doConfirm() {
  if (!currentHazard.value) return
  const h = currentHazard.value
  if (confirmAction.value === 'submitReview') { h.status = 'reviewing'; h.statusName = '待复核'; h.rectifyNote = confirmNote.value || h.rectifyNote }
  else if (confirmAction.value === 'returnRectify') { h.status = 'rectifying'; h.statusName = '整改中' }
  else if (confirmAction.value === 'approveClose') { h.status = 'closed'; h.statusName = '已闭环'; h.reviewerName = '王管理员'; h.reviewOpinion = confirmNote.value || '验收合格' }
  showConfirm.value = false
}
</script>

<style scoped>
.sh-page { height:100%; overflow-y:auto; padding:20px 24px }
.page-header { margin-bottom:16px }
.page-header h2 { font-size:20px; font-weight:600; color:#1a1a2e; margin:0 }
.page-header p { margin:4px 0 0; font-size:13px; color:#999 }

.stats-row { display:flex; gap:12px; margin-bottom:16px; flex-wrap:wrap }
.stat-card { background:rgba(255,255,255,.65); backdrop-filter:blur(10px); border:1px solid rgba(0,0,0,.06); border-radius:12px; padding:14px 20px; min-width:100px; text-align:center }
.stat-num { font-size:24px; font-weight:700; color:#1a1a2e; line-height:1.1 }
.stat-label { font-size:12px; color:#999; margin-top:2px }
.blue { color:#1a73e8 } .red { color:#e74c3c } .orange { color:#f39c12 } .green { color:#27ae60 }

.filter-bar { display:flex; align-items:center; justify-content:space-between; margin-bottom:18px; gap:12px; flex-wrap:wrap }
.filter-tabs { display:flex; gap:4px; background:rgba(255,255,255,.4); border-radius:10px; padding:4px }
.filter-tab { padding:8px 16px; border-radius:8px; font-size:13px; color:#666; cursor:pointer; transition:all .15s; display:flex; align-items:center; gap:6px; user-select:none; white-space:nowrap }
.filter-tab:hover { color:#333; background:rgba(0,0,0,.03) }
.filter-tab.active { background:#fff; color:#1a73e8; font-weight:600; box-shadow:0 1px 3px rgba(0,0,0,.08) }
.tab-count { font-size:11px; background:rgba(0,0,0,.06); padding:1px 7px; border-radius:10px; color:#888 }
.filter-tab.active .tab-count { background:rgba(26,115,232,.1); color:#1a73e8 }
.tool-select { padding:8px 12px; border:1px solid rgba(0,0,0,.1); border-radius:10px; font-size:13px; background:rgba(255,255,255,.7); outline:none; cursor:pointer }

.card-list { display:flex; flex-direction:column; gap:12px }
.hazard-card { background:rgba(255,255,255,.55); backdrop-filter:blur(10px); border-radius:12px; border:1px solid rgba(0,0,0,.04); display:flex; align-items:stretch; overflow:hidden }
.hazard-card.card-pending { border-left:3px solid #e74c3c }
.hazard-card.card-rectifying { border-left:3px solid #f39c12 }
.hazard-card.card-reviewing { border-left:3px solid #27ae60 }
.hazard-card.card-closed { border-left:3px solid #ccc; opacity:.75 }

.card-left { flex:1; padding:16px 20px; min-width:0 }
.card-right { display:flex; flex-direction:column; align-items:flex-end; justify-content:center; gap:8px; padding:16px 20px; flex-shrink:0; min-width:130px; border-left:1px solid rgba(0,0,0,.04) }

.card-header { display:flex; align-items:center; gap:8px; margin-bottom:8px }
.level-tag { padding:2px 6px; border-radius:3px; font-size:10px; font-weight:700; color:#fff }
.level-high { background:#e74c3c } .level-medium { background:#f39c12 } .level-low { background:#f1c40f; color:#7d6608 }
.status-badge { padding:2px 8px; border-radius:4px; font-size:11px; font-weight:600 }
.badge-pending { background:rgba(231,76,60,.1); color:#e74c3c }
.badge-rectifying { background:rgba(243,156,18,.1); color:#e67e22 }
.badge-reviewing { background:rgba(39,174,96,.1); color:#27ae60 }
.badge-closed { background:rgba(0,0,0,.05); color:#999 }
.hazard-no { font-size:13px; font-weight:600; color:#1a1a2e }
.hazard-time { font-size:12px; color:#999; margin-left:auto }

.card-body { display:flex; flex-direction:column; gap:5px }
.card-row { display:flex; align-items:baseline; gap:6px; font-size:13px; flex-wrap:wrap }
.title-row { margin-bottom:2px }
.hazard-title { font-weight:600; color:#1a1a2e }
.ci-label { color:#999; min-width:44px; flex-shrink:0 }
.ci-value { color:#333 }
.ci-sep { color:#ddd }
.deadline { color:#e67e22; font-weight:500 }
.desc-row { }
.desc-text { color:#666; max-width:500px; overflow:hidden; text-overflow:ellipsis }
.rectify-note { color:#888; font-size:12px }

.btn-primary { padding:9px 20px; border:none; border-radius:10px; background:linear-gradient(135deg,#1a73e8,#4a90d9); color:#fff; font-size:13px; font-weight:500; cursor:pointer; white-space:nowrap }
.btn-primary:hover { box-shadow:0 4px 12px rgba(26,115,232,.3) }
.btn-primary.green { background:linear-gradient(135deg,#27ae60,#2ecc71) }
.btn-primary.green:hover { box-shadow:0 4px 12px rgba(39,174,96,.3) }
.btn-outline { padding:9px 20px; border:1px solid rgba(0,0,0,.12); border-radius:10px; background:#fff; color:#555; font-size:13px; cursor:pointer; white-space:nowrap }
.btn-outline.orange { border-color:rgba(243,156,18,.3); color:#e67e22 }
.btn-outline.orange:hover { border-color:#e67e22; background:rgba(243,156,18,.04) }
.final-text { font-size:13px; color:#999 }

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
.modal-btn.primary { background:linear-gradient(135deg,#1a73e8,#4a90d9); color:#fff }
.modal-btn.primary:hover:not(:disabled) { box-shadow:0 4px 12px rgba(26,115,232,.3) }
.modal-btn.primary.orange-btn { background:linear-gradient(135deg,#e67e22,#f39c12) }
.modal-btn:disabled { opacity:.4; cursor:not-allowed }
.confirm-info { background:rgba(0,0,0,.02); border-radius:10px; padding:12px 16px }
.ci-row { display:flex; justify-content:space-between; padding:4px 0; font-size:13px; color:#555 }
.ci-row span:first-child { color:#999 }
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
