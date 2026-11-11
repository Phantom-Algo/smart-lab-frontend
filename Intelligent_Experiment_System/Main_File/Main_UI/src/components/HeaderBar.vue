<template>
  <div class="header-wrapper">
    <header class="header-bar">
      <div class="header-left" @click="goHome">
        <img class="logo-icon" src="../assets/logo.png" alt="logo" />
        <span class="platform-name">智慧实验室管理平台</span>
      </div>

      <nav class="header-nav" @mouseleave="onNavLeave">
        <div
          v-for="nav in currentNavItems"
          :key="nav.key"
          class="nav-tab-wrapper"
          @mouseenter="onNavEnter(nav.key)"
        >
          <span
            class="nav-tab"
            :class="{ active: activeNav === nav.key }"
            @click.stop="onNavClick(nav.key)"
          >
            {{ nav.label }}
          </span>
        </div>
      </nav>

      <div class="header-right">
        <span class="role-badge" :class="roleBadgeClass">{{ roleLabel }}</span>

        <div class="notification-bell" @click="onBellClick">
          <svg class="bell-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"/>
            <path d="M13.73 21a2 2 0 0 1-3.46 0"/>
          </svg>
          <span class="bell-badge" v-if="unreadCount > 0">{{ unreadCount > 99 ? '99+' : unreadCount }}</span>
        </div>

        <div class="user-avatar-wrapper" @mouseenter="showUserMenu = true" @mouseleave="showUserMenu = false">
          <div class="user-avatar">
            <span class="avatar-fallback">{{ userName.charAt(0) }}</span>
          </div>
          <transition name="user-menu-fade">
            <div v-show="showUserMenu" class="user-dropdown">
              <div class="user-menu-item" @click="onUserDetail">
                <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>
                用户详情
              </div>
              <div class="user-menu-divider"></div>
              <div class="user-menu-item logout" @click="onLogout">
                <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2"><path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/><polyline points="16,17 21,12 16,7"/><line x1="21" y1="12" x2="9" y2="12"/></svg>
                退出登录
              </div>
            </div>
          </transition>
        </div>
      </div>
    </header>

    <div v-show="dropdownVisible" class="dropdown-panel" @mouseenter="onDropdownEnter" @mouseleave="onDropdownLeave">
      <div class="dropdown-inner">
        <div class="dropdown-left">
          <div v-for="child in currentChildren" :key="child.key" class="dropdown-item" @click="onChildClick(child.key)">
            <div class="dropdown-item-text">
              <span class="dropdown-item-title">{{ child.label }}</span>
              <span class="dropdown-item-desc">{{ child.desc }}</span>
            </div>
          </div>
        </div>
        <div class="dropdown-right">
          <div class="dropdown-desc-title">{{ currentDesc?.title }}</div>
          <div class="dropdown-desc-text">{{ currentDesc?.desc }}</div>
        </div>
      </div>
    </div>

    <div v-show="dropdownVisible" class="bg-mask" @click="closeDropdown"></div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useMessageStore } from '../stores/messageStore.js'
import { userRole, userName, roleLabel, doLogout, isAdmin, isTeacher, isStudent, isMockMode } from '../stores/authStore.js'

const props = defineProps<{ currentPage: string }>()
const emit = defineEmits(['navigate'])
const { totalUnread: mockUnread, apiNewIdCount, fetchNewIds } = useMessageStore()

// 未读计数：API 模式用 apiNewIdCount，Mock 模式用 totalUnread
const unreadCount = computed(() => {
  return isMockMode.value ? mockUnread.value : apiNewIdCount.value
})

// 页面加载时拉取真实未读消息 ID（API 模式）
onMounted(() => { if (!isMockMode.value) fetchNewIds() })

const showUserMenu = ref(false)
const hoveredNav = ref<string | null>(null)
const activeChildKey = ref<string | null>(null)
let closeTimer: ReturnType<typeof setTimeout> | null = null

