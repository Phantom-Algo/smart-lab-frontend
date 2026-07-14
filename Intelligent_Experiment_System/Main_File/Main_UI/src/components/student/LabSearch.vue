<template>
  <div class="lab-page">
    <div class="page-header">
      <h2>实验室查询</h2>
      <p>查看实验室信息与开放时间</p>
    </div>

    <!-- 搜索与筛选栏 -->
    <div class="filter-bar">
      <div class="search-box">
        <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2"><circle cx="11" cy="11" r="8"/><path d="M21 21l-4.35-4.35"/></svg>
        <input v-model="searchText" type="text" class="search-input" placeholder="搜索实验室名称或编号..." />
        <span v-if="searchText" class="search-clear" @click="searchText = ''">
          <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2"><path d="M18 6L6 18M6 6l12 12"/></svg>
        </span>
      </div>
      <div class="filter-actions">
        <select v-model="typeFilter" class="filter-select">
          <option value="">全部类型</option>
          <option v-for="t in typeOptions" :key="t" :value="t">{{ t }}</option>
        </select>
        <select v-model="buildingFilter" class="filter-select">
          <option value="">全部楼栋</option>
          <option v-for="b in buildingOptions" :key="b" :value="b">{{ b }}</option>
        </select>
        <span class="result-count">共 {{ filteredLabs.length }} 间实验室</span>
      </div>
    </div>

    <!-- 主体：左列表 + 右详情 -->
    <div class="lab-body" v-if="labs.length > 0">
      <div class="lab-list">
        <div
          v-for="lab in filteredLabs"
          :key="lab.id"
          class="lab-row"
          :class="{ selected: selectedLabId === lab.id }"
          @click="selectLab(lab.id)"
        >
          <div class="lab-icon-box" :class="'status-' + lab.runningStatus">
            <svg v-if="lab.type === '物理实验室'" viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="1.6"><circle cx="12" cy="12" r="9"/><path d="M12 3v6l4 3"/></svg>
            <svg v-else-if="lab.type === '化学实验室'" viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="1.6"><path d="M9 3h6M10 3v4a2 2 0 01-2 2H7M14 3v4a2 2 0 002 2h1"/><rect x="4" y="18" width="16" height="3" rx="1"/><rect x="7" y="9" width="10" height="9" rx="1"/></svg>
            <svg v-else-if="lab.type === '计算机实验室'" viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="1.6"><rect x="2" y="3" width="20" height="14" rx="2"/><line x1="8" y1="21" x2="16" y2="21"/><line x1="12" y1="17" x2="12" y2="21"/></svg>
            <svg v-else viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="1.6"><path d="M4 19.5A2.5 2.5 0 016.5 17H20"/><path d="M6.5 2H20v20H6.5A2.5 2.5 0 014 19.5v-15A2.5 2.5 0 016.5 2z"/></svg>
          </div>
          <div class="lab-row-info">
            <div class="lab-row-top">
              <span class="lab-row-name">{{ lab.name }}</span>
              <span class="lab-row-code">{{ lab.code }}</span>
            </div>
            <div class="lab-row-meta">
              <span class="lab-row-loc">{{ lab.building }} {{ lab.floor }}层 {{ lab.roomNo }}</span>
            </div>
          </div>
          <div class="lab-row-right">
            <span class="status-tag" :class="'tag-' + lab.runningStatus">{{ statusLabel(lab.runningStatus) }}</span>
            <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="#ccc" stroke-width="2"><polyline points="9 18 15 12 9 6"/></svg>
          </div>
        </div>
      </div>

      <!-- 右侧详情面板 -->
      <transition name="panel-fade">
        <div v-if="selectedLab" class="detail-panel">
          <div class="panel-card">
            <div class="panel-lab-header">
              <div class="panel-lab-icon" :class="'status-' + selectedLab.runningStatus">
                <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" stroke-width="1.6"><path d="M4 19.5A2.5 2.5 0 016.5 17H20"/><path d="M6.5 2H20v20H6.5A2.5 2.5 0 014 19.5v-15A2.5 2.5 0 016.5 2z"/></svg>
              </div>
              <div>
                <h3 class="panel-lab-name">{{ selectedLab.name }}</h3>
                <p class="panel-lab-code">{{ selectedLab.code }}</p>
              </div>
            </div>

            <!-- 状态 -->
            <div class="panel-status-row">
              <span class="status-tag large" :class="'tag-' + selectedLab.runningStatus">{{ statusLabel(selectedLab.runningStatus) }}</span>
              <span v-if="selectedLab.runningStatus === 'idle'" class="status-hint green">当前可预约</span>
              <span v-else-if="selectedLab.runningStatus === 'occupied'" class="status-hint blue">正在使用中</span>
              <span v-else class="status-hint orange">暂不可用</span>
            </div>

            <!-- 位置信息 -->
            <div class="panel-section">
              <h4 class="panel-section-title">位置信息</h4>
              <div class="location-chain">
                <span class="lc-item">{{ selectedLab.campus }}</span>
                <svg viewBox="0 0 24 24" width="12" height="12" fill="none" stroke="#ccc" stroke-width="2"><polyline points="9 18 15 12 9 6"/></svg>
                <span class="lc-item">{{ selectedLab.building }}</span>
                <svg viewBox="0 0 24 24" width="12" height="12" fill="none" stroke="#ccc" stroke-width="2"><polyline points="9 18 15 12 9 6"/></svg>
                <span class="lc-item">{{ selectedLab.floor }}层</span>
                <svg viewBox="0 0 24 24" width="12" height="12" fill="none" stroke="#ccc" stroke-width="2"><polyline points="9 18 15 12 9 6"/></svg>
                <span class="lc-item">{{ selectedLab.roomNo }}室</span>
              </div>
            </div>

            <!-- 基本信息 -->
            <div class="panel-section">
              <h4 class="panel-section-title">基本信息</h4>
              <div class="info-grid-2col">
                <div class="info-cell"><span class="ic-label">实验室类型</span><span class="ic-value">{{ selectedLab.type }}</span></div>
                <div class="info-cell"><span class="ic-label">容纳人数</span><span class="ic-value">{{ selectedLab.capacity }} 人</span></div>
                <div class="info-cell"><span class="ic-label">面积</span><span class="ic-value">{{ selectedLab.areaM2 }} m²</span></div>
                <div class="info-cell"><span class="ic-label">负责人</span><span class="ic-value">{{ selectedLab.dutyPerson }}</span></div>
              </div>
            </div>

            <!-- 描述 -->
            <div class="panel-section" v-if="selectedLab.description">
              <h4 class="panel-section-title">实验室简介</h4>
              <p class="panel-desc">{{ selectedLab.description }}</p>
            </div>

            <!-- 开放时间 -->
            <div class="panel-section">
              <h4 class="panel-section-title">开放时间</h4>
              <div class="open-hours">
                <div v-for="(h, i) in selectedLab.openHours" :key="i" class="open-hour-row">
                  <span class="oh-days">{{ h.days }}</span>
                  <span class="oh-time">{{ h.time }}</span>
                </div>
              </div>
            </div>

            <!-- 已有设备 -->
            <div class="panel-section">
              <h4 class="panel-section-title">已有设备（{{ selectedLab.equipment.length }} 台）</h4>
              <div class="equip-list" v-if="selectedLab.equipment.length > 0">
                <div v-for="eq in selectedLab.equipment" :key="eq" class="equip-item">
                  <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="3" width="18" height="18" rx="3"/><circle cx="12" cy="12" r="4"/></svg>
                  <span>{{ eq }}</span>
                </div>
              </div>
              <span v-else class="empty-hint">暂无设备信息</span>
            </div>

            <!-- 预约按钮 -->
            <button
              class="reserve-btn"
              :disabled="selectedLab.runningStatus !== 'idle'"
              @click="goReserve"
            >
              {{ selectedLab.runningStatus === 'idle' ? '预约场地' : statusLabel(selectedLab.runningStatus) + '，暂不可预约' }}
            </button>
          </div>
        </div>
      </transition>
    </div>

    <div v-if="labs.length === 0" class="empty-state">
      <svg viewBox="0 0 24 24" width="48" height="48" fill="none" stroke="#ccc" stroke-width="1.2"><path d="M4 19.5A2.5 2.5 0 016.5 17H20"/><path d="M6.5 2H20v20H6.5A2.5 2.5 0 014 19.5v-15A2.5 2.5 0 016.5 2z"/></svg>
      <p>暂无实验室数据</p>
    </div>

    <!-- 预约弹窗 -->
    <teleport to="body">
      <transition name="popup-fade">
        <div v-if="showReservePopup && selectedLab" class="popup-overlay" @click.self="closeReservePopup">
          <div class="popup-panel">
            <div class="popup-header">
              <h3>预约场地</h3>
              <button class="popup-close" @click="closeReservePopup">
                <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2"><path d="M18 6L6 18M6 6l12 12"/></svg>
              </button>
            </div>
            <div class="popup-body">
              <!-- 实验室信息预览 -->
              <div class="popup-lab-preview">
                <div class="popup-lab-icon" :class="'status-' + selectedLab.runningStatus">
                  <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="1.6"><path d="M4 19.5A2.5 2.5 0 016.5 17H20"/><path d="M6.5 2H20v20H6.5A2.5 2.5 0 014 19.5v-15A2.5 2.5 0 016.5 2z"/></svg>
                </div>
                <div>
                  <span class="popup-lab-name">{{ selectedLab.name }}</span>
                  <span class="popup-lab-code">{{ selectedLab.code }}</span>
                </div>
                <span class="status-tag tag-idle">空闲</span>
              </div>

              <!-- 表单 -->
              <div class="form-group">
                <label>预约日期</label>
                <input v-model="reserveForm.date" type="date" class="form-input" :min="todayStr" />
              </div>
              <div class="form-row-2col">
                <div class="form-group">
                  <label>开始时间</label>
                  <input v-model="reserveForm.startTime" type="time" class="form-input" />
                </div>
                <div class="form-group">
                  <label>结束时间</label>
                  <input v-model="reserveForm.endTime" type="time" class="form-input" />
                </div>
              </div>
              <div class="form-group">
                <label>使用人数</label>
                <input v-model.number="reserveForm.peopleCount" type="number" class="form-input" placeholder="请输入预计使用人数" min="1" :max="selectedLab.capacity" />
                <span class="form-hint">该实验室最大容纳 {{ selectedLab.capacity }} 人</span>
              </div>
              <div class="form-group">
                <label>预约用途</label>
                <textarea v-model="reserveForm.purpose" class="form-textarea" placeholder="请简要说明实验内容或预约目的..." rows="3"></textarea>
              </div>

              <!-- 预约须知 -->
              <div class="reserve-notice">
                <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="12" y1="16" x2="12" y2="12"/><line x1="12" y1="8" x2="12.01" y2="8"/></svg>
                <div>
                  <p class="notice-title">预约须知</p>
                  <ul class="notice-list">
                    <li>提交后需等待管理员审核，结果将通过消息中心通知</li>
                    <li>请在预约时间段内使用实验室，超时可能影响后续预约</li>
                    <li>使用期间请注意实验室安全规范，保持环境整洁</li>
                  </ul>
                </div>
              </div>
            </div>
            <div class="popup-footer">
              <button class="popup-btn cancel" @click="closeReservePopup">取消</button>
              <button class="popup-btn primary" @click="submitReserve" :disabled="!reserveFormValid">提交预约</button>
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

