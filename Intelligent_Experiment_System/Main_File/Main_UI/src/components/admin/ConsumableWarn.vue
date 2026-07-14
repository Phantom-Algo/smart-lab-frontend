<template>
  <div class="cw-page">
    <div class="page-header">
      <h2>库存预警</h2>
      <p>低库存、保质期过期预警</p>
    </div>

    <div class="stats-row">
      <div class="stat-card"><div class="stat-num red">{{ lowStockItems.length }}</div><div class="stat-label">低库存</div></div>
      <div class="stat-card"><div class="stat-num orange">{{ nearExpiryItems.length }}</div><div class="stat-label">临期批次</div></div>
      <div class="stat-card"><div class="stat-num red">{{ expiredItems.length }}</div><div class="stat-label">已过期</div></div>
      <div class="stat-card"><div class="stat-num blue">{{ totalWarnings }}</div><div class="stat-label">预警总数</div></div>
    </div>

    <div class="two-col">
      <!-- 低库存预警 -->
      <div class="warn-section">
        <div class="section-title">低库存预警</div>
        <div class="warn-list" v-if="lowStockItems.length > 0">
          <div v-for="item in lowStockItems" :key="item.id" class="warn-card" :class="item.stock < item.safetyStock ? 'level-danger' : 'level-warn'">
            <div class="warn-header">
              <span class="warn-dot" :class="item.stock < item.safetyStock ? 'dot-danger' : 'dot-warn'"></span>
              <span class="warn-name">{{ item.name }}</span>
              <span class="warn-code">{{ item.itemNo }}</span>
              <span class="cat-tag" :class="'cat-' + item.category">{{ item.categoryName }}</span>
            </div>
            <div class="warn-body">
              <div class="stock-bar-wrap">
                <div class="stock-bar"><div class="stock-fill" :class="item.stock < item.safetyStock ? 'fill-danger' : 'fill-warn'" :style="{ width: stockPct(item) + '%' }"></div></div>
              </div>
              <div class="stock-info-row">
                <span>库存 <strong :class="item.stock < item.safetyStock ? 'text-danger' : 'text-warn'">{{ item.stock }}{{ item.unit }}</strong></span>
                <span>/ 安全库存 {{ item.safetyStock }}{{ item.unit }}</span>
                <span v-if="item.stock < item.safetyStock" class="gap-text">缺口 {{ item.safetyStock - item.stock }}{{ item.unit }}</span>
              </div>
            </div>
          </div>
        </div>
        <div v-else class="empty-hint">暂无低库存预警</div>
      </div>

      <!-- 保质期预警 -->
      <div class="warn-section">
        <div class="section-title">保质期预警</div>
        <div class="warn-list" v-if="expiryAlerts.length > 0">
          <div v-for="b in expiryAlerts" :key="b.batchNo" class="warn-card" :class="b.expired ? 'level-danger' : 'level-warn'">
            <div class="warn-header">
              <span class="warn-dot" :class="b.expired ? 'dot-danger' : 'dot-warn'"></span>
              <span class="warn-name">{{ b.consumableName }}</span>
              <span class="batch-no">{{ b.batchNo }}</span>
            </div>
            <div class="warn-body">
              <div class="expiry-info">
                <span>剩余 <strong>{{ b.remaining }}{{ b.unit }}</strong></span>
                <span class="ci-sep">|</span>
                <span>到期日 {{ b.expiryDate }}</span>
                <span class="expiry-tag" :class="b.expired ? 'tag-expired' : 'tag-near'">{{ b.expired ? '已过期' : '临期' }}</span>
              </div>
            </div>
          </div>
        </div>
        <div v-else class="empty-hint">暂无保质期预警</div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import api from '../../utils/api'
import { getMaterialPage } from '../../utils/materialApi'
import { isMockMode } from '../../stores/authStore'

