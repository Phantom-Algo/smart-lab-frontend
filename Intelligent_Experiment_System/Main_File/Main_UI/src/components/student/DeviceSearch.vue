<template>
  <div class="device-page">
    <div class="page-header">
      <h2>设备查询</h2>
      <p>搜索可用设备、查看设备详情</p>
    </div>

    <!-- 搜索与筛选栏 -->
    <div class="filter-bar">
      <div class="search-box">
        <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2"><circle cx="11" cy="11" r="8"/><path d="M21 21l-4.35-4.35"/></svg>
        <input
          v-model="searchText"
          type="text"
          class="search-input"
          placeholder="搜索设备名称或编号..."
        />
        <span v-if="searchText" class="search-clear" @click="searchText = ''">
          <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2"><path d="M18 6L6 18M6 6l12 12"/></svg>
        </span>
      </div>
      <div class="filter-actions">
        <select v-model="statusFilter" class="filter-select">
          <option value="">全部状态</option>
          <option value="idle">闲置</option>
          <option value="borrowed">借用中</option>
          <option value="repair">维修中</option>
        </select>
        <select v-model="labFilter" class="filter-select">
          <option value="">全部实验室</option>
          <option v-for="lab in labOptions" :key="lab" :value="lab">{{ lab }}</option>
        </select>
        <span class="result-count">共 {{ filteredDevices.length }} 台设备</span>
      </div>
    </div>

    <!-- 主体：左列表 + 右详情 -->
    <div class="device-body" v-if="devices.length > 0">
      <!-- 左侧设备列表 -->
      <div class="device-list">
        <div
          v-for="device in filteredDevices"
          :key="device.id"
          class="device-row"
          :class="{ selected: selectedDeviceId === device.id }"
          @click="selectDevice(device.id)"
        >
          <div class="device-icon-box" :class="'icon-status-' + device.status">
            <svg v-if="device.type === '电子测量'" viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="1.6"><rect x="2" y="3" width="20" height="14" rx="2"/><line x1="8" y1="21" x2="16" y2="21"/><line x1="12" y1="17" x2="12" y2="21"/></svg>
            <svg v-else-if="device.type === '生物分析'" viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="1.6"><circle cx="12" cy="12" r="9"/><path d="M12 3v6l4 3"/></svg>
            <svg v-else-if="device.type === '化学仪器'" viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="1.6"><path d="M9 3h6M10 3v4a2 2 0 01-2 2H7M14 3v4a2 2 0 002 2h1"/><rect x="4" y="18" width="16" height="3" rx="1"/><rect x="7" y="9" width="10" height="9" rx="1"/></svg>
            <svg v-else viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="1.6"><rect x="3" y="3" width="18" height="18" rx="3"/><circle cx="12" cy="12" r="4"/></svg>
          </div>
          <div class="device-row-info">
            <div class="device-row-top">
              <span class="device-row-name">{{ device.name }}</span>
              <span class="device-row-no">{{ device.deviceNo }}</span>
            </div>
            <div class="device-row-meta">
              <span class="device-row-lab">{{ device.labName }}</span>
            </div>
          </div>
          <div class="device-row-right">
            <span class="status-tag" :class="'tag-' + device.status">{{ statusLabel(device.status) }}</span>
            <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="#ccc" stroke-width="2"><polyline points="9 18 15 12 9 6"/></svg>
          </div>
        </div>
      </div>

      <!-- 右侧详情面板 -->
      <transition name="panel-fade">
        <div v-if="selectedDevice" class="detail-panel">
          <div class="panel-card">
            <!-- 设备名称标题 -->
            <div class="panel-device-header">
              <div class="panel-device-icon" :class="'icon-status-' + selectedDevice.status">
                <svg v-if="selectedDevice.type === '电子测量'" viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" stroke-width="1.6"><rect x="2" y="3" width="20" height="14" rx="2"/><line x1="8" y1="21" x2="16" y2="21"/><line x1="12" y1="17" x2="12" y2="21"/></svg>
                <svg v-else-if="selectedDevice.type === '生物分析'" viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" stroke-width="1.6"><circle cx="12" cy="12" r="9"/><path d="M12 3v6l4 3"/></svg>
                <svg v-else-if="selectedDevice.type === '化学仪器'" viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" stroke-width="1.6"><path d="M9 3h6M10 3v4a2 2 0 01-2 2H7M14 3v4a2 2 0 002 2h1"/><rect x="4" y="18" width="16" height="3" rx="1"/><rect x="7" y="9" width="10" height="9" rx="1"/></svg>
                <svg v-else viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" stroke-width="1.6"><rect x="3" y="3" width="18" height="18" rx="3"/><circle cx="12" cy="12" r="4"/></svg>
              </div>
              <div>
                <h3 class="panel-device-name">{{ selectedDevice.name }}</h3>
                <p class="panel-device-no">{{ selectedDevice.deviceNo }}</p>
              </div>
            </div>

            <!-- 状态 + 健康分 -->
            <div class="panel-status-row">
              <span class="status-tag large" :class="'tag-' + selectedDevice.status">{{ statusLabel(selectedDevice.status) }}</span>
              <div class="health-info">
                <span class="health-label">健康分</span>
                <span class="health-value" :class="healthClass(selectedDevice.healthScore)">{{ selectedDevice.healthScore }}</span>
                <div class="health-bar">
                  <div class="health-fill" :class="healthClass(selectedDevice.healthScore)" :style="{ width: selectedDevice.healthScore + '%' }"></div>
                </div>
              </div>
            </div>

            <!-- 技术参数 -->
            <div class="panel-section">
              <h4 class="panel-section-title">技术参数</h4>
              <div class="spec-list">
                <div v-for="(value, key) in selectedDevice.spec" :key="key" class="spec-row">
                  <span class="spec-key">{{ key }}</span>
                  <span class="spec-value">{{ value }}</span>
                </div>
              </div>
            </div>

            <!-- 基本信息 -->
            <div class="panel-section">
              <h4 class="panel-section-title">基本信息</h4>
              <div class="info-grid-2col">
                <div class="info-cell"><span class="ic-label">设备类型</span><span class="ic-value">{{ selectedDevice.type }}</span></div>
                <div class="info-cell"><span class="ic-label">所属实验室</span><span class="ic-value">{{ selectedDevice.labName }}</span></div>
                <div class="info-cell"><span class="ic-label">购入日期</span><span class="ic-value">{{ selectedDevice.purchaseDate }}</span></div>
                <div class="info-cell"><span class="ic-label">购置价格</span><span class="ic-value">&yen; {{ selectedDevice.purchasePrice.toLocaleString() }}</span></div>
              </div>
            </div>

            <!-- 操作按钮 -->
            <button
              class="borrow-btn"
              :disabled="selectedDevice.status !== 'idle'"
              @click="goBorrow"
            >
              {{ selectedDevice.status === 'idle' ? '申请借用' : statusLabel(selectedDevice.status) + '，暂不可借用' }}
            </button>
          </div>
        </div>
      </transition>
    </div>

    <!-- 借用申请弹窗 -->
    <teleport to="body">
      <transition name="popup-fade">
        <div v-if="showBorrowPopup && selectedDevice" class="popup-overlay" @click.self="closeBorrowPopup">
          <div class="popup-panel">
            <div class="popup-header">
              <h3>申请借用</h3>
              <button class="popup-close" @click="closeBorrowPopup">
                <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2"><path d="M18 6L6 18M6 6l12 12"/></svg>
              </button>
            </div>
            <div class="popup-body">
              <!-- 设备信息（只读） -->
              <div class="popup-device-preview">
                <div class="popup-device-icon" :class="'icon-status-' + selectedDevice.status">
                  <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="1.6"><rect x="3" y="3" width="18" height="18" rx="3"/><circle cx="12" cy="12" r="4"/></svg>
                </div>
                <div>
                  <span class="popup-device-name">{{ selectedDevice.name }}</span>
                  <span class="popup-device-no">{{ selectedDevice.deviceNo }}</span>
                </div>
                <span class="status-tag tag-idle">闲置</span>
              </div>

              <!-- 表单 -->
              <div class="form-group">
                <label>预计开始时间</label>
                <input v-model="borrowForm.startTime" type="datetime-local" class="form-input" />
              </div>
              <div class="form-group">
                <label>预计归还时间</label>
                <input v-model="borrowForm.endTime" type="datetime-local" class="form-input" />
              </div>
              <div class="form-group">
                <label>借用用途说明</label>
                <textarea
                  v-model="borrowForm.purpose"
                  class="form-textarea"
                  placeholder="请简要说明借用目的和实验内容..."
                  rows="3"
                ></textarea>
              </div>

              <!-- 借用须知 -->
              <div class="borrow-notice">
                <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="12" y1="16" x2="12" y2="12"/><line x1="12" y1="8" x2="12.01" y2="8"/></svg>
                <div>
                  <p class="notice-title">借用须知</p>
                  <ul class="notice-list">
                    <li>提交申请后需等待管理员审核，审核结果将通过消息通知</li>
                    <li>审核通过后请按时到实验室领用设备并登记</li>
                    <li>逾期未还将影响借用信用，请合理规划使用时间</li>
                  </ul>
                </div>
              </div>
            </div>
            <div class="popup-footer">
              <button class="popup-btn cancel" @click="closeBorrowPopup">取消</button>
              <button class="popup-btn primary" @click="submitBorrow" :disabled="!borrowFormValid">提交申请</button>
            </div>
          </div>
        </div>
      </transition>
    </teleport>

    <!-- 空状态 -->
    <div v-if="devices.length === 0" class="empty-state">
      <svg viewBox="0 0 24 24" width="48" height="48" fill="none" stroke="#ccc" stroke-width="1.2"><rect x="3" y="3" width="18" height="18" rx="3"/><circle cx="12" cy="12" r="4"/></svg>
      <p>暂无设备数据</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import api from '../../utils/api'
