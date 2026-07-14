<template>
  <div class="consumable-page">
    <div class="page-header">
      <h2>耗材查询</h2>
      <p>查看耗材库存信息</p>
    </div>

    <!-- 搜索与筛选栏 -->
    <div class="filter-bar">
      <div class="search-box">
        <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2"><circle cx="11" cy="11" r="8"/><path d="M21 21l-4.35-4.35"/></svg>
        <input
          v-model="searchText"
          type="text"
          class="search-input"
          placeholder="搜索耗材名称或编号..."
        />
        <span v-if="searchText" class="search-clear" @click="searchText = ''">
          <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2"><path d="M18 6L6 18M6 6l12 12"/></svg>
        </span>
      </div>
      <div class="filter-actions">
        <select v-model="categoryFilter" class="filter-select">
          <option value="">全部分类</option>
          <option v-for="cat in categories" :key="cat" :value="cat">{{ cat }}</option>
        </select>
        <select v-model="stockFilter" class="filter-select">
          <option value="">全部状态</option>
          <option value="sufficient">库存充足</option>
          <option value="low">库存偏低</option>
          <option value="critical">库存告急</option>
        </select>
        <span class="result-count">共 {{ filteredList.length }} 种耗材</span>
      </div>
    </div>

    <!-- 卡片网格：填满整屏 -->
    <div class="card-grid" v-if="pagedList.length > 0">
      <div
        v-for="item in pagedList"
        :key="item.id"
        class="consumable-card"
        @click="openDetail(item)"
      >
        <div class="card-icon-area" :class="'cat-' + item.categoryKey">
          <span class="card-cat-tag">{{ item.category }}</span>
          <span class="card-stock-badge" :class="'stock-' + item.stockStatus">{{ stockLabel(item.stockStatus) }}</span>
        </div>
        <div class="card-info">
          <div class="card-name">{{ item.name }}</div>
          <div class="card-spec">{{ item.specification }}</div>
          <div class="card-meta">
            <span class="card-unit">{{ item.unit }}</span>
            <span class="card-stock-num">
              库存 <strong :class="'text-' + item.stockStatus">{{ item.totalStock }}</strong>
            </span>
          </div>
          <div class="stock-bar-wrap">
            <div class="stock-bar" :style="{ width: stockPercent(item) + '%' }" :class="'bar-' + item.stockStatus"></div>
          </div>
        </div>
      </div>
    </div>
    <div v-else class="empty-hint">未找到匹配的耗材</div>

    <!-- 分页 -->
    <div class="pagination" v-if="totalPages > 1">
      <button :disabled="pageNum <= 1" @click="pageNum--">上一页</button>
      <span v-for="p in totalPages" :key="p" class="page-num" :class="{ active: p === pageNum }" @click="pageNum = p">{{ p }}</span>
      <button :disabled="pageNum >= totalPages" @click="pageNum++">下一页</button>
    </div>

    <!-- ===== 耗材详情弹窗 ===== -->
    <teleport to="body">
      <transition name="modal-fade">
        <div v-if="detailItem" class="modal-overlay" @click.self="closeDetail">
          <div class="modal-panel">
            <div class="modal-header">
              <div class="modal-title-row">
                <h3>{{ detailItem.name }}</h3>
                <span class="detail-tag" :class="'cat-' + detailItem.categoryKey">{{ detailItem.category }}</span>
                <span class="detail-status" :class="'stock-' + detailItem.stockStatus">{{ stockLabel(detailItem.stockStatus) }}</span>
              </div>
              <button class="modal-close" @click="closeDetail">
                <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2"><path d="M18 6L6 18M6 6l12 12"/></svg>
              </button>
            </div>

            <div class="modal-body">
              <div class="detail-grid">
                <div class="detail-item"><span class="detail-label">耗材编号</span><span class="detail-value">{{ detailItem.consumableNo }}</span></div>
                <div class="detail-item"><span class="detail-label">规格型号</span><span class="detail-value">{{ detailItem.specification }}</span></div>
                <div class="detail-item"><span class="detail-label">计量单位</span><span class="detail-value">{{ detailItem.unit }}</span></div>
                <div class="detail-item"><span class="detail-label">安全库存</span><span class="detail-value">{{ detailItem.safetyStock }} {{ detailItem.unit }}</span></div>
                <div class="detail-item"><span class="detail-label">当前总量</span><span class="detail-value" :class="'text-' + detailItem.stockStatus"><strong>{{ detailItem.totalStock }}</strong> {{ detailItem.unit }}</span></div>
                <div class="detail-item"><span class="detail-label">存放位置</span><span class="detail-value">{{ detailItem.storageLocation || '未指定' }}</span></div>
              </div>

              <div class="detail-desc" v-if="detailItem.description">
                <span class="detail-label">描述</span>
                <p>{{ detailItem.description }}</p>
              </div>

              <div class="stock-overview">
                <div class="stock-overview-header">
                  <span>库存概览</span>
                  <span class="stock-overview-num">{{ detailItem.totalStock }} / 安全线 {{ detailItem.safetyStock }} {{ detailItem.unit }}</span>
                </div>
                <div class="stock-bar-wrap large">
                  <div class="stock-bar" :style="{ width: stockPercent(detailItem) + '%' }" :class="'bar-' + detailItem.stockStatus"></div>
                </div>
              </div>

              <div class="batch-section">
                <div class="batch-header" @click="showBatch = !showBatch">
                  <span>📦 批次明细（{{ detailItem.batches.length }} 批）</span>
                  <span class="batch-toggle">{{ showBatch ? '收起 ▲' : '展开 ▼' }}</span>
                </div>
                <div class="batch-table-wrap" v-if="showBatch">
                  <table class="batch-table">
                    <thead><tr><th>批次号</th><th>生产日期</th><th>过期日期</th><th>库存量</th><th>供应商</th><th>状态</th></tr></thead>
                    <tbody>
                      <tr v-for="b in detailItem.batches" :key="b.id">
                        <td><code>{{ b.batchNo }}</code></td>
                        <td>{{ b.productionDate }}</td>
                        <td>{{ b.expiryDate }}</td>
                        <td>{{ b.stock }} {{ detailItem.unit }}</td>
                        <td>{{ b.supplier }}</td>
                        <td>
                          <span v-if="isExpired(b.expiryDate)" class="batch-tag expired">已过期</span>
                          <span v-else-if="isNearExpiry(b.expiryDate)" class="batch-tag near-expiry">临期</span>
                          <span v-else class="batch-tag normal">正常</span>
                        </td>
                      </tr>
                    </tbody>
                  </table>
                </div>
              </div>
            </div>

            <div class="modal-footer">
              <button class="modal-btn cancel" @click="closeDetail">关闭</button>
              <button class="modal-btn primary" @click="goApply(detailItem.id)">📝 申请领用</button>
            </div>
          </div>
        </div>
      </transition>
    </teleport>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import api from '../../utils/api'
