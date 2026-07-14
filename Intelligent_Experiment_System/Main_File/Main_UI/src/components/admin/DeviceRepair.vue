<template>
  <div class="dr-page">
    <div class="page-header">
      <h2>维修管理</h2>
      <p>故障报修、维修登记、费用录入</p>
    </div>

    <!-- 统计卡片 -->
    <div class="stats-row">
      <div class="stat-card">
        <div class="stat-num orange">{{ pendingCount }}</div>
        <div class="stat-label">待处理</div>
      </div>
      <div class="stat-card">
        <div class="stat-num blue">{{ processingCount }}</div>
        <div class="stat-label">维修中</div>
      </div>
      <div class="stat-card">
        <div class="stat-num red">{{ todayNew }}</div>
        <div class="stat-label">今日新增</div>
      </div>
      <div class="stat-card">
        <div class="stat-num">{{ '¥' + totalCost.toLocaleString() }}</div>
        <div class="stat-label">本月费用</div>
      </div>
    </div>

    <!-- 筛选 + 新增 -->
    <div class="toolbar">
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
      <div class="toolbar-right">
        <input v-model="searchNo" placeholder="搜索维修单号..." class="search-input" @keyup.enter="pageNum = 1" />
        <button class="add-btn" @click="openCreateDialog">+ 登记维修</button>
      </div>
    </div>

    <!-- 维修卡片列表 -->
    <div class="card-list" v-if="pagedList.length > 0">
      <div
        v-for="r in pagedList"
        :key="r.id"
        class="repair-card"
        :class="'card-' + r.status"
      >
        <div class="card-left">
          <div class="card-header">
            <span class="status-badge" :class="'badge-' + r.status">{{ r.statusName }}</span>
            <span class="repair-no">{{ r.repairNo }}</span>
            <span class="repair-time">{{ r.reportTime }}</span>
          </div>
          <div class="card-body">
            <div class="card-row">
              <span class="ci-label">设备</span>
              <span class="ci-value device-link">{{ r.deviceName }}（{{ r.deviceNo }}）</span>
            </div>
            <div class="card-row">
              <span class="ci-label">故障描述</span>
              <span class="ci-value fault-desc">{{ r.faultDescription }}</span>
            </div>
            <div class="card-row">
              <span class="ci-label">报修人</span>
              <span class="ci-value">
                {{ r.reporterName }}
                <span class="person-type" :class="r.reporterType === 1 ? 'type-stu' : 'type-tea'">{{ r.reporterTypeName }}</span>
              </span>
              <span class="ci-sep">|</span>
              <span class="ci-label">实验室</span>
              <span class="ci-value">{{ r.labName }}</span>
            </div>
            <div v-if="r.handlerName" class="card-row">
              <span class="ci-label">处理人</span>
              <span class="ci-value">{{ r.handlerName }}</span>
              <template v-if="r.totalCost > 0">
                <span class="ci-sep">|</span>
                <span class="ci-label">累计费用</span>
                <span class="ci-value cost-val">¥{{ r.totalCost.toLocaleString() }}</span>
              </template>
            </div>
            <!-- 费用明细列表 -->
            <div v-if="r.costItems.length > 0" class="cost-list">
              <div v-for="(c, i) in r.costItems" :key="i" class="cost-item">
                <span class="cost-amount">+¥{{ c.amount.toLocaleString() }}</span>
                <span class="cost-remark">{{ c.remark || '无备注' }}</span>
                <span class="cost-time">{{ c.createdAt }}</span>
              </div>
            </div>
          </div>
        </div>
        <div class="card-right">
          <!-- 待处理 -->
          <template v-if="r.status === 'pending'">
            <button class="btn-cancel" @click="cancelRepair(r)">取消</button>
            <button class="btn-primary" @click="startRepair(r)">接单维修</button>
          </template>
          <!-- 维修中 -->
          <template v-else-if="r.status === 'processing'">
            <button class="btn-outline" @click="openAddCost(r)">登记费用</button>
            <button class="btn-primary green" @click="markFixed(r)">标记已修复</button>
          </template>
          <!-- 已修复 -->
          <template v-else-if="r.status === 'fixed'">
            <button class="btn-outline orange" @click="returnToRepair(r)">退回维修</button>
            <button class="btn-primary green" @click="acceptRepair(r)">验收通过</button>
          </template>
          <!-- 已验收 / 已取消 -->
          <span v-else class="final-text">{{ r.status === 'accepted' ? '✅ 已完结' : '已取消' }}</span>
        </div>
      </div>
    </div>

    <div v-else class="empty-state">
      <svg viewBox="0 0 24 24" width="48" height="48" fill="none" stroke="#ccc" stroke-width="1.2"><path d="M10.29 3.86L1.82 18a2 2 0 001.71 3h16.94a2 2 0 001.71-3L13.71 3.86a2 2 0 00-3.42 0z"/><line x1="12" y1="9" x2="12" y2="13"/><line x1="12" y1="17" x2="12.01" y2="17"/></svg>
      <p>暂无维修记录</p>
    </div>

    <div class="pagination" v-if="filteredList.length > pageSize">
      <button :disabled="pageNum <= 1" @click="pageNum--">上一页</button>
      <span>{{ pageNum }} / {{ totalPages }}</span>
      <button :disabled="pageNum >= totalPages" @click="pageNum++">下一页</button>
    </div>

    <!-- 登记费用弹窗 -->
    <teleport to="body">
      <transition name="modal-fade">
        <div v-if="showCost" class="modal-overlay" @click.self="showCost = false">
          <div class="modal-panel">
            <div class="modal-header">
              <h3>登记维修费用</h3>
              <button class="modal-close" @click="showCost = false"><svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2"><path d="M18 6L6 18M6 6l12 12"/></svg></button>
            </div>
            <div class="modal-body">
              <div class="confirm-info">
                <div class="ci-row"><span>维修单</span><span>{{ currentRepair?.repairNo }}</span></div>
                <div class="ci-row"><span>设备</span><span>{{ currentRepair?.deviceName }}</span></div>
              </div>
              <div class="form-g">
                <label>费用金额 <span class="required">*</span></label>
                <input v-model.number="costForm.amount" type="number" class="form-input" placeholder="0" min="0" step="0.01" />
              </div>
              <div class="form-g">
                <label>费用说明</label>
                <input v-model="costForm.remark" class="form-input" placeholder="如：更换电源模块" maxlength="100" />
              </div>
            </div>
            <div class="modal-footer">
              <button class="modal-btn cancel" @click="showCost = false">取消</button>
              <button class="modal-btn primary" @click="doAddCost" :disabled="!costForm.amount || costForm.amount <= 0">确认登记</button>
            </div>
          </div>
        </div>
      </transition>
    </teleport>

    <!-- 登记维修弹窗 -->
    <teleport to="body">
      <transition name="modal-fade">
        <div v-if="showCreate" class="modal-overlay" @click.self="showCreate = false">
          <div class="modal-panel wide">
            <div class="modal-header">
              <h3>登记维修记录</h3>
              <button class="modal-close" @click="showCreate = false"><svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2"><path d="M18 6L6 18M6 6l12 12"/></svg></button>
            </div>
            <div class="modal-body">
              <div class="form-g">
                <label>选择设备 <span class="required">*</span></label>
                <select v-model="createForm.deviceId" class="form-input">
                  <option :value="0" disabled>请选择设备</option>
                  <option v-for="d in deviceOptions" :key="d.id" :value="d.id">{{ d.name }}（{{ d.deviceNo }}）</option>
                </select>
              </div>
              <div class="form-row-2">
                <div class="form-g">
                  <label>报修人 <span class="required">*</span></label>
                  <input v-model="createForm.reporterName" class="form-input" placeholder="输入报修人姓名" />
                </div>
                <div class="form-g">
                  <label>维修状态</label>
                  <select v-model="createForm.initStatus" class="form-input">
                    <option value="pending">待处理</option>
                    <option value="processing">维修中</option>
                    <option value="fixed">已修复</option>
                  </select>
                </div>
              </div>
              <div class="form-g">
                <label>故障描述 <span class="required">*</span></label>
                <textarea v-model="createForm.faultDescription" class="form-input form-textarea" rows="3" placeholder="请描述设备故障现象..." maxlength="500" />
              </div>
            </div>
            <div class="modal-footer">
              <button class="modal-btn cancel" @click="showCreate = false">取消</button>
              <button class="modal-btn primary" @click="doCreate" :disabled="!canCreate">确认登记</button>
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

