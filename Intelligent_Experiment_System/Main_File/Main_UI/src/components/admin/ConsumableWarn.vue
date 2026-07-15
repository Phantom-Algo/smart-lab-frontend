<template>
  <div class="cw-page">
    <div class="page-header">
      <h2>库存预警</h2>
      <p>低库存、保质期过期预警</p>
    </div>

    <div class="stats-row">
      <div class="stat-card"><div class="stat-num red">{{ pendingLowStock.length }}</div><div class="stat-label">低库存</div></div>
      <div class="stat-card"><div class="stat-num orange">{{ pendingNearExpiry.length }}</div><div class="stat-label">临期批次</div></div>
      <div class="stat-card"><div class="stat-num red">{{ pendingExpired.length }}</div><div class="stat-label">已过期</div></div>
      <div class="stat-card"><div class="stat-num blue">{{ alerts.length }}</div><div class="stat-label">预警总数</div></div>
    </div>

    <div class="toolbar">
      <div class="filter-tabs">
        <div v-for="t in statusFilters" :key="t.key" class="filter-tab" :class="{ active: activeStatus === t.key }" @click="activeStatus = t.key">
          {{ t.label }}
        </div>
      </div>
    </div>

    <div class="alert-list" v-if="displayList.length > 0">
      <div v-for="a in displayList" :key="a.id" class="alert-card" :class="'alert-' + alertLevel(a)">
        <div class="alert-left">
          <div class="alert-header">
            <span class="alert-type-tag" :class="'type-' + a.alertType">{{ a.alertTypeDesc }}</span>
            <span class="alert-status" :class="a.status === 0 ? 'status-pending' : 'status-done'">{{ a.status === 0 ? '待处理' : '已处理' }}</span>
            <span class="alert-time">{{ a.alertTime?.replace('T', ' ').slice(0, 16) }}</span>
          </div>
          <div class="alert-body">
            <span class="alert-material">{{ a.materialName }}</span>
            <span v-if="a.batchNo" class="alert-batch">批次 {{ a.batchNo }}</span>
          </div>
          <div class="alert-content">{{ a.alertContent }}</div>
        </div>
        <div class="alert-right" v-if="a.status === 0">
          <button class="handle-btn" @click="openHandle(a)">处理</button>
        </div>
      </div>
    </div>
    <div v-else class="empty-state">
      <p>暂无预警记录</p>
    </div>

    <!-- 处理弹窗 -->
    <teleport to="body">
      <transition name="modal-fade">
        <div v-if="showHandle" class="modal-overlay" @click.self="showHandle = false">
          <div class="modal-panel">
            <div class="modal-header">
              <h3>处理预警 - {{ currentAlert?.materialName }}</h3>
              <button class="modal-close" @click="showHandle = false"><svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2"><path d="M18 6L6 18M6 6l12 12"/></svg></button>
            </div>
            <div class="modal-body">
              <div class="form-g">
                <label>处理结果 <span class="required">*</span></label>
                <textarea v-model="handleForm.result" class="form-input form-textarea" rows="3" placeholder="请填写处理结果..." maxlength="255" />
              </div>
            </div>
            <div class="modal-footer">
              <button class="modal-btn cancel" @click="showHandle = false">取消</button>
              <button class="modal-btn primary" @click="doHandle" :disabled="!handleForm.result.trim()">确认处理</button>
            </div>
          </div>
        </div>
      </transition>
    </teleport>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, reactive } from 'vue'
import { listAlerts, handleAlert, type MaterialAlertItem } from '../../utils/materialApi'

const alerts = ref<MaterialAlertItem[]>([])

async function loadData() {
  try {
    const res = await listAlerts({ pageNum: 1, pageSize: 200 })
    alerts.value = res.records
  } catch { /* 请求失败 */ }
}

onMounted(() => loadData())

const activeStatus = ref<number | 'all'>('all')
const statusFilters = [
  { key: 'all' as const, label: '全部' },
  { key: 0 as const, label: '待处理' },
  { key: 1 as const, label: '已处理' },
]

const filteredList = computed(() => {
  let list = alerts.value
  if (activeStatus.value !== 'all') list = list.filter(a => a.status === activeStatus.value)
  return list
})

const displayList = computed(() => filteredList.value)

const pendingLowStock = computed(() => alerts.value.filter(a => a.alertType === 1 && a.status === 0))
const pendingNearExpiry = computed(() => alerts.value.filter(a => a.alertType === 2 && a.status === 0))
const pendingExpired = computed(() => alerts.value.filter(a => a.alertType === 3 && a.status === 0))

function alertLevel(a: MaterialAlertItem): string {
  if (a.alertType === 3) return 'danger'
  if (a.alertType === 2) return 'warn'
  if (a.alertType === 1) return 'danger'
  return 'info'
}

// 处理弹窗
const showHandle = ref(false)
const currentAlert = ref<MaterialAlertItem | null>(null)
const handleForm = reactive({ result: '' })

function openHandle(a: MaterialAlertItem) {
  currentAlert.value = a
  handleForm.result = ''
  showHandle.value = true
}

async function doHandle() {
  if (!currentAlert.value || !handleForm.result.trim()) return
  try {
    await handleAlert(currentAlert.value.id, handleForm.result.trim())
    showHandle.value = false
    loadData()
  } catch (e: any) {
    alert(e.message || '操作失败')
  }
}
</script>

