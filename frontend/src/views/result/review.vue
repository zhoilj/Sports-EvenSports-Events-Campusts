<template>
  <div class="page-container">
    <el-alert
      title="成绩复核与正式发布"
      description="成绩录入后先由赛事工作人员复核。管理员只能在赛程结束且所有参赛者均有复核通过的成绩后，正式发布该场成绩。正式发布后，成绩不能直接修改或删除。"
      type="info"
      :closable="false"
      show-icon
      class="page-alert"
    />
    <el-card shadow="never">
      <el-form :inline="true" class="search-form">
        <el-form-item label="赛事">
          <el-select v-model="selectedEventId" placeholder="请选择赛事" filterable clearable style="width: 320px" @change="handleEventChange">
            <el-option v-for="item in eventOptions" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :disabled="!selectedEventId" :loading="loading" @click="loadResults">查询成绩</el-button>
        </el-form-item>
        <el-form-item v-if="isAdmin" label="发布场次">
          <el-select v-model="publishScheduleId" placeholder="选择已结束场次" filterable clearable style="width: 340px">
            <el-option v-for="item in completedSchedules" :key="item.id" :label="formatSchedule(item)" :value="item.id" />
          </el-select>
          <el-button type="success" :disabled="!publishScheduleId" :loading="publishLoading" @click="publishResults">正式发布</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="tableData" v-loading="loading" border stripe>
        <el-table-column prop="eventName" label="赛事" min-width="150" />
        <el-table-column prop="sportItemName" label="项目" min-width="130" />
        <el-table-column label="参赛者/队伍" min-width="160">
          <template #default="{ row }">{{ row.relayTeamName || row.realName || row.username || '-' }}</template>
        </el-table-column>
        <el-table-column prop="studentNo" label="学号" width="120" />
        <el-table-column label="成绩" width="140">
          <template #default="{ row }">{{ row.scoreText || row.score || '-' }}</template>
        </el-table-column>
        <el-table-column prop="resultStatus" label="成绩状态" width="110" />
        <el-table-column prop="ranking" label="名次" width="80" />
        <el-table-column label="复核状态" width="120">
          <template #default="{ row }">
            <el-tag :type="getReviewType(row.reviewStatus)">{{ getReviewName(row.reviewStatus) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="发布状态" width="110">
          <template #default="{ row }">
            <el-tag :type="row.isPublished === 1 ? 'success' : 'info'">{{ row.isPublished === 1 ? '已发布' : '未发布' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="reviewRemark" label="复核意见" min-width="180" show-overflow-tooltip />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <template v-if="row.isPublished !== 1">
              <el-button type="success" link :disabled="row.reviewStatus === 2" @click="reviewResult(row, 2)">通过</el-button>
              <el-button type="warning" link @click="reviewResult(row, 3)">退回</el-button>
            </template>
            <span v-else class="sub-text">正式成绩</span>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination
        class="mt-20"
        v-model:current-page="current"
        v-model:page-size="size"
        :page-sizes="[10, 20, 50, 100]"
        :total="total"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="loadResults"
        @current-change="loadResults"
      />
      <el-empty v-if="selectedEventId && !loading && tableData.length === 0" description="该赛事暂无成绩记录" />
      <el-empty v-if="!selectedEventId" description="请选择赛事查看成绩复核状态" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { get, post } from '@/utils/request'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const isAdmin = computed(() => userStore.userInfo?.role === 'ADMIN')
const eventOptions = ref<any[]>([])
const scheduleOptions = ref<any[]>([])
const selectedEventId = ref<number | null>(null)
const publishScheduleId = ref<number | null>(null)
const tableData = ref<any[]>([])
const current = ref(1)
const size = ref(20)
const total = ref(0)
const loading = ref(false)
const publishLoading = ref(false)

const completedSchedules = computed(() => scheduleOptions.value.filter(item => item.status === 3))

onMounted(loadEvents)

async function loadEvents() {
  const res = await get('/events', { size: 100 })
  if (res.code === 200) {
    eventOptions.value = res.data?.records || []
    if (eventOptions.value.length && !selectedEventId.value) {
      selectedEventId.value = eventOptions.value[0].id
      await handleEventChange()
    }
  }
}

async function handleEventChange() {
  current.value = 1
  publishScheduleId.value = null
  if (!selectedEventId.value) {
    scheduleOptions.value = []
    tableData.value = []
    total.value = 0
    return
  }
  const res = await get('/schedules', { current: 1, size: 100, eventId: selectedEventId.value })
  if (res.code === 200) {
    scheduleOptions.value = res.data?.records || []
  }
  await loadResults()
}

async function loadResults() {
  if (!selectedEventId.value) return
  loading.value = true
  try {
    const res = await get('/results', {
      current: current.value,
      size: size.value,
      eventId: selectedEventId.value
    })
    if (res.code === 200) {
      tableData.value = res.data?.records || []
      total.value = res.data?.total || 0
    }
  } finally {
    loading.value = false
  }
}

function formatSchedule(item: any) {
  return [item.sportItemName, item.scheduleName, item.matchDate, item.startTime].filter(Boolean).join(' · ')
}

async function reviewResult(row: any, status: number) {
  let remark = ''
  try {
    if (status === 3) {
      const response = await ElMessageBox.prompt('请填写退回原因，便于成绩录入人员修改。', '退回成绩', {
        confirmButtonText: '确认退回',
        cancelButtonText: '取消',
        inputPlaceholder: '请输入复核意见',
        inputValidator: value => Boolean(value && value.trim()) || '退回时必须填写原因'
      })
      remark = response.value
    } else {
      await ElMessageBox.confirm('确认该成绩准确无误并通过复核吗？', '成绩复核', { type: 'warning' })
    }
    await post('/results/' + row.id + '/review', { status, remark })
    ElMessage.success(status === 2 ? '成绩复核通过' : '成绩已退回修改')
    await loadResults()
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      // 接口错误由请求拦截器统一显示。
    }
  }
}

async function publishResults() {
  if (!publishScheduleId.value) return
  try {
    await ElMessageBox.confirm(
      '发布后该场成绩将成为正式成绩，相关记录不能直接修改或删除。请确认该场所有成绩均已复核通过。',
      '正式发布成绩',
      { type: 'warning' }
    )
    publishLoading.value = true
    const res = await post('/results/schedules/' + publishScheduleId.value + '/publish')
    ElMessage.success(res.message || '成绩已正式发布')
    await loadResults()
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      // 接口错误由请求拦截器统一显示。
    }
  } finally {
    publishLoading.value = false
  }
}

function getReviewName(status: number | null) {
  return ({ 1: '待复核', 2: '已通过', 3: '退回修改' } as Record<number, string>)[Number(status)] || '待复核'
}

function getReviewType(status: number | null): 'success' | 'warning' | 'info' | 'primary' | 'danger' {
  return ({ 1: 'info', 2: 'success', 3: 'warning' } as Record<number, 'success' | 'warning' | 'info' | 'primary' | 'danger'>)[Number(status)] || 'info'
}
</script>

<style scoped>
.page-alert {
  margin-bottom: 16px;
}
.sub-text {
  color: var(--el-text-color-secondary);
  font-size: 12px;
}
</style>
