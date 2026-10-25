<template>
  <el-dialog
    v-model="dialogVisible"
    :title="mode === 'CONTRACT' ? '选择外销销售合同' : '选择发货单'"
    width="800px"
    append-to-body
    :close-on-click-modal="false"
  >
    <el-form :inline="true" :model="queryParams" class="demo-form-inline">
      <el-form-item label="单号/客户">
        <el-input
          v-model="queryParams.keyword"
          placeholder="单号或客户关键词"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleQuery">查询</el-button>
        <el-button @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <!-- 合同表格 -->
    <el-table
      v-if="mode === 'CONTRACT'"
      v-loading="loading"
      :data="tableData"
      highlight-current-row
      @current-change="handleRowChange"
      @row-dblclick="handleConfirm"
      height="380px"
    >
      <el-table-column label="合同编号" prop="contractNo" min-width="140" />
      <el-table-column label="客户名称" prop="clientName" min-width="160" show-overflow-tooltip />
      <el-table-column label="币种" prop="currency" width="80" align="center" />
      <el-table-column label="贸易术语" prop="incoterm" width="100" align="center" />
      <el-table-column label="签约日期" prop="signDate" width="120" align="center" />
      <el-table-column label="总金额" prop="totalAmount" width="110" align="right">
        <template #default="{ row }">
          {{ row.totalAmount != null ? row.totalAmount.toFixed(2) : '—' }}
        </template>
      </el-table-column>
    </el-table>

    <!-- 发货单表格 -->
    <el-table
      v-else
      v-loading="loading"
      :data="tableData"
      highlight-current-row
      @current-change="handleRowChange"
      @row-dblclick="handleConfirm"
      height="380px"
    >
      <el-table-column label="发货单号" prop="shipmentNo" min-width="150" />
      <el-table-column label="客户名称" prop="customerName" min-width="160" show-overflow-tooltip />
      <el-table-column label="关联合同" prop="contractNo" min-width="130" />
      <el-table-column label="发货日期" prop="shippingDate" width="120" align="center" />
      <el-table-column label="发货状态" prop="status" width="90" align="center">
        <template #default="{ row }">
          <el-tag v-if="row.status === 'COMPLETED'" type="success">已发货</el-tag>
          <el-tag v-else-if="row.status === 'DRAFT'" type="info">草稿</el-tag>
          <el-tag v-else type="danger">已作废</el-tag>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
    <div class="flex justify-end mt-12px">
      <el-pagination
        v-model:current-page="queryParams.pageNo"
        v-model:page-size="queryParams.pageSize"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        @size-change="loadData"
        @current-change="loadData"
      />
    </div>

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :disabled="!selectedRow" @click="handleConfirm">
          确定生成
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script lang="ts" setup>
import { ref, reactive } from 'vue'
import { getSalesContractPage } from '@/api/lunaerp-elmui/business/sales-contract'
import { getShipmentPage } from '@/api/lunaerp-elmui/business/shipment'

defineOptions({ name: 'SelectSourceDialog' })

const emit = defineEmits<{
  (e: 'select', payload: { type: 'CONTRACT' | 'SHIPMENT'; id: number; no: string }): void
}>()

const dialogVisible = ref(false)
const mode = ref<'CONTRACT' | 'SHIPMENT'>('CONTRACT')
const loading = ref(false)
const tableData = ref<any[]>([])
const total = ref(0)
const selectedRow = ref<any>(null)

const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  keyword: ''
})

const open = (sourceMode: 'CONTRACT' | 'SHIPMENT') => {
  mode.value = sourceMode
  dialogVisible.value = true
  selectedRow.value = null
  resetQuery()
}

const handleQuery = () => {
  queryParams.pageNo = 1
  loadData()
}

const resetQuery = () => {
  queryParams.keyword = ''
  queryParams.pageNo = 1
  loadData()
}

const loadData = async () => {
  loading.value = true
  try {
    if (mode.value === 'CONTRACT') {
      const res = await getSalesContractPage({
        pageNo: queryParams.pageNo,
        pageSize: queryParams.pageSize,
        keyword: queryParams.keyword,
        tradeType: 'EXPORT' // 仅外销合同允许生成出口单证
      })
      tableData.value = res?.list || []
      total.value = res?.total || 0
    } else {
      const res = await getShipmentPage({
        pageNo: queryParams.pageNo,
        pageSize: queryParams.pageSize,
        keyword: queryParams.keyword
      })
      tableData.value = res?.list || []
      total.value = res?.total || 0
    }
  } finally {
    loading.value = false
  }
}

const handleRowChange = (row: any) => {
  selectedRow.value = row
}

const handleConfirm = () => {
  if (!selectedRow.value) return
  const id = selectedRow.value.id
  const no = mode.value === 'CONTRACT' ? selectedRow.value.contractNo : selectedRow.value.shipmentNo
  dialogVisible.value = false
  emit('select', { type: mode.value, id, no })
}

defineExpose({ open })
</script>