import { getDevicePage } from '../../utils/deviceApi'

interface Device {
  id: number
  deviceNo: string
  name: string
  type: string
  labName: string
  status: 'idle' | 'borrowed' | 'repair' | 'scrapped'
  healthScore: number
  purchaseDate: string
  purchasePrice: number
  spec: Record<string, string>
}

const searchText = ref('')
const statusFilter = ref('')
const labFilter = ref('')
const selectedDeviceId = ref<number | null>(null)

const devices = ref<Device[]>([])

async function loadDevices() {
  try {
    const res = await getDevicePage({ pageNum: 1, pageSize: 200 })
    devices.value = res.records.map(d => ({
      id: d.id, deviceNo: d.deviceNo, name: d.name,
      type: d.deviceStatusName || '未知', labName: d.labName || '未分配',
      status: d.deviceStatus === 2 ? 'idle' : d.deviceStatus === 3 ? 'borrowed' : d.deviceStatus === 4 ? 'repair' : d.deviceStatus === 5 ? 'scrapped' : 'idle',
      healthScore: d.healthScore, purchaseDate: d.purchaseDate || '',
      purchasePrice: d.purchasePrice || 0, spec: {}
    }))
  } catch { /* API 不可用 */ }
}
onMounted(() => { loadDevices() })

