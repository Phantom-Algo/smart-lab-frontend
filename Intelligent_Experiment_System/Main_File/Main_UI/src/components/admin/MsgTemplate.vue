<template>
  <div class="mt-page">
    <div class="page-header">
      <h2>消息模板</h2>
      <p>消息模板CRUD管理</p>
    </div>

    <div class="stats-row">
      <div class="stat-card"><div class="stat-num blue">{{ templates.length }}</div><div class="stat-label">总数</div></div>
      <div class="stat-card"><div class="stat-num green">{{ statusCount(1) }}</div><div class="stat-label">已启用</div></div>
      <div class="stat-card"><div class="stat-num" style="color:#999">{{ statusCount(0) }}</div><div class="stat-label">已停用</div></div>
    </div>

    <div class="filter-bar">
      <div class="filter-tabs">
        <div v-for="t in statusTabs" :key="t.key" class="filter-tab" :class="{ active: activeStatus === t.key }" @click="activeStatus = t.key; pageNum = 1">
          {{ t.label }}<span class="tab-count">{{ t.count }}</span>
        </div>
      </div>
      <select v-model="filterType" class="tool-select"><option value="">全部类型</option><option v-for="t in typeOptions" :key="t" :value="t">{{ t }}</option></select>
      <select v-model="filterSource" class="tool-select"><option value="">全部来源</option><option v-for="s in sourceOptions" :key="s" :value="s">{{ s }}</option></select>
      <button class="btn-add" @click="openCreate">+ 新建模板</button>
    </div>

    <div class="table-wrap" v-if="pagedList.length > 0">
      <table class="tmpl-table">
        <thead>
          <tr>
            <th class="col-source">来源</th>
            <th class="col-code">模板编码</th>
            <th class="col-name">模板名称</th>
            <th class="col-type">消息类型</th>
            <th class="col-fmt">格式</th>
            <th class="col-status">状态</th>
            <th class="col-pri">优先级</th>
            <th class="col-time">创建时间</th>
            <th class="col-act">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="t in pagedList" :key="t.id">
            <td><span class="source-tag">{{ t.sourceSystem }}</span></td>
            <td><code class="code-text">{{ t.templateCode }}</code></td>
            <td class="name-cell">{{ t.templateName }}</td>
            <td class="type-cell">{{ t.messageType }}</td>
            <td><span class="fmt-tag">{{ t.contentFormat }}</span></td>
            <td>
              <span class="status-badge" :class="t.status === 1 ? 'sb-on' : 'sb-off'">
                {{ t.status === 1 ? '启用' : '停用' }}
              </span>
            </td>
            <td><span class="pri-tag" :class="'pri-' + t.priority">{{ priLabel(t.priority) }}</span></td>
            <td class="date-cell">{{ t.createdAt }}</td>
            <td class="act-cell">
              <button class="act-link" @click="openEdit(t)">编辑</button>
              <button class="act-link" @click="toggleStatus(t)" :class="t.status === 1 ? 'warn' : ''">
                {{ t.status === 1 ? '停用' : '启用' }}
              </button>
              <button class="act-link del" @click="confirmDelete(t)">删除</button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <div v-else class="empty-state">
      <svg viewBox="0 0 24 24" width="48" height="48" fill="none" stroke="#ccc" stroke-width="1.2"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/></svg>
      <p>暂无消息模板</p>
    </div>

    <div class="pagination" v-if="filteredList.length > pageSize">
      <button :disabled="pageNum <= 1" @click="pageNum--">上一页</button>
      <span>{{ pageNum }} / {{ totalPages }}</span>
      <button :disabled="pageNum >= totalPages" @click="pageNum++">下一页</button>
    </div>

    <!-- 创建/编辑弹窗 -->
    <teleport to="body">
      <transition name="modal-fade">
        <div v-if="showForm" class="modal-overlay" @click.self="showForm = false">
          <div class="modal-panel modal-wide">
            <div class="modal-header">
              <h3>{{ isEditing ? '编辑模板' : '新建模板' }}</h3>
              <button class="modal-close" @click="showForm = false"><svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2"><path d="M18 6L6 18M6 6l12 12"/></svg></button>
            </div>
            <div class="modal-body">
              <div class="form-row">
                <div class="form-g" style="width:130px">
                  <label>来源子系统 <span class="required">*</span></label>
                  <select v-model="form.sourceSystem" class="form-input">
                    <option value="">请选择</option>
                    <option value="equipment">equipment</option>
                    <option value="lab">lab</option>
                    <option value="safety">safety</option>
                    <option value="duty">duty</option>
                    <option value="consumable">consumable</option>
                    <option value="system">system</option>
                  </select>
                </div>
                <div class="form-g flex-1">
                  <label>模板编码 <span class="required">*</span></label>
                  <input v-model="form.templateCode" class="form-input" placeholder="如 equip_loan_approved" :disabled="isEditing" />
                </div>
                <div class="form-g flex-1">
                  <label>模板名称 <span class="required">*</span></label>
                  <input v-model="form.templateName" class="form-input" placeholder="如 设备借用审批通知" />
                </div>
              </div>
              <div class="form-row">
                <div class="form-g flex-1">
                  <label>消息类型 <span class="required">*</span></label>
                  <input v-model="form.messageType" class="form-input" placeholder="如 equipment.normal.loan.approved" />
                </div>
                <div class="form-g" style="width:120px">
                  <label>内容格式</label>
                  <select v-model="form.contentFormat" class="form-input">
                    <option value="TEXT">TEXT</option>
                    <option value="MARKDOWN">MARKDOWN</option>
                    <option value="HTML">HTML</option>
                  </select>
                </div>
                <div class="form-g" style="width:120px">
                  <label>优先级</label>
                  <select v-model="form.priority" class="form-input">
                    <option :value="3">普通</option>
                    <option :value="2">系统</option>
                    <option :value="1">预警</option>
                  </select>
                </div>
              </div>
              <div class="form-g">
                <label>状态</label>
                <div class="radio-row">
                  <label class="radio-item" :class="{ active: form.status === 1 }"><input type="radio" v-model.number="form.status" :value="1" /><span>启用</span></label>
                  <label class="radio-item" :class="{ active: form.status === 0 }"><input type="radio" v-model.number="form.status" :value="0" /><span>停用</span></label>
                </div>
              </div>
              <div class="form-g">
                <label>标题模板 <span class="required">*</span></label>
                <textarea v-model="form.titleTemplate" class="form-textarea" rows="2" placeholder="如 您的借用申请已{status}"></textarea>
              </div>
              <div class="form-g">
                <label>正文模板 <span class="required">*</span></label>
                <textarea v-model="form.contentTemplate" class="form-textarea code-area" rows="5" placeholder="如 您好{user}：&#10;您对{device}的借用申请已{status}。&#10;请及时前往实验室领取。"></textarea>
              </div>
              <div class="param-hint">
                <span class="hint-label">可用占位参数：</span>
                <span class="hint-tag" v-for="p in currentParams" :key="p" @click="insertParam(p)">{{ '{' + p + '}' }}</span>
                <span class="hint-desc">点击参数名插入到正文模板末尾</span>
              </div>
            </div>
            <div class="modal-footer">
              <button class="modal-btn cancel" @click="showForm = false">取消</button>
              <button class="modal-btn primary" @click="doSave" :disabled="!form.templateCode || !form.templateName || !form.titleTemplate || !form.contentTemplate">
                {{ isEditing ? '保存修改' : '确认创建' }}
              </button>
            </div>
          </div>
        </div>
      </transition>
    </teleport>

    <!-- 删除确认 -->
    <teleport to="body">
      <transition name="modal-fade">
        <div v-if="showDelete" class="modal-overlay" @click.self="showDelete = false">
          <div class="modal-panel modal-small">
            <div class="modal-header"><h3>确认删除</h3><button class="modal-close" @click="showDelete = false"><svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2"><path d="M18 6L6 18M6 6l12 12"/></svg></button></div>
            <div class="modal-body">
              <p class="delete-warn">确定要删除模板 <strong>{{ deleteTarget?.templateCode }}</strong> 吗？已使用该模板生成的历史消息不受影响。</p>
            </div>
            <div class="modal-footer">
              <button class="modal-btn cancel" @click="showDelete = false">取消</button>
              <button class="modal-btn danger" @click="doDelete">确认删除</button>
            </div>
          </div>
        </div>
      </transition>
    </teleport>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { getTemplatePage, createTemplate, updateTemplate, deleteTemplate, type MessageTemplateItem } from '../../utils/messageApi'
