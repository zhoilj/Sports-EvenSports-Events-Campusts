<template>
  <div class="page-container">
    <el-card shadow="never">
      <!-- 搜索表单 -->
      <el-form :inline="true" class="search-form">
        <el-form-item label="赛事">
          <el-select v-model="queryParams.eventId" placeholder="请选择" clearable filterable>
            <el-option v-for="item in eventOptions" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="项目名称">
          <el-input v-model="queryParams.name" placeholder="请输入项目名称" clearable />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadData">查询</el-button>
          <el-button @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>

      <!-- 操作按钮 -->
      <div class="mb-10">
        <el-button type="primary" @click="handleAdd">
          <el-icon><Plus /></el-icon>新增项目
        </el-button>
        <el-button type="warning" plain :disabled="!queryParams.eventId" @click="applyOrderBookDefaults">
          按秩序册匹配默认规则
        </el-button>
      </div>

      <!-- 数据表格 -->
      <el-table :data="tableData" v-loading="loading" border stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="eventName" label="所属赛事" min-width="150" />
        <el-table-column prop="name" label="项目名称" width="150" />
        <el-table-column prop="categoryName" label="类别" width="100" />
        <el-table-column prop="genderLimitName" label="性别限制" width="100" />
        <el-table-column label="组别" width="90">
          <template #default="{ row }">{{ divisionName(row.division) }}</template>
        </el-table-column>
        <el-table-column label="性别类别" width="100">
          <template #default="{ row }">{{ genderCategoryName(row.genderCategory) }}</template>
        </el-table-column>
        <el-table-column label="轮次规则" width="120">
          <template #default="{ row }">{{ roundPolicyName(row.roundPolicy) }}</template>
        </el-table-column>
        <el-table-column prop="scoringMultiplier" label="积分倍率" width="90" />
        <el-table-column prop="participationPoints" label="参与分" width="90" />
        <el-table-column prop="maxParticipants" label="人数上限" width="100">
          <template #default="{ row }">{{ row.maxParticipants > 0 ? row.maxParticipants : '不限' }}</template>
        </el-table-column>
        <el-table-column prop="currentParticipants" label="有效报名" width="100" />
        <el-table-column prop="isRelay" label="项目形式" width="100">
          <template #default="{ row }">{{ row.isRelay === 1 ? '接力项目' : row.isTeamEvent === 1 ? '集体队伍项目' : '个人项目' }}</template>
        </el-table-column>
        <el-table-column prop="rules" label="比赛规则" min-width="200" show-overflow-tooltip />
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleEdit(row)">编辑</el-button>
            <el-button type="danger" link @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
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

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="600px" destroy-on-close>
      <el-form ref="formRef" :model="formData" :rules="rules" label-width="100px">
        <el-form-item label="所属赛事" prop="eventId">
          <el-select v-model="formData.eventId" placeholder="请选择" filterable>
            <el-option v-for="item in eventOptions" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="项目名称" prop="name">
          <el-input v-model="formData.name" placeholder="请输入项目名称" @change="suggestRoundPolicy" />
        </el-form-item>
        <el-form-item label="项目类型" prop="type">
          <el-select v-model="formData.type" placeholder="请选择">
            <el-option label="径赛(跑步)" value="TRACK" />
            <el-option label="田赛(跳跃/投掷)" value="FIELD" />
            <el-option label="球类" value="BALL" />
            <el-option label="水上项目" value="WATER" />
            <el-option label="其他" value="OTHER" />
          </el-select>
        </el-form-item>
        <el-form-item label="竞赛组别" prop="division">
          <el-select v-model="formData.division" placeholder="请选择组别">
            <el-option label="甲组" value="A" />
            <el-option label="乙组" value="B" />
            <el-option label="教工组" value="STAFF" />
            <el-option label="不分组" value="OPEN" />
          </el-select>
        </el-form-item>
        <el-form-item label="成绩归属性别" prop="genderCategory">
          <el-select v-model="formData.genderCategory" placeholder="请选择成绩归属类别">
            <el-option label="男子" value="MALE" />
            <el-option label="女子" value="FEMALE" />
            <el-option label="混合" value="MIXED" />
            <el-option label="开放/集体" value="OPEN" />
          </el-select>
        </el-form-item>
        <el-form-item label="轮次规则" prop="roundPolicy">
          <el-select v-model="formData.roundPolicy" placeholder="请选择轮次规则">
            <el-option label="仅决赛" value="FINAL_ONLY" />
            <el-option label="预赛+决赛" value="PRELIMINARY_FINAL" />
            <el-option label="允许自定义" value="FLEXIBLE" />
          </el-select>
        </el-form-item>
        <el-form-item label="积分倍率" prop="scoringMultiplier">
          <el-input-number v-model="formData.scoringMultiplier" :min="1" :max="5" />
          <span class="form-tip">接力、集体、民体等按规程设置为2</span>
        </el-form-item>
        <el-form-item label="参与积分" prop="participationPoints">
          <el-input-number v-model="formData.participationPoints" :min="0" :max="100" />
          <span class="form-tip">仅对完成检录且实际参赛的成绩加分</span>
        </el-form-item>
        <el-form-item label="破纪录积分" prop="recordPoints">
          <el-input-number v-model="formData.recordPoints" :min="0" :max="100" />
        </el-form-item>
        <el-form-item label="性别限制" prop="genderLimit">
          <el-select v-model="formData.genderLimit" placeholder="请选择">
            <el-option label="不限" :value="0" />
            <el-option label="仅男性" :value="1" />
            <el-option label="仅女性" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item label="人数上限" prop="maxParticipants">
          <el-input-number v-model="formData.maxParticipants" :min="0" :max="1000" />
          <span class="form-tip">0 表示不限人数</span>
        </el-form-item>
        <el-form-item label="接力项目">
          <el-switch v-model="formData.isRelay" :active-value="1" :inactive-value="0" @change="onRelayChange" />
        </el-form-item>
        <el-form-item label="集体/民体/趣味项目">
          <el-switch v-model="formData.isTeamEvent" :active-value="1" :inactive-value="0" :disabled="formData.isRelay === 1" />
          <span class="form-tip">用于学院队伍参赛，不按单个学生录入成绩</span>
        </el-form-item>
        <template v-if="formData.isTeamEvent === 1 && formData.isRelay !== 1">
          <el-form-item label="队伍人数" prop="teamMemberCount">
            <el-input-number v-model="formData.teamMemberCount" :min="1" :max="100" />
          </el-form-item>
          <el-form-item label="替补人数" prop="teamReserveCount">
            <el-input-number v-model="formData.teamReserveCount" :min="0" :max="30" />
          </el-form-item>
          <el-form-item label="每学院队数" prop="maxTeamsPerCollege">
            <el-input-number v-model="formData.maxTeamsPerCollege" :min="1" :max="10" />
          </el-form-item>
          <el-form-item label="队员构成规则" prop="teamRosterRule">
            <el-select v-model="formData.teamRosterRule" placeholder="请选择队员构成规则" style="width: 260px">
              <el-option label="不限制性别" value="ANY" />
              <el-option label="全男" value="MALE_ONLY" />
              <el-option label="全女" value="FEMALE_ONLY" />
              <el-option label="5男5女" value="MIXED_5M5F" />
              <el-option label="10男10女" value="MIXED_10M10F" />
              <el-option label="15男15女" value="MIXED_15M15F" />
              <el-option label="3男3女" value="MIXED_3M3F" />
              <el-option label="6男4女" value="MIXED_6M4F" />
              <el-option label="至少6名女生（仍需分组人工核验）" value="MIXED_MIN_6F" />
              <el-option label="按专项规程人工核验" value="CUSTOM" />
            </el-select>
          </el-form-item>
        </template>
        <template v-if="formData.isRelay === 1">
          <el-form-item label="正式队员数" prop="relayMemberCount">
            <el-input-number v-model="formData.relayMemberCount" :min="2" :max="12" />
          </el-form-item>
          <el-form-item label="替补人数" prop="relayReserveCount">
            <el-input-number v-model="formData.relayReserveCount" :min="0" :max="6" />
          </el-form-item>
        </template>
        <el-form-item label="比赛规则" prop="rules">
          <el-input v-model="formData.rules" type="textarea" :rows="3" placeholder="请输入比赛规则" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox, FormInstance, FormRules } from 'element-plus'
