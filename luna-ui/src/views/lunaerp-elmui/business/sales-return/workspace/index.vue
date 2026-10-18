<template>
  <div class="sales-return-workspace">
    <!-- 顶部操作栏 -->
    <ContentWrap>
      <div class="flex items-center justify-between">
        <div class="flex items-center gap-10px">
          <el-button @click="handleBack">
            <Icon icon="ep:arrow-left" class="mr-2px" /> 返回列表
          </el-button>
          <span class="text-16px font-bold">
            {{ isNew ? '新建销售退货单' : '销售退货单详情 / 工作台' }}
          </span>
          <span v-if="formData.returnNo" class="text-14px text-gray-500 font-mono">
            【{{ formData.returnNo }}】
          </span>
          <dict-tag
            v-if="formData.status"
            :type="DICT_TYPE.LUNAERP_SALES_RETURN_STATUS"
            :value="formData.status"
          />
          <dict-tag
            v-if="formData.approvalStatus"
            :type="DICT_TYPE.LUNAERP_APPROVAL_STATUS"
            :value="formData.approvalStatus"
          />
        </div>
        <div class="flex items-center gap-10px">
          <el-button
            v-if="isNew"
            type="primary"
            plain
            @click="openShipmentSelect"
            v-hasPermi="['lunaerp:business:sales-return:create']"
          >
            <Icon icon="ep:search" class="mr-2px" />
            {{ formData.shipmentId ? '重新选择发货单' : '选择发货单' }}
          </el-button>
          <el-button
            v-if="isEditable"
            type="primary"
            :loading="saving"
            @click="handleSave"
            v-hasPermi="[
              isNew
                ? 'lunaerp:business:sales-return:create'
                : 'lunaerp:business:sales-return:update'
            ]"
          >
            <Icon icon="ep:check" class="mr-2px" /> 保存草稿
          </el-button>
          <el-button
            v-if="!isNew && actionFlags.canSubmit"
            type="success"
            :loading="submitting"
            @click="handleSubmit"
            v-hasPermi="['lunaerp:business:sales-return:submit']"
          >
            <Icon icon="ep:promotion" class="mr-2px" /> 提交审批
          </el-button>
          <el-button
            v-if="!isNew && actionFlags.canWithdraw"
            type="warning"
            :loading="withdrawing"
            @click="handleWithdraw"
            v-hasPermi="['lunaerp:business:sales-return:submit']"
          >
            <Icon icon="ep:back" class="mr-2px" /> 撤回审批
          </el-button>
          <el-button
            v-if="!isNew && actionFlags.canCancel"
            type="danger"
            :loading="cancelling"
            @click="handleCancel"
            v-hasPermi="['lunaerp:business:sales-return:cancel']"
          >
            <Icon icon="ep:circle-close" class="mr-2px" /> 作废
          </el-button>
        </div>
      </div>
    </ContentWrap>

    <!-- 发货单未选择引导 -->
    <ContentWrap v-if="isNew && !formData.shipmentId" class="text-center py-40px">
      <el-empty description="请先选择一张已发货（COMPLETED）的发货单发起销售退货">
        <el-button
          type="primary"
          @click="openShipmentSelect"
          v-hasPermi="['lunaerp:business:sales-return:create']"
        >
          <Icon icon="ep:search" class="mr-2px" /> 选择发货单
        </el-button>
      </el-empty>
    </ContentWrap>

    <template v-else>
      <!-- 头信息卡片 -->
      <ContentWrap>
        <div
          class="font-bold text-15px mb-15px pb-8px border-b border-gray-100 flex items-center gap-6px"
        >
          <Icon icon="ep:document" /> 单据头快照与基本信息
        </div>
        <el-form :model="formData" label-width="100px" :disabled="!isEditable">
          <el-row :gutter="20">
            <el-col :span="6">
              <el-form-item label="发货单号">
                <el-input v-model="formData.shipmentNo" disabled />
              </el-form-item>
            </el-col>
            <el-col :span="6">
              <el-form-item label="客户名称">
                <el-input v-model="formData.customerName" disabled />
              </el-form-item>
            </el-col>
            <el-col :span="6">
              <el-form-item label="销售合同号">
                <el-input :model-value="formData.contractNo || '（无合同）'" disabled />
              </el-form-item>
            </el-col>
            <el-col :span="6">
              <el-form-item label="币种">
                <el-input v-model="formData.currency" disabled />
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="20">
            <el-col :span="6">
              <el-form-item label="退货仓库" required>
                <el-select
                  v-model="formData.warehouseId"
                  placeholder="选择退货仓库"
                  class="w-full"
                  :disabled="!isEditable"
                >
                  <el-option
                    v-for="w in warehouseOptions"
                    :key="w.id"
                    :label="w.name"
                    :value="w.id!"
                  />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="6">
              <el-form-item label="单据日期" required>
                <el-date-picker
                  v-model="formData.returnDate"
                  type="date"
                  value-format="YYYY-MM-DD"
                  placeholder="选择单据日期"
                  class="!w-full"
                  :disabled="!isEditable"
                />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="总备注">
                <el-input
                  v-model="formData.remark"
                  placeholder="请输入整单退货原因或总备注"
                  :disabled="!isEditable"
                />
              </el-form-item>
            </el-col>
          </el-row>
          <el-row v-if="formData.stockInNo || formData.stockInId" :gutter="20">
            <el-col :span="6">
              <el-form-item label="退货入库单">
                <el-link
                  v-if="formData.stockInId"
                  type="primary"
                  :underline="false"
                  class="font-mono font-bold"
                  @click="handleJumpStockIn"
                >
                  <Icon icon="ep:link" class="mr-1" />
                  {{ formData.stockInNo || ('ID: ' + formData.stockInId) }}
                </el-link>
                <el-tag v-else type="info" class="font-mono font-bold">{{ formData.stockInNo }}</el-tag>
              </el-form-item>
            </el-col>
            <el-col :span="6">
              <el-form-item label="入库单状态">
                <el-tag :type="formData.stockInStatus === 'COMPLETED' ? 'success' : (formData.stockInStatus === 'CANCELLED' ? 'danger' : 'warning')">
                  {{ formData.stockInStatus || 'DRAFT' }}
                </el-tag>
              </el-form-item>
            </el-col>
          </el-row>
        </el-form>
      </ContentWrap>

      <!-- 明细行与卷清单卡片 -->
      <ContentWrap>
        <div
          class="font-bold text-15px mb-15px pb-8px border-b border-gray-100 flex items-center justify-between"
        >
          <div class="flex items-center gap-6px">
            <Icon icon="ep:list" /> 退货明细行与逐卷登记
          </div>
          <div class="text-13px text-gray-500 font-normal">
            共 <span class="font-bold text-primary">{{ items.length }}</span> 行， 总退货量
            <span class="font-bold text-primary">{{ summaryQty.toFixed(2) }}</span
            >， 总金额
            <span class="font-bold text-primary"
              >{{ summaryAmount.toFixed(2) }} {{ formData.currency }}</span
            >
          </div>
        </div>

        <el-table :data="items" border stripe row-key="shipmentItemId" default-expand-all>
          <!-- 展开列：逐卷登记区 -->
          <el-table-column type="expand" width="50">
            <template #default="{ row }">
              <div class="p-15px bg-gray-50 rounded">
                <!-- 软齐套差异黄色警示条 -->
                <el-alert
                  v-if="isQtyRollMismatch(row.returnQty, row.unit, row.rolls)"
                  type="warning"
                  show-icon
                  :closable="false"
                  class="!mb-10px"
                  :title="`【软齐套提示】行退货数量 (${row.returnQty || 0} ${row.unit}) 与卷数量合计 (${getLineRollQty(row.unit, row.rolls)} ${row.unit}) 存在差异（不拦截保存草稿）`"
                />

                <div class="flex items-center justify-between mb-8px">
                  <div class="text-13px font-bold text-gray-700 flex items-center gap-6px">
                    <Icon icon="ep:tickets" /> 卷明细清单（至少一卷，卷号必填，重量/米数至少填一项）
                  </div>
                  <div v-if="isEditable" class="flex gap-8px">
                    <el-button
                      size="small"
                      type="primary"
                      plain
                      @click="handleImportOriginalRolls(row)"
                      v-hasPermi="[
                        isNew
                          ? 'lunaerp:business:sales-return:create'
                          : 'lunaerp:business:sales-return:update'
                      ]"
                    >
                      <Icon icon="ep:download" class="mr-2px" /> 导入原发货卷
                    </el-button>
                    <el-button
                      size="small"
                      type="success"
                      plain
                      @click="handleAddManualRoll(row)"
                      v-hasPermi="[
                        isNew
                          ? 'lunaerp:business:sales-return:create'
                          : 'lunaerp:business:sales-return:update'
                      ]"
                    >
                      <Icon icon="ep:plus" class="mr-2px" /> 手填散退卷
                    </el-button>
                  </div>
                </div>

                <el-table :data="row.rolls" border size="small" class="bg-white">
                  <el-table-column label="序号" type="index" width="55" align="center" />
                  <el-table-column label="来源原发货卷" width="130" align="center">
                    <template #default="{ row: roll }">
                      <el-tag v-if="roll.shipmentRollId" size="small" type="info">原发货卷</el-tag>
                      <el-tag v-else size="small" type="warning">散退手填</el-tag>
                    </template>
                  </el-table-column>
                  <el-table-column label="卷号" min-width="140">
                    <template #default="{ row: roll }">
                      <el-input
                        v-model="roll.rollNo"
                        placeholder="卷号必填"
                        size="small"
                        :disabled="!isEditable"
                      />
                    </template>
                  </el-table-column>
                  <el-table-column label="实称重量 (KG)" width="150" align="right">
                    <template #default="{ row: roll }">
                      <el-input-number
                        v-model="roll.weight"
                        :min="0"
                        :precision="2"
                        :controls="false"
                        size="small"
                        class="!w-full"
                        :disabled="!isEditable"
                        placeholder="重量 KG"
                      />
                    </template>
                  </el-table-column>
                  <el-table-column label="米数 (M)" width="150" align="right">
                    <template #default="{ row: roll }">
                      <el-input-number
                        v-model="roll.lengthM"
                        :min="0"
                        :precision="2"
                        :controls="false"
                        size="small"
                        class="!w-full"
                        :disabled="!isEditable"
                        placeholder="米数 M"
                      />
                    </template>
                  </el-table-column>
                  <el-table-column label="卷备注" min-width="150">
                    <template #default="{ row: roll }">
                      <el-input
                        v-model="roll.remark"
                        placeholder="备注"
                        size="small"
                        :disabled="!isEditable"
                      />
                    </template>
                  </el-table-column>
                  <el-table-column v-if="isEditable" label="操作" width="70" align="center">
                    <template #default="{ $index }">
                      <el-button
                        type="danger"
                        link
                        size="small"
                        @click="handleRemoveRoll(row, $index)"
                        v-hasPermi="[
                          isNew
                            ? 'lunaerp:business:sales-return:create'
                            : 'lunaerp:business:sales-return:update'
                        ]"
                      >
                        删除
                      </el-button>
                    </template>
                  </el-table-column>
                </el-table>
              </div>
            </template>
          </el-table-column>

          <el-table-column label="品类产品" min-width="160" show-overflow-tooltip>
            <template #default="{ row }">
              <div class="font-bold">{{ row.productName }}</div>
              <div class="text-12px text-gray-400 font-mono">{{ row.productCode }}</div>
            </template>
          </el-table-column>

          <el-table-column label="色号 / 颜色" min-width="130" show-overflow-tooltip>
            <template #default="{ row }">
              <div>{{ row.customerColorNo || '-' }} / {{ row.colorName || '-' }}</div>
              <div class="text-11px text-gray-400">缸号: {{ row.vatNo || '-' }}</div>
            </template>
          </el-table-column>

          <el-table-column label="原发货量 / 单位" width="130" align="right">
            <template #default="{ row }">
              <span class="font-mono">{{ row.originalQty }}</span> {{ row.unit }}
            </template>
          </el-table-column>

          <el-table-column label="已占退货量" width="100" align="right">
            <template #default="{ row }">
              <span class="text-gray-500 font-mono">{{ row.occupiedQty || 0 }}</span>
            </template>
          </el-table-column>

          <el-table-column label="实时可退量" width="110" align="right">
            <template #default="{ row }">
              <span class="font-bold text-success font-mono">{{ row.availableQty }}</span>
            </template>
          </el-table-column>

          <el-table-column label="退货数量" width="150" align="center">
            <template #default="{ row }">
              <el-input-number
                v-model="row.returnQty"
                :min="0.01"
                :max="row.availableQty"
                :precision="2"
                :controls="false"
                size="small"
                class="!w-full"
                :disabled="!isEditable"
                @change="recalculateLineAmount(row)"
              />
            </template>
          </el-table-column>

          <el-table-column label="单价" width="130" align="center">
            <template #default="{ row }">
              <el-input-number
                v-model="row.unitPrice"
                :min="0"
                :precision="4"
                :controls="false"
                size="small"
                class="!w-full"
                :disabled="!isEditable"
                @change="recalculateLineAmount(row)"
              />
            </template>
          </el-table-column>

          <el-table-column label="行金额" width="120" align="right">
            <template #default="{ row }">
              <span class="font-bold text-primary font-mono">
                {{ (row.amount || 0).toFixed(2) }}
              </span>
            </template>
          </el-table-column>

          <el-table-column label="退货原因" width="160" align="center">
            <template #default="{ row }">
              <el-select
                v-model="row.returnReason"
                placeholder="请选择原因"
                size="small"
                class="w-full"
                :disabled="!isEditable"
              >
                <el-option
                  v-for="dict in getStrDictOptions(DICT_TYPE.LUNAERP_SALES_RETURN_REASON)"
                  :key="dict.value"
                  :label="dict.label"
                  :value="dict.value"
                />
              </el-select>
            </template>
          </el-table-column>

          <el-table-column label="明细备注" min-width="120">
            <template #default="{ row }">
              <el-input
                v-model="row.remark"
                placeholder="行备注"
                size="small"
                :disabled="!isEditable"
              />
            </template>
          </el-table-column>

          <el-table-column v-if="isEditable" label="操作" width="70" align="center">
            <template #default="{ $index }">
              <el-button
                type="danger"
                link
                size="small"
                @click="handleRemoveItem($index)"
                v-hasPermi="[
                  isNew
                    ? 'lunaerp:business:sales-return:create'
                    : 'lunaerp:business:sales-return:update'
                ]"
              >
                移除
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </ContentWrap>
    </template>

    <!-- 发货单选择弹窗 -->
    <ShipmentSelectDialog ref="shipmentSelectDialogRef" @select="handleShipmentSelected" />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { DICT_TYPE, getStrDictOptions } from '@/utils/dict'
