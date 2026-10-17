<template>
  <div class="page-container orderbook-page">
    <div class="page-heading">
      <div>
        <div class="eyebrow">PRE-MEET PREPARATION</div>
        <h1>秩序册生成</h1>
        <p>按“项目与报名确认 → 日程安排与核对 → 导出秩序册”完成赛前编排。数据读取自现有赛事、项目与赛程接口。</p>
      </div>
      <div class="heading-actions">
        <el-button @click="router.push('/schedules')"><el-icon><Calendar /></el-icon> 赛程管理</el-button>
        <el-button type="primary" :loading="exporting" :disabled="!selectedEventId || schedules.length === 0" @click="exportOrderBook">
          <el-icon><Download /></el-icon> 导出秩序册 CSV
        </el-button>
      </div>
    </div>

    <el-card shadow="never" class="intro-card">
      <div class="event-toolbar">
        <div class="event-title">
          <span class="event-mark"><el-icon><Trophy /></el-icon></span>
          <div>
            <strong>{{ selectedEventName }}</strong>
            <div class="muted">当前选择的赛事 · 请在正式导出前核对比赛日期、场地、轮次与分组</div>
          </div>
        </div>
        <el-select v-model="selectedEventId" placeholder="选择赛事" filterable class="event-select" @change="loadEventData">
          <el-option v-for="event in events" :key="event.id" :label="event.name" :value="event.id" />
        </el-select>
      </div>
      <el-steps :active="activeStep" finish-status="success" align-center class="workflow-steps">
        <el-step title="项目与报名确认" description="核对项目配置及报名审核" />
        <el-step title="日程安排与核对" description="检查时间、场地和轮次" />
        <el-step title="生成秩序册" description="导出并核对正式文件" />
      </el-steps>
    </el-card>

    <el-row :gutter="16" class="summary-row">
      <el-col :xs="24" :sm="12" :lg="8">
        <el-card shadow="never" class="step-card">
          <div class="step-top"><span class="step-number">01</span><el-tag :type="items.length ? 'success' : 'warning'">{{ items.length ? '已读取项目' : '待配置项目' }}</el-tag></div>
          <h3>项目与报名确认</h3>
          <p>运动项目和报名审核由现有业务模块维护。生成秩序册前，应确认参赛项目、组别、报名名单及审核状态。</p>
          <div class="step-metric"><strong>{{ items.length }}</strong><span>个项目配置</span></div>
          <div class="step-actions">
            <el-button type="primary" plain @click="router.push('/sport-items')">项目管理</el-button>
            <el-button plain @click="router.push('/registrations')">报名审核</el-button>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :lg="8">
        <el-card shadow="never" class="step-card">
          <div class="step-top"><span class="step-number">02</span><el-tag :type="schedules.length ? 'success' : 'warning'">{{ schedules.length ? '已有赛程' : '待安排赛程' }}</el-tag></div>
          <h3>日程安排与核对</h3>
          <p>按赛事日期、开始时间、场地、轮次和分组检查赛程。参赛名单生成及兼项冲突检查在竞赛执行模块完成。</p>
          <div class="step-metric"><strong>{{ schedules.length }}</strong><span>条赛程记录</span></div>
          <div class="step-actions">
            <el-button type="primary" plain @click="router.push('/schedules')">维护赛程</el-button>
            <el-button plain @click="router.push('/competition-operations')">执行与冲突检查</el-button>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :lg="8">
        <el-card shadow="never" class="step-card">
          <div class="step-top"><span class="step-number">03</span><el-tag :type="generated ? 'success' : 'info'">{{ generated ? '已导出文件' : '等待生成' }}</el-tag></div>
          <h3>生成秩序册</h3>
          <p>使用后端正式导出接口生成当前赛事的秩序册 CSV。导出后建议由各参赛学院核对，再确认正式印发版本。</p>
          <div class="step-metric"><strong>{{ generated ? '完成' : '未生成' }}</strong><span>本次操作状态</span></div>
          <div class="step-actions">
            <el-button type="success" :loading="exporting" :disabled="!selectedEventId || schedules.length === 0" @click="exportOrderBook">
              <el-icon><Download /></el-icon> 生成并下载
            </el-button>
            <el-button plain :disabled="schedules.length === 0" @click="printPreview">打印预览</el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="never" class="schedule-card">
      <template #header>
        <div class="card-header">
          <div>
            <strong>赛程预览</strong>
            <div class="muted">显示所选赛事已保存的赛程；本页面不覆盖原有赛程数据。</div>
          </div>
          <el-button :loading="loading" @click="loadEventData"><el-icon><Refresh /></el-icon> 刷新数据</el-button>
        </div>
      </template>
      <el-table :data="schedules" v-loading="loading" stripe border>
        <el-table-column prop="matchDate" label="比赛日期" width="120" />
        <el-table-column prop="startTime" label="开始时间" width="100" />
        <el-table-column prop="endTime" label="结束时间" width="100" />
        <el-table-column prop="sportItemName" label="比赛项目" min-width="150" />
        <el-table-column prop="scheduleName" label="赛程名称" min-width="120" />
        <el-table-column label="轮次" width="100">
          <template #default="{ row }">{{ roundName(row.roundType) }}</template>
        </el-table-column>
        <el-table-column prop="groupNo" label="分组" width="90" />
        <el-table-column prop="location" label="比赛场地" min-width="140" />
        <el-table-column label="状态" width="110">
          <template #default="{ row }"><el-tag :type="statusType(row.status)">{{ statusName(row.status) }}</el-tag></template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!loading && selectedEventId && schedules.length === 0" description="当前赛事还没有赛程。请先进入赛程管理创建比赛日期、时间、场地和轮次。" />
      <div class="table-foot">
        <span>项目配置 {{ items.length }} 项</span>
        <span>赛程记录 {{ schedules.length }} 条</span>
        <span>正式秩序册以导出接口返回内容为准</span>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import { get } from '@/utils/request'