interface StockWarnItem {
  id: number; itemNo: string; name: string; category: string; categoryName: string
  unit: string; stock: number; safetyStock: number
}
interface ExpiryAlert {
  batchNo: string; consumableName: string; unit: string; remaining: number
  expiryDate: string; expired: boolean
}

const MOCK_LOW_STOCK: StockWarnItem[] = [
  { id:3, itemNo:'CON-003', name:'丁腈手套', category:'protective', categoryName:'防护用品', unit:'盒', stock:8, safetyStock:20 },
  { id:4, itemNo:'CON-004', name:'浓硫酸', category:'reagent', categoryName:'试剂', unit:'瓶', stock:12, safetyStock:10 },
  { id:2, itemNo:'CON-002', name:'烧杯500ml', category:'glassware', categoryName:'玻璃器皿', unit:'个', stock:32, safetyStock:15 },
]

const MOCK_EXPIRY: ExpiryAlert[] = [
  { batchNo:'B20240705-002', consumableName:'无水乙醇', unit:'瓶', remaining:20, expiryDate:'2026-07-01', expired:true },
  { batchNo:'B20240601-001', consumableName:'浓硫酸', unit:'瓶', remaining:12, expiryDate:'2026-06-01', expired:true },
  { batchNo:'B20240520-001', consumableName:'丁腈手套', unit:'盒', remaining:8, expiryDate:'2027-05-01', expired:false },
]

const lowStockItems = ref<StockWarnItem[]>([])
const nearExpiryItems = ref<ExpiryAlert[]>([])

async function loadData() {
  if (isMockMode.value) { lowStockItems.value = [...MOCK_LOW_STOCK]; nearExpiryItems.value = [...MOCK_EXPIRY]; return }
  try {
    // 同时请求预警数据和物资详情，交叉关联
    const [alertsRes, materialsRes] = await Promise.all([
      api.get('/material/alerts', { params: { pageNum: 1, pageSize: 200 } }),
      getMaterialPage({ pageNum: 1, pageSize: 500 })
    ])

    // 构建物资ID → 物资详情的映射表
    const materialMap = new Map<number, any>()
    if (materialsRes?.records) {
      materialsRes.records.forEach((m: any) => materialMap.set(m.id, m))
    }

    const records: any[] = alertsRes.data?.data?.records || []

    // 低库存预警：alertType === 1（后端枚举 LOW_STOCK）
    lowStockItems.value = records
      .filter((r: any) => r.alertType === 1)
      .map((r: any) => {
        const mat = materialMap.get(r.materialId) || {}
        return {
          id: r.id,
          itemNo: mat.materialNo || '',
          name: r.materialName || mat.materialName || '',
          category: '',
          categoryName: '',
          unit: mat.unit || '—',
          stock: mat.currentStock ?? 0,
          safetyStock: mat.safetyStock ?? 10
        }
      })

    // 临期/过期预警：alertType === 2（NEAR_EXPIRY）或 3（EXPIRED）
    nearExpiryItems.value = records
      .filter((r: any) => r.alertType === 2 || r.alertType === 3)
      .map((r: any) => {
        const mat = materialMap.get(r.materialId) || {}
        return {
          batchNo: r.batchNo || mat.earliestBatchNo || '',
          consumableName: r.materialName || mat.materialName || '',
          unit: mat.unit || '—',
          remaining: mat.currentStock ?? 0,
          expiryDate: mat.earliestExpiryDate || '',
          expired: r.alertType === 3
        }
      })
  } catch { /* 请求失败时保持空列表，由模板显示"暂无" */ }
}

onMounted(() => loadData())

const expiredItems = computed(() => expiryAlerts.value.filter(b => b.expired))
const expiryAlerts = computed(() => [...nearExpiryItems.value].sort((a, b) => (a.expired === b.expired ? 0 : a.expired ? -1 : 1)))
const totalWarnings = computed(() => lowStockItems.value.length + expiryAlerts.value.length)