import { get, post, put, del } from '@/utils/request'

const loading = ref(false)
const tableData = ref<any[]>([])
const total = ref(0)
const eventOptions = ref<any[]>([])

const queryParams = reactive({
  current: 1,
  size: 10,
  eventId: null as number | null,
  name: ''
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
  division: 'OPEN',
  genderCategory: 'OPEN',
  roundPolicy: 'FINAL_ONLY',
  scoringMultiplier: 1,
  participationPoints: 0,
  recordPoints: 9,
  genderLimit: 0,
  maxParticipants: 100,
  isRelay: 0,
  isTeamEvent: 0,
  teamMemberCount: 0,
  teamReserveCount: 0,
  maxTeamsPerCollege: 1,
  teamRosterRule: 'ANY',
  relayMemberCount: 4,
  relayReserveCount: 0,
  rules: ''
})

const rules: FormRules = {
  eventId: [{ required: true, message: '请选择所属赛事', trigger: 'change' }],
  name: [{ required: true, message: '请输入项目名称', trigger: 'blur' }],
  type: [{ required: true, message: '请选择项目类型', trigger: 'change' }]
}

onMounted(() => {
  loadEventOptions()
  loadData()
})

async function loadEventOptions() {
  const res = await get('/events', { size: 100 })
  if (res.code === 200) {
    eventOptions.value = res.data.records || []
  }
}

