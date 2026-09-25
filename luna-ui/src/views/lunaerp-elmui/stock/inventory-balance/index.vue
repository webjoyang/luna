<template>
  <div>
    <el-tabs v-model="activeTab" type="border-card" class="inventory-query-tabs" @tab-change="handleTabChange">
      <!-- Tab 1: 库存余额 -->
      <el-tab-pane label="库存余额" name="balance">
        <ContentWrap>
          <!-- 余额搜索区 -->
          <el-form
            class="-mb-15px"
            :model="balanceQueryParams"
            ref="balanceQueryFormRef"
            :inline="true"
            label-width="80px"
          >
            <el-form-item label="产品" prop="productKeyword">
              <el-input
                v-model="balanceQueryParams.productKeyword"
                placeholder="产品编码 / 产品名称"
                clearable
                @keyup.enter="handleBalanceQuery"
                class="!w-240px"
              />
            </el-form-item>
            <el-form-item label="仓库" prop="warehouseId">
              <el-select
                v-model="balanceQueryParams.warehouseId"
                placeholder="全部仓库"
                clearable
                filterable
                class="!w-200px"
              >
                <el-option
                  v-for="item in warehouseOptions"
                  :key="item.id"
                  :label="item.name"
                  :value="item.id!"
                />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button @click="handleBalanceQuery">
                <Icon icon="ep:search" class="mr-2px" />搜索
              </el-button>
              <el-button @click="resetBalanceQuery">
                <Icon icon="ep:refresh" class="mr-2px" />重置
              </el-button>
            </el-form-item>
          </el-form>
        </ContentWrap>

        <ContentWrap>
          <el-table v-loading="balanceLoading" :data="balanceList" border stripe>
            <el-table-column label="产品编码" prop="productCode" min-width="140" show-overflow-tooltip>
              <template #default="{ row }">
                <span class="cell-code">{{ row.productCode || '—' }}</span>
              </template>
            </el-table-column>
            <el-table-column label="产品名称" prop="productName" min-width="180" show-overflow-tooltip>
              <template #default="{ row }">
                {{ row.productName || '—' }}
              </template>
            </el-table-column>
            <el-table-column label="仓库" prop="warehouseName" min-width="150" show-overflow-tooltip>
              <template #default="{ row }">
                {{ row.warehouseName || '—' }}
              </template>
            </el-table-column>
            <el-table-column label="单位" prop="unit" width="100" align="center">
              <template #default="{ row }">
                <DictTag :type="DICT_TYPE.LUNAERP_STOCK_IN_UNIT" :value="row.unit" />
              </template>
            </el-table-column>
            <el-table-column label="库存数量" prop="quantity" width="140" align="right">
              <template #default="{ row }">
                <span class="font-bold text-gray-800">
                  {{ formatNumber(row.quantity) }}
                </span>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="120" align="center" fixed="right">
              <template #default="{ row }">
                <el-button
                  link
                  type="primary"
                  @click="handleViewLedger(row)"
                  v-hasPermi="['lunaerp:stock:inventory-balance:query']"
                >
                  <Icon icon="ep:tickets" class="mr-2px" />查看流水
                </el-button>
              </template>
            </el-table-column>
            <template #empty>
              <el-empty description="暂无库存余额数据" :image-size="90" />
            </template>
          </el-table>

          <Pagination
            :total="balanceTotal"
            v-model:page="balanceQueryParams.pageNo"
            v-model:limit="balanceQueryParams.pageSize"
            @pagination="getBalanceList"
          />
        </ContentWrap>
      </el-tab-pane>

      <!-- Tab 2: 库存流水 -->
      <el-tab-pane label="库存流水" name="ledger">
        <ContentWrap>
          <!-- 流水搜索区 -->
          <el-form
            class="-mb-15px"
            :model="ledgerQueryParams"
            ref="ledgerQueryFormRef"
            :inline="true"
            label-width="80px"
          >
            <el-form-item label="产品" prop="productKeyword">
              <el-input
                v-model="ledgerQueryParams.productKeyword"
                placeholder="产品编码 / 产品名称"
                clearable
                @keyup.enter="handleLedgerQuery"
                class="!w-200px"
              />
            </el-form-item>
            <el-form-item label="仓库" prop="warehouseId">
              <el-select
                v-model="ledgerQueryParams.warehouseId"
                placeholder="全部仓库"
                clearable
                filterable
                @change="handleLedgerWarehouseChange"
                class="!w-180px"
              >
                <el-option
                  v-for="item in warehouseOptions"
                  :key="item.id"
                  :label="item.name"
                  :value="item.id!"
                />
              </el-select>
            </el-form-item>
            <el-form-item label="库位" prop="locationId">
              <el-select
                v-model="ledgerQueryParams.locationId"
                placeholder="全部库位"
                clearable
                filterable
                :disabled="!ledgerQueryParams.warehouseId"
                class="!w-160px"
              >
                <el-option
                  v-for="item in locationOptions"
                  :key="item.id"
                  :label="item.name ? `${item.code} (${item.name})` : item.code"
                  :value="item.id!"
                />
              </el-select>
            </el-form-item>
            <el-form-item label="收支类型" prop="txnType">
              <el-select v-model="ledgerQueryParams.txnType" placeholder="全部" clearable class="!w-140px">
                <el-option label="入库" value="STOCK_IN" />
                <el-option label="红冲" value="STOCK_IN_REVERSAL" />
              </el-select>
            </el-form-item>
            <el-form-item label="关联单号" prop="relatedDocNo">
              <el-input
                v-model="ledgerQueryParams.relatedDocNo"
                placeholder="入库单号"
                clearable
                @keyup.enter="handleLedgerQuery"
                class="!w-180px"
              />
            </el-form-item>
            <el-form-item label="时间区间" prop="createTime">
              <el-date-picker
                v-model="ledgerQueryParams.createTime"
                type="datetimerange"
                value-format="YYYY-MM-DD HH:mm:ss"
                range-separator="-"
                start-placeholder="流水时间起"
                end-placeholder="流水时间止"
                :default-time="[new Date(2000, 0, 1, 0, 0, 0), new Date(2000, 0, 1, 23, 59, 59)]"
                class="!w-340px"
              />
            </el-form-item>
            <el-form-item>
              <el-button @click="handleLedgerQuery">
                <Icon icon="ep:search" class="mr-2px" />搜索
              </el-button>
              <el-button @click="resetLedgerQuery">
                <Icon icon="ep:refresh" class="mr-2px" />重置
              </el-button>
            </el-form-item>
          </el-form>
        </ContentWrap>

        <ContentWrap>
          <el-table v-loading="ledgerLoading" :data="ledgerList" border stripe>
            <el-table-column label="产品编码" prop="productCode" min-width="140" show-overflow-tooltip>
              <template #default="{ row }">
                <span class="cell-code">{{ row.productCode || '—' }}</span>
              </template>
            </el-table-column>
            <el-table-column label="产品名称" prop="productName" min-width="180" show-overflow-tooltip>
              <template #default="{ row }">
                {{ row.productName || '—' }}
              </template>
            </el-table-column>
            <el-table-column label="仓库" prop="warehouseName" min-width="140" show-overflow-tooltip>
              <template #default="{ row }">
                {{ row.warehouseName || '—' }}
              </template>
            </el-table-column>
            <el-table-column label="库位" prop="locationCode" width="120" align="center">
              <template #default="{ row }">
                <el-tag v-if="row.locationCode" type="info" size="small">{{ row.locationCode }}</el-tag>
                <span v-else class="text-gray-400">—</span>
              </template>
            </el-table-column>
            <el-table-column label="收支类型" prop="txnType" width="100" align="center">
              <template #default="{ row }">
                <el-tag v-if="row.txnType === 'STOCK_IN'" type="success" effect="plain">入库</el-tag>
                <el-tag v-else-if="row.txnType === 'STOCK_IN_REVERSAL'" type="danger" effect="plain">红冲</el-tag>
                <el-tag v-else type="info">{{ row.txnType }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="数量" prop="quantity" width="130" align="right">
              <template #default="{ row }">
                <span
                  :class="[
                    'font-bold',
                    row.quantity > 0 ? 'text-green-600' : row.quantity < 0 ? 'text-red-600' : 'text-gray-600'
                  ]"
                >
                  {{ formatSignedQuantity(row.quantity) }}
                </span>
              </template>
            </el-table-column>
            <el-table-column label="单位" prop="unit" width="80" align="center">
              <template #default="{ row }">
                <DictTag :type="DICT_TYPE.LUNAERP_STOCK_IN_UNIT" :value="row.unit" />
              </template>
            </el-table-column>
            <el-table-column label="单价" prop="unitPrice" width="110" align="right">
              <template #default="{ row }">
                {{ row.unitPrice != null ? `¥ ${Number(row.unitPrice).toFixed(2)}` : '—' }}
              </template>
            </el-table-column>
            <el-table-column label="金额" prop="amount" width="120" align="right">
              <template #default="{ row }">
                <span
                  v-if="row.amount != null"
                  :class="[
                    row.amount > 0 ? 'text-green-600' : row.amount < 0 ? 'text-red-600' : ''
                  ]"
                >
                  {{ (row.amount > 0 ? '+' : '') + `¥ ${Number(row.amount).toFixed(2)}` }}
                </span>
                <span v-else class="text-gray-400">—</span>
              </template>
            </el-table-column>
            <el-table-column label="关联单号" prop="relatedDocNo" min-width="160" show-overflow-tooltip>
              <template #default="{ row }">
                <span class="cell-code">{{ row.relatedDocNo || '—' }}</span>
              </template>
            </el-table-column>
            <el-table-column label="批次/缸号" prop="batchNo" width="130" show-overflow-tooltip>
              <template #default="{ row }">
                {{ row.batchNo || '—' }}
              </template>
            </el-table-column>
            <el-table-column
              label="流水时间"
              prop="createTime"
              width="170"
              align="center"
              :formatter="dateFormatter"
            />
            <el-table-column label="备注" prop="remark" min-width="140" show-overflow-tooltip>
              <template #default="{ row }">
                {{ row.remark || '—' }}
              </template>
            </el-table-column>
            <template #empty>
              <el-empty description="暂无库存流水数据" :image-size="90" />
            </template>
          </el-table>

          <Pagination
            :total="ledgerTotal"
            v-model:page="ledgerQueryParams.pageNo"
            v-model:limit="ledgerQueryParams.pageSize"
            @pagination="getLedgerList"
          />
        </ContentWrap>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script lang="ts" setup>
