<template>
  <el-form
    ref="formRef"
    :model="formData"
    :rules="rules"
    :disabled="isReadOnly"
    label-width="110px"
    class="dyeing-order-form"
    v-loading="loading"
  >
    <!-- 第一区：基本信息 -->
    <div class="section-title">基本信息</div>
    <el-row :gutter="20">
      <el-col :span="12">
        <el-form-item label="销售合同" prop="salesContractId">
          <el-select
            v-model="formData.salesContractId"
            placeholder="输入合同号搜索销售合同（必选）"
            clearable
            filterable
            remote
            reserve-keyword
            :remote-method="searchContracts"
            :loading="contractLoading"
            class="!w-full"
            @change="handleContractChange"
          >
            <el-option
              v-for="item in contractOptions"
              :key="item.id"
              :label="item.contractNo"
              :value="item.id!"
            >
              <span>{{ item.contractNo }}</span>
              <span v-if="item.clientName" class="option-client">({{ item.clientName }})</span>
            </el-option>
          </el-select>
        </el-form-item>
      </el-col>
      <el-col :span="12">
        <el-form-item label="客户名称">
          <el-input :model-value="formData.clientName || '—'" disabled placeholder="选合同后自动带出" />
        </el-form-item>
      </el-col>
      <el-col :span="12">
        <el-form-item label="承染方" prop="dyeingFactoryId">
          <el-select
            v-model="formData.dyeingFactoryId"
            placeholder="输入供应商名称搜索染厂（必选）"
            clearable
            filterable
            remote
            reserve-keyword
            :remote-method="searchSuppliers"
            :loading="supplierLoading"
            class="!w-full"
            @change="handleSupplierChange"
          >
            <el-option
              v-for="item in supplierOptions"
              :key="item.id"
              :label="item.companyName"
              :value="item.id!"
            >
              <span>{{ item.companyName }}</span>
              <span v-if="item.supplierNo" class="option-supplier-no">({{ item.supplierNo }})</span>
            </el-option>
          </el-select>
        </el-form-item>
      </el-col>
      <el-col :span="12">
        <el-form-item label="单据日期" prop="orderDate">
          <el-date-picker
            v-model="formData.orderDate"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="请选择单据日期"
            class="!w-full"
          />
        </el-form-item>
      </el-col>
      <el-col :span="12">
        <el-form-item label="交货日期" prop="deliveryDate">
          <el-date-picker
            v-model="formData.deliveryDate"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="请选择交货日期（可空）"
            class="!w-full"
          />
        </el-form-item>
      </el-col>
      <el-col :span="12">
        <el-form-item label="打色指示单" prop="colorInstructionId">
          <el-select
            v-model="formData.colorInstructionId"
            placeholder="输入单号/客户/品名搜索打色单（可空）"
            clearable
            filterable
            remote
            reserve-keyword
            :remote-method="searchColorInstructions"
            :loading="colorInstructionLoading"
            class="!w-full"
            @change="handleColorInstructionChange"
          >
            <el-option
              v-for="item in colorInstructionOptions"
              :key="item.id"
              :label="formatColorInstructionLabel(item)"
              :value="item.id!"
            />
          </el-select>
        </el-form-item>
      </el-col>
    </el-row>

    <!-- 第二区：产品规格 -->
    <div class="section-title">产品规格</div>
    <el-row :gutter="20">
      <el-col :span="12">
        <el-form-item label="产品编码" prop="productCode">
          <el-input v-model="formData.productCode" maxlength="64" placeholder="请输入产品编码" />
        </el-form-item>
      </el-col>
      <el-col :span="12">
        <el-form-item label="产品名称" prop="productName">
          <el-input v-model="formData.productName" maxlength="128" placeholder="请输入产品名称" />
        </el-form-item>
      </el-col>
      <el-col :span="12">
        <el-form-item label="成分" prop="composition">
          <el-input v-model="formData.composition" maxlength="128" placeholder="如 95棉5氨" />
        </el-form-item>
      </el-col>
      <el-col :span="12">
        <el-form-item label="幅宽" prop="width">
          <el-input v-model="formData.width" maxlength="64" placeholder="如 160cm 有效" />
        </el-form-item>
      </el-col>
      <el-col :span="12">
        <el-form-item label="克重" prop="gsm">
          <el-input v-model="formData.gsm" maxlength="64" placeholder="如 180g" />
        </el-form-item>
      </el-col>
      <el-col :span="12">
        <el-form-item label="缩水率" prop="shrinkage">
          <el-input v-model="formData.shrinkage" maxlength="64" placeholder="如 ≤5%" />
        </el-form-item>
      </el-col>
    </el-row>

    <!-- 颜色明细 -->
    <div class="section-title">颜色明细</div>
    <el-table
      :data="colorRows"
      :row-key="getRowKey"
      border
      size="small"
      class="color-table"
      empty-text="暂无颜色明细，可点击下方按钮添加（零颜色单据也可保存）"
    >
      <el-table-column label="行号" type="index" width="55" align="center" />
      <el-table-column label="客户色号" min-width="140">
        <template #default="{ row }">
          <el-input
            v-model="row.customerColorNo"
            :disabled="isReadOnly"
            maxlength="64"
            placeholder="从合同带出可改"
          />
        </template>
      </el-table-column>
      <el-table-column label="颜色名称" min-width="140">
        <template #default="{ row }">
          <el-input
            v-model="row.colorName"
            :disabled="isReadOnly"
            maxlength="128"
            placeholder="手填颜色名称"
          />
        </template>
      </el-table-column>
      <el-table-column label="本单染色数量" width="140">
        <template #default="{ row }">
          <el-input-number
            v-model="row.quantity"
            :disabled="isReadOnly"
            :precision="2"
            :controls="false"
            :min="0"
            class="!w-full"
            placeholder="成品口径"
          />
        </template>
      </el-table-column>
      <el-table-column label="匹数" width="110">
        <template #default="{ row }">
          <el-input-number
            v-model="row.pieces"
            :disabled="isReadOnly"
            :precision="0"
            :controls="false"
            :min="0"
            class="!w-full"
            placeholder="匹数"
          />
        </template>
      </el-table-column>
      <el-table-column label="投染数" width="140">
        <template #default="{ row }">
          <el-input-number
            v-model="row.dyeingInputQty"
            :disabled="isReadOnly"
            :precision="2"
            :controls="false"
            :min="0"
            class="!w-full"
            placeholder="实际投入量"
          />
        </template>
      </el-table-column>
      <el-table-column label="投染数单位" width="120">
        <template #default="{ row }">
          <el-select
            v-model="row.dyeingInputUnit"
            :disabled="isReadOnly"
            placeholder="单位"
            class="!w-full"
          >
            <el-option
              v-for="dict in unitDictOptions"
              :key="dict.value"
              :label="dict.label"
              :value="dict.value"
            />
          </el-select>
        </template>
      </el-table-column>
      <el-table-column label="合同数量" width="120" align="right">
        <template #default="{ row }">
          <span>{{ row.contractQty != null ? Number(row.contractQty).toFixed(2) : '—' }}</span>
        </template>
      </el-table-column>
      <el-table-column
        v-if="!isReadOnly"
        label="操作"
        width="80"
        align="center"
        fixed="right"
      >
        <template #default="{ $index }">
          <el-button type="danger" link @click="removeColor($index)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <div v-if="!isReadOnly" class="items-actions">
      <el-button type="primary" plain @click="addColor">新增颜色行</el-button>
    </div>
    <div class="items-summary">
      合计：本单染色数量 <b>{{ formatQty(totalQuantity) }}</b>
      ／ 投染数 <b>{{ formatQty(totalInputQty) }}</b>
      ／ 匹数 <b>{{ totalPieces }}</b>
    </div>

    <!-- 第三区：技术规格与色牢度 -->
    <div class="section-title">技术规格&色牢度</div>
    <el-row :gutter="20">
      <el-col :span="12">
        <el-form-item label="损耗率" prop="loss">
          <el-input v-model="formData.loss" maxlength="64" placeholder="如 5%" />
        </el-form-item>
      </el-col>
      <el-col :span="12">
        <el-form-item label="pH 值" prop="ph">
          <el-input v-model="formData.ph" maxlength="64" placeholder="如 6.5" />
        </el-form-item>
      </el-col>
      <el-col :span="12">
        <el-form-item label="缩水率" prop="shrinkage">
          <el-input v-model="formData.shrinkage" maxlength="64" placeholder="如 ≤5%" />
        </el-form-item>
      </el-col>
      <el-col :span="12">
        <el-form-item label="水洗牢度" prop="fastnessWash">
          <el-input v-model="formData.fastnessWash" maxlength="64" placeholder="如 4级" />
        </el-form-item>
      </el-col>
      <el-col :span="12">
        <el-form-item label="皂洗牢度" prop="fastnessSoap">
          <el-input v-model="formData.fastnessSoap" maxlength="64" placeholder="如 4级" />
        </el-form-item>
      </el-col>
      <el-col :span="12">
        <el-form-item label="干摩擦牢度" prop="fastnessDryRub">
          <el-input v-model="formData.fastnessDryRub" maxlength="64" placeholder="如 4级" />
        </el-form-item>
      </el-col>
      <el-col :span="12">
        <el-form-item label="湿摩擦牢度" prop="fastnessWetRub">
          <el-input v-model="formData.fastnessWetRub" maxlength="64" placeholder="如 3-4级" />
        </el-form-item>
      </el-col>
      <el-col :span="12">
        <el-form-item label="日晒牢度" prop="fastnessLight">
          <el-input v-model="formData.fastnessLight" maxlength="64" placeholder="如 4级" />
        </el-form-item>
      </el-col>
      <el-col :span="12">
        <el-form-item label="汗渍牢度" prop="fastnessPerspiration">
          <el-input v-model="formData.fastnessPerspiration" maxlength="64" placeholder="如 4级" />
        </el-form-item>
      </el-col>
      <el-col :span="12">
        <el-form-item label="干洗牢度" prop="fastnessDryClean">
          <el-input v-model="formData.fastnessDryClean" maxlength="64" placeholder="如 4级" />
        </el-form-item>
      </el-col>
      <el-col :span="12">
        <el-form-item label="六千沾色牢度" prop="fastnessSixFiber">
          <el-input v-model="formData.fastnessSixFiber" maxlength="64" placeholder="如 4级" />
        </el-form-item>
      </el-col>
      <el-col :span="24">
        <el-form-item label="其他要求" prop="otherReq">
          <el-input
            v-model="formData.otherReq"
            type="textarea"
            :rows="2"
            maxlength="512"
            placeholder="请输入其他技术或质量要求"
          />
        </el-form-item>
      </el-col>
    </el-row>

    <!-- 第四区：备注 -->
    <div class="section-title">备注</div>
    <el-row :gutter="20">
      <el-col :span="24">
        <el-form-item label="备注" prop="remark">
          <el-input
            v-model="formData.remark"
            type="textarea"
            :rows="3"
            maxlength="512"
            placeholder="请输入备注说明"
          />
        </el-form-item>
      </el-col>
    </el-row>

    <!-- 操作按钮 -->
    <div v-if="hasActions" class="form-actions">
      <!-- 新建模式 -->
      <template v-if="isCreateMode">
        <el-button type="primary" :loading="submitLoading" @click="handleSave('DRAFT')">
          保存草稿
        </el-button>
        <el-button type="success" :loading="submitLoading" @click="handleSave('ACTIVE')">
          下达
        </el-button>
        <el-button @click="handleCancel">取消</el-button>
      </template>

      <!-- 草稿态 (DRAFT)：[保存草稿][下达][删除] -->
      <template v-else-if="formData.status === 'DRAFT'">
        <el-button
          type="primary"
          :loading="submitLoading"
          @click="handleSave('DRAFT')"
          v-hasPermi="['lunaerp:production:dyeing-order:update']"
        >
          保存草稿
        </el-button>
        <el-button
          type="success"
          :loading="submitLoading"
          @click="handleEffect"
          v-hasPermi="['lunaerp:production:dyeing-order:update']"
        >
          下达
        </el-button>
        <el-button
          type="danger"
          :loading="submitLoading"
          @click="handleDelete"
          v-hasPermi="['lunaerp:production:dyeing-order:delete']"
        >
          删除
        </el-button>
      </template>

      <!-- 进行中态 (ACTIVE)：[完成][取消] -->
      <template v-else-if="formData.status === 'ACTIVE'">
        <el-button
          type="success"
          :loading="submitLoading"
          @click="handleComplete"
          v-hasPermi="['lunaerp:production:dyeing-order:update']"
        >
          完成
        </el-button>
        <el-button
          type="danger"
          :loading="submitLoading"
          @click="handleCancelOrder"
          v-hasPermi="['lunaerp:production:dyeing-order:update']"
        >
          取消
        </el-button>
      </template>
    </div>
  </el-form>
