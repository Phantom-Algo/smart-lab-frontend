<template>
  <div class="apply-page">
    <div class="page-header">
      <div>
        <h2>领用申请</h2>
        <p>申请领用耗材或药剂</p>
      </div>
      <button class="back-link" @click="goBack">
        <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2"><path d="M19 12H5M12 19l-7-7 7-7"/></svg>
        返回耗材查询
      </button>
    </div>

    <div class="apply-layout">
      <!-- ========== 左侧：新建申请表单 ========== -->
      <div class="form-panel">
        <h3 class="panel-title">📝 新建领用申请</h3>

        <!-- 已选耗材列表 -->
        <div class="form-section">
          <div class="section-header">
            <span class="section-label">申请耗材</span>
            <button class="add-item-btn" @click="showPicker = true">+ 添加耗材</button>
          </div>

          <div class="selected-items" v-if="selectedItems.length > 0">
            <div v-for="(item, idx) in selectedItems" :key="item.consumableId" class="selected-row">
              <div class="selected-info">
                <span class="selected-name">{{ item.name }}</span>
                <span class="selected-spec">{{ item.specification }}</span>
              </div>
              <div class="selected-qty">
                <button class="qty-btn" @click="adjustQty(idx, -1)" :disabled="item.quantity <= 1">−</button>
                <input type="number" v-model.number="item.quantity" class="qty-input" min="1" :max="item.availableStock" @change="clampQty(idx)" />
                <button class="qty-btn" @click="adjustQty(idx, 1)" :disabled="item.quantity >= item.availableStock">+</button>
                <span class="qty-unit">{{ item.unit }}</span>
              </div>
              <span class="stock-hint">库存 {{ item.availableStock }}</span>
              <button class="remove-btn" @click="removeItem(idx)" title="移除">
                <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2"><path d="M18 6L6 18M6 6l12 12"/></svg>
              </button>
            </div>
          </div>
          <div v-else class="empty-selection">
            <span>暂未选择耗材，点击「+ 添加耗材」开始</span>
          </div>
        </div>

        <!-- 申请用途 -->
        <div class="form-section">
          <span class="section-label">申请用途 <i style="color:#e74c3c">*</i></span>
          <input v-model="purpose" type="text" class="form-input" placeholder="例如：有机化学实验课 - 酯化反应" maxlength="200" />
        </div>

        <!-- 预计使用时间 -->
        <div class="form-section">
          <span class="section-label">预计使用时间</span>
          <input v-model="expectedDate" type="date" class="form-input" />
        </div>

        <!-- 备注 -->
        <div class="form-section">
          <span class="section-label">备注</span>
          <textarea v-model="remark" class="form-textarea" placeholder="其他需要说明的信息（选填）" rows="2" maxlength="500"></textarea>
        </div>

        <!-- 提交 -->
        <div class="form-actions">
          <button class="submit-btn" :disabled="!canSubmit" @click="submitApply">
            {{ submitting ? '提交中...' : '提交申请' }}
          </button>
          <span class="form-hint" v-if="!canSubmit && selectedItems.length === 0">请先添加需要领用的耗材</span>
          <span class="form-hint" v-else-if="!canSubmit && !purpose.trim()">请填写申请用途</span>
        </div>
      </div>

      <!-- ========== 右侧：申请记录 ========== -->
      <div class="record-panel">
        <h3 class="panel-title">📋 我的申请记录 <span class="record-count">{{ records.length }} 条</span></h3>

        <div class="record-list" v-if="records.length > 0">
          <div v-for="rec in records" :key="rec.id" class="record-card" :class="{ expanded: expandedId === rec.id }" @click="toggleRecord(rec.id)">
            <div class="record-top">
              <div class="record-header">
                <span class="record-no">{{ rec.applyNo }}</span>
                <span class="record-status" :class="'status-' + rec.status">{{ statusLabel(rec.status) }}</span>
              </div>
              <div class="record-summary">{{ itemSummary(rec) }}</div>
              <div class="record-meta">
                <span>{{ rec.createdAt }}</span>
                <span class="record-toggle">{{ expandedId === rec.id ? '收起 ▲' : '详情 ▼' }}</span>
              </div>
            </div>

            <!-- 展开详情 -->
            <div class="record-detail" v-if="expandedId === rec.id" @click.stop>
              <div class="detail-row" v-if="rec.purpose">
                <span class="dl">用途</span><span class="dv">{{ rec.purpose }}</span>
              </div>
              <div class="detail-row" v-if="rec.expectedDate">
                <span class="dl">预计使用</span><span class="dv">{{ rec.expectedDate }}</span>
              </div>
              <div class="detail-row" v-if="rec.remark">
                <span class="dl">备注</span><span class="dv">{{ rec.remark }}</span>
              </div>
              <div class="detail-table">
                <table>
                  <thead><tr><th>耗材</th><th>规格</th><th>申请量</th></tr></thead>
                  <tbody>
                    <tr v-for="it in rec.items" :key="it.name">
                      <td>{{ it.name }}</td><td>{{ it.spec }}</td><td>{{ it.qty }} {{ it.unit }}</td>
                    </tr>
                  </tbody>
                </table>
              </div>
              <div class="detail-row" v-if="rec.reviewerComment">
                <span class="dl">审批意见</span><span class="dv" style="color:#e74c3c">{{ rec.reviewerComment }}</span>
              </div>
            </div>
          </div>
        </div>

        <div v-else class="empty-records">暂无申请记录</div>
      </div>
    </div>

    <!-- ===== 添加耗材弹窗 ===== -->
    <teleport to="body">
      <transition name="modal-fade">
        <div v-if="showPicker" class="modal-overlay" @click.self="showPicker = false">
          <div class="picker-panel">
            <div class="picker-header">
              <h3>选择耗材</h3>
              <button class="modal-close" @click="showPicker = false">
                <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2"><path d="M18 6L6 18M6 6l12 12"/></svg>
              </button>
            </div>
            <div class="picker-body">
              <div class="picker-search">
                <svg viewBox="0 0 24 24" width="15" height="15" fill="none" stroke="currentColor" stroke-width="2"><circle cx="11" cy="11" r="8"/><path d="M21 21l-4.35-4.35"/></svg>
                <input v-model="pickerSearch" type="text" class="search-input" placeholder="搜索耗材..." />
              </div>
              <div class="picker-list">
                <div v-for="c in pickerList" :key="c.id" class="picker-item" :class="{ picked: isPicked(c.id) }" @click="togglePick(c)">
                  <div class="picker-info">
                    <span class="picker-name">{{ c.name }}</span>
                    <span class="picker-spec">{{ c.specification }} · 库存 {{ c.totalStock }}</span>
                  </div>
                  <span class="picker-check" v-if="isPicked(c.id)">✓</span>
                  <span class="picker-add" v-else>+</span>
                </div>
                <div v-if="pickerList.length === 0" class="empty-hint">未找到匹配耗材</div>
              </div>
            </div>
            <div class="picker-footer">
              <span class="picker-count">已选 {{ pickedIds.size }} 种</span>
              <button class="modal-btn cancel" @click="showPicker = false">取消</button>
              <button class="modal-btn primary" @click="confirmPick">确认添加</button>
            </div>
          </div>
        </div>
      </transition>
    </teleport>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed } from 'vue'

