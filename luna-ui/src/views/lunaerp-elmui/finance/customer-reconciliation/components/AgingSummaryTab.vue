<template>
  <div class="aging-summary-tab">
    <!-- 过滤搜索栏 -->
    <ContentWrap>
      <el-form
        class="-mb-15px"
        :model="queryParams"
        ref="queryFormRef"
        :inline="true"
        label-width="80px"
      >
        <el-form-item label="基准日期" prop="agingDate">
          <el-date-picker
            v-model="queryParams.agingDate"
            type="date"
            placeholder="选择账龄基准日"
            value-format="YYYY-MM-DD"
            class="!w-160px"
          />
        </el-form-item>

        <el-form-item label="客户" prop="customerId">
          <el-select
            v-model="queryParams.customerId"
            placeholder="搜索并选择客户"
            clearable
            filterable
            remote
            reserve-keyword
            :remote-method="searchCustomers"
            :loading="customerLoading"
            class="!w-200px"
          >
            <el-option
              v-for="item in customerOptions"
              :key="item.id"
              :label="item.companyName"
              :value="item.id!"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="业务员ID" prop="salesmanId">
          <el-input-number
            v-model="queryParams.salesmanId"
            placeholder="业务员ID"
            :controls="false"
            class="!w-140px"
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
    </ContentWrap>

    <!-- 账龄统计卡片与五桶结构占比 -->
    <ContentWrap class="mt-4">
      <div class="grid grid-cols-2 md:grid-cols-4 lg:grid-cols-8 gap-3 mb-4">
        <!-- 1. 合计应收 -->
        <el-card shadow="never" class="!bg-purple-50/60 border-purple-200">
          <div class="text-12px text-gray-500 mb-1">合计应收总额</div>
          <div class="text-18px font-bold font-mono text-purple-700">
            ¥{{ formatMoney(grandTotals.totalAr) }}
          </div>
        </el-card>

        <!-- 2. 0-30天 -->
        <el-card shadow="never" class="!bg-green-50/60 border-green-200">
          <div class="text-12px text-green-700 mb-1">0–30天 (正常)</div>
          <div class="text-16px font-bold font-mono text-green-700">
            ¥{{ formatMoney(grandTotals.bucket0To30) }}
          </div>
        </el-card>

        <!-- 3. 31-60天 -->
        <el-card shadow="never" class="!bg-blue-50/60 border-blue-200">
          <div class="text-12px text-blue-700 mb-1">31–60天</div>
          <div class="text-16px font-bold font-mono text-blue-700">
            ¥{{ formatMoney(grandTotals.bucket31To60) }}
          </div>
        </el-card>

        <!-- 4. 61-90天 -->
        <el-card shadow="never" class="!bg-amber-50/60 border-amber-200">
          <div class="text-12px text-amber-700 mb-1">61–90天</div>
          <div class="text-16px font-bold font-mono text-amber-700">
            ¥{{ formatMoney(grandTotals.bucket61To90) }}
          </div>
        </el-card>

        <!-- 5. 91-180天 -->
        <el-card shadow="never" class="!bg-orange-50/60 border-orange-200">
          <div class="text-12px text-orange-700 mb-1">91–180天</div>
          <div class="text-16px font-bold font-mono text-orange-700">
            ¥{{ formatMoney(grandTotals.bucket91To180) }}
          </div>
        </el-card>

        <!-- 6. 181天以上 -->
        <el-card shadow="never" class="!bg-red-50/60 border-red-200">
          <div class="text-12px text-red-700 mb-1">181天以上</div>
          <div class="text-16px font-bold font-mono text-red-700">
            ¥{{ formatMoney(grandTotals.bucket181Plus) }}
          </div>
        </el-card>

        <!-- 7. 预收余额（单列） -->
        <el-card shadow="never" class="!bg-teal-50/60 border-teal-200">
          <div class="text-12px text-teal-700 mb-1">预收余额 (单列)</div>
          <div class="text-16px font-bold font-mono text-teal-700">
            ¥{{ formatMoney(grandTotals.totalAdvance) }}
          </div>
        </el-card>

        <!-- 8. 其中已逾期 -->
        <el-card shadow="never" class="!bg-rose-50/60 border-rose-200">
          <div class="text-12px text-rose-700 mb-1">其中已逾期</div>
          <div class="text-16px font-bold font-mono text-rose-700">
            ¥{{ formatMoney(grandTotals.totalOverdue) }}
          </div>
        </el-card>
      </div>

      <!-- 五桶全局占比柱状条 -->
      <div
        v-if="grandTotals.totalAr > 0"
        class="mb-4 p-3 bg-gray-50 rounded border border-gray-200"
      >
        <div class="text-12px text-gray-500 mb-2 flex justify-between">
          <span>五桶应收账龄分布占比</span>
          <span class="text-gray-400">纯函数实时计算 · FIFO冲抵</span>
        </div>
        <div class="h-6 w-full flex rounded overflow-hidden">
          <div
            v-if="globalPercentages.pct0To30 > 0"
            :style="{ width: `${globalPercentages.pct0To30}%`, backgroundColor: '#67c23a' }"
            class="flex items-center justify-center text-white text-11px font-mono"
            :title="`0–30天: ${globalPercentages.pct0To30}%`"
          >
            {{ globalPercentages.pct0To30 > 7 ? `${globalPercentages.pct0To30}%` : '' }}
          </div>
          <div
            v-if="globalPercentages.pct31To60 > 0"
            :style="{ width: `${globalPercentages.pct31To60}%`, backgroundColor: '#409eff' }"
            class="flex items-center justify-center text-white text-11px font-mono"
            :title="`31–60天: ${globalPercentages.pct31To60}%`"
          >
            {{ globalPercentages.pct31To60 > 7 ? `${globalPercentages.pct31To60}%` : '' }}
          </div>
          <div
            v-if="globalPercentages.pct61To90 > 0"
            :style="{ width: `${globalPercentages.pct61To90}%`, backgroundColor: '#e6a23c' }"
            class="flex items-center justify-center text-white text-11px font-mono"
            :title="`61–90天: ${globalPercentages.pct61To90}%`"
          >
            {{ globalPercentages.pct61To90 > 7 ? `${globalPercentages.pct61To90}%` : '' }}
          </div>
          <div
            v-if="globalPercentages.pct91To180 > 0"
            :style="{ width: `${globalPercentages.pct91To180}%`, backgroundColor: '#f56c6c' }"
            class="flex items-center justify-center text-white text-11px font-mono"
            :title="`91–180天: ${globalPercentages.pct91To180}%`"
          >
            {{ globalPercentages.pct91To180 > 7 ? `${globalPercentages.pct91To180}%` : '' }}
          </div>
          <div
            v-if="globalPercentages.pct181Plus > 0"
            :style="{ width: `${globalPercentages.pct181Plus}%`, backgroundColor: '#909399' }"
            class="flex items-center justify-center text-white text-11px font-mono"
            :title="`181天以上: ${globalPercentages.pct181Plus}%`"
          >
            {{ globalPercentages.pct181Plus > 7 ? `${globalPercentages.pct181Plus}%` : '' }}
          </div>
        </div>
        <div class="flex gap-4 text-11px text-gray-600 mt-2">
          <span class="flex items-center"
            ><span class="w-2.5 h-2.5 rounded-full mr-1 bg-green-500"></span> 0–30天</span
          >
          <span class="flex items-center"
            ><span class="w-2.5 h-2.5 rounded-full mr-1 bg-blue-500"></span> 31–60天</span
          >
          <span class="flex items-center"
            ><span class="w-2.5 h-2.5 rounded-full mr-1 bg-amber-500"></span> 61–90天</span
          >
          <span class="flex items-center"
            ><span class="w-2.5 h-2.5 rounded-full mr-1 bg-red-500"></span> 91–180天</span
          >
          <span class="flex items-center"
            ><span class="w-2.5 h-2.5 rounded-full mr-1 bg-gray-500"></span> 181天以上</span
          >
        </div>
      </div>

      <!-- 汇总表格：客户一行 -->
      <el-table v-loading="loading" :data="list" border stripe>
        <el-table-column
          label="客户公司名称"
          prop="customerName"
          min-width="170"
          show-overflow-tooltip
        >
          <template #default="{ row }">
            <el-link type="primary" :underline="false" @click="openDetail(row)">
              <span class="font-medium text-gray-800">{{ row.customerName }}</span>
            </el-link>
          </template>
        </el-table-column>

        <el-table-column label="业务员" prop="salespersonName" width="100" align="center">
          <template #default="{ row }">
            <span>{{ row.salespersonName || '—' }}</span>
          </template>
        </el-table-column>

        <el-table-column label="信用账期" prop="creditDays" width="95" align="center">
          <template #default="{ row }">
            <el-tag size="small" type="info">
              {{ row.creditDays != null ? `${row.creditDays}天` : '未设' }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="合计应收" prop="totalArBalance" min-width="120" align="right">
          <template #default="{ row }">
            <span class="font-mono font-bold text-purple-700"
              >¥{{ formatMoney(row.totalArBalance) }}</span
            >
          </template>
        </el-table-column>

        <el-table-column label="0–30天" prop="bucket0To30" min-width="105" align="right">
          <template #default="{ row }">
            <span
              :class="[
                'font-mono',
                Number(row.bucket0To30) > 0 ? 'text-green-700 font-medium' : 'text-gray-400'
              ]"
            >
              ¥{{ formatMoney(row.bucket0To30) }}
            </span>
          </template>
        </el-table-column>

        <el-table-column label="31–60天" prop="bucket31To60" min-width="105" align="right">
          <template #default="{ row }">
            <span
              :class="[
                'font-mono',
                Number(row.bucket31To60) > 0 ? 'text-blue-700 font-medium' : 'text-gray-400'
              ]"
            >
              ¥{{ formatMoney(row.bucket31To60) }}
            </span>
          </template>
        </el-table-column>

        <el-table-column label="61–90天" prop="bucket61To90" min-width="105" align="right">
          <template #default="{ row }">
            <span
              :class="[
                'font-mono',
                Number(row.bucket61To90) > 0 ? 'text-amber-700 font-medium' : 'text-gray-400'
              ]"
            >
              ¥{{ formatMoney(row.bucket61To90) }}
            </span>
          </template>
        </el-table-column>

        <el-table-column label="91–180天" prop="bucket91To180" min-width="105" align="right">
          <template #default="{ row }">
            <span
              :class="[
                'font-mono',
                Number(row.bucket91To180) > 0 ? 'text-orange-700 font-medium' : 'text-gray-400'
              ]"
            >
              ¥{{ formatMoney(row.bucket91To180) }}
            </span>
          </template>
        </el-table-column>

        <el-table-column label="181天以上" prop="bucket181Plus" min-width="105" align="right">
          <template #default="{ row }">
            <span
              :class="[
                'font-mono',
                Number(row.bucket181Plus) > 0 ? 'text-red-700 font-bold' : 'text-gray-400'
              ]"
            >
              ¥{{ formatMoney(row.bucket181Plus) }}
            </span>
          </template>
        </el-table-column>

        <el-table-column label="预收余额" prop="advanceBalance" min-width="110" align="right">
          <template #default="{ row }">
            <span
              :class="[
                'font-mono',
                Number(row.advanceBalance) > 0 ? 'text-teal-700 font-medium' : 'text-gray-400'
              ]"
            >
              ¥{{ formatMoney(row.advanceBalance) }}
            </span>
          </template>
        </el-table-column>

        <el-table-column label="其中已逾期" prop="overdueAmount" min-width="115" align="right">
          <template #default="{ row }">
            <span
              :class="[
                'font-mono',
                Number(row.overdueAmount) > 0 ? 'text-rose-600 font-bold' : 'text-gray-400'
              ]"
            >
              ¥{{ formatMoney(row.overdueAmount) }}
            </span>
          </template>
        </el-table-column>

        <el-table-column label="账龄结构" width="130" align="center">
          <template #default="{ row }">
            <div class="h-3 w-full flex rounded overflow-hidden bg-gray-100">
              <div
                v-if="calculateAgingBucketPercentages(row).pct0To30 > 0"
                :style="{
                  width: `${calculateAgingBucketPercentages(row).pct0To30}%`,
                  backgroundColor: '#67c23a'
                }"
              ></div>
              <div
                v-if="calculateAgingBucketPercentages(row).pct31To60 > 0"
                :style="{
                  width: `${calculateAgingBucketPercentages(row).pct31To60}%`,
                  backgroundColor: '#409eff'
                }"
              ></div>
              <div
                v-if="calculateAgingBucketPercentages(row).pct61To90 > 0"
                :style="{
                  width: `${calculateAgingBucketPercentages(row).pct61To90}%`,
                  backgroundColor: '#e6a23c'
                }"
              ></div>
              <div
                v-if="calculateAgingBucketPercentages(row).pct91To180 > 0"
                :style="{
                  width: `${calculateAgingBucketPercentages(row).pct91To180}%`,
                  backgroundColor: '#f56c6c'
                }"
              ></div>
              <div
                v-if="calculateAgingBucketPercentages(row).pct181Plus > 0"
                :style="{
                  width: `${calculateAgingBucketPercentages(row).pct181Plus}%`,
                  backgroundColor: '#909399'
                }"
              ></div>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="110" align="center" fixed="right">
          <template #default="{ row }">
            <el-button
              link
              type="primary"
              @click="openDetail(row)"
              v-hasPermi="['lunaerp:finance:customerrecon:query']"
            >
              <Icon icon="ep:zoom-in" class="mr-1" /> 下钻明细
            </el-button>
          </template>
        </el-table-column>

        <template #empty>
          <el-empty description="当前基准日暂无账龄应收数据" :image-size="90" />
        </template>
      </el-table>
    </ContentWrap>

    <!-- 下钻抽屉 -->
    <AgingDetailDrawer ref="detailDrawerRef" />
  </div>