</template>

<script lang="ts" setup>
import type { FormInstance, FormRules } from 'element-plus'
import { DICT_TYPE, getStrDictOptions } from '@/utils/dict'
import { formatDate } from '@/utils/formatTime'
import * as DyeingOrderApi from '@/api/lunaerp-elmui/production/dyeing-order'
import type { DyeingOrderVO, DyeingColorVO } from '@/api/lunaerp-elmui/production/dyeing-order'
import * as SalesContractApi from '@/api/lunaerp-elmui/business/sales-contract'
import type { SalesContractVO } from '@/api/lunaerp-elmui/business/sales-contract'
import * as SupplierApi from '@/api/lunaerp-elmui/baseinfo/supplier'
import type { SupplierVO } from '@/api/lunaerp-elmui/baseinfo/supplier'
import * as ColorInstructionApi from '@/api/lunaerp-elmui/production/color-instruction'
import type { ColorInstructionVO } from '@/api/lunaerp-elmui/production/color-instruction'

defineOptions({ name: 'DyeingOrderBasicInfoTab' })

const props = defineProps<{
  orderId?: string | number | null
  isViewMode?: boolean
}>()

const emit = defineEmits<{
  (e: 'loaded', data: DyeingOrderVO): void
  (e: 'success', id: number): void
  (e: 'cancel'): void
}>()