const emit = defineEmits<{ navigate: [payload: { key: string; name: string }] }>()

// ========== 表单状态 ==========
const purpose = ref('')
const expectedDate = ref('')
const remark = ref('')
const submitting = ref(false)
const showPicker = ref(false)
const pickerSearch = ref('')
const expandedId = ref<number | null>(null)

interface SelectedItem {
  consumableId: number; name: string; specification: string; unit: string
  quantity: number; availableStock: number
}
const selectedItems = reactive<SelectedItem[]>([])

// ========== 可申请的耗材池（同 ConsumableSearch 的数据） ==========
const allConsumables = [
  { id: 1, name: '无水乙醇', specification: '分析纯 500ml/瓶', unit: '瓶', totalStock: 45 },
  { id: 2, name: '硫酸铜', specification: '分析纯 500g/瓶', unit: '瓶', totalStock: 5 },
  { id: 3, name: '烧杯', specification: '250ml 高硼硅', unit: '个', totalStock: 120 },
  { id: 4, name: '定量滤纸', specification: 'Φ12.5cm 中速/盒', unit: '盒', totalStock: 3 },
  { id: 5, name: '丁腈手套', specification: 'M号 100只/盒', unit: '盒', totalStock: 200 },
  { id: 6, name: '琼脂粉', specification: '250g/瓶', unit: '瓶', totalStock: 8 },
  { id: 7, name: 'pH试纸', specification: 'pH 1-14 广泛试纸/盒', unit: '盒', totalStock: 60 },
  { id: 8, name: '试管刷', specification: '中号 尼龙毛', unit: '个', totalStock: 35 },
]