const router = useRouter()
const events = ref<any[]>([])
const items = ref<any[]>([])
const schedules = ref<any[]>([])
const selectedEventId = ref<number | null>(null)
const loading = ref(false)
const exporting = ref(false)
const generated = ref(false)

const selectedEventName = computed(() => events.value.find(item => item.id === selectedEventId.value)?.name || '请选择赛事')
const activeStep = computed(() => generated.value ? 3 : schedules.value.length ? 2 : items.value.length ? 1 : 0)

onMounted(loadEvents)

async function loadEvents() {
  loading.value = true
  try {
    const res = await get('/events', { current: 1, size: 100 })
    if (res.code === 200) {
      events.value = res.data?.records || []
      if (!selectedEventId.value && events.value.length) selectedEventId.value = events.value[0].id
      if (selectedEventId.value) await loadEventData()
    }
  } finally {
    loading.value = false
  }
}

async function loadEventData() {
  generated.value = false
  items.value = []
  schedules.value = []
  if (!selectedEventId.value) return
  loading.value = true
  try {
    const [itemRes, scheduleRes] = await Promise.all([
      get('/sport-items', { current: 1, size: 100, eventId: selectedEventId.value }),
      get('/schedules', { current: 1, size: 100, eventId: selectedEventId.value })
    ])
    if (itemRes.code === 200) items.value = itemRes.data?.records || []
    if (scheduleRes.code === 200) {
      schedules.value = (scheduleRes.data?.records || []).slice().sort((a: any, b: any) => {
        const left = `${a.matchDate || ''} ${a.startTime || ''}`
        const right = `${b.matchDate || ''} ${b.startTime || ''}`
        return left.localeCompare(right)
      })
    }
  } finally {
    loading.value = false
  }
}

async function exportOrderBook() {
  if (!selectedEventId.value || schedules.value.length === 0) {
    ElMessage.warning('请先选择赛事并完成赛程安排。')
    return
  }
  exporting.value = true
  try {
    const res = await get('/reports/order-book.csv', { eventId: selectedEventId.value })
    const blob = new Blob(['\ufeff', String(res.data ?? '')], { type: 'text/csv;charset=utf-8;' })
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `校运会秩序册-${selectedEventId.value}.csv`
    link.click()
    URL.revokeObjectURL(url)
    generated.value = true
    ElMessage.success('秩序册 CSV 已生成并下载。正式印发前请核对名单、赛次和时间。')
  } finally {
    exporting.value = false
  }
}

