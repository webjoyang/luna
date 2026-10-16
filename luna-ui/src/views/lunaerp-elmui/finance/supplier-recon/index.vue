<template>
  <ContentWrap>
    <!-- 搜索过滤区（供应商远程选择/状态/区间/关键词） -->
    <el-form
      class="-mb-15px"
      :model="queryParams"
      ref="queryFormRef"
      :inline="true"
      label-width="70px"
    >
      <el-form-item label="关键词" prop="keyword">
        <el-input
          v-model="queryParams.keyword"
          placeholder="对账单号 / 供应商名称"
          clearable
          @keyup.enter="handleQuery"
          class="!w-220px"
        />
      </el-form-item>

      <el-form-item label="供应商" prop="supplierId">
        <el-select
          v-model="queryParams.supplierId"
          placeholder="搜索并选择供应商"
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
        <el-select v-model="queryParams.status" placeholder="全部状态" clearable class="!w-140px">
          <el-option
            v-for="dict in getStrDictOptions(DICT_TYPE.LUNAERP_SUPPLIER_RECON_STATUS)"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>

      <el-form-item label="对账区间" prop="dateRange">
        <el-date-picker
          v-model="queryParams.dateRange"
          type="daterange"
          value-format="YYYY-MM-DD"
          range-separator="至"
          start-placeholder="起始日期"
          end-placeholder="截止日期"
          class="!w-240px"
        />
      </el-form-item>

      <el-form-item>
        <el-button @click="handleQuery"> <Icon icon="ep:search" class="mr-2px" /> 搜索 </el-button>
        <el-button @click="resetQuery"> <Icon icon="ep:refresh" class="mr-2px" /> 重置 </el-button>
        <el-button
          type="primary"
          plain
          @click="openWorkspace()"
          v-hasPermi="['lunaerp:finance:supplierrecon:create']"
        >
          <Icon icon="ep:plus" class="mr-2px" /> 生成对账单
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <ContentWrap>
    <el-table v-loading="loading" :data="list" border stripe>
      <el-table-column label="对账单号" prop="reconNo" min-width="150" show-overflow-tooltip>
        <template #default="{ row }">
          <el-link type="primary" :underline="false" @click="openDetail(row.id)">
            <span class="font-mono font-bold">{{ row.reconNo }}</span>
          </el-link>
        </template>
      </el-table-column>

      <el-table-column label="供应商" prop="supplierName" min-width="160" show-overflow-tooltip>
        <template #default="{ row }">
          <span class="font-medium">{{ row.supplierName || '—' }}</span>
        </template>
      </el-table-column>

      <el-table-column label="对账区间" min-width="190" align="center">
        <template #default="{ row }">
          <span class="text-12px">{{ row.periodStart || '—' }} 至 {{ row.periodEnd || '—' }}</span>
        </template>
      </el-table-column>

      <el-table-column label="确认金额" prop="totalAmount" width="130" align="right">
        <template #default="{ row }">
          <span class="font-mono font-bold text-gray-900">
            ¥ {{ formatMoney(row.totalAmount) }}
          </span>
        </template>
      </el-table-column>

      <el-table-column label="已申请金额" prop="appliedAmount" width="130" align="right">
        <template #default="{ row }">
          <span class="font-mono font-bold text-blue-700">
            ¥ {{ formatMoney(row.appliedAmount) }}
          </span>
        </template>
      </el-table-column>

      <el-table-column label="状态" prop="status" width="100" align="center">
        <template #default="{ row }">
          <DictTag :type="DICT_TYPE.LUNAERP_SUPPLIER_RECON_STATUS" :value="row.status" />
        </template>
      </el-table-column>

      <el-table-column label="确认人" prop="confirmedBy" width="110" align="center">
        <template #default="{ row }">
          <span>{{ row.confirmedBy ? `UID:${row.confirmedBy}` : row.confirmedAt ? '已确认' : '—' }}</span>
        </template>
      </el-table-column>

      <el-table-column
        label="创建时间"
        prop="createTime"
        width="160"
        align="center"
        :formatter="dateFormatter"
      />

      <el-table-column label="操作" width="260" align="center" fixed="right">
        <template #default="{ row }">
          <!-- 查看详情抽屉 -->
          <el-button
            link
            type="primary"
            @click="openDetail(row.id)"
            v-hasPermi="['lunaerp:finance:supplierrecon:query']"
          >
            <Icon icon="ep:view" class="mr-2px" /> 查看
          </el-button>

          <!-- 草稿态：编辑、删除（回收站语义） -->
          <template v-if="row.status === 'DRAFT'">
            <el-button
              link
              type="primary"
              @click="openWorkspace(row.id)"
              v-hasPermi="['lunaerp:finance:supplierrecon:update']"
            >
              <Icon icon="ep:edit" class="mr-2px" /> 编辑
            </el-button>
            <el-button
              link
              type="danger"
              @click="handleDelete(row)"
              v-hasPermi="['lunaerp:finance:supplierrecon:delete']"
            >
              <Icon icon="ep:delete" class="mr-2px" /> 删除
            </el-button>
          </template>

          <!-- 已确认态：生成付款申请、打印、作废 -->
          <template v-else-if="row.status === 'CONFIRMED'">
            <el-button
              link
              type="success"
              @click="handleOpenPaymentApply(row)"
              v-hasPermi="['lunaerp:finance:supplierrecon:generate']"
            >
              <Icon icon="ep:money" class="mr-2px" /> 生成付款申请
            </el-button>

            <PrintDispatcher biz-type="VENDOR_RECON" :biz-id="row.id" class="inline-block">
              <template #default="{ open }">
                <el-button
                  link
                  type="primary"
                  @click="open()"
                  v-hasPermi="['lunaerp:finance:supplierrecon:print']"
                >
                  <Icon icon="ep:printer" class="mr-2px" /> 打印
                </el-button>
              </template>
            </PrintDispatcher>

            <el-button
              link
              type="warning"
              @click="handleCancel(row)"
              v-hasPermi="['lunaerp:finance:supplierrecon:confirm']"
            >
              <Icon icon="ep:circle-close" class="mr-2px" /> 作废
            </el-button>
          </template>
        </template>
      </el-table-column>

      <template #empty>
        <el-empty description="暂无供应商对账单数据" :image-size="90" />
      </template>
    </el-table>

    <!-- 分页 -->
    <Pagination
      :total="total"
      v-model:page="queryParams.pageNo"
      v-model:limit="queryParams.pageSize"
      @pagination="getList"
    />

    <!-- 详情抽屉 -->
    <SupplierReconDetailDrawer ref="detailDrawerRef" @refresh="getList" />

    <!-- 付款申请弹窗 -->
    <SupplierReconPaymentApplyDialog ref="paymentApplyDialogRef" @success="getList" />
  </ContentWrap>