import { isMockMode } from '../../stores/authStore'

const emit = defineEmits<{ navigate: [payload: { key: string; name: string }] }>()

const searchText = ref('')
const categoryFilter = ref('')
const stockFilter = ref('')
const pageNum = ref(1)
const pageSize = 8
const detailItem = ref<Consumable | null>(null)
const showBatch = ref(true)

const categories = ['化学试剂', '玻璃器皿', '生物耗材', '物理器材', '安全防护', '其他']

interface Batch {
  id: number; batchNo: string; productionDate: string; expiryDate: string
  stock: number; supplier: string
}
interface Consumable {
  id: number; consumableNo: string; name: string; category: string; categoryKey: string
  specification: string; unit: string; description: string
  safetyStock: number; totalStock: number; stockStatus: 'sufficient' | 'low' | 'critical'
  storageLocation: string; batches: Batch[]
}

const MOCK_CONSUMABLES: Consumable[] = [
  { id: 1, consumableNo: 'CON-2024-0001', name: '无水乙醇', category: '化学试剂', categoryKey: 'chemical',
    specification: '分析纯 500ml/瓶', unit: '瓶', description: '用于一般化学分析和有机合成，易挥发，需密封避光保存。',
    safetyStock: 20, totalStock: 45, stockStatus: 'sufficient', storageLocation: 'A201-试剂柜-01',
    batches: [
      { id: 1, batchNo: 'LOT-20260601', productionDate: '2026-06-01', expiryDate: '2028-06-01', stock: 25, supplier: '国药试剂' },
      { id: 2, batchNo: 'LOT-20260315', productionDate: '2026-03-15', expiryDate: '2028-03-15', stock: 20, supplier: '阿拉丁试剂' },
    ] },
  { id: 2, consumableNo: 'CON-2024-0002', name: '硫酸铜', category: '化学试剂', categoryKey: 'chemical',
    specification: '分析纯 500g/瓶', unit: '瓶', description: '五水硫酸铜，蓝色结晶，用于化学实验教学。',
    safetyStock: 10, totalStock: 5, stockStatus: 'low', storageLocation: 'A201-试剂柜-02',
    batches: [
      { id: 3, batchNo: 'LOT-20260520', productionDate: '2026-05-20', expiryDate: '2028-05-20', stock: 3, supplier: '国药试剂' },
      { id: 4, batchNo: 'LOT-20260110', productionDate: '2026-01-10', expiryDate: '2027-07-10', stock: 2, supplier: '麦克林' },
    ] },
  { id: 3, consumableNo: 'CON-2024-0003', name: '烧杯', category: '玻璃器皿', categoryKey: 'glass',
    specification: '250ml 高硼硅', unit: '个', description: '高硼硅玻璃烧杯，耐高温，用于溶液配制和加热。',
    safetyStock: 50, totalStock: 120, stockStatus: 'sufficient', storageLocation: 'A201-器材柜-03',
    batches: [
      { id: 5, batchNo: 'LOT-20260701', productionDate: '2026-07-01', expiryDate: '2031-07-01', stock: 120, supplier: '蜀牛玻璃' },
    ] },
  { id: 4, consumableNo: 'CON-2024-0004', name: '定量滤纸', category: '生物耗材', categoryKey: 'bio',
    specification: 'Φ12.5cm 中速/盒', unit: '盒', description: '定量分析用无灰滤纸，用于重量分析过滤。',
    safetyStock: 15, totalStock: 3, stockStatus: 'critical', storageLocation: 'A201-耗材柜-01',
    batches: [
      { id: 6, batchNo: 'LOT-20260401', productionDate: '2026-04-01', expiryDate: '2027-04-01', stock: 3, supplier: '沃特曼' },
    ] },
  { id: 5, consumableNo: 'CON-2024-0005', name: '丁腈手套', category: '安全防护', categoryKey: 'safety',
    specification: 'M号 100只/盒', unit: '盒', description: '无粉丁腈检查手套，耐化学腐蚀，适用于实验室一般操作。',
    safetyStock: 30, totalStock: 200, stockStatus: 'sufficient', storageLocation: 'A201-防护柜-01',
    batches: [
      { id: 7, batchNo: 'LOT-20260705', productionDate: '2026-07-05', expiryDate: '2029-07-05', stock: 150, supplier: '安思尔' },
      { id: 8, batchNo: 'LOT-20260620', productionDate: '2026-06-20', expiryDate: '2029-06-20', stock: 50, supplier: '麦迪康' },
    ] },
  { id: 6, consumableNo: 'CON-2024-0006', name: '琼脂粉', category: '生物耗材', categoryKey: 'bio',
    specification: '250g/瓶', unit: '瓶', description: '细菌学级别琼脂粉，用于配制微生物培养基。',
    safetyStock: 10, totalStock: 8, stockStatus: 'low', storageLocation: 'A201-耗材柜-02',
    batches: [
      { id: 9, batchNo: 'LOT-20260610', productionDate: '2026-06-10', expiryDate: '2027-06-10', stock: 5, supplier: 'Oxoid' },
      { id: 10, batchNo: 'LOT-20260501', productionDate: '2026-05-01', expiryDate: '2027-05-01', stock: 3, supplier: '北京奥博星' },
    ] },
  { id: 7, consumableNo: 'CON-2024-0007', name: 'pH试纸', category: '化学试剂', categoryKey: 'chemical',
    specification: 'pH 1-14 广泛试纸/盒', unit: '盒', description: '广泛pH试纸，用于溶液酸碱度快速检测。',
    safetyStock: 20, totalStock: 60, stockStatus: 'sufficient', storageLocation: 'A201-试剂柜-03',
    batches: [
      { id: 11, batchNo: 'LOT-20260701-2', productionDate: '2026-07-01', expiryDate: '2028-07-01', stock: 60, supplier: '上海三爱思' },
    ] },
  { id: 8, consumableNo: 'CON-2024-0008', name: '试管刷', category: '物理器材', categoryKey: 'physics',
    specification: '中号 尼龙毛', unit: '个', description: '实验室常用清洁工具，用于清洗试管和玻璃器皿。',
    safetyStock: 20, totalStock: 35, stockStatus: 'sufficient', storageLocation: 'A201-器材柜-04',
    batches: [
      { id: 12, batchNo: 'LOT-20260301', productionDate: '2026-03-01', expiryDate: '2031-03-01', stock: 35, supplier: '国产通用' },
    ] },
]

