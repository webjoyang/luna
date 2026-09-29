<template>
  <ContentWrap>
    <!-- 搜索区：关键字 / 交期范围 / 状态（默认排除已取消） / 仅看有预警 -->
    <el-form
      class="-mb-15px"
      :model="queryParams"
      ref="queryFormRef"
      :inline="true"
      label-width="80px"
    >
      <el-form-item label="关键字" prop="keyword">
        <el-input
          v-model="queryParams.keyword"
          placeholder="合同号 / 客户名称"
          clearable
          @keyup.enter="handleQuery"
          class="!w-220px"
        />
      </el-form-item>
      <el-form-item label="交货日期" prop="deliveryDate">
        <el-date-picker
          v-model="queryParams.deliveryDate"
          type="daterange"
          value-format="YYYY-MM-DD"
          range-separator="-"
          start-placeholder="交期起"
          end-placeholder="交期止"
          class="!w-240px"
          @change="handleQuery"
        />
      </el-form-item>
      <el-form-item label="合同状态" prop="status">
        <el-select
          v-model="queryParams.status"
          placeholder="全部（不含已取消）"
          clearable
          class="!w-170px"
          @change="handleQuery"
        >
          <el-option
            v-for="dict in getStrDictOptions(DICT_TYPE.LUNAERP_SALES_CONTRACT_STATUS)"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="含已取消">
        <el-tooltip
          :disabled="!queryParams.status"
          content="已选定具体状态时由状态筛选决定，此开关不生效"
          placement="top"
        >
          <span>
            <el-switch
              v-model="queryParams.includeCancelled"
              :disabled="!!queryParams.status"
              @change="handleQuery"
            />
          </span>
        </el-tooltip>
      </el-form-item>
      <el-form-item label="仅看有预警">
        <el-switch v-model="queryParams.alertOnly" @change="handleQuery" />
      </el-form-item>
      <el-form-item>
        <el-button @click="handleQuery"><Icon icon="ep:search" class="mr-2px" />搜索</el-button>
        <el-button @click="resetQuery"><Icon icon="ep:refresh" class="mr-2px" />重置</el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <ContentWrap>
    <div class="table-tip">
      <Icon icon="ep:info-filled" class="mr-4px" />
      进度由下游执行单据与库存流水实时聚合，只读不可手工维护；默认按交期升序，无交期排最后。
    </div>

    <el-table
      v-loading="loading"
      :data="list"
      border
      stripe
      row-class-name="progress-row"
      @row-click="handleRowClick"
    >
      <el-table-column label="合同号" prop="contractNo" min-width="150" fixed="left">
        <template #default="{ row }">
          <span class="contract-link" @click.stop="openDetail(row.contractId)">
            {{ row.contractNo }}
          </span>
          <el-tooltip
            v-if="row.unit === 'M'"
            content="M 计量合同：需求与未发量不进 KG 合计（不做克重折算）"
            placement="top"
          >
            <el-tag type="info" size="small" class="ml-4px">M</el-tag>
          </el-tooltip>
        </template>
      </el-table-column>
      <el-table-column label="客户" prop="customerName" min-width="160" show-overflow-tooltip>
        <template #default="{ row }">{{ row.customerName || '—' }}</template>
      </el-table-column>
      <el-table-column label="业务员" prop="salespersonName" width="90" align="center">
        <template #default="{ row }">{{ row.salespersonName || '—' }}</template>
      </el-table-column>
      <el-table-column label="交货日期" prop="deliveryDate" width="130" align="center">
        <template #default="{ row }">
          <span :class="deliveryDateClass(row.deliveryDate)">{{
            row.deliveryDate || '未设定'
          }}</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" prop="status" width="90" align="center">
        <template #default="{ row }">
          <DictTag :type="DICT_TYPE.LUNAERP_SALES_CONTRACT_STATUS" :value="row.status" />
        </template>
      </el-table-column>
      <el-table-column label="需求(KG)" prop="demandKg" width="110" align="right">
        <template #default="{ row }">{{ demandKgCell(row, row.demandKg) }}</template>
      </el-table-column>
      <el-table-column label="胚布计划(KG)" prop="greigePlannedKg" width="120" align="right">
        <template #default="{ row }">{{ formatKg(row.greigePlannedKg) }}</template>
      </el-table-column>
      <el-table-column label="投染(KG)" prop="dyeingPlannedKg" width="110" align="right">
        <template #default="{ row }">{{ formatKg(row.dyeingPlannedKg) }}</template>
      </el-table-column>
      <el-table-column label="已发染(KG)" prop="sentToDyeingKg" width="115" align="right">
        <template #default="{ row }">{{ formatKg(row.sentToDyeingKg) }}</template>
      </el-table-column>
      <el-table-column label="排产覆盖率" prop="vatCoverage" width="120" align="center">
        <template #default="{ row }">
          <el-progress
            :percentage="coveragePercent(row.vatCoverage)"
            :stroke-width="10"
            :color="coverageColor(row.vatCoverage)"
          />
        </template>
      </el-table-column>
      <el-table-column label="胚布入库(KG)" prop="greigeInKg" width="125" align="right">
        <template #default="{ row }">{{ formatKg(row.greigeInKg) }}</template>
      </el-table-column>
      <el-table-column label="成品入库(KG)" prop="finishedInKg" width="125" align="right">
        <template #default="{ row }">{{ formatKg(row.finishedInKg) }}</template>
      </el-table-column>
      <el-table-column label="已发货(KG)" prop="shippedKg" width="115" align="right">
        <template #default="{ row }">{{ formatKg(row.shippedKg) }}</template>
      </el-table-column>
      <el-table-column label="未发量(KG)" prop="unshippedKg" width="120" align="right">
        <template #default="{ row }">
          <span :class="{ 'text-warning': !isMeterUnitRow(row) && Number(row.unshippedKg) > 0 }">
            {{ demandKgCell(row, row.unshippedKg) }}
          </span>
        </template>
      </el-table-column>
      <el-table-column label="预警" prop="alertCount" width="180" align="center">
        <template #default="{ row }">
          <template v-if="(row.alerts || []).length > 0">
            <el-tag
              v-for="alert in row.alerts"
              :key="alert"
              :type="alertTagType(alert)"
              size="small"
              effect="dark"
              class="mr-2px"
            >
              {{ alert }}
            </el-tag>
          </template>
          <span v-else class="text-muted">—</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="80" align="center" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click.stop="openDetail(row.contractId)">
            <Icon icon="ep:view" class="mr-2px" />详情
          </el-button>
        </template>
      </el-table-column>
      <template #empty>
        <el-empty description="暂无生产进度数据" :image-size="90" />
      </template>
    </el-table>

    <Pagination
      :total="total"
      v-model:page="queryParams.pageNo"
      v-model:limit="queryParams.pageSize"
      @pagination="getList"
    />
  </ContentWrap>

  <!-- 单合同全链路抽屉 -->
  <ProgressDetailDrawer ref="detailDrawerRef" />
