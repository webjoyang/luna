<template>
  <el-dialog
    v-model="dialogVisible"
    :title="title"
    width="560px"
    :close-on-click-modal="false"
    @closed="handleClosed"
  >
    <el-form
      ref="formRef"
      :model="formData"
      :rules="rules"
      label-width="100px"
      v-loading="loading"
    >
      <!-- 胚布单选择（染色单入口需要选择胚布单） -->
      <el-form-item v-if="showGreigeOrderSelect" label="胚布单" prop="greigeOrderId">
        <el-select
          v-model="formData.greigeOrderId"
          placeholder="请选择胚布单"
          filterable
          class="w-full"
          @change="handleGreigeOrderChange"
        >
          <el-option
            v-for="item in greigeOrderOptions"
            :key="item.id"
            :label="`${item.orderNo} - ${item.productName || ''}`"
            :value="item.id!"
          />
        </el-select>
      </el-form-item>

      <!-- 产品信息（只读展示） -->
      <el-form-item v-if="currentProduct" label="产品">
        <span>{{ currentProduct.productCode }} {{ currentProduct.productName }}</span>
      </el-form-item>

      <!-- 发染日期 -->
      <el-form-item label="发染日期" prop="dispatchDate">
        <el-date-picker
          v-model="formData.dispatchDate"
          type="date"
          placeholder="选择日期"
          value-format="YYYY-MM-DD"
          class="w-full"
        />
      </el-form-item>

      <!-- 出库仓库 -->
      <el-form-item label="出库仓库" prop="warehouseId">
        <el-select
          v-model="formData.warehouseId"
          placeholder="请选择仓库"
          filterable
          class="w-full"
          @change="handleWarehouseChange"
        >
          <el-option
            v-for="wh in warehouseList"
            :key="wh.id"
            :label="wh.name"
            :value="wh.id!"
          />
        </el-select>
      </el-form-item>

      <!-- 可用量提示 -->
      <el-form-item v-if="availableStock !== null" label="可用量">
        <el-tag :type="availableStock <= 0 ? 'danger' : 'success'">
          {{ formatQty(availableStock) }} KG
        </el-tag>
        <span v-if="availableStock <= 0" class="ml-8px text-red-500 text-12px">
          该仓库该产品无可用库存，请先补入库
        </span>
      </el-form-item>

      <!-- 发染数量 -->
      <el-form-item label="发染数量(KG)" prop="quantity">
        <el-input-number
          v-model="formData.quantity"
          :min="0.01"
          :precision="2"
          :step="10"
          placeholder="请输入"
          class="w-full"
        />
      </el-form-item>

      <!-- 匹数 -->
      <el-form-item label="匹数" prop="pieces">
        <el-input-number
          v-model="formData.pieces"
          :min="1"
          :precision="0"
          placeholder="选填"
          class="w-full"
        />
      </el-form-item>

      <!-- 染厂 -->
      <el-form-item label="承染染厂" prop="dyeingFactoryId">
        <el-select
          v-model="formData.dyeingFactoryId"
          placeholder="选填，可后续补填"
          filterable
          clearable
          class="w-full"
          @change="handleFactoryChange"
        >
          <el-option
            v-for="sup in supplierList"
            :key="sup.id"
            :label="sup.companyName"
            :value="sup.id!"
          />
        </el-select>
      </el-form-item>

      <!-- 备注 -->
      <el-form-item label="备注" prop="remark">
        <el-input
          v-model="formData.remark"
          type="textarea"
          :rows="2"
          placeholder="选填"
          maxlength="1000"
          show-word-limit
        />
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="submitting" @click="handleSubmit">
        确定
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, reactive, computed } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import { useMessage } from '@/hooks/web/useMessage'
import * as DispatchApi from '@/api/lunaerp-elmui/production/greige-dispatch-record'
import type { GreigeDispatchRecordCreateVO } from '@/api/lunaerp-elmui/production/greige-dispatch-record'
import * as WarehouseApi from '@/api/lunaerp-elmui/stock/warehouse'
import type { WarehouseVO } from '@/api/lunaerp-elmui/stock/warehouse'
import * as SupplierApi from '@/api/lunaerp-elmui/baseinfo/supplier'
import type { SupplierVO } from '@/api/lunaerp-elmui/baseinfo/supplier'
import * as GreigeOrderApi from '@/api/lunaerp-elmui/production/greige-order'
import type { GreigeOrderVO } from '@/api/lunaerp-elmui/production/greige-order'
import * as InventoryBalanceApi from '@/api/lunaerp-elmui/stock/inventory-balance'
import { formatDate } from '@/utils/formatTime'