import { getWarehouseListAll, type WarehouseVO } from '@/api/lunaerp-elmui/stock/warehouse'
import {
  createSalesReturn,
  updateSalesReturn,
  getSalesReturn,
  getShipmentDetail,
  submitSalesReturn,
  withdrawSalesReturn,
  cancelSalesReturn,
  type SalesReturnCandidateVO,
  type SalesReturnShipmentDetailVO,
  type SalesReturnCreateReqVO,
  type SalesReturnUpdateReqVO
} from '@/api/lunaerp-elmui/business/sales-return'
import {
  getLineRollQty,
  isQtyRollMismatch,
  validateSalesReturnLine,
  getSalesReturnActionFlags,
  type SalesReturnRollItem
} from '../model/salesReturn'
import ShipmentSelectDialog from '../components/ShipmentSelectDialog.vue'

interface EditableRollItem extends SalesReturnRollItem {
  _uid?: string
}

interface EditableLineItem {
  id?: number
  shipmentItemId: number
  productId: number
  productCode: string
  productName: string
  customerColorNo?: string
  colorName?: string
  vatNo?: string
  unit: string
  originalQty: number
  originalUnitPrice: number
  occupiedQty: number
  availableQty: number
  returnQty: number
  unitPrice: number
  amount: number
  returnReason: string
  remark?: string
  rolls: EditableRollItem[]
  originalShipmentRolls?: EditableRollItem[]
}

