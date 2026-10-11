<template>
  <ContentWrap>
    <!-- 搜索区 -->
    <el-form
      class="-mb-15px"
      :model="queryParams"
      ref="queryFormRef"
      :inline="true"
      label-width="80px"
    >
      <el-form-item label="关键词" prop="keyword">
        <el-input
          v-model="queryParams.keyword"
          placeholder="对账单号 / 供应商名称"
          clearable
          @keyup.enter="handleQuery"
          class="!w-240px"
        />
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
          class="!w-200px"
        >
          <el-option
            v-for="item in supplierOptions"
            :key="item.id"
            :label="item.companyName"
            :value="item.id!"
          />
        </el-select>
      </el-form-item>

      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="全部状态" clearable class="!w-150px">
          <el-option
            v-for="dict in getStrDictOptions(DICT_TYPE.LUNAERP_SUPPLIER_RECON_STATUS)"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>

      <el-form-item label="对账区间" prop="periodRange">
        <el-date-picker
          v-model="queryParams.periodRange"
          type="daterange"
          value-format="YYYY-MM-DD"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          class="!w-240px"
        />
      </el-form-item>

      <el-form-item>
        <el-button @click="handleQuery">
          <Icon icon="ep:search" class="mr-2px" />搜索
        </el-button>
        <el-button @click="resetQuery">
          <Icon icon="ep:refresh" class="mr-2px" />重置
        </el-button>
        <el-button
          type="primary"
          plain
          @click="openWorkspace()"
          v-hasPermi="['lunaerp:finance:supplierrecon:create']"
        >
          <Icon icon="ep:plus" class="mr-2px" />新建对账单
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <ContentWrap>
    <!-- 对账单列表表格 -->
    <el-table v-loading="loading" :data="list" border stripe>
      <el-table-column label="对账单号" prop="reconNo" min-width="160" show-overflow-tooltip>
        <template #default="{ row }">
          <el-link type="primary" :underline="false" @click="openDetail(row.id)">
            <span class="cell-code">{{ row.reconNo || '—' }}</span>
          </el-link>
        </template>
      </el-table-column>

      <el-table-column label="供应商名称" prop="supplierName" min-width="160" show-overflow-tooltip>
        <template #default="{ row }">{{ row.supplierName || '—' }}</template>
      </el-table-column>

      <el-table-column label="对账区间" min-width="180" align="center">
        <template #default="{ row }">
          <span class="font-mono text-xs">
            {{ row.periodStart && row.periodEnd ? `${row.periodStart} ~ ${row.periodEnd}` : '—' }}
          </span>
        </template>
      </el-table-column>

      <el-table-column label="确认金额" prop="totalAmount" width="130" align="right">
        <template #default="{ row }">
          <span class="font-mono font-bold">
            ¥ {{ formatAmount(row.totalAmount) }}
          </span>
        </template>
      </el-table-column>

      <el-table-column label="已申请金额" prop="appliedAmount" width="130" align="right">
        <template #default="{ row }">
          <span class="font-mono text-green-700 font-medium">
            ¥ {{ formatAmount(row.appliedAmount) }}
          </span>
        </template>
      </el-table-column>

      <el-table-column label="状态" prop="status" width="100" align="center">
        <template #default="{ row }">
          <DictTag :type="DICT_TYPE.LUNAERP_SUPPLIER_RECON_STATUS" :value="row.status" />
        </template>
      </el-table-column>

      <el-table-column label="确认人" prop="confirmedByName" width="110" align="center">
        <template #default="{ row }">
          {{ row.confirmedByName || row.confirmedBy || '—' }}
        </template>
      </el-table-column>

      <el-table-column
        label="创建时间"
        prop="createTime"
        width="160"
        align="center"
        :formatter="dateFormatter"
      />

      <!-- 操作列 -->
      <el-table-column label="操作" width="260" align="center" fixed="right">
        <template #default="{ row }">
          <el-button
            link
            type="primary"
            @click="openDetail(row.id)"
            v-hasPermi="['lunaerp:finance:supplierrecon:query']"
          >
            <Icon icon="ep:view" class="mr-2px" />详情
          </el-button>

          <!-- 草稿态操作 -->
          <template v-if="row.status === 'DRAFT'">
            <el-button
              link
              type="primary"
              @click="openWorkspace(row.id)"
              v-hasPermi="['lunaerp:finance:supplierrecon:update']"
            >
              <Icon icon="ep:edit" class="mr-2px" />编辑
            </el-button>
            <el-button
              link
              type="danger"
              @click="handleDelete(row)"
              v-hasPermi="['lunaerp:finance:supplierrecon:delete']"
            >
              <Icon icon="ep:delete" class="mr-2px" />删除
            </el-button>
          </template>

          <!-- 已确认态操作 -->
          <template v-if="row.status === 'CONFIRMED'">
            <el-button
              link
              type="success"
              @click="handleGeneratePaymentApply(row)"
              v-hasPermi="['lunaerp:finance:supplierrecon:generate']"
            >
              <Icon icon="ep:money" class="mr-2px" />生成付款申请
            </el-button>
            <el-button
              link
              type="primary"
              @click="handlePrint(row.id)"
              v-hasPermi="['lunaerp:finance:supplierrecon:print']"
            >
              <Icon icon="ep:printer" class="mr-2px" />打印
            </el-button>
            <el-button
              link
              type="warning"
              @click="handleCancel(row)"
              v-hasPermi="['lunaerp:finance:supplierrecon:confirm']"
            >
              <Icon icon="ep:circle-close" class="mr-2px" />作废
            </el-button>
          </template>
        </template>
      </el-table-column>

      <template #empty>
        <el-empty description="暂无供应商对账单数据" :image-size="90" />
      </template>
    </el-table>

    <!-- 分页组件 -->
    <Pagination
      :total="total"
      v-model:page="queryParams.pageNo"
      v-model:limit="queryParams.pageSize"
      @pagination="getList"
    />
  </ContentWrap>

  <!-- 详情抽屉 -->
  <SupplierReconDetailDrawer ref="detailDrawerRef" @print="handlePrint" />

  <!-- 打印弹窗分发器 -->
  <PrintDispatcher biz-type="VENDOR_RECON" ref="printDispatcherRef" />