import { DICT_TYPE } from '@/utils/dict'
import { dateFormatter } from '@/utils/formatTime'
import * as InventoryBalanceApi from '@/api/lunaerp-elmui/stock/inventory-balance'
import type { InventoryBalanceVO, InventoryBalancePageParam } from '@/api/lunaerp-elmui/stock/inventory-balance'
import * as StockLedgerApi from '@/api/lunaerp-elmui/stock/stock-ledger'
import type { StockLedgerVO, StockLedgerPageParam } from '@/api/lunaerp-elmui/stock/stock-ledger'
import * as WarehouseApi from '@/api/lunaerp-elmui/stock/warehouse'
import type { WarehouseVO } from '@/api/lunaerp-elmui/stock/warehouse'
import * as WarehouseLocationApi from '@/api/lunaerp-elmui/stock/warehouse-location'
import type { WarehouseLocationVO } from '@/api/lunaerp-elmui/stock/warehouse-location'

defineOptions({ name: 'LunaerpElmuiStockInventoryBalance' })

// 当前激活 Tab（balance / ledger）
const activeTab = ref('balance')

// 仓库下拉选项
const warehouseOptions = ref<WarehouseVO[]>([])
const loadWarehouses = async () => {
  try {
    warehouseOptions.value = await WarehouseApi.getWarehouseListAll('ACTIVE')
  } catch {}
}