const route = useRoute()
const router = useRouter()

const returnId = ref<number | null>(route.params.id ? Number(route.params.id) : null)
const isNew = computed(() => !returnId.value)
const saving = ref(false)
const submitting = ref(false)
const withdrawing = ref(false)
const cancelling = ref(false)

const shipmentSelectDialogRef = ref<InstanceType<typeof ShipmentSelectDialog> | null>(null)
const warehouseOptions = ref<WarehouseVO[]>([])

const formData = reactive({
  id: undefined as number | undefined,
  returnNo: '',
  status: 'DRAFT',
  approvalStatus: 'PENDING',
  shipmentId: undefined as number | undefined,
  shipmentNo: '',
  customerId: undefined as number | undefined,
  customerName: '',
  salesContractId: undefined as number | undefined,
  contractNo: '',
  warehouseId: undefined as number | undefined,
  warehouseName: '',
  currency: 'RMB',
  returnDate: new Date().toLocaleDateString('en-CA'),
  remark: '',
  stockInId: undefined as number | undefined,
  stockInNo: '',
  stockInStatus: ''
})

const items = ref<EditableLineItem[]>([])

const actionFlags = computed(() =>
  getSalesReturnActionFlags(formData.status, formData.approvalStatus)
)

const isEditable = computed(() => {
  if (isNew.value) return true
  return actionFlags.value.canEdit
})

