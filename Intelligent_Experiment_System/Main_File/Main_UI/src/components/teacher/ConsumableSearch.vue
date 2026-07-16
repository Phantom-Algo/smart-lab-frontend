<template>
  <div class="consumable-page">
    <div class="page-header">
      <h2>耗材查询</h2>
      <p>查看耗材库存信息</p>
    </div>

    <div class="filter-bar">
      <div class="search-box">
        <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2"><circle cx="11" cy="11" r="8"/><path d="M21 21l-4.35-4.35"/></svg>
        <input v-model="searchText" type="text" class="search-input" placeholder="搜索耗材名称或编号..." />
        <span v-if="searchText" class="search-clear" @click="searchText = ''">
          <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2"><path d="M18 6L6 18M6 6l12 12"/></svg>
        </span>
      </div>
      <div class="filter-actions">
        <select v-model="stockFilter" class="filter-select">
          <option value="">全部状态</option>
          <option value="sufficient">库存充足</option>
          <option value="low">库存偏低</option>
          <option value="critical">库存告急</option>
        </select>
        <span class="result-count">共 {{ filteredList.length }} 种耗材</span>
      </div>
    </div>

    <div class="card-grid" v-if="pagedList.length > 0">
      <div v-for="item in pagedList" :key="item.id" class="consumable-card" @click="openDetail(item)">
        <div class="card-icon-area" :class="'cat-' + (item.labId || 'other')">
          <span class="card-stock-badge" :class="'stock-' + stockStatus(item)">{{ stockLabel(stockStatus(item)) }}</span>
        </div>
        <div class="card-info">
          <div class="card-name">{{ item.materialName }}</div>
          <div class="card-meta">
            <span class="card-unit">{{ item.unit }}</span>
            <span class="card-stock-num">
              库存 <strong :class="'text-' + stockStatus(item)">{{ item.currentStock }}</strong>
            </span>
          </div>
          <div class="stock-bar-wrap">
            <div class="stock-bar" :style="{ width: stockPercent(item) + '%' }" :class="'bar-' + stockStatus(item)"></div>
          </div>
        </div>
      </div>
    </div>
    <div v-else class="empty-hint">暂无耗材数据</div>

    <div class="pagination" v-if="totalPages > 1">
      <button :disabled="pageNum <= 1" @click="pageNum--">上一页</button>
      <span v-for="p in totalPages" :key="p" class="page-num" :class="{ active: p === pageNum }" @click="pageNum = p">{{ p }}</span>
      <button :disabled="pageNum >= totalPages" @click="pageNum++">下一页</button>
    </div>

    <!-- 耗材详情弹窗 -->
    <teleport to="body">
      <transition name="modal-fade">
        <div v-if="detailItem" class="modal-overlay" @click.self="closeDetail">
          <div class="modal-panel">
            <div class="modal-header">
              <div class="modal-title-row">
                <h3>{{ detailItem.materialName }}</h3>
                <span class="detail-status" :class="'stock-' + stockStatus(detailItem)">{{ stockLabel(stockStatus(detailItem)) }}</span>
              </div>
              <button class="modal-close" @click="closeDetail">
                <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2"><path d="M18 6L6 18M6 6l12 12"/></svg>
              </button>
            </div>

            <div class="modal-body">
              <div class="detail-grid">
                <div class="detail-item"><span class="detail-label">耗材编号</span><span class="detail-value">{{ detailItem.materialNo }}</span></div>
                <div class="detail-item"><span class="detail-label">计量单位</span><span class="detail-value">{{ detailItem.unit }}</span></div>
                <div class="detail-item"><span class="detail-label">安全库存</span><span class="detail-value">{{ detailItem.safetyStock }} {{ detailItem.unit }}</span></div>
                <div class="detail-item"><span class="detail-label">当前总量</span><span class="detail-value" :class="'text-' + stockStatus(detailItem)"><strong>{{ detailItem.currentStock }}</strong> {{ detailItem.unit }}</span></div>
              </div>

              <div class="stock-overview">
                <div class="stock-overview-header">
                  <span>库存概览</span>
                  <span class="stock-overview-num">{{ detailItem.currentStock }} / 安全线 {{ detailItem.safetyStock }} {{ detailItem.unit }}</span>
                </div>
                <div class="stock-bar-wrap large">
                  <div class="stock-bar" :style="{ width: stockPercent(detailItem) + '%' }" :class="'bar-' + stockStatus(detailItem)"></div>
                </div>
              </div>

              <div class="batch-section">
                <div class="batch-header" @click="showBatch = !showBatch">
                  <span>📦 批次明细（{{ detailBatches.length }} 批）</span>
                  <span class="batch-toggle">{{ showBatch ? '收起 ▲' : '展开 ▼' }}</span>
                </div>
                <div v-if="showBatch" class="batch-table-wrap">
                  <div v-if="batchLoading" class="batch-loading">加载中...</div>
                  <table v-else-if="detailBatches.length > 0" class="batch-table">
                    <thead><tr><th>批次号</th><th>剩余数量</th><th>过期日期</th><th>状态</th></tr></thead>
                    <tbody>
                      <tr v-for="b in detailBatches" :key="b.id">
                        <td><code>{{ b.batchNo }}</code></td>
                        <td>{{ b.remaining }} {{ detailItem.unit }}</td>
                        <td>{{ b.expiryDate || '--' }}</td>
                        <td>
                          <span v-if="b.batchStatus === 3" class="batch-tag expired">报废</span>
                          <span v-else-if="b.expiryDate && new Date(b.expiryDate) < new Date()" class="batch-tag expired">已过期</span>
                          <span v-else-if="b.expiryDate && new Date(b.expiryDate).getTime() - Date.now() < 90*24*60*60*1000" class="batch-tag near-expiry">临期</span>
                          <span v-else class="batch-tag normal">正常</span>
                        </td>
                      </tr>
                    </tbody>
                  </table>
                  <div v-else class="batch-loading">暂无批次数据</div>
                </div>
              </div>
            </div>

            <div class="modal-footer">
              <button class="modal-btn cancel" @click="closeDetail">关闭</button>
              <button class="modal-btn primary" @click="goApply()">📝 申请领用</button>
            </div>
          </div>
        </div>
      </transition>
    </teleport>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { getMaterialPage, getMaterialDetail, type MaterialItem, type MaterialBatch } from '../../utils/materialApi'