interface OpenHour { days: string; time: string }

interface Lab {
  id: number
  code: string
  name: string
  type: string
  campus: string
  building: string
  floor: number
  roomNo: string
  capacity: number
  areaM2: number
  dutyPerson: string
  runningStatus: 'idle' | 'occupied' | 'maintenance'
  description: string
  openHours: OpenHour[]
  equipment: string[]
}

const searchText = ref('')
const typeFilter = ref('')
const buildingFilter = ref('')
const selectedLabId = ref<number | null>(null)

const MOCK_LABS: Lab[] = [
  {
    id: 1, code: 'LAB-2024-B103', name: 'B103 物理实验室', type: '物理实验室',
    campus: '主校区', building: '理工楼', floor: 1, roomNo: '103',
    capacity: 40, areaM2: 120, dutyPerson: '王老师',
    runningStatus: 'idle',
    description: '配备全套大学物理实验器材，涵盖力学、电磁学、光学、热学等基础物理实验，满足本科物理实验教学需求。',
    openHours: [{ days: '周一 ~ 周五', time: '08:00 ~ 21:00' }, { days: '周六', time: '09:00 ~ 17:00' }],
    equipment: ['数字示波器', '信号发生器', '频谱分析仪', '直流稳压电源'],
  },
  {
    id: 2, code: 'LAB-2024-A201', name: 'A201 化学实验室', type: '化学实验室',
    campus: '主校区', building: '化学楼', floor: 2, roomNo: '201',
    capacity: 30, areaM2: 150, dutyPerson: '李老师',
    runningStatus: 'occupied',
    description: '标准有机/无机化学实验室，配置通风橱、实验台、分析仪器，满足有机合成、分析化学等实验需求。',
    openHours: [{ days: '周一 ~ 周五', time: '08:00 ~ 20:00' }],
    equipment: ['高速离心机', '紫外分光光度计', '电子天平', '恒温水浴锅'],
  },
  {
    id: 3, code: 'LAB-2024-C202', name: 'C202 计算机实验室', type: '计算机实验室',
    campus: '主校区', building: '信息楼', floor: 2, roomNo: '202',
    capacity: 60, areaM2: 100, dutyPerson: '张老师',
    runningStatus: 'idle',
    description: '高性能计算机实验室，配备 60 台工作站，预装 Python、MATLAB、VS Code 等开发环境，满足编程实验和数据分析需求。',
    openHours: [{ days: '周一 ~ 周日', time: '08:00 ~ 22:00' }],
    equipment: ['高性能工作站 ×60', '投影仪', '交换机'],
  },
  {
    id: 4, code: 'LAB-2024-D305', name: 'D305 生物实验室', type: '生物实验室',
    campus: '主校区', building: '生科楼', floor: 3, roomNo: '305',
    capacity: 25, areaM2: 130, dutyPerson: '赵老师',
    runningStatus: 'maintenance',
    description: '分子生物学与微生物学实验室，配备超净工作台、PCR仪、倒置显微镜、电泳仪等设备，通风及安全设施达标。',
    openHours: [{ days: '周一 ~ 周五', time: '08:00 ~ 20:00' }, { days: '周六', time: '09:00 ~ 17:00' }],
    equipment: ['倒置显微镜', 'PCR 扩增仪', '超净工作台', '电泳仪'],
  },
  {
    id: 5, code: 'LAB-2024-E108', name: 'E108 电子实验室', type: '物理实验室',
    campus: '主校区', building: '理工楼', floor: 1, roomNo: '108',
    capacity: 35, areaM2: 110, dutyPerson: '王老师',
    runningStatus: 'idle',
    description: '电子技术综合实验室，支持模拟电路、数字电路、嵌入式系统等实验项目。',
    openHours: [{ days: '周一 ~ 周五', time: '08:00 ~ 21:00' }],
    equipment: ['数字示波器 ×10', '信号发生器 ×10', '直流稳压电源 ×15', '万用表 ×20'],
  },
  {
    id: 6, code: 'LAB-2023-A305', name: 'A305 分析化学实验室', type: '化学实验室',
    campus: '主校区', building: '化学楼', floor: 3, roomNo: '305',
    capacity: 20, areaM2: 90, dutyPerson: '李老师',
    runningStatus: 'idle',
    description: '精密分析化学实验室，配置高效液相色谱、气相色谱-质谱联用仪等大型分析仪器。',
    openHours: [{ days: '周一 ~ 周五', time: '09:00 ~ 18:00' }],
    equipment: ['高效液相色谱仪', '气相色谱-质谱联用仪', '紫外分光光度计'],
  },
  {
    id: 7, code: 'LAB-2024-F201', name: 'F201 材料实验室', type: '物理实验室',
    campus: '南校区', building: '材料楼', floor: 2, roomNo: '201',
    capacity: 25, areaM2: 140, dutyPerson: '陈老师',
    runningStatus: 'occupied',
    description: '材料科学与工程实验室，配备万能试验机、硬度计、金相显微镜等材料性能测试设备。',
    openHours: [{ days: '周一 ~ 周五', time: '08:00 ~ 20:00' }],
    equipment: ['万能试验机', '维氏硬度计', '金相显微镜', '热处理炉'],
  },
  {
    id: 8, code: 'LAB-2024-G102', name: 'G102 环境监测实验室', type: '生物实验室',
    campus: '南校区', building: '环境楼', floor: 1, roomNo: '102',
    capacity: 20, areaM2: 100, dutyPerson: '刘老师',
    runningStatus: 'idle',
    description: '环境科学与监测实验室，配备水质分析仪、大气采样器等环境监测专业设备。',
    openHours: [{ days: '周一 ~ 周五', time: '08:00 ~ 18:00' }],
    equipment: ['水质分析仪', '大气采样器', '噪声监测仪', '土壤检测仪'],
  },
]

