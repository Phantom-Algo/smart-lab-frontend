<template>
  <div class="it-page">
    <div class="page-header">
      <h2>阈值配置</h2>
      <p>安全阈值设置、传感器绑定</p>
    </div>

    <div class="section-card">
      <div class="section-header">
        <h3>安全阈值设置</h3>
        <div class="section-actions">
          <div class="lab-search">
            <input
              v-model="labKeyword"
              class="lab-search-input"
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
                  @click="selectLab(lab)"
                >
                  <span class="lab-option-name">{{ lab.labName }}</span>
                  <span class="lab-option-type">{{ lab.labTypeName }}</span>
                </button>
              </template>
              <div v-if="!labLoading && labResults.length === 0" class="search-state">暂无匹配实验室</div>
            </div>
          </div>
          <button class="btn-clear" :disabled="!selectedLab" @click="clearIndicators">清空</button>
        </div>
      </div>

      <div v-if="selectedLab" class="selected-lab">
        <span>{{ selectedLab.labName }}</span>
        <strong>{{ selectedLab.labTypeName }}</strong>
      </div>

      <div v-if="!selectedLab" class="empty-state">
        <p>请先搜索并选择实验室</p>
      </div>

      <div v-else class="threshold-table">
        <div class="th-row th-head">
          <span class="th-col-name">属性指标</span>
          <span class="th-col-unit">单位</span>
          <span class="th-col-range">安全阈值</span>
          <span class="th-col-act">操作</span>
        </div>
        <div v-if="indicatorLoading" class="empty-state inline">
          <p>加载中...</p>
        </div>
        <template v-else>
          <div v-for="item in indicators" :key="item.id" class="th-row">
            <span class="th-col-name">{{ item.indicatorName }}</span>
            <span class="th-col-unit">{{ item.unit }}</span>
            <span class="th-col-range">{{ formatSafeRange(item.safeRange, item.unit) }}</span>
            <span class="th-col-act action-group">
              <button class="act-link" @click="openIndicatorEdit(item)">编辑</button>
              <button class="act-link danger-text" @click="deleteIndicator(item)">删除</button>
            </span>
          </div>
          <div v-if="indicators.length === 0" class="empty-state inline">
            <p>暂无安全阈值指标</p>
          </div>
          <button type="button" class="add-indicator-block" @click="openIndicatorCreate">添加指标</button>
        </template>
      </div>
    </div>

    <div class="section-card">
      <div class="section-header">
        <h3>传感器绑定管理</h3>
        <button class="btn-add" :disabled="!selectedLab" @click="openSensorModal">绑定传感器</button>
      </div>

      <div v-if="!selectedLab" class="empty-state small">
        <p>请先搜索并选择实验室</p>
      </div>
      <div v-else-if="bindingLoading" class="empty-state small">
        <p>加载中...</p>
      </div>
      <div v-else-if="bindings.length > 0" class="bind-table-wrap">
        <table class="bind-table">
          <thead>
            <tr>
              <th>传感器编号</th>
              <th>传感器类型</th>
              <th>绑定状态</th>
              <th>绑定时间</th>
              <th>解绑时间</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in bindings" :key="item.bindingId">
              <td class="mono">{{ item.sensorNo }}</td>
              <td>{{ item.sensorTypeName }}</td>
              <td>
                <span class="bind-status" :class="isBound(item) ? 'bs-bound' : 'bs-unbound'">
                  {{ item.bindStatusDesc }}
                </span>
              </td>
              <td class="date-cell">{{ formatDate(item.boundAt) }}</td>
              <td class="date-cell">{{ formatDate(item.unboundAt) }}</td>
              <td>
                <button v-if="isBound(item)" class="act-link danger-text" @click="unbindSensor(item)">解绑</button>
                <button v-else class="act-link" @click="bindExistingSensor(item)">绑定</button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
      <div v-else class="empty-state small">
        <p>暂无传感器绑定记录</p>
      </div>
    </div>

    <teleport to="body">
      <transition name="modal-fade">
        <div v-if="showIndicatorModal" class="modal-overlay" @click.self="closeIndicatorModal">
          <div class="modal-panel indicator-modal">
            <div class="modal-header">
              <h3>{{ indicatorModalTitle }}</h3>
              <button class="modal-close" @click="closeIndicatorModal">
                <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M18 6L6 18M6 6l12 12" />
                </svg>
              </button>
            </div>
            <div class="modal-body">
              <div class="form-g">
                <label>指标名称 <span class="required">*</span></label>
                <input v-model="indicatorForm.indicatorName" class="form-input" placeholder="如 temperature" />
              </div>
              <div class="form-g">
                <label>指标单位 <span class="required">*</span></label>
                <input v-model="indicatorForm.unit" class="form-input" placeholder="如 °C / % / ppm" />
              </div>
              <div class="form-g">
                <label>安全阈值 <span class="required">*</span></label>
                <div class="safe-range-list">
                  <div v-for="(range, index) in indicatorForm.safeRangeRows" :key="index" class="safe-range-row">
                    <input v-model="range.lower" class="form-input" type="number" step="any" placeholder="下限，可空" />
                    <span class="range-sep">至</span>
                    <input v-model="range.upper" class="form-input" type="number" step="any" placeholder="上限，可空" />
                    <button
                      class="range-remove"
                      type="button"
                      :disabled="indicatorForm.safeRangeRows.length === 1"
                      @click="removeSafeRange(index)"
                    >
                      删除
                    </button>
                  </div>
                </div>
                <button class="add-range-btn" type="button" @click="addSafeRange">添加安全阈值</button>
              </div>
            </div>
            <div class="modal-footer">
              <button class="modal-btn cancel" @click="closeIndicatorModal">取消</button>
              <button class="modal-btn primary" :disabled="indicatorSaving" @click="saveIndicator">保存</button>
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
              <h3>绑定传感器</h3>
              <button class="modal-close" @click="closeSensorModal">
                <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M18 6L6 18M6 6l12 12" />
                </svg>
              </button>
            </div>
            <div class="modal-body">
              <div class="form-g">
                <label>搜索传感器</label>
                <input
                  v-model="sensorKeyword"
                  class="form-input"
                  placeholder="输入传感器编号 / 类型 / 属性指标名"
                  @input="onSensorInput"
                />
              </div>
              <div class="sensor-result-list">
                <div v-if="availableLoading" class="empty-state inline">
                  <p>搜索中...</p>
                </div>
                <template v-else>
                  <button
                    v-for="sensor in availableSensors"
                    :key="sensor.sensorId"
                    type="button"
                    class="sensor-option"
                    :class="{ active: selectedAvailableSensor?.sensorId === sensor.sensorId }"
                    @click="selectedAvailableSensor = sensor"
                  >
                    <span class="sensor-main">
                      <strong>{{ sensor.sensorNo }}</strong>
                      <em>{{ sensor.sensorTypeName }}</em>
                    </span>
                    <span class="sensor-data-types">{{ formatDataTypes(sensor.dataTypes) }}</span>
                  </button>
                </template>
                <div v-if="!availableLoading && availableSensors.length === 0" class="empty-state inline">
                  <p>暂无可绑定传感器</p>
                </div>
              </div>
            </div>
            <div class="modal-footer">
              <button class="modal-btn cancel" @click="closeSensorModal">取消</button>
              <button
                class="modal-btn primary"
                :disabled="!selectedAvailableSensor || sensorSaving"
                @click="bindSelectedSensor"
              >
                确认绑定
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
import {
  bindLabSensor,
  clearDetectionIndicators,
  createDetectionIndicator,
  deleteDetectionIndicator,
  getDetectionIndicators,
  getLabSensorBindings,
  searchAvailableLabSensors,
  searchThresholdLabs,
  unbindLabSensor,
  updateDetectionIndicator,
  type AvailableLabSensorItem,
  type DetectionIndicatorItem,
  type LabSensorBindingItem,
  type SensorDataTypeItem,
  type ThresholdLabItem,
} from '../../utils/iotThresholdApi'