const emit = defineEmits<{ navigate: [payload: { key: string; name: string }] }>()

const searchText = ref('')
const stockFilter = ref('')
const pageNum = ref(1)
const pageSize = 8
const detailItem = ref<MaterialItem | null>(null)
const detailBatches = ref<MaterialBatch[]>([])
const batchLoading = ref(false)
const showBatch = ref(true)

const consumables = ref<MaterialItem[]>([])

async function loadData() {
  try {
    const res = await getMaterialPage({ pageNum: 1, pageSize: 200 })
    consumables.value = res.records
  } catch { /* API 不可用 */ }
}

onMounted(() => loadData())

const filteredList = computed(() => {
  let list = consumables.value
  if (searchText.value) {
    const kw = searchText.value.toLowerCase()
    list = list.filter(c => c.materialName.toLowerCase().includes(kw) || c.materialNo.toLowerCase().includes(kw))
  }
  if (stockFilter.value) {
    list = list.filter(c => stockStatus(c) === stockFilter.value)
  }
  return list
})

const totalPages = computed(() => Math.ceil(filteredList.value.length / pageSize) || 1)
const pagedList = computed(() => {
  const start = (pageNum.value - 1) * pageSize
  return filteredList.value.slice(start, start + pageSize)
})

function stockStatus(item: MaterialItem): 'sufficient' | 'low' | 'critical' {
  if (item.currentStock <= Math.floor(item.safetyStock / 2)) return 'critical'
  if (item.currentStock < item.safetyStock) return 'low'
  return 'sufficient'
}

function stockLabel(s: string) {
  const m: Record<string, string> = { sufficient: '充足', low: '偏低', critical: '告急' }
  return m[s] || s
}

function stockPercent(item: MaterialItem) {
  return Math.min(100, (item.currentStock / Math.max(item.safetyStock * 2, 1)) * 100)
}

async function openDetail(item: MaterialItem) {
  detailItem.value = item
  showBatch.value = true
  batchLoading.value = true
  detailBatches.value = []
  try {
    const detail = await getMaterialDetail(item.id)
    detailBatches.value = detail.batches || []
  } catch {
    detailBatches.value = []
  } finally {
    batchLoading.value = false
  }
}

function closeDetail() { detailItem.value = null }

function goApply() {
  detailItem.value = null
  emit('navigate', { key: 'consumable-apply', name: '耗材领用' })
}
</script>

