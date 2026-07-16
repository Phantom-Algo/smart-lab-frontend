<template>
  <div class="page-shell">
    <header class="page-header"><div><h2>部门管理</h2><p>维护组织层级、负责人和部门状态</p></div><button class="primary-btn" @click="openCreate()"><svg viewBox="0 0 24 24"><path d="M12 5v14M5 12h14"/></svg>新增部门</button></header>
    <div v-if="message.text" class="message" :class="message.type">{{ message.text }}</div>
    <section class="layout-card">
      <aside class="tree-panel">
        <div class="panel-title"><span>组织架构</span><small>{{ flatDepartments.length }} 个部门</small></div>
        <div v-if="loading" class="state-box">正在加载…</div>
        <div v-else-if="flatDepartments.length === 0" class="state-box">暂无部门</div>
        <button v-for="node in flatDepartments" :key="node.item.id" class="tree-node" :class="{ active: selected?.id === node.item.id }" :style="{ paddingLeft: `${14 + node.depth * 20}px` }" @click="selectDepartment(node.item.id)">
          <svg viewBox="0 0 24 24"><path d="M4 20V6h6V3h10v17M2 20h20M8 10h1m-1 4h1m5-7h2m-2 4h2m-2 4h2"/></svg><span>{{ node.item.departmentName }}</span><i :class="node.item.departmentStatus === 1 ? 'on' : 'off'"></i>
        </button>
      </aside>
      <main class="detail-panel">
        <div v-if="detailLoading" class="state-box">正在加载部门详情…</div>
        <template v-else-if="selected">
          <header class="detail-header"><div><span class="eyebrow">{{ selected.departmentCode }}</span><h3>{{ selected.departmentName }}</h3><p>{{ selected.description || '暂无部门说明' }}</p></div><span class="status"><i :class="selected.departmentStatus === 1 ? 'on' : 'off'"></i>{{ selected.departmentStatus === 1 ? '正常' : '停用' }}</span></header>
          <div class="info-grid"><div><span>部门编号</span><strong>{{ selected.departmentNo }}</strong></div><div><span>上级部门</span><strong>{{ parentName(selected.parentId) }}</strong></div><div><span>负责人</span><strong>{{ selected.leaderPersonName || '未设置' }}</strong></div><div><span>排序号</span><strong>{{ selected.sortNo }}</strong></div></div>
          <div class="detail-actions"><button class="ghost-btn" @click="openCreate(selected.id)">添加子部门</button><button class="secondary-btn" @click="openEdit">编辑信息</button><button class="danger-btn" @click="toggleStatus">{{ selected.departmentStatus === 1 ? '停用部门' : '恢复部门' }}</button></div>
        </template>
        <div v-else class="state-box">选择左侧部门查看详情</div>
      </main>
    </section>

    <teleport to="body"><div v-if="dialogOpen" class="modal-overlay" @click.self="closeDialog"><section class="modal-panel">
      <header class="modal-header"><div><h3>{{ editingId ? '编辑部门' : '新增部门' }}</h3><p>部门编号和编码创建后不可修改</p></div><button class="icon-btn" @click="closeDialog"><svg viewBox="0 0 24 24"><path d="m6 6 12 12M18 6 6 18"/></svg></button></header>
      <div class="modal-body"><div class="form-grid">
        <label><span>部门编号 *</span><input v-model.trim="form.departmentNo" :disabled="Boolean(editingId)" placeholder="如 DEPT-001" /></label>
        <label><span>部门编码 *</span><input v-model.trim="form.departmentCode" :disabled="Boolean(editingId)" placeholder="如 CS" /></label>
        <label><span>部门名称 *</span><input v-model.trim="form.departmentName" /></label>
        <label><span>上级部门</span><select v-model="form.parentId"><option value="">顶级部门</option><option v-for="item in parentOptions" :key="item.id" :value="item.id">{{ item.departmentName }}</option></select></label>
        <label><span>负责人</span><select v-model="form.leaderPersonId"><option value="">暂不设置</option><option v-for="person in leaderOptions" :key="person.id" :value="person.id">{{ person.personName }} · {{ person.personNo }}</option></select></label>
        <label><span>状态 *</span><select v-model="form.departmentStatus"><option :value="1">正常</option><option :value="2">停用</option></select></label>
        <label><span>排序号</span><input v-model.number="form.sortNo" type="number" min="0" /></label>
        <label class="full"><span>部门说明</span><textarea v-model.trim="form.description" rows="3"></textarea></label>
      </div><p v-if="formError" class="form-error">{{ formError }}</p></div>
      <footer class="modal-footer"><button class="ghost-btn" @click="closeDialog">取消</button><button class="primary-btn" :disabled="saving" @click="save">{{ saving ? '保存中…' : '保存' }}</button></footer>
    </section></div></teleport>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { createDepartment, getDepartmentDetail, getDepartmentTree, getPersonPage, updateDepartment, updateDepartmentStatus, type DepartmentItem, type DepartmentStatus, type PersonDetail } from '../../utils/personnelApi'

