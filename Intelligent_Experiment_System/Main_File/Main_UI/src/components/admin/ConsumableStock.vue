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
      </div>
      <div class="toolbar-right">
        <span class="total-label">共 {{ filteredList.length }} 种耗材</span>
        <button class="add-btn" @click="openItemDialog()">+ 新增耗材</button>
      </div>
    </div>

    <div class="data-table-wrap">
      <table class="data-table">
        <thead><tr><th>编号</th><th>名称</th><th>单位</th><th>库存</th><th>安全库存</th><th>状态</th><th>操作</th></tr></thead>
        <tbody>
          <tr v-for="item in pagedList" :key="item.id">
            <td><code>{{ item.materialNo }}</code></td>
            <td><span class="item-name">{{ item.materialName }}</span></td>
            <td>{{ item.unit }}</td>
            <td><span :class="item.currentStock < item.safetyStock ? 'text-red' : ''">{{ item.currentStock }}</span></td>
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
              <button class="row-btn danger" @click="deleteItem(item)">删除</button>
            </td>
          </tr>
          <tr v-if="pagedList.length === 0"><td colspan="7" class="empty-cell">暂无耗材数据</td></tr>
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
                <div class="form-g"><label>名称 <span class="required">*</span></label><input v-model="itemForm.materialName" class="form-input" /></div>
                <div class="form-g"><label>单位 <span class="required">*</span></label><input v-model="itemForm.unit" class="form-input" placeholder="瓶/盒/包/支" /></div>
              </div>
              <div class="form-g"><label>安全库存 <span class="required">*</span></label><input v-model.number="itemForm.safetyStock" type="number" class="form-input" min="0" /></div>
            </div>
            <div class="modal-footer">
              <button class="modal-btn cancel" @click="showItem = false">取消</button>
              <button class="modal-btn primary" @click="saveItem" :disabled="!itemForm.materialName.trim()">保存</button>
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
              <h3>批次入库 - {{ batchTarget?.materialName }}</h3>
              <button class="modal-close" @click="showBatchIn = false"><svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2"><path d="M18 6L6 18M6 6l12 12"/></svg></button>
            </div>
            <div class="modal-body">
              <div class="form-g"><label>入库数量 <span class="required">*</span></label><input v-model.number="batchForm.quantity" type="number" class="form-input" min="1" /></div>
              <div class="form-g"><label>到期日期 <span class="required">*</span></label><input v-model="batchForm.expiryDate" type="date" class="form-input" /></div>
              <div class="form-g"><label>备注</label><input v-model="batchForm.remark" class="form-input" placeholder="可选" /></div>
            </div>
            <div class="modal-footer">
              <button class="modal-btn cancel" @click="showBatchIn = false">取消</button>
              <button class="modal-btn primary" @click="saveBatch" :disabled="!batchForm.quantity || !batchForm.expiryDate">确认入库</button>
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
              <h3>批次记录 - {{ batchTarget.materialName }}</h3>
              <button class="modal-close" @click="showBatches = false"><svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2"><path d="M18 6L6 18M6 6l12 12"/></svg></button>
            </div>
            <div class="modal-body">
              <div v-if="batchLoading" class="empty-hint">加载中...</div>
              <table class="mini-table" v-else-if="batchList.length > 0">
                <thead><tr><th>批次号</th><th>入库数量</th><th>剩余</th><th>到期日</th><th>入库时间</th><th>状态</th></tr></thead>
                <tbody>
                  <tr v-for="b in batchList" :key="b.id">
                    <td><code>{{ b.batchNo }}</code></td>
                    <td>{{ b.quantity }}</td>
                    <td>{{ b.remaining }}</td>
                    <td>{{ b.expiryDate || '--' }}</td>
                    <td>{{ b.stockInTime?.replace('T', ' ').slice(0, 16) || '--' }}</td>
                    <td><span class="batch-status" :class="batchStatusClass(b)">{{ b.batchStatusDesc || (b.batchStatus === 1 ? '在库' : b.batchStatus === 2 ? '已清空' : '报废') }}</span></td>
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
import { getMaterialPage, createMaterial, updateMaterial, deleteMaterial, stockIn, getBatches, getMaterialDetail, type MaterialItem, type MaterialBatch, type StockInParams } from '../../utils/materialApi'

