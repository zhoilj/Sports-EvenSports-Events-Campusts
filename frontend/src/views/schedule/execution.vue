<template>
  <div class="page-container">
    <el-alert
      title="竞赛编排与现场检录"
      description="根据审核通过的报名名单生成分组分道，检查赛程冲突，并在比赛现场登记检录状态。"
      type="info"
      :closable="false"
      show-icon
      class="page-alert"
    />
    <el-card shadow="never">
      <el-form :inline="true" class="search-form">
        <el-form-item label="赛程">
          <el-select v-model="selectedScheduleId" placeholder="请选择赛程" filterable clearable style="width: 380px" @change="loadEntries">
            <el-option v-for="item in scheduleOptions" :key="item.id" :label="formatSchedule(item)" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="每组人数">
          <el-input-number v-model="groupSize" :min="1" :max="16" />
        </el-form-item>
        <el-form-item label="可用道数">
          <el-input-number v-model="laneCount" :min="1" :max="16" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :disabled="!selectedScheduleId || currentSchedule?.status !== 1 || entries.length > 0" :loading="generateLoading" @click="generateEntries">
            生成分组分道
          </el-button>
          <el-button :disabled="!selectedScheduleId" :loading="conflictLoading" @click="checkConflicts">检查赛程冲突</el-button>
          <el-button :disabled="!selectedScheduleId" @click="loadEntries">刷新名单</el-button>
          <el-button v-if="currentSchedule?.status === 3 && currentSchedule?.roundType !== 'FIELD_FINAL'" type="success" @click="openAdvanceDialog">
            晋级下一轮
          </el-button>
        </el-form-item>
      </el-form>

      <el-descriptions v-if="currentSchedule" :column="4" border class="schedule-summary">
        <el-descriptions-item label="赛事">{{ currentSchedule.eventName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="项目">{{ currentSchedule.sportItemName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="轮次">{{ roundName(currentSchedule.roundType) }}</el-descriptions-item>
        <el-descriptions-item label="比赛时间">{{ currentSchedule.matchDate || '-' }} {{ currentSchedule.startTime || '' }}</el-descriptions-item>
        <el-descriptions-item label="场次">{{ currentSchedule.scheduleName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="场地">{{ currentSchedule.location || '-' }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="getScheduleStatusType(currentSchedule.status)">{{ getScheduleStatusName(currentSchedule.status) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="编排人数">{{ entries.length }}</el-descriptions-item>
      </el-descriptions>

      <el-alert v-if="conflicts.length" title="发现赛程冲突" type="warning" :closable="false" class="conflict-alert">
        <ul class="conflict-list">
          <li v-for="(item, index) in conflicts" :key="index">{{ item.message }}</li>
        </ul>
      </el-alert>
      <el-alert v-else-if="conflictsChecked" title="未发现已编排参赛者的时间冲突" type="success" :closable="false" class="conflict-alert" />

      <el-table :data="entries" v-loading="loading" border stripe>
        <el-table-column prop="checkinCode" label="检录编号" width="125" fixed />
        <el-table-column label="参赛者/队伍" min-width="150">
          <template #default="{ row }">
            <div class="participant-name">{{ row.participantName || row.teamName || '-' }}</div>
            <div v-if="row.studentNo" class="sub-text">学号：{{ row.studentNo }}</div>
          </template>
        </el-table-column>
        <el-table-column label="学院/班级" min-width="160">
          <template #default="{ row }">
            <div>{{ row.college || '-' }}</div>
            <div v-if="row.className" class="sub-text">{{ row.className }}</div>
          </template>
        </el-table-column>
        <el-table-column label="接力队员" min-width="180">
          <template #default="{ row }">{{ row.memberNames || '-' }}</template>
        </el-table-column>
        <el-table-column prop="groupNo" label="组别" width="85" />
        <el-table-column prop="laneNo" label="道次" width="75">
          <template #default="{ row }">{{ row.laneNo ?? '-' }}</template>
        </el-table-column>
        <el-table-column prop="startOrder" label="出场顺序" width="95" />
        <el-table-column label="检录状态" width="110">
          <template #default="{ row }">
            <el-tag :type="getCheckinStatusType(row.checkinStatus)">{{ getCheckinStatusName(row.checkinStatus) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="checkedAt" label="检录时间" width="165" />
        <el-table-column prop="checkinRemark" label="检录备注" min-width="140" show-overflow-tooltip />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button v-if="currentSchedule?.status === 1 || currentSchedule?.status === 2" type="success" link @click="updateCheckin(row, 'CHECKED_IN')">已检录</el-button>
            <el-dropdown v-if="currentSchedule?.status === 1 || currentSchedule?.status === 2" @command="status => updateCheckin(row, String(status))">
              <el-button type="primary" link>未到场/其他<el-icon class="el-icon--right"><ArrowDown /></el-icon></el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="ABSENT">缺席</el-dropdown-item>
                  <el-dropdown-item command="WITHDRAWN">弃权</el-dropdown-item>
                  <el-dropdown-item command="DQ">取消资格</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="selectedScheduleId && !loading && entries.length === 0" description="该场次尚未生成参赛名单" />
      <el-empty v-if="!selectedScheduleId" description="请选择赛程开始编排或检录" />
    </el-card>

    <el-dialog v-model="advanceDialogVisible" title="生成下一轮晋级名单" width="500px">
      <el-form :model="advanceForm" label-width="130px">
        <el-form-item label="晋级目标赛程">
          <el-select v-model="advanceForm.targetScheduleId" placeholder="请选择同项目的下一轮赛程" filterable style="width: 100%">
            <el-option v-for="item in targetSchedules" :key="item.id" :label="formatSchedule(item)" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="总晋级人数">
          <el-input-number v-model="advanceForm.qualifyCount" :min="1" :max="200" />
        </el-form-item>
        <el-form-item label="每组前几名">
          <el-input-number v-model="advanceForm.topPerGroup" :min="0" :max="16" />
          <div class="sub-text">设为 0 表示不设置分组保送名额。</div>
        </el-form-item>
        <el-form-item label="目标组容量">
          <el-input-number v-model="advanceForm.groupSize" :min="1" :max="16" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="advanceDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="advanceLoading" @click="submitAdvance">生成晋级名单</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { get, post, put } from '@/utils/request'

interface ScheduleOption {
  id: number
  eventId: number
  eventName?: string
  sportItemId: number
  sportItemName?: string
  scheduleName?: string
  roundNo?: number
  roundType?: string
  matchDate?: string
  startTime?: string
  location?: string
  status: number
}

const scheduleOptions = ref<ScheduleOption[]>([])
const selectedScheduleId = ref<number | null>(null)
const entries = ref<any[]>([])
const conflicts = ref<any[]>([])
const conflictsChecked = ref(false)
const loading = ref(false)
const generateLoading = ref(false)
const conflictLoading = ref(false)
const advanceLoading = ref(false)
const groupSize = ref(8)
const laneCount = ref(8)
const advanceDialogVisible = ref(false)
const advanceForm = reactive({
  targetScheduleId: null as number | null,
  qualifyCount: 8,
  topPerGroup: 0,
  groupSize: 8
})

const currentSchedule = computed(() => scheduleOptions.value.find(item => item.id === selectedScheduleId.value))
const targetSchedules = computed(() => scheduleOptions.value.filter(item =>
  currentSchedule.value &&
  item.id !== currentSchedule.value.id &&
  item.eventId === currentSchedule.value.eventId &&
  item.sportItemId === currentSchedule.value.sportItemId &&
  item.status === 1
))

onMounted(loadSchedules)

async function loadSchedules() {
  const res = await get('/schedules', { current: 1, size: 100 })
  if (res.code === 200) scheduleOptions.value = res.data?.records || []
}

function formatSchedule(item: ScheduleOption) {
  return [item.eventName, item.sportItemName, roundName(item.roundType), item.scheduleName, item.matchDate, item.startTime]
    .filter(Boolean).join(' · ')
}

function roundName(type?: string) {
  const labels: Record<string, string> = {
    PRELIMINARY: '预赛',
    SEMIFINAL: '半决赛',
    FINAL: '决赛',
    FIELD_FINAL: '田赛决赛'
  }
  return type ? labels[type] || type : '-'
}

async function loadEntries() {
  conflicts.value = []
  conflictsChecked.value = false
  if (!selectedScheduleId.value) {
    entries.value = []
    return
  }
  loading.value = true
  try {
    const res = await get('/checkins/schedule/' + selectedScheduleId.value)
    if (res.code === 200) entries.value = res.data || []
  } finally {
    loading.value = false
  }
}

async function generateEntries() {
  if (!selectedScheduleId.value) return
  try {
    await ElMessageBox.confirm(
      '系统将依据已审核通过的个人报名或接力队伍生成名单，并检查参赛者的赛程时间冲突。是否继续？',
      '生成参赛名单',
      { type: 'warning' }
    )
    generateLoading.value = true
    const res = await post('/schedules/' + selectedScheduleId.value + '/entries/generate', {
      groupSize: groupSize.value,
      laneCount: laneCount.value
    })
    ElMessage.success('已生成 ' + res.data + ' 条参赛记录')
    await loadEntries()
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      // 接口错误由请求拦截器统一显示。
    }
  } finally {
    generateLoading.value = false
  }
}

async function checkConflicts() {
  if (!selectedScheduleId.value) return
  conflictLoading.value = true
  try {
    const res = await get('/schedules/' + selectedScheduleId.value + '/conflicts')
    if (res.code === 200) {
      conflicts.value = res.data || []
      conflictsChecked.value = true
      if (conflicts.value.length === 0) ElMessage.success('未发现赛程时间冲突')
    }
  } finally {
    conflictLoading.value = false
  }
}

async function updateCheckin(row: any, status: string) {
  const labels: Record<string, string> = {
    CHECKED_IN: '已检录',
    ABSENT: '缺席',
    WITHDRAWN: '弃权',
    DQ: '取消资格'
  }
  try {
    if (status !== 'CHECKED_IN') {
      await ElMessageBox.confirm(
        '确定将 ' + (row.participantName || row.teamName || row.checkinCode) + ' 标记为“' + labels[status] + '”吗？',
        '确认检录状态',
        { type: 'warning' }
      )
    }
    await put('/checkins/' + row.id, { status, remark: row.checkinRemark || '' })
    ElMessage.success('检录状态已更新')
    await loadEntries()
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      // 接口错误由请求拦截器统一显示。
    }
  }
}

function openAdvanceDialog() {
  advanceForm.targetScheduleId = null
  advanceForm.qualifyCount = 8
  advanceForm.topPerGroup = 0
  advanceForm.groupSize = 8
  advanceDialogVisible.value = true
}

async function submitAdvance() {
  if (!selectedScheduleId.value || !advanceForm.targetScheduleId) {
    ElMessage.warning('请选择晋级目标赛程')
    return
  }
  advanceLoading.value = true
  try {
    const res = await post('/schedules/advance', {
      sourceScheduleId: selectedScheduleId.value,
      targetScheduleId: advanceForm.targetScheduleId,
      qualifyCount: advanceForm.qualifyCount,
      topPerGroup: advanceForm.topPerGroup,
      groupSize: advanceForm.groupSize
    })
    ElMessage.success('已生成 ' + res.data + ' 条晋级记录')
    advanceDialogVisible.value = false
    selectedScheduleId.value = advanceForm.targetScheduleId
    await loadEntries()
  } finally {
    advanceLoading.value = false
  }
}

function getScheduleStatusName(status: number) {
  return ({ 1: '未开始', 2: '进行中', 3: '已结束', 4: '已取消' } as Record<number, string>)[status] || '未知'
}

function getScheduleStatusType(status: number): 'success' | 'warning' | 'info' | 'primary' | 'danger' {
  return ({ 1: 'info', 2: 'primary', 3: 'success', 4: 'danger' } as Record<number, 'success' | 'warning' | 'info' | 'primary' | 'danger'>)[status] || 'info'
}

function getCheckinStatusName(status: string) {
  return ({
    PENDING: '待检录',
    CHECKED_IN: '已检录',
    ABSENT: '缺席',
    WITHDRAWN: '弃权',
    DQ: '取消资格'
  } as Record<string, string>)[status] || status || '待检录'
}

function getCheckinStatusType(status: string): 'success' | 'warning' | 'info' | 'primary' | 'danger' {
  return ({
    PENDING: 'info',
    CHECKED_IN: 'success',
    ABSENT: 'warning',
    WITHDRAWN: 'warning',
    DQ: 'danger'
  } as Record<string, 'success' | 'warning' | 'info' | 'primary' | 'danger'>)[status] || 'info'
}
</script>

<style scoped>
.page-alert, .conflict-alert {
  margin-bottom: 16px;
}
.schedule-summary {
  margin: 8px 0 18px;
}
.conflict-list {
  margin: 8px 0 0;
  padding-left: 20px;
}
.participant-name {
  font-weight: 600;
}
.sub-text {
  margin-top: 3px;
  color: var(--el-text-color-secondary);
  font-size: 12px;
}
</style>
