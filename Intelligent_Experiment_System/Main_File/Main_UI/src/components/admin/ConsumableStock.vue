<template>
  <div class="cs-page">
    <div class="page-header">
      <h2>耗材库存</h2>
      <p>耗材台账、批次入库、库存查询</p>
    </div>

    <div class="stats-row">
      <div class="stat-card"><div class="stat-num blue">{{ items.length }}</div><div class="stat-label">耗材种类</div></div>
      <div class="stat-card"><div class="stat-num green">{{ totalStock }}</div><div class="stat-label">总库存量</div></div>
      <div class="stat-card"><div class="stat-num red">{{ lowStockCount }}</div><div class="stat-label">低库存</div></div>
      <div class="stat-card"><div class="stat-num orange">{{ nearExpiryCount }}</div><div class="stat-label">临期批次</div></div>
    </div>

    <div class="toolbar">
      <div class="toolbar-left">
        <input v-model="searchName" placeholder="搜索名称..." class="search-input" @keyup.enter="pageNum = 1" />
        <select v-model="filterCat" class="tool-select">
          <option value="">全部分类</option>
          <option v-for="c in catOptions" :key="c.key" :value="c.key">{{ c.label }}</option>
        </select>
      </div>
      <div class="toolbar-right">
        <span class="total-label">共 {{ filteredList.length }} 种耗材</span>
        <button class="add-btn" @click="openItemDialog()">+ 新增耗材</button>
      </div>
    </div>

    <div class="data-table-wrap">
      <table class="data-table">
        <thead><tr><th>编号</th><th>名称</th><th>分类</th><th>规格</th><th>单位</th><th>库存</th><th>安全库存</th><th>状态</th><th>操作</th></tr></thead>
        <tbody>
          <tr v-for="item in pagedList" :key="item.id">
            <td><code>{{ item.itemNo }}</code></td>
            <td><span class="item-name">{{ item.name }}</span></td>
            <td><span class="cat-tag" :class="'cat-' + item.category">{{ item.categoryName }}</span></td>
            <td>{{ item.spec || '--' }}</td>
            <td>{{ item.unit }}</td>
            <td><span :class="item.stock < item.safetyStock ? 'text-red' : ''">{{ item.stock }}</span></td>
            <td>{{ item.safetyStock }}</td>
            <td>
              <div class="stock-bar-wrap">
                <div class="stock-bar"><div class="stock-fill" :class="stockLevel(item)" :style="{ width: stockPct(item) + '%' }"></div></div>
                <span class="stock-text" :class="stockLevel(item)">{{ stockLabel(item) }}</span>
              </div>
            </td>
            <td class="action-cell">
              <button class="row-btn" @click="openBatchDialog(item)">入库</button>
              <button class="row-btn" @click="viewBatches(item)">批次</button>
              <button class="row-btn" @click="openItemDialog(item)">编辑</button>
            </td>
          </tr>
          <tr v-if="pagedList.length === 0"><td colspan="9" class="empty-cell">暂无耗材数据</td></tr>
        </tbody>
      </table>
    </div>

    <div class="pagination" v-if="filteredList.length > pageSize">
      <button :disabled="pageNum <= 1" @click="pageNum--">上一页</button>
      <span>{{ pageNum }} / {{ totalPages }}</span>
      <button :disabled="pageNum >= totalPages" @click="pageNum++">下一页</button>
    </div>

    <!-- 耗材编辑弹窗 -->
    <teleport to="body">
      <transition name="modal-fade">
        <div v-if="showItem" class="modal-overlay" @click.self="showItem = false">
          <div class="modal-panel">
            <div class="modal-header">
              <h3>{{ editingItemId ? '编辑耗材' : '新增耗材' }}</h3>
              <button class="modal-close" @click="showItem = false"><svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2"><path d="M18 6L6 18M6 6l12 12"/></svg></button>
            </div>
            <div class="modal-body">
              <div class="form-row-2">
                <div class="form-g"><label>耗材编号 <span class="required">*</span></label><input v-model="itemForm.itemNo" class="form-input" :disabled="!!editingItemId" /></div>
                <div class="form-g"><label>名称 <span class="required">*</span></label><input v-model="itemForm.name" class="form-input" /></div>
              </div>
              <div class="form-row-2">
                <div class="form-g"><label>分类 <span class="required">*</span></label>
                  <select v-model="itemForm.category" class="form-input"><option v-for="c in catOptions" :key="c.key" :value="c.key">{{ c.label }}</option></select>
                </div>
                <div class="form-g"><label>单位 <span class="required">*</span></label><input v-model="itemForm.unit" class="form-input" placeholder="瓶/盒/包/支" /></div>
              </div>
              <div class="form-row-2">
                <div class="form-g"><label>规格</label><input v-model="itemForm.spec" class="form-input" placeholder="如 500ml/瓶" /></div>
                <div class="form-g"><label>安全库存 <span class="required">*</span></label><input v-model.number="itemForm.safetyStock" type="number" class="form-input" min="0" /></div>
              </div>
              <div class="form-g"><label>描述</label><textarea v-model="itemForm.description" class="form-input form-textarea" rows="2" placeholder="可选" /></div>
            </div>
            <div class="modal-footer">
              <button class="modal-btn cancel" @click="showItem = false">取消</button>
              <button class="modal-btn primary" @click="saveItem" :disabled="!itemForm.name.trim()">保存</button>
            </div>
          </div>
        </div>
      </transition>
    </teleport>

    <!-- 批次入库弹窗 -->
    <teleport to="body">
      <transition name="modal-fade">
        <div v-if="showBatchIn" class="modal-overlay" @click.self="showBatchIn = false">
          <div class="modal-panel">
            <div class="modal-header">
              <h3>批次入库 - {{ batchTarget?.name }}</h3>
              <button class="modal-close" @click="showBatchIn = false"><svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2"><path d="M18 6L6 18M6 6l12 12"/></svg></button>
            </div>
            <div class="modal-body">
              <div class="form-row-2">
                <div class="form-g"><label>批次编号 <span class="required">*</span></label><input v-model="batchForm.batchNo" class="form-input" placeholder="如 B20240709-001" /></div>
                <div class="form-g"><label>入库数量 <span class="required">*</span></label><input v-model.number="batchForm.quantity" type="number" class="form-input" min="1" /></div>
              </div>
              <div class="form-row-2">
                <div class="form-g"><label>生产日期</label><input v-model="batchForm.productionDate" type="date" class="form-input" /></div>
                <div class="form-g"><label>到期日期</label><input v-model="batchForm.expiryDate" type="date" class="form-input" /></div>
              </div>
              <div class="form-g"><label>备注</label><input v-model="batchForm.remark" class="form-input" placeholder="可选" /></div>
            </div>
            <div class="modal-footer">
              <button class="modal-btn cancel" @click="showBatchIn = false">取消</button>
              <button class="modal-btn primary" @click="saveBatch" :disabled="!batchForm.batchNo.trim() || !batchForm.quantity">确认入库</button>
            </div>
          </div>
        </div>
      </transition>
    </teleport>

    <!-- 批次记录弹窗 -->
    <teleport to="body">
      <transition name="modal-fade">
        <div v-if="showBatches && batchTarget" class="modal-overlay" @click.self="showBatches = false">
          <div class="modal-panel wide">
            <div class="modal-header">
              <h3>批次记录 - {{ batchTarget.name }}</h3>
              <button class="modal-close" @click="showBatches = false"><svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2"><path d="M18 6L6 18M6 6l12 12"/></svg></button>
            </div>
            <div class="modal-body">
              <table class="mini-table" v-if="batchTarget.batches.length > 0">
                <thead><tr><th>批次号</th><th>数量</th><th>生产日期</th><th>到期日</th><th>剩余</th><th>状态</th><th>入库时间</th></tr></thead>
                <tbody>
                  <tr v-for="b in batchTarget.batches" :key="b.batchNo">
                    <td><code>{{ b.batchNo }}</code></td>
                    <td>{{ b.quantity }}</td>
                    <td>{{ b.productionDate || '--' }}</td>
                    <td>{{ b.expiryDate || '--' }}</td>
                    <td>{{ b.remaining }}</td>
                    <td><span class="batch-status" :class="batchStatusClass(b)">{{ batchStatusLabel(b) }}</span></td>
                    <td>{{ b.entryDate }}</td>
                  </tr>
                </tbody>
              </table>
              <div v-else class="empty-hint">暂无批次记录</div>
            </div>
            <div class="modal-footer">
              <button class="modal-btn cancel" @click="showBatches = false">关闭</button>
            </div>
          </div>
        </div>
      </transition>
    </teleport>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, reactive, onMounted } from 'vue'