const labOptions = computed(() => {
  const seen = new Set<string>()
  devices.value.forEach(d => seen.add(d.labName))
  return [...seen].sort()
})

const filteredDevices = computed(() => {
  let list = devices.value
  if (searchText.value.trim()) {
    const q = searchText.value.trim().toLowerCase()
    list = devices.value.filter(d =>
      d.name.toLowerCase().includes(q) ||
      d.deviceNo.toLowerCase().includes(q) ||
      d.type.toLowerCase().includes(q)
    )
  }
  if (statusFilter.value) {
    list = list.filter(d => d.status === statusFilter.value)
  }
  if (labFilter.value) {
    list = list.filter(d => d.labName === labFilter.value)
  }
  return list
})

const selectedDevice = computed(() => {
  if (selectedDeviceId.value === null) return null
  return devices.value.find(d => d.id === selectedDeviceId.value) || null
})

function selectDevice(id: number): void {
  selectedDeviceId.value = selectedDeviceId.value === id ? null : id
}

function statusLabel(s: string): string {
  const m: Record<string, string> = { idle: '闲置', borrowed: '借用中', repair: '维修中', scrapped: '已报废' }
  return m[s] || s
}

function healthClass(score: number): string {
  if (score >= 80) return 'health-high'
  if (score >= 50) return 'health-mid'
  return 'health-low'
}

const showBorrowPopup = ref(false)

// 借用表单
const borrowForm = ref({
  startTime: '',
  endTime: '',
  purpose: '',
})

const borrowFormValid = computed(() => {
  return borrowForm.value.startTime !== '' && borrowForm.value.endTime !== '' && borrowForm.value.purpose.trim() !== ''
})