type RepairStatus = 'pending' | 'processing' | 'fixed' | 'accepted' | 'cancelled'

interface CostItem { amount: number; remark: string; createdAt: string }
interface Repair {
  id: number; repairNo: string; deviceId: number; deviceName: string; deviceNo: string
  labName: string; reporterName: string; reporterType: number; reporterTypeName: string
  handlerName: string | null; faultDescription: string; reportTime: string
  status: RepairStatus; statusName: string; totalCost: number; costItems: CostItem[]
}

const MOCK_REPAIRS: Repair[] = [
  { id:1, repairNo:'WX-2024-001', deviceId:3, deviceName:'恒温培养箱', deviceNo:'EQ-2023-015',
    labName:'生物培养实验室', reporterName:'孙老师', reporterType:2, reporterTypeName:'教师',
    handlerName:null, faultDescription:'温控异常，显示温度与实际温度偏差超过±2°C，影响培养实验结果。',
    reportTime:'2026-07-08 09:30', status:'pending', statusName:'待处理', totalCost:0, costItems:[] },
  { id:2, repairNo:'WX-2024-002', deviceId:6, deviceName:'显微镜', deviceNo:'EQ-2022-005',
    labName:'化学分析实验室', reporterName:'赵同学', reporterType:1, reporterTypeName:'学生',
    handlerName:null, faultDescription:'物镜转换器卡滞，无法顺畅切换不同倍数物镜。',
    reportTime:'2026-07-08 14:00', status:'pending', statusName:'待处理', totalCost:0, costItems:[] },
  { id:3, repairNo:'WX-2024-003', deviceId:4, deviceName:'分光光度计', deviceNo:'EQ-2022-008',
    labName:'物理光学实验室', reporterName:'李老师', reporterType:2, reporterTypeName:'教师',
    handlerName:'王管理员', faultDescription:'光源老化严重，波长校准异常，测量结果偏差较大。',
    reportTime:'2026-07-07 10:00', status:'processing', statusName:'维修中', totalCost:3200,
    costItems:[{ amount:3200, remark:'厂商更换氘灯及钨灯光源', createdAt:'2026-07-08 11:00' }] },
  { id:4, repairNo:'WX-2024-004', deviceId:1, deviceName:'高速离心机', deviceNo:'EQ-2024-001',
    labName:'化学分析实验室', reporterName:'王同学', reporterType:1, reporterTypeName:'学生',
    handlerName:'王管理员', faultDescription:'运行时异响，转速不稳定，无法达到设定转速。',
    reportTime:'2026-07-06 16:00', status:'processing', statusName:'维修中', totalCost:0, costItems:[] },
  { id:5, repairNo:'WX-2024-005', deviceId:7, deviceName:'服务器机柜', deviceNo:'EQ-2023-020',
    labName:'计算机综合实验室', reporterName:'周老师', reporterType:2, reporterTypeName:'教师',
    handlerName:'王管理员', faultDescription:'散热风扇故障，机柜内温度过高报警。',
    reportTime:'2026-07-05 08:00', status:'fixed', statusName:'已修复', totalCost:1500,
    costItems:[{ amount:1500, remark:'更换散热风扇模块×2', createdAt:'2026-07-05 16:00' }] },
  { id:6, repairNo:'WX-2024-006', deviceId:2, deviceName:'数字示波器', deviceNo:'EQ-2024-002',
    labName:'物理光学实验室', reporterName:'张同学', reporterType:1, reporterTypeName:'学生',
    handlerName:'王管理员', faultDescription:'屏幕显示异常，出现竖条纹。',
    reportTime:'2026-07-04 11:00', status:'accepted', statusName:'已验收', totalCost:800,
    costItems:[{ amount:800, remark:'更换显示屏排线', createdAt:'2026-07-04 15:00' }] },
  { id:7, repairNo:'WX-2024-007', deviceId:5, deviceName:'电子天平', deviceNo:'EQ-2024-003',
    labName:'化学分析实验室', reporterName:'陈同学', reporterType:1, reporterTypeName:'学生',
    handlerName:null, faultDescription:'称量数值漂移，零点不稳定。',
    reportTime:'2026-07-03 10:00', status:'cancelled', statusName:'已取消', totalCost:0, costItems:[] },
]

