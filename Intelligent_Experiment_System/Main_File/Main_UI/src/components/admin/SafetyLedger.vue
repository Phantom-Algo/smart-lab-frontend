<template>
  <div class="sl-page">
    <div class="page-header">
      <h2>安全台账</h2>
      <p>检查、隐患、报警综合查询</p>
    </div>

    <div class="stats-row">
      <div class="stat-card"><div class="stat-num blue">{{ records.length }}</div><div class="stat-label">总记录</div></div>
      <div class="stat-card"><div class="stat-num" style="color:#1a73e8">{{ typeCount('check') }}</div><div class="stat-label">检查记录</div></div>
      <div class="stat-card"><div class="stat-num" style="color:#f39c12">{{ typeCount('hazard') }}</div><div class="stat-label">隐患记录</div></div>
      <div class="stat-card"><div class="stat-num" style="color:#e74c3c">{{ typeCount('alarm') }}</div><div class="stat-label">报警记录</div></div>
    </div>

    <div class="filter-bar">
      <div class="filter-tabs">
        <div v-for="t in typeTabs" :key="t.key" class="filter-tab" :class="{ active: activeType === t.key }" @click="activeType = t.key; pageNum = 1">
          {{ t.label }}<span class="tab-count">{{ t.count }}</span>
        </div>
      </div>
      <select v-model="filterLab" class="tool-select"><option value="">全部实验室</option><option v-for="l in labOptions" :key="l" :value="l">{{ l }}</option></select>
      <select v-model="filterMonth" class="tool-select"><option value="">全部月份</option><option v-for="m in monthOptions" :key="m" :value="m">{{ m }}</option></select>
    </div>

    <div class="table-wrap" v-if="pagedList.length > 0">
      <table class="ledger-table">
        <thead>
          <tr>
            <th class="col-type">类型</th>
            <th class="col-no">编号</th>
            <th class="col-lab">实验室</th>
            <th class="col-title">标题 / 内容</th>
            <th class="col-status">状态</th>
            <th class="col-time">时间</th>
            <th class="col-act">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="r in pagedList" :key="r.id" @click="openDetail(r)" class="ledger-row">
            <td><span class="type-tag" :class="'type-' + r.recordType">{{ typeLabel(r.recordType) }}</span></td>
            <td class="no-cell">{{ r.recordNo }}</td>
            <td>{{ r.labName }}</td>
            <td class="title-cell">
              <span class="row-title">{{ r.title }}</span>
              <span class="row-sub" v-if="r.subtitle">{{ r.subtitle }}</span>
            </td>
            <td><span class="status-tag" :class="'st-' + r.statusColor">{{ r.statusName }}</span></td>
            <td class="time-cell">{{ r.recordTime }}</td>
            <td><span class="detail-link" @click.stop="openDetail(r)">详情</span></td>
          </tr>
        </tbody>
      </table>
    </div>

    <div v-else class="empty-state">
      <svg viewBox="0 0 24 24" width="48" height="48" fill="none" stroke="#ccc" stroke-width="1.2"><rect x="3" y="3" width="18" height="18" rx="2"/><line x1="3" y1="9" x2="21" y2="9"/><line x1="9" y1="21" x2="9" y2="9"/></svg>
      <p>暂无匹配的台账记录</p>
    </div>

    <div class="pagination" v-if="filteredList.length > pageSize">
      <button :disabled="pageNum <= 1" @click="pageNum--">上一页</button>
      <span>{{ pageNum }} / {{ totalPages }}</span>
      <button :disabled="pageNum >= totalPages" @click="pageNum++">下一页</button>
    </div>

    <!-- 详情弹窗（按类型自适应，只读） -->
    <teleport to="body">
      <transition name="modal-fade">
        <div v-if="showDetail" class="modal-overlay" @click.self="showDetail = false">
          <div class="modal-panel modal-wide">
            <div class="modal-header">
              <h3>
                <span class="type-tag" :class="'type-' + (currentRecord?.recordType || '')">{{ typeLabel(currentRecord?.recordType || 'check') }}</span>
                台账详情
              </h3>
              <button class="modal-close" @click="showDetail = false"><svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2"><path d="M18 6L6 18M6 6l12 12"/></svg></button>
            </div>
            <div class="modal-body">
              <!-- 检查记录详情 -->
              <template v-if="currentRecord?.recordType === 'check'">
                <div class="detail-section">
                  <div class="ds-title">基本信息</div>
                  <div class="detail-grid">
                    <div class="dg-item"><span class="dg-label">编号</span><span>{{ currentRecord.recordNo }}</span></div>
                    <div class="dg-item"><span class="dg-label">实验室</span><span>{{ currentRecord.labName }}</span></div>
                    <div class="dg-item"><span class="dg-label">检查人</span><span>{{ currentRecord.handler }}</span></div>
                    <div class="dg-item"><span class="dg-label">检查时间</span><span>{{ currentRecord.recordTime }}</span></div>
                    <div class="dg-item full"><span class="dg-label">任务名称</span><span>{{ currentRecord.title }}</span></div>
                  </div>
                </div>
                <div class="detail-section">
                  <div class="ds-title">检查结果</div>
                  <div class="detail-grid">
                    <div class="dg-item"><span class="dg-label">结果</span><span :class="currentRecord.statusColor === 'green' ? 'text-green' : 'text-red'">{{ currentRecord.statusName }}</span></div>
                    <div class="dg-item"><span class="dg-label">问题数</span><span>{{ currentRecord.extra?.issueCount || 0 }}</span></div>
                    <div class="dg-item full" v-if="currentRecord.extra?.problemDesc"><span class="dg-label">问题描述</span><span>{{ currentRecord.extra.problemDesc }}</span></div>
                    <div class="dg-item full" v-if="currentRecord.extra?.suggestion"><span class="dg-label">建议</span><span>{{ currentRecord.extra.suggestion }}</span></div>
                  </div>
                </div>
              </template>

              <!-- 隐患记录详情 -->
              <template v-if="currentRecord?.recordType === 'hazard'">
                <div class="detail-section">
                  <div class="ds-title">基本信息</div>
                  <div class="detail-grid">
                    <div class="dg-item"><span class="dg-label">编号</span><span>{{ currentRecord.recordNo }}</span></div>
                    <div class="dg-item"><span class="dg-label">实验室</span><span>{{ currentRecord.labName }}</span></div>
                    <div class="dg-item"><span class="dg-label">等级</span><span>{{ currentRecord.extra?.level || '-' }}</span></div>
                    <div class="dg-item"><span class="dg-label">上报时间</span><span>{{ currentRecord.recordTime }}</span></div>
                    <div class="dg-item"><span class="dg-label">上报人</span><span>{{ currentRecord.extra?.reporter || '-' }}</span></div>
                    <div class="dg-item"><span class="dg-label">期限</span><span>{{ currentRecord.extra?.deadline || '未设置' }}</span></div>
                    <div class="dg-item full"><span class="dg-label">标题</span><span>{{ currentRecord.title }}</span></div>
                    <div class="dg-item full" v-if="currentRecord.extra?.description"><span class="dg-label">描述</span><span>{{ currentRecord.extra.description }}</span></div>
                  </div>
                </div>
                <div class="detail-section">
                  <div class="ds-title">处理记录</div>
                  <div class="detail-grid">
                    <div class="dg-item"><span class="dg-label">当前状态</span><span>{{ currentRecord.statusName }}</span></div>
                    <div class="dg-item"><span class="dg-label">处理人</span><span>{{ currentRecord.handler || '-' }}</span></div>
                    <div class="dg-item full" v-if="currentRecord.extra?.rectifyNote"><span class="dg-label">整改说明</span><span>{{ currentRecord.extra.rectifyNote }}</span></div>
                    <div class="dg-item full" v-if="currentRecord.extra?.reviewOpinion"><span class="dg-label">复核意见</span><span>{{ currentRecord.extra.reviewOpinion }}</span></div>
                  </div>
                </div>
              </template>

              <!-- 报警记录详情 -->
              <template v-if="currentRecord?.recordType === 'alarm'">
                <div class="detail-section">
                  <div class="ds-title">报警信息</div>
                  <div class="detail-grid">
                    <div class="dg-item"><span class="dg-label">编号</span><span>{{ currentRecord.recordNo }}</span></div>
                    <div class="dg-item"><span class="dg-label">实验室</span><span>{{ currentRecord.labName }}</span></div>
                    <div class="dg-item"><span class="dg-label">报警类型</span><span>{{ currentRecord.extra?.alarmType || '-' }}</span></div>
                    <div class="dg-item"><span class="dg-label">报警时间</span><span>{{ currentRecord.recordTime }}</span></div>
                    <div class="dg-item"><span class="dg-label">报警对象</span><span>{{ currentRecord.extra?.alarmObject || '-' }}</span></div>
                    <div class="dg-item"><span class="dg-label">等级</span><span>{{ currentRecord.extra?.level || '-' }}</span></div>
                    <div class="dg-item full"><span class="dg-label">内容</span><span>{{ currentRecord.title }}</span></div>
                    <div class="dg-item"><span class="dg-label">报警值</span><span class="alarm-val">{{ currentRecord.extra?.alarmValue || '-' }}</span></div>
                    <div class="dg-item"><span class="dg-label">阈值</span><span class="threshold">{{ currentRecord.extra?.thresholdValue || '-' }}</span></div>
                  </div>
                </div>
                <div class="detail-section" v-if="currentRecord.statusName === '已处理'">
                  <div class="ds-title">处理信息</div>
                  <div class="detail-grid">
                    <div class="dg-item"><span class="dg-label">处理人</span><span>{{ currentRecord.handler || '-' }}</span></div>
                    <div class="dg-item full"><span class="dg-label">处理结果</span><span class="text-green">{{ currentRecord.extra?.handleResult || '-' }}</span></div>
                  </div>
                </div>
              </template>
            </div>
            <div class="modal-footer">
              <button class="modal-btn cancel" @click="showDetail = false">关闭</button>
            </div>
          </div>
        </div>
      </transition>
    </teleport>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'

