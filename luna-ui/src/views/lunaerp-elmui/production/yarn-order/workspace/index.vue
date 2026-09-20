<template>
  <ContentWrap>
    <!-- 头部导航与操作 -->
    <div class="workspace-header flex justify-between items-center mb-20px">
      <el-page-header @back="handleBack">
        <template #content>
          <div class="flex items-center gap-3">
            <span class="font-bold text-16px">{{ pageTitle }}</span>
            <DictTag
              v-if="formData.status"
              :type="DICT_TYPE.LUNAERP_YARN_ORDER_STATUS"
              :value="formData.status"
            />
          </div>
        </template>
      </el-page-header>
      <div class="action-buttons flex items-center gap-2">
        <el-button @click="handleBack">返回</el-button>

        <!-- 草稿态 / 新建态操作 -->
        <template v-if="isDraftOrNew">
          <el-button
            v-if="orderId"
            type="danger"
            plain
            @click="handleDelete"
            v-hasPermi="['lunaerp:production:yarn-order:delete']"
          >
            删除
          </el-button>
          <el-button
            type="primary"
            plain
            :loading="saving"
            @click="handleSubmit('DRAFT')"
            v-hasPermi="['lunaerp:production:yarn-order:create', 'lunaerp:production:yarn-order:update']"
          >
            保存草稿
          </el-button>
          <el-button
            type="primary"
            :loading="saving"
            @click="handleSubmit('ACTIVE')"
            v-hasPermi="['lunaerp:production:yarn-order:create', 'lunaerp:production:yarn-order:update']"
          >
            下达采购单
          </el-button>
        </template>

        <!-- 进行中态操作 -->
        <template v-else-if="formData.status === 'ACTIVE'">
          <el-button
            type="success"
            plain
            :loading="actionLoading"
            @click="handleComplete"
            v-hasPermi="['lunaerp:production:yarn-order:update']"
          >
            完成采购
          </el-button>
          <el-button
            type="danger"
            plain
            :loading="actionLoading"
            @click="handleCancel"
            v-hasPermi="['lunaerp:production:yarn-order:update']"
          >
            取消采购
          </el-button>
        </template>
      </div>
    </div>

    <!-- 基本信息卡片 -->
    <el-card shadow="never" class="mb-20px">
      <template #header>
        <div class="font-bold">基本信息</div>
      </template>
      <el-form
        ref="formRef"
        :model="formData"
        :rules="rules"
        label-width="100px"
      >
        <el-row :gutter="24">
          <el-col :span="8">
            <el-form-item label="采购单号">
              <el-input
                v-model="formData.orderNo"
                :disabled="isViewOnly"
                placeholder="系统按天自动生成"
              />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="单据日期" prop="orderDate">
              <el-date-picker
                v-model="formData.orderDate"
                type="date"
                value-format="YYYY-MM-DD"
                placeholder="请选择单据日期"
                class="!w-full"
                :disabled="isViewOnly"
              />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="交货日期" prop="deliveryDate">
              <el-date-picker
                v-model="formData.deliveryDate"
                type="date"
                value-format="YYYY-MM-DD"
                placeholder="请选择交货日期"
                class="!w-full"
                :disabled="isViewOnly"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="24">
          <el-col :span="8">
            <el-form-item label="损耗率(%)" prop="lossRate">
              <el-input-number
                v-model="formData.lossRate"
                :min="0"
                :max="100"
                :precision="2"
                :step="0.5"
                class="!w-full"
                placeholder="损耗率"
                :disabled="isViewOnly"
              />
            </el-form-item>
          </el-col>
          <el-col :span="16">
            <el-form-item label="备注" prop="remark">
              <el-input
                v-model="formData.remark"
                type="textarea"
                :rows="2"
                placeholder="请输入备注说明"
                maxlength="500"
                show-word-limit
                :disabled="isViewOnly"
              />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
    </el-card>

    <!-- 关联胚布单与 AI 测算卡片 -->
    <el-card shadow="never" class="mb-20px">
      <template #header>
        <div class="flex justify-between items-center">
          <span class="font-bold">关联胚布指示单</span>
          <div class="flex items-center gap-3">
            <el-select
              v-if="!isViewOnly"
              v-model="selectedGreigeOrderId"
              placeholder="输入单号/产品名称搜索并添加胚布单"
              clearable
              filterable
              remote
              reserve-keyword
              :remote-method="searchGreigeOrders"
              :loading="greigeLoading"
              class="!w-320px"
              @change="handleAddGreigeOrder"
            >
              <el-option
                v-for="item in greigeOptions"
                :key="item.id"
                :label="`${item.orderNo} - ${item.productName || item.productCode || '无产品'} (${item.finishedQty || 0}KG)`"
                :value="item.id!"
              />
            </el-select>
            <el-button
              type="primary"
              :loading="calculating"
              :disabled="selectedGreigeOrders.length === 0 || isViewOnly"
              @click="handleCalculate"
            >
              <Icon icon="ep:magic-stick" class="mr-1" /> AI 测算采购明细
            </el-button>
          </div>
        </div>
      </template>

      <el-table :data="selectedGreigeOrders" border stripe style="width: 100%">
        <el-table-column type="index" label="序号" width="60" align="center" />
        <el-table-column label="胚布单号" prop="orderNo" width="160" />
        <el-table-column label="产品编码" prop="productCode" width="140" />
        <el-table-column label="产品名称" prop="productName" min-width="180" show-overflow-tooltip />
        <el-table-column label="成品量(KG)" prop="finishedQty" width="130" align="right">
          <template #default="{ row }">{{ row.finishedQty != null ? Number(row.finishedQty).toFixed(2) : '0.00' }}</template>
        </el-table-column>
        <el-table-column label="预设损耗" prop="suggestedLoss" width="120" align="right">
          <template #default="{ row }">
            {{ row.suggestedLoss != null ? (Number(row.suggestedLoss) * 100).toFixed(0) + '%' : '0%' }}
          </template>
        </el-table-column>
        <el-table-column v-if="!isViewOnly" label="操作" width="80" align="center" fixed="right">
          <template #default="{ $index }">
            <el-button link type="danger" @click="handleRemoveGreigeOrder($index)">
              移除
            </el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂未选择关联胚布指示单，请在上方搜索添加" :image-size="60" />
        </template>
      </el-table>
    </el-card>

    <!-- 采购明细卡片 -->
    <el-card shadow="never">
      <template #header>
        <div class="flex justify-between items-center">
          <div class="flex items-center gap-3">
            <span class="font-bold">采购明细</span>
            <span class="text-gray-400 text-13px">共 {{ formData.items.length }} 行规格明细</span>
          </div>
        </div>
      </template>

      <!-- 已完成态入库提示 -->
      <el-alert
        v-if="formData.status === 'COMPLETED'"
        :title="`该采购单已完成，已自动写入库存台账入库记录（共 ${distinctSpecCount} 条规格记录）`"
        type="success"
        show-icon
        :closable="false"
        class="mb-15px"
      />

      <el-table :data="formData.items" border stripe style="width: 100%">
        <el-table-column type="index" label="序号" width="55" align="center" />
        <el-table-column label="纱线规格" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="font-medium">{{ row.yarnTypeName ? `[${row.yarnTypeName}] ` : '' }}{{ row.yarnSpecName || '—' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="配比(%)" prop="ratio" width="90" align="center">
          <template #default="{ row }">{{ row.ratio != null ? `${row.ratio}%` : '—' }}</template>
        </el-table-column>
        <el-table-column label="基础需求量(KG)" prop="baseQty" width="130" align="right">
          <template #default="{ row }">{{ row.baseQty != null ? Number(row.baseQty).toFixed(2) : '0.00' }}</template>
        </el-table-column>
        <el-table-column label="含损需求量(KG)" prop="requiredQty" width="130" align="right">
          <template #default="{ row }">{{ row.requiredQty != null ? Number(row.requiredQty).toFixed(2) : '0.00' }}</template>
        </el-table-column>
        <el-table-column label="在库量(KG)" prop="stockQty" width="110" align="right">
          <template #default="{ row }">{{ row.stockQty != null ? Number(row.stockQty).toFixed(2) : '0.00' }}</template>
        </el-table-column>
        <el-table-column label="实采量(KG)" width="150" align="right">
          <template #default="{ row }">
            <el-input-number
              v-if="!isViewOnly"
              v-model="row.purchaseQty"
              :min="0"
              :precision="2"
              :step="10"
              class="!w-full"
              @change="handleRowPriceChange(row)"
            />
            <span v-else>{{ row.purchaseQty != null ? Number(row.purchaseQty).toFixed(2) : '0.00' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="供应商" min-width="200">
          <template #default="{ row }">
            <el-select
              v-if="!isViewOnly"
              v-model="row.supplierId"
              placeholder="选择供应商"
              clearable
              filterable
              remote
              reserve-keyword
              :remote-method="searchSuppliers"
              :loading="supplierLoading"
              class="!w-full"
              @change="(val) => handleSupplierChange(row, val)"
            >
              <el-option
                v-for="s in supplierOptions"
                :key="s.id"
                :label="s.companyName"
                :value="s.id!"
              />
            </el-select>
            <span v-else>{{ row.supplierName || '—' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="单价(元)" width="150" align="right">
          <template #default="{ row }">
            <el-input-number
              v-if="!isViewOnly"
              v-model="row.unitPrice"
              :min="0"
              :precision="4"
              :step="0.5"
              class="!w-full"
              placeholder="单价"
              @change="handleRowPriceChange(row)"
            />
            <span v-else>{{ row.unitPrice != null ? `¥${Number(row.unitPrice).toFixed(4)}` : '—' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="金额(元)" width="130" align="right">
          <template #default="{ row }">
            <span class="font-semibold text-orange-600">
              {{ row.totalAmount != null ? `¥${Number(row.totalAmount).toFixed(2)}` : '¥0.00' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="历史参考价" prop="refUnitPrice" width="120" align="right">
          <template #default="{ row }">
            {{ row.refUnitPrice != null ? `¥${Number(row.refUnitPrice).toFixed(4)}` : '—' }}
          </template>
        </el-table-column>
        <el-table-column label="备注" min-width="140">
          <template #default="{ row }">
            <el-input v-if="!isViewOnly" v-model="row.remark" placeholder="备注" />
            <span v-else>{{ row.remark || '—' }}</span>
          </template>
        </el-table-column>
        <el-table-column v-if="!isViewOnly" label="操作" width="70" align="center" fixed="right">
          <template #default="{ $index }">
            <el-button link type="danger" @click="handleRemoveItem($index)">
              删除
            </el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无采购明细，请先关联胚布单并点击「AI 测算」生成" :image-size="60" />
        </template>
      </el-table>

      <!-- 底部统计汇总行 -->
      <div class="summary-bar flex justify-end items-center gap-8 mt-15px p-12px bg-gray-50 rounded">
        <div>
          <span class="text-gray-500">合计实采量：</span>
          <span class="font-bold text-16px text-primary">{{ totalPurchaseQty.toFixed(2) }} KG</span>
        </div>
        <div>
          <span class="text-gray-500">合计金额：</span>
          <span class="font-bold text-18px text-orange-600">¥{{ totalAmount.toFixed(2) }}</span>
        </div>
      </div>
    </el-card>
  </ContentWrap>
</template>

<script lang="ts" setup>
import { DICT_TYPE } from '@/utils/dict'
import * as YarnOrderApi from '@/api/lunaerp-elmui/production/yarn-order'
import type {
  YarnOrderSaveReqVO,
  YarnOrderItemSaveReqVO,
  YarnOrderGreigeOrderSnapshotVO
} from '@/api/lunaerp-elmui/production/yarn-order'
import * as GreigeOrderApi from '@/api/lunaerp-elmui/production/greige-order'
import type { GreigeOrderVO } from '@/api/lunaerp-elmui/production/greige-order'
import * as SupplierApi from '@/api/lunaerp-elmui/baseinfo/supplier'
import type { SupplierVO } from '@/api/lunaerp-elmui/baseinfo/supplier'

defineOptions({ name: 'LunaerpElmuiProductionYarnOrderWorkspace' })

const router = useRouter()
const route = useRoute()
const message = useMessage()

const orderId = computed(() => {
  const id = route.params.id as string
  return id ? Number(id) : null
})

const isCreateMode = computed(() => !orderId.value)
const isViewMode = computed(() => route.query.mode === 'view')
const isDraftOrNew = computed(() => isCreateMode.value || formData.status === 'DRAFT')
const isViewOnly = computed(() => isViewMode.value || (!isCreateMode.value && formData.status !== 'DRAFT'))

const pageTitle = computed(() => {
  if (isCreateMode.value) return '新建纱线采购单'
  if (isViewOnly.value) return '查看纱线采购单'
  return '编辑纱线采购单'
})

const formRef = ref()
const saving = ref(false)
const calculating = ref(false)
const actionLoading = ref(false)

// 胚布单选择
const selectedGreigeOrderId = ref<number>()
const greigeOptions = ref<GreigeOrderVO[]>([])
const greigeLoading = ref(false)
const selectedGreigeOrders = ref<YarnOrderGreigeOrderSnapshotVO[]>([])

// 供应商选择
const supplierOptions = ref<SupplierVO[]>([])
const supplierLoading = ref(false)

interface EditableYarnOrderItem extends YarnOrderItemSaveReqVO {
  yarnTypeName?: string
  yarnSpecName?: string
}

const formData = reactive<{
  id?: number
  orderNo: string
  sourceInstructions: string
  orderDate: string
  deliveryDate?: string
  lossRate: number
  status: string
  remark?: string
  items: EditableYarnOrderItem[]
}>({
  id: undefined,
  orderNo: '',
  sourceInstructions: '',
  orderDate: new Date().toISOString().split('T')[0],
  deliveryDate: undefined,
  lossRate: 5.0,
  status: 'DRAFT',
  remark: '',
  items: []
})

const rules = {
  orderDate: [{ required: true, message: '单据日期不能为空', trigger: 'change' }]
}

// 汇总统计
const totalPurchaseQty = computed(() => {
  return formData.items.reduce((sum, item) => sum + (Number(item.purchaseQty) || 0), 0)
})

const totalAmount = computed(() => {
  return formData.items.reduce((sum, item) => sum + (Number(item.totalAmount) || 0), 0)
})

const distinctSpecCount = computed(() => {
  return new Set(formData.items.map(item => item.yarnSpecId).filter(Boolean)).size
})

// 单价/实采量改动时前端实时重算该行金额
const handleRowPriceChange = (row: EditableYarnOrderItem) => {
  const price = Number(row.unitPrice) || 0
  const qty = Number(row.purchaseQty) || 0
  row.totalAmount = Math.round(price * qty * 100) / 100
}

// 胚布单远程搜索
const searchGreigeOrders = async (query: string) => {
  if (!query) {
    greigeOptions.value = []
    return
  }
  greigeLoading.value = true
  try {
    const res = await GreigeOrderApi.getGreigeOrderPage({
      keyword: query.trim(),
      pageNo: 1,
      pageSize: 20
    })
    greigeOptions.value = res.list || []
  } finally {
    greigeLoading.value = false
  }
}

// 添加胚布单
const handleAddGreigeOrder = (id?: number) => {
  if (!id) return
  const found = greigeOptions.value.find(g => g.id === id)
  if (found) {
    if (selectedGreigeOrders.value.some(g => g.id === id)) {
      message.warning('该胚布单已添加')
      selectedGreigeOrderId.value = undefined
      return
    }
    selectedGreigeOrders.value.push({
      id: found.id!,
      orderNo: found.orderNo || '',
      productId: found.productId || 0,
      productCode: found.productCode,
      productName: found.productName,
      finishedQty: found.finishedQty,
      suggestedLoss: found.suggestedLoss
    })
    // 若损耗率为默认值，可采用胚布单预设损耗更新
    if (found.suggestedLoss && Number(found.suggestedLoss) > 0) {
      const suggestedPercent = Math.round(Number(found.suggestedLoss) * 10000) / 100
      if (suggestedPercent > formData.lossRate) {
        formData.lossRate = suggestedPercent
      }
    }
  }
  selectedGreigeOrderId.value = undefined
}

// 移除胚布单
const handleRemoveGreigeOrder = (index: number) => {
  selectedGreigeOrders.value.splice(index, 1)
}

// 供应商远程搜索
const searchSuppliers = async (query: string) => {
  supplierLoading.value = true
  try {
    const res = await SupplierApi.getSupplierPage({
      search: query ? query.trim() : '',
      pageNo: 1,
      pageSize: 20,
      status: 'ACTIVE'
    })
    supplierOptions.value = res.list || []
  } finally {
    supplierLoading.value = false
  }
}

// 供应商选择
const handleSupplierChange = (row: EditableYarnOrderItem, supplierId?: number) => {
  if (!supplierId) {
    row.supplierId = undefined
    row.supplierName = ''
    return
  }
  const found = supplierOptions.value.find(s => s.id === supplierId)
  if (found) {
    row.supplierName = found.companyName
  }
}

// 移除明细行
const handleRemoveItem = (index: number) => {
  formData.items.splice(index, 1)
}

// 核心测算
const handleCalculate = async () => {
  if (selectedGreigeOrders.value.length === 0) {
    message.warning('请先添加关联胚布指示单')
    return
  }
  calculating.value = true
  try {
    const greigeOrderIds = selectedGreigeOrders.value.map(g => g.id)
    const res = await YarnOrderApi.calculateYarnOrder({
      greigeOrderIds,
      lossRate: formData.lossRate
    })
    // 覆盖明细行
    formData.items = (res.items || []).map(item => {
      const unitPrice = item.refUnitPrice != null ? Number(item.refUnitPrice) : undefined
      const purchaseQty = item.purchaseQty != null ? Number(item.purchaseQty) : 0
      const totalAmount = unitPrice != null ? Math.round(unitPrice * purchaseQty * 100) / 100 : 0
      return {
        yarnTypeId: item.yarnTypeId,
        yarnTypeName: item.yarnTypeName,
        yarnSpecId: item.yarnSpecId,
        yarnSpecName: item.yarnSpecName,
        supplierId: item.supplierId,
        supplierName: item.supplierName,
        ratio: item.ratio,
        lossRate: item.lossRate,
        baseQty: item.baseQty,
        requiredQty: item.requiredQty,
        stockQty: item.stockQty,
        purchaseQty: purchaseQty,
        unitPrice: unitPrice,
        totalAmount: totalAmount,
        refUnitPrice: item.refUnitPrice,
        remark: ''
      }
    })
    message.success('AI 测算完成，已生成采购明细')
  } finally {
    calculating.value = false
  }
}

// 提交保存 / 下达
const handleSubmit = async (targetStatus: 'DRAFT' | 'ACTIVE') => {
  await formRef.value.validate()

  const greigeIds = selectedGreigeOrders.value.map(g => g.id)
  const sourceInstructions = greigeIds.length > 0 ? JSON.stringify(greigeIds) : ''

  const payload: YarnOrderSaveReqVO = {
    id: formData.id,
    orderNo: formData.orderNo || undefined,
    sourceInstructions,
    orderDate: formData.orderDate,
    deliveryDate: formData.deliveryDate,
    lossRate: formData.lossRate,
    status: targetStatus,
    remark: formData.remark,
    items: formData.items.map(item => ({
      id: item.id,
      yarnTypeId: item.yarnTypeId,
      yarnSpecId: item.yarnSpecId,
      supplierId: item.supplierId,
      supplierName: item.supplierName,
      ratio: item.ratio,
      lossRate: item.lossRate,
      baseQty: item.baseQty,
      requiredQty: item.requiredQty,
      stockQty: item.stockQty,
      purchaseQty: item.purchaseQty,
      unitPrice: item.unitPrice,
      totalAmount: item.totalAmount,
      refUnitPrice: item.refUnitPrice,
      remark: item.remark
    }))
  }

  saving.value = true
  try {
    if (orderId.value) {
      await YarnOrderApi.updateYarnOrder(payload)
      if (targetStatus === 'ACTIVE') {
        await YarnOrderApi.effectYarnOrder(orderId.value)
      }
      message.success(targetStatus === 'ACTIVE' ? '下达成功' : '保存草稿成功')
    } else {
      const newId = await YarnOrderApi.createYarnOrder(payload)
      message.success(targetStatus === 'ACTIVE' ? '下达成功' : '保存草稿成功')
      router.replace(`/production/yarn-order/workspace/${newId}`)
      return
    }
    handleBack()
  } finally {
    saving.value = false
  }
}

// 完成采购
const handleComplete = async () => {
  if (!orderId.value) return
  try {
    await message.confirm('确认完成该采购单吗？完成后将自动写入纱线库存台账。')
    actionLoading.value = true
    await YarnOrderApi.completeYarnOrder(orderId.value)
    message.success('采购单已完成，已自动写入库存台账')
    await loadDetail(orderId.value)
  } catch {
  } finally {
    actionLoading.value = false
  }
}

// 取消采购
const handleCancel = async () => {
  if (!orderId.value) return
  try {
    await message.confirm('确认取消该采购单吗？')
    actionLoading.value = true
    await YarnOrderApi.cancelYarnOrder(orderId.value)
    message.success('采购单已取消')
    await loadDetail(orderId.value)
  } catch {
  } finally {
    actionLoading.value = false
  }
}

// 删除采购单
const handleDelete = async () => {
  if (!orderId.value) return
  try {
    await message.delConfirm()
    await YarnOrderApi.deleteYarnOrder(orderId.value)
    message.success('删除成功')
    handleBack()
  } catch {}
}

// 加载详情
const loadDetail = async (id: number) => {
  try {
    const data = await YarnOrderApi.getYarnOrder(id)
    formData.id = data.id
    formData.orderNo = data.orderNo
    formData.sourceInstructions = data.sourceInstructions || ''
    formData.orderDate = data.orderDate
    formData.deliveryDate = data.deliveryDate
    formData.lossRate = data.lossRate != null ? Number(data.lossRate) : 0
    formData.status = data.status || 'DRAFT'
    formData.remark = data.remark

    // 关联胚布单展示快照
    selectedGreigeOrders.value = data.sourceGreigeOrders || []

    // 明细行回显
    formData.items = (data.items || []).map(item => ({
      id: item.id,
      yarnTypeId: item.yarnTypeId,
      yarnTypeName: item.yarnTypeName,
      yarnSpecId: item.yarnSpecId,
      yarnSpecName: item.yarnSpecName,
      supplierId: item.supplierId,
      supplierName: item.supplierName,
      ratio: item.ratio,
      lossRate: item.lossRate,
      baseQty: item.baseQty,
      requiredQty: item.requiredQty,
      stockQty: item.stockQty,
      purchaseQty: item.purchaseQty,
      unitPrice: item.unitPrice,
      totalAmount: item.totalAmount,
      refUnitPrice: item.refUnitPrice,
      remark: item.remark
    }))

    // 回显供应商选项
    const loadedSuppliers: SupplierVO[] = []
    formData.items.forEach(item => {
      if (item.supplierId && item.supplierName) {
        if (!loadedSuppliers.some(s => s.id === item.supplierId)) {
          loadedSuppliers.push({
            id: item.supplierId,
            companyName: item.supplierName
          } as SupplierVO)
        }
      }
    })
    if (loadedSuppliers.length > 0) {
      supplierOptions.value = loadedSuppliers
    }
  } catch {}
}

// 预览单号
const loadNextOrderNo = async () => {
  try {
    const nextNo = await YarnOrderApi.getNextYarnOrderNo()
    if (nextNo && !formData.orderNo) {
      formData.orderNo = nextNo
    }
  } catch {}
}

const handleBack = () => {
  router.push('/production/yarn-order')
}

onMounted(async () => {
  if (orderId.value) {
    await loadDetail(orderId.value)
  } else {
    await loadNextOrderNo()
  }
})
</script>

<style scoped>
.workspace-header {
  border-bottom: 1px solid var(--el-border-color-lighter);
  padding-bottom: 12px;
}
.summary-bar {
  border: 1px solid var(--el-border-color-lighter);
}
</style>