</template>

<script lang="ts" setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { DICT_TYPE, getStrDictOptions } from '@/utils/dict'
import { dateFormatter } from '@/utils/formatTime'
import type {
  SupplierReconRespVO,
  SupplierReconPageReqVO
} from '@/api/lunaerp-elmui/finance/supplier-recon'
import {
  getSupplierReconPage,
  deleteSupplierRecon,
  cancelSupplierRecon
} from '@/api/lunaerp-elmui/finance/supplier-recon'
import * as SupplierApi from '@/api/lunaerp-elmui/baseinfo/supplier'
import type { SupplierVO } from '@/api/lunaerp-elmui/baseinfo/supplier'
import { formatMoney, parseReconErrorMessage } from './model/supplier-recon'
import PrintDispatcher from '@/components/Print/src/PrintDispatcher.vue'
import SupplierReconDetailDrawer from './components/SupplierReconDetailDrawer.vue'
import SupplierReconPaymentApplyDialog from './components/SupplierReconPaymentApplyDialog.vue'

defineOptions({ name: 'LunaerpFinanceSupplierRecon' })

const router = useRouter()

const loading = ref(false)
const total = ref(0)
const list = ref<SupplierReconRespVO[]>([])
const queryFormRef = ref()
const detailDrawerRef = ref<InstanceType<typeof SupplierReconDetailDrawer>>()
const paymentApplyDialogRef = ref<InstanceType<typeof SupplierReconPaymentApplyDialog>>()

