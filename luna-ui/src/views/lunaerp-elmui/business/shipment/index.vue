<template>
  <ContentWrap>
    <!-- 搜索区（多条件检索：关键词/状态/客户/仓库/合同号/发货日期区间） -->
    <el-form class="-mb-15px" :model="queryParams" ref="queryFormRef" :inline="true" label-width="80px">
      <el-form-item label="关键词" prop="keyword">
        <el-input
          v-model="queryParams.keyword"
          placeholder="发货单号 / 合同号 / 客户 / 卷号"
          clearable
          @keyup.enter="handleQuery"
          class="!w-240px"
        />
      </el-form-item>
      <el-form-item label="单据状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="全部" clearable class="!w-150px">
          <el-option
            v-for="dict in getStrDictOptions(DICT_TYPE.LUNAERP_SHIPMENT_STATUS)"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="客户" prop="customerId">
        <el-select
          v-model="queryParams.customerId"
          placeholder="搜索客户"
          clearable
          filterable
          remote
          reserve-keyword
          :remote-method="searchCustomers"
          :loading="customerLoading"
          class="!w-180px"
        >
          <el-option
            v-for="item in customerOptions"
            :key="item.id"
            :label="item.companyName"
            :value="item.id!"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="发货仓库" prop="warehouseId">
        <el-select v-model="queryParams.warehouseId" placeholder="全部仓库" clearable class="!w-160px">
          <el-option
            v-for="item in warehouseOptions"
            :key="item.id"
            :label="item.name"
            :value="item.id!"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="合同号" prop="contractNo">
        <el-input
          v-model="queryParams.contractNo"
          placeholder="销售合同号"
          clearable
          @keyup.enter="handleQuery"
          class="!w-160px"
        />
      </el-form-item>
      <el-form-item label="发货日期" prop="shippingDate">
        <el-date-picker
          v-model="queryParams.shippingDate"
          type="daterange"
          value-format="YYYY-MM-DD"
          range-separator="-"
          start-placeholder="发货日期起"
          end-placeholder="发货日期止"
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
          v-hasPermi="['lunaerp:business:shipment:create']"
        >
          <Icon icon="ep:plus" class="mr-2px" /> 新建发货单
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <ContentWrap>
    <el-table v-loading="loading" :data="list" border stripe>
      <el-table-column label="发货单号" prop="shipmentNo" min-width="160" show-overflow-tooltip>
        <template #default="{ row }">
          <span class="font-mono text-blue-600 cursor-pointer" @click="openWorkspace(row.id, true)">
            {{ row.shipmentNo || '—' }}
          </span>
        </template>
      </el-table-column>
      <el-table-column label="客户" prop="customerName" min-width="160" show-overflow-tooltip>
        <template #default="{ row }">{{ row.customerName || '—' }}</template>
      </el-table-column>
      <el-table-column label="销售合同号" prop="contractNo" min-width="140" show-overflow-tooltip>
        <template #default="{ row }">{{ row.contractNo || '—' }}</template>
      </el-table-column>
      <el-table-column label="发货仓库" prop="warehouseName" min-width="130" show-overflow-tooltip>
        <template #default="{ row }">{{ row.warehouseName || '—' }}</template>
      </el-table-column>
      <el-table-column label="发货日期" prop="shippingDate" width="110" align="center">
        <template #default="{ row }">{{ row.shippingDate || '—' }}</template>
      </el-table-column>
      <el-table-column label="总匹数" prop="totalPieces" width="90" align="right">
        <template #default="{ row }">
          {{ row.totalPieces != null ? `${row.totalPieces} 匹` : '—' }}
        </template>
      </el-table-column>
      <el-table-column label="货物金额" prop="totalAmount" width="120" align="right">
        <template #default="{ row }">
          {{ row.totalAmount != null ? `¥ ${Number(row.totalAmount).toFixed(2)}` : '—' }}
        </template>
      </el-table-column>
      <el-table-column label="运费" prop="freightFee" width="100" align="right">
        <template #default="{ row }">
          {{ row.freightFee != null ? `¥ ${Number(row.freightFee).toFixed(2)}` : '¥ 0.00' }}
        </template>
      </el-table-column>
      <el-table-column label="签收状态" prop="signStatus" width="100" align="center">
        <template #default="{ row }">
          <DictTag :type="DICT_TYPE.LUNAERP_SHIPMENT_SIGN_STATUS" :value="row.signStatus" />
        </template>
      </el-table-column>
      <el-table-column label="单据状态" prop="status" width="100" align="center">
        <template #default="{ row }">
          <DictTag :type="DICT_TYPE.LUNAERP_SHIPMENT_STATUS" :value="row.status" />
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
      <el-table-column label="操作" width="200" align="center" fixed="right">
        <template #default="{ row }">
          <el-button
            link
            type="primary"
            @click="openWorkspace(row.id, true)"
            v-hasPermi="['lunaerp:business:shipment:query']"
          >
            <Icon icon="ep:view" class="mr-2px" />查看
          </el-button>
          <el-button
            v-if="row.status === 'DRAFT'"
            link
            type="primary"
            @click="openWorkspace(row.id, false)"
            v-hasPermi="['lunaerp:business:shipment:update']"
          >
            <Icon icon="ep:edit" class="mr-2px" />编辑
          </el-button>
          <el-button
            v-if="row.status === 'DRAFT'"
            link
            type="danger"
            @click="handleDelete(row)"
            v-hasPermi="['lunaerp:business:shipment:delete']"
          >
            <Icon icon="ep:delete" class="mr-2px" />删除
          </el-button>
        </template>
      </el-table-column>
      <template #empty>
        <el-empty description="暂无发货单数据" :image-size="90" />
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
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useMessage } from '@/hooks/web/useMessage'
import { DICT_TYPE, getStrDictOptions } from '@/utils/dict'
import { dateFormatter } from '@/utils/formatTime'
import * as ShipmentApi from '@/api/lunaerp-elmui/business/shipment'
import type { ShipmentVO, ShipmentPageParam } from '@/api/lunaerp-elmui/business/shipment'
import * as WarehouseApi from '@/api/lunaerp-elmui/stock/warehouse'
import type { WarehouseVO } from '@/api/lunaerp-elmui/stock/warehouse'
import * as CustomerApi from '@/api/lunaerp-elmui/baseinfo/customer'
import type { CustomerVO } from '@/api/lunaerp-elmui/baseinfo/customer'

