<template>
  <el-dialog
    v-model="dialogVisible"
    title="选择发货单（仅已发货且含可退量）"
    width="850px"
    append-to-body
    :close-on-click-modal="false"
  >
    <!-- 筛选区 -->
    <el-form :inline="true" :model="queryParams" class="mb-15px">
      <el-form-item label="发货单号">
        <el-input
          v-model="queryParams.shipmentNo"
          placeholder="发货单号"
          clearable
          @keyup.enter="handleQuery"
          class="!w-180px"
        />
      </el-form-item>
      <el-form-item label="客户">
        <el-select
          v-model="queryParams.customerId"
          placeholder="搜索客户"
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
      <el-form-item>
        <el-button @click="handleQuery"> <Icon icon="ep:search" class="mr-2px" /> 搜索 </el-button>
        <el-button @click="resetQuery"> <Icon icon="ep:refresh" class="mr-2px" /> 重置 </el-button>
      </el-form-item>
    </el-form>

    <!-- 候选列表 -->
    <el-table
      v-loading="loading"
      :data="candidates"
      border
      stripe
      highlight-current-row
      max-height="400"
      @current-change="handleCurrentChange"
    >
      <el-table-column label="选择" width="55" align="center">
        <template #default="{ row }">
          <el-radio v-model="selectedId" :label="row.id">{{ '' }}</el-radio>
        </template>
      </el-table-column>
      <el-table-column label="发货单号" prop="shipmentNo" min-width="150" show-overflow-tooltip />
      <el-table-column label="客户名称" prop="customerName" min-width="160" show-overflow-tooltip />
      <el-table-column label="合同号" prop="contractNo" width="130" show-overflow-tooltip>
        <template #default="{ row }">
          <span>{{ row.contractNo || '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="发货仓库" prop="warehouseName" width="130" show-overflow-tooltip />
      <el-table-column label="币种" prop="currency" width="70" align="center" />
      <el-table-column label="发货日期" prop="shippingDate" width="110" align="center" />
      <el-table-column label="货物金额" prop="totalAmount" width="110" align="right">
        <template #default="{ row }">
          <span>{{
            row.totalAmount !== undefined && row.totalAmount !== null
              ? Number(row.totalAmount).toFixed(2)
              : '-'
          }}</span>
        </template>
      </el-table-column>
    </el-table>

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="dialogVisible = false">取 消</el-button>
        <el-button
          type="primary"
          :disabled="!selectedRow"
          @click="handleConfirmSelect"
          v-hasPermi="['lunaerp:business:sales-return:create']"
        >
          确 定
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import {
  getShipmentCandidates,
  type SalesReturnCandidateVO
} from '@/api/lunaerp-elmui/business/sales-return'
import { getCustomerPage, type CustomerVO } from '@/api/lunaerp-elmui/baseinfo/customer'

const emit = defineEmits<{
  (e: 'select', row: SalesReturnCandidateVO): void
}>()

const dialogVisible = ref(false)
const loading = ref(false)
const candidates = ref<SalesReturnCandidateVO[]>([])
const selectedId = ref<number | null>(null)
const selectedRow = ref<SalesReturnCandidateVO | null>(null)

const queryParams = reactive({
  shipmentNo: '',
  customerId: undefined as number | undefined
})

const customerLoading = ref(false)
const customerOptions = ref<CustomerVO[]>([])

const searchCustomers = async (query: string) => {
  if (!query || !query.trim()) {
    customerOptions.value = []
    return
  }
  customerLoading.value = true
  try {
    const res = await getCustomerPage({ search: query.trim(), pageSize: 20, status: 'ACTIVE' })
    customerOptions.value = res.list || []
  } finally {
    customerLoading.value = false
  }
}

const loadCandidates = async () => {
  loading.value = true
  try {
    const list = await getShipmentCandidates({
      customerId: queryParams.customerId,
      shipmentNo: queryParams.shipmentNo || undefined
    })
    candidates.value = list || []
  } finally {
    loading.value = false
  }
}

const open = (preselectedCustomerId?: number) => {
  dialogVisible.value = true
  selectedId.value = null
  selectedRow.value = null
  queryParams.shipmentNo = ''
  queryParams.customerId = preselectedCustomerId
  loadCandidates()
}

const handleQuery = () => {
  selectedId.value = null
  selectedRow.value = null
  loadCandidates()
}

const resetQuery = () => {
  queryParams.shipmentNo = ''
  queryParams.customerId = undefined
  selectedId.value = null
  selectedRow.value = null
  loadCandidates()
}

const handleCurrentChange = (row: SalesReturnCandidateVO | null) => {
  if (row) {
    selectedId.value = row.id
    selectedRow.value = row
  }
}

const handleConfirmSelect = () => {
  if (!selectedRow.value) return
  emit('select', selectedRow.value)
  dialogVisible.value = false
}

defineExpose({ open })
</script>