const repairs = ref<Repair[]>([])

async function loadData() {
  if (isMockMode.value) { repairs.value = [...MOCK_REPAIRS]; return }
  try {
    const res = await api.get('/equipment/repairs', { params: { pageNum: 1, pageSize: 100 } })
    if (res.data?.data?.records?.length) {
      repairs.value = res.data.data.records.map((r: any) => ({
        id: r.id, repairNo: r.repairNo || '', deviceId: r.deviceId || 0, deviceName: r.deviceName || '', deviceNo: r.deviceNo || '',
        labName: r.labName || '', reporterName: r.reporterName || '', reporterType: r.reporterType || 1, reporterTypeName: r.reporterType === 2 ? '教师' : '学生',
        handlerName: r.handlerName || null, faultDescription: r.faultDescription || r.faultDesc || '', reportTime: r.reportTime || '',
        status: mapRepairStatus(r.repairStatus ?? r.status), statusName: r.repairStatusDesc || r.statusName || '待处理',
        totalCost: Number(r.totalCost || (r.costs || []).reduce((sum: number, item: any) => sum + Number(item.cost || 0), 0)),
        costItems: (r.costItems || r.costs || []).map((item: any) => ({
          amount: Number(item.amount || item.cost || 0),
          remark: item.remark || '',
          createdAt: item.createdAt || ''
        }))
      }))
    } else repairs.value = [...MOCK_REPAIRS]
  } catch { /* API 不可用，请检查后端服务 */ }
}