defineOptions({ name: 'LunaerpElmuiBusinessShipment' })

const router = useRouter()
const message = useMessage()

const loading = ref(true)
const total = ref(0)
const list = ref<ShipmentVO[]>([])
const queryFormRef = ref()

interface QueryParams {
  pageNo: number
  pageSize: number
  keyword: string
  status: string
  customerId: number | undefined
  warehouseId: number | undefined
  contractNo: string
  shippingDate: string[] | undefined
}

const queryParams = reactive<QueryParams>({
  pageNo: 1,
  pageSize: 20,
  keyword: '',
  status: '',
  customerId: undefined,
  warehouseId: undefined,
  contractNo: '',
  shippingDate: undefined
})

// 仓库下拉选项
const warehouseOptions = ref<WarehouseVO[]>([])
const loadWarehouses = async () => {
  try {
    warehouseOptions.value = await WarehouseApi.getWarehouseListAll('ACTIVE')
  } catch {}
}

// 客户远程搜索
const customerOptions = ref<CustomerVO[]>([])
const customerLoading = ref(false)
const searchCustomers = async (query: string) => {
  customerLoading.value = true
  try {
    const res = await CustomerApi.getCustomerPage({
      pageNo: 1,
      pageSize: 20,
      search: query || '',
      status: 'ACTIVE'
    })
    customerOptions.value = res.list
  } finally {
    customerLoading.value = false
  }
}

/** 查询发货单分页 */
const getList = async () => {
  loading.value = true
  try {
    const params: ShipmentPageParam = {
      pageNo: queryParams.pageNo,
      pageSize: queryParams.pageSize,
      keyword: queryParams.keyword || undefined,
      status: queryParams.status || undefined,
      customerId: queryParams.customerId || undefined,
      warehouseId: queryParams.warehouseId || undefined,
      contractNo: queryParams.contractNo || undefined,
      shippingDateStart: queryParams.shippingDate?.[0],
      shippingDateEnd: queryParams.shippingDate?.[1]
    }
    const data = await ShipmentApi.getShipmentPage(params)
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
  queryParams.contractNo = ''
  queryParams.shippingDate = undefined
  handleQuery()
}

/** 打开工作台 */
const openWorkspace = (id?: number, isView?: boolean) => {
  if (!id) {
    router.push('/business/shipment/workspace')
  } else {
    router.push({
      path: `/business/shipment/workspace/${id}`,
      query: isView ? { mode: 'view' } : undefined
    })
  }
}

/** 删除发货单 */
const handleDelete = async (row: ShipmentVO) => {
  try {
    await message.confirm(`确认删除发货单【${row.shipmentNo || row.id}】吗？删除后明细与卷清单将一并删除。`)
    await ShipmentApi.deleteShipment(row.id!)
    message.success('删除成功')
    await getList()
  } catch {}
}

onMounted(() => {
  loadWarehouses()
  searchCustomers('')
  getList()
})
</script>