const summaryQty = computed(() => {
  return items.value.reduce((sum, item) => sum + (Number(item.returnQty) || 0), 0)
})

const summaryAmount = computed(() => {
  return items.value.reduce((sum, item) => sum + (Number(item.amount) || 0), 0)
})

const handleJumpStockIn = () => {
  if (formData.stockInId) {
    router.push(`/stock/stock-in/workspace/${formData.stockInId}`)
  }
}

const recalculateLineAmount = (line: EditableLineItem) => {
  const qty = Number(line.returnQty) || 0
  const price = Number(line.unitPrice) || 0
  line.amount = Number((qty * price).toFixed(2))
}

const handleBack = () => {
  router.push('/business/sales-return')
}

const openShipmentSelect = () => {
  shipmentSelectDialogRef.value?.open(formData.customerId)
}

const handleShipmentSelected = async (candidate: SalesReturnCandidateVO) => {
  try {
    const detail: SalesReturnShipmentDetailVO = await getShipmentDetail(candidate.id)
    formData.shipmentId = detail.shipmentId
    formData.shipmentNo = detail.shipmentNo
    formData.customerId = detail.customerId
    formData.customerName = detail.customerName
    formData.salesContractId = detail.salesContractId
    formData.contractNo = detail.contractNo
    formData.warehouseId = detail.warehouseId
    formData.warehouseName = detail.warehouseName
    formData.currency = detail.currency || 'RMB'

    items.value = (detail.items || [])
      .filter((i) => i.availableQty > 0)
      .map((i) => {
        const origRolls: EditableRollItem[] = (i.rolls || []).map((r) => ({
          shipmentRollId: r.shipmentRollId,
          rollNo: r.rollNo,
          weight: r.weight,
          lengthM: r.lengthM,
          remark: r.remark
        }))

        const lineItem: EditableLineItem = {
          shipmentItemId: i.shipmentItemId,
          productId: i.productId,
          productCode: i.productCode,
          productName: i.productName,
          customerColorNo: i.customerColorNo,
          colorName: i.colorName,
          vatNo: i.vatNo,
          unit: i.unit,
          originalQty: i.quantity,
          originalUnitPrice: i.unitPrice || 0,
          occupiedQty: i.occupiedQty || 0,
          availableQty: i.availableQty,
          returnQty: i.availableQty,
          unitPrice: i.unitPrice || 0,
          amount: Number((i.availableQty * (i.unitPrice || 0)).toFixed(2)),
          returnReason: 'QUALITY',
          remark: '',
          rolls: JSON.parse(JSON.stringify(origRolls)),
          originalShipmentRolls: origRolls
        }

        // 若原发货无卷记录，生成一卷默认手填
        if (lineItem.rolls.length === 0) {
          lineItem.rolls.push({
            rollNo: 'R-01',
            weight: i.unit === 'KG' ? i.availableQty : undefined,
            lengthM: i.unit === 'M' ? i.availableQty : undefined
          })
        }

        return lineItem
      })
  } catch (err: any) {
    ElMessage.error(err.message || '加载发货单详情失败')
  }
}