import { getMaterialPage, type MaterialItem } from '../../utils/materialApi'

interface Batch { batchNo: string; quantity: number; productionDate: string; expiryDate: string; remaining: number; entryDate: string }
interface Item {
  id: number; itemNo: string; name: string; category: string; categoryName: string
  spec: string | null; unit: string; stock: number; safetyStock: number
  description: string | null; batches: Batch[]
}

const catOptions = [
  { key:'reagent', label:'试剂' }, { key:'glassware', label:'玻璃器皿' },
  { key:'protective', label:'防护用品' }, { key:'other', label:'其他' },
]
const catNames: Record<string, string> = Object.fromEntries(catOptions.map(c => [c.key, c.label]))

const items = ref<Item[]>([])

async function loadMaterials() {
  try {
    const res = await getMaterialPage({ pageNum: 1, pageSize: 200 })
    items.value = res.records.map(m => ({
      id: m.id, itemNo: m.materialNo, name: m.materialName,
      category: 'other', categoryName: '耗材',
      spec: null, unit: m.unit, stock: m.currentStock, safetyStock: m.safetyStock,
      description: null, batches: []
    }))
  } catch { /* API 不可用 */ }
}
onMounted(() => { loadMaterials() })

const searchName = ref('')
const filterCat = ref('')
const pageNum = ref(1)
const pageSize = 8