</template>

<script lang="ts" setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { DICT_TYPE, getStrDictOptions } from '@/utils/dict'
import { dateFormatter } from '@/utils/formatTime'
import { useMessage } from '@/hooks/web/useMessage'
import * as SupplierReconApi from '@/api/lunaerp-elmui/finance/supplier-recon'
import type {
  SupplierReconVO,
  SupplierReconPageParam
} from '@/api/lunaerp-elmui/finance/supplier-recon'
import * as SupplierApi from '@/api/lunaerp-elmui/baseinfo/supplier'
import type { SupplierVO } from '@/api/lunaerp-elmui/baseinfo/supplier'
import { formatAmount } from './components/print/utils'
import SupplierReconDetailDrawer from './components/SupplierReconDetailDrawer.vue'
import { PrintDispatcher } from '@/components/Print'

defineOptions({ name: 'LunaerpElmuiFinanceSupplierRecon' })

const router = useRouter()
const message = useMessage()

const loading = ref(true)
const total = ref(0)
const list = ref<SupplierReconVO[]>([])
const queryFormRef = ref()
const detailDrawerRef = ref()
const printDispatcherRef = ref()

interface QueryParamsState {
  pageNo: number
  pageSize: number
  keyword: string
  supplierId: number | undefined
  status: string
  periodRange: [string, string] | undefined
}

const queryParams = reactive<QueryParamsState>({
  pageNo: 1,
  pageSize: 20,
  keyword: '',
  supplierId: undefined,
  status: '',
  periodRange: undefined
})

// 供应商远程搜索下拉
const supplierOptions = ref<SupplierVO[]>([])
const supplierLoading = ref(false)
const searchSuppliers = async (query: string) => {
  supplierLoading.value = true
  try {
    const res = await SupplierApi.getSupplierPage({
      pageNo: 1,
      pageSize: 30,
      search: query || '',
      status: 'ACTIVE'
    })
    supplierOptions.value = res.list
  } finally {
    supplierLoading.value = false
  }
}

/** 查询分页 */
const getList = async () => {
  loading.value = true
  try {
    const params: SupplierReconPageParam = {
      pageNo: queryParams.pageNo,
      pageSize: queryParams.pageSize,
      keyword: queryParams.keyword || undefined,
      supplierId: queryParams.supplierId || undefined,
      status: queryParams.status || undefined,
      periodStart: queryParams.periodRange?.[0],
      periodEnd: queryParams.periodRange?.[1]
    }
    const data = await SupplierReconApi.getSupplierReconPage(params)
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
  queryParams.supplierId = undefined
  queryParams.status = ''
  queryParams.periodRange = undefined
  handleQuery()
}

/** 打开工作台（新建/编辑） */
const openWorkspace = (id?: number) => {
  if (!id) {
    router.push('/finance/supplier-recon/workspace')
  } else {
    router.push(`/finance/supplier-recon/workspace/${id}`)
  }
}

/** 打开详情抽屉 */
const openDetail = (id: number) => {
  detailDrawerRef.value?.open(id)
}

/** 打开打印弹窗 */
const handlePrint = (id: number) => {
  printDispatcherRef.value?.open({ bizId: id })
}

/** 生成付款申请 */
const handleGeneratePaymentApply = (row: SupplierReconVO) => {
  router.push({
    path: '/finance/payment',
    query: {
      reconId: row.id,
      supplierId: row.supplierId,
      reconNo: row.reconNo,
      action: 'create'
    }
  })
}

/** 草稿删除（移入回收站） */
const handleDelete = async (row: SupplierReconVO) => {
  try {
    await message.confirm(
      `确定将草稿对账单【${row.reconNo || row.id}】移入回收站吗？移入后可在回收站恢复。`,
      '移入回收站'
    )
    await SupplierReconApi.deleteSupplierRecon(row.id!)
    message.success('移入回收站成功')
    getList()
  } catch {}
}

/** 已确认对账单作废 */
const handleCancel = async (row: SupplierReconVO) => {
  try {
    await ElMessageBox.confirm(
      `确定要作废对账单【${row.reconNo || row.id}】吗？作废后不可恢复，引用的源明细将释放回待对账池。`,
      '作废确认',
      {
        type: 'warning',
        confirmButtonText: '确定作废',
        cancelButtonText: '取消'
      }
    )
    await SupplierReconApi.cancelSupplierRecon(row.id!)
    message.success('作废成功')
    getList()
  } catch (err: any) {
    if (err !== 'cancel') {
      const errCode = err?.response?.data?.code
      const msg = err?.response?.data?.msg || err?.message || '作废失败'
      // 006 守卫报错：存在审批中/已审批申请
      if (errCode === 1030022006 || msg.includes('存在付款申请')) {
        await ElMessageBox.alert(msg, '禁止作废', {
          type: 'error',
          confirmButtonText: '我知道了'
        })
      } else {
        message.error(msg)
      }
    }
  }
}

onMounted(() => {
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