const consumables = ref<Consumable[]>([])

async function loadData() {
  if (isMockMode.value) { consumables.value = [...MOCK_CONSUMABLES]; return }
  try {
    // 后端端点: GET /api/material/materials → PageResult<MaterialVO>
    const res = await api.get('/material/materials', { params: { pageNum: 1, pageSize: 200 } })
    if (res.data?.data?.records?.length) {
      consumables.value = res.data.data.records.map((c: any) => ({
        id: c.id,
        consumableNo: c.materialNo || '',
        name: c.materialName || '',
        category: c.categoryName || '耗材',
        categoryKey: c.category || 'other',
        specification: c.spec || '',
        unit: c.unit || '',
        description: c.description || '',
        safetyStock: c.safetyStock || 10,
        totalStock: c.currentStock || 0,
        stockStatus: (c.currentStock <= (c.safetyStock || 10) ? (c.currentStock <= Math.floor((c.safetyStock || 10) / 2) ? 'critical' : 'low') : 'sufficient') as Consumable['stockStatus'],
        storageLocation: c.storageLocation || '',
        batches: c.batches || []
      }))
    }
  } catch { /* API 不可用 */ }
}

onMounted(() => loadData())

const filteredList = computed(() => {
  let list = consumables.value
  if (searchText.value) {
    const kw = searchText.value.toLowerCase()
    list = list.filter(c => c.name.toLowerCase().includes(kw) || c.consumableNo.toLowerCase().includes(kw))
  }
  if (categoryFilter.value) list = list.filter(c => c.category === categoryFilter.value)
  if (stockFilter.value) list = list.filter(c => c.stockStatus === stockFilter.value)
  return list
})