</template>

<script lang="ts" setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import type {
  CustomerAgingSummaryRespVO,
  CustomerAgingSummaryReqVO
} from '@/api/lunaerp-elmui/finance/customer-reconciliation'
import { getCustomerAgingSummary } from '@/api/lunaerp-elmui/finance/customer-reconciliation'
import { getCustomerPage, type CustomerVO } from '@/api/lunaerp-elmui/baseinfo/customer'
import {
  formatMoney,
  calculateAgingBucketPercentages,
  calculateAgingGrandTotals
} from '../model/customerAging'
import AgingDetailDrawer from './AgingDetailDrawer.vue'

const loading = ref(false)
const list = ref<CustomerAgingSummaryRespVO[]>([])
const detailDrawerRef = ref<InstanceType<typeof AgingDetailDrawer>>()

const todayStr = new Date().toISOString().split('T')[0]
const queryParams = reactive<{
  agingDate: string
  customerId?: number
  salesmanId?: number
}>({
  agingDate: todayStr,
  customerId: undefined,
  salesmanId: undefined
})

const customerLoading = ref(false)
const customerOptions = ref<CustomerVO[]>([])

const grandTotals = computed(() => calculateAgingGrandTotals(list.value))

const globalPercentages = computed(() => {
  const dummyRow: CustomerAgingSummaryRespVO = {
    customerId: 0,
    customerName: '',
    totalArBalance: grandTotals.value.totalAr,
    bucket0To30: grandTotals.value.bucket0To30,
    bucket31To60: grandTotals.value.bucket31To60,
    bucket61To90: grandTotals.value.bucket61To90,
    bucket91To180: grandTotals.value.bucket91To180,
    bucket181Plus: grandTotals.value.bucket181Plus,
    advanceBalance: grandTotals.value.totalAdvance,
    overdueAmount: grandTotals.value.totalOverdue
  }
  return calculateAgingBucketPercentages(dummyRow)
})

