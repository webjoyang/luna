<template>
  <ContentWrap>
    <!-- 搜索区（多条件检索：关键词/类型/状态/仓库/供应商/日期区间） -->
    <el-form class="-mb-15px" :model="queryParams" ref="queryFormRef" :inline="true" label-width="80px">
      <el-form-item label="关键词" prop="keyword">
        <el-input
          v-model="queryParams.keyword"
          placeholder="入库单号 / 供应商 / 合同号 / 纱线采购单 / 产品 / 纱线规格"
          clearable
          @keyup.enter="handleQuery"
          class="!w-260px"
        />
      </el-form-item>
      <el-form-item label="入库类型" prop="inType">
        <el-select v-model="queryParams.inType" placeholder="全部" clearable class="!w-160px">
          <el-option
            v-for="dict in getStrDictOptions(DICT_TYPE.LUNAERP_STOCK_IN_TYPE)"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="全部" clearable class="!w-160px">
          <el-option
            v-for="dict in getStrDictOptions(DICT_TYPE.LUNAERP_STOCK_IN_STATUS)"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="仓库" prop="warehouseId">
        <el-select v-model="queryParams.warehouseId" placeholder="全部仓库" clearable class="!w-180px">
          <el-option
            v-for="item in warehouseOptions"
            :key="item.id"
            :label="item.name"
            :value="item.id!"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="供应商" prop="supplierId">
        <el-select
          v-model="queryParams.supplierId"
          placeholder="搜索供应商"
          clearable
          filterable
          remote
          reserve-keyword
          :remote-method="searchSuppliers"
          :loading="supplierLoading"
          class="!w-180px"
        >
          <el-option
            v-for="item in supplierOptions"
            :key="item.id"
            :label="item.companyName"
            :value="item.id!"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="单据日期" prop="inDate">
        <el-date-picker
          v-model="queryParams.inDate"
          type="daterange"
          value-format="YYYY-MM-DD"
          range-separator="-"
          start-placeholder="单据日期起"
          end-placeholder="单据日期止"
          class="!w-240px"
        />
      </el-form-item>
      <el-form-item>
        <el-button @click="handleQuery"><Icon icon="ep:search" class="mr-2px" />搜索</el-button>
        <el-button @click="resetQuery"><Icon icon="ep:refresh" class="mr-2px" />重置</el-button>
        <el-button
          type="primary"
          plain
          @click="openWorkspace()"
          v-hasPermi="['lunaerp:stock:stock-in:create']"
        >
          <Icon icon="ep:plus" class="mr-2px" /> 新建入库单
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <ContentWrap>
    <el-table v-loading="loading" :data="list" border stripe>
      <el-table-column label="入库单号" prop="inNo" min-width="160" show-overflow-tooltip>
        <template #default="{ row }">
          <span class="cell-code">{{ row.inNo || '—' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="单据日期" prop="inDate" width="110" align="center" />
      <el-table-column label="类型" prop="inType" width="120" align="center">
        <template #default="{ row }">
          <DictTag :type="DICT_TYPE.LUNAERP_STOCK_IN_TYPE" :value="row.inType" />
        </template>
      </el-table-column>
      <el-table-column label="仓库" prop="warehouseName" min-width="130" show-overflow-tooltip>
        <template #default="{ row }">{{ row.warehouseName || '—' }}</template>
      </el-table-column>
      <el-table-column label="供应商" prop="supplierName" min-width="160" show-overflow-tooltip>
        <template #default="{ row }">{{ row.supplierName || '—' }}</template>
      </el-table-column>
      <el-table-column label="关联纱线采购单" prop="yarnOrderNo" min-width="160" show-overflow-tooltip>
        <template #default="{ row }">
          <span class="cell-code">{{ row.yarnOrderNo || '—' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="金额合计" prop="totalAmount" width="120" align="right">
        <template #default="{ row }">
          {{ row.totalAmount != null ? `¥ ${Number(row.totalAmount).toFixed(2)}` : '—' }}
        </template>
      </el-table-column>
      <el-table-column label="状态" prop="status" width="100" align="center">
        <template #default="{ row }">
          <DictTag :type="DICT_TYPE.LUNAERP_STOCK_IN_STATUS" :value="row.status" />
        </template>
      </el-table-column>
      <el-table-column label="创建人" prop="creator" width="110" align="center">
        <template #default="{ row }">{{ row.creator || '—' }}</template>
      </el-table-column>
      <el-table-column
        label="创建时间"
        prop="createTime"
        width="160"
        align="center"
        :formatter="dateFormatter"
      />
      <el-table-column label="操作" width="280" align="center" fixed="right">
        <template #default="{ row }">
          <el-button
            link
            type="primary"
            @click="openWorkspace(row.id, true)"
            v-hasPermi="['lunaerp:stock:stock-in:query']"
          >
            <Icon icon="ep:view" class="mr-2px" />查看
          </el-button>
          <el-button
            v-if="row.status === 'DRAFT'"
            link
            type="primary"
            @click="openWorkspace(row.id, false)"
            v-hasPermi="['lunaerp:stock:stock-in:update']"
          >
            <Icon icon="ep:edit" class="mr-2px" />编辑
          </el-button>
          <el-button
            v-if="row.status === 'DRAFT'"
            link
            type="success"
            @click="handleAudit(row.id)"
            v-hasPermi="['lunaerp:stock:stock-in:audit']"
          >
            <Icon icon="ep:check" class="mr-2px" />审核
          </el-button>
          <el-button
            v-if="row.status === 'DRAFT'"
            link
            type="warning"
            @click="handleCancelDraft(row.id)"
            v-hasPermi="['lunaerp:stock:stock-in:update']"
          >
            <Icon icon="ep:circle-close" class="mr-2px" />作废
          </el-button>
          <el-button
            v-if="row.status === 'DRAFT'"
            link
            type="danger"
            @click="handleDelete(row.id)"
            v-hasPermi="['lunaerp:stock:stock-in:delete']"
          >
            <Icon icon="ep:delete" class="mr-2px" />删除
          </el-button>
          <el-button
            v-if="row.status === 'COMPLETED'"
            link
            type="danger"
            @click="handleCancelReversal(row.id)"
            v-hasPermi="['lunaerp:stock:stock-in:audit']"
          >
            <Icon icon="ep:refresh-left" class="mr-2px" />红冲
          </el-button>
        </template>
      </el-table-column>
      <template #empty>
        <el-empty description="暂无入库单数据" :image-size="90" />
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
import { dateFormatter } from '@/utils/formatTime'
import * as StockInApi from '@/api/lunaerp-elmui/stock/stock-in'
import type { StockInVO, StockInPageParam } from '@/api/lunaerp-elmui/stock/stock-in'
import * as WarehouseApi from '@/api/lunaerp-elmui/stock/warehouse'
import type { WarehouseVO } from '@/api/lunaerp-elmui/stock/warehouse'
import * as SupplierApi from '@/api/lunaerp-elmui/baseinfo/supplier'
import type { SupplierVO } from '@/api/lunaerp-elmui/baseinfo/supplier'

defineOptions({ name: 'LunaerpElmuiStockStockIn' })

const router = useRouter()
const message = useMessage()

const loading = ref(true)
const total = ref(0)
const list = ref<StockInVO[]>([])
const queryFormRef = ref()

interface QueryParams {
  pageNo: number
  pageSize: number
  keyword: string
  inType: string
  status: string
  warehouseId: number | undefined
  supplierId: number | undefined
  inDate: string[] | undefined
}

const queryParams = reactive<QueryParams>({
  pageNo: 1,
  pageSize: 20,
  keyword: '',
  inType: '',
  status: '',
  warehouseId: undefined,
  supplierId: undefined,
  inDate: undefined
})

// 仓库下拉选项
const warehouseOptions = ref<WarehouseVO[]>([])
const loadWarehouses = async () => {
  try {
    warehouseOptions.value = await WarehouseApi.getWarehouseListAll('ACTIVE')
  } catch {}
}

// 供应商远程搜索
const supplierOptions = ref<SupplierVO[]>([])
const supplierLoading = ref(false)
const searchSuppliers = async (query: string) => {
  supplierLoading.value = true
  try {
    const res = await SupplierApi.getSupplierPage({
      pageNo: 1,
      pageSize: 20,
      search: query || '',
      status: 'ACTIVE'
    })
    supplierOptions.value = res.list
  } finally {
    supplierLoading.value = false
  }
}

/** 查询入库单分页 */
const getList = async () => {
  loading.value = true
  try {
    const params: StockInPageParam = {
      pageNo: queryParams.pageNo,
      pageSize: queryParams.pageSize,
      keyword: queryParams.keyword || undefined,
      inType: queryParams.inType || undefined,
      status: queryParams.status || undefined,
      warehouseId: queryParams.warehouseId || undefined,
      supplierId: queryParams.supplierId || undefined,
      inDateStart: queryParams.inDate?.[0],
      inDateEnd: queryParams.inDate?.[1]
    }
    const data = await StockInApi.getStockInPage(params)
    list.value = data.list
    total.value = data.total
  } finally {
    loading.value = false
  }
}

/** 搜索 */
const handleQuery = () => {
  queryParams.pageNo = 1
  getList()
}

/** 重置 */
const resetQuery = () => {
  queryFormRef.value?.resetFields()
  queryParams.keyword = ''
  queryParams.inType = ''
  queryParams.status = ''
  queryParams.warehouseId = undefined
  queryParams.supplierId = undefined
  queryParams.inDate = undefined
  handleQuery()
}

/** 打开工作台 */
const openWorkspace = (id?: number, isView = false) => {
  if (!id) {
    router.push('/stock/stock-in/workspace')
  } else {
    router.push({
      path: `/stock/stock-in/workspace/${id}`,
      query: isView ? { mode: 'view' } : undefined
    })
  }
}

/** 删除操作 */
const handleDelete = async (id: number) => {
  try {
    await message.confirm('确定要删除该入库单吗？删除后明细将一并删除。')
    await StockInApi.deleteStockIn(id)
    message.success('删除成功')
    getList()
  } catch {}
}

/** 审核操作 */
const handleAudit = async (id: number) => {
  try {
    await message.confirm('确定要审核该入库单吗？审核后将记入正数库存流水并不可修改。')
    await StockInApi.auditStockIn(id)
    message.success('审核成功')
    getList()
  } catch {}
}

/** 草稿作废操作 */
const handleCancelDraft = async (id: number) => {
  try {
    await message.confirm('确定要作废该入库单草稿吗？作废后不可恢复。')
    await StockInApi.cancelStockIn(id)
    message.success('作废成功')
    getList()
  } catch {}
}

/** 已审核单红冲取消操作 */
const handleCancelReversal = async (id: number) => {
  try {
    await message.confirm(
      '确定要取消并红冲该入库单吗？将生成负数库存流水并冲减库存，且不可撤销！',
      '红冲确认'
    )
    await StockInApi.cancelStockIn(id)
    message.success('红冲取消成功')
    getList()
  } catch {}
}

onMounted(() => {
  loadWarehouses()
  searchSuppliers('')
  getList()
})
</script>

<style scoped>
.cell-code {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-weight: 500;
  color: var(--el-color-primary);
}
</style>
