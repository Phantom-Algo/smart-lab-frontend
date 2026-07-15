<template>
  <div class="sd-page">
    <div class="page-header">
      <div>
        <h2>传感器设备</h2>
        <p>传感器类型、检测数据项、实例管理</p>
      </div>
      <button class="icon-text-btn" type="button" :disabled="refreshing" @click="refreshCurrent">
        <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M21 12a9 9 0 1 1-2.64-6.36" />
          <path d="M21 3v6h-6" />
        </svg>
        刷新
      </button>
    </div>

    <div class="tab-bar">
      <button
        class="tab-btn"
        :class="{ active: activeTab === 'types' }"
        type="button"
        @click="activeTab = 'types'"
      >
        传感器类型
      </button>
      <button
        class="tab-btn"
        :class="{ active: activeTab === 'sensors' }"
        type="button"
        @click="activeTab = 'sensors'"
      >
        传感器实例
      </button>
    </div>

    <section v-if="activeTab === 'types'" class="section-card">
      <div class="section-header">
        <div>
          <h3>传感器类型</h3>
          <p>维护传感器分类及其可上报的数据字段</p>
        </div>
        <button class="primary-btn" type="button" @click="openTypeCreate">
          <svg viewBox="0 0 24 24" width="15" height="15" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M12 5v14M5 12h14" />
          </svg>
          新增类型
        </button>
      </div>

      <div class="type-layout">
        <div class="type-list-panel">
          <div class="simple-table">
            <div class="table-row table-head">
              <span>类型名称</span>
              <span>检测数据</span>
              <span>操作</span>
            </div>
            <div v-if="typeLoading" class="empty-state inline">加载中...</div>
            <template v-else>
              <div
                v-for="item in sensorTypes"
                :key="item.id"
                class="table-row type-row"
                :class="{ active: selectedTypeId === item.id }"
                @click="selectSensorType(item.id)"
              >
                <span class="main-cell">
                  <strong>{{ item.sensorTypeName }}</strong>
                  <em>{{ item.description || '无描述' }}</em>
                </span>
                <span>{{ item.dataTypeCount }}</span>
                <span class="action-group">
                  <button class="small-btn" type="button" @click.stop="openTypeEdit(item)">
                    <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2">
                      <path d="M12 20h9" />
                      <path d="M16.5 3.5a2.12 2.12 0 0 1 3 3L7 19l-4 1 1-4Z" />
                    </svg>
                    编辑
                  </button>
                  <button class="small-btn danger" type="button" @click.stop="removeType(item)">
                    <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2">
                      <path d="M3 6h18" />
                      <path d="M8 6V4h8v2" />
                      <path d="M19 6l-1 14H6L5 6" />
                    </svg>
                    删除
                  </button>
                </span>
              </div>
              <div v-if="sensorTypes.length === 0" class="empty-state inline">暂无传感器类型</div>
            </template>
          </div>
        </div>

        <div class="data-type-panel">
          <div class="panel-head">
            <div>
              <h4>{{ selectedTypeDetail?.sensorTypeName || '检测数据项' }}</h4>
              <p>{{ selectedTypeDetail ? '字段名需与传感器上报数据保持一致' : '请选择传感器类型' }}</p>
            </div>
            <button
              class="ghost-btn"
              type="button"
              :disabled="!selectedTypeDetail"
              @click="openDataTypeCreate"
            >
              <svg viewBox="0 0 24 24" width="15" height="15" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M12 5v14M5 12h14" />
              </svg>
              添加数据项
            </button>
          </div>

          <div v-if="typeDetailLoading" class="empty-state">加载中...</div>
          <div v-else-if="!selectedTypeDetail" class="empty-state">请选择左侧传感器类型</div>
          <div v-else class="data-table-wrap">
            <table class="data-table">
              <thead>
                <tr>
                  <th>数据名称</th>
                  <th>单位</th>
                  <th>操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="item in selectedTypeDetail.dataTypes" :key="item.id">
                  <td class="mono">{{ item.dataName }}</td>
                  <td>{{ item.unit }}</td>
                  <td class="action-group">
                    <button class="small-btn" type="button" @click="openDataTypeEdit(item)">
                      <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2">
                        <path d="M12 20h9" />
                        <path d="M16.5 3.5a2.12 2.12 0 0 1 3 3L7 19l-4 1 1-4Z" />
                      </svg>
                      编辑
                    </button>
                    <button class="small-btn danger" type="button" @click="removeDataType(item)">
                      <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2">
                        <path d="M3 6h18" />
                        <path d="M8 6V4h8v2" />
                        <path d="M19 6l-1 14H6L5 6" />
                      </svg>
                      删除
                    </button>
                  </td>
                </tr>
              </tbody>
            </table>
            <div v-if="selectedTypeDetail.dataTypes.length === 0" class="empty-state inline">暂无检测数据项</div>
          </div>
        </div>
      </div>
    </section>

    <section v-else class="section-card">
      <div class="section-header">
        <div>
          <h3>传感器实例</h3>
          <p>注册传感器并绑定实验室</p>
        </div>
        <button class="primary-btn" type="button" @click="openSensorCreate">
          <svg viewBox="0 0 24 24" width="15" height="15" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M12 5v14M5 12h14" />
          </svg>
          注册传感器
        </button>
      </div>

      <div class="filter-row">
        <div class="filter-input-wrap">
          <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="11" cy="11" r="7" />
            <path d="M20 20l-3.5-3.5" />
          </svg>
          <input
            v-model="sensorFilter.keyword"
            class="filter-input"
            placeholder="搜索传感器编号"
            @keyup.enter="applySensorFilters"
          />
        </div>
        <select v-model="sensorFilter.sensorTypeId" class="filter-select">
          <option value="">全部类型</option>
          <option v-for="item in sensorTypes" :key="item.id" :value="item.id">{{ item.sensorTypeName }}</option>
        </select>
        <select v-model="sensorFilter.runningStatus" class="filter-select">
          <option value="">全部状态</option>
          <option :value="1">正常</option>
          <option :value="2">离线</option>
          <option :value="3">故障</option>
        </select>
        <button class="ghost-btn" type="button" @click="applySensorFilters">筛选</button>
        <button class="plain-btn" type="button" @click="resetSensorFilters">重置</button>
      </div>

      <div class="sensor-table-wrap">
        <table class="data-table sensor-table">
          <thead>
            <tr>
              <th>传感器编号</th>
              <th>类型</th>
              <th>绑定实验室</th>
              <th>运行状态</th>
              <th>最近上报</th>
              <th>备注</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in sensors" :key="item.id">
              <td class="mono strong">{{ item.sensorNo }}</td>
              <td>{{ item.sensorTypeName }}</td>
              <td>{{ formatLabTarget(item.targetId) }}</td>
              <td>
                <span class="status-badge" :class="statusClass(item.runningStatus)">
                  {{ item.runningStatusDescription }}
                </span>
              </td>
              <td class="date-cell">{{ formatDate(item.lastReportAt) }}</td>
              <td class="remark-cell">{{ item.remark || '-' }}</td>
              <td class="action-group">
                <button class="small-btn" type="button" @click="openSensorEdit(item)">
                  <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M12 20h9" />
                    <path d="M16.5 3.5a2.12 2.12 0 0 1 3 3L7 19l-4 1 1-4Z" />
                  </svg>
                  编辑
                </button>
                <button
                  v-if="item.runningStatus === 3"
                  class="small-btn"
                  type="button"
                  @click="clearSensorFault(item)"
                >
                  解除故障
                </button>
                <button v-else class="small-btn" type="button" @click="markSensorFault(item)">标记故障</button>
                <button class="small-btn danger" type="button" @click="removeSensor(item)">
                  <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M3 6h18" />
                    <path d="M8 6V4h8v2" />
                    <path d="M19 6l-1 14H6L5 6" />
                  </svg>
                  删除
                </button>
              </td>
            </tr>
          </tbody>
        </table>
        <div v-if="sensorLoading" class="empty-state inline">加载中...</div>
        <div v-else-if="sensors.length === 0" class="empty-state inline">暂无传感器实例</div>
      </div>

      <div class="pagination-row">
        <span>共 {{ sensorTotal }} 条</span>
        <div class="pager-actions">
          <button class="plain-btn" type="button" :disabled="sensorPage <= 1" @click="changeSensorPage(sensorPage - 1)">
            上一页
          </button>
          <span>{{ sensorPage }} / {{ sensorPages || 1 }}</span>
          <button
            class="plain-btn"
            type="button"
            :disabled="sensorPage >= sensorPages"
            @click="changeSensorPage(sensorPage + 1)"
          >
            下一页
          </button>
        </div>
      </div>
    </section>

    <teleport to="body">
      <transition name="modal-fade">
        <div v-if="showTypeModal" class="modal-overlay" @click.self="closeTypeModal">
          <div class="modal-panel small-modal">
            <div class="modal-header">
              <h3>{{ typeModalTitle }}</h3>
              <button class="modal-close" type="button" @click="closeTypeModal">
                <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M18 6L6 18M6 6l12 12" />
                </svg>
              </button>
            </div>
            <div class="modal-body">
              <div class="form-g">
                <label>类型名称 <span class="required">*</span></label>
                <input v-model="typeForm.sensorTypeName" class="form-input" placeholder="如 温湿度传感器" />
              </div>
              <div class="form-g">
                <label>描述</label>
                <textarea v-model="typeForm.description" class="form-textarea" rows="3" placeholder="可选"></textarea>
              </div>
            </div>
            <div class="modal-footer">
              <button class="modal-btn cancel" type="button" @click="closeTypeModal">取消</button>
              <button class="modal-btn primary" type="button" :disabled="typeSaving" @click="saveType">保存</button>
            </div>
          </div>
        </div>
      </transition>
    </teleport>

    <teleport to="body">
      <transition name="modal-fade">
        <div v-if="showDataTypeModal" class="modal-overlay" @click.self="closeDataTypeModal">
          <div class="modal-panel small-modal">
            <div class="modal-header">
              <h3>{{ dataTypeModalTitle }}</h3>
              <button class="modal-close" type="button" @click="closeDataTypeModal">
                <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M18 6L6 18M6 6l12 12" />
                </svg>
              </button>
            </div>
            <div class="modal-body">
              <div class="form-g">
                <label>数据名称 <span class="required">*</span></label>
                <input v-model="dataTypeForm.dataName" class="form-input" placeholder="如 光学强度 / temperature" />
              </div>
              <div class="form-g">
                <label>单位 <span class="required">*</span></label>
                <input v-model="dataTypeForm.unit" class="form-input" placeholder="如 °C / % / ppm" />
              </div>
            </div>
            <div class="modal-footer">
              <button class="modal-btn cancel" type="button" @click="closeDataTypeModal">取消</button>
              <button class="modal-btn primary" type="button" :disabled="dataTypeSaving" @click="saveDataType">
                保存
              </button>
            </div>
          </div>
        </div>
      </transition>
    </teleport>

    <teleport to="body">
      <transition name="modal-fade">
        <div v-if="showSensorModal" class="modal-overlay" @click.self="closeSensorModal">
          <div class="modal-panel sensor-modal">
            <div class="modal-header">
              <h3>{{ sensorModalTitle }}</h3>
              <button class="modal-close" type="button" @click="closeSensorModal">
                <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M18 6L6 18M6 6l12 12" />
                </svg>
              </button>
            </div>
            <div class="modal-body">
              <div class="form-grid">
                <div class="form-g">
                  <label>传感器编号 <span class="required">*</span></label>
                  <input
                    v-model="sensorForm.sensorNo"
                    class="form-input"
                    :disabled="sensorMode === 'edit'"
                    placeholder="如 TEMP-A305-01"
                  />
                </div>
                <div class="form-g">
                  <label>传感器类型 <span class="required">*</span></label>
                  <select v-model="sensorForm.sensorTypeId" class="form-input">
                    <option value="">请选择类型</option>
                    <option v-for="item in sensorTypes" :key="item.id" :value="item.id">{{ item.sensorTypeName }}</option>
                  </select>
                </div>
              </div>

              <div class="form-g lab-field">
                <label>绑定实验室 <span class="required">*</span></label>
                <div class="lab-search">
                  <input
                    v-model="labKeyword"
                    class="form-input"
                    placeholder="输入实验室名称 / 类型搜索"
                    @focus="openLabDropdown"
                    @input="onLabInput"
                  />
                  <div v-if="labDropdownOpen" class="lab-search-panel">
                    <div v-if="labLoading" class="search-state">搜索中...</div>
                    <template v-else>
                      <button
                        v-for="lab in labResults"
                        :key="lab.id"
                        type="button"
                        class="lab-option"
                        @click="selectSensorLab(lab)"
                      >
                        <span>{{ lab.labName }}</span>
                        <em>{{ lab.labTypeName }}</em>
                      </button>
                    </template>
                    <div v-if="!labLoading && labResults.length === 0" class="search-state">暂无匹配实验室</div>
                  </div>
                </div>
                <div v-if="selectedSensorLab" class="selected-lab">
                  <span>{{ selectedSensorLab.labName }}</span>
                  <strong>{{ selectedSensorLab.labTypeName }}</strong>
                </div>
              </div>

              <div class="form-g">
                <label>备注</label>
                <textarea v-model="sensorForm.remark" class="form-textarea" rows="3" placeholder="可选"></textarea>
              </div>
            </div>
            <div class="modal-footer">
              <button class="modal-btn cancel" type="button" @click="closeSensorModal">取消</button>
              <button class="modal-btn primary" type="button" :disabled="sensorSaving" @click="saveSensor">
                保存
              </button>
            </div>
          </div>
        </div>
      </transition>
    </teleport>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { searchThresholdLabs, type ThresholdLabItem } from '../../utils/iotThresholdApi'