const pickerList = computed(() => {
  if (!pickerSearch.value) return allConsumables
  const kw = pickerSearch.value.toLowerCase()
  return allConsumables.filter(c => c.name.toLowerCase().includes(kw))
})

// 弹窗中暂选的 ID 集合
const pickedIds = ref<Set<number>>(new Set())
function isPicked(id: number) { return pickedIds.value.has(id) }
function togglePick(c: typeof allConsumables[0]) {
  const s = new Set(pickedIds.value)
  if (s.has(c.id)) s.delete(c.id); else s.add(c.id)
  pickedIds.value = s
}
function confirmPick() {
  for (const id of pickedIds.value) {
    if (selectedItems.find(i => i.consumableId === id)) continue
    const c = allConsumables.find(x => x.id === id)
    if (c) selectedItems.push({ consumableId: c.id, name: c.name, specification: c.specification, unit: c.unit, quantity: 1, availableStock: c.totalStock })
  }
  pickedIds.value = new Set()
  showPicker.value = false
  pickerSearch.value = ''
}

function removeItem(idx: number) { selectedItems.splice(idx, 1) }
function adjustQty(idx: number, delta: number) {
  const item = selectedItems[idx]
  item.quantity = Math.max(1, Math.min(item.availableStock, item.quantity + delta))
}
function clampQty(idx: number) {
  const item = selectedItems[idx]
  if (!Number.isFinite(item.quantity) || item.quantity < 1) item.quantity = 1
  if (item.quantity > item.availableStock) item.quantity = item.availableStock
}

const canSubmit = computed(() => selectedItems.length > 0 && purpose.value.trim().length > 0 && !submitting.value)

function submitApply() {
  if (!canSubmit.value) return
  submitting.value = true
  const newRec: ApplicationRecord = {
    id: Date.now(),
    applyNo: 'APP-' + new Date().toISOString().slice(0,10).replace(/-/g,'') + '-' + String(records.value.length + 1).padStart(3,'0'),
    status: 'pending',
    items: selectedItems.map(i => ({ name: i.name, spec: i.specification, qty: i.quantity, unit: i.unit })),
    purpose: purpose.value.trim(),
    expectedDate: expectedDate.value,
    remark: remark.value.trim(),
    createdAt: new Date().toLocaleString('zh-CN', { year:'numeric', month:'2-digit', day:'2-digit', hour:'2-digit', minute:'2-digit' }),
  }
  records.value.unshift(newRec)
  // 重置表单
  selectedItems.splice(0)
  purpose.value = ''
  expectedDate.value = ''
  remark.value = ''
  setTimeout(() => { submitting.value = false }, 500)
}

// ========== 申请记录 ==========
interface RecordItem { name: string; spec: string; qty: number; unit: string }
interface ApplicationRecord {
  id: number; applyNo: string; status: 'pending' | 'approved' | 'rejected' | 'received'
  items: RecordItem[]; purpose: string; expectedDate: string; remark: string
  createdAt: string; reviewerComment?: string
}