<style scoped>
.consumable-page { height:100%; display:flex; flex-direction:column; padding:20px 24px }
.page-header { margin-bottom:14px; flex-shrink:0 }
.page-header h2 { margin:0; font-size:20px; font-weight:600; color:#1a1a2e }
.page-header p { margin:4px 0 0; font-size:13px; color:#999 }

.filter-bar { display:flex; align-items:center; gap:14px; margin-bottom:14px; flex-shrink:0; flex-wrap:wrap }
.search-box { position:relative; display:flex; align-items:center; flex:1; min-width:220px; max-width:380px; background:rgba(255,255,255,.7); backdrop-filter:blur(10px); border:1px solid rgba(0,0,0,.08); border-radius:10px; padding:0 12px }
.search-box svg { flex-shrink:0; color:#999 }
.search-input { flex:1; border:none; outline:none; background:transparent; padding:10px 8px; font-size:14px; color:#333 }
.search-input::placeholder { color:#bbb }
.search-clear { cursor:pointer; color:#bbb; padding:2px; border-radius:50%; transition:.15s }
.search-clear:hover { color:#999; background:rgba(0,0,0,.05) }
.filter-actions { display:flex; align-items:center; gap:10px; flex-wrap:wrap }
.filter-select { padding:10px 14px; border:1px solid rgba(0,0,0,.1); border-radius:10px; font-size:13px; color:#555; outline:none; background:rgba(255,255,255,.7); backdrop-filter:blur(10px); cursor:pointer }
.result-count { font-size:13px; color:#999; white-space:nowrap }

.card-grid { flex:1; display:grid; grid-template-columns:repeat(auto-fill, minmax(195px, 1fr)); gap:14px; align-content:start; overflow-y:auto; padding-bottom:8px }
.consumable-card { background:rgba(255,255,255,.65); backdrop-filter:blur(10px); border:1px solid rgba(0,0,0,.06); border-radius:14px; overflow:hidden; cursor:pointer; transition:all .2s }
.consumable-card:hover { transform:translateY(-3px); box-shadow:0 10px 28px rgba(0,0,0,.1); border-color:rgba(26,115,232,.25) }

.card-icon-area { position:relative; height:70px; display:flex; align-items:flex-start; justify-content:flex-end; padding:12px 14px }
.cat-other { background:linear-gradient(135deg,#eceff1,#cfd8dc) }

.card-stock-badge { font-size:11px; font-weight:600; padding:3px 10px; border-radius:10px; color:#fff }
.stock-sufficient { background:#27ae60 }
.stock-low { background:#f39c12 }
.stock-critical { background:#e74c3c }
.text-sufficient { color:#27ae60 }
.text-low { color:#f39c12 }
.text-critical { color:#e74c3c }

.card-info { padding:10px 14px 14px }
.card-name { font-size:15px; font-weight:600; color:#1a1a2e; margin-bottom:2px; white-space:nowrap; overflow:hidden; text-overflow:ellipsis }
.card-meta { display:flex; justify-content:space-between; align-items:center; margin-bottom:8px }
.card-unit { font-size:12px; color:#aaa }
.card-stock-num { font-size:12px; color:#666 }
.card-stock-num strong { font-size:15px }

.stock-bar-wrap { height:4px; background:rgba(0,0,0,.06); border-radius:2px; overflow:hidden }
.stock-bar { height:100%; border-radius:2px; transition:width .4s }
.bar-sufficient { background:#27ae60 }
.bar-low { background:#f39c12 }
.bar-critical { background:#e74c3c }

.empty-hint { flex:1; display:flex; align-items:center; justify-content:center; color:#bbb; font-size:14px }

.pagination { display:flex; align-items:center; justify-content:center; gap:8px; padding:12px 0 0; flex-shrink:0 }
.pagination button { padding:8px 16px; border:1px solid rgba(0,0,0,.1); border-radius:8px; background:rgba(255,255,255,.7); color:#555; font-size:13px; cursor:pointer; transition:.15s }
.pagination button:hover:not(:disabled) { border-color:#1a73e8; color:#1a73e8 }
.pagination button:disabled { opacity:.4; cursor:not-allowed }
.page-num { width:34px; height:34px; display:flex; align-items:center; justify-content:center; border-radius:8px; font-size:13px; color:#555; cursor:pointer; transition:.15s }
.page-num:hover { background:rgba(26,115,232,.08); color:#1a73e8 }
.page-num.active { background:#1a73e8; color:#fff }
</style>

<style>
.modal-overlay { position:fixed; inset:0; background:rgba(0,0,0,.35); display:flex; align-items:center; justify-content:center; z-index:1100; padding:20px }
.modal-panel { background:#fff; border-radius:18px; width:100%; max-width:680px; max-height:85vh; display:flex; flex-direction:column; box-shadow:0 24px 64px rgba(0,0,0,.18) }
.modal-header { display:flex; align-items:flex-start; justify-content:space-between; padding:24px 28px 0; flex-shrink:0 }
.modal-title-row { display:flex; align-items:center; gap:10px; flex-wrap:wrap }
.modal-title-row h3 { margin:0; font-size:18px; font-weight:600; color:#1a1a2e }
.detail-status { font-size:11px; font-weight:600; padding:3px 10px; border-radius:10px; color:#fff }
.modal-close { background:none; border:none; cursor:pointer; color:#aaa; padding:4px; border-radius:6px; transition:.15s; flex-shrink:0 }
.modal-close:hover { background:rgba(0,0,0,.05); color:#333 }

.modal-body { padding:20px 28px; overflow-y:auto; flex:1 }
.detail-grid { display:grid; grid-template-columns:repeat(2,1fr); gap:14px; margin-bottom:16px }
.detail-item { display:flex; flex-direction:column; gap:2px }
.detail-label { font-size:12px; color:#999 }
.detail-value { font-size:14px; color:#333; font-weight:500 }

.stock-overview { margin-bottom:20px; padding:14px 16px; background:rgba(0,0,0,.02); border-radius:10px }
.stock-overview-header { display:flex; justify-content:space-between; align-items:center; margin-bottom:8px; font-size:13px; color:#555 }
.stock-overview-num { font-weight:500 }
.stock-bar-wrap.large { height:8px; border-radius:4px }

.batch-section { border:1px solid rgba(0,0,0,.06); border-radius:10px; overflow:hidden }
.batch-header { display:flex; align-items:center; justify-content:space-between; padding:12px 16px; background:rgba(0,0,0,.02); font-size:13px; font-weight:500; color:#555; cursor:pointer; user-select:none; transition:.15s }
.batch-header:hover { background:rgba(0,0,0,.04) }
.batch-toggle { font-size:12px; color:#1a73e8 }
.batch-table-wrap { overflow-x:auto }
.batch-loading { text-align:center; padding:20px; color:#bbb; font-size:13px }
.batch-table { width:100%; border-collapse:collapse; font-size:13px }
.batch-table th,.batch-table td { padding:10px 14px; text-align:left; border-bottom:1px solid rgba(0,0,0,.04) }
.batch-table th { font-weight:600; color:#888; font-size:12px; background:rgba(0,0,0,.01) }
.batch-table code { font-size:12px; background:rgba(0,0,0,.04); padding:2px 6px; border-radius:4px }
.batch-tag { font-size:11px; font-weight:600; padding:2px 8px; border-radius:4px }
.batch-tag.normal { background:rgba(39,174,96,.12); color:#1e8449 }
.batch-tag.near-expiry { background:rgba(243,156,18,.12); color:#b8730a }
.batch-tag.expired { background:rgba(231,76,60,.12); color:#b03a2e }

.modal-footer { display:flex; justify-content:flex-end; gap:10px; padding:16px 28px 24px; border-top:1px solid rgba(0,0,0,.06); flex-shrink:0 }
.modal-btn { padding:10px 28px; border-radius:10px; font-size:14px; cursor:pointer; border:none; transition:all .2s; font-weight:500 }
.modal-btn.cancel { background:rgba(0,0,0,.04); color:#666 }
.modal-btn.cancel:hover { background:rgba(0,0,0,.08) }
.modal-btn.primary { background:linear-gradient(135deg,#1a73e8,#4a90d9); color:#fff }
.modal-btn.primary:hover { box-shadow:0 4px 16px rgba(26,115,232,.3); transform:translateY(-1px) }

.modal-fade-enter-active { transition:all .25s ease }
.modal-fade-leave-active { transition:all .2s ease }
.modal-fade-enter-from,.modal-fade-leave-to { opacity:0 }
.modal-fade-enter-from .modal-panel { transform:scale(.94) translateY(16px) }
.modal-fade-leave-to .modal-panel { transform:scale(.96) translateY(8px) }
</style>