// 子页面 → 父级导航 key 映射表
const childToParentMap: Record<string, string> = {
  // 学生
  'my-course':'study','my-task':'study','my-grade':'study',
  'device-search':'resource','device-borrow':'resource',
  'lab-search':'resource',
  'hazard-report':'safety','my-report':'safety',
  'clean-arrange':'safety',
  '3d-show':'3d',
  // 教师
  'my-resource':'teaching','my-score':'teaching',
  'consumable-search':'resource','consumable-apply':'resource',
  'clean-evaluate':'safety',
  // 管理员
  'person-manage':'personnel','department-manage':'personnel','position-manage':'personnel','account-manage':'personnel',
  'device-manage':'asset','device-approve':'asset','device-repair':'asset','device-stats':'asset',
  'lab-manage':'lab','lab-approve':'lab','lab-stats':'lab',
  'course-manage':'teaching','resource-manage':'teaching',
  'consumable-stock':'consumable','consumable-pick':'consumable','consumable-warn':'consumable',
  'safety-check':'safety','safety-hazard':'safety','safety-alarm':'safety','safety-ledger':'safety',
  'clean-schedule':'safety','clean-review':'safety',
  'iot-monitor':'monitor','iot-threshold':'monitor','sensor-device':'monitor','3d-manager':'monitor',
  'efficiency':'data','ai-query':'data','msg-template':'data','msg-publish':'data',
}

// 根据当前页面推导顶部导航高亮
const activeNav = computed(() => {
  if (props.currentPage === 'home') return 'home'
  return childToParentMap[props.currentPage] || 'home'
})

const pageNameMap = {
  'home':'首页',
  'person-manage':'人员档案管理','department-manage':'部门管理','position-manage':'岗位管理','account-manage':'账号管理',
  'device-manage':'设备台账','device-approve':'借用审批','device-repair':'维修管理','device-stats':'设备统计',
  'lab-manage':'场地管理','lab-approve':'预约审核','lab-stats':'使用统计',
  'course-manage':'课程管理','resource-manage':'教学资源',
  'consumable-stock':'耗材库存','consumable-pick':'领用审批','consumable-warn':'库存预警',
  'safety-check':'安全检查','safety-hazard':'隐患管理','safety-alarm':'报警记录','safety-ledger':'安全台账',
  'clean-schedule':'排班管理','clean-review':'检查评价',
  'iot-monitor':'实时监测','iot-threshold':'阈值配置','sensor-device':'传感器设备',
  '3d-manager':'场景映射管理','3d-show':'3D漫游',
  'efficiency':'效能看板',
  'ai-query':'智能数据查询',
  'msg-template':'消息模板','msg-publish':'消息发布','msg-inbox':'消息中心',
  'my-course':'我的课程','my-task':'实验任务','my-resource':'教学资源','my-score':'成绩评定',
  'device-search':'设备查询','device-borrow':'借用申请',
  'lab-search':'实验室查询',
  'consumable-search':'耗材查询','consumable-apply':'领用申请',
  'hazard-report':'隐患上报','my-report':'我的上报',
  'clean-arrange':'值日安排','clean-evaluate':'检查评价',
  'my-grade':'我的成绩',
}