const emit = defineEmits<{ success: [] }>()
const message = useMessage()

const dialogVisible = ref(false)
const loading = ref(false)
const submitting = ref(false)
const formRef = ref<FormInstance>()

// 模式：greige = 胚布单入口（greigeOrderId 固定），dyeing = 染色单入口（需选胚布单）
const mode = ref<'greige' | 'dyeing'>('greige')
const fixedGreigeOrderId = ref<number | null>(null)
const fixedDyeingOrderId = ref<number | null>(null)

// 胚布单选项（染色单入口用，只展示非取消的胚布单）
const greigeOrderOptions = ref<GreigeOrderVO[]>([])
const selectedGreigeOrder = ref<GreigeOrderVO | null>(null)

// 仓库列表（仅启用仓）
const warehouseList = ref<WarehouseVO[]>([])
// 供应商列表（染厂选择）
const supplierList = ref<SupplierVO[]>([])
// 可用库存
const availableStock = ref<number | null>(null)

const formData = reactive({
  greigeOrderId: null as number | null,
  dyeingOrderId: undefined as number | undefined,
  warehouseId: null as number | null,
  dispatchDate: formatDate(new Date(), 'YYYY-MM-DD'),
  quantity: undefined as number | undefined,
  pieces: undefined as number | undefined,
  dyeingFactoryId: undefined as number | undefined,
  dyeingFactoryName: undefined as string | undefined,
  remark: ''
})

const rules = reactive<FormRules>({
  greigeOrderId: [{ required: true, message: '请选择胚布单', trigger: 'change' }],
  dispatchDate: [{ required: true, message: '请选择发染日期', trigger: 'change' }],
  warehouseId: [{ required: true, message: '请选择出库仓库', trigger: 'change' }],
  quantity: [{ required: true, message: '请输入发染数量', trigger: 'blur' }]
})

const title = computed(() => '新增发染记录')
const showGreigeOrderSelect = computed(() => mode.value === 'dyeing')

const currentProduct = computed(() => {
  if (mode.value === 'greige' && selectedGreigeOrder.value) {
    return {
      productId: selectedGreigeOrder.value.productId,
      productCode: selectedGreigeOrder.value.productCode,
      productName: selectedGreigeOrder.value.productName
    }
  }
  if (mode.value === 'dyeing' && selectedGreigeOrder.value) {
    return {
      productId: selectedGreigeOrder.value.productId,
      productCode: selectedGreigeOrder.value.productCode,
      productName: selectedGreigeOrder.value.productName
    }
  }
  return null
})

const formatQty = (value: number) => {
  return Number(value ?? 0).toLocaleString('en-US', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  })
}

/** 打开弹窗：胚布单入口 */
const openForGreigeOrder = async (greigeOrderId: number) => {
  mode.value = 'greige'
  fixedGreigeOrderId.value = greigeOrderId
  fixedDyeingOrderId.value = null
  formData.greigeOrderId = greigeOrderId
  formData.dyeingOrderId = undefined
  try {
    // 加载胚布单详情获取产品信息
    const order = await GreigeOrderApi.getGreigeOrder(greigeOrderId)
    selectedGreigeOrder.value = order
    dialogVisible.value = true
    await loadBaseData()
  } catch {
    message.error('加载胚布单信息失败')
  }
}