function printPreview() {
  window.print()
}
function roundName(type: string) {
  const map: Record<string, string> = { PRELIMINARY: '预赛', SEMIFINAL: '半决赛', FINAL: '决赛', FIELD_FINAL: '田赛决赛' }
  return map[type] || type || '未设置'
}
function statusName(status: string | number) {
  const map: Record<string, string> = { '0': '未开始', '1': '待开始', '2': '进行中', '3': '已结束', '4': '已取消' }
  return map[String(status)] || String(status ?? '未设置')
}
function statusType(status: string | number): 'success' | 'warning' | 'info' | 'danger' {
  const map: Record<string, 'success' | 'warning' | 'info' | 'danger'> = { '1': 'info', '2': 'warning', '3': 'success', '4': 'danger' }
  return map[String(status)] || 'info'
}
</script>

<style lang="scss" scoped>
.orderbook-page { max-width: 1600px; margin: 0 auto; }
.page-heading { display: flex; align-items: flex-end; justify-content: space-between; gap: 18px; margin-bottom: 18px; }
.eyebrow { color: #24775a; font-size: 10px; font-weight: 800; letter-spacing: 1.6px; }
.page-heading h1 { margin: 7px 0 8px; color: #20372d; font-size: 25px; }
.page-heading p { max-width: 780px; margin: 0; color: #78877f; font-size: 12px; line-height: 1.8; }
.heading-actions { display: flex; gap: 8px; flex-wrap: wrap; }
.intro-card, .step-card, .schedule-card { border: 1px solid #e5ece8; border-radius: 14px; }
.event-toolbar { display: flex; align-items: center; justify-content: space-between; gap: 18px; margin-bottom: 24px; }
.event-title { display: flex; align-items: center; gap: 12px; min-width: 0; }
.event-title strong { display: block; color: #243b30; font-size: 15px; }
.event-mark { display: grid; place-items: center; width: 42px; height: 42px; border-radius: 12px; background: #e6f5ec; color: #1c7858; font-size: 21px; flex-shrink: 0; }
.muted { margin-top: 5px; color: #89968f; font-size: 11px; line-height: 1.6; }
.event-select { width: 270px; }
.workflow-steps { padding: 5px 18px 0; }
.summary-row { margin-top: 16px; margin-bottom: 16px; }
.step-card { height: 100%; }
.step-top { display: flex; align-items: center; justify-content: space-between; }
.step-number { color: #1f7959; font-size: 25px; font-weight: 850; letter-spacing: -1px; }
.step-card h3 { margin: 14px 0 9px; color: #233a30; font-size: 15px; }
.step-card p { min-height: 58px; margin: 0; color: #78877f; font-size: 11px; line-height: 1.8; }
.step-metric { display: flex; align-items: baseline; gap: 8px; margin: 17px 0; padding-top: 13px; border-top: 1px solid #edf1ee; }
.step-metric strong { color: #1d6048; font-size: 25px; }
.step-metric span { color: #89968f; font-size: 11px; }
.step-actions { display: flex; flex-wrap: wrap; gap: 6px; }
.card-header { display: flex; align-items: center; justify-content: space-between; gap: 12px; }
.card-header strong { color: #263d32; font-size: 14px; }
.table-foot { display: flex; flex-wrap: wrap; gap: 18px; margin-top: 14px; color: #8a9790; font-size: 10px; }
@media (max-width: 900px) {
  .page-heading, .event-toolbar { align-items: flex-start; flex-direction: column; }
  .event-select { width: 100%; }
  .heading-actions { width: 100%; }
}
@media print {
  :global(.layout-aside), :global(.layout-header), .heading-actions, .event-select, .step-actions, .workflow-steps, .step-card, .intro-card, .schedule-card :deep(.el-card__header), .table-foot { display: none !important; }
  :global(.layout-main) { padding: 0 !important; }
  .page-heading { margin-bottom: 8px; }
  .page-heading h1 { font-size: 18px; }
  .orderbook-page { max-width: none; }
  .schedule-card { border: 0; }
}
</style>