const navConfigs = {
  admin: {
    items: [
      { key:'home',label:'首页' },{ key:'personnel',label:'人员管理' },{ key:'asset',label:'设备资产' },
      { key:'lab',label:'场地管理' },{ key:'teaching',label:'教学管理' },{ key:'consumable',label:'耗材药剂' },
      { key:'safety',label:'安全环境' },{ key:'monitor',label:'3D与物联' },{ key:'data',label:'数据中心' },
    ],
    children: {
      personnel:[
        { key:'person-manage',label:'人员档案',desc:'学生/教师/管理员信息管理' },
        { key:'department-manage',label:'部门管理',desc:'维护实验室组织部门结构' },
        { key:'position-manage',label:'岗位管理',desc:'维护岗位信息并关联部门' },
        { key:'account-manage',label:'账号管理',desc:'创建/启停账号、分配权限' },
      ],
      asset:[
        { key:'device-manage',label:'设备台账',desc:'设备信息录入、查询、编辑' },
        { key:'device-approve',label:'借用审批',desc:'审核设备借用申请' },
        { key:'device-repair',label:'维修管理',desc:'故障报修、维修登记、费用录入' },
        { key:'device-stats',label:'统计报表',desc:'使用率、故障率、健康画像' },
      ],
      lab:[
        { key:'lab-manage',label:'场地管理',desc:'实验室信息、预约排期、使用统计' },
        { key:'lab-approve',label:'预约审核',desc:'审批教师/学生的实验室预约' },
        { key:'lab-stats',label:'使用统计',desc:'实验室利用率、空闲时段分析' },
      ],
      teaching:[
        { key:'course-manage',label:'课程管理',desc:'实验课程创建、排课选课监督' },
        { key:'resource-manage',label:'教学资源',desc:'PPT/Word/视频等资源管理' },
      ],
      consumable:[
        { key:'consumable-stock',label:'耗材库存',desc:'耗材台账、批次入库、库存查询' },
        { key:'consumable-pick',label:'领用审批',desc:'审核耗材领用申请' },
        { key:'consumable-warn',label:'库存预警',desc:'低库存、保质期过期预警' },
      ],
      safety:[
        { key:'safety-check',label:'安全检查',desc:'创建检查任务、提交检查结果' },
        { key:'safety-hazard',label:'隐患管理',desc:'隐患分派、整改复核、闭环管理' },
        { key:'safety-alarm',label:'报警记录',desc:'物联网/耗材报警处理' },
        { key:'safety-ledger',label:'安全台账',desc:'检查、隐患、报警综合查询' },
        { key:'clean-schedule',label:'排班管理',desc:'值日排班、提醒通知' },
        { key:'clean-review',label:'检查评价',desc:'卫生检查打分、评分统计' },
      ],
      monitor:[
        { key:'iot-monitor',label:'物联网环境',desc:'传感器实时监测、告警管理' },
        { key:'iot-threshold',label:'阈值配置',desc:'安全阈值设置、传感器绑定' },
        { key:'sensor-device',label:'传感器设备',desc:'传感器类型、检测数据项、实例管理' },
        { key:'3d-manager',label:'场景映射管理',desc:'实验室→3D场景的映射CRUD' },
        { key:'3d-show',label:'3D漫游',desc:'第一人称3D实验室实时漫游' },
      ],
      data:[
        { key:'ai-query',label:'智能数据查询',desc:'用自然语言安全查询实验室业务数据' },
        { key:'efficiency',label:'效能看板',desc:'设备利用率、预约统计、故障率' },
        { key:'msg-template',label:'消息模板',desc:'消息模板CRUD管理' },
        { key:'msg-publish',label:'消息发布',desc:'向指定用户/群组发送消息' },
      ],
    },
    desc: {
      personnel:{ title:'人员与权限管理中心',desc:'集中管理人员档案、部门岗位与账号权限，基于RBAC模型实现细粒度的功能与数据权限控制。' },
      asset:{ title:'设备与资产管理',desc:'涵盖设备全生命周期管理，从采购入库、借用归还到维修报废，实现设备资产安全可控、使用可追溯。' },
      lab:{ title:'实验场地管理',desc:'统一管理实验室场地信息与预约排期，审核师生预约申请，统计分析场地使用效率。' },
      teaching:{ title:'智能化教学监督',desc:'监督实验课程全流程，管理教学资源，查看教学开展情况。' },
      consumable:{ title:'耗材与药剂管理',desc:'精细化管理耗材台账、批次库存与领用流程，低库存、过期自动预警，保障实验物资供应。' },
      safety:{ title:'安全与卫生管理',desc:'集成安全检查、隐患整改闭环、报警记录处理与卫生值日管理，构建全方位实验室安全防护体系。' },
      monitor:{ title:'3D全景与物联网',desc:'管理实验室3D场景映射，实时监测物联网传感器数据，配置告警阈值，进入3D虚拟实验室漫游。' },
      data:{ title:'数据决策与消息中心',desc:'多维度效能指标监控与综合分析，管理消息模板与定向消息发布，为管理决策提供数据支撑。' },
    },
  },
  teacher: {
    items: [
      { key:'home',label:'首页' },{ key:'teaching',label:'教学中心' },{ key:'resource',label:'资源申请' },
      { key:'safety',label:'安全卫生' },{ key:'3d',label:'3D全景' },
    ],
    children: {
      teaching:[
        { key:'my-course',label:'我的课程',desc:'查看管理的实验课程与排课' },
        { key:'my-task',label:'实验任务',desc:'创建实验项目、发布实验任务' },
        { key:'my-resource',label:'教学资源',desc:'上传PPT/Word/视频等教学资料' },
        { key:'my-score',label:'成绩评定',desc:'批改实验报告、录入学生成绩' },
      ],
      resource:[
        { key:'device-search',label:'设备查询',desc:'搜索可用设备、查看设备详情' },
        { key:'device-borrow',label:'借用申请',desc:'提交设备借用申请' },
        { key:'lab-search',label:'实验室查询',desc:'查看实验室信息、开放时间' },
        { key:'consumable-search',label:'耗材查询',desc:'查看耗材库存信息' },
        { key:'consumable-apply',label:'领用申请',desc:'申请领用耗材或药剂' },
      ],
      safety:[
        { key:'hazard-report',label:'隐患上报',desc:'发现安全问题后自主上报' },
        { key:'my-report',label:'我的上报',desc:'查看上报记录与处理进度' },
        { key:'clean-arrange',label:'值日安排',desc:'查看值日排班表' },
        { key:'clean-evaluate',label:'检查评价',desc:'对学生值日情况打分评价' },
      ],
      '3d':[
        { key:'3d-show',label:'3D漫游',desc:'进入第一人称3D实验室实时漫游' },
      ],
    },
    desc: {
      teaching:{ title:'智能化教学中心',desc:'管理实验课程全流程，从课程创建、任务发布到学生成绩评定，提升实验教学效率与质量。' },
      resource:{ title:'实验资源申请',desc:'查询和借用设备、预约实验室、申领导师耗材，一站式完成实验资源准备。' },
      safety:{ title:'安全与卫生',desc:'上报实验室安全隐患，查看值日安排并评价学生值日情况，共同维护实验室安全与卫生。' },
      '3d':{ title:'3D全景展示',desc:'进入虚拟3D实验室场景，沉浸式漫游，查看设备布局与实验室环境。' },
    },
  },
  student: {
    items: [
      { key:'home',label:'首页' },{ key:'study',label:'我的学习' },{ key:'resource',label:'资源中心' },
      { key:'3d',label:'3D全景' },
    ],
    children: {
      study:[
        { key:'my-course',label:'我的课程',desc:'查看所选实验课程' },
        { key:'my-task',label:'实验任务',desc:'查看并完成实验任务' },
        { key:'my-grade',label:'我的成绩',desc:'查看实验报告评分与课程成绩' },
      ],
      resource:[
        { key:'device-search',label:'设备查询',desc:'搜索可用设备、查看设备详情' },
        { key:'device-borrow',label:'借用申请',desc:'提交设备借用申请' },
        { key:'lab-search',label:'实验室查询',desc:'查看实验室信息与开放时间' },
      ],
      '3d':[
        { key:'3d-show',label:'3D漫游',desc:'进入第一人称3D实验室实时漫游' },
      ],
    },
    desc: {
      study:{ title:'我的学习中心',desc:'查看课程、完成实验任务、追踪学习成绩，一站式管理你的实验学习进度。' },
      resource:{ title:'实验资源中心',desc:'查询可用设备和实验室信息，提交设备借用申请，跟踪借用与归还状态。' },
      '3d':{ title:'3D全景展示',desc:'进入虚拟3D实验室场景，沉浸式漫游，了解实验室环境和设备布局。' },
    },
  },
}

