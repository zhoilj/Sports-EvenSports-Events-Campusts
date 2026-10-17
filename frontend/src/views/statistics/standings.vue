<template>
  <div class="page-container">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <div>
            <strong>学院团体积分与奖牌榜</strong>
            <div class="subheading">统计正式发布的决赛成绩；按学校规程依次以总积分、破纪录项次、第一名至第八名数量排序。</div>
          </div>
          <el-button type="success" :disabled="!eventId" @click="exportStandings">导出积分榜 CSV</el-button>
        </div>
      </template>
      <el-form :inline="true">
        <el-form-item label="赛事">
          <el-select v-model="eventId" placeholder="选择赛事" filterable @change="loadAll">
            <el-option v-for="event in events" :key="event.id" :label="event.name" :value="event.id" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadAll" :disabled="!eventId">刷新</el-button>
        </el-form-item>
        <el-form-item label="排名类别">
          <el-select v-model="categoryFilter" placeholder="选择排名类别" style="width: 220px">
            <el-option v-for="category in categories" :key="category" :label="category" :value="category" />
          </el-select>
        </el-form-item>
      </el-form>
      <el-table :data="filteredStandings" v-loading="loading" border stripe>
        <el-table-column label="排名" width="80">
          <template #default="{ row }">
            <el-tag v-if="row.rank === 1" type="warning">第1名</el-tag>
            <el-tag v-else-if="row.rank === 2" type="info">第2名</el-tag>
            <el-tag v-else-if="row.rank === 3" type="success">第3名</el-tag>
            <span v-else>第{{ row.rank }}名</span>
          </template>
        </el-table-column>
        <el-table-column prop="college" :label="categoryFilter === '乙组年级专业' ? '年级/专业班级' : '学院/参赛单位'" min-width="220" />
        <el-table-column prop="points" label="总积分" width="110" sortable />
        <el-table-column prop="gold" label="金牌" width="90" sortable />
        <el-table-column prop="silver" label="银牌" width="90" sortable />
        <el-table-column prop="bronze" label="铜牌" width="90" sortable />
        <el-table-column prop="recordCount" label="破纪录项次" width="110" />
        <el-table-column prop="resultCount" label="决赛成绩数" width="130" />
      </el-table>

      <el-divider />
      <div class="card-header rule-header">
        <div>
          <strong>名次积分规则</strong>
          <div class="subheading">由管理员配置。正式发布成绩时按名次自动计算积分；同名次选手使用相同名次对应的积分。</div>
        </div>
        <div class="rule-actions">
          <el-button v-if="canEditRules" @click="applyOfficialRules">套用正式规程积分</el-button>
          <el-button v-if="canEditRules" @click="addRule">添加名次</el-button>
          <el-button v-if="canEditRules" type="primary" :loading="savingRules" @click="saveRules" :disabled="!eventId">保存规则</el-button>
        </div>
      </div>
      <el-table :data="rules" border>
        <el-table-column label="名次" width="150">
          <template #default="{ row }">
            <el-input-number v-model="row.placeNo" :min="1" :max="100" :disabled="!canEditRules" />
          </template>
        </el-table-column>
        <el-table-column label="积分" width="180">
          <template #default="{ row }">
            <el-input-number v-model="row.points" :min="0" :max="1000" :disabled="!canEditRules" />
          </template>
        </el-table-column>
        <el-table-column label="说明">
          <template #default="{ row }">
            <el-input v-model="row.ruleName" placeholder="例如：第一名积分" :disabled="!canEditRules" />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100">
          <template #default="{ $index }">
            <el-button v-if="canEditRules" link type="danger" @click="removeRule($index)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="rules.length === 0" description="尚未配置名次积分规则。请先根据本届竞赛规程设置，再发布成绩。" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { get, put } from '@/utils/request'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const canEditRules = computed(() => userStore.userInfo?.role === 'ADMIN')
const events = ref<any[]>([])
const eventId = ref<number | null>(null)
const standings = ref<any[]>([])
const categoryFilter = ref('甲组男女团体')
const categories = computed(() => Array.from(new Set(standings.value.map(row => row.category as string))))
const filteredStandings = computed(() => standings.value.filter(row => !categoryFilter.value || row.category === categoryFilter.value))
const rules = ref<any[]>([])
const loading = ref(false)
const savingRules = ref(false)

onMounted(async () => {
  const res = await get('/events', { current: 1, size: 100 })
  if (res.code === 200) {
    events.value = res.data.records || []
    if (events.value.length) {
      eventId.value = events.value[0].id
      await loadAll()
    }
  }
})

async function loadAll() {
  if (!eventId.value) return
  loading.value = true
  try {
    const [standingRes, ruleRes] = await Promise.all([
      get('/reports/standings', { eventId: eventId.value }),
      get('/scoring-rules', { eventId: eventId.value })
    ])
    if (standingRes.code === 200) {
      standings.value = standingRes.data || []
      if (!categories.value.includes(categoryFilter.value) && categories.value.length) categoryFilter.value = categories.value[0]
    }
    if (ruleRes.code === 200) rules.value = (ruleRes.data || []).map((item: any) => ({
      placeNo: item.placeNo,
      points: item.points,
      ruleName: item.ruleName || ''
    }))
  } finally {
    loading.value = false
  }
}

async function applyOfficialRules() {
  await ElMessageBox.confirm('将把名次积分设置为第1至第8名依次9、7、6、5、4、3、2、1分。接力和集体项目的倍率及参与分需在项目管理中另行配置。确认套用？', '正式规程积分模板', { type: 'warning' })
  rules.value = [9, 7, 6, 5, 4, 3, 2, 1].map((points, index) => ({
    placeNo: index + 1,
    points,
    ruleName: `第${index + 1}名`
  }))
}

function addRule() {
  const nextPlace = rules.value.length ? Math.max(...rules.value.map(r => Number(r.placeNo) || 0)) + 1 : 1
  rules.value.push({ placeNo: nextPlace, points: 0, ruleName: '' })
}

function removeRule(index: number) {
  rules.value.splice(index, 1)
}

async function saveRules() {
  if (!eventId.value) return
  if (!rules.value.length) {
    ElMessage.warning('至少保留一条积分规则')
    return
  }
  const places = rules.value.map(r => Number(r.placeNo))
  if (places.some(p => !Number.isInteger(p) || p < 1) || new Set(places).size !== places.length) {
    ElMessage.warning('名次必须为正整数，且不能重复')
    return
  }
  if (rules.value.some(r => !Number.isInteger(Number(r.points)) || Number(r.points) < 0)) {
    ElMessage.warning('积分必须为非负整数')
    return
  }
  await ElMessageBox.confirm('保存后，新发布的成绩将按这套规则计算积分。已发布成绩不会自动重算。', '保存积分规则', { type: 'warning' })
  savingRules.value = true
  try {
    await put(`/scoring-rules?eventId=${eventId.value}`, rules.value)
    ElMessage.success('积分规则已保存')
    await loadAll()
  } finally {
    savingRules.value = false
  }
}

async function exportStandings() {
  if (!eventId.value) return
  const res = await get('/reports/standings.csv', { eventId: eventId.value })
  const blob = new Blob([res.data || ''], { type: 'text/csv;charset=utf-8;' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = `学院积分与奖牌榜-${eventId.value}.csv`
  link.click()
  URL.revokeObjectURL(url)
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
  margin-top: 6px;
  color: var(--el-text-color-secondary);
  font-size: 12px;
}
.rule-header {
  margin-bottom: 14px;
}
.rule-actions {
  display: flex;
  gap: 8px;
}
</style>
