<template>
  <el-drawer
    v-model="drawerVisible"
    :title="`发染记录 - ${orderNo}`"
    size="720px"
    :close-on-click-modal="false"
    @closed="handleClosed"
  >
    <div v-loading="loading" class="dispatch-drawer-body">
      <!-- 汇总 -->
      <div class="summary-bar">
        <span>已发染合计：</span>
        <el-tag type="primary">{{ formatQty(totalDispatchedQty) }} KG</el-tag>
        <el-button
          type="primary"
          plain
          size="small"
          class="ml-auto"
          @click="handleOpenCreate"
          v-hasPermi="['lunaerp:greige-dispatch-record:create']"
        >
          <Icon icon="ep:plus" class="mr-4px" />新增
        </el-button>
      </div>

      <!-- 列表 -->
      <el-table :data="recordList" border stripe class="mt-12px">
        <el-table-column label="发染日期" prop="dispatchDate" width="110" align="center" />
        <el-table-column label="仓库" prop="warehouseName" min-width="100" />
        <el-table-column label="数量(KG)" prop="quantity" width="110" align="right">
          <template #default="{ row }">{{ formatQty(row.quantity) }}</template>
        </el-table-column>
        <el-table-column label="匹数" prop="pieces" width="70" align="center" />
        <el-table-column label="染厂" prop="dyeingFactoryName" min-width="120" show-overflow-tooltip>
          <template #default="{ row }">
            <span>{{ row.dyeingFactoryName || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="备注" prop="remark" min-width="120" show-overflow-tooltip />
        <el-table-column label="操作" width="80" align="center" fixed="right">
          <template #default="{ row }">
            <el-button
              link
              type="danger"
              size="small"
              @click="handleDelete(row.id)"
              v-hasPermi="['lunaerp:greige-dispatch-record:delete']"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无发染记录" :image-size="80" />
        </template>
      </el-table>
    </div>

    <DispatchCreateDialog ref="createDialogRef" @success="loadData" />
  </el-drawer>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useMessage } from '@/hooks/web/useMessage'
import * as DispatchApi from '@/api/lunaerp-elmui/production/greige-dispatch-record'
import type { GreigeDispatchRecordVO } from '@/api/lunaerp-elmui/production/greige-dispatch-record'
import DispatchCreateDialog from '@/views/lunaerp-elmui/production/greige-dispatch-record/components/DispatchCreateDialog.vue'

const message = useMessage()

const drawerVisible = ref(false)
const loading = ref(false)
const currentGreigeOrderId = ref<number | null>(null)
const orderNo = ref('')
const recordList = ref<GreigeDispatchRecordVO[]>([])
const createDialogRef = ref<InstanceType<typeof DispatchCreateDialog>>()

const totalDispatchedQty = computed(() => {
  return recordList.value.reduce((sum, r) => sum + (Number(r.quantity) || 0), 0)
})

const formatQty = (value: number) => {
  return Number(value ?? 0).toLocaleString('en-US', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  })
}

/** 打开抽屉 */
const open = (greigeOrderId: number, no: string) => {
  currentGreigeOrderId.value = greigeOrderId
  orderNo.value = no
  drawerVisible.value = true
  loadData()
}

/** 加载列表 */
const loadData = async () => {
  if (!currentGreigeOrderId.value) return
  loading.value = true
  try {
    recordList.value = await DispatchApi.getGreigeDispatchRecordListByGreigeOrder(
      currentGreigeOrderId.value
    )
  } catch {
    recordList.value = []
  } finally {
    loading.value = false
  }
}

/** 新增 */
const handleOpenCreate = () => {
  if (currentGreigeOrderId.value) {
    createDialogRef.value?.openForGreigeOrder(currentGreigeOrderId.value)
  }
}

/** 删除 */
const handleDelete = async (id: number) => {
  try {
    await message.delConfirm('删除后系统将自动红冲恢复库存，确定删除？')
    await DispatchApi.deleteGreigeDispatchRecord(id)
    message.success('删除成功')
    await loadData()
  } catch {
    // 取消
  }
}

/** 关闭清理 */
const handleClosed = () => {
  currentGreigeOrderId.value = null
  orderNo.value = ''
  recordList.value = []
}

defineExpose({ open })
</script>

<style scoped lang="scss">
.dispatch-drawer-body {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.summary-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
}
</style>
