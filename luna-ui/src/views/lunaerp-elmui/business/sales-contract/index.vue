<template>
  <ContentWrap>
    <!-- 搜索区 -->
    <el-form class="-mb-15px" :model="queryParams" ref="queryFormRef" :inline="true" label-width="68px">
      <el-form-item label="关键词" prop="keyword">
        <el-input
          v-model="queryParams.keyword"
          placeholder="合同号 / 客户 / 客户合同号"
          clearable
          @keyup.enter="handleQuery"
          class="!w-240px"
        />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="全部" clearable class="!w-180px">
          <el-option
            v-for="dict in getStrDictOptions(DICT_TYPE.LUNAERP_SALES_CONTRACT_STATUS)"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="贸易类型" prop="tradeType">
        <el-select v-model="queryParams.tradeType" placeholder="全部" clearable class="!w-180px">
          <el-option
            v-for="dict in getStrDictOptions(DICT_TYPE.LUNAERP_SALES_CONTRACT_TRADE_TYPE)"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="审批状态" prop="approvalStatus">
        <el-select v-model="queryParams.approvalStatus" placeholder="全部" clearable class="!w-180px">
          <el-option
            v-for="dict in getStrDictOptions(DICT_TYPE.LUNAERP_APPROVAL_INSTANCE_STATUS)"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button @click="handleQuery"><Icon icon="ep:search" />搜索</el-button>
        <el-button @click="resetQuery"><Icon icon="ep:refresh" />重置</el-button>
        <el-button
          type="primary"
          plain
          @click="openEdit()"
          v-hasPermi="['lunaerp:business:sales-contract:create']"
        >
          <Icon icon="ep:plus" /> 新建
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <ContentWrap>
    <el-table v-loading="loading" :data="list" border stripe>
      <el-table-column label="合同号" prop="contractNo" min-width="140">
        <template #default="{ row }">
          <span class="cell-no">{{ row.contractNo }}</span>
        </template>
      </el-table-column>
      <el-table-column label="来源报价" prop="sourceQuotationNo" min-width="130" align="center">
        <template #default="{ row }">
          <el-link
            v-if="row.sourceQuotationNo"
            type="primary"
            :underline="false"
            @click="router.push(`/business/quotation/edit?id=${row.sourceQuotationId}&readonly=1`)"
          >
            {{ row.sourceQuotationNo }}
          </el-link>
          <span v-else class="text-gray-400">—</span>
        </template>
      </el-table-column>
      <el-table-column label="客户" prop="clientName" min-width="200" show-overflow-tooltip />
      <el-table-column label="贸易类型" prop="tradeType" width="100" align="center">
        <template #default="{ row }">
          <DictTag :type="DICT_TYPE.LUNAERP_SALES_CONTRACT_TRADE_TYPE" :value="row.tradeType" />
        </template>
      </el-table-column>
      <el-table-column label="签订日期" prop="signDate" width="110" align="center" />
      <el-table-column label="总数量" prop="totalQuantity" width="110" align="right">
        <template #default="{ row }">{{ formatQuantity(row.totalQuantity) }}</template>
      </el-table-column>
      <el-table-column label="总金额" prop="totalAmount" min-width="130" align="right">
        <template #default="{ row }">{{ formatAmount(row) }}</template>
      </el-table-column>
      <el-table-column label="状态" prop="status" width="100" align="center">
        <template #default="{ row }">
          <DictTag :type="DICT_TYPE.LUNAERP_SALES_CONTRACT_STATUS" :value="row.status" />
        </template>
      </el-table-column>
      <el-table-column label="审批状态" prop="approvalStatus" width="100" align="center">
        <template #default="{ row }">
          <DictTag :type="DICT_TYPE.LUNAERP_APPROVAL_INSTANCE_STATUS" :value="row.approvalStatus || 'DRAFT'" />
        </template>
      </el-table-column>
      <el-table-column label="操作" width="260" align="center" fixed="right">
        <template #default="{ row }">
          <el-button
            link
            type="primary"
            @click="openView(row.id)"
            v-hasPermi="['lunaerp:business:sales-contract:query']"
          >
            <Icon icon="ep:view" class="mr-2px" />查看
          </el-button>
          <el-button
            link
            type="primary"
            :disabled="row.approvalStatus === 'APPROVING'"
            @click="openEdit(row.id)"
            v-hasPermi="['lunaerp:business:sales-contract:update']"
          >
            <Icon icon="ep:edit" class="mr-2px" />编辑
          </el-button>
          <el-button
            link
            type="primary"
            @click="handleReleaseProduction(row.id)"
            v-hasPermi="['lunaerp:production:release-production:release']"
          >
            <Icon icon="ep:promotion" class="mr-2px" />下达生产
          </el-button>
          <el-button
            link
            type="danger"
            :disabled="row.approvalStatus === 'APPROVING'"
            @click="handleDelete(row.id)"
            v-hasPermi="['lunaerp:business:sales-contract:delete']"
          >
            <Icon icon="ep:delete" class="mr-2px" />删除
          </el-button>
        </template>
      </el-table-column>
      <template #empty>
        <el-empty description="暂无销售合同数据" :image-size="90" />
      </template>
    </el-table>

    <Pagination
      :total="total"
      v-model:page="queryParams.pageNo"
      v-model:limit="queryParams.pageSize"
      @pagination="getList"
    />
  </ContentWrap>