const items = ref<MaterialItem[]>([])

async function loadMaterials() {
  try {
    const res = await getMaterialPage({ pageNum: 1, pageSize: 200 })
    items.value = res.records
  } catch { /* 请求失败 */ }
}
onMounted(() => { loadMaterials() })

const searchName = ref('')
const pageNum = ref(1)
const pageSize = 8

const filteredList = computed(() => {
  let list = items.value
  if (searchName.value) {
    const kw = searchName.value.toLowerCase()
    list = list.filter(i => i.materialName.toLowerCase().includes(kw) || i.materialNo.toLowerCase().includes(kw))
  }
  return list
})
const totalPages = computed(() => Math.ceil(filteredList.value.length / pageSize) || 1)
const pagedList = computed(() => filteredList.value.slice((pageNum.value - 1) * pageSize, pageNum.value * pageSize))

const totalStock = computed(() => items.value.reduce((s, i) => s + i.currentStock, 0))
const lowStockCount = computed(() => items.value.filter(i => i.currentStock < i.safetyStock).length)
const nearExpiryCount = computed(() => items.value.filter(i => i.earliestExpiryDate && new Date(i.earliestExpiryDate) <= new Date(Date.now() + 90*86400000)).length)

type StockLevel = 'danger' | 'warn' | 'ok'
function stockLevel(i: MaterialItem): StockLevel {
  if (i.currentStock < i.safetyStock) return 'danger'
  if (i.currentStock < i.safetyStock * 2) return 'warn'
  return 'ok'
}
function stockPct(i: MaterialItem): number { return Math.min(100, Math.round(i.currentStock / Math.max(i.safetyStock * 2, 1) * 100)) }
function stockLabel(i: MaterialItem): string { return i.currentStock < i.safetyStock ? '不足' : i.currentStock < i.safetyStock * 2 ? '偏低' : '充足' }

function batchStatusClass(b: MaterialBatch): string {
  if (b.batchStatus === 3) return 'expired'
  if (!b.expiryDate) return 'ok'
  const d = new Date(b.expiryDate)
  if (d <= new Date()) return 'expired'
  if (d <= new Date(Date.now() + 90*86400000)) return 'warn'
  return 'ok'
}

// 耗材编辑弹窗
const showItem = ref(false)
const editingItemId = ref(0)
const itemForm = reactive({ materialName: '', unit: '瓶', safetyStock: 10 })
function openItemDialog(item?: MaterialItem) {
  if (item) {
    editingItemId.value = item.id
    itemForm.materialName = item.materialName
    itemForm.unit = item.unit
    itemForm.safetyStock = item.safetyStock
  } else {
    editingItemId.value = 0
    itemForm.materialName = ''
    itemForm.unit = '瓶'
    itemForm.safetyStock = 10
  }
  showItem.value = true
}
async function saveItem() {
  if (!itemForm.materialName.trim()) return
  try {
    if (editingItemId.value) {
      await updateMaterial(editingItemId.value, {
        materialName: itemForm.materialName.trim(),
        unit: itemForm.unit,
        safetyStock: itemForm.safetyStock,
      })
    } else {
      await createMaterial({
        materialName: itemForm.materialName.trim(),
        labId: 1, // 默认实验室，用户可后续调整
        unit: itemForm.unit,
        safetyStock: itemForm.safetyStock,
      })
    }
    showItem.value = false
    await loadMaterials()
  } catch (e: any) {
    alert(e.message || '操作失败')
  }
}