interface FlatNode { item: DepartmentItem; depth: number }
const tree = ref<DepartmentItem[]>([])
const selected = ref<DepartmentItem | null>(null)
const leaderOptions = ref<PersonDetail[]>([])
const loading = ref(false)
const detailLoading = ref(false)
const dialogOpen = ref(false)
const saving = ref(false)
const editingId = ref<number | null>(null)
const formError = ref('')
const message = reactive({ text: '', type: 'success' as 'success' | 'error' })
const form = reactive({ departmentNo: '', departmentCode: '', departmentName: '', parentId: '' as number | '', leaderPersonId: '' as number | '', departmentStatus: 1 as DepartmentStatus, sortNo: 0, description: '' })
const flatDepartments = computed(() => flatten(tree.value))
const parentOptions = computed(() => flatDepartments.value.map(node => node.item).filter(item => item.id !== editingId.value && !descendantIds(editingId.value).has(item.id)))

function flatten(items: DepartmentItem[], depth = 0): FlatNode[] { return items.flatMap(item => [{ item, depth }, ...flatten(item.children || [], depth + 1)]) }
function errorText(error: unknown): string { return error instanceof Error ? error.message : '操作失败，请稍后重试' }
function showMessage(text: string, type: 'success' | 'error' = 'success'): void { message.text = text; message.type = type; window.setTimeout(() => { if (message.text === text) message.text = '' }, 4000) }
function parentName(id?: number | null): string { return flatDepartments.value.find(node => node.item.id === id)?.item.departmentName || '顶级部门' }
function descendantIds(id: number | null): Set<number> { const result = new Set<number>(); if (!id) return result; const root = flatDepartments.value.find(node => node.item.id === id)?.item; const visit = (items: DepartmentItem[]) => items.forEach(item => { result.add(item.id); visit(item.children || []) }); visit(root?.children || []); return result }
async function loadTree(preferredId?: number): Promise<void> { loading.value = true; try { tree.value = await getDepartmentTree(); const targetId = preferredId || selected.value?.id || flatDepartments.value[0]?.item.id; if (targetId) await selectDepartment(targetId); else selected.value = null } catch (error) { showMessage(errorText(error), 'error') } finally { loading.value = false } }
async function loadLeaders(): Promise<void> { try { const result = await getPersonPage({ pageNum: 1, pageSize: 500, personTypes: [2, 3], personStatuses: [1] }); leaderOptions.value = result.records } catch (error) { showMessage(errorText(error), 'error') } }
async function selectDepartment(id: number): Promise<void> { detailLoading.value = true; try { selected.value = await getDepartmentDetail(id) } catch (error) { showMessage(errorText(error), 'error') } finally { detailLoading.value = false } }
function resetForm(): void { Object.assign(form, { departmentNo: '', departmentCode: '', departmentName: '', parentId: '', leaderPersonId: '', departmentStatus: 1, sortNo: 0, description: '' }); formError.value = '' }
function openCreate(parentId?: number): void { resetForm(); editingId.value = null; form.parentId = parentId || ''; dialogOpen.value = true }
function openEdit(): void { if (!selected.value) return; editingId.value = selected.value.id; Object.assign(form, { departmentNo: selected.value.departmentNo, departmentCode: selected.value.departmentCode, departmentName: selected.value.departmentName, parentId: selected.value.parentId || '', leaderPersonId: selected.value.leaderPersonId || '', departmentStatus: selected.value.departmentStatus, sortNo: selected.value.sortNo, description: selected.value.description || '' }); formError.value = ''; dialogOpen.value = true }
function closeDialog(): void { if (!saving.value) dialogOpen.value = false }
async function save(): Promise<void> { if (!form.departmentNo || !form.departmentCode || !form.departmentName) { formError.value = '请完整填写部门编号、编码和名称'; return } saving.value = true; const common = { departmentName: form.departmentName, parentId: form.parentId === '' ? undefined : form.parentId, leaderPersonId: form.leaderPersonId === '' ? undefined : form.leaderPersonId, departmentStatus: form.departmentStatus, sortNo: form.sortNo, description: form.description || undefined }; try { const result = editingId.value ? await updateDepartment(editingId.value, common) : await createDepartment({ departmentNo: form.departmentNo, departmentCode: form.departmentCode, ...common }); dialogOpen.value = false; showMessage('部门信息已保存'); await loadTree(result.id) } catch (error) { formError.value = errorText(error) } finally { saving.value = false } }
async function toggleStatus(): Promise<void> { if (!selected.value) return; const next: DepartmentStatus = selected.value.departmentStatus === 1 ? 2 : 1; if (!window.confirm(`确认${next === 1 ? '恢复' : '停用'}“${selected.value.departmentName}”？`)) return; try { await updateDepartmentStatus(selected.value.id, next); showMessage('部门状态已更新'); await loadTree(selected.value.id) } catch (error) { showMessage(errorText(error), 'error') } }
onMounted(async () => { await Promise.all([loadTree(), loadLeaders()]) })
</script>

