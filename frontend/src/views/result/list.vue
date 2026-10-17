<template>
  <div class="page-container">
    <el-card shadow="never">
      <el-form :inline="true" class="search-form">
        <el-form-item label="赛事">
          <el-select v-model="queryParams.eventId" placeholder="选择赛事" clearable filterable @change="onQueryEventChange">
            <el-option v-for="item in eventOptions" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="运动项目">
          <el-select v-model="queryParams.sportItemId" placeholder="选择项目" clearable filterable>
            <el-option v-for="item in sportItemOptions" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadData">查询</el-button>
          <el-button @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>

      <div class="toolbar">
        <el-button type="primary" @click="handleAdd">录入成绩</el-button>
        <el-button type="success" @click="handleExport" :disabled="!queryParams.eventId">导出正式成绩 CSV</el-button>
        <el-button @click="router.push('/standings')">学院积分与奖牌榜</el-button>
      </div>

      <el-table :data="tableData" v-loading="loading" border stripe>
        <el-table-column prop="eventName" label="赛事" min-width="150" />
        <el-table-column prop="sportItemName" label="运动项目" min-width="130" />
        <el-table-column label="参赛者/队伍" min-width="150">
          <template #default="{ row }">{{ row.relayTeamName || row.realName || row.username || '—' }}</template>
        </el-table-column>
        <el-table-column prop="studentNo" label="学号" width="120" />
        <el-table-column prop="college" label="学院" min-width="130" />
        <el-table-column label="成绩" min-width="120">
          <template #default="{ row }">{{ row.scoreText || row.score || '—' }}</template>
        </el-table-column>
        <el-table-column label="成绩状态" width="105">
          <template #default="{ row }">
            <el-tag :type="resultType(row.resultStatus)">{{ resultStatusName(row.resultStatus) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="ranking" label="名次" width="75" />
        <el-table-column prop="points" label="积分" width="70" />
        <el-table-column label="破纪录" width="90">
          <template #default="{ row }">{{ row.isRecord === 1 ? '是' : '否' }}</template>
        </el-table-column>
        <el-table-column label="复核状态" width="105">
          <template #default="{ row }">
            <el-tag :type="reviewType(row.reviewStatus)">{{ reviewStatusName(row.reviewStatus) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="发布状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.isPublished === 1 ? 'success' : 'info'">{{ row.isPublished === 1 ? '已发布' : '未发布' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="recorderName" label="录入人" width="100" />
        <el-table-column label="操作" min-width="260" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.isPublished !== 1" link type="primary" @click="handleEdit(row)">编辑</el-button>
            <el-button v-if="row.isPublished !== 1 && row.reviewStatus !== 2" link type="success" @click="reviewResult(row, 2)">复核通过</el-button>
            <el-button v-if="row.isPublished !== 1" link type="warning" @click="reviewResult(row, 3)">退回修改</el-button>
            <el-button v-if="row.scheduleId && row.isPublished !== 1" link type="success" @click="publishSchedule(row)">发布本场</el-button>
            <el-button v-if="row.isPublished !== 1" link type="danger" @click="handleDelete(row)">删除</el-button>
            <el-button link type="info" @click="viewAudit(row)">审计记录</el-button>
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
      <el-form ref="formRef" :model="formData" :rules="rules" label-width="115px">
        <el-form-item label="赛事" prop="eventId">
          <el-select v-model="formData.eventId" placeholder="请选择赛事" filterable :disabled="!!editingId" @change="onEventChange">
            <el-option v-for="item in eventOptions" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="运动项目" prop="sportItemId">
          <el-select v-model="formData.sportItemId" placeholder="请选择项目" filterable :disabled="!!editingId" @change="onSportItemChange">
            <el-option v-for="item in formSportItemOptions" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="比赛场次" prop="scheduleId">
          <el-select v-model="formData.scheduleId" placeholder="请选择赛程" clearable filterable>
            <el-option v-for="item in formScheduleOptions" :key="item.id" :label="`${item.matchDate} ${item.startTime} · ${roundName(item.roundType)}`" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="参赛主体">
          <el-radio-group v-model="formData.entryType" :disabled="!!editingId" @change="loadParticipants">
            <el-radio label="INDIVIDUAL">个人</el-radio>
            <el-radio label="RELAY">{{ formData.sportItemId && formSportItemOptions.find(item => item.id === formData.sportItemId)?.isRelay === 1 ? '接力队伍' : '集体/民体/趣味队伍' }}</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="formData.entryType === 'INDIVIDUAL'" label="已通过报名" prop="registrationId">
          <el-select v-model="formData.registrationId" placeholder="选择参赛者" filterable>
            <el-option v-for="item in participantOptions" :key="item.id" :label="`${item.realName || item.username}（${item.studentNo || '无学号'} · ${item.college || '未填写学院'}）`" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item v-else label="已审核接力队伍" prop="relayTeamId">
          <el-select v-model="formData.relayTeamId" placeholder="选择接力队伍" filterable>
            <el-option v-for="item in relayTeamOptions" :key="item.id" :label="`${item.teamName}（${item.college || '未填写学院'}）`" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="成绩状态" prop="resultStatus">
          <el-select v-model="formData.resultStatus" @change="onResultStatusChange">
            <el-option label="有效成绩" value="VALID" />
            <el-option label="未出赛 DNS" value="DNS" />
            <el-option label="未完成 DNF" value="DNF" />
            <el-option label="取消资格 DQ" value="DQ" />
            <el-option label="弃权 WD" value="WD" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="formData.resultStatus === 'VALID'" label="成绩数值" prop="score">
          <el-input-number v-model="formData.score" :precision="3" :step="0.01" :min="0" />
        </el-form-item>
        <el-form-item v-if="formData.resultStatus === 'VALID'" label="成绩文本">
          <el-input v-model="formData.scoreText" placeholder="如：10.25秒、5.60米" />
        </el-form-item>
        <el-form-item v-if="formData.resultStatus === 'VALID'" label="破纪录">
          <el-switch v-model="formData.isRecord" :active-value="1" :inactive-value="0" />
          <span class="form-tip">仅在裁判确认打破正式校运会纪录时勾选</span>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="formData.remark" type="textarea" :rows="2" placeholder="成绩说明或异常情况" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">保存为待复核</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="auditDialogVisible" title="成绩变更审计记录" width="900px">
      <el-table :data="auditRows" border stripe>
        <el-table-column prop="operatedAt" label="操作时间" width="170" />
        <el-table-column prop="action" label="操作" width="110" />
        <el-table-column prop="operatorId" label="操作人ID" width="100" />
        <el-table-column prop="remark" label="说明" min-width="160" />
        <el-table-column label="变更前" min-width="200">
          <template #default="{ row }"><pre class="audit-json">{{ row.beforeValue || '—' }}</pre></template>
        </el-table-column>
        <el-table-column label="变更后" min-width="200">
          <template #default="{ row }"><pre class="audit-json">{{ row.afterValue || '—' }}</pre></template>
        </el-table-column>
      </el-table>
      <el-empty v-if="auditRows.length === 0" description="暂无审计记录" />
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, FormInstance, FormRules } from 'element-plus'
import { useRouter } from 'vue-router'
import { get, post, put, del } from '@/utils/request'

const router = useRouter()
const loading = ref(false)
const submitLoading = ref(false)
const tableData = ref<any[]>([])
const total = ref(0)
const eventOptions = ref<any[]>([])
const sportItemOptions = ref<any[]>([])
const formSportItemOptions = ref<any[]>([])
const formScheduleOptions = ref<any[]>([])
const allFormScheduleOptions = ref<any[]>([])
const participantOptions = ref<any[]>([])
const relayTeamOptions = ref<any[]>([])
const queryParams = reactive({ current: 1, size: 10, eventId: null as number | null, sportItemId: null as number | null })
const dialogVisible = ref(false)
const auditDialogVisible = ref(false)
const auditRows = ref<any[]>([])
const dialogTitle = ref('')
const formRef = ref<FormInstance>()
const editingId = ref<number | null>(null)
const formData = reactive({
  eventId: null as number | null,
  sportItemId: null as number | null,
  scheduleId: null as number | null,
  entryType: 'INDIVIDUAL',
  registrationId: null as number | null,
  relayTeamId: null as number | null,
  resultStatus: 'VALID',
  isRecord: 0,
  score: null as number | null,
  scoreText: '',
  remark: ''
})
const rules: FormRules = {
  eventId: [{ required: true, message: '请选择赛事', trigger: 'change' }],
  sportItemId: [{ required: true, message: '请选择项目', trigger: 'change' }],
  scheduleId: [{ required: true, message: '请选择比赛场次', trigger: 'change' }],
  score: [{ validator: (_rule: any, value: number | null, callback: (error?: Error) => void) => {
    if (formData.resultStatus === 'VALID' && (value === null || value === undefined || value < 0)) {
      callback(new Error('有效成绩必须填写非负数值'))
      return
    }
    callback()
  }, trigger: 'blur' }]
}

onMounted(async () => {
  await loadEventOptions()
  await loadData()
})

async function loadEventOptions() {
  const res = await get('/events', { current: 1, size: 100 })
  if (res.code === 200) eventOptions.value = res.data.records || []
}

async function loadData() {
  loading.value = true
  try {
    const res = await get('/results', queryParams)
    if (res.code === 200) {
      tableData.value = res.data.records || []
      total.value = res.data.total || 0
    }
  } finally {
    loading.value = false
  }
}

async function onQueryEventChange(eventId: number | null) {
  queryParams.sportItemId = null
  sportItemOptions.value = []
  if (eventId) {
    const res = await get('/sport-items', { current: 1, size: 100, eventId })
    if (res.code === 200) sportItemOptions.value = res.data.records || []
  }
}

function resetQuery() {
  queryParams.eventId = null
  queryParams.sportItemId = null
  queryParams.current = 1
  sportItemOptions.value = []
  loadData()
}

async function handleAdd() {
  editingId.value = null
  dialogTitle.value = '录入比赛成绩'
  const selectedEventId = queryParams.eventId
  const selectedItemId = queryParams.sportItemId
  Object.assign(formData, {
    eventId: selectedEventId,
    sportItemId: null,
    scheduleId: null,
    entryType: 'INDIVIDUAL',
    registrationId: null,
    relayTeamId: null,
    resultStatus: 'VALID',
    isRecord: 0,
    score: null,
    scoreText: '',
    remark: ''
  })
  formSportItemOptions.value = []
  formScheduleOptions.value = []
  allFormScheduleOptions.value = []
  participantOptions.value = []
  relayTeamOptions.value = []
  if (selectedEventId) {
    await onEventChange(selectedEventId)
    if (selectedItemId) {
      formData.sportItemId = selectedItemId
      await onSportItemChange(selectedItemId)
    }
  }
  dialogVisible.value = true
}

async function handleEdit(row: any) {
  editingId.value = row.id
  dialogTitle.value = '编辑待复核成绩'
  Object.assign(formData, {
    eventId: row.eventId,
    sportItemId: row.sportItemId,
    scheduleId: row.scheduleId,
    entryType: row.relayTeamId ? 'RELAY' : 'INDIVIDUAL',
    registrationId: row.registrationId,
    relayTeamId: row.relayTeamId,
    resultStatus: row.resultStatus || 'VALID',
    isRecord: row.isRecord === 1 ? 1 : 0,
    score: row.score,
    scoreText: row.scoreText || '',
    remark: row.remark || ''
  })
  await onEventChange(row.eventId)
  formData.sportItemId = row.sportItemId
  await onSportItemChange(row.sportItemId)
  formData.scheduleId = row.scheduleId
  if (row.relayTeamId) formData.relayTeamId = row.relayTeamId
  else formData.registrationId = row.registrationId
  dialogVisible.value = true
}

async function onEventChange(eventId: number | null) {
  formData.sportItemId = null
  formData.scheduleId = null
  formData.registrationId = null
  formData.relayTeamId = null
  formSportItemOptions.value = []
  formScheduleOptions.value = []
  allFormScheduleOptions.value = []
  if (!eventId) return
  const [itemsRes, schedulesRes] = await Promise.all([
    get('/sport-items', { current: 1, size: 100, eventId }),
    get('/schedules', { current: 1, size: 100, eventId })
  ])
  if (itemsRes.code === 200) formSportItemOptions.value = itemsRes.data.records || []
  if (schedulesRes.code === 200) {
    allFormScheduleOptions.value = schedulesRes.data.records || []
    formScheduleOptions.value = [...allFormScheduleOptions.value]
  }
}

async function onSportItemChange(sportItemId: number | null) {
  formData.scheduleId = null
  formData.registrationId = null
  formData.relayTeamId = null
  participantOptions.value = []
  relayTeamOptions.value = []
  if (!sportItemId) {
    formScheduleOptions.value = []
    return
  }
  formScheduleOptions.value = allFormScheduleOptions.value.filter(s => s.sportItemId === sportItemId)
  const selected = formSportItemOptions.value.find(item => item.id === sportItemId)
  if (selected?.isRelay === 1 || selected?.isTeamEvent === 1) formData.entryType = 'RELAY'
  else formData.entryType = 'INDIVIDUAL'
  await loadParticipants()
}

async function loadParticipants() {
  formData.registrationId = null
  formData.relayTeamId = null
  if (!formData.eventId || !formData.sportItemId) return
  if (formData.entryType === 'RELAY') {
    const res = await get('/relay-teams', {
      current: 1, size: 100, eventId: formData.eventId, sportItemId: formData.sportItemId, status: 2
    })
    if (res.code === 200) relayTeamOptions.value = res.data.records || []
  } else {
    const res = await get('/registrations', {
      current: 1, size: 100, eventId: formData.eventId, sportItemId: formData.sportItemId, status: 'APPROVED'
    })
    if (res.code === 200) participantOptions.value = res.data.records || []
  }
}

function onResultStatusChange(status: string) {
  if (status !== 'VALID') {
    formData.score = null
    formData.isRecord = 0
    formData.scoreText = status
  } else if (formData.scoreText === 'DNS' || formData.scoreText === 'DNF' || formData.scoreText === 'DQ' || formData.scoreText === 'WD') {
    formData.scoreText = ''
  }
}

async function handleSubmit() {
  if (!formRef.value) return
  if (formData.entryType === 'INDIVIDUAL' && !formData.registrationId) {
    ElMessage.warning('请选择已通过审核的个人报名记录')
    return
  }
  if (formData.entryType === 'RELAY' && !formData.relayTeamId) {
    ElMessage.warning('请选择已审核通过的接力队伍')
    return
  }
  await formRef.value.validate(async valid => {
    if (!valid) return
    submitLoading.value = true
    try {
      const payload: any = {
        eventId: formData.eventId,
        sportItemId: formData.sportItemId,
        scheduleId: formData.scheduleId,
        registrationId: formData.entryType === 'INDIVIDUAL' ? formData.registrationId : null,
        relayTeamId: formData.entryType === 'RELAY' ? formData.relayTeamId : null,
        resultStatus: formData.resultStatus,
        isRecord: formData.resultStatus === 'VALID' ? formData.isRecord : 0,
        score: formData.resultStatus === 'VALID' ? formData.score : null,
        scoreText: formData.scoreText,
        remark: formData.remark
      }
      if (editingId.value) {
        await put(`/results/${editingId.value}`, payload)
        ElMessage.success('成绩已保存为待复核状态')
      } else {
        await post('/results', payload)
        ElMessage.success('成绩已录入，等待复核')
      }
      dialogVisible.value = false
      await loadData()
    } finally {
      submitLoading.value = false
    }
  })
}

async function reviewResult(row: any, status: number) {
  let remark = ''
  if (status === 3) {
    try {
      const result = await ElMessageBox.prompt('请填写退回修改原因', '退回成绩', {
        inputPattern: /\S+/,
        inputErrorMessage: '退回原因不能为空'
      })
      remark = result.value
    } catch {
      return
    }
  } else {
    await ElMessageBox.confirm('确认复核通过该条成绩？', '成绩复核', { type: 'warning' })
  }
  await post(`/results/${row.id}/review`, { status, remark })
  ElMessage.success(status === 2 ? '复核通过' : '已退回修改')
  await loadData()
}

async function publishSchedule(row: any) {
  await ElMessageBox.confirm(
    `该操作将正式发布赛程ID ${row.scheduleId} 的全部成绩。必须确保所有参赛者均已录入并复核通过。确认发布？`,
    '正式发布成绩',
    { type: 'warning', confirmButtonText: '确认发布' }
  )
  const res = await post(`/results/schedules/${row.scheduleId}/publish`)
  ElMessage.success(res.message || '赛程成绩已发布')
  await loadData()
}

async function viewAudit(row: any) {
  const res = await get(`/results/${row.id}/audit`)
  if (res.code === 200) {
    auditRows.value = res.data || []
    auditDialogVisible.value = true
  }
}

async function handleDelete(row: any) {
  await ElMessageBox.confirm('确定删除该条未发布成绩？', '删除成绩', { type: 'warning' })
  await del(`/results/${row.id}`)
  ElMessage.success('成绩记录已删除')
  await loadData()
}

async function handleExport() {
  if (!queryParams.eventId) {
    ElMessage.warning('请先选择赛事')
    return
  }
  const res = await get('/reports/results.csv', { eventId: queryParams.eventId })
  downloadCsv(res.data, `正式成绩-${queryParams.eventId}.csv`)
}

function downloadCsv(content: string, filename: string) {
  const blob = new Blob([content || ''], { type: 'text/csv;charset=utf-8;' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = filename
  link.click()
  URL.revokeObjectURL(url)
}

function roundName(type: string) {
  const map: Record<string, string> = { PRELIMINARY: '预赛', SEMIFINAL: '半决赛', FINAL: '决赛', FIELD_FINAL: '田赛决赛' }
  return map[type] || type || '决赛'
}
function resultStatusName(status: string) {
  const map: Record<string, string> = { VALID: '有效', DNS: '未出赛', DNF: '未完成', DQ: '取消资格', WD: '弃权' }
  return map[status] || status || '待录入'
}
function resultType(status: string): 'success' | 'warning' | 'info' | 'danger' {
  const map: Record<string, 'success' | 'warning' | 'info' | 'danger'> = {
    VALID: 'success', DNS: 'warning', DNF: 'warning', DQ: 'danger', WD: 'info'
  }
  return map[status] || 'info'
}
function reviewStatusName(status: number) {
  const map: Record<number, string> = { 1: '待复核', 2: '已复核', 3: '已退回' }
  return map[status] || '待复核'
}
function reviewType(status: number): 'success' | 'warning' | 'info' | 'danger' {
  const map: Record<number, 'success' | 'warning' | 'info' | 'danger'> = { 1: 'warning', 2: 'success', 3: 'danger' }
  return map[status] || 'info'
}
</script>

<style scoped>
.audit-json {
  white-space: pre-wrap;
  overflow-wrap: anywhere;
  max-height: 180px;
  overflow: auto;
  margin: 0;
  font-size: 11px;
}
.toolbar {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 14px;
}
</style>
