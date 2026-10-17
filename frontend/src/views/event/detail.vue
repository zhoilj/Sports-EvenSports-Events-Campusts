<template>
  <div class="page-container">
    <el-card shadow="never" v-loading="loading">
      <template #header>
        <div class="card-header">
          <span>赛事详情</span>
          <el-button @click="goBack">返回列表</el-button>
        </div>
      </template>

      <el-descriptions :column="2" border>
        <el-descriptions-item label="赛事名称">{{ eventData.name }}</el-descriptions-item>
        <el-descriptions-item label="赛事类型">{{ eventData.typeName }}</el-descriptions-item>
        <el-descriptions-item label="开始日期">{{ eventData.startDate }}</el-descriptions-item>
        <el-descriptions-item label="结束日期">{{ eventData.endDate }}</el-descriptions-item>
        <el-descriptions-item label="报名开始">{{ eventData.regStartDate }}</el-descriptions-item>
        <el-descriptions-item label="报名截止">{{ eventData.regEndDate }}</el-descriptions-item>
        <el-descriptions-item label="举办地点">{{ eventData.location }}</el-descriptions-item>
        <el-descriptions-item label="主办单位">{{ eventData.organizer }}</el-descriptions-item>
        <el-descriptions-item label="联系电话">{{ eventData.contactPhone }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="getStatusType(eventData.status)">{{ eventData.statusName }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="赛事描述" :span="2">{{ eventData.description }}</el-descriptions-item>
      </el-descriptions>

      <!-- 报名按钮 -->
      <div class="mt-20" v-if="canRegister">
        <el-button type="primary" size="large" @click="handleRegister">立即报名</el-button>
      </div>
    </el-card>

    <!-- 运动项目列表 -->
    <el-card shadow="never" class="mt-20">
      <template #header>
        <span>运动项目</span>
      </template>
      <el-table :data="sportItems" border stripe>
        <el-table-column prop="name" label="项目名称" />
        <el-table-column prop="categoryName" label="类别" width="100" />
        <el-table-column prop="genderName" label="性别要求" width="100" />
        <el-table-column prop="maxParticipants" label="人数上限" width="100" />
        <el-table-column prop="currentParticipants" label="已报名" width="100" />
        <el-table-column label="参赛方式" width="100">
          <template #default="{ row }">{{ row.participationMode === 'RELAY' ? '接力' : '个人' }}</template>
        </el-table-column>
        <el-table-column label="队员人数" width="100">
          <template #default="{ row }">{{ row.participationMode === 'RELAY' ? row.teamMemberCount : '—' }}</template>
        </el-table-column>
        <el-table-column prop="rules" label="比赛规则" />
        <el-table-column label="操作" width="120" v-if="canRegister">
          <template #default="{ row }">
            <el-button 
              type="primary" 
              link 
              @click="handleRegisterItem(row)"
              :disabled="isFull(row)"
            >
              {{ isFull(row) ? '已满' : '报名' }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 赛程安排 -->
    <el-card shadow="never" class="mt-20">
      <template #header>
        <span>赛程安排</span>
      </template>
      <el-timeline v-if="schedules.length > 0">
        <el-timeline-item
          v-for="item in schedules"
          :key="item.id"
          :timestamp="item.matchDate + ' ' + item.startTime"
          placement="top"
        >
          <el-card shadow="hover">
            <h4>{{ item.sportItemName }} - {{ item.roundName }}</h4>
            <p>地点：{{ item.venue }}</p>
            <p v-if="item.groupName">分组：{{ item.groupName }}</p>
            <p>状态：<el-tag :type="getScheduleStatusType(item.status)">{{ item.statusName }}</el-tag></p>
          </el-card>
        </el-timeline-item>
      </el-timeline>
      <el-empty v-else description="暂无赛程安排" />
    </el-card>

    <!-- 报名弹窗 -->
    <el-dialog v-model="registerDialogVisible" title="赛事报名" width="500px">
      <el-form ref="registerFormRef" :model="registerForm" :rules="registerRules" label-width="100px">
        <el-form-item label="运动项目" prop="sportItemId">
          <el-select v-model="registerForm.sportItemId" placeholder="请选择" @change="handleSportItemChange">
            <el-option
              v-for="item in availableItems"
              :key="item.id"
              :label="item.name"
              :value="item.id"
              :disabled="isFull(item)"
            />
          </el-select>
        </el-form-item>
        <el-form-item v-if="selectedSportItem?.participationMode === 'RELAY'" label="接力队名" prop="teamName">
          <el-input v-model="registerForm.teamName" maxlength="100" show-word-limit placeholder="请输入接力队名称" />
        </el-form-item>
        <el-form-item v-if="selectedSportItem?.participationMode === 'RELAY'" label="接力队员" prop="memberUserIds">
          <el-select
            v-model="registerForm.memberUserIds"
            multiple
            filterable
            remote
            reserve-keyword
            style="width: 100%"
            placeholder="选择同学院队员，名单必须包含本人"
            :remote-method="loadEligibleAthletes"
            @visible-change="handleAthleteSelectVisible"
          >
            <el-option
              v-for="athlete in eligibleAthletes"
              :key="athlete.userId"
              :label="`${athlete.realName}（${athlete.studentNo}）`"
              :value="athlete.userId"
            />
          </el-select>
          <div class="form-tip">该项目要求 {{ selectedSportItem?.teamMemberCount }} 名队员，队员必须来自同一学院。</div>
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="registerForm.remark" type="textarea" :rows="3" placeholder="请输入备注信息" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="registerDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="registerLoading" @click="submitRegister">确定报名</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, FormInstance, FormRules } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { get, post } from '@/utils/request'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const eventData = ref<any>({})
const sportItems = ref<any[]>([])
const schedules = ref<any[]>([])

const registerDialogVisible = ref(false)
const registerLoading = ref(false)
const registerFormRef = ref<FormInstance>()
const registerForm = reactive({
  sportItemId: null as number | null,
  remark: '',
  teamName: '',
  memberUserIds: [] as number[]
})
const eligibleAthletes = ref<any[]>([])
const selectedSportItem = computed(() =>
  sportItems.value.find(item => item.id === registerForm.sportItemId)
)

const registerRules: FormRules = {
  sportItemId: [{ required: true, message: '请选择运动项目', trigger: 'change' }]
}

const canRegister = computed(() => {
  const start = eventData.value.regStartDate
  const end = eventData.value.regEndDate
  const now = new Date()
  return userStore.userInfo?.role === 'STUDENT'
    && Number(eventData.value.status) === 2
    && !!start && !!end
    && now >= new Date(`${start}T00:00:00`)
    && now <= new Date(`${end}T23:59:59`)
})

const availableItems = computed(() =>
  sportItems.value.filter(item => Number(item.status) === 1 && !isFull(item))
)

function isFull(item: any) {
  const limit = Number(item.maxParticipants ?? 0)
  return limit > 0 && Number(item.currentParticipants ?? 0) >= limit
}

onMounted(() => {
  loadEventDetail()
})

async function loadEventDetail() {
  const eventId = route.params.id
  loading.value = true
  try {
    // 加载赛事详情
    const res = await get(`/events/${eventId}`)
    if (res.code === 200) {
      eventData.value = res.data
    }
    
    // 加载运动项目
    const itemsRes = await get(`/sport-items`, { eventId, size: 100 })
    if (itemsRes.code === 200) {
      sportItems.value = itemsRes.data.records || []
    }
    
    // 加载赛程
    const schedulesRes = await get(`/schedules`, { eventId, size: 100 })
    if (schedulesRes.code === 200) {
      schedules.value = schedulesRes.data.records || []
    }
  } finally {
    loading.value = false
  }
}

function goBack() {
  router.push('/events')
}

function getStatusType(status: string | number) {
  const map: Record<string, string> = {
    '1': 'info',
    '2': 'primary',
    '3': 'success',
    '4': 'warning'
  }
  return map[String(status)] || 'info'
}

function getScheduleStatusType(status: string) {
  const map: Record<string, string> = {
    PENDING: 'info',
    ONGOING: 'primary',
    COMPLETED: 'success',
    CANCELLED: 'danger'
  }
  return map[status] || 'info'
}

function resetRegisterForm() {
  registerForm.sportItemId = null
  registerForm.remark = ''
  registerForm.teamName = ''
  registerForm.memberUserIds = []
}

function handleRegister() {
  resetRegisterForm()
  registerDialogVisible.value = true
}

function handleRegisterItem(item: any) {
  resetRegisterForm()
  registerForm.sportItemId = item.id
  registerDialogVisible.value = true
  handleSportItemChange()
}

function handleSportItemChange() {
  const item = selectedSportItem.value
  registerForm.teamName = ''
  if (item?.participationMode === 'RELAY') {
    const currentUserId = Number(userStore.userInfo?.userId)
    registerForm.memberUserIds = currentUserId ? [currentUserId] : []
    loadEligibleAthletes('')
  } else {
    registerForm.memberUserIds = []
  }
}

function handleAthleteSelectVisible(visible: boolean) {
  if (visible && eligibleAthletes.value.length === 0) {
    loadEligibleAthletes('')
  }
}

async function loadEligibleAthletes(keyword: string) {
  const res = await get('/registrations/eligible-athletes', { keyword })
  if (res.code === 200) {
    eligibleAthletes.value = res.data || []
  }
}

async function submitRegister() {
  if (!registerFormRef.value) return
  await registerFormRef.value.validate(async (valid: boolean) => {
    if (!valid) return
    registerLoading.value = true
    try {
      const selectedItem = selectedSportItem.value
      if (selectedItem?.participationMode === 'RELAY') {
        if (!registerForm.teamName.trim()) {
          ElMessage.warning('请填写接力队名称')
          return
        }
        if (registerForm.memberUserIds.length !== Number(selectedItem.teamMemberCount)) {
          ElMessage.warning(`该项目要求 ${selectedItem.teamMemberCount} 名队员，请检查名单`)
          return
        }
      }
      await post('/registrations', {
        eventId: eventData.value.id,
        sportItemId: registerForm.sportItemId,
        remark: registerForm.remark,
        ...(selectedItem?.participationMode === 'RELAY'
          ? { teamName: registerForm.teamName.trim(), memberUserIds: registerForm.memberUserIds }
          : {})
      })
      ElMessage.success('报名成功，请等待审核')
      registerDialogVisible.value = false
      loadEventDetail()
    } finally {
      registerLoading.value = false
    }
  })
}
</script>

<style scoped>
.form-tip {
  margin-top: 6px;
  color: var(--el-text-color-secondary);
  font-size: 12px;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>