const isCreateMode = computed(() => !props.orderId)
const isReadOnly = computed(
  () => props.isViewMode || (!isCreateMode.value && formData.value.status !== 'DRAFT')
)

const message = useMessage()

const loading = ref(false)
const submitLoading = ref(false)
const formRef = ref<FormInstance>()

const formData = ref<DyeingOrderVO>({
  salesContractId: undefined,
  contractNo: '',
  customerId: undefined,
  clientName: '',
  dyeingFactoryId: undefined as any,
  dyeingFactoryName: '',
  orderDate: formatDate(new Date(), 'YYYY-MM-DD'),
  deliveryDate: '',
  colorInstructionId: undefined,
  colorInstructionNo: '',
  productCode: '',
  productName: '',
  composition: '',
  width: '',
  gsm: '',
  shrinkage: '',
  loss: '',
  ph: '',
  fastnessWash: '',
  fastnessSoap: '',
  fastnessDryRub: '',
  fastnessWetRub: '',
  fastnessLight: '',
  fastnessPerspiration: '',
  fastnessDryClean: '',
  fastnessSixFiber: '',
  otherReq: '',
  remark: '',
  status: 'DRAFT'
})

// ==================== 颜色明细（聚合保存，ADR-0003 & 差异保存 ADR-0032） ====================
interface DyeingColorRowVO extends DyeingColorVO {
  _clientKey?: string
}