const handleImportOriginalRolls = (line: EditableLineItem) => {
  if (!line.originalShipmentRolls || line.originalShipmentRolls.length === 0) {
    ElMessage.info('该发货明细无原发货卷清单记录')
    return
  }
  const existingShipmentRollIds = new Set(line.rolls.map((r) => r.shipmentRollId).filter(Boolean))
  let count = 0
  for (const orig of line.originalShipmentRolls) {
    if (!existingShipmentRollIds.has(orig.shipmentRollId)) {
      line.rolls.push({ ...orig })
      count++
    }
  }
  if (count > 0) {
    ElMessage.success(`成功导入 ${count} 卷原发货记录`)
  } else {
    ElMessage.info('原发货卷已全部在卷清单中')
  }
}

const handleAddManualRoll = (line: EditableLineItem) => {
  const nextNo = `ROLL-${String(line.rolls.length + 1).padStart(2, '0')}`
  line.rolls.push({
    rollNo: nextNo,
    weight: line.unit === 'KG' ? 0 : undefined,
    lengthM: line.unit === 'M' ? 0 : undefined,
    remark: ''
  })
}

const handleRemoveRoll = (line: EditableLineItem, index: number) => {
  if (line.rolls.length <= 1) {
    ElMessage.warning('每行必须至少保留一卷登记')
    return
  }
  line.rolls.splice(index, 1)
}

