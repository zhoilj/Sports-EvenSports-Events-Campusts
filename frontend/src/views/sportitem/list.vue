<template>
  <div class="page-container sportitem-page">
    <el-card shadow="never" class="main-card">
      <div class="page-heading">
        <div>
          <div class="eyebrow">CAMPUS SPORTS · EVENT CONFIGURATION</div>
          <h2>项目管理与竞赛编排</h2>
          <p>按甲、乙组独立设置项目；为每个项目配置赛程，再进入竞赛执行生成径赛分组分道或田赛试跳试投名单。</p>
        </div>
        <div class="heading-actions">
          <el-button :disabled="!queryParams.eventId" :loading="seeding" @click="seedTrackField">
            <el-icon><Download /></el-icon> 按学校秩序册导入田径项目
          </el-button>
          <el-button type="primary" @click="handleAdd">
            <el-icon><Plus /></el-icon> 新增项目
          </el-button>
        </div>
      </div>

      <el-alert class="group-alert" type="info" :closable="false" show-icon>
        **甲组：普通本科组**；**乙组：体育专业组**。组别由管理员依据学籍信息核定，报名、接力队伍、赛程和成绩均按组别隔离。
      </el-alert>

      <el-form :inline="true" :model="queryParams" class="search-form" @submit.prevent>
        <el-form-item label="赛事">
          <el-select v-model="queryParams.eventId" placeholder="选择赛事" clearable filterable @change="loadData">
            <el-option v-for="event in eventOptions" :key="event.id" :label="event.name" :value="event.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="项目名称">
          <el-input v-model="queryParams.name" placeholder="搜索项目名称" clearable @keyup.enter="loadData" />
        </el-form-item>
        <el-form-item label="项目类别">
          <el-select v-model="queryParams.type" placeholder="全部类别" clearable>
            <el-option label="径赛" value="TRACK" />
            <el-option label="田赛" value="FIELD" />
            <el-option label="球类" value="BALL" />
            <el-option label="水上项目" value="WATER" />
            <el-option label="其他" value="OTHER" />
          </el-select>
        </el-form-item>
        <el-form-item label="竞赛组别">
          <el-select v-model="queryParams.competitionGroup" placeholder="甲组 / 乙组" clearable>
            <el-option label="甲组 · 普通本科组" value="A" />
            <el-option label="乙组 · 体育专业组" value="B" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadData">查询</el-button>
          <el-button @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>

      <div class="table-summary">
        <span>当前页 {{ tableData.length }} 个项目</span>
        <span>径赛、田赛分别配置；接力项目另行审核队员名单</span>
      </div>

      <el-table :data="tableData" v-loading="loading" border stripe row-key="id">
        <el-table-column label="项目名称" min-width="175" fixed>
          <template #default="{ row }">
            <div class="item-name">{{ row.name }}</div>
            <div class="item-sub">项目编号 E{{ String(row.id).padStart(3, '0') }}</div>
          </template>
        </el-table-column>
        <el-table-column label="竞赛组别" width="175">
          <template #default="{ row }">
            <el-tag :type="row.competitionGroup === 'B' ? 'primary' : row.competitionGroup === 'A' ? 'success' : 'warning'">
              {{ groupName(row.competitionGroup) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="项目类别" width="100">
          <template #default="{ row }">{{ typeName(row.type) }}</template>
        </el-table-column>
        <el-table-column label="性别 / 参赛形式" min-width="135">
          <template #default="{ row }">
            <div>{{ genderName(row.genderLimit) }}</div>
            <div class="item-sub">{{ row.isRelay === 1 ? '接力项目' : '个人项目' }}</div>
          </template>
        </el-table-column>
        <el-table-column label="报名 / 人数上限" min-width="125">
          <template #default="{ row }">
            <div>{{ row.currentParticipants || 0 }} 人/队</div>
            <div class="item-sub">{{ row.maxParticipants > 0 ? '上限 ' + row.maxParticipants : '不设总人数上限' }}</div>
          </template>
        </el-table-column>
        <el-table-column label="成绩规则" min-width="125">
          <template #default="{ row }">
            <div>{{ scoreTypeName(row.scoreType) }}</div>
            <div class="item-sub">{{ row.sortRule === 'DESC' ? '成绩越大越好' : '用时越短越好' }}</div>
          </template>
        </el-table-column>
        <el-table-column label="编排进度" width="115">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '启用' : '已停用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="245" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="openArrange(row)">编排赛程</el-button>
            <el-button type="primary" link @click="handleEdit(row)">编辑</el-button>
            <el-button type="danger" link @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        class="mt-20"
        v-model:current-page="queryParams.current"
        v-model:page-size="queryParams.size"
        :page-sizes="[10, 20, 50, 100]"
        :total="total"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="loadData"
        @current-change="loadData"
      />
    </el-card>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="650px" destroy-on-close>
      <el-form ref="formRef" :model="formData" :rules="rules" label-width="120px">
        <el-form-item label="所属赛事" prop="eventId">
          <el-select v-model="formData.eventId" placeholder="请选择赛事" filterable>
            <el-option v-for="event in eventOptions" :key="event.id" :label="event.name" :value="event.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="项目名称" prop="name">
          <el-input v-model="formData.name" placeholder="如：男子跳远" />
        </el-form-item>
        <el-form-item label="竞赛组别" prop="competitionGroup">
          <el-select v-model="formData.competitionGroup" placeholder="必须选择参赛组别">
            <el-option label="甲组 · 普通本科组" value="A" />
            <el-option label="乙组 · 体育专业组" value="B" />
          </el-select>
        </el-form-item>
        <el-form-item label="项目类别" prop="type">
          <el-select v-model="formData.type" placeholder="请选择">
            <el-option label="径赛" value="TRACK" />
            <el-option label="田赛（跳跃 / 投掷）" value="FIELD" />
            <el-option label="球类" value="BALL" />
            <el-option label="水上项目" value="WATER" />
            <el-option label="其他" value="OTHER" />
          </el-select>
        </el-form-item>
        <el-form-item label="性别限制" prop="genderLimit">
          <el-select v-model="formData.genderLimit" placeholder="请选择">
            <el-option label="男女不限 / 混合" :value="0" />
            <el-option label="男子" :value="1" />
            <el-option label="女子" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item label="接力项目">
          <el-switch v-model="formData.isRelay" :active-value="1" :inactive-value="0" />
        </el-form-item>
        <template v-if="formData.isRelay === 1">
          <el-form-item label="正式队员数" prop="relayMemberCount">
            <el-input-number v-model="formData.relayMemberCount" :min="2" :max="12" />
          </el-form-item>
          <el-form-item label="替补人数" prop="relayReserveCount">
            <el-input-number v-model="formData.relayReserveCount" :min="0" :max="6" />
          </el-form-item>
        </template>
        <el-form-item label="成绩类型" prop="scoreType">
          <el-select v-model="formData.scoreType">
            <el-option label="计时" value="TIME" />
            <el-option label="计距离 / 高度" value="DISTANCE" />
            <el-option label="计分" value="SCORE" />
          </el-select>
        </el-form-item>
        <el-form-item label="成绩排序" prop="sortRule">
          <el-radio-group v-model="formData.sortRule">
            <el-radio value="ASC">数值越小越好</el-radio>
            <el-radio value="DESC">数值越大越好</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="成绩单位" prop="scoreUnit">
          <el-input v-model="formData.scoreUnit" placeholder="如：秒、米、分" />
        </el-form-item>
        <el-form-item label="总人数上限" prop="maxParticipants">
          <el-input-number v-model="formData.maxParticipants" :min="0" :max="10000" />
          <span class="form-tip">0 表示不设项目总人数上限</span>
        </el-form-item>
        <el-form-item label="比赛规则" prop="rules">
          <el-input v-model="formData.rules" type="textarea" :rows="3" placeholder="限报人数、栏高、混合接力棒次等规则" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">保存项目</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="arrangeDialogVisible" :title="arrangingItem ? '编排赛程 · ' + arrangingItem.name : '编排赛程'" width="1120px" top="5vh" destroy-on-close>
      <div v-if="arrangingItem" class="arrange-intro">
        <div>
          <el-tag :type="arrangingItem.competitionGroup === 'B' ? 'primary' : 'success'">{{ groupName(arrangingItem.competitionGroup) }}</el-tag>
          <el-tag class="ml-8">{{ typeName(arrangingItem.type) }}</el-tag>
          <p>组别来自项目配置。保存后进入“竞赛执行与现场检录”，按已通过审核的本组报名名单生成分组分道或田赛比赛顺序。</p>
        </div>
        <el-button plain @click="addRound">增加轮次</el-button>
      </div>
      <el-alert v-if="arrangingItem && expectedPreFinal(arrangingItem)" type="info" :closable="false" show-icon class="mb-12">
        学校秩序册规定，100米、200米和4×100米接力进行预决赛；其他径赛项目及田赛项目按决赛设置。可根据正式规程调整轮次。
      </el-alert>
      <el-table :data="scheduleRows" v-loading="arrangeLoading" border>
        <el-table-column label="赛程名称" min-width="165">
          <template #default="{ row }"><el-input v-model="row.scheduleName" /></template>
        </el-table-column>
        <el-table-column label="轮次" width="125">
          <template #default="{ row }">
            <el-select v-model="row.roundType">
              <el-option label="预赛" value="PRELIMINARY" />
              <el-option label="半决赛" value="SEMIFINAL" />
              <el-option label="决赛" value="FINAL" />
              <el-option label="田赛决赛" value="FIELD_FINAL" />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="轮次序号" width="100">
          <template #default="{ row }"><el-input-number v-model="row.roundNo" :min="1" :max="10" /></template>
        </el-table-column>
        <el-table-column label="比赛日期" width="150">
          <template #default="{ row }"><el-date-picker v-model="row.matchDate" type="date" value-format="YYYY-MM-DD" placeholder="选择日期" /></template>
        </el-table-column>
        <el-table-column label="开始时间" width="125">
          <template #default="{ row }"><el-time-picker v-model="row.startTime" format="HH:mm" value-format="HH:mm" placeholder="开始" /></template>
        </el-table-column>
        <el-table-column label="结束时间" width="125">
          <template #default="{ row }"><el-time-picker v-model="row.endTime" format="HH:mm" value-format="HH:mm" placeholder="结束" /></template>
        </el-table-column>
        <el-table-column label="场地" min-width="135">
          <template #default="{ row }"><el-input v-model="row.location" /></template>
        </el-table-column>
        <el-table-column label="操作" width="75">
          <template #default="{ row, $index }">
            <el-button v-if="!row.id" type="danger" link @click="removeRound($index)">移除</el-button>
            <span v-else class="muted">已保存</span>
          </template>
        </el-table-column>
      </el-table>
      <template #footer>
        <el-button @click="arrangeDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="arrangeSaving" @click="saveSchedulePlan">保存赛程编排</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, FormInstance, FormRules } from 'element-plus'
import { get, post, put, del } from '@/utils/request'

const loading = ref(false)
const seeding = ref(false)
const tableData = ref<any[]>([])
const total = ref(0)
const eventOptions = ref<any[]>([])
const queryParams = reactive({
  current: 1,
  size: 20,
  eventId: null as number | null,
  name: '',
  type: '',
  competitionGroup: '',
  status: null as number | null
})

const dialogVisible = ref(false)
const dialogTitle = ref('')
const submitLoading = ref(false)
const formRef = ref<FormInstance>()
const editingId = ref<number | null>(null)
const formData = reactive({
  eventId: null as number | null,
  name: '',
  type: 'TRACK',
  competitionGroup: 'A',
  genderLimit: 0,
  maxParticipants: 0,
  currentParticipants: 0,
  isRelay: 0,
  relayMemberCount: 4,
  relayReserveCount: 0,
  scoreType: 'TIME',
  sortRule: 'ASC',
  scoreUnit: '秒',
  rules: '',
  status: 1
})
const rules: FormRules = {
  eventId: [{ required: true, message: '请选择所属赛事', trigger: 'change' }],
  name: [{ required: true, message: '请输入项目名称', trigger: 'blur' }],
  type: [{ required: true, message: '请选择项目类别', trigger: 'change' }],
  competitionGroup: [{ required: true, message: '请选择甲组或乙组', trigger: 'change' }]
}

const arrangeDialogVisible = ref(false)
const arrangeLoading = ref(false)
const arrangeSaving = ref(false)
const arrangingItem = ref<any | null>(null)
const scheduleRows = ref<any[]>([])

onMounted(() => {
  loadEventOptions()
  loadData()
})

async function loadEventOptions() {
  const res = await get('/events', { current: 1, size: 100 })
  if (res.code === 200) eventOptions.value = res.data?.records || []
}

async function loadData() {
  loading.value = true
  try {
    const res = await get('/sport-items', queryParams)
    if (res.code === 200) {
      tableData.value = res.data?.records || []
      total.value = res.data?.total || 0
    }
  } finally {
    loading.value = false
  }
}

function resetQuery() {
  queryParams.current = 1
  queryParams.eventId = null
  queryParams.name = ''
  queryParams.type = ''
  queryParams.competitionGroup = ''
  queryParams.status = null
  loadData()
}

async function seedTrackField() {
  if (!queryParams.eventId) {
    ElMessage.warning('请先选择需要导入项目的赛事。')
    return
  }
  await ElMessageBox.confirm(
    '将按学校第十九届运动会秩序册补齐男子、女子及混合径赛项目和跳高、跳远、三级跳远、铅球等田赛项目，并分别建立甲组、乙组项目。已有报名或赛程的未分组项目不会被自动改动。',
    '导入田径项目清单',
    { type: 'warning', confirmButtonText: '导入项目', cancelButtonText: '取消' }
  )
  seeding.value = true
  try {
    const res = await post('/sport-items/seed-track-field/' + queryParams.eventId)
    if (res.code === 200) {
      const data = res.data || {}
      const skipped = Array.isArray(data.skippedNames) ? data.skippedNames : []
      ElMessage.success(`导入完成：新增 ${data.created || 0} 项，自动归组 ${data.classified || 0} 项，已存在 ${data.reused || 0} 项。`)
      if (skipped.length) {
        await ElMessageBox.alert('以下项目已有报名或赛程，系统未自动归组，以免影响既有数据：\n' + skipped.join('\n'), '需要人工确认', { type: 'warning' })
      }
      queryParams.current = 1
      await loadData()
    }
  } finally {
    seeding.value = false
  }
}

function handleAdd() {
  editingId.value = null
  dialogTitle.value = '新增项目'
  Object.assign(formData, {
    eventId: queryParams.eventId,
    name: '',
    type: 'TRACK',
    competitionGroup: queryParams.competitionGroup || 'A',
    genderLimit: 0,
    maxParticipants: 0,
    currentParticipants: 0,
    isRelay: 0,
    relayMemberCount: 4,
    relayReserveCount: 0,
    scoreType: 'TIME',
    sortRule: 'ASC',
    scoreUnit: '秒',
    rules: '',
    status: 1
  })
  dialogVisible.value = true
}

function handleEdit(row: any) {
  editingId.value = row.id
  dialogTitle.value = '编辑项目'
  Object.assign(formData, {
    eventId: row.eventId,
    name: row.name,
    type: row.type || 'TRACK',
    competitionGroup: row.competitionGroup || '',
    genderLimit: row.genderLimit ?? 0,
    maxParticipants: row.maxParticipants ?? 0,
    currentParticipants: row.currentParticipants ?? 0,
    isRelay: row.isRelay ?? 0,
    relayMemberCount: row.relayMemberCount ?? 4,
    relayReserveCount: row.relayReserveCount ?? 0,
    scoreType: row.scoreType || (row.type === 'FIELD' ? 'DISTANCE' : 'TIME'),
    sortRule: row.sortRule || (row.type === 'FIELD' ? 'DESC' : 'ASC'),
    scoreUnit: row.scoreUnit || (row.type === 'FIELD' ? '米' : '秒'),
    rules: row.rules || '',
    status: row.status ?? 1
  })
  dialogVisible.value = true
}

async function handleSubmit() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid: boolean) => {
    if (!valid) return
    submitLoading.value = true
    try {
      if (editingId.value) {
        await put('/sport-items/' + editingId.value, { ...formData })
        ElMessage.success('项目更新成功')
      } else {
        await post('/sport-items', { ...formData })
        ElMessage.success('项目创建成功')
      }
      dialogVisible.value = false
      await loadData()
    } finally {
      submitLoading.value = false
    }
  })
}