type SafeRangeBoundary = string | number | null | undefined

interface SafeRangeRow {
  lower: SafeRangeBoundary
  upper: SafeRangeBoundary
}

const labKeyword = ref('')
const labResults = ref<ThresholdLabItem[]>([])
const selectedLab = ref<ThresholdLabItem | null>(null)
const labDropdownOpen = ref(false)
const labLoading = ref(false)
let labSearchTimer: number | undefined

const indicators = ref<DetectionIndicatorItem[]>([])
const bindings = ref<LabSensorBindingItem[]>([])
const indicatorLoading = ref(false)
const bindingLoading = ref(false)

const showIndicatorModal = ref(false)
const indicatorMode = ref<'create' | 'edit'>('create')
const editingIndicator = ref<DetectionIndicatorItem | null>(null)
const indicatorSaving = ref(false)
const indicatorForm = reactive<{ indicatorName: string; unit: string; safeRangeRows: SafeRangeRow[] }>({
  indicatorName: '',
  unit: '',
  safeRangeRows: [{ lower: '', upper: '' }],
})

const showSensorModal = ref(false)
const sensorKeyword = ref('')
const availableSensors = ref<AvailableLabSensorItem[]>([])
const selectedAvailableSensor = ref<AvailableLabSensorItem | null>(null)
const availableLoading = ref(false)
const sensorSaving = ref(false)
let sensorSearchTimer: number | undefined

