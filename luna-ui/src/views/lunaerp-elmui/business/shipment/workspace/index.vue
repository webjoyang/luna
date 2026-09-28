<template>
  <ContentWrap>
    <div class="workspace-header flex justify-between items-center mb-16px">
      <el-page-header @back="handleBack">
        <template #content>
          <div class="flex items-center gap-3">
            <span class="font-bold text-16px">{{ pageTitle }}</span>
            <span v-if="formData.shipmentNo" class="text-gray-500 font-mono text-14px">
              {{ formData.shipmentNo }}
            </span>
            <DictTag
              v-if="formData.status"
              :type="DICT_TYPE.LUNAERP_SHIPMENT_STATUS"
              :value="formData.status"
            />
            <DictTag
              v-if="formData.signStatus"
              :type="DICT_TYPE.LUNAERP_SHIPMENT_SIGN_STATUS"
              :value="formData.signStatus"
            />
          </div>
        </template>
      </el-page-header>

      <!-- 操作栏：按状态显示按钮 -->
      <div class="action-buttons flex items-center gap-2">
        <el-button @click="handleBack">返回</el-button>

        <!-- 已发货单 COMPLETED：无论查看还是编辑模式均可登记签收与打印 -->
        <template v-if="formData.status === 'COMPLETED'">
          <el-button
            type="primary"
            :loading="actionLoading"
            @click="handleUpdateSign"
            v-hasPermi="['lunaerp:business:shipment:update']"
          >
            保存签收
          </el-button>
          <el-button
            type="primary"
            plain
            @click="handleOpenPrint('shipment')"
            v-hasPermi="['lunaerp:business:shipment:print']"
          >
            <Icon icon="ep:printer" class="mr-2px" />打印发货单
          </el-button>
          <el-button
            type="info"
            plain
            @click="handleOpenPrint('packing')"
            v-hasPermi="['lunaerp:business:shipment:print']"
          >
            <Icon icon="ep:box" class="mr-2px" />打印装箱清单
          </el-button>
        </template>

        <template v-if="!isViewMode">
          <!-- 新建模式：保存草稿 -->
          <template v-if="isCreateMode">
            <el-button
              type="primary"
              :loading="actionLoading"
              @click="handleSave"
              v-hasPermi="['lunaerp:business:shipment:create']"
            >
              保存草稿
            </el-button>
          </template>

          <!-- 编辑模式：按状态显示操作按钮 -->
          <template v-else-if="formData.status === 'DRAFT'">
            <el-button
              type="danger"
              plain
              :loading="actionLoading"
              @click="handleDelete"
              v-hasPermi="['lunaerp:business:shipment:delete']"
            >
              删除
            </el-button>
            <el-button
              type="warning"
              plain
              :loading="actionLoading"
              @click="handleCancelDraft"
              v-hasPermi="['lunaerp:business:shipment:update']"
            >
              作废
            </el-button>
            <el-button
              type="primary"
              plain
              :loading="actionLoading"
              @click="handleSave"
              v-hasPermi="['lunaerp:business:shipment:update']"
            >
              保存修改
            </el-button>
            <el-button
              type="primary"
              :loading="actionLoading"
              @click="handleAudit"
              v-hasPermi="['lunaerp:business:shipment:audit']"
            >
              审核
            </el-button>
          </template>

          <!-- 已发货态 COMPLETED：[取消（红冲）] -->
          <template v-else-if="formData.status === 'COMPLETED'">
            <el-button
              type="danger"
              plain
              :loading="actionLoading"
              @click="handleCancelReversal"
              v-hasPermi="['lunaerp:business:shipment:audit']"
            >
              取消（红冲）
            </el-button>
          </template>
        </template>
      </div>
    </div>

    <!-- 3 Tab 容器（基本信息 / 发货明细 / 操作日志占位） -->
    <el-tabs v-model="activeTab" class="workspace-tabs">
      <el-tab-pane label="基本信息" name="basic">
        <BasicInfoTab
          ref="basicInfoTabRef"
          v-model="formData"
          :is-read-only="isAllReadOnly"
          :is-view-mode="isViewMode"
          :is-create="isCreateMode"
          :preview-no="previewNo"
        />
      </el-tab-pane>

      <el-tab-pane label="发货明细" name="items">
        <ItemsTab
          v-model:items="formData.items"
          :contract-id="formData.salesContractId"
          :is-read-only="isAllReadOnly"
          @recalculate="handleRecalculateTotals"
        />
      </el-tab-pane>

      <el-tab-pane label="卷清单总览" name="rolls">
        <RollsOverviewTab :items="formData.items" />
      </el-tab-pane>

      <el-tab-pane label="操作日志" name="logs">
        <LogsTab />
      </el-tab-pane>
    </el-tabs>

    <!-- 打印预览与输出弹窗 -->
    <PrintShipmentDialog ref="printShipmentDialogRef" />
  </ContentWrap>