function expectedPreFinal(item: any) {
  return item?.type === 'TRACK' && (/^(男子|女子)(100米|200米)$/.test(item.name || '') || /4×100米.*接力/.test(item.name || ''))
}

function expectedRounds(item: any) {
  if (expectedPreFinal(item)) {
    return [
      { scheduleName: item.name + '预赛', roundNo: 1, roundType: 'PRELIMINARY' },
      { scheduleName: item.name + '决赛', roundNo: 2, roundType: 'FINAL' }
    ]
  }
  return [{
    scheduleName: item.name + (item.type === 'FIELD' ? '田赛决赛' : '决赛'),
    roundNo: 1,
    roundType: item.type === 'FIELD' ? 'FIELD_FINAL' : 'FINAL'
  }]
}

async function openArrange(item: any) {
  arrangingItem.value = { ...item }
  arrangeDialogVisible.value = true
  arrangeLoading.value = true
  try {
    const res = await get('/schedules', { current: 1, size: 100, eventId: item.eventId, sportItemId: item.id })
    const existing = res.code === 200 ? (res.data?.records || []) : []
    const defaults = expectedRounds(item)
    const usedIds = new Set<number>()
    const rows = defaults.map(round => {
      let found = existing.find((entry: any) => !usedIds.has(entry.id) && entry.roundType === round.roundType)
      if (!found && round.roundType === 'FIELD_FINAL') {
        found = existing.find((entry: any) => !usedIds.has(entry.id) && entry.roundType === 'FINAL')
      }
      if (found) usedIds.add(found.id)
      return {
        id: found?.id,
        scheduleName: found?.scheduleName || round.scheduleName,
        roundNo: found?.roundNo ?? round.roundNo,
        roundType: found?.roundType || round.roundType,
        matchDate: found?.matchDate || '',
        startTime: found?.startTime || '',
        endTime: found?.endTime || '',
        location: found?.location || (item.type === 'FIELD' ? '田赛区' : '田径场'),
        referee: found?.referee || '',
        remark: found?.remark || '',
        status: found?.status ?? 1
      }
    })
    existing.filter((entry: any) => !usedIds.has(entry.id)).forEach((entry: any) => rows.push({
      id: entry.id,
      scheduleName: entry.scheduleName || '',
      roundNo: entry.roundNo || rows.length + 1,
      roundType: entry.roundType || 'FINAL',
      matchDate: entry.matchDate || '',
      startTime: entry.startTime || '',
      endTime: entry.endTime || '',
      location: entry.location || '',
      referee: entry.referee || '',
      remark: entry.remark || '',
      status: entry.status ?? 1
    }))
    scheduleRows.value = rows
  } finally {
    arrangeLoading.value = false
  }
}