const labs = ref<Lab[]>([])

function mapRunningStatus(status: unknown): Lab['runningStatus'] {
  if (status === 'idle' || status === 1) return 'idle'
  if (status === 'occupied' || status === 2) return 'occupied'
  return 'maintenance'
}

function buildOpenHours(raw: unknown): OpenHour[] {
  if (Array.isArray(raw) && raw.length > 0) return raw as OpenHour[]
  return [{ days: '周一 ~ 周日', time: '08:00 ~ 22:00' }]
}

async function loadData() {
  if (isMockMode.value) { labs.value = [...MOCK_LABS]; return }
  try {
    const res = await api.get('/lab-management/labs', { params: { page: 1, pageSize: 100 } })
    if (res.data?.data?.records?.length) {
      labs.value = res.data.data.records.map((l: any) => ({
        id: l.id, code: l.labCode || l.labNo || `LAB-${l.id || ''}`, name: l.labName || '', type: l.labTypeName || '',
        campus: l.campus || '主校区', building: l.building || '', floor: l.floor || 1, roomNo: l.roomNo || '',
        capacity: l.capacity || 30, areaM2: l.areaM2 || 100, dutyPerson: l.dutyUserName || l.dutyPersonName || '',
        runningStatus: mapRunningStatus(l.runningStatus),
        description: l.description || '', openHours: buildOpenHours(l.openHours), equipment: l.equipmentList || []
      }))
    } else labs.value = [...MOCK_LABS]
  } catch { /* API 不可用，请检查后端服务 */ }
}