const filteredList = computed(() => {
  let list = items.value
  if (searchName.value) list = list.filter(i => i.name.includes(searchName.value))
  if (filterCat.value) list = list.filter(i => i.category === filterCat.value)
  return list
})
const totalPages = computed(() => Math.ceil(filteredList.value.length / pageSize) || 1)
const pagedList = computed(() => filteredList.value.slice((pageNum.value - 1) * pageSize, pageNum.value * pageSize))

const totalStock = computed(() => items.value.reduce((s, i) => s + i.stock, 0))
const lowStockCount = computed(() => items.value.filter(i => i.stock < i.safetyStock).length)
const nearExpiryCount = computed(() => {
  let count = 0
  items.value.forEach(i => i.batches.forEach(b => {
    if (b.expiryDate && new Date(b.expiryDate) <= new Date(Date.now() + 90*86400000)) count++
  }))
  return count
})

function stockLevel(i: Item): string {
  if (i.stock < i.safetyStock) return 'danger'
  if (i.stock < i.safetyStock * 2) return 'warn'
  return 'ok'
}
function stockPct(i: Item): number { return Math.min(100, Math.round(i.stock / (i.safetyStock * 2) * 100)) }
function stockLabel(i: Item): string { return i.stock < i.safetyStock ? '不足' : i.stock < i.safetyStock * 2 ? '偏低' : '充足' }

function batchStatusClass(b: Batch): string {
  if (!b.expiryDate) return 'ok'
  const d = new Date(b.expiryDate)
  if (d <= new Date()) return 'expired'
  if (d <= new Date(Date.now() + 90*86400000)) return 'warn'
  return 'ok'
}
function batchStatusLabel(b: Batch): string {
  if (!b.expiryDate) return '正常'
  const d = new Date(b.expiryDate)
  if (d <= new Date()) return '已过期'
  if (d <= new Date(Date.now() + 90*86400000)) return '临期'
  return '正常'
}

// 耗材编辑弹窗
const showItem = ref(false)
const editingItemId = ref(0)
const itemForm = reactive({ itemNo:'', name:'', category:'reagent', unit:'瓶', spec:'', safetyStock:10, description:'' })
function openItemDialog(item?: Item) {
  if (item) { editingItemId.value = item.id; itemForm.itemNo = item.itemNo; itemForm.name = item.name; itemForm.category = item.category; itemForm.unit = item.unit; itemForm.spec = item.spec || ''; itemForm.safetyStock = item.safetyStock; itemForm.description = item.description || '' }
  else { editingItemId.value = 0; itemForm.itemNo = 'CON-' + String(items.value.length + 1).padStart(3,'0'); itemForm.name = ''; itemForm.category = 'reagent'; itemForm.unit = '瓶'; itemForm.spec = ''; itemForm.safetyStock = 10; itemForm.description = '' }
  showItem.value = true
}
function saveItem() {
  if (!itemForm.name.trim()) return
  if (editingItemId.value) {
    const i = items.value.find(i => i.id === editingItemId.value)
    if (i) { i.name = itemForm.name.trim(); i.category = itemForm.category; i.categoryName = catNames[itemForm.category]; i.unit = itemForm.unit; i.spec = itemForm.spec || null; i.safetyStock = itemForm.safetyStock; i.description = itemForm.description || null }
  } else {
    items.value.unshift({ id:Date.now(), itemNo:itemForm.itemNo, name:itemForm.name.trim(), category:itemForm.category, categoryName:catNames[itemForm.category], spec:itemForm.spec||null, unit:itemForm.unit, stock:0, safetyStock:itemForm.safetyStock, description:itemForm.description||null, batches:[] })
  }
  showItem.value = false
}