const indicatorModalTitle = computed(() => indicatorMode.value === 'create' ? '添加指标' : '编辑指标')

onMounted(async () => {
  await loadLabs('', true)
})

function openLabDropdown() {
  labDropdownOpen.value = true
  if (labResults.value.length === 0) {
    void loadLabs(labKeyword.value, false)
  }
}

function onLabInput() {
  selectedLab.value = null
  indicators.value = []
  bindings.value = []
  labDropdownOpen.value = true
  if (labSearchTimer) window.clearTimeout(labSearchTimer)
  labSearchTimer = window.setTimeout(() => {
    void loadLabs(labKeyword.value, false)
  }, 260)
}

async function loadLabs(keyword = '', autoSelect = false) {
  labLoading.value = true
  try {
    const page = await searchThresholdLabs(keyword.trim())
    labResults.value = page.records || []
    if (autoSelect && labResults.value.length > 0) {
      await selectLab(labResults.value[0])
    }
  } catch (error: any) {
    alert(error?.message || '搜索实验室失败')
  } finally {
    labLoading.value = false
  }
}

async function selectLab(lab: ThresholdLabItem) {
  selectedLab.value = lab
  labKeyword.value = lab.labName
  labDropdownOpen.value = false
  await loadSelectedLabData()
}

async function loadSelectedLabData() {
  if (!selectedLab.value) return
  await Promise.all([loadIndicators(), loadBindings()])
}

async function loadIndicators() {
  if (!selectedLab.value) return
  indicatorLoading.value = true
  try {
    indicators.value = await getDetectionIndicators(selectedLab.value.labTypeId)
  } catch (error: any) {
    alert(error?.message || '加载安全阈值失败')
  } finally {
    indicatorLoading.value = false
  }
}

async function loadBindings() {
  if (!selectedLab.value) return
  bindingLoading.value = true
  try {
    bindings.value = await getLabSensorBindings(selectedLab.value.id)
  } catch (error: any) {
    alert(error?.message || '加载传感器绑定失败')
  } finally {
    bindingLoading.value = false
  }
}

function openIndicatorCreate() {
  if (!selectedLab.value) return
  indicatorMode.value = 'create'
  editingIndicator.value = null
  resetIndicatorForm('', '', [[null, null]])
  showIndicatorModal.value = true
}

function openIndicatorEdit(item: DetectionIndicatorItem) {
  indicatorMode.value = 'edit'
  editingIndicator.value = item
  resetIndicatorForm(item.indicatorName, item.unit, item.safeRange)
  showIndicatorModal.value = true
}

function closeIndicatorModal() {
  showIndicatorModal.value = false
}

function resetIndicatorForm(name: string, unit: string, safeRange: Array<Array<number | null>>) {
  indicatorForm.indicatorName = name
  indicatorForm.unit = unit
  indicatorForm.safeRangeRows.splice(0, indicatorForm.safeRangeRows.length, ...toSafeRangeRows(safeRange))
}

