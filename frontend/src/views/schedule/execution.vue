<template>
  <div class="page-container">
    <el-alert
      title="竞赛编排与现场检录"
      description="选择已创建的赛程后，可按审核通过的报名记录自动生成分组、跑道、出场顺序和检录编号；比赛开始前允许调整编排。"
      type="info"
      :closable="false"
      show-icon
      class="page-alert"
    />

    <el-card shadow="never">
      <el-form :inline="true" class="search-form">
        <el-form-item label="赛程">
          <el-select
            v-model="selectedScheduleId"
            placeholder="请选择赛程"
            filterable
            clearable
            style="width: 360px"
            @change="loadEntries"
          >
            <el-option
              v-for="item in scheduleOptions"
              :key="item.id"
              :label="formatSchedule(item)"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="径赛跑道数">
          <el-input-number v-model="laneCount" :min="1" :max="12" />
        </el-form-item>
        <el-form-item>
          <el-button
            type="primary"
            :disabled="!selectedScheduleId || currentSchedule?.status !== 1 || entries.length > 0"
            :loading="assignLoading"
            @click="handleAutoAssign"
          >
            自动编排并生成检录号
          </el-button>
          <el-button :disabled="!selectedScheduleId" @click="loadEntries">刷新名单</el-button>
        </el-form-item>
      </el-form>

      <el-descriptions v-if="currentSchedule" :column="4" border class="schedule-summary">
        <el-descriptions-item label="赛事">{{ currentSchedule.eventName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="项目">{{ currentSchedule.sportItemName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="场次">{{ currentSchedule.scheduleName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="比赛时间">
          {{ currentSchedule.matchDate || '-' }} {{ currentSchedule.startTime || '' }}
        </el-descriptions-item>
        <el-descriptions-item label="比赛地点">{{ currentSchedule.location || '-' }}</el-descriptions-item>
        <el-descriptions-item label="场次状态">
          <el-tag :type="getScheduleStatusType(currentSchedule.status)">
            {{ getScheduleStatusName(currentSchedule.status) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="编排人数">{{ entries.length }}</el-descriptions-item>
        <el-descriptions-item label="检录规则">需由赛事工作人员现场确认</el-descriptions-item>
      </el-descriptions>

      <el-table :data="entries" v-loading="loading" border stripe class="entries-table">
        <el-table-column prop="checkinCode" label="检录编号" width="130" fixed />
        <el-table-column label="参赛者/队伍" min-width="150">
          <template #default="{ row }">
            <div class="participant-name">{{ row.participantName || row.teamName || '-' }}</div>
            <div v-if="row.studentNo" class="sub-text">学号：{{ row.studentNo }}</div>
          </template>
        </el-table-column>
        <el-table-column label="学院/班级" min-width="180">
          <template #default="{ row }">
            <div>{{ row.college || '-' }}</div>
            <div v-if="row.className" class="sub-text">{{ row.className }}</div>
          </template>
        </el-table-column>
        <el-table-column label="接力队员" min-width="180">
          <template #default="{ row }">{{ formatMembers(row) }}</template>
        </el-table-column>
        <el-table-column prop="groupNo" label="组别" width="90" />
        <el-table-column prop="laneNo" label="跑道" width="75">
          <template #default="{ row }">{{ row.laneNo ?? '-' }}</template>
        </el-table-column>
        <el-table-column prop="startOrder" label="出场顺序" width="95" />
        <el-table-column label="检录状态" width="110">
          <template #default="{ row }">
            <el-tag :type="getCheckinStatusType(row.checkinStatus)">
              {{ getCheckinStatusName(row.checkinStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="checkedAt" label="检录时间" width="165" />
        <el-table-column prop="checkinRemark" label="备注" min-width="150" show-overflow-tooltip />
        <el-table-column label="操作" width="190" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="currentSchedule?.status !== 3"
              type="success"
              link
              @click="updateCheckin(row, 'CHECKED_IN')"
            >已检录</el-button>
            <el-dropdown v-if="currentSchedule?.status !== 3" @command="status => updateCheckin(row, String(status))">
              <el-button type="primary" link>更多<el-icon class="el-icon--right"><ArrowDown /></el-icon></el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="ABSENT">缺席</el-dropdown-item>
                  <el-dropdown-item command="WITHDRAWN">弃权</el-dropdown-item>
                  <el-dropdown-item command="DQ">取消资格</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
            <el-button
              v-if="currentSchedule?.status === 1 && row.checkinStatus === 'PENDING'"
              type="danger"
              link
              @click="handleRemove(row)"
            >移除</el-button>
            <el-button
              v-if="currentSchedule?.status === 1"
              type="primary"
              link
              @click="handleEditAssignment(row)"
            >调整</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="selectedScheduleId && !loading && entries.length === 0" description="该场次尚未生成参赛编排名单" />
      <el-empty v-if="!selectedScheduleId" description="请先选择一个赛程" />
    </el-card>

    <el-dialog v-model="assignmentDialogVisible" title="调整竞赛编排" width="440px">
      <el-form :model="assignmentForm" label-width="100px">
        <el-form-item label="组别">
          <el-input v-model="assignmentForm.groupNo" maxlength="20" placeholder="如：A组" />
        </el-form-item>
        <el-form-item label="跑道号">
          <el-input-number v-model="assignmentForm.laneNo" :min="1" :max="12" />
        </el-form-item>
        <el-form-item label="出场顺序">
          <el-input-number v-model="assignmentForm.startOrder" :min="1" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="assignmentDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="assignmentLoading" @click="saveAssignment">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { del, get, post, put } from '@/utils/request'

interface ScheduleOption {
  id: number
  eventId: number
  eventName?: string
  sportItemId: number
  sportItemName?: string
  scheduleName?: string
  matchDate?: string
  startTime?: string
  location?: string
  status: number
}

const loading = ref(false)
const assignLoading = ref(false)
const assignmentLoading = ref(false)
const scheduleOptions = ref<ScheduleOption[]>([])
const selectedScheduleId = ref<number | null>(null)
const laneCount = ref(8)
const entries = ref<any[]>([])
const assignmentDialogVisible = ref(false)
const editingEntryId = ref<number | null>(null)
const assignmentForm = reactive({
  groupNo: '',
  laneNo: 1 as number | null,
  startOrder: 1
})

const currentSchedule = computed(() =>
  scheduleOptions.value.find(item => item.id === selectedScheduleId.value)
)

onMounted(loadSchedules)

async function loadSchedules() {
  const res = await get('/schedules', { current: 1, size: 100 })
  if (res.code === 200) {
    scheduleOptions.value = res.data?.records || []
  }
}

function formatSchedule(item: ScheduleOption) {
  return [item.eventName, item.sportItemName, item.scheduleName, item.matchDate, item.startTime]
    .filter(Boolean)
    .join(' · ')
}

async function loadEntries() {
  if (!selectedScheduleId.value) {
    entries.value = []
    return
  }
  loading.value = true
  try {
    const res = await get('/schedules/' + selectedScheduleId.value + '/entries')
    if (res.code === 200) {
      entries.value = res.data || []
    }
  } finally {
    loading.value = false
  }
}

async function handleAutoAssign() {
  if (!selectedScheduleId.value) return
  try {
    await ElMessageBox.confirm(
      '系统将把该项目所有审核通过的参赛者按报名顺序编排。径赛按设定跑道数自动分组；田赛按出场顺序排列。是否继续？',
      '自动编排确认',
      { type: 'warning', confirmButtonText: '开始编排', cancelButtonText: '取消' }
    )
    assignLoading.value = true
    const res = await post(
      '/schedules/' + selectedScheduleId.value + '/entries/auto-assign',
      undefined,
      { params: { laneCount: laneCount.value } }
    )
    ElMessage.success('已生成 ' + res.data + ' 条编排记录')
    await loadEntries()
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      // 请求拦截器负责显示接口错误。
    }
  } finally {
    assignLoading.value = false
  }
}

function handleEditAssignment(row: any) {
  editingEntryId.value = row.id
  Object.assign(assignmentForm, {
    groupNo: row.groupNo || '',
    laneNo: row.laneNo ?? 1,
    startOrder: row.startOrder || 1
  })
  assignmentDialogVisible.value = true
}

async function saveAssignment() {
  if (!editingEntryId.value) return
  assignmentLoading.value = true
  try {
    await put('/schedule-entries/' + editingEntryId.value + '/assignment', {
      groupNo: assignmentForm.groupNo,
      laneNo: assignmentForm.laneNo,
      startOrder: assignmentForm.startOrder
    })
    ElMessage.success('编排信息已更新')
    assignmentDialogVisible.value = false
    await loadEntries()
  } finally {
    assignmentLoading.value = false
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
    await post('/schedule-entries/' + row.id + '/check-in', {
      status,
      remark: row.checkinRemark || ''
    })
    ElMessage.success('检录状态已更新为：' + labels[status])
    await loadEntries()
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      // 请求拦截器负责显示接口错误。
    }
  }
}

async function handleRemove(row: any) {
  try {
    await ElMessageBox.confirm(
      '确定移除该参赛编排记录吗？此操作仅适用于比赛开始前且尚未检录的记录。',
      '移除确认',
      { type: 'warning' }
    )
    await del('/schedule-entries/' + row.id)
    ElMessage.success('已移除编排记录')
    await loadEntries()
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      // 请求拦截器负责显示接口错误。
    }
  }
}

function formatMembers(row: any) {
  return Array.isArray(row.memberNames) && row.memberNames.length ? row.memberNames.join('、') : '-'
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
.page-alert {
  margin-bottom: 16px;
}
.schedule-summary {
  margin: 8px 0 18px;
}
.entries-table {
  margin-top: 12px;
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