// 库位下拉选项（Tab 2 流水按仓库级联）
const locationOptions = ref<WarehouseLocationVO[]>([])
const loadLocationsForWarehouse = async (warehouseId?: number) => {
  if (!warehouseId) {
    locationOptions.value = []
    return
  }
  try {
    locationOptions.value = await WarehouseLocationApi.getWarehouseLocationListByWarehouse(warehouseId, 'ACTIVE')
  } catch {
    locationOptions.value = []
  }
}

// ==================== Tab 1: 库存余额 ====================
const balanceLoading = ref(false)
const balanceTotal = ref(0)
const balanceList = ref<InventoryBalanceVO[]>([])
const balanceQueryFormRef = ref()

interface BalanceQueryParams extends InventoryBalancePageParam {
  pageNo: number
  pageSize: number
  productKeyword: string
  warehouseId: number | undefined
}

const balanceQueryParams = reactive<BalanceQueryParams>({
  pageNo: 1,
  pageSize: 20,
  productKeyword: '',
  warehouseId: undefined
})

const getBalanceList = async () => {
  balanceLoading.value = true
  try {
    const data = await InventoryBalanceApi.getInventoryBalancePage(balanceQueryParams)
    balanceList.value = data.list || []
    balanceTotal.value = data.total || 0
  } finally {
    balanceLoading.value = false
  }
}

const handleBalanceQuery = () => {
  balanceQueryParams.pageNo = 1
  getBalanceList()
}