const colorRows = ref<DyeingColorRowVO[]>([])

let tempKeyCounter = 0
/** 颜色明细表格以行 id 为稳定 key；新增行未落库前以临时 key 保证行唯一性（ADR-0032） */
const getRowKey = (row: DyeingColorRowVO) => {
  if (row.id != null) {
    return row.id
  }
  if (!row._clientKey) {
    row._clientKey = `temp_${++tempKeyCounter}`
  }
  return row._clientKey
}

const unitDictOptions = computed(() => {
  const options = getStrDictOptions(DICT_TYPE.LUNAERP_SALES_CONTRACT_UNIT)
  if (options && options.length > 0) {
    return options
  }
  return [
    { label: 'KG', value: 'KG' },
    { label: 'M', value: 'M' }
  ]
})

const addColor = () => {
  colorRows.value.push({
    _clientKey: `temp_${++tempKeyCounter}`,
    id: undefined,
    customerColorNo: '',
    colorName: '',
    quantity: 0,
    pieces: undefined,
    dyeingInputQty: undefined,
    dyeingInputUnit: 'KG',
    contractQty: undefined
  })
}

const removeColor = (index: number) => {
  colorRows.value.splice(index, 1)
}

const toNumber = (val: number | undefined | null): number => (val == null ? 0 : Number(val))
const formatQty = (val: number): string => val.toFixed(2)