function stockPct(i: StockWarnItem): number {
  return Math.min(100, Math.round(i.stock / (i.safetyStock * 2) * 100))
}
</script>

<style scoped>
.cw-page { height:100%; overflow-y:auto; padding:20px 24px }
.page-header { margin-bottom:16px }
.page-header h2 { font-size:20px; font-weight:600; color:#1a1a2e; margin:0 }
.page-header p { margin:4px 0 0; font-size:13px; color:#999 }

.stats-row { display:flex; gap:12px; margin-bottom:20px; flex-wrap:wrap }
.stat-card { background:rgba(255,255,255,.65); backdrop-filter:blur(10px); border:1px solid rgba(0,0,0,.06); border-radius:12px; padding:14px 20px; min-width:100px; text-align:center }
.stat-num { font-size:24px; font-weight:700; color:#1a1a2e; line-height:1.1 }
.stat-label { font-size:12px; color:#999; margin-top:2px }
.red { color:#e74c3c } .orange { color:#f39c12 } .blue { color:#1a73e8 }

.two-col { display:grid; grid-template-columns:1fr 1fr; gap:16px }
.warn-section { background:rgba(255,255,255,.55); backdrop-filter:blur(10px); border:1px solid rgba(0,0,0,.04); border-radius:14px; padding:18px 20px }
.section-title { font-size:15px; font-weight:600; color:#555; margin-bottom:14px }

.warn-list { display:flex; flex-direction:column; gap:10px }
.warn-card { background:rgba(255,255,255,.6); border-radius:10px; padding:14px 16px; border:1px solid rgba(0,0,0,.04); transition:border-color .15s }
.warn-card.level-danger { border-left:3px solid #e74c3c }
.warn-card.level-warn { border-left:3px solid #f39c12 }

.warn-header { display:flex; align-items:center; gap:8px; margin-bottom:8px }
.warn-dot { width:8px; height:8px; border-radius:50%; flex-shrink:0 }
.dot-danger { background:#e74c3c } .dot-warn { background:#f39c12 }
.warn-name { font-size:14px; font-weight:600; color:#1a1a2e }
.warn-code { font-size:11px; color:#aaa }
.cat-tag { padding:2px 6px; border-radius:3px; font-size:10px; font-weight:500 }
.cat-reagent { background:rgba(142,68,173,.1); color:#8e44ad }
.cat-glassware { background:rgba(26,115,232,.1); color:#1557b0 }
.cat-protective { background:rgba(39,174,96,.1); color:#1e8449 }
.cat-other { background:rgba(0,0,0,.05); color:#888 }
.batch-no { font-size:11px; background:rgba(0,0,0,.04); padding:1px 6px; border-radius:3px; color:#888; margin-left:auto }

.warn-body { }
.stock-bar-wrap { margin-bottom:6px }
.stock-bar { width:100%; height:8px; border-radius:4px; background:rgba(0,0,0,.05); overflow:hidden }
.stock-fill { height:100%; border-radius:4px }
.fill-danger { background:#e74c3c } .fill-warn { background:#f39c12 }
.stock-info-row { display:flex; align-items:center; gap:6px; font-size:12px; color:#888; flex-wrap:wrap }
.text-danger { color:#e74c3c } .text-warn { color:#e67e22 }
.gap-text { color:#e74c3c; font-weight:500; margin-left:auto }

.expiry-info { display:flex; align-items:center; gap:6px; font-size:12px; color:#888; flex-wrap:wrap }
.ci-sep { color:#ddd }
.expiry-tag { padding:1px 6px; border-radius:3px; font-size:10px; font-weight:600 }
.tag-expired { background:rgba(231,76,60,.1); color:#e74c3c }
.tag-near { background:rgba(243,156,18,.1); color:#e67e22 }

.empty-hint { text-align:center; color:#bbb; padding:20px; font-size:13px }

@media (max-width: 1024px) { .two-col { grid-template-columns:1fr } }
</style>