import { isMockMode } from '../../stores/authStore'

interface MsgTemplate {
  id: number; sourceSystem: string; templateCode: string; templateName: string; messageType: string
  titleTemplate: string; contentTemplate: string; contentFormat: 'TEXT' | 'MARKDOWN' | 'HTML'; status: number
  priority: number; createdAt: string
}

const templates = ref<MsgTemplate[]>([])
const apiLoading = ref(false)

// Fallback mock data when backend is unavailable
const mockTemplates: MsgTemplate[] = [
  { id:1, sourceSystem:'equipment', templateCode:'equip_loan_approved', templateName:'设备借用审批通过', messageType:'equipment.normal.loan.approved', titleTemplate:'您的借用申请已通过', contentTemplate:'您好{user}：\n您对{device}的借用申请已审批通过。\n请于{lend_date}前至{lab}领取设备。', contentFormat:'TEXT', status:1, priority:3, createdAt:'2026-06-10' },
  { id:2, sourceSystem:'equipment', templateCode:'equip_loan_rejected', templateName:'设备借用审批拒绝', messageType:'equipment.normal.loan.rejected', titleTemplate:'您的借用申请未通过', contentTemplate:'您好{user}：\n您对{device}的借用申请未通过审批。\n原因：{reason}', contentFormat:'TEXT', status:1, priority:3, createdAt:'2026-06-12' },
  { id:3, sourceSystem:'lab', templateCode:'lab_reserve_approved', templateName:'实验室预约审批通过', messageType:'lab.system.reserve.approved', titleTemplate:'您的实验室预约已通过', contentTemplate:'您好{user}：\n您预约的{lab}已审批通过。\n预约时间：{reserve_date} {time_slot}', contentFormat:'TEXT', status:1, priority:2, createdAt:'2026-06-18' },
  { id:4, sourceSystem:'safety', templateCode:'safety_alert', templateName:'安全告警通知', messageType:'safety.warning.alert', titleTemplate:'【安全告警】{lab} {alarm_type}', contentTemplate:'实验室：{lab}\n告警对象：{alarm_object}\n告警内容：{alarm_content}\n告警值：{alarm_value}（阈值：{threshold}）\n请及时处理。', contentFormat:'TEXT', status:1, priority:1, createdAt:'2026-07-01' },
  { id:5, sourceSystem:'duty', templateCode:'duty_remind', templateName:'值日提醒通知', messageType:'duty.normal.remind', titleTemplate:'值日提醒：{date} {lab}', contentTemplate:'{user}同学：\n提醒您于{date} {time_slot}在{lab}进行值日。\n请按时到达并完成签到。', contentFormat:'TEXT', status:0, priority:3, createdAt:'2026-07-05' },
]

