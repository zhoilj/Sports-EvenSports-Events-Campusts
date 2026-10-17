<template>
  <div class="page-container">
    <el-alert
      title="田赛试跳/试投管理"
      description="仅对田赛项目开放。参赛者须先完成现场检录；成绩录入后，在赛程结束时汇总最佳有效成绩并进入成绩复核流程。"
      type="info"
      :closable="false"
      show-icon
      class="page-alert"
    />
    <el-card shadow="never">
      <el-form :inline="true" class="search-form">
        <el-form-item label="田赛场次">
          <el-select v-model="selectedScheduleId" placeholder="请选择田赛场次" filterable clearable style="width: 420px" @change="loadScheduleData">
            <el-option v-for="item in scheduleOptions" :key="item.id" :label="formatSchedule(item)" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button :disabled="!selectedScheduleId" :loading="loading" @click="loadScheduleData">刷新</el-button>
          <el-button type="primary" :disabled="currentSchedule?.status !== 3 || entries.length === 0" :loading="finalizeLoading" @click="finalizeResults">
            汇总最佳成绩
          </el-button>
        </el-form-item>
      </el-form>

      <el-descriptions v-if="currentSchedule" :column="4" border class="schedule-summary">
        <el-descriptions-item label="赛事">{{ currentSchedule.eventName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="项目">{{ currentSchedule.sportItemName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="轮次">{{ currentSchedule.roundType || '-' }}</el-descriptions-item>
        <el-descriptions-item label="比赛时间">{{ currentSchedule.matchDate || '-' }} {{ currentSchedule.startTime || '' }}</el-descriptions-item>
        <el-descriptions-item label="场地">{{ currentSchedule.location || '-' }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="getScheduleStatusType(currentSchedule.status)">{{ getScheduleStatusName(currentSchedule.status) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="参赛人数">{{ entries.length }}</el-descriptions-item>
        <el-descriptions-item label="录入规则">每人最多6次试跳/试投</el-descriptions-item>
      </el-descriptions>

      <el-table :data="entries" v-loading="loading" border stripe>
        <el-table-column prop="participantName" label="参赛者" min-width="150" fixed />
        <el-table-column prop="studentNo" label="学号" width="120" />
        <el-table-column prop="college" label="学院" min-width="150" />
        <el-table-column prop="checkinStatus" label="检录状态" width="110">
          <template #default="{ row }">
            <el-tag :type="row.checkinStatus === 'CHECKED_IN' ? 'success' : 'warning'">
              {{ row.checkinStatus === 'CHECKED_IN' ? '已检录' : row.checkinStatus || '待检录' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="试跳/试投记录" min-width="330">
          <template #default="{ row }">
            <div v-if="row.attempts?.length" class="attempt-summary">
              <span v-for="attempt in row.attempts" :key="attempt.id || attempt.attemptNo" class="attempt-chip">
                {{ attempt.attemptNo }}轮：{{ attempt.isFoul === 1 ? '犯规' : (attempt.score ?? '—') }}
              </span>
            </div>
            <span v-else class="sub-text">尚未录入</span>
          </template>
        </el-table-column>
        <el-table-column label="最佳有效成绩" width="130">
          <template #default="{ row }">{{ bestScore(row) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link :disabled="currentSchedule?.status !== 2 || row.checkinStatus !== 'CHECKED_IN'" @click="openAttempts(row)">
              录入试次
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="selectedScheduleId && !loading && entries.length === 0" description="该场次暂无参赛名单" />
      <el-empty v-if="!selectedScheduleId" description="请选择田赛场次" />
    </el-card>

    <el-dialog v-model="attemptDialogVisible" title="试跳/试投成绩录入" width="720px">
      <div v-if="editingEntry" class="dialog-participant">
        <strong>{{ editingEntry.participantName || editingEntry.teamName }}</strong>
        <span>{{ editingEntry.college || '' }} {{ editingEntry.studentNo || '' }}</span>
      </div>
      <el-table :data="editAttempts" border>
        <el-table-column prop="attemptNo" label="轮次" width="80" />
        <el-table-column label="成绩" min-width="170">
          <template #default="{ row }">
            <el-input-number v-model="row.score" :min="0" :precision="2" :controls="false" :disabled="row.foul" placeholder="有效成绩" style="width: 140px" />
          </template>
        </el-table-column>
        <el-table-column label="犯规/无效" width="120">
          <template #default="{ row }">
            <el-checkbox v-model="row.foul" @change="handleFoulChange(row)">犯规</el-checkbox>
          </template>
        </el-table-column>
        <el-table-column label="备注" min-width="200">
          <template #default="{ row }">
            <el-input v-model="row.remark" maxlength="200" placeholder="可选" />
          </template>
        </el-table-column>
      </el-table>
      <template #footer>
        <el-button @click="attemptDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saveLoading" @click="saveAttempts">保存试次</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { get, post } from '@/utils/request'

interface ScheduleOption {
  id: number
  eventId: number
  eventName?: string
  sportItemId: number
  sportItemName?: string
  scheduleName?: string
  roundType?: string
  matchDate?: string
  startTime?: string
  location?: string
  status: number
}

interface AttemptEdit {
  id?: number
  scheduleEntryId: number
  attemptNo: number
  score: number | null
  foul: boolean
  remark: string
  isFoul?: number
}

const scheduleOptions = ref<ScheduleOption[]>([])
const selectedScheduleId = ref<number | null>(null)
const entries = ref<any[]>([])
const attempts = ref<any[]>([])
const loading = ref(false)
const saveLoading = ref(false)
const finalizeLoading = ref(false)
const attemptDialogVisible = ref(false)
const editingEntry = ref<any>(null)
const editAttempts = ref<AttemptEdit[]>([])

const currentSchedule = computed(() => scheduleOptions.value.find(item => item.id === selectedScheduleId.value))

onMounted(loadSchedules)

async function loadSchedules() {
  const res = await get('/schedules', { current: 1, size: 100 })
  if (res.code !== 200) return
  const schedules = res.data?.records || []
  const eventIds = Array.from(new Set(schedules.map((item: ScheduleOption) => item.eventId)))
  const itemLists = await Promise.all(eventIds.map(eventId => get('/sport-items', { eventId, size: 100 })))
  const fieldItemIds = new Set<number>()
  itemLists.forEach(result => {
    if (result.code === 200) {
      (result.data?.records || []).forEach((item: any) => {
        if (String(item.type || '').toUpperCase() === 'FIELD') fieldItemIds.add(item.id)
      })
    }
  })
  scheduleOptions.value = schedules.filter((item: ScheduleOption) => fieldItemIds.has(item.sportItemId))
}

function formatSchedule(item: ScheduleOption) {
  return [item.eventName, item.sportItemName, item.roundType, item.scheduleName, item.matchDate, item.startTime]
    .filter(Boolean).join(' · ')
}

async function loadScheduleData() {
  if (!selectedScheduleId.value) {
    entries.value = []
    attempts.value = []
    return
  }
  loading.value = true
  try {
    const results = await Promise.all([
      get('/schedules/' + selectedScheduleId.value + '/entries'),
      get('/field-attempts/schedule/' + selectedScheduleId.value)
    ])
    entries.value = results[0].code === 200 ? (results[0].data || []) : []
    attempts.value = results[1].code === 200 ? (results[1].data || []) : []
    entries.value = entries.value.map(entry => ({
      ...entry,
      attempts: attempts.value.filter(attempt => attempt.scheduleEntryId === entry.id)
        .sort((a, b) => a.attemptNo - b.attemptNo)
    }))
  } finally {
    loading.value = false
  }
}

function bestScore(row: any) {
  const validScores = (row.attempts || [])
    .filter((attempt: any) => attempt.isFoul !== 1 && attempt.score !== null && attempt.score !== undefined)
    .map((attempt: any) => Number(attempt.score))
  return validScores.length ? Math.max(...validScores).toString() : '-'
}

function openAttempts(row: any) {
  editingEntry.value = row
  editAttempts.value = Array.from({ length: 6 }, (_, index) => {
    const attemptNo = index + 1
    const existing = (row.attempts || []).find((attempt: any) => attempt.attemptNo === attemptNo)
    return {
      id: existing?.id,
      scheduleEntryId: row.id,
      attemptNo,
      score: existing?.score ?? null,
      foul: existing?.isFoul === 1,
      remark: existing?.remark || ''
    }
  })
  attemptDialogVisible.value = true
}

function handleFoulChange(row: AttemptEdit) {
  if (row.foul) row.score = null
}

async function saveAttempts() {
  if (!editingEntry.value) return
  const payload = editAttempts.value
    .filter(item => item.id || item.score !== null || item.foul || item.remark.trim())
    .map(item => ({
      id: item.id,
      scheduleEntryId: item.scheduleEntryId,
      attemptNo: item.attemptNo,
      score: item.score,
      foul: item.foul,
      remark: item.remark
    }))
  if (!payload.length) {
    ElMessage.warning('请至少录入一条有效成绩或犯规记录')
    return
  }
  saveLoading.value = true
  try {
    await post('/field-attempts/batch', payload)
    ElMessage.success('试跳/试投记录已保存')
    attemptDialogVisible.value = false
    await loadScheduleData()
  } finally {
    saveLoading.value = false
  }
}

async function finalizeResults() {
  if (!selectedScheduleId.value) return
  try {
    await ElMessageBox.confirm(
      '汇总操作将依据每位已检录参赛者的最佳有效成绩生成待复核成绩。请确认该场比赛已结束且试次记录完整。',
      '汇总田赛成绩',
      { type: 'warning' }
    )
    finalizeLoading.value = true
    const res = await post('/field-attempts/schedule/' + selectedScheduleId.value + '/finalize')
    ElMessage.success('已生成 ' + res.data + ' 条待复核成绩')
    await loadScheduleData()
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      // 接口错误由请求拦截器统一显示。
    }
  } finally {
    finalizeLoading.value = false
  }
}

function getScheduleStatusName(status: number) {
  return ({ 1: '未开始', 2: '进行中', 3: '已结束', 4: '已取消' } as Record<number, string>)[status] || '未知'
}

function getScheduleStatusType(status: number): 'success' | 'warning' | 'info' | 'primary' | 'danger' {
  return ({ 1: 'info', 2: 'primary', 3: 'success', 4: 'danger' } as Record<number, 'success' | 'warning' | 'info' | 'primary' | 'danger'>)[status] || 'info'
}
</script>

<style scoped>
.page-alert {
  margin-bottom: 16px;
}
.schedule-summary {
  margin: 8px 0 18px;
}
.attempt-summary {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}
.attempt-chip {
  background: var(--el-fill-color-light);
  border-radius: 4px;
  padding: 2px 6px;
  font-size: 12px;
}
.sub-text {
  color: var(--el-text-color-secondary);
  font-size: 12px;
}
.dialog-participant {
  display: flex;
  gap: 14px;
  align-items: center;
  margin-bottom: 14px;
}
</style>