const getList = async () => {
  try {
    loading.value = true
    const params: CustomerAgingSummaryReqVO = {
      agingDate: queryParams.agingDate || todayStr,
      customerIds: queryParams.customerId ? [queryParams.customerId] : undefined,
      salesmanId: queryParams.salesmanId || undefined
    }
    list.value = await getCustomerAgingSummary(params)
  } catch (error) {
    ElMessage.error('加载客户账龄汇总数据失败')
  } finally {
    loading.value = false
  }
}

const handleQuery = () => {
  getList()
}

const resetQuery = () => {
  queryParams.agingDate = todayStr
  queryParams.customerId = undefined
  queryParams.salesmanId = undefined
  getList()
}

const openDetail = (row: CustomerAgingSummaryRespVO) => {
  detailDrawerRef.value?.open({
    ...row,
    agingDate: queryParams.agingDate || todayStr
  })
}

const searchCustomers = async (keyword: string) => {
  if (!keyword) {
    customerOptions.value = []
    return
  }
  try {
    customerLoading.value = true
    const res = await getCustomerPage({
      pageNo: 1,
      pageSize: 20,
      companyName: keyword
    } as any)
    customerOptions.value = res.list || []
  } catch (e) {
    customerOptions.value = []
  } finally {
    customerLoading.value = false
  }
}

onMounted(() => {
  getList()
})
</script>