async function loadData() {
  apiLoading.value = true
  try {
    const res = await getTemplatePage({ pageNum: 1, pageSize: 200 })
    templates.value = res.records.map(t => ({
      id: t.id, sourceSystem: t.messageType.split('.')[0] || 'system',
      templateCode: t.templateCode, templateName: t.templateName,
      messageType: t.messageType, titleTemplate: t.titleTemplate,
      contentTemplate: t.contentTemplate, contentFormat: t.contentFormat,
      status: t.status, priority: 3, createdAt: t.createdAt?.slice(0, 10) || ''
    }))
  } catch {
    /* API 不可用，请检查后端服务 */
  } finally { apiLoading.value = false }
}

const activeStatus = ref<number | 'all'>('all')
const filterType = ref('')
const filterSource = ref('')
const pageNum = ref(1)
const pageSize = 8

const typeOptions = computed(() => [...new Set(templates.value.map(t => t.messageType))])
const sourceOptions = computed(() => [...new Set(templates.value.map(t => t.sourceSystem))])

const statusTabs = computed(() => [
  { key:'all' as const, label:'全部', count:templates.value.length },
  { key:1 as const, label:'已启用', count:templates.value.filter(t => t.status === 1).length },
  { key:0 as const, label:'已停用', count:templates.value.filter(t => t.status === 0).length },
])