function toSafeRangeRows(safeRange: Array<Array<number | null>>): SafeRangeRow[] {
  const rows = safeRange?.length ? safeRange : [[null, null]]
  return rows.map(range => ({
    lower: range[0] == null ? '' : String(range[0]),
    upper: range[1] == null ? '' : String(range[1]),
  }))
}

function addSafeRange() {
  indicatorForm.safeRangeRows.push({ lower: '', upper: '' })
}

function removeSafeRange(index: number) {
  if (indicatorForm.safeRangeRows.length === 1) return
  indicatorForm.safeRangeRows.splice(index, 1)
}

async function saveIndicator() {
  if (!selectedLab.value) return
  const safeRange = buildSafeRangePayload()
  if (!safeRange) return
  const indicatorName = indicatorForm.indicatorName.trim()
  const unit = indicatorForm.unit.trim()
  if (!indicatorName) {
    alert('请输入指标名称')
    return
  }
  if (!unit) {
    alert('请输入指标单位')
    return
  }

  indicatorSaving.value = true
  try {
    const payload = { labTypeId: selectedLab.value.labTypeId, indicatorName, unit, safeRange }
    if (indicatorMode.value === 'edit' && editingIndicator.value) {
      await updateDetectionIndicator(editingIndicator.value.id, payload)
    } else {
      await createDetectionIndicator(payload)
    }
    showIndicatorModal.value = false
    await loadIndicators()
  } catch (error: any) {
    alert(error?.message || '保存指标失败')
  } finally {
    indicatorSaving.value = false
  }
}

function buildSafeRangePayload(): Array<Array<number | null>> | null {
  const result: Array<Array<number | null>> = []
  for (const row of indicatorForm.safeRangeRows) {
    const lower = parseBoundary(row.lower)
    const upper = parseBoundary(row.upper)
    if (lower.invalid || upper.invalid) {
      alert('安全阈值必须是合法数字')
      return null
    }
    if (lower.value == null && upper.value == null) {
      alert('每段安全阈值至少需要填写下限或上限')
      return null
    }
    if (lower.value != null && upper.value != null && lower.value > upper.value) {
      alert('安全阈值下限不能大于上限')
      return null
    }
    result.push([lower.value, upper.value])
  }
  return result
}

function parseBoundary(raw: SafeRangeBoundary): { value: number | null; invalid: boolean } {
  if (raw == null) return { value: null, invalid: false }
  if (typeof raw === 'number') {
    return Number.isFinite(raw)
      ? { value: raw, invalid: false }
      : { value: null, invalid: true }
  }

  const value = raw.trim()
  if (!value) return { value: null, invalid: false }
  const numberValue = Number(value)
  return Number.isFinite(numberValue)
    ? { value: numberValue, invalid: false }
    : { value: null, invalid: true }
}

async function deleteIndicator(item: DetectionIndicatorItem) {
  if (!confirm(`确定删除指标「${item.indicatorName}」吗？`)) return
  try {
    await deleteDetectionIndicator(item.id)
    await loadIndicators()
  } catch (error: any) {
    alert(error?.message || '删除指标失败')
  }
}

async function clearIndicators() {
  if (!selectedLab.value) return
  if (!confirm(`确定删除「${selectedLab.value.labTypeName}」下全部安全阈值指标吗？`)) return
  try {
    await clearDetectionIndicators(selectedLab.value.labTypeId)
    indicators.value = []
  } catch (error: any) {
    alert(error?.message || '清空指标失败')
  }
}

function formatSafeRange(safeRange: Array<Array<number | null>>, unit: string) {
  if (!safeRange?.length) return '-'
  return safeRange.map(range => {
    const lower = range[0]
    const upper = range[1]
    if (lower == null && upper == null) return '-'
    if (lower == null) return `小于等于 ${upper}${unit}`
    if (upper == null) return `大于等于 ${lower}${unit}`
    return `${lower}${unit} 至 ${upper}${unit}`
  }).join('；')
}

function isBound(item: LabSensorBindingItem) {
  return item.bindStatus === 1
}