/** 打开弹窗：染色单入口（需选择胚布单，自动关联 dyeingOrderId） */
const openForDyeingOrder = async (dyeingOrderId: number, greigeOrders: GreigeOrderVO[]) => {
  mode.value = 'dyeing'
  fixedDyeingOrderId.value = dyeingOrderId
  fixedGreigeOrderId.value = null
  formData.greigeOrderId = null
  formData.dyeingOrderId = dyeingOrderId
  selectedGreigeOrder.value = null
  greigeOrderOptions.value = greigeOrders
  dialogVisible.value = true
  await loadBaseData()
}

const loadBaseData = async () => {
  loading.value = true
  try {
    const [whRes, supRes] = await Promise.all([
      WarehouseApi.getWarehouseListAll('ACTIVE'),
      SupplierApi.getSupplierPage({ pageNo: 1, pageSize: 200, status: 'ACTIVE' })
    ])
    warehouseList.value = whRes || []
    supplierList.value = supRes?.list || []
  } finally {
    loading.value = false
  }
}

/** 胚布单变更（染色单入口） */
const handleGreigeOrderChange = async (greigeOrderId: number) => {
  const order = await GreigeOrderApi.getGreigeOrder(greigeOrderId)
  selectedGreigeOrder.value = order
  // 切换胚布单后重置仓库和可用量
  formData.warehouseId = null
  availableStock.value = null
}

/** 仓库变更：查询该仓该产品可用量 */
const handleWarehouseChange = async (warehouseId: number) => {
  availableStock.value = null
  if (!warehouseId || !currentProduct.value?.productCode) return
  try {
    const res = await InventoryBalanceApi.getInventoryBalancePage({
      pageNo: 1,
      pageSize: 1,
      itemType: 'PRODUCT',
      productKeyword: currentProduct.value.productCode,
      warehouseId
    })
    const item = res?.list?.[0]
    if (item && item.productId === currentProduct.value.productId) {
      availableStock.value = item.quantity
    } else {
      availableStock.value = 0
    }
  } catch {
    availableStock.value = null
  }
}

/** 染厂变更：同步名称快照 */
const handleFactoryChange = (factoryId: number) => {
  const sup = supplierList.value.find((s) => s.id === factoryId)
  formData.dyeingFactoryName = sup?.companyName
}

/** 提交 */
const handleSubmit = async () => {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  if (!formData.greigeOrderId || !formData.warehouseId) return

  submitting.value = true
  try {
    const payload: GreigeDispatchRecordCreateVO = {
      greigeOrderId: formData.greigeOrderId,
      dyeingOrderId: formData.dyeingOrderId,
      warehouseId: formData.warehouseId,
      dispatchDate: formData.dispatchDate!,
      quantity: formData.quantity!,
      pieces: formData.pieces,
      dyeingFactoryId: formData.dyeingFactoryId,
      dyeingFactoryName: formData.dyeingFactoryName,
      remark: formData.remark
    }
    await DispatchApi.createGreigeDispatchRecord(payload)
    message.success('新增成功')
    dialogVisible.value = false
    emit('success')
  } finally {
    submitting.value = false
  }
}

/** 关闭清理 */
const handleClosed = () => {
  formData.greigeOrderId = null
  formData.dyeingOrderId = undefined
  formData.warehouseId = null
  formData.dispatchDate = formatDate(new Date(), 'YYYY-MM-DD')
  formData.quantity = undefined
  formData.pieces = undefined
  formData.dyeingFactoryId = undefined
  formData.dyeingFactoryName = undefined
  formData.remark = ''
  selectedGreigeOrder.value = null
  fixedDyeingOrderId.value = null
  availableStock.value = null
  formRef.value?.resetFields()
}

defineExpose({ openForGreigeOrder, openForDyeingOrder })
</script>