function openBorrowPopup(): void {
  // 默认开始时间 = 明天，结束时间 = 后天
  const tomorrow = new Date()
  tomorrow.setDate(tomorrow.getDate() + 1)
  const dayAfter = new Date(tomorrow)
  dayAfter.setDate(dayAfter.getDate() + 1)
  borrowForm.value.startTime = toDatetimeLocal(tomorrow)
  borrowForm.value.endTime = toDatetimeLocal(dayAfter)
  borrowForm.value.purpose = ''
  showBorrowPopup.value = true
}

function closeBorrowPopup(): void {
  showBorrowPopup.value = false
}

async function submitBorrow(): Promise<void> {
  if (!borrowFormValid.value) return
  if (!selectedDevice.value) return
  try {
    await api.post(`/equipment/devices/${selectedDevice.value.id}/borrow`, {
      expectedStartTime: normalizeDateTime(borrowForm.value.startTime),
      expectedReturnTime: normalizeDateTime(borrowForm.value.endTime),
      purpose: borrowForm.value.purpose.trim(),
    })
    showBorrowPopup.value = false
    alert('借用申请已提交，请等待管理员审核，审核结果将通过消息中心通知。')
  } catch (e: any) {
    alert(e?.response?.data?.message || e?.message || '提交失败')
  }
}

