<template>
  <ContentWrap>
    <!-- 提示栏 -->
    <el-alert type="info" :closable="false" show-icon class="mb-14px">
      <template #title>
        <span class="font-medium text-13px">汇率主档规则说明</span>
      </template>
      <template #default>
        <div class="text-12px text-gray-600 mt-2px leading-relaxed">
          1. <strong>生效日撮合</strong>：业务单据（发货对账/收款折算）自动撮合<strong
            >生效日 ≤ 业务日</strong
          >且状态为<strong>启用</strong>的最新一条汇率。<br />
          2. <strong>本位币规则</strong>：人民币（RMB/CNY）作为记账本位币，汇率恒定为
          1.00000000，系统代码内置，无需且禁止新增配置。<br />
          3.
          <strong>生命周期</strong
          >：汇率主档严禁物理删除，以保证历史分录追溯完整性；废弃或失效汇率一律通过<strong>停用</strong>处理。
        </div>
      </template>
    </el-alert>

    <!-- 搜索过滤区 -->
    <el-card shadow="never" class="mb-14px !border-gray-200">
      <el-form
        :model="queryParams"
        ref="queryFormRef"
        :inline="true"
        label-width="70px"
        class="-mb-15px"
      >
        <el-form-item label="币种" prop="currency">
          <el-input
            v-model="queryParams.currency"
            placeholder="如 USD / EUR / HKD"
            clearable
            @keyup.enter="handleQuery"
            class="!w-160px"
          />
        </el-form-item>

        <el-form-item label="状态" prop="status">
          <el-select v-model="queryParams.status" placeholder="全部状态" clearable class="!w-130px">
            <el-option label="启用 (ENABLE)" value="ENABLE" />
            <el-option label="停用 (DISABLE)" value="DISABLE" />
          </el-select>
        </el-form-item>

        <el-form-item label="生效日期" prop="effectiveDate">
          <el-date-picker
            v-model="queryParams.effectiveDate"
            type="date"
            placeholder="选择生效日期"
            value-format="YYYY-MM-DD"
            format="YYYY-MM-DD"
            class="!w-160px"
          />
        </el-form-item>

        <el-form-item>
          <el-button @click="handleQuery">
            <Icon icon="ep:search" class="mr-2px" /> 搜索
          </el-button>
          <el-button @click="resetQuery">
            <Icon icon="ep:refresh" class="mr-2px" /> 重置
          </el-button>
          <el-button
            type="primary"
            plain
            v-hasPermi="['lunaerp:finance:exchangerate:create']"
            @click="handleCreate"
          >
            <Icon icon="ep:plus" class="mr-2px" /> 新增汇率
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 列表表格 -->
    <el-card shadow="never" class="!border-gray-200">
      <el-table v-loading="loading" :data="list" border stripe>
        <el-table-column label="外币币种" prop="currency" width="120" align="center">
          <template #default="{ row }">
            <el-tag effect="plain" class="font-bold font-mono text-13px">
              {{ row.currency }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="换算关系" min-width="180">
          <template #default="{ row }">
            <span class="text-gray-600 text-13px">
              1 {{ row.currency }} =
              <span class="font-mono font-bold text-gray-900">
                {{ formatExchangeRate(row.rate) }}
              </span>
              RMB
            </span>
          </template>
        </el-table-column>

        <el-table-column label="汇率（8位小数）" prop="rate" min-width="150" align="right">
          <template #default="{ row }">
            <span class="font-mono font-bold text-emerald-800 text-14px">
              {{ formatExchangeRate(row.rate) }}
            </span>
          </template>
        </el-table-column>

        <el-table-column label="生效日期" prop="effectiveDate" width="130" align="center">
          <template #default="{ row }">
            <span class="font-mono font-medium">{{ row.effectiveDate }}</span>
          </template>
        </el-table-column>

        <el-table-column label="状态" prop="status" width="110" align="center">
          <template #default="{ row }">
            <el-tag
              :type="row.status === 'ENABLE' ? 'success' : 'info'"
              effect="light"
              class="font-medium"
            >
              {{ row.status === 'ENABLE' ? '启用' : '已停用' }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="备注说明" prop="remark" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="text-gray-600">{{ row.remark || '—' }}</span>
          </template>
        </el-table-column>

        <el-table-column label="创建人" prop="creator" width="100" show-overflow-tooltip />

        <el-table-column label="创建时间" prop="createTime" width="160" align="center">
          <template #default="{ row }">
            <span class="font-mono text-12px text-gray-500">{{ row.createTime || '—' }}</span>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="110" fixed="right" align="center">
          <template #default="{ row }">
            <el-button
              v-if="row.status === 'ENABLE'"
              link
              type="danger"
              size="small"
              v-hasPermi="['lunaerp:finance:exchangerate:update']"
              @click="handleDisable(row)"
            >
              停用
            </el-button>
            <span v-else class="text-gray-400 text-12px">已停用</span>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页组件 -->
      <div class="flex justify-end mt-16px">
        <el-pagination
          v-model:current-page="queryParams.pageNo"
          v-model:page-size="queryParams.pageSize"
          :page-sizes="[10, 20, 50, 100]"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="getList"
          @current-change="getList"
        />
      </div>
    </el-card>

    <!-- 新增汇率弹窗 -->
    <ExchangeRateFormDialog ref="formDialogRef" @success="getList" />
  </ContentWrap>
</template>

<script lang="ts" setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance } from 'element-plus'
import type {
  ExchangeRateRespVO,
  ExchangeRatePageReqVO
} from '@/api/lunaerp-elmui/finance/exchange-rate'
import { getExchangeRatePage, updateExchangeRate } from '@/api/lunaerp-elmui/finance/exchange-rate'
import { formatExchangeRate, parseExchangeRateErrorMessage } from './model/exchange-rate'
import ExchangeRateFormDialog from './components/ExchangeRateFormDialog.vue'

defineOptions({ name: 'LunaerpFinanceExchangeRate' })

const loading = ref(false)
const list = ref<ExchangeRateRespVO[]>([])
const total = ref(0)
const queryFormRef = ref<FormInstance>()
const formDialogRef = ref<InstanceType<typeof ExchangeRateFormDialog>>()

const queryParams = reactive<ExchangeRatePageReqVO>({
  pageNo: 1,
  pageSize: 10,
  currency: '',
  status: '',
  effectiveDate: ''
})

const getList = async () => {
  loading.value = true
  try {
    const res = await getExchangeRatePage(queryParams)
    list.value = res.list || []
    total.value = res.total || 0
  } catch (error: any) {
    ElMessage.error(error?.message || '获取汇率列表失败')
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
  handleQuery()
}

const handleCreate = () => {
  formDialogRef.value?.open()
}

const handleDisable = async (row: ExchangeRateRespVO) => {
  try {
    await ElMessageBox.confirm(
      `确定要【停用】币种【${row.currency}】在【${row.effectiveDate}】的汇率（${formatExchangeRate(row.rate)}）吗？停用后即期撮合将跳过此记录。`,
      '停用确认',
      {
        confirmButtonText: '确定停用',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    await updateExchangeRate({
      id: row.id,
      status: 'DISABLE',
      remark: row.remark
    })
    ElMessage.success('汇率已成功停用')
    getList()
  } catch (error: any) {
    if (error !== 'cancel') {
      const code = error?.code || error?.response?.data?.code
      ElMessage.error(parseExchangeRateErrorMessage(code, error?.message || '停用失败'))
    }
  }
}

onMounted(() => {
  getList()
})
</script>
