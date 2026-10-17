<template>
  <div class="page-container">
    <el-card shadow="never" v-loading="loading">
      <template #header>
        <div class="card-header">
          <span>{{ isStaff ? '接力/集体项目队伍审核' : '接力/集体项目组队报名' }}</span>
          <el-button v-if="!isStaff" type="primary" @click="openCreate">创建接力队伍</el-button>
        </div>
      </template>

      <el-form v-if="isStaff" :inline="true" class="search-form">
        <el-form-item label="学院">
          <el-input v-model="query.college" placeholder="输入学院名称" clearable />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部状态" clearable>
            <el-option label="待审核" :value="1" />
            <el-option label="已通过" :value="2" />
            <el-option label="已拒绝" :value="3" />
            <el-option label="已取消" :value="4" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadTeams">查询</el-button>
          <el-button @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="teams" border stripe>
        <el-table-column prop="teamName" label="队伍名称" min-width="150" />
        <el-table-column prop="eventName" label="赛事" min-width="160" />
        <el-table-column prop="sportItemName" label="参赛项目" min-width="160" />
        <el-table-column prop="college" label="学院" min-width="140" />
        <el-table-column prop="creatorName" label="队长" width="100" />
        <el-table-column label="队员名单" min-width="250">
          <template #default="{ row }">
            <span>{{ formatMembers(row.members) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="statusName" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)">{{ row.statusName }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="reviewRemark" label="审核意见" min-width="140" show-overflow-tooltip />
        <el-table-column label="操作" width="210" fixed="right">
          <template #default="{ row }">
            <template v-if="isStaff && row.status === 1">
              <el-button type="success" link @click="reviewTeam(row, 2)">通过</el-button>
              <el-button type="danger" link @click="reviewTeam(row, 3)">拒绝</el-button>
            </template>
            <template v-else-if="!isStaff && row.status === 1 && row.creatorUserId === currentUserId">
              <el-button type="primary" link @click="openEdit(row)">调整队员</el-button>
              <el-button type="danger" link @click="cancelTeam(row)">取消</el-button>
            </template>
            <el-button type="primary" link @click="viewTeam(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!loading && teams.length === 0" description="暂无接力队伍记录" />
    </el-card>

    <el-dialog v-model="dialogVisible" :title="editingTeamId ? '调整队伍成员' : (selectedItem?.isRelay === 1 ? '创建接力队伍' : '创建集体项目队伍')" width="620px" destroy-on-close>
      <el-form ref="formRef" :model="formData" :rules="rules" label-width="110px">
        <el-form-item label="赛事" prop="eventId">
          <el-select v-model="formData.eventId" placeholder="请选择赛事" filterable :disabled="!!editingTeamId" @change="onEventChange">
            <el-option v-for="event in eventOptions" :key="event.id" :label="event.name" :value="event.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="参赛项目" prop="sportItemId">
          <el-select v-model="formData.sportItemId" placeholder="请选择接力项目" filterable :disabled="!!editingTeamId" @change="onItemChange">
            <el-option v-for="item in relayItems" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="队伍名称" prop="teamName">
          <el-input v-model="formData.teamName" placeholder="例如：体育学院男子4×100米队" :disabled="!!editingTeamId" />
        </el-form-item>
        <el-form-item label="队员要求">
          <span v-if="selectedItem">
            {{ requiredMemberCount }} 名队伍成员，最多 {{ reserveMemberCount }} 名替补。当前账号自动加入队伍；混合接力按女、男、女、男顺序自动安排正式棒次。{{ rosterRuleHint }}
          </span>
          <span v-else>请先选择接力项目</span>
        </el-form-item>
        <el-form-item label="选择队友" prop="memberUserIds">
          <el-select
            v-model="formData.memberUserIds"
            multiple
            filterable
            remote
            :remote-method="searchMembers"
            :loading="membersLoading"
            placeholder="按姓名、学号或班级搜索同学院学生"
            style="width: 100%"
            :multiple-limit="memberSelectionLimit"
          >
            <el-option
              v-for="member in memberOptions"
              :key="member.userId"
              :label="`${member.realName || member.username} · ${member.className || '未填写班级'} · ${member.studentNo || '无学号'}`"
              :value="member.userId"
            />
          </el-select>
          <div class="form-tip">系统会自动加入当前账号作为队长。请只选择其他队员；队员必须来自同一学院，乙组队伍还须属于同一年级/专业班级。</div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="submitTeam">
          {{ editingTeamId ? '保存队员名单' : '提交审核' }}
        </el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="detailVisible" title="接力队伍详情" width="560px">
      <el-descriptions :column="1" border v-if="currentTeam">
        <el-descriptions-item label="队伍名称">{{ currentTeam.teamName }}</el-descriptions-item>
        <el-descriptions-item label="赛事">{{ currentTeam.eventName }}</el-descriptions-item>
        <el-descriptions-item label="项目">{{ currentTeam.sportItemName }}</el-descriptions-item>
        <el-descriptions-item label="学院">{{ currentTeam.college }}</el-descriptions-item>
        <el-descriptions-item label="队员">
          <el-timeline>
            <el-timeline-item v-for="member in currentTeam.members || []" :key="member.userId">
              {{ member.reserve ? '替补' : (currentTeam.isRelay === 1 ? `第 ${member.legOrder} 棒` : `队员 ${member.legOrder}`) }}：{{ member.realName || member.username }}
              （{{ member.studentNo }}，{{ member.className || '未填写班级' }}）
            </el-timeline-item>
          </el-timeline>
        </el-descriptions-item>
        <el-descriptions-item label="审核意见">{{ currentTeam.reviewRemark || '暂无' }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox, FormInstance, FormRules } from 'element-plus'
import { get, post, put } from '@/utils/request'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const userStore = useUserStore()
const currentUserId = computed(() => userStore.userInfo?.userId)
const isStaff = computed(() => ['ADMIN', 'TEACHER'].includes(userStore.userInfo?.role || ''))
const loading = ref(false)
const membersLoading = ref(false)
const submitLoading = ref(false)
const teams = ref<any[]>([])
const eventOptions = ref<any[]>([])
const relayItems = ref<any[]>([])
const memberOptions = ref<any[]>([])
const dialogVisible = ref(false)
const detailVisible = ref(false)
const currentTeam = ref<any>(null)
const editingTeamId = ref<number | null>(null)
const formRef = ref<FormInstance>()
const query = reactive({ college: '', status: null as number | null })
const formData = reactive({
  eventId: null as number | null,
  sportItemId: null as number | null,
  teamName: '',
  memberUserIds: [] as number[]
})
const rules: FormRules = {
  eventId: [{ required: true, message: '请选择赛事', trigger: 'change' }],
  sportItemId: [{ required: true, message: '请选择接力项目', trigger: 'change' }],
  teamName: [{ required: true, message: '请输入队伍名称', trigger: 'blur' }],
  memberUserIds: [{ validator: (_rule: any, value: number[], callback: (error?: Error) => void) => {
    const required = requiredMemberCount.value - 1
    const max = required + reserveMemberCount.value
    if (!editingTeamId.value && (!value || value.length < required || value.length > max)) {
      callback(new Error(`请为队长选择 ${required} 至 ${max} 名队友`))
      return
    }
    callback()
  }, trigger: 'change' }]
}
const selectedItem = computed(() => relayItems.value.find(item => item.id === formData.sportItemId))
const requiredMemberCount = computed(() => {
  if (!selectedItem.value) return 4
  return Number(selectedItem.value.isRelay === 1
    ? selectedItem.value.relayMemberCount || 4
    : selectedItem.value.teamMemberCount || 0)
})
const reserveMemberCount = computed(() => {
  if (!selectedItem.value) return 0
  return Number(selectedItem.value.isRelay === 1
    ? selectedItem.value.relayReserveCount || 0
    : selectedItem.value.teamReserveCount || 0)
})
const memberSelectionLimit = computed(() => {
  if (!selectedItem.value) return 0
  return Math.max(0, requiredMemberCount.value - 1 + reserveMemberCount.value)
})
const rosterRuleHint = computed(() => {
  const rule = selectedItem.value?.teamRosterRule
  const hints: Record<string, string> = {
    MIXED_5M5F: '正式队员须5男5女。',
    MIXED_10M10F: '正式队员须10男10女。',
    MIXED_15M15F: '正式队员须15男15女。',
    MIXED_3M3F: '正式队员须3男3女。',
    MIXED_2M2F_MIN: '正式队员共5人，至少2男2女。',
    PAIRS_1M1F: '请按组合顺序选择；每连续2名视为一组，须1男1女。',
    MIXED_MIN_6F: '请按板鞋小组顺序选择；每连续3名视为一组，每组至少1名女生。',
    CUSTOM: '该项目规程存在需要人工确认的队员构成要求，请先向竞赛委员会核实。'
  }
  return rule ? ` ${hints[rule] || ''}` : ''
})

onMounted(async () => {
  await loadEvents()
  await loadTeams()
  if (!isStaff.value && route.query.eventId && route.query.sportItemId) {
    await openCreate()
    const eventId = Number(route.query.eventId)
    const sportItemId = Number(route.query.sportItemId)
    formData.eventId = eventId
    await onEventChange(eventId)
    if (relayItems.value.some(item => item.id === sportItemId)) {
      formData.sportItemId = sportItemId
      await loadEligibleMembers('', true)
    }
  }
})

async function loadEvents() {
  const endpoint = isStaff.value ? '/events' : '/events/available'
  const res = isStaff.value ? await get(endpoint, { current: 1, size: 100 }) : await get(endpoint)
  if (res.code === 200) {
    eventOptions.value = isStaff.value ? (res.data.records || []) : (res.data || [])
  }
}

async function loadTeams() {
  loading.value = true
  try {
    if (isStaff.value) {
      const res = await get('/relay-teams', { current: 1, size: 100, college: query.college, status: query.status })
      if (res.code === 200) teams.value = res.data.records || []
    } else {
      const res = await get('/relay-teams/my')
      if (res.code === 200) teams.value = res.data || []
    }
  } finally {
    loading.value = false
  }
}

function resetQuery() {
  query.college = ''
  query.status = null
  loadTeams()
}

async function openCreate() {
  editingTeamId.value = null
  Object.assign(formData, { eventId: null, sportItemId: null, teamName: '', memberUserIds: [] })
  relayItems.value = []
  memberOptions.value = []
  dialogVisible.value = true
}

async function onEventChange(eventId: number) {
  formData.sportItemId = null
  formData.memberUserIds = []
  relayItems.value = []
  memberOptions.value = []
  if (!eventId) return
  const res = await get('/sport-items', { eventId, current: 1, size: 100 })
  if (res.code === 200) {
    relayItems.value = (res.data.records || []).filter((item: any) => (item.isRelay === 1 || item.isTeamEvent === 1) && item.status === 1)
    if (relayItems.value.length === 1) {
      formData.sportItemId = relayItems.value[0].id
      loadEligibleMembers('', true)
    }
  }
}

async function loadEligibleMembers(keyword = '', resetSelection = false) {
  if (resetSelection) formData.memberUserIds = []
  membersLoading.value = true
  try {
    const res = await get('/relay-teams/eligible-members', {
      sportItemId: formData.sportItemId,
      teamId: editingTeamId.value,
      keyword
    })
    if (res.code === 200) {
      memberOptions.value = (res.data || []).filter((member: any) => member.userId !== currentUserId.value)
    }
  } finally {
    membersLoading.value = false
  }
}

async function onItemChange() {
  await loadEligibleMembers('', true)
}

async function searchMembers(keyword: string) {
  await loadEligibleMembers(keyword, false)
}

async function openEdit(row: any) {
  editingTeamId.value = row.id
  Object.assign(formData, {
    eventId: row.eventId,
    sportItemId: row.sportItemId,
    teamName: row.teamName,
    memberUserIds: (row.members || []).filter((m: any) => m.userId !== currentUserId.value).map((m: any) => m.userId)
  })
  await onEventChange(row.eventId)
  formData.sportItemId = row.sportItemId
  await loadEligibleMembers('', false)
  formData.memberUserIds = (row.members || []).filter((m: any) => m.userId !== currentUserId.value).map((m: any) => m.userId)
  dialogVisible.value = true
}

async function submitTeam() {
  if (!formRef.value) return
  await formRef.value.validate(async valid => {
    if (!valid) return
    submitLoading.value = true
    try {
      if (editingTeamId.value) {
        await put(`/relay-teams/${editingTeamId.value}/members`, formData.memberUserIds)
        ElMessage.success('队员名单已更新')
      } else {
        await post('/relay-teams', formData)
        ElMessage.success('接力队伍已提交审核')
      }
      dialogVisible.value = false
      await loadTeams()
    } finally {
      submitLoading.value = false
    }
  })
}

async function reviewTeam(row: any, status: number) {
  let remark = ''
  if (status === 3) {
    try {
      const result = await ElMessageBox.prompt('请输入拒绝原因', '拒绝接力队伍', {
        inputPattern: /\S+/,
        inputErrorMessage: '拒绝原因不能为空'
      })
      remark = result.value
    } catch {
      return
    }
  } else {
    await ElMessageBox.confirm('确认审核通过该接力队伍？', '提示', { type: 'warning' })
  }
  await post(`/relay-teams/${row.id}/review?status=${status}&remark=${encodeURIComponent(remark)}`)
  ElMessage.success(status === 2 ? '审核通过' : '已拒绝')
  await loadTeams()
}

async function cancelTeam(row: any) {
  await ElMessageBox.confirm('确认取消该接力队伍？', '提示', { type: 'warning' })
  await post(`/relay-teams/${row.id}/cancel`)
  ElMessage.success('已取消')
  await loadTeams()
}

async function viewTeam(row: any) {
  const res = await get(`/relay-teams/${row.id}`)
  if (res.code === 200) {
    currentTeam.value = res.data
    detailVisible.value = true
  }
}

function formatMembers(members: any[] = []) {
  return members.map(member => `${member.realName || member.username}${member.reserve ? '（替补）' : `（第${member.legOrder}棒）`}`).join('、')
}

function statusType(status: number): 'success' | 'warning' | 'info' | 'primary' | 'danger' {
  const map: Record<number, 'success' | 'warning' | 'info' | 'primary' | 'danger'> = {
    1: 'warning',
    2: 'success',
    3: 'danger',
    4: 'info'
  }
  return map[status] || 'info'
}
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.form-tip {
  display: block;
  width: 100%;
  color: var(--el-text-color-secondary);
  font-size: 12px;
  line-height: 1.5;
}
</style>
