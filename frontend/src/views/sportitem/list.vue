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
      </div>

      <!-- 数据表格 -->
      <el-table :data="tableData" v-loading="loading" border stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="eventName" label="所属赛事" min-width="150" />
        <el-table-column prop="name" label="项目名称" width="150" />
        <el-table-column prop="categoryName" label="类别" width="100" />
        <el-table-column prop="genderLimitName" label="性别限制" width="100" />
        <el-table-column prop="maxParticipants" label="人数上限" width="100" />
        <el-table-column prop="currentParticipants" label="已报名" width="100" />
        <el-table-column label="参赛方式" width="100">
          <template #default="{ row }">{{ row.participationMode === 'RELAY' ? '接力' : '个人' }}</template>
        </el-table-column>
        <el-table-column prop="teamMemberCount" label="队员人数" width="100">
          <template #default="{ row }">{{ row.participationMode === 'RELAY' ? row.teamMemberCount : '—' }}</template>
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
          <el-input v-model="formData.name" placeholder="请输入项目名称" />
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
        <el-form-item label="性别限制" prop="genderLimit">
          <el-select v-model="formData.genderLimit" placeholder="请选择">
            <el-option label="不限" :value="0" />
            <el-option label="仅男性" :value="1" />
            <el-option label="仅女性" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item label="人数/队伍上限" prop="maxParticipants">
          <el-input-number v-model="formData.maxParticipants" :min="0" :max="1000" />
          <span class="form-tip">0表示不限；接力项目按队伍计数。</span>
        </el-form-item>
        <el-form-item label="参赛方式" prop="participationMode">
          <el-select v-model="formData.participationMode" @change="handleParticipationModeChange">
            <el-option label="个人项目" value="INDIVIDUAL" />
            <el-option label="接力项目" value="RELAY" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="formData.participationMode === 'RELAY'" label="接力队员人数" prop="teamMemberCount">
          <el-input-number v-model="formData.teamMemberCount" :min="2" :max="8" />
        </el-form-item>
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
  genderLimit: 0,
  maxParticipants: 100,
  participationMode: 'INDIVIDUAL',
  teamMemberCount: null as number | null,
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

function handleAdd() {
  editingId.value = null
  dialogTitle.value = '新增项目'
  Object.assign(formData, {
    eventId: null, name: '', type: 'TRACK',
    genderLimit: 0, maxParticipants: 100, participationMode: 'INDIVIDUAL',
    teamMemberCount: null, rules: ''
  })
  dialogVisible.value = true
}

function handleEdit(row: any) {
  editingId.value = row.id
  dialogTitle.value = '编辑项目'
  Object.assign(formData, row)
  formData.participationMode = row.participationMode || 'INDIVIDUAL'
  formData.teamMemberCount = row.teamMemberCount ?? null
  dialogVisible.value = true
}

function handleParticipationModeChange(mode: string) {
  if (mode === 'RELAY') {
    formData.teamMemberCount = formData.teamMemberCount ?? 4
  } else {
    formData.teamMemberCount = null
  }
}

async function handleSubmit() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid: boolean) => {
    if (!valid) return
    if (formData.participationMode === 'RELAY'
      && (!formData.teamMemberCount || formData.teamMemberCount < 2 || formData.teamMemberCount > 8)) {
      ElMessage.warning('接力项目必须配置2至8名队员')
      return
    }
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

<style scoped>
.form-tip {
  margin-left: 10px;
  color: var(--el-text-color-secondary);
  font-size: 12px;
}
</style>