onMounted(() => loadData())
const typeOptions = computed(() => [...new Set(labs.value.map(l => l.type))].sort())
const buildingOptions = computed(() => [...new Set(labs.value.map(l => l.building))].sort())

const filteredLabs = computed(() => {
  let list = labs.value
  if (searchText.value.trim()) {
    const q = searchText.value.trim().toLowerCase()
    list = list.filter(l => l.name.toLowerCase().includes(q) || l.code.toLowerCase().includes(q) || l.type.toLowerCase().includes(q))
  }
  if (typeFilter.value) list = list.filter(l => l.type === typeFilter.value)
  if (buildingFilter.value) list = list.filter(l => l.building === buildingFilter.value)
  return list
})

const selectedLab = computed(() => {
  if (selectedLabId.value === null) return null
  return labs.value.find(l => l.id === selectedLabId.value) || null
})

function selectLab(id: number): void {
  selectedLabId.value = selectedLabId.value === id ? null : id
}

function statusLabel(s: string): string {
  const m: Record<string, string> = { idle: '空闲', occupied: '占用中', maintenance: '维护中' }
  return m[s] || s
}

function goReserve(): void {
  openReservePopup()
}

// ===== 预约弹窗 =====
const showReservePopup = ref(false)

const todayStr = computed(() => new Date().toISOString().split('T')[0])