import { getLabDetail } from '../../utils/labApi'
import {
  SENSOR_TARGET_LAB,
  createSensor,
  createSensorDataType,
  createSensorType,
  deleteSensor,
  deleteSensorDataType,
  deleteSensorType,
  getSensorPage,
  getSensorTypeDetail,
  listSensorTypes,
  updateSensor,
  updateSensorDataType,
  updateSensorStatus,
  updateSensorType,
  type SensorDataTypeItem,
  type SensorItem,
  type SensorRunningStatus,
  type SensorTypeDetail,
  type SensorTypeItem,
} from '../../utils/sensorApi'

type ActiveTab = 'types' | 'sensors'

interface SensorFilterForm {
  keyword: string
  sensorTypeId: number | ''
  runningStatus: SensorRunningStatus | ''
}

interface SensorForm {
  sensorNo: string
  sensorTypeId: number | ''
  remark: string
}

const activeTab = ref<ActiveTab>('types')
const refreshing = ref(false)

const sensorTypes = ref<SensorTypeItem[]>([])
const selectedTypeId = ref<number | null>(null)
const selectedTypeDetail = ref<SensorTypeDetail | null>(null)
const typeLoading = ref(false)
const typeDetailLoading = ref(false)

const showTypeModal = ref(false)
const typeMode = ref<'create' | 'edit'>('create')
const editingType = ref<SensorTypeItem | null>(null)
const typeSaving = ref(false)
const typeForm = reactive({ sensorTypeName: '', description: '' })

