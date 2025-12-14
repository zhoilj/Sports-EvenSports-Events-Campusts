<template>
  <div class="dashboard-container">
    <!-- 统计卡片 -->
    <el-row :gutter="20" class="stat-cards">
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-icon" style="background-color: #409eff">
            <el-icon><Trophy /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ stats.totalEvents }}</div>
            <div class="stat-label">赛事总数</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-icon" style="background-color: #67c23a">
            <el-icon><VideoPlay /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ stats.ongoingEvents }}</div>
            <div class="stat-label">进行中赛事</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-icon" style="background-color: #e6a23c">
            <el-icon><Document /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ stats.totalRegistrations }}</div>
            <div class="stat-label">报名总数</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-icon" style="background-color: #f56c6c">
            <el-icon><Clock /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ stats.pendingRegistrations }}</div>
            <div class="stat-label">待审核报名</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 图表区域 -->
    <el-row :gutter="20" class="chart-row">
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header>
            <span>赛事报名人数 Top10</span>
          </template>
          <div ref="registrationChartRef" class="chart-container"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header>
            <span>赛事状态分布</span>
          </template>
          <div ref="statusChartRef" class="chart-container"></div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 快捷入口 -->
    <el-row :gutter="20" class="quick-entry">
      <el-col :span="24">
        <el-card shadow="hover">
          <template #header>
            <span>快捷入口</span>
          </template>
          <div class="entry-list">
            <div class="entry-item" @click="router.push('/events')">
              <el-icon><Trophy /></el-icon>
              <span>赛事管理</span>
            </div>
            <div class="entry-item" @click="router.push('/registrations')">
              <el-icon><Document /></el-icon>
              <span>报名管理</span>
            </div>
            <div class="entry-item" @click="router.push('/schedules')">
              <el-icon><Calendar /></el-icon>
              <span>赛程安排</span>
            </div>
            <div class="entry-item" @click="router.push('/results')">
              <el-icon><Medal /></el-icon>
              <span>成绩管理</span>
            </div>
            <div class="entry-item" @click="router.push('/statistics')">
              <el-icon><DataAnalysis /></el-icon>
              <span>统计分析</span>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import * as echarts from 'echarts'
import { get } from '@/utils/request'

const router = useRouter()

const stats = ref({
  totalEvents: 0,
  ongoingEvents: 0,
  totalRegistrations: 0,
  pendingRegistrations: 0,
  totalResults: 0
})

const registrationChartRef = ref<HTMLElement>()
const statusChartRef = ref<HTMLElement>()
let registrationChart: echarts.ECharts | null = null
let statusChart: echarts.ECharts | null = null

onMounted(async () => {
  await loadStats()
  await loadCharts()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  registrationChart?.dispose()
  statusChart?.dispose()
})

function handleResize() {
  registrationChart?.resize()
  statusChart?.resize()
}

async function loadStats() {
  try {
    const res = await get('/statistics/dashboard')
    if (res.code === 200) {
      stats.value = res.data
    }
  } catch (error) {
    console.error('加载统计数据失败', error)
  }
}

async function loadCharts() {
  // 报名人数 Top10 图表
  if (registrationChartRef.value) {
    registrationChart = echarts.init(registrationChartRef.value)
    try {
      const res = await get('/statistics/registration-top')
      if (res.code === 200) {
        const data = res.data || []
        registrationChart.setOption({
          tooltip: { trigger: 'axis' },
          xAxis: {
            type: 'category',
            data: data.map((item: any) => item.eventName),
            axisLabel: {
              rotate: 30,
              interval: 0
            }
          },
          yAxis: { type: 'value' },
          series: [{
            type: 'bar',
            data: data.map((item: any) => item.count),
            itemStyle: {
              color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                { offset: 0, color: '#409eff' },
                { offset: 1, color: '#67c23a' }
              ])
            }
          }]
        })
      }
    } catch (error) {
      console.error('加载报名统计失败', error)
    }
  }

  // 赛事状态分布图表
  if (statusChartRef.value) {
    statusChart = echarts.init(statusChartRef.value)
    try {
      const res = await get('/statistics/event-status')
      if (res.code === 200) {
        const data = res.data || []
        statusChart.setOption({
          tooltip: { trigger: 'item' },
          legend: { bottom: 0 },
          series: [{
            type: 'pie',
            radius: ['40%', '70%'],
            data: data.map((item: any) => ({
              name: item.statusName,
              value: item.count
            })),
            emphasis: {
              itemStyle: {
                shadowBlur: 10,
                shadowOffsetX: 0,
                shadowColor: 'rgba(0, 0, 0, 0.5)'
              }
            }
          }]
        })
      }
    } catch (error) {
      console.error('加载状态统计失败', error)
    }
  }
}
</script>

<style lang="scss" scoped>
.dashboard-container {
  .stat-cards {
    margin-bottom: 20px;
  }

  .stat-card {
    :deep(.el-card__body) {
      display: flex;
      align-items: center;
      padding: 20px;
    }

    .stat-icon {
      width: 60px;
      height: 60px;
      border-radius: 8px;
      display: flex;
      align-items: center;
      justify-content: center;
      margin-right: 20px;

      .el-icon {
        font-size: 28px;
        color: #fff;
      }
    }

    .stat-info {
      .stat-value {
        font-size: 28px;
        font-weight: bold;
        color: #333;
      }

      .stat-label {
        font-size: 14px;
        color: #999;
        margin-top: 5px;
      }
    }
  }

  .chart-row {
    margin-bottom: 20px;
  }

  .chart-container {
    height: 300px;
  }

  .quick-entry {
    .entry-list {
      display: flex;
      justify-content: space-around;
    }

    .entry-item {
      display: flex;
      flex-direction: column;
      align-items: center;
      padding: 20px 40px;
      cursor: pointer;
      border-radius: 8px;
      transition: all 0.3s;

      &:hover {
        background-color: #f5f7fa;
      }

      .el-icon {
        font-size: 32px;
        color: #409eff;
        margin-bottom: 10px;
      }

      span {
        font-size: 14px;
        color: #666;
      }
    }
  }
}
</style>