onMounted(() => loadData())

const deviceOptions = [
  { id:1, name:'高速离心机', deviceNo:'EQ-2024-001' },
  { id:2, name:'数字示波器', deviceNo:'EQ-2024-002' },
  { id:3, name:'恒温培养箱', deviceNo:'EQ-2023-015' },
  { id:4, name:'分光光度计', deviceNo:'EQ-2022-008' },
  { id:5, name:'电子天平', deviceNo:'EQ-2024-003' },
  { id:6, name:'显微镜', deviceNo:'EQ-2022-005' },
  { id:7, name:'服务器机柜', deviceNo:'EQ-2023-020' },
]

// 筛选 & 分页
const activeStatus = ref<RepairStatus | 'all'>('all')
const searchNo = ref('')
const pageNum = ref(1)
const pageSize = 6

const statusTabs = computed(() => [
  { key: 'all' as const, label: '全部', count: repairs.value.length },
  { key: 'pending' as const, label: '待处理', count: repairs.value.filter(r => r.status === 'pending').length },
  { key: 'processing' as const, label: '维修中', count: repairs.value.filter(r => r.status === 'processing').length },
  { key: 'fixed' as const, label: '已修复', count: repairs.value.filter(r => r.status === 'fixed').length },
  { key: 'accepted' as const, label: '已验收', count: repairs.value.filter(r => r.status === 'accepted').length },
  { key: 'cancelled' as const, label: '已取消', count: repairs.value.filter(r => r.status === 'cancelled').length },
])

const filteredList = computed(() => {
  let list = repairs.value
  if (activeStatus.value !== 'all') list = list.filter(r => r.status === activeStatus.value)
  if (searchNo.value) list = list.filter(r => r.repairNo.toLowerCase().includes(searchNo.value.toLowerCase()))
  return list
})
const totalPages = computed(() => Math.ceil(filteredList.value.length / pageSize) || 1)
const pagedList = computed(() => filteredList.value.slice((pageNum.value - 1) * pageSize, pageNum.value * pageSize))

const pendingCount = computed(() => repairs.value.filter(r => r.status === 'pending').length)
const processingCount = computed(() => repairs.value.filter(r => r.status === 'processing').length)
const todayNew = computed(() => repairs.value.filter(r => r.reportTime.startsWith('2026-07-09')).length)
const totalCost = computed(() => repairs.value.reduce((s, r) => s + r.totalCost, 0))

