<template>
  <div class="page-container">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>我的赛程与检录编号</span>
          <el-button :loading="loading" @click="loadEntries">刷新</el-button>
        </div>
      </template>
      <el-alert
        title="请在比赛前核对项目、比赛时间、分组和检录编号，并按赛事通知到场检录。"
        type="info"
        :closable="false"
        show-icon
        class="page-alert"
      />
      <el-table :data="entries" v-loading="loading" border stripe>
        <el-table-column prop="checkinCode" label="检录编号" width="140" fixed>
          <template #default="{ row }">
            <span class="checkin-code">{{ row.checkinCode }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="eventName" label="赛事" min-width="170" />
        <el-table-column prop="sportItemName" label="竞赛项目" min-width="130" />
        <el-table-column prop="scheduleName" label="场次/轮次" min-width="120" />
        <el-table-column label="比赛时间" min-width="160">
          <template #default="{ row }">{{ row.matchDate || '-' }} {{ row.startTime || '' }}</template>
        </el-table-column>
        <el-table-column prop="location" label="比赛地点" min-width="130" />
        <el-table-column prop="groupNo" label="分组" width="85" />
        <el-table-column prop="laneNo" label="跑道" width="75">
          <template #default="{ row }">{{ row.laneNo ?? '-' }}</template>
        </el-table-column>
        <el-table-column prop="startOrder" label="出场顺序" width="95" />
        <el-table-column label="检录状态" width="110">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.checkinStatus)">{{ getStatusName(row.checkinStatus) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="checkinRemark" label="检录备注" min-width="160" show-overflow-tooltip />
      </el-table>
      <el-empty v-if="!loading && entries.length === 0" description="暂未生成你的赛程与检录编号，请关注报名审核和赛事编排公告" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { get } from '@/utils/request'

const loading = ref(false)
const entries = ref<any[]>([])

onMounted(loadEntries)

async function loadEntries() {
  loading.value = true
  try {
    const res = await get('/schedule-entries/my')
    if (res.code === 200) {
      entries.value = res.data || []
    }
  } finally {
    loading.value = false
  }
}

function getStatusName(status: string) {
  return ({
    PENDING: '待检录',
    CHECKED_IN: '已检录',
    ABSENT: '缺席',
    WITHDRAWN: '弃权',
    DQ: '取消资格'
  } as Record<string, string>)[status] || status || '待检录'
}

function getStatusType(status: string): 'success' | 'warning' | 'info' | 'primary' | 'danger' {
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
.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-weight: 600;
}
.page-alert {
  margin-bottom: 16px;
}
.checkin-code {
  font-weight: 700;
  letter-spacing: 0.5px;
  color: var(--el-color-primary);
}
</style>
