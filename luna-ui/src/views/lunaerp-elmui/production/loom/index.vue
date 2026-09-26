<template>
  <div class="loom-container">
    <el-tabs v-model="activeTab" type="border-card" class="loom-tabs">
      <!-- Tab 1: 织机档案 -->
      <el-tab-pane label="织机档案" name="loom">
        <!-- 搜索区 -->
        <ContentWrap>
          <el-form :model="queryParams" ref="queryFormRef" :inline="true" class="-mb-15px">
            <el-form-item label="关键词" prop="search">
              <el-input
                v-model="queryParams.search"
                placeholder="机台号 / 型号 / 备注"
                clearable
                @keyup.enter="handleQuery"
                class="!w-200px"
              />
            </el-form-item>
            <el-form-item label="状态" prop="status">
              <el-select
                v-model="queryParams.status"
                placeholder="全部状态"
                clearable
                class="!w-120px"
              >
                <el-option label="启用" value="ACTIVE" />
                <el-option label="停用" value="INACTIVE" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button @click="handleQuery">
                <Icon icon="ep:search" class="mr-1" />搜索
              </el-button>
              <el-button @click="resetQuery">
                <Icon icon="ep:refresh" class="mr-1" />重置
              </el-button>
            </el-form-item>
          </el-form>
        </ContentWrap>

        <!-- 列表与操作区 -->
        <ContentWrap class="mt-15px">
          <div class="flex items-center justify-between mb-15px">
            <div class="flex items-center gap-2">
              <span class="text-16px font-bold">织机列表</span>
              <span class="text-12px text-gray-400">（车间织机基础主数据；停用织机不可被新排产任务选择）</span>
            </div>
            <div class="flex items-center gap-2">
              <el-button
                type="primary"
                plain
                @click="openDialog('create')"
                v-hasPermi="['lunaerp:production:loom:create']"
              >
                <Icon icon="ep:plus" class="mr-1" /> 新增织机
              </el-button>
              <el-button @click="getList">
                <Icon icon="ep:refresh" class="mr-1" /> 刷新
              </el-button>
            </div>
          </div>

          <el-table
            v-loading="loading"
            :data="list"
            border
            stripe
            class="mt-10px"
          >
            <el-table-column label="机台号" prop="loomNo" min-width="140">
              <template #default="{ row }">
                <span class="font-bold text-primary">{{ row.loomNo }}</span>
              </template>
            </el-table-column>
            <el-table-column label="机型/型号" prop="model" min-width="180" :show-overflow-tooltip="true">
              <template #default="{ row }">
                {{ row.model || '—' }}
              </template>
            </el-table-column>
            <el-table-column label="状态" prop="status" width="100" align="center">
              <template #default="{ row }">
                <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'info'" size="small">
                  {{ row.status === 'ACTIVE' ? '启用' : '停用' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="备注" prop="remark" min-width="200" :show-overflow-tooltip="true">
              <template #default="{ row }">
                {{ row.remark || '—' }}
              </template>
            </el-table-column>
            <el-table-column
              label="创建时间"
              prop="createTime"
              width="170"
              align="center"
              :formatter="dateFormatter"
            />
            <el-table-column label="操作" width="220" align="center" fixed="right">
              <template #default="{ row }">
                <el-button
                  link
                  type="primary"
                  @click="openDialog('update', row)"
                  v-hasPermi="['lunaerp:production:loom:update']"
                >
                  <Icon icon="ep:edit" class="mr-1" />编辑
                </el-button>
                <el-button
                  v-if="row.status === 'ACTIVE'"
                  link
                  type="warning"
                  @click="handleToggleStatus(row, 'INACTIVE')"
                  v-hasPermi="['lunaerp:production:loom:update']"
                >
                  <Icon icon="ep:video-pause" class="mr-1" />停用
                </el-button>
                <el-button
                  v-else
                  link
                  type="success"
                  @click="handleToggleStatus(row, 'ACTIVE')"
                  v-hasPermi="['lunaerp:production:loom:update']"
                >
                  <Icon icon="ep:video-play" class="mr-1" />启用
                </el-button>
                <el-button
                  link
                  type="danger"
                  @click="handleDelete(row)"
                  v-hasPermi="['lunaerp:production:loom:delete']"
                >
                  <Icon icon="ep:delete" class="mr-1" />删除
                </el-button>
              </template>
            </el-table-column>
            <template #empty>
              <el-empty description="暂无织机数据" :image-size="60" />
            </template>
          </el-table>

          <Pagination
            :total="total"
            v-model:page="queryParams.pageNo"
            v-model:limit="queryParams.pageSize"
            @pagination="getList"
          />
        </ContentWrap>
      </el-tab-pane>

      <!-- Tab 2: IoT 设备接入 (Issue 12 预留) -->
      <el-tab-pane label="IoT 设备接入" name="iot">
        <ContentWrap>
          <div class="py-40px text-center">
            <el-empty description="IoT 设备接入开发中（厂商凭证 / 设备绑定 / 实时快照监控）" :image-size="80">
              <template #extra>
                <p class="text-13px text-gray-500 mt-2">
                  支持接入厂商协议数据采集，并绑定车间织机进行实时转速、圈数与运行效率监控（详见 Issue 12 · ADR-0028）。
                </p>
              </template>
            </el-empty>
          </div>
        </ContentWrap>
      </el-tab-pane>
    </el-tabs>

    <!-- 新增 / 修改 对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="520px"
      append-to-body
      destroy-on-close
    >
      <el-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        label-width="90px"
      >
        <el-form-item label="机台号" prop="loomNo">
          <div class="flex items-center gap-2 w-full">
            <el-input
              v-model="formData.loomNo"
              placeholder="留空自动取号（如 LM20260001），或输入自定义编号"
              maxlength="32"
              class="flex-1"
            />
            <el-button
              v-if="dialogAction === 'create'"
              @click="handleFetchNextNo"
              type="info"
              plain
            >
              获取编号
            </el-button>
          </div>
        </el-form-item>
        <el-form-item label="机型/型号" prop="model">
          <el-input
            v-model="formData.model"
            placeholder="例如：大圆机 34″·28G / 喷水织机"
            maxlength="64"
          />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="formData.status">
            <el-radio label="ACTIVE">启用</el-radio>
            <el-radio label="INACTIVE">停用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input
            type="textarea"
            v-model="formData.remark"
            placeholder="备注说明信息"
            :rows="3"
            maxlength="500"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="formSubmitting" @click="submitForm">
          确 定
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { type FormInstance, type FormRules } from 'element-plus'
import { useMessage } from '@/hooks/web/useMessage'
import { dateFormatter } from '@/utils/formatTime'
import {
  getLoomPage,
  createLoom,
  updateLoom,
  deleteLoom,
  getNextLoomNo,
  type LoomVO,
  type LoomPageParam
} from '@/api/lunaerp-elmui/production/loom'

defineOptions({ name: 'LunaerpElmuiProductionLoom' })

const message = useMessage()

const activeTab = ref('loom')

// ==================== 织机列表 ====================
const loading = ref(false)
const list = ref<LoomVO[]>([])
const total = ref(0)

const queryParams = reactive<LoomPageParam>({
  pageNo: 1,
  pageSize: 10,
  search: '',
  status: undefined
})
const queryFormRef = ref<FormInstance>()

const getList = async () => {
  loading.value = true
  try {
    const data = await getLoomPage(queryParams)
    list.value = data.list
    total.value = data.total
  } finally {
    loading.value = false
  }
}

const handleQuery = () => {
  queryParams.pageNo = 1
  getList()
}

const resetQuery = () => {
  queryFormRef.value?.resetFields()
  queryParams.pageNo = 1
  getList()
}

// ==================== 表单对话框 ====================
const dialogVisible = ref(false)
const dialogAction = ref<'create' | 'update'>('create')
const dialogTitle = ref('')
const formSubmitting = ref(false)
const formRef = ref<FormInstance>()

const formData = reactive<LoomVO>({
  id: undefined,
  loomNo: '',
  model: '',
  status: 'ACTIVE',
  remark: ''
})

const formRules = reactive<FormRules>({
  status: [{ required: true, message: '状态不能为空', trigger: 'change' }]
})

const openDialog = (action: 'create' | 'update', row?: LoomVO) => {
  dialogAction.value = action
  dialogTitle.value = action === 'create' ? '新增织机' : '修改织机'
  dialogVisible.value = true
  if (action === 'create') {
    Object.assign(formData, {
      id: undefined,
      loomNo: '',
      model: '',
      status: 'ACTIVE',
      remark: ''
    })
  } else if (row) {
    Object.assign(formData, {
      id: row.id,
      loomNo: row.loomNo,
      model: row.model,
      status: row.status,
      remark: row.remark
    })
  }
}

const handleFetchNextNo = async () => {
  try {
    const nextNo = await getNextLoomNo()
    formData.loomNo = nextNo
  } catch {}
}

const submitForm = async () => {
  if (!formRef.value) return
  await formRef.value.validate()
  formSubmitting.value = true
  try {
    if (dialogAction.value === 'create') {
      await createLoom(formData)
      message.success('新增织机成功')
    } else {
      await updateLoom(formData)
      message.success('修改织机成功')
    }
    dialogVisible.value = false
    await getList()
  } finally {
    formSubmitting.value = false
  }
}

const handleToggleStatus = async (row: LoomVO, newStatus: string) => {
  const actionText = newStatus === 'ACTIVE' ? '启用' : '停用'
  try {
    await message.confirm(`确认要${actionText}机台【${row.loomNo}】吗？${newStatus === 'INACTIVE' ? '停用后不可被新机台任务选择。' : ''}`)
    await updateLoom({
      id: row.id,
      status: newStatus
    })
    message.success(`${actionText}成功`)
    await getList()
  } catch {}
}

const handleDelete = async (row: LoomVO) => {
  try {
    await message.confirm(`确认要删除织机【${row.loomNo}】吗？若已被机台任务引用将禁止删除，引导改用停用。`)
    await deleteLoom(row.id!)
    message.success('删除织机成功')
    await getList()
  } catch {}
}

onMounted(() => {
  getList()
})
</script>

<style scoped>
.loom-container {
  padding: 0;
}
</style>
