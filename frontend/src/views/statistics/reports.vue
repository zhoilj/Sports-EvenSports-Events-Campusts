<template>
  <div class="page-container">
    <el-alert
      title="秩序册、成绩与学院团体排名"
      description="秩序册导出包含赛程、组别、道次/出场顺序、检录编号、参赛者及场地信息；成绩导出仅包含正式发布的成绩；学院团体排名按正式发布的有效决赛成绩汇总。"
      type="info"
      :closable="false"
      show-icon
      class="page-alert"
    />
    <el-card shadow="never">
      <el-form :inline="true" class="search-form">
        <el-form-item label="赛事">
          <el-select v-model="selectedEventId" placeholder="请选择赛事" filterable clearable style="width: 340px" @change="loadStandings">
            <el-option v-for="item in eventOptions" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :disabled="!selectedEventId" :loading="loading" @click="loadStandings">查询团体排名</el-button>
          <el-button :disabled="!selectedEventId" :loading="exporting" @click="exportFile('order-book.csv')">导出秩序册</el-button>
          <el-button :disabled="!selectedEventId" :loading="exporting" @click="exportFile('results.csv')">导出正式成绩</el-button>
          <el-button :disabled="!selectedEventId" :loading="exporting" @click="exportFile('standings.csv')">导出学院积分</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="standings" v-loading="loading" border stripe>
        <el-table-column type="index" label="排名" width="80" />
        <el-table-column prop="college" label="学院" min-width="220" />
        <el-table-column prop="points" label="总积分" width="110" sortable />
        <el-table-column prop="gold" label="金牌" width="90" />
        <el-table-column prop="silver" label="银牌" width="90" />
        <el-table-column prop="bronze" label="铜牌" width="90" />
        <el-table-column prop="resultCount" label="有效成绩数" width="120" />
      </el-table>
      <el-empty v-if="selectedEventId && !loading && standings.length === 0" description="当前赛事暂无正式发布的有效决赛成绩" />
      <el-empty v-if="!selectedEventId" description="请选择赛事查看学院团体排名或导出文件" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { get } from '@/utils/request'

const eventOptions = ref<any[]>([])
const selectedEventId = ref<number | null>(null)
const standings = ref<any[]>([])
const loading = ref(false)
const exporting = ref(false)

onMounted(loadEvents)

async function loadEvents() {
  const res = await get('/events', { size: 100 })
  if (res.code === 200) {
    eventOptions.value = res.data?.records || []
    if (eventOptions.value.length && !selectedEventId.value) {
      selectedEventId.value = eventOptions.value[0].id
      await loadStandings()
    }
  }
}

async function loadStandings() {
  if (!selectedEventId.value) {
    standings.value = []
    return
  }
  loading.value = true
  try {
    const res = await get('/reports/standings', { eventId: selectedEventId.value })
    if (res.code === 200) standings.value = res.data || []
  } finally {
    loading.value = false
  }
}

async function exportFile(fileType: 'order-book.csv' | 'results.csv' | 'standings.csv') {
  if (!selectedEventId.value) return
  exporting.value = true
  try {
    const res = await get('/reports/' + fileType, { eventId: selectedEventId.value })
    if (res.code !== 200 || typeof res.data !== 'string') {
      ElMessage.warning('导出失败，接口未返回有效的CSV内容')
      return
    }
    const blob = new Blob([res.data], { type: 'text/csv;charset=utf-8;' })
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = fileType.replace('.csv', '') + '-' + selectedEventId.value + '.csv'
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    URL.revokeObjectURL(url)
    ElMessage.success('文件已生成')
  } finally {
    exporting.value = false
  }
}
</script>

<style scoped>
.page-alert {
  margin-bottom: 16px;
}
</style>
