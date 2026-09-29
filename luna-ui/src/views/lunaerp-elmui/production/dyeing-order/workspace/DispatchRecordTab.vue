<template>
  <div class="dispatch-record-tab">
    <div v-loading="loading" class="tab-body">
      <div class="tab-toolbar">
        <span class="tab-summary">
          收胚记录 <el-tag type="primary" size="small">{{ recordList.length }} 条</el-tag>
          <span v-if="totalReceivedQty > 0" class="ml-8px">
            合计 <el-tag type="success" size="small">{{ formatQty(totalReceivedQty) }} KG</el-tag>
          </span>
        </span>
        <el-button
          type="primary"
          plain
          size="small"
          @click="handleOpenCreate"
          v-hasPermi="['lunaerp:greige-dispatch-record:create']"
        >
          <Icon icon="ep:plus" class="mr-4px" />新增
        </el-button>
      </div>

      <el-table :data="recordList" border stripe class="mt-12px">
        <el-table-column label="胚布单号" min-width="130">
          <template #default="{ row }">
            <span class="cell-no">{{ getGreigeOrderNo(row.greigeOrderId) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="产品" prop="productName" min-width="140" show-overflow-tooltip />
        <el-table-column label="发染日期" prop="dispatchDate" width="110" align="center" />
        <el-table-column label="仓库" prop="warehouseName" min-width="100" />
        <el-table-column label="数量(KG)" prop="quantity" width="110" align="right">
          <template #default="{ row }">{{ formatQty(row.quantity) }}</template>
        </el-table-column>
        <el-table-column label="匹数" prop="pieces" width="70" align="center" />
        <el-table-column label="备注" prop="remark" min-width="120" show-overflow-tooltip />
        <template #empty>
          <el-empty description="暂无收胚记录" :image-size="80" />
        </template>
      </el-table>
    </div>

    <DispatchCreateDialog ref="createDialogRef" @success="loadData" />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import * as DispatchApi from '@/api/lunaerp-elmui/production/greige-dispatch-record'
import type { GreigeDispatchRecordVO } from '@/api/lunaerp-elmui/production/greige-dispatch-record'
import * as GreigeOrderApi from '@/api/lunaerp-elmui/production/greige-order'
import type { GreigeOrderVO } from '@/api/lunaerp-elmui/production/greige-order'
import DispatchCreateDialog from '@/views/lunaerp-elmui/production/greige-dispatch-record/components/DispatchCreateDialog.vue'

const props = defineProps<{
  orderId: string | null
  contractNo?: string
}>()

const loading = ref(false)
const recordList = ref<GreigeDispatchRecordVO[]>([])
const greigeOrderCache = ref<Map<number, GreigeOrderVO>>(new Map())
const createDialogRef = ref<InstanceType<typeof DispatchCreateDialog>>()

const totalReceivedQty = computed(() => {
  return recordList.value.reduce((sum, r) => sum + (Number(r.quantity) || 0), 0)
})

const formatQty = (value: number) => {
  return Number(value ?? 0).toLocaleString('en-US', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  })
}

const getGreigeOrderNo = (greigeOrderId?: number) => {
  if (!greigeOrderId) return '-'
  const order = greigeOrderCache.value.get(greigeOrderId)
  return order?.orderNo || String(greigeOrderId)
}

const loadData = async () => {
  if (!props.orderId) {
    recordList.value = []
    return
  }
  loading.value = true
  try {
    recordList.value = await DispatchApi.getGreigeDispatchRecordListByDyeingOrder(
      Number(props.orderId)
    )
    // 缓存胚布单信息用于展示单号
    const ids = [...new Set(recordList.value.map((r) => r.greigeOrderId).filter(Boolean))] as number[]
    for (const id of ids) {
      if (!greigeOrderCache.value.has(id)) {
        try {
          const order = await GreigeOrderApi.getGreigeOrder(id)
          greigeOrderCache.value.set(id, order)
        } catch {
          // ignore
        }
      }
    }
  } catch {
    recordList.value = []
  } finally {
    loading.value = false
  }
}

/** 新增：加载同合同非取消胚布单供选择 */
const handleOpenCreate = async () => {
  if (!props.contractNo) return
  try {
    const res = await GreigeOrderApi.getGreigeOrderPage({
      pageNo: 1,
      pageSize: 100,
      keyword: props.contractNo,
      status: 'ACTIVE'
    })
    // 也加载 COMPLETED 状态的（已完成的胚布单也可以补录）
    const res2 = await GreigeOrderApi.getGreigeOrderPage({
      pageNo: 1,
      pageSize: 100,
      keyword: props.contractNo,
      status: 'COMPLETED'
    })
    const allOrders = [...(res?.list || []), ...(res2?.list || [])]
    // 过滤出合同号完全匹配的（keyword 可能命中其他字段）
    const matched = allOrders.filter((o: GreigeOrderVO) => o.contractNo === props.contractNo)
    createDialogRef.value?.openForDyeingOrder(matched)
  } catch {
    // ignore
  }
}

watch(
  () => [props.orderId],
  () => loadData(),
  { immediate: true }
)

defineExpose({ refresh: loadData })
</script>

<style scoped lang="scss">
.dispatch-record-tab {
  padding: 4px 0;
}

.tab-body {
  min-height: 300px;
}

.tab-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.tab-summary {
  font-size: 14px;
  display: flex;
  align-items: center;
}

.cell-no {
  font-weight: 600;
  color: var(--el-text-color-primary);
}
</style>
