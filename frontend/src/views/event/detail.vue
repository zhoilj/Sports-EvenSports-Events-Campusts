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
        <el-descriptions-item label="每人限报">{{ eventData.maxItemsPerUser > 0 ? eventData.maxItemsPerUser + ' 个个人项目' : '不限' }}</el-descriptions-item>
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
        <el-table-column prop="type" label="类别" width="100" />
        <el-table-column prop="genderLimit" label="性别限制" width="100">
          <template #default="{ row }">{{ row.genderLimit === 1 ? '仅男' : row.genderLimit === 2 ? '仅女' : '不限' }}</template>
        </el-table-column>
        <el-table-column prop="maxParticipants" label="人数上限" width="100">
          <template #default="{ row }">{{ row.maxParticipants > 0 ? row.maxParticipants : '不限' }}</template>
        </el-table-column>
        <el-table-column prop="currentParticipants" label="有效报名" width="100" />
        <el-table-column prop="isRelay" label="项目形式" width="100">
          <template #default="{ row }">{{ row.isRelay === 1 ? '接力项目' : '个人项目' }}</template>
        </el-table-column>
        <el-table-column prop="rules" label="比赛规则" />
        <el-table-column label="操作" width="120" v-if="canRegister">
          <template #default="{ row }">
            <el-button 
              type="primary" 
              link 
              @click="handleRegisterItem(row)"
              :disabled="isItemFull(row) || row.isRelay === 1"
            >
              {{ row.isRelay === 1 ? '接力组队报名' : isItemFull(row) ? '已满' : '报名' }}
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
          <el-select v-model="registerForm.sportItemId" placeholder="请选择">
            <el-option 
              v-for="item in availableItems" 
              :key="item.id" 
              :label="item.name" 
              :value="item.id"
              :disabled="item.isRelay === 1 || isItemFull(item)"
            />
          </el-select>
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
  remark: ''
})

const registerRules: FormRules = {
  sportItemId: [{ required: true, message: '请选择运动项目', trigger: 'change' }]
}

const canRegister = computed(() => {
  if (userStore.userInfo?.role !== 'STUDENT' || Number(eventData.value.status) !== 2) return false
  const today = new Date()
  const todayStart = new Date(today.getFullYear(), today.getMonth(), today.getDate()).getTime()
  const start = eventData.value.regStartDate
    ? new Date(`${eventData.value.regStartDate}T00:00:00`).getTime()
    : null
  const end = eventData.value.regEndDate
    ? new Date(`${eventData.value.regEndDate}T23:59:59`).getTime()
    : null
  return (start === null || todayStart >= start) && (end === null || today.getTime() <= end)
})

const availableItems = computed(() => {
  return sportItems.value.filter(item => item.isRelay !== 1 && !isItemFull(item) && item.status === 1)
})

function isItemFull(item: any) {
  const max = Number(item.maxParticipants || 0)
  const current = Number(item.currentParticipants || 0)
  return max > 0 && current >= max
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

function getStatusType(status: number): 'success' | 'warning' | 'info' | 'primary' | 'danger' {
  const map: Record<number, 'success' | 'warning' | 'info' | 'primary' | 'danger'> = {
    1: 'info',
    2: 'primary',
    3: 'success',
    4: 'warning'
  }
  return map[status] || 'info'
}

function getScheduleStatusType(status: number): 'success' | 'warning' | 'info' | 'primary' | 'danger' {
  const map: Record<number, 'success' | 'warning' | 'info' | 'primary' | 'danger'> = {
    1: 'info',
    2: 'primary',
    3: 'success',
    4: 'danger'
  }
  return map[status] || 'info'
}

function handleRegister() {
  registerForm.sportItemId = null
  registerForm.remark = ''
  registerDialogVisible.value = true
}

function handleRegisterItem(item: any) {
  registerForm.sportItemId = item.id
  registerForm.remark = ''
  registerDialogVisible.value = true
}

async function submitRegister() {
  if (!registerFormRef.value) return
  await registerFormRef.value.validate(async (valid: boolean) => {
    if (!valid) return
    registerLoading.value = true
    try {
      await post('/registrations', {
        eventId: eventData.value.id,
        sportItemId: registerForm.sportItemId,
        remark: registerForm.remark
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
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>