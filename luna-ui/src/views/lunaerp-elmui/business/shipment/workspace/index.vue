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
          </div>
        </template>
      </el-page-header>

      <!-- 操作栏：按状态显示按钮 -->
      <div class="action-buttons flex items-center gap-2">
        <el-button @click="handleBack">返回</el-button>

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

          <!-- 编辑模式：仅草稿态可保存或删除 -->
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
              type="primary"
              :loading="actionLoading"
              @click="handleSave"
              v-hasPermi="['lunaerp:business:shipment:update']"
            >
              保存修改
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
          :is-create="isCreateMode"
          :preview-no="previewNo"
        />
      </el-tab-pane>

      <el-tab-pane label="发货明细" name="items">
        <ItemsTab
          v-model:items="formData.items"
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

defineOptions({ name: 'LunaerpElmuiBusinessShipmentWorkspace' })

const route = useRoute()
const router = useRouter()
const message = useMessage()

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
    if (isCreateMode.value) {
      const newId = await ShipmentApi.createShipment(formData.value)
      message.success('创建草稿成功')
      router.push(`/business/shipment/workspace/${newId}`)
    } else {
      await ShipmentApi.updateShipment(formData.value)
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

onMounted(async () => {
  if (shipmentId.value) {
    await loadDetail(shipmentId.value)
  } else {
    await loadPreviewNo()
  }
})
</script>