</template>

<script lang="ts" setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useMessage } from '@/hooks/web/useMessage'
import { DICT_TYPE } from '@/utils/dict'
import * as ShipmentApi from '@/api/lunaerp-elmui/business/shipment'
import type { ShipmentVO } from '@/api/lunaerp-elmui/business/shipment'
import BasicInfoTab from './BasicInfoTab.vue'
import ItemsTab from './ItemsTab.vue'
import LogsTab from './LogsTab.vue'
import RollsOverviewTab from './RollsOverviewTab.vue'
import PrintShipmentDialog from '../components/PrintShipmentDialog.vue'

defineOptions({ name: 'LunaerpElmuiBusinessShipmentWorkspace' })

const route = useRoute()
const router = useRouter()
const message = useMessage()
const printShipmentDialogRef = ref()

const handleOpenPrint = (mode: 'shipment' | 'packing' = 'shipment') => {
  printShipmentDialogRef.value?.open(formData.value, mode)
}

const shipmentId = computed(() => (route.params.id ? Number(route.params.id) : undefined))
const isCreateMode = computed(() => !shipmentId.value)
const isViewMode = computed(() => route.query.mode === 'view')
const isAllReadOnly = computed(() => isViewMode.value || (!isCreateMode.value && formData.value.status !== 'DRAFT'))

const activeTab = ref('basic')
const actionLoading = ref(false)
const previewNo = ref('')
const basicInfoTabRef = ref()

const todayStr = new Date().toISOString().split('T')[0]

const formData = ref<ShipmentVO>({
  shipmentNo: '',
  status: 'DRAFT',
  customerId: undefined as any,
  customerName: '',
  salesContractId: undefined,
  contractNo: '',
  warehouseId: undefined as any,
  warehouseName: '',
  shippingAddress: '',
  receiveAddress: '',
  carrier: '',
  trackingNumber: '',
  shippingDate: todayStr,
  expectedDeliveryDate: '',
  freightFee: 0,
  totalAmount: 0,
  totalPieces: 0,
  salespersonUserId: undefined,
  salespersonName: '',
  currency: 'RMB',
  signStatus: 'PENDING',
  signedByName: '',
  signedAt: '',
  signRemark: '',
  remark: '',
  items: []
})

const pageTitle = computed(() => {
  if (isCreateMode.value) return '新建发货单'
  if (isViewMode.value) return '发货单详情'
  return '编辑发货单'
})

const handleBack = () => {
  router.push('/business/shipment')
}

const handleRecalculateTotals = () => {
  const items = formData.value.items || []
  let totalAmount = 0
  let totalPieces = 0
  for (const item of items) {
    if (item.amount != null) {
      totalAmount += item.amount
    }
    if (item.pieces != null) {
      totalPieces += item.pieces
    }
  }
  formData.value.totalAmount = Math.round(totalAmount * 100) / 100
  formData.value.totalPieces = totalPieces
}

const loadDetail = async (id: number) => {
  try {
    const res = await ShipmentApi.getShipment(id)
    formData.value = res
  } catch (e: any) {
    message.error('获取发货单详情失败')
  }
}

const loadPreviewNo = async () => {
  try {
    previewNo.value = await ShipmentApi.getNextShipmentNo(formData.value.shippingDate)
  } catch {}
}

