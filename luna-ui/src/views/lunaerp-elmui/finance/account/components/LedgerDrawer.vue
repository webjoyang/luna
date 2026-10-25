<template>
  <el-drawer v-model="visible" :title="drawerTitle" size="75%" direction="rtl" destroy-on-close>
    <div class="p-20px pt-0">
      <!-- 搜索过滤 -->
      <el-form :model="queryParams" ref="queryFormRef" :inline="true" class="-mb-15px">
        <el-form-item label="业务日期">
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            class="!w-240px"
            @change="handleDateChange"
          />
        </el-form-item>

        <el-form-item label="分录类别" prop="category">
          <el-select
            v-model="queryParams.category"
            placeholder="全部分类"
            clearable
            class="!w-150px"
          >
            <el-option
              v-for="dict in getDictOptions(DICT_TYPE.LUNAERP_FINANCE_LEDGER_CATEGORY)"
              :key="dict.value"
              :label="dict.label"
              :value="dict.value"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="收支方向" prop="direction">
          <el-select
            v-model="queryParams.direction"
            placeholder="全部方向"
            clearable
            class="!w-120px"
          >
            <el-option label="借方流入" value="DEBIT" />
            <el-option label="贷方流出" value="CREDIT" />
          </el-select>
        </el-form-item>

        <el-form-item label="综合关键字" prop="keyword">
          <el-input
            v-model="queryParams.keyword"
            placeholder="来源单号 / 往来主体"
            clearable
            class="!w-180px"
            @keyup.enter="handleQuery"
          />
        </el-form-item>

        <el-form-item>
          <el-button @click="handleQuery">
            <Icon icon="ep:search" class="mr-2px" /> 搜索
          </el-button>
          <el-button @click="resetQuery">
            <Icon icon="ep:refresh" class="mr-2px" /> 重置
          </el-button>
        </el-form-item>
      </el-form>

      <!-- 流水表格 -->
      <el-table v-loading="loading" :data="list" border stripe class="mt-20px">
        <el-table-column label="业务日期" prop="bizDate" width="110" align="center">
          <template #default="{ row }">
            <span class="font-mono text-12px">{{ row.bizDate }}</span>
          </template>
        </el-table-column>

        <el-table-column label="分录类别" prop="category" min-width="130" align="center">
          <template #default="{ row }">
            <dict-tag :type="DICT_TYPE.LUNAERP_FINANCE_LEDGER_CATEGORY" :value="row.category" />
          </template>
        </el-table-column>

        <el-table-column label="收支方向" prop="direction" width="100" align="center">
          <template #default="{ row }">
            <el-tag
              :type="row.direction === 'DEBIT' ? 'success' : 'warning'"
              effect="light"
              class="font-medium"
            >
              {{ row.direction === 'DEBIT' ? '借方流入' : '贷方流出' }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="原币金额" prop="originalAmount" min-width="130" align="right">
          <template #default="{ row }">
            <span
              class="font-mono font-medium"
              :class="row.direction === 'DEBIT' ? 'text-emerald-700' : 'text-amber-700'"
            >
              {{ row.direction === 'DEBIT' ? '+' : '-' }}{{ formatAmount(row.originalAmount) }}
            </span>
          </template>
        </el-table-column>

        <el-table-column label="币种" prop="currency" width="80" align="center">
          <template #default="{ row }">
            <el-tag effect="plain" size="small" class="font-mono">{{ row.currency }}</el-tag>
          </template>
        </el-table-column>

        <el-table-column label="汇率" prop="exchangeRate" width="90" align="right">
          <template #default="{ row }">
            <span class="font-mono text-12px text-gray-500">{{ row.exchangeRate || 1 }}</span>
          </template>
        </el-table-column>

        <el-table-column label="本位币金额" prop="baseAmount" min-width="130" align="right">
          <template #default="{ row }">
            <span class="font-mono font-bold text-gray-900">
              {{ formatAmount(row.baseAmount) }}
            </span>
          </template>
        </el-table-column>

        <el-table-column label="往来主体" prop="partyName" min-width="140" show-overflow-tooltip>
          <template #default="{ row }">
            <span>{{ row.partyName || '—' }}</span>
          </template>
        </el-table-column>

        <el-table-column label="来源单号" prop="refNo" min-width="170" show-overflow-tooltip>
          <template #default="{ row }">
            <span v-if="!row.refNo" class="text-gray-400">—</span>
            <el-link
              v-else-if="row.sourceRoute"
              type="primary"
              :underline="false"
              class="font-mono text-12px"
              @click="handleJumpRoute(row.sourceRoute)"
            >
              {{ row.refNo }}
              <Icon icon="ep:top-right" class="ml-2px text-10px" />
            </el-link>
            <span v-else class="font-mono text-12px text-gray-700">{{ row.refNo }}</span>
          </template>
        </el-table-column>

        <el-table-column label="备注说明" prop="remark" min-width="150" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="text-gray-500">{{ row.remark || '—' }}</span>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
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
    </div>
  </el-drawer>
</template>

<script lang="ts" setup>
import { ref, reactive, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { DICT_TYPE, getDictOptions } from '@/utils/dict'
import { getLedgerPage } from '@/api/lunaerp-elmui/finance/account'
import type { LedgerRespVO, LedgerPageReqVO } from '@/api/lunaerp-elmui/finance/account'
import { formatAmount } from '../model/account'

defineOptions({ name: 'LedgerDrawer' })

const router = useRouter()
const visible = ref(false)
const loading = ref(false)
const list = ref<LedgerRespVO[]>([])
const total = ref(0)
const currentAccountName = ref('')
const dateRange = ref<[string, string] | null>(null)

const queryParams = reactive<LedgerPageReqVO>({
  pageNo: 1,
  pageSize: 20,
  accountId: undefined,
  category: undefined,
  direction: undefined,
  keyword: undefined,
  periodStart: undefined,
  periodEnd: undefined
})

const drawerTitle = computed(() => {
  if (currentAccountName.value) {
    return `【${currentAccountName.value}】资金流水明细`
  }
  return '资金流水明细'
})

const handleDateChange = (val: [string, string] | null) => {
  if (val && val.length === 2) {
    queryParams.periodStart = val[0]
    queryParams.periodEnd = val[1]
  } else {
    queryParams.periodStart = undefined
    queryParams.periodEnd = undefined
  }
}

const getList = async () => {
  loading.value = true
  try {
    const res = await getLedgerPage(queryParams)
    list.value = res.list || []
    total.value = res.total || 0
  } catch (error: any) {
    ElMessage.error(error?.message || '获取流水失败')
  } finally {
    loading.value = false
  }
}

const handleQuery = () => {
  queryParams.pageNo = 1
  getList()
}

const resetQuery = () => {
  queryParams.category = undefined
  queryParams.direction = undefined
  queryParams.keyword = undefined
  queryParams.periodStart = undefined
  queryParams.periodEnd = undefined
  dateRange.value = null
  handleQuery()
}

const handleJumpRoute = (route: string) => {
  router.push(route)
}

const open = (options: {
  accountId?: number
  accountName?: string
  category?: string
  periodStart?: string
  periodEnd?: string
}) => {
  visible.value = true
  currentAccountName.value = options.accountName || ''
  queryParams.accountId = options.accountId
  queryParams.category = options.category
  queryParams.direction = undefined
  queryParams.keyword = undefined
  queryParams.pageNo = 1

  if (options.periodStart && options.periodEnd) {
    dateRange.value = [options.periodStart, options.periodEnd]
    queryParams.periodStart = options.periodStart
    queryParams.periodEnd = options.periodEnd
  } else {
    dateRange.value = null
    queryParams.periodStart = undefined
    queryParams.periodEnd = undefined
  }

  getList()
}

defineExpose({ open })
</script>
