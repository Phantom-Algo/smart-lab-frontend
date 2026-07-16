<template>
  <div class="page-shell">
    <header class="page-header">
      <div><h2>人员档案</h2><p>统一维护学生、教师和管理员的基础信息</p></div>
      <button class="primary-btn" @click="openCreate">
        <svg viewBox="0 0 24 24"><path d="M12 5v14M5 12h14"/></svg>新增人员
      </button>
    </header>

    <section class="stat-grid">
      <div class="stat-card"><span>人员总数</span><strong>{{ total }}</strong></div>
      <div class="stat-card"><span>本页正常</span><strong>{{ currentNormal }}</strong></div>
      <div class="stat-card"><span>本页已绑定账号</span><strong>{{ currentBound }}</strong></div>
    </section>

    <section class="filter-panel">
      <div class="filter-grid">
        <label><span>关键字</span><input v-model.trim="filters.keyword" placeholder="姓名、编号、学号或工号" @keyup.enter="search" /></label>
        <label><span>人员类型</span><select v-model="filters.personType"><option value="">全部</option><option :value="1">学生</option><option :value="2">教师</option><option :value="3">管理员</option></select></label>
        <label><span>所属部门</span><select v-model="filters.departmentId"><option value="">全部</option><option v-for="item in departments" :key="item.id" :value="item.id">{{ item.departmentName }}</option></select></label>
        <label><span>人员状态</span><select v-model="filters.personStatus"><option value="">全部</option><option :value="1">正常</option><option :value="2">停用</option><option :value="3">离职</option><option :value="4">归档</option></select></label>
        <label><span>账号绑定</span><select v-model="filters.hasAccount"><option value="">全部</option><option :value="true">已绑定</option><option :value="false">未绑定</option></select></label>
      </div>
      <div class="filter-actions"><button class="ghost-btn" @click="resetFilters">重置</button><button class="secondary-btn" @click="search">查询</button></div>
    </section>

    <div v-if="message.text" class="message" :class="message.type">{{ message.text }}</div>

    <section class="table-card">
      <div v-if="loading" class="state-box">正在加载人员数据…</div>
      <div v-else-if="people.length === 0" class="state-box">没有符合条件的人员</div>
      <div v-else class="table-scroll">
        <table>
          <thead><tr><th>人员</th><th>编号</th><th>类型</th><th>部门 / 岗位</th><th>联系方式</th><th>状态</th><th>账号</th><th>操作</th></tr></thead>
          <tbody>
            <tr v-for="person in people" :key="person.id">
              <td><button class="name-link" @click="openView(person.id)">{{ person.personName }}</button><small>{{ genderLabel(person.genderType) }}</small></td>
              <td><code>{{ person.personNo }}</code><small>{{ person.studentNo || person.employeeNo || '—' }}</small></td>
              <td><span class="tag" :class="`type-${person.personType}`">{{ personTypeLabel(person.personType) }}</span></td>
              <td>{{ person.departmentName }}<small>{{ person.positionName || '未配置岗位' }}</small></td>
              <td>{{ person.phone || '—' }}<small>{{ person.email || '—' }}</small></td>
              <td><span class="status"><i :class="person.personStatus === 1 ? 'on' : 'off'"></i>{{ personStatusLabel(person.personStatus) }}</span></td>
              <td><span v-if="person.account" class="status"><i :class="person.account.accountStatus === 1 ? 'on' : 'off'"></i>{{ accountStatusLabel(person.account.accountStatus) }}</span><span v-else class="muted">未绑定</span></td>
              <td><div class="row-actions"><button @click="openView(person.id)">查看</button><button @click="openEdit(person.id)">编辑</button><button @click="changeStatus(person)">{{ person.personStatus === 1 ? '停用' : '恢复' }}</button></div></td>
            </tr>
          </tbody>
        </table>
      </div>
      <footer class="pagination"><span>共 {{ total }} 条，第 {{ pageNum }} / {{ totalPages }} 页</span><div><button :disabled="pageNum <= 1" @click="goPage(pageNum - 1)">上一页</button><button :disabled="pageNum >= totalPages" @click="goPage(pageNum + 1)">下一页</button></div></footer>
    </section>

    <teleport to="body">
      <div v-if="dialogOpen" class="modal-overlay" @click.self="closeDialog">
        <section class="modal-panel">
          <header class="modal-header"><div><h3>{{ dialogTitle }}</h3><p>{{ mode === 'view' ? '人员档案与账号绑定信息' : '带 * 的字段为必填项' }}</p></div><button class="icon-btn" @click="closeDialog"><svg viewBox="0 0 24 24"><path d="m6 6 12 12M18 6 6 18"/></svg></button></header>
          <div class="modal-body">
            <div class="form-grid">
              <label><span>人员编号 *</span><input v-model.trim="form.personNo" :disabled="mode !== 'create'" /></label>
              <label><span>姓名 *</span><input v-model.trim="form.personName" :disabled="mode === 'view'" /></label>
              <label><span>人员类型 *</span><select v-model="form.personType" :disabled="mode === 'view'"><option :value="1">学生</option><option :value="2">教师</option><option :value="3">管理员</option></select></label>
              <label><span>性别 *</span><select v-model="form.genderType" :disabled="mode === 'view'"><option :value="0">未知</option><option :value="1">男</option><option :value="2">女</option></select></label>
              <label><span>所属部门 *</span><select v-model="form.departmentId" :disabled="mode === 'view'" @change="onDepartmentChange"><option :value="0" disabled>请选择</option><option v-for="item in departments" :key="item.id" :value="item.id">{{ item.departmentName }}</option></select></label>
              <label><span>岗位</span><select v-model="form.positionId" :disabled="mode === 'view'"><option value="">暂不配置</option><option v-for="item in availablePositions" :key="item.id" :value="item.id">{{ item.positionName }}</option></select></label>
              <label v-if="form.personType === 1"><span>学号 *</span><input v-model.trim="form.studentNo" :disabled="mode === 'view'" /></label>
              <label v-if="form.personType !== 1"><span>工号 *</span><input v-model.trim="form.employeeNo" :disabled="mode === 'view'" /></label>
              <label v-if="form.personType === 1"><span>班级</span><input v-model.trim="form.className" :disabled="mode === 'view'" /></label>
              <label v-if="form.personType !== 1"><span>职称</span><input v-model.trim="form.titleName" :disabled="mode === 'view'" /></label>
              <label><span>手机号</span><input v-model.trim="form.phone" :disabled="mode === 'view'" /></label>
              <label><span>邮箱</span><input v-model.trim="form.email" type="email" :disabled="mode === 'view'" /></label>
              <label><span>人员状态 *</span><select v-model="form.personStatus" :disabled="mode === 'view'"><option :value="1">正常</option><option :value="2">停用</option><option :value="3">离职</option><option :value="4">归档</option></select></label>
              <label><span>入职 / 入学时间</span><input v-model="form.entryAt" type="datetime-local" :disabled="mode === 'view'" /></label>
              <label><span>离职 / 离校时间</span><input v-model="form.leaveAt" type="datetime-local" :disabled="mode === 'view'" /></label>
              <label class="full"><span>备注</span><textarea v-model.trim="form.description" rows="3" :disabled="mode === 'view'"></textarea></label>
            </div>
            <div v-if="mode === 'view'" class="account-summary"><strong>关联账号</strong><template v-if="detail?.account"><span>{{ detail.account.username }}</span><span>{{ accountStatusLabel(detail.account.accountStatus) }}</span><span>{{ detail.account.passwordResetRequired ? '首次登录需改密' : '密码状态正常' }}</span></template><span v-else class="muted">尚未创建登录账号</span></div>
            <p v-if="formError" class="form-error">{{ formError }}</p>
          </div>
          <footer class="modal-footer"><button class="ghost-btn" @click="closeDialog">{{ mode === 'view' ? '关闭' : '取消' }}</button><button v-if="mode !== 'view'" class="primary-btn" :disabled="saving" @click="savePerson">{{ saving ? '保存中…' : '保存' }}</button></footer>
        </section>
      </div>
    </teleport>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import {
  createPerson, getDepartmentTree, getPersonDetail, getPersonPage, getPositions,
  updatePerson, updatePersonStatus,
  type DepartmentItem, type GenderType, type PersonDetail, type PersonStatus, type PersonType, type PositionItem,
} from '../../utils/personnelApi'