const records = ref<ApplicationRecord[]>([
  { id: 101, applyNo: 'APP-20260708-001', status: 'pending',
    items: [{ name: '无水乙醇', spec: '分析纯 500ml/瓶', qty: 2, unit: '瓶' }, { name: '烧杯', spec: '250ml 高硼硅', qty: 5, unit: '个' }],
    purpose: '有机化学实验 - 乙酸乙酯制备', expectedDate: '2026-07-12', remark: '',
    createdAt: '2026-07-08 14:30' },
  { id: 102, applyNo: 'APP-20260705-001', status: 'approved',
    items: [{ name: '琼脂粉', spec: '250g/瓶', qty: 3, unit: '瓶' }],
    purpose: '微生物实验 - 培养基配制', expectedDate: '2026-07-06', remark: '需冷藏保存',
    createdAt: '2026-07-05 09:15', reviewerComment: '同意领用，请到A201领取' },
  { id: 103, applyNo: 'APP-20260701-001', status: 'rejected',
    items: [{ name: '硫酸铜', spec: '分析纯 500g/瓶', qty: 8, unit: '瓶' }],
    purpose: '无机化学实验', expectedDate: '2026-07-03', remark: '',
    createdAt: '2026-07-01 10:00', reviewerComment: '库存不足，目前仅剩5瓶，请调整申请量后重新提交' },
])

function statusLabel(s: string) {
  const m: Record<string,string> = { pending:'审批中', approved:'已通过', rejected:'已驳回', received:'已领用' }
  return m[s] || s
}
function itemSummary(rec: ApplicationRecord) {
  return rec.items.map(i => i.name + ' ' + i.qty + i.unit).join('、')
}
function toggleRecord(id: number) {
  expandedId.value = expandedId.value === id ? null : id
}
function goBack() {
  emit('navigate', { key: 'consumable-search', name: '耗材查询' })
}
</script>