const totalQuantity = computed(() =>
  colorRows.value.reduce((sum, row) => sum + toNumber(row.quantity), 0)
)
const totalInputQty = computed(() =>
  colorRows.value.reduce((sum, row) => sum + toNumber(row.dyeingInputQty), 0)
)
const totalPieces = computed(() =>
  colorRows.value.reduce((sum, row) => sum + toNumber(row.pieces), 0)
)

const rules: FormRules = {
  salesContractId: [{ required: true, message: '请选择销售合同', trigger: 'change' }],
  dyeingFactoryId: [{ required: true, message: '请选择承染方（染厂）', trigger: 'change' }],
  orderDate: [{ required: true, message: '请选择单据日期', trigger: 'change' }]
}

// 销售合同远程搜索
const contractOptions = ref<SalesContractVO[]>([])
const contractLoading = ref(false)
const searchContracts = async (query: string) => {
  contractLoading.value = true
  try {
    const res = await SalesContractApi.getSalesContractPage({
      pageNo: 1,
      pageSize: 20,
      keyword: query || undefined
    })
    contractOptions.value = res.list || []
  } finally {
    contractLoading.value = false
  }
}

const handleContractChange = async (val?: number) => {
  if (!val) {
    formData.value.contractNo = ''
    formData.value.customerId = undefined
    formData.value.clientName = ''
    return
  }
  const found = contractOptions.value.find((c) => c.id === val)
  if (found) {
    formData.value.contractNo = found.contractNo || ''
    formData.value.customerId = found.customerId
    formData.value.clientName = found.clientName || ''
  }
  // 选销售合同后自动带出合同全部明细行：product_code/product_name/customer_color→customer_color_no、quantity→contract_qty
  try {
    const contract = await SalesContractApi.getSalesContract(val)
    if (contract) {
      if (contract.contractNo) formData.value.contractNo = contract.contractNo
      if (contract.customerId) formData.value.customerId = contract.customerId
      if (contract.clientName) formData.value.clientName = contract.clientName

      const items = contract.items || []
      if (items.length > 0) {
        // 主表产品规格：若未填写，从第一条明细带出
        if (!formData.value.productCode && items[0].productCode) {
          formData.value.productCode = items[0].productCode
        }
        if (!formData.value.productName && items[0].productName) {
          formData.value.productName = items[0].productName
        }
        if (!formData.value.composition && items[0].composition) {
          formData.value.composition = items[0].composition
        }
        if (!formData.value.width && items[0].width) {
          formData.value.width = items[0].width
        }
        if (!formData.value.gsm && items[0].weight) {
          formData.value.gsm = items[0].weight
        }

        // 把该合同全部明细行拷到染色单明细
        colorRows.value = items.map((item) => ({
          _clientKey: `temp_${++tempKeyCounter}`,
          id: undefined,
          customerColorNo: item.customerColor || '',
          colorName: '',
          quantity: item.quantity != null ? Number(item.quantity) : 0,
          pieces: undefined,
          dyeingInputQty: undefined,
          dyeingInputUnit: contract.unit || 'KG',
          contractQty: item.quantity != null ? Number(item.quantity) : 0
        }))
      } else {
        colorRows.value = []
      }
    }
  } catch (e) {
    console.error('加载销售合同明细失败', e)
  }
}

