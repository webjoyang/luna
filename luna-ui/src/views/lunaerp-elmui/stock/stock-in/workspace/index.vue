<template>
  <ContentWrap>
    <div class="workspace-header flex justify-between items-center mb-16px">
      <el-page-header @back="handleBack">
        <template #content>
          <div class="flex items-center gap-3">
            <span class="font-bold text-16px">{{ pageTitle }}</span>
            <span v-if="formData.inNo" class="text-gray-500 font-mono text-14px">
              {{ formData.inNo }}
            </span>
            <DictTag
              v-if="formData.status"
              :type="DICT_TYPE.LUNAERP_STOCK_IN_STATUS"
              :value="formData.status"
            />
          </div>
        </template>
      </el-page-header>

      <!-- 操作栏：按状态显示按钮 -->
      <div class="action-buttons flex items-center gap-2">
        <el-button @click="handleBack">返回</el-button>

        <!-- 查看模式不显示任何写入/修改/状态流转按钮 -->
        <template v-if="!isViewMode">
          <!-- 新建模式：保存草稿 -->
          <template v-if="isCreateMode">
            <el-button
              type="primary"
              :loading="actionLoading"
              @click="handleSave"
              v-hasPermi="['lunaerp:stock:stock-in:create']"
            >
              保存草稿
            </el-button>
          </template>

          <!-- 编辑模式：按状态显示操作按钮 -->
          <template v-else>
            <!-- 草稿态 DRAFT：[删除][作废][保存草稿][审核] -->
            <template v-if="formData.status === 'DRAFT'">
              <el-button
                type="danger"
                plain
                :loading="actionLoading"
                @click="handleDelete"
                v-hasPermi="['lunaerp:stock:stock-in:delete']"
              >
                删除
              </el-button>
              <el-button
                type="warning"
                plain
                :loading="actionLoading"
                @click="handleCancelDraft"
                v-hasPermi="['lunaerp:stock:stock-in:update']"
              >
                作废
              </el-button>
              <el-button
                type="primary"
                plain
                :loading="actionLoading"
                @click="handleSave"
                v-hasPermi="['lunaerp:stock:stock-in:update']"
              >
                保存草稿
              </el-button>
              <el-button
                type="primary"
                :loading="actionLoading"
                @click="handleAudit"
                v-hasPermi="['lunaerp:stock:stock-in:audit']"
              >
                审核
              </el-button>
            </template>

            <!-- 已审核态 COMPLETED：[取消（红冲）] -->
            <template v-else-if="formData.status === 'COMPLETED'">
              <el-button
                type="danger"
                :loading="actionLoading"
                @click="handleCancelReversal"
                v-hasPermi="['lunaerp:stock:stock-in:audit']"
              >
                取消（红冲）
              </el-button>
            </template>

            <!-- 已取消态 CANCELLED：终态全只读，无操作按钮 -->
          </template>
        </template>
      </div>
    </div>

    <!-- 3 Tab 容器（基本信息 / 入库明细 / 操作日志占位） -->
    <el-tabs v-model="activeTab" class="workspace-tabs">
      <el-tab-pane label="基本信息" name="basic">
        <BasicInfoTab
          ref="basicInfoTabRef"
          v-model="formData"
          :is-read-only="isAllReadOnly"
          :is-create="isCreateMode"
          :preview-no="previewNo"
          @warehouse-change="handleWarehouseChange"
        />
      </el-tab-pane>

      <el-tab-pane label="入库明细" name="items">
        <ItemsTab
          v-model:items="formData.items"
          :warehouse-id="formData.warehouseId"
          :warehouse-name="formData.warehouseName"
          :is-read-only="isAllReadOnly"
          @recalculate="recalculateTotalAmount"
        />
      </el-tab-pane>

      <el-tab-pane label="操作日志" name="logs">
        <LogsTab />
      </el-tab-pane>
    </el-tabs>
  </ContentWrap>
</template>