const supplierLoading = ref(false)
const supplierOptions = ref<SupplierVO[]>([])

interface QueryParamsState extends SupplierReconPageReqVO {
  dateRange?: [string, string]
}

const queryParams = reactive<QueryParamsState>({
  pageNo: 1,
  pageSize: 10,
  keyword: '',
  supplierId: undefined,
  status: '',
  dateRange: undefined
})

onMounted(async () => {
  await getList()
})

const getList = async () => {
  loading.value = true
  try {
    const params: SupplierReconPageReqVO = {
      pageNo: queryParams.pageNo,
      pageSize: queryParams.pageSize,
      keyword: queryParams.keyword || undefined,
      supplierId: queryParams.supplierId,
      status: queryParams.status || undefined,
      periodStart: queryParams.dateRange?.[0],
      periodEnd: queryParams.dateRange?.[1]
    }
    const res = await getSupplierReconPage(params)
    list.value = res?.list || []
    total.value = res?.total || 0
  } catch (err: any) {
    ElMessage.error(err?.msg || err?.message || '获取供应商对账单列表失败')
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
  queryParams.dateRange = undefined
  handleQuery()
}

// 供应商远程检索
const searchSuppliers = async (query: string) => {
  supplierLoading.value = true
  try {
    const res = await SupplierApi.getSupplierPage({
      pageNo: 1,
      pageSize: 50,
      search: query || ''
    })
    const list = res?.list || []
    // 远程搜索多选/单选下拉选项防脱落守卫 (docs/agents/coding-standards.md §6)
    if (queryParams.supplierId) {
      const existing = supplierOptions.value.find((s) => s.id === queryParams.supplierId)
      if (existing && !list.some((s: SupplierVO) => s.id === queryParams.supplierId)) {
        list.unshift(existing)
      }
    }
    supplierOptions.value = list
  } finally {
    supplierLoading.value = false
  }
}

// 打开工作台
const openWorkspace = (id?: number) => {
  if (id) {
    router.push(`/finance/supplier-recon/workspace/${id}`)
  } else {
    router.push('/finance/supplier-recon/workspace')
  }
}

// 打开详情抽屉
const openDetail = (id: number) => {
  detailDrawerRef.value?.open(id)
}

// 删除草稿（回收站交互语义）
const handleDelete = async (row: SupplierReconRespVO) => {
  try {
    await ElMessageBox.confirm(
      `是否确认将对账单【${row.reconNo}】移入回收站？移入后可在回收站查看或恢复。`,
      '移入回收站',
      {
        confirmButtonText: '确认移入',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    loading.value = true
    await deleteSupplierRecon(row.id)
    ElMessage.success('对账单已成功移入回收站')
    await getList()
  } catch (err: any) {
    if (err === 'cancel') return
    const parsed = parseReconErrorMessage(err)
    ElMessage.error(parsed.message)
  } finally {
    loading.value = false
  }
}

// 作废已确认对账单
const handleCancel = async (row: SupplierReconRespVO) => {
  try {
    await ElMessageBox.confirm(
      `作废后对账单【${row.reconNo}】将置为终态，其引用的源单明细将释放回候选池。是否确认作废？`,
      '作废对账单',
      {
        confirmButtonText: '确定作废',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    loading.value = true
    await cancelSupplierRecon(row.id)
    ElMessage.success('对账单已成功作废')
    await getList()
  } catch (err: any) {
    if (err === 'cancel') return
    const parsed = parseReconErrorMessage(err)
    ElMessage.error(parsed.message)
  } finally {
    loading.value = false
  }
}

// 打开生成付款申请弹窗
const handleOpenPaymentApply = (row: SupplierReconRespVO) => {
  paymentApplyDialogRef.value?.open(row)
}
</script>