function formatDate(value: string | null) {
  return value ? value.replace('T', ' ').slice(0, 16) : '-'
}

async function bindExistingSensor(item: LabSensorBindingItem) {
  if (!selectedLab.value) return
  try {
    await bindLabSensor(selectedLab.value.id, item.sensorId)
    await loadBindings()
  } catch (error: any) {
    alert(error?.message || '绑定传感器失败')
  }
}

async function unbindSensor(item: LabSensorBindingItem) {
  if (!selectedLab.value) return
  if (!confirm(`确定解绑传感器「${item.sensorNo}」吗？`)) return
  try {
    await unbindLabSensor(selectedLab.value.id, item.sensorId)
    await loadBindings()
  } catch (error: any) {
    alert(error?.message || '解绑传感器失败')
  }
}

async function openSensorModal() {
  if (!selectedLab.value) return
  sensorKeyword.value = ''
  availableSensors.value = []
  selectedAvailableSensor.value = null
  showSensorModal.value = true
  await loadAvailableSensors()
}

function closeSensorModal() {
  showSensorModal.value = false
}

function onSensorInput() {
  selectedAvailableSensor.value = null
  if (sensorSearchTimer) window.clearTimeout(sensorSearchTimer)
  sensorSearchTimer = window.setTimeout(() => {
    void loadAvailableSensors()
  }, 260)
}

async function loadAvailableSensors() {
  if (!selectedLab.value) return
  availableLoading.value = true
  try {
    const page = await searchAvailableLabSensors(selectedLab.value.id, sensorKeyword.value.trim())
    availableSensors.value = page.records || []
  } catch (error: any) {
    alert(error?.message || '搜索可绑定传感器失败')
  } finally {
    availableLoading.value = false
  }
}

async function bindSelectedSensor() {
  if (!selectedLab.value || !selectedAvailableSensor.value) return
  sensorSaving.value = true
  try {
    await bindLabSensor(selectedLab.value.id, selectedAvailableSensor.value.sensorId)
    showSensorModal.value = false
    await loadBindings()
  } catch (error: any) {
    alert(error?.message || '绑定传感器失败')
  } finally {
    sensorSaving.value = false
  }
}

function formatDataTypes(dataTypes: SensorDataTypeItem[]) {
  if (!dataTypes?.length) return '暂无属性指标'
  return dataTypes.map(item => `${item.dataName}${item.unit ? `(${item.unit})` : ''}`).join('、')
}
</script>