<script lang="ts" setup>
import { DICT_TYPE } from '@/utils/dict'
import BasicInfoTab from './BasicInfoTab.vue'
import ItemsTab from './ItemsTab.vue'
import LogsTab from './LogsTab.vue'
import * as StockInApi from '@/api/lunaerp-elmui/stock/stock-in'
import type { StockInVO } from '@/api/lunaerp-elmui/stock/stock-in'

defineOptions({ name: 'LunaerpElmuiStockStockInWorkspace' })

const route = useRoute()
const router = useRouter()
const message = useMessage()

const activeTab = ref('basic')
const basicInfoTabRef = ref<InstanceType<typeof BasicInfoTab>>()
const actionLoading = ref(false)

const stockInId = computed(() => (route.params.id as string) || null)
const numericStockInId = computed(() => (stockInId.value ? Number(stockInId.value) : null))
const isCreateMode = computed(() => !stockInId.value)
const isViewMode = computed(() => route.query.mode === 'view')

const isAllReadOnly = computed(() => {
  if (isViewMode.value) return true
  if (formData.value.status && formData.value.status !== 'DRAFT') return true
  return false
})

const pageTitle = computed(() => {
  if (isCreateMode.value) return '新建入库单'
  if (isViewMode.value) return '入库单详情'
  return '编辑入库单'
})

const previewNo = ref('')