// 操作
function mapRepairStatus(status: number | string): RepairStatus {
  const numericMap: Record<number, RepairStatus> = { 1: 'pending', 2: 'processing', 3: 'fixed', 4: 'accepted' }
  if (typeof status === 'number') return numericMap[status] || 'pending'
  return (status || 'pending') as RepairStatus
}

async function startRepair(r: Repair) {
  if (isMockMode.value) { r.status = 'processing'; r.statusName = '维修中'; r.handlerName = '王管理员'; return }
  try {
    await api.post(`/equipment/repairs/${r.repairNo}/start`, {})
    await loadData()
  } catch (e: any) { alert(e.message || '操作失败') }
}
function cancelRepair(r: Repair) {
  if (!confirm('确定取消该维修单吗？')) return
  if (!isMockMode.value) { alert('后端暂未提供取消维修单接口'); return }
  r.status = 'cancelled'; r.statusName = '已取消'
}
async function markFixed(r: Repair) {
  if (isMockMode.value) { r.status = 'fixed'; r.statusName = '已修复'; return }
  try {
    await api.post(`/equipment/repairs/${r.repairNo}/complete`, { repairDesc: '维修完成' })
    await loadData()
  } catch (e: any) { alert(e.message || '操作失败') }
}
async function acceptRepair(r: Repair) {
  if (isMockMode.value) { r.status = 'accepted'; r.statusName = '已验收'; return }
  try {
    await api.post(`/equipment/repairs/${r.repairNo}/accept`)
    await loadData()
  } catch (e: any) { alert(e.message || '操作失败') }
}
function returnToRepair(r: Repair) { r.status = 'processing'; r.statusName = '维修中' }

// 费用弹窗
const showCost = ref(false)
const currentRepair = ref<Repair | null>(null)
const costForm = reactive({ amount: 0, remark: '' })
function openAddCost(r: Repair) { currentRepair.value = r; costForm.amount = 0; costForm.remark = ''; showCost.value = true }
async function doAddCost() {
  if (!currentRepair.value || !costForm.amount || costForm.amount <= 0) return
  const r = currentRepair.value
  if (!isMockMode.value) {
    try {
      await api.post(`/equipment/repairs/${r.repairNo}/cost`, {
        cost: costForm.amount,
        remark: costForm.remark
      })
      showCost.value = false
      await loadData()
    } catch (e: any) { alert(e.message || '操作失败') }
    return
  }
  r.costItems.push({ amount: costForm.amount, remark: costForm.remark, createdAt: '2026-07-09 ' + new Date().toTimeString().slice(0, 5) })
  r.totalCost = r.costItems.reduce((s, c) => s + c.amount, 0)
  showCost.value = false
}

// 登记维修弹窗
const showCreate = ref(false)
const createForm = reactive({ deviceId: 0, reporterName: '', faultDescription: '', initStatus: 'pending' as RepairStatus })
const canCreate = computed(() => createForm.deviceId > 0 && createForm.reporterName.trim() && createForm.faultDescription.trim())
function openCreateDialog() { createForm.deviceId = 0; createForm.reporterName = ''; createForm.faultDescription = ''; createForm.initStatus = 'pending'; showCreate.value = true }
async function doCreate() {
  if (!canCreate.value) return
  const d = deviceOptions.find(d => d.id === createForm.deviceId)!
  if (!isMockMode.value) {
    try {
      await api.post('/equipment/repairs', {
        deviceId: createForm.deviceId,
        faultDesc: createForm.faultDescription.trim()
      })
      showCreate.value = false
      await loadData()
    } catch (e: any) { alert(e.message || '操作失败') }
    return
  }
  const statusLabels: Record<string, string> = { pending: '待处理', processing: '维修中', fixed: '已修复', accepted: '已验收', cancelled: '已取消' }
  repairs.value.unshift({
    id: Date.now(), repairNo: 'WX-' + new Date().toISOString().slice(0, 10).replace(/-/g, '') + '-' + String(repairs.value.length + 1).padStart(3, '0'),
    deviceId: d.id, deviceName: d.name, deviceNo: d.deviceNo, labName:'未知实验室',
    reporterName: createForm.reporterName.trim(), reporterType:1, reporterTypeName:'学生',
    handlerName: createForm.initStatus !== 'pending' ? '王管理员' : null,
    faultDescription: createForm.faultDescription.trim(), reportTime:'2026-07-09 ' + new Date().toTimeString().slice(0, 5),
    status: createForm.initStatus, statusName: statusLabels[createForm.initStatus], totalCost:0, costItems:[]
  })
  showCreate.value = false
}
</script>