<style scoped>
.it-page { height:100%; overflow-y:auto; padding:20px 24px }
.page-header { margin-bottom:18px }
.page-header h2 { font-size:20px; font-weight:600; color:#1a1a2e; margin:0 }
.page-header p { margin:4px 0 0; font-size:13px; color:#999 }
.section-card { background:rgba(255,255,255,.5); backdrop-filter:blur(10px); border-radius:12px; border:1px solid rgba(0,0,0,.04); padding:18px 20px; margin-bottom:16px }
.section-header { display:flex; align-items:center; justify-content:space-between; gap:16px; margin-bottom:14px }
.section-header h3 { margin:0; font-size:15px; font-weight:600; color:#1a1a2e }
.section-actions { display:flex; align-items:center; gap:10px }
.lab-search { position:relative; width:320px }
.lab-search-input { width:100%; box-sizing:border-box; padding:9px 12px; border:1px solid rgba(0,0,0,.1); border-radius:8px; font-size:13px; background:rgba(255,255,255,.86); outline:none }
.lab-search-input:focus { border-color:#1a73e8 }
.lab-search-panel { position:absolute; top:calc(100% + 6px); right:0; width:100%; max-height:260px; overflow-y:auto; background:#fff; border:1px solid rgba(0,0,0,.08); border-radius:10px; box-shadow:0 12px 30px rgba(15,35,65,.14); z-index:20; padding:6px }
.lab-option { width:100%; border:none; background:#fff; border-radius:8px; padding:9px 10px; text-align:left; cursor:pointer; display:flex; flex-direction:column; gap:3px }
.lab-option:hover { background:rgba(26,115,232,.08) }
.lab-option-name { font-size:13px; font-weight:600; color:#1a1a2e }
.lab-option-type { font-size:12px; color:#777 }
.search-state { padding:16px 10px; text-align:center; font-size:13px; color:#999 }
.btn-clear { padding:8px 14px; border:1px solid rgba(0,0,0,.1); border-radius:8px; background:#fff; font-size:12px; color:#777; cursor:pointer }
.btn-clear:hover:not(:disabled) { border-color:#e74c3c; color:#e74c3c }
.btn-clear:disabled,.btn-add:disabled { opacity:.45; cursor:not-allowed }
.selected-lab { display:flex; align-items:center; gap:8px; margin-bottom:10px; font-size:13px; color:#555 }
.selected-lab strong { font-size:12px; color:#1a73e8; background:rgba(26,115,232,.09); border-radius:6px; padding:3px 8px }
.threshold-table { display:flex; flex-direction:column }
.th-row { display:flex; align-items:center; gap:12px; min-height:46px; padding:10px 8px; border-bottom:1px solid rgba(0,0,0,.03); box-sizing:border-box }
.th-head { border-bottom:1px solid rgba(0,0,0,.06); min-height:38px; padding:8px }
.th-head span { font-size:12px; color:#888; font-weight:600 }
.th-col-name { width:150px; flex-shrink:0; font-size:14px; font-weight:600; color:#1a1a2e }
.th-col-unit { width:90px; flex-shrink:0; font-size:13px; color:#666 }
.th-col-range { flex:1; min-width:180px; font-size:13px; color:#555; line-height:1.5 }
.th-col-act { width:140px; flex-shrink:0; display:flex; justify-content:flex-start }
.action-group { gap:8px }
.act-link { padding:5px 12px; border:1px solid rgba(0,0,0,.1); border-radius:8px; background:#fff; font-size:12px; color:#555; cursor:pointer; white-space:nowrap }
.act-link:hover { border-color:#1a73e8; color:#1a73e8 }
.danger-text:hover { border-color:#e74c3c; color:#e74c3c }
.add-indicator-block { width:100%; min-height:44px; margin-top:10px; border:1px dashed rgba(26,115,232,.45); border-radius:8px; background:rgba(26,115,232,.06); color:#1a73e8; font-size:13px; font-weight:600; cursor:pointer }
.add-indicator-block:hover { background:rgba(26,115,232,.1) }
.btn-add { padding:8px 16px; border:none; border-radius:10px; background:linear-gradient(135deg,#1a73e8,#4a90d9); color:#fff; font-size:13px; font-weight:600; cursor:pointer }
.btn-add:hover:not(:disabled) { box-shadow:0 4px 12px rgba(26,115,232,.3) }
.bind-table-wrap { overflow-x:auto }
.bind-table { width:100%; border-collapse:collapse; font-size:13px }
.bind-table thead { background:rgba(0,0,0,.02) }
.bind-table th { padding:10px 14px; text-align:left; font-weight:600; color:#888; font-size:12px; border-bottom:1px solid rgba(0,0,0,.06); white-space:nowrap }
.bind-table td { padding:9px 14px; border-bottom:1px solid rgba(0,0,0,.03); color:#333; white-space:nowrap }
.mono { font-family:'SF Mono','Menlo',monospace; font-size:12px; color:#666 }
.date-cell { color:#888; font-size:12px }
.bind-status { padding:2px 8px; border-radius:4px; font-size:11px; font-weight:600 }
.bs-bound { background:rgba(39,174,96,.1); color:#27ae60 }
.bs-unbound { background:rgba(0,0,0,.05); color:#888 }
.empty-state { padding:36px 0 }
.empty-state.small { padding:32px 0 }
.empty-state.inline { padding:20px 0 }
.empty-state p { margin:0; font-size:14px; color:#aaa; text-align:center }
.safe-range-list { display:flex; flex-direction:column; gap:8px }
.safe-range-row { display:grid; grid-template-columns:minmax(0,1fr) auto minmax(0,1fr) 64px; align-items:center; gap:8px }
.range-sep { font-size:13px; color:#999 }
.range-remove { height:38px; border:1px solid rgba(0,0,0,.1); border-radius:8px; background:#fff; color:#777; cursor:pointer }
.range-remove:hover:not(:disabled) { border-color:#e74c3c; color:#e74c3c }
.range-remove:disabled { opacity:.4; cursor:not-allowed }
.add-range-btn { align-self:flex-start; padding:8px 12px; border:1px dashed rgba(26,115,232,.45); border-radius:8px; background:rgba(26,115,232,.06); color:#1a73e8; cursor:pointer; font-size:13px }
.sensor-result-list { display:flex; flex-direction:column; gap:8px; max-height:360px; overflow-y:auto }
.sensor-option { border:1px solid rgba(0,0,0,.08); border-radius:10px; background:#fff; padding:11px 12px; text-align:left; cursor:pointer; display:flex; flex-direction:column; gap:7px }
.sensor-option:hover,.sensor-option.active { border-color:#1a73e8; background:rgba(26,115,232,.06) }
.sensor-main { display:flex; justify-content:space-between; gap:12px; align-items:center }
.sensor-main strong { font-size:13px; color:#1a1a2e }
.sensor-main em { font-style:normal; font-size:12px; color:#666 }
.sensor-data-types { font-size:12px; color:#888; line-height:1.5 }
@media (max-width: 720px) {
  .section-header { align-items:flex-start; flex-direction:column }
  .section-actions { width:100%; align-items:stretch }
  .lab-search { width:100% }
  .th-row { align-items:flex-start; flex-wrap:wrap }
  .th-col-name,.th-col-unit,.th-col-act { width:auto }
  .safe-range-row { grid-template-columns:1fr; align-items:stretch }
}
</style>

<style>
.modal-overlay { position:fixed; inset:0; background:rgba(0,0,0,.35); display:flex; align-items:center; justify-content:center; z-index:1100; padding:20px }
.modal-panel { background:#fff; border-radius:16px; width:100%; max-height:85vh; display:flex; flex-direction:column; box-shadow:0 16px 48px rgba(0,0,0,.15) }
.indicator-modal { max-width:560px }
.sensor-modal { max-width:640px }
.modal-header { display:flex; align-items:center; justify-content:space-between; padding:20px 24px 0 }
.modal-header h3 { margin:0; font-size:17px; font-weight:600; color:#1a1a2e }
.modal-close { background:none; border:none; cursor:pointer; color:#aaa; padding:4px; border-radius:6px }
.modal-close:hover { background:rgba(0,0,0,.05); color:#333 }
.modal-body { padding:20px 24px; overflow-y:auto; flex:1; display:flex; flex-direction:column; gap:14px }
.modal-footer { display:flex; justify-content:flex-end; gap:10px; padding:16px 24px 20px; border-top:1px solid rgba(0,0,0,.06) }
.modal-btn { padding:10px 24px; border-radius:10px; font-size:14px; cursor:pointer; border:none; font-weight:600; transition:.2s }
.modal-btn.cancel { background:rgba(0,0,0,.04); color:#666 }
.modal-btn.cancel:hover { background:rgba(0,0,0,.08) }
.modal-btn.primary { background:linear-gradient(135deg,#1a73e8,#4a90d9); color:#fff }
.modal-btn.primary:hover:not(:disabled) { box-shadow:0 4px 12px rgba(26,115,232,.3) }
.modal-btn:disabled { opacity:.4; cursor:not-allowed }
.form-g { display:flex; flex-direction:column; gap:6px }
.form-g label { font-size:13px; font-weight:600; color:#555 }
.required { color:#e74c3c }
.form-input { padding:10px 14px; border:1px solid rgba(0,0,0,.1); border-radius:10px; font-size:14px; color:#333; outline:none; background:rgba(255,255,255,.86); transition:.15s; font-family:inherit; width:100%; box-sizing:border-box }
.form-input:focus { border-color:#1a73e8 }
.modal-fade-enter-active { transition:all .2s ease }
.modal-fade-leave-active { transition:all .15s ease }
.modal-fade-enter-from,.modal-fade-leave-to { opacity:0 }
.modal-fade-enter-from .modal-panel { transform:scale(.94) translateY(12px) }
</style>