const filteredList = computed(() => {
  let list = templates.value
  if (activeStatus.value !== 'all') list = list.filter(t => t.status === activeStatus.value)
  if (filterType.value) list = list.filter(t => t.messageType === filterType.value)
  if (filterSource.value) list = list.filter(t => t.sourceSystem === filterSource.value)
  return list
})
const totalPages = computed(() => Math.ceil(filteredList.value.length / pageSize) || 1)
const pagedList = computed(() => filteredList.value.slice((pageNum.value - 1) * pageSize, pageNum.value * pageSize))

function statusCount(s: number) { return templates.value.filter(t => t.status === s).length }
function priLabel(p: number) { return p === 1 ? '预警' : p === 2 ? '系统' : '普通' }

// Create/Edit
const showForm = ref(false)
const isEditing = ref(false)
const editId = ref(0)
const saving = ref(false)
const form = reactive({
  sourceSystem:'', templateCode:'', templateName:'', messageType:'',
  titleTemplate:'', contentTemplate:'', contentFormat:'TEXT' as 'TEXT' | 'MARKDOWN' | 'HTML', status:1, priority:3
})

const currentParams = computed(() => {
  const combined = form.titleTemplate + form.contentTemplate
  const matches = combined.match(/\{(\w+)\}/g)
  if (!matches) return []
  return [...new Set(matches.map(m => m.slice(1, -1)))]
})

function insertParam(param: string) { form.contentTemplate += '{' + param + '}' }

function openCreate() {
  isEditing.value = false; editId.value = 0
  form.sourceSystem = ''; form.templateCode = ''; form.templateName = ''; form.messageType = ''
  form.titleTemplate = ''; form.contentTemplate = ''; form.contentFormat = 'TEXT'; form.status = 1; form.priority = 3
  showForm.value = true
}
function openEdit(t: MsgTemplate) {
  isEditing.value = true; editId.value = t.id
  form.sourceSystem = t.sourceSystem; form.templateCode = t.templateCode; form.templateName = t.templateName; form.messageType = t.messageType
  form.titleTemplate = t.titleTemplate; form.contentTemplate = t.contentTemplate
  form.contentFormat = t.contentFormat; form.status = t.status; form.priority = t.priority
  showForm.value = true
}

async function doSave() {
  if (!form.templateCode || !form.templateName || !form.titleTemplate || !form.contentTemplate) return
  saving.value = true
  try {
    if (isMockMode.value) {
      if (isEditing.value) {
        const t = templates.value.find(t => t.id === editId.value)
        if (t) Object.assign(t, form)
      } else {
        const newId = Math.max(...templates.value.map(t => t.id), 0) + 1
        templates.value.push({ id: newId, ...form, createdAt: new Date().toISOString().slice(0, 10) })
      }
    } else {
      if (isEditing.value) await updateTemplate(editId.value, form)
      else await createTemplate(form)
      await loadData()
    }
    showForm.value = false
  } catch (e: any) { alert(e.message || '保存失败') }
  finally { saving.value = false }
}

async function toggleStatus(t: MsgTemplate) {
  if (isMockMode.value) { t.status = t.status === 1 ? 0 : 1; return }
  try {
    await updateTemplate(t.id, { status: t.status === 1 ? 0 : 1 })
    await loadData()
  } catch (e: any) { alert(e.message || '操作失败') }
}

const showDelete = ref(false)
const deleteTarget = ref<MsgTemplate | null>(null)
function confirmDelete(t: MsgTemplate) { deleteTarget.value = t; showDelete.value = true }

async function doDelete() {
  if (!deleteTarget.value) return
  if (isMockMode.value) {
    const idx = templates.value.findIndex(t => t.id === deleteTarget.value!.id)
    if (idx !== -1) templates.value.splice(idx, 1)
  } else {
    try { await deleteTemplate(deleteTarget.value.id); await loadData() }
    catch (e: any) { alert(e.message || '删除失败') }
  }
  showDelete.value = false; deleteTarget.value = null
}

onMounted(() => loadData())
</script>

<style scoped>
.mt-page { height:100%; overflow-y:auto; padding:20px 24px }

