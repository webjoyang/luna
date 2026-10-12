<template>
  <ContentWrap>
    <!-- 提示栏 -->
    <el-alert
      title="汇率规则提示：人民币（CNY/RMB）为系统记账本位币，系统内置汇率恒为 1.00000000，无需且禁止配置。外币记账取生效日≤业务日的最新一条启用（ENABLE）汇率。汇率主档不支持物理删除，失效请切换为「停用」。"
      type="info"
      show-icon
      :closable="false"
      class="mb-4"
    />

    <!-- 搜索表单 -->
    <el-form
      class="-mb-15px mt-2"
      :model="queryParams"
      ref="queryFormRef"
      :inline="true"
      label-width="68px"
    >
      <el-form-item label="币种" prop="currency">
        <el-input
          v-model="queryParams.currency"
          placeholder="如 USD / EUR / HKD"
          clearable
          @keyup.enter="handleQuery"
          class="!w-200px"
        />
      </el-form-item>

      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="全部状态" clearable class="!w-140px">
          <el-option label="启用" value="ENABLE" />
          <el-option label="停用" value="DISABLE" />
        </el-select>
      </el-form-item>

      <el-form-item>
        <el-button @click="handleQuery">
          <Icon icon="ep:search" class="mr-2px" />搜索
        </el-button>
        <el-button @click="resetQuery">
          <Icon icon="ep:refresh" class="mr-2px" />重置
        </el-button>
        <el-button
          type="primary"
          @click="openCreateDialog"
          v-hasPermi="['lunaerp:finance:exchange-rate:create']"
        >
          <Icon icon="ep:plus" class="mr-2px" />新增汇率
        </el-button>
      </el-form-item>
    </el-form>

    <!-- 列表数据 -->
    <div class="mt-4">
      <el-table v-loading="loading" :data="list" border stripe>
        <el-table-column label="ID" prop="id" width="80" align="center" />

        <el-table-column label="外币币种" prop="currency" width="120" align="center">
          <template #default="{ row }">
            <el-tag effect="plain" class="font-bold">{{ row.currency }}</el-tag>
          </template>
        </el-table-column>

        <el-table-column label="即期汇率（1外币兑本位币）" prop="rate" min-width="160" align="right">
          <template #default="{ row }">
            <span class="font-mono text-base font-bold text-blue-600">
              {{ formatRate(row.rate) }}
            </span>
          </template>
        </el-table-column>

        <el-table-column label="生效日期" prop="effectiveDate" width="130" align="center" />

        <el-table-column label="状态" prop="status" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ENABLE' ? 'success' : 'info'">
              {{ row.status === 'ENABLE' ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="备注说明" prop="remark" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">
            {{ row.remark || '—' }}
          </template>
        </el-table-column>

        <el-table-column
          label="创建时间"
          prop="createTime"
          width="160"
          align="center"
          :formatter="dateFormatter"
        />

        <!-- 操作列 -->
        <el-table-column label="操作" width="160" align="center" fixed="right">
          <template #default="{ row }">
            <el-button
              link
              :type="row.status === 'ENABLE' ? 'danger' : 'success'"
              size="small"
              @click="toggleStatus(row)"
              v-hasPermi="['lunaerp:finance:exchange-rate:update']"
            >
              {{ row.status === 'ENABLE' ? '停用' : '启用' }}
            </el-button>
            <el-button
              link
              type="primary"
              size="small"
              @click="openEditDialog(row)"
              v-hasPermi="['lunaerp:finance:exchange-rate:update']"
            >
              编辑备注
            </el-button>
          </template>
        </el-table-column>

        <template #empty>
          <el-empty description="暂无汇率主档记录" :image-size="80" />
        </template>
      </el-table>

      <Pagination
        :total="total"
        v-model:page="queryParams.pageNo"
        v-model:limit="queryParams.pageSize"
        @pagination="getList"
      />
    </div>

    <!-- 新增汇率弹窗 -->
    <el-dialog
      v-model="createDialogVisible"
      title="新增即期汇率"
      width="500px"
      append-to-body
      :close-on-click-modal="false"
    >
      <el-form
        ref="createFormRef"
        :model="createForm"
        :rules="createRules"
        label-width="100px"
        v-loading="dialogLoading"
      >
        <el-form-item label="外币币种" prop="currency">
          <el-select
            v-model="createForm.currency"
            filterable
            allow-create
            default-first-option
            placeholder="请选择或输入外币（如 USD）"
            style="width: 100%"
          >
            <el-option label="USD - 美元" value="USD" />
            <el-option label="EUR - 欧元" value="EUR" />
            <el-option label="HKD - 港币" value="HKD" />
            <el-option label="GBP - 英镑" value="GBP" />
            <el-option label="JPY - 日元" value="JPY" />
            <el-option label="AUD - 澳元" value="AUD" />
          </el-select>
          <div class="text-xs text-gray-400 mt-1">人民币（CNY/RMB）禁止配置</div>
        </el-form-item>

        <el-form-item label="生效日期" prop="effectiveDate">
          <el-date-picker
            v-model="createForm.effectiveDate"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="选择生效日期"
            style="width: 100%"
          />
        </el-form-item>

        <el-form-item label="即期汇率" prop="rate">
          <el-input-number
            v-model="createForm.rate"
            :precision="8"
            :step="0.0001"
            :min="0.00000001"
            controls-position="right"
            placeholder="1外币兑换的人民币金额"
            style="width: 100%"
          />
        </el-form-item>

        <el-form-item label="初始状态" prop="status">
          <el-radio-group v-model="createForm.status">
            <el-radio label="ENABLE">启用</el-radio>
            <el-radio label="DISABLE">停用</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="备注说明" prop="remark">
          <el-input
            v-model="createForm.remark"
            type="textarea"
            rows="2"
            placeholder="请输入汇率来源或说明（选填）"
            maxlength="256"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="createDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="dialogLoading" @click="handleCreateSubmit">
          确认新增
        </el-button>
      </template>
    </el-dialog>

    <!-- 编辑汇率弹窗 -->
    <el-dialog
      v-model="editDialogVisible"
      title="修改汇率说明 / 状态"
      width="480px"
      append-to-body
      :close-on-click-modal="false"
    >
      <el-form
        ref="editFormRef"
        :model="editForm"
        label-width="100px"
        v-loading="dialogLoading"
      >
        <el-form-item label="外币币种">
          <el-input :model-value="editForm.currency" disabled />
        </el-form-item>

        <el-form-item label="生效日期">
          <el-input :model-value="editForm.effectiveDate" disabled />
        </el-form-item>

        <el-form-item label="即期汇率">
          <el-input :model-value="formatRate(editForm.rate)" disabled />
        </el-form-item>

        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="editForm.status">
            <el-radio label="ENABLE">启用</el-radio>
            <el-radio label="DISABLE">停用</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="备注说明" prop="remark">
          <el-input
            v-model="editForm.remark"
            type="textarea"
            rows="3"
            placeholder="请输入备注说明"
            maxlength="256"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="editDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="dialogLoading" @click="handleEditSubmit">
          保存修改
        </el-button>
      </template>
    </el-dialog>
  </ContentWrap>
</template>

<script lang="ts" setup>
import { ref, reactive, onMounted } from 'vue'
import { dateFormatter } from '@/utils/formatTime'
import { useMessage } from '@/hooks/web/useMessage'
import * as ExchangeRateApi from '@/api/lunaerp-elmui/finance/exchange-rate'
import type { ExchangeRateVO, ExchangeRatePageParam } from '@/api/lunaerp-elmui/finance/exchange-rate'

defineOptions({ name: 'LunaerpElmuiExchangeRate' })

const message = useMessage()

const loading = ref(false)
const list = ref<ExchangeRateVO[]>([])
const total = ref(0)

const queryParams = reactive<ExchangeRatePageParam>({
  pageNo: 1,
  pageSize: 10,
  currency: undefined,
  status: undefined
})

const queryFormRef = ref()
const createFormRef = ref()
const editFormRef = ref()

const createDialogVisible = ref(false)
const editDialogVisible = ref(false)
const dialogLoading = ref(false)

const createForm = reactive({
  currency: 'USD',
  rate: 7.25,
  effectiveDate: new Date().toISOString().substring(0, 10),
  status: 'ENABLE',
  remark: ''
})

const editForm = reactive({
  id: undefined as number | undefined,
  currency: '',
  rate: 0,
  effectiveDate: '',
  status: 'ENABLE',
  remark: ''
})

const validateCurrency = (_rule: any, value: string, callback: any) => {
  if (!value) {
    return callback(new Error('请输入外币币种'))
  }
  const upper = value.trim().toUpperCase()
  if (upper === 'CNY' || upper === 'RMB') {
    return callback(new Error('人民币（CNY/RMB）为系统本位币，禁止配置'))
  }
  callback()
}

const createRules = {
  currency: [
    { required: true, message: '请选择或输入外币币种', trigger: 'blur' },
    { validator: validateCurrency, trigger: 'blur' }
  ],
  effectiveDate: [{ required: true, message: '请选择生效日期', trigger: 'change' }],
  rate: [{ required: true, message: '请输入即期汇率', trigger: 'blur' }]
}

function formatRate(val?: number | string | null): string {
  if (val == null || val === '') return '0.00000000'
  const num = Number(val)
  if (isNaN(num)) return '0.00000000'
  return num.toFixed(8)
}

const getList = async () => {
  loading.value = true
  try {
    const res = await ExchangeRateApi.getExchangeRatePage(queryParams)
    list.value = res.list || []
    total.value = res.total || 0
  } finally {
    loading.value = false
  }
}

const handleQuery = () => {
  queryParams.pageNo = 1
  getList()
}

const resetQuery = () => {
  queryFormRef.value.resetFields()
  handleQuery()
}

const openCreateDialog = () => {
  createForm.currency = 'USD'
  createForm.rate = 7.25
  createForm.effectiveDate = new Date().toISOString().substring(0, 10)
  createForm.status = 'ENABLE'
  createForm.remark = ''
  createDialogVisible.value = true
}

const handleCreateSubmit = async () => {
  await createFormRef.value.validate()
  dialogLoading.value = true
  try {
    await ExchangeRateApi.createExchangeRate({
      currency: createForm.currency.trim().toUpperCase(),
      rate: createForm.rate,
      effectiveDate: createForm.effectiveDate,
      status: createForm.status,
      remark: createForm.remark?.trim()
    })
    message.success('新增汇率成功')
    createDialogVisible.value = false
    getList()
  } finally {
    dialogLoading.value = false
  }
}

const openEditDialog = (row: ExchangeRateVO) => {
  editForm.id = row.id
  editForm.currency = row.currency
  editForm.rate = row.rate
  editForm.effectiveDate = row.effectiveDate
  editForm.status = row.status
  editForm.remark = row.remark || ''
  editDialogVisible.value = true
}

const handleEditSubmit = async () => {
  if (!editForm.id) return
  dialogLoading.value = true
  try {
    await ExchangeRateApi.updateExchangeRate({
      id: editForm.id,
      status: editForm.status,
      remark: editForm.remark?.trim()
    })
    message.success('修改成功')
    editDialogVisible.value = false
    getList()
  } finally {
    dialogLoading.value = false
  }
}

const toggleStatus = async (row: ExchangeRateVO) => {
  const newStatus = row.status === 'ENABLE' ? 'DISABLE' : 'ENABLE'
  const actionText = newStatus === 'ENABLE' ? '启用' : '停用'
  await message.confirm(`确定要${actionText}【${row.currency} - ${row.effectiveDate}】的汇率记录吗？`)
  await ExchangeRateApi.updateExchangeRate({
    id: row.id,
    status: newStatus
  })
  message.success(`${actionText}成功`)
  getList()
}

onMounted(() => {
  getList()
})
</script>

<style scoped>
</style>