const showDataTypeModal = ref(false)
const dataTypeMode = ref<'create' | 'edit'>('create')
const editingDataType = ref<SensorDataTypeItem | null>(null)
const dataTypeSaving = ref(false)
const dataTypeForm = reactive({ dataName: '', unit: '' })

const sensors = ref<SensorItem[]>([])
const sensorLoading = ref(false)
const sensorPage = ref(1)
const sensorPageSize = ref(10)
const sensorTotal = ref(0)
const sensorPages = ref(0)
const sensorFilter = reactive<SensorFilterForm>({
  keyword: '',
  sensorTypeId: '',
  runningStatus: '',
})

const showSensorModal = ref(false)
const sensorMode = ref<'create' | 'edit'>('create')
const editingSensor = ref<SensorItem | null>(null)
const sensorSaving = ref(false)
const sensorForm = reactive<SensorForm>({
  sensorNo: '',
  sensorTypeId: '',
  remark: '',
})

const labKeyword = ref('')
const labResults = ref<ThresholdLabItem[]>([])
const selectedSensorLab = ref<ThresholdLabItem | null>(null)
const labDropdownOpen = ref(false)
const labLoading = ref(false)
let labSearchTimer: number | undefined

const typeModalTitle = computed(() => typeMode.value === 'create' ? '新增传感器类型' : '编辑传感器类型')
const dataTypeModalTitle = computed(() => dataTypeMode.value === 'create' ? '添加检测数据项' : '编辑检测数据项')
const sensorModalTitle = computed(() => sensorMode.value === 'create' ? '注册传感器' : '编辑传感器')

