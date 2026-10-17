<template>
  <div class="page-container">
    <el-card shadow="never">
      <!-- 搜索表单 -->
      <el-form :inline="true" class="search-form">
        <el-form-item label="赛事名称">
          <el-input v-model="queryParams.name" placeholder="请输入赛事名称" clearable />
        </el-form-item>
        <el-form-item label="赛事类型">
          <el-select v-model="queryParams.type" placeholder="请选择" clearable>
            <el-option label="田径" value="TRACK_FIELD" />
            <el-option label="球类" value="BALL" />
            <el-option label="游泳" value="SWIMMING" />
            <el-option label="其他" value="OTHER" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="queryParams.status" placeholder="请选择" clearable>
            <el-option label="草稿"  :value="1" />
            <el-option label="报名中" :value="2" />
            <el-option label="进行中"  :value="3" />
            <el-option label="已结束"  :value="4" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadData">查询</el-button>
          <el-button @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>

      <!-- 操作按钮 -->
      <div class="mb-10">
        <el-button type="primary" @click="handleAdd">
          <el-icon><Plus /></el-icon>新增赛事
        </el-button>
      </div>

      <!-- 数据表格 -->
      <el-table :data="tableData" v-loading="loading" border stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="name" label="赛事名称" min-width="150" />
        <el-table-column prop="typeName" label="类型" width="100" />
        <el-table-column prop="startDate" label="开始日期" width="120" />
        <el-table-column prop="endDate" label="结束日期" width="120" />
        <el-table-column prop="regEndDate" label="报名截止" width="120" />
        <el-table-column prop="location" label="地点" width="150" />
        <el-table-column prop="statusName" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">{{ row.statusName }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="250" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleView(row)">查看</el-button>
            <el-button type="primary" link @click="handleEdit(row)">编辑</el-button>
            <el-button v-if="Number(row.status) === 1" type="success" link @click="handlePublish(row)">发布</el-button>
            <el-button v-if="Number(row.status) !== 4" type="danger" link @click="handleCancel(row)">取消</el-button>
            <el-button v-if="Number(row.status) === 1" type="danger" link @click="handleDelete(row)">删除</el-button>
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
        <el-form-item label="赛事名称" prop="name">
          <el-input v-model="formData.name" placeholder="请输入赛事名称" />
        </el-form-item>
        <el-form-item label="赛事类型" prop="type">
          <el-select v-model="formData.type" placeholder="请选择">
            <el-option label="田径" value="TRACK_FIELD" />
            <el-option label="球类" value="BALL" />
            <el-option label="游泳" value="SWIMMING" />
            <el-option label="其他" value="OTHER" />
          </el-select>
        </el-form-item>
        <el-form-item label="开始日期" prop="startDate">
          <el-date-picker v-model="formData.startDate" type="date" placeholder="选择日期" value-format="YYYY-MM-DD" />
        </el-form-item>
        <el-form-item label="结束日期" prop="endDate">
          <el-date-picker v-model="formData.endDate" type="date" placeholder="选择日期" value-format="YYYY-MM-DD" />
        </el-form-item>
        <el-form-item label="报名开始" prop="regStartDate">
          <el-date-picker v-model="formData.regStartDate" type="date" placeholder="选择日期" value-format="YYYY-MM-DD" />
        </el-form-item>
        <el-form-item label="报名截止" prop="regEndDate">
          <el-date-picker v-model="formData.regEndDate" type="date" placeholder="选择日期" value-format="YYYY-MM-DD" />
        </el-form-item>
        <el-form-item label="每人限报项目数" prop="maxProjectsPerAthlete">
          <el-input-number v-model="formData.maxProjectsPerAthlete" :min="0" :max="20" />
          <span class="form-tip">0表示不限，默认每人最多报名3个项目。</span>
        </el-form-item>
        <el-form-item label="举办地点" prop="location">
          <el-input v-model="formData.location" placeholder="请输入举办地点" />
        </el-form-item>
        <el-form-item label="主办单位" prop="organizer">
          <el-input v-model="formData.organizer" placeholder="请输入主办单位" />
        </el-form-item>
        <el-form-item label="联系电话" prop="contactPhone">
          <el-input v-model="formData.contactPhone" placeholder="请输入联系电话" />
        </el-form-item>
        <el-form-item label="赛事描述" prop="description">
          <el-input v-model="formData.description" type="textarea" :rows="3" placeholder="请输入赛事描述" />
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
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox, FormInstance, FormRules } from 'element-plus'
import { get, post, put, del } from '@/utils/request'