function toDatetimeLocal(date: Date): string {
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}`
}

function normalizeDateTime(value: string): string {
  return value.length === 16 ? `${value}:00` : value
}

function goBorrow(): void {
  openBorrowPopup()
}
</script>

<style scoped>
.device-page { height: 100%; overflow-y: auto; padding: 28px 36px; }

.page-header { margin-bottom: 20px; }
.page-header h2 { font-size: 22px; font-weight: 700; color: #1a1a2e; margin: 0 0 4px; }
.page-header p { font-size: 14px; color: #888; margin: 0; }

/* ===== 筛选栏 ===== */
.filter-bar { display: flex; align-items: center; justify-content: space-between; gap: 14px; margin-bottom: 18px; }

.search-box { position: relative; display: flex; align-items: center; flex: 1; max-width: 360px; background: rgba(255,255,255,.5); border: 1px solid rgba(0,0,0,.06); border-radius: 10px; padding: 0 12px; gap: 8px; color: #bbb; }
.search-box:focus-within { border-color: #1a73e8; color: #1a73e8; }
.search-input { flex: 1; border: none; outline: none; background: none; padding: 10px 0; font-size: 14px; color: #333; }
.search-input::placeholder { color: #ccc; }
.search-clear { cursor: pointer; color: #ccc; padding: 2px; border-radius: 4px; display: flex; }
.search-clear:hover { color: #999; background: rgba(0,0,0,.04); }

.filter-actions { display: flex; align-items: center; gap: 10px; }
.filter-select { padding: 8px 12px; border: 1px solid rgba(0,0,0,.08); border-radius: 8px; font-size: 13px; color: #555; outline: none; background: rgba(255,255,255,.5); cursor: pointer; }
.filter-select:focus { border-color: #1a73e8; }
.result-count { font-size: 12px; color: #aaa; white-space: nowrap; }

/* ===== 两栏布局 ===== */
.device-body { display: flex; gap: 24px; align-items: flex-start; }

/* ===== 左侧设备列表 ===== */
.device-list { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 1px; background: rgba(255,255,255,.5); border-radius: 14px; border: 1px solid rgba(0,0,0,.04); overflow: hidden; }

.device-row { display: flex; align-items: center; gap: 14px; padding: 14px 18px; cursor: pointer; transition: background .12s; background: rgba(255,255,255,.5); }
.device-row:hover { background: rgba(255,255,255,.8); }
.device-row.selected { background: rgba(26,115,232,.05); border-left: 3px solid #1a73e8; padding-left: 15px; }

.device-icon-box { width: 42px; height: 42px; border-radius: 10px; display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
.icon-status-idle { background: rgba(39,174,96,.1); color: #27ae60; }
.icon-status-borrowed { background: rgba(26,115,232,.1); color: #1a73e8; }
.icon-status-repair { background: rgba(230,126,34,.1); color: #e67e22; }
.icon-status-scrapped { background: rgba(231,76,60,.1); color: #e74c3c; }

.device-row-info { flex: 1; min-width: 0; }
.device-row-top { display: flex; align-items: center; gap: 8px; margin-bottom: 2px; }
.device-row-name { font-size: 14px; font-weight: 600; color: #1a1a2e; }
.device-row-no { font-size: 11px; color: #bbb; font-family: 'SF Mono', 'Consolas', monospace; }
.device-row-lab { font-size: 12px; color: #aaa; }

.device-row-right { display: flex; align-items: center; gap: 6px; flex-shrink: 0; }

/* 状态标签 */
.status-tag { font-size: 11px; font-weight: 600; padding: 3px 10px; border-radius: 4px; white-space: nowrap; }
.tag-idle { background: rgba(39,174,96,.1); color: #27ae60; }
.tag-borrowed { background: rgba(26,115,232,.1); color: #1a73e8; }
.tag-repair { background: rgba(230,126,34,.1); color: #e67e22; }
.tag-scrapped { background: rgba(231,76,60,.1); color: #e74c3c; }
.status-tag.large { font-size: 13px; padding: 4px 14px; border-radius: 6px; }

/* ===== 右侧详情面板 ===== */
.detail-panel { width: 360px; flex-shrink: 0; position: sticky; top: 0; }

.panel-card { background: rgba(255,255,255,.55); border-radius: 14px; border: 1px solid rgba(0,0,0,.04); padding: 24px 22px; }

.panel-device-header { display: flex; align-items: center; gap: 14px; margin-bottom: 16px; }
.panel-device-icon { width: 46px; height: 46px; border-radius: 12px; display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
.panel-device-name { font-size: 17px; font-weight: 700; color: #1a1a2e; margin: 0 0 2px; }
.panel-device-no { font-size: 12px; color: #bbb; font-family: 'SF Mono', 'Consolas', monospace; margin: 0; }

/* 状态 + 健康 */
.panel-status-row { display: flex; align-items: center; gap: 16px; margin-bottom: 20px; padding-bottom: 16px; border-bottom: 1px solid rgba(0,0,0,.05); }
.health-info { display: flex; align-items: center; gap: 8px; flex: 1; }
.health-label { font-size: 12px; color: #aaa; }
.health-value { font-size: 16px; font-weight: 700; }
.health-value.health-high { color: #27ae60; }
.health-value.health-mid { color: #e67e22; }
.health-value.health-low { color: #e74c3c; }
.health-bar { flex: 1; height: 5px; background: rgba(0,0,0,.06); border-radius: 3px; overflow: hidden; }
.health-fill { height: 100%; border-radius: 3px; transition: width .3s; }
.health-fill.health-high { background: #27ae60; }
.health-fill.health-mid { background: #e67e22; }
.health-fill.health-low { background: #e74c3c; }

/* 技术参数 */
.panel-section { margin-bottom: 18px; }
.panel-section-title { font-size: 12px; font-weight: 600; color: #aaa; text-transform: uppercase; letter-spacing: .5px; margin: 0 0 10px; padding-bottom: 6px; border-bottom: 1px solid rgba(0,0,0,.04); }

.spec-list { display: flex; flex-direction: column; gap: 6px; }
.spec-row { display: flex; align-items: center; justify-content: space-between; padding: 5px 0; }
.spec-key { font-size: 13px; color: #888; }
.spec-value { font-size: 13px; color: #333; font-weight: 500; }

/* 基本信息 */
.info-grid-2col { display: grid; grid-template-columns: repeat(2, 1fr); gap: 8px 16px; }
.info-cell { display: flex; flex-direction: column; gap: 1px; }
.ic-label { font-size: 11px; color: #aaa; }
.ic-value { font-size: 13px; color: #333; font-weight: 500; }

/* 借用按钮 */
.borrow-btn { width: 100%; margin-top: 18px; padding: 12px 0; border: none; border-radius: 10px; background: linear-gradient(135deg, #1a73e8, #4a90d9); color: #fff; font-size: 15px; font-weight: 600; cursor: pointer; transition: all .15s; }
.borrow-btn:hover:not(:disabled) { box-shadow: 0 4px 14px rgba(26,115,232,.35); }
.borrow-btn:disabled { background: rgba(0,0,0,.06); color: #bbb; cursor: not-allowed; }

/* ===== 借用弹窗 ===== */
.popup-overlay { position: fixed; inset: 0; z-index: 1000; background: rgba(0,0,0,.3); backdrop-filter: blur(4px); -webkit-backdrop-filter: blur(4px); display: flex; align-items: center; justify-content: center; }

.popup-panel { width: 500px; max-width: 92vw; background: #fff; border-radius: 18px; box-shadow: 0 20px 60px rgba(0,0,0,.15); overflow: hidden; display: flex; flex-direction: column; max-height: 90vh; }

.popup-header { display: flex; align-items: center; justify-content: space-between; padding: 24px 28px 0; }
.popup-header h3 { font-size: 18px; font-weight: 700; color: #1a1a2e; margin: 0; }
.popup-close { width: 32px; height: 32px; display: flex; align-items: center; justify-content: center; border: none; background: none; border-radius: 8px; cursor: pointer; color: #999; flex-shrink: 0; }
.popup-close:hover { background: #f5f5f5; color: #333; }

.popup-body { padding: 20px 28px; overflow-y: auto; }

/* 设备预览条 */
.popup-device-preview { display: flex; align-items: center; gap: 12px; padding: 14px 16px; background: rgba(0,0,0,.02); border-radius: 10px; margin-bottom: 20px; }
.popup-device-icon { width: 38px; height: 38px; border-radius: 8px; display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
.popup-device-name { display: block; font-size: 14px; font-weight: 600; color: #1a1a2e; }
.popup-device-no { display: block; font-size: 11px; color: #bbb; font-family: 'SF Mono', 'Consolas', monospace; margin-top: 1px; }

/* 表单 */
.form-group { display: flex; flex-direction: column; gap: 6px; margin-bottom: 16px; }
.form-group label { font-size: 13px; font-weight: 600; color: #555; }
.form-input { padding: 10px 14px; border: 1px solid rgba(0,0,0,.1); border-radius: 8px; font-size: 14px; color: #333; outline: none; transition: border .15s; background: #fff; }
.form-input:focus { border-color: #1a73e8; }

.form-textarea { padding: 10px 14px; border: 1px solid rgba(0,0,0,.1); border-radius: 8px; font-size: 14px; color: #333; outline: none; resize: vertical; font-family: inherit; transition: border .15s; }
.form-textarea:focus { border-color: #1a73e8; }
.form-textarea::placeholder { color: #ccc; }

/* 借用须知 */
.borrow-notice { display: flex; gap: 10px; padding: 14px 16px; background: rgba(26,115,232,.04); border-radius: 10px; border-left: 3px solid #1a73e8; color: #1a73e8; }
.notice-title { font-size: 13px; font-weight: 600; margin: 0 0 6px; color: #1a73e8; }
.notice-list { margin: 0; padding-left: 16px; font-size: 12px; color: #666; line-height: 1.7; }
.notice-list li { margin-bottom: 2px; }

/* 弹窗按钮 */
.popup-footer { padding: 16px 28px 22px; display: flex; justify-content: flex-end; gap: 10px; }
.popup-btn { padding: 10px 28px; border-radius: 8px; font-size: 14px; cursor: pointer; border: none; transition: all .15s; }
.popup-btn.cancel { background: rgba(0,0,0,.04); color: #666; }
.popup-btn.cancel:hover { background: rgba(0,0,0,.08); }
.popup-btn.primary { background: linear-gradient(135deg,#1a73e8,#4a90d9); color: #fff; font-weight: 600; }
.popup-btn.primary:hover:not(:disabled) { box-shadow: 0 4px 12px rgba(26,115,232,.3); }
.popup-btn.primary:disabled { background: rgba(0,0,0,.08); color: #bbb; cursor: not-allowed; }

/* 空状态 */
.empty-state { display: flex; flex-direction: column; align-items: center; justify-content: center; padding: 80px 0; color: #ccc; }
.empty-state p { font-size: 14px; margin-top: 12px; }
</style>

<style>
.panel-fade-enter-active, .panel-fade-leave-active { transition: all .25s ease; }
.panel-fade-enter-from, .panel-fade-leave-to { opacity: 0; transform: translateX(12px); }

.popup-fade-enter-active, .popup-fade-leave-active { transition: all .2s ease; }
.popup-fade-enter-from, .popup-fade-leave-to { opacity: 0; }
.popup-fade-enter-from .popup-panel, .popup-fade-leave-to .popup-panel { transform: scale(.95); }
</style>