type RecordType = 'check' | 'hazard' | 'alarm'
type StatusColor = 'green' | 'orange' | 'red' | 'gray'

interface LedgerRecord {
  id: number; recordType: RecordType; recordNo: string; labName: string
  title: string; subtitle: string | null; statusName: string; statusColor: StatusColor
  recordTime: string; handler: string | null
  extra: Record<string, string | number | null> | null
}

const records = ref<LedgerRecord[]>([
  // 检查记录
  { id:101, recordType:'check', recordNo:'CHK-2024-003', labName:'A305 分析化学实验室', title:'月度安全检查', subtitle:'安全设施与危化品', statusName:'正常', statusColor:'green', recordTime:'2026-07-05', handler:'王管理员', extra:{ issueCount:0, problemDesc:null, suggestion:'整体情况良好' } },
  { id:102, recordType:'check', recordNo:'CHK-2024-002', labName:'B103 物理实验室', title:'季度安全检查', subtitle:'电气线路与消防', statusName:'异常', statusColor:'red', recordTime:'2026-07-03', handler:'王管理员', extra:{ issueCount:2, problemDesc:'插座过热、灭火器过期', suggestion:'更换插座面板，更新灭火器' } },
  { id:103, recordType:'check', recordNo:'CHK-2024-001', labName:'C202 计算机实验室', title:'月度安全检查', subtitle:'设备运行与环境', statusName:'正常', statusColor:'green', recordTime:'2026-06-28', handler:'李老师', extra:{ issueCount:0, problemDesc:null, suggestion:null } },
  { id:104, recordType:'check', recordNo:'CHK-2024-004', labName:'D108 生物培养实验室', title:'专项安全检查', subtitle:'生物安全与灭菌设备', statusName:'正常', statusColor:'green', recordTime:'2026-06-20', handler:'王管理员', extra:{ issueCount:0, problemDesc:null, suggestion:null } },

  // 隐患记录
  { id:201, recordType:'hazard', recordNo:'HZ-2024-001', labName:'A305 分析化学实验室', title:'浓硫酸存放柜锁具损坏', subtitle:'未执行双人双锁管理', statusName:'待分派', statusColor:'red', recordTime:'2026-07-08', handler:null, extra:{ level:'高', reporter:'王管理员', deadline:'未设置', description:'锁具损坏，存在危化品流失风险', rectifyNote:null, reviewOpinion:null } },
  { id:202, recordType:'hazard', recordNo:'HZ-2024-003', labName:'A305 分析化学实验室', title:'通风橱风量不足', subtitle:'排风量低于标准值', statusName:'整改中', statusColor:'orange', recordTime:'2026-07-05', handler:'李老师', extra:{ level:'中', reporter:'孙老师', deadline:'2026-07-12', description:'通风橱排风量低于标准值', rectifyNote:'已联系厂家检测风机', reviewOpinion:null } },
  { id:203, recordType:'hazard', recordNo:'HZ-2024-006', labName:'B103 物理实验室', title:'灭火器过期', subtitle:'2具干粉灭火器已过有效期', statusName:'待复核', statusColor:'green', recordTime:'2026-07-03', handler:'周老师', extra:{ level:'高', reporter:'王管理员', deadline:'2026-07-05', description:'2具干粉灭火器已过有效期', rectifyNote:'已更换全新灭火器2具', reviewOpinion:null } },
  { id:204, recordType:'hazard', recordNo:'HZ-2024-007', labName:'A305 分析化学实验室', title:'洗眼器水压不足', subtitle:'紧急洗眼器出水压力偏低', statusName:'已闭环', statusColor:'gray', recordTime:'2026-07-01', handler:'孙老师', extra:{ level:'低', reporter:'李老师', deadline:'2026-07-03', description:'紧急洗眼器出水压力偏低', rectifyNote:'已检修管道并更换阀门', reviewOpinion:'验收合格，水压恢复正常' } },

  // 报警记录
  { id:301, recordType:'alarm', recordNo:'ALM-2024-001', labName:'A305 分析化学实验室', title:'温度异常，超过安全上限', subtitle:'温湿度传感器 #03', statusName:'未处理', statusColor:'red', recordTime:'2026-07-09', handler:null, extra:{ alarmType:'物联网环境报警', level:'重要', alarmObject:'温湿度传感器 #03', alarmValue:'38.5℃', thresholdValue:'35℃', handleResult:null } },
  { id:302, recordType:'alarm', recordNo:'ALM-2024-003', labName:'C202 计算机实验室', title:'烟雾浓度异常，疑似线路过热', subtitle:'烟雾传感器 #01', statusName:'未处理', statusColor:'red', recordTime:'2026-07-09', handler:null, extra:{ alarmType:'物联网环境报警', level:'重要', alarmObject:'烟雾传感器 #01', alarmValue:'0.8 mg/m³', thresholdValue:'0.5 mg/m³', handleResult:null } },
  { id:303, recordType:'alarm', recordNo:'ALM-2024-006', labName:'B103 物理实验室', title:'电压波动，超出正常范围', subtitle:'电压监测模块', statusName:'已处理', statusColor:'green', recordTime:'2026-07-07', handler:'王管理员', extra:{ alarmType:'物联网环境报警', level:'一般', alarmObject:'电压监测模块', alarmValue:'238V', thresholdValue:'220V±10V', handleResult:'已联系电工检修，电压恢复正常' } },
  { id:304, recordType:'alarm', recordNo:'ALM-2024-005', labName:'A305 分析化学实验室', title:'临近有效期，需及时处理', subtitle:'浓硫酸 (AR 500ml)', statusName:'未处理', statusColor:'red', recordTime:'2026-07-08', handler:null, extra:{ alarmType:'耗材药材报警', level:'重要', alarmObject:'浓硫酸 (AR 500ml)', alarmValue:'距到期 15 天', thresholdValue:'90 天', handleResult:null } },
  { id:305, recordType:'alarm', recordNo:'ALM-2024-008', labName:'A305 分析化学实验室', title:'检测到可燃气体浓度超标', subtitle:'可燃气体探测器 #02', statusName:'已处理', statusColor:'green', recordTime:'2026-07-05', handler:'王管理员', extra:{ alarmType:'物联网环境报警', level:'重要', alarmObject:'可燃气体探测器 #02', alarmValue:'12% LEL', thresholdValue:'10% LEL', handleResult:'关闭气阀，通风后浓度归零' } },
])