</template>

<script lang="ts" setup>
import { DICT_TYPE, getStrDictOptions } from '@/utils/dict'
import {
  getProductionProgressPage,
  type ProductionProgressPageItemVO,
  type ProductionProgressPageParam
} from '@/api/lunaerp-elmui/production/production-progress'
import { alertTagType, deliveryDateClass, formatKg } from './utils'
import ProgressDetailDrawer from './components/ProgressDetailDrawer.vue'

defineOptions({ name: 'LunaerpElmuiProductionProgress' })

const loading = ref(true)
const total = ref(0)
const list = ref<ProductionProgressPageItemVO[]>([])
const queryFormRef = ref()
const detailDrawerRef = ref<InstanceType<typeof ProgressDetailDrawer>>()

/** 表单状态（交期区间在 getList 拆成 start/end 两个后端参数） */
interface QueryParams {
  pageNo: number
  pageSize: number
  keyword: string
  deliveryDate: string[] | undefined
  status: string
  alertOnly: boolean
  includeCancelled: boolean
}

const queryParams = reactive<QueryParams>({
  pageNo: 1,
  pageSize: 20,
  keyword: '',
  deliveryDate: undefined,
  status: '',
  alertOnly: false,
  // 默认排除已取消合同，可切换查看（ADR-0034 取消单口径）
  includeCancelled: false
})

/** 查询生产进度分页（后端已按交期升序、无交期排最后） */
const getList = async () => {
  loading.value = true
  try {
    const params: ProductionProgressPageParam = {
      pageNo: queryParams.pageNo,
      pageSize: queryParams.pageSize,
      keyword: queryParams.keyword || undefined,
      deliveryDateStart: queryParams.deliveryDate?.[0],
      deliveryDateEnd: queryParams.deliveryDate?.[1],
      status: queryParams.status || undefined,
      alertOnly: queryParams.alertOnly,
      includeCancelled: queryParams.includeCancelled
    }
    const data = await getProductionProgressPage(params)
    list.value = data.list || []
    total.value = data.total || 0
  } finally {
    loading.value = false
  }
}

/** 搜索：回车或点按钮触发，回到第 1 页 */
const handleQuery = () => {
  queryParams.pageNo = 1
  getList()
}

/** 重置：恢复默认筛选（排除已取消、不只看预警） */
const resetQuery = () => {
  queryFormRef.value?.resetFields()
  queryParams.keyword = ''
  queryParams.deliveryDate = undefined
  queryParams.status = ''
  queryParams.alertOnly = false
  queryParams.includeCancelled = false
  handleQuery()
}

/** 点击行打开单合同抽屉 */
const handleRowClick = (row: ProductionProgressPageItemVO) => {
  openDetail(row.contractId)
}

const openDetail = (contractId: number) => {
  detailDrawerRef.value?.open(contractId)
}

const isMeterUnitRow = (row: ProductionProgressPageItemVO) => row.unit === 'M'

/**
 * 需求量与未发量：M 单位合同在 KG 口径下无定义（需求不进 KG 合计、未发量 = 需求 - 已发），
 * 显示占位而非误导性的 0.00 或负数；其余 KG 列是执行单据与 KG 流水的真实聚合，原样展示。
 */
const demandKgCell = (row: ProductionProgressPageItemVO, value?: number) => {
  return isMeterUnitRow(row) ? '—' : formatKg(value)
}

const coveragePercent = (value?: number) => {
  return Math.max(0, Math.min(100, Math.round(Number(value ?? 0) * 10) / 10))
}

const coverageColor = (value?: number) => {
  const coverage = Number(value ?? 0)
  if (coverage >= 100) return '#67c23a'
  return coverage > 0 ? '#409eff' : '#e6a23c'
}

onMounted(() => {
  getList()
})
</script>

<style scoped lang="scss">
.table-tip {
  display: flex;
  align-items: center;
  margin-bottom: 10px;
  font-size: 12px;
  color: #909399;
}

.contract-link {
  font-weight: 500;
  color: #409eff;
  cursor: pointer;

  &:hover {
    text-decoration: underline;
  }
}

.text-muted {
  color: #c0c4cc;
}

.text-danger {
  font-weight: 600;
  color: #f56c6c;
}

.text-warning {
  font-weight: 600;
  color: #e6a23c;
}

:deep(.progress-row) {
  cursor: pointer;
}
</style>