const currentNavItems = computed(() => { const r=userRole.value; if(r===1)return navConfigs.student.items; if(r===2)return navConfigs.teacher.items; return navConfigs.admin.items })
const currentNavChildren = computed(() => { const r=userRole.value; if(r===1)return navConfigs.student.children as Record<string, any>; if(r===2)return navConfigs.teacher.children as Record<string, any>; return navConfigs.admin.children as Record<string, any> })
const currentNavDesc = computed(() => { const r=userRole.value; if(r===1)return navConfigs.student.desc as Record<string, any>; if(r===2)return navConfigs.teacher.desc as Record<string, any>; return navConfigs.admin.desc as Record<string, any> })

const dropdownVisible = computed(() => { if(!hoveredNav.value)return false; return currentNavChildren.value[hoveredNav.value]!==undefined })
const currentChildren = computed(() => { if(!hoveredNav.value)return[]; return currentNavChildren.value[hoveredNav.value]||[] })
const currentDesc = computed(() => { if(!hoveredNav.value)return null; return currentNavDesc.value[hoveredNav.value]||null })
const roleBadgeClass = computed(() => { const r=userRole.value; if(r===1)return'role-student'; if(r===2)return'role-teacher'; return'role-admin' })

function onNavEnter(key: string): void { cancelCloseTimer(); if(key==='home'){ hoveredNav.value=null; return } hoveredNav.value=key }
function onNavLeave(): void { startCloseTimer() }
function onDropdownEnter(): void { cancelCloseTimer() }
function onDropdownLeave(): void { startCloseTimer() }
function onNavClick(key: string): void { if(key==='home'){ goHome() } }
function onChildClick(childKey: string): void { activeChildKey.value=childKey; emit('navigate',{ key:childKey, name:(pageNameMap as Record<string,string>)[childKey]||childKey }); closeDropdown() }
function goHome(): void { hoveredNav.value=null; activeChildKey.value=null; emit('navigate',{ key:'home', name:'首页' }) }
function closeDropdown(): void { hoveredNav.value=null }
function startCloseTimer(): void { closeTimer=setTimeout(()=>{ hoveredNav.value=null },200) }
function cancelCloseTimer(): void { if(closeTimer){ clearTimeout(closeTimer); closeTimer=null } }
function onBellClick(): void { emit('navigate',{ key:'msg-inbox', name:'消息中心' }) }
function onUserDetail(): void { showUserMenu.value=false; emit('navigate',{ key:'user-detail', name:'用户详情' }) }
function onLogout(): void { showUserMenu.value=false; doLogout(); goHome() }
</script>