<style scoped>
.cw-page { height:100%; overflow-y:auto; padding:20px 24px }
.page-header { margin-bottom:16px }
.page-header h2 { font-size:20px; font-weight:600; color:#1a1a2e; margin:0 }
.page-header p { margin:4px 0 0; font-size:13px; color:#999 }

.stats-row { display:flex; gap:12px; margin-bottom:16px; flex-wrap:wrap }
.stat-card { background:rgba(255,255,255,.65); backdrop-filter:blur(10px); border:1px solid rgba(0,0,0,.06); border-radius:12px; padding:14px 20px; min-width:100px; text-align:center }
.stat-num { font-size:24px; font-weight:700; color:#1a1a2e; line-height:1.1 }
.stat-label { font-size:12px; color:#999; margin-top:2px }
.red { color:#e74c3c } .orange { color:#f39c12 } .blue { color:#1a73e8 }

.toolbar { margin-bottom:14px }
.filter-tabs { display:inline-flex; gap:4px; background:rgba(255,255,255,.4); border-radius:10px; padding:4px }
.filter-tab { padding:8px 16px; border-radius:8px; font-size:13px; color:#666; cursor:pointer; transition:all .15s; user-select:none }
.filter-tab:hover { color:#333; background:rgba(0,0,0,.03) }
.filter-tab.active { background:#fff; color:#1a73e8; font-weight:600; box-shadow:0 1px 3px rgba(0,0,0,.08) }

.alert-list { display:flex; flex-direction:column; gap:8px }
.alert-card { background:rgba(255,255,255,.55); backdrop-filter:blur(10px); border-radius:12px; border:1px solid rgba(0,0,0,.04); display:flex; align-items:center; overflow:hidden }
.alert-card.alert-danger { border-left:3px solid #e74c3c }
.alert-card.alert-warn { border-left:3px solid #f39c12 }
.alert-card.alert-info { border-left:3px solid #1a73e8 }

.alert-left { flex:1; padding:14px 18px; min-width:0 }
.alert-header { display:flex; align-items:center; gap:8px; margin-bottom:6px }
.alert-type-tag { padding:2px 8px; border-radius:4px; font-size:11px; font-weight:600 }
.type-1 { background:rgba(231,76,60,.1); color:#c0392b }
.type-2 { background:rgba(243,156,18,.1); color:#b8730a }
.type-3 { background:rgba(0,0,0,.06); color:#888 }
.type-4 { background:rgba(142,68,173,.1); color:#8e44ad }
.alert-status { font-size:11px; padding:1px 6px; border-radius:3px }
.status-pending { background:rgba(243,156,18,.1); color:#b8730a }
.status-done { background:rgba(39,174,96,.1); color:#1e8449 }
.alert-time { font-size:11px; color:#aaa; margin-left:auto }

.alert-body { display:flex; align-items:center; gap:8px; margin-bottom:4px }
.alert-material { font-size:14px; font-weight:600; color:#1a1a2e }
.alert-batch { font-size:11px; background:rgba(0,0,0,.04); padding:1px 6px; border-radius:3px; color:#888 }

.alert-content { font-size:12px; color:#888; line-height:1.5 }

.alert-right { padding:14px 18px; flex-shrink:0 }
.handle-btn { padding:8px 18px; border:none; border-radius:8px; background:linear-gradient(135deg,#1a73e8,#4a90d9); color:#fff; font-size:13px; cursor:pointer }
.handle-btn:hover { box-shadow:0 4px 12px rgba(26,115,232,.3) }

.empty-state { display:flex; align-items:center; justify-content:center; padding:60px 0; color:#bbb }
.empty-state p { margin:0 }

@media (max-width: 768px) { .alert-card { flex-direction:column; align-items:stretch } }
</style>

<style>
.modal-overlay { position:fixed; inset:0; background:rgba(0,0,0,.35); display:flex; align-items:center; justify-content:center; z-index:1100; padding:20px }
.modal-panel { background:#fff; border-radius:16px; width:100%; max-width:480px; max-height:85vh; display:flex; flex-direction:column; box-shadow:0 16px 48px rgba(0,0,0,.15) }
.modal-header { display:flex; align-items:center; justify-content:space-between; padding:20px 24px 0 }
.modal-header h3 { margin:0; font-size:17px; font-weight:600; color:#1a1a2e }
.modal-close { background:none; border:none; cursor:pointer; color:#aaa; padding:4px; border-radius:6px }
.modal-close:hover { background:rgba(0,0,0,.05); color:#333 }
.modal-body { padding:20px 24px; overflow-y:auto; flex:1 }
.modal-footer { display:flex; justify-content:flex-end; gap:10px; padding:16px 24px 20px; border-top:1px solid rgba(0,0,0,.06) }
.modal-btn { padding:10px 24px; border-radius:10px; font-size:14px; cursor:pointer; border:none; font-weight:500; transition:.2s }
.modal-btn.cancel { background:rgba(0,0,0,.04); color:#666 }
.modal-btn.cancel:hover { background:rgba(0,0,0,.08) }
.modal-btn.primary { background:linear-gradient(135deg,#1a73e8,#4a90d9); color:#fff }
.modal-btn.primary:hover { box-shadow:0 4px 12px rgba(26,115,232,.3) }
.modal-btn:disabled { opacity:.4; cursor:not-allowed }
.form-g { display:flex; flex-direction:column; gap:4px }
.form-g label { font-size:13px; font-weight:500; color:#555 }
.required { color:#e74c3c }
.form-input { padding:10px 14px; border:1px solid rgba(0,0,0,.1); border-radius:10px; font-size:14px; outline:none; background:rgba(255,255,255,.7); font-family:inherit; width:100%; box-sizing:border-box }
.form-input:focus { border-color:#1a73e8 }
.form-textarea { resize:vertical; min-height:60px }
.modal-fade-enter-active { transition:all .2s ease }
.modal-fade-leave-active { transition:all .15s ease }
.modal-fade-enter-from,.modal-fade-leave-to { opacity:0 }
.modal-fade-enter-from .modal-panel { transform:scale(.94) translateY(12px) }
</style>