// 批次入库弹窗
const showBatchIn = ref(false)
const batchTarget = ref<Item | null>(null)
const batchForm = reactive({ batchNo:'', quantity:0, productionDate:'', expiryDate:'', remark:'' })
function openBatchDialog(item: Item) {
  batchTarget.value = item
  batchForm.batchNo = 'B' + new Date().toISOString().slice(0,10).replace(/-/g,'') + '-' + String(item.batches.length + 1).padStart(3,'0')
  batchForm.quantity = 0; batchForm.productionDate = ''; batchForm.expiryDate = ''; batchForm.remark = ''
  showBatchIn.value = true
}
function saveBatch() {
  if (!batchTarget.value || !batchForm.batchNo.trim() || !batchForm.quantity) return
  const item = batchTarget.value
  item.batches.unshift({ batchNo:batchForm.batchNo.trim(), quantity:batchForm.quantity, productionDate:batchForm.productionDate, expiryDate:batchForm.expiryDate, remaining:batchForm.quantity, entryDate:'2026-07-09 ' + new Date().toTimeString().slice(0,5) })
  item.stock = item.batches.reduce((s, b) => s + b.remaining, 0)
  showBatchIn.value = false
}

// 批次记录弹窗
const showBatches = ref(false)
function viewBatches(item: Item) { batchTarget.value = item; showBatches.value = true }
</script>