<style scoped>
.apply-page { height:100%; display:flex; flex-direction:column; padding:20px 24px }
.page-header { display:flex; align-items:center; justify-content:space-between; margin-bottom:16px; flex-shrink:0 }
.page-header h2 { margin:0; font-size:20px; font-weight:600; color:#1a1a2e }
.page-header p { margin:4px 0 0; font-size:13px; color:#999 }
.back-link { display:flex; align-items:center; gap:6px; background:none; border:none; color:#1a73e8; font-size:13px; cursor:pointer; padding:8px 12px; border-radius:8px; transition:.15s }
.back-link:hover { background:rgba(26,115,232,.06) }

.apply-layout { flex:1; display:grid; grid-template-columns:1fr 1fr; gap:20px; overflow:hidden }

/* ===== 表单面板 ===== */
.form-panel { background:rgba(255,255,255,.65); backdrop-filter:blur(10px); border:1px solid rgba(0,0,0,.06); border-radius:14px; padding:20px 24px; overflow-y:auto; display:flex; flex-direction:column; gap:14px }
.panel-title { font-size:15px; font-weight:600; color:#1a1a2e; margin:0 0 4px }

.form-section { display:flex; flex-direction:column; gap:8px }
.section-header { display:flex; align-items:center; justify-content:space-between }
.section-label { font-size:13px; font-weight:500; color:#555 }
.add-item-btn { padding:6px 14px; border:1px dashed rgba(26,115,232,.4); border-radius:8px; background:rgba(26,115,232,.04); color:#1a73e8; font-size:12px; cursor:pointer; transition:.15s }
.add-item-btn:hover { background:rgba(26,115,232,.1); border-style:solid }

/* 已选耗材 */
.selected-items { display:flex; flex-direction:column; gap:6px }
.selected-row { display:flex; align-items:center; gap:10px; padding:10px 12px; background:rgba(0,0,0,.02); border-radius:10px; flex-wrap:wrap }
.selected-info { flex:1; min-width:0; display:flex; flex-direction:column; gap:1px }
.selected-name { font-size:14px; font-weight:500; color:#222 }
.selected-spec { font-size:11px; color:#999 }
.selected-qty { display:flex; align-items:center; gap:4px }
.qty-btn { width:28px; height:28px; border:1px solid rgba(0,0,0,.12); border-radius:6px; background:#fff; font-size:14px; cursor:pointer; display:flex; align-items:center; justify-content:center; color:#555; transition:.1s }
.qty-btn:hover:not(:disabled) { border-color:#1a73e8; color:#1a73e8 }
.qty-btn:disabled { opacity:.35; cursor:not-allowed }
.qty-input { width:48px; height:28px; border:1px solid rgba(0,0,0,.12); border-radius:6px; text-align:center; font-size:13px; outline:none; -moz-appearance:textfield }
.qty-input::-webkit-inner-spin-button,.qty-input::-webkit-outer-spin-button { -webkit-appearance:none }
.qty-unit { font-size:12px; color:#888; width:20px }
.stock-hint { font-size:11px; color:#bbb; white-space:nowrap }
.remove-btn { background:none; border:none; cursor:pointer; color:#ccc; padding:2px; border-radius:4px; transition:.15s; flex-shrink:0 }
.remove-btn:hover { color:#e74c3c; background:rgba(231,76,60,.06) }

.empty-selection { padding:20px; text-align:center; color:#bbb; font-size:13px; background:rgba(0,0,0,.02); border-radius:10px; border:1px dashed rgba(0,0,0,.08) }

/* 表单控件 */
.form-input { padding:10px 14px; border:1px solid rgba(0,0,0,.1); border-radius:10px; font-size:14px; color:#333; outline:none; background:rgba(255,255,255,.7); transition:.15s }
.form-input:focus { border-color:#1a73e8 }
.form-textarea { padding:10px 14px; border:1px solid rgba(0,0,0,.1); border-radius:10px; font-size:14px; color:#333; outline:none; background:rgba(255,255,255,.7); resize:vertical; font-family:inherit; transition:.15s }
.form-textarea:focus { border-color:#1a73e8 }

.form-actions { display:flex; align-items:center; gap:12px; margin-top:4px }
.submit-btn { padding:12px 36px; border:none; border-radius:10px; background:linear-gradient(135deg,#1a73e8,#4a90d9); color:#fff; font-size:14px; font-weight:500; cursor:pointer; transition:all .2s }
.submit-btn:hover:not(:disabled) { box-shadow:0 4px 16px rgba(26,115,232,.3); transform:translateY(-1px) }
.submit-btn:disabled { opacity:.4; cursor:not-allowed }
.form-hint { font-size:12px; color:#e74c3c }

/* ===== 记录面板 ===== */
.record-panel { background:rgba(255,255,255,.65); backdrop-filter:blur(10px); border:1px solid rgba(0,0,0,.06); border-radius:14px; padding:20px 24px; overflow-y:auto; display:flex; flex-direction:column; gap:10px }
.record-count { font-size:12px; font-weight:400; color:#999; margin-left:6px }
.record-list { display:flex; flex-direction:column; gap:8px }
.record-card { background:rgba(0,0,0,.02); border-radius:10px; padding:14px 16px; cursor:pointer; transition:.15s; border:1px solid transparent }
.record-card:hover { background:rgba(0,0,0,.04) }
.record-card.expanded { border-color:rgba(26,115,232,.15); background:rgba(26,115,232,.02) }

.record-header { display:flex; align-items:center; justify-content:space-between; margin-bottom:6px }
.record-no { font-size:13px; font-weight:500; color:#333; font-family:monospace }
.record-status { font-size:11px; font-weight:600; padding:2px 10px; border-radius:10px }
.status-pending { background:rgba(243,156,18,.12); color:#b8730a }
.status-approved { background:rgba(39,174,96,.12); color:#1e8449 }
.status-rejected { background:rgba(231,76,60,.12); color:#b03a2e }
.status-received { background:rgba(26,115,232,.12); color:#1557b0 }

.record-summary { font-size:13px; color:#555; margin-bottom:4px; white-space:nowrap; overflow:hidden; text-overflow:ellipsis }
.record-meta { display:flex; align-items:center; justify-content:space-between; font-size:11px; color:#aaa }
.record-toggle { color:#1a73e8 }

.record-detail { margin-top:12px; padding-top:12px; border-top:1px solid rgba(0,0,0,.05); display:flex; flex-direction:column; gap:8px }
.detail-row { display:flex; gap:8px; font-size:13px }
.dl { color:#999; min-width:56px; flex-shrink:0 }
.dv { color:#333 }

.detail-table { margin-top:4px }
.detail-table table { width:100%; border-collapse:collapse; font-size:12px }
.detail-table th,.detail-table td { padding:6px 10px; text-align:left; border-bottom:1px solid rgba(0,0,0,.04) }
.detail-table th { color:#999; font-weight:500; font-size:11px }

.empty-records { padding:40px 20px; text-align:center; color:#bbb; font-size:13px }
</style>

<!-- ===== 弹窗样式（非 scoped） ===== -->
<style>
.modal-overlay { position:fixed; inset:0; background:rgba(0,0,0,.35); display:flex; align-items:center; justify-content:center; z-index:1100; padding:20px }
.picker-panel { background:#fff; border-radius:18px; width:100%; max-width:520px; max-height:80vh; display:flex; flex-direction:column; box-shadow:0 24px 64px rgba(0,0,0,.18) }
.picker-header { display:flex; align-items:center; justify-content:space-between; padding:20px 24px 0; flex-shrink:0 }
.picker-header h3 { margin:0; font-size:17px; font-weight:600; color:#1a1a2e }
.modal-close { background:none; border:none; cursor:pointer; color:#aaa; padding:4px; border-radius:6px; transition:.15s }
.modal-close:hover { background:rgba(0,0,0,.05); color:#333 }
.picker-body { padding:16px 24px; overflow-y:auto; flex:1 }
.picker-search { display:flex; align-items:center; gap:8px; padding:10px 14px; background:rgba(0,0,0,.03); border-radius:10px; margin-bottom:12px }
.picker-search svg { color:#999; flex-shrink:0 }
.picker-search .search-input { flex:1; border:none; outline:none; background:transparent; font-size:14px; color:#333 }
.picker-list { display:flex; flex-direction:column; gap:4px }
.picker-item { display:flex; align-items:center; justify-content:space-between; padding:12px 14px; border-radius:10px; cursor:pointer; transition:.15s }
.picker-item:hover { background:rgba(0,0,0,.03) }
.picker-item.picked { background:rgba(26,115,232,.06) }
.picker-info { display:flex; flex-direction:column; gap:2px }
.picker-name { font-size:14px; font-weight:500; color:#222 }
.picker-spec { font-size:12px; color:#999 }
.picker-check { width:24px; height:24px; border-radius:50%; background:#1a73e8; color:#fff; display:flex; align-items:center; justify-content:center; font-size:12px; font-weight:700 }
.picker-add { width:24px; height:24px; border-radius:50%; border:2px solid #ddd; color:#bbb; display:flex; align-items:center; justify-content:center; font-size:14px; font-weight:600; transition:.15s }
.picker-item:hover .picker-add { border-color:#1a73e8; color:#1a73e8 }
.picker-footer { display:flex; align-items:center; justify-content:flex-end; gap:10px; padding:16px 24px 20px; border-top:1px solid rgba(0,0,0,.06); flex-shrink:0 }
.picker-count { font-size:13px; color:#888; margin-right:auto }
.modal-btn { padding:10px 24px; border-radius:10px; font-size:14px; cursor:pointer; border:none; transition:all .2s; font-weight:500 }
.modal-btn.cancel { background:rgba(0,0,0,.04); color:#666 }
.modal-btn.cancel:hover { background:rgba(0,0,0,.08) }
.modal-btn.primary { background:linear-gradient(135deg,#1a73e8,#4a90d9); color:#fff }
.modal-btn.primary:hover { box-shadow:0 4px 16px rgba(26,115,232,.3); transform:translateY(-1px) }

.modal-fade-enter-active { transition:all .25s ease }
.modal-fade-leave-active { transition:all .2s ease }
.modal-fade-enter-from,.modal-fade-leave-to { opacity:0 }
.modal-fade-enter-from .picker-panel { transform:scale(.94) translateY(16px) }
.modal-fade-leave-to .picker-panel { transform:scale(.96) translateY(8px) }
</style>
