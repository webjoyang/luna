<template>
  <ContentWrap>
    <!-- 期间选择与筛选栏 -->
    <el-card shadow="never" class="mb-16px !border-gray-200">
      <el-form
        :model="queryParams"
        ref="queryFormRef"
        :inline="true"
        label-width="70px"
        class="-mb-15px"
      >
        <el-form-item label="统计期间">
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

        <el-form-item label="快捷期间">
          <el-radio-group v-model="activePreset" size="default" @change="handlePresetChange">
            <el-radio-button label="thisMonth">本月</el-radio-button>
            <el-radio-button label="lastMonth">上月</el-radio-button>
            <el-radio-button label="thisQuarter">本季度</el-radio-button>
            <el-radio-button label="thisYear">今年</el-radio-button>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="资金账户" prop="accountId">
          <el-select
            v-model="queryParams.accountId"
            placeholder="全部账户"
            clearable
            class="!w-160px"
          >
            <el-option
              v-for="acc in simpleAccountList"
              :key="acc.id"
              :label="acc.accountName"
              :value="acc.id"
            />
          </el-select>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="handleQuery">
            <Icon icon="ep:search" class="mr-2px" /> 统计汇总
          </el-button>
          <el-button @click="resetQuery">
            <Icon icon="ep:refresh" class="mr-2px" /> 重置
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 1. 资金账户收支汇总表格 -->
    <el-card shadow="never" class="mb-16px !border-gray-200">
      <template #header>
        <div class="flex items-center justify-between">
          <div class="flex items-center space-x-8px">
            <Icon icon="ep:wallet" class="text-18px text-primary" />
            <span class="font-bold text-15px text-gray-900">资金账户汇总</span>
          </div>
          <span class="text-12px text-gray-500">
            统计区间：<span class="font-mono text-gray-800">{{ queryParams.periodStart }}</span> 至
            <span class="font-mono text-gray-800">{{ queryParams.periodEnd }}</span>
          </span>
        </div>
      </template>

      <el-table
        v-loading="summaryLoading"
        :data="accountTableData"
        border
        stripe
        show-summary
        :summary-method="getAccountSummaries"
      >
        <el-table-column label="资金账户" prop="accountName" min-width="160">
          <template #default="{ row }">
            <span class="font-medium text-gray-900">{{ row.accountName }}</span>
          </template>
        </el-table-column>

        <el-table-column label="账户类型" prop="accountType" width="110" align="center">
          <template #default="{ row }">
            <dict-tag
              v-if="row.accountType"
              :type="DICT_TYPE.LUNAERP_FINANCE_ACCOUNT_TYPE"
              :value="row.accountType"
            />
            <span v-else class="text-gray-400">—</span>
          </template>
        </el-table-column>

        <el-table-column label="公私性质" prop="accountNature" width="100" align="center">
          <template #default="{ row }">
            <dict-tag
              v-if="row.accountNature"
              :type="DICT_TYPE.LUNAERP_FINANCE_ACCOUNT_NATURE"
              :value="row.accountNature"
            />
            <span v-else class="text-gray-400">—</span>
          </template>
        </el-table-column>

        <el-table-column label="币种" prop="currency" width="80" align="center">
          <template #default="{ row }">
            <el-tag effect="plain" class="font-mono font-bold">{{ row.currency || 'RMB' }}</el-tag>
          </template>
        </el-table-column>

        <el-table-column label="期初余额" prop="openingBalance" min-width="130" align="right">
          <template #default="{ row }">
            <span class="font-mono text-gray-700">¥ {{ formatAmount(row.openingBalance) }}</span>
          </template>
        </el-table-column>

        <el-table-column label="本期流入 (借方)" prop="inflowAmount" min-width="140" align="right">
          <template #default="{ row }">
            <span class="font-mono font-bold text-emerald-700">
              +¥ {{ formatAmount(row.inflowAmount) }}
            </span>
          </template>
        </el-table-column>

        <el-table-column label="本期流出 (贷方)" prop="outflowAmount" min-width="140" align="right">
          <template #default="{ row }">
            <span class="font-mono font-bold text-amber-700">
              -¥ {{ formatAmount(row.outflowAmount) }}
            </span>
          </template>
        </el-table-column>

        <el-table-column label="期末余额" prop="endingBalance" min-width="140" align="right">
          <template #default="{ row }">
            <span class="font-mono font-bold text-gray-900">
              ¥ {{ formatAmount(row.endingBalance) }}
            </span>
          </template>
        </el-table-column>

        <el-table-column
          label="当前可用余额 (物化)"
          prop="currentBalance"
          min-width="150"
          align="right"
        >
          <template #default="{ row }">
            <span class="font-mono font-bold text-emerald-800">
              ¥ {{ formatAmount(row.currentBalance) }}
            </span>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 2. 类别借贷汇总表格 -->
    <el-card shadow="never" class="mb-16px !border-gray-200">
      <template #header>
        <div class="flex items-center justify-between">
          <div class="flex items-center space-x-8px">
            <Icon icon="ep:pie-chart" class="text-18px text-primary" />
            <span class="font-bold text-15px text-gray-900">分录类别发生额汇总</span>
          </div>
          <span class="text-12px text-gray-400">点击【钻取流水】可穿透查询该分类当期明细</span>
        </div>
      </template>

      <el-table v-loading="summaryLoading" :data="categoryTableData" border stripe>
        <el-table-column label="分录类别" prop="category" min-width="160">
          <template #default="{ row }">
            <dict-tag :type="DICT_TYPE.LUNAERP_FINANCE_LEDGER_CATEGORY" :value="row.category" />
          </template>
        </el-table-column>

        <el-table-column label="主导方向" prop="direction" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="row.direction === 'DEBIT' ? 'success' : 'warning'" effect="light">
              {{ row.direction === 'DEBIT' ? '借方流入' : '贷方流出' }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="借方发生额 (流入)" prop="debitAmount" min-width="140" align="right">
          <template #default="{ row }">
            <span class="font-mono text-emerald-700">¥ {{ formatAmount(row.debitAmount) }}</span>
          </template>
        </el-table-column>

        <el-table-column
          label="贷方发生额 (流出)"
          prop="creditAmount"
          min-width="140"
          align="right"
        >
          <template #default="{ row }">
            <span class="font-mono text-amber-700">¥ {{ formatAmount(row.creditAmount) }}</span>
          </template>
        </el-table-column>

        <el-table-column label="净发生额 (借-贷)" prop="netAmount" min-width="140" align="right">
          <template #default="{ row }">
            <span
              class="font-mono font-bold"
              :class="(row.netAmount || 0) >= 0 ? 'text-emerald-800' : 'text-amber-800'"
            >
              ¥ {{ formatAmount(row.netAmount) }}
            </span>
          </template>
        </el-table-column>

        <el-table-column label="发生笔数" prop="count" width="100" align="center">
          <template #default="{ row }">
            <span class="font-mono font-bold">{{ row.count }}</span>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="110" align="center" fixed="right">
          <template #default="{ row }">
            <el-button
              link
              type="primary"
              size="small"
              v-hasPermi="['lunaerp:finance:account:query']"
              @click="handleDrilldownLedger(row)"
            >
              <Icon icon="ep:search" class="mr-2px" /> 钻取流水
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 3. 经营毛利参考卡片 -->
    <GrossMarginCard :margin-data="marginData" :loading="marginLoading" />

    <!-- 流水穿透抽屉 -->
    <LedgerDrawer ref="ledgerDrawerRef" />
  </ContentWrap>
</template>

<script lang="ts" setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import type { TableColumnCtx } from 'element-plus'
import { DICT_TYPE } from '@/utils/dict'
import { getOverviewSummary, getGrossMargin } from '@/api/lunaerp-elmui/finance/overview'
import type {
  OverviewSummaryReqVO,
  AccountSummaryRespVO,
  CategorySummaryRespVO,
  GrossMarginRespVO
} from '@/api/lunaerp-elmui/finance/overview'
import { getSimpleAccountList } from '@/api/lunaerp-elmui/finance/account'
import type { FinanceAccountSimpleRespVO } from '@/api/lunaerp-elmui/finance/account'
import { formatAmount } from '../account/model/account'
import { getPeriodPresets } from './model/overview'
import GrossMarginCard from './components/GrossMarginCard.vue'
import LedgerDrawer from '../account/components/LedgerDrawer.vue'

defineOptions({ name: 'LunaerpFinanceOverview' })

const summaryLoading = ref(false)
const marginLoading = ref(false)
const simpleAccountList = ref<FinanceAccountSimpleRespVO[]>([])

const accountTableData = ref<AccountSummaryRespVO[]>([])
const categoryTableData = ref<CategorySummaryRespVO[]>([])
const summaryTotal = ref<AccountSummaryRespVO>()
const marginData = ref<GrossMarginRespVO>()

const ledgerDrawerRef = ref<InstanceType<typeof LedgerDrawer>>()

const activePreset = ref<'thisMonth' | 'lastMonth' | 'thisQuarter' | 'thisYear'>('thisMonth')
const presets = getPeriodPresets()
const dateRange = ref<[string, string]>(presets.thisMonth)

const queryParams = reactive<OverviewSummaryReqVO>({
  periodStart: presets.thisMonth[0],
  periodEnd: presets.thisMonth[1],
  accountId: undefined
})

const handlePresetChange = (preset: 'thisMonth' | 'lastMonth' | 'thisQuarter' | 'thisYear') => {
  const range = presets[preset]
  dateRange.value = range
  queryParams.periodStart = range[0]
  queryParams.periodEnd = range[1]
  handleQuery()
}

const handleDateChange = (val: [string, string] | null) => {
  if (val && val.length === 2) {
    queryParams.periodStart = val[0]
    queryParams.periodEnd = val[1]
    activePreset.value = '' as any
  } else {
    queryParams.periodStart = undefined
    queryParams.periodEnd = undefined
  }
}

const loadSimpleAccounts = async () => {
  try {
    simpleAccountList.value = await getSimpleAccountList()
  } catch (err: any) {
    console.error('加载资金账户下拉失败', err)
  }
}

const fetchOverviewSummary = async () => {
  summaryLoading.value = true
  try {
    const res = await getOverviewSummary(queryParams)
    accountTableData.value = res.accountList || []
    categoryTableData.value = res.categoryList || []
    summaryTotal.value = res.total
  } catch (error: any) {
    ElMessage.error(error?.message || '获取收支总览汇总失败')
  } finally {
    summaryLoading.value = false
  }
}

const fetchGrossMargin = async () => {
  marginLoading.value = true
  try {
    const res = await getGrossMargin({
      periodStart: queryParams.periodStart,
      periodEnd: queryParams.periodEnd
    })
    marginData.value = res
  } catch (error: any) {
    ElMessage.error(error?.message || '获取经营毛利参考失败')
  } finally {
    marginLoading.value = false
  }
}

const handleQuery = () => {
  fetchOverviewSummary()
  fetchGrossMargin()
}

const resetQuery = () => {
  activePreset.value = 'thisMonth'
  dateRange.value = presets.thisMonth
  queryParams.periodStart = presets.thisMonth[0]
  queryParams.periodEnd = presets.thisMonth[1]
  queryParams.accountId = undefined
  handleQuery()
}

const handleDrilldownLedger = (row: CategorySummaryRespVO) => {
  ledgerDrawerRef.value?.open({
    category: row.category,
    periodStart: queryParams.periodStart,
    periodEnd: queryParams.periodEnd
  })
}

interface SummaryMethodProps<T = AccountSummaryRespVO> {
  columns: TableColumnCtx<T>[]
  data: T[]
}

const getAccountSummaries = (param: SummaryMethodProps) => {
  const { columns } = param
  const sums: string[] = []

  if (summaryTotal.value) {
    const t = summaryTotal.value
    columns.forEach((column, index) => {
      if (index === 0) {
        sums[index] = '全账户合计'
        return
      }
      if (column.property === 'openingBalance') {
        sums[index] = `¥ ${formatAmount(t.openingBalance)}`
      } else if (column.property === 'inflowAmount') {
        sums[index] = `+¥ ${formatAmount(t.inflowAmount)}`
      } else if (column.property === 'outflowAmount') {
        sums[index] = `-¥ ${formatAmount(t.outflowAmount)}`
      } else if (column.property === 'endingBalance') {
        sums[index] = `¥ ${formatAmount(t.endingBalance)}`
      } else if (column.property === 'currentBalance') {
        sums[index] = `¥ ${formatAmount(t.currentBalance)}`
      } else {
        sums[index] = ''
      }
    })
    return sums
  }

  return sums
}

onMounted(() => {
  loadSimpleAccounts()
  handleQuery()
})
</script>