const handleRemoveItem = (index: number) => {
  if (items.value.length <= 1) {
    ElMessage.warning('退货单必须至少包含一行明细')
    return
  }
  items.value.splice(index, 1)
}

const handleSave = async (silent = false): Promise<boolean> => {
  if (!formData.warehouseId) {
    ElMessage.warning('请选择退货仓库')
    return false
  }
  if (items.value.length === 0) {
    ElMessage.warning('退货单明细不能为空')
    return false
  }

  for (let i = 0; i < items.value.length; i++) {
    const item = items.value[i]
    const check = validateSalesReturnLine(item)
    if (!check.valid) {
      ElMessage.error(`第 ${i + 1} 行【${item.productName}】：${check.message}`)
      return false
    }
    if (item.returnQty > item.availableQty) {
      ElMessage.error(
        `第 ${i + 1} 行退货数量 (${item.returnQty}) 超过可退量 (${item.availableQty})`
      )
      return false
    }
    const maxAmount = Number((item.availableQty * item.originalUnitPrice).toFixed(2))
    if (item.amount > maxAmount) {
      ElMessage.error(`第 ${i + 1} 行金额 (${item.amount}) 超过可退额上限 (${maxAmount})`)
      return false
    }
  }

  const itemsPayload = items.value.map((item) => ({
    id: item.id,
    shipmentItemId: item.shipmentItemId,
    returnQty: item.returnQty,
    pieces: item.rolls.length,
    unitPrice: item.unitPrice,
    returnReason: item.returnReason,
    remark: item.remark,
    rolls: item.rolls.map((r) => ({
      id: r.id,
      shipmentRollId: r.shipmentRollId,
      rollNo: r.rollNo,
      weight: r.weight,
      lengthM: r.lengthM,
      remark: r.remark
    }))
  }))

  saving.value = true
  try {
    if (isNew.value) {
      const payload: SalesReturnCreateReqVO = {
        shipmentId: formData.shipmentId!,
        warehouseId: formData.warehouseId,
        returnDate: formData.returnDate,
        remark: formData.remark,
        items: itemsPayload
      }
      const newId = await createSalesReturn(payload)
      if (!silent) {
        ElMessage.success('销售退货草稿创建成功')
      }
      router.push(`/business/sales-return/workspace/${newId}`)
    } else {
      const payload: SalesReturnUpdateReqVO = {
        id: formData.id!,
        warehouseId: formData.warehouseId,
        returnDate: formData.returnDate,
        remark: formData.remark,
        items: itemsPayload
      }
      await updateSalesReturn(payload)
      if (!silent) {
        ElMessage.success('销售退货草稿保存成功')
      }
      await loadSalesReturn(formData.id!)
    }
    return true
  } catch (err: any) {
    ElMessage.error(err.message || '保存失败')
    return false
  } finally {
    saving.value = false
  }
}

const handleSubmit = async () => {
  if (!formData.id) return
  try {
    await ElMessageBox.confirm(`确认提交销售退货单【${formData.returnNo}】审批吗？`, '提交确认', {
      confirmButtonText: '确定提交',
      cancelButtonText: '取消',
      type: 'info'
    })
    submitting.value = true
    // 依 coding-standards.md §6 规约：终态流转前自动前置执行静默保存草稿
    const saved = await handleSave(true)
    if (!saved) {
      return
    }
    await submitSalesReturn(formData.id)
    ElMessage.success('提交审批成功')
    await loadSalesReturn(formData.id)
  } catch (err: any) {
    if (err !== 'cancel') {
      ElMessage.error(err.message || '提交审批失败')
    }
  } finally {
    submitting.value = false
  }
}