// 删除
async function deleteItem(item: MaterialItem) {
  if (!confirm(`确定删除耗材「${item.materialName}」吗？`)) return
  try {
    await deleteMaterial(item.id)
    await loadMaterials()
  } catch (e: any) {
    alert(e.message || '删除失败')
  }
}

// 批次入库
const showBatchIn = ref(false)
const batchTarget = ref<MaterialItem | null>(null)
const batchForm = reactive({ quantity: 0, expiryDate: '', remark: '' })
function openBatchDialog(item: MaterialItem) {
  batchTarget.value = item
  batchForm.quantity = 0
  batchForm.expiryDate = ''
  batchForm.remark = ''
  showBatchIn.value = true
}
async function saveBatch() {
  if (!batchTarget.value || !batchForm.quantity || !batchForm.expiryDate) return
  try {
    const params: StockInParams = {
      materialId: batchTarget.value.id,
      quantity: batchForm.quantity,
      expiryDate: batchForm.expiryDate,
      remark: batchForm.remark || undefined,
    }
    await stockIn(params)
    showBatchIn.value = false
    await loadMaterials()
  } catch (e: any) {
    alert(e.message || '入库失败')
  }
}

// 批次记录弹窗
const showBatches = ref(false)
const batchList = ref<MaterialBatch[]>([])
const batchLoading = ref(false)
async function viewBatches(item: MaterialItem) {
  batchTarget.value = item
  showBatches.value = true
  batchLoading.value = true
  batchList.value = []
  try {
    const detail = await getMaterialDetail(item.id)
    batchList.value = detail.batches || []
  } catch {
    batchList.value = []
  } finally {
    batchLoading.value = false
  }
}
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
.total-label { font-size:13px; color:#999 }
.add-btn { padding:8px 18px; border:none; border-radius:10px; background:linear-gradient(135deg,#1a73e8,#4a90d9); color:#fff; font-size:13px; font-weight:500; cursor:pointer }

.data-table-wrap { background:rgba(255,255,255,.5); border-radius:12px; border:1px solid rgba(0,0,0,.04); overflow-x:auto }
.data-table { width:100%; border-collapse:collapse; min-width:700px }
.data-table th,.data-table td { padding:11px 14px; text-align:left; font-size:13px; border-bottom:1px solid rgba(0,0,0,.04); white-space:nowrap }
.data-table th { font-weight:600; color:#666; background:rgba(0,0,0,.015); font-size:12px }
.item-name { font-weight:600; color:#1a1a2e }
.text-red { color:#e74c3c; font-weight:600 }

.stock-bar-wrap { display:flex; align-items:center; gap:6px }
.stock-bar { width:50px; height:6px; border-radius:3px; background:rgba(0,0,0,.06); overflow:hidden }
.stock-fill { height:100%; border-radius:3px }
.stock-fill.ok { background:#27ae60 } .stock-fill.warn { background:#f39c12 } .stock-fill.danger { background:#e74c3c }
.stock-text { font-size:11px; font-weight:500 } .stock-text.ok { color:#27ae60 } .stock-text.warn { color:#f39c12 } .stock-text.danger { color:#e74c3c }

.action-cell { display:flex; gap:4px; flex-wrap:wrap }
.row-btn { padding:4px 10px; border:1px solid rgba(0,0,0,.1); border-radius:6px; background:#fff; font-size:11px; color:#555; cursor:pointer; white-space:nowrap }
.row-btn:hover { border-color:#1a73e8; color:#1a73e8 }
.row-btn.danger:hover { border-color:#e74c3c; color:#e74c3c }
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
.modal-fade-enter-active { transition:all .2s ease }
.modal-fade-leave-active { transition:all .15s ease }
.modal-fade-enter-from,.modal-fade-leave-to { opacity:0 }
.modal-fade-enter-from .modal-panel { transform:scale(.94) translateY(12px) }
</style>
