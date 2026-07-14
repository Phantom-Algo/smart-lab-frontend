<template>
  <div class="mp-page">
    <div class="page-header">
      <h2>消息发布</h2>
      <p>向指定用户/群组发送消息</p>
    </div>

    <div class="two-col">
      <!-- 左栏：输入区 -->
      <div class="col-left">
        <div class="form-row-2">
          <div class="form-g flex-1">
            <label>选择模板 <span class="required">*</span></label>
            <select v-model="selectedTemplateId" class="form-input" @change="onTemplateChange">
              <option :value="0" disabled>请选择消息模板</option>
              <option v-for="t in availableTemplates" :key="t.id" :value="t.id">{{ t.templateName }}</option>
            </select>
          </div>
          <div class="form-g" style="width:140px">
            <label>优先级</label>
            <select v-model="priority" class="form-input">
              <option :value="3">普通通知</option>
              <option :value="2">系统通知</option>
              <option :value="1">预警通知</option>
            </select>
          </div>
        </div>

        <div v-if="selectedTemplate" class="section-card">
          <div class="sc-title">模板内容</div>
          <div class="tmpl-box">
            <div class="tmpl-title">{{ selectedTemplate.titleTemplate }}</div>
            <div class="tmpl-body">{{ selectedTemplate.contentTemplate }}</div>
          </div>
        </div>

        <div v-if="paramList.length > 0" class="section-card">
          <div class="sc-title">填写参数</div>
          <div class="param-grid">
            <div v-for="p in paramList" :key="p" class="form-g">
              <label class="param-label">{{ '{' + p + '}' }}</label>
              <input v-model="paramValues[p]" class="form-input" :placeholder="p" />
            </div>
          </div>
        </div>

        <div class="section-card">
          <div class="sc-title">发送目标 <span class="required">*</span></div>
          <div class="target-radios">
            <label v-for="t in targetTypes" :key="t.value" class="radio-card" :class="{ active: targetType === t.value }">
              <input type="radio" v-model="targetType" :value="t.value" />
              <span>{{ t.label }}</span>
            </label>
          </div>
          <div v-if="targetType === 'single' || targetType === 'multi'" class="user-picker">
            <div class="picker-row">
              <select v-model="selectedUser" class="form-input" style="flex:1">
                <option value="">选择用户</option>
                <option v-for="u in userOptions" :key="u" :value="u">{{ u }}</option>
              </select>
              <button class="btn-add-user" @click="addUser" :disabled="!selectedUser">添加</button>
            </div>
            <div class="selected-users" v-if="selectedUsers.length > 0">
              <span v-for="(u, i) in selectedUsers" :key="u" class="user-tag">
                {{ u }}
                <button class="remove-user" @click="selectedUsers.splice(i, 1)">&times;</button>
              </span>
            </div>
            <div v-else class="no-user-hint">尚未选择用户</div>
          </div>
        </div>

        <div class="section-card">
          <div class="sc-title">跳转链接</div>
          <input v-model="actionUrl" class="form-input" placeholder="可选，用户点击消息后的跳转路径" />
        </div>

        <div class="form-actions">
          <button class="btn-cancel" @click="resetForm">重置</button>
          <button class="btn-send" @click="doSend" :disabled="!canSend">发送消息</button>
        </div>
      </div>

      <!-- 右栏：实时预览 -->
      <div class="col-right">
        <div class="preview-panel">
          <div class="preview-head">
            <span class="preview-dot"></span>实时预览
          </div>

          <div v-if="!selectedTemplate" class="preview-empty">
            <svg viewBox="0 0 24 24" width="40" height="40" fill="none" stroke="#ddd" stroke-width="1.2"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/><line x1="16" y1="13" x2="8" y2="13"/><line x1="16" y1="17" x2="8" y2="17"/></svg>
            <p>请选择模板并填写参数</p>
          </div>

          <div v-else class="preview-body">
            <div class="msg-card">
              <div class="msg-card-head">
                <span class="msg-pri-tag" :class="'pri-' + priority">
                  {{ priority === 1 ? '预警' : priority === 2 ? '系统' : '通知' }}
                </span>
                <span class="msg-time">刚刚</span>
              </div>
              <div class="msg-card-title">{{ renderedTitle }}</div>
              <div class="msg-card-content">{{ renderedContent }}</div>
              <div v-if="actionUrl" class="msg-card-link">
                <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2"><path d="M10 13a5 5 0 0 0 7.54.54l3-3a5 5 0 0 0-7.07-7.07l-1.72 1.71"/><path d="M14 11a5 5 0 0 0-7.54-.54l-3 3a5 5 0 0 0 7.07 7.07l1.71-1.71"/></svg>
                {{ actionUrl }}
              </div>
            </div>

            <div class="send-summary">
              <div class="sum-title">发送摘要</div>
              <div class="sum-row"><span class="sl">模板</span><span class="sv">{{ selectedTemplate.templateName }}</span></div>
              <div class="sum-row"><span class="sl">优先级</span><span class="sv">{{ priLabel }}</span></div>
              <div class="sum-row"><span class="sl">目标</span><span class="sv">{{ targetDesc }}</span></div>
              <div class="sum-row" v-if="actionUrl"><span class="sl">链接</span><span class="sv mono">{{ actionUrl }}</span></div>
              <div class="sum-div"></div>
              <div class="sum-row">
                <span class="sl">状态</span>
                <span v-if="canSend" class="sv ready">就绪，可以发送</span>
                <span v-else class="sv not-ready">{{ missingHint }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 发送成功 -->
    <teleport to="body">
      <transition name="modal-fade">
        <div v-if="showSuccess" class="modal-overlay" @click.self="showSuccess = false">
          <div class="modal-panel modal-small">
            <div class="modal-header"><h3>发送成功</h3><button class="modal-close" @click="showSuccess = false"><svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2"><path d="M18 6L6 18M6 6l12 12"/></svg></button></div>
            <div class="modal-body" style="text-align:center">
              <svg viewBox="0 0 24 24" width="48" height="48" fill="none" stroke="#27ae60" stroke-width="1.5" style="margin:0 auto"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/><polyline points="22 4 12 14.01 9 11.01"/></svg>
              <p style="margin:12px 0 4px;font-size:15px;color:#333">消息已成功发送</p>
              <p style="margin:0;font-size:13px;color:#999">已推送到 {{ targetDesc }} 的收件箱</p>
            </div>
            <div class="modal-footer" style="justify-content:center">
              <button class="modal-btn primary" @click="showSuccess = false; resetForm()">确定</button>
            </div>
          </div>
        </div>
      </transition>
    </teleport>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { publishMessage } from '../../utils/messageApi'
import { getTemplatePage, type MessageTemplateItem } from '../../utils/messageApi'
import { isMockMode } from '../../stores/authStore'

interface Template {
  id: number; templateCode: string; templateName: string
  titleTemplate: string; contentTemplate: string; messageType: string
}

const availableTemplates = ref<Template[]>([])
const sending = ref(false)
const sendError = ref('')

const mockTemplates: Template[] = [
  { id:1, templateCode:'equip_loan_approved', templateName:'设备借用审批通过', titleTemplate:'您的借用申请已通过', contentTemplate:'您好{user}：\n您对{device}的借用申请已审批通过。', messageType:'equipment.loan.approved' },
  { id:2, templateCode:'lab_reserve_approved', templateName:'实验室预约审批通过', titleTemplate:'您的实验室预约已通过', contentTemplate:'您好{user}：\n您预约的{lab}已审批通过。\n预约时间：{reserve_date} {time_slot}', messageType:'lab.reserve.approved' },
  { id:3, templateCode:'safety_alert', templateName:'安全告警通知', titleTemplate:'【安全告警】{lab} {alarm_type}', contentTemplate:'实验室：{lab}\n告警对象：{alarm_object}\n告警值：{alarm_value}（阈值：{threshold}）\n请及时处理。', messageType:'safety.warning.alert' },
]

async function loadTemplates() {
  if (isMockMode.value) { availableTemplates.value = [...mockTemplates]; return }
  try {
    const res = await getTemplatePage({ pageNum: 1, pageSize: 100 })
    availableTemplates.value = res.records.map(t => ({
      id: t.id, templateCode: t.templateCode, templateName: t.templateName,
      titleTemplate: t.titleTemplate, contentTemplate: t.contentTemplate,
      messageType: t.messageType
    }))
  } catch { /* API 不可用，请检查后端服务 */ }
}

const selectedTemplateId = ref(0)
const priority = ref(3)
const targetType = ref('single')
const selectedUser = ref('')
const selectedUsers = ref<string[]>([])
const userOptions = ['张同学', '赵同学', '李同学', '王同学', '刘同学', '陈同学', '孙老师', '周老师']
const actionUrl = ref('')
const paramValues = reactive<Record<string, string>>({})
const showSuccess = ref(false)

const targetTypes = [
  { value:'single', label:'指定用户' },
  { value:'all_student', label:'全部学生' },
  { value:'all_teacher', label:'全部教师' },
  { value:'all_manager', label:'全部管理员' },
  { value:'all', label:'全体用户' },
]

const priLabel = computed(() => priority.value === 1 ? '预警通知' : priority.value === 2 ? '系统通知' : '普通通知')
const selectedTemplate = computed(() => availableTemplates.value.find(t => t.id === selectedTemplateId.value))

const paramList = computed(() => {
  if (!selectedTemplate.value) return []
  const combined = selectedTemplate.value.titleTemplate + selectedTemplate.value.contentTemplate
  const matches = combined.match(/\{(\w+)\}/g)
  if (!matches) return []
  return [...new Set(matches.map(m => m.slice(1, -1)))]
})

const renderedTitle = computed(() => selectedTemplate.value ? replaceParams(selectedTemplate.value.titleTemplate) : '')
const renderedContent = computed(() => selectedTemplate.value ? replaceParams(selectedTemplate.value.contentTemplate) : '')

function replaceParams(t: string): string {
  let r = t
  for (const p of paramList.value) r = r.replace(new RegExp('\\{' + p + '\\}', 'g'), paramValues[p] || '{' + p + '}')
  return r
}

const isTargetValid = computed(() => {
  if (targetType.value === 'single' || targetType.value === 'multi') return selectedUsers.value.length > 0
  return true
})
const allParamsFilled = computed(() => paramList.value.length > 0 && paramList.value.every(p => paramValues[p]?.trim()))
const canSend = computed(() => selectedTemplateId.value > 0 && allParamsFilled.value && isTargetValid.value)

const targetDesc = computed(() => {
  const m: Record<string,string> = { all:'全体用户', all_student:'全部学生', all_teacher:'全部教师', all_manager:'全部管理员' }
  return m[targetType.value] || selectedUsers.value.join('、')
})

const missingHint = computed(() => {
  if (!selectedTemplate.value) return '请选择模板'
  if (!allParamsFilled.value) return '请填写所有参数'
  if (!isTargetValid.value) return '请选择发送目标'
  return ''
})

function onTemplateChange() { paramList.value.forEach(p => { if (!paramValues[p]) paramValues[p] = '' }) }
function addUser() { if (selectedUser.value && !selectedUsers.value.includes(selectedUser.value)) { selectedUsers.value.push(selectedUser.value); selectedUser.value = '' } }
function resetForm() { selectedTemplateId.value = 0; priority.value = 3; targetType.value = 'single'; selectedUser.value = ''; selectedUsers.value = []; actionUrl.value = ''; Object.keys(paramValues).forEach(k => delete paramValues[k]); sendError.value = '' }

async function doSend() {
  if (!canSend.value) return
  if (!selectedTemplate.value) return
  sendError.value = ''
  sending.value = true
  try {
    if (isMockMode.value) {
      showSuccess.value = true
      return
    }
    const targetUserIds = (targetType.value === 'single' || targetType.value === 'multi')
      ? selectedUsers.value.map((_, i) => i + 1) // map to numeric IDs
      : undefined
    await publishMessage({
      messageType: selectedTemplate.value.messageType,
      targetType: targetType.value,
      targetUserIds,
      templateCode: selectedTemplate.value.templateCode,
      title: renderedTitle.value,
      content: renderedContent.value,
      actionUrl: actionUrl.value || undefined,
      params: paramValues
    })
    showSuccess.value = true
  } catch (e: any) { sendError.value = e.message || '发送失败' }
  finally { sending.value = false }
}

onMounted(() => loadTemplates())
</script>

<style scoped>
.mp-page { height:100%; overflow-y:auto; padding:20px 24px }
.page-header { margin-bottom:18px }
.page-header h2 { font-size:20px; font-weight:600; color:#1a1a2e; margin:0 }
.page-header p { margin:4px 0 0; font-size:13px; color:#999 }

.two-col { display:flex; gap:20px; align-items:flex-start }
.col-left { flex:1; min-width:0; display:flex; flex-direction:column; gap:14px }
.col-right { width:340px; flex-shrink:0 }

.form-row-2 { display:flex; gap:12px; align-items:flex-end; background:rgba(255,255,255,.5); backdrop-filter:blur(10px); border-radius:12px; border:1px solid rgba(0,0,0,.04); padding:16px }
.flex-1 { flex:1 }
.form-g { display:flex; flex-direction:column; gap:4px }
.form-g label { font-size:13px; font-weight:500; color:#555 }
.required { color:#e74c3c }
.form-input { padding:10px 14px; border:1px solid rgba(0,0,0,.1); border-radius:10px; font-size:14px; color:#333; outline:none; background:rgba(255,255,255,.7); transition:.15s; font-family:inherit; width:100%; box-sizing:border-box }
.form-input:focus { border-color:#1a73e8 }

.section-card { background:rgba(255,255,255,.5); backdrop-filter:blur(10px); border-radius:12px; border:1px solid rgba(0,0,0,.04); padding:16px }
.sc-title { font-size:14px; font-weight:600; color:#1a1a2e; margin-bottom:10px; padding-bottom:6px; border-bottom:1px solid rgba(0,0,0,.05) }

.tmpl-box { background:rgba(0,0,0,.02); border:1px solid rgba(0,0,0,.05); border-radius:10px; padding:14px }
.tmpl-title { font-size:14px; font-weight:600; color:#1a1a2e; margin-bottom:8px }
.tmpl-body { font-size:13px; color:#555; white-space:pre-wrap; line-height:1.7 }

.param-grid { display:grid; grid-template-columns:1fr 1fr; gap:10px }
.param-label { font-family:'SF Mono','Menlo',monospace; font-size:12px }

.target-radios { display:flex; gap:6px; flex-wrap:wrap }
.radio-card { padding:7px 14px; border:1px solid rgba(0,0,0,.1); border-radius:8px; font-size:13px; cursor:pointer; color:#666; transition:.15s; display:flex; align-items:center; gap:4px }
.radio-card input { display:none }
.radio-card.active { border-color:#1a73e8; background:rgba(26,115,232,.04); color:#1a73e8; font-weight:500 }

.user-picker { background:rgba(0,0,0,.01); border-radius:10px; padding:12px; margin-top:8px }
.picker-row { display:flex; gap:8px }
.btn-add-user { padding:10px 16px; border:1px solid rgba(26,115,232,.3); border-radius:10px; background:rgba(26,115,232,.04); color:#1a73e8; font-size:13px; cursor:pointer; white-space:nowrap }
.btn-add-user:hover:not(:disabled) { background:rgba(26,115,232,.1) }
.btn-add-user:disabled { opacity:.4; cursor:not-allowed }
.selected-users { display:flex; gap:6px; flex-wrap:wrap; margin-top:10px }
.user-tag { display:flex; align-items:center; gap:2px; padding:4px 10px; background:rgba(26,115,232,.08); border:1px solid rgba(26,115,232,.15); border-radius:6px; font-size:12px; color:#1a73e8 }
.remove-user { background:none; border:none; cursor:pointer; color:#1a73e8; font-size:16px; line-height:1; padding:0 2px; opacity:.6 }
.remove-user:hover { opacity:1 }
.no-user-hint { font-size:12px; color:#ccc; padding:6px 0; text-align:center }

.form-actions { display:flex; justify-content:flex-end; gap:12px; padding-top:4px }
.btn-cancel { padding:10px 22px; border:1px solid rgba(0,0,0,.1); border-radius:10px; background:#fff; font-size:14px; color:#666; cursor:pointer }
.btn-cancel:hover { border-color:#aaa }
.btn-send { padding:10px 28px; border:none; border-radius:10px; background:linear-gradient(135deg,#1a73e8,#4a90d9); color:#fff; font-size:14px; font-weight:500; cursor:pointer }
.btn-send:hover:not(:disabled) { box-shadow:0 4px 14px rgba(26,115,232,.35) }
.btn-send:disabled { opacity:.4; cursor:not-allowed }

/* 右栏预览 */
.preview-panel { position:sticky; top:20px; background:rgba(255,255,255,.55); backdrop-filter:blur(10px); border-radius:14px; border:1px solid rgba(0,0,0,.04); overflow:hidden }
.preview-head { display:flex; align-items:center; gap:8px; padding:14px 18px; font-size:14px; font-weight:600; color:#1a1a2e; border-bottom:1px solid rgba(0,0,0,.05) }
.preview-dot { width:8px; height:8px; border-radius:50%; background:#27ae60 }
.preview-empty { display:flex; flex-direction:column; align-items:center; padding:50px 20px; gap:10px }
.preview-empty p { margin:0; font-size:13px; color:#ccc }
.preview-body { padding:16px }

.msg-card { background:#fff; border:1px solid rgba(0,0,0,.06); border-radius:10px; padding:14px; margin-bottom:14px }
.msg-card-head { display:flex; align-items:center; justify-content:space-between; margin-bottom:10px }
.msg-pri-tag { padding:2px 8px; border-radius:3px; font-size:11px; font-weight:600; color:#fff }
.pri-1 { background:#e74c3c } .pri-2 { background:#f39c12 } .pri-3 { background:#1a73e8 }
.msg-time { font-size:11px; color:#bbb }
.msg-card-title { font-size:14px; font-weight:600; color:#1a1a2e; margin-bottom:8px }
.msg-card-content { font-size:13px; color:#555; white-space:pre-wrap; line-height:1.7 }
.msg-card-link { display:flex; align-items:center; gap:4px; margin-top:10px; padding-top:10px; border-top:1px solid rgba(0,0,0,.04); font-size:12px; color:#1a73e8 }

.send-summary { background:rgba(0,0,0,.02); border-radius:10px; padding:14px }
.sum-title { font-size:13px; font-weight:600; color:#888; margin-bottom:10px }
.sum-row { display:flex; justify-content:space-between; padding:5px 0; font-size:13px }
.sl { color:#999 } .sv { color:#333; text-align:right; max-width:180px; overflow:hidden; text-overflow:ellipsis }
.sv.mono { font-family:'SF Mono','Menlo',monospace; font-size:11px }
.sum-div { height:1px; background:rgba(0,0,0,.05); margin:8px 0 }
.sv.ready { color:#27ae60; font-weight:500 } .sv.not-ready { color:#e74c3c; font-weight:500 }
</style>

<style>
.modal-overlay { position:fixed; inset:0; background:rgba(0,0,0,.35); display:flex; align-items:center; justify-content:center; z-index:1100; padding:20px }
.modal-panel { background:#fff; border-radius:16px; width:100%; max-width:480px; max-height:85vh; display:flex; flex-direction:column; box-shadow:0 16px 48px rgba(0,0,0,.15) }
.modal-small { max-width:380px }
.modal-header { display:flex; align-items:center; justify-content:space-between; padding:20px 24px 0 }
.modal-header h3 { margin:0; font-size:17px; font-weight:600; color:#1a1a2e }
.modal-close { background:none; border:none; cursor:pointer; color:#aaa; padding:4px; border-radius:6px }
.modal-close:hover { background:rgba(0,0,0,.05); color:#333 }
.modal-body { padding:20px 24px; overflow-y:auto; flex:1 }
.modal-footer { display:flex; justify-content:flex-end; gap:10px; padding:16px 24px 20px; border-top:1px solid rgba(0,0,0,.06) }
.modal-btn { padding:10px 24px; border-radius:10px; font-size:14px; cursor:pointer; border:none; font-weight:500; transition:.2s }
.modal-btn.primary { background:linear-gradient(135deg,#1a73e8,#4a90d9); color:#fff }
.modal-btn.primary:hover { box-shadow:0 4px 12px rgba(26,115,232,.3) }
.modal-fade-enter-active { transition:all .2s ease }
.modal-fade-leave-active { transition:all .15s ease }
.modal-fade-enter-from,.modal-fade-leave-to { opacity:0 }
.modal-fade-enter-from .modal-panel { transform:scale(.94) translateY(12px) }
</style>