onMounted(async () => {
  await loadSensorTypes()
  await loadSensors()
})

async function refreshCurrent() {
  refreshing.value = true
  try {
    if (activeTab.value === 'types') {
      await loadSensorTypes()
    } else {
      await Promise.all([loadSensorTypes(), loadSensors()])
    }
  } finally {
    refreshing.value = false
  }
}

async function loadSensorTypes() {
  typeLoading.value = true
  try {
    const records = await listSensorTypes()
    sensorTypes.value = records
    if (records.length === 0) {
      selectedTypeId.value = null
      selectedTypeDetail.value = null
      return
    }
    if (!selectedTypeId.value || !records.some(item => item.id === selectedTypeId.value)) {
      selectedTypeId.value = records[0].id
    }
    await loadSelectedTypeDetail()
  } catch (error: any) {
    alert(error?.message || '加载传感器类型失败')
  } finally {
    typeLoading.value = false
  }
}

async function selectSensorType(sensorTypeId: number) {
  if (selectedTypeId.value === sensorTypeId) return
  selectedTypeId.value = sensorTypeId
  await loadSelectedTypeDetail()
}

async function loadSelectedTypeDetail() {
  if (!selectedTypeId.value) return
  typeDetailLoading.value = true
  try {
    selectedTypeDetail.value = await getSensorTypeDetail(selectedTypeId.value)
  } catch (error: any) {
    alert(error?.message || '加载检测数据项失败')
  } finally {
    typeDetailLoading.value = false
  }
}

function openTypeCreate() {
  typeMode.value = 'create'
  editingType.value = null
  typeForm.sensorTypeName = ''
  typeForm.description = ''
  showTypeModal.value = true
}

function openTypeEdit(item: SensorTypeItem) {
  typeMode.value = 'edit'
  editingType.value = item
  typeForm.sensorTypeName = item.sensorTypeName
  typeForm.description = item.description || ''
  showTypeModal.value = true
}

function closeTypeModal() {
  showTypeModal.value = false
}

async function saveType() {
  const sensorTypeName = typeForm.sensorTypeName.trim()
  if (!sensorTypeName) {
    alert('请输入传感器类型名称')
    return
  }
  typeSaving.value = true
  try {
    const payload = { sensorTypeName, description: optionalText(typeForm.description) }
    if (typeMode.value === 'edit' && editingType.value) {
      await updateSensorType(editingType.value.id, payload)
      selectedTypeId.value = editingType.value.id
    } else {
      const created = await createSensorType(payload)
      selectedTypeId.value = created.id
    }
    showTypeModal.value = false
    await loadSensorTypes()
  } catch (error: any) {
    alert(error?.message || '保存传感器类型失败')
  } finally {
    typeSaving.value = false
  }
}