function addRound() {
  scheduleRows.value.push({
    scheduleName: (arrangingItem.value?.name || '项目') + '新增轮次',
    roundNo: scheduleRows.value.length + 1,
    roundType: arrangingItem.value?.type === 'FIELD' ? 'FIELD_FINAL' : 'SEMIFINAL',
    matchDate: '',
    startTime: '',
    endTime: '',
    location: arrangingItem.value?.type === 'FIELD' ? '田赛区' : '田径场',
    referee: '',
    remark: '',
    status: 1
  })
}

function removeRound(index: number) {
  scheduleRows.value.splice(index, 1)
}

async function saveSchedulePlan() {
  if (!arrangingItem.value || scheduleRows.value.length === 0) {
    ElMessage.warning('请至少配置一个赛程轮次。')
    return
  }
  const missing = scheduleRows.value.find(row => !row.scheduleName?.trim() || !row.matchDate || !row.startTime || !row.location?.trim())
  if (missing) {
    ElMessage.warning('每个赛程都必须填写名称、比赛日期、开始时间和场地。')
    return
  }
  arrangeSaving.value = true
  let saved = 0
  try {
    for (const row of scheduleRows.value) {
      const payload = {
        eventId: arrangingItem.value.eventId,
        sportItemId: arrangingItem.value.id,
        scheduleName: row.scheduleName.trim(),
        roundNo: row.roundNo,
        roundType: row.roundType,
        groupNo: '',
        matchDate: row.matchDate,
        startTime: row.startTime,
        endTime: row.endTime || null,
        location: row.location.trim(),
        referee: row.referee || '',
        remark: row.remark || '',
        status: row.status ?? 1
      }
      if (row.id) await put('/schedules/' + row.id, payload)
      else await post('/schedules', payload)
      saved++
    }
    ElMessage.success(`已保存 ${saved} 条赛程。下一步请进入“竞赛执行与现场检录”生成本组参赛名单。`)
    arrangeDialogVisible.value = false
    await loadData()
  } catch (error: any) {
    ElMessage.error(`赛程保存中断：已处理 ${saved} 条。请重新打开编排窗口核对已保存记录，再继续操作。`)
  } finally {
    arrangeSaving.value = false
  }
}