// 承染方（供应商）远程搜索
const supplierOptions = ref<SupplierVO[]>([])
const supplierLoading = ref(false)
const searchSuppliers = async (query: string) => {
  supplierLoading.value = true
  try {
    const res = await SupplierApi.getSupplierPage({
      pageNo: 1,
      pageSize: 20,
      search: query || undefined,
      status: 'ACTIVE'
    })
    supplierOptions.value = res.list || []
  } finally {
    supplierLoading.value = false
  }
}

const handleSupplierChange = (val?: number) => {
  if (!val) {
    formData.value.dyeingFactoryName = ''
    return
  }
  const found = supplierOptions.value.find((s) => s.id === val)
  if (found) {
    formData.value.dyeingFactoryName = found.companyName || ''
  }
}

// 打色指示单远程搜索
const colorInstructionOptions = ref<ColorInstructionVO[]>([])
const colorInstructionLoading = ref(false)
const formatColorInstructionLabel = ColorInstructionApi.formatColorInstructionOptionLabel

const searchColorInstructions = async (query: string) => {
  colorInstructionLoading.value = true
  try {
    const res = await ColorInstructionApi.getColorInstructionPage({
      pageNo: 1,
      pageSize: 20,
      keyword: query || undefined,
      statusList: ['CONFIRMED', 'IN_PRODUCTION']
    })
    colorInstructionOptions.value = res.list || []
  } finally {
    colorInstructionLoading.value = false
  }
}

const handleColorInstructionChange = (val?: number) => {
  if (!val) {
    formData.value.colorInstructionId = undefined
    formData.value.colorInstructionNo = ''
    return
  }
  const found = colorInstructionOptions.value.find((c) => c.id === val)
  formData.value.colorInstructionId = val
  formData.value.colorInstructionNo = found?.instructionNo || ''
}

// 详情加载
const loadDetail = async () => {
  if (!props.orderId) return
  loading.value = true
  try {
    const res = await DyeingOrderApi.getDyeingOrder(Number(props.orderId))
    formData.value = res
    colorRows.value = (res.colors || []).map((color) => ({
      id: color.id,
      dyeingOrderId: color.dyeingOrderId,
      lineNo: color.lineNo,
      customerColorNo: color.customerColorNo || '',
      colorName: color.colorName || '',
      quantity: color.quantity != null ? Number(color.quantity) : 0,
      pieces: color.pieces != null ? Number(color.pieces) : undefined,
      dyeingInputQty: color.dyeingInputQty != null ? Number(color.dyeingInputQty) : undefined,
      dyeingInputUnit: color.dyeingInputUnit || 'KG',
      contractQty: color.contractQty != null ? Number(color.contractQty) : undefined
    }))
    emit('loaded', res)
    if (res.salesContractId && res.contractNo) {
      contractOptions.value = [
        {
          id: res.salesContractId,
          contractNo: res.contractNo,
          customerId: res.customerId || 0,
          clientName: res.clientName
        }
      ]
    }
    if (res.dyeingFactoryId && res.dyeingFactoryName) {
      supplierOptions.value = [
        {
          id: res.dyeingFactoryId,
          companyName: res.dyeingFactoryName
        }
      ]
    }
    if (res.colorInstructionId) {
      colorInstructionOptions.value = [
        {
          id: res.colorInstructionId,
          instructionNo: res.colorInstructionNo || ''
        } as ColorInstructionVO
      ]
    }
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  if (isCreateMode.value) {
    searchColorInstructions('')
  }
  loadDetail()
})