async function removeType(item: SensorTypeItem) {
  if (!confirm(`确定删除传感器类型「${item.sensorTypeName}」吗？`)) return
  try {
    await deleteSensorType(item.id)
    if (selectedTypeId.value === item.id) {
      selectedTypeId.value = null
    }
    await Promise.all([loadSensorTypes(), loadSensors()])
  } catch (error: any) {
    alert(error?.message || '删除传感器类型失败')
  }
}

function openDataTypeCreate() {
  if (!selectedTypeDetail.value) return
  dataTypeMode.value = 'create'
  editingDataType.value = null
  dataTypeForm.dataName = ''
  dataTypeForm.unit = ''
  showDataTypeModal.value = true
}

function openDataTypeEdit(item: SensorDataTypeItem) {
  dataTypeMode.value = 'edit'
  editingDataType.value = item
  dataTypeForm.dataName = item.dataName
  dataTypeForm.unit = item.unit
  showDataTypeModal.value = true
}

function closeDataTypeModal() {
  showDataTypeModal.value = false
}

async function saveDataType() {
  if (!selectedTypeId.value) return
  const dataName = dataTypeForm.dataName.trim()
  const unit = dataTypeForm.unit.trim()
  if (!dataName) {
    alert('请输入检测数据名称')
    return
  }
  if (!unit) {
    alert('请输入检测数据单位')
    return
  }
  dataTypeSaving.value = true
  try {
    const payload = { dataName, unit }
    if (dataTypeMode.value === 'edit' && editingDataType.value) {
      await updateSensorDataType(editingDataType.value.id, payload)
    } else {
      await createSensorDataType(selectedTypeId.value, payload)
    }
    showDataTypeModal.value = false
    await loadSensorTypes()
  } catch (error: any) {
    alert(error?.message || '保存检测数据项失败')
  } finally {
    dataTypeSaving.value = false
  }
}

async function removeDataType(item: SensorDataTypeItem) {
  if (!confirm(`确定删除检测数据项「${item.dataName}」吗？`)) return
  try {
    await deleteSensorDataType(item.id)
    await loadSensorTypes()
  } catch (error: any) {
    alert(error?.message || '删除检测数据项失败')
  }
}

async function loadSensors() {
  sensorLoading.value = true
  try {
    const page = await getSensorPage({
      page: sensorPage.value,
      pageSize: sensorPageSize.value,
      keyword: optionalText(sensorFilter.keyword) || undefined,
      sensorTypeId: sensorFilter.sensorTypeId === '' ? undefined : Number(sensorFilter.sensorTypeId),
      runningStatus: sensorFilter.runningStatus === '' ? undefined : Number(sensorFilter.runningStatus) as SensorRunningStatus,
      targetType: SENSOR_TARGET_LAB,
      sortField: 'created_at',
      sortOrder: 'desc',
    })
    sensors.value = page.records || []
    sensorTotal.value = page.total || 0
    sensorPages.value = page.pages || Math.ceil(sensorTotal.value / sensorPageSize.value)
  } catch (error: any) {
    alert(error?.message || '加载传感器实例失败')
  } finally {
    sensorLoading.value = false
  }
}

function applySensorFilters() {
  sensorPage.value = 1
  void loadSensors()
}

function resetSensorFilters() {
  sensorFilter.keyword = ''
  sensorFilter.sensorTypeId = ''
  sensorFilter.runningStatus = ''
  sensorPage.value = 1
  void loadSensors()
}

function changeSensorPage(page: number) {
  if (page < 1 || page > sensorPages.value) return
  sensorPage.value = page
  void loadSensors()
}

function openSensorCreate() {
  if (sensorTypes.value.length === 0) {
    alert('请先创建传感器类型')
    activeTab.value = 'types'
    return
  }
  sensorMode.value = 'create'
  editingSensor.value = null
  sensorForm.sensorNo = ''
  sensorForm.sensorTypeId = sensorTypes.value[0].id
  sensorForm.remark = ''
  labKeyword.value = ''
  labResults.value = []
  selectedSensorLab.value = null
  labDropdownOpen.value = false
  showSensorModal.value = true
}

async function openSensorEdit(item: SensorItem) {
  sensorMode.value = 'edit'
  editingSensor.value = item
  sensorForm.sensorNo = item.sensorNo
  sensorForm.sensorTypeId = item.sensorTypeId
  sensorForm.remark = item.remark || ''
  labKeyword.value = ''
  labResults.value = []
  selectedSensorLab.value = null
  labDropdownOpen.value = false
  showSensorModal.value = true
  await loadSensorLabById(item.targetId)
}

function closeSensorModal() {
  showSensorModal.value = false
}