const reserveForm = ref({
  date: '',
  startTime: '',
  endTime: '',
  peopleCount: 1,
  purpose: '',
})

const reserveFormValid = computed(() => {
  return reserveForm.value.date !== '' &&
    reserveForm.value.startTime !== '' &&
    reserveForm.value.endTime !== '' &&
    reserveForm.value.peopleCount > 0 &&
    reserveForm.value.purpose.trim() !== ''
})

function openReservePopup(): void {
  const tomorrow = new Date()
  tomorrow.setDate(tomorrow.getDate() + 1)
  reserveForm.value.date = tomorrow.toISOString().split('T')[0]
  reserveForm.value.startTime = '09:00'
  reserveForm.value.endTime = '12:00'
  reserveForm.value.peopleCount = 1
  reserveForm.value.purpose = ''
  showReservePopup.value = true
}

function closeReservePopup(): void {
  showReservePopup.value = false
}

async function submitReserve(): Promise<void> {
  if (!reserveFormValid.value) return
  const lab = selectedLab.value
  if (!lab) return

  if (isMockMode.value) {
    showReservePopup.value = false
    alert(`场地预约已提交！\n\n实验室：${lab.name}\n预约日期：${reserveForm.value.date}\n时间段：${reserveForm.value.startTime} ~ ${reserveForm.value.endTime}\n人数：${reserveForm.value.peopleCount} 人\n\n请等待管理员审核，审核结果将通过消息中心通知。`)
    return
  }

  try {
    await api.post('/lab-management/reservations', {
      labId: lab.id,
      reservationDate: reserveForm.value.date,
      startTime: reserveForm.value.startTime,
      endTime: reserveForm.value.endTime,
      purpose: `${reserveForm.value.purpose.trim()}；使用人数：${reserveForm.value.peopleCount}`,
      courseName: 'Web 场地预约'
    })
    showReservePopup.value = false
    window.dispatchEvent(new CustomEvent('refresh-reservation-list'))
    alert('场地预约已提交，请等待管理员审核。')
  } catch (e: any) {
    alert(e.message || '提交预约失败')
  }
}
</script>