const handleWithdraw = async () => {
  if (!formData.id) return
  try {
    await ElMessageBox.confirm(
      `确认撤回销售退货单【${formData.returnNo}】审批吗？撤回后单据恢复为草稿状态并释放占量。`,
      '撤回确认',
      {
        confirmButtonText: '确定撤回',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    withdrawing.value = true
    await withdrawSalesReturn(formData.id)
    ElMessage.success('撤回审批成功')
    await loadSalesReturn(formData.id)
  } catch (err: any) {
    if (err !== 'cancel') {
      ElMessage.error(err.message || '撤回审批失败')
    }
  } finally {
    withdrawing.value = false
  }
}

const handleCancel = async () => {
  if (!formData.id) return
  try {
    const { value } = await ElMessageBox.prompt(
      `请输入作废销售退货单【${formData.returnNo}】的原因：`,
      '作废确认',
      {
        confirmButtonText: '确定作废',
        cancelButtonText: '取消',
        inputPattern: /\S+/,
        inputErrorMessage: '作废原因不能为空'
      }
    )
    cancelling.value = true
    await cancelSalesReturn(formData.id, { cancelReason: value })
    ElMessage.success('作废成功')
    await loadSalesReturn(formData.id)
  } catch (err: any) {
    if (err !== 'cancel') {
      ElMessage.error(err.message || '作废失败')
    }
  } finally {
    cancelling.value = false
  }
}

const loadSalesReturn = async (id: number) => {
  try {
    const res = await getSalesReturn(id)
    formData.id = res.id
    formData.returnNo = res.returnNo || ''
    formData.status = res.status || 'DRAFT'
    formData.approvalStatus = res.approvalStatus || 'PENDING'
    formData.shipmentId = res.shipmentId
    formData.shipmentNo = res.shipmentNo
    formData.customerId = res.customerId
    formData.customerName = res.customerName
    formData.salesContractId = res.salesContractId
    formData.contractNo = res.contractNo || ''
    formData.warehouseId = res.warehouseId
    formData.warehouseName = res.warehouseName || ''
    formData.currency = res.currency || 'RMB'
    formData.returnDate = res.returnDate || new Date().toLocaleDateString('en-CA')
    formData.remark = res.remark || ''
    formData.stockInId = res.stockInId
    formData.stockInNo = res.stockInNo || ''
    formData.stockInStatus = res.stockInStatus || ''

    // 查询对应发货单的实时可退量
    const shipmentDetail = await getShipmentDetail(res.shipmentId)
    const shipmentItemMap = new Map(shipmentDetail.items.map((i) => [i.shipmentItemId, i]))

    items.value = (res.items || []).map((i) => {
      const sItem = shipmentItemMap.get(i.shipmentItemId)
      const origQty = sItem ? sItem.quantity : i.returnQty
      const origPrice = sItem && sItem.unitPrice ? sItem.unitPrice : i.unitPrice || 0
      const available = sItem ? sItem.availableQty : i.returnQty

      return {
        id: i.id,
        shipmentItemId: i.shipmentItemId,
        productId: i.productId,
        productCode: i.productCode,
        productName: i.productName,
        customerColorNo: i.customerColorNo,
        colorName: i.colorName,
        vatNo: i.vatNo,
        unit: i.unit,
        originalQty: origQty,
        originalUnitPrice: origPrice,
        occupiedQty: sItem ? sItem.occupiedQty : 0,
        availableQty: available,
        returnQty: i.returnQty,
        unitPrice: i.unitPrice || origPrice,
        amount: i.amount || Number((i.returnQty * (i.unitPrice || origPrice)).toFixed(2)),
        returnReason: i.returnReason,
        remark: i.remark,
        rolls: (i.rolls || []).map((r) => ({
          id: r.id,
          shipmentRollId: r.shipmentRollId,
          rollNo: r.rollNo,
          weight: r.weight,
          lengthM: r.lengthM,
          remark: r.remark
        })),
        originalShipmentRolls: sItem ? sItem.rolls : []
      }
    })
  } catch (err: any) {
    ElMessage.error(err.message || '加载退货单详情失败')
  }
}

onMounted(async () => {
  const warehouses = await getWarehouseListAll('ACTIVE')
  warehouseOptions.value = warehouses || []

  if (returnId.value) {
    await loadSalesReturn(returnId.value)
  }
})
</script>

<style scoped>
.sales-return-workspace {
  padding-bottom: 30px;
}
</style>
