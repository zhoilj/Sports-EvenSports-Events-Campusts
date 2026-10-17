<template>
  <div class="page-container">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <div>
            <strong>竞赛执行与现场检录</strong>
            <div class="subheading">参赛名单、分组分道、检录、田赛试跳试投及赛程冲突检查</div>
          </div>
          <el-button @click="exportOrderBook" :disabled="!query.eventId">导出秩序册 CSV</el-button>
        </div>
      </template>

      <el-form :inline="true" class="search-form">
        <el-form-item label="赛事">
          <el-select v-model="query.eventId" placeholder="选择赛事" filterable clearable @change="onEventChange">
            <el-option v-for="event in eventOptions" :key="event.id" :label="event.name" :value="event.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="赛程">
          <el-select v-model="query.scheduleId" placeholder="选择赛程" filterable clearable @change="loadEntries">
            <el-option
              v-for="schedule in scheduleOptions"
              :key="schedule.id"
              :label="`${schedule.matchDate} ${schedule.startTime} · ${schedule.sportItemName} · ${schedule.scheduleName || '赛程'}`"
              :value="schedule.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadEntries" :disabled="!query.scheduleId">刷新名单</el-button>
          <el-button @click="checkConflicts" :disabled="!query.scheduleId">检查时间冲突</el-button>
        </el-form-item>
      </el-form>

      <el-alert v-if="currentSchedule" type="info" :closable="false" show-icon class="mb-16">
        <template #title>
          {{ currentSchedule.sportItemName }} · {{ roundName(currentSchedule.roundType) }} ·
          {{ currentSchedule.matchDate }} {{ currentSchedule.startTime }}–{{ currentSchedule.endTime || '约60分钟' }} ·
          {{ currentSchedule.location || '未设置场地' }}
        </template>
      </el-alert>

      <div class="toolbar">
        <el-button type="primary" @click="generateDialogVisible = true" :disabled="!currentSchedule || currentSchedule.status !== 1">
          生成参赛名单
        </el-button>
        <el-button @click="openAdvanceDialog" :disabled="!currentSchedule || currentSchedule.status !== 3 || !isTrack">
          按成绩晋级
        </el-button>
        <el-button type="success" @click="startSchedule" :disabled="!currentSchedule || currentSchedule.status !== 1">开始赛程</el-button>
        <el-button type="warning" @click="completeSchedule" :disabled="!currentSchedule || currentSchedule.status !== 2">结束赛程</el-button>
        <el-button v-if="isField" @click="openFieldAttempts" :disabled="!currentSchedule || !entryRows.length">田赛试跳/试投</el-button>
        <el-button v-if="isField" type="success" plain @click="finalizeFieldResults" :disabled="!currentSchedule || currentSchedule.status !== 3">汇总田赛成绩</el-button>
      </div>

      <el-table :data="entryRows" v-loading="loading" border stripe>
        <el-table-column label="组别" prop="groupNo" width="75" />
        <el-table-column label="道次/顺序" width="95">
          <template #default="{ row }">{{ row.laneNo ?? row.startOrder ?? '—' }}</template>
        </el-table-column>
        <el-table-column label="参赛者/队伍" min-width="150">
          <template #default="{ row }">
            <div>{{ row.participantName || row.teamName || '—' }}</div>
            <el-text v-if="row.memberNames" type="info" size="small">{{ row.memberNames }}</el-text>
          </template>
        </el-table-column>
        <el-table-column label="学号" prop="studentNo" width="120" />
        <el-table-column label="学院" prop="college" min-width="140" />
        <el-table-column label="检录状态" width="110">
          <template #default="{ row }">
            <el-tag :type="checkinType(row.checkinStatus)">{{ checkinName(row.checkinStatus) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="成绩" width="120">
          <template #default="{ row }">{{ row.scoreText || row.score || '—' }}</template>
        </el-table-column>
        <el-table-column label="名次" prop="ranking" width="70" />
        <el-table-column label="成绩状态" width="110">
          <template #default="{ row }">
            <el-tag v-if="row.resultStatus" :type="row.published ? 'success' : 'info'">
              {{ resultStatusName(row.resultStatus) }}{{ row.published ? ' · 已发布' : '' }}
            </el-tag>
            <span v-else>—</span>
          </template>
        </el-table-column>
        <el-table-column label="检录操作" width="245" fixed="right">
          <template #default="{ row }">
            <el-button link type="success" :disabled="!canCheckin(row)" @click="setCheckin(row, 'CHECKED_IN')">已检录</el-button>
            <el-dropdown :disabled="!canCheckin(row)" @command="(status: string) => setCheckin(row, status)">
              <el-button link type="warning" :disabled="!canCheckin(row)">异常状态<el-icon class="el-icon--right"><ArrowDown /></el-icon></el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="ABSENT">缺席（DNS）</el-dropdown-item>
                  <el-dropdown-item command="WITHDRAWN">弃权（WD）</el-dropdown-item>
                  <el-dropdown-item command="DQ">取消资格（DQ）</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!loading && query.scheduleId && entryRows.length === 0" description="该赛程尚未生成参赛名单" />
      <el-empty v-if="!query.scheduleId" description="请先选择赛事和赛程" />
    </el-card>

    <el-dialog v-model="generateDialogVisible" title="生成分组分道名单" width="460px">
      <el-form label-width="130px">
        <el-form-item label="每组人数">
          <el-input-number v-model="generateForm.groupSize" :min="1" :max="16" />
        </el-form-item>
        <el-form-item label="可用道数">
          <el-input-number v-model="generateForm.laneCount" :min="1" :max="16" />
        </el-form-item>
        <el-alert type="info" :closable="false" show-icon>
          径赛按组别和道次顺序编排；田赛按检录顺序生成参赛顺序。生成前会检查参赛者在同日其他赛程中的时间冲突。
        </el-alert>
      </el-form>
      <template #footer>
        <el-button @click="generateDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="generateEntries">生成名单</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="advanceDialogVisible" title="按预赛/半决赛成绩晋级" width="500px">
      <el-form label-width="140px">
        <el-form-item label="目标赛程">
          <el-select v-model="advanceForm.targetScheduleId" placeholder="选择下一轮赛程" style="width:100%">
            <el-option v-for="schedule in targetSchedules" :key="schedule.id" :label="`${schedule.matchDate} ${schedule.startTime} · ${roundName(schedule.roundType)}`" :value="schedule.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="总晋级人数">
          <el-input-number v-model="advanceForm.qualifyCount" :min="1" :max="64" />
        </el-form-item>
        <el-form-item label="每组直接晋级">
          <el-input-number v-model="advanceForm.topPerGroup" :min="0" :max="8" />
        </el-form-item>
        <el-form-item label="目标每组人数">
          <el-input-number v-model="advanceForm.groupSize" :min="1" :max="16" />
        </el-form-item>
        <el-alert type="warning" :closable="false" show-icon>
          只有已结束赛程中复核通过的有效成绩可以晋级。各组直接晋级名额总和不能超过总晋级人数。
        </el-alert>
      </el-form>
      <template #footer>
        <el-button @click="advanceDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="advanceEntries">生成晋级名单</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="conflictDialogVisible" title="赛程时间冲突检查" width="850px">
      <el-alert v-if="conflicts.length === 0" type="success" :closable="false" title="未发现参赛者时间冲突" />
      <el-table v-else :data="conflicts" border stripe>
        <el-table-column prop="participantName" label="参赛者" width="120" />
        <el-table-column prop="currentScheduleName" label="当前赛程" min-width="130" />
        <el-table-column prop="currentTime" label="当前时段" width="130" />
        <el-table-column prop="conflictingScheduleName" label="冲突赛程" min-width="130" />
        <el-table-column prop="conflictingTime" label="冲突时段" width="130" />
        <el-table-column prop="location" label="场地" width="100" />
      </el-table>
    </el-dialog>

    <el-dialog v-model="fieldDialogVisible" title="田赛试跳/试投成绩" width="96%" top="4vh">
      <el-alert type="info" :closable="false" class="mb-12">
        每名参赛者可登记1—6次试跳/试投。犯规请勾选“犯规”，系统不会将该次成绩计入最佳有效成绩。
      </el-alert>
      <el-table :data="fieldRows" border stripe max-height="58vh">
        <el-table-column prop="participantName" label="参赛者" fixed min-width="130" />
        <el-table-column prop="college" label="学院" min-width="120" />
        <el-table-column v-for="attemptNo in 6" :key="attemptNo" :label="`第${attemptNo}次`" min-width="170">
          <template #default="{ row }">
            <div class="attempt-cell">
              <el-input-number v-model="row.attempts[attemptNo - 1].score" :min="0" :precision="3" :step="0.01" :disabled="row.attempts[attemptNo - 1].foul" controls-position="right" />
              <el-checkbox v-model="row.attempts[attemptNo - 1].foul">犯规</el-checkbox>
            </div>
          </template>
        </el-table-column>
      </el-table>
      <template #footer>
        <el-button @click="fieldDialogVisible = false">关闭</el-button>
        <el-button type="primary" :loading="submitLoading" @click="saveFieldAttempts">保存试跳/试投</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { get, post, put } from '@/utils/request'

const loading = ref(false)
const submitLoading = ref(false)
const eventOptions = ref<any[]>([])
const scheduleOptions = ref<any[]>([])
const sportItemOptions = ref<any[]>([])
const entryRows = ref<any[]>([])
const conflicts = ref<any[]>([])
const fieldRows = ref<any[]>([])
const query = reactive({ eventId: null as number | null, scheduleId: null as number | null })
const generateDialogVisible = ref(false)
const advanceDialogVisible = ref(false)
const conflictDialogVisible = ref(false)
const fieldDialogVisible = ref(false)
const generateForm = reactive({ groupSize: 8, laneCount: 8 })
const advanceForm = reactive({
  targetScheduleId: null as number | null,
  qualifyCount: 8,
  topPerGroup: 2,
  groupSize: 8
})
const currentSchedule = computed(() => scheduleOptions.value.find(s => s.id === query.scheduleId))
const selectedItem = computed(() => sportItemOptions.value.find(i => i.id === currentSchedule.value?.sportItemId))
const isField = computed(() => selectedItem.value?.type === 'FIELD')
const isTrack = computed(() => selectedItem.value?.type === 'TRACK' || (selectedItem.value?.isRelay === 1 && selectedItem.value?.type !== 'FIELD'))
const targetSchedules = computed(() => scheduleOptions.value.filter(s =>
  s.id !== query.scheduleId && s.sportItemId === currentSchedule.value?.sportItemId && s.status === 1
))

onMounted(async () => {
  await loadEvents()
})

async function loadEvents() {
  const res = await get('/events', { current: 1, size: 100 })
  if (res.code === 200) eventOptions.value = res.data.records || []
}

async function onEventChange() {
  query.scheduleId = null
  entryRows.value = []
  scheduleOptions.value = []
  sportItemOptions.value = []
  if (!query.eventId) return
  const [scheduleRes, itemRes] = await Promise.all([
    get('/schedules', { current: 1, size: 100, eventId: query.eventId }),
    get('/sport-items', { current: 1, size: 100, eventId: query.eventId })
  ])
  if (scheduleRes.code === 200) scheduleOptions.value = scheduleRes.data.records || []
  if (itemRes.code === 200) sportItemOptions.value = itemRes.data.records || []
}

async function loadEntries() {
  entryRows.value = []
  if (!query.scheduleId) return
  loading.value = true
  try {
    const res = await get(`/schedules/${query.scheduleId}/entries`)
    if (res.code === 200) entryRows.value = res.data || []
  } finally {
    loading.value = false
  }
}

async function generateEntries() {
  if (!query.scheduleId) return
  if (!isField.value && generateForm.groupSize > generateForm.laneCount) {
    ElMessage.warning('径赛每组人数不能超过可用道数')
    return
  }
  submitLoading.value = true
  try {
    const res = await post(`/schedules/${query.scheduleId}/entries/generate`, generateForm)
    ElMessage.success(`已生成 ${res.data} 个参赛名额`)
    generateDialogVisible.value = false
    await loadEntries()
  } finally {
    submitLoading.value = false
  }
}

async function checkConflicts() {
  if (!query.scheduleId) return
  const res = await get(`/schedules/${query.scheduleId}/conflicts`)
  if (res.code === 200) {
    conflicts.value = res.data || []
    conflictDialogVisible.value = true
  }
}

async function openAdvanceDialog() {
  if (!query.scheduleId) return
  if (targetSchedules.value.length === 0) {
    ElMessage.warning('请先创建同一项目的下一轮赛程')
    return
  }
  advanceForm.targetScheduleId = targetSchedules.value[0].id
  advanceForm.qualifyCount = Math.min(8, Math.max(1, entryRows.value.length))
  advanceForm.topPerGroup = 2
  advanceForm.groupSize = 8
  advanceDialogVisible.value = true
}

async function advanceEntries() {
  if (!query.scheduleId || !advanceForm.targetScheduleId) {
    ElMessage.warning('请选择晋级目标赛程')
    return
  }
  submitLoading.value = true
  try {
    const res = await post('/schedules/advance', {
      sourceScheduleId: query.scheduleId,
      ...advanceForm
    })
    ElMessage.success(`已生成 ${res.data} 个晋级名额`)
    advanceDialogVisible.value = false
    query.scheduleId = advanceForm.targetScheduleId
    await loadEntries()
  } finally {
    submitLoading.value = false
  }
}

async function startSchedule() {
  if (!query.scheduleId) return
  await post(`/schedules/${query.scheduleId}/start`)
  ElMessage.success('赛程已开始')
  await reloadSchedulesAndEntries()
}

async function completeSchedule() {
  if (!query.scheduleId) return
  await ElMessageBox.confirm('确认结束该赛程？结束后将不能继续修改检录状态。', '确认结束', { type: 'warning' })
  await post(`/schedules/${query.scheduleId}/complete`)
  ElMessage.success('赛程已结束')
  await reloadSchedulesAndEntries()
}

async function reloadSchedulesAndEntries() {
  if (!query.eventId) return
  const currentId = query.scheduleId
  const res = await get('/schedules', { current: 1, size: 100, eventId: query.eventId })
  if (res.code === 200) scheduleOptions.value = res.data.records || []
  query.scheduleId = currentId
  await loadEntries()
}

function canCheckin(row: any) {
  return currentSchedule.value && [1, 2].includes(currentSchedule.value.status)
    && !['ABSENT', 'WITHDRAWN', 'DQ'].includes(row.checkinStatus)
}

async function setCheckin(row: any, status: string) {
  let remark = ''
  if (['ABSENT', 'WITHDRAWN', 'DQ'].includes(status)) {
    try {
      const prompt = await ElMessageBox.prompt('可填写原因或备注', '登记异常检录状态', { inputPlaceholder: '例如：未到场、主动弃权、犯规取消资格' })
      remark = prompt.value
    } catch {
      return
    }
  }
  await put(`/checkins/${row.id}`, { status, remark })
  ElMessage.success('检录状态已更新')
  await loadEntries()
}

async function openFieldAttempts() {
  if (!query.scheduleId || !entryRows.value.length) return
  const res = await get(`/field-attempts/schedule/${query.scheduleId}`)
  const attemptMap = new Map<string, any>()
  if (res.code === 200) {
    for (const item of res.data || []) attemptMap.set(`${item.scheduleEntryId}-${item.attemptNo}`, item)
  }
  fieldRows.value = entryRows.value.filter(row => row.checkinStatus === 'CHECKED_IN').map(row => ({
    ...row,
    attempts: Array.from({ length: 6 }, (_, index) => {
      const saved = attemptMap.get(`${row.id}-${index + 1}`)
      return { score: saved?.score ?? null, foul: saved?.isFoul === 1, remark: saved?.remark || '' }
    })
  }))
  fieldDialogVisible.value = true
}

async function saveFieldAttempts() {
  const payload: any[] = []
  for (const row of fieldRows.value) {
    row.attempts.forEach((attempt: any, index: number) => {
      if (attempt.foul || attempt.score !== null && attempt.score !== undefined) {
        payload.push({
          scheduleEntryId: row.id,
          attemptNo: index + 1,
          score: attempt.foul ? null : attempt.score,
          foul: attempt.foul,
          remark: attempt.remark
        })
      }
    })
  }
  if (!payload.length) {
    ElMessage.warning('请至少录入一条有效成绩或犯规记录')
    return
  }
  submitLoading.value = true
  try {
    await post('/field-attempts/batch', payload)
    ElMessage.success('试跳/试投记录已保存')
    fieldDialogVisible.value = false
    await loadEntries()
  } finally {
    submitLoading.value = false
  }
}

async function finalizeFieldResults() {
  if (!query.scheduleId) return
  await ElMessageBox.confirm('系统将根据每名参赛者的最佳有效成绩生成待复核成绩。确认继续？', '汇总田赛成绩', { type: 'warning' })
  const res = await post(`/field-attempts/schedule/${query.scheduleId}/finalize`)
  ElMessage.success(`已汇总 ${res.data} 条待复核成绩`)
  await loadEntries()
}

async function exportOrderBook() {
  if (!query.eventId) return
  const res = await get('/reports/order-book.csv', { eventId: query.eventId })
  downloadCsv(res.data, `校运会秩序册-${query.eventId}.csv`)
}

function downloadCsv(content: string, filename: string) {
  const blob = new Blob([content || ''], { type: 'text/csv;charset=utf-8;' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = filename
  link.click()
  URL.revokeObjectURL(url)
}

function roundName(type: string) {
  const map: Record<string, string> = { PRELIMINARY: '预赛', SEMIFINAL: '半决赛', FINAL: '决赛', FIELD_FINAL: '田赛决赛' }
  return map[type] || type || '决赛'
}
function checkinName(status: string) {
  const map: Record<string, string> = { PENDING: '待检录', CHECKED_IN: '已检录', ABSENT: '缺席', WITHDRAWN: '弃权', DQ: '取消资格' }
  return map[status] || status || '待检录'
}
function checkinType(status: string): 'success' | 'warning' | 'info' | 'danger' {
  const map: Record<string, 'success' | 'warning' | 'info' | 'danger'> = {
    CHECKED_IN: 'success', PENDING: 'info', ABSENT: 'danger', WITHDRAWN: 'warning', DQ: 'danger'
  }
  return map[status] || 'info'
}
function resultStatusName(status: string) {
  const map: Record<string, string> = { VALID: '有效', DNS: '未出赛', DNF: '未完成', DQ: '取消资格', WD: '弃权' }
  return map[status] || status
}
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
}
.subheading {
  color: var(--el-text-color-secondary);
  font-size: 12px;
  margin-top: 6px;
}
.toolbar {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 14px;
}
.mb-16 { margin-bottom: 16px; }
.mb-12 { margin-bottom: 12px; }
.attempt-cell {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
</style>