async function saveSensor() {
  const sensorTypeId = sensorForm.sensorTypeId === '' ? null : Number(sensorForm.sensorTypeId)
  if (!sensorTypeId) {
    alert('请选择传感器类型')
    return
  }
  if (!selectedSensorLab.value) {
    alert('请选择绑定实验室')
    return
  }
  const sensorNo = sensorForm.sensorNo.trim()
  if (sensorMode.value === 'create' && !sensorNo) {
    alert('请输入传感器编号')
    return
  }
  sensorSaving.value = true
  try {
    const basePayload = {
      sensorTypeId,
      targetType: SENSOR_TARGET_LAB,
      targetId: selectedSensorLab.value.id,
      remark: optionalText(sensorForm.remark),
    }
    if (sensorMode.value === 'edit' && editingSensor.value) {
      await updateSensor(editingSensor.value.id, basePayload)
    } else {
      await createSensor({ ...basePayload, sensorNo })
    }
    showSensorModal.value = false
    await loadSensors()
  } catch (error: any) {
    alert(error?.message || '保存传感器失败')
  } finally {
    sensorSaving.value = false
  }
}

async function removeSensor(item: SensorItem) {
  if (!confirm(`确定删除传感器「${item.sensorNo}」吗？`)) return
  try {
    await deleteSensor(item.id)
    await loadSensors()
  } catch (error: any) {
    alert(error?.message || '删除传感器失败')
  }
}

async function markSensorFault(item: SensorItem) {
  const reason = window.prompt(`请输入传感器「${item.sensorNo}」的故障原因`)
  if (reason === null) return
  const normalized = reason.trim()
  if (!normalized) {
    alert('请输入故障原因')
    return
  }
  try {
    await updateSensorStatus(item.id, { runningStatus: 3, reason: normalized })
    await loadSensors()
  } catch (error: any) {
    alert(error?.message || '标记故障失败')
  }
}

async function clearSensorFault(item: SensorItem) {
  if (!confirm(`确定解除传感器「${item.sensorNo}」的故障状态吗？`)) return
  try {
    await updateSensorStatus(item.id, { runningStatus: 1, reason: '管理员解除故障' })
    await loadSensors()
  } catch (error: any) {
    alert(error?.message || '解除故障失败')
  }
}

function openLabDropdown() {
  labDropdownOpen.value = true
  if (labResults.value.length === 0) {
    void loadLabs(labKeyword.value)
  }
}

function onLabInput() {
  selectedSensorLab.value = null
  labDropdownOpen.value = true
  if (labSearchTimer) window.clearTimeout(labSearchTimer)
  labSearchTimer = window.setTimeout(() => {
    void loadLabs(labKeyword.value)
  }, 260)
}

async function loadLabs(keyword = '') {
  labLoading.value = true
  try {
    const page = await searchThresholdLabs(keyword.trim())
    labResults.value = page.records || []
  } catch (error: any) {
    alert(error?.message || '搜索实验室失败')
  } finally {
    labLoading.value = false
  }
}

function selectSensorLab(lab: ThresholdLabItem) {
  selectedSensorLab.value = lab
  labKeyword.value = lab.labName
  labDropdownOpen.value = false
}

async function loadSensorLabById(labId: number) {
  labLoading.value = true
  try {
    const lab = await getLabDetail(labId)
    selectedSensorLab.value = {
      id: lab.id,
      labName: lab.labName,
      labTypeId: lab.labTypeId,
      labTypeName: lab.labTypeName,
    }
    labKeyword.value = lab.labName
  } catch {
    selectedSensorLab.value = {
      id: labId,
      labName: `实验室 #${labId}`,
      labTypeId: 0,
      labTypeName: '已绑定',
    }
    labKeyword.value = `实验室 #${labId}`
  } finally {
    labLoading.value = false
  }
}

function optionalText(value: string): string | null {
  const normalized = value.trim()
  return normalized || null
}

function formatDate(value: string | null | undefined): string {
  return value ? value.replace('T', ' ').slice(0, 16) : '-'
}

function formatLabTarget(targetId: number): string {
  return `实验室 #${targetId}`
}

function statusClass(status: SensorRunningStatus): string {
  if (status === 1) return 'normal'
  if (status === 2) return 'offline'
  return 'fault'
}
</script>