<style scoped>
.lab-page { height: 100%; overflow-y: auto; padding: 28px 36px; }

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
.lab-body { display: flex; gap: 24px; align-items: flex-start; }

/* ===== 左侧列表 ===== */
.lab-list { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 1px; background: rgba(255,255,255,.5); border-radius: 14px; border: 1px solid rgba(0,0,0,.04); overflow: hidden; }

.lab-row { display: flex; align-items: center; gap: 14px; padding: 14px 18px; cursor: pointer; transition: background .12s; background: rgba(255,255,255,.5); }
.lab-row:hover { background: rgba(255,255,255,.8); }
.lab-row.selected { background: rgba(26,115,232,.05); border-left: 3px solid #1a73e8; padding-left: 15px; }

.lab-icon-box { width: 42px; height: 42px; border-radius: 10px; display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
.status-idle { background: rgba(39,174,96,.1); color: #27ae60; }
.status-occupied { background: rgba(26,115,232,.1); color: #1a73e8; }
.status-maintenance { background: rgba(230,126,34,.1); color: #e67e22; }

.lab-row-info { flex: 1; min-width: 0; }
.lab-row-top { display: flex; align-items: center; gap: 8px; margin-bottom: 2px; }
.lab-row-name { font-size: 14px; font-weight: 600; color: #1a1a2e; }
.lab-row-code { font-size: 11px; color: #bbb; font-family: 'SF Mono', 'Consolas', monospace; }
.lab-row-loc { font-size: 12px; color: #aaa; }

.lab-row-right { display: flex; align-items: center; gap: 6px; flex-shrink: 0; }

/* 状态标签 */
.status-tag { font-size: 11px; font-weight: 600; padding: 3px 10px; border-radius: 4px; white-space: nowrap; }
.tag-idle { background: rgba(39,174,96,.1); color: #27ae60; }
.tag-occupied { background: rgba(26,115,232,.1); color: #1a73e8; }
.tag-maintenance { background: rgba(230,126,34,.1); color: #e67e22; }
.status-tag.large { font-size: 13px; padding: 4px 14px; border-radius: 6px; }

/* ===== 右侧详情面板 ===== */
.detail-panel { width: 360px; flex-shrink: 0; position: sticky; top: 0; }
.panel-card { background: rgba(255,255,255,.55); border-radius: 14px; border: 1px solid rgba(0,0,0,.04); padding: 24px 22px; }

.panel-lab-header { display: flex; align-items: center; gap: 14px; margin-bottom: 14px; }
.panel-lab-icon { width: 46px; height: 46px; border-radius: 12px; display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
.panel-lab-name { font-size: 17px; font-weight: 700; color: #1a1a2e; margin: 0 0 2px; }
.panel-lab-code { font-size: 12px; color: #bbb; font-family: 'SF Mono', 'Consolas', monospace; margin: 0; }

.panel-status-row { display: flex; align-items: center; gap: 10px; margin-bottom: 18px; padding-bottom: 14px; border-bottom: 1px solid rgba(0,0,0,.05); }
.status-hint { font-size: 12px; }
.status-hint.green { color: #27ae60; }
.status-hint.blue { color: #1a73e8; }
.status-hint.orange { color: #e67e22; }

/* 位置链 */
.location-chain { display: flex; align-items: center; gap: 4px; flex-wrap: wrap; }
.lc-item { font-size: 13px; color: #555; padding: 3px 8px; background: rgba(0,0,0,.02); border-radius: 4px; }

/* 基本信息 */
.panel-section { margin-bottom: 18px; }
.panel-section-title { font-size: 12px; font-weight: 600; color: #aaa; text-transform: uppercase; letter-spacing: .5px; margin: 0 0 10px; padding-bottom: 6px; border-bottom: 1px solid rgba(0,0,0,.04); }
.info-grid-2col { display: grid; grid-template-columns: repeat(2, 1fr); gap: 8px 16px; }
.info-cell { display: flex; flex-direction: column; gap: 1px; }
.ic-label { font-size: 11px; color: #aaa; }
.ic-value { font-size: 13px; color: #333; font-weight: 500; }

.panel-desc { font-size: 13px; color: #666; line-height: 1.6; margin: 0; }

/* 开放时间 */
.open-hours { display: flex; flex-direction: column; gap: 4px; }
.open-hour-row { display: flex; align-items: center; gap: 12px; }
.oh-days { font-size: 13px; color: #555; min-width: 80px; }
.oh-time { font-size: 13px; color: #888; }

/* 设备 */
.equip-list { display: flex; flex-direction: column; gap: 4px; }
.equip-item { display: flex; align-items: center; gap: 8px; font-size: 13px; color: #555; }
.empty-hint { font-size: 12px; color: #ccc; }

/* 预约按钮 */
.reserve-btn { width: 100%; margin-top: 18px; padding: 12px 0; border: none; border-radius: 10px; background: linear-gradient(135deg, #1a73e8, #4a90d9); color: #fff; font-size: 15px; font-weight: 600; cursor: pointer; transition: all .15s; }
.reserve-btn:hover:not(:disabled) { box-shadow: 0 4px 14px rgba(26,115,232,.35); }
.reserve-btn:disabled { background: rgba(0,0,0,.06); color: #bbb; cursor: not-allowed; }

/* ===== 预约弹窗 ===== */
.popup-overlay { position: fixed; inset: 0; z-index: 1000; background: rgba(0,0,0,.3); backdrop-filter: blur(4px); -webkit-backdrop-filter: blur(4px); display: flex; align-items: center; justify-content: center; }
.popup-panel { width: 500px; max-width: 92vw; background: #fff; border-radius: 18px; box-shadow: 0 20px 60px rgba(0,0,0,.15); overflow: hidden; display: flex; flex-direction: column; max-height: 90vh; }
.popup-header { display: flex; align-items: center; justify-content: space-between; padding: 24px 28px 0; }
.popup-header h3 { font-size: 18px; font-weight: 700; color: #1a1a2e; margin: 0; }
.popup-close { width: 32px; height: 32px; display: flex; align-items: center; justify-content: center; border: none; background: none; border-radius: 8px; cursor: pointer; color: #999; flex-shrink: 0; }
.popup-close:hover { background: #f5f5f5; color: #333; }
.popup-body { padding: 20px 28px; overflow-y: auto; }

/* 实验室预览条 */
.popup-lab-preview { display: flex; align-items: center; gap: 12px; padding: 14px 16px; background: rgba(0,0,0,.02); border-radius: 10px; margin-bottom: 20px; }
.popup-lab-icon { width: 38px; height: 38px; border-radius: 8px; display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
.popup-lab-name { display: block; font-size: 14px; font-weight: 600; color: #1a1a2e; }
.popup-lab-code { display: block; font-size: 11px; color: #bbb; font-family: 'SF Mono', 'Consolas', monospace; margin-top: 1px; }

/* 表单 */
.form-group { display: flex; flex-direction: column; gap: 6px; margin-bottom: 16px; }
.form-group label { font-size: 13px; font-weight: 600; color: #555; }
.form-input { padding: 10px 14px; border: 1px solid rgba(0,0,0,.1); border-radius: 8px; font-size: 14px; color: #333; outline: none; transition: border .15s; background: #fff; }
.form-input:focus { border-color: #1a73e8; }
.form-row-2col { display: grid; grid-template-columns: repeat(2, 1fr); gap: 12px; }
.form-row-2col .form-group { margin-bottom: 16px; }
.form-hint { font-size: 11px; color: #aaa; }
.form-textarea { padding: 10px 14px; border: 1px solid rgba(0,0,0,.1); border-radius: 8px; font-size: 14px; color: #333; outline: none; resize: vertical; font-family: inherit; transition: border .15s; }
.form-textarea:focus { border-color: #1a73e8; }
.form-textarea::placeholder { color: #ccc; }

/* 预约须知 */
.reserve-notice { display: flex; gap: 10px; padding: 14px 16px; background: rgba(26,115,232,.04); border-radius: 10px; border-left: 3px solid #1a73e8; color: #1a73e8; }
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
