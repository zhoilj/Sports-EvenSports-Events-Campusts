<template>
  <div class="dashboard-container">
    <section class="dashboard-hero">
      <div class="hero-copy">
        <div class="hero-eyebrow">CAMPUS SPORTS · MEET OPERATIONS</div>
        <h1>让每一场比赛有序运行</h1>
        <p>从赛事设置、报名审核到竞赛编排、现场检录与成绩发布，统一管理校运会全流程。</p>
        <div class="hero-actions">
          <el-button type="primary" @click="router.push('/orderbook')"><el-icon><Document /></el-icon> 进入秩序册编排</el-button>
          <el-button plain @click="router.push('/competition-operations')"><el-icon><Flag /></el-icon> 竞赛执行工作台</el-button>
        </div>
      </div>
      <div class="hero-visual" aria-hidden="true">
        <div class="track-ring ring-one"></div><div class="track-ring ring-two"></div><div class="track-ring ring-three"></div>
        <div class="hero-trophy"><el-icon><Trophy /></el-icon></div>
        <span class="hero-year">2026</span>
      </div>
    </section>

    <section class="workflow-shortcuts">
      <div class="workflow-heading"><strong>赛事工作流程</strong><span>建议按业务顺序完成各项准备</span></div>
      <el-row :gutter="12">
        <el-col :xs="24" :sm="12" :lg="6">
          <button class="workflow-step" @click="router.push('/events')"><span class="workflow-number">01</span><span class="workflow-copy"><strong>赛事与项目</strong><small>届次、规则与比赛项目</small></span><el-icon><ArrowRight /></el-icon></button>
        </el-col>
        <el-col :xs="24" :sm="12" :lg="6">
          <button class="workflow-step" @click="router.push('/registrations')"><span class="workflow-number">02</span><span class="workflow-copy"><strong>报名审核</strong><small>资格校验与接力名单</small></span><el-icon><ArrowRight /></el-icon></button>
        </el-col>
        <el-col :xs="24" :sm="12" :lg="6">
          <button class="workflow-step" @click="router.push('/schedules')"><span class="workflow-number">03</span><span class="workflow-copy"><strong>赛程编排</strong><small>时间、场地与轮次</small></span><el-icon><ArrowRight /></el-icon></button>
        </el-col>
        <el-col :xs="24" :sm="12" :lg="6">
          <button class="workflow-step" @click="router.push('/orderbook')"><span class="workflow-number">04</span><span class="workflow-copy"><strong>秩序册生成</strong><small>导出并核对正式文件</small></span><el-icon><ArrowRight /></el-icon></button>
        </el-col>
      </el-row>
    </section>
    <!-- 统计卡片 -->
    <el-row :gutter="20" class="stat-cards">
      <el-col :xs="24" :sm="12" :lg="6">
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
      <el-col :xs="24" :lg="12">
        <el-card shadow="hover">
          <template #header>
            <span>赛事报名人数 Top10</span>
          </template>
          <div ref="registrationChartRef" class="chart-container"></div>
        </el-card>
      </el-col>
      <el-col :xs="24" :lg="12">
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
  .dashboard-hero {
    min-height: 205px;
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 24px;
    padding: 26px 32px;
    margin-bottom: 18px;
    overflow: hidden;
    position: relative;
    border-radius: 16px;
    background: linear-gradient(112deg, #102b25 0%, #1a4c3b 62%, #276c50 100%);
    color: #f5faf6;
    box-shadow: 0 12px 28px rgba(16, 43, 37, .12);
  }
  .hero-copy { position: relative; z-index: 1; max-width: 710px; }
  .hero-eyebrow { color: #d8f36a; font-size: 10px; font-weight: 800; letter-spacing: 1.7px; }
  .hero-copy h1 { margin: 12px 0 8px; color: #fff; font-size: 25px; font-weight: 800; letter-spacing: -.4px; }
  .hero-copy p { max-width: 630px; margin: 0; color: #c0d7cb; font-size: 12px; line-height: 1.8; }
  .hero-actions { display: flex; flex-wrap: wrap; gap: 9px; margin-top: 18px; }
  .hero-actions .el-button--primary { --el-button-bg-color: #d8f36a; --el-button-border-color: #d8f36a; --el-button-text-color: #18392e; --el-button-hover-bg-color: #e6ff8b; }
  .hero-actions .el-button.is-plain { background: transparent; border-color: #6c9882; color: #e9f4ed; }
  .hero-visual { width: 210px; height: 155px; flex: 0 0 auto; position: relative; display: grid; place-items: center; opacity: .95; }
  .track-ring { position: absolute; border: 1px solid rgba(216,243,106,.36); border-radius: 50%; transform: rotate(-24deg); }
  .ring-one { width: 205px; height: 92px; }
  .ring-two { width: 170px; height: 72px; }
  .ring-three { width: 130px; height: 52px; }
  .hero-trophy { display: grid; place-items: center; width: 62px; height: 62px; border-radius: 20px; background: #d8f36a; color: #17392e; font-size: 32px; box-shadow: 0 8px 26px rgba(0,0,0,.18); }
  .hero-year { position: absolute; right: 2px; bottom: 0; color: rgba(255,255,255,.25); font-size: 31px; font-weight: 900; letter-spacing: 2px; }
  .workflow-shortcuts { margin-bottom: 20px; }
  .workflow-heading { display: flex; align-items: baseline; gap: 11px; margin: 0 0 11px 2px; }
  .workflow-heading strong { color: #253d32; font-size: 14px; }
  .workflow-heading span { color: #8b9891; font-size: 10px; }
  .workflow-step { width: 100%; display: flex; align-items: center; gap: 11px; padding: 14px 13px; border: 1px solid #e3ebe6; border-radius: 12px; background: #fff; text-align: left; cursor: pointer; transition: border-color .18s, transform .18s, box-shadow .18s; }
  .workflow-step:hover { border-color: #b7d8c5; box-shadow: 0 7px 18px rgba(27,86,61,.07); transform: translateY(-1px); }
  .workflow-number { display: grid; place-items: center; width: 32px; height: 32px; flex: 0 0 auto; border-radius: 10px; background: #e7f5ed; color: #1d7857; font-size: 11px; font-weight: 850; }
  .workflow-copy { display: flex; flex: 1; flex-direction: column; gap: 5px; min-width: 0; }
  .workflow-copy strong { color: #263e32; font-size: 12px; }
  .workflow-copy small { color: #89968f; font-size: 10px; }
  .workflow-step > .el-icon { color: #96a69c; }

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
        color: #1f7959;
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