<style scoped>
.page-shell{height:100%;overflow:auto;padding:24px;color:#172033;background:transparent}.page-header{display:flex;align-items:center;justify-content:space-between;margin-bottom:18px}.page-header h2{margin:0;font-size:22px}.page-header p,.modal-header p{margin:5px 0 0;color:#7b8495;font-size:13px}.primary-btn,.secondary-btn,.ghost-btn,.danger-btn{border:0;border-radius:8px;padding:9px 15px;font:inherit;font-size:13px;cursor:pointer}.primary-btn{display:inline-flex;align-items:center;gap:7px;background:#2563eb;color:#fff}.primary-btn svg{width:16px;height:16px;fill:none;stroke:currentColor;stroke-width:2}.secondary-btn{background:#172033;color:#fff}.ghost-btn{background:#eef1f6;color:#4b5565}.danger-btn{background:#fff0f1;color:#b42336}.primary-btn:disabled{opacity:.55}.message{margin-bottom:12px;border-radius:8px;padding:10px 13px;font-size:13px}.message.success{background:#ecfdf3;color:#137a45}.message.error{background:#fff1f2;color:#b42336}.layout-card{min-height:520px;display:grid;grid-template-columns:330px 1fr;overflow:hidden;background:#fff;border:1px solid #e7ebf1;border-radius:12px}.tree-panel{border-right:1px solid #e7ebf1;padding:12px 0}.panel-title{display:flex;justify-content:space-between;padding:8px 16px 14px;font-size:14px;font-weight:600}.panel-title small{color:#9098a7;font-weight:400}.tree-node{box-sizing:border-box;width:100%;display:flex;align-items:center;gap:9px;border:0;background:none;padding-top:10px;padding-bottom:10px;padding-right:14px;color:#4b5565;text-align:left;cursor:pointer}.tree-node:hover,.tree-node.active{background:#f1f6ff;color:#1d4ed8}.tree-node svg{width:17px;height:17px;fill:none;stroke:currentColor;stroke-width:1.8}.tree-node span{flex:1}.tree-node i,.status i{width:7px;height:7px;border-radius:50%;background:#dc5b61}.tree-node i.on,.status i.on{background:#22a06b}.detail-panel{padding:28px}.detail-header{display:flex;align-items:flex-start;justify-content:space-between;border-bottom:1px solid #eef1f5;padding-bottom:22px}.detail-header h3{margin:5px 0;font-size:24px}.detail-header p{margin:0;color:#7b8495;font-size:13px}.eyebrow{color:#2563eb;font-size:12px;font-weight:600}.status{display:inline-flex;align-items:center;gap:7px;border:1px solid #e7ebf1;border-radius:20px;padding:7px 11px;font-size:12px}.info-grid{display:grid;grid-template-columns:repeat(2,1fr);gap:12px;margin:24px 0}.info-grid div{padding:16px;border:1px solid #e7ebf1;border-radius:10px}.info-grid span{display:block;color:#7b8495;font-size:12px}.info-grid strong{display:block;margin-top:7px;font-size:14px}.detail-actions{display:flex;gap:9px}.state-box{padding:56px 20px;text-align:center;color:#8992a2}.modal-overlay{position:fixed;inset:0;z-index:1200;display:flex;align-items:center;justify-content:center;padding:24px;background:rgba(15,23,42,.42)}.modal-panel{width:min(700px,100%);max-height:88vh;display:flex;flex-direction:column;background:#fff;border-radius:14px}.modal-header{display:flex;justify-content:space-between;padding:20px 22px;border-bottom:1px solid #eef1f5}.modal-header h3{margin:0;font-size:18px}.icon-btn{border:0;background:none;cursor:pointer}.icon-btn svg{width:20px;height:20px;fill:none;stroke:#687386;stroke-width:2}.modal-body{padding:20px 22px;overflow:auto}.form-grid{display:grid;grid-template-columns:repeat(2,1fr);gap:14px}.form-grid label{display:flex;flex-direction:column;gap:6px}.form-grid span{font-size:12px;color:#687386}.form-grid input,.form-grid select,.form-grid textarea{box-sizing:border-box;width:100%;border:1px solid #dfe4ec;border-radius:8px;padding:9px 10px;background:#fff;font:inherit;font-size:13px;outline:none}.form-grid :disabled{background:#f5f6f8}.form-grid .full{grid-column:1/-1}.form-grid textarea{resize:vertical}.form-error{color:#b42336;font-size:13px}.modal-footer{display:flex;justify-content:flex-end;gap:8px;padding:15px 22px;border-top:1px solid #eef1f5}@media(max-width:800px){.layout-card{grid-template-columns:1fr}.tree-panel{border-right:0;border-bottom:1px solid #e7ebf1}.form-grid,.info-grid{grid-template-columns:1fr}.form-grid .full{grid-column:auto}}
</style>