async function handleDelete(row: any) {
  await ElMessageBox.confirm('确定删除该项目吗？已有报名或赛程的项目可能无法删除。', '提示', { type: 'warning' })
  await del('/sport-items/' + row.id)
  ElMessage.success('项目删除成功')
  await loadData()
}

function groupName(group: string) {
  if (group === 'A') return '甲组 · 普通本科组'
  if (group === 'B') return '乙组 · 体育专业组'
  return '待核定组别'
}
function typeName(type: string) {
  return ({ TRACK: '径赛', FIELD: '田赛', BALL: '球类', WATER: '水上项目', SWIMMING: '水上项目', OTHER: '其他' } as Record<string, string>)[type || ''] || type || '未分类'
}
function genderName(value: number) {
  return ({ 0: '男女不限 / 混合', 1: '男子', 2: '女子' } as Record<number, string>)[value ?? 0] || '未设置'
}
function scoreTypeName(value: string) {
  return ({ TIME: '计时成绩', DISTANCE: '距离 / 高度', SCORE: '计分成绩' } as Record<string, string>)[value || ''] || '未设置'
}
</script>

<style lang="scss" scoped>
.sportitem-page { max-width: 1700px; margin: 0 auto; }
.main-card { border: 1px solid #e3ebe6; border-radius: 15px; }
.page-heading { display: flex; align-items: flex-end; justify-content: space-between; gap: 20px; margin-bottom: 20px; }
.eyebrow { color: #1f7959; font-size: 10px; font-weight: 800; letter-spacing: 1.5px; }
.page-heading h2 { margin: 8px 0; color: #20382e; font-size: 24px; }
.page-heading p { margin: 0; max-width: 820px; color: #7d8c83; font-size: 12px; line-height: 1.8; }
.heading-actions { display: flex; gap: 9px; flex-wrap: wrap; }
.group-alert { margin-bottom: 18px; }
.search-form { display: flex; align-items: center; flex-wrap: wrap; gap: 4px 0; margin-bottom: 4px; }
.search-form :deep(.el-select) { width: 210px; }
.search-form :deep(.el-input) { width: 220px; }
.table-summary { display: flex; justify-content: space-between; gap: 12px; margin: 7px 0 12px; color: #819087; font-size: 11px; }
.item-name { color: #263d32; font-weight: 750; font-size: 12px; }
.item-sub { margin-top: 4px; color: #92a097; font-size: 10px; line-height: 1.5; }
.form-tip { margin-left: 10px; color: #8a9890; font-size: 11px; }
.arrange-intro { display: flex; justify-content: space-between; align-items: flex-start; gap: 18px; margin-bottom: 15px; }
.arrange-intro p { margin: 10px 0 0; color: #77867d; font-size: 12px; line-height: 1.7; }
.ml-8 { margin-left: 8px; }
.mb-12 { margin-bottom: 12px; }
.muted { color: #89968f; font-size: 10px; }
@media (max-width: 900px) {
  .page-heading { flex-direction: column; align-items: flex-start; }
  .table-summary { flex-direction: column; }
}
</style>