</template>

<script lang="ts" setup>
import { DICT_TYPE, getStrDictOptions } from '@/utils/dict'
import * as SalesContractApi from '@/api/lunaerp-elmui/business/sales-contract'
import type { SalesContractVO } from '@/api/lunaerp-elmui/business/sales-contract'

defineOptions({ name: 'LunaerpElmuiBusinessSalesContract' })

const router = useRouter()
const message = useMessage()

const loading = ref(true)
const total = ref(0)
const list = ref<SalesContractVO[]>([])
const queryFormRef = ref()

const queryParams = reactive({
  pageNo: 1,
  pageSize: 20,
  keyword: '',
  status: '',
  approvalStatus: '',
  tradeType: ''
})

/** 查询销售合同分页 */
const getList = async () => {
  loading.value = true
  try {
    const data = await SalesContractApi.getSalesContractPage(queryParams)
    list.value = data.list
    total.value = data.total
  } finally {
    loading.value = false
  }
}

/** 搜索：回车或点按钮触发，回到第 1 页 */
const handleQuery = () => {
  queryParams.pageNo = 1
  getList()
}

/** 重置 */
const resetQuery = () => {
  queryFormRef.value?.resetFields()
  queryParams.pageNo = 1
  getList()
}

/** 总数量：千分位 */
const formatQuantity = (value?: number | string) => {
  return Number(value ?? 0).toLocaleString('en-US')
}

/** 总金额：币种 + 千分位两位小数 */
const formatAmount = (row: SalesContractVO) => {
  const amount = Number(row.totalAmount ?? 0).toLocaleString('en-US', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  })
  return row.currency ? `${row.currency} ${amount}` : amount
}

/** 新建/编辑：整页表单（无 id 新建，有 id 编辑） */
const openEdit = (id?: number) => {
  router.push(id ? `/business/sales-contract/edit/${id}` : '/business/sales-contract/edit')
}

/** 查看：同一表单页只读模式打开 */
const openView = (id: number) => {
  router.push(`/business/sales-contract/edit/${id}?mode=view`)
}

/** 下达生产：跳详情并激活「生产」Tab */
const handleReleaseProduction = (id: number) => {
  router.push(`/business/sales-contract/edit/${id}?tab=production`)
}

/** 删除合同（二次确认） */
const handleDelete = async (id: number) => {
  try {
    await message.delConfirm()
    await SalesContractApi.deleteSalesContract(id)
    message.success('删除成功')
    await getList()
  } catch {}
}

/** 初始化 */
onMounted(() => {
  getList()
})
</script>

<style lang="scss" scoped>
.cell-no {
  font-weight: 600;
  color: var(--el-text-color-primary);
}
</style>