const router = useRouter()

const loading = ref(false)
const tableData = ref<any[]>([])
const total = ref(0)

const queryParams = reactive({
  current: 1,
  size: 10,
  name: '',
  type: '',
  status: null as number | null
})

const dialogVisible = ref(false)
const dialogTitle = ref('')
const submitLoading = ref(false)
const formRef = ref<FormInstance>()
const editingId = ref<number | null>(null)

const formData = reactive({
  name: '',
  type: '',
  startDate: '',
  endDate: '',
  regStartDate: '',
  regEndDate: '',
  maxProjectsPerAthlete: 3,
  location: '',
  organizer: '',
  contactPhone: '',
  description: ''
})

const rules: FormRules = {
  name: [{ required: true, message: '请输入赛事名称', trigger: 'blur' }],
  type: [{ required: true, message: '请选择赛事类型', trigger: 'change' }],
  startDate: [{ required: true, message: '请选择开始日期', trigger: 'change' }],
  endDate: [{ required: true, message: '请选择结束日期', trigger: 'change' }],
  regStartDate: [{ required: true, message: '请选择报名开始日期', trigger: 'change' }],
  regEndDate: [{ required: true, message: '请选择报名截止日期', trigger: 'change' }]
}

onMounted(() => {
  loadData()
})

async function loadData() {
  loading.value = true
  try {
    const res = await get('/events', queryParams)
    if (res.code === 200) {
      tableData.value = res.data.records
      total.value = res.data.total
    }
  } finally {
    loading.value = false
  }
}

function resetQuery() {
  queryParams.name = ''
  queryParams.type = ''
  queryParams.status = null
  queryParams.current = 1
  loadData()
}

function getStatusType(status: string | number): 'info' | 'primary' | 'success' | 'warning' {
  const map: Record<string, 'info' | 'primary' | 'success' | 'warning'> = {
    '1': 'info',
    '2': 'primary',
    '3': 'success',
    '4': 'warning'
  }
  return map[String(status)] || 'info'
}

function handleAdd() {
  editingId.value = null
  dialogTitle.value = '新增赛事'
  Object.assign(formData, {
    name: '', type: '', startDate: '', endDate: '',
    regStartDate: '', regEndDate: '', maxProjectsPerAthlete: 3,
    location: '', organizer: '', contactPhone: '', description: ''
  })
  dialogVisible.value = true
}

function handleEdit(row: any) {
  editingId.value = row.id
  dialogTitle.value = '编辑赛事'
  Object.assign(formData, row)
  formData.maxProjectsPerAthlete = Number(row.maxProjectsPerAthlete ?? 3)
  dialogVisible.value = true
}

function handleView(row: any) {
  router.push(`/events/${row.id}`)
}

async function handleSubmit() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    submitLoading.value = true
    try {
      if (editingId.value) {
        await put(`/events/${editingId.value}`, formData)
        ElMessage.success('更新成功')
      } else {
        await post('/events', formData)
        ElMessage.success('创建成功')
      }
      dialogVisible.value = false
      loadData()
    } finally {
      submitLoading.value = false
    }
  })
}

async function handlePublish(row: any) {
  await ElMessageBox.confirm('确定要发布该赛事吗？', '提示', { type: 'warning' })
  await post(`/events/${row.id}/publish`)
  ElMessage.success('发布成功')
  loadData()
}

async function handleCancel(row: any) {
  await ElMessageBox.confirm('确定要取消该赛事吗？', '提示', { type: 'warning' })
  await post(`/events/${row.id}/cancel`)
  ElMessage.success('取消成功')
  loadData()
}

async function handleDelete(row: any) {
  await ElMessageBox.confirm('确定要删除该赛事吗？', '提示', { type: 'warning' })
  await del(`/events/${row.id}`)
  ElMessage.success('删除成功')
  loadData()
}
</script>

<style scoped>
.form-tip {
  margin-left: 10px;
  color: var(--el-text-color-secondary);
  font-size: 12px;
}
</style>