// 保存/下达
const buildSubmitData = (status: 'DRAFT' | 'ACTIVE'): DyeingOrderVO => ({
  ...formData.value,
  status,
  colorInstructionId:
    formData.value.colorInstructionId != null ? formData.value.colorInstructionId : null,
  colorInstructionNo: formData.value.colorInstructionNo?.trim() || null,
  colors: colorRows.value.map((row) => ({
    id: row.id != null ? row.id : undefined,
    dyeingOrderId: row.dyeingOrderId,
    lineNo: row.lineNo,
    customerColorNo: row.customerColorNo || '',
    colorName: row.colorName || '',
    quantity: row.quantity != null ? Number(row.quantity) : 0,
    pieces: row.pieces != null ? Number(row.pieces) : undefined,
    dyeingInputQty: row.dyeingInputQty != null ? Number(row.dyeingInputQty) : undefined,
    dyeingInputUnit: row.dyeingInputUnit || undefined,
    contractQty: row.contractQty != null ? Number(row.contractQty) : undefined
  }))
})

const handleSave = async (status: 'DRAFT' | 'ACTIVE') => {
  if (!formRef.value) return
  await formRef.value.validate()

  submitLoading.value = true
  try {
    const submitData = buildSubmitData(status)
    if (isCreateMode.value) {
      const id = await DyeingOrderApi.createDyeingOrder(submitData)
      message.success(status === 'ACTIVE' ? '染色单下达成功' : '染色单保存为草稿成功')
      emit('success', id)
    } else {
      await DyeingOrderApi.updateDyeingOrder(submitData)
      message.success('染色单修改成功')
      await loadDetail()
    }
  } finally {
    submitLoading.value = false
  }
}

const hasActions = computed(() => {
  if (props.isViewMode) return false
  if (isCreateMode.value) return true
  return formData.value.status === 'DRAFT' || formData.value.status === 'ACTIVE'
})

const handleEffect = async () => {
  try {
    await message.confirm('确认下达该染色单吗？下达后将进入进行中状态，且不可修改。')
  } catch {
    return
  }

  submitLoading.value = true
  try {
    await DyeingOrderApi.effectDyeingOrder(Number(props.orderId))
    message.success('染色单下达成功')
    await loadDetail()
  } finally {
    submitLoading.value = false
  }
}

const handleDelete = async () => {
  try {
    await message.delConfirm('确认删除该染色单草稿吗？')
  } catch {
    return
  }
  submitLoading.value = true
  try {
    await DyeingOrderApi.deleteDyeingOrder(Number(props.orderId))
    message.success('删除成功')
    emit('cancel')
  } finally {
    submitLoading.value = false
  }
}

const handleComplete = async () => {
  try {
    await message.confirm('确认将该染色单标记为已完成吗？')
  } catch {
    return
  }
  submitLoading.value = true
  try {
    await DyeingOrderApi.completeDyeingOrder(Number(props.orderId))
    message.success('染色单已完成')
    await loadDetail()
  } finally {
    submitLoading.value = false
  }
}

const handleCancelOrder = async () => {
  try {
    await message.confirm('确认取消该染色单吗？取消后单据将变为已取消状态。')
  } catch {
    return
  }
  submitLoading.value = true
  try {
    await DyeingOrderApi.cancelDyeingOrder(Number(props.orderId))
    message.success('染色单已取消')
    await loadDetail()
  } finally {
    submitLoading.value = false
  }
}

const handleCancel = () => {
  emit('cancel')
}
</script>

<style lang="scss" scoped>
.section-title {
  margin: 16px 0 12px;
  padding-left: 10px;
  font-size: 14px;
  font-weight: 600;
  color: var(--el-text-color-primary);
  border-left: 3px solid var(--el-color-primary);

  &:first-child {
    margin-top: 0;
  }
}

.option-client,
.option-supplier-no {
  margin-left: 8px;
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.color-table {
  margin-bottom: 12px;
}

.items-actions {
  margin-bottom: 12px;
}

.items-summary {
  margin-bottom: 16px;
  padding: 8px 12px;
  font-size: 13px;
  color: var(--el-text-color-regular);
  background-color: var(--el-fill-color-light);
  border-radius: 4px;

  b {
    color: var(--el-color-primary);
  }
}

.form-actions {
  margin-top: 24px;
  padding-top: 16px;
  border-top: 1px solid var(--el-border-color-lighter);
  text-align: center;
}
</style>
