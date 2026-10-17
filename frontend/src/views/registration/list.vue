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
        <el-form-item label="学院">
          <el-input v-model="queryParams.college" placeholder="输入学院名称" clearable />
        </el-form-item>
        <el-form-item label="班级">
          <el-input v-model="queryParams.className" placeholder="输入班级名称" clearable />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="queryParams.status" placeholder="请选择" clearable>
            <el-option label="待审核" value="PENDING" />
            <el-option label="已通过" value="APPROVED" />
            <el-option label="已拒绝" value="REJECTED" />
            <el-option label="已取消" value="CANCELLED" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadData">查询</el-button>
          <el-button @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>

      <!-- 数据表格 -->
      <el-table :data="tableData" v-loading="loading" border stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="eventName" label="赛事名称" min-width="150" />
        <el-table-column prop="sportItemName" label="运动项目" width="120" />
        <el-table-column prop="realName" label="报名人" width="100" />
        <el-table-column prop="studentNo" label="学号" width="120" />
        <el-table-column prop="college" label="学院" min-width="140" />
        <el-table-column prop="className" label="班级" min-width="120" />
        <el-table-column label="参赛组别" width="165"><template #default="{ row }">{{ groupName(row.competitionGroup) }}</template></el-table-column>
        <el-table-column prop="phone" label="联系电话" width="120" />
        <el-table-column prop="statusName" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">{{ row.statusName }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="报名时间" width="160" />
        <el-table-column prop="remark" label="备注" min-width="150" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <template v-if="row.status === 'PENDING'">
              <el-button type="success" link @click="handleApprove(row)">通过</el-button>
              <el-button type="danger" link @click="handleReject(row)">拒绝</el-button>
            </template>
            <el-button type="primary" link @click="handleView(row)">详情</el-button>
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

    <!-- 详情弹窗 -->
    <el-dialog v-model="detailDialogVisible" title="报名详情" width="600px">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="赛事名称">{{ currentRow.eventName }}</el-descriptions-item>
        <el-descriptions-item label="运动项目">{{ currentRow.sportItemName }}</el-descriptions-item>
        <el-descriptions-item label="报名人">{{ currentRow.realName || currentRow.username }}</el-descriptions-item>
        <el-descriptions-item label="学号">{{ currentRow.studentNo }}</el-descriptions-item>
        <el-descriptions-item label="学院">{{ currentRow.college }}</el-descriptions-item>
        <el-descriptions-item label="班级">{{ currentRow.className }}</el-descriptions-item>
        <el-descriptions-item label="参赛组别">{{ groupName(currentRow.competitionGroup) }}</el-descriptions-item>
        <el-descriptions-item label="联系电话">{{ currentRow.phone }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="getStatusType(currentRow.status)">{{ currentRow.statusName }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="报名时间">{{ currentRow.createTime }}</el-descriptions-item>
        <el-descriptions-item label="审核时间">{{ currentRow.reviewTime }}</el-descriptions-item>
        <el-descriptions-item label="审核人">{{ currentRow.reviewerName }}</el-descriptions-item>
        <el-descriptions-item label="审核意见">{{ currentRow.reviewRemark }}</el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">{{ currentRow.remark }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>

    <!-- 拒绝原因弹窗 -->
    <el-dialog v-model="rejectDialogVisible" title="拒绝报名" width="400px">
      <el-form ref="rejectFormRef" :model="rejectForm" :rules="rejectRules" label-width="80px">
        <el-form-item label="拒绝原因" prop="reason">
          <el-input v-model="rejectForm.reason" type="textarea" :rows="3" placeholder="请输入拒绝原因" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="rejectDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="rejectLoading" @click="submitReject">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox, FormInstance, FormRules } from 'element-plus'
import { get, post } from '@/utils/request'

const loading = ref(false)
const tableData = ref<any[]>([])
const total = ref(0)
const eventOptions = ref<any[]>([])

const queryParams = reactive({
  current: 1,
  size: 10,
  eventId: null as number | null,
  college: '',
  className: '',
  status: ''
})

const detailDialogVisible = ref(false)
const currentRow = ref<any>({})

const rejectDialogVisible = ref(false)
const rejectLoading = ref(false)
const rejectFormRef = ref<FormInstance>()
const rejectForm = reactive({
  reason: ''
})
const rejectRules: FormRules = {
  reason: [{ required: true, message: '请输入拒绝原因', trigger: 'blur' }]
}
const rejectingId = ref<number | null>(null)

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
    const res = await get('/registrations', queryParams)
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
  queryParams.college = ''
  queryParams.className = ''
  queryParams.status = ''
  queryParams.current = 1
  loadData()
}

function groupName(group: string) {
  if (group === 'A') return '甲组 · 普通本科组'
  if (group === 'B') return '乙组 · 体育专业组'
  return '待核定'
}

function getStatusType(status: string): 'success' | 'warning' | 'info' | 'primary' | 'danger' {
  const map: Record<string, 'success' | 'warning' | 'info' | 'primary' | 'danger'> = {
    PENDING: 'warning',
    APPROVED: 'success',
    REJECTED: 'danger',
    CANCELLED: 'info'
  }
  return map[status] || 'info'
}

function handleView(row: any) {
  currentRow.value = row
  detailDialogVisible.value = true
}

async function handleApprove(row: any) {
  await ElMessageBox.confirm('确定要通过该报名申请吗？', '提示', { type: 'warning' })
  await post(`/registrations/${row.id}/approve`)
  ElMessage.success('审核通过')
  loadData()
}

function handleReject(row: any) {
  rejectingId.value = row.id
  rejectForm.reason = ''
  rejectDialogVisible.value = true
}

async function submitReject() {
  if (!rejectFormRef.value) return
  await rejectFormRef.value.validate(async (valid: boolean) => {
    if (!valid) return
    rejectLoading.value = true
    try {
      await post(`/registrations/${rejectingId.value}/reject`, { reason: rejectForm.reason })
      ElMessage.success('已拒绝')
      rejectDialogVisible.value = false
      loadData()
    } finally {
      rejectLoading.value = false
    }
  })
}
</script>