const handleSave = async () => {
  // 1. 校验基本信息表单
  if (basicInfoTabRef.value) {
    await basicInfoTabRef.value.validate()
  }

  // 2. 校验明细必填项
  const items = formData.value.items || []
  for (let i = 0; i < items.length; i++) {
    const item = items[i]
    if (!item.productId) {
      activeTab.value = 'items'
      message.error(`明细第 ${i + 1} 行未选择产品，请补充`)
      return
    }
    if (!item.quantity || item.quantity <= 0) {
      activeTab.value = 'items'
      message.error(`明细第 ${i + 1} 行发货数量必须大于0`)
      return
    }
    if (!item.unit) {
      activeTab.value = 'items'
      message.error(`明细第 ${i + 1} 行未选择单位`)
      return
    }
  }

  actionLoading.value = true
  try {
    handleRecalculateTotals()
    const payload: ShipmentVO = {
      ...formData.value,
      items: formData.value.items?.map((item) => {
        const { _clientKey, ...itemRest } = item as any
        return {
          ...itemRest,
          id: item.id || undefined,
          shipmentId: item.shipmentId || formData.value.id || undefined,
          rolls: item.rolls?.map((roll) => {
            const { _clientKey: rollKey, ...rollRest } = roll as any
            return {
              ...rollRest,
              id: roll.id || undefined,
              shipmentId: roll.shipmentId || formData.value.id || undefined,
              shipmentItemId: roll.shipmentItemId || item.id || undefined
            }
          })
        }
      })
    }
    if (isCreateMode.value) {
      const newId = await ShipmentApi.createShipment(payload)
      message.success('创建草稿成功')
      router.push(`/business/shipment/workspace/${newId}`)
    } else {
      await ShipmentApi.updateShipment(payload)
      message.success('更新草稿成功')
      await loadDetail(shipmentId.value!)
    }
  } finally {
    actionLoading.value = false
  }
}

const handleDelete = async () => {
  if (!shipmentId.value) return
  try {
    await message.confirm('确认删除该发货单草稿吗？删除后明细与卷清单将一并删除。')
    actionLoading.value = true
    await ShipmentApi.deleteShipment(shipmentId.value)
    message.success('删除成功')
    handleBack()
  } finally {
    actionLoading.value = false
  }
}

/** 作废草稿 */
const handleCancelDraft = async () => {
  if (!shipmentId.value) return
  try {
    await message.confirm('确定要作废该发货单草稿吗？作废后不可恢复。', '作废确认')
    actionLoading.value = true
    await ShipmentApi.cancelShipment(shipmentId.value)
    message.success('作废成功')
    await loadDetail(shipmentId.value)
  } catch {
  } finally {
    actionLoading.value = false
  }
}

/** 审核发货单 */
const handleAudit = async () => {
  if (!shipmentId.value) return
  const items = formData.value.items || []
  if (items.length === 0) {
    message.error('审核失败：发货单没有明细')
    return
  }
  try {
    await message.confirm('确定要审核该发货单吗？审核后将校验库存、扣减库存并记入负数出库流水，且不可修改。', '审核确认')
    actionLoading.value = true
    await ShipmentApi.auditShipment(shipmentId.value)
    message.success('审核出库成功')
    await loadDetail(shipmentId.value)
  } catch {
  } finally {
    actionLoading.value = false
  }
}

/** 取消已发货单（红冲） */
const handleCancelReversal = async () => {
  if (!shipmentId.value) return
  try {
    const tip = formData.value.signStatus === 'SIGNED'
      ? '【注意：该单客户已签收！】确定要取消并红冲该发货单吗？将生成正数红冲流水并恢复库存，且不可撤销！'
      : '确定要取消并红冲该发货单吗？将生成正数红冲流水并恢复库存，且不可撤销！'
    await message.confirm(tip, '红冲确认')
    actionLoading.value = true
    await ShipmentApi.cancelShipment(shipmentId.value)
    message.success('红冲取消成功')
    await loadDetail(shipmentId.value)
  } catch {
  } finally {
    actionLoading.value = false
  }
}

/** 登记/修改签收信息 */
const handleUpdateSign = async () => {
  if (!formData.value.id) return
  if (!formData.value.signStatus) {
    message.error('签收状态不能为空')
    return
  }
  try {
    actionLoading.value = true
    await ShipmentApi.updateShipmentSign({
      id: formData.value.id,
      signStatus: formData.value.signStatus,
      signedByName: formData.value.signedByName,
      signedAt: formData.value.signedAt,
      signRemark: formData.value.signRemark
    })
    message.success('签收信息已更新')
    await loadDetail(formData.value.id)
  } catch {
  } finally {
    actionLoading.value = false
  }
}

onMounted(async () => {
  if (shipmentId.value) {
    await loadDetail(shipmentId.value)
  } else {
    await loadPreviewNo()
  }
})
</script>