const defaultToday = () => {
  const d = new Date()
  const year = d.getFullYear()
  const month = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

const formData = ref<StockInVO>({
  inNo: '',
  inType: 'PURCHASE',
  status: 'DRAFT',
  warehouseId: undefined as any,
  warehouseName: '',
  supplierId: undefined,
  supplierName: '',
  salesContractId: undefined,
  contractNo: '',
  greigeOrderId: undefined,
  greigeOrderNo: '',
  dyeingOrderId: undefined,
  greigeTypeId: undefined,
  greigeTypeName: '',
  yarnOrderId: undefined,
  yarnOrderNo: '',
  totalAmount: undefined,
  inDate: defaultToday(),
  remark: '',
  items: []
})

/** 获取单号预览 */
const loadNextNo = async () => {
  try {
    previewNo.value = await StockInApi.getNextInNo(formData.value.inDate)
  } catch {}
}

/** 加载详情 */
const loadDetail = async (id: number) => {
  try {
    const data = await StockInApi.getStockIn(id)
    formData.value = {
      ...data,
      items: data.items || []
    }
  } catch {}
}

/** 重新计算金额合计 */
const recalculateTotalAmount = () => {
  if (!formData.value.items || formData.value.items.length === 0) {
    formData.value.totalAmount = undefined
    return
  }
  let sum = 0
  let hasAmount = false
  for (const item of formData.value.items) {
    if (item.amount != null) {
      sum += Number(item.amount)
      hasAmount = true
    }
  }
  formData.value.totalAmount = hasAmount ? Number(sum.toFixed(2)) : undefined
}

/** 仓库变更处理：清空明细中已分配的库位（库位属于特定仓库） */
const handleWarehouseChange = () => {
  if (formData.value.items && formData.value.items.length > 0) {
    formData.value.items.forEach((item) => {
      item.placements = []
    })
  }
}

/** 返回列表 */
const handleBack = () => {
  router.push('/stock/stock-in')
}

/** 保存草稿 */
const handleSave = async () => {
  // 1. 校验基本信息
  const basicValid = await basicInfoTabRef.value?.validate()
  if (!basicValid) {
    activeTab.value = 'basic'
    message.warning('请完善基本信息必填项')
    return
  }

  // 2. 校验明细行数据完整性（若有明细行）
  if (formData.value.items && formData.value.items.length > 0) {
    for (let i = 0; i < formData.value.items.length; i++) {
      const item = formData.value.items[i]
      if (item.itemType === 'YARN_SPEC') {
        if (!item.yarnSpecId) {
          activeTab.value = 'items'
          message.warning(`第 ${i + 1} 行明细未选择纱线规格`)
          return
        }
      } else {
        if (!item.productId) {
          activeTab.value = 'items'
          message.warning(`第 ${i + 1} 行明细未选择产品`)
          return
        }
      }
      if (!item.quantity || item.quantity <= 0) {
        activeTab.value = 'items'
        message.warning(`第 ${i + 1} 行明细数量必须大于0`)
        return
      }
      if (!item.unit) {
        activeTab.value = 'items'
        message.warning(`第 ${i + 1} 行明细未选择单位`)
        return
      }
    }
  }

  actionLoading.value = true
  try {
    if (isCreateMode.value) {
      const id = await StockInApi.createStockIn(formData.value)
      message.success('创建成功')
      router.push(`/stock/stock-in/workspace/${id}`)
    } else {
      await StockInApi.updateStockIn(formData.value)
      message.success('保存成功')
      if (numericStockInId.value) {
        await loadDetail(numericStockInId.value)
      }
    }
  } finally {
    actionLoading.value = false
  }
}

/** 删除单据 */
const handleDelete = async () => {
  if (!numericStockInId.value) return
  try {
    await message.confirm('确定要删除该入库单草稿吗？删除后明细将一并删除。')
    actionLoading.value = true
    await StockInApi.deleteStockIn(numericStockInId.value)
    message.success('删除成功')
    handleBack()
  } catch {
  } finally {
    actionLoading.value = false
  }
}

/** 审核入库单 */
const handleAudit = async () => {
  if (!numericStockInId.value) return

  // 1. 校验基本信息必填项
  const basicValid = await basicInfoTabRef.value?.validate()
  if (!basicValid) {
    activeTab.value = 'basic'
    message.warning('请完善基本信息必填项')
    return
  }

  // 2. 校验明细非空（零明细不可审核）
  if (!formData.value.items || formData.value.items.length === 0) {
    activeTab.value = 'items'
    message.warning('审核失败：入库单没有明细')
    return
  }

  // 3. 校验明细数据完整性
  for (let i = 0; i < formData.value.items.length; i++) {
    const item = formData.value.items[i]
    if (item.itemType === 'YARN_SPEC') {
      if (!item.yarnSpecId) {
        activeTab.value = 'items'
        message.warning(`第 ${i + 1} 行明细未选择纱线规格`)
        return
      }
    } else {
      if (!item.productId) {
        activeTab.value = 'items'
        message.warning(`第 ${i + 1} 行明细未选择产品`)
        return
      }
    }
    if (!item.quantity || item.quantity <= 0) {
      activeTab.value = 'items'
      message.warning(`第 ${i + 1} 行明细数量必须大于0`)
      return
    }
    if (!item.unit) {
      activeTab.value = 'items'
      message.warning(`第 ${i + 1} 行明细未选择单位`)
      return
    }
  }

  try {
    await message.confirm('确定要审核该入库单吗？审核后将记入正数库存流水并不可修改。')
    actionLoading.value = true
    // 执行审核过账
    await StockInApi.auditStockIn(numericStockInId.value)
    message.success('审核成功')
    await loadDetail(numericStockInId.value)
  } catch {
  } finally {
    actionLoading.value = false
  }
}

/** 草稿作废操作 */
const handleCancelDraft = async () => {
  if (!numericStockInId.value) return
  try {
    await message.confirm('确定要作废该入库单草稿吗？作废后不可恢复。')
    actionLoading.value = true
    await StockInApi.cancelStockIn(numericStockInId.value)
    message.success('作废成功')
    await loadDetail(numericStockInId.value)
  } catch {
  } finally {
    actionLoading.value = false
  }
}

/** 已审核单红冲取消操作 */
const handleCancelReversal = async () => {
  if (!numericStockInId.value) return
  try {
    await message.confirm(
      '确定要取消并红冲该入库单吗？将生成负数库存流水并冲减库存，且不可撤销！',
      '红冲确认'
    )
    actionLoading.value = true
    await StockInApi.cancelStockIn(numericStockInId.value)
    message.success('红冲取消成功')
    await loadDetail(numericStockInId.value)
  } catch {
  } finally {
    actionLoading.value = false
  }
}

onMounted(() => {
  if (numericStockInId.value) {
    loadDetail(numericStockInId.value)
  } else {
    loadNextNo()
  }
})
</script>

<style scoped>
.workspace-tabs {
  margin-top: 10px;
}
</style>