const resetBalanceQuery = () => {
  balanceQueryFormRef.value?.resetFields()
  balanceQueryParams.productKeyword = ''
  balanceQueryParams.warehouseId = undefined
  handleBalanceQuery()
}

// 点击余额行"查看流水"：联动切换 Tab 2 并带产品+仓库条件（透明填入表单输入框，对齐 Spec 42）
const handleViewLedger = async (row: InventoryBalanceVO) => {
  // 设置 Tab 2 筛选参数（以产品编码或产品名称填入 productKeyword 输入框）
  ledgerQueryParams.productKeyword = row.productCode || row.productName || ''
  ledgerQueryParams.warehouseId = row.warehouseId
  ledgerQueryParams.locationId = undefined
  ledgerQueryParams.txnType = ''
  ledgerQueryParams.relatedDocNo = ''
  ledgerQueryParams.createTime = undefined
  ledgerQueryParams.pageNo = 1

  // 预载库位选项
  if (row.warehouseId) {
    await loadLocationsForWarehouse(row.warehouseId)
  }

  // 切换到 Tab 2 并执行查询
  activeTab.value = 'ledger'
  getLedgerList()
}

// ==================== Tab 2: 库存流水 ====================
const ledgerLoading = ref(false)
const ledgerTotal = ref(0)
const ledgerList = ref<StockLedgerVO[]>([])
const ledgerQueryFormRef = ref()

interface LedgerQueryParams extends StockLedgerPageParam {
  pageNo: number
  pageSize: number
  productKeyword: string
  warehouseId: number | undefined
  locationId: number | undefined
  txnType: string
  relatedDocNo: string
  createTime: string[] | undefined
}

const ledgerQueryParams = reactive<LedgerQueryParams>({
  pageNo: 1,
  pageSize: 20,
  productKeyword: '',
  warehouseId: undefined,
  locationId: undefined,
  txnType: '',
  relatedDocNo: '',
  createTime: undefined
})

const handleLedgerWarehouseChange = (val?: number) => {
  ledgerQueryParams.locationId = undefined
  loadLocationsForWarehouse(val)
}

const getLedgerList = async () => {
  ledgerLoading.value = true
  try {
    const params: StockLedgerPageParam = {
      pageNo: ledgerQueryParams.pageNo,
      pageSize: ledgerQueryParams.pageSize,
      productKeyword: ledgerQueryParams.productKeyword || undefined,
      warehouseId: ledgerQueryParams.warehouseId || undefined,
      locationId: ledgerQueryParams.locationId || undefined,
      txnType: ledgerQueryParams.txnType || undefined,
      relatedDocNo: ledgerQueryParams.relatedDocNo || undefined,
      createTime: ledgerQueryParams.createTime
    }
    const data = await StockLedgerApi.getStockLedgerPage(params)
    ledgerList.value = data.list || []
    ledgerTotal.value = data.total || 0
  } finally {
    ledgerLoading.value = false
  }
}

const handleLedgerQuery = () => {
  ledgerQueryParams.pageNo = 1
  getLedgerList()
}

const resetLedgerQuery = () => {
  ledgerQueryFormRef.value?.resetFields()
  ledgerQueryParams.productKeyword = ''
  ledgerQueryParams.warehouseId = undefined
  ledgerQueryParams.locationId = undefined
  ledgerQueryParams.txnType = ''
  ledgerQueryParams.relatedDocNo = ''
  ledgerQueryParams.createTime = undefined
  locationOptions.value = []
  handleLedgerQuery()
}

// 切换 Tab 时按需加载
const handleTabChange = (name: string | number) => {
  if (name === 'balance' && balanceList.value.length === 0) {
    getBalanceList()
  } else if (name === 'ledger' && ledgerList.value.length === 0) {
    getLedgerList()
  }
}

// ==================== 格式化辅助函数 ====================
const formatNumber = (num: number | string | undefined | null) => {
  if (num == null) return '0.00'
  const val = Number(num)
  return isNaN(val) ? '0.00' : val.toFixed(2)
}

const formatSignedQuantity = (num: number | string | undefined | null) => {
  const formatted = formatNumber(num)
  const val = Number(num)
  return val > 0 ? `+${formatted}` : formatted
}

// ==================== 生命周期 ====================
onMounted(async () => {
  await loadWarehouses()
  getBalanceList()
})
</script>

<style scoped>
.cell-code {
  font-family: var(--el-font-family-monospace, monospace);
  font-weight: 500;
  color: var(--el-color-primary);
}

.inventory-query-tabs :deep(.el-tabs__content) {
  padding-top: 16px;
}
</style>
