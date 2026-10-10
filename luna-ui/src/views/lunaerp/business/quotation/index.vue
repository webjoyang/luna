<template>
  <ContentWrap>
    <!-- 搜索工作栏 -->
    <el-form
      ref="queryFormRef"
      :inline="true"
      :model="queryParams"
      class="-mb-15px"
      label-width="70px"
    >
      <el-form-item label="关键词" prop="keyword">
        <el-input
          v-model="queryParams.keyword"
          placeholder="单号 / 客户 / 备注"
          clearable
          @keyup.enter="handleQuery"
          class="!w-220px"
        />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select
          v-model="queryParams.status"
          placeholder="请选择状态"
          clearable
          class="!w-150px"
        >
          <el-option
            v-for="dict in getStrDictOptions(DICT_TYPE.LUNAERP_QUOTATION_STATUS)"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="客户" prop="customerId">
        <el-select
          v-model="queryParams.customerId"
          placeholder="请选择客户"
          clearable
          filterable
          remote
          :remote-method="searchCustomers"
          :loading="customerLoading"
          class="!w-220px"
        >
          <el-option
            v-for="item in customerOptions"
            :key="item.id"
            :label="item.companyName"
            :value="item.id"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="仅看最新" prop="latestOnly">
        <el-switch
          v-model="queryParams.latestOnly"
          inline-prompt
          active-text="是"
          inactive-text="否"
          @change="handleQuery"
        />
      </el-form-item>
      <el-form-item>
        <el-button @click="handleQuery"><Icon icon="ep:search" class="mr-5px" /> 搜索</el-button>
        <el-button @click="resetQuery"><Icon icon="ep:refresh" class="mr-5px" /> 重置</el-button>
        <el-button
          type="primary"
          plain
          @click="handleCreate"
          v-hasPermi="['lunaerp:business:quotation:create']"
        >
          <Icon icon="ep:plus" class="mr-5px" /> 新建报价单
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <!-- 列表卡片 -->
  <ContentWrap>
    <el-table v-loading="loading" :data="list" stripe border>
      <el-table-column label="报价单号" align="center" prop="quotationNo" min-width="160">
        <template #default="{ row }">
          <span class="font-mono font-medium">{{ row.quotationNo }}-v{{ row.versionNo }}</span>
        </template>
      </el-table-column>

      <el-table-column label="客户名称" align="left" prop="clientName" min-width="180" show-overflow-tooltip />

      <el-table-column label="币种 / 汇率" align="center" width="120">
        <template #default="{ row }">
          <span>{{ row.currency }}</span>
          <span v-if="row.currency !== 'RMB'" class="text-xs text-gray-400 block">
            ({{ Number(row.exchangeRate || 1).toFixed(4) }})
          </span>
        </template>
      </el-table-column>

      <el-table-column label="报价日期" align="center" prop="quotationDate" width="120" />

      <el-table-column label="有效期至" align="center" width="130">
        <template #default="{ row }">
          <span v-if="row.validTo">{{ row.validTo }}</span>
          <span v-else class="text-gray-400">长期有效</span>
        </template>
      </el-table-column>

      <el-table-column label="总数量" align="right" prop="totalQuantity" width="120">
        <template #default="{ row }">
          <span class="font-mono">{{ Number(row.totalQuantity || 0).toFixed(2) }}</span>
        </template>
      </el-table-column>

      <el-table-column label="总金额" align="right" prop="totalAmount" width="140">
        <template #default="{ row }">
          <span class="font-mono font-bold">{{ formatCurrency(row.totalAmount, row.currency) }}</span>
        </template>
      </el-table-column>

      <el-table-column
        v-if="hasAmountPerm"
        label="总成本额"
        align="right"
        prop="totalCostAmount"
        width="140"
      >
        <template #default="{ row }">
          <span v-if="row.totalCostAmount != null" class="font-mono text-gray-600">
            {{ formatCurrency(row.totalCostAmount, row.currency) }}
          </span>
          <span v-else class="text-gray-400">—</span>
        </template>
      </el-table-column>

      <el-table-column
        v-if="hasAmountPerm"
        label="整单毛利率"
        align="right"
        prop="avgMarginRate"
        width="130"
      >
        <template #default="{ row }">
          <template v-if="row.avgMarginRate != null">
            <span
              :class="[
                'font-mono font-medium',
                row.lowMarginWarning ? 'text-red-600 font-bold' : 'text-gray-700'
              ]"
            >
              {{ (Number(row.avgMarginRate) * 100).toFixed(2) }}%
            </span>
            <el-tag
              v-if="row.lowMarginWarning"
              size="small"
              type="danger"
              effect="dark"
              class="ml-1 scale-90"
            >
              低毛利
            </el-tag>
          </template>
          <span v-else class="text-gray-400">—</span>
        </template>
      </el-table-column>

      <el-table-column label="状态" align="center" prop="status" width="100">
        <template #default="{ row }">
          <el-tag v-if="row.expired" type="info" effect="plain">已失效</el-tag>
          <dict-tag
            v-else
            :type="DICT_TYPE.LUNAERP_QUOTATION_STATUS"
            :value="row.status"
          />
        </template>
      </el-table-column>

      <el-table-column label="操作" align="center" fixed="right" width="220">
        <template #default="{ row }">
          <el-button
            link
            type="primary"
            @click="handleView(row)"
          >
            查看
          </el-button>

          <el-button
            v-if="row.status === 'DRAFT'"
            link
            type="primary"
            @click="handleEdit(row)"
            v-hasPermi="['lunaerp:business:quotation:update']"
          >
            编辑
          </el-button>

          <el-button
            v-if="row.status === 'DRAFT'"
            link
            type="success"
            @click="handleConfirm(row)"
            v-hasPermi="['lunaerp:business:quotation:update']"
          >
            确认
          </el-button>

          <el-button
            v-if="row.status === 'DRAFT'"
            link
            type="danger"
            @click="handleDelete(row)"
            v-hasPermi="['lunaerp:business:quotation:delete']"
          >
            删除
          </el-button>

          <el-button
            v-if="row.status === 'CONFIRMED'"
            link
            type="warning"
            @click="handleRevise(row)"
            v-hasPermi="['lunaerp:business:quotation:create']"
          >
            修订
          </el-button>

          <el-button
            v-if="row.status === 'CONFIRMED'"
            link
            type="danger"
            @click="handleCancel(row)"
            v-hasPermi="['lunaerp:business:quotation:update']"
          >
            取消
          </el-button>

          <el-button
            link
            type="info"
            @click="handleOpenVersions(row)"
          >
            历史版本
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
    <Pagination
      :total="total"
      v-model:page="queryParams.pageNo"
      v-model:limit="queryParams.pageSize"
      @pagination="getList"
    />
  </ContentWrap>

  <!-- 历史版本抽屉 -->
  <el-drawer
    v-model="versionsDrawerVisible"
    :title="`报价单 [${currentQuotationNo}] 历史版本`"
    size="700px"
  >
    <el-table :data="versionsList" stripe border v-loading="versionsLoading">
      <el-table-column label="版本号" align="center" width="90">
        <template #default="{ row }">
          <el-tag size="small">v{{ row.versionNo }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="报价日期" align="center" prop="quotationDate" width="120" />
      <el-table-column label="状态" align="center" width="100">
        <template #default="{ row }">
          <el-tag v-if="row.expired" type="info" effect="plain">已失效</el-tag>
          <dict-tag
            v-else
            :type="DICT_TYPE.LUNAERP_QUOTATION_STATUS"
            :value="row.status"
          />
        </template>
      </el-table-column>
      <el-table-column label="总金额" align="right" min-width="120">
        <template #default="{ row }">
          <span class="font-mono">{{ formatCurrency(row.totalAmount, row.currency) }}</span>
        </template>
      </el-table-column>
      <el-table-column
        v-if="hasAmountPerm"
        label="整单毛利率"
        align="right"
        width="110"
      >
        <template #default="{ row }">
          <span v-if="row.avgMarginRate != null" class="font-mono">
            {{ (Number(row.avgMarginRate) * 100).toFixed(2) }}%
          </span>
          <span v-else>—</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="90">
        <template #default="{ row }">
          <el-button link type="primary" @click="handleView(row)">查看</el-button>
        </template>
      </el-table-column>
    </el-table>
  </el-drawer>
</template>

<script lang="ts" setup>
import { reactive, ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { DICT_TYPE, getStrDictOptions } from '@/utils/dict'
import { checkPermi } from '@/utils/permission'
import * as QuotationApi from '@/api/lunaerp/business/quotation'
import type { QuotationVO, QuotationPageReqVO } from '@/api/lunaerp/business/quotation'
import * as CustomerApi from '@/api/lunaerp-elmui/baseinfo/customer'
import type { CustomerVO } from '@/api/lunaerp-elmui/baseinfo/customer'

defineOptions({ name: 'LunaerpBusinessQuotation' })

const router = useRouter()
const message = ElMessage

const loading = ref(false)
const list = ref<QuotationVO[]>([])
const total = ref(0)
const queryFormRef = ref()

const queryParams = reactive<QuotationPageReqVO>({
  pageNo: 1,
  pageSize: 10,
  keyword: '',
  status: undefined,
  customerId: undefined,
  latestOnly: true
})

// 金额脱敏权限
const hasAmountPerm = computed(() => checkPermi(['lunaerp:sensitive:amount']))

// 客户选项
const customerLoading = ref(false)
const customerOptions = ref<CustomerVO[]>([])

const searchCustomers = async (query: string) => {
  customerLoading.value = true
  try {
    const data = await CustomerApi.getCustomerPage({ pageNo: 1, pageSize: 20, search: query || '' })
    customerOptions.value = data.list
  } finally {
    customerLoading.value = false
  }
}

// 格式化金额
const formatCurrency = (amount: number | undefined, currency: string | undefined) => {
  if (amount == null) return '—'
  const symbol = currency === 'USD' ? '$' : currency === 'EUR' ? '€' : '¥'
  return `${symbol}${Number(amount).toFixed(4)}`
}

// 列表查询
const getList = async () => {
  loading.value = true
  try {
    const data = await QuotationApi.getQuotationPage(queryParams)
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
  queryParams.latestOnly = true
  getList()
}

// 新建
const handleCreate = () => {
  router.push('/business/quotation/edit')
}

// 编辑
const handleEdit = (row: QuotationVO) => {
  router.push(`/business/quotation/edit?id=${row.id}`)
}

// 查看
const handleView = (row: QuotationVO) => {
  router.push(`/business/quotation/edit?id=${row.id}&readonly=1`)
}

// 确认
const handleConfirm = async (row: QuotationVO) => {
  try {
    await ElMessageBox.confirm(`确定要确认报价单【${row.quotationNo}】吗？确认后将不可修改。`, '提示', {
      type: 'warning'
    })
    await QuotationApi.confirmQuotation(row.id!)
    message.success('报价确认成功')
    getList()
  } catch {}
}

// 取消
const handleCancel = async (row: QuotationVO) => {
  try {
    await ElMessageBox.confirm(`确定要取消报价单【${row.quotationNo}】吗？`, '提示', {
      type: 'warning'
    })
    await QuotationApi.cancelQuotation(row.id!)
    message.success('报价已取消')
    getList()
  } catch {}
}

// 修订（Ticket 09）
const handleRevise = async (row: QuotationVO) => {
  try {
    await ElMessageBox.confirm(`确定对已确认报价【${row.quotationNo}】进行修订吗？旧版本将标记为已修订，并生成新版本草稿。`, '修订确认', {
      type: 'primary'
    })
    const newId = await QuotationApi.reviseQuotation(row.id!)
    message.success('已生成新版本草稿')
    router.push(`/business/quotation/edit?id=${newId}`)
  } catch {}
}

// 删除
const handleDelete = async (row: QuotationVO) => {
  try {
    await ElMessageBox.confirm(`确定要删除草稿报价单【${row.quotationNo}】吗？`, '提示', {
      type: 'warning'
    })
    await QuotationApi.deleteQuotation(row.id!)
    message.success('删除成功')
    getList()
  } catch {}
}

// 历史版本抽屉
const versionsDrawerVisible = ref(false)
const versionsLoading = ref(false)
const currentQuotationNo = ref('')
const versionsList = ref<QuotationVO[]>([])

const handleOpenVersions = async (row: QuotationVO) => {
  currentQuotationNo.value = row.quotationNo || ''
  versionsDrawerVisible.value = true
  versionsLoading.value = true
  try {
    versionsList.value = await QuotationApi.getQuotationVersions(row.quotationNo!)
  } finally {
    versionsLoading.value = false
  }
}

onMounted(() => {
  searchCustomers('')
  getList()
})
</script>
