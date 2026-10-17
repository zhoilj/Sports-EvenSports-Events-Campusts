<template>
  <div class="page-container">
    <el-alert
      title="学院团体积分规则"
      description="按赛事配置名次积分。正式发布成绩时，系统会依据已审核通过的最终成绩和本规则计算团体积分。修改规则仅影响之后发布的成绩。"
      type="info"
      :closable="false"
      show-icon
      class="page-alert"
    />
    <el-card shadow="never">
      <el-form :inline="true" class="search-form">
        <el-form-item label="赛事">
          <el-select v-model="selectedEventId" placeholder="请选择赛事" filterable clearable style="width: 320px" @change="loadRules">
            <el-option v-for="item in eventOptions" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button :disabled="!selectedEventId" :loading="loading" @click="loadRules">刷新规则</el-button>
          <el-button v-if="isAdmin" type="primary" :disabled="!selectedEventId" @click="addRule">新增名次</el-button>
          <el-button v-if="isAdmin" type="success" :disabled="!selectedEventId || rules.length === 0" :loading="saveLoading" @click="saveRules">保存积分规则</el-button>
        </el-form-item>
      </el-form>

      <el-alert
        v-if="!isAdmin && selectedEventId"
        title="当前为只读模式，只有管理员可以修改积分规则。"
        type="info"
        :closable="false"
        class="read-only-alert"
      />

      <el-table :data="rules" v-loading="loading" border stripe>
        <el-table-column label="名次" width="140">
          <template #default="{ row }">
            <el-input-number v-model="row.placeNo" :min="1" :max="100" :disabled="!isAdmin" />
          </template>
        </el-table-column>
        <el-table-column label="积分" width="160">
          <template #default="{ row }">
            <el-input-number v-model="row.points" :min="0" :max="1000" :disabled="!isAdmin" />
          </template>
        </el-table-column>
        <el-table-column label="规则说明" min-width="240">
          <template #default="{ row }">
            <el-input v-model="row.ruleName" maxlength="100" :disabled="!isAdmin" placeholder="如：第一名积分" />
          </template>
        </el-table-column>
        <el-table-column v-if="isAdmin" label="操作" width="100" fixed="right">
          <template #default="{ $index }">
            <el-button type="danger" link @click="removeRule($index)">移除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="selectedEventId && !loading && rules.length === 0" description="该赛事尚未配置积分规则，请新增名次后保存" />
      <el-empty v-if="!selectedEventId" description="请选择赛事查看积分规则" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { get, put } from '@/utils/request'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const isAdmin = computed(() => userStore.userInfo?.role === 'ADMIN')
const eventOptions = ref<any[]>([])
const selectedEventId = ref<number | null>(null)
const rules = ref<any[]>([])
const loading = ref(false)
const saveLoading = ref(false)

onMounted(loadEvents)

async function loadEvents() {
  const res = await get('/events', { size: 100 })
  if (res.code === 200) {
    eventOptions.value = res.data?.records || []
    if (eventOptions.value.length && !selectedEventId.value) {
      selectedEventId.value = eventOptions.value[0].id
      await loadRules()
    }
  }
}

async function loadRules() {
  if (!selectedEventId.value) {
    rules.value = []
    return
  }
  loading.value = true
  try {
    const res = await get('/scoring-rules', { eventId: selectedEventId.value })
    if (res.code === 200) {
      rules.value = (res.data || []).map((item: any) => ({
        placeNo: item.placeNo,
        points: item.points,
        ruleName: item.ruleName || ('第' + item.placeNo + '名')
      }))
    }
  } finally {
    loading.value = false
  }
}

function addRule() {
  const nextPlace = rules.value.reduce((max, item) => Math.max(max, Number(item.placeNo) || 0), 0) + 1
  rules.value.push({ placeNo: nextPlace, points: 0, ruleName: '第' + nextPlace + '名' })
}

function removeRule(index: number) {
  rules.value.splice(index, 1)
}

async function saveRules() {
  if (!selectedEventId.value) return
  const places = rules.value.map(item => Number(item.placeNo))
  if (places.some(place => !Number.isInteger(place) || place < 1)) {
    ElMessage.warning('名次必须是大于0的整数')
    return
  }
  if (new Set(places).size !== places.length) {
    ElMessage.warning('名次不能重复')
    return
  }
  if (rules.value.some(item => !Number.isInteger(Number(item.points)) || Number(item.points) < 0)) {
    ElMessage.warning('积分必须是非负整数')
    return
  }
  saveLoading.value = true
  try {
    const payload = rules.value
      .slice()
      .sort((a, b) => a.placeNo - b.placeNo)
      .map(item => ({
        placeNo: Number(item.placeNo),
        points: Number(item.points),
        ruleName: item.ruleName || ('第' + item.placeNo + '名')
      }))
    await put('/scoring-rules?eventId=' + selectedEventId.value, payload)
    ElMessage.success('积分规则已保存')
    await loadRules()
  } finally {
    saveLoading.value = false
  }
}
</script>

<style scoped>
.page-alert, .read-only-alert {
  margin-bottom: 16px;
}
</style>
