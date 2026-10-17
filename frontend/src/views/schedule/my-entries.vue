<template>
  <div class="page-container">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <div>
            <strong>我的赛程与检录编号</strong>
            <div class="subheading">仅显示本人已审核通过并完成赛程编排的项目，正式发布成绩后同步显示名次。</div>
          </div>
          <el-button :loading="loading" @click="loadEntries">刷新</el-button>
        </div>
      </template>
      <el-alert
        title="请按公布的比赛时间提前到达指定场地，并出示检录编号。检录状态和成绩以赛事工作人员登记及正式发布信息为准。"
        type="info"
        :closable="false"
        show-icon
        class="page-alert"
      />
      <el-table :data="entries" v-loading="loading" border stripe>
        <el-table-column prop="checkinCode" label="检录编号" width="140" fixed>
          <template #default="{ row }"><span class="checkin-code">{{ row.checkinCode || '—' }}</span></template>
        </el-table-column>
        <el-table-column prop="eventName" label="赛事" min-width="160" />
        <el-table-column prop="sportItemName" label="竞赛项目" min-width="130" />
        <el-table-column prop="scheduleName" label="场次/轮次" min-width="130" />
        <el-table-column label="比赛时间" min-width="165">
          <template #default="{ row }">{{ row.matchDate || '—' }} {{ row.startTime || '' }}</template>
        </el-table-column>
        <el-table-column prop="location" label="比赛地点" min-width="130" />
        <el-table-column prop="groupNo" label="组别" width="85" />
        <el-table-column prop="laneNo" label="道次" width="75">
          <template #default="{ row }">{{ row.laneNo ?? '—' }}</template>
        </el-table-column>
        <el-table-column prop="startOrder" label="出场顺序" width="95" />
        <el-table-column label="检录状态" width="110">
          <template #default="{ row }">
            <el-tag :type="statusType(row.checkinStatus)">{{ statusName(row.checkinStatus) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="checkinRemark" label="检录备注" min-width="150" show-overflow-tooltip />
        <el-table-column label="正式成绩" min-width="140">
          <template #default="{ row }">
            <template v-if="row.published">
              {{ row.scoreText || row.score || '—' }}
              <span v-if="row.ranking">（第{{ row.ranking }}名）</span>
            </template>
            <el-tag v-else type="info">尚未发布</el-tag>
          </template>
        </el-table-column>
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
    const res = await get('/checkins/my')
    if (res.code === 200) entries.value = res.data || []
  } finally {
    loading.value = false
  }
}

function statusName(status: string) {
  return ({
    PENDING: '待检录',
    CHECKED_IN: '已检录',
    ABSENT: '缺席',
    WITHDRAWN: '弃权',
    DQ: '取消资格'
  } as Record<string, string>)[status] || status || '待检录'
}

function statusType(status: string): 'success' | 'warning' | 'info' | 'primary' | 'danger' {
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
}
.subheading {
  margin-top: 5px;
  color: var(--el-text-color-secondary);
  font-size: 12px;
  font-weight: normal;
}
.page-alert {
  margin-bottom: 16px;
}
.checkin-code {
  color: var(--el-color-primary);
  font-weight: 700;
  letter-spacing: 0.5px;
}
</style>