<style scoped>
.cs-page { height:100%; overflow-y:auto; padding:20px 24px }
.page-header { margin-bottom:16px }
.page-header h2 { font-size:20px; font-weight:600; color:#1a1a2e; margin:0 }
.page-header p { margin:4px 0 0; font-size:13px; color:#999 }

.stats-row { display:flex; gap:12px; margin-bottom:16px; flex-wrap:wrap }
.stat-card { background:rgba(255,255,255,.65); backdrop-filter:blur(10px); border:1px solid rgba(0,0,0,.06); border-radius:12px; padding:14px 20px; min-width:100px; text-align:center }
.stat-num { font-size:24px; font-weight:700; color:#1a1a2e; line-height:1.1 }
.stat-label { font-size:12px; color:#999; margin-top:2px }
.blue { color:#1a73e8 } .green { color:#27ae60 } .red { color:#e74c3c } .orange { color:#f39c12 }

.toolbar { display:flex; align-items:center; justify-content:space-between; margin-bottom:14px; flex-wrap:wrap; gap:10px }
.toolbar-left { display:flex; align-items:center; gap:8px }
.toolbar-right { display:flex; align-items:center; gap:12px }
.search-input { padding:8px 12px; border:1px solid rgba(0,0,0,.1); border-radius:10px; font-size:13px; outline:none; width:150px; background:rgba(255,255,255,.7) }
.tool-select { padding:8px 12px; border:1px solid rgba(0,0,0,.1); border-radius:10px; font-size:13px; background:rgba(255,255,255,.7); outline:none; cursor:pointer }
.total-label { font-size:13px; color:#999 }
.add-btn { padding:8px 18px; border:none; border-radius:10px; background:linear-gradient(135deg,#1a73e8,#4a90d9); color:#fff; font-size:13px; font-weight:500; cursor:pointer }

.data-table-wrap { background:rgba(255,255,255,.5); border-radius:12px; border:1px solid rgba(0,0,0,.04); overflow-x:auto }
.data-table { width:100%; border-collapse:collapse; min-width:860px }
.data-table th,.data-table td { padding:11px 14px; text-align:left; font-size:13px; border-bottom:1px solid rgba(0,0,0,.04); white-space:nowrap }
.data-table th { font-weight:600; color:#666; background:rgba(0,0,0,.015); font-size:12px }
.item-name { font-weight:600; color:#1a1a2e }
.cat-tag { padding:2px 8px; border-radius:4px; font-size:11px; font-weight:600 }
.cat-reagent { background:rgba(142,68,173,.1); color:#8e44ad }
.cat-glassware { background:rgba(26,115,232,.1); color:#1557b0 }
.cat-protective { background:rgba(39,174,96,.1); color:#1e8449 }
.cat-other { background:rgba(0,0,0,.05); color:#888 }
.text-red { color:#e74c3c; font-weight:600 }

.stock-bar-wrap { display:flex; align-items:center; gap:6px }
.stock-bar { width:50px; height:6px; border-radius:3px; background:rgba(0,0,0,.06); overflow:hidden }
.stock-fill { height:100%; border-radius:3px }
.stock-fill.ok { background:#27ae60 } .stock-fill.warn { background:#f39c12 } .stock-fill.danger { background:#e74c3c }
.stock-text { font-size:11px; font-weight:500 } .stock-text.ok { color:#27ae60 } .stock-text.warn { color:#f39c12 } .stock-text.danger { color:#e74c3c }

.action-cell { display:flex; gap:4px; flex-wrap:wrap }
.row-btn { padding:4px 10px; border:1px solid rgba(0,0,0,.1); border-radius:6px; background:#fff; font-size:11px; color:#555; cursor:pointer; white-space:nowrap }
.row-btn:hover { border-color:#1a73e8; color:#1a73e8 }
.empty-cell { text-align:center; color:#ccc; padding:40px }
code { font-size:11px; background:rgba(0,0,0,.03); padding:1px 6px; border-radius:3px; color:#888 }

.pagination { display:flex; align-items:center; justify-content:center; gap:10px; margin-top:14px }
.pagination button { padding:7px 16px; border:1px solid rgba(0,0,0,.1); border-radius:8px; background:#fff; font-size:13px; cursor:pointer; color:#555 }
.pagination button:disabled { opacity:.4; cursor:not-allowed }

.mini-table { width:100%; border-collapse:collapse }
.mini-table th,.mini-table td { padding:8px 12px; text-align:left; font-size:12px; border-bottom:1px solid rgba(0,0,0,.04) }
.mini-table th { font-weight:500; color:#999; font-size:11px }
.batch-status { padding:1px 6px; border-radius:3px; font-size:11px; font-weight:500 }
.batch-status.ok { background:rgba(39,174,96,.1); color:#27ae60 }
.batch-status.warn { background:rgba(243,156,18,.1); color:#e67e22 }
.batch-status.expired { background:rgba(231,76,60,.1); color:#e74c3c }
.empty-hint { text-align:center; color:#bbb; padding:20px; font-size:13px }
</style>

<style>
.modal-overlay { position:fixed; inset:0; background:rgba(0,0,0,.35); display:flex; align-items:center; justify-content:center; z-index:1100; padding:20px }
.modal-panel { background:#fff; border-radius:16px; width:100%; max-width:480px; max-height:85vh; display:flex; flex-direction:column; box-shadow:0 16px 48px rgba(0,0,0,.15) }
.modal-panel.wide { max-width:640px }
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
.modal-btn:disabled { opacity:.4; cursor:not-allowed }
.form-row-2 { display:grid; grid-template-columns:1fr 1fr; gap:14px }
.form-g { display:flex; flex-direction:column; gap:4px }
.form-g label { font-size:13px; font-weight:500; color:#555 }
.required { color:#e74c3c }
.form-input { padding:10px 14px; border:1px solid rgba(0,0,0,.1); border-radius:10px; font-size:14px; color:#333; outline:none; background:rgba(255,255,255,.7); transition:.15s; font-family:inherit; width:100%; box-sizing:border-box }
.form-input:focus { border-color:#1a73e8 }
.form-input:disabled { background:rgba(0,0,0,.03); color:#999 }
.form-textarea { resize:vertical; min-height:60px }
.modal-fade-enter-active { transition:all .2s ease }
.modal-fade-leave-active { transition:all .15s ease }
.modal-fade-enter-from,.modal-fade-leave-to { opacity:0 }
.modal-fade-enter-from .modal-panel { transform:scale(.94) translateY(12px) }
</style>