<style>
.dropdown-panel{ animation:dropdownIn .2s ease }
@keyframes dropdownIn{ from{ opacity:0; transform:translateY(-6px) } to{ opacity:1; transform:translateY(0) } }
.bg-mask{ animation:maskIn .25s ease }
@keyframes maskIn{ from{ opacity:0 } to{ opacity:1 } }
.user-menu-fade-enter-active,.user-menu-fade-leave-active{ transition:all .15s ease }
.user-menu-fade-enter-from,.user-menu-fade-leave-to{ opacity:0; transform:translateY(-4px) }
</style>

<style scoped>
.header-wrapper{ position:relative; z-index:100 }
.header-bar{ display:flex; align-items:center; height:56px; padding:0 24px; background:rgba(255,255,255,.15); backdrop-filter:blur(12px); -webkit-backdrop-filter:blur(12px); border-bottom:1px solid rgba(255,255,255,.25); position:relative; z-index:101 }
.header-left{ display:flex; align-items:center; gap:8px; flex-shrink:0; margin-right:28px; cursor:pointer; user-select:none }
.logo-icon{ width:64px; height:64px; object-fit:contain; flex-shrink:0 }
.platform-name{ font-size:15px; font-weight:600; color:#1a1a2e; letter-spacing:.5px; white-space:nowrap }
.header-nav{ display:flex; align-items:center; gap:4px; flex:1; overflow-x:auto; scrollbar-width:none; -ms-overflow-style:none }
.header-nav::-webkit-scrollbar{ display:none }
.nav-tab-wrapper{ flex-shrink:0 }
.nav-tab{ display:block; padding:6px 14px; border-radius:8px; font-size:13.5px; font-weight:500; color:#555; cursor:pointer; transition:all .18s; white-space:nowrap; user-select:none }
.nav-tab:hover{ background:rgba(26,115,232,.08); color:#1a73e8 }
.nav-tab.active{ background:rgba(26,115,232,.12); color:#1a73e8; font-weight:600 }
.header-right{ display:flex; align-items:center; gap:14px; flex-shrink:0; margin-left:auto }
.role-badge{ font-size:11px; font-weight:600; padding:3px 10px; border-radius:10px; letter-spacing:.5px; user-select:none }
.role-admin{ background:rgba(233,30,99,.12); color:#c2185b }
.role-teacher{ background:rgba(26,115,232,.1); color:#1557b0 }
.role-student{ background:rgba(76,175,80,.12); color:#2e7d32 }
.notification-bell{ position:relative; display:flex; align-items:center; justify-content:center; width:36px; height:36px; border-radius:10px; cursor:pointer; transition:background .15s }
.notification-bell:hover{ background:rgba(0,0,0,.05) }
.bell-icon{ width:20px; height:20px; color:#666 }
.bell-badge{ position:absolute; top:4px; right:4px; min-width:16px; height:16px; border-radius:8px; background:#e53935; color:#fff; font-size:10px; font-weight:700; display:flex; align-items:center; justify-content:center; padding:0 4px }
.user-avatar-wrapper{ position:relative }
.user-avatar{ width:36px; height:36px; border-radius:50%; background:linear-gradient(135deg,#1a73e8,#4a90d9); display:flex; align-items:center; justify-content:center; cursor:pointer; transition:box-shadow .15s }
.user-avatar:hover{ box-shadow:0 2px 8px rgba(26,115,232,.3) }
.avatar-fallback{ font-size:16px; font-weight:700; color:#fff; user-select:none }
.user-dropdown{ position:absolute; top:calc(100% + 8px); right:0; min-width:180px; background:rgba(255,255,255,.96); backdrop-filter:blur(16px); border-radius:12px; padding:6px 0; box-shadow:0 8px 28px rgba(0,0,0,.12); border:1px solid rgba(0,0,0,.06); z-index:200 }
.user-menu-item{ display:flex; align-items:center; gap:10px; padding:9px 16px; font-size:13px; color:#333; cursor:pointer; transition:background .12s; white-space:nowrap }
.user-menu-item:hover{ background:rgba(26,115,232,.06) }
.user-menu-item.logout{ color:#e53935 }
.user-menu-item.logout:hover{ background:rgba(229,57,53,.06) }
.user-menu-divider{ height:1px; background:rgba(0,0,0,.06); margin:4px 0 }
.dropdown-panel{ position:absolute; top:56px; left:0; right:0; background:rgba(255,255,255,.94); backdrop-filter:blur(20px); -webkit-backdrop-filter:blur(20px); border-bottom:1px solid rgba(0,0,0,.06); box-shadow:0 8px 32px rgba(0,0,0,.08); z-index:99 }
.dropdown-inner{ max-width:1100px; margin:0 auto; display:flex; padding:20px 32px; gap:40px }
.dropdown-left{ flex:3; display:grid; grid-template-columns:repeat(auto-fill,minmax(200px,1fr)); gap:4px }
.dropdown-item{ padding:10px 14px; border-radius:10px; cursor:pointer; transition:background .15s }
.dropdown-item:hover{ background:rgba(26,115,232,.06) }
.dropdown-item-title{ display:block; font-size:14px; font-weight:600; color:#222; margin-bottom:3px }
.dropdown-item-desc{ display:block; font-size:12px; color:#999; line-height:1.4 }
.dropdown-right{ flex:2; padding:12px 0 12px 32px; border-left:1px solid rgba(0,0,0,.06) }
.dropdown-desc-title{ font-size:15px; font-weight:700; color:#1a1a2e; margin-bottom:8px }
.dropdown-desc-text{ font-size:13px; color:#888; line-height:1.7 }
.bg-mask{ position:fixed; inset:0; background:rgba(0,0,0,.08); z-index:50 }
</style>