type Mode = 'create' | 'edit' | 'view'
const people = ref<PersonDetail[]>([])
const departments = ref<DepartmentItem[]>([])
const positions = ref<PositionItem[]>([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = 10
const loading = ref(false)
const saving = ref(false)
const dialogOpen = ref(false)
const mode = ref<Mode>('view')
const detail = ref<PersonDetail | null>(null)
const formError = ref('')
const message = reactive({ text: '', type: 'success' as 'success' | 'error' })
const filters = reactive<{ keyword: string; personType: PersonType | ''; departmentId: number | ''; personStatus: PersonStatus | ''; hasAccount: boolean | '' }>({ keyword: '', personType: '', departmentId: '', personStatus: '', hasAccount: '' })
const form = reactive({ personNo: '', personName: '', personType: 1 as PersonType, genderType: 0 as GenderType, phone: '', email: '', studentNo: '', employeeNo: '', departmentId: 0, positionId: '' as number | '', className: '', titleName: '', personStatus: 1 as PersonStatus, entryAt: '', leaveAt: '', description: '' })

const currentNormal = computed(() => people.value.filter(item => item.personStatus === 1).length)
const currentBound = computed(() => people.value.filter(item => item.account).length)
const totalPages = computed(() => Math.max(1, Math.ceil(total.value / pageSize)))
const availablePositions = computed(() => positions.value.filter(item => item.departmentId === form.departmentId && item.positionStatus === 1))
const dialogTitle = computed(() => mode.value === 'create' ? '新增人员' : mode.value === 'edit' ? '编辑人员' : '人员详情')

function flatten(items: DepartmentItem[]): DepartmentItem[] { return items.flatMap(item => [item, ...flatten(item.children || [])]) }
function errorText(error: unknown): string { return error instanceof Error ? error.message : '操作失败，请稍后重试' }
function showMessage(text: string, type: 'success' | 'error' = 'success'): void { message.text = text; message.type = type; window.setTimeout(() => { if (message.text === text) message.text = '' }, 4000) }
function personTypeLabel(value: number): string { return ({ 1: '学生', 2: '教师', 3: '管理员' } as Record<number, string>)[value] || '未知' }
function personStatusLabel(value: number): string { return ({ 1: '正常', 2: '停用', 3: '离职', 4: '归档' } as Record<number, string>)[value] || '未知' }
function accountStatusLabel(value: number): string { return ({ 1: '正常', 2: '停用', 3: '锁定' } as Record<number, string>)[value] || '未知' }
function genderLabel(value: number): string { return ({ 0: '性别未填写', 1: '男', 2: '女' } as Record<number, string>)[value] || '未知' }
function formatDate(value?: string | null): string { return value ? value.slice(0, 16) : '' }

async function loadReferenceData(): Promise<void> {
  const [tree, positionPage] = await Promise.all([getDepartmentTree(), getPositions({ pageNum: 1, pageSize: 500 })])
  departments.value = flatten(tree)
  positions.value = positionPage.records
}
async function loadPeople(): Promise<void> {
  loading.value = true
  try {
    const result = await getPersonPage({ pageNum: pageNum.value, pageSize, keyword: filters.keyword || undefined, personTypes: filters.personType === '' ? undefined : [filters.personType], departmentId: filters.departmentId === '' ? undefined : filters.departmentId, personStatuses: filters.personStatus === '' ? undefined : [filters.personStatus], hasAccount: filters.hasAccount === '' ? undefined : filters.hasAccount })
    people.value = result.records; total.value = result.total
  } catch (error) { people.value = []; total.value = 0; showMessage(errorText(error), 'error') }
  finally { loading.value = false }
}
function search(): void { pageNum.value = 1; void loadPeople() }
function goPage(page: number): void { pageNum.value = page; void loadPeople() }
function resetFilters(): void { filters.keyword = ''; filters.personType = ''; filters.departmentId = ''; filters.personStatus = ''; filters.hasAccount = ''; search() }
function resetForm(): void { Object.assign(form, { personNo: '', personName: '', personType: 1, genderType: 0, phone: '', email: '', studentNo: '', employeeNo: '', departmentId: 0, positionId: '', className: '', titleName: '', personStatus: 1, entryAt: '', leaveAt: '', description: '' }); formError.value = ''; detail.value = null }
function fillForm(person: PersonDetail): void { Object.assign(form, { personNo: person.personNo, personName: person.personName, personType: person.personType, genderType: person.genderType, phone: person.phone || '', email: person.email || '', studentNo: person.studentNo || '', employeeNo: person.employeeNo || '', departmentId: person.departmentId, positionId: person.positionId || '', className: person.className || '', titleName: person.titleName || '', personStatus: person.personStatus, entryAt: formatDate(person.entryAt), leaveAt: formatDate(person.leaveAt), description: person.description || '' }) }
function openCreate(): void { resetForm(); mode.value = 'create'; dialogOpen.value = true }
async function openView(id: number): Promise<void> { await openExisting(id, 'view') }
async function openEdit(id: number): Promise<void> { await openExisting(id, 'edit') }
async function openExisting(id: number, nextMode: Mode): Promise<void> { try { const person = await getPersonDetail(id); detail.value = person; fillForm(person); mode.value = nextMode; dialogOpen.value = true } catch (error) { showMessage(errorText(error), 'error') } }
function closeDialog(): void { if (!saving.value) dialogOpen.value = false }
function onDepartmentChange(): void { if (!availablePositions.value.some(item => item.id === form.positionId)) form.positionId = '' }
function validate(): string { if (!form.personNo || !form.personName || !form.departmentId) return '请填写人员编号、姓名并选择所属部门'; if (form.personType === 1 && !form.studentNo) return '学生人员必须填写学号'; if (form.personType !== 1 && !form.employeeNo) return '教师或管理员必须填写工号'; return '' }
async function savePerson(): Promise<void> {
  formError.value = validate(); if (formError.value) return
  saving.value = true
  const common = { personName: form.personName, personType: form.personType, genderType: form.genderType, phone: form.phone || undefined, email: form.email || undefined, studentNo: form.personType === 1 ? form.studentNo : undefined, employeeNo: form.personType === 1 ? undefined : form.employeeNo, departmentId: form.departmentId, positionId: form.positionId === '' ? undefined : form.positionId, className: form.personType === 1 ? form.className || undefined : undefined, titleName: form.personType === 1 ? undefined : form.titleName || undefined, personStatus: form.personStatus, entryAt: form.entryAt || undefined, leaveAt: form.leaveAt || undefined, description: form.description || undefined }
  try { if (mode.value === 'create') await createPerson({ personNo: form.personNo, sourceType: 1, ...common }); else if (detail.value) await updatePerson(detail.value.id, common); dialogOpen.value = false; showMessage('人员档案已保存'); await loadPeople() }
  catch (error) { formError.value = errorText(error) }
  finally { saving.value = false }
}
async function changeStatus(person: PersonDetail): Promise<void> {
  const nextStatus: PersonStatus = person.personStatus === 1 ? 2 : 1
  if (!window.confirm(`确认将“${person.personName}”设为${personStatusLabel(nextStatus)}状态？${nextStatus !== 1 ? '其登录账号也会被停用。' : ''}`)) return
  try { await updatePersonStatus(person.id, nextStatus); showMessage('人员状态已更新'); await loadPeople() } catch (error) { showMessage(errorText(error), 'error') }
}

onMounted(async () => { try { await loadReferenceData() } catch (error) { showMessage(errorText(error), 'error') } await loadPeople() })
</script>

<style scoped>
.page-shell{height:100%;overflow:auto;padding:24px;color:#172033;background:transparent}.page-header{display:flex;align-items:center;justify-content:space-between;margin-bottom:18px}.page-header h2{margin:0;font-size:22px}.page-header p,.modal-header p{margin:5px 0 0;color:#7b8495;font-size:13px}.primary-btn,.secondary-btn,.ghost-btn{border:0;border-radius:8px;padding:9px 16px;font:inherit;font-size:13px;cursor:pointer}.primary-btn{display:inline-flex;align-items:center;gap:7px;background:#2563eb;color:white}.primary-btn svg{width:16px;height:16px;fill:none;stroke:currentColor;stroke-width:2}.secondary-btn{background:#172033;color:#fff}.ghost-btn{background:#eef1f6;color:#4b5565}.primary-btn:disabled{opacity:.55;cursor:not-allowed}.stat-grid{display:grid;grid-template-columns:repeat(3,minmax(150px,1fr));gap:12px;margin-bottom:14px}.stat-card,.filter-panel,.table-card{background:#fff;border:1px solid #e7ebf1;border-radius:12px}.stat-card{padding:16px 18px}.stat-card span{display:block;color:#7b8495;font-size:12px}.stat-card strong{display:block;margin-top:5px;font-size:24px}.filter-panel{padding:15px;margin-bottom:14px}.filter-grid{display:grid;grid-template-columns:2fr repeat(4,1fr);gap:10px}.filter-grid label,.form-grid label{display:flex;flex-direction:column;gap:6px}.filter-grid span,.form-grid span{font-size:12px;color:#687386}.filter-grid input,.filter-grid select,.form-grid input,.form-grid select,.form-grid textarea{box-sizing:border-box;width:100%;border:1px solid #dfe4ec;border-radius:8px;background:#fff;padding:9px 10px;color:#172033;font:inherit;font-size:13px;outline:none}.filter-grid input:focus,.filter-grid select:focus,.form-grid input:focus,.form-grid select:focus,.form-grid textarea:focus{border-color:#8cb3f5;box-shadow:0 0 0 3px #eff6ff}.filter-actions{display:flex;justify-content:flex-end;gap:8px;margin-top:12px}.message{margin-bottom:12px;border-radius:8px;padding:10px 13px;font-size:13px}.message.success{background:#ecfdf3;color:#137a45}.message.error{background:#fff1f2;color:#b42336}.table-card{overflow:hidden}.table-scroll{overflow-x:auto}table{width:100%;border-collapse:collapse;min-width:1040px}th,td{padding:13px 14px;text-align:left;border-bottom:1px solid #eef1f5;font-size:13px;vertical-align:middle}th{background:#fafbfc;color:#687386;font-size:12px;font-weight:600}td small{display:block;margin-top:4px;color:#9098a7}.name-link{padding:0;border:0;background:none;color:#1d4ed8;font:inherit;font-weight:600;cursor:pointer}code{font-size:12px;color:#4b5565}.tag{display:inline-block;border-radius:5px;padding:3px 8px;font-size:12px}.type-1{background:#eefbf3;color:#187647}.type-2{background:#eff6ff;color:#1d4ed8}.type-3{background:#f5f3ff;color:#6d28d9}.status{display:inline-flex;align-items:center;gap:6px;white-space:nowrap}.status i{width:7px;height:7px;border-radius:50%;background:#9ca3af}.status i.on{background:#22a06b}.status i.off{background:#e05252}.muted{color:#9098a7}.row-actions{display:flex;gap:6px}.row-actions button,.pagination button{border:1px solid #dfe4ec;border-radius:7px;background:#fff;padding:6px 9px;color:#4b5565;font-size:12px;cursor:pointer}.state-box{padding:56px;text-align:center;color:#8992a2}.pagination{display:flex;align-items:center;justify-content:space-between;padding:12px 14px;color:#7b8495;font-size:12px}.pagination div{display:flex;gap:7px}.pagination button:disabled{opacity:.4;cursor:not-allowed}.modal-overlay{position:fixed;inset:0;z-index:1200;display:flex;align-items:center;justify-content:center;padding:24px;background:rgba(15,23,42,.42)}.modal-panel{width:min(820px,100%);max-height:88vh;display:flex;flex-direction:column;background:#fff;border-radius:14px;box-shadow:0 22px 60px rgba(15,23,42,.2)}.modal-header{display:flex;justify-content:space-between;align-items:flex-start;padding:20px 22px;border-bottom:1px solid #eef1f5}.modal-header h3{margin:0;font-size:18px}.icon-btn{border:0;background:none;color:#687386;cursor:pointer}.icon-btn svg{width:20px;height:20px;fill:none;stroke:currentColor;stroke-width:2}.modal-body{padding:20px 22px;overflow:auto}.form-grid{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:14px}.form-grid .full{grid-column:1/-1}.form-grid textarea{resize:vertical}.form-grid :disabled{background:#f5f6f8;color:#7b8495}.account-summary{display:flex;gap:18px;align-items:center;margin-top:18px;padding:13px 15px;border:1px solid #e7ebf1;border-radius:9px;font-size:13px}.form-error{margin:14px 0 0;color:#b42336;font-size:13px}.modal-footer{display:flex;justify-content:flex-end;gap:8px;padding:15px 22px;border-top:1px solid #eef1f5}@media(max-width:900px){.filter-grid{grid-template-columns:repeat(2,1fr)}.stat-grid{grid-template-columns:1fr}.form-grid{grid-template-columns:1fr}.form-grid .full{grid-column:auto}}@media(max-width:560px){.page-shell{padding:16px}.page-header{align-items:flex-start;gap:12px}.filter-grid{grid-template-columns:1fr}.pagination{align-items:flex-start;gap:10px;flex-direction:column}}
</style>