const activeType = ref<RecordType | 'all'>('all')
const filterLab = ref('')
const filterMonth = ref('')
const pageNum = ref(1)
const pageSize = 8

const labOptions = ['A305 分析化学实验室', 'B103 物理实验室', 'C202 计算机实验室', 'D108 生物培养实验室']
const monthOptions = ['2026-07', '2026-06', '2026-05']

const typeTabs = computed(() => [
  { key:'all' as const, label:'全部', count:records.value.length },
  { key:'check' as const, label:'检查记录', count:records.value.filter(r => r.recordType === 'check').length },
  { key:'hazard' as const, label:'隐患记录', count:records.value.filter(r => r.recordType === 'hazard').length },
  { key:'alarm' as const, label:'报警记录', count:records.value.filter(r => r.recordType === 'alarm').length },
])

const filteredList = computed(() => {
  let list = records.value
  if (activeType.value !== 'all') list = list.filter(r => r.recordType === activeType.value)
  if (filterLab.value) list = list.filter(r => r.labName === filterLab.value)
  if (filterMonth.value) list = list.filter(r => r.recordTime.startsWith(filterMonth.value))
  return list
})
const totalPages = computed(() => Math.ceil(filteredList.value.length / pageSize) || 1)
const pagedList = computed(() => filteredList.value.slice((pageNum.value - 1) * pageSize, pageNum.value * pageSize))