<style scoped>
.dr-page { height:100%; overflow-y:auto; padding:20px 24px }
.page-header { margin-bottom:16px }
.page-header h2 { font-size:20px; font-weight:600; color:#1a1a2e; margin:0 }
.page-header p { margin:4px 0 0; font-size:13px; color:#999 }

.stats-row { display:flex; gap:12px; margin-bottom:16px; flex-wrap:wrap }
.stat-card { background:rgba(255,255,255,.65); backdrop-filter:blur(10px); border:1px solid rgba(0,0,0,.06); border-radius:12px; padding:14px 20px; min-width:100px; text-align:center }
.stat-num { font-size:24px; font-weight:700; color:#1a1a2e; line-height:1.1 }
.stat-label { font-size:12px; color:#999; margin-top:2px }
.orange { color:#f39c12 }
.blue { color:#1a73e8 }
.red { color:#e74c3c }

.toolbar { display:flex; align-items:center; justify-content:space-between; margin-bottom:18px; gap:12px; flex-wrap:wrap }
.filter-tabs { display:flex; gap:4px; background:rgba(255,255,255,.4); border-radius:10px; padding:4px; flex-wrap:wrap }
.filter-tab { padding:8px 16px; border-radius:8px; font-size:13px; color:#666; cursor:pointer; transition:all .15s; display:flex; align-items:center; gap:6px; user-select:none; white-space:nowrap }
.filter-tab:hover { color:#333; background:rgba(0,0,0,.03) }
.filter-tab.active { background:#fff; color:#1a73e8; font-weight:600; box-shadow:0 1px 3px rgba(0,0,0,.08) }
.tab-count { font-size:11px; background:rgba(0,0,0,.06); padding:1px 7px; border-radius:10px; color:#888 }
.filter-tab.active .tab-count { background:rgba(26,115,232,.1); color:#1a73e8 }
.toolbar-right { display:flex; align-items:center; gap:10px }
.search-input { padding:8px 12px; border:1px solid rgba(0,0,0,.1); border-radius:10px; font-size:13px; outline:none; width:170px; background:rgba(255,255,255,.7) }
.add-btn { padding:8px 18px; border:none; border-radius:10px; background:linear-gradient(135deg,#1a73e8,#4a90d9); color:#fff; font-size:13px; font-weight:500; cursor:pointer; white-space:nowrap }

.card-list { display:flex; flex-direction:column; gap:12px }
.repair-card { background:rgba(255,255,255,.55); backdrop-filter:blur(10px); border-radius:12px; border:1px solid rgba(0,0,0,.04); display:flex; align-items:stretch; overflow:hidden }
.repair-card.card-pending { border-left:3px solid #f39c12 }
.repair-card.card-processing { border-left:3px solid #1a73e8 }
.repair-card.card-fixed { border-left:3px solid #27ae60 }
.repair-card.card-accepted { border-left:3px solid #27ae60 }
.repair-card.card-cancelled { border-left:3px solid #ccc; opacity:.65 }

.card-left { flex:1; padding:16px 20px; min-width:0 }
.card-right { display:flex; flex-direction:column; align-items:flex-end; justify-content:center; gap:8px; padding:16px 20px; flex-shrink:0; min-width:140px; border-left:1px solid rgba(0,0,0,.04) }

.card-header { display:flex; align-items:center; gap:10px; margin-bottom:10px }
.status-badge { padding:2px 8px; border-radius:4px; font-size:11px; font-weight:600 }
.badge-pending { background:rgba(243,156,18,.1); color:#e67e22 }
.badge-processing { background:rgba(26,115,232,.1); color:#1a73e8 }
.badge-fixed { background:rgba(39,174,96,.1); color:#27ae60 }
.badge-accepted { background:rgba(39,174,96,.15); color:#1e8449 }
.badge-cancelled { background:rgba(0,0,0,.05); color:#999 }
.repair-no { font-size:13px; font-weight:600; color:#1a1a2e }
.repair-time { font-size:12px; color:#999; margin-left:auto }

.card-body { display:flex; flex-direction:column; gap:5px }
.card-row { display:flex; align-items:baseline; gap:6px; font-size:13px; flex-wrap:wrap }
.ci-label { color:#999; min-width:52px; flex-shrink:0 }
.ci-value { color:#333 }
.device-link { color:#1a73e8; font-weight:500 }
.fault-desc { color:#555; max-width:400px; overflow:hidden; text-overflow:ellipsis; white-space:nowrap }
.person-type { padding:1px 6px; border-radius:3px; font-size:11px; margin-left:4px }
.type-stu { background:rgba(76,175,80,.1); color:#2e7d32 }
.type-tea { background:rgba(26,115,232,.1); color:#1557b0 }
.ci-sep { color:#ddd }
.cost-val { color:#e67e22; font-weight:600 }

.cost-list { margin-top:6px; padding:8px 12px; background:rgba(0,0,0,.015); border-radius:8px }
.cost-item { display:flex; align-items:center; gap:10px; font-size:12px; padding:2px 0 }
.cost-amount { color:#e67e22; font-weight:600; min-width:70px }
.cost-remark { color:#666; flex:1 }
.cost-time { color:#bbb; font-size:11px }

.btn-primary { padding:9px 20px; border:none; border-radius:10px; background:linear-gradient(135deg,#1a73e8,#4a90d9); color:#fff; font-size:13px; font-weight:500; cursor:pointer; white-space:nowrap }
.btn-primary:hover { box-shadow:0 4px 12px rgba(26,115,232,.3) }
.btn-primary.green { background:linear-gradient(135deg,#27ae60,#2ecc71) }
.btn-primary.green:hover { box-shadow:0 4px 12px rgba(39,174,96,.3) }
.btn-outline { padding:9px 20px; border:1px solid rgba(0,0,0,.12); border-radius:10px; background:#fff; color:#555; font-size:13px; cursor:pointer; white-space:nowrap }
.btn-outline:hover { border-color:#1a73e8; color:#1a73e8 }
.btn-outline.orange { border-color:rgba(243,156,18,.3); color:#e67e22 }
.btn-outline.orange:hover { border-color:#e67e22; background:rgba(243,156,18,.04) }
.btn-cancel { padding:8px 16px; border:1px solid rgba(0,0,0,.1); border-radius:8px; background:#fff; color:#999; font-size:12px; cursor:pointer }
.btn-cancel:hover { border-color:#e74c3c; color:#e74c3c }
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
.modal-panel.wide { max-width:560px }
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
.confirm-info { background:rgba(0,0,0,.02); border-radius:10px; padding:12px 16px }
.ci-row { display:flex; justify-content:space-between; padding:4px 0; font-size:13px; color:#555 }
.ci-row span:first-child { color:#999 }
.form-g { display:flex; flex-direction:column; gap:4px }
.form-g label { font-size:13px; font-weight:500; color:#555 }
.required { color:#e74c3c }
.form-input { padding:10px 14px; border:1px solid rgba(0,0,0,.1); border-radius:10px; font-size:14px; color:#333; outline:none; background:rgba(255,255,255,.7); transition:.15s; font-family:inherit; width:100%; box-sizing:border-box }
.form-input:focus { border-color:#1a73e8 }
.form-textarea { resize:vertical; min-height:60px }
.form-row-2 { display:grid; grid-template-columns:1fr 1fr; gap:14px }
.modal-fade-enter-active { transition:all .2s ease }
.modal-fade-leave-active { transition:all .15s ease }
.modal-fade-enter-from,.modal-fade-leave-to { opacity:0 }
.modal-fade-enter-from .modal-panel { transform:scale(.94) translateY(12px) }
</style>