async function loadData() {
  loading.value = true
  try {
    const res = await get('/sport-items', queryParams)
    if (res.code === 200) {
      tableData.value = res.data.records
      total.value = res.data.total
    }
  } finally {
    loading.value = false
  }
}

function resetQuery() {
  queryParams.eventId = null
  queryParams.name = ''
  queryParams.current = 1
  loadData()
}

async function applyOrderBookDefaults() {
  if (!queryParams.eventId) {
    ElMessage.warning('请先选择赛事')
    return
  }
  await ElMessageBox.confirm(
    '系统将根据项目名称匹配甲/乙/教工组、男女/混合类别、轮次规则、积分倍率和参与分。此操作会覆盖该赛事所有项目的上述配置，完成后仍需逐项核对秩序册。',
    '按正式秩序册匹配规则',
    { type: 'warning', confirmButtonText: '匹配并保存' }
  )
  const res = await post(`/sport-items/event/${queryParams.eventId}/apply-orderbook-defaults`)
  ElMessage.success(res.message || `已匹配 ${res.data} 个项目，请逐项复核`)
  await loadData()
}

function handleAdd() {
  editingId.value = null
  dialogTitle.value = '新增项目'
  Object.assign(formData, {
    eventId: null, name: '', type: 'TRACK',
    division: 'OPEN', genderCategory: 'OPEN', roundPolicy: 'FINAL_ONLY',
    scoringMultiplier: 1, participationPoints: 0, recordPoints: 9,
    genderLimit: 0, maxParticipants: 100, isRelay: 0,
    isTeamEvent: 0, teamMemberCount: 0, teamReserveCount: 0, maxTeamsPerCollege: 1, teamRosterRule: 'ANY',
    relayMemberCount: 4, relayReserveCount: 0, rules: ''
  })
  dialogVisible.value = true
}

function handleEdit(row: any) {
  editingId.value = row.id
  dialogTitle.value = '编辑项目'
  Object.assign(formData, row)
  dialogVisible.value = true
}

function onRelayChange(value: string | number | boolean) {
  const relay = Number(value) === 1
  formData.isTeamEvent = relay ? 1 : 0
  if (relay && formData.scoringMultiplier === 1) formData.scoringMultiplier = 2
  if (!relay && formData.scoringMultiplier === 2) formData.scoringMultiplier = 1
}

function suggestRoundPolicy() {
  const name = formData.name.replace(/\\s/g, '')
  const isMixedRelay = /混合接力/.test(name)
  const isPreliminaryFinal = /100米|200米/.test(name)
    || (!isMixedRelay && /4[×xX]100米接力/.test(name))
  formData.roundPolicy = formData.type === 'FIELD' || !isPreliminaryFinal
    ? 'FINAL_ONLY'
    : 'PRELIMINARY_FINAL'
}

function divisionName(value: string) {
  const map: Record<string, string> = { A: '甲组', B: '乙组', STAFF: '教工组', OPEN: '不分组' }
  return map[value] || value || '不分组'
}
function genderCategoryName(value: string) {
  const map: Record<string, string> = { MALE: '男子', FEMALE: '女子', MIXED: '混合', OPEN: '开放/集体' }
  return map[value] || value || '开放/集体'
}
function roundPolicyName(value: string) {
  const map: Record<string, string> = { FINAL_ONLY: '仅决赛', PRELIMINARY_FINAL: '预赛+决赛', FLEXIBLE: '允许自定义' }
  return map[value] || value || '允许自定义'
}

async function handleSubmit() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid: boolean) => {
    if (!valid) return
    submitLoading.value = true
    try {
      if (editingId.value) {
        await put(`/sport-items/${editingId.value}`, formData)
        ElMessage.success('更新成功')
      } else {
        await post('/sport-items', formData)
        ElMessage.success('创建成功')
      }
      dialogVisible.value = false
      loadData()
    } finally {
      submitLoading.value = false
    }
  })
}

async function handleDelete(row: any) {
  await ElMessageBox.confirm('确定要删除该项目吗？', '提示', { type: 'warning' })
  await del(`/sport-items/${row.id}`)
  ElMessage.success('删除成功')
  loadData()
}
</script>