const totalPages = computed(() => Math.ceil(filteredList.value.length / pageSize))
const pagedList = computed(() => {
  const start = (pageNum.value - 1) * pageSize
  return filteredList.value.slice(start, start + pageSize)
})

function openDetail(item: Consumable) { detailItem.value = item; showBatch.value = true }
function closeDetail() { detailItem.value = null }
function stockLabel(s: string) { const m: Record<string,string>={sufficient:'充足',low:'偏低',critical:'告急'}; return m[s]||s }
function stockPercent(item: Consumable) { return Math.min(100, (item.totalStock / Math.max(item.totalStock, item.safetyStock * 3)) * 100) }
function isExpired(d: string) { return new Date(d) < new Date() }
function isNearExpiry(d: string) { const t = new Date(d).getTime() - Date.now(); return t > 0 && t < 90*24*60*60*1000 }
function goApply(_id: number) { detailItem.value = null; emit('navigate', { key: 'consumable-apply', name: '耗材领用' }) }
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

.card-icon-area { position:relative; height:78px; display:flex; align-items:flex-start; justify-content:space-between; padding:12px 14px }
.card-cat-tag { font-size:11px; font-weight:500; padding:3px 10px; border-radius:4px; background:rgba(255,255,255,.75); color:#555 }
.card-stock-badge { font-size:11px; font-weight:600; padding:3px 10px; border-radius:10px; color:#fff }

.cat-chemical { background:linear-gradient(135deg,#e3f2fd,#bbdefb) }
.cat-glass { background:linear-gradient(135deg,#e8f5e9,#c8e6c9) }
.cat-bio { background:linear-gradient(135deg,#fce4ec,#f8bbd0) }
.cat-physics { background:linear-gradient(135deg,#fff3e0,#ffe0b2) }
.cat-safety { background:linear-gradient(135deg,#f3e5f5,#e1bee7) }
.cat-other { background:linear-gradient(135deg,#eceff1,#cfd8dc) }

.stock-sufficient { background:#27ae60 }
.stock-low { background:#f39c12 }
.stock-critical { background:#e74c3c }
.text-sufficient { color:#27ae60 }
.text-low { color:#f39c12 }
.text-critical { color:#e74c3c }

.card-info { padding:10px 14px 14px }
.card-name { font-size:15px; font-weight:600; color:#1a1a2e; margin-bottom:2px; white-space:nowrap; overflow:hidden; text-overflow:ellipsis }
.card-spec { font-size:12px; color:#888; margin-bottom:8px; white-space:nowrap; overflow:hidden; text-overflow:ellipsis }
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

<!-- 弹窗样式 (非 scoped) -->
<style>
.modal-overlay { position:fixed; inset:0; background:rgba(0,0,0,.35); display:flex; align-items:center; justify-content:center; z-index:1100; padding:20px }
.modal-panel { background:#fff; border-radius:18px; width:100%; max-width:680px; max-height:85vh; display:flex; flex-direction:column; box-shadow:0 24px 64px rgba(0,0,0,.18) }
.modal-header { display:flex; align-items:flex-start; justify-content:space-between; padding:24px 28px 0; flex-shrink:0 }
.modal-title-row { display:flex; align-items:center; gap:10px; flex-wrap:wrap }
.modal-title-row h3 { margin:0; font-size:18px; font-weight:600; color:#1a1a2e }
.detail-tag { font-size:11px; font-weight:500; padding:3px 10px; border-radius:4px; background:rgba(0,0,0,.06); color:#666 }
.detail-status { font-size:11px; font-weight:600; padding:3px 10px; border-radius:10px; color:#fff }
.modal-close { background:none; border:none; cursor:pointer; color:#aaa; padding:4px; border-radius:6px; transition:.15s; flex-shrink:0 }
.modal-close:hover { background:rgba(0,0,0,.05); color:#333 }

.modal-body { padding:20px 28px; overflow-y:auto; flex:1 }

.detail-grid { display:grid; grid-template-columns:repeat(3,1fr); gap:14px; margin-bottom:16px }
.detail-item { display:flex; flex-direction:column; gap:2px }
.detail-label { font-size:12px; color:#999 }
.detail-value { font-size:14px; color:#333; font-weight:500 }

.detail-desc { margin-bottom:16px }
.detail-desc p { margin:4px 0 0; font-size:13px; color:#666; line-height:1.6 }

.stock-overview { margin-bottom:20px; padding:14px 16px; background:rgba(0,0,0,.02); border-radius:10px }
.stock-overview-header { display:flex; justify-content:space-between; align-items:center; margin-bottom:8px; font-size:13px; color:#555 }
.stock-overview-num { font-weight:500 }
.stock-bar-wrap.large { height:8px; border-radius:4px }

.batch-section { border:1px solid rgba(0,0,0,.06); border-radius:10px; overflow:hidden }
.batch-header { display:flex; align-items:center; justify-content:space-between; padding:12px 16px; background:rgba(0,0,0,.02); font-size:13px; font-weight:500; color:#555; cursor:pointer; user-select:none; transition:.15s }
.batch-header:hover { background:rgba(0,0,0,.04) }
.batch-toggle { font-size:12px; color:#1a73e8 }

.batch-table-wrap { overflow-x:auto }
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