<style scoped>
.sd-page { height:100%; overflow-y:auto; padding:20px 24px; box-sizing:border-box }
.page-header { display:flex; align-items:flex-start; justify-content:space-between; gap:16px; margin-bottom:16px }
.page-header h2 { margin:0; font-size:20px; font-weight:600; color:#1a1a2e }
.page-header p { margin:4px 0 0; color:#888; font-size:13px }
.tab-bar { display:inline-flex; gap:4px; padding:4px; border:1px solid rgba(0,0,0,.06); border-radius:10px; background:rgba(255,255,255,.55); margin-bottom:16px }
.tab-btn { min-width:112px; border:none; border-radius:8px; padding:9px 14px; background:transparent; color:#666; font-size:13px; font-weight:600; cursor:pointer }
.tab-btn.active { background:#fff; color:#1a73e8; box-shadow:0 4px 12px rgba(20,40,80,.08) }
.section-card { background:rgba(255,255,255,.54); backdrop-filter:blur(10px); border:1px solid rgba(0,0,0,.05); border-radius:12px; padding:18px 20px }
.section-header { display:flex; align-items:center; justify-content:space-between; gap:16px; margin-bottom:16px }
.section-header h3 { margin:0; color:#1a1a2e; font-size:16px; font-weight:600 }
.section-header p { margin:4px 0 0; color:#888; font-size:13px }
.primary-btn,.icon-text-btn,.ghost-btn,.plain-btn,.small-btn { display:inline-flex; align-items:center; justify-content:center; gap:6px; border-radius:8px; font-family:inherit; cursor:pointer; transition:.15s; white-space:nowrap }
.primary-btn { border:none; padding:9px 15px; background:#1a73e8; color:#fff; font-size:13px; font-weight:600 }
.primary-btn:hover { background:#1767cf }
.icon-text-btn,.ghost-btn { border:1px solid rgba(26,115,232,.22); background:rgba(255,255,255,.78); color:#1a73e8; padding:8px 13px; font-size:13px; font-weight:600 }
.icon-text-btn:hover:not(:disabled),.ghost-btn:hover:not(:disabled) { border-color:#1a73e8; background:rgba(26,115,232,.06) }
.plain-btn { border:1px solid rgba(0,0,0,.08); background:#fff; color:#666; padding:8px 13px; font-size:13px }
.plain-btn:hover:not(:disabled) { border-color:#1a73e8; color:#1a73e8 }
.small-btn { border:1px solid rgba(0,0,0,.08); background:#fff; color:#555; padding:5px 9px; font-size:12px }
.small-btn:hover { border-color:#1a73e8; color:#1a73e8 }
.small-btn.danger:hover { border-color:#e74c3c; color:#e74c3c }
button:disabled { opacity:.45; cursor:not-allowed }
.type-layout { display:grid; grid-template-columns:minmax(420px,1.05fr) minmax(360px,.95fr); gap:16px }
.type-list-panel,.data-type-panel { min-width:0; border:1px solid rgba(0,0,0,.05); border-radius:10px; background:rgba(255,255,255,.48); overflow:hidden }
.simple-table { display:flex; flex-direction:column }
.table-row { display:grid; grid-template-columns:minmax(160px,1fr) 88px 172px; align-items:center; gap:12px; min-height:48px; padding:10px 12px; border-bottom:1px solid rgba(0,0,0,.04); box-sizing:border-box }
.table-row.table-head { min-height:36px; background:rgba(0,0,0,.025); color:#888; font-size:12px; font-weight:600 }
.type-row { cursor:pointer }
.type-row:hover,.type-row.active { background:rgba(26,115,232,.06) }
.main-cell { display:flex; flex-direction:column; gap:3px; min-width:0 }
.main-cell strong { color:#1a1a2e; font-size:13px; overflow:hidden; text-overflow:ellipsis; white-space:nowrap }
.main-cell em { color:#888; font-size:12px; font-style:normal; overflow:hidden; text-overflow:ellipsis; white-space:nowrap }
.action-group { display:flex; align-items:center; gap:6px; flex-wrap:wrap }
.panel-head { display:flex; align-items:center; justify-content:space-between; gap:12px; padding:14px 14px 12px; border-bottom:1px solid rgba(0,0,0,.05) }
.panel-head h4 { margin:0; color:#1a1a2e; font-size:15px; font-weight:600 }
.panel-head p { margin:4px 0 0; color:#888; font-size:12px }
.data-table-wrap,.sensor-table-wrap { width:100%; overflow-x:auto }
.data-table { width:100%; border-collapse:collapse; font-size:13px }
.data-table thead { background:rgba(0,0,0,.025) }
.data-table th { padding:10px 12px; text-align:left; color:#888; font-size:12px; font-weight:600; border-bottom:1px solid rgba(0,0,0,.05); white-space:nowrap }
.data-table td { padding:10px 12px; color:#333; border-bottom:1px solid rgba(0,0,0,.04); vertical-align:middle }
.sensor-table th:last-child,.sensor-table td:last-child { min-width:230px }
.mono { font-family:'SF Mono','Menlo',monospace; font-size:12px; color:#666 }
.mono.strong { color:#1a1a2e; font-weight:600 }
.date-cell { color:#888; font-size:12px; white-space:nowrap }
.remark-cell { max-width:180px; color:#666; overflow:hidden; text-overflow:ellipsis; white-space:nowrap }
.empty-state { padding:34px 12px; text-align:center; color:#aaa; font-size:14px }
.empty-state.inline { padding:22px 12px }
.filter-row { display:flex; align-items:center; gap:10px; margin-bottom:14px; flex-wrap:wrap }
.filter-input-wrap { width:260px; display:flex; align-items:center; gap:8px; border:1px solid rgba(0,0,0,.09); border-radius:9px; background:#fff; padding:0 10px; box-sizing:border-box; color:#999 }
.filter-input { width:100%; height:38px; border:none; outline:none; background:transparent; font-size:13px; color:#333 }
.filter-select { height:38px; min-width:150px; border:1px solid rgba(0,0,0,.09); border-radius:9px; background:#fff; padding:0 10px; color:#333; font-size:13px; outline:none }
.status-badge { display:inline-flex; align-items:center; min-width:42px; justify-content:center; border-radius:5px; padding:3px 8px; font-size:11px; font-weight:600 }
.status-badge.normal { background:rgba(39,174,96,.1); color:#219653 }
.status-badge.offline { background:rgba(120,120,120,.1); color:#777 }
.status-badge.fault { background:rgba(231,76,60,.1); color:#d6453b }
.pagination-row { display:flex; align-items:center; justify-content:space-between; gap:12px; margin-top:14px; color:#777; font-size:13px }
.pager-actions { display:flex; align-items:center; gap:10px }
.modal-overlay { position:fixed; inset:0; background:rgba(0,0,0,.35); display:flex; align-items:center; justify-content:center; z-index:1100; padding:20px; box-sizing:border-box }
.modal-panel { width:100%; max-height:86vh; display:flex; flex-direction:column; background:#fff; border-radius:14px; box-shadow:0 16px 48px rgba(0,0,0,.15) }
.small-modal { max-width:520px }
.sensor-modal { max-width:680px }
.modal-header { display:flex; align-items:center; justify-content:space-between; gap:12px; padding:20px 24px 0 }
.modal-header h3 { margin:0; color:#1a1a2e; font-size:17px; font-weight:600 }
.modal-close { border:none; background:transparent; color:#aaa; cursor:pointer; padding:4px; border-radius:6px }
.modal-close:hover { background:rgba(0,0,0,.05); color:#333 }
.modal-body { display:flex; flex-direction:column; gap:14px; padding:20px 24px; overflow-y:auto }
.modal-footer { display:flex; justify-content:flex-end; gap:10px; padding:16px 24px 20px; border-top:1px solid rgba(0,0,0,.06) }
.modal-btn { min-width:88px; border:none; border-radius:9px; padding:10px 20px; cursor:pointer; font-size:14px; font-weight:600 }
.modal-btn.cancel { background:rgba(0,0,0,.05); color:#666 }
.modal-btn.primary { background:#1a73e8; color:#fff }
.form-grid { display:grid; grid-template-columns:1fr 1fr; gap:14px }
.form-g { display:flex; flex-direction:column; gap:6px }
.form-g label { color:#555; font-size:13px; font-weight:600 }
.required { color:#e74c3c }
.form-input,.form-textarea { width:100%; box-sizing:border-box; border:1px solid rgba(0,0,0,.1); border-radius:9px; background:#fff; color:#333; outline:none; font-family:inherit; font-size:14px; transition:.15s }
.form-input { height:40px; padding:0 12px }
.form-textarea { padding:10px 12px; resize:vertical }
.form-input:focus,.form-textarea:focus { border-color:#1a73e8 }
.form-input:disabled { background:rgba(0,0,0,.04); color:#777 }
.lab-field { position:relative }
.lab-search { position:relative }
.lab-search-panel { position:absolute; top:calc(100% + 6px); left:0; right:0; max-height:260px; overflow-y:auto; background:#fff; border:1px solid rgba(0,0,0,.08); border-radius:10px; box-shadow:0 12px 30px rgba(15,35,65,.14); z-index:10; padding:6px }
.lab-option { width:100%; display:flex; flex-direction:column; gap:3px; border:none; border-radius:8px; background:#fff; padding:9px 10px; text-align:left; cursor:pointer }
.lab-option:hover { background:rgba(26,115,232,.08) }
.lab-option span { color:#1a1a2e; font-size:13px; font-weight:600 }
.lab-option em { color:#777; font-size:12px; font-style:normal }
.search-state { padding:16px 10px; color:#999; font-size:13px; text-align:center }
.selected-lab { display:flex; align-items:center; gap:8px; margin-top:4px; font-size:13px; color:#555 }
.selected-lab strong { border-radius:5px; background:rgba(26,115,232,.09); color:#1a73e8; padding:3px 8px; font-size:12px }
.modal-fade-enter-active { transition:all .18s ease }
.modal-fade-leave-active { transition:all .14s ease }
.modal-fade-enter-from,.modal-fade-leave-to { opacity:0 }
.modal-fade-enter-from .modal-panel { transform:translateY(10px) scale(.96) }
@media (max-width: 980px) {
  .type-layout { grid-template-columns:1fr }
  .filter-input-wrap { width:100% }
}
@media (max-width: 720px) {
  .sd-page { padding:16px }
  .page-header,.section-header,.pagination-row { align-items:flex-start; flex-direction:column }
  .form-grid { grid-template-columns:1fr }
  .table-row { grid-template-columns:minmax(130px,1fr) 70px 1fr }
}
</style>