.page-header { margin-bottom:16px }
.page-header h2 { font-size:20px; font-weight:600; color:#1a1a2e; margin:0 }
.page-header p { margin:4px 0 0; font-size:13px; color:#999 }

.stats-row { display:flex; gap:12px; margin-bottom:14px; flex-wrap:wrap }
.stat-card { background:rgba(255,255,255,.65); backdrop-filter:blur(10px); border:1px solid rgba(0,0,0,.06); border-radius:12px; padding:12px 18px; min-width:80px; text-align:center }
.stat-num { font-size:22px; font-weight:700; color:#1a1a2e; line-height:1.1 }
.stat-label { font-size:11px; color:#999; margin-top:2px }
.blue { color:#1a73e8 } .green { color:#27ae60 }

.filter-bar { display:flex; align-items:center; gap:10px; margin-bottom:16px; flex-wrap:wrap }
.filter-tabs { display:flex; gap:4px; background:rgba(255,255,255,.4); border-radius:10px; padding:4px }
.filter-tab { padding:7px 14px; border-radius:8px; font-size:13px; color:#666; cursor:pointer; transition:all .15s; display:flex; align-items:center; gap:6px; user-select:none }
.filter-tab:hover { color:#333; background:rgba(0,0,0,.03) }
.filter-tab.active { background:#fff; color:#1a73e8; font-weight:600; box-shadow:0 1px 3px rgba(0,0,0,.08) }
.tab-count { font-size:11px; background:rgba(0,0,0,.06); padding:1px 7px; border-radius:10px; color:#888 }
.filter-tab.active .tab-count { background:rgba(26,115,232,.1); color:#1a73e8 }
.tool-select { padding:7px 10px; border:1px solid rgba(0,0,0,.1); border-radius:8px; font-size:13px; background:rgba(255,255,255,.7); outline:none; cursor:pointer }
.btn-add { padding:9px 18px; border:none; border-radius:10px; background:linear-gradient(135deg,#1a73e8,#4a90d9); color:#fff; font-size:13px; font-weight:500; cursor:pointer; margin-left:auto }
.btn-add:hover { box-shadow:0 4px 12px rgba(26,115,232,.3) }

.table-wrap { background:rgba(255,255,255,.5); backdrop-filter:blur(10px); border-radius:12px; border:1px solid rgba(0,0,0,.04); overflow:hidden }
.tmpl-table { width:100%; border-collapse:collapse; font-size:13px }
.tmpl-table thead { background:rgba(0,0,0,.02) }
.tmpl-table th { padding:11px 14px; text-align:left; font-weight:600; color:#888; font-size:12px; border-bottom:1px solid rgba(0,0,0,.06); white-space:nowrap }
.tmpl-table td { padding:10px 14px; border-bottom:1px solid rgba(0,0,0,.03); color:#333; vertical-align:middle }
.col-source { width:90px } .col-code { width:130px } .col-name { width:110px } .col-type { width:140px } .col-fmt { width:55px } .col-status { width:55px } .col-pri { width:55px } .col-time { width:80px } .col-act { width:130px }
.pri-tag { padding:1px 6px; border-radius:3px; font-size:11px; font-weight:600; color:#fff }
.pri-1 { background:#e74c3c } .pri-2 { background:#f39c12 } .pri-3 { background:#1a73e8 }
.date-cell { font-size:12px; color:#888 }
.source-tag { padding:1px 8px; background:rgba(142,68,173,.08); color:#8e44ad; border-radius:4px; font-size:11px; font-weight:500 }
.code-text { font-family:'SF Mono','Menlo',monospace; font-size:11px; background:rgba(0,0,0,.03); padding:2px 6px; border-radius:3px; color:#555 }
.name-cell { font-weight:500; color:#1a1a2e }
.type-cell { font-size:11px; color:#888; font-family:'SF Mono','Menlo',monospace }
.fmt-tag { padding:1px 6px; background:rgba(0,0,0,.04); border-radius:3px; font-size:11px; color:#666 }
.status-badge { padding:2px 8px; border-radius:4px; font-size:11px; font-weight:500 }
.sb-on { background:rgba(39,174,96,.1); color:#27ae60 }
.sb-off { background:rgba(0,0,0,.05); color:#999 }
.act-cell { display:flex; gap:4px }
.act-link { padding:4px 10px; border:1px solid rgba(0,0,0,.1); border-radius:6px; background:#fff; font-size:11px; color:#555; cursor:pointer }
.act-link:hover { border-color:#1a73e8; color:#1a73e8 }
.act-link.warn:hover { border-color:#f39c12; color:#e67e22 }
.act-link.del:hover { border-color:#e74c3c; color:#e74c3c }

.empty-state { display:flex; flex-direction:column; align-items:center; padding:60px 0; color:#ccc; gap:8px }
.empty-state p { margin:0; font-size:15px; color:#bbb }

.pagination { display:flex; align-items:center; justify-content:center; gap:10px; margin-top:16px }
.pagination button { padding:7px 16px; border:1px solid rgba(0,0,0,.1); border-radius:8px; background:#fff; font-size:13px; cursor:pointer; color:#555 }
.pagination button:disabled { opacity:.4; cursor:not-allowed }

.param-hint { display:flex; align-items:center; gap:6px; flex-wrap:wrap; background:rgba(26,115,232,.04); border:1px solid rgba(26,115,232,.1); border-radius:8px; padding:8px 12px }
.hint-label { font-size:12px; color:#888 }
.hint-tag { padding:2px 8px; background:rgba(26,115,232,.1); color:#1a73e8; border-radius:4px; font-size:11px; font-family:'SF Mono','Menlo',monospace; cursor:pointer }
.hint-tag:hover { background:rgba(26,115,232,.2) }
.hint-desc { font-size:11px; color:#aaa; margin-left:auto }

.form-row { display:flex; gap:10px }
.flex-1 { flex:1 }
.delete-warn { font-size:14px; color:#555; line-height:1.6; margin:0 }
</style>

<style>
.modal-overlay { position:fixed; inset:0; background:rgba(0,0,0,.35); display:flex; align-items:center; justify-content:center; z-index:1100; padding:20px }
.modal-panel { background:#fff; border-radius:16px; width:100%; max-width:480px; max-height:85vh; display:flex; flex-direction:column; box-shadow:0 16px 48px rgba(0,0,0,.15) }
.modal-wide { max-width:560px }
.modal-small { max-width:400px }
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
.modal-btn.danger { background:linear-gradient(135deg,#e74c3c,#c0392b); color:#fff }
.modal-btn.danger:hover { box-shadow:0 4px 12px rgba(231,76,60,.3) }
.form-g { display:flex; flex-direction:column; gap:4px }
.form-g label { font-size:13px; font-weight:500; color:#555 }
.required { color:#e74c3c }
.form-input { padding:10px 14px; border:1px solid rgba(0,0,0,.1); border-radius:10px; font-size:14px; color:#333; outline:none; background:rgba(255,255,255,.7); transition:.15s; font-family:inherit; width:100%; box-sizing:border-box }
.form-input:focus { border-color:#1a73e8 }
.form-textarea { padding:10px 14px; border:1px solid rgba(0,0,0,.1); border-radius:10px; font-size:14px; color:#333; outline:none; background:rgba(255,255,255,.7); transition:.15s; font-family:inherit; resize:vertical; width:100%; box-sizing:border-box }
.form-textarea:focus { border-color:#1a73e8 }
.code-area { font-family:'SF Mono','Menlo',monospace; font-size:13px; line-height:1.6 }
.radio-row { display:flex; gap:8px }
.radio-item { flex:1; padding:9px 12px; border:1px solid rgba(0,0,0,.1); border-radius:10px; font-size:13px; cursor:pointer; display:flex; align-items:center; gap:6px; color:#666; transition:.15s; justify-content:center }
.radio-item input { display:none }
.radio-item.active { border-color:#1a73e8; background:rgba(26,115,232,.04); color:#1a73e8; font-weight:500 }
.modal-fade-enter-active { transition:all .2s ease }
.modal-fade-leave-active { transition:all .15s ease }
.modal-fade-enter-from,.modal-fade-leave-to { opacity:0 }
.modal-fade-enter-from .modal-panel { transform:scale(.94) translateY(12px) }
</style>