function typeCount(t: RecordType) { return records.value.filter(r => r.recordType === t).length }
function typeLabel(t: RecordType) { const m: Record<string,string> = { check:'检查', hazard:'隐患', alarm:'报警' }; return m[t] || t }

// 详情弹窗
const showDetail = ref(false)
const currentRecord = ref<LedgerRecord | null>(null)
function openDetail(r: LedgerRecord) { currentRecord.value = r; showDetail.value = true }
</script>

<style scoped>
.sl-page { height:100%; overflow-y:auto; padding:20px 24px }

.page-header { margin-bottom:16px }
.page-header h2 { font-size:20px; font-weight:600; color:#1a1a2e; margin:0 }
.page-header p { margin:4px 0 0; font-size:13px; color:#999 }

.stats-row { display:flex; gap:12px; margin-bottom:16px; flex-wrap:wrap }
.stat-card { background:rgba(255,255,255,.65); backdrop-filter:blur(10px); border:1px solid rgba(0,0,0,.06); border-radius:12px; padding:14px 20px; min-width:90px; text-align:center }
.stat-num { font-size:24px; font-weight:700; color:#1a1a2e; line-height:1.1 }
.stat-label { font-size:12px; color:#999; margin-top:2px }
.blue { color:#1a73e8 }

.filter-bar { display:flex; align-items:center; gap:10px; margin-bottom:18px; flex-wrap:wrap }
.filter-tabs { display:flex; gap:4px; background:rgba(255,255,255,.4); border-radius:10px; padding:4px }
.filter-tab { padding:8px 16px; border-radius:8px; font-size:13px; color:#666; cursor:pointer; transition:all .15s; display:flex; align-items:center; gap:6px; user-select:none; white-space:nowrap }
.filter-tab:hover { color:#333; background:rgba(0,0,0,.03) }
.filter-tab.active { background:#fff; color:#1a73e8; font-weight:600; box-shadow:0 1px 3px rgba(0,0,0,.08) }
.tab-count { font-size:11px; background:rgba(0,0,0,.06); padding:1px 7px; border-radius:10px; color:#888 }
.filter-tab.active .tab-count { background:rgba(26,115,232,.1); color:#1a73e8 }
.tool-select { padding:8px 12px; border:1px solid rgba(0,0,0,.1); border-radius:10px; font-size:13px; background:rgba(255,255,255,.7); outline:none; cursor:pointer }
.tool-select:last-child { margin-left:auto }

/* 表格 */
.table-wrap { background:rgba(255,255,255,.5); backdrop-filter:blur(10px); border-radius:12px; border:1px solid rgba(0,0,0,.04); overflow:hidden }
.ledger-table { width:100%; border-collapse:collapse; font-size:13px }
.ledger-table thead { background:rgba(0,0,0,.02) }
.ledger-table th { padding:12px 16px; text-align:left; font-weight:600; color:#888; font-size:12px; border-bottom:1px solid rgba(0,0,0,.06); white-space:nowrap }
.ledger-table td { padding:10px 16px; border-bottom:1px solid rgba(0,0,0,.03); color:#333; vertical-align:middle }
.ledger-row { cursor:pointer; transition:background .12s }
.ledger-row:hover { background:rgba(26,115,232,.03) }
.col-type { width:60px } .col-no { width:110px } .col-lab { width:150px } .col-status { width:70px } .col-time { width:90px } .col-act { width:60px }

.type-tag { padding:2px 8px; border-radius:4px; font-size:11px; font-weight:600; white-space:nowrap }
.type-check { background:rgba(26,115,232,.1); color:#1a73e8 }
.type-hazard { background:rgba(243,156,18,.1); color:#e67e22 }
.type-alarm { background:rgba(231,76,60,.1); color:#e74c3c }

.no-cell { font-family:'SF Mono','Menlo',monospace; font-size:12px; color:#666 }
.title-cell { max-width:280px }
.row-title { color:#1a1a2e; font-weight:500; display:block }
.row-sub { color:#999; font-size:11px; display:block; margin-top:1px; overflow:hidden; text-overflow:ellipsis; white-space:nowrap; max-width:260px }

.status-tag { padding:2px 8px; border-radius:4px; font-size:11px; font-weight:500; white-space:nowrap }
.st-green { background:rgba(39,174,96,.1); color:#27ae60 }
.st-orange { background:rgba(243,156,18,.1); color:#e67e22 }
.st-red { background:rgba(231,76,60,.1); color:#e74c3c }
.st-gray { background:rgba(0,0,0,.05); color:#999 }

.time-cell { color:#888; font-size:12px; white-space:nowrap }
.detail-link { color:#1a73e8; font-size:12px; cursor:pointer; font-weight:500 }
.detail-link:hover { text-decoration:underline }

.empty-state { display:flex; flex-direction:column; align-items:center; padding:60px 0; color:#ccc; gap:8px }
.empty-state p { margin:0; font-size:15px; color:#bbb }

.pagination { display:flex; align-items:center; justify-content:center; gap:10px; margin-top:18px }
.pagination button { padding:7px 16px; border:1px solid rgba(0,0,0,.1); border-radius:8px; background:#fff; font-size:13px; cursor:pointer; color:#555 }
.pagination button:disabled { opacity:.4; cursor:not-allowed }
</style>

<style>
.modal-overlay { position:fixed; inset:0; background:rgba(0,0,0,.35); display:flex; align-items:center; justify-content:center; z-index:1100; padding:20px }
.modal-panel { background:#fff; border-radius:16px; width:100%; max-width:480px; max-height:85vh; display:flex; flex-direction:column; box-shadow:0 16px 48px rgba(0,0,0,.15) }
.modal-wide { max-width:520px }
.modal-header { display:flex; align-items:center; justify-content:space-between; padding:20px 24px 0 }
.modal-header h3 { margin:0; font-size:17px; font-weight:600; color:#1a1a2e; display:flex; align-items:center; gap:8px }
.modal-close { background:none; border:none; cursor:pointer; color:#aaa; padding:4px; border-radius:6px }
.modal-close:hover { background:rgba(0,0,0,.05); color:#333 }
.modal-body { padding:20px 24px; overflow-y:auto; flex:1; display:flex; flex-direction:column; gap:14px }
.modal-footer { display:flex; justify-content:flex-end; gap:10px; padding:16px 24px 20px; border-top:1px solid rgba(0,0,0,.06) }
.modal-btn { padding:10px 24px; border-radius:10px; font-size:14px; cursor:pointer; border:none; font-weight:500; transition:.2s }
.modal-btn.cancel { background:rgba(0,0,0,.04); color:#666 }
.modal-btn.cancel:hover { background:rgba(0,0,0,.08) }

.detail-section { margin-bottom:6px }
.ds-title { font-size:14px; font-weight:600; color:#1a1a2e; margin-bottom:8px; padding-bottom:6px; border-bottom:1px solid rgba(0,0,0,.06) }
.detail-grid { display:grid; grid-template-columns:1fr 1fr; gap:8px 16px }
.dg-item { display:flex; justify-content:space-between; font-size:13px; color:#555; padding:4px 0 }
.dg-item.full { grid-column:1 / -1 }
.dg-label { color:#999; flex-shrink:0; margin-right:12px }
.text-green { color:#27ae60; font-weight:500 } .text-red { color:#e74c3c; font-weight:500 }
.alarm-val { color:#e74c3c; font-weight:600 } .threshold { color:#1a73e8; font-weight:500 }

.modal-fade-enter-active { transition:all .2s ease }
.modal-fade-leave-active { transition:all .15s ease }
.modal-fade-enter-from,.modal-fade-leave-to { opacity:0 }
.modal-fade-enter-from .modal-panel { transform:scale(.94) translateY(12px) }
</style>
